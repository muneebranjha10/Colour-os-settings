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
    interface MotionListener{void position(float slot);}
    private MotionListener listener;
    void setMotionListener(MotionListener l){listener=l;publish();}
    private void publish(){if(listener!=null)listener.position(slot);}
    private void frame(){publish();postInvalidateOnAnimation();}
    float slot=-1;private float head=-1,tail=-1,target=-1,energy;private boolean dragging;private ValueAnimator anim;
    GlassRail(Activity a,View[] tiles,IntSupplier style,BooleanSupplier quiet,IntUnaryOperator duration,boolean compact){
        super(a);this.tiles=tiles;this.style=style;this.quiet=quiet;this.duration=duration;this.compact=compact;setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private float dp(float x){return x*getResources().getDisplayMetrics().density;}
    private float clamp(float x){return GlassMath.clamp(x,0,tiles.length-1);}
    void stop(){if(anim!=null){anim.removeAllListeners();anim.cancel();anim=null;}target=-1;dragging=false;}
    void beginDrag(){stop();dragging=true;}
    void preview(float value){
        float next=clamp(value),previous=slot;slot=next;head=next;
        tail=previous<0?next:GlassMath.clamp(next+(next-previous)*.35f,Math.max(0,next-.16f),Math.min(tiles.length-1,next+.16f));
        energy=Math.min(1,Math.abs(next-previous)*10+.35f);frame();
    }
    void settle(int next){
        dragging=false;
        if(next<0||next>=tiles.length){stop();slot=head=tail=-1;energy=0;frame();return;}
        if(target==next&&anim!=null&&anim.isRunning())return;
        if(slot<0||quiet.getAsBoolean()||!MotionUi.enabled()||(Math.abs(head-next)<.001f&&Math.abs(tail-next)<.001f)){stop();slot=head=tail=target=next;energy=0;frame();return;}
        stop();target=next;final float startHead=head,startTail=tail,startSlot=slot,startEnergy=energy;
        anim=ValueAnimator.ofFloat(0f,1f);anim.setInterpolator(new LinearInterpolator());anim.setDuration(duration.applyAsInt(Math.abs(next-startSlot)>1.1f?490:360));
        anim.addUpdateListener(v->{float t=(float)v.getAnimatedValue();head=clamp(GlassMath.between(startHead,next,GlassMath.railHead(t)));tail=clamp(GlassMath.between(startTail,next,GlassMath.railTail(t)));slot=clamp(GlassMath.between(startSlot,next,GlassMath.railHead(t)));energy=Math.max(startEnergy*(1-t),(float)Math.sin(Math.PI*t));frame();});
        anim.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){slot=head=tail=next;energy=0;frame();}});anim.start();
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
        float movement=Math.min(1,Math.abs(head-tail)),lift=quiet.getAsBoolean()?0:Math.min(dp(3),h*.055f)*energy;
        top+=lift;bottom-=lift;h=bottom-top;float radius=Math.min(dp(compact?15:23),h*.43f),span=right-left,mid=(left+right)*.5f,neck=movement*h*.15f;float k=Math.min(span*.22f,dp(24));radius=Math.min(radius,span*.45f);
        liquid.reset();liquid.moveTo(left+radius,top);
        liquid.cubicTo(left+radius+k,top,mid-k*.5f,top+neck,mid,top+neck);
        liquid.cubicTo(mid+k*.5f,top+neck,right-radius-k,top,right-radius,top);
        liquid.quadTo(right,top,right,top+radius);liquid.lineTo(right,bottom-radius);liquid.quadTo(right,bottom,right-radius,bottom);
        liquid.cubicTo(right-radius-k,bottom,mid+k*.5f,bottom-neck,mid,bottom-neck);
        liquid.cubicTo(mid-k*.5f,bottom-neck,left+radius+k,bottom,left+radius,bottom);
        liquid.quadTo(left,bottom,left,bottom-radius);liquid.lineTo(left,top+radius);liquid.quadTo(left,top,left+radius,top);liquid.close();
        // Cache at the fixed card height, not the animated height.
        colors(tiles[0].getTop(),tiles[0].getHeight());
        paint.setShader(body);canvas.drawPath(liquid,paint);
        if(!quiet.getAsBoolean()){
            paint.setShader(shine);canvas.drawPath(liquid,paint);

        }
        paint.setShader(edge);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(style.getAsInt()==2?1.25f:.9f));paint.setStrokeJoin(Paint.Join.ROUND);canvas.drawPath(liquid,paint);paint.setStyle(Paint.Style.FILL);paint.setShader(null);
        // Restrained ColorOS accent: only the short bottom highlight, not the entire glass body.
        if(!compact){float cx=xAt(slot)+widthAt(slot)*.5f;float half=Math.min(dp(9),widthAt(slot)*.12f);paint.setColor(ColorOsPalette.ACCENT);canvas.drawRoundRect(cx-half,bottom-dp(4),cx+half,bottom-dp(2),dp(1),dp(1),paint);}
    }
}
