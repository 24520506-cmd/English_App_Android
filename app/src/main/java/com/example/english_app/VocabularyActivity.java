package com.example.english_app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class VocabularyActivity extends Activity {
    private static final String[] FALLBACK_WORDS = {
        "Discretionary spending", "Household budget", "Financial literacy",
        "To tighten one's belt", "Emergency fund", "Expenditure",
        "Living paycheck to paycheck"
    };
    private static final String[] FALLBACK_MEANINGS = {
        "Chi tiêu tùy ý", "Ngân sách hộ gia đình", "Kiến thức tài chính",
        "Thắt chặt chi tiêu", "Quỹ khẩn cấp", "Khoản chi tiêu",
        "Sống phụ thuộc vào từng kỳ lương"
    };
    private static final String[] FALLBACK_IPAS = {
        "/dɪˈskreʃəneri ˈspendɪŋ/", "/ˈhaʊshoʊld ˈbʌdʒɪt/",
        "/faɪˈnænʃəl ˈlɪtərəsi/", "/tə ˈtaɪtən wʌnz belt/",
        "/ɪˈmɜːrdʒənsi fʌnd/", "/ɪkˈspendɪtʃər/",
        "/ˈlɪvɪŋ ˈpeɪtʃek tə ˈpeɪtʃek/"
    };
    private static final String[] FALLBACK_EXAMPLES = {
        "I want to cut my discretionary spending this month.",
        "We need to make a household budget.",
        "Financial literacy helps people manage money.",
        "We must tighten our belts this month.",
        "She has an emergency fund.",
        "They reduced their monthly expenditure.",
        "He is living paycheck to paycheck."
    };

    private static final class WordCard {
        String headword;
        String meaning;
        String ipa;
        String example;
        String dialogue;
        String usage;
    }

    private final List<WordCard> words = new ArrayList<>();
    private int index;
    private String topicId;
    private TextView counter, title, meaning, ipa, example, dialogue, usage, next;
    private LinearLayout segments;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_vocabulary);
        topicId = getIntent().getStringExtra(LearningProgress.EXTRA_TOPIC_ID);
        counter = findViewById(R.id.vocabCounter);
        title = findViewById(R.id.wordTitle);
        meaning = findViewById(R.id.wordMeaning);
        ipa = findViewById(R.id.wordIpa);
        example = findViewById(R.id.wordExample);
        dialogue = findViewById(R.id.wordDialogue);
        usage = findViewById(R.id.wordUsage);
        next = findViewById(R.id.vocabNext);
        segments = findViewById(R.id.progressSegments);

        String lessonJson = getIntent().getStringExtra(LessonDetailActivity.EXTRA_LESSON_JSON);
        if (lessonJson != null) {
            loadApiVocabulary(lessonJson);
            JsonObject lesson = parseLesson(lessonJson);
            String lessonTitle = lesson == null ? null : text(lesson, "title");
            if (lessonTitle != null) {
                ((TextView) findViewById(R.id.vocabHeading)).setText("Từ vựng · " + lessonTitle);
            }
            if (words.isEmpty()) {
                WordCard empty = new WordCard();
                empty.headword = "Bài học chưa có từ vựng";
                empty.meaning = "Dữ liệu từ vựng chưa được cung cấp cho bài này.";
                words.add(empty);
            }
        } else {
            loadFallbackVocabulary();
        }

        findViewById(R.id.vocabBack).setOnClickListener(v -> finish());
        next.setOnClickListener(v -> {
            if (index < words.size() - 1) {
                index++;
                render(true);
            } else {
                if (topicId == null) LearningProgress.markFinanceVocabularyDone(this);
                else LearningProgress.markVocabularyDone(this, topicId);
                Intent home = new Intent(this, HomeActivity.class);
                home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(home);
                finish();
            }
        });
        ipa.setOnClickListener(v -> Toast.makeText(this,
            "Chưa có audio phát âm cho từ này.", Toast.LENGTH_SHORT).show());
        render(false);
    }

    private void loadApiVocabulary(String lessonJson) {
        try {
            JsonObject lesson = parseLesson(lessonJson);
            if (lesson == null) return;
            JsonElement vocabulary = lesson.get("vocabulary");
            if (vocabulary == null || !vocabulary.isJsonArray()) return;
            JsonArray entries = vocabulary.getAsJsonArray();
            for (JsonElement entry : entries) {
                if (!entry.isJsonObject()) continue;
                JsonObject item = entry.getAsJsonObject();
                WordCard card = new WordCard();
                card.headword = text(item, "headword");
                if (card.headword == null) continue;
                card.meaning = text(item, "description_vi", "explanation_en");
                card.ipa = text(item, "ipa");
                card.example = text(item, "example");
                card.usage = text(item, "usage_situation");
                card.dialogue = dialogue(item.get("dialogue"));
                words.add(card);
            }
        } catch (Exception ignored) {
            words.clear();
        }
    }

    private JsonObject parseLesson(String lessonJson) {
        try {
            JsonElement parsed = JsonParser.parseString(lessonJson);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void loadFallbackVocabulary() {
        for (int i = 0; i < FALLBACK_WORDS.length; i++) {
            WordCard card = new WordCard();
            card.headword = FALLBACK_WORDS[i];
            card.meaning = FALLBACK_MEANINGS[i];
            card.ipa = FALLBACK_IPAS[i];
            card.example = FALLBACK_EXAMPLES[i];
            card.dialogue = "A: How can we save more?\nB: Let's review our monthly expenses.";
            card.usage = "Diễn đạt một kế hoạch chi tiêu thực tế trong cuộc sống.";
            words.add(card);
        }
    }

    private void render(boolean animate) {
        WordCard word = words.get(index);
        counter.setText("TỪ " + (index + 1) + " TRONG " + words.size());
        title.setText(word.headword);
        meaning.setText(word.meaning == null ? "" : word.meaning);
        ipa.setText((word.ipa == null ? "Chưa có phiên âm" : word.ipa) + "    ▶ Phát âm");
        example.setText(word.example == null ? "Chưa có câu ví dụ." : word.example);
        dialogue.setText(word.dialogue == null ? "Chưa có hội thoại." : word.dialogue);
        usage.setText(word.usage == null ? "" : word.usage);
        next.setText(index == words.size() - 1 ? "Hoàn tất" : "Tiếp tục");
        renderProgressSegments();
        if (animate && Settings.Global.getFloat(getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f) {
            int[] ids = {R.id.vocabCounter, R.id.progressSegments, R.id.wordTitle,
                R.id.wordMeaning, R.id.sectionPronunciation, R.id.sectionExample,
                R.id.sectionDialogue, R.id.sectionUsage, R.id.vocabNext};
            float density = getResources().getDisplayMetrics().density;
            for (int i = 0; i < ids.length; i++) {
                View view = findViewById(ids[i]);
                view.animate().cancel();
                view.setAlpha(0f);
                view.setTranslationY(16 * density);
                view.animate().alpha(1f).translationY(0f).setStartDelay(i * 90L)
                    .setDuration(300L).setInterpolator(new DecelerateInterpolator()).start();
            }
        }
    }

    private void renderProgressSegments() {
        segments.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < words.size(); i++) {
            View bar = new View(this);
            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(20 * density);
            background.setColor(Color.parseColor(i < index ? "#77E4C2"
                : i == index ? "#F8C957" : "#445074"));
            bar.setBackground(background);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, (int) (6 * density), 1);
            params.setMargins(0, 0, i == words.size() - 1 ? 0 : (int) (5 * density), 0);
            segments.addView(bar, params);
        }
    }

    private String dialogue(JsonElement value) {
        if (value == null || !value.isJsonArray()) return null;
        StringBuilder result = new StringBuilder();
        for (JsonElement line : value.getAsJsonArray()) {
            if (!line.isJsonObject()) continue;
            JsonObject turn = line.getAsJsonObject();
            String speaker = text(turn, "speaker");
            String words = text(turn, "text");
            if (words == null) continue;
            if (result.length() > 0) result.append('\n');
            if (speaker != null) result.append(speaker).append(": ");
            result.append(words);
        }
        return result.length() == 0 ? null : result.toString();
    }

    private String text(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement value = object.get(key);
            if (value != null && value.isJsonPrimitive()) {
                String string = value.getAsString().trim();
                if (!string.isEmpty()) return string;
            }
        }
        return null;
    }
}
