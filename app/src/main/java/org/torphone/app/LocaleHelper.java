package org.torphone.app;
import android.app.Activity;import android.app.LocaleManager;import android.content.Context;import android.content.SharedPreferences;import android.content.res.Configuration;import android.os.Build;import android.os.LocaleList;import java.util.Locale;
public final class LocaleHelper{
 public static final String LANGUAGE_ENGLISH="en",LANGUAGE_ARABIC="ar";private static final String PREFS="tor_phone_settings",KEY="app_language";private LocaleHelper(){}
 public static String getLanguage(Context c){SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);return p.getString(KEY,LANGUAGE_ENGLISH);}
 public static Context wrap(Context c){Locale l=Locale.forLanguageTag(getLanguage(c));Locale.setDefault(l);Configuration cfg=new Configuration(c.getResources().getConfiguration());cfg.setLocale(l);cfg.setLayoutDirection(l);return c.createConfigurationContext(cfg);}
 public static void setLanguage(Activity a,String language){if(!LANGUAGE_ARABIC.equals(language)&&!LANGUAGE_ENGLISH.equals(language))throw new IllegalArgumentException("Unsupported application language");a.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,language).apply();if(Build.VERSION.SDK_INT>=33){LocaleManager m=a.getSystemService(LocaleManager.class);if(m!=null)m.setApplicationLocales(LocaleList.forLanguageTags(language));}a.recreate();}
}