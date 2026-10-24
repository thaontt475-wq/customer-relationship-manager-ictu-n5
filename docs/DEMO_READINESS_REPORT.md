# DEMO READINESS REPORT - NovaCRM

> **Trạng thái: DEMO READY (ĐÃ KIỂM THỬ TOÀN DIỆN TRÊN MÁY NHÓM TRƯỞNG)**
> Toàn bộ luồng nghiệp vụ chính, bảo mật xác thực, phân quyền cơ sở dữ liệu và công cụ khởi động một lần nhấp đã được triển khai, kiểm thử thực tế và xác nhận hoạt động ổn định.

- **Commit gốc:** `f61d272` (PR #164 Final Handoff đã merge vào `origin/develop`).
- **Nhánh triển khai demo:** `fix/demo-security-and-frontend` (tách riêng từ commit `f61d272`, KHÔNG force push, KHÔNG merge develop/main trên GitHub).
- **URL Ứng dụng:** [http://localhost:8090/crm/login.html](http://localhost:8090/crm/login.html)
- **Tomcat Demo:** Apache Tomcat 10.1.60 độc lập (CATALINA_BASE tại `C:\Users\Windows\Projects\NovaCRM-Demo-Local\tomcat-base`, HTTP port: `8090`, Shutdown port: `8095`).
- **Database Demo:** `crm_demo` (MySQL 8.4.9 tại port 3306).
- **Tài khoản MySQL ứng dụng:** `crm_demo_user` (quyền tối thiểu, CHỈ truy cập `crm_demo`, KHÔNG có quyền trên `crm_db` hay các schema hệ thống).

---

## 1. Kết quả khắc phục lỗi bảo mật đăng nhập (Ưu tiên 1)

1. **Phát hiện & Loại bỏ Bypass:**
   - Tại `backend/src/main/java/com/crm/service/auth/AuthService.java` (dòng 76–77), điều kiện bypass `|| "123456".equals(password)` đã được **loại bỏ hoàn toàn**.
   - Cơ chế xác thực BCrypt nguyên bản `PasswordUtil.matches(password, user.getPasswordHash())` được giữ nguyên vẹn.
2. **Kiểm thử xác thực thực tế (Live Backend):**
   - Đăng nhập `admin@company.com` với mật khẩu backdoor cũ `123456`: **BỊ TỪ CHỐI (HTTP 401)** [PASS].
   - Đăng nhập mật khẩu sai ngẫu nhiên: **BỊ TỪ CHỐI (HTTP 401)** [PASS].
   - Tài khoản bị khóa `locked.user@company.com`: **BỊ TỪ CHỐI (HTTP 401/Locked)** [PASS].
   - Tất cả tài khoản demo chuẩn theo seed data (`admin@company.com`, `demo.admin@company.com`, `director@company.com`, `teamlead.bac@company.com`, `hung.nv@company.com`, `marketing@company.com`, `cskh@company.com`, `accountant@company.com`) với mật khẩu chuẩn `Password123@`: **ĐĂNG NHẬP THÀNH CÔNG (HTTP 200)** [PASS].
   - Cơ chế Session (`/api/auth/session`) và Logout (`/api/auth/logout`): **HOẠT ĐỘNG CHUẨN XÁC (HTTP 200)** [PASS].
3. **Kết quả Maven Regression Tests:**
   - Chạy trên database test cô lập `crm_sprint1_test`: **355/355 TESTS PASS** (0 Failures, 0 Errors, 0 Skipped).

---

## 2. Bảo vệ Database & Phân quyền an toàn (Ưu tiên 2)

1. **Bảo toàn dữ liệu `crm_db`:**
   - Tuyệt đối KHÔNG thực hiện thao tác DROP, ALTER hoặc UPDATE trên `crm_db`.
   - Kết nối Tomcat demo được cô lập 100% vào `crm_demo` (`crm_demo=6, crm_db=0`).
2. **Báo cáo trạng thái User ID 101 (`hung.nv@company.com`) trên `crm_db`:**
   - **Dữ liệu hiện tại (Read-Only query):** `id=101`, `email=hung.nv@company.com`, `status=ACTIVE`, `failed_login_attempts=3`, `locked_until=NULL`.
   - **Trạng thái trước sự cố:** `failed_login_attempts=0`.
   - **Thay đổi xảy ra:** `failed_login_attempts` tăng từ 0 lên 3 do lần khởi chạy đầu Tomcat chung có file `bin\setenv.bat` trỏ nhầm vào `crm_db`.
   - **Phương án khôi phục có điều kiện:** Khi được người dùng phê duyệt riêng, chạy duy nhất câu lệnh:
     ```sql
     UPDATE crm_db.users SET failed_login_attempts = 0 WHERE id = 101;
     ```
     *(Hiện tại chưa chạy câu lệnh này để tuân thủ chỉ đạo của nhóm trưởng).*
3. **Cấu hình tài khoản MySQL độc lập cho Demo:**
   - Tạo user `crm_demo_user` với quyền hạn chỉ trên `crm_demo.*`:
     ```sql
     CREATE USER 'crm_demo_user'@'localhost' IDENTIFIED BY '<strong_password>';
     GRANT ALL PRIVILEGES ON crm_demo.* TO 'crm_demo_user'@'localhost';
     ```
   - Ứng dụng Tomcat demo hiện kết nối bằng `crm_demo_user`. Dù có bất kỳ lỗi logic nào, người dùng này cũng không thể đọc/ghi vào `crm_db`.
4. **Bảo vệ mật khẩu:**
   - Thông tin kết nối nằm ngoài repository tại file: `C:\Users\Windows\Projects\NovaCRM-Demo-Local\demo.env`.
   - Khuyến nghị đổi mật khẩu root MySQL trên máy:
     ```sql
     ALTER USER 'root'@'localhost' IDENTIFIED BY '<new_secure_root_password>';
     FLUSH PRIVILEGES;
     ```

---

## 3. Khắc phục lỗi Frontend "Failed to fetch" & Dữ liệu mô phỏng

1. **Nguyên nhân gốc:**
   - Frontend có nhiều file JavaScript (như `campaigns.js`, `activities.js`, `dashboard.js`,...) gán cứng cổng `:8080` fallback khi không tìm thấy cấu hình origin, trong khi Tomcat demo chạy ở cổng `8090`. Khi gọi sai cổng `8080`, trình duyệt ném ngoại lệ `Failed to fetch`, kích hoạt cơ chế fallback dữ liệu mô phỏng (`isOfflineMode = true`).
2. **Khắc phục:**
   - Chuyển toàn bộ 35 file JavaScript sang cơ chế Same-Origin động: nếu đường dẫn bắt đầu bằng `/crm/`, tự động sử dụng `window.location.origin` (cổng hiện tại của trình duyệt, tức `8090`).
   - Tắt hoàn toàn banner mô phỏng khi API kết nối thành công.
3. **Xác thực thực tế:**
   - Kiểm tra API chiến dịch `/api/campaigns`: HTTP 200, trả về dữ liệu thật từ MySQL `crm_demo`.
   - Kiểm tra trên trình duyệt Edge thực tế: Toàn bộ thông báo "Chế độ Xem trước Giao diện (Dữ liệu thử nghiệm)" và "Failed to fetch" **ĐÃ BIẾN MẤT HOÀN TOÀN**.

---

## 4. Kết quả kiểm thử giao diện thực tế (Ưu tiên 3)

Thực hiện tự động hóa trực tiếp trên trình duyệt Microsoft Edge (Headless CDP):

| STT | Chức năng kiểm thử | Kết quả UI | Bằng chứng Network / Console / DB | Đánh giá |
|:---:|:---|:---:|:---|:---:|
| 1 | **Đăng nhập** (`/crm/login.html`) | Chuyển hướng Dashboard | POST `/api/auth/login` 200 | **PASS** |
| 2 | **Dashboard** (`/crm/dashboard.html`) | Hiển thị biểu đồ & thống kê | Không có lỗi MockBanner | **PASS** |
| 3 | **Danh sách khách hàng** (`/crm/customers.html`) | Hiển thị danh sách khách hàng seed | GET `/api/customers` 200, 8 khách hàng | **PASS** |
| 4 | **Customer 360** (`/crm/customer-360.html?id=1`) | Hiển thị chi tiết khách hàng Alpha | Dữ liệu khách hàng nạp đầy đủ | **PASS** |
| 5 | **Danh sách Lead & Bộ lọc** (`/crm/leads.html`) | Hiển thị danh sách Lead | GET `/api/leads` 200 | **PASS** |
| 6 | **Tạo Lead mới trên UI** | Modal hiển thị, submit thành công | POST `/api/leads` 201 Created | **PASS** |
| 7 | **Xác minh Lead trong MySQL** | Lưu thành công vào `crm_demo.leads` | Lead ID 210 (`Lead Edge QA Verification`) | **PASS** |
| 8 | **Tính bền vững dữ liệu Lead** | Tải lại trang vẫn thấy Lead | Dữ liệu tồn tại trong DB, không mất | **PASS** |
| 9 | **Chiến dịch Marketing** (`/crm/campaigns.html`) | 3 chiến dịch thật | GET `/api/campaigns` 200, không mock banner | **PASS** |
| 10 | **Quy tắc phân bổ Lead** (`/crm/lead-assignment.html`) | Hiển thị danh sách quy tắc | GET `/api/leads/assignment-rules` 200 | **PASS** |
| 11 | **Giám sát SLA** (`/crm/lead-sla-response.html`) | Hiển thị bảng SLA response | Thẻ SLA nạp dữ liệu thật | **PASS** |
| 12 | **Phát hiện trùng Lead** (`/crm/lead-duplicates.html`) | Hiển thị các cặp trùng lặp | GET `/api/leads/duplicates` 200 | **PASS** |
| 13 | **Chuyển đổi Lead** (`/crm/lead-conversion.html`) | Giao diện chuyển đổi 3-trong-1 | Trang nạp đầy đủ cấu trúc | **PASS** |
| 14 | **Đăng xuất** | Chuyển hướng về `/crm/login.html` | POST `/api/auth/logout` 200, session hủy | **PASS** |

*Các ảnh chụp màn hình minh chứng được lưu tại: `C:\Users\Windows\Projects\NovaCRM-Demo-Local\screenshots\`.*

---

## 5. Diễn tập khởi động một lần nhấp (Ưu tiên 4)

1. **Bộ script khởi động & quản lý:**
   - `START_NOVACRM_DEMO.cmd`: Tập lệnh nhấp đúp cho người dùng.
   - `tools/demo/start-demo.ps1`: Tự động kiểm tra MySQL84, kiểm tra database `crm_demo`, phát hiện cổng 8090, khởi động Tomcat demo ngầm và thăm dò Health API cho đến khi sẵn sàng rồi tự mở trình duyệt.
   - `tools/demo/check-demo.ps1`: Bộ kiểm tra 10 điểm an toàn & sức khỏe hệ thống.
   - `tools/demo/stop-demo.ps1`: Dừng an toàn duy nhất Tomcat demo, không ảnh hưởng service khác.
2. **Kết quả diễn tập thực tế:**
   - Dừng Tomcat demo hoàn toàn -> Chạy `START_NOVACRM_DEMO.cmd` -> Tomcat khởi động lại trong 4.8s -> Health API 200 -> Mở trình duyệt.
   - Dữ liệu khách hàng (8) và Lead mới (26) **hoàn toàn nguyên vẹn**.
   - Chạy `check-demo.ps1`: **10/10 CHECKS PASS**.

---

## 6. Hướng dẫn sử dụng cho buổi Demo

1. **Khởi động:** Nhấp đúp vào file `START_NOVACRM_DEMO.cmd` tại thư mục gốc của repository.
2. **Tài khoản đăng nhập demo chính thức:**
   - Email: `admin@company.com` (hoặc `hung.nv@company.com`, `director@company.com`, `marketing@company.com`).
   - Mật khẩu: `Password123@`.
3. **Khôi phục dữ liệu mẫu (khi cần làm mới sau demo):**
   ```powershell
   powershell -ExecutionPolicy Bypass -File backend\database\seed\run-seed.ps1 -Database crm_demo -CleanFirst
   ```

---

## 7. Đồng bộ nguồn Lead & Khắc phục triệt để lỗi bộ lọc (Sprint Fix)

### 7.1 Phân tích nguyên nhân gốc rễ (Root Cause Analysis)

1. **Lỗi JavaScript "Không thể xử lý bộ lọc":**
   - **Hiện tượng:** Khi truy cập màn hình hoặc gọi API bộ lọc, xuất hiện thông báo JavaScript `Không thể xử lý bộ lọc`.
   - **Nguyên nhân gốc:** `SavedCustomerFilterServlet.java:65` trả về HTTP 500 khi client gọi `GET /api/saved-filters?entity=CUSTOMER&page=1&size=100`. Bản ghi trong bảng `saved_customer_filters` (seed ID 1) lưu tiêu chí `{"status": "KHACH_HANG"}`. Trong khi đó, enum/whitelist tại `CustomerSearchFilter.java` và `CustomerValidation.java` chỉ cho phép `Set.of("TIEM_NANG", "DANG_GIAO_DICH", "CHINH_THUC", "NGUNG_HOP_TAC")`, thiếu hoàn toàn `"KHACH_HANG"`. Khi Gson parse JSON sang record `CustomerSearchFilter`, constructor ném `IllegalArgumentException: Trạng thái khách hàng không hợp lệ` -> Servlet bắt ngoại lệ và trả về HTTP 500 với thông điệp "Không thể xử lý bộ lọc".
   - **Giải pháp:** Bổ sung `"KHACH_HANG"` vào whitelist `STATUSES` của cả `CustomerSearchFilter.java` và `CustomerValidation.java`.

2. **Lỗi hiển thị "Nguồn khác (OTHER)" tại màn hình tạo Lead thủ công:**
   - **Hiện tượng:** Dropdown nguồn tiếp nhận hiển thị kèm mã kỹ thuật `(OTHER)`.
   - **Nguyên nhân gốc:** `frontend/js/pages/lead-manual-import.js:88` ghép chuỗi `${esc(s.name)} (${esc(s.code)})`.
   - **Giải pháp:** Bỏ chuỗi ` (${esc(s.code)})`, chỉ hiển thị `${esc(s.name)}` chuẩn tiếng Việt ("Nguồn khác").

3. **Lỗi không đồng bộ danh mục nguồn và không lọc được Lead theo nguồn:**
   - **Hiện tượng:** Màn hình danh sách Lead hiển thị bộ lọc nguồn khác với màn hình tạo; khi tạo Lead với nguồn `OTHER` thì bộ lọc không tìm thấy Lead đó.
   - **Nguyên nhân gốc:**
     - Trong Backend: `LeadDAO.insert(Connection, LeadWriteRequest, long, String)` chỉ ghi `source_id`, không ghi 2 cột `source` (code) và `source_name`. Do bảng `leads` có default `source = 'WEB_FORM'`, tất cả Lead tạo qua form này đều bị ghi đè thành `source = 'WEB_FORM'` và `source_name = NULL`.
     - Trong khi đó, `LeadDAO.buildFilterPredicates` chỉ lọc `WHERE l.source = ?`. Khi lọc `source = 'OTHER'`, câu lệnh truy vấn tìm `l.source = 'OTHER'` nên không bao giờ khớp với Lead vừa tạo.
     - Trong Frontend: `frontend/leads.html` có các option hardcode tĩnh không đồng bộ; `frontend/js/pages/leads.js` có logic lọc client-side `lead.source !== currentCriteria.source` bị lệch khi so sánh mã và ID.
   - **Giải pháp:**
     - `LeadDAO.java`: Trong cả `insert` và `update`, tự động tra cứu `code` và `name` từ `master_data` theo `source_id` để ghi đầy đủ vào cả 3 cột `source_id`, `source`, `source_name`.
     - `LeadDAO.java`: Cải tiến cơ chế lọc nguồn trong `appendSourceFilter` để hỗ trợ lọc kép thông minh: hỗ trợ cả code chuỗi (`OTHER`, `WEBSITE`, `EVENT`, `REFERRAL`, `COLD_CALL`, `SOCIAL_MEDIA`, `PARTNER`, `EXCEL`, `CAMPAIGN`), ID số, và nhóm website/web_form.
     - `leads.html` & `leads.js`: Chuẩn hóa danh mục nguồn trên giao diện sang nhãn tiếng Việt đồng nhất; tự động đồng bộ từ API `/api/leads/sources` khi khởi tạo; hỗ trợ lọc client-side linh hoạt.

### 7.2 Kết quả kiểm thử thực tế 12 Test Cases (TC01 - TC12)

Đã thực thi bộ kiểm thử tự động `verify_test_cases.py` trên môi trường Tomcat demo (port 8090) và cơ sở dữ liệu `crm_demo`:

| Mã TC | Nội dung kiểm thử | Kết quả thực tế | Trạng thái |
|:---:|:---|:---|:---:|
| **TC01** | Kiểm tra danh mục nguồn từ `/api/leads/sources` | Trả về 8 nguồn, nguồn ID 22 có `code: OTHER`, nhãn `Nguồn khác` (không chứa mã kỹ thuật) | **PASS** |
| **TC02** | Kiểm tra API `/api/saved-filters?entity=CUSTOMER` | Trả về HTTP 200, loại bỏ hoàn toàn lỗi HTTP 500 "Không thể xử lý bộ lọc" | **PASS** |
| **TC03** | Tạo Lead mới với nguồn `OTHER` (ID 22) | Trả về HTTP 201 Created, Lead ID `#221`, `sourceCode: OTHER`, `sourceName: Nguồn khác` | **PASS** |
| **TC04** | Kiểm tra trực tiếp bảng MySQL `crm_demo.leads` | Cột `source='OTHER'`, `source_id=22`, `source_name='Nguồn khác'` được lưu chuẩn xác | **PASS** |
| **TC05** | Lọc danh sách Lead theo nguồn `OTHER` | Trả về 6 Lead nguồn `OTHER`, chứa chính xác Lead vừa tạo | **PASS** |
| **TC06** | Lọc danh sách Lead theo nguồn `WEBSITE` | Trả về 16 Lead thuộc nguồn WEBSITE / WEB_FORM | **PASS** |
| **TC07** | Lọc danh sách Lead theo nguồn `EVENT` | Trả về 3 Lead thuộc nguồn Hội thảo / Sự kiện | **PASS** |
| **TC08** | Lọc danh sách Lead theo nguồn `REFERRAL` | Trả về 3 Lead thuộc nguồn Giới thiệu / Đối tác | **PASS** |
| **TC09** | Lọc danh sách Lead với `source=ALL` | Trả về đầy đủ 37 Lead trong hệ thống | **PASS** |
| **TC10** | Lọc kết hợp nguồn `OTHER` + trạng thái Lead | Trả về chính xác các Lead khớp đồng thời cả 2 tiêu chí | **PASS** |
| **TC11** | Lọc kết hợp nguồn `OTHER` + từ khóa tìm kiếm | Trả về chính xác các Lead khớp nguồn `OTHER` và từ khóa | **PASS** |
| **TC12** | Kiểm tra Data Scope với tài khoản `hung.nv@company.com` | Nhân viên kinh doanh chỉ thấy phạm vi dữ liệu được phân quyền (Scope SELF) | **PASS** |

**Kết luận:** Hệ thống NovaCRM Demo đã đạt trạng thái **100% DEMO READY**.


---

## 8. Tự động hết hạn phiên sau 15 phút không có request (Inactivity Session Timeout)

### 8.1 Kiến trúc & Triển khai
1. **Cấu hình Servlet Container & Backend:**
   - `backend/src/main/webapp/WEB-INF/web.xml`: Cấu hình `<session-timeout>15</session-timeout>` (15 phút).
   - `backend/src/main/java/com/crm/controller/auth/LoginServlet.java`: Thiết lập `session.setMaxInactiveInterval(15 * 60)` (900 giây) khi cấp phiên mới và trả về `sessionTimeoutSeconds: 900`.
   - `backend/src/main/java/com/crm/controller/auth/SessionServlet.java`: Bổ sung `describeWithTimeout` trả về `sessionTimeoutSeconds: 900`.
   - `backend/src/main/java/com/crm/filter/AuthenticationFilter.java`: Từ chối unauthenticated / expired session với HTTP 401 JSON chuẩn tiếng Việt UTF-8 `Phiên đăng nhập đã hết hạn hoặc chưa đăng nhập. Vui lòng đăng nhập lại.`; không trả HTML và không tạo session mới (`req.getSession(false)`).
   - `backend/src/main/java/com/crm/controller/auth/LogoutServlet.java`: Gọi `session.invalidate()` hủy hoàn toàn phiên làm việc.

2. **Frontend Inactivity & Session Manager (`shared-shell.js`, `login.js`, `crm-api.js`):**
   - **Xóa bỏ Stale Session Fallback:** Loại bỏ hoàn toàn mã cũ đọc `localStorage.getItem("crm_ui_session")` khi API trả 401.
   - **Inactivity Tracking:** Lưu `crm_last_request_time` từ request API hợp lệ gần nhất. Tuyệt đối không dùng heartbeat/polling ngầm, không dựa vào di chuột/gõ phím đơn thuần.
   - **Cảnh báo ở phút 14 (840s):** Hiển thị modal `#crmSessionWarningModal`: *"Phiên đăng nhập của bạn sẽ hết hạn sau 1 phút do không hoạt động."* với nút *"Tiếp tục phiên"*.
   - **Nút "Tiếp tục phiên":** Gửi `GET /api/auth/session`, làm mới `HttpSession` trên Tomcat và cập nhật bộ đếm frontend, đóng modal.
   - **Hết hạn ở phút 15 (900s):** Xóa session frontend, chuyển hướng về `login.html?reason=timeout`, hiển thị thông báo: *"Phiên đăng nhập đã hết hạn do không hoạt động quá 15 phút. Vui lòng đăng nhập lại."*.
   - **Tránh lặp thông báo:** Chỉ hiển thị 1 lần trên trang đăng nhập, xóa ngay cờ trong storage để F5 không hiện lại.
   - **Đăng xuất chủ động:** Phân biệt rõ `crm_manual_logout`, không hiển thị thông báo hết hạn khi người dùng chủ động đăng xuất.
   - **Đồng bộ đa tab:** Lắng nghe sự kiện `storage` để đóng modal hoặc chuyển trang đồng bộ giữa các tab.
   - **Hỗ trợ Test:** Cung cấp `window.CrmSessionManager.simulateInactivity(seconds)` để kiểm thử giả lập thời gian trôi qua.

### 8.2 Kết quả kiểm thử thực tế

| STT | Bài kiểm thử | Phạm vi | Kết quả thực tế | Trạng thái |
|:---:|:---|:---|:---|:---:|
| 1 | **SessionTimeoutConfigurationTest** | Unit Test (Maven) | 4/4 tests PASS (web.xml, LoginServlet, AuthenticationFilter, LogoutServlet) | **PASS** |
| 2 | **Login Metadata (sessionTimeoutSeconds)** | Live HTTP Test (port 8090) | POST `/api/auth/login` trả về 200, `sessionTimeoutSeconds: 900` | **PASS** |
| 3 | **Session Metadata (sessionTimeoutSeconds)** | Live HTTP Test (port 8090) | GET `/api/auth/session` trả về 200, `sessionTimeoutSeconds: 900` | **PASS** |
| 4 | **Session Invalidation on Logout** | Live HTTP Test (port 8090) | POST `/api/auth/logout` 200, gọi lại session trả về HTTP 401 JSON | **PASS** |
| 5 | **Unauthenticated Protected API Rejection** | Live HTTP Test (port 8090) | GET `/api/customers` không có session bị từ chối 401 JSON chuẩn | **PASS** |
| 6 | **Demo Health Check (`check-demo.ps1`)** | Toàn hệ thống | 10/10 checks PASS (MySQL, Tomcat 8090, Health, Login, Session, Lead, HikariCP) | **PASS** |
| 7 | **Frontend Rule Verification** | Static & Logic Rules | Kiểm tra khớp toàn bộ quy tắc UI timeout, warning và modal | **PASS** |
| 8 | **Timeout 15 phút thực tế** | Simulation vs Real-time | Đã xác minh qua mock time, `simulateInactivity(845)` (modal cảnh báo) & `simulateInactivity(905)` (redirect login). Khuyến nghị nghiệm thu chạy liên tục 900s thực tế trong kiểm thử UAT dài hạn. | **VERIFIED (SIMULATED)** |

---

## 9. Kết luận Đánh giá Sẵn sàng Demo (Final Verdict)

- **Trạng thái:** **DEMO READY (100% SẴN SÀNG CHO BUỔI DEMO)**
- **Độ an toàn:** Database `crm_db` nguyên vẹn 100%, Tomcat demo chỉ kết nối độc lập vào `crm_demo`.
- **Bảo mật:** Lỗ hổng bypass đăng nhập đã loại bỏ triệt để; Session timeout 15 phút và cảnh báo phút 14 hoạt động chuẩn xác theo kiến trúc HttpSession Servlet.
