package com.galaxy.batteryhook;
final class ColorOsPalette {
    static final int ACCENT=0xff42d653;
    static int[] body(int style){return style==0?new int[]{0xff4b4d4b,0xff393a39,0xff303130,0xff414341}:style==2?new int[]{0xff5aa866,0xff245e35,0xff173c25,0xff3c8550}:new int[]{0xff34583b,0xff27442e,0xff1e3022,0xff305037};}
    static int[] edge(int style){return style==2?new int[]{0xffb2f8ba,0xff5d9a65,0xff3c7349,0xff87d997}:style==0?new int[]{0xffa6aaa6,0xff6d706d,0xff4a4c4a,0xff848984}:new int[]{0xff94d09b,0xff588663,0xff3e5d45,0xff78aa80};}
    static int[] sheen(int style){return style==2?new int[]{0x52d7ffde,0x10d5ffe3,0x00ffffff,0x006aff92,0x247dffae}:new int[]{0x32ffffff,0x0cffffff,0x00ffffff,0x00ffffff,0x20ffffff};}
    static int[] glint(int style){return style==2?new int[]{0x64defff0,0x249cf9c6,0x00baffdb}:new int[]{0x45ffffff,0x18ffffff,0x00ffffff};}
    static int[] thumb(int style){return style==2?new int[]{0xffeaffee,0xff8adca6,0xff36774d,0xff86daa1}:new int[]{0xfff7f7f7,0xffdedede,0xffb5b8b5,0xffe5e5e5};}
    static int mix(int start,int end,float factor){
        float t=Math.max(0f,Math.min(1f,factor));
        int a=Math.round(((start>>>24)&255)*(1-t)+((end>>>24)&255)*t);
        int r=Math.round(((start>>>16)&255)*(1-t)+((end>>>16)&255)*t);
        int g=Math.round(((start>>>8)&255)*(1-t)+((end>>>8)&255)*t);
        int b=Math.round((start&255)*(1-t)+(end&255)*t);
        return a<<24|r<<16|g<<8|b;
    }
    static int chroma(int color){int r=(color>>16)&255,g=(color>>8)&255,b=color&255;return Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b));}
}
