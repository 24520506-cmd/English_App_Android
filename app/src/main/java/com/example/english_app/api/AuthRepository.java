package com.example.english_app.api;

import android.content.Context;
import com.example.english_app.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final UserApi api;
    private final SecureTokenStore store;
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private String accessToken;
    private boolean refreshRunning;
    private final List<Result<UserApi.Tokens>> refreshWaiters = new ArrayList<>();

    private AuthRepository(Context context) {
        store = new SecureTokenStore(context);
        api = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build().create(UserApi.class);
    }

    public static synchronized AuthRepository get(Context context) {
        if (instance == null) instance = new AuthRepository(context.getApplicationContext());
        return instance;
    }

    public void register(String username, String password, Result<UserApi.Account> result) {
        api.register(new UserApi.RegisterRequest(username, password, null, null)).enqueue(callback(result));
    }

    public void login(String username, String password, String deviceLabel, Result<UserApi.Tokens> result) {
        api.login(new UserApi.LoginRequest(username, password, deviceLabel)).enqueue(new Callback<UserApi.Tokens>() {
            @Override public void onResponse(Call<UserApi.Tokens> call, Response<UserApi.Tokens> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    fail(response, result); return;
                }
                acceptTokens(response.body(), result);
            }
            @Override public void onFailure(Call<UserApi.Tokens> call, Throwable error) {
                result.failure("NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    public void restore(Result<UserApi.Tokens> result) {
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
        api.refresh(Collections.singletonMap("refresh_token", refresh)).enqueue(new Callback<UserApi.Tokens>() {
            @Override public void onResponse(Call<UserApi.Tokens> call, Response<UserApi.Tokens> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    UserApi.ApiError error = parseError(response);
                    if (response.code() == 401) { store.clear(); accessToken = null; }
                    completeRestore(null, error.code, error.message);
                    return;
                }
                acceptTokens(response.body(), new Result<UserApi.Tokens>() {
                    @Override public void success(UserApi.Tokens value) { completeRestore(value, null, null); }
                    @Override public void failure(String code, String message) { completeRestore(null, code, message); }
                });
            }
            @Override public void onFailure(Call<UserApi.Tokens> call, Throwable error) {
                completeRestore(null, "NETWORK_ERROR", "Không kết nối được máy chủ. Vui lòng thử lại.");
            }
        });
    }

    private void completeRestore(UserApi.Tokens tokens, String code, String message) {
        List<Result<UserApi.Tokens>> waiters;
        synchronized (this) {
            waiters = new ArrayList<>(refreshWaiters);
            refreshWaiters.clear();
            refreshRunning = false;
        }
        for (Result<UserApi.Tokens> waiter : waiters) {
            if (tokens != null) waiter.success(tokens);
            else waiter.failure(code, message);
        }
    }

    private void acceptTokens(UserApi.Tokens tokens, Result<UserApi.Tokens> result) {
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
        api.profile("Bearer " + accessToken).enqueue(new Callback<UserApi.Account>() {
            @Override public void onResponse(Call<UserApi.Account> call, Response<UserApi.Account> response) {
                if (response.isSuccessful() && response.body() != null) { result.success(response.body()); return; }
                UserApi.ApiError error = parseError(response);
                if (!retried && response.code() == 401 && "SESSION_INVALID".equals(error.code)) {
                    restore(new Result<UserApi.Tokens>() {
                        @Override public void success(UserApi.Tokens value) { profile(result, true); }
                        @Override public void failure(String code, String message) { result.failure(code, message); }
                    });
                } else result.failure(error.code, error.message);
            }
            @Override public void onFailure(Call<UserApi.Account> call, Throwable error) {
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
        api.updateProfile("Bearer " + accessToken,
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
                        restore(new Result<UserApi.Tokens>() {
                            @Override public void success(UserApi.Tokens value) {
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

    private void preferences(Result<UserApi.LearnerPreferences> result, boolean retried) {
        if (accessToken == null) {
            result.failure("NO_SESSION", "Chưa có phiên đăng nhập.");
            return;
        }
        api.preferences("Bearer " + accessToken).enqueue(new Callback<UserApi.LearnerPreferences>() {
            @Override public void onResponse(Call<UserApi.LearnerPreferences> call,
                                            Response<UserApi.LearnerPreferences> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.success(response.body());
                    return;
                }
                UserApi.ApiError error = parseError(response);
                if (!retried && response.code() == 401 && "SESSION_INVALID".equals(error.code)) {
                    restore(new Result<UserApi.Tokens>() {
                        @Override public void success(UserApi.Tokens value) {
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

    private void updatePreferences(String band, List<String> topicIds,
                                   Result<Void> result, boolean retried) {
        if (accessToken == null) {
            result.failure("NO_SESSION", "Chưa có phiên đăng nhập.");
            return;
        }
        api.updatePreferences("Bearer " + accessToken,
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
                        restore(new Result<UserApi.Tokens>() {
                            @Override public void success(UserApi.Tokens value) {
                                updatePreferences(band, topicIds, result, true);
                            }
                            @Override public void failure(String code, String message) {
                                result.failure(code, message);
                            }
                        });
                    } else result.failure(error.code, error.message);
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
        api.logout("Bearer " + accessToken).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    UserApi.ApiError error = parseError(response);
                    if (!retried && response.code() == 401 && "SESSION_INVALID".equals(error.code)) {
                        restore(new Result<UserApi.Tokens>() {
                            @Override public void success(UserApi.Tokens value) { logout(result, true); }
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
}
