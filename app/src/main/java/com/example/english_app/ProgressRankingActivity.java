package com.example.english_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProgressRankingActivity extends Activity {
    public static final String EXTRA_SCREEN = "screen";
    public static final String EXTRA_BACK_TO_PROFILE = "back_to_profile";
    public static final int SCREEN_PROGRESS = 0;
    public static final int SCREEN_HISTORY = 2;
    public static final int SCREEN_RANKING = 4;
    private static final String STATE_SCREEN = "current_screen";
    private static final String STATE_PERIOD = "ranking_period";
    private static final String STATE_BACK_TO_PROFILE = "back_to_profile_state";
    private static final int[] LAYOUTS = {
        R.layout.activity_progress,
        R.layout.activity_activity,
        R.layout.activity_lesson_history,
        R.layout.activity_lesson_history_detail,
        R.layout.activity_ranking
    };
    private int screen;
    private int rankPeriod;
    private boolean backToProfile;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff151932);
        getWindow().setNavigationBarColor(0xff151932);
        screen = state == null ? getIntent().getIntExtra(EXTRA_SCREEN, SCREEN_PROGRESS)
            : state.getInt(STATE_SCREEN, SCREEN_PROGRESS);
        rankPeriod = state == null ? 0 : Math.max(0,
            Math.min(2, state.getInt(STATE_PERIOD, 0)));
        backToProfile = state == null
            ? getIntent().getBooleanExtra(EXTRA_BACK_TO_PROFILE, false)
            : state.getBoolean(STATE_BACK_TO_PROFILE, false);
        show(Math.max(0, Math.min(LAYOUTS.length - 1, screen)));
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SCREEN, screen);
        outState.putInt(STATE_PERIOD, rankPeriod);
        outState.putBoolean(STATE_BACK_TO_PROFILE, backToProfile);
        super.onSaveInstanceState(outState);
    }

    @Override protected void onResume() {
        super.onResume();
        updateLocalProgress();
    }

    private void show(int next) {
        screen = next;
        setContentView(LAYOUTS[screen]);
        bind(R.id.backLink, this::onBackPressed);
        bind(R.id.openActivity, () -> show(1));
        bind(R.id.openHistory, () -> show(2));
        bind(R.id.openHistoryDetail, () -> show(3));
        bind(R.id.goProgress, () -> show(SCREEN_PROGRESS));
        bind(R.id.openWriting, () ->
            startActivity(new Intent(this, WritingFeedbackActivity.class)));
        for (int i = 0; i < 3; i++) {
            final int selected = i;
            bind(new int[]{R.id.rank0, R.id.rank1, R.id.rank2}[i], () -> {
                rankPeriod = selected;
                updateRanking();
            });
        }
        bind(R.id.nav0, this::goHome);
        bind(R.id.nav1, this::goReview);
        bind(R.id.nav2, () -> show(SCREEN_PROGRESS));
        bind(R.id.nav3, () -> show(SCREEN_RANKING));
        bind(R.id.nav4, () -> {
            startActivity(new Intent(this, ProfileUiActivity.class));
            finish();
        });
        updateLocalProgress();
        updateRanking();
        updateBottomNavigation();
        animateEntrance();
    }

    private void bind(int id, Runnable action) {
        View view = findViewById(id);
        if (view != null) view.setOnClickListener(v -> action.run());
    }

    private void updateLocalProgress() {
        boolean vocabularyDone = LearningProgress.isFinanceVocabularyDone(this);
        boolean listeningWritingDone = LearningProgress.isFinanceListeningWritingDone(this);
        int completed = (vocabularyDone ? 1 : 0) + (listeningWritingDone ? 1 : 0);
        String steps = "Từ vựng: " + (vocabularyDone ? "đã xong" : "chưa xong")
            + " • Nghe và viết: " + (listeningWritingDone ? "đã xong" : "chưa xong");
        setTextIfPresent(R.id.progressCompletion,
            "Đã hoàn tất " + completed + "/2 hoạt động trong chủ đề này.");
        setTextIfPresent(R.id.progressSteps, steps);
        setTextIfPresent(R.id.historyCompletion, steps);
        setTextIfPresent(R.id.historyStepTwo,
            listeningWritingDone ? "Bước nghe và viết đã hoàn tất."
                : "Bước nghe và viết chưa hoàn tất.");
        setTextIfPresent(R.id.historyDetailStatus,
            completed == 2 ? "CHỦ ĐỀ ĐÃ HOÀN TẤT" : "CHỦ ĐỀ ĐANG HỌC");
    }

    private void setTextIfPresent(int id, String text) {
        TextView view = findViewById(id);
        if (view != null) view.setText(text);
    }

    private void updateRanking() {
        if (screen != SCREEN_RANKING) return;
        String[] periods = {"tuần này", "tháng này", "toàn thời gian"};
        setTextIfPresent(R.id.rankingPeriodTitle,
            "✦ Top 10 " + periods[rankPeriod] + " · Minh họa");
        setTextIfPresent(R.id.rankingNote,
            "Dữ liệu mẫu; chưa có bảng xếp hạng " + periods[rankPeriod] + ".");
        int[] buttons = {R.id.rank0, R.id.rank1, R.id.rank2};
        for (int i = 0; i < buttons.length; i++) {
            Button button = findViewById(buttons[i]);
            button.setBackgroundResource(i == rankPeriod
                ? R.drawable.pr_button_bg : R.drawable.pr_tab_bg);
            button.setBackgroundTintList(null);
            button.setTextColor(getColor(i == rankPeriod
                ? R.color.pr_bg : R.color.pr_white));
            button.setSelected(i == rankPeriod);
        }
    }

    private void updateBottomNavigation() {
        int selected = screen == SCREEN_RANKING ? 3 : 2;
        int[] buttons = {R.id.nav0, R.id.nav1, R.id.nav2, R.id.nav3, R.id.nav4};
        for (int i = 0; i < buttons.length; i++) {
            Button button = findViewById(buttons[i]);
            if (button == null) continue;
            button.setBackgroundResource(i == selected
                ? R.drawable.pr_mint_bg : R.drawable.pr_tab_bg);
            button.setBackgroundTintList(null);
            button.setTextColor(getColor(i == selected
                ? R.color.pr_bg : R.color.pr_white));
            button.setSelected(i == selected);
        }
    }

    private void goHome() {
        Intent home = new Intent(this, HomeActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
        finish();
    }

    private void goReview() {
        startActivity(new Intent(this, ReviewFlowActivity.class));
        finish();
    }

    private void animateEntrance() {
        LinearLayout column = findViewById(R.id.entranceColumn);
        if (column == null || Settings.Global.getFloat(getContentResolver(),
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) return;
        float offset = 16 * getResources().getDisplayMetrics().density;
        for (int i = 0; i < column.getChildCount(); i++) {
            View child = column.getChildAt(i);
            if (child.getId() == R.id.backLink) continue;
            child.setAlpha(0f);
            child.setTranslationY(offset);
            child.animate().alpha(1f).translationY(0f)
                .setStartDelay(Math.min(i, 8) * 60L).setDuration(300L)
                .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    @Override public void onBackPressed() {
        if (screen == 3) show(2);
        else if (screen == SCREEN_HISTORY && backToProfile) super.onBackPressed();
        else if (screen == 1 || screen == 2) show(SCREEN_PROGRESS);
        else super.onBackPressed();
    }
}
