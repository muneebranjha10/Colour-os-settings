package com.galaxy.batteryhook;
import android.content.Context;
import android.provider.Settings;
import java.lang.reflect.Method;
final class ChargingPolicy {
    private final Context context;
    private final Method reader,writer;
    ChargingPolicy(Context c){context=c;Method r=null,w=null;try{if(ReleaseRules.testedHelperFamily(android.os.Build.MODEL,android.os.Build.VERSION.SDK_INT)){Class<?> cl=Class.forName("ad.m",false,c.getClassLoader());Method rr=cl.getDeclaredMethod("e",Context.class),ww=cl.getDeclaredMethod("p",Context.class,boolean.class);if(rr.getReturnType()==boolean.class&&java.lang.reflect.Modifier.isStatic(rr.getModifiers())&&java.lang.reflect.Modifier.isStatic(ww.getModifiers())){rr.setAccessible(true);ww.setAccessible(true);r=rr;w=ww;}}}catch(Throwable e){BatteryHook.log("charging settings helper unavailable: "+e);}reader=r;writer=w;}
    int read(){try{int n=Settings.System.getInt(context.getContentResolver(),"adaptive_fast_charging",-1);return n==0||n==1?n:-1;}catch(Throwable e){return -1;}}
    boolean canApply(){try{return reader!=null&&writer!=null&&read()>=0&&(Boolean.TRUE.equals(reader.invoke(null,context))?1:0)==read();}catch(Throwable e){return false;}}
    boolean apply(int mode){if(mode<0||mode>1||!canApply())return false;try{android.content.Intent i=context.registerReceiver(null,new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));if(i==null||i.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED,0)!=0)return false;writer.invoke(null,context,mode==1);return read()==mode;}catch(Throwable e){BatteryHook.log("charging policy: "+e);return false;}}
}
