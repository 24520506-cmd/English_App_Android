package com.example.english_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import com.example.english_app.api.AuthRepository;
import com.example.english_app.api.UserApi;

public class RegisterActivity extends Activity {
    private EditText username, password, confirm;
    private TextView error;
    private boolean motion;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(getColor(R.color.login_bg));
        getWindow().setNavigationBarColor(getColor(R.color.login_bg));
        setContentView(R.layout.activity_register);
        motion = Settings.Global.getFloat(getContentResolver(), "animator_duration_scale", 1f) > 0f;
        username = findViewById(R.id.usernameInput);
        password = findViewById(R.id.passwordInput);
        confirm = findViewById(R.id.confirmInput);
        error = findViewById(R.id.errorMessage);
        View back = findViewById(R.id.backButton);
        View loginLink = findViewById(R.id.loginLink);
        back.setOnClickListener(v -> finish());
        loginLink.setOnClickListener(v -> finish());
        setupFocus(username); setupFocus(password); setupFocus(confirm);
        toggleVisibility(password, findViewById(R.id.togglePassword));
        toggleVisibility(confirm, findViewById(R.id.toggleConfirm));
        TextView submit = findViewById(R.id.createAccountButton);
        pressFeedback(back);
        pressFeedback(submit);
        pressFeedback(loginLink);
        pressFeedback(findViewById(R.id.togglePassword));
        pressFeedback(findViewById(R.id.toggleConfirm));
        submit.setOnClickListener(v -> {
            String u = username.getText().toString().trim();
            String p = password.getText().toString();
            String c = confirm.getText().toString();
            if (u.isEmpty() || p.isEmpty() || c.isEmpty()) { showError("Vui lòng nhập đầy đủ ba trường."); return; }
            if (!p.equals(c)) { showError("Xác nhận mật khẩu chưa khớp."); confirm.requestFocus(); return; }
            if (!u.matches("[A-Za-z0-9_]{3,30}")) {
                showError("Tên đăng nhập cần từ 3–30 ký tự gồm chữ, số hoặc dấu gạch dưới.");
                StateScreensActivity.open(this, "register_validation",
                    "Tên đăng nhập cần từ 3–30 ký tự gồm chữ, số hoặc dấu gạch dưới.");
                return;
            }
            if (p.length() < 8) {
                showError("Mật khẩu cần có ít nhất 8 ký tự.");
                StateScreensActivity.open(this, "register_validation",
                    "Mật khẩu cần có ít nhất 8 ký tự.");
                return;
            }
            error.setVisibility(View.GONE);
            submit.setEnabled(false);
            submit.setText("Đang tạo tài khoản...");
            AuthRepository.get(this).register(u, p, new AuthRepository.Result<UserApi.Account>() {
                @Override public void success(UserApi.Account account) {
                    if (isFinishing()) return;
                    OnboardingProgress.markPending(RegisterActivity.this, account.user_id);
                    submit.setText("Đang đăng nhập...");
                    AuthRepository.get(RegisterActivity.this).login(u, p,
                        Build.MANUFACTURER + " " + Build.MODEL,
                        new AuthRepository.Result<UserApi.Tokens>() {
                            @Override public void success(UserApi.Tokens tokens) {
                                if (isFinishing()) return;
                                Intent onboarding = new Intent(RegisterActivity.this,
                                    OnboardingBandActivity.class);
                                onboarding.putExtra(OnboardingProgress.EXTRA_USER_ID,
                                    account.user_id);
                                onboarding.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(onboarding);
                            }

                            @Override public void failure(String code, String message) {
                                if (isFinishing()) return;
                                Intent login = new Intent(RegisterActivity.this,
                                    LoginActivity.class);
                                login.putExtra(LoginActivity.EXTRA_USERNAME, u);
                                login.putExtra(LoginActivity.EXTRA_MESSAGE,
                                    "Tài khoản đã được tạo. Vui lòng đăng nhập để tiếp tục.");
                                login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(login);
                            }
                        });
                }
                @Override public void failure(String code, String message) {
                    if (isFinishing()) return;
                    submit.setEnabled(true);
                    submit.setText("Tạo tài khoản  →");
                    if ("VALIDATION_ERROR".equals(code)) {
                        showError(message);
                        StateScreensActivity.open(RegisterActivity.this,
                            "register_validation", message);
                    } else {
                        showError(message);
                    }
                }
            });
        });
        animateIn(back, 0);
        animateIn(findViewById(R.id.registerHeader), 130);
        animateIn(findViewById(R.id.registerUsernameGroup), 260);
        animateIn(findViewById(R.id.registerPasswordGroup), 390);
        animateIn(findViewById(R.id.registerConfirmGroup), 520);
        animateIn(findViewById(R.id.contactNotice), 650);
        animateIn(submit, 780);
        animateIn(loginLink, 910);
    }
    private void setupFocus(EditText field) {
        field.setOnFocusChangeListener((v, focused) -> {
            field.setBackgroundResource(focused ? R.drawable.bg_input_focus : R.drawable.bg_input);
            if (motion) v.animate().scaleX(focused ? 1.01f : 1f).scaleY(focused ? 1.01f : 1f)
                .setDuration(170).start();
        });
    }
    private void toggleVisibility(EditText field, TextView button) {
        button.setOnClickListener(v -> {
            boolean reveal = !(field.getTransformationMethod() instanceof HideReturnsTransformationMethod);
            field.setTransformationMethod(reveal
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            field.setSelection(field.length());
            button.setText(reveal ? "Ẩn" : "Hiện");
        });
    }
    private void showError(String message) {
        error.setText(message); error.setVisibility(View.VISIBLE);
        if (motion) { error.setAlpha(0); error.animate().alpha(1).setDuration(180).start(); }
    }
    private void animateIn(View view, long delay) {
        if (!motion) return;
        float dp = getResources().getDisplayMetrics().density;
        view.setAlpha(0f); view.setTranslationY(12f * dp);
        view.animate().alpha(1f).translationY(0f).setStartDelay(delay)
            .setDuration(350).setInterpolator(new DecelerateInterpolator()).start();
    }
    private void pressFeedback(View view) {
        view.setOnTouchListener((v, event) -> {
            if (!motion) return false;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                float scale = action == MotionEvent.ACTION_DOWN ? 0.97f : 1f;
                v.animate().scaleX(scale).scaleY(scale).setDuration(130).start();
            }
            return false;
        });
    }
}
