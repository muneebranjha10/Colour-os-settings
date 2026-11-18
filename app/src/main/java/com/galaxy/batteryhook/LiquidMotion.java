package com.galaxy.batteryhook;
final class LiquidMotion {
    private static float unit(float x){return Math.max(0f,Math.min(1f,x));}
    static float mix(float a,float b,float t){return a+(b-a)*unit(t);}
    private static float smooth(float x){x=unit(x);return x*x*(3f-2f*x);}
    // Front deforms early; rear stays attached to source longer and catches up at the end.
    static float leading(float t){return smooth(t/.76f);}
    static float trailing(float t){return smooth((t-.22f)/.78f);}
    static float dragTail(float previous,float next){if(previous<0)return next;return mix(previous,next,.46f);}
    static float neck(float distance,float height,float strength){return unit(distance)*Math.max(0,height)*Math.max(0,Math.min(.35f,strength));}
}
