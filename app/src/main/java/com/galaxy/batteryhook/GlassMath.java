package com.galaxy.batteryhook;
final class GlassMath {
    static final int MAX_PROGRESS=300;
    static float clamp(float p,float lo,float hi){return Math.max(lo,Math.min(hi,p));}
    static int limitForProgress(int n){return 80+5*Math.round(clamp(n,0,MAX_PROGRESS)/100f);}
    static int progressForLimit(int n){return Math.round(clamp(n,80,95)-80)*20;}
    static float spring(float t){t=clamp(t,0,1);if(t==0||t==1)return t;return 1-(float)(Math.exp(-7.4*t)*Math.cos(9.0*t));}
    private static float ease(float t){t=clamp(t,0,1);return t*t*(3-2*t);}
    // Finger-responsive easing: strong initial movement and a soft final settle.
    // The trailing edge arrives shortly afterwards, but never delays the leading edge.
    static float railHead(float t){t=clamp(t,0,1);return 1-(1-t)*(1-t)*(1-t);}
    static float railTail(float t){t=clamp((t-.055f)/.945f,0,1);return 1-(1-t)*(1-t)*(1-t);}
    static float thumbFraction(float progress,boolean rtl){float f=clamp(progress/MAX_PROGRESS,0,1);return rtl?1-f:f;}
    static float coverage(float slot,int tile){if(slot<0)return 0;float t=clamp(1-Math.abs(slot-tile),0,1);return t*t*(3-2*t);}
    static float followFraction(float elapsedMs){return 1-(float)Math.exp(-Math.max(0,elapsedMs)/22f);}
    static float dragFollower(float previous,float target,float elapsedMs){return between(previous,target,followFraction(elapsedMs));}
    static float elasticPulse(float t){if(t<=0||t>=1)return 0;return .20f*(float)(Math.sin(2*Math.PI*t)*Math.exp(-3.6*t));}
    static float neckFraction(float stretch){return .10f*ease(clamp(stretch,0,1));}
    static float between(float a,float b,float fraction){return a+(b-a)*fraction;}
}
