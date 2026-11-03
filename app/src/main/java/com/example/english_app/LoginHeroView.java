package com.example.english_app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Draws the original layered hero (phone + 7 words + review) without image assets. */
public class LoginHeroView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private float phase = 0f;
    private ValueAnimator animator;
    private final int bg = Color.rgb(44,57,99), mint = Color.rgb(121,230,197);
    private final int yellow = Color.rgb(247,203,88), white = Color.rgb(249,250,255);
    private final int navy = Color.rgb(17,23,47);
    public LoginHeroView(Context c, AttributeSet a){super(c,a);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    private void fill(Canvas c,float l,float t,float rt,float b,float rad,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(l,t,rt,b,rad,rad,p);}
    private void stroke(Canvas c,float l,float t,float rt,float b,float rad,int color,float sw){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(sw);c.drawRoundRect(l,t,rt,b,rad,rad,p);p.setStyle(Paint.Style.FILL);}
    private void text(Canvas c,String s,float x,float y,int color,float size,boolean bold, boolean center){p.setColor(color);p.setTextSize(size);p.setTypeface(bold?android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD):android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL)); p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(s,x,y,p);}
    private void star(Canvas c,float cx,float cy,float radius,int color){Path path=new Path();path.moveTo(cx,cy-radius);path.quadTo(cx+radius*.18f,cy-radius*.18f,cx+radius,cy);path.quadTo(cx+radius*.18f,cy+radius*.18f,cx,cy+radius);path.quadTo(cx-radius*.18f,cy+radius*.18f,cx-radius,cy);path.quadTo(cx-radius*.18f,cy-radius*.18f,cx,cy-radius);path.close();p.setColor(color);c.drawPath(path,p);}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float w=getWidth(),h=getHeight();float sx=w/342f,sy=h/268f;canvas.save();canvas.scale(sx,sy);
        fill(canvas,1,1,341,267,27,bg);stroke(canvas,1,1,341,267,27,0xff5b709c,1);
        p.setColor(0xff4357a0);canvas.drawCircle(172,137,91,p);
        // main phone
        float bounce=(float)Math.sin(phase*2*Math.PI)*2.4f;
        fill(canvas,98,17+bounce,244,262+bounce,32,navy);stroke(canvas,98,17+bounce,244,262+bounce,32,mint,2.4f);
        fill(canvas,144,23+bounce,198,35+bounce,10,0xff080d21);
        text(canvas,"✦ DAILY FLOW",171,61+bounce,mint,12,true,true);
        fill(canvas,116,82+bounce,225,91+bounce,6,0xff3b4c77);
        fill(canvas,116,82+bounce,183+(float)Math.sin(phase*2*Math.PI)*2,91+bounce,6,yellow);
        p.setColor(yellow);canvas.drawCircle(171,141+bounce,36,p);star(canvas,171,141+bounce,16,navy);
        text(canvas,"BÀI 01",171,205+bounce,white,18,true,true);
        text(canvas,"Học từ · Nghe · Viết",171,225+bounce,0xffb9c3de,10,false,true);
        // outer chips float slightly with phased animation
        float leftY=(float)Math.sin(phase*2*Math.PI+1)*2;
        fill(canvas,9,112+leftY,132,157+leftY,23,white);
        text(canvas,"✓  7 từ mới",71,140+leftY,0xff263050,14,true,true);
        float rightY=(float)Math.sin(phase*2*Math.PI+2.7)*2;
        fill(canvas,225,132+rightY,335,177+rightY,17,mint);
        text(canvas,"✓  Ôn tập",280,159+rightY,0xff183e40,14,true,true);
        canvas.restore();
    }
    public void startMotion(){if(Settings.Global.getFloat(getContext().getContentResolver(),"animator_duration_scale",1f)==0f)return;if(animator!=null)return;animator=ValueAnimator.ofFloat(0,1);animator.setDuration(3800);animator.setRepeatCount(ValueAnimator.INFINITE);animator.setInterpolator(new LinearInterpolator());animator.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});animator.start();}
    public void stopMotion(){if(animator!=null){animator.cancel();animator=null;}}
    @Override protected void onDetachedFromWindow(){stopMotion();super.onDetachedFromWindow();}
}
