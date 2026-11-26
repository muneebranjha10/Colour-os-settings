package com.galaxy.batteryhook;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.Toast;

final class ProtectionCard extends LinearLayout {
    private static final String MODE="protect_battery",LIMIT="battery_protection_threshold";
    private static final int WHITE=0xfff2f2f2,MUTE=0xffaaaaaa,GREEN=0xff45d45a;
    private final Activity a; private final Runnable nativePage;
    private final Switch enabled;private final TextView basic,max,desc,status;private final GlassLimit slider;private final GlassRail modeRail;private final java.util.function.BooleanSupplier haptics;
    private final LinearLayout choices;private boolean binding,observing,dragging;
    private int remembered=1;
    private final ContentObserver observer=new ContentObserver(new Handler(Looper.getMainLooper())){@Override public void onChange(boolean self){refresh();}};
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color,int radius,boolean active){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(active)d.setStroke(dp(1),GREEN);return d;}
    private TextView text(String label,int size,int color){TextView t=new TextView(a);t.setText(label);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create(FontStore.get(a),400,false));return t;}
    private void add(LinearLayout p,View v){p.addView(v,new LayoutParams(-1,-2));}
    private void gap(LinearLayout p,int h){p.addView(new View(a),new LayoutParams(1,dp(h)));}
    ProtectionCard(Activity activity,Runnable nativePage,java.util.function.IntSupplier style,java.util.function.BooleanSupplier quiet,java.util.function.IntUnaryOperator duration,java.util.function.BooleanSupplier haptics){
        super(activity);a=activity;this.nativePage=nativePage;this.haptics=haptics;setOrientation(VERTICAL);setBackground(shape(0xff1b1b1b,25,false));setPadding(dp(18),dp(16),dp(18),dp(16));
        LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);TextView name=text("Battery protection",18,WHITE);head.addView(name,new LayoutParams(0,-2,1));enabled=new Switch(a);enabled.setThumbTintList(ColorStateList.valueOf(WHITE));enabled.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xff43af54,0xff555555}));enabled.setContentDescription("Battery protection");head.addView(enabled,new LayoutParams(-2,-2));add(this,head);
        status=text("",13,MUTE);add(this,status);gap(this,14);
        choices=new LinearLayout(a);choices.setOrientation(VERTICAL);
        LinearLayout modes=new LinearLayout(a);basic=text("Basic",15,WHITE);max=text("Maximum",15,WHITE);
        for(TextView t:new TextView[]{basic,max}){t.setGravity(Gravity.CENTER);t.setPadding(dp(4),dp(14),dp(4),dp(14));LayoutParams p=new LayoutParams(0,-2,1);if(t==max)p.leftMargin=dp(8);modes.addView(t,p);}
        android.widget.FrameLayout modeHost=new android.widget.FrameLayout(a);modeRail=new GlassRail(a,new View[]{basic,max},style,quiet,duration,true);modeHost.addView(modeRail,new android.widget.FrameLayout.LayoutParams(-1,-1));modeHost.addView(modes,new android.widget.FrameLayout.LayoutParams(-1,-1));add(choices,modeHost);modeHost.getLayoutParams().height=dp(48);gap(choices,10);desc=text("",13,MUTE);add(choices,desc);gap(choices,10);
        slider=new GlassLimit(a,style,quiet,duration);add(choices,slider);slider.getLayoutParams().height=dp(48);
        LinearLayout labels=new LinearLayout(a);for(int i=0;i<4;i++){final int n=i;TextView t=text((80+i*5)+"%",12,MUTE);t.setGravity(Gravity.CENTER);labels.addView(t,new LayoutParams(0,-2,1));t.setOnClickListener(v->{if(readMode()==1){setLimit(80+5*n);}});MotionUi.ripple(t);}add(choices,labels);add(this,choices);gap(this,12);
        TextView more=text("Original Samsung protection settings  ›",14,WHITE);more.setPadding(0,dp(8),0,dp(4));more.setOnClickListener(v->nativePage.run());add(this,more);
        enabled.setOnCheckedChangeListener((b,value)->{if(binding)return;int current=readMode();if(current<0||current==4){refresh();nativePage.run();return;}if(!value&&current>0)remembered=current;setMode(value?remembered:0);});
        basic.setOnClickListener(v->setMode(3));max.setOnClickListener(v->setMode(1));MotionUi.glassTouch(basic,15);MotionUi.glassTouch(max,15);MotionUi.ripple(more);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onStartTrackingTouch(SeekBar b){dragging=true;slider.beginDrag();}
            @Override public void onProgressChanged(SeekBar b,int progress,boolean fromUser){if(fromUser&&!binding){int limit=GlassMath.limitForProgress(progress);slider.preview(progress);status.setText("Maximum protection · preview "+limit+"%");desc.setText("Release to set "+limit+"%");if(!dragging&&readMode()==1)setLimit(limit);}}
            @Override public void onStopTrackingTouch(SeekBar b){dragging=false;slider.endDrag();if(!slider.cancelled()&&readMode()==1)setLimit(GlassMath.limitForProgress(b.getProgress()));else refresh();}
        });refresh();
    }
    private int get(String key){try{return Settings.Global.getInt(a.getContentResolver(),key,-1);}catch(Throwable t){return -1;}}
    private int readMode(){return get(MODE);}
    private boolean validLimit(int n){return n>=80&&n<=95&&n%5==0;}
    private boolean supported(int mode,int limit){return ReleaseRules.supportsProtection(android.os.Build.VERSION.SDK_INT,mode,limit);}
    private boolean put(String key,int value){try{return Settings.Global.putInt(a.getContentResolver(),key,value)&&get(key)==value;}catch(Throwable t){BatteryHook.log("Protection setting unavailable: "+key+" "+t.getClass().getSimpleName());return false;}}
    private void setMode(int mode){int old=readMode(),limit=get(LIMIT);if(!supported(old,limit)){nativePage.run();return;}if(mode!=0&&mode!=1&&mode!=3)return;if(mode==old){refresh();return;}if(!put(MODE,mode))fail();else{if(mode>0)remembered=mode;refresh();}}
    private void setLimit(int limit){if(readMode()!=1||!validLimit(limit)||!validLimit(get(LIMIT))){refresh();return;}if(get(LIMIT)==limit){refresh();return;}if(!put(LIMIT,limit))fail();else{if(haptics.getAsBoolean())slider.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);refresh();}}
    private void fail(){refresh();Toast.makeText(a,"Samsung did not confirm the setting. Use original protection settings.",Toast.LENGTH_LONG).show();}
    boolean interacting(){return dragging;}
    void appearanceChanged(){slider.invalidate();modeRail.invalidate();}
    void refresh(){
        if(dragging)return;int mode=readMode(),limit=get(LIMIT);boolean supported=supported(mode,limit);binding=true;
        if(mode==1||mode==3)remembered=mode;
        enabled.setChecked(mode>0);enabled.setEnabled(supported);basic.setEnabled(supported);max.setEnabled(supported);
        modeRail.settle(mode==3?0:mode==1?1:-1);basic.setTextColor(WHITE);max.setTextColor(WHITE);basic.setSelected(mode==3);max.setSelected(mode==1);
        choices.setAlpha(supported?1f:.45f);slider.setEnabled(supported&&mode==1);
        slider.setVisibility(validLimit(limit)?VISIBLE:GONE);
        if(validLimit(limit))slider.snapTo(GlassMath.progressForLimit(limit));
        status.setText(!supported?(mode==4?"Sleep time protection · use original settings":"Use original settings on this firmware"):mode==0?"Off":mode==3?"Basic protection":"Maximum protection · "+limit+"%");
        if(!dragging)desc.setText(mode==3?"Stops at 100%; resumes at 95%.":mode==1?"Stop charging at "+limit+"%":"Enable protection or choose a mode.");
        binding=false;
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();try{a.getContentResolver().registerContentObserver(Settings.Global.getUriFor(MODE),false,observer);observing=true;a.getContentResolver().registerContentObserver(Settings.Global.getUriFor(LIMIT),false,observer);}catch(Throwable ignored){}refresh();}
    @Override protected void onDetachedFromWindow(){dispose();super.onDetachedFromWindow();}
    void dispose(){slider.stop();modeRail.stop();if(observing){try{a.getContentResolver().unregisterContentObserver(observer);}catch(Throwable ignored){}observing=false;}}
}
