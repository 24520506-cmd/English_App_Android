package com.example.english_app;

import android.content.Context;

/** Local UI progress until lesson completion is connected to saved account data. */
final class LearningProgress {
    private static final String PREFS = "learning_progress";
    private static final String FINANCE_VOCAB_DONE = "finance_vocab_done";
    private static final String FINANCE_LISTENING_WRITING_DONE = "finance_listening_writing_done";
    static final String EXTRA_TOPIC_ID = "learning_topic_id";

    private LearningProgress() {}

    static boolean isFinanceVocabularyDone(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(FINANCE_VOCAB_DONE, false)
            || isVocabularyDone(context, "personal_finance");
    }

    static void markFinanceVocabularyDone(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(FINANCE_VOCAB_DONE, true).apply();
        markVocabularyDone(context, "personal_finance");
    }

    static boolean isFinanceListeningWritingDone(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(FINANCE_LISTENING_WRITING_DONE, false)
            || isListeningWritingDone(context, "personal_finance");
    }

    static void markFinanceListeningWritingDone(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(FINANCE_LISTENING_WRITING_DONE, true).apply();
        markListeningWritingDone(context, "personal_finance");
    }

    static boolean isVocabularyDone(Context context, String topicId) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(key("vocabulary", topicId), false);
    }

    static void markVocabularyDone(Context context, String topicId) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(key("vocabulary", topicId), true).apply();
    }

    static boolean isListeningWritingDone(Context context, String topicId) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(key("listening_writing", topicId), false);
    }

    static void markListeningWritingDone(Context context, String topicId) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(key("listening_writing", topicId), true).apply();
    }

    private static String key(String stage, String topicId) {
        String safeTopic = topicId == null ? "default"
            : topicId.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]+", "_");
        return stage + "_done_" + safeTopic;
    }
}
