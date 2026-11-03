package com.example.english_app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

public class ListeningActivity extends LearningBaseActivity {
    private static final long DEMO_DURATION_MS = 30_000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean playing;
    private long startedMs;
    private long listenedMs;
    private ProgressBar audioProgress;
    private TextView audioTime;
    private Button playButton;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!playing) return;
            long elapsedMs = Math.min(DEMO_DURATION_MS,
                listenedMs + SystemClock.elapsedRealtime() - startedMs);
            showProgress(elapsedMs);
            if (elapsedMs >= DEMO_DURATION_MS) {
                playing = false;
                listenedMs = DEMO_DURATION_MS;
                playButton.setText("▶ Phát lại");
                message(R.id.audioBody, "Đã chạy xong phần mô phỏng tiến độ nghe.");
            } else {
                handler.postDelayed(this, 120L);
            }
        }
    };

    @Override protected int layout() { return R.layout.activity_listening; }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        audioProgress = findViewById(R.id.audioProgress);
        audioTime = findViewById(R.id.audioTime);
        playButton = findViewById(R.id.play);
        showProgress(0L);
        playButton.setOnClickListener(v -> {
            pulse(v);
            if (playing) {
                pauseProgress();
            } else {
                if (listenedMs >= DEMO_DURATION_MS) listenedMs = 0L;
                startedMs = SystemClock.elapsedRealtime();
                playing = true;
                playButton.setText("Ⅱ Tạm dừng");
                handler.post(ticker);
            }
        });
        findViewById(R.id.submit).setOnClickListener(v -> {
            boolean chosen = ((RadioButton) findViewById(R.id.q0)).isChecked()
                || ((RadioButton) findViewById(R.id.q1)).isChecked()
                || ((RadioButton) findViewById(R.id.q2)).isChecked();
            String answer = ((EditText) findViewById(R.id.answer)).getText().toString().trim();
            if (!chosen || answer.isEmpty()) {
                message(R.id.notice, "Vui lòng trả lời tất cả câu hỏi.");
                StateScreensActivity.open(this, "listening_missing");
                return;
            }
            go(ListeningResultActivity.class);
        });
    }

    private void showProgress(long elapsedMs) {
        audioProgress.setProgress((int) (elapsedMs * 100 / DEMO_DURATION_MS));
        audioTime.setText(String.format(java.util.Locale.ROOT,
            "00:%02d / 00:30", elapsedMs / 1000));
    }

    private void pauseProgress() {
        if (!playing) return;
        listenedMs = Math.min(DEMO_DURATION_MS,
            listenedMs + SystemClock.elapsedRealtime() - startedMs);
        playing = false;
        handler.removeCallbacks(ticker);
        showProgress(listenedMs);
        playButton.setText("▶ Tiếp tục");
    }

    @Override protected void onStop() {
        pauseProgress();
        super.onStop();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }
}
