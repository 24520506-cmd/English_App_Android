# Đối chiếu API User: Backend và Android

**Ngày rà soát:** 2026-11-01  
**Nguồn trạng thái backend:** `user-api-contract.md` (draft; mô tả backend đã triển khai `API-U01`–`API-U11` và `API-U23`)  
**Phạm vi:** Các endpoint User API trong contract và phần Android Retrofit đang khai báo/gọi.  
**Giới hạn xác minh:** Workspace hiện không có source backend, cấu hình server hoặc kết quả integration test. Trạng thái backend bên dưới được trích từ tài liệu, chưa được xác minh trực tiếp bằng mã nguồn hay request chạy thật.

## 1. Kết luận

Backend được tài liệu ghi nhận đã có 12 endpoint (`API-U01`–`API-U11`, `API-U23`). Android đã khai báo Retrofit cho 12 endpoint này, nhưng mới gọi thực tế 7 endpoint trong luồng app: đăng ký, đăng nhập, refresh, đọc `/me`, cập nhật `/me`, logout. `GET /auth/session`, đổi mật khẩu, quản lý phiên và activity signal hiện chỉ khai báo trong interface hoặc chưa được gọi.

Luồng đăng ký/đăng nhập → lấy hồ sơ → Home đã được nối ở mức mã nguồn. Chưa thể xác nhận tương thích request/response hoặc chạy end-to-end vì backend và PostgreSQL không nằm trong workspace, đồng thời contract còn là bản nháp.

## 2. Bảng đối chiếu endpoint

| ID | Endpoint | Backend theo tài liệu | Android khai báo | Android gọi từ luồng app | Khoảng trống / việc tiếp theo |
|---|---|---|---|---|---|
| API-U01 | `POST /users` | Đã triển khai | Có | Có — `RegisterActivity` tạo user | Đối chiếu payload, validation và response thực tế; đăng ký hiện gửi email/phone null. |
| API-U02 | `POST /auth/login` | Đã triển khai | Có | Có — `LoginActivity`, sau đăng ký và màn hình auth state | Kiểm tra response token, lỗi `INVALID_CREDENTIALS`, giới hạn đăng nhập và device label. |
| API-U03 | `POST /auth/refresh` | Đã triển khai | Có | Có — khôi phục session bằng refresh token | Kiểm tra token rotation, xử lý refresh không chắc kết quả và hành vi khi refresh/revoke đồng thời. |
| API-U04 | `GET /auth/session` | Đã triển khai | Có | Chưa thấy nơi gọi | Chưa dùng để xác thực/restoration; hiện client khôi phục bằng API-U03. |
| API-U05 | `POST /auth/logout` | Đã triển khai | Có | Có — luồng logout trong `StateScreensActivity` | Kiểm tra revoke server thành công trước khi xóa token/cập nhật UI. |
| API-U06 | `GET /me` | Đã triển khai | Có | Có — Home và Profile | Home chỉ dùng tên hiển thị; khi lỗi API, giữ lời chào trung tính nên lỗi không hiện rõ. |
| API-U07 | `PATCH /me` | Đã triển khai | Có | Có — chỉnh tên hồ sơ trong `ProfileUiActivity` | Kiểm tra trường được phép sửa, null/empty và lỗi validation theo backend. |
| API-U08 | `POST /me/password-change` | Đã triển khai | Có | Chưa thấy nơi gọi | Thiếu repository method và luồng UI xử lý đổi mật khẩu/đăng xuất sau đổi. |
| API-U09 | `GET /me/sessions` | Đã triển khai | Có | Chưa thấy nơi gọi | Thiếu repository method và UI hiển thị các phiên đang hoạt động. |
| API-U10 | `DELETE /me/sessions/{session_id}` | Đã triển khai | Có | Chưa thấy nơi gọi | Thiếu repository method/UI thu hồi phiên; phải xử lý riêng khi thu hồi phiên hiện tại. |
| API-U11 | `POST /me/sessions/revoke-all` | Đã triển khai | Có | Chưa thấy nơi gọi | Thiếu repository method/UI xác nhận thu hồi toàn bộ phiên và xóa token cục bộ. |
| API-U12–U15 | Password reset | Còn mở/chưa triển khai theo tài liệu | Chưa khai báo | Chưa gọi | Cần backend recovery API, sau đó bổ sung Retrofit và hoàn thiện `ForgotPasswordActivity`. |
| API-U16–U20 | Phone verification/contact change | Còn mở/chưa triển khai theo tài liệu | Chưa khai báo | Chưa gọi | Cần quyết định/chốt luồng OTP và backend trước khi nối Android. |
| API-U21–U22 | Google link/callback | Còn mở/chưa triển khai theo tài liệu | Chưa khai báo | Chưa gọi | Cần backend OAuth và quyết định callback/deep link Android. |
| API-U23 | `POST /auth/activity` | Đã triển khai | Có | Chưa thấy nơi gọi | Cần gửi đúng lúc có tương tác foreground theo contract; không gửi chỉ vì mở app hoặc chạy nền. |

## 3. Chênh lệch ưu tiên

### P0 — Cần để xác nhận luồng cơ bản chạy thật

1. Cung cấp backend đang chạy cùng PostgreSQL/schema đã migrate và URL mà emulator/thiết bị truy cập được.
2. Đối chiếu contract với backend thật cho `API-U01`, `U02`, `U03`, `U05`, `U06`, `U07`: path, JSON field, status code, response và error envelope.
3. Chạy app với URL backend đó, kiểm tra đăng ký → đăng nhập → refresh/khôi phục → Home tải `/me`.
4. Xác minh cấu hình cleartext HTTP cho debug: hiện manifest có quyền Internet nhưng không thấy `usesCleartextTraffic`; nếu server debug chỉ dùng HTTP, request có thể bị Android chặn.

### P1 — API backend đã có nhưng Android chưa nối

1. Bổ sung repository và UI cho `API-U08`–`U11` (đổi mật khẩu, xem/thu hồi phiên).
2. Nối `API-U23` theo điều kiện hoạt động trong contract.
3. Quyết định có cần gọi `API-U04` hay tiếp tục chỉ dùng refresh cho khôi phục session; hiện interface có endpoint nhưng không sử dụng.

### P2 — Chưa có ở cả phạm vi backend được mô tả và Retrofit

1. Chốt các quyết định product còn mở cho reset password, xác minh contact và Google/Zalo.
2. Triển khai API backend tương ứng (`API-U12`–`U22`) rồi mới thêm client/UI. Không tính các API này là chênh lệch Android so với backend hiện hữu vì contract nói backend còn mở.

## 4. Các điểm cần đối chiếu trực tiếp với backend

- Contract nói mutating request có thể retry cần `Idempotency-Key`; các khai báo Retrofit hiện tại chưa gửi header này. Cần xác nhận endpoint nào bắt buộc header và bổ sung trước khi retry tự động.
- Contract đề xuất refresh token rotate mỗi lần; client đang lưu refresh token mới sau response thành công. Cần kiểm tra server có đúng response shape `access_token`, `refresh_token`, `session`, `user`.
- Contract yêu cầu lỗi theo envelope `{ "error": { "code", "message", ... } }`; client parse cấu trúc này nhưng fallback sang `HTTP_<status>` nếu response backend khác định dạng.
- Tài liệu gọi contract là draft và nói integration tests PostgreSQL chưa chạy trên database truy cập được. Đây là các phụ thuộc còn lại trước khi kết luận API khớp và hoạt động.

## 5. Tiêu chí hoàn tất đối chiếu P0

- Backend owner xác nhận phiên bản endpoint/schema đang chạy khớp contract hoặc cung cấp OpenAPI/collection chính thức.
- Đăng ký tạo đúng learner; login trả session/token hợp lệ; refresh xoay token; logout revoke session.
- `GET /me` trả đúng account của token hiện tại; Home hiển thị `profile.display_name` hoặc fallback `username`.
- Session bị hết hạn/thu hồi không đưa user vào Home; lỗi mạng được phân biệt với lỗi xác thực.
- Các request/response và kết quả thử nghiệm được ghi nhận (HTTP status, mã lỗi, kết quả mong đợi/thực tế), không lưu password/token thật vào tài liệu hoặc log.
