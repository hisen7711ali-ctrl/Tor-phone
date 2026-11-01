package org.torphone.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Tor-phone dashboard and Orbot companion launcher.
 * Tor traffic is NOT routed by this app yet: Orbot owns Android's VPN slot.
 */
public final class MainActivity extends Activity {
    private final int bg = Color.rgb(8, 19, 33);
    private final int surface = Color.rgb(17, 31, 48);
    private final int teal = Color.rgb(50, 214, 194);
    private final int muted = Color.rgb(145, 163, 184);
    private TextView modeInfo;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        render();
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
        TextView disconnected = label("Check connection in Orbot", 21, Color.WHITE, true);
        disconnected.setPadding(0, dp(9), 0, dp(5));
        state.addView(disconnected);
        state.addView(label("Tor-phone does not yet own or verify the active VPN tunnel.", 13, muted, false));
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
        modeInfo = label("Tor selected · use Orbot companion for device-wide Tor", 13, muted, false);
        modeInfo.setPadding(dp(2), dp(5), 0, dp(18));
        page.addView(modeInfo);
        vpn.setOnClickListener(v -> modeInfo.setText("VPN selected · configure a provider or import a profile when VPN engine is added"));
        tor.setOnClickListener(v -> modeInfo.setText("Tor selected · Orbot will provide the Android VPN tunnel in this phase"));

        Button torLaunch = new Button(this);
        torLaunch.setText("OPEN ORBOT · ENABLE TOR");
        torLaunch.setTextColor(bg);
        torLaunch.setTextSize(14);
        torLaunch.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        torLaunch.setAllCaps(false);
        torLaunch.setBackground(shape(teal, 16));
        page.addView(torLaunch, margin(-1, 56, 0, 0, 0, 12));
        torLaunch.setOnClickListener(v -> openOrbot());

        TextView hint = label("In Orbot, approve Android's VPN prompt, start Tor, then enable VPN mode and Protect all apps. Return here only after Orbot reports connected.", 13, muted, false);
        hint.setPadding(dp(3), dp(3), dp(3), dp(22));
        page.addView(hint);

        page.addView(label("PRIVACY CENTER", 11, muted, true));
        addInfo(page, "Tor for all apps", "Orbot can route supported phone apps through Tor using Android VpnService. Some apps may not work correctly through Tor.");
        addInfo(page, "VPN providers", "Proton VPN Free is a separate provider app. Tor-phone cannot use its private servers without an authorized integration or profile.");
        addInfo(page, "Add your own server", "A future import screen will accept provider-issued WireGuard or OpenVPN profiles. Never paste passwords or private keys into public support chats.");
        addInfo(page, "VPN Gate warning", "Volunteer relays are experimental and may log connection metadata. Do not use them for sensitive traffic.");
        addInfo(page, "Location privacy", "VPN/Tor changes network routing, not GPS. Manage location permissions per app in Android Settings.");
        addInfo(page, "Emergency protection", "Enable Android Always-on VPN and Block connections without VPN only after verifying the selected VPN app works reliably.");
        TextView footer = label("TRANSPARENT STATUS  •  USER-CONTROLLED PERMISSIONS", 10, muted, true);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(24), 0, 0);
        page.addView(footer);
        setContentView(scroll);
    }

    private void openOrbot() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("org.torproject.android");
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=org.torproject.android")));
        } catch (Exception e) {
            Toast.makeText(this, "Install Orbot from the official Tor/Guardian Project download page.", Toast.LENGTH_LONG).show();
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
        Button b = new Button(this);
        b.setText(title);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setBackground(shape(surface, 14));
        return b;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private LinearLayout.LayoutParams margin(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                width < 0 ? width : dp(width), height < 0 ? height : dp(height));
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return p;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
