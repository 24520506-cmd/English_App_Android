package com.example.english_app.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.example.english_app.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import okhttp3.ResponseBody;
import java.util.UUID;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Android-facing boundary for the first User API slice. */
public final class AuthRepository {
    public interface Result<T> {
        void success(T value);
        void failure(String code, String message);
    }

    private static AuthRepository instance;
    private final AuthApi authApi;
    private final UserApi userApi;
    private final HomeApi homeApi;
    private final SecureTokenStore store;
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService fileExecutor = Executors.newSingleThreadExecutor();
    private String accessToken;
    private boolean refreshRunning;
    private final List<Result<AuthApi.Tokens>> refreshWaiters = new ArrayList<>();

    private AuthRepository(Context context) {
        appContext = context.getApplicationContext();
        store = new SecureTokenStore(context);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        authApi = retrofit.create(AuthApi.class);
        userApi = retrofit.create(UserApi.class);
        homeApi = retrofit.create(HomeApi.class);
    }

    public static synchronized AuthRepository get(Context context) {
        if (instance == null) instance = new AuthRepository(context.getApplicationContext());
        return instance;
    }

    public void register(String username, String password, Result<UserApi.Account> result) {
        register(username, password, null, null, result);
    }

    public void register(String username, String password, String email, String phone,
                         Result<UserApi.Account> result) {
        authApi.register(new AuthApi.RegisterRequest(username, password, email, phone)).enqueue(callback(result));
    }

    public void login(String username, String password, String deviceLabel, Result<AuthApi.Tokens> result) {
        authApi.login(new AuthApi.LoginRequest(username, password, deviceLabel)).enqueue(new Callback<AuthApi.Tokens>() {
            @Override public void onResponse(Call<AuthApi.Tokens> call, Response<AuthApi.Tokens> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    fail(response, result); return;
                }
                acceptTokens(response.body(), result);
            }
            @Override public void onFailure(Call<AuthApi.Tokens> call, Throwable error) {
                result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    public void startPasswordReset(String username, String email,
                                   Result<AuthApi.PasswordResetChallenge> result) {
        authApi.startPasswordReset(new AuthApi.RecoveryStartRequest(username, email))
            .enqueue(callback(result));
    }

    public void resendPasswordReset(String challengeId,
                                    Result<AuthApi.PasswordResetChallenge> result) {
        authApi.resendPasswordReset(challengeId).enqueue(callback(result));
    }

    public void verifyPasswordReset(String challengeId, String otp,
                                    Result<AuthApi.ResetGrant> result) {
        authApi.verifyPasswordReset(challengeId, new AuthApi.RecoveryVerifyRequest(otp))
            .enqueue(callback(result));
    }

    public void completePasswordReset(String resetGrant, String newPassword,
                                      Result<Void> result) {
        authApi.completePasswordReset(new AuthApi.RecoveryCompleteRequest(resetGrant, newPassword))
            .enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) result.success(null);
                    else fail(response, result);
                }
                @Override public void onFailure(Call<Void> call, Throwable error) {
                    result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
                }
            });
    }

    public void restore(Result<AuthApi.Tokens> result) {
        synchronized (this) {
            refreshWaiters.add(result);
            if (refreshRunning) return;
            refreshRunning = true;
        }
        final String refresh;
        try { refresh = store.read(); }
        catch (Exception error) {
            store.clear();
            completeRestore(null, "TOKEN_STORAGE_ERROR", "Không thể khôi phục phiên. Vui lòng đăng nhập lại.");
            return;
        }
        if (refresh == null) {
            completeRestore(null, "NO_SESSION", "Chưa có phiên đăng nhập.");
            return;
        }
        authApi.refresh(Collections.singletonMap("refresh_token", refresh)).enqueue(new Callback<AuthApi.Tokens>() {
            @Override public void onResponse(Call<AuthApi.Tokens> call, Response<AuthApi.Tokens> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    UserApi.ApiError error = parseError(response);
                    if (response.code() == 401) { store.clear(); accessToken = null; }
                    completeRestore(null, error.code, error.message);
                    return;
                }
                acceptTokens(response.body(), new Result<AuthApi.Tokens>() {
                    @Override public void success(AuthApi.Tokens value) { completeRestore(value, null, null); }
                    @Override public void failure(String code, String message) { completeRestore(null, code, message); }
                });
            }
            @Override public void onFailure(Call<AuthApi.Tokens> call, Throwable error) {
                completeRestore(null, "NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    private void completeRestore(AuthApi.Tokens tokens, String code, String message) {
        List<Result<AuthApi.Tokens>> waiters;
        synchronized (this) {
            waiters = new ArrayList<>(refreshWaiters);
            refreshWaiters.clear();
            refreshRunning = false;
        }
        for (Result<AuthApi.Tokens> waiter : waiters) {
            if (tokens != null) waiter.success(tokens);
            else waiter.failure(code, message);
        }
    }

    private void acceptTokens(AuthApi.Tokens tokens, Result<AuthApi.Tokens> result) {
        if (tokens.refresh_token == null || tokens.access_token == null) {
            result.failure("INVALID_RESPONSE", "Phản hồi phiên không hợp lệ.");
            return;
        }
        try {
            store.save(tokens.refresh_token);
            accessToken = tokens.access_token;
            result.success(tokens);
        } catch (Exception error) {
            accessToken = null;
            store.clear();
            result.failure("TOKEN_STORAGE_ERROR", "Không thể lưu phiên. Vui lòng đăng nhập lại.");
        }
    }

    public void profile(Result<UserApi.Account> result) {
        profile(result, false);
    }

    private void profile(Result<UserApi.Account> result, boolean retried) {
        if (accessToken == null) { result.failure("NO_SESSION", "Chưa có phiên đăng nhập."); return; }
        userApi.profile("Bearer " + accessToken).enqueue(new Callback<UserApi.Account>() {
            @Override public void onResponse(Call<UserApi.Account> call, Response<UserApi.Account> response) {
                if (response.isSuccessful() && response.body() != null) { result.success(response.body()); return; }
                UserApi.ApiError error = parseError(response);
                if (!retried && response.code() == 401 && "SESSION_INVALID".equals(error.code)) {
                    restore(new Result<AuthApi.Tokens>() {
                        @Override public void success(AuthApi.Tokens value) { profile(result, true); }
                        @Override public void failure(String code, String message) { result.failure(code, message); }
                    });
                } else result.failure(error.code, error.message);
            }
            @Override public void onFailure(Call<UserApi.Account> call, Throwable error) {
                result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    public void startEmailVerification(Result<UserApi.EmailOtpChallenge> result) {
        authenticated(bearer -> userApi.startEmailVerification(bearer), result, false);
    }

    public void resendEmailVerification(String challengeId, Result<UserApi.EmailOtpChallenge> result) {
        authenticated(bearer -> userApi.resendEmailVerification(bearer, challengeId), result, false);
    }

    public void verifyEmail(String challengeId, String code,
                            Result<UserApi.EmailVerificationResult> result) {
        authenticated(bearer -> userApi.verifyEmail(bearer, challengeId,
            new UserApi.OtpCodeRequest(code)), result, false);
    }

    private interface AuthenticatedCall<T> {
        Call<T> create(String bearer);
    }

    private <T> void authenticated(AuthenticatedCall<T> request, Result<T> result,
                                   boolean retried) {
        if (accessToken == null) {
            result.failure("NO_SESSION", "Chưa có phiên đăng nhập.");
            return;
        }
        request.create("Bearer " + accessToken).enqueue(new Callback<T>() {
            @Override public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.success(response.body());
                    return;
                }
                UserApi.ApiError error = parseError(response);
                if (!retried && response.code() == 401
                    && "SESSION_INVALID".equals(error.code)) {
                    restore(new Result<AuthApi.Tokens>() {
                        @Override public void success(AuthApi.Tokens value) {
                            authenticated(request, result, true);
                        }
                        @Override public void failure(String code, String message) {
                            result.failure(code, message);
                        }
                    });
                } else result.failure(error.code, error.message);
            }

            @Override public void onFailure(Call<T> call, Throwable error) {
                result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    public void updateProfile(String displayName, Result<UserApi.Account> result) {
        updateProfile(displayName, result, false);
    }

    private void updateProfile(String displayName, Result<UserApi.Account> result, boolean retried) {
        if (accessToken == null) {
            result.failure("NO_SESSION", "Chưa có phiên đăng nhập.");
            return;
        }
        userApi.updateProfile("Bearer " + accessToken,
            new UserApi.UpdateProfileRequest(displayName)).enqueue(new Callback<UserApi.Account>() {
                @Override public void onResponse(Call<UserApi.Account> call,
                                                Response<UserApi.Account> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        result.success(response.body());
                        return;
                    }
                    UserApi.ApiError error = parseError(response);
                    if (!retried && response.code() == 401
                        && "SESSION_INVALID".equals(error.code)) {
                        restore(new Result<AuthApi.Tokens>() {
                            @Override public void success(AuthApi.Tokens value) {
                                updateProfile(displayName, result, true);
                            }
                            @Override public void failure(String code, String message) {
                                result.failure(code, message);
                            }
                        });
                    } else {
                        result.failure(error.code, error.message);
                    }
                }

                @Override public void onFailure(Call<UserApi.Account> call, Throwable error) {
                    result.failure("NETWORK_ERROR",
                        "Không kết nối được máy chủ. Vui lòng thử lại.");
                }
            });
    }

    public void preferences(Result<UserApi.LearnerPreferences> result) {
        preferences(result, false);
    }

    public void topics(Result<HomeApi.TopicsResult> result) {
        homeApi.topics().enqueue(callback(result));
    }

    public void bands(Result<HomeApi.BandsResult> result) {
        homeApi.bands().enqueue(callback(result));
    }

    private void preferences(Result<UserApi.LearnerPreferences> result, boolean retried) {
        if (accessToken == null) {
            result.failure("NO_SESSION", "Chưa có phiên đăng nhập.");
            return;
        }
        userApi.preferences("Bearer " + accessToken).enqueue(new Callback<UserApi.LearnerPreferences>() {
            @Override public void onResponse(Call<UserApi.LearnerPreferences> call,
                                            Response<UserApi.LearnerPreferences> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.success(response.body());
                    return;
                }
                UserApi.ApiError error = parseError(response);
                if (!retried && response.code() == 401 && "SESSION_INVALID".equals(error.code)) {
                    restore(new Result<AuthApi.Tokens>() {
                        @Override public void success(AuthApi.Tokens value) {
                            preferences(result, true);
                        }
                        @Override public void failure(String code, String message) {
                            result.failure(code, message);
                        }
                    });
                } else result.failure(error.code, error.message);
            }
            @Override public void onFailure(Call<UserApi.LearnerPreferences> call, Throwable error) {
                result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    public void updatePreferences(String band, List<String> topicIds,
                                  Result<Void> result) {
        updatePreferences(band, topicIds, result, false);
    }

    public synchronized String currentAccessToken() { return accessToken; }

    public synchronized void clearSession() {
        accessToken = null;
        store.clear();
    }

    public void currentSession(Result<AuthApi.SessionResult> result) {
        currentSession(result, false);
    }

    private void currentSession(Result<AuthApi.SessionResult> result, boolean retried) {
        if (accessToken == null) { result.failure("NO_SESSION", "Chưa có phiên đăng nhập."); return; }
        authApi.session("Bearer " + accessToken).enqueue(new Callback<AuthApi.SessionResult>() {
            @Override public void onResponse(Call<AuthApi.SessionResult> call, Response<AuthApi.SessionResult> response) {
                if (response.isSuccessful() && response.body() != null) { result.success(response.body()); return; }
                UserApi.ApiError error = parseError(response);
                if (!retried && response.code() == 401) {
                    restore(new Result<AuthApi.Tokens>() {
                        @Override public void success(AuthApi.Tokens value) { currentSession(result, true); }
                        @Override public void failure(String code, String message) { result.failure(code, message); }
                    });
                } else result.failure(error.code, error.message);
            }
            @Override public void onFailure(Call<AuthApi.SessionResult> call, Throwable error) {
                result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    public void sessions(Result<UserApi.SessionsResult> result) { sessions(result, false); }

    private void sessions(Result<UserApi.SessionsResult> result, boolean retried) {
        if (accessToken == null) { result.failure("NO_SESSION", "Chưa có phiên đăng nhập."); return; }
        userApi.sessions("Bearer " + accessToken).enqueue(new Callback<UserApi.SessionsResult>() {
            @Override public void onResponse(Call<UserApi.SessionsResult> call, Response<UserApi.SessionsResult> response) {
                if (response.isSuccessful() && response.body() != null) { result.success(response.body()); return; }
                UserApi.ApiError error = parseError(response);
                if (!retried && response.code() == 401) {
                    restore(new Result<AuthApi.Tokens>() {
                        @Override public void success(AuthApi.Tokens value) { sessions(result, true); }
                        @Override public void failure(String code, String message) { result.failure(code, message); }
                    });
                } else result.failure(error.code, error.message);
            }
            @Override public void onFailure(Call<UserApi.SessionsResult> call, Throwable error) {
                result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    public void revokeSession(String sessionId, Result<Void> result) { revokeSession(sessionId, result, false); }

    private void revokeSession(String sessionId, Result<Void> result, boolean retried) {
        if (accessToken == null) { result.failure("NO_SESSION", "Chưa có phiên đăng nhập."); return; }
        userApi.revokeSession("Bearer " + accessToken, sessionId).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) { result.success(null); return; }
                UserApi.ApiError error = parseError(response);
                if (!retried && response.code() == 401) {
                    restore(new Result<AuthApi.Tokens>() {
                        @Override public void success(AuthApi.Tokens value) { revokeSession(sessionId, result, true); }
                        @Override public void failure(String code, String message) { result.failure(code, message); }
                    });
                } else result.failure(error.code, error.message);
            }
            @Override public void onFailure(Call<Void> call, Throwable error) {
                result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    public void revokeAllSessions(Result<Void> result) { revokeAllSessions(result, false); }

    private void revokeAllSessions(Result<Void> result, boolean retried) {
        if (accessToken == null) { result.failure("NO_SESSION", "Chưa có phiên đăng nhập."); return; }
        userApi.revokeAllSessions("Bearer " + accessToken).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) { clearSession(); result.success(null); return; }
                UserApi.ApiError error = parseError(response);
                if (!retried && response.code() == 401) {
                    restore(new Result<AuthApi.Tokens>() {
                        @Override public void success(AuthApi.Tokens value) { revokeAllSessions(result, true); }
                        @Override public void failure(String code, String message) { result.failure(code, message); }
                    });
                } else result.failure(error.code, error.message);
            }
            @Override public void onFailure(Call<Void> call, Throwable error) {
                result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    public void changePassword(String currentPassword, String newPassword, Result<Void> result) {
        changePassword(currentPassword, newPassword, result, false);
    }

    private void changePassword(String currentPassword, String newPassword, Result<Void> result, boolean retried) {
        if (accessToken == null) { result.failure("NO_SESSION", "Chưa có phiên đăng nhập."); return; }
        userApi.changePassword("Bearer " + accessToken,
            new UserApi.PasswordChangeRequest(currentPassword, newPassword)).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) { clearSession(); result.success(null); return; }
                    UserApi.ApiError error = parseError(response);
                    if (!retried && response.code() == 401) {
                        restore(new Result<AuthApi.Tokens>() {
                            @Override public void success(AuthApi.Tokens value) {
                                changePassword(currentPassword, newPassword, result, true);
                            }
                            @Override public void failure(String code, String message) { result.failure(code, message); }
                        });
                    } else result.failure(error.code, error.message);
                }
                @Override public void onFailure(Call<Void> call, Throwable error) {
                    result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
                }
            });
    }

    public void lessons(String band, String topic, Result<List<HomeApi.LessonSummary>> result) {
        lessons(band, topic, result, false);
    }

    private void lessons(String band, String topic, Result<List<HomeApi.LessonSummary>> result, boolean retried) {
        if (accessToken == null) { result.failure("NO_SESSION", "Chưa có phiên đăng nhập."); return; }
        homeApi.lessons("Bearer " + accessToken, band, topic).enqueue(new Callback<com.google.gson.JsonElement>() {
            @Override public void onResponse(Call<com.google.gson.JsonElement> call, Response<com.google.gson.JsonElement> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try { result.success(parseLessons(response.body())); }
                    catch (Exception invalid) {
                        result.failure("INVALID_RESPONSE", "Dữ liệu danh sách bài học không đúng định dạng.");
                    }
                    return;
                }
                UserApi.ApiError error = parseError(response);
                if (!retried && response.code() == 401) {
                    restore(new Result<AuthApi.Tokens>() {
                        @Override public void success(AuthApi.Tokens value) { lessons(band, topic, result, true); }
                        @Override public void failure(String code, String message) { result.failure(code, message); }
                    });
                } else result.failure(error.code, error.message);
            }
            @Override public void onFailure(Call<com.google.gson.JsonElement> call, Throwable error) {
                result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    private List<HomeApi.LessonSummary> parseLessons(com.google.gson.JsonElement response) {
        com.google.gson.JsonArray array = null;
        if (response.isJsonArray()) array = response.getAsJsonArray();
        else if (response.isJsonObject()) {
            com.google.gson.JsonObject object = response.getAsJsonObject();
            String[] collectionKeys = {"lessons", "items", "content", "data", "results"};
            for (String key : collectionKeys) {
                com.google.gson.JsonElement value = object.get(key);
                if (value != null && value.isJsonArray()) { array = value.getAsJsonArray(); break; }
            }
            if (array == null && (object.has("content_id") || object.has("lesson_id"))) {
                array = new com.google.gson.JsonArray();
                array.add(object);
            }
        }
        if (array == null) throw new IllegalArgumentException("Expected a lesson list");
        List<HomeApi.LessonSummary> lessons = new ArrayList<>();
        for (com.google.gson.JsonElement item : array) {
            lessons.add(gson.fromJson(item, HomeApi.LessonSummary.class));
        }
        return lessons;
    }

    public void lesson(String contentId, Result<com.google.gson.JsonObject> result) {
        lesson(contentId, result, false);
    }

    private void lesson(String contentId, Result<com.google.gson.JsonObject> result, boolean retried) {
        if (accessToken == null) { result.failure("NO_SESSION", "Chưa có phiên đăng nhập."); return; }
        homeApi.lesson("Bearer " + accessToken, contentId).enqueue(new Callback<com.google.gson.JsonObject>() {
            @Override public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) { result.success(response.body()); return; }
                UserApi.ApiError error = parseError(response);
                if (!retried && response.code() == 401) {
                    restore(new Result<AuthApi.Tokens>() {
                        @Override public void success(AuthApi.Tokens value) { lesson(contentId, result, true); }
                        @Override public void failure(String code, String message) { result.failure(code, message); }
                    });
                } else result.failure(error.code, error.message);
            }
            @Override public void onFailure(Call<com.google.gson.JsonObject> call, Throwable error) {
                result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    public void lessonAudio(String contentId, Result<File> result) { lessonAudio(contentId, result, false); }

    private void lessonAudio(String contentId, Result<File> result, boolean retried) {
        if (accessToken == null) { result.failure("NO_SESSION", "Chưa có phiên đăng nhập."); return; }
        homeApi.lessonAudio("Bearer " + accessToken, contentId).enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    UserApi.ApiError error = parseError(response);
                    if (!retried && response.code() == 401) {
                        restore(new Result<AuthApi.Tokens>() {
                            @Override public void success(AuthApi.Tokens value) { lessonAudio(contentId, result, true); }
                            @Override public void failure(String code, String message) { result.failure(code, message); }
                        });
                    } else result.failure(error.code, error.message);
                    return;
                }
                ResponseBody body = response.body();
                fileExecutor.execute(() -> {
                    String cacheId = UUID.nameUUIDFromBytes(contentId.getBytes(StandardCharsets.UTF_8)).toString();
                    File output = new File(appContext.getCacheDir(), "lesson_audio_" + cacheId + ".wav");
                    try (InputStream input = body.byteStream(); FileOutputStream stream = new FileOutputStream(output)) {
                        byte[] buffer = new byte[8192]; int count;
                        while ((count = input.read(buffer)) != -1) stream.write(buffer, 0, count);
                        mainHandler.post(() -> result.success(output));
                    } catch (Exception error) {
                        mainHandler.post(() -> result.failure("AUDIO_DOWNLOAD_ERROR", "Không tải được audio bài học."));
                    } finally { body.close(); }
                });
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable error) {
                result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    public void reportActivity() {
        final long now = System.currentTimeMillis();
        synchronized (this) {
            if (accessToken == null || now - lastActivityRequestAt < 60_000L) return;
            lastActivityRequestAt = now;
        }
        authApi.activity("Bearer " + accessToken).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) { }
            @Override public void onFailure(Call<Void> call, Throwable error) { }
        });
    }

    private long lastActivityRequestAt;

    private void updatePreferences(String band, List<String> topicIds,
                                   Result<Void> result, boolean retried) {
        if (accessToken == null) {
            result.failure("NO_SESSION", "Chưa có phiên đăng nhập.");
            return;
        }
        userApi.updatePreferences("Bearer " + accessToken,
            new UserApi.LearnerPreferencesRequest(band, topicIds))
            .enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        result.success(null);
                        return;
                    }
                    UserApi.ApiError error = parseError(response);
                    if (!retried && response.code() == 401
                        && "SESSION_INVALID".equals(error.code)) {
                        restore(new Result<AuthApi.Tokens>() {
                            @Override public void success(AuthApi.Tokens value) {
                                updatePreferences(band, topicIds, result, true);
                            }
                            @Override public void failure(String code, String message) {
                                result.failure(code, message);
                            }
                        });
                    } else result.failure(error.code, errorMessageWithFields(error));
                }
                @Override public void onFailure(Call<Void> call, Throwable error) {
                    result.failure("NETWORK_ERROR",
                        "Không kết nối được máy chủ. Vui lòng thử lại.");
                }
            });
    }

    public void logout(Result<Void> result) {
        logout(result, false);
    }

    private void logout(Result<Void> result, boolean retried) {
        if (accessToken == null) { result.failure("NO_SESSION", "Chưa có phiên đăng nhập."); return; }
        authApi.logout("Bearer " + accessToken).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    UserApi.ApiError error = parseError(response);
                    if (!retried && response.code() == 401 && "SESSION_INVALID".equals(error.code)) {
                        restore(new Result<AuthApi.Tokens>() {
                            @Override public void success(AuthApi.Tokens value) { logout(result, true); }
                            @Override public void failure(String code, String message) { result.failure(code, message); }
                        });
                    } else result.failure(error.code, error.message);
                    return;
                }
                accessToken = null;
                store.clear();
                result.success(null);
            }
            @Override public void onFailure(Call<Void> call, Throwable error) {
                result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    private <T> Callback<T> callback(Result<T> result) {
        return new Callback<T>() {
            @Override public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) result.success(response.body());
                else fail(response, result);
            }
            @Override public void onFailure(Call<T> call, Throwable error) {
                result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        };
    }

    private <T> void fail(Response<?> response, Result<T> result) {
        UserApi.ApiError error = parseError(response);
        result.failure(error.code, error.message);
    }

    private UserApi.ApiError parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                UserApi.ErrorBody body = gson.fromJson(response.errorBody().string(), UserApi.ErrorBody.class);
                if (body != null && body.error != null && body.error.code != null) return body.error;
            }
        } catch (Exception ignored) { }
        UserApi.ApiError error = new UserApi.ApiError();
        error.code = "HTTP_" + response.code();
        error.message = "Yêu cầu không thành công. Vui lòng thử lại.";
        return error;
    }

    private String errorMessageWithFields(UserApi.ApiError error) {
        if (error.fields == null || error.fields.isEmpty()) return error.message;
        StringBuilder message = new StringBuilder(error.message == null ? "" : error.message);
        for (java.util.Map.Entry<String, String> field : error.fields.entrySet()) {
            if (message.length() > 0) message.append('\n');
            message.append(field.getKey()).append(": ").append(field.getValue());
        }
        return message.toString();
    }
}
