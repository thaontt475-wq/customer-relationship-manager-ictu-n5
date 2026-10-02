# BÁO CÁO PHÂN TÍCH THIẾU HỤT & ĐỐI CHIẾU TIÊU CHÍ CHẤP NHẬN (API GAP ANALYSIS)

> Phiên bản: 1.2 (TASK 03 — Final Validation)
> Repository: `customer-relationship-manager-ictu-n5`
> Baseline commit: `b61bee548e07fe5c33041a7cbc2e392d7c58744e`
> Trạng thái dừng: **WAITING_FOR_APPROVAL**

---

## 1. Tóm tắt tình trạng và nguyên tắc đánh giá

Theo yêu cầu chuẩn hóa trong TASK 03, việc đánh giá toàn bộ 20 User Story thuộc Sprint 1 và Sprint 2 được thực hiện trên nguyên tắc độc lập, khách quan, dựa trên bằng chứng kiểm thử thực tế và mã nguồn hiện hữu. Không sử dụng báo cáo QA cũ để tự động coi các tiêu chí chấp nhận (Acceptance Criteria - AC) là đạt.

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
> Theo chỉ đạo nghiêm ngặt của người dùng tại mục 3, **chúng tôi trân trọng đề nghị người dùng cung cấp lại bảng Acceptance Criteria gốc nguyên văn** trước khi đưa ra kết luận nghiệm thu cuối cùng.

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

## 3. Ma trận đối chiếu chi tiết 20 User Story

| Story ID | Acceptance Criteria | Mã nguồn liên quan | Kiểm thử hiện có | Trạng thái | Việc còn thiếu / Bất đồng kỹ thuật |
|---|---|---|---|---|---|
| **S1-01** (CRM-21) | Đăng nhập đúng thông tin, hỗ trợ đa vai trò, khóa tạm thời 15 phút khi sai 5 lần liên tiếp | `LoginServlet.java`, `AuthService.java`, `LoginAttemptGuard.java`, `UserDAO.java` | `LoginServletTest`, `LoginAttemptGuardTest`, `CRM21LoginCheck` | **TESTED** | Chưa nghiệm thu E2E UI chuyển hướng theo vai trò; `LoginAttemptGuard` lưu tạm trong RAM máy chủ (chưa phân tán qua DB/Redis). |
| **S1-02** (CRM-22) | Đăng xuất vô hiệu hóa session, dọn dẹp SessionRegistry, timeout 30 phút, bảo vệ CSRF | `LogoutServlet.java`, `SessionServlet.java`, `CsrfFilter.java`, `SessionRegistry.java` | `CsrfFilterTest`, `SessionRegistryTest`, kiểm thử HTTP logout | **AC PASS (tạm thời)** | Cần kiểm chứng timeout 30 phút trên môi trường Tomcat staging thực tế. |
| **S1-03** (CRM-23) | Yêu cầu reset pass qua email, token hiệu lực 30 phút, dùng 1 lần, chống email enumeration | `AuthService.java`, `PasswordResetTokenDAO.java`, `EmailService.java`, `ResetTokenUtil.java` | `ForgotPasswordAcceptanceIntegrationTest`, `ResetTokenUtilTest` | **PARTIAL** | Chưa kiểm thử gửi email thực tế ra máy chủ SMTP thật; cần cấu hình biến môi trường `CRM_SMTP_*`. |
| **S1-04** (CRM-24) | Đổi mật khẩu yêu cầu pass cũ, kiểm tra chính sách độ mạnh, thu hồi tất cả các phiên khác | `ChangePasswordServlet.java`, `AuthService.java`, `SessionRegistry.java`, `PasswordUtil.java` | `PasswordUtilTest`, `SessionRegistryTest` | **TESTED** | Chưa có kiểm thử E2E tự động xác nhận các phiên đăng nhập khác trên trình duyệt bị đăng xuất ngay lập tức. |
| **S1-05** (CRM-25) | Phân quyền SELF/TEAM/ALL trên 4 thực thể; xem danh sách, chi tiết và xuất Excel | `ScopedEntityServlet.java`, `DataScopeService.java`, `ScopedEntityDAO.java`, `ExcelService.java` | `ScopedEntityServletTest`, `DataScopeServiceTest`, `ScopeAccessPolicyTest`, HTTP Excel check | **PARTIAL** | TEAM hiện chỉ so khớp cùng `team_id`, chưa hỗ trợ cây nhóm con; phân trang HTML đang cắt danh sách tại tầng Service. |
| **S1-06** (CRM-26) | Menu động theo vai trò; hiển thị đúng quyền; thông tin tài khoản | `MenuNavigationFilter.java`, `MenuService.java`, `MenuDAO.java`, `sidebar.jsp` | `MenuServiceTest`, `MenuNavigationFilterTest`, `MenuServletTest` | **PARTIAL** | Tuyến đường `/quotes` đang hoạt động bình thường. Chỉ có 3 mục `/leads`, `/kpi`, `/automation` chưa có trang riêng, đề xuất ẩn khỏi menu người dùng. |
| **S1-07** (CRM-27) | Hiển thị trang lỗi 401, 403, 404, 500 với đúng mã HTTP status | `ErrorPageServlet.java`, `web.xml`, `jsp/errors/*.jsp` | Kiểm tra HTTP trả về đúng status 401, 403, 404, 500 | **AC PASS (tạm thời)** | Cần kiểm tra giao diện hiển thị trên các độ phân giải màn hình khác nhau (responsive). |
| **S1-08** (CRM-28) | Quản lý tài khoản: danh sách, tạo mới, chỉnh sửa, tìm kiếm, lọc, phân trang | `UserServlet.java`, `UserService.java`, `UserDAO.java`, `user-list.jsp` | `UserServletTest`, kiểm tra HTTP `user-list.jsp` | **PARTIAL** | Biểu mẫu `user-create.jsp` và `user-edit.jsp` hiện là placeholder tối giản, chưa có giao diện nhập liệu hoàn chỉnh. Cần gói công việc FE riêng. |
| **S1-09** (CRM-29) | Hỗ trợ đa vai trò; trưởng nhóm bắt buộc gắn team; chặn tự thu hồi quyền Admin | `PermissionServlet.java`, `PermissionService.java`, `UserRoleService.java`, `UserRoleDAO.java` | `UserRoleServiceTest`, kiểm tra HTTP trang quyền | **TESTED** | Cần bổ sung kiểm thử E2E thu hồi ngay lập tức phiên đăng nhập của người dùng khi bị thay đổi vai trò. |
| **S1-10** (CRM-30) | Khóa tài khoản, bắt buộc bàn giao dữ liệu (khách hàng, cơ hội), ghi log, thu hồi session | `UserService.java`, `OwnershipTransferService.java`, `UserLockHandoverDAO.java`, `SessionRegistry.java` | Kiểm tra mã nguồn giao dịch JDBC, test đơn vị dịch vụ | **NEEDS APPROVAL** | Cần phê duyệt phương án hiển thị thông báo tài khoản bị khóa để không vi phạm nguyên tắc chống tiết lộ thông tin tài khoản (anti-enumeration); kiểm chứng bàn giao trên DB staging. |
| **S2-01** (CRM-32) | Nhập người dùng từ Excel: tải mẫu, kiểm tra dữ liệu, bỏ qua dòng lỗi, lưu lô và báo cáo | `UserImportServlet.java`, `ExcelService.java`, `UserImportDAO.java`, `user-import.jsp` | `ExcelServiceTest`, kiểm tra xử lý workbook | **TESTED** | Chưa có kiểm thử E2E tải lên tệp multipart lớn và tải xuống báo cáo tổng kết lỗi. |
| **S2-02** (CRM-35) | Xem và cập nhật hồ sơ cá nhân; cập nhật số điện thoại; chặn sửa email và vai trò | `ProfileServlet.java`, `ProfileService.java`, `UserDAO.java`, `profile.jsp` | `ProfileServiceTest`, kiểm tra HTTP trang profile | **AC PASS (tạm thời)** | Cần bổ sung kiểm thử E2E tự động trên trình duyệt xác nhận các trường bị khóa không thể bị ghi đè qua DevTools. |
| **S2-03** (CRM-36) | Tải lên ảnh đại diện JPG/PNG tối đa 2MB; tự động crop vuông 512x512 và sinh thumbnail 128x128 | `AvatarServlet.java`, `AvatarService.java`, `AvatarImageProcessor.java`, `avatar.jsp` | `AvatarServiceTest`, `AvatarImageProcessorTest` | **TESTED** | AC gốc không quy định kích thước pixel cụ thể; kích thước trong mã là 512x512 và 128x128. Chưa thực hiện kiểm thử HTTP/E2E tải ảnh trên máy chủ Tomcat thật (`CRM36AvatarHttpCheck`). |
| **S2-04** (CRM-37) | Nhật ký kiểm toán before/after JSON; bộ lọc theo user, action, thời gian | `AuditLogServlet.java`, `AuditLogService.java`, `AuditLogDAO.java`, `audit-log.jsp` | `AuditLogServiceTest`, `AuditLogDAOTest` | **PARTIAL** | Phương thức ghi nhật ký chiết khấu (`DISCOUNT_CHANGED`) và chỉ tiêu (`TARGET_CHANGED`) chưa được gọi từ các luồng nghiệp vụ thực tế. |
| **S2-05** (CRM-39) | Danh mục sản phẩm: bảo vệ giá vốn (chỉ Director/Admin), giá niêm yết >= sàn, chặn xóa an toàn | `ProductServlet.java`, `ProductPageServlet.java`, `ProductService.java`, `ProductDAO.java` | `ProductServiceTest`, `ProductServletAuthorizationTest` | **TESTED** | AC yêu cầu chỉ "Giám đốc kinh doanh" xem/sửa giá vốn, nhưng mã nguồn hiện cho phép cả Admin và Director. Cần phê duyệt GAP phân quyền này; chưa kiểm thử E2E xóa sản phẩm có liên kết giao dịch thật. |
| **S2-06** (CRM-42) | Cây cơ cấu tổ chức nhóm kinh doanh, phân cấp cha-con, chỉ định trưởng nhóm và khu vực | `OrganizationServlet.java`, `OrganizationService.java`, `OrganizationDAO.java`, `organization.jsp` | `OrganizationServiceTest`, `OrganizationServletTest` | **PARTIAL** | Phân quyền phạm vi TEAM hiện chỉ giới hạn trong cùng `team_id`, chưa mở rộng ra cây nhóm con. |
| **S2-07** (CRM-44) | Danh mục dùng chung: CRUD danh mục, quản lý thứ tự sắp xếp, chặn xóa khi có tham chiếu | `CategoryServlet.java`, `CategoryService.java`, `CategoryDAO.java`, `configuration.jsp` | `CategoryServiceTest` | **AC PASS (tạm thời)** | Cần kiểm tra giao diện kéo thả sắp xếp thứ tự và kiểm chứng chặn xóa trên dữ liệu thực tế. |
| **S2-08** (CRM-46) | Quản lý trường tùy chỉnh (TEXT, NUMBER, DATE, SELECT) cho CUSTOMER và OPPORTUNITY | `CustomFieldServlet.java`, `CustomFieldService.java`, `CustomFieldDAO.java`, `custom-field-list.jsp` | `CustomFieldServiceTest`, `CustomFieldServletTest` | **NEEDS APPROVAL** | CRUD định nghĩa trường tùy chỉnh đã hoàn tất; tuy nhiên việc tích hợp các trường này vào Biểu mẫu nhập liệu, Bộ lọc và Xuất Excel của Khách hàng/Cơ hội chưa được triển khai. |
| **S2-09** (CRM-47) | Giai đoạn bán hàng (Pipeline Stages): thứ tự, xác suất thắng 0-100%, bảo toàn cơ hội khi xóa stage | `PipelineServlet.java`, `PipelineService.java`, `PipelineDAO.java`, `pipeline-config.jsp` | `PipelineServiceTest` | **TESTED** | Đã có logic `reassignOpportunities` trong transaction khi xóa giai đoạn; chưa có kiểm thử E2E trên cơ sở dữ liệu có sẵn cơ hội. |
| **S2-10** (CRM-48) | Danh mục lý do Thắng/Thua (WIN/LOSS) và Đối thủ cạnh tranh phục vụ phân tích bán hàng | `WinLossServlet.java`, `WinLossService.java`, `WinLossDAO.java`, `winloss.jsp` | `WinLossServiceTest`, `WinLossServletTest` | **AC PASS (tạm thời)** | Quản lý danh mục Master Data đã hoàn tất và kiểm thử đầy đủ. Việc tích hợp bắt buộc nhập lý do khi đóng cơ hội thuộc phạm vi Sprint 5. |

---

## 4. Phân tích chi tiết các điểm bất đồng kỹ thuật

### 4.1. CRM-36 (S2-03) — Kích thước xử lý ảnh đại diện (AvatarImageProcessor)
- **Kiểm tra mã nguồn thực tế:**
  - `AvatarImageProcessor.java` định nghĩa `MAX_BYTES = 2 * 1024 * 1024` (2 MiB), `MAX_PIXELS = 16_000_000L`.
  - Dòng 46-47:
    - `BufferedImage square = resizeSquare(decoded, 512);` -> Ảnh chính vuông có kích thước **512x512 pixel**.
    - `return new Images(square, resizeSquare(square, 128));` -> Ảnh thu nhỏ (thumbnail) có kích thước **128x128 pixel**.
- **Kết luận:** Tiêu chí chấp nhận gốc không quy định kích thước pixel cụ thể, mà chỉ yêu cầu ảnh vuông và ảnh thu nhỏ. Kích thước 512x512 và 128x128 là lựa chọn triển khai kỹ thuật hợp lý trong mã nguồn. Báo cáo trước đó ghi 200x200 và 48x48 là chưa chính xác so với mã nguồn thực tế và đã được sửa đổi toàn diện.

### 4.2. CRM-39 (S2-05) — Phân quyền giá vốn: "Giám đốc kinh doanh" vs Admin/Director
- **Yêu cầu AC gốc:** Chỉ "Giám đốc kinh doanh" (Sales Director) mới được xem và sửa giá vốn (`cost_price`).
- **Mã nguồn hiện tại:**
  - `ProductService.java` định nghĩa tập quyền: `DIRECTOR_ADMIN_ROLES = Set.of("admin", "director", "giám đốc", "giam doc", "quản trị viên", "quan tri vien")`.
  - Hàm `isDirectorOrAdmin(userRoles)` cấp quyền xem/sửa giá vốn cho cả `Admin` (Quản trị viên) và `Director` (Giám đốc chung).
- **Khoảng cách kỹ thuật (GAP):**
  - Mã nguồn cho phép Admin can thiệp giá vốn (vượt quá phạm vi "chỉ Giám đốc kinh doanh").
  - Hệ thống hiện chưa có vai trò riêng biệt `Sales Director` ("Giám đốc kinh doanh"), mà chỉ có vai trò `Director` (Giám đốc) và `Sales Manager` (Trưởng phòng kinh doanh).
  - Điểm này được ghi nhận vào nhóm **`NEEDS APPROVAL`** để người dùng quyết định: giữ nguyên quyền cho Admin hay thu hồi quyền giá vốn của Admin và bổ sung vai trò Giám đốc kinh doanh.

### 4.3. CRM-26 (S1-06) — Kiểm tra tuyến đường `/quotes`
- **Kiểm tra mã nguồn:**
  - Tuyến đường `/quotes` được ánh xạ trực tiếp trong `ScopedEntityPageServlet.java` (`@WebServlet({"/customers", "/opportunities", "/activities", "/quotes"})`), forward tới `/jsp/shared/scoped-records.jsp`.
  - API `/api/quotes` và `/api/quotes/*` được ánh xạ trong `ScopedEntityServlet.java`.
  - Tuyến đường này đã được kiểm thử HTTP 200 thành công trong `backend/tests/CrmHttpCheck.java`.
- **Kết luận:** Tuyến đường `/quotes` là một chức năng **đang hoạt động bình thường** trong phân hệ phân quyền dữ liệu. Tuyệt đối không được ẩn mục này khỏi menu hoặc coi là route thiếu. Chỉ có 3 mục `/leads`, `/kpi`, `/automation` là chưa có trang riêng.

### 4.4. CRM-30 (S1-10) — Tài khoản bị khóa và cơ chế chống rò rỉ thông tin tài khoản (Anti-Enumeration)
- **Kiểm tra mã nguồn:**
  - Trong `AuthService.login()`: khi tài khoản bị khóa (`!user.isActive() || !"ACTIVE".equals(user.getStatus())`), hệ thống trả về `null`.
  - `LoginServlet` hiển thị thông báo chung: `"Email hoặc mật khẩu không đúng"`.
- **Phân tích an toàn thông tin:**
  - Đây là cơ chế bảo mật tiêu chuẩn nhằm chống lại kiểu tấn công dò quét sự tồn tại và trạng thái tài khoản (Account Enumeration Attack). Nếu hệ thống hiển thị "Tài khoản của bạn đã bị khóa" cho một phiên chưa xác thực mật khẩu, kẻ tấn công bên ngoài có thể xác định được email nào tồn tại và đang bị khóa.
  - Việc hiển thị thông báo tài khoản bị khóa chỉ nên diễn ra sau khi người dùng đã nhập đúng mật khẩu, hoặc chỉ hiển thị trong màn hình quản trị nội bộ.
  - Không tự phê duyệt việc thay đổi thông báo công khai. Điểm này được giữ ở trạng thái **`NEEDS APPROVAL`**.

### 4.5. CRM-25 và CRM-42 (S1-05, S2-06) — Cây tổ chức và phạm vi dữ liệu TEAM
- **Đối chiếu Schema và Mã nguồn:**
  - Bảng `teams` có cột `parent_id` (tham chiếu `teams.id`) xác lập quan hệ cây cha-con; `leader_user_id` chỉ định người phụ trách; `region` xác định khu vực. Bảng `users` liên kết qua `team_id`.
  - `OrganizationService` và `OrganizationDAO` hỗ trợ quản lý cây tổ chức.
  - Tuy nhiên, trong `ScopeAccessPolicy.java` và `DataScopeHelper.java`, phạm vi `TEAM` chỉ lọc phẳng: `record.ownerTeamId() == actor.teamId()` hoặc `u.team_id = ?`.
- **Kết luận dựa trên AC, Schema và Mã nguồn:**
  - Phạm vi `TEAM` hiện tại được giới hạn chính xác trong cùng một nhóm trực tiếp (`team_id`).
  - Hệ thống chưa hỗ trợ cơ chế đệ quy để Trưởng nhóm cấp trên tự động truy cập dữ liệu của các nhóm con (`parent_id`). Đây là hiện trạng kỹ thuật của hệ thống, không tự suy diễn hành vi mở rộng khi chưa có quyết định phê duyệt chính thức.

---

## 5. Phân loại khối lượng công việc tiếp theo

1. **Frontend (FE):**
   - Xây dựng giao diện Form nhập liệu cho `user-create.jsp` và `user-edit.jsp` (CRM-28).
   - Tích hợp giao diện hiển thị trường tùy chỉnh động vào form khách hàng và cơ hội (CRM-46).
   - Ẩn 3 mục menu chưa có trang (`/leads`, `/kpi`, `/automation`) trên `sidebar.jsp` (CRM-26).
2. **Backend (BE):**
   - Thống nhất phương án xử lý thông báo tài khoản bị khóa sau khi xác thực mật khẩu (CRM-30).
   - Quyết định việc phân tách vai trò Giám đốc kinh doanh cho giá vốn (CRM-39).
   - Tích hợp đọc/ghi giá trị trường tùy chỉnh vào các dịch vụ Khách hàng và Cơ hội (CRM-46).
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
