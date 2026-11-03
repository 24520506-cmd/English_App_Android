package com.example.english_app;

import android.content.Context;

/** Local UI progress until lesson completion is connected to saved account data. */
final class LearningProgress {
    private static final String PREFS = "learning_progress";
    private static final String FINANCE_VOCAB_DONE = "finance_vocab_done";
    private static final String FINANCE_LISTENING_WRITING_DONE = "finance_listening_writing_done";

    private LearningProgress() {}

    static boolean isFinanceVocabularyDone(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(FINANCE_VOCAB_DONE, false);
    }

    static void markFinanceVocabularyDone(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(FINANCE_VOCAB_DONE, true).apply();
    }

    static boolean isFinanceListeningWritingDone(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(FINANCE_LISTENING_WRITING_DONE, false);
    }

    static void markFinanceListeningWritingDone(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(FINANCE_LISTENING_WRITING_DONE, true).apply();
    }
}
