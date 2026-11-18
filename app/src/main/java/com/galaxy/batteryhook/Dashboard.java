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
    private final Runnable powerTick=new Runnable(){public void run(){if(isAttachedToWindow()&&isShown()&&hasWindowFocus()&&pluggedIn){updateChargingHeader();handler.postDelayed(this,1500);}}};
    private String[] estimates={"Learning…","Learning…","Learning…"};
    private boolean pluggedIn;
    private int currentPercent=-1;
    private LinearLayout tileRow;
    private ValueAnimator slideMotion;
    private SelectionLens lens;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Glass[] glass=new Glass[3];
    private final ValueAnimator[] tileAnimations=new ValueAnimator[3];
    private final NativeModes engine;
    private Switch superSwitch;
    private TextView superNote;
    private int selected=-2;
    private boolean syncingSwitch, busy, ready;
    private long queryGeneration;
    private final Runnable tick=new Runnable(){public void run(){if(isAttachedToWindow()){refresh();handler.postDelayed(this,15000);}}};
    private final ContentObserver modeObserver=new ContentObserver(handler){@Override public void onChange(boolean self){refresh();}};
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
    private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){refresh();}};
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
    private void tap(View v,Runnable action){v.setClickable(true);v.setFocusable(true);v.setOnClickListener(w->{try{action.run();}catch(Throwable e){BatteryHook.log("action failed: "+e);Toast.makeText(a,"This control is unavailable on this device.",Toast.LENGTH_SHORT).show();}});}
    Dashboard(Activity activity,Runnable restore){
        super(activity);a=activity;original=restore;
        uiContext=a;Typeface nativeFont=new TextView(a).getTypeface();deviceTypeface=nativeFont==null?Typeface.DEFAULT:nativeFont;engine=new NativeModes(a);telemetry=new ChargeTelemetry(a);chargingPolicy=new ChargingPolicy(a);thermalLimit=a.getSharedPreferences("colour_battery_options",0).getInt("thermal_alert",0);
        setOrientation(VERTICAL);setBackgroundColor(BG);setClickable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setOnApplyWindowInsetsListener((v,insets)->{applySafeInsets(insets);return insets;});
        LinearLayout header=row();header.setPadding(dp(19),dp(6),dp(19),dp(6));
        FrameLayout back=new FrameLayout(a);back.setBackground(shape(TILE,25));Symbol arrow=new Symbol(0,WHITE);back.addView(arrow,new FrameLayout.LayoutParams(-1,-1));back.setContentDescription("Back");tap(back,()->a.finish());header.addView(back,new LayoutParams(dp(40),dp(40)));
        TextView title=text("Battery",19,WHITE,false);LayoutParams tl=new LayoutParams(0,-2,1);tl.leftMargin=dp(12);header.addView(title,tl);
        TextView menu=text("⋮",28,MUTED,true);menu.setGravity(Gravity.CENTER);menu.setContentDescription("Original Samsung battery settings");header.addView(menu,new LayoutParams(dp(40),dp(44)));tap(menu,()->new AlertDialog.Builder(a).setTitle("Battery settings").setItems(new String[]{"Original Samsung Battery page","Samsung battery protection","Charging settings"},(d,w)->{if(w==0)original.run();else if(w==1)openNative("com.samsung.android.sm.battery.ui.protection.BatteryProtectionActivity");else openNative("com.samsung.android.sm.battery.ui.BatteryAdvancedMenuActivity");}).show());
        add(this,header,56);
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);addView(scroll,new LayoutParams(-1,0,1));
        LinearLayout body=col();body.setPadding(dp(18),dp(12),dp(18),dp(24));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        LinearLayout chargeRow=row();chargeRow.setGravity(Gravity.BOTTOM);
        LinearLayout amount=row();amount.setGravity(Gravity.BOTTOM);number=text("—",44,WHITE,true);amount.addView(number);TextView pct=text("%",16,WHITE,true);pct.setPadding(dp(4),0,0,dp(5));amount.addView(pct);chargeRow.addView(amount,new LayoutParams(-2,-2));LinearLayout chargingText=col();chargingText.setGravity(Gravity.END);chargingText.setPadding(dp(6),0,0,dp(4));charge=text("",12,WHITE,true);charge.setGravity(Gravity.END);charge.setSingleLine(true);charge.setAutoSizeTextTypeUniformWithConfiguration(9,12,1,TypedValue.COMPLEX_UNIT_SP);chargeDetail=text("",10,MUTED,false);chargeDetail.setGravity(Gravity.END);chargeDetail.setMaxLines(2);chargingText.addView(charge,new LayoutParams(-1,dp(19)));chargingText.addView(chargeDetail,new LayoutParams(-1,-2));chargeRow.addView(chargingText,new LayoutParams(0,-2,1));tap(chargingText,this::chargingInfo);add(body,chargeRow,-2);space(body,8);
        bar=new Gauge();add(body,bar,29);space(body,23);
        TextView instructions=text("Instructions ⓘ",14,MUTED,false);instructions.setPadding(dp(10),0,0,dp(2));add(body,instructions,-2);tap(instructions,()->dialog("Instructions","Balanced uses Samsung Light profile on supported phones; High performance uses Samsung Standard (no overclocking). Power saving and Super power saving control the same Samsung saver. Times are Samsung estimates or measured discharge estimates, not guarantees. Tap ⋮ for the original page."));space(body,9);
        LinearLayout main=card();add(body,main,-2);
        LinearLayout tiles=row();tileRow=tiles;String[] captions={"Balanced\nmode","Power saving\nmode","High performance\nmode"};
        for(int i=0;i<3;i++){
            final int index=i;LinearLayout tile=col();tile.setGravity(Gravity.CENTER_HORIZONTAL);tile.setPadding(dp(3),dp(12),dp(3),dp(10));glass[i]=new Glass();tile.setBackground(glass[i]);
            LayoutParams p=new LayoutParams(0,dp(122),1);if(i>0)p.leftMargin=dp(9);tiles.addView(tile,p);modes[i]=tile;
            Symbol symbol=new Symbol(i+1,WHITE);modeIcons[i]=symbol;tile.addView(symbol,new LayoutParams(dp(31),dp(30)));space(tile,5);
            TextView label=text(captions[i],12.5f,WHITE,false);label.setGravity(Gravity.CENTER);label.setMaxLines(3);label.setBreakStrategy(0);label.setHyphenationFrequency(0);label.setAutoSizeTextTypeUniformWithConfiguration(10,13,1,TypedValue.COMPLEX_UNIT_SP);modeLabels[i]=label;tile.addView(label,new LayoutParams(-1,0,1));
            TextView note=text("Estimating…",10.5f,MUTED,false);note.setGravity(Gravity.CENTER);note.setSingleLine(true);note.setAutoSizeTextTypeUniformWithConfiguration(9,11,1,TypedValue.COMPLEX_UNIT_SP);modeNotes[i]=note;add(tile,note,16);
            tap(tile,()->choose(index));
        }FrameLayout tileHost=new FrameLayout(a);tileHost.addView(tiles,new FrameLayout.LayoutParams(-1,dp(122)));lens=new SelectionLens();tileHost.addView(lens,new FrameLayout.LayoutParams(-1,-1));lens.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);add(main,tileHost,122);space(main,12);
        TextView modeExplanation=text("Balanced: Light · High performance: Standard",11,MUTED,false);modeExplanation.setGravity(Gravity.CENTER);add(main,modeExplanation,-2);space(main,12);
        LinearLayout superRow=row();LinearLayout superText=col();add(superText,text("Super power saving mode",18,WHITE,false),-2);space(superText,5);superNote=text("Uses Samsung Power saving",13,MUTED,false);add(superText,superNote,-2);superRow.addView(superText,new LayoutParams(0,-2,1));superSwitch=new Switch(uiContext);superSwitch.setShowText(false);superSwitch.setThumbTintList(ColorStateList.valueOf(WHITE));superSwitch.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xff43af54,0xff555555}));superSwitch.setContentDescription("Super power saving mode (Samsung Power saving)");superRow.addView(superSwitch,new LayoutParams(-2,-2));add(main,superRow,-2);superSwitch.setOnCheckedChangeListener((b,checked)->{if(!syncingSwitch)choose(checked?1:engine.normalMode());});tap(superText,()->{if(!busy)choose(engine.isSaving()?engine.normalMode():1);});space(main,15);line(main);space(main,14);
        item(main,"Adaptive battery","Intelligently extends battery life based on your usage patterns and activity.",()->openNative("com.samsung.android.sm.battery.ui.setting.AdaptiveBatteryMenuActivity"));space(main,22);
        item(main,"Battery health",null,this::health);
        space(body,18);
        LinearLayout assistant=card();LinearLayout ar=row();Symbol ai=new Symbol(4,WHITE);ar.addView(ai,new LayoutParams(dp(35),dp(38)));LinearLayout at=col();LayoutParams ap=new LayoutParams(0,-2,1);ap.leftMargin=dp(14);ar.addView(at,ap);add(at,text("AI Battery Assistant",18,WHITE,false),-2);space(at,6);TextView info=text("See live battery insights and charging status.",14,MUTED,false);assistantSummary=info;info.setLineSpacing(dp(3),1);add(at,info,-2);ar.addView(chevron(),new LayoutParams(dp(20),dp(32)));add(assistant,ar,-2);tap(ar,this::showInsights);add(body,assistant,-2);
        space(body,26);section(body,"Battery usage");
        LinearLayout usage=card();LinearLayout tabs=row();tabs.setBackground(shape(TILE,22));tabs.setPadding(dp(4),dp(4),dp(4),dp(4));String[] ranges={"Today","Last 7 days"};for(int i=0;i<2;i++){final int ix=i;TextView t=text(ranges[i],14,WHITE,false);t.setGravity(Gravity.CENTER);if(i==0)t.setBackground(shape(0xff555555,18));tabs.addView(t,new LayoutParams(0,dp(36),1));tap(t,()->openNative(ix==0?"com.samsung.android.sm.battery.ui.graph.LastChargeGraphActivity":"com.samsung.android.sm.battery.ui.graph.DailyUsageGraphActivity"));}add(usage,tabs,-2);space(usage,16);add(usage,text("View real battery usage in Samsung's graphs",14,MUTED,false),-2);space(usage,14);item(usage,"Open original battery statistics",null,original);add(body,usage,-2);
        space(body,22);section(body,"Charging mode");addChargingModes(body);space(body,22);section(body,"Live battery monitor");LinearLayout monitor=card();
        LinearLayout metrics=row();liveTemp=metric(metrics,"Temperature");liveVoltage=metric(metrics,"Voltage");add(monitor,metrics,-2);space(monitor,16);line(monitor);space(monitor,16);
        LinearLayout metrics2=row();liveCurrent=metric(metrics2,"Battery current");liveCharge=metric(metrics2,"Charge stored");add(monitor,metrics2,-2);space(monitor,18);
        chargeNeeded=text("Charge needed to 100%: —",13,MUTED,false);add(monitor,chargeNeeded,-2);space(monitor,12);
        liveSession=text("Connect a charger to start monitoring",13,WHITE,false);add(monitor,liveSession,-2);space(monitor,12);
        powerGraph=new PowerGraph();add(monitor,powerGraph,84);space(monitor,8);liveSource=text("Measured battery power · current page only",11,MUTED,false);add(monitor,liveSource,-2);space(monitor,16);
        item(monitor,"Detailed battery diagnostics",null,this::chargingInfo);add(body,monitor,-2);
        space(body,22);section(body,"Custom features");LinearLayout custom=card();thermalNote=text(thermalLimit==0?"Disabled · only while this page is open":"Warn at "+thermalLimit+" °C · while this page is open",13,MUTED,false);item(custom,"Battery temperature alert",null,this::chooseThermalAlert);space(custom,5);add(custom,thermalNote,-2);add(body,custom,-2);
        space(body,22);section(body,"Battery settings");LinearLayout settings=card();
        nativeItem(settings,"Power saving settings","com.samsung.android.sm.powermode.ui.PowerModeSettingsActivity");
        nativeItem(settings,"Charging settings","com.samsung.android.sm.battery.ui.BatteryAdvancedMenuActivity");
        nativeItem(settings,"Battery protection","com.samsung.android.sm.battery.ui.protection.BatteryProtectionActivity");
        nativeItem(settings,"Background usage limits","com.samsung.android.sm.battery.ui.setting.AppPowerManagementActivity");
        nativeItem(settings,"App battery restrictions","com.samsung.android.sm.battery.ui.setting.AppRestrictionActivity");
        nativeItem(settings,"Performance profile","com.samsung.android.sm.battery.ui.setting.PerformanceOptimizationActivity");
        nativeItem(settings,"Enhanced processing","com.samsung.android.sm.battery.ui.setting.EnhancedProcessingActivity");
        nativeItem(settings,"Adaptive power saving","com.samsung.android.sm.powermode.ui.AdaptivePowerSavingActivity");
        nativeItem(settings,"Fast wireless charging","com.samsung.android.sm.battery.ui.fastwirelesscharging.FastWirelessActivity");
        nativeItem(settings,"Wireless power sharing","com.samsung.android.sm.powershare.ui.PowerShareActivity");
        if(settings.getChildCount()>0){space(settings,14);line(settings);space(settings,14);}
        item(settings,"All original Samsung battery features","Open the unmodified Samsung Battery page",original);add(body,settings,-2);
        ready=true;refresh();
        body.setAlpha(0.65f);body.setTranslationY(dp(12));body.animate().alpha(1).translationY(0).setDuration(350).setInterpolator(new PathInterpolator(.2f,.8f,.2f,1f)).start();
    }
    private TextView chevron(){TextView t=text("›",27,0xff696969,false);t.setGravity(Gravity.CENTER);return t;}
    private void item(LinearLayout parent,String name,String desc,Runnable action){LinearLayout r=row();LinearLayout t=col();add(t,text(name,18,WHITE,false),-2);if(desc!=null){space(t,5);TextView d=text(desc,14,MUTED,false);d.setLineSpacing(dp(3),1);add(t,d,-2);}r.addView(t,new LayoutParams(0,-2,1));r.addView(chevron(),new LayoutParams(dp(22),dp(36)));add(parent,r,-2);tap(r,action);}
    private void nativeItem(LinearLayout parent,String label,String cls){
        try{android.content.pm.ActivityInfo info=a.getPackageManager().getActivityInfo(new android.content.ComponentName(a.getPackageName(),cls),0);if(!info.enabled||!info.applicationInfo.enabled)return;}catch(Throwable t){return;}
        if(parent.getChildCount()>0){space(parent,14);line(parent);space(parent,14);}item(parent,label,null,()->openNative(cls));
    }
    private void showInsights(){
        String mode=selected==0?"Balanced":selected==1?"Power saving":selected==2?"High performance":"Unknown";
        String estimate=selected>=0&&selected<3?estimates[selected]:"Unavailable";
        String advice=pluggedIn?"Battery protection can reduce time spent at full charge.":"Use Power saving or review background activity to reduce drain.";
        new AlertDialog.Builder(a).setTitle("Battery insights").setMessage("Battery: "+(currentPercent<0?"Unavailable":currentPercent+"%")+"\nMode: "+mode+"\nEstimated time: "+estimate+"\n\n"+advice+"\n\nLocal readings and rule-based tips, not AI-generated analysis.").setNegativeButton("Close",null).setPositiveButton("Samsung usage statistics",(d,w)->original.run()).show();
    }
    private void openNative(String cls){try{Intent i=new Intent().setClassName(a.getPackageName(),cls);a.startActivity(i);}catch(Throwable t){BatteryHook.log("native screen unavailable: "+cls+": "+t);original.run();Toast.makeText(a,"Use the original Samsung settings.",Toast.LENGTH_SHORT).show();}}
    private void dialog(String title,String msg){new AlertDialog.Builder(a).setTitle(title).setMessage(msg).setPositiveButton(android.R.string.ok,null).show();}
    private void health(){refresh();String t="Unavailable",v="Unavailable",h="Unavailable";if(battery!=null){int raw=battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,Integer.MIN_VALUE);if(raw!=Integer.MIN_VALUE)t=String.format(Locale.US,"%.1f °C",raw/10f);int mv=battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE,-1);if(mv>0)v=String.format(Locale.US,"%.2f V",mv/1000f);int status=battery.getIntExtra(BatteryManager.EXTRA_HEALTH,0);if(status==BatteryManager.BATTERY_HEALTH_GOOD)h="Good";else if(status==BatteryManager.BATTERY_HEALTH_OVERHEAT)h="Overheating";else if(status==BatteryManager.BATTERY_HEALTH_COLD)h="Cold";else if(status==BatteryManager.BATTERY_HEALTH_DEAD)h="Dead";else if(status==BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE)h="Over voltage";else if(status==BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE)h="Reported failure";}
        new AlertDialog.Builder(a).setTitle("Battery health").setMessage("Maximum capacity: Not exposed by this build\n\nHealth status: "+h+"\nTemperature: "+t+"\nVoltage: "+v).setNegativeButton("Close",null).setPositiveButton("Battery protection",(d,w)->openNative("com.samsung.android.sm.battery.ui.protection.BatteryProtectionActivity")).show();}
    void refresh(){try{battery=a.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));int percent=-1;boolean plugged=false;String state="Battery status unavailable";if(battery!=null){int l=battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),s=battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1);if(l>=0&&s>0&&l<=s)percent=Math.round(100f*l/s);int status=battery.getIntExtra(BatteryManager.EXTRA_STATUS,0);int plug=battery.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);plugged=plug!=0;switch(status){case BatteryManager.BATTERY_STATUS_FULL:state=plugged?"⚡ Fully charged":"Fully charged";break;case BatteryManager.BATTERY_STATUS_CHARGING:state=plug==BatteryManager.BATTERY_PLUGGED_WIRELESS?"⚡ Wireless charging":"⚡ Charging";try{BatteryManager bm=(BatteryManager)a.getSystemService(Context.BATTERY_SERVICE);long ms=bm.computeChargeTimeRemaining();if(ms>0){long min=Math.max(1,(ms+59999)/60000);state+=" | "+(min/60>0?min/60+" h ":"")+(min%60)+" min until full";}}catch(Throwable ignore){}break;case BatteryManager.BATTERY_STATUS_DISCHARGING:state="Not charging";break;case BatteryManager.BATTERY_STATUS_NOT_CHARGING:state="Not charging";break;}}
       boolean plugChanged=pluggedIn!=plugged;currentPercent=percent;pluggedIn=plugged;number.setText(percent<0?"—":String.valueOf(percent));bar.setLevel(percent,plugged);
       if(plugged){updateChargingHeader();if(plugChanged){handler.removeCallbacks(powerTick);handler.postDelayed(powerTick,1500);}}else{handler.removeCallbacks(powerTick);updateMonitor(telemetry.read());chargeDetail.setText("");charge.setTextColor(MUTED);charge.setTypeface(Typeface.create(deviceTypeface,400,false));String e=selected>=0&&selected<3?estimates[selected]:"";charge.setText(e.contains("min")?"Can last about "+e:state);}
       if(ready){showMode(engine.activeMode());updateEstimates();}
       }catch(Throwable t){BatteryHook.log("battery refresh: "+t);}}
    private void updateChargingHeader(){
        if(!pluggedIn)return;
        ChargeTelemetry.Reading r=telemetry.read();
        if(!r.plugged)return;
        charge.setTextColor(WHITE);charge.setTypeface(Typeface.create(deviceTypeface,650,true));
        charge.setText("ϟ "+(r.full?"Fully charged":r.discharging?"Connected":r.wattsText==null?(r.charging?"Charging":"Connected"):(r.estimated?"≈ ":"")+r.wattsText));
        String detail=r.uncertain?"Current magnitude":r.discharging?"Battery discharging":r.wattsText==null?(r.charging?"Measuring power…":"Charging paused"):(r.wireless?"Wireless · ":"")+(r.estimated?"Estimated battery power":"Battery power");
        if(r.charging&&r.minutes>0&&!r.uncertain)detail+=" · "+(r.minutes/60>0?r.minutes/60+" h ":"")+r.minutes%60+" min";
        chargeDetail.setText(detail);
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
        liveTemp.setText(temperature(r));liveVoltage.setText(voltage(r));liveCurrent.setText(Double.isFinite(r.effectiveUa)?String.format(Locale.US,"%s%.2f A",r.estimated?"≈ ":"",r.effectiveUa/1000000d):Math.abs((long)r.ua)>=50000&&r.ua!=Integer.MIN_VALUE?current(r.ua):"—");liveCharge.setText(capacity(r));
        if(chargeNeeded!=null){double full=r.percent>=10&&r.counter>0?r.counter*100d/r.percent:Double.NaN;chargeNeeded.setText(Double.isFinite(full)&&full>=1000000&&full<=20000000?"Charge needed to 100%: ≈ "+String.format(Locale.US,"%.0f mAh",Math.max(0,full-r.counter)/1000):"Charge needed to 100%: unavailable");}
        if(thermalLimit>0&&r.temp!=Integer.MIN_VALUE){boolean hot=r.temp>=thermalLimit*10;if(hot&&!thermalWarned&&hasWindowFocus()){thermalWarned=true;Toast.makeText(a,"Battery temperature reached "+temperature(r)+". Charging may slow down.",Toast.LENGTH_LONG).show();}else if(!hot&&r.temp<(thermalLimit-1)*10)thermalWarned=false;}
        refreshChargingPolicy();
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
        new AlertDialog.Builder(a).setTitle("Battery temperature alert").setSingleChoiceItems(names,checked,(d,w)->{thermalLimit=values[w];thermalWarned=false;a.getSharedPreferences("colour_battery_options",0).edit().putInt("thermal_alert",thermalLimit).apply();thermalNote.setText(thermalLimit==0?"Disabled · only while this page is open":"Warn at "+thermalLimit+" °C · while this page is open");d.dismiss();}).setNegativeButton("Cancel",null).show();
    }
    private void addChargingModes(LinearLayout body){
        LinearLayout c=card(),r=row();String[] titles={"Standard\ncharging","Fast & Super fast\ncharging"};String[] desc={"Normal speed","Automatically negotiated"};
        for(int i=0;i<2;i++){final int mode=i;LinearLayout tile=col();tile.setGravity(Gravity.CENTER);tile.setPadding(dp(6),dp(9),dp(6),dp(9));chargingTiles[i]=tile;TextView z=text(i==0?"ϟ":"ϟϟ",23,WHITE,true);z.setGravity(Gravity.CENTER);add(tile,z,28);TextView title=text(titles[i],13,WHITE,false);title.setGravity(Gravity.CENTER);title.setMaxLines(3);chargingTitles[i]=title;add(tile,title,48);TextView sub=text(desc[i],10,MUTED,false);sub.setGravity(Gravity.CENTER);sub.setMaxLines(2);add(tile,sub,30);LayoutParams lp=new LayoutParams(0,dp(119),1);if(i>0)lp.leftMargin=dp(10);r.addView(tile,lp);tap(tile,()->selectChargingPolicy(mode));}
        add(c,r,-2);space(c,12);chargePolicyNote=text("Reading Samsung charging setting",12,MUTED,false);add(c,chargePolicyNote,-2);space(c,14);item(c,"More charging settings",null,()->openNative("com.samsung.android.sm.battery.ui.BatteryAdvancedMenuActivity"));add(body,c,-2);refreshChargingPolicy();
    }
    private void refreshChargingPolicy(){
        if(chargingTiles[0]==null)return;int state=chargingPolicy.read();
        for(int i=0;i<2;i++){boolean active=state==i;GradientDrawable bg=shape(active?0xff16291b:TILE,22);if(active)bg.setStroke(dp(1),0xff66d66e);chargingTiles[i].setBackground(bg);chargingTitles[i].setTextColor(active?GREEN:WHITE);}
        if(chargePolicyNote!=null)chargePolicyNote.setText(state<0?"Open native settings to configure charging":state==0?"Fast charging disabled":"Fast / Super fast enabled · charger, battery and temperature determine actual speed");
    }
    private void selectChargingPolicy(int mode){
        if(chargingPolicy.read()<0){openNative("com.samsung.android.sm.battery.ui.BatteryAdvancedMenuActivity");return;}
        if(chargingPolicy.read()==mode)return;
        if(pluggedIn){dialog("Charging mode","Unplug the charger before changing charging speed, then reconnect it.");return;}
        boolean ok=chargingPolicy.apply(mode);refreshChargingPolicy();if(!ok)Toast.makeText(a,"Samsung did not confirm the charging mode. Use original Charging settings.",Toast.LENGTH_LONG).show();else{chargingTiles[mode].setAlpha(.65f);chargingTiles[mode].animate().alpha(1).setDuration(280).start();}
    }
    private void chargingInfo(){
        ChargeTelemetry.Reading r=telemetry.read();
        dialog("Battery diagnostics", "Battery power: "+(r.wattsText==null?"Unavailable":r.wattsText)+"\nSource: "+r.source+"\nVoltage: "+voltage(r)+"\nRaw current: "+current(r.ua)+"\nAverage current: "+current(r.average)+"\nFuel gauge current: "+current(r.sysfsCurrent)+"\nCharge stored: "+capacity(r)+"\nTemperature: "+temperature(r)+"\nCycle count: "+(r.cycles<0?"Unavailable":r.cycles)+"\n\nBattery-side power is not charger output. A magnitude reading has unverified direction until the charge counter confirms polarity.");
    }
    // Only the hooked BatteryActivity window; no global status bar settings.
    void ensureBars(){
        if(!isAttachedToWindow()||!hasWindowFocus())return;
        try{
            android.view.Window w=a.getWindow();w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            View decor=w.getDecorView();int flags=decor.getSystemUiVisibility();
            int hide=View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LOW_PROFILE|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            decor.setSystemUiVisibility(flags&~hide);
            if(android.os.Build.VERSION.SDK_INT>=30){WindowInsetsController ctl=w.getInsetsController();if(ctl!=null){ctl.setSystemBarsAppearance(0,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);ctl.show(WindowInsets.Type.systemBars());}}
            w.setStatusBarColor(Color.BLACK);
            w.setNavigationBarColor(Color.BLACK);
            applySafeInsets(getRootWindowInsets());requestApplyInsets();
        }catch(Throwable t){BatteryHook.log("system bars: "+t);}
    }
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus){post(this::ensureBars);handler.removeCallbacks(powerTick);if(pluggedIn)handler.post(powerTick);}else{handler.removeCallbacks(powerTick);}}
    private void applySafeInsets(WindowInsets insets){
        if(insets==null)return;
        android.graphics.Insets i=insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
        int[] pos=new int[2];getLocationOnScreen(pos);
        int top=Math.max(0,i.top-Math.max(0,pos[1]));
        int bottom=0;
        if(getHeight()>0){int screen=a.getWindowManager().getCurrentWindowMetrics().getBounds().height();bottom=Math.max(0,i.bottom-Math.max(0,screen-pos[1]-getHeight()));}
        if(getPaddingTop()!=top||getPaddingBottom()!=bottom)setPadding(0,top,0,bottom);
    }
    private void showMode(int which){
        int old=selected;boolean changed=which!=selected;selected=which;
        if(changed&&lens!=null){lens.move(old,which); }
        boolean saving=engine.isSaving();
        syncingSwitch=true;superSwitch.setChecked(saving);syncingSwitch=false;
        for(int j=0;j<3;j++){
            final int k=j;boolean chosen=j==which;
            modes[j].setSelected(chosen);
            if(changed){
                if(tileAnimations[j]!=null)tileAnimations[j].cancel();
                ValueAnimator anim=ValueAnimator.ofFloat(glass[j].amount,chosen?1f:0f);anim.setDuration(420);anim.setInterpolator(new PathInterpolator(.18f,.72f,.18f,1f));anim.addUpdateListener(v->{float f=(float)v.getAnimatedValue();glass[k].amount=f;modeLabels[k].setTextColor(blend(WHITE,GREEN,f));modeIcons[k].color=blend(WHITE,GREEN,f);modeIcons[k].invalidate();modeNotes[k].setTextColor(blend(MUTED,GREEN,f));});tileAnimations[j]=anim;anim.start();
                
            }
        }
    }
    private void choose(int mode){
        if(busy||!isAttachedToWindow())return;
        busy=true;superSwitch.setEnabled(false);showMode(engine.activeMode());
        for(LinearLayout tile:modes)tile.setEnabled(false);
        engine.apply(mode,()->{
            if(!isAttachedToWindow())return;
            busy=false;superSwitch.setEnabled(true);for(LinearLayout tile:modes)tile.setEnabled(true);refresh();
            if(engine.activeMode()!=mode){Toast.makeText(a,"Samsung did not confirm the requested mode.",Toast.LENGTH_SHORT).show();BatteryHook.log("mode not confirmed "+mode);}
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
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();try{IntentFilter filter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);if(android.os.Build.VERSION.SDK_INT>=33)a.registerReceiver(receiver,filter,Context.RECEIVER_EXPORTED);else a.registerReceiver(receiver,filter);registered=true;}catch(Throwable t){BatteryHook.log("receiver: "+t);}try{a.getContentResolver().registerContentObserver(Settings.Global.getUriFor("low_power"),false,modeObserver);observing=true;a.getContentResolver().registerContentObserver(Settings.Global.getUriFor("sem_low_heat_mode"),false,modeObserver);}catch(Throwable t){BatteryHook.log("observer: "+t);}handler.removeCallbacks(tick);handler.post(tick);post(()->{ensureBars();applySafeInsets(getRootWindowInsets());requestApplyInsets();});}
    @Override protected void onDetachedFromWindow(){handler.removeCallbacks(tick);handler.removeCallbacks(powerTick);if(observing){try{a.getContentResolver().unregisterContentObserver(modeObserver);}catch(Throwable ignored){}observing=false;}if(registered){try{a.unregisterReceiver(receiver);}catch(Throwable ignored){}registered=false;}for(ValueAnimator an:tileAnimations)if(an!=null)an.cancel();if(slideMotion!=null){slideMotion.removeAllListeners();slideMotion.cancel();}if(lens!=null)lens.release();bar.stop();engine.close();super.onDetachedFromWindow();}
    private static int blend(int a,int b,float f){return android.graphics.Color.rgb(Math.round(Color.red(a)+(Color.red(b)-Color.red(a))*f),Math.round(Color.green(a)+(Color.green(b)-Color.green(a))*f),Math.round(Color.blue(a)+(Color.blue(b)-Color.blue(a))*f));}
    private final class Glass extends Drawable {
        private final Paint p=new Paint(3);private final RectF box=new RectF();float amount;
        @Override public void draw(Canvas c){box.set(getBounds());p.setColor(TILE);c.drawRoundRect(box,dp(22),dp(22),p);}
        @Override public void setAlpha(int a){} @Override public void setColorFilter(ColorFilter f){} @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    // A single persistent lens moves from the actual previous card to the confirmed next card.
    // No duplicate selection backgrounds or fake flash on refresh.
    private final class SelectionLens extends View {
        private final Paint p=new Paint(3);private final RectF rect=new RectF(),inner=new RectF();private final Path clip=new Path();
        private float slot=-1,progress=1,travel=0;private boolean moving;
        private android.graphics.Bitmap snapshot;private android.graphics.RuntimeShader optical;private boolean shaderFailed;
        private static final String OPTICS=
            "uniform shader scene; uniform float4 box; uniform float radius; uniform float depth;"+
            "float sd(float2 p){float2 q=abs(p-box.xy)-box.zw+radius;return length(max(q,float2(0.0)))+min(max(q.x,q.y),0.0)-radius;}"+
            "half4 main(float2 p){float d=sd(p);float edge=exp(-abs(d+depth*0.48)/(depth*0.38));"+
            "float2 n=normalize(float2(sd(p+float2(1.0,0.0))-sd(p-float2(1.0,0.0)),sd(p+float2(0.0,1.0))-sd(p-float2(0.0,1.0)))+float2(0.0001));"+
            "half3 src=scene.eval(p-n*(depth*0.95*edge)).rgb;float spec=pow(max(0.0,dot(n,normalize(float2(-0.8,-1.0)))),5.0);"+
            "float lower=pow(max(0.0,dot(n,normalize(float2(0.45,1.0)))),4.0);"+
            "half3 col=src*half3(0.76,1.02,0.76)+half3(0.035,0.15,0.045)+half3(0.24,0.58,0.22)*half(edge*(spec*0.65+lower*0.42));"+
            "half a=half(clamp(edge*0.85,0.0,0.88));return half4(clamp(col,half3(0.0),half3(1.0))*a,a);}";
        SelectionLens(){super(a);setClickable(false);setFocusable(false);}
        void release(){snapshot=null;optical=null;p.setShader(null);}
        private void capture(){
            if(shaderFailed||android.os.Build.VERSION.SDK_INT<33||tileRow==null||tileRow.getWidth()<=0||tileRow.getHeight()<=0)return;
            try{snapshot=android.graphics.Bitmap.createBitmap(tileRow.getWidth(),tileRow.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);tileRow.draw(new Canvas(snapshot));optical=new android.graphics.RuntimeShader(OPTICS);android.graphics.BitmapShader bitmapShader=new android.graphics.BitmapShader(snapshot,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);bitmapShader.setFilterMode(android.graphics.BitmapShader.FILTER_MODE_LINEAR);optical.setInputShader("scene",bitmapShader);BatteryHook.log("optical lens shader ready");}catch(Throwable e){shaderFailed=true;release();BatteryHook.log("glass GPU fallback: "+e);}
        }
        void move(int from,int to){
            float initial=slot<0?from:slot;if(slideMotion!=null){slideMotion.removeAllListeners();slideMotion.cancel();}release();
            if(to<0){slot=-1;moving=false;invalidate();return;}
            if(initial<0||Math.abs(initial-to)<.001f){slot=to;moving=false;progress=1;invalidate();return;}
            moving=true;progress=0;travel=to-initial;capture();
            final float start=initial;
            slideMotion=ValueAnimator.ofFloat(0f,1f);slideMotion.setDuration(520);slideMotion.setInterpolator(new PathInterpolator(.20f,.70f,.19f,1f));
            slideMotion.addUpdateListener(v->{progress=(float)v.getAnimatedValue();slot=start+travel*progress;invalidate();});
            slideMotion.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator animation){moving=false;slot=to;progress=1;release();invalidate();}});
            slideMotion.start();
        }
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);invalidate();}
        @Override protected void onDraw(Canvas c){
            if(slot<0||tileRow==null||modes[0].getWidth()==0)return;
            float wave=moving?(float)Math.sin(Math.PI*progress):0,w=modes[0].getWidth(),step=modes[1].getLeft()-modes[0].getLeft(),x=modes[0].getLeft()+slot*step;
            float stretch=dp(12)*wave,shift=dp(4)*wave*Math.signum(travel);rect.set(x+dp(1)-stretch+shift,dp(1)+dp(2)*wave,x+w-dp(1)+stretch+shift,getHeight()-dp(1)-dp(2)*wave);float r=dp(22);
            p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(rect.left,rect.top,rect.right,rect.bottom,new int[]{0x70215c29,0x42102914,0x64265b2b},new float[]{0,.50f,1},Shader.TileMode.CLAMP));c.drawRoundRect(rect,r,r,p);p.setShader(null);
            if(moving&&optical!=null&&c.isHardwareAccelerated()){
                try{optical.setFloatUniform("box",rect.centerX(),rect.centerY(),rect.width()/2,rect.height()/2);optical.setFloatUniform("radius",r);optical.setFloatUniform("depth",dp(13));p.setShader(optical);c.drawRoundRect(rect,r,r,p);p.setShader(null);}catch(Throwable e){shaderFailed=true;release();BatteryHook.log("glass draw fallback: "+e);}
            }else if(moving&&wave>0){
                p.setShader(new RadialGradient(rect.centerX(),rect.bottom,Math.max(1,rect.width()),new int[]{0x2424cf37,0x0024cf37},null,Shader.TileMode.CLAMP));c.drawRoundRect(rect,r,r,p);p.setShader(null);
            }
            // Beveled edge: wide low-intensity body, hairline rim, and interior specular edge.
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(4));p.setColor(0x4024c939);inner.set(rect);inner.inset(dp(1.5f),dp(1.5f));c.drawRoundRect(inner,r-dp(1.5f),r-dp(1.5f),p);
            p.setStrokeWidth(dp(1.15f));p.setShader(new LinearGradient(rect.left,rect.top,rect.right,rect.bottom,new int[]{0xff86ef79,0x9e2dc333,0x891d7726,0xef94e68b},new float[]{0,.27f,.68f,1},Shader.TileMode.CLAMP));c.drawRoundRect(rect,r,r,p);p.setShader(null);
            inner.set(rect);inner.inset(dp(3),dp(3));p.setStrokeWidth(dp(1));p.setShader(new LinearGradient(inner.left,inner.top,inner.right,inner.bottom,new int[]{0x969aff96,0x00146220,0x0086ff7e,0x7893ff83},new float[]{0,.36f,.68f,1},Shader.TileMode.CLAMP));c.drawRoundRect(inner,r-dp(3),r-dp(3),p);p.setShader(null);p.setStyle(Paint.Style.FILL);
        }
    }
    private final class Gauge extends View {
        private final Paint p=new Paint(3);private final Path clip=new Path();private final RectF bounds=new RectF();
        private float level=0,wanted=-1,front=1;private int base=0xff17db26,targetColor=base;private boolean known=false,connected;
        private ValueAnimator motion,transition;
        Gauge(){super(a);}
        void stop(){if(motion!=null)motion.cancel();if(transition!=null)transition.cancel();}
        void setLevel(int value,boolean plug){
            float target=Math.max(0,value)/100f;int color=plug?BLUE:0xff17db26;
            boolean changed=known&&connected!=plug;boolean first=!known;known=true;connected=plug;
            if(wanted!=target){wanted=target;if(motion!=null)motion.cancel();final float initial=level;motion=ValueAnimator.ofFloat(initial,target);motion.setDuration(first?360:240);motion.setInterpolator(new PathInterpolator(.18f,.75f,.2f,1f));motion.addUpdateListener(v->{level=(float)v.getAnimatedValue();invalidate();});motion.start();}
            if(first){base=targetColor=color;front=1;invalidate();}
            else if(changed){if(transition!=null)transition.cancel();base=targetColor;targetColor=color;front=0;transition=ValueAnimator.ofFloat(0f,1f);transition.setDuration(plug?620:440);transition.setInterpolator(new PathInterpolator(.12f,.75f,.18f,1f));transition.addUpdateListener(v->{front=(float)v.getAnimatedValue();invalidate();});transition.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator an){base=targetColor;front=1;invalidate();}});transition.start();}
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
