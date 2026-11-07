package com.example.english_app;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.english_app.api.HomeApi;
import com.example.english_app.api.AuthRepository;
import com.example.english_app.api.UserApi;
import java.util.ArrayList;
import java.util.List;

/** Native Android Views + Java. No Kotlin / Compose / third-party dependencies. */
public class OnboardingBandActivity extends UserSessionActivity {
    private static final long STAGGER_MS = 130L;
    private static final long DURATION_MS = 350L;
    private final List<HomeApi.Band> bands = new ArrayList<>();
    private final List<View> options = new ArrayList<>();
    private final List<TextView> checks = new ArrayList<>();
    private LinearLayout bandsContainer;
    private TextView continueButton, description;
    private String selectedBandId;
    private boolean motion, userSelectedBand, bandsLoaded;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_band);
        motion = Settings.Global.getFloat(getContentResolver(), "animator_duration_scale", 1f) > 0f;
        continueButton = findViewById(R.id.obContinue);
        description = findViewById(R.id.obDesc);
        bandsContainer = findViewById(R.id.obBandsContainer);
        findViewById(R.id.obBack).setOnClickListener(v -> finish());
        if (savedInstanceState != null) {
            selectedBandId = savedInstanceState.getString("selectedBandId");
            userSelectedBand = savedInstanceState.getBoolean("userSelectedBand", false);
        }
        continueButton.setOnClickListener(v -> {
            if (selectedBandId == null) return;
            Intent intent = new Intent(this, OnboardingTopicsActivity.class);
            intent.putExtra("selectedBand", selectedBandId);
            intent.putExtra(OnboardingProgress.EXTRA_USER_ID,
                getIntent().getStringExtra(OnboardingProgress.EXTRA_USER_ID));
            startActivity(intent);
        });
        loadBands(savedInstanceState == null);
        animateEntry();
    }

    private void loadBands(boolean loadSavedPreference) {
        AuthRepository.get(this).bands(new AuthRepository.Result<HomeApi.BandsResult>() {
            @Override public void success(HomeApi.BandsResult response) {
                if (isFinishing()) return;
                bands.clear();
                if (response != null && response.bands != null) {
                    for (HomeApi.Band band : response.bands) {
                        if (band != null && band.id != null && band.label != null) bands.add(band);
                    }
                }
                bands.sort((a, b) -> Integer.compare(a.sort_order, b.sort_order));
                bandsLoaded = true;
                renderBands();
                if (selectedBandId != null) {
                    selectBandById(selectedBandId, false);
                } else if (loadSavedPreference) {
                    loadSavedBand();
                }
                if (bands.isEmpty()) {
                    description.setText("Chưa có mức nội dung để chọn.");
                }
            }
            @Override public void failure(String code, String message) {
                if (isFinishing()) return;
                bandsLoaded = true;
                description.setText("Không tải được danh sách mức nội dung.");
                setContinueEnabled(false);
                Toast.makeText(OnboardingBandActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderBands() {
        bandsContainer.removeAllViews();
        options.clear();
        checks.clear();
        for (int i = 0; i < bands.size(); i++) {
            HomeApi.Band band = bands.get(i);
            LinearLayout card = new LinearLayout(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
            cardParams.bottomMargin = dp(10);
            card.setLayoutParams(cardParams);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(16), 0, dp(16), 0);
            card.setBackgroundResource(R.drawable.ob_option_normal);
            card.setClickable(true);
            card.setFocusable(true);

            TextView label = new TextView(this);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            label.setLayoutParams(labelParams);
            label.setText(band.label);
            label.setTextColor(getColor(R.color.ob_white));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            label.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView check = new TextView(this);
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(24), dp(24));
            checkParams.leftMargin = dp(12);
            check.setLayoutParams(checkParams);
            check.setGravity(Gravity.CENTER);
            check.setText("✓");
            check.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
            check.setTypeface(null, android.graphics.Typeface.BOLD);
            check.setTextColor(getColor(R.color.ob_mint));
            check.setVisibility(View.INVISIBLE);
            card.addView(label);
            card.addView(check);
            bandsContainer.addView(card);
            options.add(card);
            checks.add(check);
            card.setOnClickListener(v -> {
                userSelectedBand = true;
                selectBandById(band.id, true);
            });
            if (motion) {
                card.setAlpha(0f);
                card.setTranslationY(dp(18));
                card.animate().alpha(1f).translationY(0f)
                    .setStartDelay(STAGGER_MS * i).setDuration(DURATION_MS)
                    .setInterpolator(new DecelerateInterpolator()).start();
            }
        }
        setContinueEnabled(selectedBandId != null && findBandIndex(selectedBandId) >= 0);
    }

    private void loadSavedBand() {
        AuthRepository.get(this).preferences(new AuthRepository.Result<UserApi.LearnerPreferences>() {
            @Override public void success(UserApi.LearnerPreferences preferences) {
                if (isFinishing() || userSelectedBand || preferences == null) return;
                selectBandById(preferences.band, false);
            }
            @Override public void failure(String code, String message) {
                // The learner can choose from the server-provided list without a saved preference.
            }
        });
    }

    private void selectBandById(String bandId, boolean animateSelection) {
        int index = findBandIndex(bandId);
        if (index < 0) return;
        selectedBandId = bands.get(index).id;
        for (int i = 0; i < options.size(); i++) {
            View card = options.get(i);
            boolean selected = i == index;
            card.setBackgroundResource(selected
                ? R.drawable.ob_option_selected : R.drawable.ob_option_normal);
            card.setSelected(selected);
            checks.get(i).setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
        View selected = options.get(index);
        if (animateSelection && motion) {
            selected.animate().cancel();
            selected.animate().scaleX(1.015f).scaleY(1.015f).setDuration(120)
                .withEndAction(() -> selected.animate().scaleX(1f).scaleY(1f)
                    .setDuration(150).start()).start();
        }
        setContinueEnabled(true);
    }

    private int findBandIndex(String id) {
        if (id == null) return -1;
        for (int i = 0; i < bands.size(); i++) {
            if (id.equals(bands.get(i).id) || id.equals(bands.get(i).label)) return i;
        }
        return -1;
    }

    private void setContinueEnabled(boolean enabled) {
        continueButton.setEnabled(enabled);
        continueButton.setText(enabled ? "Tiếp tục  →"
            : !bandsLoaded ? "Đang tải mức nội dung…"
            : bands.isEmpty() ? "Chưa có mức nội dung"
            : "Chọn một mức để tiếp tục  →");
        continueButton.setTextColor(enabled ? 0xFF151A35 : getColor(R.color.ob_white));
        continueButton.setBackgroundResource(enabled
            ? R.drawable.ob_continue_bg : R.drawable.ob_continue_disabled);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void animateEntry() {
        if (!motion) return;
        View[] sequence = {
            findViewById(R.id.obBack), findViewById(R.id.obHeading),
            findViewById(R.id.obDesc), continueButton
        };
        float distance = dp(18);
        for (View item : sequence) {
            item.animate().cancel();
            item.setAlpha(0f);
            item.setTranslationY(distance);
        }
        for (int i = 0; i < sequence.length; i++) {
            View item = sequence[i];
            item.animate().alpha(1f).translationY(0f).setStartDelay(STAGGER_MS * i)
                .setDuration(DURATION_MS).setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putString("selectedBandId", selectedBandId);
        outState.putBoolean("userSelectedBand", userSelectedBand);
        super.onSaveInstanceState(outState);
    }
}
