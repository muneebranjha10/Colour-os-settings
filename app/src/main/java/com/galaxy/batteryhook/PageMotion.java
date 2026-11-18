package com.galaxy.batteryhook;
final class PageMotion {
    static int time(int ms,boolean quiet){return quiet?Math.min(120,Math.max(1,ms)):Math.max(1,ms);}
    static float parallax(int width,boolean returning){return returning?0f:-Math.max(0,width)*.035f;}
}
