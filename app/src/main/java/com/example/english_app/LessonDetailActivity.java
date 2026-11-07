package com.example.english_app;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.example.english_app.api.AuthRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;

public class LessonDetailActivity extends UserSessionActivity {
    public static final String EXTRA_CONTENT_ID = "lesson_content_id";
    public static final String EXTRA_HAS_AUDIO = "lesson_has_audio";
    public static final String EXTRA_LESSON_JSON = "lesson_json";
    private String contentId;
    private String topicId;
    private MediaPlayer player;
    private TextView audioStatus;
    private JsonObject lessonPayload;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_lesson_detail);
        contentId = getIntent().getStringExtra(EXTRA_CONTENT_ID);
        topicId = getIntent().getStringExtra(LearningProgress.EXTRA_TOPIC_ID);
        audioStatus = findViewById(R.id.lessonAudioStatus);
        View play = findViewById(R.id.btnPlayAudio);
        play.setVisibility(getIntent().getBooleanExtra(EXTRA_HAS_AUDIO, true) ? View.VISIBLE : View.GONE);
        findViewById(R.id.lessonBack).setOnClickListener(v -> finish());
        View start = findViewById(R.id.startLesson);
        start.setEnabled(false);
        start.setOnClickListener(v -> openVocabulary());
        play.setOnClickListener(v -> loadAndPlayAudio());
        if (contentId == null || contentId.trim().isEmpty()) {
            ((TextView) findViewById(R.id.lessonContent)).setText("Không có mã bài học.");
            play.setVisibility(View.GONE);
            return;
        }
        loadLesson();
    }

    private void loadLesson() {
        AuthRepository.get(this).lesson(contentId,
            new AuthRepository.Result<JsonObject>() {
                @Override public void success(JsonObject lesson) {
                    if (isFinishing()) return;
                    lessonPayload = lesson;
                    String title = text(lesson, "title", "name");
                    String band = text(lesson, "band");
                    String topic = text(lesson, "topic");
                    ((TextView) findViewById(R.id.lessonTitle)).setText(
                        title == null ? "Bài học" : title);
                    ((TextView) findViewById(R.id.lessonBadge)).setText(
                        (band == null ? "Bài học" : "Band " + band)
                            + (topic == null ? "" : " · " + topic));
                    String objective = text(lesson, "objective");
                    String cefr = text(lesson, "cefr_reference");
                    StringBuilder metadata = new StringBuilder();
                    if (objective != null) metadata.append(objective);
                    if (cefr != null) {
                        if (metadata.length() > 0) metadata.append("\n");
                        metadata.append("Tham chiếu CEFR: ").append(cefr);
                    }
                    ((TextView) findViewById(R.id.lessonMeta)).setText(metadata);
                    ((TextView) findViewById(R.id.lessonContent)).setText(formatLesson(lesson));
                    findViewById(R.id.startLesson).setEnabled(true);
                    if (lesson.has("has_audio") && !lesson.get("has_audio").getAsBoolean()) {
                        findViewById(R.id.btnPlayAudio).setVisibility(View.GONE);
                    }
                }
                @Override public void failure(String code, String message) {
                    if (!isFinishing()) ((TextView) findViewById(R.id.lessonContent)).setText(message);
                }
            });
    }

    private String text(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement value = object.get(key);
            if (value != null && !value.isJsonNull() && value.isJsonPrimitive()) {
                String result = value.getAsString();
                if (!result.trim().isEmpty()) return result;
            }
        }
        return null;
    }

    private void loadAndPlayAudio() {
        findViewById(R.id.btnPlayAudio).setEnabled(false);
        audioStatus.setText("Đang tải audio WAV…");
        AuthRepository.get(this).lessonAudio(contentId, new AuthRepository.Result<File>() {
            @Override public void success(File file) {
                if (isFinishing()) return;
                try {
                    releasePlayer();
                    player = new MediaPlayer();
                    player.setDataSource(file.getAbsolutePath());
                    player.setOnPreparedListener(MediaPlayer::start);
                    player.setOnCompletionListener(mp -> {
                        audioStatus.setText("Đã phát xong audio.");
                        releasePlayer();
                    });
                    player.setOnErrorListener((mp, what, extra) -> {
                        audioStatus.setText("Không phát được file audio.");
                        releasePlayer();
                        return true;
                    });
                    player.prepareAsync();
                    audioStatus.setText("Đang chuẩn bị phát…");
                } catch (Exception error) {
                    audioStatus.setText("Không phát được file audio.");
                    releasePlayer();
                } finally { findViewById(R.id.btnPlayAudio).setEnabled(true); }
            }
            @Override public void failure(String code, String message) {
                if (isFinishing()) return;
                audioStatus.setText(message);
                findViewById(R.id.btnPlayAudio).setEnabled(true);
            }
        });
    }

    private void openVocabulary() {
        if (lessonPayload == null) return;
        android.content.Intent intent = new android.content.Intent(this, VocabularyActivity.class);
        intent.putExtra(EXTRA_LESSON_JSON, lessonPayload.toString());
        if (topicId != null) intent.putExtra(LearningProgress.EXTRA_TOPIC_ID, topicId);
        startActivity(intent);
    }

    private String formatLesson(JsonObject lesson) {
        StringBuilder content = new StringBuilder();
        JsonElement vocabularyValue = lesson.get("vocabulary");
        if (vocabularyValue != null && vocabularyValue.isJsonArray()) {
            JsonArray vocabulary = vocabularyValue.getAsJsonArray();
            appendSection(content, "TỪ VỰNG · " + vocabulary.size() + " TỪ");
            for (int i = 0; i < vocabulary.size(); i++) {
                JsonElement entry = vocabulary.get(i);
                if (!entry.isJsonObject()) continue;
                JsonObject word = entry.getAsJsonObject();
                appendLine(content, (i + 1) + ". " + fallback(text(word, "headword"), "Từ vựng"));
                appendField(content, "Phiên âm", text(word, "ipa"));
                appendField(content, "Nghĩa", text(word, "description_vi"));
                appendField(content, "Giải thích", text(word, "explanation_en"));
                appendField(content, "Ví dụ", text(word, "example"));
                appendField(content, "Ngữ cảnh", text(word, "usage_situation"));
                appendDialogue(content, word.get("dialogue"));
                content.append('\n');
            }
        }

        JsonElement listeningValue = lesson.get("listening");
        if (listeningValue != null && listeningValue.isJsonObject()) {
            JsonObject listening = listeningValue.getAsJsonObject();
            appendSection(content, "LUYỆN NGHE");
            appendField(content, "Bài nghe", text(listening, "transcript"));
            JsonElement questionsValue = listening.get("questions");
            if (questionsValue != null && questionsValue.isJsonArray()) {
                JsonArray questions = questionsValue.getAsJsonArray();
                if (!questions.isEmpty()) appendLine(content, "Câu hỏi luyện nghe:");
                for (int i = 0; i < questions.size(); i++) {
                    JsonElement item = questions.get(i);
                    if (!item.isJsonObject()) continue;
                    JsonObject question = item.getAsJsonObject();
                    appendLine(content, (i + 1) + ". " + fallback(text(question, "question", "sentence"), "Câu hỏi"));
                    appendOptions(content, question.get("options"));
                }
            }
        }

        JsonElement writingValue = lesson.get("writing");
        if (writingValue != null && writingValue.isJsonArray()) {
            JsonArray writing = writingValue.getAsJsonArray();
            appendSection(content, "LUYỆN VIẾT · " + writing.size() + " GỢI Ý");
            for (int i = 0; i < writing.size(); i++) {
                JsonElement item = writing.get(i);
                if (!item.isJsonObject()) continue;
                JsonObject prompt = item.getAsJsonObject();
                appendLine(content, (i + 1) + ". " + fallback(text(prompt, "instruction"), "Đề bài"));
                appendField(content, "Tình huống", text(prompt, "context"));
                appendField(content, "Vai trò", text(prompt, "role"));
                appendField(content, "Mục tiêu", text(prompt, "communicative_goal"));
                content.append('\n');
            }
        }

        return content.length() == 0 ? "Bài học chưa có nội dung hiển thị." : content.toString().trim();
    }

    private void appendSection(StringBuilder out, String title) {
        if (out.length() > 0) out.append("\n\n");
        out.append(title).append('\n');
    }

    private void appendLine(StringBuilder out, String value) {
        if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') out.append('\n');
        out.append(value).append('\n');
    }

    private void appendField(StringBuilder out, String label, String value) {
        if (value != null) appendLine(out, label + ": " + value);
    }

    private void appendDialogue(StringBuilder out, JsonElement value) {
        if (value == null || !value.isJsonArray()) return;
        for (JsonElement line : value.getAsJsonArray()) {
            if (!line.isJsonObject()) continue;
            JsonObject turn = line.getAsJsonObject();
            String speaker = text(turn, "speaker");
            String words = text(turn, "text");
            if (words != null) appendLine(out, (speaker == null ? "" : speaker + ": ") + words);
        }
    }

    private void appendOptions(StringBuilder out, JsonElement value) {
        if (value == null || !value.isJsonArray()) return;
        JsonArray options = value.getAsJsonArray();
        for (int i = 0; i < options.size(); i++) {
            JsonElement option = options.get(i);
            if (option.isJsonPrimitive()) appendLine(out, "   " + (char) ('A' + i) + ". " + option.getAsString());
        }
    }

    private String fallback(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    private void releasePlayer() {
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); } catch (IllegalStateException ignored) { }
            player.release();
            player = null;
        }
    }

    @Override protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}
