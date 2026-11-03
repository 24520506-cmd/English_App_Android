package com.example.english_app;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import com.example.english_app.api.AuthRepository;
import com.example.english_app.api.UserApi;

public class OnboardingNameActivity extends Activity {
 private static final long STEP=130L,DURATION=350L;
 private EditText name;
 private TextView finishButton;
 private boolean motion;
 @Override public void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_onboarding_name);
 motion=Settings.Global.getFloat(getContentResolver(),"animator_duration_scale",1f)>0f;
 name=findViewById(R.id.obName);TextView counter=findViewById(R.id.obCounter);
 findViewById(R.id.obBack).setOnClickListener(v->finish());
 name.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){counter.setText(s.length()+"/30");}public void afterTextChanged(Editable e){}});
 finishButton=findViewById(R.id.obFinish);
 finishButton.setOnClickListener(v->saveNameAndFinish());
 findViewById(R.id.obSkip).setOnClickListener(v->openHome());animateEntry();}
 private void saveNameAndFinish(){
  String label=name.getText().toString().trim();
  if(label.isEmpty()){openHome();return;}
  if(label.length()<2||label.length()>30){name.setError("Tên hiển thị cần từ 2 đến 30 ký tự");return;}
  finishButton.setEnabled(false);finishButton.setText("Đang lưu...");
  AuthRepository.get(this).updateProfile(label,new AuthRepository.Result<UserApi.Account>(){
   @Override public void success(UserApi.Account account){if(!isFinishing())openHome();}
   @Override public void failure(String code,String message){
    if(isFinishing())return;
    finishButton.setEnabled(true);finishButton.setText("Hoàn tất  →");
    name.setError(message);
   }
  });
 }
 private void openHome(){OnboardingProgress.markComplete(this,getIntent().getStringExtra(OnboardingProgress.EXTRA_USER_ID));Intent intent=new Intent(this,HomeActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(intent);}
 private void animateEntry(){if(!motion)return;View[] v={findViewById(R.id.obBack),findViewById(R.id.obTitle),findViewById(R.id.obSubtitle),findViewById(R.id.obDesc),findViewById(R.id.obFieldLabel),name,findViewById(R.id.obCounter),findViewById(R.id.obFinish),findViewById(R.id.obSkip)};float d=16*getResources().getDisplayMetrics().density;for(View w:v){w.setAlpha(0f);w.setTranslationY(d);}for(int i=0;i<v.length;i++)v[i].animate().alpha(1f).translationY(0).setDuration(DURATION).setStartDelay(STEP*i).setInterpolator(new DecelerateInterpolator()).start();}
}
