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
    private TextView assistantSummary,chargeDetail;
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
        uiContext=a;Typeface nativeFont=new TextView(a).getTypeface();deviceTypeface=nativeFont==null?Typeface.DEFAULT:nativeFont;engine=new NativeModes(a);telemetry=new ChargeTelemetry(a);
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
        LinearLayout tiles=row();tileRow=tiles;String[] captions={"Balanced\nmode","Power saving\nmode","High\nperformance\nmode"};
        for(int i=0;i<3;i++){
            final int index=i;LinearLayout tile=col();tile.setGravity(Gravity.CENTER_HORIZONTAL);tile.setPadding(dp(3),dp(12),dp(3),dp(10));glass[i]=new Glass();tile.setBackground(glass[i]);
            LayoutParams p=new LayoutParams(0,dp(122),1);if(i>0)p.leftMargin=dp(9);tiles.addView(tile,p);modes[i]=tile;
            Symbol symbol=new Symbol(i+1,WHITE);modeIcons[i]=symbol;tile.addView(symbol,new LayoutParams(dp(31),dp(30)));space(tile,5);
            TextView label=text(captions[i],12.5f,WHITE,false);label.setGravity(Gravity.CENTER);label.setMaxLines(3);label.setBreakStrategy(0);label.setHyphenationFrequency(0);label.setAutoSizeTextTypeUniformWithConfiguration(10,13,1,TypedValue.COMPLEX_UNIT_SP);modeLabels[i]=label;tile.addView(label,new LayoutParams(-1,0,1));
            TextView note=text("Estimating…",10.5f,MUTED,false);note.setGravity(Gravity.CENTER);note.setSingleLine(true);note.setAutoSizeTextTypeUniformWithConfiguration(9,11,1,TypedValue.COMPLEX_UNIT_SP);modeNotes[i]=note;add(tile,note,16);
            tap(tile,()->choose(index));
        }FrameLayout tileHost=new FrameLayout(a);tileHost.addView(tiles,new FrameLayout.LayoutParams(-1,dp(122)));lens=new SelectionLens();tileHost.addView(lens,new FrameLayout.LayoutParams(-1,-1));lens.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);add(main,tileHost,122);space(main,21);
        LinearLayout superRow=row();LinearLayout superText=col();add(superText,text("Super power saving mode",18,WHITE,false),-2);space(superText,5);superNote=text("Uses Samsung Power saving",13,MUTED,false);add(superText,superNote,-2);superRow.addView(superText,new LayoutParams(0,-2,1));superSwitch=new Switch(uiContext);superSwitch.setShowText(false);superSwitch.setThumbTintList(ColorStateList.valueOf(WHITE));superSwitch.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xff43af54,0xff555555}));superSwitch.setContentDescription("Super power saving mode (Samsung Power saving)");superRow.addView(superSwitch,new LayoutParams(-2,-2));add(main,superRow,-2);superSwitch.setOnCheckedChangeListener((b,checked)->{if(!syncingSwitch)choose(checked?1:engine.normalMode());});tap(superText,()->{if(!busy)choose(engine.isSaving()?engine.normalMode():1);});space(main,15);line(main);space(main,14);
        item(main,"Adaptive battery","Intelligently extends battery life based on your usage patterns and activity.",()->openNative("com.samsung.android.sm.battery.ui.setting.AdaptiveBatteryMenuActivity"));space(main,22);
        item(main,"Battery health",null,this::health);
        space(body,18);
        LinearLayout assistant=card();LinearLayout ar=row();Symbol ai=new Symbol(4,WHITE);ar.addView(ai,new LayoutParams(dp(35),dp(38)));LinearLayout at=col();LayoutParams ap=new LayoutParams(0,-2,1);ap.leftMargin=dp(14);ar.addView(at,ap);add(at,text("AI Battery Assistant",18,WHITE,false),-2);space(at,6);TextView info=text("See live battery insights and charging status.",14,MUTED,false);assistantSummary=info;info.setLineSpacing(dp(3),1);add(at,info,-2);ar.addView(chevron(),new LayoutParams(dp(20),dp(32)));add(assistant,ar,-2);tap(ar,this::showInsights);add(body,assistant,-2);
        space(body,26);section(body,"Battery usage");
        LinearLayout usage=card();LinearLayout tabs=row();tabs.setBackground(shape(TILE,22));tabs.setPadding(dp(4),dp(4),dp(4),dp(4));String[] ranges={"Today","Last 7 days"};for(int i=0;i<2;i++){final int ix=i;TextView t=text(ranges[i],14,WHITE,false);t.setGravity(Gravity.CENTER);if(i==0)t.setBackground(shape(0xff555555,18));tabs.addView(t,new LayoutParams(0,dp(36),1));tap(t,()->openNative(ix==0?"com.samsung.android.sm.battery.ui.graph.LastChargeGraphActivity":"com.samsung.android.sm.battery.ui.graph.DailyUsageGraphActivity"));}add(usage,tabs,-2);space(usage,16);add(usage,text("View real battery usage in Samsung's graphs",14,MUTED,false),-2);space(usage,14);item(usage,"Open original battery statistics",null,original);add(body,usage,-2);
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
       if(plugged){updateChargingHeader();if(plugChanged){handler.removeCallbacks(powerTick);handler.postDelayed(powerTick,1500);}}else{handler.removeCallbacks(powerTick);telemetry.reset();chargeDetail.setText("");charge.setTextColor(MUTED);charge.setTypeface(Typeface.create(deviceTypeface,400,false));String e=selected>=0&&selected<3?estimates[selected]:"";charge.setText(e.contains("min")?"Can last about "+e:state);}
       if(ready){showMode(engine.activeMode());updateEstimates();}
       }catch(Throwable t){BatteryHook.log("battery refresh: "+t);}}
    private void updateChargingHeader(){
        if(!pluggedIn)return;
        ChargeTelemetry.Reading r=telemetry.read();
        if(!r.plugged)return;
        charge.setTextColor(WHITE);charge.setTypeface(Typeface.create(deviceTypeface,650,true));
        charge.setText("ϟ "+(r.full?"Fully charged":r.charging?(r.wattsText==null?"Charging":r.wattsText+" charging"):"Connected"));
        String detail=r.wireless?"Wireless":"Battery input";
        if(r.wattsText!=null&&r.full)detail+=" · "+r.wattsText;
        if(r.charging&&r.minutes>0)detail+=" · "+(r.minutes/60>0?r.minutes/60+" h ":"")+r.minutes%60+" min until full";
        else if(r.wattsText==null&&!r.full&&r.charging)detail+=" · Watts unavailable";
        chargeDetail.setText(detail);
    }
    private void chargingInfo(){
        if(!pluggedIn)return;
        ChargeTelemetry.Reading r=telemetry.read();
        dialog("Charging power", "Live battery input: "+(r.wattsText==null?"Unavailable":r.wattsText)+"\nBattery voltage: "+(r.mv>0?String.format(Locale.US,"%.2f V",r.mv/1000f):"Unavailable")+"\nNet battery current: "+(r.ua==Integer.MIN_VALUE?"Unavailable":String.format(Locale.US,"%.2f A",r.ua/1000000f))+"\n\nThe wattage is net power entering the battery from the fuel gauge. It is not wall power or the charger's advertised maximum. Charging may slow near full or because of temperature and battery protection.");
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
                ValueAnimator anim=ValueAnimator.ofFloat(glass[j].amount,chosen?1f:0f);anim.setDuration(420);anim.setInterpolator(new PathInterpolator(.18f,.72f,.18f,1f));anim.addUpdateListener(v->{float f=(float)v.getAnimatedValue();glass[k].amount=f;glass[k].invalidateSelf();modeLabels[k].setTextColor(blend(WHITE,GREEN,f));modeIcons[k].color=blend(WHITE,GREEN,f);modeIcons[k].invalidate();modeNotes[k].setTextColor(blend(MUTED,GREEN,f));});tileAnimations[j]=anim;anim.start();
                if(chosen){modes[j].animate().cancel();modes[j].setScaleX(.985f);modes[j].setScaleY(.985f);modes[j].animate().scaleX(1).scaleY(1).setDuration(290).setInterpolator(new android.view.animation.OvershootInterpolator(.5f)).start();}
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
    @Override protected void onDetachedFromWindow(){handler.removeCallbacks(tick);handler.removeCallbacks(powerTick);if(observing){try{a.getContentResolver().unregisterContentObserver(modeObserver);}catch(Throwable ignored){}observing=false;}if(registered){try{a.unregisterReceiver(receiver);}catch(Throwable ignored){}registered=false;}for(ValueAnimator an:tileAnimations)if(an!=null)an.cancel();if(slideMotion!=null)slideMotion.cancel();if(lens!=null)lens.release();bar.stop();engine.close();super.onDetachedFromWindow();}
    private static int blend(int a,int b,float f){return android.graphics.Color.rgb(Math.round(Color.red(a)+(Color.red(b)-Color.red(a))*f),Math.round(Color.green(a)+(Color.green(b)-Color.green(a))*f),Math.round(Color.blue(a)+(Color.blue(b)-Color.blue(a))*f));}
    private final class Glass extends Drawable {
        private final Paint p=new Paint(3);private final RectF box=new RectF();float amount;
        @Override public void draw(Canvas c){
            box.set(getBounds());float r=dp(22);p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(TILE);c.drawRoundRect(box,r,r,p);
            if(amount<=.001f)return;
            p.setShader(new LinearGradient(box.left,box.top,box.right,box.bottom,new int[]{blend(TILE,0xff172c1a,amount),blend(TILE,0xff142216,amount),blend(TILE,0xff1b321e,amount)},new float[]{0,.55f,1},Shader.TileMode.CLAMP));c.drawRoundRect(box,r,r,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));box.inset(dp(2),dp(2));
            p.setColor(((int)(amount*23)<<24)|0x29d43e);p.setShader(null);c.drawRoundRect(box,r-dp(2),r-dp(2),p);
            p.setStrokeWidth(dp(1.15f));box.inset(-dp(1),-dp(1));
            p.setShader(new LinearGradient(box.left,box.top,box.right,box.bottom,new int[]{((int)(amount*210)<<24)|0x6cdd67,((int)(amount*95)<<24)|0x299c30,((int)(amount*68)<<24)|0x20862a,((int)(amount*185)<<24)|0x80dd74},new float[]{0,.28f,.68f,1},Shader.TileMode.CLAMP));c.drawRoundRect(box,r-dp(1),r-dp(1),p);
            box.inset(dp(2),dp(2));p.setStrokeWidth(dp(.7f));p.setShader(new LinearGradient(box.left,box.top,box.right,box.bottom,new int[]{((int)(amount*65)<<24)|0x9cfb99,0x001b7026,((int)(amount*42)<<24)|0x79f07a},null,Shader.TileMode.CLAMP));c.drawRoundRect(box,r-dp(3),r-dp(3),p);p.setShader(null);p.setStyle(Paint.Style.FILL);
        }
        @Override public void setAlpha(int a){} @Override public void setColorFilter(ColorFilter f){} @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private final class SelectionLens extends View {
        private final Paint p=new Paint(3);private final RectF rect=new RectF();private float slot=-1,progress=1;private boolean moving;
        private android.graphics.Bitmap snapshot;private android.graphics.RuntimeShader optical;private boolean shaderFailed;
        private static final String OPTICS=
            "uniform shader scene; uniform float4 box; uniform float radius; uniform float depth; uniform float amount;"+
            "float sd(float2 p){float2 q=abs(p-box.xy)-box.zw+radius;return length(max(q,float2(0)))+min(max(q.x,q.y),0.0)-radius;}"+
            "half4 main(float2 p){float d=sd(p);float mask=1.0-smoothstep(-0.5,0.65,d);float edge=exp(-abs(d+depth*0.46)/(depth*0.36));"+
            "float2 normal=normalize(float2(sd(p+float2(1,0))-sd(p-float2(1,0)),sd(p+float2(0,1))-sd(p-float2(0,1)))+float2(0.0001));"+
            "float2 uv=p-normal*depth*0.82*edge;half3 src=scene.eval(uv).rgb;"+
            "float spec=pow(max(0.0,dot(normal,normalize(float2(-0.7,-1.0)))),4.0);"+
            "float bottom=pow(max(0.0,dot(normal,normalize(float2(0.6,1.0)))),5.0);"+
            "half3 rgb=mix(src,half3(0.06,0.26,0.08),half(0.25));rgb+=half3(0.36,0.75,0.29)*half(spec*edge*0.58+bottom*edge*0.40);"+
            "float rim=exp(-abs(d+0.8)/1.15);rgb+=half3(0.10,0.65,0.10)*half(rim*0.4);"+
            "half alpha=half(mask*amount*clamp(edge*0.83+0.055,0.0,1.0));return half4(clamp(rgb,half3(0),half3(1))*alpha,alpha);}";
        SelectionLens(){super(a);setClickable(false);setFocusable(false);}
        void release(){snapshot=null;optical=null;p.setShader(null);}
        private void capture(){
            if(shaderFailed||android.os.Build.VERSION.SDK_INT<33||tileRow==null||tileRow.getWidth()<=0||tileRow.getHeight()<=0)return;
            try{snapshot=android.graphics.Bitmap.createBitmap(tileRow.getWidth(),tileRow.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);tileRow.draw(new Canvas(snapshot));optical=new android.graphics.RuntimeShader(OPTICS);optical.setInputShader("scene",new android.graphics.BitmapShader(snapshot,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP));}catch(Throwable e){shaderFailed=true;release();BatteryHook.log("glass GPU fallback: "+e);}
        }
        void move(int from,int to){
            if(slideMotion!=null)slideMotion.cancel();
            release();
            if(to<0){slot=-1;moving=false;invalidate();return;}
            if(from<0||from==to){slot=to;moving=false;invalidate();return;}
            float begin=slot<0?from:slot;moving=true;progress=0;capture();
            slideMotion=ValueAnimator.ofFloat(0f,1f);slideMotion.setDuration(430);slideMotion.setInterpolator(new PathInterpolator(.18f,.74f,.16f,1f));
            slideMotion.addUpdateListener(v->{progress=(float)v.getAnimatedValue();slot=begin+(to-begin)*progress;invalidate();});
            slideMotion.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator animation){moving=false;slot=to;release();invalidate();}});
            slideMotion.start();
        }
        @Override protected void onDraw(Canvas c){
            if(!moving||tileRow==null||modes[0].getWidth()==0)return;
            float wave=(float)Math.sin(Math.PI*progress),w=modes[0].getWidth(),step=modes[1].getLeft()-modes[0].getLeft(),x=modes[0].getLeft()+slot*step;
            float stretch=dp(6)*wave;rect.set(x+dp(1)-stretch,dp(1)+dp(2)*wave,x+w-dp(1)+stretch,getHeight()-dp(1)-dp(2)*wave);float r=dp(22),alpha=Math.min(1,4*wave);
            if(optical!=null&&c.isHardwareAccelerated()){
                try{optical.setFloatUniform("box",rect.centerX(),rect.centerY(),rect.width()/2,rect.height()/2);optical.setFloatUniform("radius",r);optical.setFloatUniform("depth",dp(8));optical.setFloatUniform("amount",alpha);p.setShader(optical);p.setStyle(Paint.Style.FILL);c.drawRoundRect(rect,r,r,p);p.setShader(null);return;}catch(Throwable e){shaderFailed=true;release();BatteryHook.log("glass draw fallback: "+e);}
            }
            p.setStyle(Paint.Style.FILL);p.setColor(((int)(alpha*22)<<24)|0x54df55);c.drawRoundRect(rect,r,r,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.3f));p.setShader(new LinearGradient(rect.left,rect.top,rect.right,rect.bottom,new int[]{((int)(alpha*185)<<24)|0x97ff8e,((int)(alpha*36)<<24)|0x26972a,((int)(alpha*180)<<24)|0x77ee6c},null,Shader.TileMode.CLAMP));c.drawRoundRect(rect,r,r,p);p.setShader(null);p.setStyle(Paint.Style.FILL);
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
