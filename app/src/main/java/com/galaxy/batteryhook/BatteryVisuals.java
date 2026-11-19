package com.galaxy.batteryhook;
final class BatteryVisuals {
    static final int NORMAL=0xff17db26,CHARGING=0xff168cf4,SAVING=0xffe8a23a,LOW=0xffed555c,UNKNOWN=0xff666666;
    static float level(int percent){return Math.max(0,Math.min(100,percent))/100f;}
    static int color(int percent,boolean connected,boolean charging,boolean saving,boolean dynamic){
        if(percent<0||percent>100)return UNKNOWN;
        if(!dynamic)return connected?CHARGING:NORMAL;
        if(charging&&connected&&percent<100)return CHARGING;
        if(percent<=15)return LOW;
        if(saving)return SAVING;
        return NORMAL;
    }
    static boolean animateWave(boolean charging,boolean enabled,boolean quiet,boolean motion,boolean visible,int level){return charging&&enabled&&!quiet&&motion&&visible&&level>0&&level<100;}
    static float waveCenter(float fill,float length,float phase){return -length+(Math.max(0,fill)+2*length)*GlassMath.clamp(phase,0,1);}
}
