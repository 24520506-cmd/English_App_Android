package com.example.english_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.util.TypedValue;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import com.example.english_app.api.AuthApi;
import com.example.english_app.api.HomeApi;
import com.example.english_app.api.AuthRepository;
import com.example.english_app.api.UserApi;
import java.util.ArrayList;
import java.util.List;

public class ProfileUiActivity extends UserSessionActivity {
    public static final String EXTRA_SCREEN = "profile_screen";
    private static final String STATE_PAGE = "current_profile_page";
    private static final String[] SCREENS = {
        "profile", "profile_edit", "preferences",
        "notification_settings", "account_settings", "change_password",
        "password_changed", "active_sessions", "sessions_revoke_confirm"
    };
    private static final int[] LAYOUTS = {
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
    private static final int PAGE_PROFILE = 0;
    private static final int PAGE_PROFILE_EDIT = 1;
    private static final int PAGE_PREFERENCES = 2;
    private final List<HomeApi.Band> preferenceBands = new ArrayList<>();
    private final List<CheckBox> preferenceBandChecks = new ArrayList<>();
    private final List<HomeApi.Topic> preferenceTopics = new ArrayList<>();
    private final List<CheckBox> preferenceTopicChecks = new ArrayList<>();
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
        if (page == PAGE_PREFERENCES) {
            preferenceBands.clear();
            preferenceBandChecks.clear();
            preferenceTopics.clear();
            preferenceTopicChecks.clear();
            int[] oldBandViews = { R.id.pfBand0, R.id.pfBand1, R.id.pfBand2,
                R.id.pfBand3, R.id.pfBand4, R.id.pfBand5, R.id.pfBand6 };
            for (int id : oldBandViews) findViewById(id).setVisibility(View.GONE);
            int[] oldTopicViews = { R.id.pfTopic0, R.id.pfTopic1, R.id.pfTopic2,
                R.id.pfTopic3, R.id.pfTopic4, R.id.pfTopic5, R.id.pfTopic6, R.id.pfTopic7 };
            for (int id : oldTopicViews) findViewById(id).setVisibility(View.GONE);
        }
        bind(R.id.pfBack, this::onBackPressed);
        bind(R.id.pfOpenPreferences, () -> go("preferences"));
        bind(R.id.pfOpenAccount, () -> go("account_settings"));
        bind(R.id.pfOpenEditName, () -> go("profile_edit"));
        bind(R.id.pfOpenNotifications, () -> go("notification_settings"));
        bind(R.id.pfOpenHistory, this::openHistory);
        bind(R.id.pfLogout, () -> StateScreensActivity.open(this, "profile_logout_confirm"));
        bind(R.id.pfOpenChangePassword, () -> go("change_password"));
        bind(R.id.pfOpenSessions, () -> go("active_sessions"));
        bind(R.id.pfOpenRevoke, () -> go("sessions_revoke_confirm"));
        bind(R.id.pfCancelRevoke, () -> go("active_sessions"));
        bind(R.id.pfVerifyEmail, this::startEmailVerification);

        bind(R.id.pfSaveName, this::saveProfileDisplayName);
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
            View button = findViewById(R.id.pfChangePwd);
            button.setEnabled(false);
            AuthRepository.get(this).changePassword(oldPassword.getText().toString(),
                newPassword.getText().toString(), new AuthRepository.Result<Void>() {
                    @Override public void success(Void ignored) {
                        if (isFinishing()) return;
                        button.setEnabled(true);
                        oldPassword.setText(""); newPassword.setText(""); confirmation.setText("");
                        go("password_changed");
                    }
                    @Override public void failure(String code, String message) {
                        if (isFinishing()) return;
                        button.setEnabled(true);
                        info(message);
                    }
                });
        });
        bind(R.id.pfConfirmRevoke, this::revokeAllSessions);
        bind(R.id.pfGoLogin, this::goLogin);

        bind(R.id.pfNav0, this::goHome);
        bind(R.id.pfNav1, () -> openActivity(ReviewFlowActivity.class));
        bind(R.id.pfNav2, () -> openProgress(ProgressRankingActivity.SCREEN_PROGRESS));
        bind(R.id.pfNav3, () -> openProgress(ProgressRankingActivity.SCREEN_RANKING));
        bind(R.id.pfNav4, () -> go("profile"));
        highlightProfileTab();
        animateEntrance();
        if (page == PAGE_PROFILE || page == PAGE_PROFILE_EDIT || page == position("account_settings")) loadAccount();
        if (page == PAGE_PREFERENCES) loadPreferences();
        if (page == position("active_sessions")) loadSessions();
    }

    private void loadSessions() {
        TextView status = findViewById(R.id.pfSessionStatus);
        TextView currentStatus = findViewById(R.id.pfCurrentSessionStatus);
        LinearLayout list = findViewById(R.id.pfSessionsList);
        findViewById(R.id.pfCurrentSessionCard).setVisibility(View.GONE);
        findViewById(R.id.pfOtherSessionsCard).setVisibility(View.GONE);
        status.setText("Đang tải phiên đăng nhập…");
        AuthRepository.get(this).currentSession(new AuthRepository.Result<AuthApi.SessionResult>() {
            @Override public void success(AuthApi.SessionResult response) {
                if (isFinishing() || page != position("active_sessions")) return;
                UserApi.Session session = response == null ? null : response.session;
                currentStatus.setText(session == null ? "Không có dữ liệu phiên hiện tại."
                    : "Phiên hiện tại: " + session.device_label + " · " + session.platform
                        + "\nĐăng nhập: " + session.created_at
                        + "\nHoạt động gần nhất: " + session.last_activity_at);
            }
            @Override public void failure(String code, String message) {
                if (!isFinishing() && page == position("active_sessions")) currentStatus.setText(message);
            }
        });
        AuthRepository.get(this).sessions(new AuthRepository.Result<UserApi.SessionsResult>() {
            @Override public void success(UserApi.SessionsResult response) {
                if (isFinishing() || page != position("active_sessions")) return;
                list.removeAllViews();
                List<UserApi.Session> sessions = response == null ? null : response.sessions;
                if (sessions == null || sessions.isEmpty()) {
                    status.setText("Không có phiên đăng nhập đang hoạt động.");
                    return;
                }
                status.setText("Có " + sessions.size() + " phiên đăng nhập.");
                for (UserApi.Session session : sessions) addSessionRow(list, session);
            }
            @Override public void failure(String code, String message) {
                if (!isFinishing() && page == position("active_sessions")) status.setText(message);
            }
        });
    }

    private void addSessionRow(LinearLayout container, UserApi.Session session) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 14, 16, 14);
        card.setBackgroundResource(R.drawable.pf_card_bg);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = 12;
        container.addView(card, cardParams);
        TextView title = new TextView(this);
        title.setText((session.is_current ? "Phiên hiện tại · " : "")
            + safe(session.device_label, "Thiết bị") + " · " + safe(session.platform, "platform"));
        title.setTextColor(getColor(R.color.pf_text)); title.setTextSize(15); title.setTypeface(null, 1);
        card.addView(title);
        TextView details = new TextView(this);
        details.setText("Đăng nhập: " + safe(session.created_at, "—")
            + "\nHoạt động gần nhất: " + safe(session.last_activity_at, "—")
            + "\nHết hạn: " + safe(session.idle_expires_at, "—"));
        details.setTextColor(getColor(R.color.pf_muted)); details.setTextSize(13);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = 6;
        card.addView(details, detailParams);
        Button revoke = new Button(this);
        revoke.setText(session.is_current ? "Đăng xuất thiết bị này" : "Thu hồi phiên");
        LinearLayout.LayoutParams revokeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        revokeParams.topMargin = 8;
        card.addView(revoke, revokeParams);
        revoke.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle(session.is_current ? "Đăng xuất phiên hiện tại?" : "Thu hồi phiên này?")
            .setMessage("Thiết bị này sẽ cần đăng nhập lại.")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xác nhận", (dialog, which) -> revokeSession(session, revoke))
            .show());
    }

    private void revokeSession(UserApi.Session session, View button) {
        button.setEnabled(false);
        AuthRepository.get(this).revokeSession(session.session_id, new AuthRepository.Result<Void>() {
            @Override public void success(Void ignored) {
                if (isFinishing()) return;
                if (session.is_current) {
                    AuthRepository.get(ProfileUiActivity.this).clearSession();
                    goLogin();
                } else loadSessions();
            }
            @Override public void failure(String code, String message) {
                if (!isFinishing()) { button.setEnabled(true); info(message); }
            }
        });
    }

    private void revokeAllSessions() {
        View button = findViewById(R.id.pfConfirmRevoke);
        button.setEnabled(false);
        AuthRepository.get(this).revokeAllSessions(new AuthRepository.Result<Void>() {
            @Override public void success(Void ignored) {
                if (!isFinishing()) goLogin();
            }
            @Override public void failure(String code, String message) {
                if (!isFinishing()) { button.setEnabled(true); info(message); }
            }
        });
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void loadPreferences() {
        AuthRepository.get(this).bands(new AuthRepository.Result<HomeApi.BandsResult>() {
            @Override public void success(HomeApi.BandsResult response) {
                if (isFinishing() || page != PAGE_PREFERENCES) return;
                preferenceBands.clear();
                if (response != null && response.bands != null) {
                    for (HomeApi.Band band : response.bands) {
                        if (band != null && band.id != null && band.label != null) {
                            preferenceBands.add(band);
                        }
                    }
                }
                preferenceBands.sort((a, b) -> Integer.compare(a.sort_order, b.sort_order));
                renderPreferenceBands();
                loadPreferenceTopics();
            }
            @Override public void failure(String code, String message) {
                if (!isFinishing() && page == PAGE_PREFERENCES) info(message);
            }
        });
    }

    private void renderPreferenceBands() {
        LinearLayout container = findViewById(R.id.pfBandsContainer);
        if (container == null) return;
        container.removeAllViews();
        preferenceBandChecks.clear();
        for (int i = 0; i < preferenceBands.size(); i++) {
            HomeApi.Band band = preferenceBands.get(i);
            CheckBox checkBox = new CheckBox(this);
            checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            checkBox.setMinHeight(dp(48));
            checkBox.setText(band.label);
            checkBox.setTextColor(getColor(R.color.pf_text));
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            checkBox.setButtonTintList(getColorStateList(R.color.pf_mint));
            checkBox.setPadding(dp(4), dp(4), dp(4), dp(4));
            preferenceBandChecks.add(checkBox);
            container.addView(checkBox);
            final int selected = i;
            checkBox.setOnClickListener(v -> {
                for (int j = 0; j < preferenceBandChecks.size(); j++) {
                    preferenceBandChecks.get(j).setChecked(j == selected);
                }
            });
        }
    }

    private void loadPreferenceTopics() {
        AuthRepository.get(this).topics(new AuthRepository.Result<HomeApi.TopicsResult>() {
            @Override public void success(HomeApi.TopicsResult response) {
                if (isFinishing() || page != PAGE_PREFERENCES) return;
                preferenceTopics.clear();
                if (response != null && response.topics != null) {
                    for (HomeApi.Topic topic : response.topics) {
                        if (topic != null && topic.id != null && topic.label != null) {
                            preferenceTopics.add(topic);
                        }
                    }
                }
                preferenceTopics.sort((a, b) -> Integer.compare(a.sort_order, b.sort_order));
                renderPreferenceTopics();
                loadSavedPreferences();
            }
            @Override public void failure(String code, String message) {
                if (!isFinishing() && page == PAGE_PREFERENCES) info(message);
            }
        });
    }

    private void renderPreferenceTopics() {
        LinearLayout container = findViewById(R.id.pfTopicsContainer);
        if (container == null) return;
        container.removeAllViews();
        preferenceTopicChecks.clear();
        for (HomeApi.Topic topic : preferenceTopics) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            checkBox.setMinHeight(dp(48));
            checkBox.setText(topic.label);
            checkBox.setTextColor(getColor(R.color.pf_text));
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            checkBox.setButtonTintList(getColorStateList(R.color.pf_mint));
            checkBox.setPadding(dp(4), dp(4), dp(4), dp(4));
            container.addView(checkBox);
            preferenceTopicChecks.add(checkBox);
        }
    }

    private void loadSavedPreferences() {
        AuthRepository.get(this).preferences(
            new AuthRepository.Result<UserApi.LearnerPreferences>() {
                @Override public void success(UserApi.LearnerPreferences preferences) {
                    if (isFinishing() || page != PAGE_PREFERENCES || preferences == null) return;
                    for (int i = 0; i < preferenceBands.size(); i++) {
                        HomeApi.Band band = preferenceBands.get(i);
                        preferenceBandChecks.get(i).setChecked(
                            band.id.equals(preferences.band) || band.label.equals(preferences.band));
                    }
                    for (int i = 0; i < preferenceTopics.size(); i++) {
                        boolean selected = preferences.topic_ids != null
                            && preferences.topic_ids.contains(preferenceTopics.get(i).id);
                        preferenceTopicChecks.get(i).setChecked(selected);
                    }
                }
                @Override public void failure(String code, String message) {
                    if (!isFinishing() && page == PAGE_PREFERENCES) info(message);
                }
            });
    }

    private void savePreferences() {
        String band = null;
        for (int i = 0; i < preferenceBands.size(); i++) {
            if (preferenceBandChecks.get(i).isChecked()) {
                band = preferenceBands.get(i).id;
                break;
            }
        }
        ArrayList<String> topics = new ArrayList<>();
        for (int i = 0; i < preferenceTopics.size(); i++) {
            if (preferenceTopicChecks.get(i).isChecked()) {
                topics.add(preferenceTopics.get(i).id);
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
                    if (currentName != null) currentName.setText("Tên hiển thị: "
                        + displayName(account));
                } else if (page == PAGE_PROFILE_EDIT) {
                    EditText name = findViewById(R.id.pfDisplayName);
                    if (name != null) name.setText(displayName(account));
                } else if (page == position("account_settings")) {
                    bindEmailVerification(account.contacts == null ? null : account.contacts.email);
                }
            }
            @Override public void failure(String code, String message) {
                if (page != PAGE_PROFILE && page != PAGE_PROFILE_EDIT
                    && page != position("account_settings")) return;
                if (code.startsWith("SESSION_") || "REFRESH_REUSE_DETECTED".equals(code)) {
                    StateScreensActivity.openAsRoot(ProfileUiActivity.this, "session_expired");
                }
                else if (accountLabel != null) accountLabel.setText(message);
                else info(message);
            }
        });
    }

    private void bindEmailVerification(UserApi.Contact contact) {
        TextView status = findViewById(R.id.pfEmailVerificationStatus);
        Button verify = findViewById(R.id.pfVerifyEmail);
        if (status == null || verify == null) return;
        if (contact == null || contact.masked_value == null) {
            status.setText("Chưa có email trong tài khoản.");
            verify.setVisibility(View.GONE);
            return;
        }
        boolean verified = contact.verified_at != null;
        status.setText(contact.masked_value + (verified ? " · Đã xác minh" : " · Chưa xác minh"));
        verify.setVisibility(verified ? View.GONE : View.VISIBLE);
    }

    private void startEmailVerification() {
        Button button = findViewById(R.id.pfVerifyEmail);
        if (button != null) {
            button.setEnabled(false);
            button.setText("Đang gửi mã…");
        }
        AuthRepository.Result<UserApi.EmailOtpChallenge> result = new AuthRepository.Result<UserApi.EmailOtpChallenge>() {
            @Override public void success(UserApi.EmailOtpChallenge challenge) {
                if (isFinishing() || page != position("account_settings")) return;
                resetVerifyButton();
                if (challenge == null || challenge.challenge_id == null) {
                    info("Máy chủ trả về mã xác minh không hợp lệ.");
                    return;
                }
                showVerificationDialog(challenge);
            }
            @Override public void failure(String code, String message) {
                if (isFinishing()) return;
                resetVerifyButton();
                info(message);
            }
        };
        AuthRepository.get(this).startEmailVerification(result);
    }

    private void resetVerifyButton() {
        Button button = findViewById(R.id.pfVerifyEmail);
        if (button == null) return;
        button.setEnabled(true);
        button.setText("Xác minh email");
    }

    private void showVerificationDialog(UserApi.EmailOtpChallenge challenge) {
        EditText codeField = new EditText(this);
        codeField.setHint("Nhập mã 6 chữ số");
        codeField.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        codeField.setFilters(new InputFilter[] { new InputFilter.LengthFilter(6) });
        codeField.setSingleLine(true);

        TextView resendStatus = new TextView(this);
        resendStatus.setTextColor(getColor(R.color.pf_muted));
        resendStatus.setTextSize(13);
        resendStatus.setPadding(0, dp(12), 0, dp(4));
        Button resendButton = new Button(this);
        resendButton.setText("Gửi lại mã (60 giây)");
        resendButton.setAllCaps(false);
        resendButton.setEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(8), dp(24), 0);
        content.addView(codeField, new LinearLayout.LayoutParams(-1, -2));
        content.addView(resendStatus, new LinearLayout.LayoutParams(-1, -2));
        content.addView(resendButton, new LinearLayout.LayoutParams(-1, dp(48)));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Xác minh email")
            .setMessage("Đã gửi mã đến " + safe(challenge.masked_email, "email của bạn"))
            .setView(content)
            .setNegativeButton("Để sau", null)
            .setPositiveButton("Xác minh", null)
            .create();
        final CountDownTimer[] resendTimer = new CountDownTimer[1];
        Runnable startResendTimer = () -> {
            resendButton.setEnabled(false);
            if (resendTimer[0] != null) resendTimer[0].cancel();
            resendTimer[0] = new CountDownTimer(60_000L, 1_000L) {
                @Override public void onTick(long millisUntilFinished) {
                    long seconds = (millisUntilFinished + 999L) / 1_000L;
                    resendButton.setText("Gửi lại mã (" + seconds + " giây)");
                    resendStatus.setText("Bạn có thể yêu cầu mã mới sau " + seconds + " giây.");
                }
                @Override public void onFinish() {
                    resendButton.setEnabled(true);
                    resendButton.setText("Gửi lại mã");
                    resendStatus.setText("Nếu chưa nhận được mã, bạn có thể gửi lại.");
                }
            }.start();
        };
        dialog.setOnDismissListener(ignored -> {
            if (resendTimer[0] != null) resendTimer[0].cancel();
        });
        resendButton.setOnClickListener(view -> {
            resendButton.setEnabled(false);
            resendButton.setText("Đang gửi lại mã…");
            resendStatus.setText("");
            AuthRepository.Result<UserApi.EmailOtpChallenge> result = new AuthRepository.Result<UserApi.EmailOtpChallenge>() {
                @Override public void success(UserApi.EmailOtpChallenge updated) {
                    if (isFinishing() || !dialog.isShowing()) return;
                    if (updated != null && updated.challenge_id != null) {
                        challenge.challenge_id = updated.challenge_id;
                    }
                    codeField.setText("");
                    resendStatus.setText("Đã gửi lại mã đến "
                        + safe(updated == null ? null : updated.masked_email, "email của bạn"));
                    startResendTimer.run();
                }
                @Override public void failure(String code, String message) {
                    if (!dialog.isShowing()) return;
                    resendStatus.setText(message);
                    resendButton.setEnabled(true);
                    resendButton.setText("Gửi lại mã");
                }
            };
            AuthRepository.get(this).resendEmailVerification(challenge.challenge_id, result);
        });
        dialog.setOnShowListener(ignored -> {
            startResendTimer.run();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String code = codeField.getText().toString().trim();
                if (!code.matches("[0-9]{6}")) {
                    codeField.setError("Nhập đủ 6 chữ số");
                    return;
                }
                Button verifyButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                verifyButton.setEnabled(false);
                AuthRepository.Result<UserApi.EmailVerificationResult> result = new AuthRepository.Result<UserApi.EmailVerificationResult>() {
                    @Override public void success(UserApi.EmailVerificationResult response) {
                        if (isFinishing() || !dialog.isShowing()) return;
                        verifyButton.setEnabled(true);
                        if (response == null || !response.verified) {
                            codeField.setError("Mã xác minh không hợp lệ.");
                            return;
                        }
                        dialog.dismiss();
                        loadAccount();
                        info("Email đã được xác minh.");
                    }
                    @Override public void failure(String code, String message) {
                        if (!dialog.isShowing()) return;
                        verifyButton.setEnabled(true);
                        codeField.setError(message);
                    }
                };
                AuthRepository.get(this).verifyEmail(challenge.challenge_id, code, result);
            });
        });
        dialog.show();
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
            field.setError("Tên hiển thị cần từ 2 đến 30 ký tự");
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
                    info("Tên hiển thị đã được lưu.");
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
        login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
        if (page == position("sessions_revoke_confirm")) go("active_sessions");
        else if (page == position("change_password") || page == position("active_sessions")) {
            go("account_settings");
        }
        else if (page == position("password_changed")) goLogin();
        else if (page != PAGE_PROFILE) go("profile");
        else super.onBackPressed();
    }
}
