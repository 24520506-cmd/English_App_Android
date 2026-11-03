package com.example.english_app;
import android.app.Activity;import android.os.Bundle;import android.content.Intent;import android.view.View;import android.view.animation.DecelerateInterpolator;import android.widget.TextView;import android.provider.Settings;
public abstract class LearningBaseActivity extends Activity {
 protected abstract int layout();
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xff151932);getWindow().setNavigationBarColor(0xff151932);setContentView(layout());View back=findViewById(R.id.back);if(back!=null)back.setOnClickListener(v->finish());View root=findViewById(R.id.contentColumn);if(root instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)root;float scale=Settings.Global.getFloat(getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f);if(scale!=0f){for(int i=0;i<g.getChildCount();i++){View v=g.getChildAt(i);v.setAlpha(0f);v.setTranslationY(dp(16));v.animate().alpha(1f).translationY(0).setStartDelay(i*130L).setDuration(350L).setInterpolator(new DecelerateInterpolator()).start();}}}}
 protected float dp(float v){return v*getResources().getDisplayMetrics().density;}
 protected void go(Class<?> target){startActivity(new Intent(this,target));}
 protected void finishStepTwo(){LearningProgress.markFinanceListeningWritingDone(this);Intent home=new Intent(this,HomeActivity.class);home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(home);finish();}
 protected void pulse(View v){v.animate().scaleX(.97f).scaleY(.97f).setDuration(100).withEndAction(()->v.animate().scaleX(1).scaleY(1).setDuration(160).start()).start();}
 protected void message(int id,String s){TextView v=findViewById(id);if(v!=null)v.setText(s);}
}
