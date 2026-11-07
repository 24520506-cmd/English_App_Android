package com.example.english_app.api;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.POST;

/** Retrofit routes owned by the backend auth controller. */
public interface AuthApi {
    @POST("users") Call<UserApi.Account> register(@Body RegisterRequest request);
    @POST("auth/login") Call<Tokens> login(@Body LoginRequest request);
    @POST("auth/refresh") Call<Tokens> refresh(@Body Map<String, String> request);
    @GET("auth/session") Call<SessionResult> session(
            @Header("Authorization") String bearer);
    @POST("auth/logout") Call<Void> logout(@Header("Authorization") String bearer);
    @POST("auth/activity") Call<Void> activity(@Header("Authorization") String bearer);

    @POST("auth/password-reset/challenges") Call<PasswordResetChallenge> startPasswordReset(
            @Body RecoveryStartRequest request);
    @POST("auth/password-reset/challenges/{challengeId}/resend")
    Call<PasswordResetChallenge> resendPasswordReset(
            @Path("challengeId") String challengeId);
    @POST("auth/password-reset/challenges/{challengeId}/verify")
    Call<ResetGrant> verifyPasswordReset(
            @Path("challengeId") String challengeId,
            @Body RecoveryVerifyRequest request);
    @POST("auth/password-reset/complete") Call<Void> completePasswordReset(
            @Body RecoveryCompleteRequest request);

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

    final class Tokens {
        public String access_token, access_expires_at, refresh_token;
        public UserApi.Session session;
        public UserApi.Account user;
    }

    final class SessionResult {
        public UserApi.Session session;
        public UserApi.Account user;
    }

    final class RecoveryStartRequest {
        public final String channel, login_identifier, contact;
        public RecoveryStartRequest(String loginIdentifier, String email) {
            this.channel = "gmail";
            this.login_identifier = loginIdentifier;
            this.contact = email;
        }
    }

    final class PasswordResetChallenge {
        public String challenge_id, message;
    }

    final class RecoveryVerifyRequest {
        public final String otp;
        public RecoveryVerifyRequest(String otp) { this.otp = otp; }
    }

    final class ResetGrant {
        public String reset_grant, expires_at;
    }

    final class RecoveryCompleteRequest {
        public final String reset_grant, new_password;
        public RecoveryCompleteRequest(String resetGrant, String newPassword) {
            this.reset_grant = resetGrant; this.new_password = newPassword;
        }
    }
}
