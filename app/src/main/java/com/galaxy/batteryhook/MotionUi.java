package com.galaxy.batteryhook;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.Window;
import android.view.animation.PathInterpolator;

final class MotionUi {
    private static final PathInterpolator ENTER=new PathInterpolator(.18f,.82f,.22f,1f);
    private static final PathInterpolator EXIT=new PathInterpolator(.33f,0f,.65f,1f);
    static boolean enabled(){return ValueAnimator.areAnimatorsEnabled();}
    static void ripple(View v){if(v.getForeground()==null)v.setForeground(new RippleDrawable(ColorStateList.valueOf(0x28ffffff),null,new ColorDrawable(Color.WHITE)));}
    static void restore(View v){if(v==null)return;v.animate().cancel();v.setAlpha(1);v.setTranslationX(0);v.setTranslationY(0);v.setScaleX(1);v.setScaleY(1);}
    static void enter(View v,int duration,boolean quiet,float dx,float dy){
        if(v==null)return;restore(v);if(!enabled())return;
        v.setAlpha(0f);v.setTranslationX(quiet?0:dx);v.setTranslationY(quiet?0:dy);
        v.animate().alpha(1).translationX(0).translationY(0).setDuration(PageMotion.time(duration,quiet)).setInterpolator(ENTER).start();
    }
    static void exit(View v,int duration,boolean quiet,float dx,float dy,Runnable end){
        if(v==null||!v.isAttachedToWindow()||!enabled()){if(end!=null)end.run();return;}
        v.animate().cancel();v.animate().alpha(0).translationX(quiet?0:dx).translationY(quiet?0:dy).setDuration(PageMotion.time(duration,quiet)).setInterpolator(EXIT).withEndAction(end).start();
    }
    static void parallax(View v,int duration,boolean quiet,boolean returning){
        if(v==null)return;v.animate().cancel();if(quiet||!enabled()){restore(v);return;}
        float shift=PageMotion.parallax(v.getWidth(),returning);v.animate().translationX(shift).scaleX(returning?1:.985f).scaleY(returning?1:.985f).alpha(returning?1:.82f).setDuration(PageMotion.time(duration,false)).setInterpolator(ENTER).start();
    }
    static void panel(Dialog dialog,int duration,boolean quiet){
        if(!enabled())return;Window w=dialog.getWindow();if(w==null)return;View content=w.findViewById(android.R.id.content);if(content==null)return;
        content.setAlpha(.15f);if(!quiet){content.setScaleX(.96f);content.setScaleY(.96f);content.setTranslationY(10*content.getResources().getDisplayMetrics().density);}
        content.animate().alpha(1).scaleX(1).scaleY(1).translationY(0).setDuration(PageMotion.time(duration,quiet)).setInterpolator(ENTER).start();
    }
}
