package com.galaxy.batteryhook;
final class PageMotion {
    static int time(int ms,boolean quiet){return quiet?Math.min(120,Math.max(1,ms)):Math.max(1,ms);}
    static int duration(int ms,int style,boolean quiet){return Math.max(1,Math.round(ms*(quiet?.5f:style==0?.8f:style==2?1.6f:1.22f)));}
    static int healthExit(int ms,boolean quiet){return quiet?Math.min(100,Math.max(1,ms)):Math.max(140,Math.min(480,Math.round(ms*.95f)));}
    static float parallax(int width,boolean returning){return returning?0f:-Math.max(0,width)*.035f;}
}
