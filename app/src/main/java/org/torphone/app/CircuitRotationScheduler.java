package org.torphone.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class CircuitRotationScheduler {
    private static final String PREFS = "torphone_preferences";
    private static final String WORK_NAME = "torphone-circuit-rotation";

    private CircuitRotationScheduler() {}

    public static void apply(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean sticky = prefs.getBoolean("privacy_sticky_exit", false);
        int minutes = prefs.getInt("privacy_rotation_minutes", 0);
        WorkManager manager = WorkManager.getInstance(context.getApplicationContext());

        if (sticky || minutes < 15) {
            manager.cancelUniqueWork(WORK_NAME);
            return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                CircuitRotationWorker.class, minutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }
}
