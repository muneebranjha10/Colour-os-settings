package com.galaxy.batteryhook;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HealthReader {
    static final class Reading {
        int asoc=-1,bsoh=-1,api=-1,cycles=-1,temp=-1,maxTemp=-1,mv=-1,design=-1,full=-1,status=0;
        String asocSource="",bsohSource="",cycleSource="";
        int savedUsage=-1;
        long readTime=System.currentTimeMillis();
        boolean imported;
        int main(){return asoc>=0?asoc:api>=0?api:bsoh;}
        String mainLabel(){return asoc>=0?"Fuel gauge health (ASOC)":api>=0?"Android state of health":bsoh>=0?"Samsung saved health (BSOH)":"Health estimate unavailable";}
    }
    static int readInt(String path){
        try(BufferedReader reader=new BufferedReader(new FileReader(path))){String value=reader.readLine();return value==null?-1:Integer.parseInt(value.trim());}catch(Throwable t){return -1;}
    }
    static int range(int n,int min,int max){return n>=min&&n<=max?n:-1;}
    static int health(int n){return range(n,1,100);}
    static int field(String input,String name,int min,int max){
        try{Matcher m=Pattern.compile("(?m)^\\s*"+Pattern.quote(name)+"\\s*[:=]\\s*(-?\\d+)\\s*$").matcher(input);int last=-1;while(m.find()){int v=range(Integer.parseInt(m.group(1)),min,max);if(v>=0)last=v;}return last;}catch(Throwable t){return -1;}
    }
    static void parseDump(Reading r,String data,String source){
        if(r.asoc<0){int v=field(data,"fg_asoc",1,100);if(v<0)v=field(data,"mSavedBatteryAsoc",1,100);if(v>=0){r.asoc=v;r.asocSource=source;}}
        if(r.bsoh<0){int v=field(data,"mSavedBatteryBsoh",1,100);if(v>=0){r.bsoh=v;r.bsohSource=source;}}
        if(r.maxTemp<0)r.maxTemp=field(data,"mSavedBatteryMaxTemp",0,1000);
        if(r.savedUsage<0){try{Matcher u=Pattern.compile("(?m)^\\s*mSavedBatteryUsage\\s*[:=]\\s*\\[?\\s*(\\d+)\\s*\\]?\\s*$").matcher(data);if(u.find())r.savedUsage=range(Integer.parseInt(u.group(1)),0,5000000);}catch(Throwable ignored){}}
        if(r.cycles<=0){int n=field(data,"battery_cycle",1,50000);
            if(n<0){try{Matcher path=Pattern.compile("(?m)^/sys/class/power_supply/battery/battery_cycle\\s*\\r?\\n\\s*(\\d+)\\s*$").matcher(data);if(path.find())n=range(Integer.parseInt(path.group(1)),1,50000);}catch(Throwable ignored){}}
            if(n>=0){r.cycles=n;r.cycleSource=source+" · battery_cycle";}
        }
    }
    static Reading read(Context c){
        Reading r=new Reading();
        try{
            Intent i=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(i!=null){r.temp=i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,-1);r.mv=i.getIntExtra(BatteryManager.EXTRA_VOLTAGE,-1);r.status=i.getIntExtra(BatteryManager.EXTRA_HEALTH,0);r.cycles=range(i.getIntExtra("android.os.extra.CYCLE_COUNT",-1),1,50000);if(r.cycles>0)r.cycleSource="Android cycle count";}
        }catch(Throwable ignored){}
        r.asoc=health(readInt("/sys/class/power_supply/battery/fg_asoc"));if(r.asoc>=0)r.asocSource="Live fg_asoc";
        int vendor=range(readInt("/sys/class/power_supply/battery/battery_cycle"),1,50000);
        if(vendor>0){r.cycles=vendor;r.cycleSource="Live Samsung battery_cycle";}
        else if(r.cycles<0){int kernel=range(readInt("/sys/class/power_supply/battery/cycle_count"),1,50000);if(kernel>0){r.cycles=kernel;r.cycleSource="Live kernel cycle_count";}}
        r.design=range(readInt("/sys/class/power_supply/battery/charge_full_design"),1000000,20000000);
        r.full=range(readInt("/sys/class/power_supply/battery/charge_full"),1000000,20000000);
        try{BatteryManager b=(BatteryManager)c.getSystemService(Context.BATTERY_SERVICE);if(b!=null){long val=b.getLongProperty(10);if(val>=1&&val<=100)r.api=(int)val;}}catch(Throwable ignored){}
        // Never request root in the host Settings/Device Care process.
        // dumpsys is bounded, read-only, and may fail when the host lacks DUMP permission.
        Process p=null;
        try{
            p=new ProcessBuilder("/system/bin/dumpsys","-t","2","battery").redirectErrorStream(true).start();
            try(BufferedReader in=new BufferedReader(new InputStreamReader(p.getInputStream()))){
                String line;int lines=0;
                while((line=in.readLine())!=null&&++lines<20000){
                    String clean=line.trim();
                    if(clean.matches("(mSavedBatteryAsoc|mSavedBatteryBsoh|mSavedBatteryMaxTemp|mSavedBatteryUsage|battery_cycle|cycle_count|cycle count)\\s*[:=].*"))parseDump(r,clean,"Live Samsung battery service");
                }
            }
        }catch(Throwable t){BatteryHook.log("Health service access unavailable: "+t.getClass().getSimpleName());}finally{if(p!=null)p.destroy();}
        resolveCycles(r);
        return r;
    }
    static void resolveCycles(Reading r){if(r.cycles<=0&&r.savedUsage>=0){r.cycles=r.savedUsage/100;r.cycleSource="Samsung saved usage ÷ 100 (estimate)";}}
}
