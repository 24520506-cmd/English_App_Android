package com.example.english_app.api;

import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Retrofit routes used by Home and lesson content screens. */
public interface HomeApi {
    @GET("topics") Call<TopicsResult> topics();
    @GET("bands") Call<BandsResult> bands();

    @GET("lessons") Call<com.google.gson.JsonElement> lessons(
            @Header("Authorization") String bearer,
            @Query("band") String band,
            @Query("topic") String topic);
    @GET("lessons/{content_id}") Call<com.google.gson.JsonObject> lesson(
            @Header("Authorization") String bearer,
            @Path("content_id") String contentId);
    @GET("lessons/{content_id}/audio") Call<ResponseBody> lessonAudio(
            @Header("Authorization") String bearer,
            @Path("content_id") String contentId);

    final class LessonSummary {
        public String content_id, lesson_id, title, band, band_id, topic, topic_id, status;
        public boolean has_audio;
    }

    final class Topic {
        public String id, label;
        public int sort_order;
    }

    final class TopicsResult {
        public List<Topic> topics;
    }

    final class Band {
        public String id, label;
        public int sort_order;
    }

    final class BandsResult {
        public List<Band> bands;
    }
}
