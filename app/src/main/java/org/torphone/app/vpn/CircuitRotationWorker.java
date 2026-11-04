package org.torphone.app.vpn;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.torproject.onionmasq.OnionMasq;
public final class CircuitRotationWorker extends Worker {
 public CircuitRotationWorker(@NonNull Context context,@NonNull WorkerParameters params){super(context,params);}
 @NonNull @Override public Result doWork(){
  SharedPreferences p=getApplicationContext().getSharedPreferences("torphone_preferences",Context.MODE_PRIVATE);
  if(p.getBoolean("privacy_sticky_exit",false)||p.getInt("privacy_rotation_minutes",0)<15)return Result.success();
  if(!p.getBoolean("vpn_active",false)||!TorPhoneVpnService.isTunnelActive())return Result.success();
  try{OnionMasq.refreshCircuits();return Result.success();}catch(Throwable e){return Result.retry();}
 }
}
