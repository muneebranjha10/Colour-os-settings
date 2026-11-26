package com.galaxy.batteryhook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class NativeModes {
    private final Activity a;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final android.content.SharedPreferences history;
    private final PowerManager pm;
    private final BatteryManager bm;
    private volatile boolean closed;
    private boolean estimating;
    private volatile long lastQuery;
    private int observedMode=-1;
    private long dischargeAnchorTime;private int dischargeAnchor=-1;
    private long stableSince;
    private final boolean lightSupported;
    interface Estimates{void display(String[] values);}
    NativeModes(Activity a){
        this.a=a;pm=(PowerManager)a.getSystemService(Context.POWER_SERVICE);bm=(BatteryManager)a.getSystemService(Context.BATTERY_SERVICE);
        history=a.getSharedPreferences("colour_os_battery_profile_samples_v3",Context.MODE_PRIVATE);
        boolean supported=false;
        try{// Never guess support from a Galaxy model name: validate the host, firmware setting and actual helper.
        if(ReleaseRules.canProbeLightProfile(android.os.Build.MANUFACTURER,a.getPackageName(),android.os.Build.VERSION.SDK_INT,profile())){
            Class<?> c=Class.forName("tg.a",false,a.getClassLoader());
            Method check=c.getDeclaredMethod("b");Method set=c.getDeclaredMethod("c",int.class);
            Constructor<?> ctor=c.getDeclaredConstructor(Context.class);
            if(check.getReturnType()==boolean.class&&java.lang.reflect.Modifier.isStatic(check.getModifiers())&&!java.lang.reflect.Modifier.isStatic(set.getModifiers())&&ctor!=null){
                supported=Boolean.TRUE.equals(callStatic(c,"b",new Class<?>[]{}));
            }
        }}catch(Throwable e){BatteryHook.log("Light feature: "+e);}
        lightSupported=supported;
        BatteryHook.log("Samsung Light profile helper confirmed="+lightSupported+" (firmware profile="+profile()+")");
    }
    boolean supportsLight(){return lightSupported;}
    boolean isSaving(){try{return pm!=null&&pm.isPowerSaveMode();}catch(Throwable e){return false;}}
    private int profile(){try{return Settings.Global.getInt(a.getContentResolver(),"sem_low_heat_mode",-1);}catch(Throwable e){return -1;}}
    int normalMode(){return 0;}
    // Standard is the default Balanced profile, not a performance boost.
    int activeMode(){if(isSaving())return 1;return profile()==1?-1:0;}
    boolean isLightProfile(){return !isSaving()&&profile()==1;}
    boolean needsManualStandard(){return profile()==1&&!lightSupported;}
    private static Object callStatic(Class<?> c,String name,Class<?>[] args,Object... values)throws Exception{Method m=c.getDeclaredMethod(name,args);m.setAccessible(true);return m.invoke(null,values);}
    private void power(boolean on)throws Exception{
        if(isSaving()==on)return;
        // Device Care itself writes Settings.Global.low_power as part of its native saver controller.
        // Prefer the platform service, which validates policy and reports state.
        try{Method method=PowerManager.class.getDeclaredMethod("setPowerSaveModeEnabled",boolean.class);method.setAccessible(true);Object ok=method.invoke(pm,on);BatteryHook.log("PowerManager result="+ok);}catch(Throwable e){BatteryHook.log("PowerManager saver API unavailable: "+e);}
        for(int i=0;i<5&&isSaving()!=on;i++)Thread.sleep(120);
        if(isSaving()!=on){
            boolean ok=Settings.Global.putInt(a.getContentResolver(),"low_power",on?1:0);
            BatteryHook.log("native low_power write="+ok);
            for(int i=0;i<10&&isSaving()!=on;i++)Thread.sleep(150);
        }
        if(isSaving()!=on)throw new IllegalStateException("Power saving state did not change");
    }
    private void applyProfile(int value)throws Exception{
        if(!lightSupported)throw new UnsupportedOperationException("No separate Light profile on this device");
        if(profile()==value)return;
        Class<?> c=Class.forName("tg.a",false,a.getClassLoader());Constructor<?> ctor=c.getDeclaredConstructor(Context.class);ctor.setAccessible(true);
        Object helper=ctor.newInstance(a.getApplicationContext());Method m=c.getDeclaredMethod("c",int.class);m.setAccessible(true);m.invoke(helper,value);
        if(profile()!=value)throw new IllegalStateException("Profile not confirmed");
    }
    void apply(int mode,Runnable result){
        if(closed)return;
        worker.execute(()->{
            try{
                if(mode==1)power(true);
                else if(mode==0){
                    power(false);
                    if(lightSupported)applyProfile(ReleaseRules.samsungProfileForBalanced());
                    else if(profile()==1)throw new UnsupportedOperationException("Open native Samsung profile to restore Standard");
                }
                else if(mode==2)throw new UnsupportedOperationException("Performance routes to native controls");
                BatteryHook.log("requested "+mode+" active="+activeMode());
            }catch(Throwable t){BatteryHook.log("apply mode "+mode+": "+t);}
            finally{lastQuery=0;main.postDelayed(()->{if(!closed)result.run();},350);}
        });
    }
    void setLight(Runnable done){
        if(closed)return;
        worker.execute(()->{
            try{
                if(!lightSupported)throw new UnsupportedOperationException("Light profile helper unavailable");
                power(false);
                applyProfile(ReleaseRules.samsungProfileForLight());
                BatteryHook.log("Light profile readback "+profile());
            }catch(Throwable t){BatteryHook.log("Light profile not applied: "+t);}
            finally{lastQuery=0;main.postDelayed(()->{if(!closed)done.run();},350);}
        });
    }
    private int samsungTime(int mode){
        try{
            Class<?> c=Class.forName("com.samsung.android.hardware.SemBatteryUtils",false,a.getClassLoader());
            Object v=callStatic(c,"getBatteryRemainingUsageTime",new Class<?>[]{Context.class,int.class},a,mode);
            int minutes=((Number)v).intValue();return minutes>0&&minutes<=20160?minutes:-1;
        }catch(Throwable t){BatteryHook.log("Samsung estimate mode "+mode+": "+t);return -1;}
    }
    private int chargeCounter(){try{int value=bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);return value>0?value:-1;}catch(Throwable t){return -1;}}
    private boolean discharging(){try{Intent i=a.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));return i!=null&&i.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)==0&&i.getIntExtra(BatteryManager.EXTRA_STATUS,0)==BatteryManager.BATTERY_STATUS_DISCHARGING;}catch(Throwable t){return false;}}
    private void sample(int mode){
        long now=SystemClock.elapsedRealtime();if(mode!=observedMode){observedMode=mode;stableSince=now;dischargeAnchor=-1;dischargeAnchorTime=0;return;}
        if(mode<0||!discharging()){dischargeAnchor=-1;return;}
        int remaining=chargeCounter();
        if(remaining>0){if(dischargeAnchor<0){dischargeAnchor=remaining;dischargeAnchorTime=now;}else if(now-dischargeAnchorTime>=60000&&dischargeAnchor-remaining>=500){double ua=(dischargeAnchor-remaining)*3600000d/(now-dischargeAnchorTime);dischargeAnchor=remaining;dischargeAnchorTime=now;if(ua>=30000&&ua<=8000000){float previous=history.getFloat("rate"+mode,0);float rate=(float)ua;history.edit().putFloat("rate"+mode,previous>0?previous*.65f+rate*.35f:rate).putLong("date"+mode,System.currentTimeMillis()).apply();return;}}}
        if(now-stableSince<45000)return;
        try{int ua=bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);if(ua>=-30000||ua<-8000000)return;float rate=-ua;float old=history.getFloat("rate"+mode,0);history.edit().putFloat("rate"+mode,old>0?old*.8f+rate*.2f:rate).putLong("date"+mode,System.currentTimeMillis()).apply();}catch(Throwable ignored){}
    }
    private int measured(int mode,int charge){
        float rate=history.getFloat("rate"+mode,0);long age=System.currentTimeMillis()-history.getLong("date"+mode,0);if(charge<=0||rate<=0||age<0||age>7L*86400000L)return -1;
        int mins=Math.round(charge/rate*60f);return mins>0&&mins<=20160?mins:-1;
    }
    private String format(int value){if(value<=0)return "Learning…";return value/60>0?value/60+" h "+value%60+" min":value+" min";}
    void estimate(Estimates callback){
        long now=SystemClock.elapsedRealtime();
        if(closed||estimating||(lastQuery!=0&&now-lastQuery<10000))return;
        estimating=true;lastQuery=now;
        worker.execute(()->{
            String[] values={"Learning…","Unavailable","Open settings"};
            try{
                int active=activeMode(),charge=chargeCounter();sample(active);
                int regular=samsungTime(0),saver=samsungTime(1);
                values[1]=format(saver>0?saver:measured(1,charge));
                int normalDischarge=measured(0,charge);
                values[0]=format(normalDischarge>0?normalDischarge:regular);
            }catch(Throwable t){BatteryHook.log("estimate: "+t);}
            final String[] display=values;
            main.post(()->{estimating=false;if(!closed)callback.display(display);});
        });
    }
    void close(){closed=true;worker.shutdownNow();}
}
