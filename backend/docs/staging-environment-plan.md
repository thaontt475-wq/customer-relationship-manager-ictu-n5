# KẾ HOẠCH XÂY DỰNG MÔI TRƯỜNG KIỂM THỬ ĐỘC LẬP (STAGING ENVIRONMENT PLAN)

> **Phiên bản:** 3.1 (TASK 04A.3 — Final Staging Plan Validation & Multi-Tier GO/NO-GO Classification)
> **Repository:** `thaontt475-wq/customer-relationship-manager-ictu-n5`
> **Baseline Commit:** `0479ea50109bb11fa28a751555ebc0be51ab75fe` (Khớp `origin/develop`)
> **Worktree:** `crm-staging-test-plan` | **Branch:** `docs/CRM-staging-test-plan`
> **Trạng thái:** **WAITING_FOR_APPROVAL** (Chỉ khảo sát, kiểm chứng an toàn và lập tài liệu; chưa triển khai runtime)

---

## 1. Mục tiêu và Nguyên tắc an toàn bất khả xâm phạm

Tài liệu này xác lập thiết kế kiến trúc và quy trình triển khai môi trường kiểm thử độc lập (Isolated Staging Environment) nhằm phục vụ nghiệm thu toàn diện Acceptance Criteria (AC) cho Sprint 1 và Sprint 2 mà **không gây ảnh hưởng, gián đoạn hoặc làm biến đổi bất kỳ dữ liệu và dịch vụ hiện hành nào**.

### Các quy tắc an toàn bất khả xâm phạm (Zero-Impact Rules):
1. **Tuyệt đối không tác động database hiện tại:** Không sửa đổi, ghi đè, xóa hoặc chạy migration lên database `crm_db`.
2. **Không khởi động lại Tomcat hiện hành:** Tomcat tại `C:\Tools\apache-tomcat-10.1.60` đang phục vụ hệ thống không được phép tắt hoặc tái khởi động.
3. **Không ghi đè bản build hiện tại:** Giữ nguyên tệp `C:\Tools\apache-tomcat-10.1.60\webapps\ROOT.war` và thư mục `webapps\ROOT`.
4. **Không đưa bí mật/thông tin xác thực vào Git:** Tuyệt đối không ghi mật khẩu, chuỗi kết nối nhạy cảm hoặc SMTP credentials vào mã nguồn và tài liệu Markdown.
5. **Cách ly hoàn toàn ngoài checkout Sprint 2:** Toàn bộ cấu hình staging, dữ liệu tạm, thư mục upload và instance Tomcat phải nằm độc lập, không đặt trong thư mục repository.
6. **Cơ chế Fail-Fast (Dừng tức thì khi có lỗi):** Nếu bất kỳ bước xác minh an toàn nào thất bại, toàn bộ quy trình phải dừng lại ngay lập tức, không cố gắng chạy các bước tiếp theo.
7. **Phân quyền hai tầng nghiêm ngặt (Two-Tier MySQL Privilege Isolation):** Tài khoản runtime của ứng dụng (`crm_staging_user`) tuyệt đối không có quyền DDL (`CREATE`, `ALTER`, `DROP`, `INDEX`, `REFERENCES`). Chỉ tài khoản khởi tạo hạ tầng (`crm_staging_admin`) mới có quyền DDL trong phạm vi `crm_db_staging`.

---

## 2. Kết quả khảo sát hệ thống hiện hành

### 2.1. Nền tảng Java & Maven
- **Java Runtime & Compiler:**
  - Phiên bản: `OpenJDK 21.0.12.1 LTS` (Temurin-21.0.12.1+1, 64-Bit Server VM).
  - Vị trí cài đặt: `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`.
  - Cấu hình Maven compiler: `<maven.compiler.release>21</maven.compiler.release>` trong `backend/pom.xml`.
- **Apache Maven:**
  - Phiên bản: `Apache Maven 3.9.16`.
  - Vị trí cài đặt: `C:\Tools\apache-maven-3.9.16`.
- **Quy trình đóng gói `ROOT.war`:**
  - Tệp cấu hình: [`backend/pom.xml`](file:///C:/Users/thang/Projects/crm-staging-test-plan/backend/pom.xml).
  - Đóng gói: `<packaging>war</packaging>`, `<finalName>ROOT</finalName>`.
  - Plugin: `maven-war-plugin:3.4.0`.
  - Nhúng tài nguyên Frontend: Tự động gom tài nguyên từ thư mục `../frontend` gồm `jsp/**/*.jsp`, `css/**/*.css`, `html/**/*.html`, `images/**/*` vào WAR.
  - Lệnh sinh gói: `mvn clean package -DskipTests` (sinh ra tại `backend/target/ROOT.war`).

### 2.2. Máy chủ ứng dụng Apache Tomcat
- **Phiên bản:** `Apache Tomcat 10.1.60` (Hỗ trợ Jakarta EE 10 / Servlet 6.0).
- **Vị trí gốc (CATALINA_HOME):** `C:\Tools\apache-tomcat-10.1.60`.
- **Thư mục chạy hiện tại (CATALINA_BASE):** `C:\Tools\apache-tomcat-10.1.60`.
- **Các cổng mạng cấu hình trong `conf/server.xml`:**
  - Cổng Server Shutdown: `8005`.
  - Cổng HTTP Connector: `8080`.
  - Cổng HTTPS Connector: `8443` (không bắt buộc).
- **Tình trạng runtime hiện thời:** Cổng `8080` và `8005` hiện đang rảnh (Tomcat đang dừng hoặc không lắng nghe).
- **Bộ lọc & Cơ chế bảo vệ web:**
  - `web.xml`: Khai báo `EncodingFilter` (`/*`), `CsrfFilter` (`/*`), `session-timeout` 30 phút, `error-page` (401, 403, 404, 500).
  - WebFilter annotations: `AuthenticationFilter`, `AuthorizationFilter`, `ViewAccessFilter` (chặn truy cập trực tiếp file JSP), `MenuNavigationFilter`, `SprintNavigationFilter`, `AvatarAuthenticationFilter`.
- **Cơ chế lưu trữ và truy cập Avatar (CRM-36):**
  - Đọc từ biến môi trường `CRM_AVATAR_DIR`.
  - Nếu null: Mặc định rơi về `${catalina.base}/data/avatars`.
  - Thư mục avatar hiện hữu: `C:\Tools\apache-tomcat-10.1.60\data\avatars`.

### 2.3. Hệ quản trị cơ sở dữ liệu MySQL
- **Phiên bản:** `MySQL Community Server 8.4.9 LTS` (Win64 on x86_64).
- **Vị trí tệp thực thi:** `C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe`.
- **Cổng mạng:** `3306` (đang lắng nghe, PID `7008`).
- **Database hiện hành:** `crm_db`.
- **Cơ chế nạp cấu hình kết nối ([`DBConnection.java`](file:///C:/Users/thang/Projects/crm-staging-test-plan/backend/src/main/java/com/crm/util/DBConnection.java)):
  - Ưu tiên nạp từ biến môi trường: `CRM_DB_URL`, `CRM_DB_USERNAME`, `CRM_DB_PASSWORD`.
  - Dự phòng nạp từ tệp `db.properties` trên classpath nếu biến môi trường chưa thiết lập.

### 2.4. Các tích hợp dịch vụ bên ngoài
- **Dịch vụ Email ([`EmailService.java`](file:///C:/Users/thang/Projects/crm-staging-test-plan/backend/src/main/java/com/crm/service/email/EmailService.java)):**
  - Biến môi trường: `CRM_SMTP_HOST`, `CRM_SMTP_PORT`, `CRM_SMTP_USERNAME`, `CRM_SMTP_PASSWORD`, `CRM_SMTP_FROM`, `CRM_APP_BASE_URL`.
  - Cần môi trường SMTP mock cục bộ (port 2525) để kiểm tra CRM-23 (Reset Password).
- **Xử lý tệp Excel (CRM-32):** Thư viện Apache POI `5.3.0` nạp và ghi trực tiếp từ bộ nhớ máy chủ.

---

## 3. Kiến trúc thiết kế môi trường Staging độc lập

Môi trường Staging sử dụng mô hình **Tách biệt CATALINA_BASE (Separate CATALINA_BASE Pattern)** kết hợp với **Phân quyền Database MySQL hai tầng (Two-Tier MySQL Privilege Isolation)**.

```
+-----------------------------------------------------------------------------------+
|                               HỆ THỐNG MÁY CHỦ WINDOWS                            |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  [ MÔI TRƯỜNG HIỆN HÀNH / PROD ]                [ MÔI TRƯỜNG STAGING ĐỘC LẬP ]    |
|                                                                                   |
|  CATALINA_HOME: C:\Tools\apache-tomcat-10.1.60  CATALINA_HOME: (dùng chung bin,lib)|
|  CATALINA_BASE: C:\Tools\apache-tomcat-10.1.60  CATALINA_BASE: C:\Tools\crm-stg   |
|  - HTTP Port: 8080                              - HTTP Port: 8088                 |
|  - Shutdown Port: 8005                          - Shutdown Port: 8009             |
|  - AJP: Không sử dụng                           - AJP: Vô hiệu hóa (disabled)     |
|  - War: webapps/ROOT.war (gốc)                  - War: webapps/ROOT.war (staging) |
|  - Data: data/avatars                           - Data: C:\Tools\crm-stg-data     |
|                                                                                   |
|  ---------------------------------------------  --------------------------------- |
|  MySQL Server 8.4 (Port 3306)                   MySQL Server 8.4 (Port 3306)      |
|  - Database: crm_db                             - Database: crm_db_staging        |
|  - User: root / dev                             - Setup User: crm_staging_admin   |
|                                                   (DDL + DML chỉ trên staging DB) |
|                                                 - Runtime User: crm_staging_user  |
|                                                   (CHỈ DML: SELECT, INSERT, UPDATE|
|                                                    DELETE; CẤM DDL; CẤM crm_db)   |
|                                                                                   |
|  ---------------------------------------------  --------------------------------- |
|  SMTP Service: Không cấu hình                   Mock SMTP Sink: Port 2525         |
|                                                   (Bắt gói tin email, không gửi ra|
|                                                    Internet, kiểm tra CRM-23)     |
+-----------------------------------------------------------------------------------+
```

### 3.1. Bảng đối chiếu thông số cô lập

| Thành phần | Môi trường hiện hành | Môi trường Staging đề xuất | Biện pháp cô lập & Kiểm tra xung đột |
|---|---|---|---|
| **Tomcat CATALINA_BASE** | `C:\Tools\apache-tomcat-10.1.60` | `C:\Tools\crm-stg-tomcat` | Tách biệt hoàn toàn thư mục `conf`, `logs`, `temp`, `webapps`, `work`. |
| **HTTP Port** | `8080` | `8088` | Đã kiểm tra `netstat`: Cổng `8088` hiện không bị chiếm dụng. |
| **Shutdown Port** | `8005` | `8009` | Đã kiểm tra `netstat`: Cổng `8009` hiện không bị chiếm dụng. |
| **Database Name** | `crm_db` | `crm_db_staging` | Cơ sở dữ liệu riêng biệt, không dùng chung bảng. |
| **Database Setup Account** | `root` / quản trị | `crm_staging_admin` | Chỉ dùng chạy migration DDL lúc dựng môi trường; tước quyền trên `crm_db`. |
| **Database Runtime Account** | `root` / quản trị | `crm_staging_user` | Tài khoản JDBC kết nối từ Tomcat; **CHỈ CÓ DML, CẤM DDL**; tước quyền trên `crm_db`. |
| **Thư mục Avatar** | `C:\Tools\apache-tomcat-10.1.60\data\avatars` | `C:\Tools\crm-stg-data\avatars` | Thiết lập qua biến môi trường `CRM_AVATAR_DIR`. |
| **SMTP Server** | Không xác định | `localhost:2525` | Mock SMTP cục bộ (Sink mode), không chuyển tiếp thư ra Internet. |
| **Base App URL** | `http://localhost:8080` | `http://localhost:8088` | Thiết lập qua `CRM_APP_BASE_URL`. |
| **Logs kiểm thử** | `C:\Tools\apache-tomcat-10.1.60\logs` | `C:\Tools\crm-stg-tomcat\logs` | Nhật ký độc lập, không ghi đè log gốc. |

---

## 4. Cơ chế bảo vệ kết nối & Thẩm định an toàn trước khởi động (Pre-flight Probes)

Trước khi khởi động Tomcat staging hoặc thực thi bất kỳ bài kiểm thử ghi dữ liệu nào, hệ thống bắt buộc phải thực thi bộ kiểm tra tiền trạm tự động (Pre-flight Probe). **Nếu thiếu hoặc sai lệch bất kỳ điều kiện nào dưới đây, quy trình lập tức dừng lại (ABORT):**

### 4.1. Thẩm định kết nối Database (Database Connection Safeguard)
1. **Kiểm tra cơ sở dữ liệu hiện hành:**
   - Thực thi: `SELECT DATABASE();`
   - Tiêu chí bắt buộc: Giá trị trả về phải là `'crm_db_staging'`. Nếu trả về `'crm_db'` hoặc bất kỳ giá trị nào khác -> **DỪNG NGAY LẬP TỨC**.
2. **Kiểm tra tài khoản thực thi runtime:**
   - Thực thi: `SELECT CURRENT_USER();`
   - Tiêu chí bắt buộc: Phải là `'crm_staging_user'@'localhost'` (hoặc `127.0.0.1`). Tuyệt đối không chấp nhận tài khoản `root` hoặc tài khoản có quyền SUPER/ALL trên server.
3. **Thẩm định giới hạn quyền DDL của tài khoản runtime:**
   - Thực thi: `CREATE TABLE crm_db_staging.probe_ddl (id INT);` bằng `crm_staging_user`.
   - Tiêu chí bắt buộc: MySQL bắt buộc phải từ chối với lỗi:
     `ERROR 1142 (42000): CREATE command denied to user 'crm_staging_user'@'localhost' for table 'probe_ddl'`.
     Nếu tạo thành công -> **DỪNG NGAY LẬP TỨC** do tài khoản runtime thừa quyền DDL.
4. **Thẩm định tước quyền trên `crm_db` (Negative Permission Probe):**
   - Thực thi: `SELECT 1 FROM crm_db.users LIMIT 1;` bằng cả hai tài khoản `crm_staging_admin` và `crm_staging_user`.
   - Tiêu chí bắt buộc: MySQL bắt buộc phải ném lỗi:
     `ERROR 1142 (42000): SELECT command denied to user ... for table 'users'` hoặc `ERROR 1044: Access denied`.
     Nếu lệnh này thực thi thành công -> **DỪNG NGAY LẬP TỨC** do cấu hình quyền bị rò rỉ sang production.
5. **Kiểm tra URL JDBC thực tế của ứng dụng:**
   - Đọc qua `DBConnection.getConnection().getMetaData().getURL()`:
   - Chuỗi kết nối phải chứa chính xác `jdbc:mysql://.../crm_db_staging?...`. Tuyệt đối không chứa `crm_db?` hoặc `crm_db/`.
6. **Kiểm tra biến môi trường tiến trình:**
   - Biến `CRM_DB_URL`, `CRM_DB_USERNAME`, `CRM_DB_PASSWORD` phải được nạp cục bộ trong phạm vi tiến trình Tomcat qua `setenv.bat`.
   - Xác nhận rằng các biến môi trường này ghi đè hoàn toàn cấu hình mặc định trong `db.properties`.

### 4.2. Thẩm định cô lập máy chủ Tomcat (Tomcat Isolation Safeguard)
1. **Tách biệt đường dẫn nhị phân và dữ liệu:**
   - `CATALINA_HOME` = `C:\Tools\apache-tomcat-10.1.60` (chỉ đọc các tệp `.jar` trong `bin/` và `lib/`).
   - `CATALINA_BASE` = `C:\Tools\crm-stg-tomcat` (thư mục chạy độc lập hoàn toàn).
2. **Quét cổng mạng trước khởi động:**
   - Chạy lệnh `netstat -ano | findstr ":8088"` -> Kết quả phải là RỖNG (không có tiến trình nào đang chiếm cổng).
   - Chạy lệnh `netstat -ano | findstr ":8009"` -> Kết quả phải là RỖNG.
   - Vô hiệu hóa cổng AJP trong `conf/server.xml` của Staging (comment thẻ `<Connector protocol="AJP/1.3" ... />`) để tránh xung đột cổng `8009` mặc định.
3. **Thư mục ứng dụng và bộ đệm độc lập:**
   - Thư mục `C:\Tools\crm-stg-tomcat\webapps\` chỉ chứa duy nhất bản build staging `ROOT.war`. Tuyệt đối không tạo liên kết symlink hoặc copy đè lên `C:\Tools\apache-tomcat-10.1.60\webapps\ROOT.war`.
   - Thư mục `work/` và `temp/` nằm bên trong `crm-stg-tomcat` để các lớp JSP biên dịch runtime không chạm tới môi trường chính.
4. **Thư mục Avatar độc lập:**
   - Thư mục `C:\Tools\crm-stg-data\avatars` được tạo riêng và cấu hình qua `CRM_AVATAR_DIR`.
   - Kiểm tra xác nhận không có bất kỳ tệp ảnh nào từ môi trường chính bị sao chép sang.

### 4.3. Thẩm định cô lập dịch vụ Email (SMTP Sink Isolation)
1. **Chế độ Sink cục bộ (Local Sink Only):**
   - Dịch vụ Mock SMTP lắng nghe tại `127.0.0.1:2525`.
   - Máy chủ này được cấu hình **KHÔNG CÓ upstream relay**, hoàn toàn không kết nối tới bất kỳ máy chủ SMTP Internet nào (như Gmail, SendGrid, Mailgun).
2. **Kiểm tra cơ chế bắt gói tin:**
   - Toàn bộ email reset mật khẩu từ CRM-23 được lưu trữ trong bộ nhớ đệm (In-memory buffer) hoặc thư mục tệp tạm của Mock SMTP để phục vụ việc trích xuất token tự động trong kịch bản E2E.
   - Kiểm tra log của Mock SMTP: Xác nhận các địa chỉ người nhận `*@example.invalid` hoặc `*@crm-staging.local` không bị đẩy ra mạng ngoài.

---

## 5. Quy trình tạo bản SQL Staging riêng & Trình tự khởi tạo có thể tái lập

### 5.1. Thiết kế bản SQL Staging chuyên biệt (Deterministic Staging Artifacts)
Tuyệt đối không sửa đổi các tệp migration gốc trong kho mã nguồn. Để đảm bảo tương thích 100% với MySQL 8.4 và tránh các lỗi cú pháp (như `ADD COLUMN IF NOT EXISTS`), hệ thống sử dụng quy trình tạo tệp staging tất định (deterministic DDL) và kiểm toán tĩnh trước khi thực thi:

```
[MÃ NGUỒN GỐC]                                      [TỆP TIN STAGING ĐỘC LẬP]
backend/database/schema.sql  ---> Lược bỏ CREATE DB, USE  ---> staging-01-schema.sql
CRM-44, CRM-47               ---> Trích xuất đối tượng thiếu ---> staging-02-reconcile.sql
backend/database/data.sql    ---> Fixtures + Synthetic Data ---> staging-03-fixtures.sql
```

**Quy tắc kiểm tra tệp SQL Staging trước khi thực thi:**
- Tệp sinh ra được lưu ngoài thư mục workspace (tại `C:\Tools\crm-stg-artifacts\`).
- Quét tĩnh (Regex Scan) bắt buộc:
  - Không được chứa chuỗi `USE ` (để context database do tham số kết nối kiểm soát 100%).
  - Không chứa chuỗi `crm_db.` hoặc tham chiếu chéo database.
  - Không chứa lệnh `DROP DATABASE` hoặc `CREATE DATABASE`.
  - Không chứa cú pháp `ADD COLUMN IF NOT EXISTS` (gây lỗi 1064 trên MySQL 8.4).

### 5.2. Trình tự khởi tạo chuẩn từ Database rỗng (Reproducible Staging Sequence)
Để tránh lỗi trùng lặp cột/bảng/chỉ mục (xem chi tiết tại [`staging-migration-audit.md`](file:///C:/Users/thang/Projects/crm-staging-test-plan/backend/docs/staging-migration-audit.md)), trình tự khởi tạo database staging từ trạng thái rỗng được chuẩn hóa gồm 3 bước:

1. **Bước 1: Nạp Lược đồ cơ sở 26 bảng (`staging-01-schema.sql`):**
   - Khởi tạo 26 bảng cốt lõi từ `schema.sql`.
   - Thiết lập các ràng buộc toàn vẹn và `fk_teams_leader`.
2. **Bước 2: Nạp các đối tượng Sprint 2 còn thiếu (`staging-02-reconcile.sql`):**
   - Tạo bảng `leads`.
   - Bổ sung cột bằng DDL tất định: `industry_id`, `company_size_id` trên `customers`; `activity_type_id` trên `activities`; `stage_id`, `amount`, `contact_name`, `lost_reason`, `probability` trên `opportunities`.
   - Nạp **27 danh mục Master Data** (`categories`): 8 INDUSTRY, 5 COMPANY_SIZE, 8 LEAD_SOURCE, 6 ACTIVITY_TYPE.
   - Nạp **6 giai đoạn bán hàng** (`pipeline_stages`): PROSPECTING, QUALIFICATION, PROPOSAL, NEGOTIATION, CLOSED_WON, CLOSED_LOST.
   - Nạp vai trò `'Director'` vào bảng `roles`.
3. **Bước 3: Nạp Dữ liệu kiểm thử tổng hợp (`staging-03-fixtures.sql`):**
   - Nạp các vai trò cơ sở từ `data.sql` (Admin, Sales Rep, Accountant, Team Lead).
   - Nạp toàn bộ dữ liệu giả lập phục vụ kịch bản kiểm thử E2E (xem Mục 6).

### 5.3. Cú pháp thực thi MySQL CLI thực tế (Verified MySQL 8.4 Syntax):
Trong MySQL CLI 8.4, cờ `-f, --force` dùng để tiếp tục khi gặp lỗi. Khi không truyền `-f`, MySQL CLI luôn dừng ngay lập tức khi phát sinh bất kỳ lỗi SQL nào (`force = FALSE` mặc định):

```powershell
# Chạy bằng tài khoản crm_staging_admin (Fail-Fast: dừng ngay nếu có lỗi)
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" --host=localhost --port=3306 --user=crm_staging_admin -p crm_db_staging -e "source C:/Tools/crm-stg-artifacts/staging-01-schema.sql"
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" --host=localhost --port=3306 --user=crm_staging_admin -p crm_db_staging -e "source C:/Tools/crm-stg-artifacts/staging-02-reconcile.sql"
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" --host=localhost --port=3306 --user=crm_staging_admin -p crm_db_staging -e "source C:/Tools/crm-stg-artifacts/staging-03-fixtures.sql"
```

---

## 6. Thiết kế Dữ liệu kiểm thử tổng hợp (Synthetic Test Fixtures)

Tất cả dữ liệu kiểm thử được thiết kế độc lập, bảo đảm tính có thể tái tạo (reproducible), không sử dụng thông tin cá nhân (PII) thật hoặc mật khẩu thật:

### 6.1. Tài khoản người dùng (Mật khẩu chuẩn kiểm thử: `Staging@123` - BCrypt Hashed)
- `admin_stg`: `admin@crm-staging.local`, Vai trò: `Admin`, Data Scope: `ALL`.
- `director_stg`: `director@crm-staging.local`, Vai trò: `Director`, Data Scope: `ALL`.
- `lead_stg`: `lead@crm-staging.local`, Vai trò: `Team Lead`, Data Scope: `TEAM`, Nhóm: `Đội Miền Bắc`.
- `rep1_stg`: `rep1@crm-staging.local`, Vai trò: `Sales Rep`, Data Scope: `SELF`, Nhóm: `Đội Miền Bắc`.
- `rep2_stg`: `rep2@crm-staging.local`, Vai trò: `Sales Rep`, Data Scope: `SELF`, Nhóm: `Đội Miền Nam`.
- `recipient_stg`: `recipient@crm-staging.local`, Vai trò: `Sales Rep`, Trạng thái: `ACTIVE` (Nhận bàn giao).
- `locked_stg`: `locked@crm-staging.local`, Vai trò: `Sales Rep`, Trạng thái: `ACTIVE` (Mục tiêu khóa & bàn giao).

### 6.2. Cơ cấu tổ chức nhóm (Teams)
- `T-01`: `"Khối Kinh Doanh Toàn Quốc"`, `parent_id = NULL`, `region = 'NATIONAL'`, Trưởng đơn vị: `director_stg`.
- `T-02`: `"Đội Miền Bắc"`, `parent_id = T-01.id`, `region = 'NORTH'`, Trưởng nhóm: `lead_stg`.
- `T-03`: `"Đội Miền Nam"`, `parent_id = T-01.id`, `region = 'SOUTH'`, Trưởng nhóm: `NULL`.

### 6.3. Khách hàng, Cơ hội & Lead mẫu (Domain Records)
- `CUST-01`: `"Công ty TNHH Staging Alpha"`, Chủ sở hữu: `rep1_stg`, Ngành: `IT`, Quy mô: `SMALL`.
- `CUST-02`: `"Công ty Cổ phần Staging Beta"`, Chủ sở hữu: `rep2_stg`, Ngành: `RETAIL`, Quy mô: `MEDIUM`.
- `CUST-03`: `"Tập đoàn Staging Gamma"`, Chủ sở hữu: `lead_stg`, Ngành: `FINANCE`, Quy mô: `ENTERPRISE`.
- `CUST-04`: `"Doanh nghiệp Bàn Giao Delta"`, Chủ sở hữu: `locked_stg`, Ngành: `MANUFACTURING`, Quy mô: `LARGE`.
- `OPP-01`: `"Dự án Phần mềm CRM Alpha"`, Khách hàng: `CUST-01`, Chủ sở hữu: `rep1_stg`, Giai đoạn: `PROPOSAL`, Giá trị: `100.000.000`.
- `OPP-02`: `"Dự án Chuyển Đổi Số Delta"`, Khách hàng: `CUST-04`, Chủ sở hữu: `locked_stg`, Giai đoạn: `PROSPECTING`, Giá trị: `50.000.000`.
- `LEAD-01`: `"Nguyễn Văn Tiềm Năng"`, Chủ sở hữu: `rep1_stg`, Nguồn: `WEBSITE`, Ngành: `IT`.

### 6.4. Danh mục sản phẩm & Bảng giá (Products)
- `PROD-01`: `"CRM Enterprise License"`, Giá niêm yết: `10.000.000`, Giá sàn: `8.000.000`, Giá vốn: `5.000.000`. (Được gắn vào một chi tiết đơn hàng mẫu -> Kiểm tra chặn xóa an toàn CRM-39).
- `PROD-02`: `"Gói Dịch Vụ Đào Tạo"`, Giá niêm yết: `5.000.000`, Giá sàn: `4.000.000`, Giá vốn: `2.500.000`. (Không có liên kết -> Kiểm tra xóa thành công CRM-39).

### 6.5. Giai đoạn bán hàng mẫu (Pipeline Stages)
- 1: `"Tìm kiếm & Tiếp cận"` (PROSPECTING), Thứ tự: 1, Xác suất: 10%.
- 2: `"Đánh giá & Xác định nhu cầu"` (QUALIFICATION), Thứ tự: 2, Xác suất: 25%.
- 3: `"Gửi đề xuất & Báo giá"` (PROPOSAL), Thứ tự: 3, Xác suất: 50%.
- 4: `"Thương lượng & Đàm phán"` (NEGOTIATION), Thứ tự: 4, Xác suất: 75%.
- 5: `"Chốt thành công (Won)"` (CLOSED_WON), Thứ tự: 5, Xác suất: 100%, `is_won = TRUE`.
- 6: `"Thất bại (Lost)"` (CLOSED_LOST), Thứ tự: 6, Xác suất: 0%, `is_lost = TRUE`.

---

## 7. Bảng kiểm định GO/NO-GO trước khi thực thi TASK 04B

Quy chuẩn phân loại trạng thái kiểm định:
- **`GO`**: Đã kiểm chứng điều kiện bắt buộc trong môi trường thực tế tại máy chủ hiện hành.
- **`DESIGN VERIFIED`**: Thiết kế kiến trúc và giải pháp kỹ thuật đã được kiểm tra, thẩm định tĩnh và đối chiếu mã nguồn thành công.
- **`PENDING RUNTIME`**: Chưa triển khai hoặc chưa thể có bằng chứng thực tế runtime (cần thực thi trong TASK 04B).
- **`NO-GO`**: Điều kiện chưa đáp ứng hoặc còn rủi ro chưa giải quyết.
- **`WAITING_FOR_APPROVAL`**: Đang chờ người dùng phê duyệt chính thức trước khi kích hoạt bất kỳ lệnh runtime nào.

| STT | Hạng mục kiểm tra | Phương pháp kiểm chứng | Tiêu chuẩn ĐẠT (Evidence Criteria) | Trạng thái hiện tại | Biện pháp xử lý nếu NO-GO |
|:---:|---|---|---|:---:|---|
| **1** | Cổng HTTP Staging rảnh | `netstat -ano \| findstr ":8088"` | Không trả về dòng nào | **GO** (Đã đo đạc thực tế) | Đổi sang cổng rảnh khác (ví dụ: 8089) |
| **2** | Cổng Shutdown Staging rảnh | `netstat -ano \| findstr ":8009"` | Không trả về dòng nào | **GO** (Đã đo đạc thực tế) | Đổi sang cổng rảnh khác (ví dụ: 8010) |
| **3** | Cổng AJP bị vô hiệu hóa | Đọc `conf/server.xml` Staging | Thẻ `<Connector protocol="AJP..."/>` đã bị comment | **DESIGN VERIFIED** | Xóa/comment cấu hình AJP |
| **4** | Phân quyền 2 tầng MySQL Staging | `SHOW GRANTS FOR 'crm_staging_user'@'localhost';` | `crm_staging_user` CHỈ có SELECT, INSERT, UPDATE, DELETE trên `crm_db_staging.*`. Không có CREATE, ALTER, DROP | **PENDING RUNTIME** | Chạy lệnh cấp quyền tối thiểu trong 04B |
| **5** | Thử nghiệm tước quyền âm tính | Chạy `SELECT 1 FROM crm_db.users;` qua cả 2 tài khoản staging | MySQL báo lỗi `Access denied` | **PENDING RUNTIME** | Tước toàn bộ quyền trên `crm_db` |
| **6** | Biến môi trường Staging cô lập | Kiểm tra `setenv.bat` của Tomcat Staging | `CRM_DB_URL` chứa `crm_db_staging`. Không có biến hệ thống toàn cục trỏ nhầm | **DESIGN VERIFIED** | Xóa biến môi trường toàn cục |
| **7** | Không ghi đè bản build hiện tại | So sánh đường dẫn file | WAR staging đặt tại `C:\Tools\crm-stg-tomcat\webapps\ROOT.war`. Giữ nguyên tệp gốc | **DESIGN VERIFIED** | Dừng nếu đường dẫn trùng tệp gốc |
| **8** | Thư mục Avatar tách biệt | Kiểm tra giá trị `CRM_AVATAR_DIR` | Trỏ tới `C:\Tools\crm-stg-data\avatars`, không dùng thư mục gốc | **PENDING RUNTIME** | Tạo thư mục riêng cho staging trong 04B |
| **9** | Mock SMTP Sink cục bộ | Kiểm tra cổng `2525` và cấu hình sink | Lắng nghe tại `127.0.0.1:2525`, không chuyển tiếp email ra ngoài | **PENDING RUNTIME** | Khởi động Mock SMTP trước khi test CRM-23 |
| **10** | Thiết kế xây dựng 3 tệp SQL staging | Thẩm định tĩnh mô hình DDL tất định | Đã xác minh thiết kế; loại bỏ `USE crm_db` và `IF NOT EXISTS` | **DESIGN VERIFIED** | Sửa thiết kế DDL |
| **10a** | Tạo 3 tệp SQL staging vật lý | Kiểm tra tệp trong `C:\Tools\crm-stg-artifacts\` | Chưa tạo ba tệp vật lý trên đĩa | **PENDING RUNTIME** | Tạo tệp vật lý trong TASK 04B |
| **10b** | Xác minh nội dung SQL staging thực tế | Quét regex `USE ` trên 3 tệp vật lý | Chưa quét trên tệp vật lý thực tế | **PENDING RUNTIME** | Quét tự động ngay khi tệp được sinh ra |
| **10c** | Thực thi trên engine MySQL 8.4 | Chạy batch script qua `mysql.exe` | Chưa thực thi trên MySQL 8.4 | **PENDING RUNTIME** | Thực thi tuần tự trong TASK 04B |
| **11** | Kiểm toán 15 migration thật | Đối chiếu mã băm SHA-256 với Git | Toàn bộ 15 tệp thật được kiểm toán chi tiết | **GO** (Đã đối chiếu Git) | Đã cập nhật báo cáo kiểm toán |
| **12** | Loại trừ CRM-50 và CRM-51 | So khớp danh sách migration chạy staging | Chỉ chạy `staging-01`, `staging-02`, `staging-03`. Loại bỏ CRM-50/51 | **GO** (Đã cô lập baseline) | Đánh dấu NOT VERIFIED cho CRM-50/51 |
| **13** | Khởi tạo thành công `crm_db_staging` | Thực thi kết nối JDBC staging | Kết nối thành công, `DATABASE() = crm_db_staging` | **PENDING RUNTIME** | Khởi tạo DB trong TASK 04B |
| **14** | Phê duyệt từ người dùng | Nhận thông điệp đồng ý từ người dùng | Trạng thái chuyển sang APPROVED | **WAITING_FOR_APPROVAL** | Dừng lại, chưa triển khai |

> [!WARNING]
> **KẾT LUẬN KIỂM ĐỊNH HIỆN TẠI:**
> Hệ thống **CHƯA ĐỦ ĐIỀU KIỆN ĐỂ KẾT LUẬN TOÀN BỘ CHECKLIST LÀ GO**.
> - **Đã xác minh thiết kế xây dựng SQL staging:** `DESIGN VERIFIED`.
> - **Chưa tạo ba tệp SQL staging vật lý:** `PENDING RUNTIME`.
> - **Chưa xác minh toàn bộ nội dung SQL staging thực tế trên tệp vật lý:** `PENDING RUNTIME`.
> - **Chưa thực thi trên MySQL 8.4:** `PENDING RUNTIME`.
> - **Mọi kết luận về khả năng khởi tạo thành công database phải giữ ở trạng thái `PENDING RUNTIME`**; tuyệt đối không ghi nhận SQL staging đã vượt qua kiểm tra chỉ dựa trên thiết kế tĩnh.
> - Các hạng mục về phân quyền MySQL, Mock SMTP, khởi tạo database và Tomcat staging bắt buộc phải ở trạng thái **`PENDING RUNTIME`** và chỉ được kích hoạt sau khi người dùng phê duyệt chính thức.

---

## 8. Kế hoạch triển khai kỹ thuật cho TASK 04B

Sau khi nhận được sự phê duyệt chính thức từ người dùng:
1. **Bước 1 (Hạ tầng Database & Phân quyền):** Tạo database `crm_db_staging`. Tạo user `crm_staging_admin` (DDL + DML) và user `crm_staging_user` (DML only).
2. **Bước 2 (Chạy Pre-flight Database Probe):** Kiểm tra âm tính chặn truy cập `crm_db` và chặn quyền DDL của `crm_staging_user`.
3. **Bước 3 (Nạp Lược đồ & Dữ liệu mẫu qua crm_staging_admin):** Chạy tuần tự `staging-01-schema.sql`, `staging-02-reconcile.sql`, và `staging-03-fixtures.sql`.
4. **Bước 4 (Dựng Instance Tomcat Staging):** Sao chép cấu hình sang `C:\Tools\crm-stg-tomcat`, tạo `setenv.bat`, thiết lập cổng `8088` và `8009`, vô hiệu hóa AJP.
5. **Bước 5 (Đóng gói & Triển khai):** Thực thi `mvn clean package -DskipTests` và copy vào `ROOT.war` của staging.
6. **Bước 6 (Khởi chạy Mock SMTP & Instance):** Khởi động Mock SMTP port 2525, khởi động Tomcat staging.
7. **Bước 7 (Thực thi Ma trận E2E):** Chạy lần lượt 20 User Story theo [`e2e-test-matrix.md`](file:///C:/Users/thang/Projects/crm-staging-test-plan/backend/docs/e2e-test-matrix.md).
8. **Bước 8 (Xuất Báo cáo Nghiệm thu):** Tổng kết kết quả và kích hoạt kịch bản dọn dẹp (Teardown).
