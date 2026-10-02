# BẢNG ÁNH XẠ GIAO DIỆN VÀ HỆ THỐNG (FE — BE MAPPING)

> Phiên bản: 2.0 (TASK 03B — Codebase Route Verification & PR #82 Correction)
> Repository: `customer-relationship-manager-ictu-n5`
> Công nghệ: Frontend JSP (SSR) | Backend Servlet & Service & DAO | Cơ sở dữ liệu MySQL

Tài liệu này cung cấp bảng ánh xạ chi tiết từ giao diện người dùng (JSP), biểu mẫu gửi dữ liệu, đường dẫn URL thực tế, Servlet tiếp nhận, Service xử lý logic nghiệp vụ cho đến DAO truy xuất dữ liệu trong cơ sở dữ liệu.
Toàn bộ các endpoint đã được kiểm toán đối chiếu 1:1 với mã nguồn Java Servlet trong `backend/src/main/java/` và `route-inventory.md`.

---

## 1. Bảng ánh xạ tổng thể 20 User Story

| Story ID & Mã Issue | Tên chức năng | Tệp JSP / Biểu mẫu | URL hiển thị | API Endpoint thực tế | Phương thức HTTP | Servlet tiếp nhận | Service xử lý | DAO truy cập dữ liệu | Trạng thái |
|---|---|---|---|---|---|---|---|---|---|
| **S1-01** (CRM-21) | Đăng nhập & Khóa tạm thời | `/jsp/auth/login.jsp` | `/login` | `/login`, `/api/auth/login` | GET, POST | `LoginServlet` | `AuthService`, `LoginAttemptGuard` | `UserDAO` | **TESTED** |
| **S1-02** (CRM-22) | Đăng xuất & Phiên làm việc | `/jsp/shared/header.jsp` | *(Header UI)* | `/api/auth/logout`, `/api/auth/session` | GET, POST | `LogoutServlet`, `SessionServlet` | `AuthService`, `SessionRegistry` | `UserDAO` | **AC PASS (tạm thời)** |
| **S1-03** (CRM-23) | Quên & Đặt lại mật khẩu | `/jsp/auth/forgot-password.jsp`, `reset-password.jsp` | `/forgot-password`, `/reset-password` | `/api/auth/forgot-password`, `/api/auth/reset-password` | GET, POST | `ForgotPasswordServlet`, `ResetPasswordServlet` | `AuthService`, `EmailService` | `PasswordResetTokenDAO`, `UserDAO` | **PARTIAL** |
| **S1-04** (CRM-24) | Đổi mật khẩu cá nhân | `/jsp/auth/change-password.jsp` | `/change-password` | `/change-password`, `/api/auth/change-password` | GET, POST | `ChangePasswordServlet` | `AuthService`, `SessionRegistry` | `UserDAO` | **TESTED** |
| **S1-05** (CRM-25) | Phân quyền phạm vi dữ liệu | `/jsp/shared/scoped-records.jsp` | `/customers`, `/opportunities`, `/activities`, `/quotes` | `/api/{entity}`, `/api/{entity}/{id}`, `/api/{entity}/export` | GET, POST, PUT, DELETE | `ScopedEntityPageServlet`, `ScopedEntityServlet` | `DataScopeService`, `ExcelService` | `ScopedEntityDAO`, `DataScopeHelper` | **PARTIAL** |
| **S1-06** (CRM-26) | Menu điều hướng động | `/jsp/shared/sidebar.jsp`, `header.jsp` | *(Layout chung)* | `/api/navigation/menu`, `/api/menu`, `/navigation`, `/menu` | GET | `MenuServlet`, `NavigationServlet`, `MenuNavigationFilter` | `MenuService` | `MenuDAO` | **PARTIAL** |
| **S1-07** (CRM-27) | Các trang báo lỗi hệ thống | `/jsp/errors/401.jsp`, `403.jsp`, `404.jsp`, `500.jsp` | `/errors/401`, `/errors/403`, `/errors/404`, `/errors/500` | *(Tự động trả JSON khi gọi `/api/*`)* | ANY (service) | `ErrorPageServlet` | *(Container Error Handler)* | N/A | **AC PASS (tạm thời)** |
| **S1-08** (CRM-28) | Quản lý tài khoản người dùng | `/jsp/users/user-list.jsp`, `user-detail.jsp` | `/users`, `/users/detail` | `/api/users`, `/api/users/{id}` | GET, POST, PUT, DELETE | `UserServlet` | `UserService`, `TeamService` | `UserDAO` | **PARTIAL** |
| **S1-09** (CRM-29) | Phân quyền vai trò & Nhóm | `/jsp/permissions/role-permission.jsp` | `/permissions` | `/permissions/assign`, `/permissions/team`, `/api/roles`, `/api/roles/*`, `/api/permissions/users/*`, `/api/permissions/assign`, `/api/teams` | GET, POST, PUT, DELETE | `PermissionServlet`, `UserRoleServlet`, `PermissionApiServlet`, `TeamServlet` | `PermissionService`, `UserRoleService`, `TeamService` | `RoleDAO`, `UserRoleDAO`, `UserTeamDAO`, `TeamDAO` | **TESTED** |
| **S1-10** (CRM-30) | Khóa tài khoản & Bàn giao | `/jsp/users/user-detail.jsp` (Modal Khóa) | `/users/detail` | `/users/lock-handover`, `/users/unlock`, `/api/users/{id}/lock`, `/api/users/{id}/lock-handover`, `/api/users/{id}/unlock`, `/api/users/{id}/transfer-data` | POST | `UserServlet` | `UserService`, `OwnershipTransferService` | `UserDAO`, `UserLockHandoverDAO`, `ScopedEntityDAO` | **NEEDS APPROVAL** |
| **S2-01** (CRM-32) | Nhập người dùng từ Excel | `/jsp/users/user-import.jsp` | `/users/import` | `/users/import`, `/api/users/import/template`, `/preview`, `/confirm` | GET, POST | `UserImportServlet` | `ExcelService` | `UserImportDAO`, `UserDAO` | **TESTED** |
| **S2-02** (CRM-35) | Hồ sơ cá nhân người dùng | `/jsp/users/profile.jsp` | `/profile`, `/user/profile` | `/profile`, `/api/profile`, `/api/users/me` | GET, POST, PUT | `ProfileServlet` | `ProfileService` | `UserDAO` | **AC PASS (tạm thời)** |
| **S2-03** (CRM-36) | Tải lên ảnh đại diện | `/jsp/users/avatar.jsp` | `/profile/avatar` | `/profile/avatar`, `/profile/avatar/image`, `/thumbnail`, `/api/users/me/avatar` | GET, POST | `AvatarServlet` | `AvatarService`, `AvatarImageProcessor` | `AvatarDAO` | **TESTED** |
| **S2-04** (CRM-37) | Nhật ký kiểm toán hệ thống | `/jsp/audit/audit-log.jsp` | `/audit` | `/api/audit-logs` | GET | `AuditPageServlet`, `AuditLogServlet` | `AuditLogService` | `AuditLogDAO` | **PARTIAL** |
| **S2-05** (CRM-39) | Danh mục sản phẩm & Giá vốn | `/jsp/products/product-list.jsp` | `/products/page` | `/products`, `/products/page`, `/api/products`, `/api/products/{id}` | GET, POST, PUT, DELETE | `ProductPageServlet`, `ProductServlet` | `ProductService` | `ProductDAO` | **TESTED** |
| **S2-06** (CRM-42) | Cây cơ cấu tổ chức nhóm | `/jsp/organization/organization.jsp` | `/organization`, `/organization/page` | `/api/organization/units`, `/api/organization/units/{id}` | GET, POST, PUT | `OrganizationPageServlet`, `OrganizationServlet` | `OrganizationService` | `OrganizationDAO`, `UserTeamDAO`, `UserDAO` | **PARTIAL** |
| **S2-07** (CRM-44) | Danh mục cấu hình dùng chung | `/jsp/configuration/configuration.jsp` | `/configuration`, `/configuration/page` | `/api/categories`, `/api/categories/{id}`, `/api/categories/{id}/display-order`, `/api/master-data` | GET, POST, PUT, DELETE | `CategoryPageServlet`, `CategoryServlet` | `CategoryService` | `CategoryDAO` | **AC PASS (tạm thời)** |
| **S2-08** (CRM-46) | Quản lý trường tùy chỉnh | `/jsp/customfields/custom-field-list.jsp` | `/customfields`, `/customfields/page` | `/api/custom-fields`, `/api/custom-fields/{id}`, `/api/custom-fields/values` | GET, POST, PUT, DELETE | `CustomFieldPageServlet`, `CustomFieldServlet` | `CustomFieldService` | `CustomFieldDAO` | **NEEDS APPROVAL** |
| **S2-09** (CRM-47) | Quy trình giai đoạn bán hàng | `/jsp/pipeline/pipeline-config.jsp` | `/pipeline`, `/pipeline/page` | `/api/pipeline/stages`, `/api/pipeline/stages/{id}`, `/api/pipeline/stages/reorder`, `/validate-transition` | GET, POST, PUT, DELETE | `PipelinePageServlet`, `PipelineServlet` | `PipelineService` | `PipelineDAO` | **TESTED** |
| **S2-10** (CRM-48) | Lý do Thắng/Thua & Đối thủ | `/jsp/winloss/winloss.jsp` | `/winloss` | `/api/winloss/reasons`, `/api/winloss/competitors` *(DELETE qua `?id=`)* | GET, POST, PUT, DELETE | `WinLossPageServlet`, `WinLossServlet` | `WinLossService` | `WinLossDAO` | **AC PASS (tạm thời)** |

---

## 2. Chi tiết luồng tương tác và xử lý mã nguồn (FE ➔ BE)

### S1-01: Đăng nhập & Khóa tài khoản
- **Biểu mẫu JSP:** `/jsp/auth/login.jsp`
  - Thẻ form: `<form action="${pageContext.request.contextPath}/login" method="post">`
  - Các input: `email`, `password`.
- **Luồng xử lý:**
  1. Trình duyệt gửi HTTP POST tới `/login` hoặc API client gửi POST tới `/api/auth/login`.
  2. `LoginServlet.doPost()` (`LoginServlet.java:52`) nhận request, đọc `email` và `password`.
  3. Gọi `AuthService.login(email, password)`:
     - Kiểm tra `LoginAttemptGuard`: nếu email bị khóa tạm (quá 5 lần sai trong 15 phút), từ chối ngay.
     - `UserDAO.findForLogin(email)` truy vấn tài khoản và kiểm tra `active = TRUE` cùng `status = 'ACTIVE'`.
     - `PasswordUtil.verifyPassword(password, hash)` kiểm chứng mật khẩu với chuẩn BCrypt.
  4. Nếu đăng nhập thành công:
     - Hủy session cũ (`session.invalidate()`), tạo session mới.
     - Lưu `userId`, `roles`, `displayName`, `currentUser` vào session.
     - Khởi tạo CSRF token mới.
     - HTML form chuyển hướng 302 về `/dashboard`; API JSON trả HTTP 200 kèm thông tin user.
  5. Nếu đăng nhập thất bại:
     - Tăng biến đếm thất bại trong `LoginAttemptGuard`.
     - Forward lại `/jsp/auth/login.jsp` kèm thông báo lỗi; API JSON trả 401 hoặc 403.

### S1-05: Phân quyền phạm vi dữ liệu (Data Scope)
- **Giao diện JSP:** `/jsp/shared/scoped-records.jsp`
  - Hiển thị danh sách bản ghi có phân trang, thanh tìm kiếm và nút xuất Excel.
  - Hỗ trợ đầy đủ 4 thực thể: `/customers`, `/opportunities`, `/activities`, `/quotes`.
- **Luồng xử lý:**
  1. `ScopedEntityPageServlet` (`ScopedEntityPageServlet.java:12`) tiếp nhận request GET.
  2. Xác định người dùng hiện hành từ `session.getAttribute("userId")`.
  3. `DataScopeService` lấy quyền phạm vi (`SELF`, `TEAM`, `ALL`) của người dùng.
  4. `ScopedEntityDAO` xây dựng câu lệnh SQL có mệnh đề `WHERE` được lọc bởi `DataScopeHelper`:
     - `ALL`: `WHERE 1=1`
     - `TEAM`: `WHERE u.team_id = ?` (phạm vi nhóm trực tiếp)
     - `SELF`: `WHERE r.owner_user_id = ?`
  5. Khi bấm "Xuất Excel": Client gửi GET tới `/api/{entity}/export` (hoặc `/api/{entity}?export=true`). `ScopedEntityServlet.doGet()` (`ScopedEntityServlet.java:95`) gọi `exportExcel()` kết xuất workbook XLSX và trả về luồng nhị phân với `Content-Disposition: attachment; filename="crm-...-export.xlsx"`.

### S1-10: Khóa tài khoản & Bàn giao dữ liệu
- **Giao diện JSP:** `/jsp/users/user-detail.jsp` (Hộp thoại Modal Khóa tài khoản).
  - Form HTML gửi POST tới `${pageContext.request.contextPath}/users/lock-handover`.
  - Tham số: `userId`, `recipientUserId`, `reason`.
- **Luồng xử lý:**
  1. `UserServlet.doPost()` (`UserServlet.java:127`) tiếp nhận yêu cầu, kiểm tra quyền quản trị (`Admin`).
  2. Gọi `UserService.lockUser(targetUserId, actorUserId, recipientUserId, reason)`.
  3. Mở kết nối JDBC và thiết lập `conn.setAutoCommit(false)`.
  4. `OwnershipTransferService` khóa các dòng dữ liệu bằng `SELECT ... FOR UPDATE` trên các bảng `customers` và `opportunities`.
  5. Nếu người dùng sở hữu dữ liệu mà không có `recipientUserId`: Rollback giao dịch và trả về lỗi yêu cầu chọn người nhận bàn giao.
  6. Thực hiện chuyển giao quyền sở hữu: `UPDATE customers SET owner_user_id = ? WHERE owner_user_id = ?` (tương tự với `opportunities`).
  7. Cập nhật trạng thái người dùng thành `LOCKED` trong bảng `users`.
  8. Ghi nhật ký vào bảng `user_lock_handovers` và `audit_logs`.
  9. `conn.commit()`.
  10. Gọi `SessionRegistry.revokeAll(targetUserId)` để hủy lập tức mọi phiên đăng nhập đang hoạt động của tài khoản bị khóa.
  11. Đối với API: Endpoint là `POST /api/users/{id}/lock-handover` hoặc `POST /api/users/{id}/unlock`.

### S2-03: Tải lên ảnh đại diện (Avatar)
- **Giao diện JSP:** `/jsp/users/avatar.jsp` *(Đã chuẩn hóa vị trí trong TASK 02C)*
  - Biểu mẫu: `<form action="${pageContext.request.contextPath}/profile/avatar" method="post" enctype="multipart/form-data">`
  - Input file: `<input type="file" name="avatar" accept="image/jpeg,image/png">`
- **Luồng xử lý:**
  1. `AvatarServlet.doPost()` (`AvatarServlet.java:63`) tiếp nhận request multipart với trường tệp `avatar`.
  2. Kiểm tra kích thước tệp (không vượt quá 2 MiB = 2.097.152 byte) và định dạng ảnh qua ImageIO (chỉ chấp nhận JPEG/PNG hợp lệ).
  3. `AvatarImageProcessor` thực hiện cắt vuông tâm (center square crop) và co giãn thành kích thước ảnh chính 512x512 pixel, cùng ảnh thu nhỏ (thumbnail) 128x128 pixel.
  4. `AvatarService` lưu dữ liệu ảnh dạng nhị phân vào đĩa/thư mục cấu hình và ghi nhận vào cơ sở dữ liệu qua `AvatarDAO`.
  5. Cập nhật lại đường dẫn ảnh trong session của người dùng để hiển thị tức thì trên thanh điều hướng (`header.jsp`).

### S2-04: Nhật ký kiểm toán hệ thống (Audit Logs)
- **Giao diện JSP:** `/jsp/audit/audit-log.jsp`
  - Servlet hiển thị: `AuditPageServlet` (`AuditPageServlet.java:27`) tại URL `/audit`.
  - Bộ lọc: `userId`, `objectId`, `objectType`, `from`, `to`.
- **API Xử lý dữ liệu:**
  - Endpoint: `GET /api/audit-logs` (`AuditLogServlet.java:35`).
  - Hỗ trợ tham số phân trang: `limit` (tối đa 500, mặc định 100), `offset` (mặc định 0).
  - Trả về danh sách nhật ký JSON chứa các giá trị `beforeValue` và `afterValue`.
  - Nghiêm cấm ghi trực tiếp qua API: POST, PUT, DELETE đều trả về 405 Method Not Allowed.

### S2-06: Cây cơ cấu tổ chức nhóm (CRM-42)
- **Giao diện JSP:** `/jsp/organization/organization.jsp`
  - Servlet hiển thị: `OrganizationPageServlet` (`OrganizationPageServlet.java:20`) tại URL `/organization` hoặc `/organization/page`.
- **API Xử lý dữ liệu:**
  - Servlet: `OrganizationServlet` (`OrganizationServlet.java:27-30`) tại `@WebServlet({"/api/organization/units", "/api/organization/units/*"})`.
  - Tuyến đường thực tế:
    - `GET /api/organization/units`: Trả về danh sách/cây đơn vị tổ chức (`UnitList` với danh sách `items`).
    - `POST /api/organization/units`: Tạo mới đơn vị (yêu cầu Admin/Director). Body JSON: `name`, `parentId`, `managerId`, `region`, `active`. Trả về 201 Created.
    - `PUT /api/organization/units/{id}`: Cập nhật thông tin đơn vị theo ID trong path.
  - Tuyến đường không tồn tại trong mã nguồn: `/api/organization/tree`, `/api/organization/teams`. Thao tác xóa `DELETE` chưa triển khai (ghi nhận GAP).

### S2-08: Quản lý trường tùy chỉnh (CRM-46)
- **Giao diện JSP:** `/jsp/customfields/custom-field-list.jsp`
  - Servlet hiển thị: `CustomFieldPageServlet` (`CustomFieldPageServlet.java:21`) tại URL `/customfields` hoặc `/customfields/page`.
  - Hỗ trợ thao tác form POST với các action: `create`, `update`, `delete`.
- **API Xử lý dữ liệu:**
  - Servlet: `CustomFieldServlet` (`CustomFieldServlet.java:36`) tại `@WebServlet({"/api/custom-fields", "/api/custom-fields/*"})`.
  - Tuyến đường thực tế:
    - `GET /api/custom-fields`: Lấy danh sách định nghĩa theo `entityType` (`CUSTOMER`, `OPPORTUNITY`).
    - `GET /api/custom-fields/{id}`: Lấy chi tiết định nghĩa trường theo ID.
    - `POST /api/custom-fields`: Tạo mới định nghĩa trường (yêu cầu Admin/Director). Trả về 201 Created.
    - `PUT /api/custom-fields/{id}`: Cập nhật định nghĩa trường.
    - `DELETE /api/custom-fields/{id}`: Xóa hoặc vô hiệu hóa trường nếu đã có dữ liệu.
    - `GET /api/custom-fields/values?entity=...&recordId=...`: Lấy giá trị trường tùy chỉnh của một bản ghi.
    - `PUT /api/custom-fields/values`: Lưu giá trị trường tùy chỉnh cho bản ghi (phương thức PUT, body chứa `entityType`, `recordId`, `values: {...}`).
  - Tuyến đường không tồn tại: `/api/customfields` (dạng viết liền không dấu gạch nối không tồn tại ở API layer).

### S2-10: Lý do Thắng/Thua & Đối thủ cạnh tranh (CRM-48)
- **Giao diện JSP:** `/jsp/winloss/winloss.jsp`
  - Servlet hiển thị: `WinLossPageServlet` (`WinLossPageServlet.java:19`) tại URL `/winloss`.
  - Hỗ trợ 3 tab: `WIN`, `LOSS`, `COMPETITOR` qua query param `?tab=`.
- **API Xử lý dữ liệu:**
  - Servlet: `WinLossServlet` (`WinLossServlet.java:35`) tại `@WebServlet({"/api/winloss/reasons", "/api/winloss/competitors"})`.
  - Tuyến đường thực tế:
    - `GET /api/winloss/reasons`: Danh sách lý do thắng/thua (`?includeInactive=true|false`).
    - `POST /api/winloss/reasons`: Thêm lý do. Body JSON: `type`, `reasonText`, `description`, `displayOrder`, `active`. Trả về 201 Created.
    - `PUT /api/winloss/reasons`: Cập nhật lý do. Body JSON chứa `id` và thông tin mới.
    - `DELETE /api/winloss/reasons?id={id}`: Xóa hoặc chuyển `DEACTIVATED` qua tham số query `?id=`.
    - `GET /api/winloss/competitors`: Danh sách đối thủ cạnh tranh.
    - `POST /api/winloss/competitors`: Thêm đối thủ. Body JSON: `name`, `strengths`, `weaknesses`, `website`, `displayOrder`, `active`.
    - `PUT /api/winloss/competitors`: Cập nhật đối thủ. Body JSON chứa `id`.
    - `DELETE /api/winloss/competitors?id={id}`: Xóa hoặc chuyển `DEACTIVATED` qua tham số query `?id=`.
  - Lưu ý kỹ thuật: `@WebServlet` không có wildcard `/*`, do đó thao tác DELETE nhận `id` qua tham số query `?id=...`, không nhận qua đường dẫn `/{id}`.

---

## 3. Quy chuẩn bảo vệ an toàn tệp JSP
- Mọi tệp giao diện nằm trong thư mục `/jsp/` đều được bảo vệ bởi `ViewAccessFilter`.
- `ViewAccessFilter` kiểm tra `request.getDispatcherType()`:
  - Nếu là `DispatcherType.REQUEST` (người dùng gõ trực tiếp URL dạng `http://.../jsp/...` trên trình duyệt): Lập tức gửi phản hồi `404 Not Found`.
  - Nếu là `DispatcherType.FORWARD` (do các Servlet controller chuyển tiếp nội bộ qua `RequestDispatcher.forward()`): Cho phép xử lý bình thường.
- Cơ chế này ngăn chặn hoàn toàn việc rò rỉ mã nguồn JSP hoặc truy cập trái phép vượt qua kiểm tra quyền của Servlet.
