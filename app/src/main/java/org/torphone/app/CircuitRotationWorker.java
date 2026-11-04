package org.torphone.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.torproject.onionmasq.OnionMasq;

public final class CircuitRotationWorker extends Worker {
    private static final String PREFS = "torphone_preferences";

    public CircuitRotationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override public Result doWork() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean("privacy_sticky_exit", false)
                || prefs.getInt("privacy_rotation_minutes", 0) < 15
                || !prefs.getBoolean("vpn_active", false)) {
            return Result.success();
        }
        try {
            // WorkManager runs in the app process. Init is idempotent when the VPN
            // service already owns the same OnionMasq singleton.
            OnionMasq.init(getApplicationContext());
            if (!OnionMasq.isRunning()) return Result.success();
            OnionMasq.refreshCircuits();
            return Result.success();
        } catch (Throwable error) {
            // A stopped proxy is not a reason to keep retrying indefinitely.
            // The next periodic execution will re-check vpn_active and isRunning.
            return prefs.getBoolean("vpn_active", false) ? Result.retry() : Result.success();
        }
    }
}
