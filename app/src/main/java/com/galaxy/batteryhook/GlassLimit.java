package com.galaxy.batteryhook;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.PathInterpolator;
import android.widget.SeekBar;
import java.util.function.*;

final class GlassLimit extends SeekBar {
    private final IntSupplier style;private final BooleanSupplier quiet;private final IntUnaryOperator duration;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final RectF rect=new RectF();private final Matrix matrix=new Matrix();
    private final GradientDrawable invisibleThumb;private Shader track,fill,lens,edge,specular;
    private int shaderStyle=-1;private float visual=0,target=-1,press=0;private boolean cancelled,tracking;private ValueAnimator motion,pressAnim;
    GlassLimit(Activity a,IntSupplier style,BooleanSupplier quiet,IntUnaryOperator duration){
        super(a);this.style=style;this.quiet=quiet;this.duration=duration;
        setMax(GlassMath.MAX_PROGRESS);setKeyProgressIncrement(100);setSplitTrack(false);
        setPadding(dp(15),0,dp(15),0);setProgressDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        invisibleThumb=new GradientDrawable();invisibleThumb.setColor(Color.TRANSPARENT);invisibleThumb.setSize(dp(24),dp(24));setThumb(invisibleThumb);setThumbOffset(dp(12));setBackground(null);setContentDescription("Maximum battery charge limit");
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    boolean cancelled(){return cancelled;}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN)cancelled=false;if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)cancelled=true;return super.onTouchEvent(e);}
    void beginDrag(){tracking=true;stopMotion();preview(getProgress());pressTo(1);}
    void endDrag(){tracking=false;pressTo(0);}
    void preview(int n){stopMotion();visual=GlassMath.clamp(n,0,GlassMath.MAX_PROGRESS);postInvalidateOnAnimation();}
    void snapTo(int n){
        n=Math.round(GlassMath.clamp(n,0,GlassMath.MAX_PROGRESS));setProgress(n);
        if(tracking)return;
        if(target==n&&motion!=null&&motion.isRunning())return;
        if(Math.abs(visual-n)<.01f||quiet.getAsBoolean()||!MotionUi.enabled()){stopMotion();visual=n;target=n;invalidate();return;}
        stopMotion();target=n;final int finish=n;motion=ValueAnimator.ofFloat(visual,n);motion.setDuration(duration.applyAsInt(210));motion.setInterpolator(new PathInterpolator(.15f,.82f,.23f,1));motion.addUpdateListener(v->{visual=(float)v.getAnimatedValue();postInvalidateOnAnimation();});motion.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){visual=finish;invalidate();}});motion.start();
    }
    private void stopMotion(){if(motion!=null){motion.removeAllListeners();motion.cancel();motion=null;}target=-1;}
    void stop(){stopMotion();if(pressAnim!=null){pressAnim.cancel();pressAnim=null;}tracking=false;press=0;}
    private void pressTo(float amount){
        if(pressAnim!=null)pressAnim.cancel();if(quiet.getAsBoolean()||!MotionUi.enabled()){press=0;invalidate();return;}
        pressAnim=ValueAnimator.ofFloat(press,amount);pressAnim.setDuration(duration.applyAsInt(160));pressAnim.setInterpolator(new PathInterpolator(.2f,.8f,.2f,1));pressAnim.addUpdateListener(v->{press=(float)v.getAnimatedValue();postInvalidateOnAnimation();});pressAnim.start();
    }
    private void shaders(){int v=style.getAsInt();if(shaderStyle==v)return;shaderStyle=v;
        track=new LinearGradient(0,-1,0,1,0xff414944,0xff26332c,Shader.TileMode.CLAMP);
        fill=new LinearGradient(0,-1,0,1,0xffa4f7b2,0xff268b4c,Shader.TileMode.CLAMP);
        lens=new LinearGradient(0,-1,0,1,new int[]{0xffeaffee,0xff8adca6,0xff36774d,0xff86daa1},new float[]{0,.28f,.70f,1},Shader.TileMode.CLAMP);
        edge=new LinearGradient(0,-1,0,1,0xfff0fff3,0xff4b9962,Shader.TileMode.CLAMP);
        specular=new RadialGradient(-.15f,-.55f,1,new int[]{0xbfffffff,0x00ffffff},null,Shader.TileMode.CLAMP);
    }
    private void map(Shader shader,float sx,float sy,float x,float y){matrix.reset();matrix.setScale(sx,sy);matrix.postTranslate(x,y);shader.setLocalMatrix(matrix);p.setShader(shader);}
    @Override protected synchronized void onDraw(Canvas c){
        float left=getPaddingLeft(),right=getWidth()-getPaddingRight(),cy=getHeight()*.5f;if(right<=left)return;
        boolean rtl=getLayoutDirection()==LAYOUT_DIRECTION_RTL;float x=left+(right-left)*GlassMath.thumbFraction(visual,rtl);shaders();float half=dp(3);
        p.setStyle(Paint.Style.FILL);p.setShader(null);p.setAlpha(255);map(track,1,half,0,cy);rect.set(left,cy-half,right,cy+half);c.drawRoundRect(rect,half,half,p);
        float start=rtl?right:left;rect.set(Math.min(x,start),cy-half,Math.max(x,start),cy+half);map(fill,1,half,0,cy);c.drawRoundRect(rect,half,half,p);
        p.setShader(null);p.setColor(0xffabb3ad);for(int i=0;i<4;i++){float dot=left+(right-left)*GlassMath.thumbFraction(i*100,rtl);c.drawCircle(dot,cy,dp(2),p);}
        float rx=dp(10)+dp(8)*press,ry=dp(10)+dp(3)*press;
        if(!quiet.getAsBoolean()){p.setColor(0x3a85f5a0);c.drawRoundRect(x-rx-dp(3),cy-ry-dp(3),x+rx+dp(3),cy+ry+dp(3),ry+dp(3),ry+dp(3),p);}
        rect.set(x-rx,cy-ry,x+rx,cy+ry);p.setColor(Color.WHITE);map(lens,rx,ry,x,cy);c.drawRoundRect(rect,ry,ry,p);
        if(!quiet.getAsBoolean()){map(specular,rx,ry,x,cy);c.drawRoundRect(rect,ry,ry,p);}
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));map(edge,rx,ry,x,cy);c.drawRoundRect(rect,ry,ry,p);p.setStyle(Paint.Style.FILL);p.setShader(null);
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT,80,95,GlassMath.limitForProgress(getProgress())));}
    @Override public boolean performAccessibilityAction(int action,Bundle args){
        if(action==android.R.id.accessibilityActionSetProgress&&args!=null&&isEnabled()){
            float n=args.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,GlassMath.limitForProgress(getProgress()));int limit=GlassMath.limitForProgress(Math.round((n-80)*20));
            Bundle mapped=new Bundle();mapped.putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,GlassMath.progressForLimit(limit));return super.performAccessibilityAction(action,mapped);
        }
        return super.performAccessibilityAction(action,args);
    }
}
