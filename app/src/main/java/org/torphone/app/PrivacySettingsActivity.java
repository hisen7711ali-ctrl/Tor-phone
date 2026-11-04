package org.torphone.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.torphone.app.vpn.TorPhoneVpnService;

import java.util.ArrayList;
import java.util.List;

public final class PrivacySettingsActivity extends Activity {
    private static final String PREFS = "torphone_preferences";
    private Switch bridgesSwitch;
    private Switch stickySwitch;
    private EditText bridgeInput;
    private EditText countryInput;
    private Spinner rotationSpinner;
    private boolean dark;

    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.wrap(base));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ThemePrefs.apply(this);
        dark = ThemePrefs.isDark(this);
        render();
    }

    private void render() {
        int bg = dark ? Color.rgb(10,14,26) : Color.rgb(246,248,252);
        int fg = dark ? Color.rgb(244,247,255) : Color.rgb(24,31,48);
        int secondary = dark ? Color.rgb(166,178,199) : Color.rgb(100,112,132);
        int panel = dark ? Color.rgb(24,30,49) : Color.WHITE;

        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        page.setPadding(dp(20), dp(20), dp(20), dp(30));
        page.setBackgroundColor(bg);
        scroll.addView(page);

        TextView title = text("Privacy Settings", 26, fg, true);
        page.addView(title);
        TextView subtitle = text("إعدادات Bridges والتحكم بدوائر Tor", 14, secondary, false);
        subtitle.setPadding(0, dp(6), 0, dp(18));
        page.addView(subtitle);

        bridgesSwitch = new Switch(this);
        bridgesSwitch.setText("Use Bridges · obfs4");
        bridgesSwitch.setTextColor(fg);
        bridgesSwitch.setTextSize(16);
        bridgesSwitch.setChecked(prefs.getBoolean("use_bridges", false));
        page.addView(card(bridgesSwitch, panel));

        TextView bridgeHelp = text("الصق Bridge lines التي حصلت عليها من Tor Project. تُمرّر إلى IPtProxy/obfs4 عند بدء Onionmasq. Bridges help bypass DPI blocks (e.g., Syria).", 13, secondary, false);
        bridgeHelp.setPadding(dp(4), dp(10), dp(4), dp(8));
        page.addView(bridgeHelp);

        bridgeInput = new EditText(this);
        bridgeInput.setText(prefs.getString("bridge_lines", ""));
        bridgeInput.setHint("obfs4 host:port fingerprint cert=... iat-mode=0");
        bridgeInput.setTextColor(fg);
        bridgeInput.setHintTextColor(secondary);
        bridgeInput.setTextSize(13);
        bridgeInput.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        bridgeInput.setMinLines(4);
        bridgeInput.setMaxLines(8);
        bridgeInput.setSingleLine(false);
        bridgeInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        page.addView(card(bridgeInput, panel));

        TextView stickyTitle = text("Circuit control", 18, fg, true);
        stickyTitle.setPadding(0, dp(24), 0, dp(8));
        page.addView(stickyTitle);

        stickySwitch = new Switch(this);
        stickySwitch.setText("Sticky Exit · Fixed IP (best effort)");
        stickySwitch.setTextColor(fg);
        stickySwitch.setTextSize(15);
        stickySwitch.setChecked(prefs.getBoolean("privacy_sticky_exit", false));
        page.addView(card(stickySwitch, panel));

        TextView stickyInfo = text("تنبيه تقني: Onionmasq لا يوفّر في JNI الحالي ضبط MaxCircuitDirtiness=86400. هذا الخيار يوقف التدوير الدوري المجدول فقط؛ ولا يضمن ثبات نفس Public IP أو نفس Exit relay لمدة 24 ساعة. Refresh Circuits يطبّق على الاتصالات الجديدة؛ الاتصالات القائمة لا تنتقل تلقائيًا، وقد يختار Tor نفس Exit IP مجددًا.", 13,
                dark ? Color.rgb(255,195,105) : Color.rgb(153,91,0), false);
        stickyInfo.setPadding(dp(14), dp(12), dp(14), dp(12));
        stickyInfo.setBackground(round(dark ? Color.rgb(54,43,32) : Color.rgb(255,245,225)));
        page.addView(stickyInfo);

        TextView countryLabel = text("Preferred Exit Country (optional)", 14, fg, true);
        countryLabel.setPadding(0, dp(12), 0, dp(6));
        page.addView(countryLabel);
        countryInput = new EditText(this);
        countryInput.setSingleLine(true);
        countryInput.setTextColor(fg);
        countryInput.setHintTextColor(secondary);
        countryInput.setHint("2-letter country code, e.g. DE");
        countryInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        countryInput.setText(prefs.getString("privacy_sticky_country", ""));
        page.addView(card(countryInput, panel));

        TextView rotationLabel = text("Refresh Tor circuits every", 15, fg, true);
        rotationLabel.setPadding(0, dp(18), 0, dp(8));
        page.addView(rotationLabel);

        String[] labels = {"Off", "15 minutes", "1 hour", "6 hours", "24 hours"};
        int[] values = {0, 15, 60, 360, 1440};
        rotationSpinner = new Spinner(this);
        rotationSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        int selected = 0;
        int current = prefs.getInt("privacy_rotation_minutes", 0);
        for (int i = 0; i < values.length; i++) if (values[i] == current) selected = i;
        rotationSpinner.setSelection(selected);
        page.addView(card(rotationSpinner, panel));

        Button save = new Button(this);
        save.setText("Save & Reconnect");
        save.setAllCaps(false);
        save.setTextSize(15);
        save.setTextColor(Color.WHITE);
        save.setMinHeight(dp(56));
        save.setBackground(round(Color.rgb(108,76,230)));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, dp(56));
        saveLp.topMargin = dp(24);
        page.addView(save, saveLp);
        save.setOnClickListener(v -> saveAndReconnect(values));

        setContentView(scroll);
    }

    private void saveAndReconnect(int[] rotationValues) {
        boolean useBridges = bridgesSwitch.isChecked();
        String raw = bridgeInput.getText().toString();
        List<String> validLines = new ArrayList<>();
        for (String line : raw.split("\\r?\\n")) {
            String value = line.trim();
            if (value.isEmpty() || value.startsWith("#")) continue;
            String[] parts = value.split("\\s+");
            String[] endpoint = parts.length > 1 ? parts[1].split(":") : new String[0];
            if (parts.length < 5 || !"obfs4".equals(parts[0])
                    || endpoint.length != 2 || endpoint[0].isEmpty()
                    || !endpoint[1].matches("[0-9]{1,5}")
                    || !parts[2].matches("[A-Fa-f0-9]{40}")
                    || !value.contains("cert=") || !value.contains("iat-mode=")) {
                Toast.makeText(this, "صيغة Bridge غير صحيحة. استخدم obfs4 host:port fingerprint cert=... iat-mode=0", Toast.LENGTH_LONG).show();
                return;
            }
            validLines.add(value);
        }
        if (useBridges && validLines.isEmpty()) {
            Toast.makeText(this, "أدخل obfs4 Bridge line واحدة على الأقل", Toast.LENGTH_LONG).show();
            return;
        }

        String country = countryInput.getText().toString().trim().toUpperCase(java.util.Locale.ROOT);
        if (!country.isEmpty() && !country.matches("[A-Z]{2}")) {
            countryInput.setError("Enter a 2-letter country code");
            return;
        }
        int rotationMinutes = rotationValues[rotationSpinner.getSelectedItemPosition()];
        boolean sticky = stickySwitch.isChecked();
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean("use_bridges", useBridges)
                .putString("bridge_lines", String.join("\n", validLines))
                .putBoolean("privacy_sticky_exit", sticky)
                .putString("privacy_sticky_country", country)
                .putInt("privacy_rotation_minutes", rotationMinutes)
                .apply();
        CircuitRotationScheduler.apply(this);

        boolean active = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("vpn_active", false);
        if (active) {
            Intent restart = new Intent(this, TorPhoneVpnService.class);
            restart.setAction(TorPhoneVpnService.ACTION_RESTART);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(restart);
            else startService(restart);
            Toast.makeText(this, "تم الحفظ وإعادة تشغيل VPN بالإعدادات الجديدة", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "تم حفظ الإعدادات. ستُستخدم عند الاتصال التالي.", Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        view.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        return view;
    }

    private LinearLayout card(View child, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(round(color));
        box.addView(child, new LinearLayout.LayoutParams(-1, -2));
        return box;
    }

    private GradientDrawable round(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(18));
        return shape;
    }
}
