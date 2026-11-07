package com.example.english_app;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.LinearLayout;
import com.example.english_app.api.HomeApi;
import com.example.english_app.api.AuthRepository;
import com.example.english_app.api.UserApi;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends UserSessionActivity {
    private final long delay = 130L, duration = 350L;
    private final List<HomeApi.LessonSummary> homeLessons = new ArrayList<>();
    private final LinkedHashMap<String, List<HomeApi.LessonSummary>> homeLessonsByTopic = new LinkedHashMap<>();
    private Map<String, String> homeTopicLabels = new LinkedHashMap<>();
    private List<String> selectedTopicIds = new ArrayList<>();
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_home);
        findViewById(R.id.cardReview).setOnClickListener(v -> openReview());
        findViewById(R.id.btnReview).setOnClickListener(v -> openReview());
        findViewById(R.id.tabReview).setOnClickListener(v -> openReview());
        findViewById(R.id.tabProgress).setOnClickListener(v -> openProgressScreen(ProgressRankingActivity.SCREEN_PROGRESS));
        findViewById(R.id.tabRanking).setOnClickListener(v -> openProgressScreen(ProgressRankingActivity.SCREEN_RANKING));
        findViewById(R.id.tabProfile).setOnClickListener(v ->
            startActivity(new Intent(this, ProfileUiActivity.class)));
        int[] blocks={R.id.tvHomeTitle,R.id.cardJourney,R.id.cardReview,R.id.topicJourneysContainer};
        if(Settings.Global.getFloat(getContentResolver(),"animator_duration_scale",1f)>0f){
            for(int i=0;i<blocks.length;i++){
                View v=findViewById(blocks[i]);v.setAlpha(0);v.setTranslationY(22*getResources().getDisplayMetrics().density);
                v.animate().alpha(1).translationY(0).setDuration(duration).setStartDelay(delay*i)
                        .setInterpolator(new DecelerateInterpolator()).start();
            }
        }
    }
    @Override protected void onResume(){
        super.onResume();
        loadGreeting();
        loadHomePreferences();
    }
    private void loadGreeting() {
        TextView greeting = findViewById(R.id.tvHomeGreeting);
        AuthRepository.get(this).profile(new AuthRepository.Result<UserApi.Account>() {
            @Override public void success(UserApi.Account account) {
                if (isFinishing()) return;
                String name = account.profile == null ? null : account.profile.display_name;
                if (name == null || name.trim().isEmpty()) name = account.username;
                if (name == null || name.trim().isEmpty()) {
                    greeting.setText("Xin chào");
                } else {
                    greeting.setText("Xin chào, " + name.trim());
                }
            }

            @Override public void failure(String code, String message) {
                // Keep the neutral greeting if the profile is temporarily unavailable.
            }
        });
    }
    private void loadHomePreferences() {
        AuthRepository.get(this).bands(new AuthRepository.Result<HomeApi.BandsResult>() {
            @Override public void success(HomeApi.BandsResult response) {
                Map<String, String> labelsById = new LinkedHashMap<>();
                Map<String, String> idsByValue = new LinkedHashMap<>();
                if (response != null && response.bands != null) {
                    for (HomeApi.Band band : response.bands) {
                        if (band != null && band.id != null && band.label != null) {
                            labelsById.put(band.id, band.label);
                            labelsById.put(band.label, band.label);
                            idsByValue.put(band.id, band.id);
                            idsByValue.put(band.label, band.id);
                        }
                    }
                }
                loadHomeTopics(labelsById, idsByValue);
            }
            @Override public void failure(String code, String message) {
                loadHomeTopics(new LinkedHashMap<>(), new LinkedHashMap<>());
            }
        });
    }

    private void loadHomeTopics(Map<String, String> bandLabels, Map<String, String> bandIds) {
        AuthRepository.get(this).topics(new AuthRepository.Result<HomeApi.TopicsResult>() {
            @Override public void success(HomeApi.TopicsResult response) {
                Map<String, String> labelsById = new LinkedHashMap<>();
                if (response != null && response.topics != null) {
                    for (HomeApi.Topic topic : response.topics) {
                        if (topic != null && topic.id != null && topic.label != null) {
                            labelsById.put(topic.id, topic.label);
                        }
                    }
                }
                homeTopicLabels = labelsById;
                loadHomePreferences(bandLabels, bandIds, labelsById);
            }
            @Override public void failure(String code, String message) {
                loadHomePreferences(bandLabels, bandIds, new LinkedHashMap<>());
            }
        });
    }

    private void loadHomePreferences(Map<String, String> bandLabels,
                                     Map<String, String> bandIds,
                                     Map<String, String> topicLabels) {
        TextView summary = findViewById(R.id.tvHomePreferences);
        AuthRepository.get(this).preferences(
            new AuthRepository.Result<UserApi.LearnerPreferences>() {
                @Override public void success(UserApi.LearnerPreferences preferences) {
                    if (isFinishing() || preferences == null) return;
                    String band = preferences.band == null || preferences.band.trim().isEmpty()
                        ? "Chưa chọn" : bandLabels.containsKey(preferences.band)
                            ? bandLabels.get(preferences.band) : preferences.band;
                    List<String> topics = new ArrayList<>();
                    if (preferences.topic_ids != null) {
                        for (String topicId : preferences.topic_ids) {
                            topics.add(topicLabels.containsKey(topicId)
                                ? topicLabels.get(topicId) : topicId);
                        }
                    }
                    String topicText = topics.isEmpty()
                        ? "Chưa chọn" : android.text.TextUtils.join(", ", topics);
                    summary.setText("Mức học: " + band + "\nChủ đề ưu tiên: " + topicText);
                    selectedTopicIds = preferences.topic_ids == null
                        ? new ArrayList<>() : new ArrayList<>(preferences.topic_ids);
                    loadSuggestedLessons(preferences, bandIds);
                }

                @Override public void failure(String code, String message) {
                    if (!isFinishing()) {
                        summary.setText("Mức học và chủ đề chưa tải được. Mở Hồ sơ để thử lại.");
                        TextView status = findViewById(R.id.tvLessonsStatus);
                        if (status != null) status.setText("Chọn band và chủ đề trong Hồ sơ để tải bài học.");
                        selectedTopicIds.clear();
                        clearHomeLessons();
                    }
                }
            });
    }
    private void loadSuggestedLessons(UserApi.LearnerPreferences preferences,
                                      Map<String, String> bandIds) {
        TextView status = findViewById(R.id.tvLessonsStatus);
        LinearLayout list = findViewById(R.id.lessonListContainer);
        if (preferences.band == null || preferences.topic_ids == null || preferences.topic_ids.isEmpty()) {
            status.setText("Chọn band và ít nhất một chủ đề trong Hồ sơ để xem bài học.");
            clearHomeLessons();
            return;
        }
        list.removeAllViews();
        homeLessons.clear();
        homeLessonsByTopic.clear();
        ((LinearLayout) findViewById(R.id.topicJourneysContainer)).removeAllViews();
        LinkedHashMap<String, LinkedHashMap<String, HomeApi.LessonSummary>> collected = new LinkedHashMap<>();
        for (String topicId : preferences.topic_ids) collected.put(topicId, new LinkedHashMap<>());
        status.setText("Đang tải bài học…");
        String bandId = bandIds.containsKey(preferences.band)
            ? bandIds.get(preferences.band) : preferences.band;
        loadLessonsForTopics(bandId, preferences.topic_ids, 0,
            collected, null);
    }

    private void loadLessonsForTopics(String band, List<String> topicIds, int index,
                                      LinkedHashMap<String, LinkedHashMap<String, HomeApi.LessonSummary>> collected,
                                      String lastError) {
        if (isFinishing()) return;
        if (index >= topicIds.size()) {
            renderSuggestedLessons(collected, lastError);
            return;
        }
        String topicId = topicIds.get(index);
        LinkedHashMap<String, HomeApi.LessonSummary> topicLessons = collected.get(topicId);
        if (topicLessons == null) {
            topicLessons = new LinkedHashMap<>();
            collected.put(topicId, topicLessons);
        }
        LinkedHashMap<String, HomeApi.LessonSummary> currentTopicLessons = topicLessons;
        AuthRepository.get(this).lessons(band, topicId,
            new AuthRepository.Result<List<HomeApi.LessonSummary>>() {
                @Override public void success(List<HomeApi.LessonSummary> lessons) {
                    if (lessons != null) {
                        int position = 0;
                        for (HomeApi.LessonSummary lesson : lessons) {
                            if (lesson == null) continue;
                            if (lesson.topic_id == null) lesson.topic_id = topicId;
                            String key = lesson.content_id != null ? lesson.content_id
                                : lesson.lesson_id != null ? lesson.lesson_id
                                : topicId + "_" + position;
                            currentTopicLessons.put(key, lesson);
                            position++;
                        }
                    }
                    loadLessonsForTopics(band, topicIds, index + 1, collected, lastError);
                }
                @Override public void failure(String code, String message) {
                    loadLessonsForTopics(band, topicIds, index + 1, collected, message);
                }
            });
    }

    private void renderSuggestedLessons(
            LinkedHashMap<String, LinkedHashMap<String, HomeApi.LessonSummary>> collected,
            String lastError) {
        if (isFinishing()) return;
        TextView status = findViewById(R.id.tvLessonsStatus);
        LinearLayout list = findViewById(R.id.lessonListContainer);
        list.removeAllViews();
        homeLessons.clear();
        homeLessonsByTopic.clear();

        for (String topicId : selectedTopicIds) {
            LinkedHashMap<String, HomeApi.LessonSummary> topicResult = collected.get(topicId);
            List<HomeApi.LessonSummary> topicLessons = new ArrayList<>();
            if (topicResult != null) {
                for (HomeApi.LessonSummary lesson : topicResult.values()) {
                    if (lesson == null || lesson.content_id == null || lesson.content_id.trim().isEmpty()) continue;
                    topicLessons.add(lesson);
                    homeLessons.add(lesson);
                }
            }
            homeLessonsByTopic.put(topicId, topicLessons);
        }

        if (homeLessons.isEmpty()) {
            status.setText(lastError == null
                ? "Chưa có bài học cho band và chủ đề đã chọn." : lastError);
        } else {
            status.setText("Chạm vào bài học để xem nội dung và nghe audio.");
        }

        for (int lessonIndex = 0; lessonIndex < homeLessons.size(); lessonIndex++) {
            HomeApi.LessonSummary lesson = homeLessons.get(lessonIndex);
            TextView row = new TextView(HomeActivity.this);
            String title = lesson.title == null ? "Bài học" : lesson.title;
            row.setText(title + "\n" + lesson.band + " · " + lesson.topic
                + (lesson.has_audio ? " · Có audio" : ""));
            row.setTextColor(0xfff7f8ff);
            row.setTextSize(15);
            row.setPadding(dp(16), dp(14), dp(16), dp(14));
            row.setBackgroundResource(R.drawable.bg_home_card);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(10);
            list.addView(row, params);
            int index = lessonIndex;
            row.setOnClickListener(v -> openLessonAtIndex(index));
            animateEntry(row, lessonIndex);
        }
        renderTopicJourneys();
    }

    private void renderTopicJourneys() {
        LinearLayout container = findViewById(R.id.topicJourneysContainer);
        container.removeAllViews();
        for (String topicId : selectedTopicIds) {
            List<HomeApi.LessonSummary> lessons = homeLessonsByTopic.get(topicId);
            renderTopicJourney(container, topicId, lessons == null ? new ArrayList<>() : lessons);
        }
    }

    private void renderTopicJourney(LinearLayout container, String topicId,
                                    List<HomeApi.LessonSummary> lessons) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sectionParams.topMargin = dp(24);
        container.addView(section, sectionParams);

        TextView heading = new TextView(this);
        heading.setText("LỘ TRÌNH HỌC TẬP");
        heading.setTextColor(0xffc4cde8);
        heading.setTextSize(12);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        section.addView(heading);

        TextView title = new TextView(this);
        title.setText(homeTopicLabels.containsKey(topicId) ? homeTopicLabels.get(topicId) : topicId);
        title.setTextColor(0xfff9ca58);
        title.setTextSize(23);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(9);
        section.addView(title, titleParams);

        TextView description = new TextView(this);
        description.setText(lessons.isEmpty()
            ? "Chưa có bài học phù hợp cho chủ đề này."
            : "① Từ vựng  •  ② Nghe và viết");
        description.setTextColor(0xffb8c4e1);
        description.setTextSize(13);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        descriptionParams.topMargin = dp(5);
        section.addView(description, descriptionParams);

        LearningPathView path = new LearningPathView(this);
        path.setLessonCount(2);
        path.setLessonAvailable(!lessons.isEmpty());
        boolean vocabularyDone = LearningProgress.isVocabularyDone(this, topicId);
        boolean listeningDone = LearningProgress.isListeningWritingDone(this, topicId);
        path.setFirstLessonCompleted(vocabularyDone);
        path.setSecondLessonCompleted(listeningDone);
        path.setContentDescription("Lộ trình " + title.getText() + ". Chặng 1 từ vựng"
            + (vocabularyDone ? " đã hoàn thành" : " đang mở")
            + "; chặng 2 nghe và viết" + (vocabularyDone ? " đã mở" : " đang khóa"));
        path.setOnLessonClickListener(stage -> openPathStage(topicId, lessons, stage));
        LinearLayout.LayoutParams pathParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(320));
        pathParams.topMargin = dp(6);
        section.addView(path, pathParams);
        animateEntry(section, 0);
    }

    private void animateEntry(View view, int index) {
        if (Settings.Global.getFloat(getContentResolver(), "animator_duration_scale", 1f) <= 0f) return;
        view.setAlpha(0f);
        view.setTranslationY(dp(12));
        view.animate().alpha(1f).translationY(0f).setDuration(duration)
            .setStartDelay(delay * index).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void clearHomeLessons() {
        homeLessons.clear();
        homeLessonsByTopic.clear();
        ((LinearLayout) findViewById(R.id.lessonListContainer)).removeAllViews();
        renderTopicJourneys();
    }

    private void openLessonAtIndex(int index) {
        if (index < 0 || index >= homeLessons.size()) return;
        HomeApi.LessonSummary lesson = homeLessons.get(index);
        openLesson(lesson, lesson.topic_id);
    }

    private void openLesson(HomeApi.LessonSummary lesson, String topicId) {
        Intent intent = new Intent(this, LessonDetailActivity.class);
        intent.putExtra(LessonDetailActivity.EXTRA_CONTENT_ID, lesson.content_id);
        intent.putExtra(LessonDetailActivity.EXTRA_HAS_AUDIO, lesson.has_audio);
        if (topicId != null) intent.putExtra(LearningProgress.EXTRA_TOPIC_ID, topicId);
        startActivity(intent);
    }

    private void openPathStage(String topicId, List<HomeApi.LessonSummary> lessons, int stage) {
        if (lessons.isEmpty()) return;
        if (stage == 0) {
            openLesson(lessons.get(0), topicId);
        } else if (stage == 1 && LearningProgress.isVocabularyDone(this, topicId)) {
            Intent intent = new Intent(this, ListeningActivity.class);
            intent.putExtra(LearningProgress.EXTRA_TOPIC_ID, topicId);
            startActivity(intent);
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void openReview(){startActivity(new Intent(this,ReviewFlowActivity.class));}
    private void openProgressScreen(int screen){Intent intent=new Intent(this,ProgressRankingActivity.class);intent.putExtra(ProgressRankingActivity.EXTRA_SCREEN,screen);startActivity(intent);}
}
