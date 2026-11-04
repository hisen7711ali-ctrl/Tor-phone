package org.torphone.app.vpn;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;

import com.wireguard.android.backend.GoBackend;
import com.wireguard.android.backend.Tunnel;
import com.wireguard.android.backend.Statistics;
import com.wireguard.config.Config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;

/** Standalone WireGuard backend. Android permits only one active VPN interface per user. */
public final class WireGuardBackend {
    public static final String STATUS_CONNECTED = "wireguard_connected";
    public static final String STATUS_DISCONNECTED = "wireguard_disconnected";
    private static final String PREFS = "torphone_preferences";
    private static final String TUNNEL_NAME = "torphone-wg";
    private static volatile WireGuardBackend instance;

    private final Context app;
    private final GoBackend backend;
    private final Tunnel tunnel = new Tunnel() {
        @Override public String getName() { return TUNNEL_NAME; }
        @Override public void onStateChange(State state) {
            boolean up = state == State.UP;
            preferences().edit().putBoolean("wireguard_active", up).apply();
            publish(up ? STATUS_CONNECTED : STATUS_DISCONNECTED);
        }
    };

    private WireGuardBackend(Context context) {
        app = context.getApplicationContext();
        backend = new GoBackend(app);
    }

    private static synchronized WireGuardBackend get(Context context) {
        if (instance == null) instance = new WireGuardBackend(context);
        return instance;
    }

    private SharedPreferences preferences() {
        return app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void connect(Context context, File configFile) throws Exception {
        WireGuardBackend self = get(context);
        if (VpnService.prepare(self.app) != null)
            throw new IllegalStateException("VPN permission has not been granted");
        if (configFile == null || !configFile.isFile())
            throw new IllegalArgumentException("WireGuard config file is missing");

        Config config;
        try (FileInputStream input = new FileInputStream(configFile)) {
            config = Config.parse(input);
        }

        self.preferences().edit()
                .putString("vpn_mode", "wireguard")
                .putString("wireguard_profile", configFile.getName())
                .apply();

        // Stop the Tor VpnService before establishing WireGuard's VpnService.
        self.app.stopService(new Intent(self.app, TorPhoneVpnService.class));
        self.backend.setState(self.tunnel, Tunnel.State.UP, config);
    }

    public static void disconnect(Context context) throws Exception {
        WireGuardBackend self = get(context);
        if (self.backend.getState(self.tunnel) == Tunnel.State.UP)
            self.backend.setState(self.tunnel, Tunnel.State.DOWN, null);
        self.preferences().edit().putBoolean("wireguard_active", false).apply();
        self.publish(STATUS_DISCONNECTED);
    }

    public static boolean isConnected(Context context) {
        try {
            return get(context).backend.getState(get(context).tunnel) == Tunnel.State.UP;
        } catch (Exception ignored) {
            return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getBoolean("wireguard_active", false);
        }
    }

    public static long getTxBytes(Context context) {
        try {
            WireGuardBackend self = get(context);
            Statistics stats = self.backend.getStatistics(self.tunnel);
            return stats.totalTx();
        } catch (Exception ignored) { return 0L; }
    }

    public static long getRxBytes(Context context) {
        try {
            WireGuardBackend self = get(context);
            Statistics stats = self.backend.getStatistics(self.tunnel);
            return stats.totalRx();
        } catch (Exception ignored) { return 0L; }
    }

    private void publish(String status) {
        Intent update = new Intent(TorPhoneVpnService.ACTION_STATUS);
        update.setPackage(app.getPackageName());
        update.putExtra(TorPhoneVpnService.EXTRA_STATUS, status);
        update.putExtra(TorPhoneVpnService.EXTRA_SENT, getTxBytes(app));
        update.putExtra(TorPhoneVpnService.EXTRA_RECEIVED, getRxBytes(app));
        update.putExtra(TorPhoneVpnService.EXTRA_EXIT_IP, "");
        app.sendBroadcast(update);
    }
}
