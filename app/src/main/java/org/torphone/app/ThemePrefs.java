package org.torphone.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public final class ThemePrefs {
    private static final String PREFS = "torphone_preferences";
    private static final String KEY_DARK = "dark_theme";
    private ThemePrefs() {}

    public static boolean isDark(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_DARK, true);
    }

    public static void toggle(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DARK, !prefs.getBoolean(KEY_DARK, true)).apply();
    }

    public static void apply(Activity activity) {
        activity.setTheme(isDark(activity) ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
    }
}
