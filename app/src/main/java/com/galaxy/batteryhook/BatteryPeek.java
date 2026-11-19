package com.galaxy.batteryhook;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;

// Inline, read-only battery details. Uses the dashboard's existing samples.
final class BatteryPeek extends LinearLayout {
    private static final int TEXT=0xffeeeeee,MUTE=0xffa9a9a9;
    private final LinearLayout panel;
    private final TextView status,level,temp,voltage,current,stored,power,source;
    private final IntUnaryOperator duration;private final BooleanSupplier quiet;
    private ValueAnimator animation;private boolean open;
    BatteryPeek(Activity a,Typeface font,IntUnaryOperator duration,BooleanSupplier quiet){
        super(a);this.duration=duration;this.quiet=quiet;setOrientation(VERTICAL);setClipChildren(true);setVisibility(GONE);
        panel=new LinearLayout(a);panel.setOrientation(VERTICAL);panel.setPadding(dp(16),dp(16),dp(16),dp(16));
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xff262d29,0xff171a18});bg.setCornerRadius(dp(22));bg.setStroke(Math.max(1,dp(.7f)),0xff48534a);panel.setBackground(bg);
        LayoutParams lp=new LayoutParams(-1,-2);lp.topMargin=dp(8);addView(panel,lp);
        status=label(a,font,"Battery details",16,TEXT,true);panel.addView(status);gap(12);
        LinearLayout first=row(a);level=metric(a,font,first,"Level");temp=metric(a,font,first,"Temperature");panel.addView(first);gap(12);
        LinearLayout second=row(a);voltage=metric(a,font,second,"Voltage");current=metric(a,font,second,"Current");panel.addView(second);gap(12);
        LinearLayout third=row(a);stored=metric(a,font,third,"Charge stored");power=metric(a,font,third,"Battery power");panel.addView(third);gap(12);
        source=label(a,font,"Battery-side readings",12,MUTE,false);panel.addView(source);
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(Activity a,Typeface f,String s,int sp,int color,boolean bold){TextView v=new TextView(a);v.setText(s);v.setTextSize(sp);v.setTextColor(color);v.setTypeface(Typeface.create(f,bold?650:400,false));return v;}
    private LinearLayout row(Activity a){LinearLayout l=new LinearLayout(a);l.setGravity(Gravity.TOP);return l;}
    private void gap(int n){panel.addView(new View(getContext()),new LayoutParams(1,dp(n)));}
    private TextView metric(Activity a,Typeface f,LinearLayout row,String name){LinearLayout c=new LinearLayout(a);c.setOrientation(VERTICAL);c.setPadding(0,0,dp(6),0);c.addView(label(a,f,name,12,MUTE,false));TextView v=label(a,f,"—",18,TEXT,true);c.addView(v);row.addView(c,new LayoutParams(0,-2,1));return v;}
    private void set(TextView v,String s){if(!s.contentEquals(v.getText()))v.setText(s);}
    void update(ChargeTelemetry.Reading r,int accent,boolean saving){
        set(status,r.full?"Fully charged":r.charging&&!r.discharging?"Charging":r.plugged?"Connected · not charging":saving?"Power saving":"Battery details");status.setTextColor(accent);
        set(level,r.percent>=0&&r.percent<=100?r.percent+"%":"—");
        set(temp,r.temp!=Integer.MIN_VALUE?String.format(Locale.US,"%.1f °C",r.temp/10f):"—");
        set(voltage,r.mv>0?String.format(Locale.US,"%.2f V",r.mv/1000f):"—");
        set(current,Double.isFinite(r.effectiveUa)?String.format(Locale.US,"%s%.2f A",r.estimated?"≈ ":"",r.effectiveUa/1000000d):r.ua!=Integer.MIN_VALUE&&Math.abs((long)r.ua)<=15000000?String.format(Locale.US,"%+.2f A",r.ua/1000000d):"—");
        set(stored,r.counter>0?String.format(Locale.US,"%.0f mAh",r.counter/1000d):"—");
        set(power,r.plugged&&r.wattsText!=null?(r.estimated?"≈ ":"")+r.wattsText:"—");
        set(source,r.plugged?r.source+" · Battery-side power, not charger output.":"Current direction depends on device firmware. Readings are from available battery sensors.");
    }
    boolean expanded(){return open;}
    private void cancel(){if(animation!=null){animation.removeAllListeners();animation.cancel();animation=null;}}
    boolean toggle(){
        int from=getVisibility()==GONE?0:getHeight();cancel();open=!open;
        if(open){setVisibility(VISIBLE);int width=getWidth();if(width<=0&&getParent() instanceof View)width=((View)getParent()).getWidth()-((View)getParent()).getPaddingLeft()-((View)getParent()).getPaddingRight();if(width<=0||quiet.getAsBoolean()||!MotionUi.enabled()){finish();return open;}
            panel.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));animateHeight(from,panel.getMeasuredHeight()+dp(8));
        }else{if(quiet.getAsBoolean()||!MotionUi.enabled()||from<=0)finish();else animateHeight(from,0);}
        return open;
    }
    private void height(int size){android.view.ViewGroup.LayoutParams lp=getLayoutParams();lp.height=size;setLayoutParams(lp);}
    private void animateHeight(int start,int end){
        height(start);panel.setPivotY(0);animation=ValueAnimator.ofInt(start,end);animation.setDuration(duration.applyAsInt(280));animation.setInterpolator(new PathInterpolator(.22f,.35f,.22f,1f));
        animation.addUpdateListener(v->{int h=(int)v.getAnimatedValue();height(h);float f=GlassMath.clamp(h/(float)Math.max(1,Math.max(start,end)),0,1);panel.setAlpha(f);panel.setScaleY(.96f+.04f*f);});
        animation.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){finish();}});animation.start();
    }
    private void finish(){height(-2);panel.setAlpha(1);panel.setScaleY(1);setVisibility(open?VISIBLE:GONE);}
    void stop(){cancel();finish();}
}
