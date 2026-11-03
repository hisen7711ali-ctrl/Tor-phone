package org.torphone.app.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.io.IOException;

/**
 * Owns Tor-phone's Android VPN interface.
 *
 * The TUN interface is deliberately not advertised as protected: packet
 * forwarding is not connected yet. The service notification says so plainly.
 */
public final class TorPhoneVpnService extends VpnService {
    public static final String ACTION_START = "org.torphone.app.vpn.START";
    public static final String ACTION_STOP = "org.torphone.app.vpn.STOP";

    private static final String CHANNEL_ID = "tor_phone_vpn";
    private static final int NOTIFICATION_ID = 7001;
    private static final int TUN_MTU = 1500;

    private ParcelFileDescriptor tunInterface;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || ACTION_STOP.equals(intent.getAction())) {
            stopTunnel();
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        if (!ACTION_START.equals(intent.getAction())) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());

        try {
            establishTunInterface();
        } catch (Exception error) {
            stopTunnel();
            stopSelf(startId);
        }

        return START_NOT_STICKY;
    }

    private void establishTunInterface() throws IOException {
        if (tunInterface != null) {
            return;
        }

        Builder builder = new Builder()
                .setSession("Tor-phone")
                .setMtu(TUN_MTU)
                .addAddress("10.77.0.2", 32)
                .addAddress("fd77:7070:7070::2", 128)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDnsServer("10.77.0.1")
                .addDnsServer("fd77:7070:7070::1");

        tunInterface = builder.establish();
        if (tunInterface == null) {
            throw new IOException("Android did not establish the VPN interface");
        }
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Tor-phone")
                .setContentText("واجهة الشبكة مهيأة، لكن تمرير البيانات غير جاهز")
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "اتصال Tor-phone",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("حالة واجهة الشبكة الافتراضية");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void stopTunnel() {
        if (tunInterface != null) {
            try {
                tunInterface.close();
            } catch (IOException ignored) {
                // The descriptor is closed best-effort during shutdown.
            }
            tunInterface = null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    @Override
    public void onRevoke() {
        stopTunnel();
        stopSelf();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        stopTunnel();
        super.onDestroy();
    }
}
