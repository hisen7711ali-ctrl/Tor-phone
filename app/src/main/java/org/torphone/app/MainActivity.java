package org.torphone.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.torphone.app.vpn.OnionmasqRouter;
import org.torphone.app.vpn.TorPhoneVpnService;

public final class MainActivity extends Activity {
    private static final int VPN_PERMISSION_REQUEST = 7101;

    private final int bg = Color.rgb(8, 19, 33);
    private final int surface = Color.rgb(17, 31, 48);
    private final int teal = Color.rgb(50, 214, 194);
    private final int muted = Color.rgb(145, 163, 184);
    private TextView modeInfo;
    private boolean statusReceiverRegistered;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (modeInfo != null && intent != null) {
                modeInfo.setText(intent.getStringExtra(TorPhoneVpnService.EXTRA_STATUS));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        render();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(TorPhoneVpnService.ACTION_STATUS);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }
        statusReceiverRegistered = true;
    }

    @Override
    protected void onStop() {
        if (statusReceiverRegistered) {
            unregisterReceiver(statusReceiver);
            statusReceiverRegistered = false;
        }
        super.onStop();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(25), dp(22), dp(30));
        page.setBackgroundColor(bg);
        scroll.addView(page);

        TextView brand = label("TOR  /  PHONE", 12, teal, true);
        brand.setLetterSpacing(.18f);
        page.addView(brand);
        TextView heading = label("Privacy,\nunder your control.", 30, Color.WHITE, true);
        heading.setPadding(0, dp(15), 0, dp(8));
        page.addView(heading);
        page.addView(label("Your privacy connection dashboard.", 14, muted, false));

        LinearLayout state = panel();
        state.setPadding(dp(18), dp(18), dp(18), dp(18));
        state.addView(label("PROTECTION STATUS", 11, muted, true));
        TextView disconnected = label("Protection is not active", 21, Color.WHITE, true);
        disconnected.setPadding(0, dp(9), 0, dp(5));
        state.addView(disconnected);
        state.addView(label("VPN permission alone does not mean traffic is protected. Tor startup and network readiness are checked separately.", 13, muted, false));
        page.addView(state, margin(-1, -2, 0, 25, 0, 22));

        page.addView(label("CONNECTION MODES", 11, muted, true));
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        Button vpn = modeButton("VPN");
        Button tor = modeButton("Tor");
        choices.addView(vpn, new LinearLayout.LayoutParams(0, dp(52), 1));
        View gap = new View(this);
        choices.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));
        choices.addView(tor, new LinearLayout.LayoutParams(0, dp(52), 1));
        page.addView(choices, margin(-1, -2, 0, 11, 0, 0));
        modeInfo = label("Select a connection mode", 13, muted, false);
        modeInfo.setPadding(dp(2), dp(5), 0, dp(18));
        page.addView(modeInfo);
        vpn.setOnClickListener(v -> modeInfo.setText("VPN selected"));
        tor.setOnClickListener(v -> modeInfo.setText("Tor selected"));

        Button startVpn = modeButton("بدء اتصال Tor");
        startVpn.setTextColor(bg);
        startVpn.setBackground(shape(teal, 16));
        page.addView(startVpn, margin(-1, 56, 0, 0, 0, 22));
        startVpn.setOnClickListener(v -> requestVpnPermission());

        page.addView(label("PRIVACY CENTER", 11, muted, true));
        addInfo(page, "Tor protection", "Protection is not reported until Onionmasq confirms that its native proxy is running.");
        addInfo(page, "VPN providers", "VPN connections require a real provider integration or a user-imported configuration.");
        addInfo(page, "Your own server", "Server configuration is accepted only from a real provider or a configuration supplied by the user.");
        addInfo(page, "Network status", "The application reports the actual engine state rather than treating a button press as proof.");
        addInfo(page, "Location privacy", "Network routing does not automatically change GPS or other device-location signals.");
        addInfo(page, "Emergency protection", "If the engine fails, the VPN tunnel is closed and no direct-network fallback is used.");
        TextView footer = label("TRANSPARENT STATUS  •  USER-CONTROLLED PERMISSIONS", 10, muted, true);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(24), 0, 0);
        page.addView(footer);
        setContentView(scroll);
    }

    private void requestVpnPermission() {
        if (OnionmasqRouter.prepare(this) != OnionmasqRouter.INIT_OK) {
            String message = "فشل المحرك: " + OnionmasqRouter.loadFailureMessage();
            if (modeInfo != null) modeInfo.setText(message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }

        Intent permissionIntent = VpnService.prepare(this);
        if (permissionIntent == null) {
            startVpnService();
        } else {
            startActivityForResult(permissionIntent, VPN_PERMISSION_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != VPN_PERMISSION_REQUEST) return;

        if (resultCode == RESULT_OK) {
            startVpnService();
        } else {
            String message = "لم تتم الموافقة على إذن VPN؛ لم يبدأ النفق.";
            if (modeInfo != null) modeInfo.setText(message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void startVpnService() {
        if (modeInfo != null) modeInfo.setText("تم تجهيز المحرك؛ جارٍ تشغيل خدمة VPN...");
        Intent service = new Intent(this, TorPhoneVpnService.class);
        service.setAction(TorPhoneVpnService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service);
        } else {
            startService(service);
        }
    }

    private void addInfo(LinearLayout parent, String title, String detail) {
        LinearLayout item = panel();
        item.setPadding(dp(16), dp(15), dp(16), dp(15));
        item.addView(label(title, 16, Color.WHITE, true));
        TextView description = label(detail, 13, muted, false);
        description.setPadding(0, dp(7), 0, 0);
        item.addView(description);
        parent.addView(item, margin(-1, -2, 0, 12, 0, 0));
    }

    private LinearLayout panel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable drawable = shape(surface, 20);
        drawable.setStroke(dp(1), Color.rgb(38, 57, 78));
        box.setBackground(drawable);
        return box;
    }

    private Button modeButton(String title) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setBackground(shape(surface, 14));
        return button;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private LinearLayout.LayoutParams margin(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                width < 0 ? width : dp(width), height < 0 ? height : dp(height));
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
