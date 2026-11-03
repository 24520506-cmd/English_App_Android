package com.example.english_app;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ForgotPasswordActivity extends Activity {
    private static final long STAGGER_MS = 130L;
    private static final long DURATION_MS = 350L;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        View back = findViewById(R.id.forgotBack);
        View title = findViewById(R.id.forgotTitle);
        View subtitle = findViewById(R.id.forgotSubtitle);
        View desc = findViewById(R.id.forgotDescription);
        View channels = findViewById(R.id.forgotChannelSection);
        View accountLabel = findViewById(R.id.forgotAccountLabel);
        EditText account = findViewById(R.id.forgotAccount);
        Button send = findViewById(R.id.forgotSend);
        TextView status = findViewById(R.id.forgotStatus);
        boolean motion = Settings.Global.getFloat(getContentResolver(), "animator_duration_scale", 1f) > 0f;

        back.setOnClickListener(v -> finish());
        send.setOnClickListener(v -> {
            String identity = account.getText().toString().trim();
            if (identity.isEmpty()) {
                account.setError("Vui lòng nhập tài khoản hoặc liên hệ đã lưu");
                account.requestFocus();
                return;
            }
            status.setText("Khôi phục mật khẩu chưa được kết nối với máy chủ.");
            status.setVisibility(View.VISIBLE);
        });
        account.setOnFocusChangeListener((v, focused) -> {
            if (motion) v.animate().scaleX(focused ? 1.01f : 1f)
                .scaleY(focused ? 1.01f : 1f).setDuration(180).start();
        });

        View[] sequence = { back, title, subtitle, desc, channels, accountLabel, account, send };
        if (!motion) return;
        float distance = getResources().getDisplayMetrics().density * 18f;
        for (View view : sequence) { view.setAlpha(0f); view.setTranslationY(distance); }
        for (int i = 0; i < sequence.length; i++) {
            sequence[i].animate().alpha(1f).translationY(0f).setStartDelay(i * STAGGER_MS)
                .setDuration(DURATION_MS).setInterpolator(new DecelerateInterpolator()).start();
        }
        // Last of 8 starts at 910ms and finishes at 1260ms.
    }
}
