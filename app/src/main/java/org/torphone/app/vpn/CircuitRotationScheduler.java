package org.torphone.app.vpn;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
public final class CircuitRotationScheduler {
 private static final String WORK_NAME="torphone-circuit-rotation";
 private CircuitRotationScheduler(){}
 public static void apply(Context context){
  SharedPreferences p=context.getSharedPreferences("torphone_preferences",Context.MODE_PRIVATE);
  int minutes=p.getInt("privacy_rotation_minutes",0);
  if(p.getBoolean("privacy_sticky_exit",false)||minutes<15){WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);return;}
  PeriodicWorkRequest request=new PeriodicWorkRequest.Builder(CircuitRotationWorker.class,minutes,TimeUnit.MINUTES).build();
  WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME,ExistingPeriodicWorkPolicy.UPDATE,request);
 }
}
