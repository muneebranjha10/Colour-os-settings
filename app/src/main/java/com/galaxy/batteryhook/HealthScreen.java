package com.galaxy.batteryhook;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import java.util.Locale;

final class HealthScreen {
    private final Activity a;
    private final Runnable protection,onClosed;
    private final int duration;private final boolean quiet;private final java.util.function.Consumer<Boolean> parallax;
    private final Typeface font;
    private Dialog dialog;
    private LinearLayout root,page,details;private View headerView;private ScrollView scrollView;private boolean closing,closed,loading;private Runnable afterClose;private android.window.OnBackInvokedCallback backCallback;
    private Gauge gauge;
    private TextView source,summary;
    private HealthReader.Reading reading;
    private int generation;
    private ValueAnimator animation;
    private static final int WHITE=0xffeeeeee,MUTE=0xffa5a5a5,GREEN=0xff36d54b,CARD=0xff1b1b1b;
    HealthScreen(Activity a,Runnable protection,int duration,boolean quiet,Runnable onClosed,java.util.function.Consumer<Boolean> parallax){this.a=a;this.protection=protection;this.duration=duration;this.quiet=quiet;this.onClosed=onClosed;this.parallax=parallax;font=new TextView(a).getTypeface();}
    private int dp(float n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    private LinearLayout col(){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String title,int sp,int color,boolean bold){TextView t=new TextView(a);t.setText(title);t.setTextColor(color);t.setTextSize(sp);t.setTypeface(Typeface.create(font==null?Typeface.DEFAULT:font,bold?600:400,false));return t;}
    private GradientDrawable bg(int color,int radius){GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(dp(radius));return b;}
    private void add(LinearLayout l,View v,int height){l.addView(v,new LinearLayout.LayoutParams(-1,height<0?height:dp(height)));}
    private void gap(LinearLayout l,int h){add(l,new View(a),h);}
    private LinearLayout card(){LinearLayout c=col();c.setBackground(bg(CARD,23));c.setPadding(dp(18),dp(14),dp(18),dp(14));return c;}
    private void action(LinearLayout parent,String title,String sub,Runnable event){LinearLayout r=col();add(r,text(title,17,WHITE,false),-2);if(sub!=null)add(r,text(sub,13,MUTE,false),-2);r.setPadding(0,dp(10),0,dp(10));r.setClickable(true);r.setFocusable(true);MotionUi.ripple(r);r.setOnClickListener(v->{if(!closing)event.run();});add(parent,r,-2);}
    void show(){
        dialog=new Dialog(a,android.R.style.Theme_Material_NoActionBar){
            @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus&&root!=null&&!closing)WindowBars.schedule(getWindow(),root);}
            @Override public void onBackPressed(){close(null);}
        };dialog.setCanceledOnTouchOutside(false);
        root=col();root.setBackgroundColor(Color.TRANSPARENT);page=col();page.setBackgroundColor(Color.BLACK);root.addView(page,new LinearLayout.LayoutParams(-1,0,1));page.setAlpha(0f);
        LinearLayout header=new LinearLayout(a);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(18),dp(6),dp(18),dp(6));
        TextView back=text("‹",30,WHITE,true);back.setGravity(Gravity.CENTER);back.setBackground(bg(0xff292929,24));back.setContentDescription("Back");MotionUi.ripple(back);back.setOnClickListener(v->close(null));header.addView(back,new LinearLayout.LayoutParams(dp(41),dp(41)));
        TextView title=text("Battery health",21,WHITE,true);LinearLayout.LayoutParams tl=new LinearLayout.LayoutParams(0,-2,1);tl.leftMargin=dp(12);header.addView(title,tl);
        TextView refresh=text("↻",23,WHITE,false);refresh.setGravity(Gravity.CENTER);refresh.setContentDescription("Refresh readings");MotionUi.ripple(refresh);refresh.setOnClickListener(v->{if(!closing&&!loading){if(!quiet&&MotionUi.enabled())refresh.animate().rotationBy(180).setDuration(Math.max(120,duration/2)).start();load();}});header.addView(refresh,new LinearLayout.LayoutParams(dp(42),dp(42)));headerView=header;add(page,header,56);
        ScrollView scroll=new ScrollView(a);scroll.setVerticalScrollBarEnabled(false);scrollView=scroll;page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout content=col();content.setPadding(dp(20),dp(12),dp(20),dp(24));scroll.addView(content);
        gauge=new Gauge();add(content,gauge,198);source=text("Reading battery health…",13,MUTE,false);source.setGravity(Gravity.CENTER);add(content,source,-2);gap(content,20);
        details=card();add(content,details,-2);gap(content,17);
        LinearLayout care=card();action(care,"Battery protection","Open Samsung’s original charging protection settings",()->close(protection));action(care,"Import root diagnostic","Only needed when live readings are restricted",this::importReading);add(content,care,-2);
        gap(content,15);summary=text("Battery health is a firmware estimate, not a laboratory capacity test.",13,MUTE,false);add(content,summary,-2);
        dialog.setContentView(root);
        dialog.setOnDismissListener(v->finishDismiss());
        Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setWindowAnimations(0);}
        dialog.show();
        if(w!=null){w.setLayout(-1,-1);WindowBars.bind(w,root);WindowBars.schedule(w,root);}
        if(android.os.Build.VERSION.SDK_INT>=33){try{backCallback=()->close(null);dialog.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY,backCallback);}catch(Throwable ignored){backCallback=null;}}
        parallax.accept(false);
        page.post(()->{if(!closing&&!closed&&dialog.isShowing())MotionUi.enter(page,duration,quiet,dp(54),0);});
        load();
    }
    private void close(Runnable next){
        if(closing||closed)return;closing=true;afterClose=next;generation++;if(animation!=null)animation.cancel();parallax.accept(true);
        MotionUi.exit(page,Math.max(100,Math.round(duration*.78f)),quiet,dp(64),0,()->{if(dialog!=null&&dialog.isShowing())dialog.dismiss();else finishDismiss();});
    }
    void closeNow(){if(closed)return;closing=true;afterClose=null;if(dialog!=null&&dialog.isShowing())dialog.dismiss();else finishDismiss();}
    private void finishDismiss(){if(closed)return;closed=true;generation++;if(animation!=null)animation.cancel();if(page!=null)page.animate().cancel();if(backCallback!=null&&android.os.Build.VERSION.SDK_INT>=33){try{dialog.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);}catch(Throwable ignored){}backCallback=null;}onClosed.run();Runnable next=afterClose;afterClose=null;if(next!=null&&!a.isFinishing()&&!a.isDestroyed())next.run();}
    private void load(){
        if(closing||loading)return;loading=true;final int token=++generation;source.setText("Reading battery health…");
        new Thread(()->{HealthReader.Reading r=HealthReader.read(a);a.runOnUiThread(()->{if(token==generation&&dialog.isShowing()&&!closing){loading=false;reading=r;render();}});},"BatteryHealthRead").start();
    }
    private void field(String label,String value){
        LinearLayout r=new LinearLayout(a);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(0,dp(11),0,dp(11));
        TextView key=text(label,14,WHITE,false);key.setPadding(0,0,dp(12),0);r.addView(key,new LinearLayout.LayoutParams(0,-2,1));TextView v=text(value,14,MUTE,false);v.setGravity(Gravity.END);v.setMaxWidth(dp(150));r.addView(v,new LinearLayout.LayoutParams(-2,-2));add(details,r,-2);
        View line=new View(a);line.setBackgroundColor(0xff303030);details.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
    }
    private String value(int v,String units){return v<0?"Unavailable":v+units;}
    private String temp(int v){return v<0?"Unavailable":String.format(Locale.US,"%.1f °C",v/10f);}
    private String status(int v){switch(v){case BatteryManager.BATTERY_HEALTH_GOOD:return "Good";case BatteryManager.BATTERY_HEALTH_OVERHEAT:return "Overheating";case BatteryManager.BATTERY_HEALTH_COLD:return "Cold";case BatteryManager.BATTERY_HEALTH_DEAD:return "Dead";case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:return "Over voltage";case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:return "Reported failure";default:return "Unavailable";}}
    private void render(){
        HealthReader.Reading r=reading;int target=r.main();gauge.label=r.mainLabel();gauge.value=target;gauge.setContentDescription(gauge.label+" "+(target<0?"unavailable":target+" percent"));
        if(animation!=null)animation.cancel();if(!MotionUi.enabled()){gauge.progress=target<0?0:target/100f;gauge.invalidate();}else{animation=ValueAnimator.ofFloat(gauge.progress,target<0?0:target/100f);animation.setDuration(quiet?180:Math.round(duration*1.55f));animation.setInterpolator(new android.view.animation.DecelerateInterpolator(1.8f));animation.addUpdateListener(v->{gauge.progress=(float)v.getAnimatedValue();gauge.invalidate();});animation.start();}
        String src=r.asoc>=0?r.asocSource:r.api>=0?"Android battery property 10":r.bsoh>=0?r.bsohSource:"No readable health source";
        source.setText(src+(r.imported?" · manual snapshot":""));
        boolean initial=details.getChildCount()==0;details.removeAllViews();field("Fuel gauge ASOC",value(r.asoc,"%"));field("Saved Samsung BSOH",value(r.bsoh,"%"));if(r.api>=0)field("Android SOH",value(r.api,"%"));field("Battery condition",status(r.status));field("Charge cycles",value(r.cycles,""));if(r.cycles>=0)field("Cycle count source",r.cycleSource);field("Design capacity",r.design<0?"Unavailable":r.design/1000+" mAh");field("Reported full capacity",r.full<0?"Unavailable":r.full/1000+" mAh");field("Temperature",temp(r.temp));field("Highest recorded temperature",temp(r.maxTemp));field("Voltage",r.mv<=0?"Unavailable":String.format(Locale.US,"%.2f V",r.mv/1000f));
        if(initial&&!closing)MotionUi.enter(details,Math.max(120,duration),quiet,0,dp(12));
        summary.setText((r.temp>=420?"Battery is warm. Let it cool before prolonged fast charging.\n\n":"")+"ASOC and BSOH are separate estimates. Unavailable readings are never filled with guessed percentages.");
    }
    private void importReading(){
        EditText edit=new EditText(a);edit.setHint("fg_asoc=…\nmSavedBatteryBsoh: …");edit.setMinLines(4);edit.setGravity(Gravity.TOP);edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        AlertDialog importDialog=new AlertDialog.Builder(a).setTitle("Import diagnostic snapshot").setMessage("Paste the filtered Termux output. It will be labeled as a manual snapshot, not a live reading. No root permission is requested by this app.").setView(edit).setNegativeButton("Cancel",null).setPositiveButton("Import",(d,w)->{
            HealthReader.Reading parsed=new HealthReader.Reading();HealthReader.parseDump(parsed,edit.getText().toString(),"Imported diagnostic");
            HealthReader.resolveCycles(parsed);if(parsed.asoc<0&&parsed.bsoh<0&&parsed.cycles<0){Toast.makeText(a,"No valid health or cycle field found",Toast.LENGTH_LONG).show();return;}
            generation++;loading=false;if(reading==null)reading=new HealthReader.Reading();if(parsed.asoc>=0){reading.asoc=parsed.asoc;reading.asocSource=parsed.asocSource;}if(parsed.bsoh>=0){reading.bsoh=parsed.bsoh;reading.bsohSource=parsed.bsohSource;}if(parsed.maxTemp>=0)reading.maxTemp=parsed.maxTemp;if(parsed.cycles>=0){reading.cycles=parsed.cycles;reading.cycleSource="Manual snapshot · "+parsed.cycleSource;}reading.imported=true;render();
        }).create();importDialog.show();MotionUi.panel(importDialog,Math.max(120,duration/2),quiet);
    }
    private final class Gauge extends View{
        final Paint p=new Paint(3);final RectF rect=new RectF();final Typeface bold=Typeface.create(font==null?Typeface.DEFAULT:font,600,false);int value=-1;float progress;String label="Reading…";
        Gauge(){super(a);}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight(),r=Math.min(w*.34f,h*.65f),cx=w/2,cy=h*.74f;rect.set(cx-r,cy-r,cx+r,cy+r);
            p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(dp(13));p.setColor(0xff292929);c.drawArc(rect,180,180,false,p);
            if(progress>0){p.setColor(GREEN);c.drawArc(rect,180,180*progress,false,p);}
            p.setStyle(Paint.Style.FILL);p.setTypeface(bold);p.setTextAlign(Paint.Align.CENTER);p.setColor(WHITE);p.setTextSize(dp(47));c.drawText(value<0?"—":value+"%",cx,cy-dp(12),p);
            p.setTypeface(font);p.setTextSize(dp(12));p.setColor(value<0?MUTE:GREEN);c.drawText(label,cx,cy+dp(13),p);

        }
    }
}
