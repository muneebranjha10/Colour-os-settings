package com.galaxy.batteryhook;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.PathInterpolator;
import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;

/** ColorOS reference fill, based on source recording: no permanent animation at rest. */
final class LiquidBatteryBar extends View {
    private final BooleanSupplier quiet;
    private final IntUnaryOperator duration;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect=new RectF();
    private final Matrix matrix=new Matrix();
    private LinearGradient reflection;
    private ValueAnimator fillAnimator,colorAnimator,sweepAnimator;
    private int style=0,oldColor=0xff22ce37,newColor=0xff22ce37;
    private boolean glint=false,flow=false,valid;
    private float displayed,target=-1,colorMix=1f,sweep=1f;

    LiquidBatteryBar(Activity a,BooleanSupplier quiet,IntUnaryOperator duration){
        super(a);this.quiet=quiet;this.duration=duration;
    }
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    void configure(int style,boolean glint,boolean flow){
        this.style=ReleaseRules.clampOption(style);this.glint=glint;this.flow=flow;
        if(!glint&&!flow&&sweepAnimator!=null){sweepAnimator.cancel();sweep=1f;}
        invalidate();
    }
    void refreshEffects(){if(!canAnimate())stopAnimations();invalidate();}
    private boolean canAnimate(){return isShown()&&hasWindowFocus()&&!quiet.getAsBoolean()&&ValueAnimator.areAnimatorsEnabled();}
    private void stopAnimations(){
        if(fillAnimator!=null){fillAnimator.cancel();fillAnimator=null;}
        if(colorAnimator!=null){colorAnimator.cancel();colorAnimator=null;}
        if(sweepAnimator!=null){sweepAnimator.cancel();sweepAnimator=null;}
    }
    void stop(){stopAnimations();}
    @Override protected void onDetachedFromWindow(){stopAnimations();super.onDetachedFromWindow();}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        reflection=new LinearGradient(0,0,dp(74),0,
            new int[]{0x00ffffff,0x0cffffff,0x55ffffff,0x00ffffff},
            new float[]{0,.32f,.59f,1},Shader.TileMode.CLAMP);
    }
    private void oneShotReflection(){
        if(style==2||(!glint&&!flow)||!canAnimate())return;
        if(sweepAnimator!=null)sweepAnimator.cancel();
        sweep=0f;
        sweepAnimator=ValueAnimator.ofFloat(0f,1f);
        sweepAnimator.setDuration(duration.applyAsInt(560));
        sweepAnimator.setInterpolator(new PathInterpolator(.24f,.58f,.26f,1f));
        sweepAnimator.addUpdateListener(v->{sweep=(float)v.getAnimatedValue();invalidate();});
        sweepAnimator.start();
    }
    void setLevel(int level,boolean plugged,boolean saving){
        int nextColor=ReleaseRules.batteryBarColor(plugged,saving);
        if(level<0||level>100){
            stopAnimations();valid=false;target=displayed=0f;
            setContentDescription("Battery level unavailable");invalidate();return;
        }
        boolean first=!valid;valid=true;float next=level/100f;boolean enabled=canAnimate();
        boolean changed=target!=next;
        if(first||!enabled){
            if(fillAnimator!=null)fillAnimator.cancel();
            target=displayed=next;
        }else if(changed){
            if(fillAnimator!=null)fillAnimator.cancel();
            float from=displayed;target=next;
            fillAnimator=ValueAnimator.ofFloat(from,next);
            fillAnimator.setDuration(duration.applyAsInt(580));
            fillAnimator.setInterpolator(new PathInterpolator(.16f,.76f,.21f,1f));
            fillAnimator.addUpdateListener(v->{displayed=(float)v.getAnimatedValue();invalidate();});
            fillAnimator.start();
        }
        boolean colorChanged=newColor!=nextColor;
        if(first||!enabled){
            if(colorAnimator!=null)colorAnimator.cancel();
            oldColor=newColor=nextColor;colorMix=1f;
        }else if(colorChanged){
            if(colorAnimator!=null)colorAnimator.cancel();
            oldColor=ColorOsPalette.mix(oldColor,newColor,colorMix);
            newColor=nextColor;colorMix=0f;
            colorAnimator=ValueAnimator.ofFloat(0f,1f);
            colorAnimator.setDuration(duration.applyAsInt(620));
            colorAnimator.setInterpolator(new PathInterpolator(.18f,.74f,.24f,1f));
            colorAnimator.addUpdateListener(v->{colorMix=(float)v.getAnimatedValue();invalidate();});
            colorAnimator.start();
        }
        if((colorChanged&&glint)||(changed&&flow&&!first))oneShotReflection();
        setContentDescription("Battery "+level+" percent"+(saving?", power saving active":"")+(plugged?", charging":""));
        invalidate();
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
        float radius=Math.min(h*.32f,dp(10));
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff2c2c2c);
        canvas.drawRoundRect(0,0,w,h,radius,radius,paint);
        if(!valid||displayed<=0)return;
        float x=w*Math.max(0,Math.min(1f,displayed));
        canvas.save();canvas.clipRect(0,0,x,h);
        paint.setColor(ColorOsPalette.mix(oldColor,newColor,colorMix));
        canvas.drawRoundRect(0,0,w,h,radius,radius,paint);
        if(style==1){
            // Polished alternative: subtle specular rim, never the old translucent wave.
            paint.setColor(0x14ffffff);
            rect.set(dp(1),dp(.8f),w-dp(1),h*.13f);
            canvas.drawRoundRect(rect,dp(2),dp(2),paint);
        }
        if(style!=2&&sweep<1f&&reflection!=null){
            matrix.setTranslate((w+dp(100))*sweep-dp(90),0);
            reflection.setLocalMatrix(matrix);
            paint.setShader(reflection);
            canvas.drawRoundRect(0,0,w,h,radius,radius,paint);
            paint.setShader(null);
        }
        canvas.restore();
    }
}
