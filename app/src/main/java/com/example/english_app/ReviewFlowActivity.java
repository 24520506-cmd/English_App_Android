package com.example.english_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

public class ReviewFlowActivity extends UserSessionActivity {
    public static final String EXTRA_PAGE = "review_page";
    private static final String STATE_PAGE = "current_page";
    private static final String STATE_WORD = "current_word";
    private static final String STATE_LISTENING_DRAFT = "listening_draft";
    private static final String STATE_WRITING_DRAFT = "writing_draft";
    private static final String REVIEW_SENTENCE = "They need an emergency fund.";
    private static final String REVIEW_UTTERANCE_PREFIX = "review_sentence_";
    private static final int[] LAYOUTS = {
        R.layout.activity_review,
        R.layout.activity_review_detail,
        R.layout.activity_review_vocab,
        R.layout.activity_review_listening,
        R.layout.activity_review_writing,
        R.layout.activity_review_result
    };
    private static final String[] WORDS = {
        "Discretionary spending", "Household budget", "Financial literacy",
        "To tighten one's belt", "Emergency fund", "Expenditure",
        "Living paycheck to paycheck"
    };
    private static final String[] MEANINGS = {
        "Chi tiêu tùy ý", "Ngân sách hộ gia đình", "Kiến thức tài chính",
        "Thắt chặt chi tiêu", "Quỹ khẩn cấp", "Khoản chi tiêu",
        "Sống phụ thuộc vào từng kỳ lương"
    };
    private static final String[] EXAMPLES = {
        "I want to cut my discretionary spending this month.",
        "We need to make a household budget.",
        "Financial literacy helps people manage money.",
        "We must tighten our belts this month.",
        "She has an emergency fund.",
        "They reduced their monthly expenditure.",
        "He is living paycheck to paycheck."
    };
    private static final String[] CONTEXTS = {
        "Bạn cân nhắc những khoản chi có thể giảm trong ngân sách cá nhân.",
        "Bạn lập kế hoạch chi tiêu hằng tháng cho gia đình.",
        "Bạn giải thích vì sao nên hiểu cách quản lý tiền.",
        "Bạn cần giảm chi tiêu trong một tháng khó khăn.",
        "Bạn chuẩn bị tiền để ứng phó với việc bất ngờ.",
        "Bạn xem lại các khoản chi trong tháng.",
        "Bạn nói về việc gần như dùng hết lương trước kỳ lương tiếp theo."
    };

    private int page;
    private int wordIndex;
    private String listeningDraft = "";
    private String writingDraft = "";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextToSpeech speech;
    private boolean speechReady;
    private boolean speechUnavailable;
    private boolean pendingSpeech;
    private int audioRequestCount;
    private String activeUtteranceId;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff151932);
        getWindow().setNavigationBarColor(0xff151932);
        page = state == null ? getIntent().getIntExtra(EXTRA_PAGE, 0) : state.getInt(STATE_PAGE, 0);
        wordIndex = state == null ? 0 : Math.max(0,
            Math.min(WORDS.length - 1, state.getInt(STATE_WORD, 0)));
        if (state != null) {
            listeningDraft = state.getString(STATE_LISTENING_DRAFT, "");
            writingDraft = state.getString(STATE_WRITING_DRAFT, "");
        }
        showPage(Math.max(0, Math.min(LAYOUTS.length - 1, page)));
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        captureDrafts();
        outState.putInt(STATE_PAGE, page);
        outState.putInt(STATE_WORD, wordIndex);
        outState.putString(STATE_LISTENING_DRAFT, listeningDraft);
        outState.putString(STATE_WRITING_DRAFT, writingDraft);
        super.onSaveInstanceState(outState);
    }

    private void showPage(int next) {
        if (page == 3 && next != 3 && speech != null) {
            pendingSpeech = false;
            activeUtteranceId = null;
            speech.stop();
        }
        captureDrafts();
        page = next;
        setContentView(LAYOUTS[page]);
        View back = findViewById(R.id.backLink);
        if (back != null) back.setOnClickListener(v -> onBackPressed());
        bind(R.id.openDetail, () -> showPage(1));
        bind(R.id.freeReview, () -> showPage(1));
        bind(R.id.startVocab, () -> { wordIndex = 0; showPage(2); });
        bind(R.id.nextWord, () -> {
            if (wordIndex < WORDS.length - 1) {
                wordIndex++;
                showWord(true);
            } else {
                showPage(3);
            }
        });
        bind(R.id.playAudio, this::playReviewSentence);
        bind(R.id.notifyButton, this::showReminderChoices);
        bind(R.id.goWriting, () -> {
            EditText answer = findViewById(R.id.fillAnswer);
            if (answer.getText().toString().trim().isEmpty()) {
                answer.setError("Vui lòng nhập đáp án");
                return;
            }
            showPage(4);
        });
        bind(R.id.submitReview, () -> {
            EditText sentence = findViewById(R.id.reviewSentence);
            String value = sentence.getText().toString().trim();
            if (value.isEmpty() || value.split("\\s+").length < 3) {
                sentence.setError("Nhập tối thiểu 3 từ tiếng Anh");
                return;
            }
            showPage(5);
        });
        bind(R.id.backToReview, () -> showPage(0));
        if (page == 2) showWord(false);
        if (page == 3) ((EditText) findViewById(R.id.fillAnswer)).setText(listeningDraft);
        if (page == 4) ((EditText) findViewById(R.id.reviewSentence)).setText(writingDraft);
        if (page == 3) {
            prepareSpeech();
            if (speechUnavailable) setAudioStatus("Thiết bị chưa có giọng đọc tiếng Anh.");
        }
        constrainContentWidth();
        animateChildren();
    }

    private void prepareSpeech() {
        if (speech != null) return;
        speech = new TextToSpeech(getApplicationContext(), status ->
            mainHandler.post(() -> onSpeechInitialized(status)));
    }

    private void onSpeechInitialized(int status) {
        if (isFinishing() || isDestroyed()) return;
        if (status != TextToSpeech.SUCCESS || speech == null
            || speech.setLanguage(Locale.US) < 0) {
            speechReady = false;
            speechUnavailable = true;
            pendingSpeech = false;
            setAudioStatus("Thiết bị chưa có giọng đọc tiếng Anh.");
            setAudioProgress(false, 0);
            return;
        }
        speech.setSpeechRate(0.9f);
        speech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {
                mainHandler.post(() -> {
                    if (!utteranceId.equals(activeUtteranceId)) return;
                    setAudioButton("Ⅱ Dừng nghe");
                    setAudioStatus("Đang phát câu mẫu…");
                    setAudioProgress(true, 0);
                });
            }
            @Override public void onRangeStart(String utteranceId,
                int start, int end, int frame) {
                mainHandler.post(() -> {
                    if (!utteranceId.equals(activeUtteranceId)) return;
                    int progress = Math.max(1, Math.min(95,
                        end * 100 / REVIEW_SENTENCE.length()));
                    setAudioProgress(false, progress);
                });
            }
            @Override public void onDone(String utteranceId) {
                mainHandler.post(() -> {
                    if (!utteranceId.equals(activeUtteranceId)) return;
                    activeUtteranceId = null;
                    setAudioButton("▶ Nghe lại");
                    setAudioStatus("Đã phát xong câu mẫu. Bạn có thể nghe lại.");
                    setAudioProgress(false, 100);
                });
            }
            @Override public void onError(String utteranceId) {
                mainHandler.post(() -> {
                    if (!utteranceId.equals(activeUtteranceId)) return;
                    activeUtteranceId = null;
                    setAudioStatus("Không phát được câu mẫu trên thiết bị này.");
                    setAudioButton("▶ Nghe lại");
                    setAudioProgress(false, 0);
                });
            }
        });
        speechReady = true;
        if (pendingSpeech) playReviewSentence();
    }

    private void playReviewSentence() {
        if (speechUnavailable) {
            Toast.makeText(this, "Thiết bị chưa có giọng đọc tiếng Anh.",
                Toast.LENGTH_LONG).show();
            return;
        }
        if (!speechReady || speech == null) {
            pendingSpeech = true;
            prepareSpeech();
            setAudioStatus("Đang chuẩn bị giọng đọc tiếng Anh…");
            setAudioProgress(true, 0);
            return;
        }
        pendingSpeech = false;
        if (speech.isSpeaking()) {
            activeUtteranceId = null;
            speech.stop();
            setAudioButton("▶ Nghe lại");
            setAudioStatus("Đã dừng câu mẫu.");
            setAudioProgress(false, 0);
            return;
        }
        setAudioStatus("Nghe câu mẫu tiếng Anh rồi điền từ còn thiếu.");
        setAudioProgress(false, 0);
        activeUtteranceId = REVIEW_UTTERANCE_PREFIX + (++audioRequestCount);
        if (speech.speak(REVIEW_SENTENCE, TextToSpeech.QUEUE_FLUSH,
            null, activeUtteranceId) == TextToSpeech.ERROR) {
            activeUtteranceId = null;
            setAudioStatus("Không phát được câu mẫu trên thiết bị này.");
            setAudioProgress(false, 0);
            return;
        }
        setAudioButton("Ⅱ Dừng nghe");
    }

    private void setAudioButton(String label) {
        if (page != 3) return;
        TextView button = findViewById(R.id.playAudio);
        if (button != null) button.setText(label);
    }

    private void setAudioStatus(String status) {
        if (page != 3) return;
        TextView message = findViewById(R.id.reviewAudioStatus);
        if (message != null) message.setText(status);
    }

    private void setAudioProgress(boolean indeterminate, int progress) {
        if (page != 3) return;
        ProgressBar bar = findViewById(R.id.reviewAudioProgress);
        if (bar == null) return;
        bar.setIndeterminate(indeterminate);
        if (!indeterminate) bar.setProgress(progress);
    }

    private void showReminderChoices() {
        String[] milestones = {"Sau 1 ngày (D+1)", "Sau 3 ngày (D+3)",
            "Sau 7 ngày (D+7)"};
        int[] days = {1, 3, 7};
        new AlertDialog.Builder(this)
            .setTitle("Tạo lịch nhắc ôn")
            .setItems(milestones, (dialog, choice) -> openCalendarReminder(days[choice]))
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void openCalendarReminder(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        calendar.set(Calendar.HOUR_OF_DAY, 20);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long begin = calendar.getTimeInMillis();
        Intent intent = new Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE,
                "Ôn tiếng Anh · Personal Finance (D+" + days + ")")
            .putExtra(CalendarContract.Events.DESCRIPTION,
                "Ôn từ vựng, nghe và viết trong English App.")
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, begin + 30 * 60 * 1000L);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Thiết bị chưa có ứng dụng Lịch để tạo nhắc ôn.",
                Toast.LENGTH_LONG).show();
        }
    }

    private void captureDrafts() {
        if (page == 3) {
            EditText answer = findViewById(R.id.fillAnswer);
            if (answer != null) listeningDraft = answer.getText().toString();
        } else if (page == 4) {
            EditText sentence = findViewById(R.id.reviewSentence);
            if (sentence != null) writingDraft = sentence.getText().toString();
        }
    }

    private void showWord(boolean animate) {
        ((TextView) findViewById(R.id.wordProgress)).setText(
            "TỪ " + (wordIndex + 1) + " / " + WORDS.length);
        boolean motionEnabled = Settings.Global.getFloat(getContentResolver(),
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
        ((ProgressBar) findViewById(R.id.reviewWordBar))
            .setProgress(wordIndex + 1, animate && motionEnabled);
        ((TextView) findViewById(R.id.reviewWordTitle)).setText(WORDS[wordIndex]);
        ((TextView) findViewById(R.id.reviewWordMeaning)).setText(MEANINGS[wordIndex]);
        ((TextView) findViewById(R.id.reviewWordExample)).setText(EXAMPLES[wordIndex]);
        ((TextView) findViewById(R.id.reviewWordContext)).setText(CONTEXTS[wordIndex]);
        ((TextView) findViewById(R.id.nextWord)).setText(
            wordIndex == WORDS.length - 1 ? "Tiếp tục nghe  →" : "Từ tiếp theo  →");
        if (animate) {
            ScrollView scroll = findViewById(R.id.reviewScroll);
            if (motionEnabled) scroll.smoothScrollTo(0, 0);
            else scroll.scrollTo(0, 0);
            animateWordCards(motionEnabled);
            findViewById(R.id.reviewWordCard).announceForAccessibility(
                "Từ " + (wordIndex + 1) + ": " + WORDS[wordIndex]);
        }
    }

    private void animateWordCards(boolean motionEnabled) {
        int[] cards = {
            R.id.reviewWordCard, R.id.reviewExampleCard, R.id.reviewContextCard
        };
        float offset = 14 * getResources().getDisplayMetrics().density;
        for (int i = 0; i < cards.length; i++) {
            View card = findViewById(cards[i]);
            card.animate().cancel();
            card.setAlpha(1f);
            card.setTranslationY(0f);
            if (!motionEnabled) continue;
            card.setAlpha(0f);
            card.setTranslationY(offset);
            card.animate().alpha(1f).translationY(0f)
                .setStartDelay(i * 70L).setDuration(320L)
                .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    private void bind(int id, Runnable action) {
        View view = findViewById(id);
        if (view != null) view.setOnClickListener(v -> action.run());
    }

    private void constrainContentWidth() {
        if (getResources().getConfiguration().screenWidthDp < 600) return;
        LinearLayout column = findViewById(R.id.contentColumn);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) column.getLayoutParams();
        params.width = (int) (560 * getResources().getDisplayMetrics().density);
        params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
        column.setLayoutParams(params);
    }

    private void animateChildren() {
        LinearLayout column = findViewById(R.id.contentColumn);
        if (column == null || Settings.Global.getFloat(getContentResolver(),
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) return;
        float offset = 18 * getResources().getDisplayMetrics().density;
        for (int i = 0; i < column.getChildCount(); i++) {
            View child = column.getChildAt(i);
            if (child.getId() == R.id.backLink) continue;
            child.setAlpha(0f);
            child.setTranslationY(offset);
            child.animate().alpha(1f).translationY(0f)
                .setStartDelay(Math.min(i - 1, 8) * 60L)
                .setDuration(300L).setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    @Override public void onBackPressed() {
        if (page > 0) showPage(page - 1);
        else super.onBackPressed();
    }

    @Override protected void onStop() {
        if (speech != null) speech.stop();
        pendingSpeech = false;
        activeUtteranceId = null;
        if (page == 3) {
            setAudioButton("▶ Nghe lại");
            setAudioProgress(false, 0);
        }
        super.onStop();
    }

    @Override protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        if (speech != null) speech.shutdown();
        super.onDestroy();
    }
}
