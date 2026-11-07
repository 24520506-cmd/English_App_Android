package com.example.english_app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.english_app.api.HomeApi;
import com.example.english_app.api.AuthRepository;
import com.example.english_app.api.UserApi;
import java.util.ArrayList;
import java.util.List;

public class OnboardingTopicsActivity extends UserSessionActivity {
    private static final long STEP = 130L, DURATION = 350L;
    private final List<HomeApi.Topic> topics = new ArrayList<>();
    private final List<CheckBox> topicChecks = new ArrayList<>();
    private TextView next, note;
    private LinearLayout topicsContainer;
    private boolean motion, applyingSavedTopics, topicsTouched, saving, topicsLoaded;
    private ArrayList<String> restoredTopicIds;
    private String topicsLoadMessage;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_onboarding_topics);
        motion = Settings.Global.getFloat(getContentResolver(),
            "animator_duration_scale", 1f) > 0f;
        next = findViewById(R.id.obContinue);
        note = findViewById(R.id.obSelectionNote);
        topicsContainer = findViewById(R.id.obTopicsContainer);
        findViewById(R.id.obBack).setOnClickListener(v -> finish());
        if (state != null) {
            restoredTopicIds = state.getStringArrayList("selected_topic_ids");
            topicsTouched = state.getBoolean("topics_touched", false);
        }
        next.setOnClickListener(v -> savePreferencesAndContinue());
        refresh();
        loadTopics(state != null);
        animateEntry();
    }

    private void loadTopics(boolean restoringState) {
        AuthRepository.get(this).topics(new AuthRepository.Result<HomeApi.TopicsResult>() {
            @Override public void success(HomeApi.TopicsResult response) {
                if (isFinishing()) return;
                topics.clear();
                if (response != null && response.topics != null) {
                    for (HomeApi.Topic topic : response.topics) {
                        if (topic != null && topic.id != null && topic.label != null) {
                            topics.add(topic);
                        }
                    }
                }
                topics.sort((a, b) -> Integer.compare(a.sort_order, b.sort_order));
                if (topics.isEmpty()) {
                    topicsLoaded = true;
                    topicsLoadMessage = "Chưa có chủ đề để chọn.";
                    refresh();
                    return;
                }
                renderTopics();
                topicsLoaded = true;
                refresh();
                if (restoringState && restoredTopicIds != null) {
                    applySelectedTopics(restoredTopicIds);
                } else {
                    loadSavedTopics();
                }
            }

            @Override public void failure(String code, String message) {
                if (isFinishing()) return;
                topicsLoaded = false;
                topicsLoadMessage = "Không tải được danh sách chủ đề.";
                Toast.makeText(OnboardingTopicsActivity.this, message, Toast.LENGTH_LONG).show();
                refresh();
            }
        });
    }

    private void renderTopics() {
        topicsContainer.removeAllViews();
        topicChecks.clear();
        for (HomeApi.Topic topic : topics) {
            CheckBox checkBox = new CheckBox(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
            params.topMargin = dp(8);
            checkBox.setLayoutParams(params);
            checkBox.setBackgroundResource(R.drawable.ob_choice);
            checkBox.setButtonTintList(ColorStateList.valueOf(getColor(R.color.ob_mint)));
            checkBox.setPadding(dp(14), 0, dp(12), 0);
            checkBox.setGravity(Gravity.CENTER_VERTICAL);
            checkBox.setText(topic.label);
            checkBox.setTextColor(getColor(R.color.ob_white));
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            checkBox.setContentDescription("Chọn chủ đề " + topic.label);
            topicsContainer.addView(checkBox);
            topicChecks.add(checkBox);
        }
        applySelectedTopics(restoredTopicIds);
        for (CheckBox checkBox : topicChecks) {
            checkBox.setOnCheckedChangeListener((button, checked) -> {
                if (!applyingSavedTopics) topicsTouched = true;
                checkBox.setBackgroundResource(checked
                    ? R.drawable.ob_choice_selected : R.drawable.ob_choice);
                refresh();
            });
        }
    }

    private void applySelectedTopics(List<String> selectedIds) {
        if (selectedIds == null) return;
        applyingSavedTopics = true;
        for (int i = 0; i < topics.size(); i++) {
            topicChecks.get(i).setChecked(selectedIds.contains(topics.get(i).id));
            topicChecks.get(i).setBackgroundResource(topicChecks.get(i).isChecked()
                ? R.drawable.ob_choice_selected : R.drawable.ob_choice);
        }
        applyingSavedTopics = false;
        refresh();
    }

    private ArrayList<String> getSelectedLabels() {
        ArrayList<String> selected = new ArrayList<>();
        for (int i = 0; i < topics.size(); i++) {
            if (topicChecks.get(i).isChecked()) selected.add(topics.get(i).label);
        }
        return selected;
    }

    private ArrayList<String> getSelectedTopicIds() {
        ArrayList<String> selected = new ArrayList<>();
        for (int i = 0; i < topics.size(); i++) {
            if (topicChecks.get(i).isChecked()) selected.add(topics.get(i).id);
        }
        return selected;
    }

    private void loadSavedTopics() {
        AuthRepository.get(this).preferences(new AuthRepository.Result<UserApi.LearnerPreferences>() {
            @Override public void success(UserApi.LearnerPreferences preferences) {
                if (isFinishing() || topicsTouched || preferences == null
                    || preferences.topic_ids == null) return;
                applySelectedTopics(preferences.topic_ids);
            }
            @Override public void failure(String code, String message) {
                // Keep the server topic list usable when no saved preferences exist yet.
            }
        });
    }

    private void savePreferencesAndContinue() {
        if (saving || !topicsLoaded) return;
        ArrayList<String> selectedTopics = getSelectedLabels();
        ArrayList<String> selectedTopicIds = getSelectedTopicIds();
        if (selectedTopicIds.isEmpty()) return;
        String band = getIntent().getStringExtra("selectedBand");
        if (band == null || band.isEmpty()) {
            Toast.makeText(this, "Thiếu band học. Vui lòng quay lại chọn band.",
                Toast.LENGTH_LONG).show();
            return;
        }

        saving = true;
        next.setEnabled(false);
        next.setText("Đang lưu...");
        AuthRepository.get(this).updatePreferences(band, selectedTopicIds,
            new AuthRepository.Result<Void>() {
                @Override public void success(Void ignored) {
                    if (isFinishing()) return;
                    Intent intent = new Intent(OnboardingTopicsActivity.this,
                        OnboardingNameActivity.class);
                    intent.putStringArrayListExtra("selectedTopics", selectedTopics);
                    intent.putExtra("selectedBand", band);
                    intent.putExtra(OnboardingProgress.EXTRA_USER_ID,
                        getIntent().getStringExtra(OnboardingProgress.EXTRA_USER_ID));
                    startActivity(intent);
                }

                @Override public void failure(String code, String message) {
                    if (isFinishing()) return;
                    saving = false;
                    next.setText("Lưu lại và tiếp tục  →");
                    Toast.makeText(OnboardingTopicsActivity.this, message,
                        Toast.LENGTH_LONG).show();
                    refresh();
                }
            });
    }

    private void refresh() {
        int count = 0;
        for (CheckBox checkBox : topicChecks) if (checkBox.isChecked()) count++;
        if (!topicsLoaded) {
            note.setText(topicsLoadMessage == null ? "Đang tải chủ đề..." : topicsLoadMessage);
        } else if (topics.isEmpty()) {
            note.setText("Chưa có chủ đề để chọn.");
        } else {
            note.setText(count + " chủ đề đã chọn");
        }
        boolean enabled = topicsLoaded && count > 0 && !saving;
        next.setEnabled(enabled);
        next.setAlpha(enabled ? 1f : .5f);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void animateEntry() {
        if (!motion) return;
        View[] views = {
            findViewById(R.id.obBack), findViewById(R.id.obTitle),
            findViewById(R.id.obSubtitle), findViewById(R.id.obDesc), note,
            findViewById(R.id.obContinueWrap)
        };
        float distance = 16 * getResources().getDisplayMetrics().density;
        for (View view : views) {
            view.setAlpha(0);
            view.setTranslationY(distance);
        }
        for (int i = 0; i < views.length; i++) {
            views[i].animate().alpha(1f).translationY(0).setStartDelay(STEP * i)
                .setDuration(DURATION).setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putStringArrayList("selected_topic_ids", getSelectedTopicIds());
        out.putBoolean("topics_touched", topicsTouched);
        super.onSaveInstanceState(out);
    }
}
