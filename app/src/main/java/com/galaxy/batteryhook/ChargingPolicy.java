package com.galaxy.batteryhook;
import android.content.Context;
import android.provider.Settings;
import java.lang.reflect.Method;
final class ChargingPolicy {
    private final Context context;
    private final Method reader,writer;
    ChargingPolicy(Context c){context=c;Method r=null,w=null;try{Class<?> cl=Class.forName("ad.m",false,c.getClassLoader());r=cl.getDeclaredMethod("e",Context.class);w=cl.getDeclaredMethod("p",Context.class,boolean.class);r.setAccessible(true);w.setAccessible(true);}catch(Throwable e){BatteryHook.log("charging settings helper unavailable: "+e);}reader=r;writer=w;}
    int read(){try{if(reader!=null&&writer!=null)return Boolean.TRUE.equals(reader.invoke(null,context))?1:0;}catch(Throwable e){BatteryHook.log("read charging policy: "+e);}return -1;}
    boolean apply(int mode){if(mode<0||mode>1||reader==null||writer==null)return false;try{android.content.Intent i=context.registerReceiver(null,new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));if(i==null||i.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED,0)!=0)return false;writer.invoke(null,context,mode==1);return read()==mode;}catch(Throwable e){BatteryHook.log("charging policy: "+e);return false;}}
}
