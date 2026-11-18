package com.galaxy.batteryhook;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.SystemClock;
import java.util.Locale;
import java.util.ArrayDeque;

final class ChargeTelemetry {
    private final Context context;
    private final BatteryManager manager;
    private double smooth=Double.NaN,peak=Double.NaN;
    private long sampleTime, anchorTime, startTime;
    private int anchorCounter=-1,startPercent=-1, polarity=1, plugVotes=0;
    private double counterCurrent=Double.NaN;
    private long counterTime;
    private String smoothSource="";
    private final ArrayDeque<Double> samples=new ArrayDeque<>();
    ChargeTelemetry(Context c){context=c;manager=(BatteryManager)c.getSystemService(Context.BATTERY_SERVICE);}
    static final class Reading {
        int mv=-1,ua=Integer.MIN_VALUE,average=Integer.MIN_VALUE,counter=-1,temp=Integer.MIN_VALUE,percent=-1,cycles=-1;
        long minutes=-1,visibleSeconds;
        boolean plugged,charging,full,wireless,estimated,uncertain,discharging;
        String wattsText,source="Unavailable";
        double watts=Double.NaN,peak=Double.NaN;
        double[] history=new double[0];
        int gained=-1;
    }
    void reset(){smooth=Double.NaN;sampleTime=0;anchorTime=0;startTime=0;anchorCounter=-1;counterCurrent=Double.NaN;counterTime=0;startPercent=-1;plugVotes=0;peak=Double.NaN;samples.clear();smoothSource="";}
    private int property(int prop){try{return manager==null?Integer.MIN_VALUE:manager.getIntProperty(prop);}catch(Throwable t){return Integer.MIN_VALUE;}}
    private static boolean valid(int v){return v!=Integer.MIN_VALUE&&Math.abs((long)v)<=15000000L;}
    private static boolean useful(int v){return valid(v)&&Math.abs((long)v)>=1000;}
    private void counterSample(Reading r,long now){
        if(r.counter<=0)return;
        if(anchorCounter<0){anchorCounter=r.counter;anchorTime=now;return;}
        long elapsed=now-anchorTime;
        if(elapsed<15000)return;
        int delta=r.counter-anchorCounter;
        if(Math.abs((long)delta)<10&&elapsed<60000)return;
        double ua=delta*3600000d/elapsed;
        anchorCounter=r.counter;anchorTime=now;
        if(Math.abs(ua)>15000000d){counterCurrent=Double.NaN;return;}
        counterCurrent=ua;counterTime=now;
        int raw=useful(r.ua)?r.ua:r.average;
        if(useful(raw)&&Math.abs(ua)>30000){
            int detected=Math.signum(ua)==Math.signum(raw)?1:-1;
            if(detected!=polarity){if(++plugVotes>=2){polarity=detected;plugVotes=0;smooth=Double.NaN;BatteryHook.log("current polarity calibrated: "+polarity);}}else plugVotes=0;
        }
    }
    Reading read(){
        Reading r=new Reading();
        try{
            Intent b=context.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(b==null)return r;
            int plug=b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0),status=b.getIntExtra(BatteryManager.EXTRA_STATUS,0);
            r.plugged=plug!=0;r.charging=status==BatteryManager.BATTERY_STATUS_CHARGING;r.full=status==BatteryManager.BATTERY_STATUS_FULL;r.wireless=plug==BatteryManager.BATTERY_PLUGGED_WIRELESS;
            r.mv=b.getIntExtra(BatteryManager.EXTRA_VOLTAGE,-1);r.temp=b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,Integer.MIN_VALUE);
            r.cycles=b.getIntExtra("android.os.extra.CYCLE_COUNT",-1);
            int l=b.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=b.getIntExtra(BatteryManager.EXTRA_SCALE,-1);if(l>=0&&scale>0&&l<=scale)r.percent=Math.round(100f*l/scale);
            r.ua=property(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);r.average=property(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
            r.counter=property(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);if(r.counter<=0)r.counter=b.getIntExtra("charge_counter",-1);
            long now=SystemClock.elapsedRealtime();
            if(!r.plugged){reset();return r;}
            if(startTime==0){startTime=now;startPercent=r.percent;}
            r.visibleSeconds=(now-startTime)/1000;r.gained=r.percent>=0&&startPercent>=0?r.percent-startPercent:-1;
            counterSample(r,now);
            boolean counterFresh=!Double.isNaN(counterCurrent)&&now-counterTime<=75000;
            double current=Double.NaN;
            if(useful(r.ua)&&r.ua*(double)polarity>0){current=r.ua*(double)polarity;r.source="Instant current";}
            else if(useful(r.average)&&r.average*(double)polarity>0){current=r.average*(double)polarity;r.source="Average current";r.estimated=true;}
            else if(counterFresh&&counterCurrent>0){current=counterCurrent;r.source="Charge-counter estimate";r.estimated=true;}
            else if(r.charging&&(useful(r.ua)||useful(r.average))&&!counterFresh){
                // Some vendor gauges invert polarity. Until confirmed, show magnitude only, not net input.
                int raw=useful(r.ua)?r.ua:r.average;current=Math.abs((double)raw);r.source="Current magnitude · direction unverified";r.uncertain=true;r.estimated=true;
            }else if(counterFresh&&counterCurrent<0){r.discharging=true;r.source="Battery currently discharging";}
            else if(r.full&&valid(r.ua)&&r.ua==0){current=0;r.source="Instant current";}
            else if(r.charging){r.source="Waiting for battery sensor";}
            else{r.source="Charging paused";}
            if(!Double.isNaN(current)&&r.mv>=2500&&r.mv<=5000){
                double power=current*r.mv/1000000000d;
                if(Double.isNaN(smooth)||now-sampleTime>6000||!smoothSource.equals(r.source))smooth=power;
                else if(now-sampleTime>=700)smooth=.45*smooth+.55*power;
                if(now-sampleTime>=700){
                    sampleTime=now;smoothSource=r.source;
                    if(!r.uncertain){peak=Double.isNaN(peak)?smooth:Math.max(peak,smooth);if(samples.size()>=40)samples.removeFirst();samples.addLast(smooth);}
                }
                r.watts=smooth;r.wattsText=String.format(Locale.US,"%.2f W",smooth);
            }else{smooth=Double.NaN;smoothSource="";}
            r.peak=peak;r.history=new double[samples.size()];int ix=0;for(double v:samples)r.history[ix++]=v;
            if(r.charging&&manager!=null){try{long ms=manager.computeChargeTimeRemaining();if(ms>0)r.minutes=Math.max(1,(ms+59999)/60000);}catch(Throwable ignored){}}
        }catch(Throwable e){BatteryHook.log("charging telemetry: "+e);}
        return r;
    }
}
