# CRM API CONTRACT — SPRINT 1 & SPRINT 2

> Phiên bản: 1.2 (TASK 03 — Final Validation)
> Baseline commit: `b61bee548e07fe5c33041a7cbc2e392d7c58744e`
> Công nghệ: Jakarta Servlet 6.0 (Java 21) | Apache Tomcat 10.1 | MySQL 8.x | JSP SSR & JSON REST API

Tài liệu này chuẩn hóa API Contract chính thức cho toàn bộ 20 User Story thuộc Sprint 1 và Sprint 2 của dự án CRM.

Hệ thống phân loại trạng thái kiểm chứng:
- **`IMPLEMENTED`**: Đã có mã nguồn thực hiện chức năng trong hệ thống.
- **`TESTED`**: Có bài kiểm thử tự động đã chạy thành công (lưu ý: `TESTED` không tương đương hoàn thành toàn bộ Acceptance Criteria).
- **`AC PASS`**: Có đầy đủ bằng chứng kiểm chứng đáp ứng tiêu chí chấp nhận cụ thể.
- **`PARTIAL`**: Chỉ đáp ứng một phần tiêu chí chấp nhận.
- **`NOT TESTED`**: Tiêu chí chưa được kiểm thử hoặc chưa có bằng chứng thực tế trên môi trường độc lập.
- **`NEEDS APPROVAL`**: Có quyết định kỹ thuật hoặc nghiệp vụ chưa được phê duyệt/chốt.

---

## 1. Nguyên tắc chung của API Contract

1. **Kiến trúc phân tầng và giao thức:**
   - Kiến trúc xử lý chuẩn: `Filter -> Servlet -> Service -> DAO -> JDBC -> MySQL`.
   - **Giao diện HTML/JSP (Server-Side Rendering):** Dành cho hiển thị trên trình duyệt. Servlet xử lý `GET` forward tới JSP nội bộ (`/jsp/...`). `ViewAccessFilter` chặn toàn bộ truy cập HTTP trực tiếp tới thư mục `/jsp/*` (trả về HTTP 404).
   - **JSON REST API:** Dành cho các thao tác dữ liệu, AJAX hoặc tích hợp hệ thống. Endpoint chuẩn có tiền tố `/api/` hoặc xử lý khi request có header `Accept: application/json`.
   - Cấu trúc phản hồi JSON chuẩn:
     ```json
     {
       "success": true,
       "message": "Thông báo kết quả thực hiện",
       "data": { ... }
     }
     ```
   - Cấu trúc phản hồi lỗi JSON (`success: false`):
     ```json
     {
       "success": false,
       "message": "Mô tả nguyên nhân lỗi cụ thể"
     }
     ```

2. **Quản lý phiên (Session) & Xác thực (Authentication):**
   - Quản lý phiên thông qua cookie `JSESSIONID` do Tomcat cấp phát (`HttpOnly: true`, thời gian chờ không hoạt động: 30 phút).
   - Thuộc tính phiên máy chủ (`HttpSession`): `userId` (Long), `roles` (List<String>), `displayName` (String), `currentUser` (Map/DTO chứa id người dùng). Tuyệt đối không lưu mật khẩu hoặc password hash vào session.
   - Endpoint công khai (Public): `/login`, `/api/auth/login`, `/forgot-password`, `/api/auth/forgot-password`, `/reset-password`, `/api/auth/reset-password`, `/errors/*`.
   - Khi phiên hết hạn hoặc chưa đăng nhập:
     - Yêu cầu trang HTML: Chuyển hướng 302 về `/login?expired=1`.
     - Yêu cầu API JSON: Trả về HTTP `401 Unauthorized`.

3. **Bảo vệ chống tấn công CSRF:**
   - `CsrfFilter` kiểm tra token đối với mọi HTTP request làm thay đổi dữ liệu (POST, PUT, DELETE) khi có phiên đăng nhập hoạt động.
   - Client lấy token hợp lệ từ thuộc tính `data.csrfToken` qua API `GET /api/auth/session` hoặc trường ẩn `<input type="hidden" name="csrfToken" value="...">` trong form JSP.
   - Header hỗ trợ: `X-CSRF-Token` hoặc form parameter: `csrfToken`.

4. **Phân quyền phạm vi dữ liệu (Data Scope):**
   - Ba mức phạm vi: `SELF` (chỉ xem bản ghi của chính mình), `TEAM` (xem bản ghi của đội nhóm), `ALL` (xem toàn bộ dữ liệu hệ thống).
   - Áp dụng trên 4 thực thể chính: Khách hàng (`customers`), Cơ hội (`opportunities`), Hoạt động (`activities`), Báo giá (`quotes`).
   - **Quy chuẩn kỹ thuật phạm vi TEAM:** Dựa trên AC, schema và mã nguồn hiện tại, phạm vi `TEAM` được thực thi chính xác bằng điều kiện so khớp trực tiếp `record.ownerTeamId() == actor.teamId()` hoặc `u.team_id = ?`. Hệ thống chưa hỗ trợ truy cập dữ liệu của các nhóm con (`parent_id`) trong cây tổ chức; mọi mở rộng ngoài phạm vi này đều phải có quyết định phê duyệt chính thức.

5. **Quy chuẩn mã phản hồi HTTP:**
   - `200 OK`: Thực hiện thành công.
   - `201 Created`: Tạo mới tài nguyên thành công.
   - `302 Found`: Chuyển hướng sau khi gửi biểu mẫu JSP thành công.
   - `400 Bad Request`: Tham số không hợp lệ, dữ liệu lỗi định dạng hoặc vi phạm ràng buộc nghiệp vụ.
   - `401 Unauthorized`: Chưa đăng nhập hoặc phiên làm việc đã hết hạn.
   - `403 Forbidden`: Người dùng không có quyền thực hiện hoặc thiếu/sai token CSRF.
   - `404 Not Found`: Đường dẫn hoặc bản ghi không tồn tại (bao gồm truy cập trực tiếp `/jsp/*`).
   - `405 Method Not Allowed`: Phương thức HTTP không được hỗ trợ trên endpoint.
   - `409 Conflict`: Xung đột dữ liệu hoặc tài nguyên đang được tham chiếu không thể xóa.
   - `413 Payload Too Large`: Kích thước tệp tải lên vượt giới hạn cho phép (Avatar > 2MB).
   - `500 Internal Server Error`: Lỗi máy chủ hoặc lỗi cơ sở dữ liệu.

---

## 2. Chi tiết API Contract cho 20 User Story

### S1-01 (CRM-21) — Đăng nhập & Khóa bảo vệ tạm thời
- **Mục đích:** Xác thực người dùng, kiểm tra trạng thái hoạt động, hỗ trợ đa vai trò, khóa tạm thời 15 phút nếu nhập sai quá 5 lần liên tiếp.
- **URL hiển thị JSP:** `GET /login` (forward `/jsp/auth/login.jsp`).
- **API Endpoints:**
  - `POST /login`: Submit form đăng nhập JSP. Form params: `email` (string), `password` (string). Chuyển hướng 302 về `/dashboard` nếu thành công; trả lại trang đăng nhập với thông báo lỗi nếu thất bại.
  - `POST /api/auth/login`: API JSON. Content-Type: `application/json` hoặc `application/x-www-form-urlencoded`. Body: `{"email": "...", "password": "..."}`.
- **Response Format:**
  - 200 OK: `{"success": true, "message": "Đăng nhập thành công", "data": {"userId": 1, "displayName": "...", "roles": ["Admin"]}}`
  - 400 Bad Request: `{"success": false, "message": "Vui lòng nhập email và mật khẩu"}`
  - 401 Unauthorized: `{"success": false, "message": "Email hoặc mật khẩu không đúng"}`
  - 403 Forbidden: `{"success": false, "message": "Tài khoản bị tạm khóa do nhập sai quá 5 lần liên tiếp. Vui lòng thử lại sau 15 phút."}`
- **Quyền truy cập:** Public (chưa đăng nhập).
- **Session & CSRF:** Khi đăng nhập thành công, hủy session cũ, tạo session mới, cấp CSRF token.

### S1-02 (CRM-22) — Đăng xuất & Quản lý phiên
- **Mục đích:** Đăng xuất người dùng, vô hiệu hóa session phía máy chủ, thu hồi CSRF token, dọn dẹp phiên khỏi `SessionRegistry`.
- **API Endpoints:**
  - `POST /api/auth/logout`: Đăng xuất phiên hiện tại.
    - Param tùy chọn: `redirectToLogin=true` (chuyển hướng 302 về `/login`).
    - Response 200 JSON: `{"success": true, "message": "Đăng xuất thành công"}`
  - `GET /api/auth/session`: Lấy thông tin phiên hiện tại và CSRF token.
    - Response 200 (đã đăng nhập): `{"success": true, "data": {"authenticated": true, "userId": 1, "roles": [...], "csrfToken": "..."}}`
    - Response 200 (chưa đăng nhập): `{"success": true, "data": {"authenticated": false}}`
- **Quyền truy cập:** Đã đăng nhập (`POST /api/auth/logout`); Public (`GET /api/auth/session`).

### S1-03 (CRM-23) — Quên & Đặt lại mật khẩu
- **Mục đích:** Yêu cầu đặt lại mật khẩu qua email, cấp token có hiệu lực 30 phút, sử dụng đúng một lần, thông báo chung để chống rò rỉ thông tin tài khoản (anti-enumeration).
- **URL hiển thị JSP:**
  - `GET /forgot-password`: Forward `/jsp/auth/forgot-password.jsp`.
  - `GET /reset-password?token=...`: Forward `/jsp/auth/reset-password.jsp`.
- **API Endpoints:**
  - `POST /forgot-password`, `POST /api/auth/forgot-password`: Yêu cầu gửi email reset. Param/JSON: `email`.
    - Response 200: `{"success": true, "message": "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi."}`
  - `POST /reset-password`, `POST /api/auth/reset-password`: Đặt lại mật khẩu mới. Param/JSON: `token`, `newPassword`.
    - Response 200: `{"success": true, "message": "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới."}`
    - Response 400: `{"success": false, "message": "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."}`
- **Quyền truy cập:** Public.

### S1-04 (CRM-24) — Đổi mật khẩu & Thu hồi phiên khác
- **Mục đích:** Người dùng đã đăng nhập tự đổi mật khẩu; kiểm tra mật khẩu hiện tại, áp dụng chính sách mật khẩu, tự động thu hồi tất cả các phiên đăng nhập khác của người dùng qua `SessionRegistry`.
- **URL hiển thị JSP:** `GET /change-password` (forward `/jsp/auth/change-password.jsp`).
- **API Endpoints:**
  - `POST /change-password`, `POST /api/auth/change-password`: Gửi yêu cầu đổi mật khẩu.
    - Param/JSON: `currentPassword`, `newPassword`, `confirmPassword`.
    - Response 200: `{"success": true, "message": "Đổi mật khẩu thành công", "data": {"revokedSessions": 2}}`
    - Response 400: `{"success": false, "message": "Mật khẩu hiện tại không đúng"}` hoặc `{"success": false, "message": "Mật khẩu mới phải có ít nhất 8 ký tự, gồm chữ và số"}`
- **Quyền truy cập:** Đã đăng nhập. Bắt buộc kiểm tra CSRF token.

### S1-05 (CRM-25) — Phân quyền phạm vi dữ liệu (SELF / TEAM / ALL)
- **Mục đích:** Kiểm soát quyền xem danh sách, xem chi tiết và xuất Excel cho 4 thực thể: `customers`, `opportunities`, `activities`, `quotes` theo phạm vi dữ liệu của người dùng.
- **URL hiển thị JSP:**
  - `GET /customers`, `GET /opportunities`, `GET /activities`, `GET /quotes`: Forward `/jsp/shared/scoped-records.jsp`.
  - `GET /customers/detail?id=...`, `/quotes/detail?id=...`: Forward `/jsp/shared/scoped-record-detail.jsp`.
- **API Endpoints:**
  - `GET /api/customers`, `/api/opportunities`, `/api/activities`, `/api/quotes`: Lấy danh sách bản ghi theo phạm vi phân quyền.
  - `GET /api/scope/records/export?type={customers|opportunities|activities|quotes}`: Xuất dữ liệu ra tệp Excel (.xlsx) theo đúng phạm vi phân quyền.
- **Response Format:**
  - Response 200 JSON: `{"success": true, "data": {"type": "quotes", "items": [...], "pagination": {"page": 1, "size": 20, "total": 45}}}`
  - Response 200 XLSX: Tệp nhị phân Excel `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
  - Response 403: `{"success": false, "message": "Bạn không có quyền truy cập bản ghi này."}`
- **Quyền truy cập:** Đã đăng nhập; lọc dữ liệu tự động theo `data_scope` trong phiên.

### S1-06 (CRM-26) — Menu điều hướng động & Thông tin người dùng
- **Mục đích:** Hiển thị menu chức năng động theo vai trò người dùng, hiển thị thông tin tài khoản hiện hành.
- **API Endpoints:**
  - `GET /api/navigation/menu`: Trả về danh sách mục menu được phép truy cập theo vai trò.
    - Response 200: `{"success": true, "data": {"menuItems": [{"id": "CUSTOMERS", "label": "Khách hàng", "path": "/customers", ...}, ...]}}`
- **Quyền truy cập:** Đã đăng nhập.
- **Lưu ý định tuyến:** Tuyến đường `/quotes` là chức năng đang hoạt động (thuộc phân hệ scoped records). Chỉ có các mục menu `/leads`, `/kpi`, `/automation` là chưa có trang hiển thị riêng nên cần được xem xét ẩn khỏi thanh điều hướng người dùng.

### S1-07 (CRM-27) — Các trang lỗi hệ thống
- **Mục đích:** Xử lý và hiển thị thông báo lỗi thân thiện, trả về đúng mã trạng thái HTTP tiêu chuẩn (401, 403, 404, 500).
- **URL hiển thị JSP:**
  - `GET /errors/401`: Forward `/jsp/errors/401.jsp` (HTTP 401).
  - `GET /errors/403`: Forward `/jsp/errors/403.jsp` (HTTP 403).
  - `GET /errors/404`: Forward `/jsp/errors/404.jsp` (HTTP 404).
  - `GET /errors/500`: Forward `/jsp/errors/500.jsp` (HTTP 500).
- **Quyền truy cập:** Public.

### S1-08 (CRM-28) — Quản lý tài khoản người dùng
- **Mục đích:** Quản trị viên tạo, sửa, tìm kiếm, lọc và phân trang tài khoản người dùng.
- **URL hiển thị JSP:**
  - `GET /users`: Forward `/jsp/users/user-list.jsp` (Danh sách tài khoản).
  - `GET /users/create`: Forward `/jsp/users/user-create.jsp` *(Hiện là placeholder)*.
  - `GET /users/detail?id=...`: Forward `/jsp/users/user-detail.jsp`.
  - `GET /users/edit?id=...`: Forward `/jsp/users/user-edit.jsp` *(Hiện là placeholder)*.
- **API Endpoints:**
  - `GET /api/users?search=...&role=...&status=...&page=1&size=20`: Tìm kiếm người dùng.
  - `GET /api/users/detail?id=...`: Lấy thông tin chi tiết người dùng.
  - `POST /api/users`: Tạo mới tài khoản người dùng (yêu cầu Admin).
  - `PUT /api/users`: Cập nhật thông tin tài khoản người dùng (yêu cầu Admin).
  - `DELETE /api/users?id=...`: Xóa tài khoản người dùng (chặn xóa chính mình).
- **Quyền truy cập:** Vai trò `Admin` hoặc `Director`. Bắt buộc kiểm tra CSRF token cho các thao tác ghi.

### S1-09 (CRM-29) — Phân vai trò & Quản lý nhóm
- **Mục đích:** Gán vai trò cho người dùng, hỗ trợ đa vai trò, gắn người dùng vào nhóm kinh doanh, quy tắc bắt buộc gắn nhóm đối với vai trò Trưởng nhóm (Team Lead), chống tự thu hồi quyền Admin của chính mình.
- **URL hiển thị JSP:** `GET /permissions`: Forward `/jsp/permissions/role-permission.jsp`.
- **API Endpoints:**
  - `GET /api/permissions/roles`: Lấy danh sách tất cả vai trò.
  - `GET /api/permissions/user-roles?userId=...`: Lấy vai trò của người dùng.
  - `POST /api/permissions/user-roles`: Cập nhật danh sách vai trò cho người dùng.
    - Body JSON: `{"userId": 5, "roles": ["Sales Rep", "Team Lead"], "teamId": 2}`
    - Response 200: `{"success": true, "message": "Cập nhật vai trò thành công"}`
    - Response 400: `{"success": false, "message": "Vai trò Trưởng nhóm bắt buộc người dùng phải thuộc về một nhóm kinh doanh"}`
    - Response 403: `{"success": false, "message": "Quản trị viên không thể tự thu hồi quyền Admin của chính mình"}`
- **Quyền truy cập:** Vai trò `Admin`.

### S1-10 (CRM-30) — Khóa tài khoản & Bàn giao dữ liệu
- **Mục đích:** Khóa tài khoản nhân viên, bắt buộc bàn giao toàn bộ dữ liệu đang phụ trách (khách hàng, cơ hội) sang nhân sự khác trong một giao dịch cơ sở dữ liệu duy nhất, ghi nhật ký bàn giao, thu hồi phiên làm việc ngay lập tức.
- **API Endpoints:**
  - `POST /api/users/lock`: Khóa tài khoản người dùng.
    - Body JSON: `{"targetUserId": 10, "recipientUserId": 12, "reason": "Nghỉ việc"}`
    - Response 200: `{"success": true, "message": "Khóa tài khoản và hoàn tất bàn giao dữ liệu thành công"}`
    - Response 400: `{"success": false, "message": "Người dùng đang sở hữu khách hàng/cơ hội, bắt buộc chọn người nhận bàn giao"}`
  - `POST /api/users/unlock`: Mở khóa tài khoản người dùng.
    - Body JSON: `{"targetUserId": 10}`
    - Response 200: `{"success": true, "message": "Mở khóa tài khoản thành công"}`
- **Bảo mật trạng thái tài khoản:** Trên màn hình đăng nhập công khai, hệ thống duy trì thông báo chung để tránh tiết lộ trạng thái tài khoản cho người chưa được xác thực (anti-enumeration).
- **Quyền truy cập:** Vai trò `Admin`.

### S2-01 (CRM-32) — Nhập người dùng từ Excel hàng loạt
- **Mục đích:** Tải tệp biểu mẫu (.xlsx / .csv), tải lên tệp danh sách người dùng, kiểm tra định dạng và dữ liệu, bỏ qua các dòng lỗi để nạp các dòng hợp lệ theo lô vào cơ sở dữ liệu, xuất báo cáo kết quả chi tiết.
- **URL hiển thị JSP:** `GET /users/import`: Forward `/jsp/users/user-import.jsp`.
- **API Endpoints:**
  - `GET /api/users/import/template?format={xlsx|csv}`: Tải tệp mẫu Excel/CSV.
  - `POST /api/users/import/preview`: Tải lên tệp để kiểm tra và xem trước kết quả. Multipart form data (`file`).
  - `POST /api/users/import/confirm`: Xác nhận nhập khẩu lô đã kiểm tra vào cơ sở dữ liệu.
- **Response Format:**
  - Response 200 JSON: `{"success": true, "data": {"totalRows": 50, "successCount": 45, "errorCount": 5, "errors": [{"rowNumber": 4, "field": "email", "errorMessage": "Email đã tồn tại:..."}]}}`
- **Quyền truy cập:** Vai trò `Admin`.

### S2-02 (CRM-35) — Quản lý hồ sơ cá nhân
- **Mục đích:** Người dùng tự xem thông tin hồ sơ, cập nhật họ tên và số điện thoại; hệ thống nghiêm cấm tự ý chỉnh sửa email, vai trò hoặc trạng thái tài khoản.
- **URL hiển thị JSP:** `GET /profile`: Forward `/jsp/users/profile.jsp`.
- **API Endpoints:**
  - `GET /api/users/me`: Lấy thông tin hồ sơ của chính mình.
  - `POST /profile`, `PUT /api/users/me`: Cập nhật thông tin hồ sơ cá nhân.
    - Form/JSON: `fullName`, `phone`. (Email và các trường phân quyền bị bỏ qua/giữ nguyên giá trị cũ).
    - Response 200: `{"success": true, "message": "Cập nhật hồ sơ thành công"}`
- **Quyền truy cập:** Đã đăng nhập.

### S2-03 (CRM-36) — Tải lên ảnh đại diện (Avatar)
- **Mục đích:** Tải lên ảnh đại diện (JPG/PNG, tối đa 2MB), xử lý cắt vuông tâm tự động và tạo ảnh thu nhỏ; hiển thị ảnh trên header và trang cá nhân.
- **Thông số kỹ thuật xử lý ảnh (AvatarImageProcessor):**
  - Tệp tối đa: 2.097.152 byte (2 MiB); tối đa 16.000.000 pixels.
  - Ảnh đại diện chính (square): 512x512 pixel.
  - Ảnh thu nhỏ (thumbnail): 128x128 pixel.
  - *Lưu ý:* AC gốc không quy định kích thước pixel cụ thể; các giá trị trên là thông số cài đặt kỹ thuật trong mã nguồn.
- **URL hiển thị JSP:** `GET /profile/avatar`: Forward `/jsp/users/avatar.jsp`.
- **API Endpoints:**
  - `GET /profile/avatar/image?userId=...`: Trả về dữ liệu nhị phân ảnh đại diện gốc (512x512 PNG).
  - `GET /profile/avatar/thumbnail?userId=...`: Trả về dữ liệu nhị phân ảnh thu nhỏ (128x128 PNG).
  - `POST /profile/avatar`, `POST /api/users/me/avatar`: Tải lên tệp ảnh đại diện (Multipart `image`).
    - Response 200: `{"success": true, "message": "Cập nhật ảnh đại diện thành công"}`
    - Response 400: `{"success": false, "message": "Vui lòng chọn ảnh JPG/JPEG hoặc PNG."}`
    - Response 413: `{"success": false, "message": "Ảnh không được vượt quá 2 MiB (2.097.152 byte)."}`
- **Quyền truy cập:** Đã đăng nhập.

### S2-04 (CRM-37) — Nhật ký kiểm toán hệ thống (Audit Logs)
- **Mục đích:** Ghi nhận lịch sử các biến động dữ liệu nhạy cảm kèm giá trị trước/sau dạng JSON trong cùng một giao dịch JDBC; cung cấp giao diện tra cứu và bộ lọc theo người thực hiện, loại hành động và khoảng thời gian.
- **URL hiển thị JSP:** `GET /audit`: Forward `/jsp/audit/audit-log.jsp`.
- **API Endpoints:**
  - `GET /api/audit/logs?actorUserId=...&action=...&objectType=...&from=...&to=...&page=1&size=20`: Tra cứu danh sách nhật ký kiểm toán.
- **Response Format:**
  - Response 200 JSON: `{"success": true, "data": {"items": [{"id": 1, "actorUserId": 2, "action": "ROLE_CHANGED", "objectType": "USER", "objectId": 5, "beforeValue": {...}, "afterValue": {...}, "createdAt": "..."}], "total": 120}}`
- **Quyền truy cập:** Vai trò `Admin` hoặc `Director`.

### S2-05 (CRM-39) — Danh mục sản phẩm & Bảng giá
- **Mục đích:** Quản lý danh mục sản phẩm và bảng giá bán hàng; phân quyền bảo mật giá vốn; kiểm tra ràng buộc giá niêm yết >= giá sàn; cơ chế chống xóa an toàn khi sản phẩm đã được tham chiếu trong báo giá, cơ hội, đơn hàng.
- **Đặc tả phân quyền giá vốn & Điểm bất đồng kỹ thuật:**
  - **Yêu cầu AC gốc:** Chỉ Giám đốc kinh doanh (Sales Director) mới được xem và sửa giá vốn (`cost_price`).
  - **Mã nguồn hiện tại:** `ProductService.isDirectorOrAdmin` cho phép cả `Admin` (Quản trị viên) và `Director` (Giám đốc) xem và sửa giá vốn.
  - **Ghi nhận GAP:** Hệ thống cho phép Quản trị viên can thiệp giá vốn; đồng thời hệ thống hiện tại chưa phân tách vai trò "Giám đốc kinh doanh" riêng biệt mà sử dụng vai trò chung `Director`. Điểm này cần được phê duyệt trước khi thay đổi mã nguồn.
- **URL hiển thị JSP:**
  - `GET /products`: Forward `/jsp/products/product-list.jsp`.
  - `GET /products/create`: Forward `/jsp/products/product-form.jsp`.
  - `GET /products/edit?id=...`: Forward `/jsp/products/product-form.jsp`.
- **API Endpoints:**
  - `GET /api/products?keyword=...&category=...&activeOnly=...&page=1&size=10`: Danh sách sản phẩm.
  - `GET /api/products/detail?id=...`: Chi tiết sản phẩm.
  - `POST /api/products`: Tạo mới sản phẩm.
  - `PUT /api/products`: Cập nhật sản phẩm.
  - `DELETE /api/products?id=...`: Xóa sản phẩm.
- **Response Format:**
  - Response 200: Thành công.
  - Response 400: `{"success": false, "message": "Giá niêm yết không được nhỏ hơn giá sàn"}`
  - Response 409: `{"success": false, "message": "Không thể xóa sản phẩm vì đã được sử dụng trong các giao dịch/chứng từ"}`
- **Quyền truy cập:** Xem: Mọi nhân viên; Thao tác ghi & Giá vốn: Phân quyền theo quy tắc trên.

### S2-06 (CRM-42) — Cơ cấu tổ chức & Cây nhóm kinh doanh
- **Mục đích:** Thiết lập sơ đồ tổ chức phòng ban/nhóm kinh doanh theo mô hình phân cấp cây (cha - con), phân bổ trưởng nhóm và chỉ định khu vực/chi nhánh phụ trách.
- **Cấu trúc dữ liệu & Phạm vi:**
  - Bảng `teams` có cột `parent_id` (tự tham chiếu) biểu diễn quan hệ cha-con, `leader_user_id` chỉ định người quản lý, `region` xác định khu vực. Bảng `users` liên kết qua `team_id`.
  - Phân quyền phạm vi `TEAM` chỉ giới hạn trong nhóm trực tiếp (`team_id = actor.teamId()`), chưa hỗ trợ truy cập dữ liệu nhóm con qua cây tổ chức.
- **URL hiển thị JSP:** `GET /organization`: Forward `/jsp/organization/organization.jsp`.
- **API Endpoints:**
  - `GET /api/organization/tree`: Lấy toàn bộ cây tổ chức kinh doanh.
  - `GET /api/organization/teams`: Lấy danh sách các nhóm kinh doanh.
  - `POST /api/organization/teams`: Tạo nhóm kinh doanh mới.
  - `PUT /api/organization/teams`: Cập nhật thông tin nhóm kinh doanh.
  - `DELETE /api/organization/teams?id=...`: Xóa nhóm kinh doanh (chặn nếu còn nhóm con hoặc nhân viên).
- **Quyền truy cập:** Vai trò `Admin`, `Director`.

### S2-07 (CRM-44) — Danh mục dùng chung
- **Mục đích:** Quản lý các danh mục cấu hình dùng chung trong toàn hệ thống (loại khách hàng, nguồn dữ liệu, v.v.), hỗ trợ sắp xếp thứ tự hiển thị và chặn xóa khi đang có dữ liệu liên kết.
- **URL hiển thị JSP:** `GET /configuration`: Forward `/jsp/configuration/configuration.jsp`.
- **API Endpoints:**
  - `GET /api/categories?type=...`: Lấy danh mục theo loại.
  - `POST /api/categories`: Tạo mới mục danh mục.
  - `PUT /api/categories`: Cập nhật mục danh mục.
  - `DELETE /api/categories?id=...`: Xóa mục danh mục.
- **Quyền truy cập:** Vai trò `Admin`, `Director`.

### S2-08 (CRM-46) — Quản lý trường tùy chỉnh (Custom Fields)
- **Mục đích:** Cho phép định nghĩa các trường dữ liệu động với 4 kiểu dữ liệu: `TEXT`, `NUMBER`, `DATE`, `SELECT` áp dụng cho hai thực thể bắt buộc theo AC gốc: Khách hàng (`CUSTOMER`) và Cơ hội (`OPPORTUNITY`).
- **URL hiển thị JSP:** `GET /customfields`: Forward `/jsp/customfields/custom-field-list.jsp`.
- **API Endpoints:**
  - `GET /api/customfields?entity={CUSTOMER|OPPORTUNITY}`: Lấy danh sách định nghĩa trường.
  - `POST /api/customfields`: Tạo định nghĩa trường tùy chỉnh mới.
  - `PUT /api/customfields`: Cập nhật định nghĩa trường tùy chỉnh.
  - `DELETE /api/customfields?id=...`: Xóa hoặc ngừng kích hoạt trường tùy chỉnh.
  - `GET /api/customfields/values?entity=...&recordId=...`: Lấy giá trị trường tùy chỉnh của bản ghi.
  - `POST /api/customfields/values`: Lưu giá trị trường tùy chỉnh cho bản ghi.
- **Quyền truy cập:** Định nghĩa: `Admin`; Đọc/Ghi giá trị: Theo quyền bản ghi thực thể.

### S2-09 (CRM-47) — Quy trình bán hàng (Pipeline Stages)
- **Mục đích:** Thiết lập các giai đoạn trong chu trình bán hàng (tên giai đoạn, thứ tự sắp xếp, xác suất thành công từ 0% đến 100%, điều kiện chuyển giai đoạn); bảo toàn các cơ hội đang hoạt động khi thay đổi hoặc xóa giai đoạn.
- **URL hiển thị JSP:** `GET /pipeline`: Forward `/jsp/pipeline/pipeline-config.jsp`.
- **API Endpoints:**
  - `GET /api/pipeline/stages?pipelineId=...`: Lấy danh sách các giai đoạn bán hàng.
  - `POST /api/pipeline/stages`: Tạo giai đoạn bán hàng mới.
  - `PUT /api/pipeline/stages`: Cập nhật thông tin giai đoạn bán hàng.
  - `POST /api/pipeline/stages/reorder`: Cập nhật lại thứ tự sắp xếp các giai đoạn.
  - `DELETE /api/pipeline/stages?id=...&targetStageIdForMigration=...`: Xóa giai đoạn và di chuyển các cơ hội sang giai đoạn mới (chặn xóa nếu có cơ hội mà không chỉ định giai đoạn đích).
- **Quyền truy cập:** Vai trò `Admin`, `Director`.

### S2-10 (CRM-48) — Quản lý lý do Thắng/Thua & Đối thủ cạnh tranh
- **Mục đích:** Quản lý danh mục lý do thành công (WIN), thất bại (LOSS) và danh sách đối thủ cạnh tranh nhằm phục vụ phân tích bán hàng. (Lưu ý: Nghiệp vụ bắt buộc nhập lý do khi đóng cơ hội sẽ được tích hợp ở Sprint 5).
- **URL hiển thị JSP:** `GET /winloss`: Forward `/jsp/winloss/winloss.jsp`.
- **API Endpoints:**
  - `GET /api/winloss/reasons?includeInactive=...`: Lấy danh sách lý do Thắng/Thua.
  - `POST /api/winloss/reasons`: Tạo lý do mới.
  - `PUT /api/winloss/reasons`: Cập nhật lý do.
  - `DELETE /api/winloss/reasons?id=...`: Xóa hoặc vô hiệu hóa lý do.
  - `GET /api/winloss/competitors`: Lấy danh sách đối thủ cạnh tranh.
  - `POST /api/winloss/competitors`: Tạo đối thủ cạnh tranh mới.
  - `PUT /api/winloss/competitors`: Cập nhật đối thủ cạnh tranh.
  - `DELETE /api/winloss/competitors?id=...`: Xóa đối thủ cạnh tranh.
- **Quyền truy cập:** Xem: Toàn bộ nhân viên; Quản trị danh mục: `Admin`, `Director`.

---

## 3. Ma trận Acceptance Criteria cho 20 User Story

| Story ID | Acceptance Criteria | Mã nguồn liên quan | Kiểm thử | Trạng thái | Việc còn thiếu / Cần hoàn thiện |
|---|---|---|---|---|---|
| **S1-01** (CRM-21) | Đăng nhập đúng thông tin, hỗ trợ đa vai trò, khóa tạm thời 15 phút khi sai 5 lần liên tiếp | `LoginServlet.java`, `AuthService.java`, `LoginAttemptGuard.java`, `UserDAO.java` | `LoginServletTest`, `LoginAttemptGuardTest`, `CRM21LoginCheck` | **TESTED** | Chưa nghiệm thu E2E UI chuyển hướng theo vai trò; `LoginAttemptGuard` lưu tạm trong RAM máy chủ. |
| **S1-02** (CRM-22) | Đăng xuất vô hiệu hóa session, dọn dẹp SessionRegistry, timeout 30 phút, bảo vệ CSRF | `LogoutServlet.java`, `SessionServlet.java`, `CsrfFilter.java`, `SessionRegistry.java` | `CsrfFilterTest`, `SessionRegistryTest`, HTTP logout check | **AC PASS (tạm thời)** | Cần kiểm chứng timeout 30 phút trên môi trường Tomcat staging thực tế. |
| **S1-03** (CRM-23) | Yêu cầu reset pass qua email, token hiệu lực 30 phút, dùng 1 lần, chống email enumeration | `AuthService.java`, `PasswordResetTokenDAO.java`, `EmailService.java`, `ResetTokenUtil.java` | `ForgotPasswordAcceptanceIntegrationTest`, `ResetTokenUtilTest` | **PARTIAL** | Chưa kiểm thử gửi email thực tế ra máy chủ SMTP thật; cần cấu hình biến môi trường `CRM_SMTP_*`. |
| **S1-04** (CRM-24) | Đổi mật khẩu yêu cầu pass cũ, kiểm tra chính sách độ mạnh, thu hồi tất cả các phiên khác | `ChangePasswordServlet.java`, `AuthService.java`, `SessionRegistry.java`, `PasswordUtil.java` | `PasswordUtilTest`, `SessionRegistryTest` | **TESTED** | Chưa có kiểm thử E2E tự động xác nhận các phiên đăng nhập khác trên trình duyệt bị đăng xuất ngay lập tức. |
| **S1-05** (CRM-25) | Phân quyền SELF/TEAM/ALL trên 4 thực thể; xem danh sách, chi tiết và xuất Excel | `ScopedEntityServlet.java`, `DataScopeService.java`, `ScopedEntityDAO.java`, `ExcelService.java` | `ScopedEntityServletTest`, `DataScopeServiceTest`, `ScopeAccessPolicyTest`, HTTP Excel check | **PARTIAL** | TEAM hiện chỉ so khớp cùng `team_id`, chưa hỗ trợ cây nhóm con; phân trang HTML đang cắt danh sách tại tầng Service. |
| **S1-06** (CRM-26) | Menu động theo vai trò; hiển thị đúng quyền; thông tin tài khoản | `MenuNavigationFilter.java`, `MenuService.java`, `MenuDAO.java`, `sidebar.jsp` | `MenuServiceTest`, `MenuNavigationFilterTest`, `MenuServletTest` | **PARTIAL** | Các mục `/leads`, `/kpi`, `/automation` chưa có trang riêng (riêng `/quotes` đã hoạt động). Đề xuất ẩn 3 mục này khỏi giao diện. |
| **S1-07** (CRM-27) | Hiển thị trang lỗi 401, 403, 404, 500 với đúng mã HTTP status | `ErrorPageServlet.java`, `web.xml`, `jsp/errors/*.jsp` | Kiểm tra HTTP trả về đúng status 401, 403, 404, 500 | **AC PASS (tạm thời)** | Cần kiểm tra giao diện hiển thị trên các độ phân giải màn hình khác nhau (responsive). |
| **S1-08** (CRM-28) | Quản lý tài khoản: danh sách, tạo mới, chỉnh sửa, tìm kiếm, lọc, phân trang | `UserServlet.java`, `UserService.java`, `UserDAO.java`, `user-list.jsp` | `UserServletTest`, kiểm tra HTTP `user-list.jsp` | **PARTIAL** | Biểu mẫu `user-create.jsp` và `user-edit.jsp` hiện là placeholder tối giản, chưa có giao diện nhập liệu hoàn chỉnh. Cần gói công việc FE riêng. |
| **S1-09** (CRM-29) | Hỗ trợ đa vai trò; trưởng nhóm bắt buộc gắn team; chặn tự thu hồi quyền Admin | `PermissionServlet.java`, `PermissionService.java`, `UserRoleService.java`, `UserRoleDAO.java` | `UserRoleServiceTest`, kiểm tra HTTP trang quyền | **TESTED** | Cần bổ sung kiểm thử E2E thu hồi ngay lập tức phiên đăng nhập của người dùng khi bị thay đổi vai trò. |
| **S1-10** (CRM-30) | Khóa tài khoản, bắt buộc bàn giao dữ liệu (khách hàng, cơ hội), ghi log, thu hồi session | `UserService.java`, `OwnershipTransferService.java`, `UserLockHandoverDAO.java`, `SessionRegistry.java` | Kiểm tra mã nguồn giao dịch JDBC, test đơn vị dịch vụ | **NEEDS APPROVAL** | Cần phê duyệt phương án thông báo tài khoản bị khóa trên UI để không phá vỡ cơ chế chống tiết lộ tài khoản; kiểm chứng bàn giao trên DB staging. |
| **S2-01** (CRM-32) | Nhập người dùng từ Excel: tải mẫu, kiểm tra dữ liệu, bỏ qua dòng lỗi, lưu lô và báo cáo | `UserImportServlet.java`, `ExcelService.java`, `UserImportDAO.java`, `user-import.jsp` | `ExcelServiceTest`, kiểm tra xử lý workbook | **TESTED** | Chưa có kiểm thử E2E tải lên tệp multipart lớn và tải xuống báo cáo tổng kết lỗi. |
| **S2-02** (CRM-35) | Xem và cập nhật hồ sơ cá nhân; cập nhật số điện thoại; chặn sửa email và vai trò | `ProfileServlet.java`, `ProfileService.java`, `UserDAO.java`, `profile.jsp` | `ProfileServiceTest`, kiểm tra HTTP trang profile | **AC PASS (tạm thời)** | Cần bổ sung kiểm thử E2E tự động trên trình duyệt xác nhận các trường bị khóa không thể bị ghi đè qua DevTools. |
| **S2-03** (CRM-36) | Tải lên ảnh đại diện JPG/PNG tối đa 2MB; tự động crop vuông 512x512 và sinh thumbnail 128x128 | `AvatarServlet.java`, `AvatarService.java`, `AvatarImageProcessor.java`, `avatar.jsp` | `AvatarServiceTest`, `AvatarImageProcessorTest` | **TESTED** | Đã chuẩn hóa JSP sang `/jsp/users/avatar.jsp`; chưa thực hiện kiểm thử HTTP/E2E tải ảnh trên máy chủ Tomcat thật (`CRM36AvatarHttpCheck`). |
| **S2-04** (CRM-37) | Nhật ký kiểm toán before/after JSON; bộ lọc theo user, action, thời gian | `AuditLogServlet.java`, `AuditLogService.java`, `AuditLogDAO.java`, `audit-log.jsp` | `AuditLogServiceTest`, `AuditLogDAOTest` | **PARTIAL** | Phương thức ghi nhật ký chiết khấu (`DISCOUNT_CHANGED`) và chỉ tiêu (`TARGET_CHANGED`) chưa được gọi từ các luồng nghiệp vụ thực tế. |
| **S2-05** (CRM-39) | Danh mục sản phẩm: bảo vệ giá vốn (chỉ Director/Admin), giá niêm yết >= sàn, chặn xóa an toàn | `ProductServlet.java`, `ProductPageServlet.java`, `ProductService.java`, `ProductDAO.java` | `ProductServiceTest`, `ProductServletAuthorizationTest` | **TESTED** | Cần phê duyệt GAP phân quyền giá vốn (cho phép Admin cùng Director); chưa kiểm thử E2E toàn bộ luồng thêm/sửa/xóa và ngăn xóa trên DB thật. |
| **S2-06** (CRM-42) | Cây cơ cấu tổ chức nhóm kinh doanh, phân cấp cha-con, chỉ định trưởng nhóm và khu vực | `OrganizationServlet.java`, `OrganizationService.java`, `OrganizationDAO.java`, `organization.jsp` | `OrganizationServiceTest`, `OrganizationServletTest` | **PARTIAL** | Phân quyền phạm vi TEAM hiện chỉ giới hạn trong cùng `team_id`, chưa mở rộng ra cây nhóm con. |
| **S2-07** (CRM-44) | Danh mục dùng chung: CRUD danh mục, quản lý thứ tự sắp xếp, chặn xóa khi có tham chiếu | `CategoryServlet.java`, `CategoryService.java`, `CategoryDAO.java`, `configuration.jsp` | `CategoryServiceTest` | **AC PASS (tạm thời)** | Cần kiểm tra giao diện kéo thả sắp xếp thứ tự và kiểm chứng chặn xóa trên dữ liệu thực tế. |
| **S2-08** (CRM-46) | Quản lý trường tùy chỉnh (TEXT, NUMBER, DATE, SELECT) cho CUSTOMER và OPPORTUNITY | `CustomFieldServlet.java`, `CustomFieldService.java`, `CustomFieldDAO.java`, `custom-field-list.jsp` | `CustomFieldServiceTest`, `CustomFieldServletTest` | **NEEDS APPROVAL** | CRUD định nghĩa trường tùy chỉnh đã hoàn tất; tuy nhiên việc tích hợp các trường này vào Biểu mẫu nhập liệu, Bộ lọc và Xuất Excel của Khách hàng/Cơ hội chưa được triển khai. |
| **S2-09** (CRM-47) | Giai đoạn bán hàng (Pipeline Stages): thứ tự, xác suất thắng 0-100%, bảo toàn cơ hội khi xóa stage | `PipelineServlet.java`, `PipelineService.java`, `PipelineDAO.java`, `pipeline-config.jsp` | `PipelineServiceTest` | **TESTED** | Đã có logic `reassignOpportunities` trong transaction khi xóa giai đoạn; chưa có kiểm thử E2E trên cơ sở dữ liệu có sẵn cơ hội. |
| **S2-10** (CRM-48) | Danh mục lý do Thắng/Thua (WIN/LOSS) và Đối thủ cạnh tranh phục vụ phân tích bán hàng | `WinLossServlet.java`, `WinLossService.java`, `WinLossDAO.java`, `winloss.jsp` | `WinLossServiceTest`, `WinLossServletTest` | **AC PASS (tạm thời)** | Quản lý danh mục Master Data đã hoàn tất và kiểm thử đầy đủ. Việc tích hợp bắt buộc nhập lý do khi đóng cơ hội thuộc phạm vi Sprint 5. |

---

## 4. Chú giải & Quy tắc nghiệm thu

1. Một User Story chỉ được chuyển sang trạng thái **`AC PASS`** khi có đầy đủ cả ba điều kiện:
   - Mã nguồn triển khai đầy đủ nghiệp vụ.
   - Có kiểm thử đơn vị hoặc kiểm thử tích hợp tự động bao phủ các trường hợp biên.
   - Đã được kiểm chứng thực tế trên môi trường máy chủ chạy độc lập (Tomcat 10.1 & MySQL 8.x).
2. Các mục ghi nhận **`NEEDS APPROVAL`** cần được người dùng phê duyệt phương án xử lý trước khi tiến hành viết mã hoặc đóng gói phát hành.
