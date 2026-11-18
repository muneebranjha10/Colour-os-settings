package com.galaxy.batteryhook;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

final class BetaWelcome extends LinearLayout {
    private final Activity a;
    private boolean opening;
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String label,int sp,int color,boolean bold){
        TextView v=new TextView(a);v.setText(label);v.setTextSize(sp);v.setTextColor(color);
        v.setTypeface(Typeface.create(v.getTypeface(),bold?650:400,false));v.setGravity(Gravity.CENTER);return v;
    }
    private void gap(LinearLayout parent,int h){parent.addView(new View(a),new LayoutParams(1,dp(h)));}
    private void add(LinearLayout parent,View view){parent.addView(view,new LayoutParams(-1,-2));}
    BetaWelcome(Activity a,BetaConsent consent,Runnable open){
        super(a);this.a=a;setOrientation(VERTICAL);setBackgroundColor(0xff000000);setClickable(true);setFocusableInTouchMode(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        WindowBars.bind(a.getWindow(),this);
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        addView(scroll,new LayoutParams(-1,0,1));
        LinearLayout content=new LinearLayout(a);content.setOrientation(VERTICAL);content.setGravity(Gravity.CENTER);
        content.setPadding(dp(24),dp(32),dp(24),dp(28));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        View icon=new View(a){
            final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final RectF r=new RectF();
            @Override protected void onDraw(Canvas c){float s=Math.min(getWidth(),getHeight())/96f;c.save();c.translate((getWidth()-96*s)/2,(getHeight()-96*s)/2);c.scale(s,s);
                p.setStyle(Paint.Style.FILL);p.setColor(0xff162a1c);c.drawRoundRect(2,2,94,94,25,25,p);
                p.setColor(0xff345c3c);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);c.drawRoundRect(3,3,93,93,24,24,p);
                p.setColor(0xff82e493);p.setStrokeWidth(3);r.set(21,32,71,63);c.drawRoundRect(r,7,7,p);p.setStyle(Paint.Style.FILL);c.drawRoundRect(75,41,79,54,2,2,p);
                p.setColor(0xff43ce57);c.drawRoundRect(26,37,55,58,3,3,p);c.restore();}
        };content.addView(icon,new LayoutParams(dp(96),dp(96)));gap(content,22);
        TextView badge=text("PUBLIC BETA  ·  1",12,0xff9be7a4,true);badge.setBackground(shape(0xff1c3020,18));badge.setPadding(dp(14),dp(7),dp(14),dp(7));content.addView(badge,new LayoutParams(-2,-2));gap(content,15);
        TextView title=text("Welcome to\nColourOS Battery",30,0xfff2f2f2,true);add(content,title);gap(content,12);
        TextView intro=text("You're using an experimental beta version.",16,0xffd2d2d2,false);add(content,intro);gap(content,22);
        LinearLayout warning=new LinearLayout(a);warning.setOrientation(VERTICAL);warning.setPadding(dp(18),dp(16),dp(18),dp(16));warning.setBackground(shape(0xff1b1b1b,22));
        TextView label=text("Before you continue",17,0xffffce83,true);label.setGravity(Gravity.START);add(warning,label);gap(warning,9);
        TextView body=text("This beta may contain bugs, inaccurate battery readings, incomplete device compatibility or unexpected behavior. Some controls may not work correctly on your Samsung firmware.\n\nIf you experience problems, disable the module in LSPosed and reboot. Please report bugs with your device model and One UI version.",14,0xffc3c3c3,false);body.setGravity(Gravity.START);body.setLineSpacing(dp(3),1);add(warning,body);add(content,warning);gap(content,24);
        TextView button=text("Continue",16,0xff08200b,true);button.setBackground(shape(0xff46d657,24));button.setMinHeight(dp(54));button.setPadding(dp(16),dp(12),dp(16),dp(12));MotionUi.ripple(button);button.setClickable(true);button.setFocusable(true);add(content,button);
        button.setOnClickListener(v->{if(opening)return;opening=true;button.setEnabled(false);
            if(!consent.accept()){opening=false;button.setEnabled(true);Toast.makeText(a,"Couldn't save your acknowledgment. Please try again.",Toast.LENGTH_SHORT).show();return;}
            try{open.run();}catch(Throwable t){opening=false;button.setEnabled(true);BatteryHook.log("Beta dashboard: "+t);Toast.makeText(a,"Couldn't open the battery page. Try again or go back.",Toast.LENGTH_LONG).show();}
        });
        gap(content,14);add(content,text("This notice is shown only once after you continue.",12,0xff909090,false));gap(content,24);add(content,text("Made by Muneeb",13,0xffa5a5a5,false));
    }
    void ensureBars(){if(isAttachedToWindow()&&hasWindowFocus())WindowBars.schedule(a.getWindow(),this);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)ensureBars();}
}
