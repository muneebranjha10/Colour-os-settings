package com.galaxy.batteryhook;
final class ReleaseRules {
    static int cyclesFromUsage(int usage){return usage>=0&&usage<=5000000?usage/100:-1;}
    static double watts(double ua,int mv){return ua>=0&&Double.isFinite(ua)&&ua<=15000000&&mv>=2500&&mv<=5000?ua*mv/1000000000d:Double.NaN;}
    static int samsungProfileForStandard(){return 0;}
    static int samsungProfileForLight(){return 1;}
    static int modeForSamsungProfile(int profile){return profile==1?0:profile==0?2:-1;}
    static int clampOption(int n){return Math.max(0,Math.min(2,n));}
    // Only probe the native helper inside Samsung Device Care on supported Android versions.
    // The helper's own availability check and a valid firmware profile are required too.
    // ChargingPolicy still requires a tested model/firmware combination;
    // do not confuse this strict charging gate with the read-verified performance helper.
    static boolean testedHelperFamily(String model,int api){
        return "SM-S911B".equalsIgnoreCase(model)&&(api==36||api==37);
    }
    static boolean canProbeLightProfile(String manufacturer,String packageName,int api,int current){
        return "samsung".equalsIgnoreCase(manufacturer)
            && "com.samsung.android.lool".equals(packageName)
            && api>=33&&api<=37&&(current==0||current==1);
    }
    // An unverified or tiny current must not be published as exact charging power.
    static boolean publishPower(double watts,boolean charging,boolean full,boolean uncertain){
        return !uncertain&&Double.isFinite(watts)&&watts>=0&&watts<=75
          &&((charging&&watts>=.5)||(full&&watts<=.5));
    }
    static int batteryBarColor(boolean plugged,boolean saving){
        return saving?0xffffae13:plugged?0xff198bfe:0xff22ce37;
    }
    static boolean supportsProtection(int api,int mode,int limit){return (api==36||api==37)&&(mode==0||mode==1||mode==3)&&limit>=80&&limit<=95&&limit%5==0;}
}
