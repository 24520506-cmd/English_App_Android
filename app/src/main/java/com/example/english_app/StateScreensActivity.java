package com.example.english_app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.english_app.api.AuthApi;
import com.example.english_app.api.AuthRepository;
import com.example.english_app.api.UserApi;
import java.util.ArrayList;

/** The supplied UI states. Call open() only when the corresponding state actually occurs. */
public class StateScreensActivity extends Activity {
    public static final String EXTRA_SCREEN = "screen";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_USERNAME = "auth_username";
    private static final String EXTRA_ONBOARDING_USER_ID = "onboarding_user_id";
    private static final String CATALOG_EMPTY = "catalog_empty";
    private static final String CATALOG_SEARCH = "catalog_search";

    private String screen;
    private String stateMessage;
    private String authUsername;
    private String onboardingUserId;
    private final ArrayList<String> screenHistory = new ArrayList<>();

    public static void open(Activity caller, String screen) {
        open(caller, screen, null);
    }

    public static void open(Activity caller, String screen, String message) {
        open(caller, screen, message, null);
    }

    public static void open(Activity caller, String screen, String message, String username) {
        open(caller, screen, message, username, null);
    }

    public static void open(Activity caller, String screen, String message, String username,
                            String onboardingUserId) {
        Intent intent = new Intent(caller, StateScreensActivity.class);
        intent.putExtra(EXTRA_SCREEN, screen);
        if (message != null) intent.putExtra(EXTRA_MESSAGE, message);
        if (username != null) intent.putExtra(EXTRA_USERNAME, username);
        if (onboardingUserId != null) intent.putExtra(EXTRA_ONBOARDING_USER_ID, onboardingUserId);
        caller.startActivity(intent);
    }

    public static void openAsRoot(Activity caller, String screen) {
        Intent intent = new Intent(caller, StateScreensActivity.class);
        intent.putExtra(EXTRA_SCREEN, screen);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        caller.startActivity(intent);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.st_bg));
        getWindow().setNavigationBarColor(getColor(R.color.st_bg));
        screen = getIntent().getStringExtra(EXTRA_SCREEN);
        stateMessage = getIntent().getStringExtra(EXTRA_MESSAGE);
        authUsername = getIntent().getStringExtra(EXTRA_USERNAME);
        onboardingUserId = getIntent().getStringExtra(EXTRA_ONBOARDING_USER_ID);
        if (savedInstanceState != null) {
            screen = savedInstanceState.getString(EXTRA_SCREEN, screen);
            stateMessage = savedInstanceState.getString(EXTRA_MESSAGE, stateMessage);
            authUsername = savedInstanceState.getString(EXTRA_USERNAME, authUsername);
            onboardingUserId = savedInstanceState.getString(EXTRA_ONBOARDING_USER_ID,
                onboardingUserId);
            ArrayList<String> history = savedInstanceState.getStringArrayList("screenHistory");
            if (history != null) screenHistory.addAll(history);
        }
        showScreen(screen);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString(EXTRA_SCREEN, screen);
        state.putString(EXTRA_MESSAGE, stateMessage);
        state.putString(EXTRA_USERNAME, authUsername);
        state.putString(EXTRA_ONBOARDING_USER_ID, onboardingUserId);
        state.putStringArrayList("screenHistory", new ArrayList<>(screenHistory));
        super.onSaveInstanceState(state);
    }

    private void showScreen(String name) {
        int layout = layoutFor(name);
        if (layout == 0) {
            finish();
            return;
        }
        screen = name;
        setContentView(layout);
        if ("auth_invalid".equals(name) && authUsername != null) {
            EditText username = findViewById(R.id.field0);
            if (username != null) username.setText(authUsername);
        }
        if (stateMessage != null) {
            int messageViewId = "auth_invalid".equals(name) ? R.id.content2
                : "register_validation".equals(name) ? R.id.content1
                : "onboarding_name_error".equals(name) ? R.id.content0
                : R.id.screenDescription;
            TextView messageView = findViewById(messageViewId);
            if (messageView != null) messageView.setText(stateMessage);
        }
        findViewById(R.id.screenBack).setOnClickListener(v -> goBack());
        LinearLayout content = findViewById(R.id.stateContent);
        boolean animate = Settings.Global.getFloat(getContentResolver(),
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof String) {
                String action = (String) tag;
                child.setOnClickListener(v -> onAction(action));
            }
            if (animate && i > 0) {
                child.setAlpha(0f);
                child.setTranslationY(dp(14));
                child.animate().alpha(1f).translationY(0f).setDuration(330L)
                    .setStartDelay((i - 1) * 65L)
                    .setInterpolator(new DecelerateInterpolator()).start();
            }
        }
    }

    private void onAction(String action) {
        if (screen.startsWith("admin_")) {
            onAdminAction(action);
            return;
        }
        switch (action) {
            case "retry":
                if ("auth_invalid".equals(screen)) attemptLogin();
                else finish();
                break;
            case "edit_register":
            case "edit_name":
            case "choose_topics":
            case "retry_audio":
            case "continue_vocab":
            case "edit":
            case "submit":
                finish();
                break;
            case "forgot":
                openActivity(ForgotPasswordActivity.class);
                break;
            case "filters":
            case "review_outside":
                navigateTo("filters".equals(action) ? CATALOG_SEARCH : "review_outside");
                break;
            case "cancel":
                goBack();
                break;
            case "all_lessons":
            case "catalog":
                openActivity(HomeActivity.class);
                break;
            case "skip_name":
                OnboardingProgress.markComplete(this, onboardingUserId);
                openHomeClearingTask();
                break;
            case "continue_lesson":
                openActivity(VocabularyActivity.class);
                break;
            case "lesson":
                openActivity(LessonDetailActivity.class);
                break;
            case "retry_listening":
            case "resume_listening":
            case "practice":
                openActivity(ListeningActivity.class);
                break;
            case "listening":
                if ("listening_missing".equals(screen)) finish();
                else openActivity(ListeningActivity.class);
                break;
            case "progress":
                openProgress(ProgressRankingActivity.SCREEN_PROGRESS);
                break;
            case "result":
                openActivity(ListeningResultActivity.class);
                break;
            case "writing":
                openActivity(WritingActivity.class);
                break;
            case "review_result":
                openReview(5);
                break;
            case "review_start":
            case "review":
                openReview(1);
                break;
            case "review_home":
            case "review_schedule":
                openReview(0);
                break;
            case "revise":
                openActivity(WritingRevisionActivity.class);
                break;
            case "feedback_status":
            case "feedback_new":
                openActivity(WritingFeedbackActivity.class);
                break;
            case "feedback_history":
                openProgress(ProgressRankingActivity.SCREEN_HISTORY);
                break;
            case "retry_feedback":
                finish();
                break;
            case "profile":
                openActivity(ProfileUiActivity.class);
                break;
            case "start_lesson":
                openActivity(LessonDetailActivity.class);
                break;
            case "history":
                openProgress(ProgressRankingActivity.SCREEN_HISTORY);
                break;
            case "ranking":
            case "ranking_week":
                openProgress(ProgressRankingActivity.SCREEN_RANKING);
                break;
            case "permission":
                openNotificationSettings();
                break;
            case "logout":
                performLogout();
                break;
            case "login":
                openLoginClearingTask();
                break;
            case "verify_otp":
                validateOtp();
                break;
            case "reset_password":
                validatePassword();
                break;
            default:
                Toast.makeText(this, "Thao tác chưa được hỗ trợ", Toast.LENGTH_SHORT).show();
        }
    }

    private void onAdminAction(String action) {
        switch (action) {
            case "topics": navigateTo("admin_topic_edit"); break;
            case "lessons": navigateTo("admin_lesson_list"); break;
            case "moderation": navigateTo("admin_audit"); break;
            case "name_reports": navigateTo("admin_name_reports"); break;
            case "new_lesson":
            case "edit_lesson": navigateTo("admin_vocab_edit"); break;
            case "save_vocab":
                previewNext("admin_listening_edit"); break;
            case "save_listening":
                previewNext("admin_writing_edit"); break;
            case "save_writing":
                previewNext("admin_audio_tts"); break;
            case "approve_audio":
                previewNext("admin_preview"); break;
            case "check_publish": navigateTo("admin_publish_check"); break;
            case "hide_lesson": navigateTo("admin_hide_confirm"); break;
            case "revoke_version": navigateTo("admin_revoke"); break;
            case "cancel": goBack(); break;
            case "save_topic":
            case "resolve_report":
            case "publish":
            case "confirm_hide":
            case "confirm_revoke":
                Toast.makeText(this, "Bản mẫu: chưa kết nối API quản trị", Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(this, "Thao tác quản trị chưa được hỗ trợ", Toast.LENGTH_SHORT).show();
        }
    }

    private void previewNext(String next) {
        Toast.makeText(this, "Xem tiếp bản mẫu; dữ liệu chưa được lưu", Toast.LENGTH_LONG).show();
        navigateTo(next);
    }

    private void navigateTo(String next) {
        screenHistory.add(screen);
        showScreen(next);
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void validateOtp() {
        EditText code = findViewById(R.id.field1);
        if (!code.getText().toString().matches("[0-9]{6}")) {
            code.setError("Nhập đúng 6 chữ số");
            return;
        }
        Toast.makeText(this, "Chưa kết nối xác minh mã OTP", Toast.LENGTH_SHORT).show();
    }

    private void validatePassword() {
        EditText password = findViewById(R.id.field0);
        EditText confirmation = findViewById(R.id.field1);
        String value = password.getText().toString();
        if (value.isEmpty()) {
            password.setError("Nhập mật khẩu mới");
        } else if (!value.equals(confirmation.getText().toString())) {
            confirmation.setError("Mật khẩu xác nhận không khớp");
        } else {
            Toast.makeText(this, "Chưa kết nối đặt lại mật khẩu", Toast.LENGTH_SHORT).show();
        }
    }

    private void attemptLogin() {
        EditText username = findViewById(R.id.field0);
        EditText password = findViewById(R.id.field1);
        String user = username.getText().toString().trim();
        String secret = password.getText().toString();
        if (user.isEmpty()) {
            username.setError("Nhập tên đăng nhập");
            return;
        }
        if (secret.isEmpty()) {
            password.setError("Nhập mật khẩu");
            return;
        }
        View retry = findViewById(R.id.action0);
        if (retry != null) retry.setEnabled(false);
        AuthRepository.get(this).login(user, secret, Build.MANUFACTURER + " " + Build.MODEL,
            new AuthRepository.Result<AuthApi.Tokens>() {
                @Override public void success(AuthApi.Tokens tokens) {
                    if (isFinishing()) return;
                    openAfterAuthentication(tokens);
                }

                @Override public void failure(String code, String message) {
                    if (isFinishing()) return;
                    if (retry != null) retry.setEnabled(true);
                    TextView feedback = findViewById(R.id.content2);
                    if (feedback != null) feedback.setText(message);
                }
            });
    }

    private void performLogout() {
        View logout = findViewById(R.id.action1);
        if (logout != null) logout.setEnabled(false);
        AuthRepository.get(this).logout(new AuthRepository.Result<Void>() {
            @Override public void success(Void ignored) {
                if (!isFinishing()) openLoginClearingTask();
            }

            @Override public void failure(String code, String message) {
                if (isFinishing()) return;
                if (logout != null) logout.setEnabled(true);
                Toast.makeText(StateScreensActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openLoginClearingTask() {
        Intent login = new Intent(this, LoginActivity.class);
        login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(login);
    }

    private void openHomeClearingTask() {
        Intent home = new Intent(this, HomeActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(home);
    }

    private void openAfterAuthentication(AuthApi.Tokens tokens) {
        String userId = tokens.user == null ? null : tokens.user.user_id;
        if (!OnboardingProgress.isPending(this, userId)) {
            openHomeClearingTask();
            return;
        }
        Intent onboarding = new Intent(this, OnboardingBandActivity.class);
        onboarding.putExtra(OnboardingProgress.EXTRA_USER_ID, userId);
        onboarding.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(onboarding);
    }

    private void openActivity(Class<? extends Activity> target) {
        startActivity(new Intent(this, target));
        finish();
    }

    private void openProgress(int page) {
        Intent intent = new Intent(this, ProgressRankingActivity.class);
        intent.putExtra(ProgressRankingActivity.EXTRA_SCREEN, page);
        startActivity(intent);
        finish();
    }

    private void openReview(int page) {
        Intent intent = new Intent(this, ReviewFlowActivity.class);
        intent.putExtra(ReviewFlowActivity.EXTRA_PAGE, page);
        startActivity(intent);
        finish();
    }

    private void goBack() {
        if (!screenHistory.isEmpty()) {
            showScreen(screenHistory.remove(screenHistory.size() - 1));
        } else {
            finish();
        }
    }

    @Override public void onBackPressed() {
        goBack();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private int layoutFor(String name) {
        if (name == null) return 0;
        switch (name) {
            case "auth_invalid": return R.layout.state_auth_invalid;
            case CATALOG_EMPTY: return R.layout.state_catalog_empty;
            case CATALOG_SEARCH: return R.layout.state_catalog_search;
            case "lesson_continue": return R.layout.state_lesson_continue;
            case "lesson_locked": return R.layout.state_lesson_locked;
            case "listening_audio_error": return R.layout.state_listening_audio_error;
            case "listening_loading": return R.layout.state_listening_loading;
            case "listening_not_finished": return R.layout.state_listening_not_finished;
            case "onboarding_name_error": return R.layout.state_onboarding_name_error;
            case "onboarding_topics_error": return R.layout.state_onboarding_topics_error;
            case "register_validation": return R.layout.state_register_validation;
            case "reset_complete": return R.layout.state_reset_complete;
            case "reset_password": return R.layout.state_reset_password;
            case "reset_sent": return R.layout.state_reset_sent;
            case "session_expired": return R.layout.state_session_expired;
            case "vocab_audio_error": return R.layout.state_vocab_audio_error;
            case "vocab_situation": return R.layout.state_vocab_situation;
            case "lesson_points_capped": return R.layout.state_lesson_points_capped;
            case "listening_missing": return R.layout.state_listening_missing;
            case "listening_practice_again": return R.layout.state_listening_practice_again;
            case "listening_submitting": return R.layout.state_listening_submitting;
            case "listening_transcript": return R.layout.state_listening_transcript;
            case "review_answer_reveal": return R.layout.state_review_answer_reveal;
            case "review_expired": return R.layout.state_review_expired;
            case "review_outside": return R.layout.state_review_outside;
            case "review_revoked": return R.layout.state_review_revoked;
            case "review_today": return R.layout.state_review_today;
            case "review_upcoming": return R.layout.state_review_upcoming;
            case "writing_dictionary_warning": return R.layout.state_writing_dictionary_warning;
            case "writing_feedback_deferred": return R.layout.state_writing_feedback_deferred;
            case "writing_feedback_detail": return R.layout.state_writing_feedback_detail;
            case "writing_feedback_error": return R.layout.state_writing_feedback_error;
            case "writing_feedback_pending": return R.layout.state_writing_feedback_pending;
            case "writing_missing_target": return R.layout.state_writing_missing_target;
            case "writing_revision_blocked": return R.layout.state_writing_revision_blocked;
            case "writing_revision_success": return R.layout.state_writing_revision_success;
            case "writing_save_failed": return R.layout.state_writing_save_failed;
            case "writing_validation": return R.layout.state_writing_validation;
            case "admin_audio_tts": return R.layout.state_admin_audio_tts;
            case "admin_audit": return R.layout.state_admin_audit;
            case "admin_dashboard": return R.layout.state_admin_dashboard;
            case "admin_hide_confirm": return R.layout.state_admin_hide_confirm;
            case "admin_lesson_list": return R.layout.state_admin_lesson_list;
            case "admin_listening_edit": return R.layout.state_admin_listening_edit;
            case "admin_name_reports": return R.layout.state_admin_name_reports;
            case "admin_preview": return R.layout.state_admin_preview;
            case "admin_publish_check": return R.layout.state_admin_publish_check;
            case "admin_publish_error": return R.layout.state_admin_publish_error;
            case "admin_published": return R.layout.state_admin_published;
            case "admin_revoke": return R.layout.state_admin_revoke;
            case "admin_topic_edit": return R.layout.state_admin_topic_edit;
            case "admin_vocab_edit": return R.layout.state_admin_vocab_edit;
            case "admin_writing_edit": return R.layout.state_admin_writing_edit;
            case "notification_denied": return R.layout.state_notification_denied;
            case "preferences_saved": return R.layout.state_preferences_saved;
            case "profile_logout_confirm": return R.layout.state_profile_logout_confirm;
            case "profile_name_cooldown": return R.layout.state_profile_name_cooldown;
            case "progress_empty": return R.layout.state_progress_empty;
            case "progress_heatmap": return R.layout.state_progress_heatmap;
            case "progress_listening": return R.layout.state_progress_listening;
            case "progress_points": return R.layout.state_progress_points;
            case "progress_streak": return R.layout.state_progress_streak;
            case "ranking_alltime": return R.layout.state_ranking_alltime;
            case "ranking_month": return R.layout.state_ranking_month;
            case "ranking_no_listening": return R.layout.state_ranking_no_listening;
            case "ranking_report_success": return R.layout.state_ranking_report_success;
            case "review_notify_permission": return R.layout.state_review_notify_permission;
            default: return 0;
        }
    }
}
