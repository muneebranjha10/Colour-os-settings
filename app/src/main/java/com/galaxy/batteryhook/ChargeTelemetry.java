package com.galaxy.batteryhook;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import java.util.Locale;
final class ChargeTelemetry {
    private final Context context;
    private final BatteryManager manager;
    private double smooth=Double.NaN;
    private long sampleTime;
    ChargeTelemetry(Context c){context=c;manager=(BatteryManager)c.getSystemService(Context.BATTERY_SERVICE);}
    static final class Reading {
        int mv=-1,ua=Integer.MIN_VALUE;long minutes=-1;boolean plugged,charging,full,wireless;String wattsText;
    }
    void reset(){smooth=Double.NaN;sampleTime=0;}
    Reading read(){
        Reading r=new Reading();
        try{
            Intent b=context.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(b==null)return r;
            int plug=b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0),status=b.getIntExtra(BatteryManager.EXTRA_STATUS,0);r.plugged=plug!=0;r.charging=status==BatteryManager.BATTERY_STATUS_CHARGING;r.full=status==BatteryManager.BATTERY_STATUS_FULL;r.wireless=plug==BatteryManager.BATTERY_PLUGGED_WIRELESS;r.mv=b.getIntExtra(BatteryManager.EXTRA_VOLTAGE,-1);
            if(!r.plugged||manager==null){reset();return r;}
            r.ua=manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            if(r.ua!=Integer.MIN_VALUE&&r.ua>=-15000000&&r.ua<=15000000&&r.mv>=2500&&r.mv<=5000){
                double watts=Math.max(0,r.ua)*r.mv/1000000000d;
                long now=android.os.SystemClock.elapsedRealtime();if(Double.isNaN(smooth)||now-sampleTime>5000||!r.charging||watts==0)smooth=watts;else if(now-sampleTime>=650)smooth=.48*smooth+.52*watts;sampleTime=now;
                r.wattsText=String.format(Locale.US,"%.1f W",smooth);
            }else reset();
            if(r.charging){long time=manager.computeChargeTimeRemaining();if(time>0)r.minutes=Math.max(1,(time+59999)/60000);}
        }catch(Throwable e){BatteryHook.log("charging telemetry: "+e);}
        return r;
    }
}
