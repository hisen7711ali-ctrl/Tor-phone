package org.torphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Dashboard shell. It never reports a connection until a real tunnel engine exists. */
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
        page.addView(label("A transparent connection dashboard.", 14, muted, false));

        LinearLayout state = panel();
        state.setPadding(dp(18), dp(18), dp(18), dp(18));
        state.addView(label("PROTECTION STATUS", 11, muted, true));
        TextView disconnected = label("Not connected", 24, Color.WHITE, true);
        disconnected.setPadding(0, dp(9), 0, dp(5));
        state.addView(disconnected);
        state.addView(label("VPN and Tor engines are not connected in this version.", 13, muted, false));
        page.addView(state, margin(-1, -2, 0, 25, 0, 22));

        page.addView(label("SELECT CONNECTION MODE", 11, muted, true));
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        Button vpn = modeButton("VPN");
        Button tor = modeButton("Tor");
        choices.addView(vpn, new LinearLayout.LayoutParams(0, dp(52), 1));
        View gap = new View(this);
        choices.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));
        choices.addView(tor, new LinearLayout.LayoutParams(0, dp(52), 1));
        page.addView(choices, margin(-1, -2, 0, 11, 0, 0));
        modeInfo = label("VPN selected · provider profile required", 13, muted, false);
        modeInfo.setPadding(dp(2), dp(5), 0, dp(18));
        page.addView(modeInfo);
        vpn.setOnClickListener(v -> modeInfo.setText("VPN selected · provider profile required"));
        tor.setOnClickListener(v -> modeInfo.setText("Tor selected · Tor service integration pending"));

        Button connect = new Button(this);
        connect.setText("CONNECT");
        connect.setTextColor(bg);
        connect.setTextSize(14);
        connect.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        connect.setAllCaps(false);
        connect.setBackground(shape(teal, 16));
        page.addView(connect, margin(-1, 56, 0, 0, 0, 25));
        connect.setOnClickListener(v -> Toast.makeText(this,
                "Not available yet: no VPN/Tor engine is installed. Your connection is not protected by Tor-phone.",
                Toast.LENGTH_LONG).show());

        page.addView(label("PRIVACY CENTER", 11, muted, true));
        addInfo(page, "Network diagnostics", "IP, IPv6 and DNS leak checks will be added with a trusted test endpoint.");
        addInfo(page, "Server locations", "No fake servers. Import a verified provider profile when one is available.");
        addInfo(page, "Location privacy", "Review app permissions in Android Settings. Approximate location may be available per app.");
        addInfo(page, "Emergency protection", "Always-on VPN and Android Lockdown guidance will be added after tunnel integration.");
        TextView footer = label("NO HIDDEN ROUTING  •  NO FALSE SECURITY CLAIMS", 10, muted, true);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(24), 0, 0);
        page.addView(footer);
        setContentView(scroll);
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
