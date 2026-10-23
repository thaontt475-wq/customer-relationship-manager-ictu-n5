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
