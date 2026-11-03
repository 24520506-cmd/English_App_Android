package com.example.english_app;

import android.content.Context;
import android.content.SharedPreferences;

/** Tracks whether a newly registered account has finished the local onboarding flow. */
final class OnboardingProgress {
    static final String EXTRA_USER_ID = "onboarding_user_id";
    private static final String PREFS = "onboarding_progress";

    private OnboardingProgress() { }

    static void markPending(Context context, String userId) {
        if (userId == null || userId.isEmpty()) return;
        preferences(context).edit().putBoolean(key(userId), true).apply();
    }

    static boolean isPending(Context context, String userId) {
        return userId != null && !userId.isEmpty()
            && preferences(context).getBoolean(key(userId), false);
    }

    static void markComplete(Context context, String userId) {
        if (userId == null || userId.isEmpty()) return;
        preferences(context).edit().remove(key(userId)).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(String userId) {
        return "pending_" + userId;
    }
}
