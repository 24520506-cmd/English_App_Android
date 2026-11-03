package com.example.english_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import com.example.english_app.api.AuthRepository;
import com.example.english_app.api.UserApi;
import java.util.ArrayList;

public class OnboardingTopicsActivity extends Activity {
    private static final long STEP = 130L, DURATION = 350L;
    private static final String[] LABELS = {
        "Daily Life", "Education", "Technology", "Environment",
        "Finance", "Travel", "Health", "Work"
    };
    private static final String[] TOPIC_IDS = {
        "daily_life", "education", "technology", "environment",
        "finance", "travel", "health", "work"
    };
    private final int[] ids = {
        R.id.topic0, R.id.topic1, R.id.topic2, R.id.topic3,
        R.id.topic4, R.id.topic5, R.id.topic6, R.id.topic7
    };
    private TextView next, note;
    private boolean motion, applyingSavedTopics, topicsTouched, saving;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_onboarding_topics);
        motion = Settings.Global.getFloat(getContentResolver(),
            "animator_duration_scale", 1f) > 0f;
        next = findViewById(R.id.obContinue);
        note = findViewById(R.id.obSelectionNote);
        findViewById(R.id.obBack).setOnClickListener(v -> finish());

        for (int i = 0; i < ids.length; i++) {
            CheckBox checkBox = findViewById(ids[i]);
            if (state != null) checkBox.setChecked(state.getBoolean("topic_" + i, false));
            checkBox.setOnCheckedChangeListener((button, checked) -> {
                if (!applyingSavedTopics) topicsTouched = true;
                refresh();
            });
        }
        refresh();
        next.setOnClickListener(v -> savePreferencesAndContinue());
        if (state == null) loadSavedTopics();
        animateEntry();
    }

    private ArrayList<String> getSelectedLabels() {
        ArrayList<String> out = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            if (((CheckBox) findViewById(ids[i])).isChecked()) out.add(LABELS[i]);
        }
        return out;
    }

    private ArrayList<String> getSelectedTopicIds() {
        ArrayList<String> out = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            if (((CheckBox) findViewById(ids[i])).isChecked()) out.add(TOPIC_IDS[i]);
        }
        return out;
    }

    private void loadSavedTopics() {
        AuthRepository.get(this).preferences(new AuthRepository.Result<UserApi.LearnerPreferences>() {
            @Override public void success(UserApi.LearnerPreferences preferences) {
                if (isFinishing() || topicsTouched || preferences == null
                    || preferences.topic_ids == null) return;
                applyingSavedTopics = true;
                for (int i = 0; i < ids.length; i++) {
                    ((CheckBox) findViewById(ids[i]))
                        .setChecked(preferences.topic_ids.contains(TOPIC_IDS[i]));
                }
                applyingSavedTopics = false;
                refresh();
            }
            @Override public void failure(String code, String message) {
                // The learner can still choose topics and save them in this session.
            }
        });
    }

    private void savePreferencesAndContinue() {
        if (saving) return;
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
                    next.setEnabled(true);
                    next.setText("Lưu lại và tiếp tục  →");
                    Toast.makeText(OnboardingTopicsActivity.this, message,
                        Toast.LENGTH_LONG).show();
                }
            });
    }

    private void refresh() {
        int count = 0;
        for (int id : ids) {
            CheckBox checkBox = findViewById(id);
            boolean checked = checkBox.isChecked();
            if (checked) count++;
            checkBox.setBackgroundResource(checked
                ? R.drawable.ob_choice_selected : R.drawable.ob_choice);
        }
        note.setText(count + " chủ đề đã chọn");
        next.setEnabled(count > 0 && !saving);
        next.setAlpha(count > 0 && !saving ? 1f : .5f);
    }

    private void animateEntry() {
        if (!motion) return;
        View[] views = {
            findViewById(R.id.obBack), findViewById(R.id.obTitle),
            findViewById(R.id.obSubtitle), findViewById(R.id.obDesc), note,
            findViewById(R.id.topic0), findViewById(R.id.topic1),
            findViewById(R.id.topic2), findViewById(R.id.topic3),
            findViewById(R.id.topic4), findViewById(R.id.topic5),
            findViewById(R.id.topic6), findViewById(R.id.topic7),
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
        for (int i = 0; i < ids.length; i++) {
            out.putBoolean("topic_" + i, ((CheckBox) findViewById(ids[i])).isChecked());
        }
        super.onSaveInstanceState(out);
    }
}
