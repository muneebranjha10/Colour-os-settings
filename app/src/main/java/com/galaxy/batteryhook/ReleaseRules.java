package com.galaxy.batteryhook;
final class ReleaseRules {
    static int cyclesFromUsage(int usage){return usage>=0&&usage<=5000000?usage/100:-1;}
    static double watts(double ua,int mv){return ua>=0&&Double.isFinite(ua)&&ua<=15000000&&mv>=2500&&mv<=5000?ua*mv/1000000000d:Double.NaN;}
    static int clampOption(int n){return Math.max(0,Math.min(2,n));}
    static boolean testedHelperFamily(String model,int api){return "SM-S911B".equalsIgnoreCase(model)&&(api==36||api==37);}
    static boolean supportsProtection(int api,int mode,int limit){return (api==36||api==37)&&(mode==0||mode==1||mode==3)&&limit>=80&&limit<=95&&limit%5==0;}
}
