package com.example.english_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import com.example.english_app.api.AuthRepository;
import com.example.english_app.api.UserApi;
import java.util.ArrayList;

public class ProfileUiActivity extends Activity {
    public static final String EXTRA_SCREEN = "profile_screen";
    private static final String STATE_PAGE = "current_profile_page";
    private static final String[] SCREENS = {
        "report_name", "profile", "profile_edit", "preferences",
        "notification_settings", "account_settings", "change_password",
        "password_changed", "active_sessions", "sessions_revoke_confirm"
    };
    private static final int[] LAYOUTS = {
        R.layout.activity_pf_report_name,
        R.layout.activity_pf_profile,
        R.layout.activity_pf_profile_edit,
        R.layout.activity_pf_preferences,
        R.layout.activity_pf_notification_settings,
        R.layout.activity_pf_account_settings,
        R.layout.activity_pf_change_password,
        R.layout.activity_pf_password_changed,
        R.layout.activity_pf_active_sessions,
        R.layout.activity_pf_sessions_revoke_confirm
    };
    private static final int PAGE_PROFILE = 1;
    private static final int PAGE_PROFILE_EDIT = 2;
    private static final int PAGE_PREFERENCES = 3;
    private static final int[] PREFERENCE_BANDS = {
        R.id.pfBand0, R.id.pfBand1, R.id.pfBand2, R.id.pfBand3
    };
    private static final String[] PREFERENCE_BAND_VALUES = {
        "BELOW_4_0", "4_0_5_0", "5_5_6_5", "7_0_PLUS"
    };
    private static final int[] PREFERENCE_TOPICS = {
        R.id.pfTopic0, R.id.pfTopic1, R.id.pfTopic2, R.id.pfTopic3,
        R.id.pfTopic4, R.id.pfTopic5, R.id.pfTopic6, R.id.pfTopic7
    };
    private static final String[] PREFERENCE_TOPIC_IDS = {
        "daily_life", "education", "technology", "environment",
        "finance", "travel", "health", "work"
    };
    private int page;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff151932);
        getWindow().setNavigationBarColor(0xff151932);
        page = state == null ? position(getIntent().getStringExtra(EXTRA_SCREEN))
            : Math.max(0, Math.min(SCREENS.length - 1,
                state.getInt(STATE_PAGE, PAGE_PROFILE)));
        render();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_PAGE, page);
        super.onSaveInstanceState(outState);
    }

    private int position(String name) {
        for (int i = 0; i < SCREENS.length; i++) {
            if (SCREENS[i].equals(name)) return i;
        }
        return PAGE_PROFILE;
    }

    private void go(String screen) {
        page = position(screen);
        render();
    }

    private void bind(int id, Runnable action) {
        View view = findViewById(id);
        if (view != null) view.setOnClickListener(v -> action.run());
    }

    private void render() {
        setContentView(LAYOUTS[page]);
        bind(R.id.pfBack, this::onBackPressed);
        bind(R.id.pfOpenPreferences, () -> go("preferences"));
        bind(R.id.pfOpenAccount, () -> go("account_settings"));
        bind(R.id.pfOpenEditName, () -> go("profile_edit"));
        bind(R.id.pfOpenNotifications, () -> go("notification_settings"));
        bind(R.id.pfOpenReport, () -> go("report_name"));
        bind(R.id.pfOpenHistory, this::openHistory);
        bind(R.id.pfLogout, () -> StateScreensActivity.open(this, "profile_logout_confirm"));
        bind(R.id.pfOpenChangePassword, () -> go("change_password"));
        bind(R.id.pfOpenSessions, () -> go("active_sessions"));
        bind(R.id.pfOpenRevoke, () -> go("sessions_revoke_confirm"));
        bind(R.id.pfCancelRevoke, () -> go("active_sessions"));
        bind(R.id.pfContactInfo, () -> info("Thông tin liên hệ chưa được kết nối với tài khoản."));

        bind(R.id.pfReportSubmit, () -> {
            EditText reason = findViewById(R.id.pfReportReason);
            if (reason.getText().toString().trim().length() < 3) {
                reason.setError("Vui lòng mô tả lý do");
                return;
            }
            info("Báo cáo chưa được gửi; cần kết nối API tài khoản.");
        });
        bind(R.id.pfSaveName, this::saveProfileDisplayName);
        for (int i = 0; i < PREFERENCE_BANDS.length; i++) {
            final int selected = i;
            bind(PREFERENCE_BANDS[i], () -> {
                for (int j = 0; j < PREFERENCE_BANDS.length; j++) {
                    CheckBox option = findViewById(PREFERENCE_BANDS[j]);
                    option.setChecked(j == selected);
                }
            });
        }
        bind(R.id.pfSavePreferences, this::savePreferences);
        bind(R.id.pfSaveNotify, () ->
            info("Tùy chọn thông báo chưa được lưu hoặc kích hoạt."));
        bind(R.id.pfChangePwd, () -> {
            EditText oldPassword = findViewById(R.id.pfOldPwd);
            EditText newPassword = findViewById(R.id.pfNewPwd);
            EditText confirmation = findViewById(R.id.pfConfirmPwd);
            if (oldPassword.length() == 0 || newPassword.length() == 0) {
                info("Nhập đầy đủ mật khẩu.");
                return;
            }
            if (!newPassword.getText().toString().equals(
                confirmation.getText().toString())) {
                confirmation.setError("Mật khẩu không khớp");
                return;
            }
            info("Mật khẩu chưa được đổi; cần API xác thực tài khoản.");
        });
        bind(R.id.pfConfirmRevoke, () ->
            info("Các phiên chưa bị thu hồi; cần API tài khoản."));
        bind(R.id.pfGoLogin, this::goLogin);

        bind(R.id.pfNav0, this::goHome);
        bind(R.id.pfNav1, () -> openActivity(ReviewFlowActivity.class));
        bind(R.id.pfNav2, () -> openProgress(ProgressRankingActivity.SCREEN_PROGRESS));
        bind(R.id.pfNav3, () -> openProgress(ProgressRankingActivity.SCREEN_RANKING));
        bind(R.id.pfNav4, () -> go("profile"));
        highlightProfileTab();
        animateEntrance();
        if (page == PAGE_PROFILE || page == PAGE_PROFILE_EDIT) loadAccount();
        if (page == PAGE_PREFERENCES) loadPreferences();
    }

    private void loadPreferences() {
        AuthRepository.get(this).preferences(
            new AuthRepository.Result<UserApi.LearnerPreferences>() {
                @Override public void success(UserApi.LearnerPreferences preferences) {
                    if (isFinishing() || page != PAGE_PREFERENCES || preferences == null) return;
                    for (int i = 0; i < PREFERENCE_BANDS.length; i++) {
                        ((CheckBox) findViewById(PREFERENCE_BANDS[i])).setChecked(
                            PREFERENCE_BAND_VALUES[i].equals(preferences.band));
                    }
                    for (int i = 0; i < PREFERENCE_TOPICS.length; i++) {
                        boolean selected = preferences.topic_ids != null
                            && preferences.topic_ids.contains(PREFERENCE_TOPIC_IDS[i]);
                        ((CheckBox) findViewById(PREFERENCE_TOPICS[i])).setChecked(selected);
                    }
                }
                @Override public void failure(String code, String message) {
                    if (!isFinishing() && page == PAGE_PREFERENCES) info(message);
                }
            });
    }

    private void savePreferences() {
        String band = null;
        for (int i = 0; i < PREFERENCE_BANDS.length; i++) {
            if (((CheckBox) findViewById(PREFERENCE_BANDS[i])).isChecked()) {
                band = PREFERENCE_BAND_VALUES[i];
                break;
            }
        }
        ArrayList<String> topics = new ArrayList<>();
        for (int i = 0; i < PREFERENCE_TOPICS.length; i++) {
            if (((CheckBox) findViewById(PREFERENCE_TOPICS[i])).isChecked()) {
                topics.add(PREFERENCE_TOPIC_IDS[i]);
            }
        }
        if (band == null || topics.isEmpty()) {
            info("Chọn một mức nội dung và ít nhất một chủ đề.");
            return;
        }
        View saveButton = findViewById(R.id.pfSavePreferences);
        if (saveButton != null) saveButton.setEnabled(false);
        AuthRepository.get(this).updatePreferences(band, topics,
            new AuthRepository.Result<Void>() {
                @Override public void success(Void ignored) {
                    if (isFinishing() || page != PAGE_PREFERENCES) return;
                    if (saveButton != null) saveButton.setEnabled(true);
                    info("Lựa chọn học tập đã được lưu.");
                }
                @Override public void failure(String code, String message) {
                    if (isFinishing()) return;
                    if (saveButton != null) saveButton.setEnabled(true);
                    info(message);
                }
            });
    }

    private void loadAccount() {
        TextView accountLabel = findViewById(R.id.pfAccountUsername);
        AuthRepository.get(this).profile(new AuthRepository.Result<UserApi.Account>() {
            @Override public void success(UserApi.Account account) {
                if (page == PAGE_PROFILE && accountLabel != null) {
                    accountLabel.setText("Tài khoản: " + account.username);
                    TextView currentName = findViewById(R.id.pfProfileDisplayName);
                    if (currentName != null) currentName.setText("Tên riêng: "
                        + displayName(account));
                } else if (page == PAGE_PROFILE_EDIT) {
                    EditText name = findViewById(R.id.pfDisplayName);
                    if (name != null) name.setText(displayName(account));
                }
            }
            @Override public void failure(String code, String message) {
                if (page != PAGE_PROFILE && page != PAGE_PROFILE_EDIT) return;
                if (code.startsWith("SESSION_") || "REFRESH_REUSE_DETECTED".equals(code)) {
                    StateScreensActivity.openAsRoot(ProfileUiActivity.this, "session_expired");
                }
                else if (accountLabel != null) accountLabel.setText(message);
                else info(message);
            }
        });
    }

    private String displayName(UserApi.Account account) {
        if (account.profile == null || account.profile.display_name == null
            || account.profile.display_name.trim().isEmpty()) return account.username;
        return account.profile.display_name.trim();
    }

    private void saveProfileDisplayName() {
        EditText field = findViewById(R.id.pfDisplayName);
        if (field == null) return;
        String value = field.getText().toString().trim();
        if (!value.isEmpty() && (value.length() < 2 || value.length() > 30)) {
            field.setError("Tên riêng cần từ 2 đến 30 ký tự");
            return;
        }
        View saveButton = findViewById(R.id.pfSaveName);
        if (saveButton != null) saveButton.setEnabled(false);
        AuthRepository.get(this).updateProfile(value.isEmpty() ? null : value,
            new AuthRepository.Result<UserApi.Account>() {
                @Override public void success(UserApi.Account account) {
                    if (isFinishing() || page != PAGE_PROFILE_EDIT) return;
                    if (saveButton != null) saveButton.setEnabled(true);
                    go("profile");
                    info("Tên riêng đã được lưu.");
                }

                @Override public void failure(String code, String message) {
                    if (isFinishing()) return;
                    if (saveButton != null) saveButton.setEnabled(true);
                    field.setError(message);
                }
            });
    }

    private void openHistory() {
        Intent intent = new Intent(this, ProgressRankingActivity.class);
        intent.putExtra(ProgressRankingActivity.EXTRA_SCREEN,
            ProgressRankingActivity.SCREEN_HISTORY);
        intent.putExtra(ProgressRankingActivity.EXTRA_BACK_TO_PROFILE, true);
        startActivity(intent);
    }

    private void openProgress(int screen) {
        Intent intent = new Intent(this, ProgressRankingActivity.class);
        intent.putExtra(ProgressRankingActivity.EXTRA_SCREEN, screen);
        startActivity(intent);
        finish();
    }

    private void openActivity(Class<?> target) {
        startActivity(new Intent(this, target));
        finish();
    }

    private void goHome() {
        Intent home = new Intent(this, HomeActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
        finish();
    }

    private void goLogin() {
        Intent login = new Intent(this, LoginActivity.class);
        login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(login);
        finish();
    }

    private void info(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void highlightProfileTab() {
        Button tab = findViewById(R.id.pfNav4);
        if (tab == null) return;
        tab.setBackgroundResource(R.drawable.pf_outline_bg);
        tab.setBackgroundTintList(null);
        tab.setTextColor(getColor(R.color.pf_mint));
        tab.setSelected(true);
    }

    private void animateEntrance() {
        LinearLayout column = findViewById(R.id.pfContent);
        if (column == null || Settings.Global.getFloat(getContentResolver(),
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) return;
        float offset = 16 * getResources().getDisplayMetrics().density;
        for (int i = 0; i < column.getChildCount(); i++) {
            View child = column.getChildAt(i);
            if (child.getId() == R.id.pfBack) continue;
            child.setAlpha(0f);
            child.setTranslationY(offset);
            child.animate().alpha(1f).translationY(0f)
                .setStartDelay(Math.min(i, 8) * 60L).setDuration(300L)
                .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    @Override public void onBackPressed() {
        if (page == 9) go("active_sessions");
        else if (page == 6 || page == 8) go("account_settings");
        else if (page == 7) goLogin();
        else if (page != PAGE_PROFILE) go("profile");
        else super.onBackPressed();
    }
}
