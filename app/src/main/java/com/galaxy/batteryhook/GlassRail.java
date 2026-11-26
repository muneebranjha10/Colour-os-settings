package com.galaxy.batteryhook;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.function.*;

class GlassRail extends View {
    private final View[] tiles;private final IntSupplier style;private final BooleanSupplier quiet;private final IntUnaryOperator duration;private final boolean compact;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path liquid=new Path();private final RectF card=new RectF();
    private Shader body,edge,shine;private int cachedStyle=-1;private float cachedHeight=-1,cachedTop=Float.NaN;
    private LensOptics optics;private boolean opticsTried;private final float[] tileCenters=new float[3];
    private long dragTime;private boolean dragFramePosted;private float pulse;
    private boolean savingTint;private float savingMix;private ValueAnimator tintAnimation;
    interface MotionListener{void position(float slot);}
    private MotionListener listener;
    void setMotionListener(MotionListener l){listener=l;publish();}
    private void publish(){if(listener!=null)listener.position(slot);}
    private void frame(){publish();postInvalidateOnAnimation();}
    float slot=-1;private float head=-1,tail=-1,target=-1,energy;private boolean dragging;private ValueAnimator anim;
    private final Runnable dragFrame=()->{dragFramePosted=false;if(!dragging||!isAttachedToWindow())return;advanceDrag();frame();queueDrag();};
    GlassRail(Activity a,View[] tiles,IntSupplier style,BooleanSupplier quiet,IntUnaryOperator duration,boolean compact){
        super(a);this.tiles=tiles;this.style=style;this.quiet=quiet;this.duration=duration;this.compact=compact;setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private float dp(float x){return x*getResources().getDisplayMetrics().density;}
    private float clamp(float x){return GlassMath.clamp(x,0,tiles.length-1);}
    void stop(){removeCallbacks(dragFrame);dragFramePosted=false;if(anim!=null){anim.removeAllListeners();anim.cancel();anim=null;}target=-1;dragging=false;}
    // Tint only the power-mode rail, not charging or usage selectors.
    void setPowerSavingTint(boolean enabled){
        if(enabled==savingTint)return;savingTint=enabled;
        if(tintAnimation!=null){tintAnimation.cancel();tintAnimation=null;}
        float end=enabled?1f:0f;
        if(quiet.getAsBoolean()||!MotionUi.enabled()){savingMix=end;frame();return;}
        tintAnimation=ValueAnimator.ofFloat(savingMix,end);tintAnimation.setDuration(duration.applyAsInt(480));tintAnimation.setInterpolator(new android.view.animation.PathInterpolator(.2f,.75f,.2f,1f));
        tintAnimation.addUpdateListener(v->{savingMix=(float)v.getAnimatedValue();frame();});tintAnimation.start();
    }
    @Override protected void onDetachedFromWindow(){stop();if(tintAnimation!=null){tintAnimation.cancel();tintAnimation=null;}super.onDetachedFromWindow();}
    void beginDrag(){stop();dragging=true;dragTime=android.os.SystemClock.uptimeMillis();pulse=0;}
    private void advanceDrag(){
        long now=android.os.SystemClock.uptimeMillis();float elapsed=Math.max(1,Math.min(64,now-dragTime));dragTime=now;
        tail=GlassMath.dragFollower(tail,slot,elapsed);energy=GlassMath.between(energy,.35f,GlassMath.followFraction(elapsed));pulse=0;
    }
    private void queueDrag(){if(dragging&&!dragFramePosted&&(Math.abs(tail-slot)>.002f||Math.abs(energy-.35f)>.01f)){dragFramePosted=true;postOnAnimation(dragFrame);}}
    void preview(float value){
        float next=clamp(value);slot=head=next;if(tail<0)tail=next;
        if(quiet.getAsBoolean()||!MotionUi.enabled()){tail=next;energy=pulse=0;frame();return;}
        if(dragging){advanceDrag();queueDrag();}else{tail=next;}
        frame();
    }
    void settle(int next){
        dragging=false;
        if(next<0||next>=tiles.length){stop();slot=head=tail=-1;energy=pulse=0;frame();return;}
        if(target==next&&anim!=null&&anim.isRunning())return;
        if(slot<0||quiet.getAsBoolean()||!MotionUi.enabled()||(Math.abs(head-next)<.001f&&Math.abs(tail-next)<.001f&&energy<.01f&&Math.abs(pulse)<.01f)){stop();slot=head=tail=target=next;energy=pulse=0;frame();return;}
        stop();target=next;final float startHead=head,startTail=tail,startSlot=slot,startEnergy=energy,startPulse=pulse;
        anim=ValueAnimator.ofFloat(0f,1f);anim.setInterpolator(new LinearInterpolator());anim.setDuration(duration.applyAsInt(Math.abs(next-startSlot)>1.1f?560:440));
        anim.addUpdateListener(v->{float t=(float)v.getAnimatedValue();head=clamp(GlassMath.between(startHead,next,GlassMath.railHead(t)));tail=clamp(GlassMath.between(startTail,next,GlassMath.railTail(t)));slot=clamp(GlassMath.between(startSlot,next,GlassMath.railHead(t)));energy=Math.max(startEnergy*(1-t),(float)Math.sin(Math.PI*t));pulse=startPulse*(1-t)+GlassMath.elasticPulse(t);frame();});
        anim.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){slot=head=tail=next;energy=pulse=0;frame();}});anim.start();
    }
    private float xAt(float v){int i=Math.min(tiles.length-2,Math.max(0,(int)Math.floor(v)));return GlassMath.between(tiles[i].getLeft(),tiles[i+1].getLeft(),v-i);}
    private float widthAt(float v){int i=Math.min(tiles.length-2,Math.max(0,(int)Math.floor(v)));return GlassMath.between(tiles[i].getWidth(),tiles[i+1].getWidth(),v-i);}
    private void colors(float top,float h){
        int variant=style.getAsInt();if(cachedStyle==variant&&cachedHeight==h&&cachedTop==top)return;cachedStyle=variant;cachedHeight=h;cachedTop=top;
        int[] palette=ColorOsPalette.body(variant);
        body=new LinearGradient(0,top,0,top+Math.max(1,h),palette,null,Shader.TileMode.CLAMP);
        edge=new LinearGradient(0,top,0,top+Math.max(1,h),ColorOsPalette.edge(variant),new float[]{0,.28f,.68f,1},Shader.TileMode.CLAMP);
        shine=new LinearGradient(0,top,0,top+Math.max(1,h),ColorOsPalette.sheen(variant),new float[]{0,.13f,.43f,.74f,1},Shader.TileMode.CLAMP);

    }
    @Override protected void onDraw(Canvas canvas){
        if(tiles.length<2||tiles[0]==null||tiles[0].getWidth()<=0)return;
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(0xff292929);
        for(View t:tiles){card.set(t.getLeft(),t.getTop(),t.getRight(),t.getBottom());canvas.drawRoundRect(card,dp(compact?15:22),dp(compact?15:22),paint);}
        if(slot<0)return;
        float x1=xAt(head),x2=xAt(tail),w1=widthAt(head),w2=widthAt(tail);float left=Math.min(x1,x2)+dp(1),right=Math.max(x1+w1,x2+w2)-dp(1);
        float top=tiles[0].getTop()+dp(1),bottom=tiles[0].getBottom()-dp(1),h=bottom-top;if(h<=0||right<=left)return;
        float movement=Math.min(1,Math.abs(head-tail)),lift=quiet.getAsBoolean()?0:Math.min(dp(3),h*.055f)*(energy*.65f+pulse*.35f);
        top+=lift;bottom-=lift;h=bottom-top;float radius=Math.min(dp(compact?15:23),h*.43f),span=right-left,mid=(left+right)*.5f,neck=GlassMath.neckFraction(movement)*h;radius=Math.min(radius,span*.45f);float k=Math.max(0,Math.min((span-2*radius)*.33f,dp(24)));
        liquid.reset();if(movement<.002f){card.set(left,top,right,bottom);liquid.addRoundRect(card,radius,radius,Path.Direction.CW);}else{liquid.moveTo(left+radius,top);
        liquid.cubicTo(left+radius+k,top,mid-k*.5f,top+neck,mid,top+neck);
        liquid.cubicTo(mid+k*.5f,top+neck,right-radius-k,top,right-radius,top);
        liquid.quadTo(right,top,right,top+radius);liquid.lineTo(right,bottom-radius);liquid.quadTo(right,bottom,right-radius,bottom);
        liquid.cubicTo(right-radius-k,bottom,mid+k*.5f,bottom-neck,mid,bottom-neck);
        liquid.cubicTo(mid-k*.5f,bottom-neck,left+radius+k,bottom,left+radius,bottom);
        liquid.quadTo(left,bottom,left,bottom-radius);liquid.lineTo(left,top+radius);liquid.quadTo(left,top,left+radius,top);liquid.close();}
        // Cache at the fixed card height, not the animated height.
        colors(tiles[0].getTop(),tiles[0].getHeight());
        boolean optical=false;
        if(!quiet.getAsBoolean()&&android.os.Build.VERSION.SDK_INT>=33&&canvas.isHardwareAccelerated()){
            try{
                if(!opticsTried){opticsTried=true;optics=new LensOptics();}
                if(optics!=null){
                    for(int i=0;i<3;i++)tileCenters[i]=i<tiles.length?(tiles[i].getLeft()+tiles[i].getRight())*.5f:-100000f;
                    paint.setShader(optics.shader(body,left,top,right,bottom,tileCenters,tiles[0].getWidth()*.5f,dp(compact?15:22),dp(1),style.getAsInt(),energy));
                    canvas.drawPath(liquid,paint);optical=true;
                }
            }catch(Throwable e){optics=null;opticsTried=true;BatteryHook.log("Optical glass fallback: "+e.getClass().getSimpleName());}
        }
        if(!optical){paint.setShader(body);canvas.drawPath(liquid,paint);if(!quiet.getAsBoolean()){paint.setShader(shine);canvas.drawPath(liquid,paint);}}
        if(savingMix>.001f){
            paint.setShader(null);paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(Math.round(65*savingMix),255,199,39));canvas.drawPath(liquid,paint);
        }
        paint.setShader(edge);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(style.getAsInt()==2?1.25f:.9f));paint.setStrokeJoin(Paint.Join.ROUND);canvas.drawPath(liquid,paint);paint.setStyle(Paint.Style.FILL);paint.setShader(null);
        // Restrained ColorOS accent: only the short bottom highlight, not the entire glass body.
        if(!compact){float cx=xAt(slot)+widthAt(slot)*.5f;float half=Math.min(dp(9),widthAt(slot)*.12f);paint.setColor(ColorOsPalette.mix(ColorOsPalette.ACCENT,0xffffcc35,savingMix));canvas.drawRoundRect(cx-half,bottom-dp(4),cx+half,bottom-dp(2),dp(1),dp(1),paint);}
    }
}
