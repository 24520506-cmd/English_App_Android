package com.example.english_app;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.example.english_app.api.AuthRepository;
import com.example.english_app.api.UserApi;

public class LoginActivity extends Activity {
    public static final String EXTRA_USERNAME = "login_username";
    public static final String EXTRA_MESSAGE = "login_message";
    private LoginHeroView hero;
    private EditText username, password;
    private TextView error;
    private boolean motion;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff151932);
        getWindow().setNavigationBarColor(0xff151932);
        setContentView(R.layout.activity_login);
        hero=findViewById(R.id.heroCard);
        username=findViewById(R.id.usernameInput);
        password=findViewById(R.id.passwordInput);
        View passwordField=findViewById(R.id.passwordField);
        error=findViewById(R.id.loginError);
        if (state == null) {
            String prefilledUsername = getIntent().getStringExtra(EXTRA_USERNAME);
            if (prefilledUsername != null) username.setText(prefilledUsername);
            String loginMessage = getIntent().getStringExtra(EXTRA_MESSAGE);
            if (loginMessage != null) {
                error.setText(loginMessage);
                error.setVisibility(View.VISIBLE);
            }
        }
        motion=Settings.Global.getFloat(getContentResolver(),"animator_duration_scale",1f)>0f;
        TextView toggle=findViewById(R.id.togglePassword);
        toggle.setOnClickListener(v->{
            boolean reveal=!(password.getTransformationMethod() instanceof HideReturnsTransformationMethod);
            password.setTransformationMethod(reveal?HideReturnsTransformationMethod.getInstance():PasswordTransformationMethod.getInstance());
            toggle.setText(reveal?"Ẩn":"Hiện");
            password.setSelection(password.length());
            if(motion){
                toggle.animate().cancel();
                toggle.setAlpha(0.45f);
                toggle.animate().alpha(1f).setDuration(160).start();
            }
        });
        Button login=findViewById(R.id.loginButton);
        View createAccount=findViewById(R.id.createAccount);
        View forgotPassword=findViewById(R.id.forgotPassword);
        if(motion){
            enter(findViewById(R.id.introGroup),0);
            enter(findViewById(R.id.usernameGroup),90);
            enter(findViewById(R.id.passwordGroup),180);
            enter(login,270);
            enter(findViewById(R.id.registerRow),360);
            enter(forgotPassword,450);
            enter(findViewById(R.id.termsText),540);
        }
        pressFeedback(login);
        pressFeedback(createAccount);
        pressFeedback(forgotPassword);
        clearErrorOnEdit(username,username);
        clearErrorOnEdit(password,passwordField);
        login.setOnClickListener(v->{
            boolean missingUsername=username.getText().toString().trim().isEmpty();
            boolean missingPassword=password.getText().toString().isEmpty();
            if(missingUsername || missingPassword){
                error.setText("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
                error.setVisibility(View.VISIBLE);
                if(motion){error.setAlpha(0f);error.animate().alpha(1).setDuration(200).start();}
                if(missingUsername){
                    username.setBackgroundResource(R.drawable.login_field_error_bg);
                    shake(username);
                }
                if(missingPassword){
                    passwordField.setBackgroundResource(R.drawable.login_field_error_bg);
                    shake(passwordField);
                }
                return;
            }
            error.setVisibility(View.GONE);
            username.setBackgroundResource(R.drawable.login_field_bg);
            passwordField.setBackgroundResource(R.drawable.login_field_bg);
            login.setEnabled(false);
            AuthRepository.get(this).login(username.getText().toString().trim(),
                password.getText().toString(), Build.MANUFACTURER + " " + Build.MODEL,
                new AuthRepository.Result<UserApi.Tokens>() {
                    @Override public void success(UserApi.Tokens value) {
                        if (isFinishing()) return;
                        login.setEnabled(true);
                        password.setText("");
                        openAfterAuthentication(value);
                    }
                    @Override public void failure(String code, String message) {
                        if (isFinishing()) return;
                        login.setEnabled(true);
                        if ("INVALID_CREDENTIALS".equals(code)) {
                            error.setText(message);
                            error.setVisibility(View.VISIBLE);
                            StateScreensActivity.open(LoginActivity.this, "auth_invalid",
                                "Thông tin đăng nhập không hợp lệ", username.getText().toString().trim());
                        } else {
                            error.setText(message);
                            error.setVisibility(View.VISIBLE);
                        }
                    }
                });
        });
        createAccount.setOnClickListener(v->startActivity(new Intent(this,RegisterActivity.class)));
        forgotPassword.setOnClickListener(v->startActivity(new Intent(this,ForgotPasswordActivity.class)));
        View.OnFocusChangeListener focus=(v,has)->{
            if(motion) v.animate().scaleX(has?1.008f:1f).scaleY(has?1.008f:1f)
                .setDuration(160).setInterpolator(new DecelerateInterpolator()).start();
        };
        username.setOnFocusChangeListener(focus);
        password.setOnFocusChangeListener(focus);
        AuthRepository.get(this).restore(new AuthRepository.Result<UserApi.Tokens>() {
            @Override public void success(UserApi.Tokens value) {
                if (!isFinishing()) openAfterAuthentication(value);
            }
            @Override public void failure(String code, String message) {
                if (isFinishing()) return;
                if ("NETWORK_ERROR".equals(code)) {
                    error.setText(message); error.setVisibility(View.VISIBLE);
                } else if (code.startsWith("SESSION_") || "REFRESH_REUSE_DETECTED".equals(code)) {
                    StateScreensActivity.open(LoginActivity.this, "session_expired");
                }
            }
        });
    }
    private void openHome() {
        Intent intent=new Intent(this,HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    private void openAfterAuthentication(UserApi.Tokens tokens) {
        String userId = tokens.user == null ? null : tokens.user.user_id;
        if (!OnboardingProgress.isPending(this, userId)) {
            openHome();
            return;
        }
        Intent intent = new Intent(this, OnboardingBandActivity.class);
        intent.putExtra(OnboardingProgress.EXTRA_USER_ID, userId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    private void enter(View view,long delay){
        float dp=getResources().getDisplayMetrics().density;
        view.setAlpha(0f);
        view.setTranslationY(12f*dp);
        view.animate().alpha(1f).translationY(0f).setDuration(350).setStartDelay(delay)
            .setInterpolator(new DecelerateInterpolator()).start();
    }
    private void pressFeedback(View view){
        view.setOnTouchListener((v,event)->{
            if(!motion)return false;
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL){
                float scale=action==MotionEvent.ACTION_DOWN?0.97f:1f;
                v.animate().scaleX(scale).scaleY(scale).setDuration(130).start();
            }
            return false;
        });
    }
    private void clearErrorOnEdit(EditText input,View border){
        input.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){}
            @Override public void afterTextChanged(Editable text){
                if(text.length()>0){
                    border.setBackgroundResource(R.drawable.login_field_bg);
                    if(!username.getText().toString().trim().isEmpty() && password.length()>0)
                        error.setVisibility(View.GONE);
                }
            }
        });
    }
    private void shake(View view){
        if(!motion)return;
        float dp=getResources().getDisplayMetrics().density;
        ObjectAnimator.ofFloat(view,View.TRANSLATION_X,0,-6*dp,6*dp,-4*dp,4*dp,0)
            .setDuration(280).start();
    }
    @Override protected void onResume(){super.onResume();if(hero!=null && motion)hero.startMotion();}
    @Override protected void onPause(){if(hero!=null)hero.stopMotion();super.onPause();}
}
