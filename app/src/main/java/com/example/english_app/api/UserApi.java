package com.example.english_app.api;

import java.util.Map;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface UserApi {
    @POST("users") Call<Account> register(@Body RegisterRequest request);
    @POST("auth/login") Call<Tokens> login(@Body LoginRequest request);
    @POST("auth/refresh") Call<Tokens> refresh(@Body Map<String, String> request);
    @GET("auth/session") Call<SessionResult> session(@Header("Authorization") String bearer);
    @GET("me") Call<Account> profile(@Header("Authorization") String bearer);
    @POST("auth/logout") Call<Void> logout(@Header("Authorization") String bearer);
    @POST("auth/activity") Call<Void> activity(@Header("Authorization") String bearer);
    @PATCH("me") Call<Account> updateProfile(@Header("Authorization") String bearer,
                                             @Body UpdateProfileRequest request);
    @GET("me/preferences") Call<LearnerPreferences> preferences(
            @Header("Authorization") String bearer);
    @PUT("me/preferences") Call<Void> updatePreferences(
            @Header("Authorization") String bearer, @Body LearnerPreferencesRequest request);
    @POST("me/password-change") Call<Void> changePassword(@Header("Authorization") String bearer,
                                                           @Body PasswordChangeRequest request);
    @GET("me/sessions") Call<SessionsResult> sessions(@Header("Authorization") String bearer);
    @DELETE("me/sessions/{session_id}") Call<Void> revokeSession(@Header("Authorization") String bearer,
                                                                  @Path("session_id") String sessionId);
    @POST("me/sessions/revoke-all") Call<Void> revokeAllSessions(@Header("Authorization") String bearer);

    final class RegisterRequest {
        public final String username, password, email, phone;
        public RegisterRequest(String username, String password, String email, String phone) {
            this.username = username; this.password = password; this.email = email; this.phone = phone;
        }
    }

    final class LoginRequest {
        public final String username, password, device_label, platform;
        public LoginRequest(String username, String password, String deviceLabel) {
            this.username = username; this.password = password;
            this.device_label = deviceLabel; this.platform = "android";
        }
    }

    final class Account {
        public String user_id, username, role, status;
        public Profile profile;
        public Contacts contacts;
    }

    final class Profile {
        public String display_name;
    }

    final class Contacts {
        public Contact email, phone;
        public Object pending_change;
    }

    final class Contact {
        public String masked_value, verified_at;
    }

    final class UpdateProfileRequest {
        public final String display_name;
        public UpdateProfileRequest(String displayName) { this.display_name = displayName; }
    }

    final class LearnerPreferencesRequest {
        public final String band;
        public final List<String> topic_ids;
        public LearnerPreferencesRequest(String band, List<String> topicIds) {
            this.band = band;
            this.topic_ids = topicIds;
        }
    }

    final class LearnerPreferences {
        public String band;
        public List<String> topic_ids;
    }

    final class PasswordChangeRequest {
        public final String current_password, new_password;
        public PasswordChangeRequest(String currentPassword, String newPassword) {
            this.current_password = currentPassword;
            this.new_password = newPassword;
        }
    }

    final class Session {
        public String session_id, device_label, platform, created_at, last_activity_at,
                idle_expires_at, absolute_expires_at;
        public boolean is_current;
    }

    final class SessionsResult {
        public List<Session> sessions;
    }

    final class Tokens {
        public String access_token, access_expires_at, refresh_token;
        public Session session;
        public Account user;
    }

    final class SessionResult {
        public Session session;
        public Account user;
    }

    final class ErrorBody {
        public ApiError error;
    }

    final class ApiError {
        public String code, message, request_id;
        public Map<String, String> fields;
        public String retry_after_at;
    }
}
