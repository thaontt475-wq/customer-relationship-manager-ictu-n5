# BÁO CÁO PHÂN TÍCH THIẾU HỤT & ĐỐI CHIẾU TIÊU CHÍ CHẤP NHẬN (API GAP ANALYSIS)

> Phiên bản: 2.0 (TASK 03B — Codebase Route Verification & PR #82 Correction)
> Repository: `customer-relationship-manager-ictu-n5`
> Baseline commit: `b61bee548e07fe5c33041a7cbc2e392d7c58744e`
> Trạng thái dừng: **WAITING_FOR_APPROVAL**

---

## 1. Tóm tắt tình trạng và nguyên tắc đánh giá

Theo yêu cầu chuẩn hóa trong TASK 03 và kiểm chứng tuyến đường trong TASK 03B, việc đánh giá toàn bộ 20 User Story thuộc Sprint 1 và Sprint 2 được thực hiện trên nguyên tắc độc lập, khách quan, dựa trên bằng chứng kiểm thử thực tế và mã nguồn hiện hữu. Không sử dụng báo cáo QA cũ để tự động coi các tiêu chí chấp nhận (Acceptance Criteria - AC) là đạt.

Hệ thống phân loại trạng thái:
- **`IMPLEMENTED`**: Đã có mã nguồn thực hiện chức năng.
- **`TESTED`**: Có bài kiểm thử tự động đã chạy thành công (lưu ý: `TESTED` không tương đương hoàn thành toàn bộ Acceptance Criteria).
- **`AC PASS`**: Có đủ bằng chứng kiểm chứng đáp ứng tiêu chí chấp nhận cụ thể.
- **`PARTIAL`**: Chỉ đáp ứng một phần tiêu chí chấp nhận.
- **`NOT TESTED`**: Chưa được kiểm thử hoặc chưa có bằng chứng kiểm thử thực tế trên môi trường độc lập.
- **`NEEDS APPROVAL`**: Có quyết định kỹ thuật hoặc nghiệp vụ chưa được phê duyệt/chốt.

### Báo cáo về tài liệu Acceptance Criteria gốc:
> [!IMPORTANT]
> Trong môi trường Antigravity hiện tại, **chưa tìm thấy văn bản gốc nguyên văn** chứa bộ tiêu chí chấp nhận (Acceptance Criteria) cho 20 User Story. Tài liệu `backend/docs/crm-audit-20261001.md` chỉ là báo cáo QA thứ cấp và không được sử dụng để thay thế AC gốc.
> Theo chỉ đạo nghiêm ngặt của người dùng, **chúng tôi trân trọng đề nghị người dùng cung cấp lại bảng Acceptance Criteria gốc nguyên văn** trước khi đưa ra kết luận nghiệm thu cuối cùng.

---

## 2. Thống kê số lượng chính xác & Giới hạn kết luận (20 User Story)

- **Tổng số User Story:** Đúng 20 / 20 User Story (S1-01 đến S1-10 và S2-01 đến S2-10), không trùng lặp, không bỏ sót.
- **Phân loại trạng thái tổng hợp (mỗi User Story chỉ có một trạng thái duy nhất):**
  - **AC PASS (tạm thời):** 5 story (S1-02, S1-07, S2-02, S2-07, S2-10).
  - **TESTED:** 7 story (S1-01, S1-04, S1-09, S2-01, S2-03, S2-05, S2-09).
  - **PARTIAL:** 6 story (S1-03, S1-05, S1-06, S1-08, S2-04, S2-06).
  - **NEEDS APPROVAL:** 2 story (S1-10, S2-08).
  - *Tổng cộng:* Đúng 20 / 20 story (5 + 7 + 6 + 2 = 20).

> [!WARNING]
> **Giới hạn kết luận bắt buộc:**
> Vì chưa có toàn bộ Acceptance Criteria gốc nguyên văn trong repository:
> 1. Tài liệu này **tuyệt đối không tuyên bố 5 story trên đã được nghiệm thu chính thức**.
> 2. **Không công bố bất kỳ tỷ lệ phần trăm hoàn thành AC nào** để tránh gây hiểu nhầm.
> 3. Phân biệt rạch ròi giữa kiểm thử mã nguồn (unit/integration test), kiểm thử HTTP và kiểm thử E2E trên hệ thống thực tế.
> 4. Toàn bộ các phần chưa được kiểm chứng đều được ghi nhận cụ thể trong từng User Story.
> 5. Giữ nguyên các vấn đề chưa được phê duyệt thuộc CRM-25/42, CRM-30, CRM-39 và CRM-46. Các quyết định nghiệp vụ chưa được người dùng phê duyệt không được xem là yêu cầu triển khai đã chốt.

---

## 3. Bảng bằng chứng đối chiếu toàn diện 20 User Story (Mapping Evidence)

Dưới đây là bảng đối chiếu đầy đủ từng endpoint trong tài liệu với mã nguồn thực tế theo cấu trúc bắt buộc:
`Story | Endpoint trong tài liệu | Mapping mã nguồn | HTTP method | File:Line | Kết quả | Điều chỉnh`

| Story | Endpoint trong tài liệu | Mapping mã nguồn | HTTP method | File:Line | Kết quả | Điều chỉnh |
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

---

## 4. Chi tiết các bất đồng đã kiểm chứng theo yêu cầu mục 1

### 4.1. CRM-37: `/api/audit/logs` so với `/api/audit-logs`
- **Mã nguồn Servlet:** `AuditLogServlet.java:35` định nghĩa rõ ràng: `@WebServlet("/api/audit-logs")`.
- **Phương thức xử lý:** Chỉ triển khai `doGet` (`AuditLogServlet.java:55-76`). Các phương thức `doPost`, `doPut`, `doDelete` đều trả về HTTP 405 Method Not Allowed.
- **Tham số thực tế:** Hỗ trợ `userId`, `objectId`, `objectType`, `from`, `to`, `limit` (tối đa 500, mặc định 100), `offset` (mặc định 0).
- **Phản hồi:** Trả về JSON 200 danh sách `AuditLog` kèm giá trị `beforeValue` và `afterValue`.
- **Giao diện HTML:** Được hiển thị qua `AuditPageServlet.java:27` (`@WebServlet("/audit")`) forward tới `/jsp/audit/audit-log.jsp`.
- **Kết luận:** Route `/api/audit/logs` trong các tài liệu trước đây là sai quy cách ký tự (dùng dấu gạch chéo thay vì gạch nối). URL runtime thực tế chính xác là `/api/audit-logs`. Đã sửa toàn diện.

### 4.2. CRM-42: `/api/organization/tree`, `/api/organization/teams` so với `/api/organization/units`
- **Mã nguồn Servlet:** `OrganizationServlet.java:27-30` định nghĩa: `@WebServlet({"/api/organization/units", "/api/organization/units/*"})`.
- **Phương thức xử lý:**
  - `GET /api/organization/units`: Trả về cây tổ chức (`OrganizationServlet.java:50-72`).
  - `POST /api/organization/units`: Tạo mới đơn vị (`OrganizationServlet.java:74-112`).
  - `PUT /api/organization/units/{id}`: Cập nhật đơn vị theo ID số trong đường dẫn (`OrganizationServlet.java:114-154`).
  - `DELETE`: Chưa được triển khai (trả về 405 mặc định).
- **Kết luận:** Hai tuyến đường `/api/organization/tree` và `/api/organization/teams` hoàn toàn không tồn tại trong mã nguồn Servlet. Tuyến đường thực tế duy nhất là `/api/organization/units`. Thao tác xóa đơn vị là GAP chưa triển khai. Đã sửa tài liệu và ghi nhận GAP.

### 4.3. CRM-46: `/api/customfields` so với `/api/custom-fields`
- **Mã nguồn Servlet:** `CustomFieldServlet.java:36` định nghĩa: `@WebServlet({"/api/custom-fields", "/api/custom-fields/*"})`.
- **Phương thức xử lý:**
  - `GET /api/custom-fields`: Lấy danh sách định nghĩa trường.
  - `GET /api/custom-fields/{id}`: Lấy định nghĩa trường theo ID.
  - `POST /api/custom-fields`: Tạo mới định nghĩa trường (HTTP 201).
  - `PUT /api/custom-fields/{id}`: Cập nhật định nghĩa trường.
  - `DELETE /api/custom-fields/{id}`: Xóa hoặc vô hiệu hóa trường.
  - `GET /api/custom-fields/values?entity=...&recordId=...`: Lấy giá trị trường của bản ghi.
  - `PUT /api/custom-fields/values`: Lưu giá trị trường cho bản ghi qua phương thức **PUT** (không phải POST như tài liệu cũ mô tả).
- **Giao diện HTML:** `CustomFieldPageServlet.java:21` (`@WebServlet({"/customfields", "/customfields/page"})`) forward tới `/jsp/customfields/custom-field-list.jsp`.
- **Kết luận:** Route API chuẩn có dấu gạch ngang `/api/custom-fields`. Thao tác lưu giá trị là `PUT /api/custom-fields/values`. Đã sửa toàn bộ.

### 4.4. CRM-30: `/api/users/lock`, `/api/users/unlock` so với `/users/lock-handover`, `/users/unlock` và `/api/users/*`
- **Mã nguồn Servlet:** Toàn bộ chức năng quản lý người dùng và khóa tài khoản được xử lý tập trung trong `UserServlet.java:29-35`:
  `@WebServlet({"/users", "/users/detail", "/users/lock-handover", "/users/unlock", "/api/users/*"})`.
  Không có class `UserLockServlet` nào tồn tại trong hệ thống.
- **Phương thức và tuyến đường thực tế:**
  - **Form action từ giao diện HTML (Modal Khóa):**
    - `POST /users/lock-handover`: Xử lý khóa tài khoản kèm bàn giao dữ liệu, sau đó redirect về trang chi tiết người dùng (`UserServlet.java:127,340`).
    - `POST /users/unlock`: Xử lý mở khóa tài khoản từ form HTML (`UserServlet.java:127,421`).
  - **REST API:**
    - Tuyến đường API sử dụng cấu trúc `splitApiPath`: ID người dùng nằm trong đường dẫn, theo sau là hành động:
      - `POST /api/users/{id}/lock`: Khóa tài khoản không bắt buộc bàn giao (`UserServlet.java:175`).
      - `POST /api/users/{id}/lock-handover`: Khóa tài khoản bắt buộc bàn giao dữ liệu (`UserServlet.java:176`).
      - `POST /api/users/{id}/unlock`: Mở khóa tài khoản (`UserServlet.java:178`).
      - `POST /api/users/{id}/transfer-data`: Chuyển quyền sở hữu dữ liệu độc lập (`UserServlet.java:179`).
- **Kết luận:** Các endpoint `/api/users/lock` và `/api/users/unlock` truyền `targetUserId` trong request body không tồn tại trong mã nguồn (sẽ bị trả về 404). Các endpoint chuẩn là `POST /api/users/{id}/lock-handover` và `POST /api/users/{id}/unlock`. Đã sửa tài liệu khớp mã nguồn.

---

## 5. Các khoảng cách kỹ thuật (GAP) và Quyết định chờ phê duyệt (NEEDS APPROVAL)

### 5.1. CRM-30 (S1-10) — Cơ chế chống rò rỉ thông tin tài khoản (Anti-Enumeration) trên màn hình đăng nhập
- **Hiện trạng mã nguồn:**
  - Trong `AuthService.login()`: khi tài khoản bị khóa (`!user.isActive() || !"ACTIVE".equals(user.getStatus())`), hệ thống trả về `null`.
  - `LoginServlet` hiển thị thông báo chung: `"Email hoặc mật khẩu không đúng"`.
- **Phân tích an toàn thông tin:**
  - Đây là cơ chế bảo mật tiêu chuẩn nhằm chống lại kiểu tấn công dò quét sự tồn tại và trạng thái tài khoản (Account Enumeration Attack). Nếu hệ thống hiển thị "Tài khoản của bạn đã bị khóa" cho một phiên chưa xác thực mật khẩu, kẻ tấn công bên ngoài có thể xác định được email nào tồn tại và đang bị khóa.
  - Việc hiển thị thông báo tài khoản bị khóa chỉ nên diễn ra sau khi người dùng đã nhập đúng mật khẩu, hoặc chỉ hiển thị trong màn hình quản trị nội bộ.
- **Trạng thái:** Giữ nguyên ở trạng thái **`NEEDS APPROVAL`** chờ người dùng quyết định phương án giao diện.

### 5.2. CRM-39 (S2-05) — Phân quyền giá vốn: "Giám đốc kinh doanh" vs Admin/Director
- **Yêu cầu AC gốc:** Chỉ "Giám đốc kinh doanh" (Sales Director) mới được xem và sửa giá vốn (`cost_price`).
- **Mã nguồn hiện tại:**
  - `ProductService.java` định nghĩa: `DIRECTOR_ADMIN_ROLES = Set.of("admin", "director", "giám đốc", "giam doc", "quản trị viên", "quan tri vien")`.
  - Hàm `isDirectorOrAdmin(userRoles)` cấp quyền xem/sửa giá vốn cho cả `Admin` (Quản trị viên) và `Director` (Giám đốc chung).
- **Khoảng cách kỹ thuật (GAP):**
  - Hệ thống cho phép Quản trị viên can thiệp giá vốn; đồng thời hệ thống hiện tại chưa phân tách vai trò "Giám đốc kinh doanh" riêng biệt mà sử dụng vai trò chung `Director`.
- **Trạng thái:** Giữ nguyên ở trạng thái **`NEEDS APPROVAL`** chờ người dùng quyết định có thu hồi quyền giá vốn của Admin hay không.

### 5.3. CRM-25 & CRM-42 (S1-05, S2-06) — Cây tổ chức và phạm vi dữ liệu TEAM
- **Đối chiếu Schema và Mã nguồn:**
  - Bảng `teams` có cột `parent_id` (tự tham chiếu) biểu diễn quan hệ cha-con; `leader_user_id` chỉ định người phụ trách; `region` xác định khu vực. Bảng `users` liên kết qua `team_id`.
  - Trong `ScopeAccessPolicy.java` và `DataScopeHelper.java`, phạm vi `TEAM` chỉ lọc phẳng: `record.ownerTeamId() == actor.teamId()` hoặc `u.team_id = ?`.
- **Hiện trạng kỹ thuật:**
  - Phạm vi `TEAM` hiện tại được giới hạn chính xác trong cùng một nhóm trực tiếp (`team_id`).
  - Hệ thống chưa hỗ trợ cơ chế đệ quy để Trưởng nhóm cấp trên tự động truy cập dữ liệu của các nhóm con (`parent_id`). Đây là hiện trạng kỹ thuật của hệ thống, không tự suy diễn hành vi mở rộng khi chưa có quyết định phê duyệt chính thức.

### 5.4. CRM-46 (S2-08) — Tích hợp trường tùy chỉnh vào Khách hàng và Cơ hội
- **Hiện trạng mã nguồn:**
  - CRUD định nghĩa trường tùy chỉnh (`CustomFieldServlet`) và lưu giá trị theo cặp khóa/giá trị (`saveValues`) đã hoàn tất.
  - Tuy nhiên, việc tích hợp tự động các trường này vào Biểu mẫu nhập liệu JSP, Bộ lọc tìm kiếm và Xuất Excel của Khách hàng (`customers`) và Cơ hội (`opportunities`) chưa được triển khai.
- **Trạng thái:** **`NEEDS APPROVAL`** (Yêu cầu thiết kế UI/UX và kế hoạch tích hợp ở các Sprint tiếp theo).

---

## 6. Phân loại khối lượng công việc tiếp theo

1. **Frontend (FE):**
   - Xây dựng giao diện Form nhập liệu cho `user-create.jsp` và `user-edit.jsp` (CRM-28).
   - Tích hợp giao diện hiển thị trường tùy chỉnh động vào form khách hàng và cơ hội (CRM-46).
   - Ẩn 3 mục menu chưa có trang (`/leads`, `/kpi`, `/automation`) trên `sidebar.jsp` (CRM-26).
2. **Backend (BE):**
   - Triển khai endpoint `DELETE /api/organization/units/{id}` cho CRM-42.
   - Thống nhất phương án xử lý thông báo tài khoản bị khóa sau khi xác thực mật khẩu (CRM-30).
   - Quyết định việc phân tách vai trò Giám đốc kinh doanh cho giá vốn (CRM-39).
   - Kích hoạt gọi hàm `recordDiscountChange` và `recordTargetChange` khi phân hệ Báo giá và KPI được triển khai (CRM-37).
3. **Integration & Environment:**
   - Cấu hình SMTP (`CRM_SMTP_*`) trên môi trường kiểm thử để kiểm chứng gửi email thật (CRM-23).
   - Kiểm tra hiển thị responsive trên thiết bị di động (CRM-26, CRM-27).
4. **Quality Assurance (QA):**
   - Chạy kiểm thử HTTP/E2E tải ảnh đại diện trên Tomcat thật (`CRM36AvatarHttpCheck`) (CRM-36).
   - Kiểm thử E2E đa phiên thu hồi session khi đổi mật khẩu (CRM-24).
   - Kiểm thử E2E bàn giao dữ liệu khi khóa tài khoản trên DB staging (CRM-30).
   - Kiểm thử tải lên tệp Excel lớn và tải xuống báo cáo tổng kết lỗi (CRM-32).
   - Kiểm thử chặn xóa sản phẩm có liên kết giao dịch thực tế (CRM-39).
