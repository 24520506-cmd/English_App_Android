package com.example.english_app;
import android.content.Context;import android.graphics.Canvas;import android.graphics.Color;import android.graphics.Paint;import android.util.AttributeSet;import android.view.View;import android.animation.ValueAnimator;import android.view.animation.LinearInterpolator;
public class StarfieldView extends View {
 private final Paint paint=new Paint(3); private float phase=0; private final float[] xs={.08f,.76f,.91f,.31f,.64f,.15f,.82f,.48f,.24f,.93f,.55f,.06f}; private final float[] ys={.11f,.19f,.32f,.41f,.55f,.67f,.76f,.88f,.94f,.61f,.24f,.82f}; private ValueAnimator twinkle;
 public StarfieldView(Context c){super(c);init();}public StarfieldView(Context c,AttributeSet a){super(c,a);init();}public StarfieldView(Context c,AttributeSet a,int d){super(c,a,d);init();}
 private void init(){setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();boolean reduce=android.provider.Settings.Global.getFloat(getContext().getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f;if(!reduce){twinkle=ValueAnimator.ofFloat(0f,6.283185f);twinkle.setDuration(6500);twinkle.setRepeatCount(ValueAnimator.INFINITE);twinkle.setInterpolator(new LinearInterpolator());twinkle.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});twinkle.start();}}
 @Override protected void onDetachedFromWindow(){if(twinkle!=null)twinkle.cancel();super.onDetachedFromWindow();}
 @Override protected void onDraw(Canvas c){super.onDraw(c);float density=getResources().getDisplayMetrics().density;for(int i=0;i<xs.length;i++){int alpha=(int)(65+55*(.5+.5*Math.sin(phase+i*1.71)));paint.setColor(Color.argb(alpha,i%4==0?119:165,i%3==0?228:188,225));c.drawCircle(xs[i]*getWidth(),ys[i]*getHeight(),(i%3==0?1.2f:.75f)*density,paint);}}
}
