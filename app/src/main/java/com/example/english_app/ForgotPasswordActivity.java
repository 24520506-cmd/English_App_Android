package com.example.english_app;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.english_app.api.AuthApi;
import com.example.english_app.api.AuthRepository;

public class ForgotPasswordActivity extends Activity {
    private static final long STAGGER_MS = 110L;
    private static final long DURATION_MS = 320L;

    private int step;
    private String challengeId;
    private String resetGrant;
    private CountDownTimer resendTimer;
    private EditText username, email, otp, newPassword, confirmPassword;
    private TextView subtitle, description, status;
    private Button send, verify, continueReset, resend, complete;
    private View startSection, otpSection, resetSection;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        getWindow().setStatusBarColor(0xff151932);
        getWindow().setNavigationBarColor(0xff151932);

        View back = findViewById(R.id.forgotBack);
        subtitle = findViewById(R.id.forgotSubtitle);
        description = findViewById(R.id.forgotDescription);
        status = findViewById(R.id.forgotStatus);
        startSection = findViewById(R.id.forgotStartSection);
        otpSection = findViewById(R.id.forgotOtpSection);
        resetSection = findViewById(R.id.forgotResetSection);
        username = findViewById(R.id.forgotUsername);
        email = findViewById(R.id.forgotEmail);
        otp = findViewById(R.id.forgotOtp);
        newPassword = findViewById(R.id.forgotNewPassword);
        confirmPassword = findViewById(R.id.forgotConfirmPassword);
        send = findViewById(R.id.forgotSend);
        verify = findViewById(R.id.forgotVerify);
        continueReset = findViewById(R.id.forgotContinue);
        resend = findViewById(R.id.forgotResend);
        complete = findViewById(R.id.forgotComplete);

        back.setOnClickListener(v -> navigateBack());
        send.setOnClickListener(v -> requestCode());
        verify.setOnClickListener(v -> verifyCode());
        continueReset.setOnClickListener(v -> showStep(2));
        resend.setOnClickListener(v -> resendCode());
        complete.setOnClickListener(v -> completeReset());
        View.OnFocusChangeListener focus = (view, focused) -> {
            if (Settings.Global.getFloat(getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f) {
                view.animate().scaleX(focused ? 1.01f : 1f).scaleY(focused ? 1.01f : 1f)
                    .setDuration(180L).start();
            }
        };
        username.setOnFocusChangeListener(focus);
        email.setOnFocusChangeListener(focus);
        showStep(0);
        animateEntrance(back);
    }

    @Override protected void onDestroy() {
        if (resendTimer != null) resendTimer.cancel();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        navigateBack();
    }

    private void navigateBack() {
        if (step == 2) {
            showStep(1);
        } else if (step == 1) {
            clearChallenge();
            showStep(0);
        } else {
            finish();
        }
    }

    private void requestCode() {
        String user = username.getText().toString().trim();
        String mail = email.getText().toString().trim();
        if (!user.matches("[A-Za-z0-9_]{3,30}")) {
            username.setError("Nhập đúng tên đăng nhập");
            username.requestFocus();
            return;
        }
        if (!mail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            email.setError("Nhập Gmail đã đăng ký");
            email.requestFocus();
            return;
        }
        send.setEnabled(false);
        AuthRepository.get(this).startPasswordReset(user, mail,
            new AuthRepository.Result<AuthApi.PasswordResetChallenge>() {
                @Override public void success(AuthApi.PasswordResetChallenge challenge) {
                    send.setEnabled(true);
                    if (isFinishing()) return;
                    if (challenge == null || challenge.challenge_id == null) {
                        showStatus("Máy chủ trả về yêu cầu không hợp lệ.");
                        return;
                    }
                    challengeId = challenge.challenge_id;
                    resetGrant = null;
                    otp.setText("");
                    showStep(1);
                    showStatus(challenge.message == null
                        ? "Nếu thông tin phù hợp, mã khôi phục sẽ được gửi qua Gmail."
                        : challenge.message);
                    startResendTimer();
                }
                @Override public void failure(String code, String message) {
                    send.setEnabled(true);
                    showStatus(message);
                }
            });
    }

    private void resendCode() {
        if (challengeId == null) return;
        resend.setEnabled(false);
        resend.setText("Đang gửi lại mã…");
        AuthRepository.get(this).resendPasswordReset(challengeId,
            new AuthRepository.Result<AuthApi.PasswordResetChallenge>() {
                @Override public void success(AuthApi.PasswordResetChallenge challenge) {
                    if (isFinishing()) return;
                    if (challenge != null && challenge.challenge_id != null) {
                        challengeId = challenge.challenge_id;
                    }
                    otp.setText("");
                    showStatus(challenge == null || challenge.message == null
                        ? "Nếu thông tin phù hợp, mã khôi phục sẽ được gửi qua Gmail."
                        : challenge.message);
                    startResendTimer();
                }
                @Override public void failure(String code, String message) {
                    resend.setEnabled(true);
                    resend.setText("Gửi lại mã");
                    showStatus(message);
                }
            });
    }

    private void verifyCode() {
        String code = otp.getText().toString().trim();
        if (!code.matches("[0-9]{6}")) {
            otp.setError("Nhập đủ 6 chữ số");
            return;
        }
        if (challengeId == null) {
            showStatus("Yêu cầu mã xác minh mới.");
            showStep(0);
            return;
        }
        verify.setEnabled(false);
        AuthRepository.get(this).verifyPasswordReset(challengeId, code,
            new AuthRepository.Result<AuthApi.ResetGrant>() {
                @Override public void success(AuthApi.ResetGrant grant) {
                    verify.setEnabled(true);
                    if (isFinishing()) return;
                    if (grant == null || grant.reset_grant == null) {
                        showStatus("Máy chủ không cấp quyền đặt lại mật khẩu.");
                        return;
                    }
                    resetGrant = grant.reset_grant;
                    if (resendTimer != null) resendTimer.cancel();
                    showStep(2);
                    showStatus("Mã đã xác minh. Đặt mật khẩu mới.");
                }
                @Override public void failure(String code, String message) {
                    verify.setEnabled(true);
                    otp.setError(message);
                }
            });
    }

    private void completeReset() {
        String password = newPassword.getText().toString();
        String confirmation = confirmPassword.getText().toString();
        if (password.length() < 8) {
            newPassword.setError("Mật khẩu cần ít nhất 8 ký tự");
            return;
        }
        if (!password.equals(confirmation)) {
            confirmPassword.setError("Mật khẩu không khớp");
            return;
        }
        if (resetGrant == null) {
            showStatus("Mã xác minh đã hết hạn. Hãy bắt đầu lại.");
            clearChallenge();
            showStep(0);
            return;
        }
        complete.setEnabled(false);
        AuthRepository.get(this).completePasswordReset(resetGrant, password,
            new AuthRepository.Result<Void>() {
                @Override public void success(Void ignored) {
                    if (isFinishing()) return;
                    Toast.makeText(ForgotPasswordActivity.this,
                        "Đổi mật khẩu thành công. Hãy đăng nhập lại.", Toast.LENGTH_LONG).show();
                    finish();
                }
                @Override public void failure(String code, String message) {
                    complete.setEnabled(true);
                    showStatus(message);
                }
            });
    }

    private void showStep(int next) {
        step = next;
        startSection.setVisibility(step == 0 ? View.VISIBLE : View.GONE);
        otpSection.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        resetSection.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        subtitle.setText(step == 0 ? "Nhận mã qua Gmail"
            : step == 1 ? "Xác minh mã OTP" : "Đặt mật khẩu mới");
        description.setText(step == 0 ? "Mã khôi phục sẽ được gửi đến Gmail đã đăng ký."
            : step == 1 ? "Nhập mã 6 số đã gửi đến Gmail của bạn. Mã có hiệu lực trong 5 phút."
            : "Chọn mật khẩu mới cho tài khoản của bạn.");
        boolean verified = step == 1 && resetGrant != null;
        otp.setVisibility(verified ? View.GONE : View.VISIBLE);
        verify.setVisibility(verified ? View.GONE : View.VISIBLE);
        resend.setVisibility(verified ? View.GONE : View.VISIBLE);
        continueReset.setVisibility(verified ? View.VISIBLE : View.GONE);
        if (step != 1 && resendTimer != null) {
            resendTimer.cancel();
            resendTimer = null;
        }
    }

    private void startResendTimer() {
        if (resendTimer != null) resendTimer.cancel();
        resend.setEnabled(false);
        resendTimer = new CountDownTimer(60_000L, 1_000L) {
            @Override public void onTick(long millisUntilFinished) {
                long seconds = (millisUntilFinished + 999L) / 1_000L;
                resend.setText("Gửi lại mã (" + seconds + " giây)");
            }
            @Override public void onFinish() {
                resend.setEnabled(true);
                resend.setText("Gửi lại mã");
            }
        }.start();
    }

    private void clearChallenge() {
        if (resendTimer != null) resendTimer.cancel();
        resendTimer = null;
        challengeId = null;
        resetGrant = null;
        otp.setText("");
        newPassword.setText("");
        confirmPassword.setText("");
    }

    private void showStatus(String message) {
        status.setText(message);
        status.setVisibility(View.VISIBLE);
    }

    private void animateEntrance(View back) {
        if (Settings.Global.getFloat(getContentResolver(),
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) return;
        View[] sequence = { back, findViewById(R.id.forgotTitle), subtitle, description,
            startSection, status };
        float distance = getResources().getDisplayMetrics().density * 18f;
        for (View view : sequence) {
            view.setAlpha(0f);
            view.setTranslationY(distance);
        }
        for (int i = 0; i < sequence.length; i++) {
            sequence[i].animate().alpha(1f).translationY(0f).setStartDelay(i * STAGGER_MS)
                .setDuration(DURATION_MS).setInterpolator(new DecelerateInterpolator()).start();
        }
    }
}
