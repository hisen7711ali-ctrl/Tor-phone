package org.torphone.app.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns Tor-phone's Android VPN interface.
 *
 * The TUN reader is real, but no packet router is installed until a Tor
 * transport and a compatible packet bridge are selected and integrated.
 * The default router therefore fails closed instead of leaking traffic.
 */
public final class TorPhoneVpnService extends VpnService {
    public static final String ACTION_START = "org.torphone.app.vpn.START";
    public static final String ACTION_STOP = "org.torphone.app.vpn.STOP";

    private static final String CHANNEL_ID = "tor_phone_vpn";
    private static final int NOTIFICATION_ID = 7001;
    private static final int TUN_MTU = 1500;
    private static final int MAX_PACKET_SIZE = 32767;

    private final AtomicBoolean packetLoopRunning = new AtomicBoolean(false);
    private final PacketRouter packetRouter = new UnconfiguredPacketRouter();

    private ParcelFileDescriptor tunInterface;
    private Thread packetThread;

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
            startPacketLoop();
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

    private void startPacketLoop() throws IOException {
        if (tunInterface == null) {
            throw new IOException("TUN interface is not established");
        }
        if (!packetLoopRunning.compareAndSet(false, true)) {
            return;
        }

        FileInputStream tunInput =
                new FileInputStream(tunInterface.getFileDescriptor());
        FileOutputStream tunOutput =
                new FileOutputStream(tunInterface.getFileDescriptor());

        packetThread = new Thread(
                () -> readAndRoutePackets(tunInput, tunOutput),
                "TorPhone-TUN-Reader"
        );
        packetThread.start();
    }

    private void readAndRoutePackets(
            FileInputStream tunInput,
            FileOutputStream tunOutput
    ) {
        byte[] packetBuffer = new byte[MAX_PACKET_SIZE];

        try {
            while (packetLoopRunning.get()) {
                int packetLength = tunInput.read(packetBuffer);
                if (packetLength < 0) {
                    throw new EOFException("TUN interface reached end of stream");
                }
                if (packetLength == 0) {
                    continue;
                }

                // The router must consume the packet synchronously or copy it.
                packetRouter.route(packetBuffer, packetLength, tunOutput);
            }
        } catch (IOException | RuntimeException failure) {
            if (packetLoopRunning.get()) {
                stopTunnel();
                stopSelf();
            }
        }
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Tor-phone")
                .setContentText("واجهة الشبكة تعمل؛ موجه الحزم غير متاح، والحماية غير نشطة")
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
        packetLoopRunning.set(false);

        Thread currentPacketThread = packetThread;
        packetThread = null;
        if (currentPacketThread != null
                && currentPacketThread != Thread.currentThread()) {
            currentPacketThread.interrupt();
        }

        if (tunInterface != null) {
            try {
                tunInterface.close();
            } catch (IOException ignored) {
                // Closing the descriptor is best-effort during shutdown.
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

    private interface PacketRouter {
        void route(byte[] packet, int length, FileOutputStream tunOutput)
                throws IOException;
    }

    /**
     * No traffic is forwarded until a reviewed packet-to-stream bridge exists.
     * Throwing here closes the VPN instead of allowing a direct-network fallback.
     */
    private static final class UnconfiguredPacketRouter implements PacketRouter {
        @Override
        public void route(
                byte[] packet,
                int length,
                FileOutputStream tunOutput
        ) throws IOException {
            throw new IOException(
                    "No packet router is configured; refusing to forward traffic"
            );
        }
    }
}
