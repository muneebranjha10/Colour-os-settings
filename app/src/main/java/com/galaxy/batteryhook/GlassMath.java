package com.galaxy.batteryhook;
final class GlassMath {
    static final int MAX_PROGRESS=300;
    static float clamp(float p,float lo,float hi){return Math.max(lo,Math.min(hi,p));}
    static int limitForProgress(int n){return 80+5*Math.round(clamp(n,0,MAX_PROGRESS)/100f);}
    static int progressForLimit(int n){return Math.round(clamp(n,80,95)-80)*20;}
    static float spring(float t){t=clamp(t,0,1);if(t==0||t==1)return t;return 1-(float)(Math.exp(-7.4*t)*Math.cos(9.0*t));}
    static float railHead(float t){return clamp(spring(t),0,1.035f);}
    static float railTail(float t){return spring(clamp((t-.10f)/.90f,0,1));}
    static float thumbFraction(float progress,boolean rtl){float f=clamp(progress/MAX_PROGRESS,0,1);return rtl?1-f:f;}
    static float between(float a,float b,float fraction){return a+(b-a)*fraction;}
}
