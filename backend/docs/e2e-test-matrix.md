# MA TRẬN KIỂM THỬ TÍCH HỢP HTTP & E2E (E2E TEST MATRIX)

> **Phiên bản:** 3.1 (TASK 04A.3 — Staging Safety Validation & Synthetic Test Coverage)
> **Repository:** `thaontt475-wq/customer-relationship-manager-ictu-n5`
> **Baseline Commit:** `0479ea50109bb11fa28a751555ebc0be51ab75fe`
> **Nhánh thực hiện:** `docs/CRM-staging-test-plan` (Worktree: `crm-staging-test-plan`)
> **Trạng thái:** **WAITING_FOR_APPROVAL** (Chỉ thiết kế ma trận, chưa thực thi kịch bản)

---

## 1. Nguyên tắc kiểm thử trên môi trường Staging độc lập

1. **Kiểm thử HTTP thực tế (Real HTTP Calls):** Toàn bộ test case được thực thi thông qua yêu cầu HTTP thực tế gửi tới cổng staging `http://localhost:8088`, không sử dụng mock Servlet.
2. **Xác minh đa tầng (Multi-layer Verification):**
   - Tầng HTTP: Mã trạng thái HTTP (200, 201, 302, 400, 401, 403, 404, 405, 409), Header, Cookies (`JSESSIONID`).
   - Tầng Cơ sở dữ liệu: Truy vấn SQL trực tiếp trên `crm_db_staging` để xác minh trạng thái biến động dữ liệu.
   - Tầng Bộ nhớ máy chủ: Kiểm tra `SessionRegistry` (thu hồi phiên), `LoginAttemptGuard` (khóa bộ nhớ RAM), tệp ảnh nhị phân trên đĩa.
3. **Thẩm định an toàn tiền trạm (Pre-flight Assertion):**
   - Trước khi bất kỳ test case nào chạy: Thực hiện truy vấn `SELECT DATABASE() = 'crm_db_staging'`. Nếu sai -> Dừng toàn bộ suite test ngay lập tức.
   - Kiểm tra tài khoản kết nối: `SELECT CURRENT_USER() LIKE 'crm_staging_user@%'`.
   - Kiểm tra âm tính tước quyền trên production: `SELECT 1 FROM crm_db.users LIMIT 1` BẮT BUỘC nhận lỗi `Access denied`.
   - Kiểm tra âm tính giới hạn DDL: Thử `CREATE TABLE probe_ddl` bằng tài khoản runtime BẮT BUỘC nhận lỗi `CREATE command denied`.
   - Xác nhận bảng phụ trợ Sprint 2 (`leads`, cột bổ sung của `opportunities`, `customers`, `activities`, 27 categories (8 INDUSTRY, 5 COMPANY_SIZE, 8 LEAD_SOURCE, 6 ACTIVITY_TYPE), 6 pipeline stages) đã được nạp hoàn tất qua `staging-02-reconcile.sql`.
4. **Phân loại kết quả:**
   - **`PASS`**: Đạt 100% các tiêu chí mong đợi và xác minh dữ liệu thành công.
   - **`FAIL`**: Không đạt mã HTTP, sai lệch nội dung phản hồi, hoặc dữ liệu DB không đồng nhất.
   - **`NOT TESTED`**: Chưa thể kiểm thử do tính năng là GAP hoặc đang chờ người dùng phê duyệt phương án.

---

## 2. Ma trận kiểm thử chi tiết 14 User Story trọng tâm

---

### Nhóm 1: Xác thực & Quản lý phiên (CRM-21, CRM-22, CRM-23, CRM-24)

#### `TC-E2E-CRM21-01`: Đăng nhập thành công với đa vai trò
- **User Story:** CRM-21 (S1-01) — Đăng nhập hệ thống
- **Acceptance Criteria:** Người dùng có tài khoản hợp lệ đăng nhập thành công, nhận cookie phiên làm việc, lưu đủ danh sách vai trò và chuyển hướng hoặc nhận dữ liệu người dùng.
- **Điều kiện chuẩn bị:** Tài khoản `admin@crm-staging.local` tồn tại với mật khẩu `Staging@123`, trạng thái `ACTIVE`, được gán 2 vai trò `Admin` và `Director`.
- **Dữ liệu đầu vào:** `email=admin@crm-staging.local`, `password=Staging@123`.
- **Các bước thực hiện:**
  1. Gửi request `POST http://localhost:8088/api/auth/login` với body JSON `{"email": "admin@crm-staging.local", "password": "Staging@123"}`.
  2. Lưu lại cookie `JSESSIONID` trả về trong phản hồi.
- **Kết quả mong đợi:** HTTP `200 OK`. Body JSON: `success=true`, `data.roles` chứa cả `"Admin"` và `"Director"`. Cookie `JSESSIONID` có thuộc tính `HttpOnly`.
- **Cách xác minh dữ liệu:** Gửi `GET /api/auth/session` kèm `JSESSIONID` -> Trả về 200 kèm danh sách vai trò và CSRF token.
- **Phương án khôi phục:** Không cần khôi phục (thao tác đọc).
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

#### `TC-E2E-CRM21-02`: Khóa tạm thời 15 phút khi nhập sai mật khẩu 5 lần liên tiếp
- **User Story:** CRM-21 (S1-01) — Cơ chế chống tấn công Brute-force
- **Acceptance Criteria:** Nhập sai mật khẩu 5 lần liên tiếp trên cùng một email sẽ bị khóa tạm thời 15 phút, trả về HTTP 403 Forbidden và thông báo rõ thời gian khóa.
- **Điều kiện chuẩn bị:** Tài khoản `rep1@crm-staging.local` ở trạng thái `ACTIVE`.
- **Dữ liệu đầu vào:** `email=rep1@crm-staging.local`, `password=WrongPassword!`.
- **Các bước thực hiện:**
  1. Gửi liên tiếp 4 request `POST /api/auth/login` với mật khẩu sai.
  2. Gửi request lần thứ 5 với mật khẩu sai.
  3. Gửi request lần thứ 6 với mật khẩu ĐÚNG (`Staging@123`).
- **Kết quả mong đợi:**
  - Lần 1 đến 4: HTTP `401 Unauthorized` kèm thông báo số lần còn lại hoặc sai mật khẩu.
  - Lần 5: HTTP `403 Forbidden` kèm thông báo `"Tài khoản bị tạm khóa do nhập sai quá 5 lần liên tiếp. Vui lòng thử lại sau 15 phút."`
  - Lần 6 (dù nhập đúng): Vẫn nhận HTTP `403 Forbidden`.
- **Cách xác minh dữ liệu:** Kiểm tra trạng thái trong `LoginAttemptGuard`. Trạng thái người dùng trong DB vẫn là `ACTIVE` (khóa RAM, không đổi DB).
- **Phương án khôi phục:** Xóa key của email trong `LoginAttemptGuard` hoặc đợi hết thời gian timeout.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

#### `TC-E2E-CRM22-01`: Đăng xuất, hủy session và thu hồi CSRF token
- **User Story:** CRM-22 (S1-02) — Đăng xuất an toàn & Session Timeout
- **Acceptance Criteria:** Đăng xuất vô hiệu hóa hoàn toàn session phía server, thu hồi phiên trong `SessionRegistry`, các request tiếp theo bằng session này bị từ chối 401.
- **Điều kiện chuẩn bị:** Phiên đăng nhập đang hoạt động với cookie `JSESSIONID_ACTIVE`.
- **Dữ liệu đầu vào:** Cookie `JSESSIONID_ACTIVE`.
- **Các bước thực hiện:**
  1. Gửi request `POST /api/auth/logout` kèm cookie `JSESSIONID_ACTIVE`.
  2. Gửi request `GET /api/auth/session` với cookie `JSESSIONID_ACTIVE`.
- **Kết quả mong đợi:**
  - Bước 1: HTTP `200 OK`, JSON `success=true`.
  - Bước 2: HTTP `401 Unauthorized`, JSON `message="Chưa đăng nhập"`.
- **Cách xác minh dữ liệu:** Kiểm tra `SessionRegistry`: Session ID không còn trong danh sách phiên người dùng.
- **Phương án khôi phục:** Không cần.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

#### `TC-E2E-CRM23-01`: Quên mật khẩu, gửi email token qua SMTP thử nghiệm và reset thành công
- **User Story:** CRM-23 (S1-03) — Quên và đặt lại mật khẩu
- **Acceptance Criteria:** Gửi token có hiệu lực 30 phút qua email, sử dụng 1 lần, chống lộ thông tin tài khoản (anti-enumeration); đặt lại mật khẩu thành công và cập nhật hash BCrypt.
- **Điều kiện chuẩn bị:** Mock SMTP đang chạy tại `localhost:2525`. Tài khoản `rep1@crm-staging.local` tồn tại.
- **Dữ liệu đầu vào:** `email=rep1@crm-staging.local`, mật khẩu mới: `NewSecret@2026`.
- **Các bước thực hiện:**
  1. Gửi `POST /api/auth/forgot-password` với `email=rep1@crm-staging.local`.
  2. Đọc gói tin email nhận được từ Mock SMTP, trích xuất raw token.
  3. Gửi `POST /api/auth/reset-password` với `token` vừa lấy và `newPassword=NewSecret@2026`.
  4. Gửi lại request bước 3 với token đó lần thứ hai (thử tái sử dụng token).
  5. Đăng nhập lại với `NewSecret@2026`.
- **Kết quả mong đợi:**
  - Bước 1: HTTP `200 OK` với thông báo chung (chống enumeration).
  - Bước 3: HTTP `200 OK`, thông báo đặt lại mật khẩu thành công.
  - Bước 4: HTTP `400 Bad Request` (token đã dùng không thể tái sử dụng).
  - Bước 5: Đăng nhập thành công với mật khẩu mới.
- **Cách xác minh dữ liệu:** `SELECT used_at FROM password_reset_tokens WHERE token_hash = ...` có giá trị timestamp khác NULL; hash mật khẩu trong bảng `users` đã thay đổi.
- **Phương án khôi phục:** Cập nhật lại mật khẩu ban đầu cho `rep1`.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B) (Yêu cầu Mock SMTP)`.

#### `TC-E2E-CRM24-01`: Đổi mật khẩu và thu hồi tức thì mọi phiên đăng nhập khác
- **User Story:** CRM-24 (S1-04) — Đổi mật khẩu & Thu hồi đa phiên
- **Acceptance Criteria:** Người dùng đổi mật khẩu thành công thì toàn bộ các phiên đăng nhập khác của người dùng đó trên các thiết bị/trình duyệt khác bị vô hiệu hóa ngay lập tức.
- **Điều kiện chuẩn bị:** Tài khoản `rep1@crm-staging.local` có 2 phiên làm việc đồng thời: Phiên A (`JSESSIONID_A`) và Phiên B (`JSESSIONID_B`).
- **Dữ liệu đầu vào:** `currentPassword=Staging@123`, `newPassword=Changed@12345`.
- **Các bước thực hiện:**
  1. Trên Phiên A: Lấy CSRF token, gửi `POST /api/auth/change-password` với mật khẩu hiện tại và mật khẩu mới.
  2. Trên Phiên B: Gửi request `GET /api/profile`.
- **Kết quả mong đợi:**
  - Bước 1: HTTP `200 OK`, thông báo `"Đổi mật khẩu thành công. Các phiên đăng nhập khác đã được thu hồi."`
  - Bước 2: Phiên B nhận HTTP `401 Unauthorized` (bị đá khỏi hệ thống ngay lập tức).
- **Cách xác minh dữ liệu:** `SessionRegistry`: `JSESSIONID_B` đã bị hủy bỏ; chỉ còn phiên hiện hành hoặc danh sách phiên rỗng.
- **Phương án khôi phục:** Đổi lại mật khẩu gốc `Staging@123`.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

---

### Nhóm 2: Phân quyền dữ liệu & Quản lý tổ chức (CRM-25, CRM-42, CRM-28, CRM-29, CRM-30)

#### `TC-E2E-CRM25-01`: Phân quyền phạm vi dữ liệu SELF, TEAM và ALL trên Khách hàng
- **User Story:** CRM-25 (S1-05) — Data Scope Access Control
- **Acceptance Criteria:** User có scope `SELF` chỉ xem được khách hàng do mình sở hữu; `TEAM` xem được khách hàng của các thành viên trong cùng nhóm; `ALL` xem được toàn bộ khách hàng.
- **Điều kiện chuẩn bị:**
  - `CUST-01` do `rep1` sở hữu (Nhóm Miền Bắc).
  - `CUST-02` do `rep2` sở hữu (Nhóm Miền Nam).
  - `CUST-03` do `lead` sở hữu (Nhóm Miền Bắc).
- **Các bước thực hiện:**
  1. Đăng nhập bằng `rep1` (Scope `SELF`): Gửi `GET /api/customers`.
  2. Đăng nhập bằng `lead` (Scope `TEAM`): Gửi `GET /api/customers`.
  3. Đăng nhập bằng `admin` (Scope `ALL`): Gửi `GET /api/customers`.
  4. Đăng nhập bằng `rep1`: Thử gửi `GET /api/customers/{id_cua_CUST-02}` (khách hàng của nhóm khác).
- **Kết quả mong đợi:**
  - Bước 1: Danh sách chỉ chứa đúng `CUST-01`.
  - Bước 2: Danh sách chứa `CUST-01` và `CUST-03`, không chứa `CUST-02`.
  - Bước 3: Danh sách chứa cả 3 khách hàng.
  - Bước 4: Nhận HTTP `403 Forbidden` do vi phạm phạm vi dữ liệu.
- **Cách xác minh dữ liệu:** Kiểm tra số lượng bản ghi trả về trong JSON payload.
- **Phương án khôi phục:** Không cần.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

#### `TC-E2E-CRM28-01`: Quản trị người dùng: CRUD, tìm kiếm, phân trang và bảo vệ email
- **User Story:** CRM-28 (S1-08) — Quản lý tài khoản người dùng
- **Acceptance Criteria:** Admin xem danh sách phân trang, tìm kiếm; tạo mới người dùng (bắt buộc username/email không trùng); cập nhật thông tin; xóa người dùng (chặn tự xóa chính mình).
- **Điều kiện chuẩn bị:** Đăng nhập bằng tài khoản `admin`.
- **Dữ liệu đầu vào:** Tạo người dùng mới `test_user_crud`: `email="crud_test@crm-staging.local"`, `fullName="Test User CRUD"`, `username="crud_test"`.
- **Các bước thực hiện:**
  1. Gửi `GET /api/users?page=1&size=10&search=admin` -> Kiểm tra phân trang và tìm kiếm.
  2. Gửi `POST /api/users` tạo `crud_test`.
  3. Gửi `PUT /api/users/{id_vua_tao}` cập nhật `phone="0912345678"`.
  4. Gửi `DELETE /api/users/{id_admin}` (thử tự xóa chính mình).
  5. Gửi `DELETE /api/users/{id_vua_tao}` (xóa người dùng vừa tạo).
- **Kết quả mong đợi:**
  - Bước 1: HTTP `200 OK`, dữ liệu phân trang chuẩn.
  - Bước 2: HTTP `201 Created`.
  - Bước 3: HTTP `200 OK`, số điện thoại được cập nhật.
  - Bước 4: HTTP `400 Bad Request` hoặc `403 Forbidden` (chặn tự xóa tài khoản đang đăng nhập).
  - Bước 5: HTTP `200 OK`, người dùng bị xóa.
- **Cách xác minh dữ liệu:** `SELECT * FROM users WHERE email = 'crud_test@crm-staging.local'`.
- **Phương án khôi phục:** Xóa bản ghi nếu còn tồn tại.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

#### `TC-E2E-CRM29-01`: Phân vai trò, quy tắc bắt buộc gắn nhóm cho Team Lead và chống tự hạ quyền Admin
- **User Story:** CRM-29 (S1-09) — Phân vai trò & Quản lý nhóm
- **Acceptance Criteria:** Gán đa vai trò; nếu gán vai trò `Team Lead` bắt buộc `teamId` phải khác null; Admin không thể tự gỡ vai trò Admin của chính mình.
- **Điều kiện chuẩn bị:** Tài khoản `admin` đăng nhập. Người dùng `rep1` chưa có nhóm.
- **Các bước thực hiện:**
  1. Gán vai trò `Team Lead` cho `rep1` nhưng gửi `teamId = null`: `POST /api/roles/assign` với `{"userId": {rep1_id}, "roleIds": [{team_lead_role_id}], "teamId": null}`.
  2. Gán vai trò `Team Lead` kèm `teamId = 1`: `POST /api/roles/assign` với `{"userId": {rep1_id}, "roleIds": [{team_lead_role_id}], "teamId": 1}`.
  3. Admin tự gỡ bỏ vai trò Admin của chính mình: `DELETE /api/roles/user/{admin_id}/remove` với body `{"roleId": {admin_role_id}}`.
- **Kết quả mong đợi:**
  - Bước 1: HTTP `400 Bad Request` (`Vai trò Team Lead bắt buộc người dùng phải thuộc về ít nhất một nhóm kinh doanh`).
  - Bước 2: HTTP `200 OK`, gán vai trò và nhóm thành công.
  - Bước 3: HTTP `400 Bad Request` (`Không thể tự gỡ bỏ vai trò Admin của chính mình`).
- **Cách xác minh dữ liệu:** `SELECT role_id FROM user_roles WHERE user_id = ...`.
- **Phương án khôi phục:** Trả lại vai trò cũ cho `rep1`.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

#### `TC-E2E-CRM42-01`: Tạo, cập nhật và hiển thị cây cơ cấu tổ chức nhóm kinh doanh
- **User Story:** CRM-42 (S2-06) — Quản lý cơ cấu tổ chức
- **Acceptance Criteria:** Xem cây đơn vị tổ chức qua `/api/organization/units`; tạo mới đơn vị cha-con, chỉ định trưởng nhóm và khu vực; cập nhật đơn vị.
- **Điều kiện chuẩn bị:** Đăng nhập bằng tài khoản `admin`.
- **Dữ liệu đầu vào:** Đơn vị mới: `name="Phòng Kinh Doanh Số 3"`, `region="SOUTH"`, `parentId=2`.
- **Các bước thực hiện:**
  1. Gửi `GET /api/organization/units` -> Kiểm tra cấu trúc danh sách `data.items`.
  2. Gửi `POST /api/organization/units` với JSON payload tạo đơn vị mới.
  3. Gửi `PUT /api/organization/units/{id_vua_tao}` để đổi tên thành `"Phòng Khách Hàng Doanh Nghiệp"`.
  4. Thử gửi `DELETE /api/organization/units/{id_vua_tao}`.
- **Kết quả mong đợi:**
  - Bước 1: HTTP `200 OK`, danh sách đơn vị có cấu trúc cha-con.
  - Bước 2: HTTP `201 Created`.
  - Bước 3: HTTP `200 OK`.
  - Bước 4: HTTP `405 Method Not Allowed` (ghi nhận đúng khoảng cách GAP chưa triển khai DELETE).
- **Cách xác minh dữ liệu:** `SELECT name, parent_id, region FROM teams WHERE id = ...` trong database.
- **Phương án khôi phục:** `DELETE FROM teams WHERE name LIKE '%Phòng Khách Hàng Doanh Nghiệp%';`.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

#### `TC-E2E-CRM30-01`: Khóa tài khoản, bàn giao dữ liệu bắt buộc và tính nguyên vẹn giao dịch (Atomic Handover)
- **User Story:** CRM-30 (S1-10) — Khóa tài khoản & Bàn giao dữ liệu
- **Acceptance Criteria:** Khóa tài khoản đang sở hữu dữ liệu bắt buộc phải chọn nhân sự nhận bàn giao; toàn bộ khách hàng và cơ hội được chuyển giao trong 1 transaction JDBC duy nhất; phiên của user bị khóa bị thu hồi ngay; rollback toàn bộ nếu có lỗi.
- **Điều kiện chuẩn bị:**
  - User mục tiêu `locked_stg` sở hữu 1 khách hàng `CUST-04` và 1 cơ hội `OPP-02`.
  - User nhận bàn giao `recipient_stg` đang `ACTIVE`.
- **Dữ liệu đầu vào:** `userId={target_id}`, `recipientId={recipient_id}`, `confirm=true`, `reason="Nhân viên thôi việc"`.
- **Các bước thực hiện:**
  1. Gửi request khóa KHÔNG có `recipientId`: `POST /api/users/{target_id}/lock-handover` với `confirm=true`.
  2. Gửi request khóa ĐẦY ĐỦ: `POST /api/users/{target_id}/lock-handover` kèm `recipientId`, `confirm=true`, `reason="..."`.
  3. Thử đăng nhập lại bằng tài khoản `locked_stg`.
- **Kết quả mong đợi:**
  - Bước 1: HTTP `400 Bad Request` (`recipientId là bắt buộc khi user có dữ liệu`). Dữ liệu khách hàng không bị thay đổi.
  - Bước 2: HTTP `200 OK`, trạng thái user thành `LOCKED`.
  - Bước 3: Đăng nhập thất bại (HTTP 401 hoặc bị từ chối).
- **Cách xác minh dữ liệu:**
  - `SELECT owner_user_id FROM customers WHERE id = {CUST-04_id}` -> Phải đổi sang `recipient_stg.id`.
  - `SELECT status FROM users WHERE id = {target_id}` -> Phải là `'LOCKED'`.
  - `SELECT * FROM user_lock_handovers WHERE source_user_id = {target_id}` -> Phải có bản ghi nhật ký bàn giao.
- **Phương án khôi phục:** Chạy `POST /api/users/{target_id}/unlock` và chuyển lại chủ sở hữu.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

---

### Nhóm 3: Dữ liệu & Tệp đính kèm (CRM-32, CRM-36, CRM-37, CRM-39, CRM-44, CRM-46, CRM-47, CRM-48)

#### `TC-E2E-CRM32-01`: Nhập người dùng từ Excel hàng loạt và bỏ qua dòng lỗi
- **User Story:** CRM-32 (S2-01) — Nhập người dùng từ Excel
- **Acceptance Criteria:** Tải tệp template; upload tệp `.xlsx` có 5 dòng (3 dòng hợp lệ, 2 dòng lỗi: 1 trùng email, 1 sai định dạng); preview báo lỗi chi tiết; xác nhận import thì 3 dòng hợp lệ được nạp vào DB, 2 dòng lỗi bị bỏ qua.
- **Điều kiện chuẩn bị:** Đăng nhập bằng `admin`. Tệp Excel kiểm thử `users_test_batch.xlsx`.
- **Các bước thực hiện:**
  1. Gửi `GET /api/users/import/template` -> Kiểm tra tải tệp nhị phân `.xlsx`.
  2. Gửi `POST /api/users/import/preview` (multipart form với tệp Excel).
  3. Gửi `POST /api/users/import/confirm` xác nhận lưu lô.
- **Kết quả mong đợi:**
  - Bước 1: HTTP `200 OK`, Content-Type là spreadsheetml.
  - Bước 2: HTTP `200 OK`, JSON trả về `totalRows=5`, `successCount=3`, `errorCount=2` kèm mảng `errors`.
  - Bước 3: HTTP `200 OK`, 3 người dùng mới được tạo với mật khẩu kích hoạt.
- **Cách xác minh dữ liệu:** `SELECT count(*) FROM users WHERE email LIKE '%@import-test.local%'` trả về đúng 3.
- **Phương án khôi phục:** `DELETE FROM users WHERE email LIKE '%@import-test.local%';`.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

#### `TC-E2E-CRM36-01`: Tải lên ảnh đại diện, kiểm tra CSRF, cắt vuông 512x512 và tạo thumbnail 128x128
- **User Story:** CRM-36 (S2-03) — Quản lý ảnh đại diện
- **Acceptance Criteria:** Upload ảnh JPEG/PNG <= 2MB thành công; tự động cắt vuông tâm 512x512 pixel và thumbnail 128x128 pixel; chặn tệp > 2MB (HTTP 413 hoặc lỗi kích thước); yêu cầu token chống CSRF.
- **Điều kiện chuẩn bị:** User `rep1` đã đăng nhập. Tệp ảnh test `avatar_valid.png` (800x600, 300KB) và `avatar_oversize.png` (3MB).
- **Các bước thực hiện:**
  1. Gửi `GET /api/users/me/avatar` để lấy CSRF token.
  2. Gửi `POST /profile/avatar` multipart với file 3MB -> Kiểm tra chặn kích thước.
  3. Gửi `POST /profile/avatar` multipart với file `avatar_valid.png` hợp lệ kèm CSRF token.
  4. Gửi `GET /profile/avatar/image` và `GET /profile/avatar/thumbnail`.
- **Kết quả mong đợi:**
  - Bước 2: Bị từ chối với lỗi kích thước vượt quá 2MB.
  - Bước 3: Upload thành công, cập nhật đường dẫn ảnh.
  - Bước 4: Trả về ảnh PNG với kích thước chính xác 512x512 và 128x128 byte nhị phân.
- **Cách xác minh dữ liệu:** Kiểm tra tệp ảnh trong thư mục `C:\Tools\crm-stg-data\avatars` và bản ghi trong bảng `user_avatars`.
- **Phương án khôi phục:** Xóa tệp ảnh sinh ra trong thư mục avatar staging.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

#### `TC-E2E-CRM37-01`: Tra cứu nhật ký kiểm toán và bất biến dữ liệu (Immutability)
- **User Story:** CRM-37 (S2-04) — Nhật ký kiểm toán hệ thống
- **Acceptance Criteria:** Nhật ký ghi nhận `before_value` và `after_value` dạng JSON; hỗ trợ lọc theo thời gian, người dùng, loại đối tượng; cấm ghi trực tiếp qua API (POST/PUT/DELETE nhận 405).
- **Điều kiện chuẩn bị:** Thực hiện 1 thao tác thay đổi vai trò hoặc trạng thái người dùng để sinh ra ít nhất 1 bản ghi audit log.
- **Các bước thực hiện:**
  1. Gửi `GET /api/audit-logs?objectType=USER&limit=10`.
  2. Gửi `POST /api/audit-logs` với body JSON giả lập.
  3. Gửi `DELETE /api/audit-logs?id=1`.
- **Kết quả mong đợi:**
  - Bước 1: HTTP `200 OK`, danh sách audit log có đầy đủ `beforeValue`, `afterValue` định dạng JSON.
  - Bước 2 & 3: Đều nhận HTTP `405 Method Not Allowed`.
- **Cách xác minh dữ liệu:** So sánh JSON trả về với bản ghi trong bảng `audit_logs`.
- **Phương án khôi phục:** Không cần.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

#### `TC-E2E-CRM39-01`: Bảo mật giá vốn sản phẩm và cơ chế chống xóa an toàn (Safe-Delete)
- **User Story:** CRM-39 (S2-05) — Danh mục sản phẩm & Bảng giá
- **Acceptance Criteria:** Nhân viên kinh doanh thông thường không được thấy giá vốn (`costPrice` bị ẩn/mask null); giá niêm yết phải >= giá sàn; chặn xóa (HTTP 409 Conflict) khi sản phẩm đã được tham chiếu trong đơn hàng/báo giá.
- **Điều kiện chuẩn bị:** Sản phẩm `PROD-01` có giá vốn `5.000.000` và đang có liên kết trong bảng `order_items`.
- **Các bước thực hiện:**
  1. Đăng nhập bằng `rep1` (Sales Rep): Gửi `GET /api/products/{id_PROD-01}`.
  2. Đăng nhập bằng `admin` (Admin): Gửi `GET /api/products/{id_PROD-01}`.
  3. Đăng nhập bằng `admin`: Gửi `POST /api/products` với `listPrice=5000000`, `floorPrice=7000000` (giá niêm yết < giá sàn).
  4. Đăng nhập bằng `admin`: Gửi `DELETE /api/products/{id_PROD-01}` (sản phẩm có ràng buộc).
- **Kết quả mong đợi:**
  - Bước 1: HTTP `200 OK`, trường `costPrice` là `null`.
  - Bước 2: HTTP `200 OK`, trường `costPrice` hiển thị đúng giá trị `5000000`.
  - Bước 3: HTTP `400 Bad Request` (vi phạm quy tắc giá niêm yết >= giá sàn).
  - Bước 4: HTTP `409 Conflict` (báo lỗi sản phẩm đang được tham chiếu, không thể xóa).
- **Cách xác minh dữ liệu:** `SELECT * FROM products WHERE id = ...` vẫn nguyên vẹn sau bước 4.
- **Phương án khôi phục:** Không cần.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

#### `TC-E2E-CRM44-01`: Quản lý danh mục dùng chung và sắp xếp thứ tự hiển thị
- **User Story:** CRM-44 (S2-07) — Danh mục dùng chung (Master Data)
- **Acceptance Criteria:** CRUD danh mục; cập nhật thứ tự hiển thị `displayOrder`; chặn xóa khi đang có dữ liệu tham chiếu (HTTP 409 Conflict).
- **Điều kiện chuẩn bị:** Đăng nhập bằng `admin`.
- **Các bước thực hiện:**
  1. Gửi `GET /api/categories?type=INDUSTRY` -> Kiểm tra danh sách ngành nghề sắp xếp theo thứ tự hiển thị.
  2. Gửi `POST /api/categories` tạo danh mục mới: `{"type": "LEAD_SOURCE", "code": "TIKTOK", "name": "Kênh TikTok", "displayOrder": 10}`.
  3. Gửi `PUT /api/categories/{id_vua_tao}/display-order` với body JSON đổi thứ tự thành 1.
  4. Gửi `DELETE /api/categories/{id_vua_tao}`.
- **Kết quả mong đợi:**
  - Bước 1: HTTP `200 OK`.
  - Bước 2: HTTP `201 Created`.
  - Bước 3: HTTP `200 OK`.
  - Bước 4: HTTP `200 OK`, xóa danh mục thành công do chưa có liên kết.
- **Cách xác minh dữ liệu:** `SELECT display_order FROM categories WHERE id = ...`.
- **Phương án khôi phục:** Không cần.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

#### `TC-E2E-CRM46-01`: Định nghĩa trường tùy chỉnh động và lưu giá trị bản ghi
- **User Story:** CRM-46 (S2-08) — Quản lý trường tùy chỉnh (Custom Fields)
- **Acceptance Criteria:** Admin tạo định nghĩa trường tùy chỉnh (kiểu TEXT, NUMBER, DATE, SELECT); lưu và đọc giá trị trường tùy chỉnh của khách hàng qua `/api/custom-fields/values`.
- **Điều kiện chuẩn bị:** Khách hàng `CUST-01` tồn tại.
- **Dữ liệu đầu vào:** Định nghĩa trường `ma_so_thue` (kiểu `TEXT`, áp dụng cho `CUSTOMER`). Giá trị gán: `"0101234567"`.
- **Các bước thực hiện:**
  1. Gửi `POST /api/custom-fields` tạo định nghĩa trường `ma_so_thue`.
  2. Gửi `PUT /api/custom-fields/values` với body JSON: `{"entityType": "CUSTOMER", "recordId": {cust_id}, "values": {"ma_so_thue": "0101234567"}}`.
  3. Gửi `GET /api/custom-fields/values?entity=CUSTOMER&recordId={cust_id}`.
- **Kết quả mong đợi:**
  - Bước 1: HTTP `201 Created`.
  - Bước 2: HTTP `200 OK`, lưu giá trị thành công.
  - Bước 3: HTTP `200 OK`, trả về đúng giá trị `"0101234567"`.
- **Cách xác minh dữ liệu:** `SELECT string_value FROM custom_field_values WHERE field_id = ... AND record_id = ...`.
- **Phương án khôi phục:** Xóa định nghĩa trường và giá trị đã tạo.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

#### `TC-E2E-CRM47-01`: Quy trình bán hàng (Pipeline), sắp xếp thứ tự và di chuyển cơ hội an toàn khi xóa giai đoạn
- **User Story:** CRM-47 (S2-09) — Pipeline bán hàng
- **Acceptance Criteria:** Tỷ lệ thắng 0-100%; cập nhật thứ tự giai đoạn qua `PUT /api/pipeline/stages/reorder`; xóa giai đoạn đang có cơ hội bắt buộc phải chỉ định `targetStageIdForMigration`, toàn bộ cơ hội được di chuyển sang giai đoạn mới trong cùng transaction.
- **Điều kiện chuẩn bị:** Giai đoạn `Stage 6` (ID: 6) có chứa 2 cơ hội đang hoạt động. Giai đoạn đích `Stage 2` (ID: 2).
- **Các bước thực hiện:**
  1. Gửi `PUT /api/pipeline/stages/reorder` với danh sách thứ tự mới: `{"pipelineId": 1, "stageIds": [2, 1, 3, 4, 5, 6]}`.
  2. Gửi `DELETE /api/pipeline/stages/6` KHÔNG truyền giai đoạn đích.
  3. Gửi `DELETE /api/pipeline/stages/6?targetStageIdForMigration=2`.
- **Kết quả mong đợi:**
  - Bước 1: HTTP `200 OK`, thứ tự sắp xếp cập nhật thành công.
  - Bước 2: HTTP `409 Conflict` (báo lỗi giai đoạn đang có cơ hội, không thể xóa trực tiếp).
  - Bước 3: HTTP `200 OK`, toàn bộ 2 cơ hội được chuyển sang Stage 2 và Stage 6 bị xóa khỏi hệ thống.
- **Cách xác minh dữ liệu:** `SELECT stage_id FROM opportunities WHERE stage_id = 6` trả về 0 bản ghi; `stage_id = 2` tăng thêm 2 bản ghi.
- **Phương án khôi phục:** Tạo lại giai đoạn kiểm thử nếu cần.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

#### `TC-E2E-CRM48-01`: Quản lý lý do Thắng/Thua và Đối thủ cạnh tranh
- **User Story:** CRM-48 (S2-10) — Win/Loss Reasons & Competitors
- **Acceptance Criteria:** CRUD danh mục lý do thành công/thất bại và đối thủ cạnh tranh; xóa có cơ chế chuyển ngừng kích hoạt (`DEACTIVATED`) nếu có tham chiếu.
- **Điều kiện chuẩn bị:** Đăng nhập bằng `admin`.
- **Các bước thực hiện:**
  1. Gửi `POST /api/winloss/reasons` tạo lý do mới: `{"reasonText": "Đối thủ chiết khấu cao hơn", "type": "LOSS", "displayOrder": 1, "active": true}`.
  2. Gửi `POST /api/winloss/competitors` tạo đối thủ mới: `{"name": "Đối thủ Staging X", "strengths": "Giá rẻ", "weaknesses": "Chất lượng kém", "displayOrder": 1, "active": true}`.
  3. Gửi `DELETE /api/winloss/reasons?id={reason_id}`.
  4. Gửi `DELETE /api/winloss/competitors?id={competitor_id}`.
- **Kết quả mong đợi:**
  - Bước 1 & 2: HTTP `201 Created`.
  - Bước 3 & 4: HTTP `200 OK`, trả về JSON chứa `outcome: "DELETED"` hoặc `"DEACTIVATED"`.
- **Cách xác minh dữ liệu:** `SELECT * FROM win_loss_reasons WHERE id = ...`.
- **Phương án khôi phục:** Không cần.
- **Trạng thái:** `NOT TESTED (Chờ thực thi TASK 04B)`.

---

## 3. Bảng tổng hợp trạng thái chuẩn bị nghiệm thu 20 User Story

| Nhóm chức năng | User Story | Trạng thái mã nguồn | Điều kiện kiểm thử trên Staging | Trạng thái kiểm thử |
|---|---|---|---|---|
| **Xác thực & Phiên** | S1-01 (CRM-21) | Triển khai đầy đủ | Cần tài khoản seed mẫu & RAM Guard | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| | S1-02 (CRM-22) | Triển khai đầy đủ | Kiểm tra timeout & CSRF | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| | S1-03 (CRM-23) | Triển khai đầy đủ | **Cần Mock SMTP (Port 2525)** | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| | S1-04 (CRM-24) | Triển khai đầy đủ | Kiểm tra thu hồi phiên đa trình duyệt | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| **Phân quyền & Menu** | S1-05 (CRM-25) | Triển khai đầy đủ | Cần seed dữ liệu 2 team & 3 user | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| | S1-06 (CRM-26) | Triển khai đầy đủ | Kiểm tra hiển thị menu theo vai trò | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| | S1-07 (CRM-27) | Triển khai đầy đủ | Kiểm tra mã lỗi 401, 403, 404, 500 | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| **Quản trị người dùng** | S1-08 (CRM-28) | Triển khai đầy đủ | Kiểm tra CRUD, phân trang và tìm kiếm | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| | S1-09 (CRM-29) | Triển khai đầy đủ | Kiểm tra gán đa vai trò, chặn hạ quyền Admin | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| | S1-10 (CRM-30) | Triển khai đầy đủ | Kiểm tra bàn giao dữ liệu bắt buộc & rollback | NOT TESTED (Chờ phê duyệt UI anti-enumeration) |
| **Hồ sơ & Đổi dữ liệu** | S2-01 (CRM-32) | Triển khai đầy đủ | Cần tệp Excel mẫu kiểm thử lỗi | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| | S2-02 (CRM-35) | Triển khai đầy đủ | Kiểm tra chặn sửa email/vai trò | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| | S2-03 (CRM-36) | Triển khai đầy đủ | Cần thư mục `CRM_AVATAR_DIR` staging | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| | S2-04 (CRM-37) | Triển khai đầy đủ | Kiểm tra đọc JSON & chặn POST/PUT/DELETE | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| **Cấu hình & Bán hàng** | S2-05 (CRM-39) | Triển khai đầy đủ | Kiểm tra che giá vốn & chặn xóa an toàn | NOT TESTED (Chờ phê duyệt quyền giá vốn Admin) |
| | S2-06 (CRM-42) | Triển khai đầy đủ | Kiểm tra cây tổ chức, DELETE trả 405 (GAP) | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| | S2-07 (CRM-44) | Triển khai đầy đủ | Kiểm tra danh mục dùng chung & thứ tự | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| | S2-08 (CRM-46) | Triển khai đầy đủ | CRUD định nghĩa & lưu giá trị thành công | NOT TESTED (Chờ phê duyệt tích hợp form JSP) |
| | S2-09 (CRM-47) | Triển khai đầy đủ | Kiểm tra reorder PUT & xóa migrate cơ hội | NOT TESTED (Chờ nghiệm thu TASK 04B) |
| | S2-10 (CRM-48) | Triển khai đầy đủ | CRUD lý do thắng/thua & đối thủ cạnh tranh | NOT TESTED (Chờ nghiệm thu TASK 04B) |
