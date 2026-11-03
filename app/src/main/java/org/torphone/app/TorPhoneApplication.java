package org.torphone.app;
import android.app.Application;import android.content.Context;
public final class TorPhoneApplication extends Application{@Override protected void attachBaseContext(Context base){super.attachBaseContext(LocaleHelper.wrap(base));}}