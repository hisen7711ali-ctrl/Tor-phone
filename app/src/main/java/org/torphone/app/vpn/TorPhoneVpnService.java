package org.torphone.app.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import org.torphone.app.R;

import org.torproject.onionmasq.ISocketProtect;
import org.torproject.onionmasq.OnionMasq;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TorPhoneVpnService extends VpnService {
    public static final String ACTION_START = "org.torphone.app.vpn.START";
    public static final String ACTION_STOP = "org.torphone.app.vpn.STOP";
    public static final String ACTION_STATUS = "org.torphone.app.vpn.STATUS";
    public static final String EXTRA_STATUS = "status";

    private static final String CHANNEL_ID = "tor_phone_vpn";
    private static final int NOTIFICATION_ID = 7001;
    private static final int TUN_MTU = 1500;
    private static volatile String lastStatus;

    public static String getLastStatus() {
        return lastStatus;
    }

    private final OnionmasqRouter packetRouter = new OnionmasqRouter();
    private final AtomicBoolean startupInProgress = new AtomicBoolean(false);
    private final IBinder serviceBinder = new ServiceBinder(this);

    private ParcelFileDescriptor tunInterface;
    private volatile boolean onionmasqBound;

    private static final class ServiceBinder extends Binder implements ISocketProtect {
        private final WeakReference<TorPhoneVpnService> service;

        ServiceBinder(TorPhoneVpnService owner) {
            service = new WeakReference<>(owner);
        }

        @Override
        public boolean protect(int socket) {
            TorPhoneVpnService owner = service.get();
            return owner != null && owner.protectSocket(socket);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        publishStatus(getString(R.string.state_disconnected));
        packetRouter.setFailureListener(error -> {
            publishStatus(getString(R.string.status_engine_failed));
            stopSelf();
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent != null && "android.net.VpnService".equals(intent.getAction())) {
            return super.onBind(intent);
        }
        return serviceBinder;
    }

    private boolean protectSocket(int socket) {
        return super.protect(socket);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopTunnel();
            publishStatus(getString(R.string.state_disconnected));
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (!ACTION_START.equals(action)) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        // Native loading and Arti initialization must succeed before TUN exists.
        if (packetRouter.init(getApplicationContext()) != OnionmasqRouter.INIT_OK) {
            publishStatus(getString(R.string.status_engine_init_failed));
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        if (!onionmasqBound) {
            try {
                OnionMasq.bindVPNService(TorPhoneVpnService.class);
                onionmasqBound = true;
            } catch (Throwable error) {
                publishStatus(getString(R.string.status_socket_protection_failed));
                stopSelf(startId);
                return START_NOT_STICKY;
            }
        }

        publishStatus(getString(R.string.status_engine_starting));
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.notification_engine_starting)));
        if (startupInProgress.compareAndSet(false, true)) {
            new Thread(() -> startTunnel(startId), "TorPhone-VPN-Startup").start();
        }
        return START_NOT_STICKY;
    }

    private void startTunnel(int startId) {
        try {
            Builder builder = new Builder()
                    .setSession(getString(R.string.app_name))
                    .setMtu(TUN_MTU)
                    .addAddress("169.254.42.1", 16)
                    .addAddress("fc00::", 7)
                    .addRoute("0.0.0.0", 0)
                    .addRoute("::", 0)
                    .addDnsServer("169.254.42.53")
                    .addDnsServer("fe80::53");

            tunInterface = builder.establish();
            if (tunInterface == null) {
                throw new IOException(getString(R.string.status_engine_failed));
            }

            packetRouter.start(tunInterface);
            tunInterface.close();
            tunInterface = null;

            if (!packetRouter.isRunning()) {
                throw new IOException(getString(R.string.status_engine_failed));
            }
            publishStatus(getString(R.string.status_engine_running_unverified));
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID,
                        buildNotification(getString(R.string.notification_engine_running)));
            }
        } catch (Throwable error) {
            publishStatus(getString(R.string.status_engine_failed));
            stopTunnel();
            stopSelf(startId);
        } finally {
            startupInProgress.set(false);
        }
    }

    private Notification buildNotification(String status) {
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(status)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notification_channel_description));
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void publishStatus(String status) {
        lastStatus = status;
        Intent update = new Intent(ACTION_STATUS);
        update.setPackage(getPackageName());
        update.putExtra(EXTRA_STATUS, status);
        sendBroadcast(update);
    }

    private void stopTunnel() {
        packetRouter.stop();
        if (tunInterface != null) {
            try {
                tunInterface.close();
            } catch (IOException ignored) {
                // Descriptor cleanup is best-effort during shutdown.
            }
            tunInterface = null;
        }
        if (onionmasqBound) {
            try {
                OnionMasq.unbindVPNService();
            } catch (Throwable ignored) {
                // The library may already have been shut down.
            }
            onionmasqBound = false;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    @Override
    public void onRevoke() {
        stopTunnel();
        publishStatus(getString(R.string.status_vpn_permission_revoked));
        stopSelf();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        stopTunnel();
        if (lastStatus != null && (lastStatus.equals(getString(R.string.status_engine_starting))
                || lastStatus.equals(getString(R.string.status_engine_running_unverified)))) {
            publishStatus(getString(R.string.state_disconnected));
        }
        super.onDestroy();
    }
}
