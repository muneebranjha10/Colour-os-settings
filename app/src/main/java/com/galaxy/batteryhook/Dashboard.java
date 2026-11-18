package com.galaxy.batteryhook;

import android.app.Activity;
import android.animation.ValueAnimator;
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
    private static final int BG=0xff000000,CARD=0xff1b1b1b,TILE=0xff292929,WHITE=0xfff2f2f2,MUTED=0xff9a9a9a,BLUE=0xff168dff,GREEN=0xff4cce61;
    private final Activity a;
    private final Context uiContext;
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
    private TextView text(String value,float sp,int color,boolean bold){TextView t=new TextView(uiContext);t.setText(value);t.setTextSize(TypedValue.COMPLEX_UNIT_SP,sp);t.setTextColor(color);t.setIncludeFontPadding(false);t.setTypeface(Typeface.create(Typeface.create("sans-serif",Typeface.NORMAL),bold?650:400,false));return t;}
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
        android.content.res.Configuration config=new android.content.res.Configuration(a.getResources().getConfiguration());config.fontScale=Math.min(config.fontScale,1.05f);if(android.os.Build.VERSION.SDK_INT>=31)config.fontWeightAdjustment=0;uiContext=a.createConfigurationContext(config);engine=new NativeModes(a);
        setOrientation(VERTICAL);setBackgroundColor(BG);setClickable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setOnApplyWindowInsetsListener((v,insets)->{applySafeInsets(insets);return insets;});
        LinearLayout header=row();header.setPadding(dp(19),dp(6),dp(19),dp(6));
        FrameLayout back=new FrameLayout(a);back.setBackground(shape(TILE,25));Symbol arrow=new Symbol(0,WHITE);back.addView(arrow,new FrameLayout.LayoutParams(-1,-1));back.setContentDescription("Back");tap(back,()->a.finish());header.addView(back,new LayoutParams(dp(40),dp(40)));
        TextView title=text("Battery",21,WHITE,true);LayoutParams tl=new LayoutParams(0,-2,1);tl.leftMargin=dp(12);header.addView(title,tl);
        TextView menu=text("⋮",28,MUTED,true);menu.setGravity(Gravity.CENTER);menu.setContentDescription("Original Samsung battery settings");header.addView(menu,new LayoutParams(dp(40),dp(44)));tap(menu,original);
        add(this,header,56);
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);addView(scroll,new LayoutParams(-1,0,1));
        LinearLayout body=col();body.setPadding(dp(18),dp(15),dp(18),dp(24));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        LinearLayout chargeRow=row();chargeRow.setGravity(Gravity.BOTTOM);
        LinearLayout amount=row();amount.setGravity(Gravity.BOTTOM);number=text("—",46,WHITE,true);amount.addView(number);TextView pct=text("%",16,WHITE,true);pct.setPadding(dp(4),0,0,dp(5));amount.addView(pct);chargeRow.addView(amount,new LayoutParams(-2,-2));charge=text("",12,MUTED,false);charge.setGravity(Gravity.END);charge.setPadding(dp(10),0,0,dp(6));chargeRow.addView(charge,new LayoutParams(0,-2,1));add(body,chargeRow,-2);space(body,8);
        bar=new Gauge();add(body,bar,30);space(body,23);
        TextView instructions=text("Instructions ⓘ",14,MUTED,false);instructions.setPadding(dp(10),0,0,dp(2));add(body,instructions,-2);tap(instructions,()->dialog("Instructions","Balanced uses Samsung Light profile on supported phones; High performance uses Samsung Standard (no overclocking). Power saving and Super power saving control the same Samsung saver. Times are Samsung estimates or measured discharge estimates, not guarantees. Tap ⋮ for the original page."));space(body,9);
        LinearLayout main=card();add(body,main,-2);
        LinearLayout tiles=row();String[] captions={"Balanced\nmode","Power saving\nmode","High\nperformance\nmode"};
        for(int i=0;i<3;i++){
            final int index=i;LinearLayout tile=col();tile.setGravity(Gravity.CENTER_HORIZONTAL);tile.setPadding(dp(3),dp(12),dp(3),dp(10));glass[i]=new Glass();tile.setBackground(glass[i]);
            LayoutParams p=new LayoutParams(0,dp(122),1);if(i>0)p.leftMargin=dp(9);tiles.addView(tile,p);modes[i]=tile;
            Symbol symbol=new Symbol(i+1,WHITE);modeIcons[i]=symbol;tile.addView(symbol,new LayoutParams(dp(31),dp(30)));space(tile,5);
            TextView label=text(captions[i],12.5f,WHITE,false);label.setGravity(Gravity.CENTER);label.setMaxLines(3);label.setBreakStrategy(0);label.setHyphenationFrequency(0);label.setAutoSizeTextTypeUniformWithConfiguration(10,13,1,TypedValue.COMPLEX_UNIT_SP);modeLabels[i]=label;tile.addView(label,new LayoutParams(-1,0,1));
            TextView note=text("Estimating…",10.5f,MUTED,false);note.setGravity(Gravity.CENTER);note.setSingleLine(true);note.setAutoSizeTextTypeUniformWithConfiguration(9,11,1,TypedValue.COMPLEX_UNIT_SP);modeNotes[i]=note;add(tile,note,16);
            tap(tile,()->choose(index));
        }add(main,tiles,-2);space(main,21);
        LinearLayout superRow=row();LinearLayout superText=col();add(superText,text("Super power saving mode",18,WHITE,false),-2);space(superText,5);superNote=text("Uses Samsung Power saving",13,MUTED,false);add(superText,superNote,-2);superRow.addView(superText,new LayoutParams(0,-2,1));superSwitch=new Switch(uiContext);superSwitch.setShowText(false);superSwitch.setThumbTintList(ColorStateList.valueOf(WHITE));superSwitch.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xff43af54,0xff555555}));superSwitch.setContentDescription("Super power saving mode (Samsung Power saving)");superRow.addView(superSwitch,new LayoutParams(-2,-2));add(main,superRow,-2);superSwitch.setOnCheckedChangeListener((b,checked)->{if(!syncingSwitch)choose(checked?1:engine.normalMode());});tap(superText,()->{if(!busy)choose(engine.isSaving()?engine.normalMode():1);});space(main,15);line(main);space(main,14);
        item(main,"Adaptive battery","Intelligently extends battery life based on your usage patterns and activity.",()->openNative("com.samsung.android.sm.battery.ui.setting.AdaptiveBatteryMenuActivity"));space(main,22);
        item(main,"Battery health",null,this::health);
        space(body,18);
        LinearLayout assistant=card();LinearLayout ar=row();Symbol ai=new Symbol(4,WHITE);ar.addView(ai,new LayoutParams(dp(35),dp(38)));LinearLayout at=col();LayoutParams ap=new LayoutParams(0,-2,1);ap.leftMargin=dp(14);ar.addView(at,ap);add(at,text("AI Battery Assistant",18,WHITE,false),-2);space(at,6);TextView info=text("Battery insights and recommendations. Analysis not yet available.",14,MUTED,false);info.setLineSpacing(dp(3),1);add(at,info,-2);ar.addView(chevron(),new LayoutParams(dp(20),dp(32)));add(assistant,ar,-2);tap(ar,()->dialog("AI Battery Assistant","The animated analysis screen is not implemented in this visual integration build. No usage predictions are generated."));add(body,assistant,-2);
        space(body,26);section(body,"Battery usage");
        LinearLayout usage=card();LinearLayout tabs=row();tabs.setBackground(shape(TILE,22));tabs.setPadding(dp(4),dp(4),dp(4),dp(4));String[] ranges={"Today","Last 7 days"};for(int i=0;i<2;i++){final int ix=i;TextView t=text(ranges[i],14,WHITE,false);t.setGravity(Gravity.CENTER);if(i==0)t.setBackground(shape(0xff555555,18));tabs.addView(t,new LayoutParams(0,dp(36),1));tap(t,()->openNative(ix==0?"com.samsung.android.sm.battery.ui.graph.LastChargeGraphActivity":"com.samsung.android.sm.battery.ui.graph.DailyUsageGraphActivity"));}add(usage,tabs,-2);space(usage,16);add(usage,text("View real battery usage in Samsung's graphs",14,MUTED,false),-2);space(usage,14);item(usage,"Open original battery statistics",null,original);add(body,usage,-2);
        space(body,22);section(body,"Battery settings");LinearLayout settings=card();item(settings,"Power saving settings",null,()->openNative("com.samsung.android.sm.powermode.ui.PowerModeSettingsActivity"));space(settings,16);line(settings);space(settings,16);item(settings,"Charging settings",null,()->openNative("com.samsung.android.sm.battery.ui.BatteryAdvancedMenuActivity"));add(body,settings,-2);
        ready=true;refresh();
        body.setAlpha(0.65f);body.setTranslationY(dp(12));body.animate().alpha(1).translationY(0).setDuration(350).setInterpolator(new PathInterpolator(.2f,.8f,.2f,1f)).start();
    }
    private TextView chevron(){TextView t=text("›",27,0xff696969,false);t.setGravity(Gravity.CENTER);return t;}
    private void item(LinearLayout parent,String name,String desc,Runnable action){LinearLayout r=row();LinearLayout t=col();add(t,text(name,18,WHITE,false),-2);if(desc!=null){space(t,5);TextView d=text(desc,14,MUTED,false);d.setLineSpacing(dp(3),1);add(t,d,-2);}r.addView(t,new LayoutParams(0,-2,1));r.addView(chevron(),new LayoutParams(dp(22),dp(36)));add(parent,r,-2);tap(r,action);}
    private void openNative(String cls){try{Intent i=new Intent().setClassName(a.getPackageName(),cls);a.startActivity(i);}catch(Throwable t){BatteryHook.log("native screen unavailable: "+cls+": "+t);original.run();Toast.makeText(a,"Use the original Samsung settings.",Toast.LENGTH_SHORT).show();}}
    private void dialog(String title,String msg){new AlertDialog.Builder(a).setTitle(title).setMessage(msg).setPositiveButton(android.R.string.ok,null).show();}
    private void health(){refresh();String t="Unavailable",v="Unavailable",h="Unavailable";if(battery!=null){int raw=battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,Integer.MIN_VALUE);if(raw!=Integer.MIN_VALUE)t=String.format(Locale.US,"%.1f °C",raw/10f);int mv=battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE,-1);if(mv>0)v=String.format(Locale.US,"%.2f V",mv/1000f);int status=battery.getIntExtra(BatteryManager.EXTRA_HEALTH,0);if(status==BatteryManager.BATTERY_HEALTH_GOOD)h="Good";else if(status==BatteryManager.BATTERY_HEALTH_OVERHEAT)h="Overheating";else if(status==BatteryManager.BATTERY_HEALTH_COLD)h="Cold";else if(status==BatteryManager.BATTERY_HEALTH_DEAD)h="Dead";else if(status==BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE)h="Over voltage";else if(status==BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE)h="Reported failure";}
        new AlertDialog.Builder(a).setTitle("Battery health").setMessage("Maximum capacity: Not exposed by this build\n\nHealth status: "+h+"\nTemperature: "+t+"\nVoltage: "+v).setNegativeButton("Close",null).setPositiveButton("Battery protection",(d,w)->openNative("com.samsung.android.sm.battery.ui.protection.BatteryProtectionActivity")).show();}
    void refresh(){try{battery=a.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));int percent=-1;String state="Battery status unavailable";if(battery!=null){int l=battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),s=battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1);if(l>=0&&s>0&&l<=s)percent=Math.round(100f*l/s);int status=battery.getIntExtra(BatteryManager.EXTRA_STATUS,0);switch(status){case BatteryManager.BATTERY_STATUS_FULL:state="Fully charged";break;case BatteryManager.BATTERY_STATUS_CHARGING:state="⚡ Charging";try{BatteryManager bm=(BatteryManager)a.getSystemService(Context.BATTERY_SERVICE);long ms=bm.computeChargeTimeRemaining();if(ms>0){long min=Math.max(1,(ms+59999)/60000);state+=" | "+(min/60>0?min/60+" h ":"")+(min%60)+" min until full";}}catch(Throwable ignore){}break;case BatteryManager.BATTERY_STATUS_DISCHARGING:state="Not charging";break;case BatteryManager.BATTERY_STATUS_NOT_CHARGING:state="Not charging";break;}}
       number.setText(percent<0?"—":String.valueOf(percent));charge.setText(state);bar.setLevel(percent);
       if(ready){showMode(engine.activeMode());updateEstimates();}
       }catch(Throwable t){BatteryHook.log("battery refresh: "+t);}}
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
        boolean changed=which!=selected;selected=which;
        boolean saving=engine.isSaving();
        syncingSwitch=true;superSwitch.setChecked(saving);syncingSwitch=false;
        for(int j=0;j<3;j++){
            final int k=j;boolean chosen=j==which;
            modes[j].setSelected(chosen);modeLabels[j].setTextColor(chosen?GREEN:WHITE);modeIcons[j].color=chosen?GREEN:WHITE;modeIcons[j].invalidate();modeNotes[j].setTextColor(chosen?GREEN:MUTED);
            if(changed){
                if(tileAnimations[j]!=null)tileAnimations[j].cancel();
                ValueAnimator anim=ValueAnimator.ofFloat(glass[j].amount,chosen?1f:0f);anim.setDuration(430);anim.setInterpolator(new PathInterpolator(.18f,.75f,.2f,1f));anim.addUpdateListener(v->{glass[k].amount=(float)v.getAnimatedValue();glass[k].invalidateSelf();});tileAnimations[j]=anim;anim.start();
                if(chosen){modes[j].animate().cancel();modes[j].setScaleX(.96f);modes[j].setScaleY(.96f);modes[j].animate().scaleX(1).scaleY(1).setDuration(380).setInterpolator(new android.view.animation.OvershootInterpolator(.75f)).start();}
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
            for(int j=0;j<3;j++)modeNotes[j].setText(values[j]);
            superNote.setText(values[1].contains("min")||values[1].contains(" h")?"Can last about "+values[1]:"Uses Samsung Power saving");
        });
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();try{IntentFilter filter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);if(android.os.Build.VERSION.SDK_INT>=33)a.registerReceiver(receiver,filter,Context.RECEIVER_EXPORTED);else a.registerReceiver(receiver,filter);registered=true;}catch(Throwable t){BatteryHook.log("receiver: "+t);}try{a.getContentResolver().registerContentObserver(Settings.Global.getUriFor("low_power"),false,modeObserver);observing=true;a.getContentResolver().registerContentObserver(Settings.Global.getUriFor("sem_low_heat_mode"),false,modeObserver);}catch(Throwable t){BatteryHook.log("observer: "+t);}handler.removeCallbacks(tick);handler.post(tick);post(()->{applySafeInsets(getRootWindowInsets());requestApplyInsets();});}
    @Override protected void onDetachedFromWindow(){handler.removeCallbacks(tick);if(observing){try{a.getContentResolver().unregisterContentObserver(modeObserver);}catch(Throwable ignored){}observing=false;}if(registered){try{a.unregisterReceiver(receiver);}catch(Throwable ignored){}registered=false;}for(ValueAnimator an:tileAnimations)if(an!=null)an.cancel();bar.stop();engine.close();super.onDetachedFromWindow();}
    private final class Glass extends Drawable {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final RectF box=new RectF();float amount;
        @Override public void draw(Canvas canvas){
            box.set(getBounds());float radius=dp(21);
            p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(TILE);canvas.drawRoundRect(box,radius,radius,p);
            if(amount<=.001f)return;
            int alpha=(int)(amount*255);
            int top=(alpha<<24)|0x244b24,bottom=(alpha<<24)|0x14241c;
            p.setShader(new LinearGradient(0,box.top,box.width()*.65f,box.bottom,new int[]{top,bottom},null,Shader.TileMode.CLAMP));canvas.drawRoundRect(box,radius,radius,p);
            p.setShader(new RadialGradient(box.centerX(),box.top,Math.max(1,box.height()*.9f),new int[]{((int)(amount*72)<<24)|0x75f17d,0x00000000},null,Shader.TileMode.CLAMP));canvas.drawRoundRect(box,radius,radius,p);
            p.setShader(new LinearGradient(box.left,box.top,box.right,box.bottom,new int[]{((int)(205*amount)<<24)|0x75ef79,((int)(85*amount)<<24)|0x39a94a,((int)(175*amount)<<24)|0x7bea78},new float[]{0,.55f,1},Shader.TileMode.CLAMP));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.3f));box.inset(dp(1),dp(1));canvas.drawRoundRect(box,radius,radius,p);
            p.setShader(null);p.setStyle(Paint.Style.FILL);
        }
        @Override public void setAlpha(int a){} @Override public void setColorFilter(ColorFilter f){} @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private final class Gauge extends View{private final Paint p=new Paint(3);private float level=0,wanted=-1;private ValueAnimator motion;Gauge(){super(a);}void stop(){if(motion!=null)motion.cancel();}void setLevel(int value){float target=Math.max(0,value)/100f;if(wanted==target)return;wanted=target;stop();motion=ValueAnimator.ofFloat(level,target);motion.setDuration(650);motion.setInterpolator(new PathInterpolator(.16f,.85f,.2f,1f));motion.addUpdateListener(v->{level=(float)v.getAnimatedValue();invalidate();});motion.start();setContentDescription(value<0?"Battery level unavailable":"Battery level "+value+" percent");}
       @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setColor(TILE);c.drawRoundRect(0,0,w,h,h*.39f,h*.39f,p);if(level>0){c.save();android.graphics.Path path=new android.graphics.Path();path.addRoundRect(new RectF(0,0,w,h),h*.39f,h*.39f,android.graphics.Path.Direction.CW);c.clipPath(path);p.setColor(BLUE);c.drawRoundRect(0,0,w*level,h,h*.39f,h*.39f,p);c.restore();}}}
    private final class Symbol extends View{final int kind;int color;final Paint p=new Paint(3);Symbol(int k,int tint){super(a);kind=k;color=tint;}
       @Override protected void onDraw(Canvas c){super.onDraw(c);float scale=Math.min(getWidth(),getHeight())/48f;c.save();c.translate((getWidth()-48*scale)/2,(getHeight()-48*scale)/2);c.scale(scale,scale);p.setColor(color);p.setStrokeWidth(3);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);if(kind==0){c.drawLine(12,24,35,24,p);c.drawLine(12,24,23,13,p);c.drawLine(12,24,23,35,p);}else if(kind==1||kind==2||kind==4){c.drawRoundRect(6,12,39,36,5,5,p);c.drawLine(43,19,43,29,p);p.setStyle(Paint.Style.FILL);if(kind==1)c.drawRoundRect(10,16,23,32,2,2,p);else if(kind==2){c.drawOval(13,17,32,28,p);p.setColor(TILE);p.setStrokeWidth(2);c.drawLine(13,31,27,21,p);}else{android.graphics.Path s=new android.graphics.Path();s.moveTo(23,11);s.lineTo(27,20);s.lineTo(36,24);s.lineTo(27,28);s.lineTo(23,37);s.lineTo(19,28);s.lineTo(10,24);s.lineTo(19,20);s.close();c.drawPath(s,p);}}else{c.drawArc(7,7,41,41,145,250,false,p);c.drawLine(24,24,32,13,p);p.setStyle(Paint.Style.FILL);c.drawCircle(24,24,3,p);}c.restore();}
    }
}
