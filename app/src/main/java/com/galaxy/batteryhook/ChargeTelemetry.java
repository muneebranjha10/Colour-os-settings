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
    private int socAnchor=-1; private long socTime,socSampleTime; private double socCurrent=Double.NaN;
    private boolean sysfsDenied;private long sysfsReadAt;private int sysfsCache=Integer.MIN_VALUE;
    private double counterCurrent=Double.NaN;
    private long counterTime;
    private String smoothSource="";
    private final ArrayDeque<Double> samples=new ArrayDeque<>();
    ChargeTelemetry(Context c){context=c;manager=(BatteryManager)c.getSystemService(Context.BATTERY_SERVICE);}
    static final class Reading {
        int mv=-1,ua=Integer.MIN_VALUE,average=Integer.MIN_VALUE,counter=-1,temp=Integer.MIN_VALUE,percent=-1,cycles=-1,sysfsCurrent=Integer.MIN_VALUE;
        double effectiveUa=Double.NaN;
        long minutes=-1,visibleSeconds;
        boolean plugged,charging,full,wireless,estimated,uncertain,discharging;
        String wattsText,source="Unavailable";
        double watts=Double.NaN,peak=Double.NaN;
        double[] history=new double[0];
        int gained=-1;
    }
    void reset(){smooth=Double.NaN;sampleTime=0;anchorTime=0;startTime=0;anchorCounter=-1;counterCurrent=Double.NaN;counterTime=0;startPercent=-1;plugVotes=0;peak=Double.NaN;samples.clear();smoothSource="";socAnchor=-1;socTime=0;socCurrent=Double.NaN;socSampleTime=0;}
    private int property(int prop){try{return manager==null?Integer.MIN_VALUE:manager.getIntProperty(prop);}catch(Throwable t){return Integer.MIN_VALUE;}}
    private static boolean valid(int v){return v!=Integer.MIN_VALUE&&Math.abs((long)v)<=15000000L;}
    private static boolean useful(int v){return valid(v)&&Math.abs((long)v)>=50000;}
    private void counterSample(Reading r,long now){
        if(r.counter<=0)return;
        if(anchorCounter<0){anchorCounter=r.counter;anchorTime=now;return;}
        long elapsed=now-anchorTime;
        if(elapsed<20000)return;
        int delta=r.counter-anchorCounter;
        if(Math.abs((long)delta)<150&&elapsed<120000)return;
        double ua=delta*3600000d/elapsed;
        anchorCounter=r.counter;anchorTime=now;
        if(Math.abs(ua)>15000000d){counterCurrent=Double.NaN;return;}
        if(Math.abs(ua)>=20000||Double.isNaN(counterCurrent)||now-counterTime>120000){counterCurrent=ua;counterTime=now;}
        int raw=useful(r.ua)?r.ua:r.average;
        if(useful(raw)&&Math.abs(ua)>30000){
            int detected=Math.signum(ua)==Math.signum(raw)?1:-1;
            if(detected!=polarity){if(++plugVotes>=2){polarity=detected;plugVotes=0;smooth=Double.NaN;BatteryHook.log("current polarity calibrated: "+polarity);}}else plugVotes=0;
        }
    }
    private int readSysfs(){
        if(sysfsDenied)return Integer.MIN_VALUE;long now=SystemClock.elapsedRealtime();if(sysfsReadAt>0&&now-sysfsReadAt<4000)return sysfsCache;sysfsReadAt=now;sysfsCache=Integer.MIN_VALUE;
        boolean accessible=false;
        String[] paths={"/sys/class/power_supply/battery/current_now","/sys/class/power_supply/bms/current_now","/sys/class/power_supply/fuelgauge/current_now"};
        for(String path:paths){try(java.io.BufferedReader in=new java.io.BufferedReader(new java.io.FileReader(path))){accessible=true;String line=in.readLine();if(line!=null){int v=Integer.parseInt(line.trim());if(useful(v)){sysfsCache=v;return v;}}}catch(Throwable e){}}
        if(!accessible)sysfsDenied=true;return Integer.MIN_VALUE;
    }
    private void socSample(Reading r,long now){
        if(r.percent<1||r.counter<=0)return;
        if(socAnchor<0){socAnchor=r.percent;socTime=now;return;}
        long elapsed=now-socTime;if(elapsed<45000||r.percent==socAnchor)return;
        double full=(double)r.counter*100/r.percent, current=(r.percent-socAnchor)*full/100*3600000d/elapsed;
        if(full>=1000000&&full<=20000000&&Math.abs(current)>=50000&&Math.abs(current)<=15000000){socCurrent=current;socSampleTime=now;}
        socAnchor=r.percent;socTime=now;
    }
    Reading read(){
        Reading r=new Reading();
        try{
            Intent b=context.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(b==null)return r;
            int plug=b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0),status=b.getIntExtra(BatteryManager.EXTRA_STATUS,0);
            r.plugged=plug!=0;r.charging=status==BatteryManager.BATTERY_STATUS_CHARGING;r.full=status==BatteryManager.BATTERY_STATUS_FULL;r.wireless=plug==BatteryManager.BATTERY_PLUGGED_WIRELESS;
            r.mv=b.getIntExtra(BatteryManager.EXTRA_VOLTAGE,-1);r.temp=b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,Integer.MIN_VALUE);
            r.cycles=b.getIntExtra("android.os.extra.CYCLE_COUNT",-1);if(r.cycles<=0)r.cycles=-1;
            int l=b.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=b.getIntExtra(BatteryManager.EXTRA_SCALE,-1);if(l>=0&&scale>0&&l<=scale)r.percent=Math.round(100f*l/scale);
            r.ua=property(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);r.average=property(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
            r.counter=property(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);if(r.counter<=0)r.counter=b.getIntExtra("charge_counter",-1);
            long now=SystemClock.elapsedRealtime();
            if(!r.plugged){reset();return r;}
            if(startTime==0){startTime=now;startPercent=r.percent;}
            r.visibleSeconds=(now-startTime)/1000;r.gained=r.percent>=0&&startPercent>=0?r.percent-startPercent:-1;
            counterSample(r,now);socSample(r,now);
            boolean counterFresh=!Double.isNaN(counterCurrent)&&now-counterTime<=120000;
            boolean socFresh=!Double.isNaN(socCurrent)&&now-socSampleTime<=180000;
            r.sysfsCurrent=readSysfs();
            double current=Double.NaN;
            double direct=useful(r.ua)?r.ua*(double)polarity:Double.NaN;
            if(counterFresh&&counterCurrent<-50000){r.discharging=true;r.source="Battery currently discharging";}
            else if(!r.charging&&!r.full){r.source="Charging paused";}
            else if(counterFresh&&counterCurrent>50000&&(!Double.isFinite(direct)||direct<=0||direct<counterCurrent*.10)){
                current=counterCurrent;r.source="Charge-counter rate";r.estimated=true;
            }else if(Double.isFinite(direct)&&direct>0){current=direct;r.source="Instant current";}
            else if(useful(r.average)&&r.average*(double)polarity>0&&(!counterFresh||counterCurrent<=0||r.average*(double)polarity>=counterCurrent*.10)){current=r.average*(double)polarity;r.source="Average current";r.estimated=true;}
            else if(useful(r.sysfsCurrent)&&r.charging&&(!counterFresh||counterCurrent>=0)){
                current=Math.abs((double)r.sysfsCurrent);r.source="Fuel gauge · current magnitude";r.uncertain=true;r.estimated=true;
            }else if(counterFresh&&counterCurrent>50000){current=counterCurrent;r.source="Charge-counter rate";r.estimated=true;}
            else if(counterFresh&&counterCurrent<-50000){r.discharging=true;r.source="Battery currently discharging";}
            else if(r.charging&&socFresh&&socCurrent>50000){current=socCurrent;r.source="Percentage-change estimate";r.estimated=true;}
            else if(r.charging&&(useful(r.ua)||useful(r.average))&&!counterFresh){
                int raw=useful(r.ua)?r.ua:r.average;current=Math.abs((double)raw);r.source="Current magnitude · direction unverified";r.uncertain=true;r.estimated=true;
            }else if(r.full&&valid(r.ua)&&r.ua==0){current=0;r.source="Instant current";}
            else if(r.charging){r.source="Measuring · current sensor unavailable";}
            else{r.source="Charging paused";}
            r.effectiveUa=current;
            if(!Double.isNaN(current)&&r.mv>=2500&&r.mv<=5000){
                double power=ReleaseRules.watts(current,r.mv);
                if(Double.isNaN(smooth)||now-sampleTime>6000||!smoothSource.equals(r.source))smooth=power;
                else if(now-sampleTime>=700)smooth=.45*smooth+.55*power;
                if(now-sampleTime>=700){
                    sampleTime=now;smoothSource=r.source;
                    if(ReleaseRules.publishPower(power,r.charging,r.full,r.uncertain)){peak=Double.isNaN(peak)?smooth:Math.max(peak,smooth);if(samples.size()>=40)samples.removeFirst();samples.addLast(smooth);}
                }
                if(ReleaseRules.publishPower(smooth,r.charging,r.full,r.uncertain)){
                    r.watts=smooth;
                    r.wattsText=String.format(Locale.US,"%.1f W",smooth);
                }else{
                    r.watts=Double.NaN;
                    r.wattsText=null;
                    if(r.uncertain)r.source="Battery current direction unverified";
                    else if(r.charging&&Double.isFinite(smooth)&&smooth<.5)r.source="Charging · current too low to verify watts";
                }
            }else{smooth=Double.NaN;smoothSource="";}
            r.peak=peak;r.history=new double[samples.size()];int ix=0;for(double v:samples)r.history[ix++]=v;
            if(r.charging&&manager!=null){try{long ms=manager.computeChargeTimeRemaining();if(ms>0)r.minutes=Math.max(1,(ms+59999)/60000);}catch(Throwable ignored){}}
        }catch(Throwable e){BatteryHook.log("charging telemetry: "+e);}
        return r;
    }
}
