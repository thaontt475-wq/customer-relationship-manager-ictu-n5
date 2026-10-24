# BÁO CÁO KIỂM TOÁN VÀ NGHIỆM THU PHÁT HÀNH (FINAL RELEASE AUDIT)
**Dự án:** NovaCRM — Phân hệ Quản trị Quan hệ Khách hàng (Nhóm 5 - ICTU)  
**Thời điểm thực hiện:** 23/10/2026  
**Nhánh phát hành (Source Branch):** `release/final-handoff-20261023`  
**Nhánh đích PR (Target Branch):** `develop`  
**Môi trường kiểm thử:** Local QA / Windows 11 / JDK 21 / Apache Tomcat 10.1.60 / MySQL 8.4 LTS  

---

## 1. TỔNG QUAN KẾT QUẢ KIỂM THỬ THỰC TẾ

| Hạng mục | Công cụ / Kịch bản | Quy mô / Tiêu chí | Kết quả thực tế | Trạng thái |
| :--- | :--- | :--- | :--- | :--- |
| **Backend Unit & Regression** | Apache Maven Surefire | 355 test cases (S1 -> S4) | **355 PASS**, 0 fail, 0 error, 0 skip | **PASS** |
| **Frontend Automated Suite** | Node.js Test Runner | 9 test cases (Capture & Lead Modal) | **9 PASS**, 0 fail, 123.7ms | **PASS** |
| **Dữ liệu mẫu (Seed SQL)** | PowerShell `run-seed.ps1` | 8 scripts (01 -> 08), 14 tài khoản | **100% PASS**, 0 orphan records | **PASS** |
| **Tải cao (Load Test 50 VU)** | Grafana k6 (`leads-read.js`) | 50 VU, 60s, có xác thực | **3,000/3,000 checks PASS** (100%) | **PASS** |
| **Tỷ lệ lỗi HTTP tải cao** | k6 `http_req_failed` | Tiêu chí `< 1.00%` | **0.00% lỗi** (0 / 3,001 requests) | **PASS** |
| **Thời gian phản hồi P95** | k6 `p(95)` | Tiêu chí `< 200 ms` | **30.95 ms** (avg 9.49ms, med 5.05ms) | **PASS** |
| **Cạn kiệt Socket OS** | `netstat -ano (TIME_WAIT)` | Giảm tải dồn cổng 3306 | **0 socket TIME_WAIT** sau test | **PASS** |
| **Kiểm toán rò rỉ mã nguồn** | Gitleaks (all branches/tags) | 468 commits lịch sử Git | **0 leak phát hiện** (33.23 MB scanned) | **PASS** |
| **Kiểm toán Working Tree** | Gitleaks `dir` | Toàn bộ thư mục dự án | **0 leak phát hiện** (18.90 MB scanned) | **PASS** |

---

## 2. MINH CHỨNG & ĐƯỜNG DẪN LOG GỐC

Mọi số liệu kiểm thử đều được ghi nhận trực tiếp từ log thực thi, không sử dụng số liệu giả lập:

- **Log Maven Clean Verify:** `.local-qa/CRM-261-20261023/verify-final.log`
  - Gói artifact: `backend/target/crm.war`
  - Tổng số test: 355, Thời gian build: ~32 giây.
- **Log Frontend Test:**
  - Lệnh: `node --test frontend/tests/*.test.mjs`
  - 9 bài test bao phủ: double submit prevention, honeypot spam protection, 6 contract fields validation, error display, HTTP 400 recovery, non-loss status isolation.
- **Dữ liệu k6 50 VU có HikariCP:**
  - File JSON gốc: `.local-qa/CRM-261-20261023/k6-authenticated-50-hikari.json`
  - File Log chi tiết: `.local-qa/CRM-261-20261023/k6-authenticated-50-hikari.log`
  - Chi tiết chỉ số:
    - `checks`: 3,000 passes, 0 fails (tỷ lệ 100%).
    - `http_reqs`: 3,001 requests.
    - `http_req_duration`: avg=9.49ms, med=5.05ms, p(95)=30.95ms, p(99)=67.64ms.
    - `data_received`: 85,026,519 bytes (~85 MB).

---

## 3. GIẢI TRÌNH KỸ THUẬT & LÀM RÕ SỐ LIỆU

### 3.1. Phân biệt cấu trúc 3.000 checks PASS và 3.001 HTTP requests
Trong kịch bản `performance/leads-read.js`:
1. **Hàm `setup()` (1 request):** Chạy 1 lần duy nhất khi khởi tạo kịch bản để gửi `POST /api/auth/login` với tài khoản QA. Request này thiết lập phiên làm việc, lấy cookie `JSESSIONID` (`66508AF6B83DF15EF0C671AAAE4BD594`) và chia sẻ cho tất cả VU. Hàm `setup()` không gọi hàm `check()` của k6 mà ném Exception nếu status != 200.
2. **Hàm `default(session)` (3.000 requests):** 50 VU luân phiên gửi `GET /api/leads?size=10` có cookie phiên trong 60 giây (giới hạn 60 lượt/VU theo công thức `3000/vus`). Mỗi lượt kiểm tra điều kiện `status === 200 && json.success === true` thông qua 1 hàm `check()`.
-> **Kết quả:** Tổng cộng 1 request đăng nhập setup + 3.000 requests đọc dữ liệu = **3.001 HTTP requests** và **3.000 checks**.

### 3.2. Làm rõ số liệu lỗi cũ: 52/1.517 lỗi vs số liệu 103 lỗi
- **Số liệu gốc trong log `k6-authenticated-50.log` (lúc 14:48:16):**
  - Khi chưa có HikariCP, mỗi DAO mở một `DriverManager.getConnection()`. Hệ điều hành Windows dồn tích 13.210 socket TCP `TIME_WAIT` vào cổng 3306, làm cạn kiệt dải cổng tạm thời (ephemeral ports) dẫn đến lỗi `java.net.BindException: Address already in use: connect`.
  - Đến giây thứ 31.7, có **52 requests thất bại trên tổng số 1.517 requests** (tỷ lệ lỗi 3.42% / 3.43% failed checks).
  - Do k6 được cấu hình ngưỡng an toàn `thresholds: { http_req_failed: [{threshold:'rate<0.01',abortOnFail:true}] }`, k6 đã **tự động ngắt sớm ở giây thứ 31.7** vì tỷ lệ lỗi 3.42% vượt quá 1.00%.
- **Nguồn gốc con số 103:** Là con số ước tính/ngoại suy nếu test chạy đủ 60 giây ở cùng tỷ lệ lỗi (~3.43% x 3.000 requests ≈ 103 lỗi).
- **Kết quả thực nghiệm sau khi tích hợp HikariCP:** Khi chạy lại đủ toàn bộ 60 giây với HikariCP pool size 30, hệ thống ghi nhận **0 lỗi (0 / 3.001 requests thất bại)**, chứng minh triệt để xóa bỏ nút thắt cổ chai.

### 3.3. Phân biệt lỗi kiểm tra `java -e` và scratch file
- Lệnh `java -e` báo lỗi `Unrecognized option: -e` do `java.exe` (JDK 21) không có tham số `-e` dòng lệnh (khác với node hoặc python).
- File `VerifyPassword.java` được tạo tạm trong thư mục scratch của IDE (`.gemini/.../scratch/`), hoàn toàn nằm ngoài workspace mã nguồn của dự án.
- Dự án NovaCRM biên dịch hoàn toàn sạch lỗi qua Maven (`mvn test-compile`), không có bất kỳ lỗi cú pháp hay thiếu thư viện nào trong `backend/src/`. File scratch đã được dọn dẹp.

---

## 4. BẢO MẬT VÀ KIỂM TOÁN TRƯỚC PUBLIC

1. **Vệ sinh tệp cấu hình:**
   - File `backend/src/main/resources/db.properties` đã được xóa sạch mật khẩu plain text (`db.password=`), tên đăng nhập đổi thành `crm_user`. Hệ thống tự động ưu tiên nạp thông tin từ biến môi trường bảo mật `CRM_DB_PASSWORD` và `CRM_DB_USERNAME`.
2. **Dữ liệu mẫu an toàn (Seed Data):**
   - 14 tài khoản trong `02_seed_users_roles.sql` đều là tài khoản mẫu với domain giả định `@company.com`.
   - Toàn bộ mật khẩu sử dụng chuỗi demo tiêu chuẩn `Password123@` được mã hóa một chiều qua hàm băm BCrypt (`$2a$12$QVtdE...`).
   - Không chứa bất kỳ email thật, số điện thoại thật, thông tin định danh cá nhân (PII) hay thông tin tài khoản sản xuất.
3. **Loại trừ dữ liệu QA khỏi Git:**
   - Thư mục `.local-qa/` (chứa credential QA ngẫu nhiên, dump test, log kiểm thử) đã nằm trong `.gitignore` và không bị Git theo dõi.
4. **Kết quả quét Gitleaks:**
   - Đã quét toàn bộ 468 commits trên tất cả các nhánh và tags (`gitleaks detect --log-opts="--all"`): **0 LEAKS**.
   - Đã quét toàn bộ thư mục làm việc hiện tại (`gitleaks dir`): **0 LEAKS**.

---

## 5. HƯỚNG DẪN TRIỂN KHAI VÀ CẤU HÌNH PRODUCTION

1. **Yêu cầu biến môi trường trên máy chủ:**
   ```bash
   export CRM_DB_URL="jdbc:mysql://<host>:3306/<database>?useSSL=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
   export CRM_DB_USERNAME="<db_user>"
   export CRM_DB_PASSWORD="<db_strong_password>"
   ```
2. **Khởi tạo dữ liệu:**
   - Chạy migration: `backend/database/run-migrations.ps1 -Database <database>`
   - Nạp dữ liệu demo (nếu là môi trường QA/Demo): `backend/database/seed/run-seed.ps1 -Database <database>`
3. **Quy trình đóng gói:**
   ```bash
   mvn clean package -DskipTests=false
   ```
   Artifact đầu ra: `backend/target/crm.war` deploy vào Tomcat 10.1+ (Jakarta EE 10).

---

## 6. KẾT LUẬN VÀ TRẠNG THÁI BÀN GIAO

- Mã nguồn đã hoàn thiện, đạt 100% các tiêu chí về chất lượng mã, kiểm thử tự động, hiệu năng tải cao và an toàn bảo mật.
- Toàn bộ các thay đổi được tạo Pull Request từ nhánh `release/final-handoff-20261023` về nhánh `develop`.
- **Tuân thủ nguyên tắc:** Không tự ý merge vào `main` và không tự ý chuyển repository sang chế độ Public.
