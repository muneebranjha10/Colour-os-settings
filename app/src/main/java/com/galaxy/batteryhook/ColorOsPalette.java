package com.galaxy.batteryhook;
final class ColorOsPalette {
    static final int ACCENT=0xff42d653;
    static int[] body(int style){
        return style==0?new int[]{0xff3d3d3d,0xff303030,0xff343434}:style==2?new int[]{0xff646664,0xff414241,0xff303130,0xff424442,0xff535653}:new int[]{0xff4b4d4b,0xff393a39,0xff303130,0xff414341};
    }
    static int[] edge(int style){return new int[]{style==2?0xffd1d3d1:0xffa6aaa6,0xff6d706d,0xff4a4c4a,0xff848984};}
    static int[] thumb(int style){return style==2?new int[]{0xfffafafa,0xffd8dbd8,0xff959995,0xffd0d3d0}:new int[]{0xfff7f7f7,0xffdedede,0xffb5b8b5,0xffe5e5e5};}
    static int chroma(int color){int r=(color>>16)&255,g=(color>>8)&255,b=color&255;return Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b));}
}
