package org.torphone.app.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import org.torphone.app.R;

import org.torproject.onionmasq.ISocketProtect;
import org.torproject.onionmasq.OnionMasq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class TorPhoneVpnService extends VpnService {
    public static final String ACTION_START = "org.torphone.app.vpn.START";
    public static final String ACTION_STOP = "org.torphone.app.vpn.STOP";
    public static final String ACTION_RESTART = "org.torphone.app.vpn.RESTART";
    public static final String ACTION_STATUS = "org.torphone.app.vpn.STATUS";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_SENT = "bytes_sent";
    public static final String EXTRA_RECEIVED = "bytes_received";

    private static final String CHANNEL_ID = "tor_phone_vpn";
    private static final int NOTIFICATION_ID = 7001;
    private static final int TUN_MTU = 1500;
    private static final String PREFS = "torphone_preferences";
    private static volatile String lastStatus;

    public static String getLastStatus() { return lastStatus; }

    private final OnionmasqRouter packetRouter = new OnionmasqRouter();
    private final AtomicBoolean startupInProgress = new AtomicBoolean(false);
    private final IBinder serviceBinder = new ServiceBinder(this);
    private final AtomicLong tunnelGeneration = new AtomicLong();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ParcelFileDescriptor tunInterface;
    private volatile boolean onionmasqBound;
    private volatile boolean tunnelActive;

    private final Runnable trafficTicker = new Runnable() {
        @Override public void run() {
            if (!tunnelActive) return;
            publishStatusBroadcast(lastStatus);
            mainHandler.postDelayed(this, 2000L);
        }
    };

    private static final class ServiceBinder extends Binder implements ISocketProtect {
        private final WeakReference<TorPhoneVpnService> service;
        ServiceBinder(TorPhoneVpnService owner) { service = new WeakReference<>(owner); }
        @Override public boolean protect(int socket) {
            TorPhoneVpnService owner = service.get();
            return owner != null && owner.protectSocket(socket);
        }
    }

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        publishStatus(getString(R.string.state_disconnected));
        packetRouter.setFailureListener(error -> {
            publishStatus(getString(R.string.status_engine_failed));
            stopSelf();
        });
    }

    @Override public IBinder onBind(Intent intent) {
        if (intent != null && "android.net.VpnService".equals(intent.getAction())) return super.onBind(intent);
        return serviceBinder;
    }

    private boolean protectSocket(int socket) { return super.protect(socket); }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_RESTART.equals(action)) {
            stopTunnel();
            publishStatus(getString(R.string.state_disconnected));
            startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.notification_engine_starting)));
            mainHandler.postDelayed(() -> {
                if (packetRouter.init(getApplicationContext()) != OnionmasqRouter.INIT_OK) {
                    publishStatus(getString(R.string.status_engine_init_failed));
                    stopSelf(startId);
                    return;
                }
                if (!onionmasqBound) {
                    try {
                        OnionMasq.bindVPNService(TorPhoneVpnService.class);
                        onionmasqBound = true;
                    } catch (Throwable error) {
                        publishStatus(getString(R.string.status_socket_protection_failed));
                        stopSelf(startId);
                        return;
                    }
                }
                startupInProgress.set(false);
                if (startupInProgress.compareAndSet(false, true)) {
                    new Thread(() -> startTunnel(startId), "TorPhone-VPN-Restart").start();
                }
            }, 1200L);
            return START_NOT_STICKY;
        }

        if (action == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isAlwaysOn()) {
            action = ACTION_START;
        }
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
        final long generation = tunnelGeneration.incrementAndGet();
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

            Set<String> bypassPackages = new HashSet<>(getSharedPreferences(
                    PREFS, MODE_PRIVATE).getStringSet(
                    "split_tunnel_bypass_packages", java.util.Collections.emptySet()));
            for (String packageName : bypassPackages) {
                if (packageName == null || packageName.isEmpty() || packageName.equals(getPackageName())) continue;
                try { builder.addDisallowedApplication(packageName); }
                catch (PackageManager.NameNotFoundException ignored) { }
            }

            tunInterface = builder.establish();
            if (tunInterface == null) throw new IOException(getString(R.string.status_engine_failed));

            android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            String bridgeLines = prefs.getBoolean("use_bridges", false)
                    ? prefs.getString("bridge_lines", null) : null;
            if (bridgeLines != null && bridgeLines.trim().isEmpty()) bridgeLines = null;

            packetRouter.start(tunInterface, bridgeLines);
            tunInterface.close();
            tunInterface = null;

            if (!packetRouter.isRunning()) throw new IOException(getString(R.string.status_engine_failed));
            tunnelActive = true;
            prefs.edit().putBoolean("vpn_active", true).apply();
            publishStatus(getString(R.string.status_engine_running_unverified));
            mainHandler.removeCallbacks(trafficTicker);
            mainHandler.post(trafficTicker);
            new Thread(() -> verifyTorExit(generation), "TorPhone-IP-Verification").start();
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.notify(NOTIFICATION_ID,
                    buildNotification(getString(R.string.notification_engine_running)));
        } catch (Throwable error) {
            if (generation == tunnelGeneration.get()) {
                publishStatus(getString(R.string.status_engine_failed));
                stopTunnel();
                stopSelf(startId);
            }
        } finally {
            startupInProgress.set(false);
        }
    }

    /**
     * This request is intentionally NOT protected with VpnService.protect().
     * It must follow the VPN routes; a direct underlying-network request could
     * falsely report a Tor exit.
     */
    private void verifyTorExit(long generation) {
        boolean verified = false;
        for (int attempt = 0; attempt < 3 && tunnelActive && generation == tunnelGeneration.get(); attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL("https://check.torproject.org/api/ip").openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");
                int code = connection.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) throw new IOException("Tor check HTTP " + code);
                StringBuilder body = new StringBuilder();
                try (InputStream stream = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                }
                verified = new JSONObject(body.toString()).optBoolean("IsTor", false);
                break;
            } catch (Exception error) {
                if (attempt < 2 && tunnelActive) {
                    try { Thread.sleep(3000L); }
                    catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        if (!tunnelActive || generation != tunnelGeneration.get() || !packetRouter.isRunning()) return;
        publishStatus(verified
                ? getString(R.string.status_verified_protected)
                : getString(R.string.status_engine_running_unverified));
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification(getString(
                verified ? R.string.notification_verified_protected : R.string.notification_engine_running)));
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
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.notification_channel_description));
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void publishStatus(String status) {
        lastStatus = status;
        publishStatusBroadcast(status);
    }

    private void publishStatusBroadcast(String status) {
        Intent update = new Intent(ACTION_STATUS);
        update.setPackage(getPackageName());
        update.putExtra(EXTRA_STATUS, status);
        if (tunnelActive) {
            try {
                update.putExtra(EXTRA_SENT, OnionMasq.getBytesSent());
                update.putExtra(EXTRA_RECEIVED, OnionMasq.getBytesReceived());
            } catch (Throwable ignored) { }
        } else {
            update.putExtra(EXTRA_SENT, 0L);
            update.putExtra(EXTRA_RECEIVED, 0L);
        }
        sendBroadcast(update);
    }

    private void stopTunnel() {
        tunnelGeneration.incrementAndGet();
        tunnelActive = false;
        mainHandler.removeCallbacks(trafficTicker);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("vpn_active", false).apply();
        packetRouter.stop();
        if (tunInterface != null) {
            try { tunInterface.close(); } catch (IOException ignored) { }
            tunInterface = null;
        }
        if (onionmasqBound) {
            try { OnionMasq.unbindVPNService(); } catch (Throwable ignored) { }
            onionmasqBound = false;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    @Override public void onRevoke() {
        stopTunnel();
        publishStatus(getString(R.string.status_vpn_permission_revoked));
        stopSelf();
        super.onRevoke();
    }

    @Override public void onDestroy() {
        stopTunnel();
        if (lastStatus != null && (lastStatus.equals(getString(R.string.status_engine_starting))
                || lastStatus.equals(getString(R.string.status_engine_running_unverified))
                || lastStatus.equals(getString(R.string.status_verified_protected)))) {
            publishStatus(getString(R.string.state_disconnected));
        }
        super.onDestroy();
    }
}
