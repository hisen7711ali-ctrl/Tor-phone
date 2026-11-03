package org.torphone.app;
import android.app.Activity;import android.content.*;import android.net.VpnService;import android.os.*;import android.view.Gravity;import android.view.View;import android.widget.*;import org.torphone.app.vpn.OnionmasqRouter;import org.torphone.app.vpn.TorPhoneVpnService;
public final class MainActivity extends Activity{
 private static final int VPN_PERMISSION_REQUEST=7101;private TextView modeInfo,dashboardState;private boolean registered;
 private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(modeInfo!=null&&i!=null){String s=i.getStringExtra(TorPhoneVpnService.EXTRA_STATUS);String value=s==null?getString(R.string.state_unknown):s;updateStatus(value);}}};
 @Override protected void attachBaseContext(Context b){super.attachBaseContext(LocaleHelper.wrap(b));}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(getColor(R.color.navy));getWindow().setNavigationBarColor(getColor(R.color.navy));render();}
 @Override protected void onStart(){super.onStart();IntentFilter f=new IntentFilter(TorPhoneVpnService.ACTION_STATUS);if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);registered=true;String current=TorPhoneVpnService.getLastStatus();if(current!=null){updateStatus(current);}}
 @Override protected void onStop(){if(registered){unregisterReceiver(receiver);registered=false;}super.onStop();}
 private void render(){
  ScrollView sc=new ScrollView(this);sc.setFillViewport(true);LinearLayout p=new LinearLayout(this);p.setOrientation(1);p.setPadding(sp(R.dimen.space_24),sp(R.dimen.space_24),sp(R.dimen.space_24),sp(R.dimen.space_32));p.setBackgroundColor(getColor(R.color.navy));sc.addView(p);
  p.addView(label(R.string.brand_name,R.style.TorPhone_Brand));TextView h=label(R.string.dashboard_title,R.style.TorPhone_Headline);h.setPadding(0,sp(R.dimen.space_16),0,sp(R.dimen.space_8));p.addView(h);p.addView(label(R.string.dashboard_subtitle,R.style.TorPhone_Body));
  LinearLayout card=panel();card.setPadding(sp(R.dimen.space_16),sp(R.dimen.space_16),sp(R.dimen.space_16),sp(R.dimen.space_16));card.addView(label(R.string.protection_status,R.style.TorPhone_Section));dashboardState=label(R.string.state_unknown,R.style.TorPhone_HeadlineSmall);dashboardState.setPadding(0,sp(R.dimen.space_8),0,sp(R.dimen.space_4));card.addView(dashboardState);card.addView(label(R.string.status_not_verified_explanation,R.style.TorPhone_Body));p.addView(card,lp(-1,-2,0,0,0,sp(R.dimen.space_24)));
  p.addView(label(R.string.connection_mode_title,R.style.TorPhone_Section));TextView mode=label(R.string.connection_mode_tor_tunnel,R.style.TorPhone_BodyStrong);mode.setPadding(0,sp(R.dimen.space_8),0,sp(R.dimen.space_16));p.addView(mode);modeInfo=label(R.string.state_unknown,R.style.TorPhone_Body);p.addView(modeInfo,lp(-1,-2,0,0,0,sp(R.dimen.space_16)));
  Button start=new Button(this);start.setText(R.string.action_start_tor);start.setTextAppearance(this,R.style.TorPhone_Button);start.setAllCaps(false);start.setBackgroundResource(R.drawable.bg_primary_button);p.addView(start,lp(-1,sp(R.dimen.button_height),0,0,0,sp(R.dimen.space_24)));start.setOnClickListener(v->startVpn());
  p.addView(label(R.string.privacy_center_title,R.style.TorPhone_Section));info(p,R.string.tor_protection_title,R.string.tor_protection_detail);info(p,R.string.vpn_provider_title,R.string.vpn_provider_detail);info(p,R.string.server_configuration_title,R.string.server_configuration_detail);info(p,R.string.location_privacy_title,R.string.location_privacy_detail);info(p,R.string.kill_switch_title,R.string.kill_switch_detail);
  p.addView(label(R.string.settings_title,R.style.TorPhone_Section),lp(-1,-2,0,sp(R.dimen.space_16),0,sp(R.dimen.space_8)));LinearLayout row=panel();row.setGravity(Gravity.CENTER_VERTICAL);row.setOrientation(0);row.setPadding(sp(R.dimen.space_16),sp(R.dimen.space_8),sp(R.dimen.space_16),sp(R.dimen.space_8));row.addView(label(R.string.language_label,R.style.TorPhone_BodyStrong),new LinearLayout.LayoutParams(0,-2,1));Button ar=lang(R.string.language_arabic),en=lang(R.string.language_english);row.addView(ar,new LinearLayout.LayoutParams(0,sp(R.dimen.button_height_small),1));View gap=new View(this);row.addView(gap,new LinearLayout.LayoutParams(sp(R.dimen.space_8),1));row.addView(en,new LinearLayout.LayoutParams(0,sp(R.dimen.button_height_small),1));ar.setOnClickListener(v->LocaleHelper.setLanguage(this,LocaleHelper.LANGUAGE_ARABIC));en.setOnClickListener(v->LocaleHelper.setLanguage(this,LocaleHelper.LANGUAGE_ENGLISH));p.addView(row,lp(-1,-2,0,0,0,sp(R.dimen.space_16)));
  TextView foot=label(R.string.footer_transparent_status,R.style.TorPhone_Caption);foot.setGravity(Gravity.CENTER);foot.setPadding(0,sp(R.dimen.space_24),0,0);p.addView(foot);setContentView(sc);
 }
 private Button lang(int id){Button b=new Button(this);b.setText(id);b.setTextAppearance(this,R.style.TorPhone_ButtonSmall);b.setAllCaps(false);b.setBackgroundResource(R.drawable.bg_panel);return b;}
 private void startVpn(){if(OnionmasqRouter.prepare(this)!=OnionmasqRouter.INIT_OK){String m=getString(R.string.engine_initialization_failed);updateStatus(m);Toast.makeText(this,m,Toast.LENGTH_LONG).show();return;}Intent i=VpnService.prepare(this);if(i==null)launchService();else startActivityForResult(i,VPN_PERMISSION_REQUEST);}
 @Override protected void onActivityResult(int r,int result,Intent data){super.onActivityResult(r,result,data);if(r!=VPN_PERMISSION_REQUEST)return;if(result==RESULT_OK)launchService();else{String m=getString(R.string.vpn_permission_denied);updateStatus(m);Toast.makeText(this,m,Toast.LENGTH_LONG).show();}}
 private void launchService(){updateStatus(getString(R.string.status_engine_starting));Intent i=new Intent(this,TorPhoneVpnService.class);i.setAction(TorPhoneVpnService.ACTION_START);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}
 private void updateStatus(String value){
  boolean failed=value.equals(getString(R.string.status_engine_failed))
      ||value.equals(getString(R.string.status_engine_init_failed))
      ||value.equals(getString(R.string.status_socket_protection_failed))
      ||value.equals(getString(R.string.engine_initialization_failed));
  boolean warning=value.equals(getString(R.string.status_engine_starting))
      ||value.equals(getString(R.string.vpn_permission_denied))
      ||value.equals(getString(R.string.status_vpn_permission_revoked));
  int color=failed?getColor(R.color.error):warning?getColor(R.color.amber)
      :(value.equals(getString(R.string.state_unknown))||value.equals(getString(R.string.state_disconnected)))
      ?getColor(R.color.muted):getColor(R.color.white);
  animateStatus(modeInfo,value,color);
  animateStatus(dashboardState,value,color);
 }
 private void animateStatus(TextView view,String value,int color){
  if(view==null)return;
  view.animate().cancel();
  view.setTextColor(color);
  view.animate().alpha(.78f).setDuration(80).withEndAction(()->{
   view.setText(value);
   view.animate().alpha(1f).setDuration(140).start();
  }).start();
 }
 private void info(LinearLayout p,int title,int detail){LinearLayout c=panel();c.setPadding(sp(R.dimen.space_16),sp(R.dimen.space_12),sp(R.dimen.space_16),sp(R.dimen.space_12));c.addView(label(title,R.style.TorPhone_BodyStrong));TextView d=label(detail,R.style.TorPhone_Body);d.setPadding(0,sp(R.dimen.space_8),0,0);c.addView(d);p.addView(c,lp(-1,-2,0,sp(R.dimen.space_12),0,0));}
 private LinearLayout panel(){LinearLayout b=new LinearLayout(this);b.setOrientation(1);b.setBackgroundResource(R.drawable.bg_panel);return b;}
 private TextView label(int id,int style){TextView t=new TextView(this);t.setText(id);t.setTextAppearance(this,style);return t;}
 private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(w,h);x.setMargins(dp(l),t,dp(r),b);return x;}
 private int sp(int id){return getResources().getDimensionPixelSize(id);}private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}