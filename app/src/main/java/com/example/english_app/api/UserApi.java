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
    @GET("me") Call<Account> profile(@Header("Authorization") String bearer);

    // These protected verification and contact-management routes require the active bearer token.
    @POST("me/email-verifications") Call<EmailOtpChallenge> startEmailVerification(
            @Header("Authorization") String bearer);
    @POST("me/email-verifications/{challengeId}/resend") Call<EmailOtpChallenge> resendEmailVerification(
            @Header("Authorization") String bearer, @Path("challengeId") String challengeId);
    @POST("me/email-verifications/{challengeId}/verify") Call<EmailVerificationResult> verifyEmail(
            @Header("Authorization") String bearer, @Path("challengeId") String challengeId,
            @Body OtpCodeRequest request);
    @POST("me/contact-changes") Call<com.google.gson.JsonObject> startContactChange(
            @Header("Authorization") String bearer, @Body ContactChangeRequest request);
    @POST("me/contact-changes/{changeId}/verify") Call<Account> verifyContactChange(
            @Header("Authorization") String bearer, @Path("changeId") String changeId,
            @Body ContactChangeVerifyRequest request);
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

    final class EmailOtpChallenge {
        public String challenge_id, masked_email, resend_after_at;
    }

    final class EmailVerificationResult {
        public boolean verified;
        public String email;
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

    final class OtpCodeRequest {
        public final String code;
        public OtpCodeRequest(String code) { this.code = code; }
    }

    final class ContactChangeRequest {
        public final String kind, new_value, current_password;
        public ContactChangeRequest(String kind, String newValue, String currentPassword) {
            this.kind = kind;
            this.new_value = newValue;
            this.current_password = currentPassword;
        }
    }

    final class ContactChangeVerifyRequest {
        public final String otp;
        public ContactChangeVerifyRequest(String otp) { this.otp = otp; }
    }

    final class Session {
        public String session_id, device_label, platform, created_at, last_activity_at,
                idle_expires_at, absolute_expires_at;
        public boolean is_current;
    }

    final class SessionsResult {
        public List<Session> sessions;
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
