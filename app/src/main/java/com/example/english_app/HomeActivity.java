package com.example.english_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import com.example.english_app.api.AuthRepository;
import com.example.english_app.api.UserApi;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity {
    private final long delay = 130L, duration = 350L;
    private static final String[] BAND_IDS = {
        "BELOW_4_0", "4_0_5_0", "5_5_6_5", "7_0_PLUS"
    };
    private static final String[] BAND_LABELS = {"Dưới 4.0", "4.0–5.0", "5.5–6.5", "7.0+"};
    private static final String[] TOPIC_IDS = {
        "daily_life", "education", "technology", "environment",
        "finance", "travel", "health", "work"
    };
    private static final String[] TOPIC_LABELS = {
        "Daily Life", "Education", "Technology", "Environment",
        "Finance", "Travel", "Health", "Work"
    };
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_home);
        bindPath(R.id.learningPath,"Personal Finance & Budgeting",true);
        bindPath(R.id.learningPathFeeling1,"Feeling 1",false);
        bindPath(R.id.learningPathFeeling2,"Feeling 2",false);
        findViewById(R.id.cardReview).setOnClickListener(v -> openReview());
        findViewById(R.id.btnReview).setOnClickListener(v -> openReview());
        findViewById(R.id.tabReview).setOnClickListener(v -> openReview());
        findViewById(R.id.tabProgress).setOnClickListener(v -> openProgressScreen(ProgressRankingActivity.SCREEN_PROGRESS));
        findViewById(R.id.tabRanking).setOnClickListener(v -> openProgressScreen(ProgressRankingActivity.SCREEN_RANKING));
        findViewById(R.id.tabProfile).setOnClickListener(v ->
            startActivity(new Intent(this, ProfileUiActivity.class)));
        int[] blocks={R.id.tvHomeTitle,R.id.cardJourney,R.id.cardReview,R.id.pathContainer,R.id.feeling1Section,R.id.feeling2Section};
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
        LearningPathView financePath=findViewById(R.id.learningPath);
        boolean vocabularyDone=LearningProgress.isFinanceVocabularyDone(this);
        boolean listeningWritingDone=LearningProgress.isFinanceListeningWritingDone(this);
        financePath.setFirstLessonCompleted(vocabularyDone);
        financePath.setSecondLessonCompleted(listeningWritingDone);
        financePath.setContentDescription(listeningWritingDone
            ?"Hoạt động 1 và 2 đã hoàn tất"
            :vocabularyDone
            ?"Hoạt động 1 đã hoàn tất. Hoạt động 2: nghe và viết"
            :"Hoạt động 1: từ vựng. Hoạt động 2: nghe và viết");
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
        TextView summary = findViewById(R.id.tvHomePreferences);
        AuthRepository.get(this).preferences(
            new AuthRepository.Result<UserApi.LearnerPreferences>() {
                @Override public void success(UserApi.LearnerPreferences preferences) {
                    if (isFinishing() || preferences == null) return;
                    String band = "Chưa chọn";
                    for (int i = 0; i < BAND_IDS.length; i++) {
                        if (BAND_IDS[i].equals(preferences.band)) {
                            band = BAND_LABELS[i];
                            break;
                        }
                    }
                    List<String> topics = new ArrayList<>();
                    if (preferences.topic_ids != null) {
                        for (int i = 0; i < TOPIC_IDS.length; i++) {
                            if (preferences.topic_ids.contains(TOPIC_IDS[i])) {
                                topics.add(TOPIC_LABELS[i]);
                            }
                        }
                    }
                    String topicText = topics.isEmpty()
                        ? "Chưa chọn" : android.text.TextUtils.join(", ", topics);
                    summary.setText("Mức học: " + band + "\nChủ đề ưu tiên: " + topicText);
                }

                @Override public void failure(String code, String message) {
                    if (!isFinishing()) {
                        summary.setText("Mức học và chủ đề chưa tải được. Mở Hồ sơ để thử lại.");
                    }
                }
            });
    }
    private void bindPath(int id,String topic,boolean hasDetail){
        LearningPathView path=findViewById(id);
        path.setOnLessonClickListener(index -> {
            if(hasDetail){
                startActivity(new Intent(this,index==0?LessonDetailActivity.class:ListeningActivity.class));
            }else{
                Toast.makeText(this,topic+" · Bài học "+(index+1),Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void openReview(){startActivity(new Intent(this,ReviewFlowActivity.class));}
    private void openProgressScreen(int screen){Intent intent=new Intent(this,ProgressRankingActivity.class);intent.putExtra(ProgressRankingActivity.EXTRA_SCREEN,screen);startActivity(intent);}
}
