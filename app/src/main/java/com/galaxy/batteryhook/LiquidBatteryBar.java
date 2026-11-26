package com.galaxy.batteryhook;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.PathInterpolator;
import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;

final class LiquidBatteryBar extends View {
    private final BooleanSupplier quiet;
    private final IntUnaryOperator duration;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path rounded=new Path(),wave=new Path();
    private final RectF rect=new RectF();
    private final Matrix matrix=new Matrix();
    private LinearGradient shine;
    private ValueAnimator fillAnim,colorAnim,flashAnim;
    private int style=1,from=0xff17db26,to=0xff17db26;
    private boolean glint=true,flow=true,plugged,valid;
    private float level,target=-1,blend=1f,flash=1f;
    private boolean queued;
    private final Runnable frame=new Runnable(){
        @Override public void run(){queued=false;if(!running())return;invalidate();queue();}
    };
    LiquidBatteryBar(Activity activity,BooleanSupplier quiet,IntUnaryOperator duration){
        super(activity);this.quiet=quiet;this.duration=duration;
    }
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private boolean running(){return isShown()&&hasWindowFocus()&&!quiet.getAsBoolean()&&ValueAnimator.areAnimatorsEnabled()&&plugged&&flow&&style!=2&&valid&&level>0;}
    private void queue(){if(!queued&&running()){queued=true;postDelayed(frame,45);}}
    private void cancelFrame(){removeCallbacks(frame);queued=false;}
    void configure(int style,boolean glint,boolean flow){
        this.style=ReleaseRules.clampOption(style);this.glint=glint;this.flow=flow;
        cancelFrame();invalidate();queue();
    }
    void refreshEffects(){if(quiet.getAsBoolean())cancelFrame();else queue();invalidate();}
    void stop(){cancelFrame();if(fillAnim!=null)fillAnim.cancel();if(colorAnim!=null)colorAnim.cancel();if(flashAnim!=null)flashAnim.cancel();}
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();queue();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)queue();else cancelFrame();}
    @Override protected void onWindowVisibilityChanged(int state){super.onWindowVisibilityChanged(state);if(state==VISIBLE)queue();else cancelFrame();}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        shine=new LinearGradient(0,0,dp(88),0,new int[]{0x00ffffff,0x15ffffff,0x87ffffff,0x20ffffff,0x00ffffff},
            new float[]{0,.22f,.5f,.78f,1},Shader.TileMode.CLAMP);
    }
    void setLevel(int percent,boolean charging,boolean saving){
        plugged=charging;int newColor=ReleaseRules.batteryBarColor(charging,saving);boolean fresh=!valid;
        if(percent<0||percent>100){valid=false;level=target=0;stop();invalidate();setContentDescription("Battery level unavailable");return;}
        valid=true;float aim=percent/100f;
        boolean animate=ValueAnimator.areAnimatorsEnabled()&&!quiet.getAsBoolean();
        if(fresh||!animate){if(fillAnim!=null)fillAnim.cancel();level=target=aim;}
        else if(target!=aim){if(fillAnim!=null)fillAnim.cancel();float origin=level;target=aim;
            fillAnim=ValueAnimator.ofFloat(origin,aim);fillAnim.setDuration(duration.applyAsInt(680));
            fillAnim.setInterpolator(new PathInterpolator(.18f,.77f,.23f,1f));
            fillAnim.addUpdateListener(v->{level=(float)v.getAnimatedValue();invalidate();});fillAnim.start();
        }
        if(fresh||!animate){if(colorAnim!=null)colorAnim.cancel();from=to=newColor;blend=1;}
        else if(to!=newColor){
            if(colorAnim!=null)colorAnim.cancel();
            from=ColorOsPalette.mix(from,to,blend);to=newColor;blend=0;
            colorAnim=ValueAnimator.ofFloat(0,1);colorAnim.setDuration(duration.applyAsInt(720));
            colorAnim.setInterpolator(new PathInterpolator(.19f,.78f,.18f,1f));
            colorAnim.addUpdateListener(v->{blend=(float)v.getAnimatedValue();invalidate();});colorAnim.start();
            if(flashAnim!=null)flashAnim.cancel();
            flash=0;flashAnim=ValueAnimator.ofFloat(0,1);flashAnim.setDuration(duration.applyAsInt(650));
            flashAnim.addUpdateListener(v->{flash=(float)v.getAnimatedValue();invalidate();});flashAnim.start();
        }
        setContentDescription("Battery "+percent+" percent"+(saving?", power saving":"")+(charging?", charger connected":""));
        if(running())queue();else cancelFrame();invalidate();
    }
    @Override protected void onDraw(Canvas c){
        float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
        float r=h*.42f;p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(0xff292929);
        c.drawRoundRect(0,0,w,h,r,r,p);
        if(!valid||level<=0)return;
        float fill=w*Math.max(0,Math.min(1,level));
        rect.set(0,0,w,h);rounded.reset();rounded.addRoundRect(rect,r,r,Path.Direction.CW);
        c.save();c.clipPath(rounded);c.clipRect(0,0,fill,h);
        int color=ColorOsPalette.mix(from,to,blend);
        p.setColor(color);c.drawRect(0,0,fill,h,p);
        if(style!=2){
            p.setColor(ColorOsPalette.mix(color,Color.WHITE,style==0?.20f:.12f));
            c.drawRect(0,0,fill,h*.3f,p);
            p.setColor(0x22000000);c.drawRect(0,h*.77f,fill,h,p);
        }else{p.setColor(0x16ffffff);c.drawRect(0,0,fill,h*.2f,p);}
        if(running()){
            long time=SystemClock.uptimeMillis();
            if(style==1){
                float phase=(time%3200L)/3200f*6.2831853f;
                wave.reset();wave.moveTo(0,h*.32f);
                for(int i=0;i<=30;i++){float x=fill*i/30f;wave.lineTo(x,h*(.29f+.075f*(float)Math.sin(x/Math.max(dp(35),w)*15f-phase)));}
                wave.lineTo(fill,h*.58f);wave.lineTo(0,h*.58f);wave.close();
                p.setColor(0x2effffff);c.drawPath(wave,p);
            }
            if(glint&&shine!=null){
                float phase=(time%4100L)/4100f;
                matrix.setTranslate((w+dp(170))*phase-dp(100),0);
                shine.setLocalMatrix(matrix);p.setShader(shine);c.drawRect(0,0,fill,h,p);p.setShader(null);
            }
            p.setColor(0x55ffffff);
            c.drawCircle(Math.max(dp(5),fill-dp(5)),h*.45f,Math.min(dp(5),h*.17f),p);
        }
        c.restore();
        if(flash<1f){
            p.setColor(Color.argb(Math.round(100*(float)Math.sin(Math.PI*flash)),255,255,255));
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.2f));
            rect.set(dp(1),dp(1),w-dp(1),h-dp(1));c.drawRoundRect(rect,r,r,p);p.setStyle(Paint.Style.FILL);
        }
    }
}
