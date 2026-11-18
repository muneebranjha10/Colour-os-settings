package com.galaxy.batteryhook;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.animation.ArgbEvaluator;
import android.graphics.Path;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.database.ContentObserver;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.WindowInsets;
import android.view.animation.PathInterpolator;
import android.util.TypedValue;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

final class Dashboard extends LinearLayout {
    private static final int BG=0xff000000,CARD=0xff1b1b1b,TILE=0xff292929,WHITE=0xfff2f2f2,MUTED=0xff9a9a9a,BLUE=0xff168cf4,GREEN=0xff39ce49;
    private final Activity a;
    private final Context uiContext;
    private final Typeface deviceTypeface;
    private TextView assistantSummary,chargeDetail,liveTemp,liveCurrent,liveVoltage,liveCharge,liveSession,liveSource,chargeNeeded,chargePolicyNote,thermalNote;
    private final LinearLayout[] chargingTiles=new LinearLayout[2];private final TextView[] chargingTitles=new TextView[2];
    private int thermalLimit=0;private boolean thermalWarned;
    private ChargingPolicy chargingPolicy;
    private PowerGraph powerGraph;
    private ChargeTelemetry.Reading lastReading;
    private ChargeTelemetry telemetry;
    private final Runnable powerTick=new Runnable(){public void run(){if(!leaving&&healthScreen==null&&isAttachedToWindow()&&isShown()&&hasWindowFocus()&&pluggedIn){updateChargingHeader();handler.postDelayed(this,quietMode?5000:2000);}}};
    private String[] estimates={"Learning…","Learning…","Learning…"};
    private boolean pluggedIn;
    private int currentPercent=-1;
    private LinearLayout tileRow;
    
    private SelectionLens lens;private GlassRail usageRail;private int usageRange;private boolean usageOpening;
    private HealthScreen healthScreen;private View dashboardHeader,dashboardScroll;private boolean leaving;
    private ChargingLens chargingLens;private LinearLayout chargingRow,monitorSection;private boolean chargingDragging;
    private int glassStyle=1,animationStyle=1;private boolean showMonitor=true,showWatts=true,quietMode=false,haptics=true;
    private int chargeReminder=0;private boolean reminderFired;private TextView reminderNote;
    private ProtectionCard protectionCard;
    private boolean modeDragging;
    private final Handler handler=new Handler(Looper.getMainLooper());
    
    private final NativeModes engine;
    private Switch superSwitch;
    private TextView superNote;
    private int selected=-2;
    private boolean syncingSwitch, busy, ready;
    private long queryGeneration;
    private final Runnable tick=new Runnable(){public void run(){if(!leaving&&healthScreen==null&&isAttachedToWindow()&&isShown()&&hasWindowFocus()){refresh();handler.postDelayed(this,quietMode?30000:15000);}}};
    private final ContentObserver modeObserver=new ContentObserver(handler){@Override public void onChange(boolean self){if(!leaving&&hasWindowFocus()&&isShown())refresh();}};
    private boolean observing;
    private final Runnable original;
    private final TextView number,charge;
    private final Gauge bar;
    private final LinearLayout[] modes=new LinearLayout[3];
    private final TextView[] modeLabels=new TextView[3];
    private final TextView[] modeNotes=new TextView[3];
    private final Symbol[] modeIcons=new Symbol[3];
    private Intent battery;
    private boolean registered;
    private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(!leaving&&hasWindowFocus()&&isShown())refresh();}};
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int c,int radius){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,float sp,int color,boolean bold){TextView t=new TextView(uiContext);t.setText(value);t.setTextSize(TypedValue.COMPLEX_UNIT_SP,sp);t.setTextColor(color);t.setIncludeFontPadding(false);t.setTypeface(Typeface.create(deviceTypeface,bold?650:400,false));return t;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(a);l.setOrientation(VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(a);l.setOrientation(HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void space(LinearLayout l,int height){l.addView(new View(a),new LayoutParams(1,dp(height)));}
    private void add(LinearLayout l,View v,int height){l.addView(v,new LayoutParams(-1,height<0?height:dp(height)));}
    private LinearLayout card(){LinearLayout l=col();l.setBackground(shape(CARD,25));l.setPadding(dp(18),dp(18),dp(18),dp(17));return l;}
    private void section(LinearLayout l,String label){TextView t=text(label,13,MUTED,false);t.setPadding(dp(14),0,0,0);add(l,t,-2);space(l,10);}
    private void line(LinearLayout l){View v=new View(a);v.setBackgroundColor(0xff343434);add(l,v,1);}
    private void tap(View v,Runnable action){MotionUi.ripple(v);bindTap(v,action);}
    private void tapGlass(View v,float radius,Runnable action){MotionUi.glassTouch(v,radius);bindTap(v,action);}
    private void bindTap(View v,Runnable action){v.setClickable(true);v.setFocusable(true);v.setOnClickListener(w->{try{action.run();}catch(Throwable e){BatteryHook.log("action failed: "+e);Toast.makeText(a,"This control is unavailable on this device.",Toast.LENGTH_SHORT).show();}});}
    Dashboard(Activity activity,Runnable restore){
        super(activity);a=activity;original=restore;
        uiContext=a;Typeface nativeFont=new TextView(a).getTypeface();deviceTypeface=nativeFont==null?Typeface.DEFAULT:nativeFont;engine=new NativeModes(a);telemetry=new ChargeTelemetry(a);chargingPolicy=new ChargingPolicy(a);android.content.SharedPreferences opts=a.getSharedPreferences("colour_battery_options",0);glassStyle=ReleaseRules.clampOption(opts.getInt("glass_style",1));animationStyle=ReleaseRules.clampOption(opts.getInt("animation_style",1));showMonitor=opts.getBoolean("show_monitor",true);showWatts=opts.getBoolean("show_watts",true);quietMode=opts.getBoolean("quiet_mode",false);haptics=opts.getBoolean("haptics",true);chargeReminder=opts.getInt("charge_reminder",0);thermalLimit=a.getSharedPreferences("colour_battery_options",0).getInt("thermal_alert",0);
        setOrientation(VERTICAL);setBackgroundColor(BG);setClickable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        WindowBars.bind(a.getWindow(),this);
        LinearLayout header=row();dashboardHeader=header;header.setPadding(dp(19),dp(6),dp(19),dp(6));
        FrameLayout back=new FrameLayout(a);back.setBackground(shape(TILE,25));Symbol arrow=new Symbol(0,WHITE);back.addView(arrow,new FrameLayout.LayoutParams(-1,-1));back.setContentDescription("Back");tap(back,this::leaveDashboard);header.addView(back,new LayoutParams(dp(40),dp(40)));
        TextView title=text("Battery",19,WHITE,false);LayoutParams tl=new LayoutParams(0,-2,1);tl.leftMargin=dp(12);header.addView(title,tl);
        TextView menu=text("⋮",28,MUTED,true);menu.setGravity(Gravity.CENTER);menu.setContentDescription("Original Samsung battery settings");header.addView(menu,new LayoutParams(dp(40),dp(44)));tap(menu,()->showPanel(new AlertDialog.Builder(a).setTitle("Battery settings").setItems(new String[]{"Customize appearance","Original Samsung Battery page","Samsung battery protection","Charging settings"},(d,w)->{if(w==0)customize();else if(w==1)original.run();else if(w==2)openNative("com.samsung.android.sm.battery.ui.protection.BatteryProtectionActivity");else openNative("com.samsung.android.sm.battery.ui.BatteryAdvancedMenuActivity");})));
        add(this,header,56);
        ScrollView scroll=new ScrollView(a);dashboardScroll=scroll;scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);addView(scroll,new LayoutParams(-1,0,1));
        LinearLayout body=col();body.setPadding(dp(18),dp(12),dp(18),dp(24));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        LinearLayout chargeRow=row();chargeRow.setGravity(Gravity.BOTTOM);
        LinearLayout amount=row();amount.setGravity(Gravity.BOTTOM);number=text("—",44,WHITE,true);number.setIncludeFontPadding(true);amount.addView(number);TextView pct=text("%",16,WHITE,true);pct.setPadding(dp(4),0,0,dp(5));amount.addView(pct);chargeRow.addView(amount,new LayoutParams(-2,-2));LinearLayout chargingText=col();chargingText.setGravity(Gravity.END);chargingText.setPadding(dp(6),0,0,dp(4));charge=text("",12,WHITE,true);charge.setGravity(Gravity.END);charge.setSingleLine(true);charge.setAutoSizeTextTypeUniformWithConfiguration(9,12,1,TypedValue.COMPLEX_UNIT_SP);chargeDetail=text("",10,MUTED,false);chargeDetail.setGravity(Gravity.END);chargeDetail.setMaxLines(2);chargingText.addView(charge,new LayoutParams(-1,dp(19)));chargingText.addView(chargeDetail,new LayoutParams(-1,-2));chargeRow.addView(chargingText,new LayoutParams(0,-2,1));tap(chargingText,this::chargingInfo);add(body,chargeRow,-2);space(body,8);
        bar=new Gauge();add(body,bar,29);space(body,23);
        TextView instructions=text("Instructions ⓘ",14,MUTED,false);instructions.setPadding(dp(10),0,0,dp(2));add(body,instructions,-2);tap(instructions,()->dialog("Instructions","Balanced uses Samsung Light profile on supported phones; High performance uses Samsung Standard (no overclocking). Power saving and Super power saving control the same Samsung saver. Times are Samsung estimates or measured discharge estimates, not guarantees. Tap ⋮ for the original page."));space(body,9);
        LinearLayout main=card();add(body,main,-2);
        LinearLayout tiles=row();tileRow=tiles;String[] captions={"Balanced\nmode","Power saving\nmode",engine.supportsLight()?"High performance\nmode":"Performance\nsettings"};
        for(int i=0;i<3;i++){
            final int index=i;LinearLayout tile=col();tile.setGravity(Gravity.CENTER_HORIZONTAL);tile.setPadding(dp(3),dp(12),dp(3),dp(10));
            LayoutParams p=new LayoutParams(0,dp(122),1);if(i>0)p.leftMargin=dp(9);tiles.addView(tile,p);modes[i]=tile;
            Symbol symbol=new Symbol(i+1,WHITE);modeIcons[i]=symbol;tile.addView(symbol,new LayoutParams(dp(31),dp(30)));space(tile,5);
            TextView label=text(captions[i],12.5f,WHITE,false);label.setGravity(Gravity.CENTER);label.setMaxLines(3);label.setBreakStrategy(0);label.setHyphenationFrequency(0);label.setAutoSizeTextTypeUniformWithConfiguration(10,13,1,TypedValue.COMPLEX_UNIT_SP);modeLabels[i]=label;tile.addView(label,new LayoutParams(-1,0,1));
            TextView note=text("Estimating…",10.5f,MUTED,false);note.setGravity(Gravity.CENTER);note.setSingleLine(true);note.setAutoSizeTextTypeUniformWithConfiguration(9,11,1,TypedValue.COMPLEX_UNIT_SP);modeNotes[i]=note;add(tile,note,16);
            tapGlass(tile,22,()->choose(index));
        }FrameLayout tileHost=new ModeTouchHost();lens=new SelectionLens();tileHost.addView(lens,new FrameLayout.LayoutParams(-1,-1));tileHost.addView(tiles,new FrameLayout.LayoutParams(-1,dp(122)));lens.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);add(main,tileHost,122);space(main,12);
        TextView modeExplanation=text(engine.supportsLight()?"Balanced: Light · High performance: Standard":"Balanced: normal · Performance opens native settings",11,MUTED,false);modeExplanation.setGravity(Gravity.CENTER);add(main,modeExplanation,-2);space(main,12);
        LinearLayout superRow=row();LinearLayout superText=col();add(superText,text("Super power saving mode",18,WHITE,false),-2);space(superText,5);superNote=text("Uses Samsung Power saving",13,MUTED,false);add(superText,superNote,-2);superRow.addView(superText,new LayoutParams(0,-2,1));superSwitch=new Switch(uiContext);superSwitch.setShowText(false);superSwitch.setThumbTintList(ColorStateList.valueOf(WHITE));superSwitch.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xff43af54,0xff555555}));superSwitch.setContentDescription("Super power saving mode (Samsung Power saving)");superRow.addView(superSwitch,new LayoutParams(-2,-2));add(main,superRow,-2);superSwitch.setOnCheckedChangeListener((b,checked)->{if(!syncingSwitch)choose(checked?1:engine.normalMode());});tap(superText,()->{if(!busy)choose(engine.isSaving()?engine.normalMode():1);});space(main,15);line(main);space(main,14);
        item(main,"Adaptive battery","Intelligently extends battery life based on your usage patterns and activity.",()->openNative("com.samsung.android.sm.battery.ui.setting.AdaptiveBatteryMenuActivity"));space(main,22);
        item(main,"Battery health",null,this::health);
        space(body,18);protectionCard=new ProtectionCard(a,()->openNative("com.samsung.android.sm.battery.ui.protection.BatteryProtectionActivity"),()->glassStyle,()->quietMode,this::duration,()->haptics);add(body,protectionCard,-2);
        space(body,18);
        LinearLayout assistant=card();LinearLayout ar=row();Symbol ai=new Symbol(4,WHITE);ar.addView(ai,new LayoutParams(dp(35),dp(38)));LinearLayout at=col();LayoutParams ap=new LayoutParams(0,-2,1);ap.leftMargin=dp(14);ar.addView(at,ap);add(at,text("AI Battery Assistant",18,WHITE,false),-2);space(at,6);TextView info=text("See live battery insights and charging status.",14,MUTED,false);assistantSummary=info;info.setLineSpacing(dp(3),1);add(at,info,-2);ar.addView(chevron(),new LayoutParams(dp(20),dp(32)));add(assistant,ar,-2);tap(ar,this::showInsights);add(body,assistant,-2);
        space(body,26);section(body,"Battery usage");
        LinearLayout usage=card();LinearLayout tabs=row();TextView[] rangeLabels=new TextView[2];String[] ranges={"Today","Last 7 days"};
        for(int i=0;i<2;i++){final int ix=i;TextView t=text(ranges[i],14,WHITE,false);t.setGravity(Gravity.CENTER);rangeLabels[i]=t;LayoutParams lp=new LayoutParams(0,dp(40),1);if(i>0)lp.leftMargin=dp(5);tabs.addView(t,lp);tapGlass(t,15,()->openUsage(ix));}
        FrameLayout tabHost=new FrameLayout(a);usageRail=new GlassRail(a,rangeLabels,()->glassStyle,()->quietMode,this::duration,true);tabHost.addView(usageRail,new FrameLayout.LayoutParams(-1,-1));tabHost.addView(tabs,new FrameLayout.LayoutParams(-1,-1));usageRail.settle(0);add(usage,tabHost,40);space(usage,16);add(usage,text("View real battery usage in Samsung's graphs",14,MUTED,false),-2);space(usage,14);item(usage,"Open original battery statistics",null,original);add(body,usage,-2);
        space(body,22);section(body,"Charging mode");addChargingModes(body);space(body,22);monitorSection=col();section(monitorSection,"Live battery monitor");LinearLayout monitor=card();
        LinearLayout metrics=row();liveTemp=metric(metrics,"Temperature");liveVoltage=metric(metrics,"Voltage");add(monitor,metrics,-2);space(monitor,16);line(monitor);space(monitor,16);
        LinearLayout metrics2=row();liveCurrent=metric(metrics2,"Battery current");liveCharge=metric(metrics2,"Charge stored");add(monitor,metrics2,-2);space(monitor,18);
        chargeNeeded=text("Charge needed to 100%: —",13,MUTED,false);add(monitor,chargeNeeded,-2);space(monitor,12);
        liveSession=text("Connect a charger to start monitoring",13,WHITE,false);add(monitor,liveSession,-2);space(monitor,12);
        powerGraph=new PowerGraph();add(monitor,powerGraph,84);space(monitor,8);liveSource=text("Measured battery power · current page only",11,MUTED,false);add(monitor,liveSource,-2);space(monitor,16);
        item(monitor,"Detailed battery diagnostics",null,this::chargingInfo);add(monitorSection,monitor,-2);add(body,monitorSection,-2);monitorSection.setVisibility(showMonitor?VISIBLE:GONE);
        space(body,22);section(body,"Custom features");LinearLayout custom=card();item(custom,"Customize appearance","Glass strength, animation speed and monitor visibility",this::customize);space(custom,12);line(custom);space(custom,12);thermalNote=text(thermalLimit==0?"Disabled · only while this page is open":"Warn at "+thermalLimit+" °C · while this page is open",13,MUTED,false);item(custom,"Battery temperature alert",null,this::chooseThermalAlert);space(custom,5);add(custom,thermalNote,-2);space(custom,12);line(custom);space(custom,12);item(custom,"Charge target reminder","Optional alert while this page is open",this::chooseChargeReminder);reminderNote=text(reminderLabel(),13,MUTED,false);add(custom,reminderNote,-2);space(custom,12);line(custom);space(custom,12);item(custom,"Compatibility report","Device, software and available controls",this::compatibility);add(body,custom,-2);
        space(body,22);section(body,"Power & performance");LinearLayout power=card();
        nativeItem(power,"Power saving settings","com.samsung.android.sm.powermode.ui.PowerModeSettingsActivity");
        nativeItem(power,"Performance profile","com.samsung.android.sm.battery.ui.setting.PerformanceOptimizationActivity");
        nativeItem(power,"Enhanced processing","com.samsung.android.sm.battery.ui.setting.EnhancedProcessingActivity");
        nativeItem(power,"Adaptive power saving","com.samsung.android.sm.powermode.ui.AdaptivePowerSavingActivity");if(power.getChildCount()>0)add(body,power,-2);
        space(body,18);section(body,"Charging & protection");LinearLayout charging=card();
        nativeItem(charging,"Charging settings","com.samsung.android.sm.battery.ui.BatteryAdvancedMenuActivity");
        nativeItem(charging,"Battery protection","com.samsung.android.sm.battery.ui.protection.BatteryProtectionActivity");
        nativeItem(charging,"Fast wireless charging","com.samsung.android.sm.battery.ui.fastwirelesscharging.FastWirelessActivity");
        nativeItem(charging,"Wireless power sharing","com.samsung.android.sm.powershare.ui.PowerShareActivity");if(charging.getChildCount()>0)add(body,charging,-2);
        space(body,18);section(body,"App battery management");LinearLayout apps=card();
        nativeItem(apps,"Background usage limits","com.samsung.android.sm.battery.ui.setting.AppPowerManagementActivity");
        nativeItem(apps,"App battery restrictions","com.samsung.android.sm.battery.ui.setting.AppRestrictionActivity");if(apps.getChildCount()>0)add(body,apps,-2);
        space(body,18);LinearLayout stockCard=card();item(stockCard,"Original Samsung Battery","All native battery features",original);add(body,stockCard,-2);
        space(body,27);TextView credit=text("Made by Muneeb",14,0xffb6b6b6,false);credit.setGravity(Gravity.CENTER);credit.setIncludeFontPadding(true);add(body,credit,-2);
        space(body,5);TextView edition=text("ColourOS Battery  ·  v1.1 RC7",11,0xff777777,false);edition.setGravity(Gravity.CENTER);add(body,edition,-2);space(body,14);
        ready=true;refresh();
        MotionUi.enter(header,duration(240),quietMode,0,dp(8));MotionUi.enter(scroll,duration(340),quietMode,0,dp(20));
    }
    private TextView chevron(){TextView t=text("›",27,0xff696969,false);t.setGravity(Gravity.CENTER);return t;}
    private void item(LinearLayout parent,String name,String desc,Runnable action){LinearLayout r=row();LinearLayout t=col();add(t,text(name,18,WHITE,false),-2);if(desc!=null){space(t,5);TextView d=text(desc,14,MUTED,false);d.setLineSpacing(dp(3),1);add(t,d,-2);}r.addView(t,new LayoutParams(0,-2,1));r.addView(chevron(),new LayoutParams(dp(22),dp(36)));add(parent,r,-2);tap(r,action);}
    private void nativeItem(LinearLayout parent,String label,String cls){
        try{android.content.pm.ActivityInfo info=a.getPackageManager().getActivityInfo(new android.content.ComponentName(a.getPackageName(),cls),0);if(!info.enabled||!info.applicationInfo.enabled)return;}catch(Throwable t){return;}
        if(parent.getChildCount()>0)line(parent);LinearLayout r=row();r.setMinimumHeight(dp(52));r.setPadding(0,dp(9),0,dp(9));
        TextView title=text(label,15,WHITE,false);title.setIncludeFontPadding(true);r.addView(title,new LayoutParams(0,-2,1));r.addView(chevron(),new LayoutParams(dp(22),dp(36)));add(parent,r,-2);tap(r,()->openNative(cls));
    }
    private void showInsights(){
        String mode=selected==0?"Balanced":selected==1?"Power saving":selected==2?"High performance":"Unknown";
        String estimate=selected>=0&&selected<3?estimates[selected]:"Unavailable";
        String advice=pluggedIn?"Battery protection can reduce time spent at full charge.":"Use Power saving or review background activity to reduce drain.";
        showPanel(new AlertDialog.Builder(a).setTitle("Battery insights").setMessage("Battery: "+(currentPercent<0?"Unavailable":currentPercent+"%")+"\nMode: "+mode+"\nEstimated time: "+estimate+"\n\n"+advice+"\n\nLocal readings and rule-based tips, not AI-generated analysis.").setNegativeButton("Close",null).setPositiveButton("Samsung usage statistics",(d,w)->original.run()));
    }
    private void openUsage(int ix){if(usageOpening||leaving)return;usageOpening=true;usageRange=ix;usageRail.settle(ix);postDelayed(()->{usageOpening=false;if(isAttachedToWindow()&&hasWindowFocus()&&!leaving)openNative(ix==0?"com.samsung.android.sm.battery.ui.graph.LastChargeGraphActivity":"com.samsung.android.sm.battery.ui.graph.DailyUsageGraphActivity");},MotionUi.enabled()&&!quietMode?Math.min(175,duration(140)):0);}
    private boolean manipulating(){return healthScreen!=null||modeDragging||chargingDragging||(protectionCard!=null&&protectionCard.interacting());}
    private void openNative(String cls){try{Intent i=new Intent().setClassName(a.getPackageName(),cls);a.startActivity(i);}catch(Throwable t){BatteryHook.log("native screen unavailable: "+cls+": "+t);original.run();Toast.makeText(a,"Use the original Samsung settings.",Toast.LENGTH_SHORT).show();}}
    private void dialog(String title,String msg){showPanel(new AlertDialog.Builder(a).setTitle(title).setMessage(msg).setPositiveButton(android.R.string.ok,null));}
    private void health(){
        if(healthScreen!=null||leaving||!(getParent() instanceof FrameLayout))return;
        FrameLayout host=(FrameLayout)getParent();
        MotionUi.restore(dashboardHeader);MotionUi.restore(dashboardScroll);
        handler.removeCallbacks(tick);handler.removeCallbacks(powerTick);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        healthScreen=new HealthScreen(a,host,()->openNative("com.samsung.android.sm.battery.ui.protection.BatteryProtectionActivity"),duration(230),quietMode,()->{
            healthScreen=null;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            if(isAttachedToWindow()&&hasWindowFocus()&&!a.isFinishing()&&!a.isDestroyed()){
                ensureBars();handler.removeCallbacks(tick);handler.post(tick);handler.removeCallbacks(powerTick);if(pluggedIn)handler.post(powerTick);
            }
        });
        try{healthScreen.show();}catch(Throwable t){if(healthScreen!=null)healthScreen.closeNow();healthScreen=null;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);BatteryHook.log("Health page unavailable: "+t);Toast.makeText(a,"Battery health page unavailable",Toast.LENGTH_SHORT).show();}
    }
    boolean handleHealthBack(){if(healthScreen==null)return false;healthScreen.back();return true;}
    private void leaveDashboard(){
        if(leaving)return;leaving=true;handler.removeCallbacks(tick);handler.removeCallbacks(powerTick);
        MotionUi.exit(dashboardHeader,duration(200),quietMode,dp(12),0,null);
        MotionUi.exit(dashboardScroll,duration(230),quietMode,dp(20),0,()->{if(!a.isFinishing()&&!a.isDestroyed())a.finish();});
    }
    private AlertDialog showPanel(AlertDialog.Builder builder){AlertDialog d=builder.create();d.show();MotionUi.panel(d,duration(230),quietMode);return d;}
    void refresh(){if(manipulating())return;try{battery=a.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));int percent=-1;boolean plugged=false;String state="Battery status unavailable";if(battery!=null){int l=battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),s=battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1);if(l>=0&&s>0&&l<=s)percent=Math.round(100f*l/s);int status=battery.getIntExtra(BatteryManager.EXTRA_STATUS,0);int plug=battery.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);plugged=plug!=0;switch(status){case BatteryManager.BATTERY_STATUS_FULL:state=plugged?"⚡ Fully charged":"Fully charged";break;case BatteryManager.BATTERY_STATUS_CHARGING:state=plug==BatteryManager.BATTERY_PLUGGED_WIRELESS?"⚡ Wireless charging":"⚡ Charging";try{BatteryManager bm=(BatteryManager)a.getSystemService(Context.BATTERY_SERVICE);long ms=bm.computeChargeTimeRemaining();if(ms>0){long min=Math.max(1,(ms+59999)/60000);state+=" | "+(min/60>0?min/60+" h ":"")+(min%60)+" min until full";}}catch(Throwable ignore){}break;case BatteryManager.BATTERY_STATUS_DISCHARGING:state="Not charging";break;case BatteryManager.BATTERY_STATUS_NOT_CHARGING:state="Not charging";break;}}
       boolean plugChanged=pluggedIn!=plugged;currentPercent=percent;pluggedIn=plugged;if(!plugged)reminderFired=false;if(plugged&&chargeReminder>0&&percent>=chargeReminder&&!reminderFired&&hasWindowFocus()){reminderFired=true;Toast.makeText(a,"Charge target reached: "+percent+"%",Toast.LENGTH_LONG).show();}number.setText(percent<0?"—":String.valueOf(percent));bar.setLevel(percent,plugged);
       if(plugged){updateChargingHeader();if(plugChanged){handler.removeCallbacks(powerTick);if(hasWindowFocus())handler.postDelayed(powerTick,quietMode?5000:2000);}}else{handler.removeCallbacks(powerTick);updateMonitor(telemetry.read());chargeDetail.setText("");charge.setTextColor(MUTED);charge.setTypeface(Typeface.create(deviceTypeface,400,false));String e=selected>=0&&selected<3?estimates[selected]:"";charge.setText(e.contains("min")?"Can last about "+e:state);}
       if(ready){showMode(engine.activeMode());updateEstimates();if(protectionCard!=null)protectionCard.refresh();}
       }catch(Throwable t){BatteryHook.log("battery refresh: "+t);}}
    private void updateChargingHeader(){
        if(!pluggedIn||manipulating())return;
        ChargeTelemetry.Reading r=telemetry.read();
        if(!r.plugged)return;
        charge.setTextColor(WHITE);charge.setTypeface(Typeface.create(deviceTypeface,650,true));
        charge.setText("ϟ "+(r.full?"Fully charged":r.discharging?"Connected":!showWatts||r.wattsText==null?(r.charging?"Charging":"Connected"):(r.estimated?"≈ ":"")+r.wattsText));
        String detail=r.uncertain?"Current magnitude":r.discharging?"Battery discharging":r.wattsText==null?(r.charging?"Measuring power…":"Charging paused"):(r.wireless?"Wireless · ":"")+(r.estimated?"Estimated battery power":"Battery power");
        if(r.charging&&r.minutes>0&&!r.uncertain)detail+=" · "+(r.minutes/60>0?r.minutes/60+" h ":"")+r.minutes%60+" min";
        chargeDetail.setText(showWatts?detail:r.charging&&r.minutes>0?(r.minutes/60)+" h "+(r.minutes%60)+" min until full":r.charging?"Charging":r.full?"Charge complete":"Connected");
        updateMonitor(r);
    }
    private TextView metric(LinearLayout parent,String label){
        LinearLayout c=col();c.setPadding(0,0,dp(4),0);add(c,text(label,12,MUTED,false),-2);space(c,5);TextView val=text("—",20,WHITE,false);val.setMaxLines(1);val.setAutoSizeTextTypeUniformWithConfiguration(12,20,1,TypedValue.COMPLEX_UNIT_SP);c.addView(val,new LayoutParams(-1,dp(26)));parent.addView(c,new LayoutParams(0,-2,1));return val;
    }
    private String voltage(ChargeTelemetry.Reading r){return r.mv>0?String.format(Locale.US,"%.2f V",r.mv/1000f):"—";}
    private String temperature(ChargeTelemetry.Reading r){return r.temp!=Integer.MIN_VALUE?String.format(Locale.US,"%.1f °C",r.temp/10f):"—";}
    private String current(int v){return v!=Integer.MIN_VALUE&&Math.abs((long)v)<=15000000L?String.format(Locale.US,"%+.2f A",v/1000000f):"—";}
    private String capacity(ChargeTelemetry.Reading r){return r.counter>0?String.format(Locale.US,"%.0f mAh",r.counter/1000f):"—";}
    private void updateMonitor(ChargeTelemetry.Reading r){
        lastReading=r;if(liveTemp==null)return;
        refreshChargingPolicy();
        liveTemp.setText(temperature(r));liveVoltage.setText(voltage(r));liveCurrent.setText(Double.isFinite(r.effectiveUa)?String.format(Locale.US,"%s%.2f A",r.estimated?"≈ ":"",r.effectiveUa/1000000d):Math.abs((long)r.ua)>=50000&&r.ua!=Integer.MIN_VALUE?current(r.ua):"—");liveCharge.setText(capacity(r));
        if(chargeNeeded!=null){double full=r.percent>=10&&r.counter>0?r.counter*100d/r.percent:Double.NaN;chargeNeeded.setText(Double.isFinite(full)&&full>=1000000&&full<=20000000?"Charge needed to 100%: ≈ "+String.format(Locale.US,"%.0f mAh",Math.max(0,full-r.counter)/1000):"Charge needed to 100%: unavailable");}
        if(thermalLimit>0&&r.temp!=Integer.MIN_VALUE){boolean hot=r.temp>=thermalLimit*10;if(hot&&!thermalWarned&&hasWindowFocus()){thermalWarned=true;Toast.makeText(a,"Battery temperature reached "+temperature(r)+". Charging may slow down.",Toast.LENGTH_LONG).show();}else if(!hot&&r.temp<(thermalLimit-1)*10)thermalWarned=false;}
        if(!showMonitor)return;
        liveSession.setText(r.plugged?"Monitoring "+r.visibleSeconds/60+" min "+r.visibleSeconds%60+" s"+(r.gained>=0?" · +"+r.gained+"%":"")+(Double.isNaN(r.peak)?"":" · peak "+String.format(Locale.US,"%.1f W",r.peak)):"Connect a charger to start monitoring");
        liveSource.setText(r.plugged?r.source+" · recent measured samples":"Battery-side power · no active session");
        powerGraph.update(r.history);
    }
    private final class PowerGraph extends View {
        private final Paint p=new Paint(3);private final Path path=new Path();private double[] data=new double[0];
        PowerGraph(){super(a);setContentDescription("Recent measured battery power graph");}
        void update(double[] values){data=values;invalidate();}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight(),pad=dp(3);p.setShader(null);p.setStrokeWidth(dp(1));p.setColor(0xff343434);for(int k=0;k<4;k++){float y=pad+(h-2*pad)*k/3;c.drawLine(0,y,w,y,p);}
            if(data.length<2){p.setColor(MUTED);p.setTypeface(deviceTypeface);p.setTextSize(dp(12));p.setTextAlign(Paint.Align.CENTER);c.drawText("Collecting measured samples",w/2,h/2,p);return;}
            double max=1;for(double value:data)max=Math.max(max,value);path.reset();
            for(int i=0;i<data.length;i++){float x=pad+(w-2*pad)*i/(data.length-1),y=h-pad-(float)(data[i]/max)*(h-2*pad);if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}
            Path fill=new Path(path);fill.lineTo(w-pad,h);fill.lineTo(pad,h);fill.close();p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,0,0,h,0x55168cf4,0x00168cf4,Shader.TileMode.CLAMP));c.drawPath(fill,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(BLUE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        }
    }
    private void chooseThermalAlert(){
        int[] values={0,40,42,45};String[] names={"Disabled","40 °C","42 °C","45 °C"};int checked=0;for(int i=0;i<values.length;i++)if(values[i]==thermalLimit)checked=i;
        showPanel(new AlertDialog.Builder(a).setTitle("Battery temperature alert").setSingleChoiceItems(names,checked,(d,w)->{thermalLimit=values[w];thermalWarned=false;a.getSharedPreferences("colour_battery_options",0).edit().putInt("thermal_alert",thermalLimit).apply();thermalNote.setText(thermalLimit==0?"Disabled · only while this page is open":"Warn at "+thermalLimit+" °C · while this page is open");d.dismiss();}).setNegativeButton("Cancel",null));
    }
    private int duration(int ms){return quietMode?Math.max(1,ms/2):animationStyle==0?Math.max(1,ms/2):animationStyle==2?Math.round(ms*1.45f):ms;}
    private String reminderLabel(){return chargeReminder==0?"Disabled":"Alert at "+chargeReminder+"% · page only";}
    private void chooseChargeReminder(){int[] values={0,70,80,85,90,95,100};String[] names={"Disabled","70%","80%","85%","90%","95%","100%"};int ix=0;for(int k=0;k<values.length;k++)if(values[k]==chargeReminder)ix=k;showPanel(new AlertDialog.Builder(a).setTitle("Charge target reminder").setSingleChoiceItems(names,ix,(d,w)->{chargeReminder=values[w];reminderFired=false;a.getSharedPreferences("colour_battery_options",0).edit().putInt("charge_reminder",chargeReminder).apply();reminderNote.setText(reminderLabel());d.dismiss();refresh();}).setNegativeButton("Cancel",null));}
    private void compatibility(){dialog("Compatibility report","Device: "+android.os.Build.MODEL+"\nAndroid: "+android.os.Build.VERSION.RELEASE+" (API "+android.os.Build.VERSION.SDK_INT+")\nModule: 1.1 RC7\nPerformance controls: "+(engine.supportsLight()?"Samsung helper available":"Native settings fallback")+"\nCharging controls: "+(chargingPolicy.canApply()?"Inline available":"Native settings fallback")+"\nGlass rendering: "+(android.os.Build.VERSION.SDK_INT>=33?"ColorOS-style glass renderer (hardware Canvas)":"ColorOS-style glass renderer")+"\n\nNo global SystemUI hooks or charging firmware modifications.");}
    private void customize(){
        LinearLayout p=col();p.setPadding(dp(20),dp(8),dp(20),dp(8));add(p,text("Liquid glass strength",15,WHITE,false),-2);
        android.widget.RadioGroup g=new android.widget.RadioGroup(a);String[] styles={"Subtle charcoal","ColorOS glass","Deep silver glass"};for(int k=0;k<3;k++){android.widget.RadioButton b=new android.widget.RadioButton(a);b.setId(100+k);b.setText(styles[k]);g.addView(b);if(k==glassStyle)g.check(b.getId());}add(p,g,-2);
        add(p,text("Animation speed",15,WHITE,false),-2);android.widget.RadioGroup speeds=new android.widget.RadioGroup(a);String[] speedNames={"Fast","Smooth","Slow"};for(int k=0;k<3;k++){android.widget.RadioButton b=new android.widget.RadioButton(a);b.setId(200+k);b.setText(speedNames[k]);speeds.addView(b);if(k==animationStyle)speeds.check(b.getId());}add(p,speeds,-2);
        Switch show=new Switch(a);show.setText("Show live battery monitor");show.setChecked(showMonitor);add(p,show,-2);
        Switch watts=new Switch(a);watts.setText("Show charging wattage");watts.setChecked(showWatts);add(p,watts,-2);
        Switch quiet=new Switch(a);quiet.setText("Low power UI (less polling and effects)");quiet.setChecked(quietMode);add(p,quiet,-2);
        Switch vibration=new Switch(a);vibration.setText("Selection haptics");vibration.setChecked(haptics);add(p,vibration,-2);
        ScrollView popupScroll=new ScrollView(a);popupScroll.addView(p);
        showPanel(new AlertDialog.Builder(a).setTitle("Customize appearance").setView(popupScroll).setNegativeButton("Cancel",null).setNeutralButton("Reset",(d,w)->applyOptions(1,1,true,true,false,true)).setPositiveButton("Apply",(d,w)->applyOptions(Math.max(0,g.getCheckedRadioButtonId()-100),Math.max(0,speeds.getCheckedRadioButtonId()-200),show.isChecked(),watts.isChecked(),quiet.isChecked(),vibration.isChecked())));
    }
    private void applyOptions(int g,int speed,boolean monitor,boolean watts,boolean quiet,boolean vibration){glassStyle=ReleaseRules.clampOption(g);animationStyle=ReleaseRules.clampOption(speed);showMonitor=monitor;showWatts=watts;quietMode=quiet;haptics=vibration;a.getSharedPreferences("colour_battery_options",0).edit().putInt("glass_style",glassStyle).putInt("animation_style",animationStyle).putBoolean("show_monitor",showMonitor).putBoolean("show_watts",showWatts).putBoolean("quiet_mode",quietMode).putBoolean("haptics",haptics).apply();if(lens!=null)lens.invalidate();if(chargingLens!=null)chargingLens.invalidate();if(usageRail!=null)usageRail.invalidate();if(protectionCard!=null)protectionCard.appearanceChanged();if(monitorSection!=null)monitorSection.setVisibility(showMonitor?VISIBLE:GONE);refresh();}
    private void addChargingModes(LinearLayout body){
        LinearLayout c=card(),r=row();chargingRow=r;String[] titles={"Standard\ncharging","Fast & super fast\ncharging"};String[] desc={"Normal speed","Automatically negotiated"};
        for(int i=0;i<2;i++){final int mode=i;LinearLayout tile=col();tile.setGravity(Gravity.CENTER);tile.setPadding(dp(6),dp(9),dp(6),dp(9));chargingTiles[i]=tile;TextView z=text(i==0?"ϟ":"ϟϟ",23,WHITE,true);z.setGravity(Gravity.CENTER);add(tile,z,28);TextView title=text(titles[i],13,WHITE,false);title.setGravity(Gravity.CENTER);title.setMaxLines(3);chargingTitles[i]=title;add(tile,title,48);TextView sub=text(desc[i],10,MUTED,false);sub.setGravity(Gravity.CENTER);sub.setMaxLines(2);add(tile,sub,30);LayoutParams lp=new LayoutParams(0,dp(119),1);if(i>0)lp.leftMargin=dp(10);r.addView(tile,lp);tapGlass(tile,22,()->selectChargingPolicy(mode));}
        FrameLayout host=new ChargingTouchHost();chargingLens=new ChargingLens();host.addView(chargingLens,new FrameLayout.LayoutParams(-1,-1));host.addView(r,new FrameLayout.LayoutParams(-1,dp(124)));add(c,host,124);space(c,12);chargePolicyNote=text("Reading Samsung charging setting",12,MUTED,false);add(c,chargePolicyNote,-2);space(c,14);item(c,"More charging settings",null,()->openNative("com.samsung.android.sm.battery.ui.BatteryAdvancedMenuActivity"));add(body,c,-2);refreshChargingPolicy();
    }
    private void refreshChargingPolicy(){
        if(chargingTiles[0]==null)return;int state=chargingPolicy.read();
        for(int i=0;i<2;i++){boolean active=state==i;chargingTiles[i].setSelected(active);chargingTitles[i].setTextColor(active?ColorOsPalette.ACCENT:WHITE);}if(chargingLens!=null&&!chargingDragging)chargingLens.settle(state);
        if(chargePolicyNote!=null)chargePolicyNote.setText(state<0||!chargingPolicy.canApply()?"Open native settings to configure charging":state==0?"Fast charging disabled":"Fast / Super fast enabled · charger, battery and temperature determine actual speed");
    }
    private void selectChargingPolicy(int mode){
        if(chargingPolicy.read()<0||!chargingPolicy.canApply()){refreshChargingPolicy();openNative("com.samsung.android.sm.battery.ui.BatteryAdvancedMenuActivity");return;}
        if(chargingPolicy.read()==mode){refreshChargingPolicy();return;}
        if(pluggedIn){refreshChargingPolicy();dialog("Charging mode","Unplug the charger before changing charging speed, then reconnect it.");return;}
        boolean ok=chargingPolicy.apply(mode);refreshChargingPolicy();if(!ok)Toast.makeText(a,"Samsung did not confirm the charging mode. Use original Charging settings.",Toast.LENGTH_LONG).show();else if(haptics){chargingTiles[mode].performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);}
    }
    private void chargingInfo(){
        ChargeTelemetry.Reading r=telemetry.read();
        dialog("Battery diagnostics", "Battery power: "+(r.wattsText==null?"Unavailable":r.wattsText)+"\nSource: "+r.source+"\nVoltage: "+voltage(r)+"\nRaw current: "+current(r.ua)+"\nAverage current: "+current(r.average)+"\nFuel gauge current: "+current(r.sysfsCurrent)+"\nCharge stored: "+capacity(r)+"\nTemperature: "+temperature(r)+"\nCycle count: "+(r.cycles<0?"Unavailable":r.cycles)+"\n\nBattery-side power is not charger output. A magnitude reading has unverified direction until the charge counter confirms polarity.");
    }
    // Only the hooked BatteryActivity window; no global status bar settings.
    void ensureBars(){if(isAttachedToWindow()&&hasWindowFocus())WindowBars.schedule(a.getWindow(),this);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus&&!leaving&&healthScreen==null){ensureBars();handler.removeCallbacks(tick);handler.post(tick);handler.removeCallbacks(powerTick);if(pluggedIn)handler.post(powerTick);}else{handler.removeCallbacks(powerTick);handler.removeCallbacks(tick);}}
    private void applySafeInsets(WindowInsets insets){WindowBars.pad(a.getWindow(),this,insets);}
    private void showMode(int which){
        int old=selected;selected=which;
        if(lens!=null&&!modeDragging&&!busy)lens.settle(which);
        boolean saving=engine.isSaving();
        syncingSwitch=true;superSwitch.setChecked(saving);syncingSwitch=false;
        for(int j=0;j<3;j++){
            boolean chosen=j==which;modes[j].setSelected(chosen);
            if(old!=which){modeLabels[j].setTextColor(chosen?ColorOsPalette.ACCENT:WHITE);modeIcons[j].color=chosen?ColorOsPalette.ACCENT:WHITE;modeIcons[j].invalidate();modeNotes[j].setTextColor(MUTED);}
        }
    }
    private void choose(int mode){
        if(busy||!isAttachedToWindow())return;
        if(mode==2&&!engine.supportsLight()){if(lens!=null)lens.settle(engine.activeMode());openNative("com.samsung.android.sm.battery.ui.setting.PerformanceOptimizationActivity");return;}
        if(mode==engine.activeMode()){if(lens!=null)lens.settle(mode);showMode(mode);return;}
        busy=true;superSwitch.setEnabled(false);
        if(lens!=null)lens.settle(mode);
        for(LinearLayout tile:modes)tile.setEnabled(false);
        engine.apply(mode,()->{
            if(!isAttachedToWindow())return;
            busy=false;superSwitch.setEnabled(true);for(LinearLayout tile:modes)tile.setEnabled(true);
            int confirmed=engine.activeMode();if(lens!=null)lens.settle(confirmed);refresh();
            if(confirmed!=mode){Toast.makeText(a,"Samsung did not confirm the requested mode.",Toast.LENGTH_SHORT).show();BatteryHook.log("mode not confirmed "+mode);}
        });
    }
    private void updateEstimates(){
        if(!isAttachedToWindow())return;
        engine.estimate((values)->{
            if(!isAttachedToWindow())return;
            estimates=values.clone();for(int j=0;j<3;j++)modeNotes[j].setText(values[j]);
            if(!pluggedIn&&selected>=0&&selected<3&&values[selected].contains("min"))charge.setText("Can last about "+values[selected]);
            if(assistantSummary!=null)assistantSummary.setText(pluggedIn?"Charging information and battery protection.":engine.isSaving()?"Power saving is active. Review your usage.":"Review your battery usage and power-saving options.");
            superNote.setText(values[1].contains("min")||values[1].contains(" h")?"Can last about "+values[1]:"Uses Samsung Power saving");
        });
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();try{IntentFilter filter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);if(android.os.Build.VERSION.SDK_INT>=33)a.registerReceiver(receiver,filter,Context.RECEIVER_EXPORTED);else a.registerReceiver(receiver,filter);registered=true;}catch(Throwable t){BatteryHook.log("receiver: "+t);}try{a.getContentResolver().registerContentObserver(Settings.Global.getUriFor("low_power"),false,modeObserver);observing=true;a.getContentResolver().registerContentObserver(Settings.Global.getUriFor("sem_low_heat_mode"),false,modeObserver);}catch(Throwable t){BatteryHook.log("observer: "+t);}handler.removeCallbacks(tick);if(hasWindowFocus())handler.post(tick);post(()->{ensureBars();applySafeInsets(getRootWindowInsets());requestApplyInsets();});}
    @Override protected void onDetachedFromWindow(){if(healthScreen!=null){healthScreen.closeNow();healthScreen=null;}MotionUi.restore(dashboardHeader);MotionUi.restore(dashboardScroll);handler.removeCallbacks(tick);handler.removeCallbacks(powerTick);if(observing){try{a.getContentResolver().unregisterContentObserver(modeObserver);}catch(Throwable ignored){}observing=false;}if(registered){try{a.unregisterReceiver(receiver);}catch(Throwable ignored){}registered=false;}if(lens!=null)lens.stop();if(chargingLens!=null)chargingLens.stop();if(usageRail!=null)usageRail.stop();usageOpening=false;if(protectionCard!=null)protectionCard.dispose();bar.stop();engine.close();super.onDetachedFromWindow();}
    private static int blend(int a,int b,float f){return android.graphics.Color.rgb(Math.round(Color.red(a)+(Color.red(b)-Color.red(a))*f),Math.round(Color.green(a)+(Color.green(b)-Color.green(a))*f),Math.round(Color.blue(a)+(Color.blue(b)-Color.blue(a))*f));}
    private final class ModeTouchHost extends FrameLayout {
        private float startX,startY,startSlot;
        private boolean eligible,dragging;
        private final int slop=android.view.ViewConfiguration.get(a).getScaledTouchSlop();
        ModeTouchHost(){super(a);}
        private float step(){return modes[1].getLeft()-modes[0].getLeft();}
        @Override public boolean onInterceptTouchEvent(android.view.MotionEvent e){
            switch(e.getActionMasked()){
                case android.view.MotionEvent.ACTION_DOWN:
                    startX=e.getX();startY=e.getY();startSlot=lens==null?selected:lens.slot;dragging=false;
                    eligible=!busy&&selected>=0&&selected<3&&startSlot>=0&&Math.abs(step())>0;return false;
                case android.view.MotionEvent.ACTION_MOVE:
                    if(eligible&&(Math.abs(e.getY()-startY)>slop&&Math.abs(e.getY()-startY)>Math.abs(e.getX()-startX))){eligible=false;return false;}
                    if(eligible&&Math.abs(e.getX()-startX)>slop&&Math.abs(e.getX()-startX)>Math.abs(e.getY()-startY)*1.2f){
                        dragging=true;modeDragging=true;getParent().requestDisallowInterceptTouchEvent(true);lens.beginDrag();lens.dragTo(startSlot+(e.getX()-startX)/step());return true;
                    }return false;
                default:return false;
            }
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e){
            if(!dragging)return super.onTouchEvent(e);
            switch(e.getActionMasked()){
                case android.view.MotionEvent.ACTION_MOVE: if(Math.abs(step())>0)lens.dragTo(startSlot+(e.getX()-startX)/step());return true;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    boolean cancel=e.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL;
                    if(!cancel&&Math.abs(step())>0)lens.dragTo(startSlot+(e.getX()-startX)/step());int next=cancel?engine.activeMode():Math.max(0,Math.min(2,Math.round(lens.slot)));
                    dragging=false;modeDragging=false;eligible=false;getParent().requestDisallowInterceptTouchEvent(false);
                    if(!cancel&&next!=engine.activeMode()){if(haptics)performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);choose(next);}else{lens.settle(engine.activeMode());}
                    return true;
                default:return true;
            }
        }
    }
    private final class ChargingTouchHost extends FrameLayout {
        float x,y,start;boolean eligible,drag;final int slop=android.view.ViewConfiguration.get(a).getScaledTouchSlop();
        ChargingTouchHost(){super(a);}
        float step(){return chargingTiles[1].getLeft()-chargingTiles[0].getLeft();}
        @Override public boolean onInterceptTouchEvent(android.view.MotionEvent e){
            switch(e.getActionMasked()){
                case 0:x=e.getX();y=e.getY();start=chargingLens==null?-1:chargingLens.slot;eligible=start>=0&&Math.abs(step())>0;drag=false;return false;
                case 2:float dx=e.getX()-x,dy=e.getY()-y;if(Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx)){eligible=false;return false;}if(eligible&&Math.abs(dx)>slop&&Math.abs(dx)>Math.abs(dy)*1.2f){drag=true;chargingDragging=true;getParent().requestDisallowInterceptTouchEvent(true);chargingLens.beginDrag();chargingLens.preview(start+dx/step());return true;}return false;
                default:return false;
            }
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e){if(!drag)return super.onTouchEvent(e);switch(e.getActionMasked()){
            case 2:if(Math.abs(step())>0)chargingLens.preview(start+(e.getX()-x)/step());return true;
            case 1:case 3:boolean cancel=e.getActionMasked()==3;if(!cancel&&Math.abs(step())>0)chargingLens.preview(start+(e.getX()-x)/step());int target=Math.round(chargingLens.slot);drag=false;chargingDragging=false;getParent().requestDisallowInterceptTouchEvent(false);if(cancel)refreshChargingPolicy();else selectChargingPolicy(target);return true;
            default:return true;
        }}
    }
    private final class ChargingLens extends GlassRail {ChargingLens(){super(Dashboard.this.a,chargingTiles,()->glassStyle,()->quietMode,Dashboard.this::duration,false);}}
    private final class SelectionLens extends GlassRail {SelectionLens(){super(Dashboard.this.a,modes,()->glassStyle,()->quietMode,Dashboard.this::duration,false);}void dragTo(float value){preview(value);}}
    private final class Gauge extends View {
        private final Paint p=new Paint(3);private final Path clip=new Path();private final RectF bounds=new RectF();
        private float level=0,wanted=-1,front=1;private int base=0xff17db26,targetColor=base;private boolean known=false,connected;
        private ValueAnimator motion,transition;
        Gauge(){super(a);}
        void stop(){if(motion!=null)motion.cancel();if(transition!=null)transition.cancel();}
        void setLevel(int value,boolean plug){
            float target=Math.max(0,value)/100f;int color=plug?BLUE:0xff17db26;
            boolean changed=known&&connected!=plug;boolean first=!known;known=true;connected=plug;
            if(wanted!=target){wanted=target;if(motion!=null)motion.cancel();final float initial=level;motion=ValueAnimator.ofFloat(initial,target);motion.setDuration(duration(first?360:240));motion.setInterpolator(new PathInterpolator(.18f,.75f,.2f,1f));motion.addUpdateListener(v->{level=(float)v.getAnimatedValue();invalidate();});motion.start();}
            if(first){base=targetColor=color;front=1;invalidate();}
            else if(changed){if(transition!=null)transition.cancel();base=targetColor;targetColor=color;front=0;transition=ValueAnimator.ofFloat(0f,1f);transition.setDuration(duration(plug?620:440));transition.setInterpolator(new PathInterpolator(.12f,.75f,.18f,1f));transition.addUpdateListener(v->{front=(float)v.getAnimatedValue();invalidate();});transition.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator an){base=targetColor;front=1;invalidate();}});transition.start();}
            setContentDescription(value<0?"Battery level unavailable":"Battery level "+value+" percent"+(plug?", connected to power":""));
        }
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight(),r=h*.36f;p.setShader(null);p.setColor(TILE);c.drawRoundRect(0,0,w,h,r,r,p);if(level<=0)return;
            float fill=w*level;bounds.set(0,0,fill,h);clip.reset();clip.addRoundRect(bounds,r,r,Path.Direction.CW);c.save();c.clipPath(clip);p.setColor(base);c.drawRect(0,0,fill,h,p);
            if(front<.999f){float edge=(fill+h)*front-h*.5f,soft=Math.max(1,h*.34f);p.setShader(new LinearGradient(edge-soft,0,edge+soft,0,new int[]{targetColor,blend(targetColor,0xffa8eaff,.42f),base},new float[]{0,.60f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,fill,h,p);p.setShader(null);}else{p.setColor(targetColor);c.drawRect(0,0,fill,h,p);}
            c.restore();
        }
    }
    private final class Symbol extends View{final int kind;int color;final Paint p=new Paint(3);Symbol(int k,int tint){super(a);kind=k;color=tint;}
       @Override protected void onDraw(Canvas c){super.onDraw(c);float scale=Math.min(getWidth(),getHeight())/48f;c.save();c.translate((getWidth()-48*scale)/2,(getHeight()-48*scale)/2);c.scale(scale,scale);p.setColor(color);p.setStrokeWidth(3);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);if(kind==0){c.drawLine(12,24,35,24,p);c.drawLine(12,24,23,13,p);c.drawLine(12,24,23,35,p);}else if(kind==1||kind==2||kind==4){c.drawRoundRect(6,12,39,36,5,5,p);c.drawLine(43,19,43,29,p);p.setStyle(Paint.Style.FILL);if(kind==1)c.drawRoundRect(10,16,23,32,2,2,p);else if(kind==2){c.drawOval(13,17,32,28,p);p.setColor(TILE);p.setStrokeWidth(2);c.drawLine(13,31,27,21,p);}else{android.graphics.Path s=new android.graphics.Path();s.moveTo(23,11);s.lineTo(27,20);s.lineTo(36,24);s.lineTo(27,28);s.lineTo(23,37);s.lineTo(19,28);s.lineTo(10,24);s.lineTo(19,20);s.close();c.drawPath(s,p);}}else{c.drawArc(7,7,41,41,145,250,false,p);c.drawLine(24,24,32,13,p);p.setStyle(Paint.Style.FILL);c.drawCircle(24,24,3,p);}c.restore();}
    }
}
