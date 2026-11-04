package org.torphone.app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SplitTunnelingActivity extends Activity {
    private static final String PREFS = "torphone_preferences";
    private static final String KEY_BYPASS = "split_tunnel_bypass_packages";
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.wrap(base));
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ThemePrefs.apply(this);
        render();
    }

    private void render() {
        boolean dark = ThemePrefs.isDark(this);
        int background = dark ? Color.rgb(10,14,26) : Color.rgb(246,248,252);
        int primary = dark ? Color.rgb(244,247,255) : Color.rgb(24,31,48);
        int secondary = dark ? Color.rgb(166,178,199) : Color.rgb(100,112,132);
        int panel = dark ? Color.rgb(24,30,49) : Color.WHITE;

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        page.setPadding(dp(20), dp(18), dp(20), dp(28));
        page.setBackgroundColor(background);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText("Split Tunneling");
        title.setTextSize(25);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setTextColor(primary);
        page.addView(title);

        TextView description = new TextView(this);
        description.setText("اختر التطبيقات التي تريد تشغيلها مباشرة عبر شبكة Android خارج Tor. بقية التطبيقات ستبقى داخل Tor.");
        description.setTextSize(14);
        description.setTextColor(secondary);
        description.setPadding(0, dp(8), 0, dp(8));
        page.addView(description);

        TextView warning = new TextView(this);
        warning.setText("تنبيه: التطبيقات المستثناة لن تحصل على حماية Tor. التغييرات تُطبّق عند فصل VPN وإعادة الاتصال.");
        warning.setTextSize(13);
        warning.setTextColor(dark ? Color.rgb(255,195,105) : Color.rgb(153,91,0));
        warning.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable warningBg = new GradientDrawable();
        warningBg.setColor(dark ? Color.rgb(54,43,32) : Color.rgb(255,245,225));
        warningBg.setCornerRadius(dp(14));
        warning.setBackground(warningBg);
        page.addView(warning);

        TextView section = new TextView(this);
        section.setText("INSTALLED APPS");
        section.setTextSize(11);
        section.setLetterSpacing(.12f);
        section.setTypeface(Typeface.DEFAULT_BOLD);
        section.setTextColor(secondary);
        section.setPadding(0, dp(20), 0, dp(10));
        page.addView(section);

        Set<String> selected = new HashSet<>(getPreferences().getStringSet(KEY_BYPASS, Collections.emptySet()));
        List<ResolveInfo> launchables = getPackageManager().queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        List<AppEntry> apps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo info : launchables) {
            if (info.activityInfo == null) continue;
            String packageName = info.activityInfo.packageName;
            if (packageName == null || packageName.equals(getPackageName()) || !seen.add(packageName)) continue;
            apps.add(new AppEntry(packageName, info.loadLabel(getPackageManager()).toString(),
                    info.loadIcon(getPackageManager())));
        }
        apps.sort(Comparator.comparing(a -> a.label.toLowerCase(java.util.Locale.ROOT)));

        for (AppEntry app : apps) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(8), dp(8), dp(8));
            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setColor(panel);
            rowBg.setCornerRadius(dp(14));
            row.setBackground(rowBg);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(68));
            rowParams.bottomMargin = dp(7);
            page.addView(row, rowParams);

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(app.icon);
            row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));

            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setPadding(dp(12), 0, dp(4), 0);
            TextView name = new TextView(this);
            name.setText(app.label);
            name.setTextSize(14);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            name.setTextColor(primary);
            TextView packageLabel = new TextView(this);
            packageLabel.setText(app.packageName);
            packageLabel.setTextSize(10);
            packageLabel.setTextColor(secondary);
            labels.addView(name);
            labels.addView(packageLabel);
            row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));

            CheckBox check = new CheckBox(this);
            check.setButtonTintList(android.content.res.ColorStateList.valueOf(
                    dark ? Color.rgb(124,92,255) : Color.rgb(108,76,230)));
            check.setChecked(selected.contains(app.packageName));
            check.setContentDescription("Bypass Tor for " + app.label);
            row.addView(check);
            check.setOnCheckedChangeListener((button, checked) -> {
                Set<String> updated = new HashSet<>(getPreferences().getStringSet(KEY_BYPASS, Collections.emptySet()));
                if (checked) updated.add(app.packageName);
                else updated.remove(app.packageName);
                getPreferences().edit().putStringSet(KEY_BYPASS, updated).apply();
            });
        }

        if (apps.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("لم يتم العثور على تطبيقات قابلة للتشغيل.");
            empty.setTextColor(secondary);
            page.addView(empty);
        }
        setContentView(scroll);
    }

    private android.content.SharedPreferences getPreferences() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private static final class AppEntry {
        final String packageName;
        final String label;
        final android.graphics.drawable.Drawable icon;
        AppEntry(String packageName, String label, android.graphics.drawable.Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }
}
