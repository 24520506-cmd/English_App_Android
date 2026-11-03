package com.example.english_app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Decorative lesson journey. Actual node actions are handled by an Activity listener. */
public class LearningPathView extends View {
    public interface OnLessonClickListener { void onLessonClick(int lessonIndex); }
    private OnLessonClickListener listener;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final PathMeasure measure = new PathMeasure();
    private final float[] pos = new float[2];
    private final float[] tangent = new float[2];
    private final float[] xs = {0.25f,0.72f};
    private final float[] ys = {0.25f,0.75f};
    private ValueAnimator pulse;
    private float phase = 0f;
    private boolean firstLessonCompleted;
    private boolean secondLessonCompleted;
    private final int mint = Color.rgb(117, 229, 194);
    private final int gold = Color.rgb(250, 199, 81);
    private final int purple = Color.rgb(113, 133, 215);

    public LearningPathView(Context context) { super(context); init(); }
    public LearningPathView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public LearningPathView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }
    private float dp(float n) { return n*getResources().getDisplayMetrics().density; }
    private void init() { setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
    public void setOnLessonClickListener(OnLessonClickListener l) { listener=l; }
    public void setFirstLessonCompleted(boolean completed) {
        firstLessonCompleted = completed;
        invalidate();
    }
    public void setSecondLessonCompleted(boolean completed) {
        secondLessonCompleted = completed;
        invalidate();
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (android.provider.Settings.Global.getFloat(getContext().getContentResolver(), "animator_duration_scale", 1f) != 0f) startMotion();
    }
    // Kept separate so builds on older Android versions without dependency libraries.
    private float dummy() { return 1f; }
    private void startMotion() {
        if (pulse!=null) pulse.cancel();
        pulse=ValueAnimator.ofFloat(0,1);
        pulse.setDuration(3200);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setInterpolator(new LinearInterpolator());
        pulse.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});
        pulse.start();
    }
    @Override protected void onDetachedFromWindow() { if(pulse!=null) pulse.cancel();super.onDetachedFromWindow(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w=getWidth(),h=getHeight(); if(w<=0||h<=0)return;
        float[] x=new float[xs.length],y=new float[ys.length];
        for(int i=0;i<x.length;i++){x[i]=xs[i]*w;y[i]=ys[i]*h;}
        path.reset();path.moveTo(x[0],y[0]);
        for(int i=0;i<x.length-1;i++) {
            float mid=(y[i]+y[i+1])/2;
            path.cubicTo(x[i],mid,x[i+1],mid,x[i+1],y[i+1]);
        }
        paint.reset();paint.setAntiAlias(true);paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Color.rgb(42,52,93));paint.setStrokeWidth(dp(32));canvas.drawPath(path,paint);
        paint.setColor(Color.rgb(60,75,130));paint.setStrokeWidth(dp(3));canvas.drawPath(path,paint);
        // Highlight trail to lead the eye without distracting from the lesson nodes.
        measure.setPath(path,false);float len=measure.getLength();
        Path segment=new Path();measure.getSegment(Math.max(0,phase*len-dp(90)),phase*len,segment,true);
        paint.setColor(mint);paint.setAlpha(120);paint.setStrokeWidth(dp(4));paint.setShadowLayer(dp(9),0,0,mint);canvas.drawPath(segment,paint);paint.clearShadowLayer();
        measure.getPosTan(Math.max(0.001f,phase*len),pos,tangent);
        paint.setStyle(Paint.Style.FILL);paint.setColor(mint);paint.setAlpha(90);paint.setShadowLayer(dp(18),0,0,mint);canvas.drawCircle(pos[0],pos[1],dp(7),paint);paint.clearShadowLayer();
        paint.setAlpha(255);canvas.drawCircle(pos[0],pos[1],dp(3.5f),paint);
        for(int i=0;i<x.length;i++)drawNode(canvas,x[i],y[i],i);
    }
    private void drawNode(Canvas c,float x,float y,int i){
        boolean active=!secondLessonCompleted&&i==(firstLessonCompleted?1:0);float radius=dp(active?37:28);
        paint.reset();paint.setAntiAlias(true);paint.setStyle(Paint.Style.FILL);
        if(active){paint.setColor(gold);paint.setShadowLayer(dp(19),0,0,gold);c.drawCircle(x,y,radius+dp(3),paint);paint.clearShadowLayer();}
        paint.setColor(active?0xFF293460:0xFF2B375F);c.drawCircle(x,y,radius,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(active?4:2));paint.setColor(active?gold:purple);c.drawCircle(x,y,radius,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(active?gold:mint);c.drawCircle(x,y,dp(active?12:8),paint);
        paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));paint.setTextSize(dp(11));
        paint.setColor(active?0xFF161B38:0xFF14213C);
        c.drawText((i==0&&firstLessonCompleted)||(i==1&&secondLessonCompleted)?"✓":String.valueOf(i+1),x,y+dp(4),paint);
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent ev){
        if(ev.getAction()==android.view.MotionEvent.ACTION_UP){
            float w=getWidth(),h=getHeight();
            for(int i=0;i<xs.length;i++){
                float dx=ev.getX()-xs[i]*w,dy=ev.getY()-ys[i]*h;
                if(dx*dx+dy*dy<dp(46)*dp(46)){
                    performClick();if(listener!=null)listener.onLessonClick(i);return true;
                }
            }
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
