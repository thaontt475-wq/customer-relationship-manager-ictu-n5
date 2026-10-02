# BẢNG ÁNH XẠ GIAO DIỆN VÀ HỆ THỐNG (FE — BE MAPPING)

> Phiên bản: 1.2 (TASK 03 — Final Validation)
> Repository: `customer-relationship-manager-ictu-n5`
> Công nghệ: Frontend JSP (SSR) | Backend Servlet & Service & DAO | Cơ sở dữ liệu MySQL

Tài liệu này cung cấp bảng ánh xạ chi tiết từ giao diện người dùng (JSP), biểu mẫu gửi dữ liệu, đường dẫn URL, Servlet tiếp nhận, Service xử lý logic nghiệp vụ cho đến DAO truy xuất dữ liệu trong cơ sở dữ liệu.

---

## 1. Bảng ánh xạ tổng thể 20 User Story

| Story ID & Mã Issue | Tên chức năng | Tệp JSP / Biểu mẫu | URL hiển thị | API Endpoint | Phương thức HTTP | Servlet tiếp nhận | Service xử lý | DAO truy cập dữ liệu | Trạng thái |
|---|---|---|---|---|---|---|---|---|---|
| **S1-01** (CRM-21) | Đăng nhập & Khóa tạm thời | `/jsp/auth/login.jsp` | `/login` | `/api/auth/login` | GET, POST | `LoginServlet` | `AuthService`, `LoginAttemptGuard` | `UserDAO` | **TESTED** |
| **S1-02** (CRM-22) | Đăng xuất & Phiên làm việc | `/jsp/shared/header.jsp` | *(Header UI)* | `/api/auth/logout`, `/api/auth/session` | GET, POST | `LogoutServlet`, `SessionServlet` | `AuthService`, `SessionRegistry` | `UserDAO` | **AC PASS (tạm thời)** |
| **S1-03** (CRM-23) | Quên & Đặt lại mật khẩu | `/jsp/auth/forgot-password.jsp`, `reset-password.jsp` | `/forgot-password`, `/reset-password` | `/api/auth/forgot-password`, `/api/auth/reset-password` | GET, POST | `ForgotPasswordServlet`, `ResetPasswordServlet` | `AuthService`, `EmailService` | `PasswordResetTokenDAO`, `UserDAO` | **PARTIAL** |
| **S1-04** (CRM-24) | Đổi mật khẩu cá nhân | `/jsp/auth/change-password.jsp` | `/change-password` | `/api/auth/change-password` | GET, POST | `ChangePasswordServlet` | `AuthService`, `SessionRegistry` | `UserDAO` | **TESTED** |
| **S1-05** (CRM-25) | Phân quyền phạm vi dữ liệu | `/jsp/shared/scoped-records.jsp`, `scoped-record-detail.jsp` | `/customers`, `/opportunities`, `/activities`, `/quotes` | `/api/customers`, `/api/opportunities`, `/api/activities`, `/api/quotes`, `/api/scope/records/export` | GET | `ScopedEntityPageServlet`, `ScopedEntityServlet` | `DataScopeService`, `ExcelService` | `ScopedEntityDAO` | **PARTIAL** |
| **S1-06** (CRM-26) | Menu điều hướng động | `/jsp/shared/sidebar.jsp`, `header.jsp` | *(Layout chung)* | `/api/navigation/menu` | GET | `MenuNavigationFilter`, `MenuServlet` | `MenuService` | `MenuDAO` | **PARTIAL** |
| **S1-07** (CRM-27) | Các trang báo lỗi hệ thống | `/jsp/errors/401.jsp`, `403.jsp`, `404.jsp`, `500.jsp` | `/errors/401`, `/errors/403`, `/errors/404`, `/errors/500` | *(Tự động theo mã lỗi)* | GET | `ErrorPageServlet` | *(Container Error Handler)* | N/A | **AC PASS (tạm thời)** |
| **S1-08** (CRM-28) | Quản lý tài khoản người dùng | `/jsp/users/user-list.jsp`, `user-create.jsp`, `user-edit.jsp` | `/users`, `/users/create`, `/users/edit` | `/api/users`, `/api/users/detail` | GET, POST, PUT, DELETE | `UserServlet`, `UserPageServlet` | `UserService` | `UserDAO` | **PARTIAL** |
| **S1-09** (CRM-29) | Phân quyền vai trò & Nhóm | `/jsp/permissions/role-permission.jsp` | `/permissions` | `/api/permissions/roles`, `/api/permissions/user-roles` | GET, POST | `PermissionServlet` | `PermissionService`, `UserRoleService` | `RoleDAO`, `UserRoleDAO` | **TESTED** |
| **S1-10** (CRM-30) | Khóa tài khoản & Bàn giao | `/jsp/users/user-detail.jsp` (Modal Khóa) | `/users/detail` | `/api/users/lock`, `/api/users/unlock` | POST | `UserLockServlet` | `UserService`, `OwnershipTransferService` | `UserDAO`, `UserLockHandoverDAO`, `ScopedEntityDAO` | **NEEDS APPROVAL** |
| **S2-01** (CRM-32) | Nhập người dùng từ Excel | `/jsp/users/user-import.jsp` | `/users/import` | `/api/users/import/template`, `/preview`, `/confirm` | GET, POST | `UserImportServlet` | `ExcelService` | `UserImportDAO`, `UserDAO` | **TESTED** |
| **S2-02** (CRM-35) | Hồ sơ cá nhân người dùng | `/jsp/users/profile.jsp` | `/profile` | `/api/users/me` | GET, POST, PUT | `ProfileServlet` | `ProfileService` | `UserDAO` | **AC PASS (tạm thời)** |
| **S2-03** (CRM-36) | Tải lên ảnh đại diện | `/jsp/users/avatar.jsp` | `/profile/avatar` | `/profile/avatar/image`, `/thumbnail`, `/api/users/me/avatar` | GET, POST | `AvatarServlet` | `AvatarService`, `AvatarImageProcessor` | `UserAvatarDAO` | **TESTED** |
| **S2-04** (CRM-37) | Nhật ký kiểm toán hệ thống | `/jsp/audit/audit-log.jsp` | `/audit` | `/api/audit/logs` | GET | `AuditPageServlet`, `AuditLogServlet` | `AuditLogService` | `AuditLogDAO` | **PARTIAL** |
| **S2-05** (CRM-39) | Danh mục sản phẩm & Giá vốn | `/jsp/products/product-list.jsp`, `product-form.jsp` | `/products`, `/products/create`, `/products/edit` | `/api/products`, `/api/products/detail` | GET, POST, PUT, DELETE | `ProductPageServlet`, `ProductServlet` | `ProductService` | `ProductDAO` | **TESTED** |
| **S2-06** (CRM-42) | Cây cơ cấu tổ chức nhóm | `/jsp/organization/organization.jsp` | `/organization` | `/api/organization/tree`, `/api/organization/teams` | GET, POST, PUT, DELETE | `OrganizationServlet` | `OrganizationService` | `OrganizationDAO` | **PARTIAL** |
| **S2-07** (CRM-44) | Danh mục cấu hình dùng chung | `/jsp/configuration/configuration.jsp` | `/configuration` | `/api/categories` | GET, POST, PUT, DELETE | `CategoryServlet` | `CategoryService` | `CategoryDAO` | **AC PASS (tạm thời)** |
| **S2-08** (CRM-46) | Quản lý trường tùy chỉnh | `/jsp/customfields/custom-field-list.jsp` | `/customfields` | `/api/customfields`, `/api/customfields/values` | GET, POST, PUT, DELETE | `CustomFieldServlet` | `CustomFieldService` | `CustomFieldDAO` | **NEEDS APPROVAL** |
| **S2-09** (CRM-47) | Quy trình giai đoạn bán hàng | `/jsp/pipeline/pipeline-config.jsp` | `/pipeline` | `/api/pipeline/stages`, `/stages/reorder` | GET, POST, PUT, DELETE | `PipelineServlet` | `PipelineService` | `PipelineDAO` | **TESTED** |
| **S2-10** (CRM-48) | Lý do Thắng/Thua & Đối thủ | `/jsp/winloss/winloss.jsp` | `/winloss` | `/api/winloss/reasons`, `/api/winloss/competitors` | GET, POST, PUT, DELETE | `WinLossServlet` | `WinLossService` | `WinLossDAO` | **AC PASS (tạm thời)** |

---

## 2. Chi tiết luồng xử lý và tương tác (FE ➔ BE)

### S1-01: Đăng nhập & Khóa tài khoản
- **Biểu mẫu JSP:** `/jsp/auth/login.jsp`
  - Thẻ form: `<form action="${pageContext.request.contextPath}/login" method="post">`
  - Các input: `email`, `password`.
- **Luồng xử lý:**
  1. Trình duyệt gửi HTTP POST tới `/login`.
  2. `LoginServlet.doPost()` nhận request, đọc `email` và `password`.
  3. Gọi `AuthService.login(email, password)`:
     - Kiểm tra `LoginAttemptGuard`: nếu địa chỉ email bị khóa tạm (quá 5 lần thử thất bại trong 15 phút), từ chối ngay lập tức.
     - `UserDAO.findForLogin(email)` truy vấn thông tin tài khoản và kiểm tra `active = TRUE` cùng `status = 'ACTIVE'`.
     - `PasswordUtil.verifyPassword(password, hash)` kiểm chứng mật khẩu với chuẩn mã hóa BCrypt.
  4. Nếu đăng nhập thành công:
     - Hủy session hiện tại (`session.invalidate()`), tạo session mới.
     - Lưu `userId`, `roles`, `displayName`, `currentUser` vào session.
     - Khởi tạo CSRF token mới.
     - Chuyển hướng 302 về `/dashboard`.
  5. Nếu đăng nhập thất bại:
     - Tăng biến đếm thất bại trong `LoginAttemptGuard`.
     - Thiết lập thuộc tính `error` và forward lại `/jsp/auth/login.jsp`.

### S1-05: Phân quyền phạm vi dữ liệu (Data Scope)
- **Giao diện JSP:** `/jsp/shared/scoped-records.jsp`
  - Hiển thị danh sách bản ghi có phân trang, thanh tìm kiếm và nút xuất Excel.
  - Hỗ trợ đầy đủ 4 thực thể: `/customers`, `/opportunities`, `/activities`, `/quotes`.
- **Luồng xử lý:**
  1. `ScopedEntityPageServlet` tiếp nhận request hiển thị các trang `/customers`, `/opportunities`, `/activities`, `/quotes`.
  2. Xác định người dùng hiện hành từ `session.getAttribute("userId")`.
  3. `DataScopeService` lấy quyền phạm vi (`SELF`, `TEAM`, `ALL`) của người dùng.
  4. `ScopedEntityDAO` xây dựng câu lệnh SQL có mệnh đề `WHERE` được lọc bởi `DataScopeHelper`:
     - `ALL`: `WHERE 1=1`
     - `TEAM`: `WHERE u.team_id = ?` (phạm vi nhóm trực tiếp)
     - `SELF`: `WHERE r.owner_user_id = ?`
  5. Khi người dùng bấm "Xuất Excel": `ScopedEntityServlet.doGet()` gọi `ExcelService` kết xuất workbook XLSX hợp lệ và trả về luồng nhị phân với `Content-Disposition: attachment; filename="records.xlsx"`.

### S1-10: Khóa tài khoản & Bàn giao dữ liệu
- **Giao diện JSP:** `/jsp/users/user-detail.jsp` (Hộp thoại Modal Khóa tài khoản).
  - Form gửi AJAX hoặc POST tới `/api/users/lock`.
  - Tham số: `targetUserId`, `recipientUserId`, `reason`.
- **Luồng xử lý:**
  1. `UserLockServlet.doPost()` tiếp nhận yêu cầu, kiểm tra quyền quản trị (`Admin`).
  2. Gọi `UserService.lockUser(targetUserId, actorUserId, recipientUserId, reason)`.
  3. Mở kết nối JDBC và thiết lập `conn.setAutoCommit(false)`.
  4. `OwnershipTransferService` khóa các dòng dữ liệu bằng `SELECT ... FOR UPDATE` trên các bảng `customers` và `opportunities`.
  5. Nếu người dùng sở hữu dữ liệu mà không có `recipientUserId`: Rollback giao dịch và trả về lỗi yêu cầu chọn người nhận bàn giao.
  6. Thực hiện chuyển giao quyền sở hữu: `UPDATE customers SET owner_user_id = ? WHERE owner_user_id = ?` (tương tự với `opportunities`).
  7. Cập nhật trạng thái người dùng thành `LOCKED` trong bảng `users`.
  8. Ghi nhật ký vào bảng `user_lock_handovers` và `audit_logs`.
  9. `conn.commit()`.
  10. Gọi `SessionRegistry.revokeAll(targetUserId)` để hủy lập tức mọi phiên đăng nhập đang hoạt động của tài khoản bị khóa.

### S2-03: Tải lên ảnh đại diện (Avatar)
- **Giao diện JSP:** `/jsp/users/avatar.jsp` *(Đã chuẩn hóa vị trí trong TASK 02C)*
  - Biểu mẫu: `<form action="${pageContext.request.contextPath}/profile/avatar" method="post" enctype="multipart/form-data">`
  - Input: `<input type="file" name="avatar" accept="image/jpeg,image/png">`
- **Luồng xử lý:**
  1. `AvatarServlet` tiếp nhận request multipart.
  2. Kiểm tra kích thước tệp (không vượt quá 2 MiB = 2.097.152 byte) và định dạng ảnh qua ImageIO (chỉ chấp nhận JPEG/PNG hợp lệ).
  3. `AvatarImageProcessor` thực hiện cắt vuông tâm (center square crop) và co giãn thành kích thước ảnh chính 512x512 pixel, cùng ảnh thu nhỏ (thumbnail) 128x128 pixel.
  4. `AvatarService` lưu dữ liệu ảnh dạng nhị phân vào cơ sở dữ liệu qua `UserAvatarDAO`.
  5. Cập nhật lại đường dẫn ảnh trong session của người dùng để hiển thị tức thì trên thanh điều hướng (`header.jsp`).

---

## 3. Quy chuẩn bảo vệ an toàn tệp JSP
- Mọi tệp giao diện nằm trong thư mục `/jsp/` đều được bảo vệ bởi `ViewAccessFilter`.
- `ViewAccessFilter` kiểm tra `request.getDispatcherType()`:
  - Nếu là `DispatcherType.REQUEST` (người dùng gõ trực tiếp URL dạng `http://.../jsp/...` trên trình duyệt): Lập tức gửi phản hồi `404 Not Found`.
  - Nếu là `DispatcherType.FORWARD` (do các Servlet controller chuyển tiếp nội bộ qua `RequestDispatcher.forward()`): Cho phép xử lý bình thường.
- Cơ chế này ngăn chặn hoàn toàn việc rò rỉ mã nguồn JSP hoặc truy cập trái phép vượt qua kiểm tra quyền của Servlet.
