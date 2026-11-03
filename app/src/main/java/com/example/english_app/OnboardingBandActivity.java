package com.example.english_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import com.example.english_app.api.AuthRepository;
import com.example.english_app.api.UserApi;

/** Native Android Views + Java. No Kotlin / Compose / third-party dependencies. */
public class OnboardingBandActivity extends Activity {
    private static final long STAGGER_MS = 130L;
    private static final long DURATION_MS = 350L;
    private static final String[] BAND_VALUES = {"BELOW_4_0", "4_0_5_0", "5_5_6_5", "7_0_PLUS"};
    private final int[] optionIds = {R.id.option_below40, R.id.option_from40to50, R.id.option_from55to65, R.id.option_above70};
    private final int[] checkIds = {R.id.check_below40, R.id.check_from40to50, R.id.check_from55to65, R.id.check_above70};
    private int selected = -1;
    private TextView continueButton;
    private boolean motion;
    private boolean userSelectedBand;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_band);
        motion = Settings.Global.getFloat(getContentResolver(), "animator_duration_scale", 1f) > 0f;
        continueButton = findViewById(R.id.obContinue);
        findViewById(R.id.obBack).setOnClickListener(v -> finish());
        for (int i = 0; i < optionIds.length; i++) {
            final int index = i;
            findViewById(optionIds[i]).setOnClickListener(v -> {
                userSelectedBand = true;
                selectBand(index);
            });
        }
        continueButton.setOnClickListener(v -> {
            if (selected < 0) return;
            Intent intent = new Intent(this, OnboardingTopicsActivity.class);
            intent.putExtra("selectedBand", BAND_VALUES[selected]);
            intent.putExtra(OnboardingProgress.EXTRA_USER_ID,
                getIntent().getStringExtra(OnboardingProgress.EXTRA_USER_ID));
            startActivity(intent);
        });
        if (savedInstanceState != null) {
            int restored = savedInstanceState.getInt("selectedBand", -1);
            if (restored >= 0 && restored < optionIds.length) selectBand(restored);
            userSelectedBand = savedInstanceState.getBoolean("userSelectedBand", restored >= 0);
        } else {
            loadSavedBand();
        }
        playEntranceAnimation();
    }

    private void loadSavedBand() {
        AuthRepository.get(this).preferences(new AuthRepository.Result<UserApi.LearnerPreferences>() {
            @Override public void success(UserApi.LearnerPreferences preferences) {
                if (isFinishing() || userSelectedBand || preferences == null) return;
                if (preferences.band != null) {
                    for (int i = 0; i < BAND_VALUES.length; i++) {
                        if (BAND_VALUES[i].equals(preferences.band)) {
                            selectBand(i);
                            break;
                        }
                    }
                }
            }
            @Override public void failure(String code, String message) {
                // A learner can still choose a band while saved preferences are unavailable.
            }
        });
    }

    private void selectBand(int index) {
        selected = index;
        for (int i = 0; i < optionIds.length; i++) {
            View card = findViewById(optionIds[i]);
            boolean isSelected = i == index;
            card.setBackgroundResource(isSelected ? R.drawable.ob_option_selected : R.drawable.ob_option_normal);
            card.setSelected(isSelected);
            findViewById(checkIds[i]).setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
        }
        View current = findViewById(optionIds[index]);
        if (motion) {
            current.animate().cancel();
            current.animate().scaleX(1.015f).scaleY(1.015f).setDuration(120)
                .withEndAction(() -> current.animate().scaleX(1f).scaleY(1f).setDuration(150).start()).start();
        }
        continueButton.setEnabled(true);
        continueButton.setText("Tiếp tục  →");
        continueButton.setTextColor(0xFF151A35);
        continueButton.setBackgroundResource(R.drawable.ob_continue_bg);
    }

    private void playEntranceAnimation() {
        if (!motion) return;
        View[] sequence = {
            findViewById(R.id.obBack), findViewById(R.id.obHeading), findViewById(R.id.obDesc),
            findViewById(R.id.option_below40), findViewById(R.id.option_from40to50),
            findViewById(R.id.option_from55to65), findViewById(R.id.option_above70), continueButton
        };
        float distance = getResources().getDisplayMetrics().density * 18f;
        for (View item : sequence) {
            item.animate().cancel(); item.setAlpha(0f); item.setTranslationY(distance);
        }
        // 8 elements: (8-1)*130+350 = 1260ms
        for (int i = 0; i < sequence.length; i++) {
            View item = sequence[i];
            item.animate().alpha(1f).translationY(0f).setStartDelay(i*STAGGER_MS)
                .setDuration(DURATION_MS).setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("selectedBand", selected);
        outState.putBoolean("userSelectedBand", userSelectedBand);
        super.onSaveInstanceState(outState);
    }
}
