package com.galaxy.batteryhook;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
final class WindowBars {
    static void bind(Window w,View root){if(w==null)return;root.setOnApplyWindowInsetsListener((v,i)->{pad(w,v,i);return i;});root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(t!=ot||b!=ob)pad(w,v,v.getRootWindowInsets());});}
    static void schedule(Window w,View root){if(w==null)return;apply(w,root);root.postDelayed(()->apply(w,root),100);root.postDelayed(()->apply(w,root),350);root.postDelayed(()->apply(w,root),850);}
    static void apply(Window w,View root){if(!root.isAttachedToWindow()||!root.hasWindowFocus())return;try{
        w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS|WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        View decor=w.getDecorView();int remove=View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LOW_PROFILE|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        decor.setSystemUiVisibility(decor.getSystemUiVisibility()&~remove);
        w.setStatusBarColor(Color.BLACK);w.setNavigationBarColor(Color.BLACK);
        if(Build.VERSION.SDK_INT>=30){w.setDecorFitsSystemWindows(true);WindowInsetsController c=w.getInsetsController();if(c!=null){c.setSystemBarsAppearance(0,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);c.show(WindowInsets.Type.systemBars());}}
        pad(w,root,root.getRootWindowInsets());root.requestApplyInsets();
    }catch(Throwable t){BatteryHook.log("Window bars: "+t);}}
    static void pad(Window w,View root,WindowInsets i){if(i==null||Build.VERSION.SDK_INT<30||root.getHeight()==0)return;try{
        Insets safe=i.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());int[] pos=new int[2],decorPos=new int[2];root.getLocationOnScreen(pos);w.getDecorView().getLocationOnScreen(decorPos);
        int top=Math.max(0,safe.top-Math.max(0,pos[1]-decorPos[1]));int bottom=Math.max(0,safe.bottom-Math.max(0,w.getDecorView().getHeight()-(pos[1]-decorPos[1])-root.getHeight()));
        if(root.getPaddingTop()!=top||root.getPaddingBottom()!=bottom)root.setPadding(0,top,0,bottom);
    }catch(Throwable ignored){}}
}
