package com.galaxy.batteryhook;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class BatteryHook implements IXposedHookLoadPackage {
    private static final String PKG="com.samsung.android.lool";
    private static final String ACT="com.samsung.android.sm.battery.ui.BatteryActivity";
    private static final String TAG="ColourOSBatteryRC1";
    static void log(String s){ try{XposedBridge.log("["+TAG+"] "+s);}catch(Throwable ignored){} }
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p){
        if(!PKG.equals(p.packageName)||!PKG.equals(p.processName))return;
        if(!"samsung".equalsIgnoreCase(android.os.Build.MANUFACTURER)||android.os.Build.VERSION.SDK_INT<31){log("Unsupported platform; stock page retained");return;}
        try{
            XposedHelpers.findAndHookMethod(ACT,p.classLoader,"onResume",new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam param){
                    Activity a=(Activity)param.thisObject;
                    a.getWindow().getDecorView().post(()->{try{if(!a.isFinishing()&&!a.isDestroyed())attach(a);}catch(Throwable t){log("attach failed: "+t);}});
                }
            });
            log("onResume hook installed");
        }catch(Throwable t){log("hook failed: "+t);}
        installFontPickerHook(p.classLoader);
        // Legacy back fallback, only while our health page is open in BatteryActivity.
        XC_MethodHook back=new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam param){
            if(!(param.thisObject instanceof Activity)||!ACT.equals(param.thisObject.getClass().getName()))return;
            Activity a=(Activity)param.thisObject;ViewGroup host=a.findViewById(android.R.id.content);if(host==null)return;View v=host.findViewWithTag(TAG);
            if(v instanceof Dashboard&&((Dashboard)v).handleHealthBack())param.setResult(null);
        }};
        try{XposedHelpers.findAndHookMethod(ACT,p.classLoader,"onBackPressed",back);}catch(Throwable t){
            try{XposedHelpers.findAndHookMethod("android.app.Activity",p.classLoader,"onBackPressed",back);}catch(Throwable ignored){log("legacy health back fallback unavailable");}
        }
    }
    private static final java.util.concurrent.atomic.AtomicBoolean fontPickerBusy=new java.util.concurrent.atomic.AtomicBoolean(false);
    private void installFontPickerHook(ClassLoader loader){
        XC_MethodHook result=new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){
                if(!(p.thisObject instanceof Activity)||!ACT.equals(p.thisObject.getClass().getName()))return;
                if(((Integer)p.args[0])!=FontStore.REQUEST||((Integer)p.args[1])!=Activity.RESULT_OK)return;
                android.content.Intent data=(android.content.Intent)p.args[2];
                if(data==null||data.getData()==null||!fontPickerBusy.compareAndSet(false,true))return;
                Activity activity=(Activity)p.thisObject;android.net.Uri uri=data.getData();
                new Thread(()->{
                    String feedback="OPPO Sans font imported";
                    try{FontStore.importFromDocument(activity,uri);}catch(Throwable t){feedback="Font import failed: "+t.getMessage();log("Font import: "+t);}
                    final String message=feedback;
                    activity.runOnUiThread(()->{
                        try{ViewGroup content=activity.findViewById(android.R.id.content);if(content!=null){View view=content.findViewWithTag(TAG);if(view instanceof Dashboard)((Dashboard)view).refreshFont();}
                            android.widget.Toast.makeText(activity,message,android.widget.Toast.LENGTH_LONG).show();
                        }catch(Throwable t){log("Font UI refresh: "+t);}finally{fontPickerBusy.set(false);}
                    });
                },"ColorBattery-FontImport").start();
            }
        };
        try{XposedHelpers.findAndHookMethod(ACT,loader,"onActivityResult",int.class,int.class,android.content.Intent.class,result);}catch(Throwable ignored){}
        try{XposedHelpers.findAndHookMethod("android.app.Activity",loader,"onActivityResult",int.class,int.class,android.content.Intent.class,result);}catch(Throwable t){log("Font picker callback unsupported: "+t);}
    }
    private final java.util.Set<Activity> stock=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<Activity,Boolean>());
    private void attach(Activity a){
        if(stock.contains(a))return;
        ViewGroup host=a.findViewById(android.R.id.content);
        if(!(host instanceof FrameLayout)){log("unsupported content root");return;}
        View old=host.findViewWithTag(TAG);
        if(old instanceof Dashboard){((Dashboard)old).ensureBars();((Dashboard)old).refresh();return;}
        Dashboard d=new Dashboard(a,()->{stock.add(a);View current=host.findViewWithTag(TAG);if(current!=null)host.removeView(current);});
        d.setTag(TAG);
        host.addView(d,new FrameLayout.LayoutParams(-1,-1));
        d.bringToFront();d.post(d::ensureBars);
        log("native dashboard attached");
    }
}
