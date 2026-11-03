package org.torphone.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import java.util.Locale;

public final class LocaleHelper {
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_ARABIC = "ar";
    private static final String PREFERENCES = "tor_phone_settings";
    private static final String LANGUAGE_KEY = "app_language";

    private LocaleHelper() { }

    public static String getLanguage(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            return Api33.getLanguage(context);
        }
        return preferences(context).getString(LANGUAGE_KEY, LANGUAGE_ENGLISH);
    }

    public static Context wrap(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            return context;
        }
        Locale locale = Locale.forLanguageTag(getLanguage(context));
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        return context.createConfigurationContext(configuration);
    }

    public static void setLanguage(Activity activity, String language) {
        if (!LANGUAGE_ARABIC.equals(language) && !LANGUAGE_ENGLISH.equals(language)) {
            throw new IllegalArgumentException("Unsupported application language");
        }
        preferences(activity).edit().putString(LANGUAGE_KEY, language).apply();
        if (Build.VERSION.SDK_INT >= 33) {
            Api33.setLanguage(activity, language);
        }
        activity.recreate();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    private static final class Api33 {
        static String getLanguage(Context context) {
            android.app.LocaleManager manager = context.getSystemService(android.app.LocaleManager.class);
            if (manager != null) {
                String tags = manager.getApplicationLocales().toLanguageTags();
                if (!tags.isEmpty()) {
                    return tags.split(",")[0];
                }
            }
            return "ar".equals(Locale.getDefault().getLanguage()) ? LANGUAGE_ARABIC : LANGUAGE_ENGLISH;
        }

        static void setLanguage(Context context, String language) {
            android.app.LocaleManager manager = context.getSystemService(android.app.LocaleManager.class);
            if (manager != null) {
                manager.setApplicationLocales(android.os.LocaleList.forLanguageTags(language));
            }
        }
    }
}
