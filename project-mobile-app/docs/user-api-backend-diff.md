# Đối chiếu API User: Backend và Android

**Ngày rà soát:** 2026-11-07
**Nguồn đối chiếu:** OpenAPI đang chạy tại `https://doangay.duckdns.org/v3/api-docs` và thông tin base URL từ backend owner.
**Phạm vi:** Các endpoint User API trong contract và phần Android Retrofit đang khai báo/gọi.
**Giới hạn xác minh:** Workspace không có source backend. OpenAPI truy cập được qua HTTPS; trạng thái Gmail/eSMS/Google credentials trên VPS chưa được xác nhận. Chưa xác minh runtime trên Android.

## 1. Kết luận

Android đã gọi ở mã nguồn các luồng đăng ký/đăng nhập/refresh, hồ sơ, sở thích, session, đổi mật khẩu, logout, activity signal và lesson/audio. Onboarding, Preferences trong Hồ sơ và Home tải topics từ `GET /topics`; response live hiện có 3 ID `emotions`, `feelings_emotions`, `personal_finance`. Các màn chọn band và Home tải danh sách từ `GET /bands`; response live hiện có 6 band ID từ `5_5_6_0` đến `8_0_PLUS`. Home tải lessons cho từng topic learner đã chọn rồi gộp theo content ID. Base URL mặc định của cả debug và release hiện là `https://doangay.duckdns.org/api/v1/` (có thể override bằng Gradle property `apiBaseUrl`). Các routes password reset, xác minh email/điện thoại, đổi liên hệ và bắt đầu Google link hiện đã được khai báo trong Retrofit với OTP field theo OpenAPI; UI chưa gọi các routes này. Google callback là redirect backend đã đăng ký, không phải request app gửi trực tiếp.

Swagger `servers.url` hiện tự sinh `http://doangay.duckdns.org`; app dùng HTTPS theo base URL backend owner xác nhận. Cần cập nhật `servers.url` trên backend để nút “Try it out” cũng gửi qua HTTPS.

Các luồng OTP, xác minh/thay đổi liên hệ và Google chưa nối UI. Backend owner cho biết có Gmail, eSMS và Google OAuth, nhưng credentials trên VPS chưa được xác nhận. Màn quên mật khẩu hiện chưa gọi API.

## 2. Bảng đối chiếu endpoint

| ID | Endpoint | Backend theo tài liệu | Android khai báo | Android gọi từ luồng app | Khoảng trống / việc tiếp theo |
|---|---|---|---|---|---|
| API-U01 | `POST /users` | Đã triển khai | Có | Có — `RegisterActivity` tạo user | Đối chiếu payload, validation và response thực tế; đăng ký hiện gửi email/phone null. |
| API-U02 | `POST /auth/login` | Đã triển khai | Có | Có — `LoginActivity`, sau đăng ký và màn hình auth state | Kiểm tra response token, lỗi `INVALID_CREDENTIALS`, giới hạn đăng nhập và device label. |
| API-U03 | `POST /auth/refresh` | Đã triển khai | Có | Có — khôi phục session bằng refresh token | Kiểm tra token rotation, xử lý refresh không chắc kết quả và hành vi khi refresh/revoke đồng thời. |
| API-U04 | `GET /auth/session` | Đã triển khai | Có | Có — màn quản lý phiên | Hiện phiên hiện tại từ response. |
| API-U05 | `POST /auth/logout` | Đã triển khai | Có | Có — luồng logout trong `StateScreensActivity` | Kiểm tra revoke server thành công trước khi xóa token/cập nhật UI. |
| API-T01 | `GET /topics` | Public, trả danh sách theo `sort_order` | Có | Có — Onboarding, Preferences trong Hồ sơ và Home summary | Response live hiện có 3 topics; app hiển thị `label` và gửi `id` trong `topic_ids`. APK cũ có thể còn chứa danh sách trước đây. |
| API-T02 | `GET /bands` | Public, trả danh sách theo `sort_order` | Có | Có — Onboarding, Preferences trong Hồ sơ và Home summary | Response live hiện có 6 bands; app hiển thị `label` và gửi `id` trong `band`. |
| API-U06 | `GET /me` | Đã triển khai | Có | Có — Home và Profile | Home chỉ dùng tên hiển thị; khi lỗi API, giữ lời chào trung tính nên lỗi không hiện rõ. |
| API-U07 | `PATCH /me` | Đã triển khai | Có | Có — chỉnh tên hồ sơ trong `ProfileUiActivity` | Kiểm tra trường được phép sửa, null/empty và lỗi validation theo backend. |
| API-U08 | `POST /me/password-change` | Có theo checklist | Có | Có — Hồ sơ | Thành công xóa token cục bộ và đưa về đăng nhập. |
| API-U09 | `GET /me/sessions` | Có theo checklist | Có | Có — Hồ sơ → Quản lý phiên | Hiển thị session và thao tác thu hồi. |
| API-U10 | `DELETE /me/sessions/{session_id}` | Có theo checklist | Có | Có — từng dòng session | Thu hồi session hiện tại sẽ kết thúc task và về đăng nhập. |
| API-U11 | `POST /me/sessions/revoke-all` | Có theo checklist | Có | Có — xác nhận đăng xuất tất cả | Xóa token cục bộ sau khi BE xác nhận. |
| API-U12–U15 | Password reset | Có trong OpenAPI | Có trong Retrofit | Chưa gọi từ UI | Payload dùng `otp` ở verify; cần xác minh Gmail/eSMS credentials và response thật. |
| API-U16–U18 | Email/phone verification | Có trong OpenAPI | Có trong Retrofit | Chưa gọi từ UI | Verify dùng trường `code`; cần xác minh Gmail/eSMS credentials và response thật. |
| API-U19–U20 | Contact change | Có trong OpenAPI | Có trong Retrofit | Chưa gọi từ UI | Verify dùng trường `otp`; cần kiểm tra quy tắc gửi OTP của provider. |
| API-U21–U22 | Google link/callback | Có trong OpenAPI | Start có trong Retrofit; callback do backend nhận redirect | Chưa gọi từ UI | Xác nhận `GOOGLE_CALLBACK_URI`, Google registered URI và `GOOGLE_MOBILE_RETURN_URI` trên VPS. |
| API-U23 | `POST /auth/activity` | Có theo checklist | Có | Có — tương tác foreground trong các màn learner | Throttle tối đa một request mỗi phút. |

### API bài học

| Endpoint | Android | Trạng thái |
|---|---|---|
| `GET /lessons?band={band_id}&topic={topic_id}` | Có | Home gửi band ID cùng từng topic ID đã chọn; gộp kết quả và khử trùng theo content ID. |
| `GET /lessons/{content_id}` | Có | Mở từ danh sách lesson trên Home. |
| `GET /lessons/{content_id}/audio` | Có | Tải WAV có auth vào cache rồi phát bằng MediaPlayer. |

`GET /topics` và `GET /bands` đều public, trả ID, label và thứ tự sắp xếp. Onboarding, Preferences trong Hồ sơ và Home lấy cả danh sách topics lẫn bands từ backend; `PUT /me/preferences` gửi band ID và topic IDs. Live backend hiện trả 6 bands, gồm `5_5_6_0`, `6_0_6_5`, `6_5_7_0`, `7_0_7_5`, `7_5_8_0`, `8_0_PLUS`. Product spec vẫn mô tả thêm `<5.5`; cần xử lý như khác biệt nội dung giữa spec và danh sách đang được backend cung cấp.

## 3. Chênh lệch ưu tiên

### P0 — Cần để xác nhận luồng chạy thật

1. Xác nhận runtime trên Android với domain public HTTPS.
2. So sánh response thật của đăng ký, đăng nhập, refresh, `/me`, preferences và lessons với model Android; OpenAPI hiện mô tả phần lớn response là object tổng quát.
3. Chạy đăng ký → đăng nhập → refresh/khôi phục → Home tải `/me` trên thiết bị/emulator.

### P1 — Đối chiếu sau khi có URL

1. Xác nhận response list lesson là JSON array và chi tiết trả JSON trực tiếp như client đang khai báo.
2. Kiểm tra audio response là WAV body; xác nhận codec WAV mà thiết bị Genymotion hỗ trợ.
3. Android lấy 6 lựa chọn live từ `GET /bands` và gửi `id` trong `PUT /me/preferences.band` cùng query lessons. Product spec còn có thêm `<5.5`, hiện không được endpoint cung cấp.

### P2 — Provider-dependent flows

1. Xác minh Gmail/eSMS/OAuth credentials đã cấu hình trên VPS chưa.
2. Sau khi provider credentials và callback URI sẵn sàng, nối UI cho recovery, xác minh liên hệ và Google link.

## 4. Các điểm cần đối chiếu trực tiếp với backend

- Contract đề xuất `Idempotency-Key` cho một số mutation, nhưng OpenAPI hiện không đánh dấu header này là bắt buộc; chưa bật retry tự động cho các mutation.
- Contract đề xuất refresh token rotate mỗi lần; client đang lưu refresh token mới sau response thành công. Cần kiểm tra server có đúng response shape `access_token`, `refresh_token`, `session`, `user`.
- Contract yêu cầu lỗi theo envelope `{ "error": { "code", "message", ... } }`; client parse cấu trúc này nhưng fallback sang `HTTP_<status>` nếu response backend khác định dạng.
- OpenAPI có schema OTP khác nhau theo luồng: email/phone verification dùng `code`; password recovery dùng `otp`; contact-change verification dùng `otp`. Client hiện khai báo đúng các field này.
- Schema response của Swagger còn tổng quát, vì vậy cần response mẫu hoặc kiểm tra runtime trước khi kết luận toàn bộ model Android khớp.

## 5. Tiêu chí hoàn tất đối chiếu P0

- Backend owner xác nhận phiên bản endpoint/schema đang chạy khớp contract hoặc cung cấp OpenAPI/collection chính thức.
- Đăng ký tạo đúng learner; login trả session/token hợp lệ; refresh xoay token; logout revoke session.
- `GET /me` trả đúng account của token hiện tại; Home hiển thị `profile.display_name` hoặc fallback `username`.
- Session bị hết hạn/thu hồi không đưa user vào Home; lỗi mạng được phân biệt với lỗi xác thực.
- Các request/response và kết quả thử nghiệm được ghi nhận (HTTP status, mã lỗi, kết quả mong đợi/thực tế), không lưu password/token thật vào tài liệu hoặc log.
