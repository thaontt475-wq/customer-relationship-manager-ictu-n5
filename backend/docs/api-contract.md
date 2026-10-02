# CRM API CONTRACT — SPRINT 1 & SPRINT 2

> Phiên bản: 2.0 (TASK 03B — Codebase Route Verification & PR #82 Correction)
> Baseline commit: `b61bee548e07fe5c33041a7cbc2e392d7c58744e`
> Công nghệ: Jakarta Servlet 6.0 (Java 21) | Apache Tomcat 10.1 | MySQL 8.x | JSP SSR & JSON REST API

Tài liệu này chuẩn hóa API Contract chính thức cho toàn bộ 20 User Story thuộc Sprint 1 và Sprint 2 của dự án CRM.
Tất cả các endpoint trong tài liệu này đã được kiểm chứng trực tiếp đối chiếu 1:1 với mã nguồn Servlet (`@WebServlet`), bộ lọc (`Filter`), cấu hình `web.xml`, phương thức HTTP (`doGet`, `doPost`, `doPut`, `doDelete`) và `route-inventory.md`.

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
   - Kiến trúc chuẩn: `Filter -> Servlet -> Service -> DAO -> JDBC -> MySQL`.
   - **Giao diện HTML/JSP (Server-Side Rendering):** Dành cho hiển thị trên trình duyệt. Servlet xử lý `GET` forward tới JSP nội bộ (`/jsp/...`). `ViewAccessFilter` chặn toàn bộ truy cập HTTP trực tiếp tới thư mục `/jsp/*` (trả về HTTP 404).
   - **JSON REST API:** Dành cho các thao tác dữ liệu, AJAX hoặc tích hợp hệ thống. Endpoint có tiền tố `/api/` hoặc xử lý khi request có header `Accept: application/json`.
   - Cấu trúc phản hồi JSON chuẩn (`success: true`):
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
       "message": "Mô tả nguyên nhân lỗi cụ thể",
       "data": null
     }
     ```

2. **Quản lý phiên (Session) & Xác thực (Authentication):**
   - Quản lý phiên qua cookie `JSESSIONID` do Tomcat cấp phát (`HttpOnly: true`, timeout không hoạt động: 30 phút).
   - Thuộc tính phiên máy chủ (`HttpSession`): `userId` (Long), `roles` (List<String>), `displayName` (String), `currentUser` (Map/DTO chứa id người dùng). Tuyệt đối không lưu mật khẩu hoặc password hash vào session.
   - Endpoint công khai (Public): `/login`, `/api/auth/login`, `/forgot-password`, `/api/auth/forgot-password`, `/reset-password`, `/api/auth/reset-password`, `/errors/*`.
   - Khi phiên hết hạn hoặc chưa đăng nhập:
     - Yêu cầu trang HTML: Chuyển hướng 302 về `/login?expired=1`.
     - Yêu cầu API JSON: Trả về HTTP `401 Unauthorized` (`{"success": false, "message": "Yêu cầu đăng nhập"}`).

3. **Bảo vệ chống tấn công CSRF:**
   - `CsrfFilter` kiểm tra token đối với mọi HTTP request làm thay đổi dữ liệu (POST, PUT, DELETE) khi có phiên đăng nhập hoạt động.
   - Client lấy token hợp lệ từ `data.csrfToken` qua API `GET /api/auth/session` hoặc trường ẩn `<input type="hidden" name="csrfToken" value="...">` trong form JSP.
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

## 2. Bảng bằng chứng đối chiếu toàn bộ 20 User Story

Bảng đối chiếu dưới đây ghi nhận bằng chứng mã nguồn cho toàn bộ các endpoint được giữ lại trong API Contract, xác định rõ URL runtime, HTTP method, file mã nguồn và số dòng tương ứng.

| Story ID | Endpoint trong tài liệu | Mapping mã nguồn | HTTP method | File:Line | Kết quả | Điều chỉnh |
|---|---|---|---|---|---|---|
| **S1-01** | `/login` | `LoginServlet` | GET | `LoginServlet.java:22,32` | Khớp | Forward `/jsp/auth/login.jsp` |
| **S1-01** | `/login` | `LoginServlet` | POST | `LoginServlet.java:22,52,69` | Khớp | Xử lý form đăng nhập HTML, 302 về `/dashboard` |
| **S1-01** | `/api/auth/login` | `LoginServlet` | POST | `LoginServlet.java:22,52,143` | Khớp | API JSON, trả lời 200/400/401/403 |
| **S1-02** | `/api/auth/logout` | `LogoutServlet` | POST | `LogoutServlet.java:16,22` | Khớp | Hủy session, trả JSON 200 hoặc 302 nếu có param |
| **S1-02** | `/api/auth/session` | `SessionServlet` | GET | `SessionServlet.java:17,23` | Khớp | Trả 200 (kèm CSRF) khi đã login; trả 401 khi chưa login |
| **S1-03** | `/forgot-password` | `ForgotPasswordServlet` | GET, POST | `ForgotPasswordServlet.java:15,31,50` | Khớp | GET forward JSP; POST form submit redirect 302 |
| **S1-03** | `/api/auth/forgot-password` | `ForgotPasswordServlet` | POST | `ForgotPasswordServlet.java:15,50` | Khớp | Nhận email, trả JSON 200 thông báo chung |
| **S1-03** | `/reset-password` | `ResetPasswordServlet` | GET, POST | `ResetPasswordServlet.java:15,31,60` | Khớp | GET forward JSP; POST form đổi mật khẩu |
| **S1-03** | `/api/auth/reset-password` | `ResetPasswordServlet` | POST | `ResetPasswordServlet.java:15,60` | Khớp | JSON body token + newPassword, trả 200 hoặc 400 |
| **S1-04** | `/change-password` | `ChangePasswordServlet` | GET, POST | `ChangePasswordServlet.java:21,32,52` | Khớp | GET forward JSP; POST form submit |
| **S1-04** | `/api/auth/change-password` | `ChangePasswordServlet` | POST | `ChangePasswordServlet.java:21,52` | Khớp | JSON/form đổi mật khẩu, thu hồi session khác |
| **S1-05** | `/customers`, `/opportunities`, `/activities`, `/quotes` | `ScopedEntityPageServlet` | GET | `ScopedEntityPageServlet.java:12,17` | Khớp | Forward `/jsp/shared/scoped-records.jsp` |
| **S1-05** | `/api/{entity}` | `ScopedEntityServlet` | GET, POST | `ScopedEntityServlet.java:43-52,69,140` | Khớp | Lấy danh sách theo scope (hỗ trợ search); POST tạo bản ghi mới |
| **S1-05** | `/api/{entity}/{id}` | `ScopedEntityServlet` | GET, PUT, DELETE | `ScopedEntityServlet.java:43-52,110,190,258` | Khớp | CRUD chi tiết bản ghi theo phạm vi |
| **S1-05** | `/api/{entity}/export` (hoặc `?export=true`) | `ScopedEntityServlet` | GET | `ScopedEntityServlet.java:43-52,95` | Đã sửa | Loại bỏ route `/api/scope/records/export` không tồn tại |
| **S1-06** | `/api/navigation/menu`, `/api/menu` | `MenuServlet` | GET | `MenuServlet.java:25,40` | Khớp | Trả JSON danh mục menu theo vai trò |
| **S1-06** | `/navigation`, `/menu` | `NavigationServlet` | GET | `NavigationServlet.java:26,41` | Khớp | Điều hướng menu hoặc trả JSON nếu Accept: json |
| **S1-07** | `/errors/{code}` (401, 403, 404, 500) | `ErrorPageServlet` | ANY (service) | `ErrorPageServlet.java:16,24` | Khớp | Forward `/jsp/errors/{code}.jsp` hoặc trả JSON cho `/api/*` |
| **S1-08** | `/users` | `UserServlet` | GET | `UserServlet.java:30,47,76` | Khớp | Forward `/jsp/users/user-list.jsp` |
| **S1-08** | `/users/detail` | `UserServlet` | GET | `UserServlet.java:31,47,81` | Khớp | Forward `/jsp/users/user-detail.jsp` |
| **S1-08** | `/api/users` | `UserServlet` | GET, POST | `UserServlet.java:34,47,86,121,150` | Khớp | GET danh sách phân trang; POST tạo người dùng mới |
| **S1-08** | `/api/users/{id}` | `UserServlet` | GET, POST, PUT, DELETE | `UserServlet.java:34,94,156,194,228` | Khớp | Chi tiết, cập nhật (PUT/POST), xóa người dùng |
| **S1-09** | `/permissions` | `PermissionServlet` | GET | `PermissionServlet.java:22,35` | Khớp | Forward `/jsp/permissions/role-permission.jsp` |
| **S1-09** | `/permissions/assign`, `/permissions/team` | `PermissionServlet` | POST | `PermissionServlet.java:23-24,65` | Khớp | Form submit gán vai trò & nhóm, 302 redirect |
| **S1-09** | `/api/roles`, `/api/roles/all` | `UserRoleServlet` | GET | `UserRoleServlet.java:45,68,81` | Đã sửa | Lấy danh sách tất cả vai trò (thay `/api/permissions/roles`) |
| **S1-09** | `/api/roles/user/{id}` | `UserRoleServlet` | GET | `UserRoleServlet.java:45,100` | Đã sửa | Lấy vai trò user (thay `/api/permissions/user-roles`) |
| **S1-09** | `/api/roles/user/{id}/team` | `UserRoleServlet` | GET | `UserRoleServlet.java:45,88` | Đã thêm | Lấy nhóm kinh doanh của người dùng |
| **S1-09** | `/api/roles/assign` | `UserRoleServlet` | POST | `UserRoleServlet.java:45,129,149` | Đã sửa | Gán vai trò & nhóm kinh doanh (body: roleIds, teamId) |
| **S1-09** | `/api/roles/team`, `/api/roles/user/{id}/team` | `UserRoleServlet` | POST | `UserRoleServlet.java:45,155,161` | Đã sửa | Gán hoặc gỡ nhóm kinh doanh |
| **S1-09** | `/api/roles/user/{id}/add` | `UserRoleServlet` | PUT | `UserRoleServlet.java:45,178` | Đã thêm | Thêm một vai trò đơn lẻ cho người dùng |
| **S1-09** | `/api/roles/user/{id}/remove` | `UserRoleServlet` | DELETE | `UserRoleServlet.java:45,234` | Đã thêm | Gỡ bỏ một vai trò đơn lẻ của người dùng (body: roleId) |
| **S1-09** | `/api/permissions/users/{id}` | `PermissionApiServlet` | GET | `PermissionApiServlet.java:23,39` | Khớp | API phụ trợ đọc thông tin phân quyền người dùng |
| **S1-09** | `/api/permissions/assign` | `PermissionApiServlet` | POST | `PermissionApiServlet.java:24,100` | Khớp | API phụ trợ lưu phân quyền (userId, roleIds, dataScope) |
| **S1-09** | `/api/teams` | `TeamServlet` | GET | `TeamServlet.java:19,33` | Đã thêm | Lấy danh sách toàn bộ nhóm kinh doanh |
| **S1-10** | `/users/lock-handover` | `UserServlet` | POST | `UserServlet.java:32,121,127` | Đã sửa | Form action từ modal; sửa servlet mapping từ `UserLockServlet` |
| **S1-10** | `/users/unlock` | `UserServlet` | POST | `UserServlet.java:33,121,127` | Đã sửa | Form action mở khóa từ giao diện HTML |
| **S1-10** | `/api/users/{id}/lock` | `UserServlet` | POST | `UserServlet.java:34,160,175` | Đã sửa | Khóa không bắt buộc bàn giao (thay `/api/users/lock`) |
| **S1-10** | `/api/users/{id}/lock-handover` | `UserServlet` | POST | `UserServlet.java:34,160,176` | Đã sửa | Khóa bắt buộc bàn giao dữ liệu (thay `/api/users/lock`) |
| **S1-10** | `/api/users/{id}/unlock` | `UserServlet` | POST | `UserServlet.java:34,160,178` | Đã sửa | Mở khóa tài khoản (thay `/api/users/unlock`) |
| **S1-10** | `/api/users/{id}/transfer-data` | `UserServlet` | POST | `UserServlet.java:34,160,179` | Đã thêm | Bàn giao dữ liệu độc lập |
| **S1-10** | `/api/users/{id}/team` | `UserServlet` | POST | `UserServlet.java:34,160,181` | Đã thêm | Gán nhóm cho người dùng qua UserServlet |
| **S2-01** | `/users/import` | `UserImportServlet` | GET, POST | `UserImportServlet.java:40,67,138,159` | Khớp | GET forward JSP; POST tải lên trực tiếp |
| **S2-01** | `/api/users/import/template` | `UserImportServlet` | GET | `UserImportServlet.java:42,97` | Khớp | Tải tệp mẫu Excel .xlsx |
| **S2-01** | `/api/users/import/preview` | `UserImportServlet` | POST | `UserImportServlet.java:42,147` | Khớp | Upload file multipart, kiểm tra dữ liệu |
| **S2-01** | `/api/users/import/confirm` | `UserImportServlet` | POST | `UserImportServlet.java:42,153` | Khớp | Xác nhận nạp lô dữ liệu hợp lệ |
| **S2-02** | `/profile`, `/user/profile` | `ProfileServlet` | GET, POST | `ProfileServlet.java:33-34,57,106` | Khớp | GET forward JSP; POST form update profile |
| **S2-02** | `/api/profile`, `/api/users/me` | `ProfileServlet` | GET, POST, PUT | `ProfileServlet.java:35,38,57,106,112` | Khớp | Lấy và cập nhật hồ sơ cá nhân qua API JSON |
| **S2-03** | `/profile/avatar` | `AvatarServlet` | GET, POST | `AvatarServlet.java:18,34,63` | Khớp | GET forward JSP; POST form multipart field `avatar` |
| **S2-03** | `/profile/avatar/image`, `/thumbnail` | `AvatarServlet` | GET | `AvatarServlet.java:18,38` | Khớp | Xuất byte nhị phân PNG 512x512 và 128x128 |
| **S2-03** | `/api/users/me/avatar` | `AvatarServlet` | GET, POST | `AvatarServlet.java:18,49,63` | Khớp | GET metadata ảnh; POST upload ảnh qua API |
| **S2-04** | `/audit` | `AuditPageServlet` | GET | `AuditPageServlet.java:27,36` | Khớp | Forward `/jsp/audit/audit-log.jsp` |
| **S2-04** | `/api/audit-logs` | `AuditLogServlet` | GET | `AuditLogServlet.java:35,55` | Đã sửa | Đổi từ `/api/audit/logs` sang `/api/audit-logs`; POST/PUT/DELETE trả 405 |
| **S2-05** | `/products` | `ProductServlet` | GET | `ProductServlet.java:38,65` | Đã sửa | Redirect 302 về `/products/page` |
| **S2-05** | `/products/page` | `ProductPageServlet` | GET, POST | `ProductPageServlet.java:18,25,58` | Đã sửa | GET hiển thị danh sách; POST form lưu sản phẩm |
| **S2-05** | `/api/products` | `ProductServlet` | GET, POST | `ProductServlet.java:40,59,126` | Khớp | GET danh sách sản phẩm; POST tạo mới |
| **S2-05** | `/api/products/{id}` | `ProductServlet` | GET, PUT, DELETE | `ProductServlet.java:41,80,168,210` | Đã sửa | Chi tiết, sửa, xóa sản phẩm (thay `/api/products/detail`) |
| **S2-06** | `/organization`, `/organization/page` | `OrganizationPageServlet` | GET | `OrganizationPageServlet.java:20,27` | Khớp | Forward `/jsp/organization/organization.jsp` |
| **S2-06** | `/api/organization/units` | `OrganizationServlet` | GET, POST | `OrganizationServlet.java:28,50,75` | Đã sửa | GET cây tổ chức; POST tạo đơn vị (thay `/tree`, `/teams`) |
| **S2-06** | `/api/organization/units/{id}` | `OrganizationServlet` | PUT | `OrganizationServlet.java:29,115` | Đã sửa | Cập nhật đơn vị tổ chức |
| **S2-07** | `/configuration`, `/configuration/page` | `CategoryPageServlet` | GET, POST | `CategoryPageServlet.java:20,27,62` | Khớp | Forward `/jsp/configuration/configuration.jsp` |
| **S2-07** | `/api/categories`, `/api/master-data` | `CategoryServlet` | GET, POST | `CategoryServlet.java:43,45,64,141` | Khớp | GET danh sách danh mục; POST tạo danh mục mới |
| **S2-07** | `/api/categories/{type}` | `CategoryServlet` | GET | `CategoryServlet.java:44,108` | Đã thêm | Lấy danh mục theo mã loại (string) |
| **S2-07** | `/api/categories/{id}` | `CategoryServlet` | GET, PUT, DELETE | `CategoryServlet.java:44,95,182,230` | Khớp | Chi tiết, cập nhật, xóa danh mục |
| **S2-07** | `/api/categories/{id}/display-order` | `CategoryServlet` | PUT | `CategoryServlet.java:44,196` | Đã thêm | Cập nhật thứ tự hiển thị |
| **S2-08** | `/customfields`, `/customfields/page` | `CustomFieldPageServlet` | GET, POST | `CustomFieldPageServlet.java:21,28,49` | Khớp | Forward `/jsp/customfields/custom-field-list.jsp` |
| **S2-08** | `/api/custom-fields` | `CustomFieldServlet` | GET, POST | `CustomFieldServlet.java:36,51,83` | Đã sửa | GET danh sách định nghĩa; POST tạo định nghĩa (có dấu gạch ngang) |
| **S2-08** | `/api/custom-fields/{id}` | `CustomFieldServlet` | GET, PUT, DELETE | `CustomFieldServlet.java:36,67,98,130` | Đã sửa | Chi tiết, sửa, xóa định nghĩa trường |
| **S2-08** | `/api/custom-fields/values` | `CustomFieldServlet` | GET, PUT | `CustomFieldServlet.java:36,59,101` | Đã sửa | GET đọc giá trị; PUT lưu giá trị (sửa từ POST sang PUT) |
| **S2-09** | `/pipeline`, `/pipeline/page` | `PipelinePageServlet` | GET, POST | `PipelinePageServlet.java:19,26,60` | Khớp | Forward `/jsp/pipeline/pipeline-config.jsp` |
| **S2-09** | `/api/pipeline/stages`, `/api/stages` | `PipelineServlet` | GET, POST | `PipelineServlet.java:40,42,61,108` | Khớp | GET danh sách stage; POST tạo stage mới |
| **S2-09** | `/api/pipeline/stages/{id}`, `/api/stages/{id}` | `PipelineServlet` | GET, PUT, DELETE | `PipelineServlet.java:41,43,75,156,204` | Khớp | Chi tiết, sửa, xóa stage (kèm migrate cơ hội) |
| **S2-09** | `/api/pipeline/stages/reorder`, `/api/stages/reorder` | `PipelineServlet` | PUT | `PipelineServlet.java:41,156,169` | Đã sửa | Sắp xếp lại thứ tự stage (sửa từ POST sang PUT) |
| **S2-09** | `/api/pipeline/stages/validate-transition`, `/api/stages/validate-transition` | `PipelineServlet` | POST | `PipelineServlet.java:41,108,131,267` | Đã thêm | Kiểm tra điều kiện chuyển đổi giai đoạn cơ hội |
| **S2-10** | `/winloss` | `WinLossPageServlet` | GET, POST | `WinLossPageServlet.java:19,26,47` | Khớp | Forward `/jsp/winloss/winloss.jsp` |
| **S2-10** | `/api/winloss/reasons` | `WinLossServlet` | GET, POST, PUT | `WinLossServlet.java:35,50,72,91` | Khớp | Lấy, tạo, sửa danh mục lý do thắng/thua (body chứa id khi PUT) |
| **S2-10** | `/api/winloss/reasons?id=...` | `WinLossServlet` | DELETE | `WinLossServlet.java:35,110` | Đã sửa | Xóa lý do thắng/thua (DELETE dùng param `?id=`, không dùng path /{id}) |
| **S2-10** | `/api/winloss/competitors` | `WinLossServlet` | GET, POST, PUT | `WinLossServlet.java:35,50,72,91` | Khớp | Lấy, tạo, sửa đối thủ cạnh tranh (body chứa id khi PUT) |
| **S2-10** | `/api/winloss/competitors?id=...` | `WinLossServlet` | DELETE | `WinLossServlet.java:35,110` | Đã sửa | Xóa đối thủ cạnh tranh (DELETE dùng param `?id=`, không dùng path /{id}) |

*Ghi chú giới hạn xác nhận:* Toàn bộ mapping URL, Servlet và HTTP Method nêu trên đã được xác minh đối chiếu trực tiếp với mã nguồn Servlet (`@WebServlet` và override handlers). Các payload request/response JSON cụ thể được xác nhận dựa trên luồng đọc `request.getReader()` và serialize Gson trong mã nguồn; các bài test kiểm thử HTTP runtime cụ thể được liệt kê trong Bảng Ma trận nghiệm thu bên dưới.

---

## 3. Chi tiết API Contract cho 20 User Story

### S1-01 (CRM-21) — Đăng nhập & Khóa bảo vệ tạm thời
- **Mục đích:** Xác thực người dùng, kiểm tra trạng thái hoạt động, hỗ trợ đa vai trò, khóa tạm thời 15 phút nếu nhập sai quá 5 lần liên tiếp.
- **URL hiển thị JSP:** `GET /login` (forward `/jsp/auth/login.jsp`).
- **API Endpoints:**
  - `POST /login`: Submit form đăng nhập JSP. Form params: `email` (string), `password` (string). Chuyển hướng 302 về `/dashboard` nếu thành công; forward lại `/jsp/auth/login.jsp` kèm thuộc tính `error` nếu thất bại.
  - `POST /api/auth/login`: API JSON. Content-Type: `application/json` hoặc `application/x-www-form-urlencoded`. Body: `{"email": "...", "password": "..."}`.
- **Response Format:**
  - 200 OK: `{"success": true, "message": "Đăng nhập thành công", "data": {"userId": 1, "displayName": "...", "roles": ["Admin"]}}`
  - 400 Bad Request: `{"success": false, "message": "Vui lòng nhập email và mật khẩu", "data": null}`
  - 401 Unauthorized: `{"success": false, "message": "Email hoặc mật khẩu không đúng", "data": null}`
  - 403 Forbidden: `{"success": false, "message": "Tài khoản bị tạm khóa do nhập sai quá 5 lần liên tiếp. Vui lòng thử lại sau 15 phút.", "data": null}`
- **Quyền truy cập:** Public (chưa đăng nhập).

### S1-02 (CRM-22) — Đăng xuất & Quản lý phiên
- **Mục đích:** Đăng xuất người dùng, vô hiệu hóa session phía máy chủ, thu hồi CSRF token, dọn dẹp phiên khỏi `SessionRegistry`.
- **API Endpoints:**
  - `POST /api/auth/logout`: Đăng xuất phiên hiện tại.
    - Param tùy chọn: `redirectToLogin=true` (chuyển hướng 302 về `/login`).
    - Response 200 JSON: `{"success": true, "message": "Đăng xuất thành công", "data": null}`
  - `GET /api/auth/session`: Lấy thông tin phiên hiện tại và CSRF token.
    - Response 200 (đã đăng nhập): `{"success": true, "message": "Lấy thông tin phiên làm việc thành công", "data": {"currentUser": {...}, "roles": [...], "expiresAt": "...", "csrfToken": "..."}}`
    - Response 401 (chưa đăng nhập hoặc session null): `{"success": false, "message": "Chưa đăng nhập", "data": null}`
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
    - Response 400: `{"success": false, "message": "Liên kết đặt lại mật khẩu không hợp lệ, đã hết hạn hoặc đã được sử dụng."}`
- **Quyền truy cập:** Public.

### S1-04 (CRM-24) — Đổi mật khẩu & Thu hồi phiên khác
- **Mục đích:** Người dùng đã đăng nhập tự đổi mật khẩu; kiểm tra mật khẩu hiện tại, áp dụng chính sách mật khẩu, tự động thu hồi tất cả các phiên đăng nhập khác của người dùng qua `SessionRegistry`.
- **URL hiển thị JSP:** `GET /change-password` (forward `/jsp/auth/change-password.jsp`).
- **API Endpoints:**
  - `POST /change-password`, `POST /api/auth/change-password`: Gửi yêu cầu đổi mật khẩu.
    - Param/JSON: `currentPassword`, `newPassword`, `confirmPassword`.
    - Response 200: `{"success": true, "message": "Đổi mật khẩu thành công. Các phiên đăng nhập khác đã được thu hồi."}`
    - Response 400: `{"success": false, "message": "Mật khẩu hiện tại không đúng"}` hoặc `{"success": false, "message": "Mật khẩu mới phải có ít nhất 8 ký tự, gồm ít nhất một chữ và một số."}`
- **Quyền truy cập:** Đã đăng nhập. Bắt buộc kiểm tra CSRF token.

### S1-05 (CRM-25) — Phân quyền phạm vi dữ liệu (SELF / TEAM / ALL)
- **Mục đích:** Kiểm soát quyền xem danh sách, xem chi tiết và xuất Excel cho 4 thực thể: `customers`, `opportunities`, `activities`, `quotes` theo phạm vi dữ liệu của người dùng.
- **URL hiển thị JSP:** `GET /customers`, `GET /opportunities`, `GET /activities`, `GET /quotes` (forward `/jsp/shared/scoped-records.jsp`).
- **API Endpoints:**
  - `GET /api/{customers|opportunities|activities|quotes}`: Lấy danh sách bản ghi theo phạm vi. Hỗ trợ query params: `search` (hoặc `q`, `keyword`), `page`.
  - `GET /api/{customers|opportunities|activities|quotes}/{id}`: Xem chi tiết bản ghi theo id.
  - `GET /api/{customers|opportunities|activities|quotes}/export` (hoặc `?export=true`): Xuất dữ liệu ra file Excel (.xlsx).
- **Response Format:**
  - Response 200 JSON (List): `{"success": true, "message": "Lấy danh sách thành công", "data": {"items": [{"id": 1, "label": "...", "ownerUserId": 2, "ownerTeamId": 3}]}}`
  - Response 200 JSON (Detail): `{"success": true, "message": "Lấy dữ liệu thành công", "data": {"id": 1, "label": "...", "ownerUserId": 2, "ownerTeamId": 3}}`
  - Response 200 XLSX: Binary stream `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
  - Response 403: `{"success": false, "message": "Bạn không có quyền truy cập bản ghi này do phạm vi dữ liệu."}`
- **Quyền truy cập:** Đã đăng nhập.

### S1-06 (CRM-26) — Menu điều hướng động & Thông tin người dùng
- **Mục đích:** Hiển thị menu chức năng động theo vai trò người dùng, hiển thị thông tin tài khoản hiện hành.
- **API Endpoints:**
  - `GET /api/navigation/menu`, `GET /api/menu`: Trả về danh sách mục menu được phép truy cập theo vai trò.
    - Response 200: `{"success": true, "message": "Lấy danh sách menu thành công", "data": {"menuItems": [...], "userInfo": {...}}}`
  - `GET /navigation`, `GET /menu`: Điều hướng giao diện hoặc trả JSON nếu client yêu cầu `Accept: application/json`.
- **Quyền truy cập:** Đã đăng nhập.

### S1-07 (CRM-27) — Các trang lỗi hệ thống
- **Mục đích:** Xử lý và hiển thị thông báo lỗi thân thiện, trả về đúng mã trạng thái HTTP tiêu chuẩn (401, 403, 404, 500).
- **URL hiển thị JSP:**
  - `GET /errors/401`: Forward `/jsp/errors/401.jsp` (HTTP 401).
  - `GET /errors/403`: Forward `/jsp/errors/403.jsp` (HTTP 403).
  - `GET /errors/404`: Forward `/jsp/errors/404.jsp` (HTTP 404).
  - `GET /errors/500`: Forward `/jsp/errors/500.jsp` (HTTP 500).
- **Xử lý API:** Với request có tiền tố `/api/`, `ErrorPageServlet` tự động trả về phản hồi JSON với mã lỗi tương ứng.
- **Quyền truy cập:** Public.

### S1-08 (CRM-28) — Quản lý tài khoản người dùng
- **Mục đích:** Quản trị viên xem danh sách, chi tiết, tạo mới, chỉnh sửa và xóa tài khoản người dùng.
- **URL hiển thị JSP:**
  - `GET /users`: Forward `/jsp/users/user-list.jsp`.
  - `GET /users/detail?id=...`: Forward `/jsp/users/user-detail.jsp`.
- **API Endpoints:**
  - `GET /api/users`: Lấy danh sách người dùng phân trang và lọc (`search`, `role`, `status`, `page`, `size`).
  - `GET /api/users/{id}`: Xem chi tiết tài khoản người dùng theo ID trong đường dẫn.
  - `POST /api/users`: Tạo mới tài khoản người dùng (yêu cầu Admin).
  - `PUT /api/users/{id}` (hoặc `POST /api/users/{id}`): Cập nhật thông tin tài khoản.
  - `DELETE /api/users/{id}`: Xóa tài khoản người dùng (chặn xóa chính mình hoặc tài khoản có liên kết).
- **Quyền truy cập:** Vai trò `Admin` hoặc `Director`.

### S1-09 (CRM-29) — Phân vai trò & Quản lý nhóm
- **Mục đích:** Gán vai trò cho người dùng, hỗ trợ đa vai trò, gắn người dùng vào nhóm kinh doanh, quy tắc bắt buộc gắn nhóm đối với vai trò Trưởng nhóm (Team Lead), chống tự thu hồi quyền Admin của chính mình.
- **URL hiển thị JSP:** `GET /permissions`: Forward `/jsp/permissions/role-permission.jsp`.
- **Form Actions JSP:**
  - `POST /permissions/assign`: Gán vai trò qua biểu mẫu JSP.
  - `POST /permissions/team`: Gán nhóm kinh doanh qua biểu mẫu JSP.
- **API Endpoints:**
  - `GET /api/roles`, `GET /api/roles/all`: Lấy danh sách tất cả vai trò hệ thống.
  - `GET /api/roles/user/{id}`: Lấy danh sách vai trò hiện tại của người dùng.
  - `GET /api/roles/user/{id}/team`: Lấy thông tin nhóm kinh doanh của người dùng.
  - `POST /api/roles/assign`: Cập nhật đồng thời danh sách vai trò và nhóm kinh doanh. Body: `{"userId": 5, "roleIds": [2, 3], "teamId": 2}`.
  - `POST /api/roles/team`: Gán hoặc gỡ nhóm kinh doanh. Body: `{"userId": 5, "teamId": 2}` (hoặc `teamId: null`).
  - `POST /api/roles/user/{id}/team`: Gán hoặc gỡ nhóm kinh doanh qua URL path. Body: `{"teamId": 2}`.
  - `PUT /api/roles/user/{id}/add`: Thêm một vai trò đơn lẻ cho người dùng. Body: `{"roleId": 2}`.
  - `DELETE /api/roles/user/{id}/remove`: Gỡ bỏ một vai trò đơn lẻ của người dùng. Body: `{"roleId": 2}`.
  - `GET /api/permissions/users/{id}`: Đọc thông tin quyền người dùng qua PermissionApiServlet.
  - `POST /api/permissions/assign`: Gán vai trò qua PermissionApiServlet.
- **Quyền truy cập:** Vai trò `Admin` hoặc `Director`.

### S1-10 (CRM-30) — Khóa tài khoản & Bàn giao dữ liệu
- **Mục đích:** Khóa tài khoản nhân viên, bắt buộc bàn giao toàn bộ dữ liệu đang phụ trách (khách hàng, cơ hội) sang nhân sự khác trong một giao dịch JDBC duy nhất, ghi nhật ký bàn giao, thu hồi phiên làm việc ngay lập tức.
- **Form Actions JSP:**
  - `POST /users/lock-handover`: Thực hiện khóa và bàn giao dữ liệu từ giao diện modal HTML. Params: `userId` (Long), `recipientId` (Long, bắt buộc nếu có dữ liệu), `reason`/`lockReason` (String), `confirm` ("true"/"1").
  - `POST /users/unlock`: Mở khóa tài khoản từ giao diện HTML. Param: `userId` (Long).
- **API Endpoints:**
  - `POST /api/users/{id}/lock-handover`: Khóa tài khoản kèm bàn giao dữ liệu bắt buộc qua API. Params (query/form): `recipientId` (Long), `reason` hoặc `lockReason` (String), `confirm` ("true"/"1").
  - `POST /api/users/{id}/lock`: Khóa tài khoản không bắt buộc bàn giao (nếu người dùng không sở hữu dữ liệu). Params: `recipientId` (tùy chọn), `reason`/`lockReason`.
  - `POST /api/users/{id}/unlock`: Mở khóa tài khoản người dùng qua API.
  - `POST /api/users/{id}/transfer-data`: Thực hiện chuyển nhượng dữ liệu độc lập. Params: `recipientId` (Long).
- **Quyền truy cập:** Vai trò `Admin`.

### S2-01 (CRM-32) — Nhập người dùng từ Excel hàng loạt
- **Mục đích:** Tải tệp biểu mẫu (.xlsx), tải lên tệp danh sách người dùng, kiểm tra định dạng và dữ liệu, bỏ qua các dòng lỗi để nạp các dòng hợp lệ theo lô vào cơ sở dữ liệu, xuất báo cáo kết quả chi tiết.
- **URL hiển thị JSP:** `GET /users/import`: Forward `/jsp/users/user-import.jsp`.
- **API Endpoints:**
  - `GET /api/users/import/template`: Tải tệp biểu mẫu Excel (.xlsx).
  - `POST /api/users/import/preview`: Tải lên tệp để kiểm tra và xem trước kết quả. Multipart form data (trường `file`).
  - `POST /api/users/import/confirm`: Xác nhận nhập khẩu lô đã kiểm tra vào cơ sở dữ liệu.
  - `POST /api/users/import`: Tải lên và nạp dữ liệu trực tiếp trong một bước.
- **Response Format:**
  - Response 200 JSON: `{"success": true, "message": "Xác thực tệp hoàn tất", "data": {"totalRows": 50, "successCount": 45, "errorCount": 5, "errors": [{"rowNumber": 4, "field": "email", "errorMessage": "Email đã tồn tại:..."}]}}`
- **Quyền truy cập:** Vai trò `Admin` hoặc `Director`.

### S2-02 (CRM-35) — Quản lý hồ sơ cá nhân
- **Mục đích:** Người dùng tự xem thông tin hồ sơ, cập nhật họ tên và số điện thoại; hệ thống nghiêm cấm tự ý chỉnh sửa email, vai trò hoặc trạng thái tài khoản.
- **URL hiển thị JSP:** `GET /profile`, `GET /user/profile`: Forward `/jsp/users/profile.jsp`.
- **API Endpoints:**
  - `GET /api/profile`, `GET /api/users/me`: Lấy thông tin hồ sơ của chính mình.
  - `POST /profile`: Form submit từ JSP cập nhật họ tên và số điện thoại.
  - `POST /api/profile`, `PUT /api/profile`, `PUT /api/users/me`: Cập nhật thông tin hồ sơ cá nhân qua API JSON. Form/JSON: `fullName`, `phone`, `signature`. (Email và các trường phân quyền bị bỏ qua/giữ nguyên giá trị cũ).
- **Quyền truy cập:** Đã đăng nhập.

### S2-03 (CRM-36) — Tải lên ảnh đại diện (Avatar)
- **Mục đích:** Tải lên ảnh đại diện (JPG/PNG, tối đa 2MB), xử lý cắt vuông tâm tự động và tạo ảnh thu nhỏ; hiển thị ảnh trên header và trang cá nhân.
- **Thông số kỹ thuật xử lý ảnh (AvatarImageProcessor):**
  - Tệp tối đa: 2.097.152 byte (2 MiB); tối đa 16.000.000 pixels.
  - Kích thước ảnh chính (square): 512x512 pixel.
  - Kích thước ảnh thu nhỏ (thumbnail): 128x128 pixel.
- **URL hiển thị JSP:** `GET /profile/avatar`: Forward `/jsp/users/avatar.jsp`.
- **API Endpoints:**
  - `GET /profile/avatar/image`: Trả về dữ liệu nhị phân ảnh đại diện chính (512x512 PNG).
  - `GET /profile/avatar/thumbnail`: Trả về dữ liệu nhị phân ảnh thu nhỏ (128x128 PNG).
  - `POST /profile/avatar`: Form POST tải lên tệp ảnh đại diện (Multipart, trường `avatar`).
  - `GET /api/users/me/avatar`: Lấy siêu dữ liệu ảnh đại diện và CSRF token qua JSON.
  - `POST /api/users/me/avatar`: Upload ảnh đại diện qua API (Multipart, trường `avatar`).
- **Quyền truy cập:** Đã đăng nhập.

### S2-04 (CRM-37) — Nhật ký kiểm toán hệ thống (Audit Logs)
- **Mục đích:** Ghi nhận lịch sử các biến động dữ liệu nhạy cảm kèm giá trị trước/sau dạng JSON trong cùng một giao dịch JDBC; cung cấp giao diện tra cứu và bộ lọc theo người thực hiện, loại hành động và khoảng thời gian.
- **URL hiển thị JSP:** `GET /audit`: Forward `/jsp/audit/audit-log.jsp`.
- **API Endpoints:**
  - `GET /api/audit-logs`: Tra cứu danh sách nhật ký kiểm toán.
    - Query params: `userId` (Long), `objectId` (Long), `objectType` (String), `from` (ISO OffsetDateTime), `to` (ISO OffsetDateTime), `limit` (int, default 100, max 500), `offset` (int, default 0).
    - Response 200 JSON: `{"success": true, "message": "Lấy nhật ký thay đổi thành công", "data": [{"id": 1, "userId": 2, "action": "ROLE_CHANGED", "objectType": "USER", "objectId": 5, "beforeValue": {...}, "afterValue": {...}, "createdAt": "..."}]}`
  - Các phương thức `POST`, `PUT`, `DELETE` trên `/api/audit-logs` đều trả về HTTP `405 Method Not Allowed`.
- **Quyền truy cập:** Đã đăng nhập (kiểm tra phân quyền màn hình qua MenuService).

### S2-05 (CRM-39) — Danh mục sản phẩm & Bảng giá
- **Mục đích:** Quản lý danh mục sản phẩm và bảng giá bán hàng; phân quyền bảo mật giá vốn; kiểm tra ràng buộc giá niêm yết >= giá sàn; cơ chế chống xóa an toàn khi sản phẩm đã được tham chiếu trong báo giá, cơ hội, đơn hàng.
- **URL hiển thị JSP:**
  - `GET /products`: Redirect 302 về `/products/page`.
  - `GET /products/page`: Forward `/jsp/products/product-list.jsp`.
  - `POST /products/page`: Xử lý form submit tạo mới hoặc sửa sản phẩm từ giao diện HTML.
- **API Endpoints:**
  - `GET /api/products`: Danh sách sản phẩm phân trang. Params: `q` (hoặc `keyword`), `category`, `active`, `page`, `size`. Giá vốn (`costPrice`) tự động ẩn (mask thành null) đối với người dùng không có quyền quản lý.
  - `GET /api/products/{id}`: Xem chi tiết sản phẩm theo ID trong đường dẫn.
  - `POST /api/products`: Tạo mới sản phẩm (yêu cầu quyền Admin hoặc Director để thiết lập giá vốn).
  - `PUT /api/products/{id}`: Cập nhật thông tin sản phẩm.
  - `DELETE /api/products/{id}`: Xóa sản phẩm (trả về 409 Conflict nếu đã được tham chiếu trong dữ liệu giao dịch).
- **Quyền truy cập:** Xem: Mọi nhân viên đã đăng nhập; Thao tác ghi & Giá vốn: Admin hoặc Director.

### S2-06 (CRM-42) — Cơ cấu tổ chức & Cây nhóm kinh doanh
- **Mục đích:** Thiết lập sơ đồ tổ chức phòng ban/nhóm kinh doanh theo mô hình phân cấp cây (cha - con), phân bổ trưởng nhóm và chỉ định khu vực/chi nhánh phụ trách.
- **URL hiển thị JSP:** `GET /organization`, `GET /organization/page`: Forward `/jsp/organization/organization.jsp`.
- **API Endpoints:**
  - `GET /api/organization/units`: Lấy toàn bộ cây đơn vị tổ chức.
    - Response 200: `{"success": true, "message": "Lấy cây cơ cấu tổ chức thành công", "data": {"items": [{"id": 1, "name": "...", "parentId": null, "managerId": 2, "region": "...", "active": true, ...}]}}`
  - `POST /api/organization/units`: Tạo mới đơn vị tổ chức. Body JSON: `{"name": "...", "parentId": 1, "managerId": 2, "region": "...", "active": true}`. Trả về HTTP 201 Created.
  - `PUT /api/organization/units/{id}`: Cập nhật thông tin đơn vị tổ chức theo ID trong đường dẫn. Body JSON: `{"name": "...", "parentId": 1, "managerId": 2, "region": "...", "active": true}`. Trả về HTTP 200 OK.
  - *Lưu ý:* Thao tác `DELETE` hiện chưa được hỗ trợ tại tầng Servlet này (trả về 405 Method Not Allowed) và được ghi nhận trong bảng GAP.
- **Quyền truy cập:** Xem: Đã đăng nhập; Thao tác ghi: Admin hoặc Director.

### S2-07 (CRM-44) — Danh mục dùng chung (Master Data)
- **Mục đích:** Quản lý các danh mục cấu hình dùng chung trong toàn hệ thống (loại khách hàng, nguồn dữ liệu, v.v.), hỗ trợ sắp xếp thứ tự hiển thị và chặn xóa khi đang có dữ liệu liên kết.
- **URL hiển thị JSP:** `GET /configuration`, `GET /configuration/page`: Forward `/jsp/configuration/configuration.jsp`.
- **API Endpoints:**
  - `GET /api/categories`, `GET /api/master-data`: Lấy danh sách danh mục. Params: `type` (hoặc `category_type`), `active`, `q`.
  - `GET /api/categories/{type}`: Lấy danh mục theo mã loại (chuỗi ký tự).
  - `GET /api/categories/{id}`: Lấy chi tiết một mục danh mục theo ID số.
  - `POST /api/categories`: Tạo mới mục danh mục. Body: `{"type": "...", "code": "...", "name": "...", "displayOrder": 1}`. Trả về HTTP 201 Created.
  - `PUT /api/categories/{id}`: Cập nhật mục danh mục.
  - `PUT /api/categories/{id}/display-order`: Cập nhật lại thứ tự hiển thị của mục danh mục.
  - `DELETE /api/categories/{id}`: Xóa mục danh mục (trả về 409 Conflict nếu có dữ liệu liên kết).
- **Quyền truy cập:** Xem: Đã đăng nhập; Quản trị: Admin hoặc Director.

### S2-08 (CRM-46) — Quản lý trường tùy chỉnh (Custom Fields)
- **Mục đích:** Cho phép định nghĩa các trường dữ liệu động với 4 kiểu dữ liệu: `TEXT`, `NUMBER`, `DATE`, `SELECT` áp dụng cho hai thực thể bắt buộc theo AC gốc: Khách hàng (`CUSTOMER`) và Cơ hội (`OPPORTUNITY`).
- **URL hiển thị JSP:** `GET /customfields`, `GET /customfields/page`: Forward `/jsp/customfields/custom-field-list.jsp`.
- **API Endpoints:**
  - `GET /api/custom-fields`: Lấy danh sách định nghĩa trường. Params: `entity` (hoặc `entityType`).
  - `GET /api/custom-fields/{id}`: Lấy thông tin định nghĩa trường theo ID số.
  - `POST /api/custom-fields`: Tạo định nghĩa trường tùy chỉnh mới. Body: `{"entityType": "CUSTOMER", "fieldName": "...", "fieldLabel": "...", "fieldType": "TEXT", "required": false, "options": [...]}`. Trả về HTTP 201 Created.
  - `PUT /api/custom-fields/{id}`: Cập nhật định nghĩa trường tùy chỉnh.
  - `DELETE /api/custom-fields/{id}`: Xóa hoặc vô hiệu hóa trường tùy chỉnh.
  - `GET /api/custom-fields/values`: Lấy giá trị các trường tùy chỉnh của một bản ghi. Params: `entity` (hoặc `entityType`), `recordId`.
  - `PUT /api/custom-fields/values`: Lưu giá trị các trường tùy chỉnh cho một bản ghi. Body JSON: `{"entityType": "CUSTOMER", "recordId": 12, "values": {"field_name": "value"}}`. Trả về HTTP 200 OK.
- **Quyền truy cập:** Định nghĩa: Admin hoặc Director; Đọc/Ghi giá trị: Theo quyền bản ghi thực thể.

### S2-09 (CRM-47) — Quy trình bán hàng (Pipeline Stages)
- **Mục đích:** Thiết lập các giai đoạn trong chu trình bán hàng (tên giai đoạn, thứ tự sắp xếp, xác suất thành công từ 0% đến 100%, điều kiện chuyển giai đoạn); bảo toàn các cơ hội đang hoạt động khi thay đổi hoặc xóa giai đoạn.
- **URL hiển thị JSP:** `GET /pipeline`, `GET /pipeline/page`: Forward `/jsp/pipeline/pipeline-config.jsp`.
- **API Endpoints:**
  - `GET /api/pipeline/stages`, `GET /api/stages`: Lấy danh sách các giai đoạn bán hàng sắp xếp theo thứ tự `stage_order ASC`. Params: `pipelineId`, `active`.
  - `GET /api/pipeline/stages/{id}`: Xem chi tiết giai đoạn theo ID.
  - `POST /api/pipeline/stages`: Tạo giai đoạn bán hàng mới. Body: `{"pipelineId": 1, "code": "QUAL", "name": "Đánh giá", "winProbability": 50, "stageOrder": 2, "requirements": "..."}`. Trả về HTTP 201 Created.
  - `PUT /api/pipeline/stages/{id}`: Cập nhật thông tin giai đoạn. Body tương tự POST.
  - `PUT /api/pipeline/stages/reorder`: Cập nhật lại thứ tự sắp xếp các giai đoạn qua phương thức HTTP PUT. Body: `{"pipelineId": 1, "stageIds": [1, 2, 3]}`.
  - `POST /api/pipeline/stages/validate-transition`: Kiểm tra điều kiện hợp lệ khi chuyển đổi giai đoạn của cơ hội bán hàng. Body: `{"currentStageId": 1, "targetStageId": 2, "amount": 50000000, "contactName": "Nguyen Van A", "lostReason": null, "requirementsConfirmed": true}`.
  - `DELETE /api/pipeline/stages/{id}`: Xóa giai đoạn. Query param: `targetStageIdForMigration` (hoặc `targetStageId`) để di chuyển các cơ hội sang giai đoạn mới trong một giao dịch. Trả về 409 Conflict nếu có cơ hội mà không chỉ định giai đoạn chuyển đổi.
- **Quyền truy cập:** Xem: Đã đăng nhập; Quản trị: Admin hoặc Director.

### S2-10 (CRM-48) — Quản lý lý do Thắng/Thua & Đối thủ cạnh tranh
- **Mục đích:** Quản lý danh mục lý do thành công (WIN), thất bại (LOSS) và danh sách đối thủ cạnh tranh nhằm phục vụ phân tích bán hàng.
- **URL hiển thị JSP:** `GET /winloss`: Forward `/jsp/winloss/winloss.jsp`.
- **API Endpoints:**
  - `GET /api/winloss/reasons`: Lấy danh sách lý do Thắng/Thua. Param: `includeInactive=true|false`.
  - `POST /api/winloss/reasons`: Tạo lý do mới. Body JSON: `{"reasonText": "Giá cao hơn đối thủ", "type": "LOSS", "description": "...", "displayOrder": 1, "active": true}`. Trả về HTTP 201 Created.
  - `PUT /api/winloss/reasons`: Cập nhật lý do. Body JSON chứa `id`, `reasonText`, `type`, `description`, `displayOrder`, `active`. Trả về HTTP 200 OK.
  - `DELETE /api/winloss/reasons?id=...`: Xóa hoặc vô hiệu hóa lý do theo param `id`. Trả về JSON chứa `outcome: "DELETED"` hoặc `"DEACTIVATED"`.
  - `GET /api/winloss/competitors`: Lấy danh sách đối thủ cạnh tranh. Param: `includeInactive=true|false`.
  - `POST /api/winloss/competitors`: Tạo đối thủ mới. Body JSON: `{"name": "Đối thủ A", "strengths": "...", "weaknesses": "...", "website": "...", "displayOrder": 1, "active": true}`. Trả về HTTP 201 Created.
  - `PUT /api/winloss/competitors`: Cập nhật đối thủ. Body JSON chứa `id`, `name`, `strengths`, `weaknesses`, `website`, `displayOrder`, `active`. Trả về HTTP 200 OK.
  - `DELETE /api/winloss/competitors?id=...`: Xóa hoặc vô hiệu hóa đối thủ theo param `id`.
- **Quyền truy cập:** Xem: Toàn bộ nhân viên đã đăng nhập; Quản trị: Admin hoặc Director.

---

## 4. Ma trận Acceptance Criteria cho 20 User Story

| Story ID | Acceptance Criteria | Mã nguồn liên quan | Kiểm thử | Trạng thái | Việc còn thiếu / Cần hoàn thiện |
|---|---|---|---|---|---|
| **S1-01** (CRM-21) | Đăng nhập đúng thông tin, hỗ trợ đa vai trò, khóa tạm thời 15 phút khi sai 5 lần liên tiếp | `LoginServlet.java`, `AuthService.java`, `LoginAttemptGuard.java`, `UserDAO.java` | `LoginServletTest`, `LoginAttemptGuardTest`, `CRM21LoginCheck` | **TESTED** | Chưa nghiệm thu E2E UI chuyển hướng theo vai trò; `LoginAttemptGuard` lưu tạm trong RAM máy chủ. |
| **S1-02** (CRM-22) | Đăng xuất vô hiệu hóa session, dọn dẹp SessionRegistry, timeout 30 phút, bảo vệ CSRF | `LogoutServlet.java`, `SessionServlet.java`, `CsrfFilter.java`, `SessionRegistry.java` | `CsrfFilterTest`, `SessionRegistryTest`, HTTP logout check | **AC PASS (tạm thời)** | Cần kiểm chứng timeout 30 phút trên môi trường Tomcat staging thực tế. |
| **S1-03** (CRM-23) | Yêu cầu reset pass qua email, token hiệu lực 30 phút, dùng 1 lần, chống email enumeration | `AuthService.java`, `PasswordResetTokenDAO.java`, `EmailService.java`, `ResetTokenUtil.java` | `ForgotPasswordAcceptanceIntegrationTest`, `ResetTokenUtilTest` | **PARTIAL** | Chưa kiểm thử gửi email thực tế ra máy chủ SMTP thật; cần cấu hình biến môi trường `CRM_SMTP_*`. |
| **S1-04** (CRM-24) | Đổi mật khẩu yêu cầu pass cũ, kiểm tra chính sách độ mạnh, thu hồi tất cả các phiên khác | `ChangePasswordServlet.java`, `AuthService.java`, `SessionRegistry.java`, `PasswordUtil.java` | `PasswordUtilTest`, `SessionRegistryTest` | **TESTED** | Chưa có kiểm thử E2E tự động xác nhận các phiên đăng nhập khác trên trình duyệt bị đăng xuất ngay lập tức. |
| **S1-05** (CRM-25) | Phân quyền SELF/TEAM/ALL trên 4 thực thể; xem danh sách, chi tiết và xuất Excel | `ScopedEntityServlet.java`, `DataScopeService.java`, `ScopedEntityDAO.java`, `ExcelService.java` | `ScopedEntityServletTest`, `DataScopeServiceTest`, `ScopeAccessPolicyTest`, HTTP Excel check | **PARTIAL** | TEAM hiện chỉ so khớp cùng `team_id`, chưa hỗ trợ cây nhóm con; phân trang HTML đang cắt danh sách tại tầng Service. |
| **S1-06** (CRM-26) | Menu động theo vai trò; hiển thị đúng quyền; thông tin tài khoản | `MenuNavigationFilter.java`, `MenuService.java`, `MenuDAO.java`, `sidebar.jsp` | `MenuServiceTest`, `MenuNavigationFilterTest`, `MenuServletTest` | **PARTIAL** | Các mục `/leads`, `/kpi`, `/automation` chưa có trang riêng (riêng `/quotes` đã hoạt động). Đề xuất ẩn 3 mục này khỏi giao diện. |
| **S1-07** (CRM-27) | Hiển thị trang lỗi 401, 403, 404, 500 với đúng mã HTTP status | `ErrorPageServlet.java`, `web.xml`, `jsp/errors/*.jsp` | Kiểm tra HTTP trả về đúng status 401, 403, 404, 500 | **AC PASS (tạm thời)** | Cần kiểm tra giao diện hiển thị trên các độ phân giải màn hình khác nhau (responsive). |
| **S1-08** (CRM-28) | Quản lý tài khoản: danh sách, tạo mới, chỉnh sửa, tìm kiếm, lọc, phân trang | `UserServlet.java`, `UserService.java`, `UserDAO.java`, `user-list.jsp` | `UserServletTest`, kiểm tra HTTP `user-list.jsp` | **PARTIAL** | Biểu mẫu tạo mới và chỉnh sửa hiện sử dụng modal trong `user-list.jsp` và `user-detail.jsp`, chưa có trang form nhập liệu riêng. |
| **S1-09** (CRM-29) | Hỗ trợ đa vai trò; trưởng nhóm bắt buộc gắn team; chặn tự thu hồi quyền Admin | `PermissionServlet.java`, `UserRoleServlet.java`, `PermissionService.java`, `UserRoleService.java` | `UserRoleServiceTest`, kiểm tra HTTP trang quyền | **TESTED** | Cần bổ sung kiểm thử E2E thu hồi ngay lập tức phiên đăng nhập của người dùng khi bị thay đổi vai trò. |
| **S1-10** (CRM-30) | Khóa tài khoản, bắt buộc bàn giao dữ liệu (khách hàng, cơ hội), ghi log, thu hồi session | `UserServlet.java`, `UserService.java`, `OwnershipTransferService.java`, `UserLockHandoverDAO.java` | Kiểm tra mã nguồn giao dịch JDBC, test đơn vị dịch vụ | **NEEDS APPROVAL** | Cần phê duyệt phương án thông báo tài khoản bị khóa trên UI để không phá vỡ cơ chế chống tiết lộ tài khoản; kiểm chứng bàn giao trên DB staging. |
| **S2-01** (CRM-32) | Nhập người dùng từ Excel: tải mẫu, kiểm tra dữ liệu, bỏ qua dòng lỗi, lưu lô và báo cáo | `UserImportServlet.java`, `ExcelService.java`, `UserImportDAO.java`, `user-import.jsp` | `ExcelServiceTest`, kiểm tra xử lý workbook | **TESTED** | Chưa có kiểm thử E2E tải lên tệp multipart lớn và tải xuống báo cáo tổng kết lỗi. |
| **S2-02** (CRM-35) | Xem và cập nhật hồ sơ cá nhân; cập nhật số điện thoại; chặn sửa email và vai trò | `ProfileServlet.java`, `ProfileService.java`, `UserDAO.java`, `profile.jsp` | `ProfileServiceTest`, kiểm tra HTTP trang profile | **AC PASS (tạm thời)** | Cần bổ sung kiểm thử E2E tự động trên trình duyệt xác nhận các trường bị khóa không thể bị ghi đè qua DevTools. |
| **S2-03** (CRM-36) | Tải lên ảnh đại diện JPG/PNG tối đa 2MB; tự động crop vuông 512x512 và sinh thumbnail 128x128 | `AvatarServlet.java`, `AvatarService.java`, `AvatarImageProcessor.java`, `avatar.jsp` | `AvatarServiceTest`, `AvatarImageProcessorTest` | **TESTED** | Đã chuẩn hóa JSP sang `/jsp/users/avatar.jsp`; chưa thực hiện kiểm thử HTTP/E2E tải ảnh trên máy chủ Tomcat thật (`CRM36AvatarHttpCheck`). |
| **S2-04** (CRM-37) | Nhật ký kiểm toán before/after JSON; bộ lọc theo user, action, thời gian | `AuditLogServlet.java`, `AuditLogService.java`, `AuditLogDAO.java`, `audit-log.jsp` | `AuditLogServiceTest`, `AuditLogDAOTest` | **PARTIAL** | Phương thức ghi nhật ký chiết khấu (`DISCOUNT_CHANGED`) và chỉ tiêu (`TARGET_CHANGED`) chưa được gọi từ các luồng nghiệp vụ thực tế. |
| **S2-05** (CRM-39) | Danh mục sản phẩm: bảo vệ giá vốn (chỉ Director/Admin), giá niêm yết >= sàn, chặn xóa an toàn | `ProductServlet.java`, `ProductPageServlet.java`, `ProductService.java`, `ProductDAO.java` | `ProductServiceTest`, `ProductServletAuthorizationTest` | **TESTED** | Cần phê duyệt GAP phân quyền giá vốn (cho phép Admin cùng Director); chưa kiểm thử E2E toàn bộ luồng thêm/sửa/xóa và ngăn xóa trên DB thật. |
| **S2-06** (CRM-42) | Cây cơ cấu tổ chức nhóm kinh doanh, phân cấp cha-con, chỉ định trưởng nhóm và khu vực | `OrganizationServlet.java`, `OrganizationPageServlet.java`, `OrganizationService.java` | `OrganizationServiceTest`, `OrganizationServletTest` | **PARTIAL** | Phân quyền phạm vi TEAM hiện chỉ giới hạn trong cùng `team_id`, chưa mở rộng ra cây nhóm con. Thao tác DELETE đơn vị tổ chức chưa triển khai. |
| **S2-07** (CRM-44) | Danh mục dùng chung: CRUD danh mục, quản lý thứ tự sắp xếp, chặn xóa khi có tham chiếu | `CategoryServlet.java`, `CategoryPageServlet.java`, `CategoryService.java`, `CategoryDAO.java` | `CategoryServiceTest` | **AC PASS (tạm thời)** | Cần kiểm tra giao diện kéo thả sắp xếp thứ tự và kiểm chứng chặn xóa trên dữ liệu thực tế. |
| **S2-08** (CRM-46) | Quản lý trường tùy chỉnh (TEXT, NUMBER, DATE, SELECT) cho CUSTOMER và OPPORTUNITY | `CustomFieldServlet.java`, `CustomFieldPageServlet.java`, `CustomFieldService.java` | `CustomFieldServiceTest`, `CustomFieldServletTest` | **NEEDS APPROVAL** | CRUD định nghĩa trường tùy chỉnh đã hoàn tất; tuy nhiên việc tích hợp các trường này vào Biểu mẫu nhập liệu, Bộ lọc và Xuất Excel của Khách hàng/Cơ hội chưa được triển khai. |
| **S2-09** (CRM-47) | Giai đoạn bán hàng (Pipeline Stages): thứ tự, xác suất thắng 0-100%, bảo toàn cơ hội khi xóa stage | `PipelineServlet.java`, `PipelinePageServlet.java`, `PipelineService.java`, `PipelineDAO.java` | `PipelineServiceTest` | **TESTED** | Đã có logic `reassignOpportunities` trong transaction khi xóa giai đoạn; chưa có kiểm thử E2E trên cơ sở dữ liệu có sẵn cơ hội. |
| **S2-10** (CRM-48) | Danh mục lý do Thắng/Thua (WIN/LOSS) và Đối thủ cạnh tranh phục vụ phân tích bán hàng | `WinLossServlet.java`, `WinLossPageServlet.java`, `WinLossService.java`, `WinLossDAO.java` | `WinLossServiceTest`, `WinLossServletTest` | **AC PASS (tạm thời)** | Quản lý danh mục Master Data đã hoàn tất và kiểm thử đầy đủ. Việc tích hợp bắt buộc nhập lý do khi đóng cơ hội thuộc phạm vi Sprint 5. |

---

## 5. Chú giải & Quy tắc nghiệm thu

1. Một User Story chỉ được chuyển sang trạng thái **`AC PASS`** khi có đầy đủ cả ba điều kiện:
   - Mã nguồn triển khai đầy đủ nghiệp vụ.
   - Có kiểm thử đơn vị hoặc kiểm thử tích hợp tự động bao phủ các trường hợp biên.
   - Đã được kiểm chứng thực tế trên môi trường máy chủ chạy độc lập (Tomcat 10.1 & MySQL 8.x).
2. Các mục ghi nhận **`NEEDS APPROVAL`** cần được người dùng phê duyệt phương án xử lý trước khi tiến hành viết mã hoặc đóng gói phát hành.
