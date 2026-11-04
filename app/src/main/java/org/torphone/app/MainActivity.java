package org.torphone.app;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.*;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.torphone.app.vpn.OnionmasqRouter;
import org.torphone.app.vpn.TorPhoneVpnService;

public final class MainActivity extends Activity {
    public static final String ACTION_START_VPN_FROM_COMPOSE = "org.torphone.app.action.START_VPN_FROM_COMPOSE";
    private static final int VPN_PERMISSION_REQUEST = 7101;
    private TextView modeInfo, statusTitle, statusSubtitle;
    private Button protectionButton;
    private boolean registered, connected, dark;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (i != null) updateStatus(i.getStringExtra(TorPhoneVpnService.EXTRA_STATUS));
        }
    };

    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.wrap(base));
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ThemePrefs.apply(this);
        dark = ThemePrefs.isDark(this);
        applySystemBars();
        render();
        handleComposeStart(getIntent());
        updateStatus(TorPhoneVpnService.getLastStatus());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleComposeStart(intent);
    }

    private void handleComposeStart(Intent intent) {
        if (intent != null && ACTION_START_VPN_FROM_COMPOSE.equals(intent.getAction())) {
            intent.setAction(null);
            startVpn();
        }
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(TorPhoneVpnService.ACTION_STATUS);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver, filter);
        registered = true;
        updateStatus(TorPhoneVpnService.getLastStatus());
    }

    @Override protected void onStop() {
        if (registered) { unregisterReceiver(receiver); registered = false; }
        super.onStop();
    }

    private int c(int darkColor, int lightColor) { return getColor(dark ? darkColor : lightColor); }
    private void applySystemBars() {
        getWindow().setStatusBarColor(dark ? Color.rgb(10,14,26) : Color.rgb(246,248,252));
        getWindow().setNavigationBarColor(dark ? Color.rgb(10,14,26) : Color.rgb(246,248,252));
        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(dark ? 0 :
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    private void render() {
        int bg = dark ? Color.rgb(10,14,26) : Color.rgb(246,248,252);
        int primary = dark ? Color.rgb(244,247,255) : Color.rgb(24,31,48);
        int secondary = dark ? Color.rgb(166,178,199) : Color.rgb(100,112,132);
        int accent = Color.rgb(124,92,255);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        page.setPadding(dp(22), dp(18), dp(22), dp(30));
        page.setBackgroundColor(bg);
        scroll.addView(page);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView brand = text(R.string.brand_name, 12, accent, true);
        brand.setLetterSpacing(.16f);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        Button theme = new Button(this);
        theme.setText(dark ? "☀" : "☾");
        theme.setContentDescription(getString(R.string.theme_toggle));
        theme.setTextSize(20);
        theme.setAllCaps(false);
        theme.setTextColor(primary);
        theme.setMinWidth(dp(52));
        theme.setMinHeight(dp(48));
        theme.setBackground(panelDrawable());
        theme.setOnClickListener(v -> {
            ThemePrefs.toggle(this);
            recreate();
        });
        top.addView(theme);
        page.addView(top);

        TextView heading = text(R.string.dashboard_title, 29, primary, true);
        heading.setPadding(0, dp(22), 0, dp(5));
        page.addView(heading);
        TextView sub = text(R.string.dashboard_subtitle, 14, secondary, false);
        page.addView(sub);

        LinearLayout status = panel();
        status.setPadding(dp(18), dp(18), dp(18), dp(18));
        TextView overline = text(R.string.protection_status, 11, secondary, true);
        overline.setLetterSpacing(.12f);
        status.addView(overline);
        statusTitle = text(R.string.state_disconnected, 21, primary, true);
        statusTitle.setPadding(0, dp(12), 0, dp(4));
        status.addView(statusTitle);
        statusSubtitle = text(R.string.badge_disconnected_subtitle, 13, secondary, false);
        status.addView(statusSubtitle);
        page.addView(status, lp(-1, -2, 0, dp(22), 0, dp(22)));

        TextView connectionTitle = text(R.string.connection_mode_title, 11, secondary, true);
        connectionTitle.setLetterSpacing(.12f);
        page.addView(connectionTitle);
        TextView mode = text(R.string.connection_mode_tor_tunnel, 15, primary, true);
        mode.setPadding(0, dp(8), 0, dp(4));
        page.addView(mode);
        modeInfo = text(R.string.connection_mode_detail, 13, secondary, false);
        page.addView(modeInfo, lp(-1, -2, 0, 0, 0, dp(18)));

        protectionButton = new Button(this);
        protectionButton.setText(R.string.action_start_tor);
        protectionButton.setAllCaps(false);
        protectionButton.setTextSize(15);
        protectionButton.setTextColor(Color.WHITE);
        protectionButton.setMinHeight(dp(56));
        protectionButton.setBackground(primaryButtonDrawable());
        page.addView(protectionButton, lp(-1, dp(56), 0, dp(4), 0, dp(22)));
        protectionButton.setOnClickListener(v -> { if (connected) stopVpn(); else startVpn(); });

        TextView privacy = text(R.string.privacy_center_title, 11, secondary, true);
        privacy.setLetterSpacing(.12f);
        page.addView(privacy, lp(-1, -2, 0, dp(8), 0, dp(8)));
        info(page, R.string.tor_protection_title, R.string.tor_protection_detail, primary, secondary);
        info(page, R.string.vpn_provider_title, R.string.vpn_provider_detail, primary, secondary);
        info(page, R.string.server_configuration_title, R.string.server_configuration_detail, primary, secondary);
        info(page, R.string.location_privacy_title, R.string.location_privacy_detail, primary, secondary);
        info(page, R.string.kill_switch_title, R.string.kill_switch_detail, primary, secondary);

        TextView settings = text(R.string.settings_title, 11, secondary, true);
        settings.setLetterSpacing(.12f);
        page.addView(settings, lp(-1, -2, 0, dp(22), 0, dp(10)));
        Button splitTunneling = languageButton(R.string.split_tunneling_title, primary);
        splitTunneling.setTextSize(14);
        splitTunneling.setMinHeight(dp(52));
        splitTunneling.setOnClickListener(v ->
                startActivity(new Intent(this, SplitTunnelingActivity.class)));
        page.addView(splitTunneling, lp(-1, dp(52), 0, 0, 0, dp(12)));
        LinearLayout language = panel();
        language.setGravity(Gravity.CENTER_VERTICAL);
        language.setPadding(dp(12), dp(6), dp(12), dp(6));
        TextView languageLabel = text(R.string.language_label, 14, primary, true);
        language.addView(languageLabel, new LinearLayout.LayoutParams(0, -2, 1));
        Button ar = languageButton(R.string.language_arabic, primary);
        Button en = languageButton(R.string.language_english, primary);
        language.addView(ar, new LinearLayout.LayoutParams(0, dp(46), 1));
        View gap = new View(this);
        language.addView(gap, new LinearLayout.LayoutParams(dp(6), 1));
        language.addView(en, new LinearLayout.LayoutParams(0, dp(46), 1));
        ar.setOnClickListener(v -> LocaleHelper.setLanguage(this, LocaleHelper.LANGUAGE_ARABIC));
        en.setOnClickListener(v -> LocaleHelper.setLanguage(this, LocaleHelper.LANGUAGE_ENGLISH));
        page.addView(language, lp(-1, -2, 0, 0, 0, dp(16)));

        TextView footer = text(R.string.footer_transparent_status, 10, secondary, true);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(12), 0, 0);
        page.addView(footer);
        setContentView(scroll);
    }

    private void info(LinearLayout parent, int title, int detail, int primary, int secondary) {
        LinearLayout card = panel();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(text(title, 15, primary, true));
        TextView body = text(detail, 13, secondary, false);
        body.setPadding(0, dp(7), 0, 0);
        card.addView(body);
        parent.addView(card, lp(-1, -2, 0, dp(8), 0, 0));
    }

    private LinearLayout panel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(panelDrawable());
        return box;
    }

    private GradientDrawable panelDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(dark ? Color.rgb(24,30,49) : Color.WHITE);
        d.setCornerRadius(dp(20));
        d.setStroke(dp(1), dark ? Color.rgb(48,57,82) : Color.rgb(225,230,240));
        return d;
    }

    private GradientDrawable primaryButtonDrawable() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF7C5CFF, 0xFF32BFAF});
        d.setCornerRadius(dp(16));
        return d;
    }

    private Button languageButton(int id, int color) {
        Button b = new Button(this);
        b.setText(id);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setTextColor(color);
        b.setBackground(panelDrawable());
        return b;
    }

    private TextView text(int id, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(id);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(android.graphics.Typeface.create("sans-serif", bold ? 1 : 0));
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        return t;
    }

    private void startVpn() {
        if (OnionmasqRouter.prepare(this) != OnionmasqRouter.INIT_OK) {
            String message = getString(R.string.engine_initialization_failed);
            updateStatus(message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }
        Intent permission = VpnService.prepare(this);
        if (permission == null) launchService();
        else startActivityForResult(permission, VPN_PERMISSION_REQUEST);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != VPN_PERMISSION_REQUEST) return;
        if (result == RESULT_OK) launchService();
        else {
            String message = getString(R.string.vpn_permission_denied);
            updateStatus(message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void stopVpn() {
        Intent stop = new Intent(this, TorPhoneVpnService.class);
        stop.setAction(TorPhoneVpnService.ACTION_STOP);
        startService(stop);
        updateStatus(getString(R.string.state_disconnected));
    }

    private void launchService() {
        updateStatus(getString(R.string.status_engine_starting));
        Intent service = new Intent(this, TorPhoneVpnService.class);
        service.setAction(TorPhoneVpnService.ACTION_START);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
        else startService(service);
    }

    private void updateStatus(String value) {
        if (value == null) value = getString(R.string.state_disconnected);
        boolean failed = value.equals(getString(R.string.status_engine_failed))
                || value.equals(getString(R.string.status_engine_init_failed))
                || value.equals(getString(R.string.status_socket_protection_failed))
                || value.equals(getString(R.string.engine_initialization_failed))
                || value.equals(getString(R.string.vpn_permission_denied))
                || value.equals(getString(R.string.status_vpn_permission_revoked));
        boolean starting = value.equals(getString(R.string.status_engine_starting));
        boolean verified = value.equals(getString(R.string.status_verified_protected));
        connected = verified || value.equals(getString(R.string.status_engine_running_unverified));
        if (statusTitle == null) return;
        int color = failed ? 0xFFFF647C : verified ? 0xFF32D6A0
                : starting || connected ? 0xFFF3B95F
                : dark ? 0xFFF4F7FF : 0xFF182030;
        statusTitle.setText(failed ? R.string.badge_failed
                : starting ? R.string.badge_connecting
                : verified ? R.string.badge_verified_protected
                : connected ? R.string.badge_engine_running
                : R.string.badge_disconnected);
        statusSubtitle.setText(failed ? R.string.badge_failed_subtitle
                : starting ? R.string.badge_connecting_subtitle
                : verified ? R.string.badge_verified_subtitle
                : connected ? R.string.badge_engine_running_subtitle
                : R.string.badge_disconnected_subtitle);
        statusTitle.setTextColor(color);
        protectionButton.setText(connected ? R.string.action_stop_protection : R.string.action_start_tor);
        protectionButton.setEnabled(!starting);
    }

    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
}
