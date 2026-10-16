# HƯỚNG DẪN THIẾT LẬP MÔI TRƯỜNG DỰ ÁN NOVACRM (TEAM SETUP GUIDE)

Tài liệu này chuẩn hóa quy trình cài đặt, cấu hình cơ sở dữ liệu, đóng gói, kiểm thử và chạy thử nghiệm hệ thống **NovaCRM Enterprise B2B** dành cho toàn bộ thành viên trong nhóm phát triển.

---

## 1. Yêu cầu phần mềm và công cụ (Prerequisites)

Mọi thành viên cần cài đặt các công cụ sau trước khi bắt đầu:

| Công cụ | Phiên bản yêu cầu | Ghi chú |
| :--- | :--- | :--- |
| **JDK (Java Development Kit)** | **Java 21 LTS** (Eclipse Adoptium Temurin hoặc Oracle) | Bắt buộc Java 21, cấu hình biến môi trường `JAVA_HOME`. |
| **Apache Maven** | **3.9.x trở lên** | Thêm thư mục `bin` của Maven vào biến môi trường `PATH`. |
| **MySQL Server** | **8.0 hoặc 8.4 LTS** | Đảm bảo service MySQL đang chạy tại cổng `3306`. |
| **Apache Tomcat** | **10.1.x** (Jakarta EE 10 / Servlet 6.0) | Đặt biến `CATALINA_HOME` trỏ về thư mục cài đặt Tomcat. |
| **Git** | Bản mới nhất | Dùng cho quản lý mã nguồn. |

---

## 2. Quy chuẩn biến môi trường (Environment Standards)

Hệ thống sử dụng 3 biến môi trường chuẩn để kết nối CSDL, giúp cấu hình linh hoạt mà **không cần sửa mã nguồn**:

- `CRM_DB_URL`: Đường dẫn JDBC đến CSDL.
  - Development mặc định: `jdbc:mysql://localhost:3306/crm_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8`
  - Test tự động mặc định: `jdbc:mysql://localhost:3306/crm_sprint1_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8`
- `CRM_DB_USERNAME`: Tên tài khoản MySQL (Khuyến nghị: `crm_user`, **không dùng `root`**).
- `CRM_DB_PASSWORD`: Mật khẩu tài khoản MySQL tương ứng.

> [!CAUTION]
> **QUY TẮC BẢO MẬT BẮT BUỘC:**
> 1. Tuyệt đối **KHÔNG** commit mật khẩu hoặc thông tin xác thực cá nhân lên Git.
> 2. Không chỉnh sửa cứng tài khoản/mật khẩu trong file `db.properties` gốc. Mọi cấu hình cá nhân nên dùng biến môi trường hoặc script tự động.

---

## 3. Quy trình thiết lập từng bước (Quick Start)

### Bước 1: Lấy mã nguồn mới nhất
```powershell
git checkout develop
git pull origin develop
```

---

### Bước 2: Thiết lập Database (Một lệnh duy nhất)

#### 2.1. Khuyến nghị: Tạo tài khoản database riêng cho CRM
Mở MySQL Client bằng tài khoản `root` và tạo user riêng cho dự án:
```sql
CREATE USER IF NOT EXISTS 'crm_user'@'localhost' IDENTIFIED BY 'MatKhauCuaBan123';
GRANT ALL PRIVILEGES ON crm_db.* TO 'crm_user'@'localhost';
GRANT ALL PRIVILEGES ON crm_sprint1_test.* TO 'crm_user'@'localhost';
FLUSH PRIVILEGES;
```

#### 2.2. Kích hoạt script khởi tạo tự động
Chạy script PowerShell từ thư mục gốc của dự án:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\setup-dev.ps1
```
Script sẽ:
- Tự động phát hiện Java 21, Maven, MySQL CLI trên máy của bạn.
- Hỏi Username/Password (hoặc nhận diện từ biến môi trường). Mật khẩu được ẩn an toàn khi nhập.
- Tự động tạo 2 database: `crm_db` (phát triển) và `crm_sprint1_test` (chạy test).
- Áp dụng tuần tự toàn bộ 35 migration SQL từ `backend/database` và ghi nhận vào bảng `schema_migrations`.
- Tự động bỏ qua các migration đã chạy, đảm bảo **không bao giờ phá hủy dữ liệu cũ**.

---

### Bước 3: Đóng gói ứng dụng (Build một lệnh)

Đóng gói file WAR hoàn chỉnh mà **không yêu cầu MySQL**:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\build.ps1
```
- Kết quả: Tạo file `backend\target\crm.war` chứa cả Backend (Java Servlets) và Frontend (HTML/CSS/JS).

---

### Bước 4: Chạy kiểm thử tự động (Test một lệnh)

Chạy toàn bộ bộ test cô lập trên database `crm_sprint1_test`:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\test.ps1
```
- Script tự động chặn chạy test trên `crm_db` để bảo vệ dữ liệu nghiệp vụ.
- Báo cáo rõ ràng: Tổng số test, số test PASS, FAIL, ERROR và nguyên nhân chi tiết nếu có lỗi.
- Đảm bảo đạt chuẩn: **322/322 Maven tests PASS**.

---

### Bước 5: Khởi chạy hệ thống để demo (Run một lệnh)

Triển khai và khởi động máy chủ Apache Tomcat 10.1:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\run.ps1
```
Script sẽ:
- Dừng an toàn phiên bản Tomcat cũ đang chạy trên cổng 8080 (nếu có).
- Cấu hình an toàn biến môi trường database vào file `bin\setenv.bat` của Tomcat.
- Deploy file `crm.war` mới nhất vào thư mục `webapps`.
- Khởi động Tomcat và kiểm tra kết nối HTTP đến khi sẵn sàng.
- Hiển thị URL truy cập:
  - **Trang đăng nhập:** [http://localhost:8080/crm/login.html](http://localhost:8080/crm/login.html)
  - **Dashboard:** [http://localhost:8080/crm/dashboard.html](http://localhost:8080/crm/dashboard.html)

---

## 4. Xử lý các sự cố thường gặp (Troubleshooting)

### 1. Lỗi `Access denied for user 'root'@'localhost'` (hoặc `crm_user`)
- **Nguyên nhân:** Mật khẩu trong biến môi trường hoặc nhập vào không trùng khớp với mật khẩu tài khoản MySQL trên máy bạn.
- **Cách xử lý:**
  1. Kiểm tra lại mật khẩu MySQL bằng lệnh:
     ```powershell
     & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u crm_user -p
     ```
  2. Cập nhật biến môi trường đúng trong PowerShell:
     ```powershell
     $env:CRM_DB_USERNAME = "crm_user"
     $env:CRM_DB_PASSWORD = "MatKhauChinhXac"
     ```

### 2. Lỗi `Tests must not use crm_db` hoặc `Exact isolated test database required`
- **Nguyên nhân:** Bạn đang chạy `mvn test` trực tiếp khi biến `CRM_DB_URL` đang trỏ vào `crm_db`. Bộ test của NovaCRM có cơ chế an toàn bắt buộc phải chạy trên `crm_sprint1_test` để tránh xóa dữ liệu thật.
- **Cách xử lý:** Sử dụng script `.\scripts\test.ps1` để tự động thiết lập đúng database test.

### 3. Lỗi cổng `8080` bị chiếm dụng (Port already in use)
- **Nguyên nhân:** Phiên bản Tomcat cũ hoặc tiến trình khác đang chiếm cổng 8080.
- **Cách xử lý:** Chạy `.\scripts\run.ps1` với cờ ép buộc khởi động lại:
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts\run.ps1 -ForceRestart
  ```

### 4. Lỗi `POM file backend/pom.xml does not exist`
- **Nguyên nhân:** Bạn đang đứng bên trong thư mục `backend` nhưng lại truyền `-f backend/pom.xml`.
- **Cách xử lý:** Đứng tại thư mục gốc của repository khi chạy các lệnh script, hoặc dùng:
  ```powershell
  mvn clean package -DskipTests   # Nếu đang ở trong thư mục backend
  ```

---

## 5. Tóm tắt các lệnh nhanh cho thành viên

| Tác vụ | Lệnh PowerShell thực thi |
| :--- | :--- |
| **Thiết lập DB** | `.\scripts\setup-dev.ps1` |
| **Build WAR** | `.\scripts\build.ps1` |
| **Chạy Test** | `.\scripts\test.ps1` |
| **Khởi động Demo** | `.\scripts\run.ps1` |
