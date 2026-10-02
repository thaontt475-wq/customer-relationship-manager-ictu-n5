# CRM-36 / S2-03 — Ảnh đại diện

> Các đường dẫn tệp bên dưới tính từ tài liệu trong `backend/docs/`. Lệnh terminal và đường dẫn `target/` vẫn tính từ thư mục làm việc `backend/`.

## Thiết kế theo project hiện có

Giữ package `com.crm`, Java 21, Jakarta Servlet 6 / Tomcat 10.1, MySQL 8,
Maven WAR và JDBC `DBConnection`. Không thêm dependency. ImageIO/Graphics2D
của JDK xử lý ảnh; Gson và JUnit đã có trong pom.xml.

Luồng: Browser/JSP → AvatarAuthenticationFilter → AvatarServlet → AvatarService
→ AvatarDAO → JDBC → MySQL. DAO chỉ thực hiện SQL; Service quản lý nghiệp vụ,
transaction và lưu file. Servlet xử lý HTTP/multipart và forward JSP.

Repo chưa có contract upload/crop hoặc thông tin avatar trong User. Bổ sung
contract riêng bên dưới, không đổi các API login/user hiện có. ID lấy từ session
`userId` của CRM-21; không nhận ID tài khoản cần sửa từ browser.
`UserAvatar` là model riêng của bảng `user_avatars`, FK tới `users.id`.

## File mới / chỉnh sửa

- `../src/main/java/com/crm/filter/AvatarAuthenticationFilter.java`: yêu cầu session.
- `../src/main/java/com/crm/controller/users/AvatarServlet.java`: multipart, CSRF,
  HTTP/JSON/JSP, phục vụ ảnh qua Service.
- `../src/main/java/com/crm/service/users/AvatarImageProcessor.java`: validation,
  đọc có giới hạn, xác thực nội dung, decode/crop/resize.
- `../src/main/java/com/crm/service/users/AvatarService.java`: lưu cặp file,
  transaction cập nhật đường dẫn, dọn ảnh cũ hoặc upload thất bại.
- `../src/main/java/com/crm/service/users/AvatarException.java`: lỗi nghiệp vụ.
- `../src/main/java/com/crm/dao/users/AvatarDAO.java`: SELECT / khóa user / upsert.
- `../src/main/java/com/crm/model/UserAvatar.java`: hai storage key.
- `../database/migrations/CRM-36-avatar.sql`: migration chạy lại được.
- `../../frontend/WEB-INF/views/users/avatar.jsp`: form và preview, chỉ truy cập
  qua Servlet; đặt dưới WEB-INF để không bỏ qua Filter khi gọi JSP trực tiếp.
- `../../frontend/css/users/avatar.css`: giao diện form.
- `../src/test/java/com/crm/service/users/Avatar*Test.java`: unit tests.
- `../tests/CRM36AvatarHttpCheck.java`: kiểm thử HTTP/JDBC có chủ đích.
- Chỉnh `../database/schema.sql`, `../../frontend/jsp/shared/header.jsp` và
  `../../frontend/css/shared/header.css`: schema mới, link đổi ảnh và thumbnail.

## Quy tắc ảnh

1. Chấp nhận đuôi `.jpg`, `.jpeg`, `.png`, không phân biệt hoa thường.
2. Giới hạn **2 MiB = 2.097.152 byte**, bao gồm đúng ngưỡng. Đọc stream có
   giới hạn ngay cả khi kích thước khai báo sai. Multipart tổng tối đa 2 MiB +
   64 KiB; giới hạn này gồm token và multipart headers.
3. ImageIO xác định định dạng nội dung và phải khớp đuôi. Không tin MIME do
   browser gửi. GIF/PDF/EXE hoặc dữ liệu đổi đuôi bị từ chối; ảnh rỗng/hỏng lỗi 400.
4. Đọc kích thước trước decode, từ chối quá 16 triệu pixel để giới hạn bộ nhớ.
5. **Backend crop giữa ảnh 1:1**, resize 512×512; thumbnail từ ảnh đã xử lý
   128×128. Ảnh nhỏ được phóng lên. Giữ alpha PNG. Không có tọa độ crop phía FE.
   Crop theo pixel đã decode, chưa tự xoay theo EXIF của điện thoại.
6. Encode lại thành PNG, không giữ metadata/file gốc. Tên là UUID.png và
   UUID-thumb.png; không dùng filename người dùng làm đường dẫn.
7. Lưu cả hai file rồi upsert metadata trong transaction. Khóa hàng `users`
   giúp hai upload đồng thời cùng tài khoản không ghi đè lẫn nhau.
   Chỉ xóa cặp ảnh cũ sau commit. Lỗi trước commit dọn cặp ảnh mới.

Nếu mất kết nối đúng lúc commit, kết quả commit có thể không xác định: giữ file
để không làm mất ảnh đã được DB tham chiếu. Lỗi dọn file được log. Crash hoặc
xóa user có thể để lại file không tham chiếu; khi vận hành cần đối chiếu storage
với bảng trước khi dọn, tránh dọn trong lúc có upload. Backup DB cùng thư mục ảnh.

## Contract bổ sung

| Method / URL | Hành vi |
|---|---|
| GET `/profile/avatar` | Form JSP, token CSRF và preview |
| POST `/profile/avatar` | Form multipart; thành công redirect về trang |
| GET `/api/users/me/avatar` | JSON metadata, URL ảnh và `data.csrfToken` |
| POST `/api/users/me/avatar` | Multipart `avatar` và `csrfToken`, hoặc header `X-CSRF-Token` |
| GET `/profile/avatar/image` | PNG 512×512 của người đăng nhập |
| GET `/profile/avatar/thumbnail` | PNG 128×128 của người đăng nhập |

API JSON: `{ "success": true/false, "message": "...", "data": {...} }`.
GET metadata trả `hasAvatar`, `csrfToken`, `imageUrl`, `thumbnailUrl`.
POST thành công trả hai URL; client GET lại URL để tải nội dung mới.
HTTP: 400 ảnh/multipart không hợp lệ; 401 chưa đăng nhập; 403 CSRF hoặc tài khoản
không hoạt động; 404 chưa có file ảnh; 413 quá dung lượng; 415 sai Content-Type;
500 lỗi lưu file/DB. Các endpoint riêng tư có `Cache-Control: no-store`.

## Migration và chạy trên Tomcat 10.1

1. Dùng JDK 21. Với DB mới: chạy `../database/schema.sql`, rồi `../database/data.sql`
   như CRM-21. Với DB hiện có: chọn đúng database rồi chạy
   `../database/migrations/CRM-36-avatar.sql` **trước khi deploy WAR**. Không chạy
   lại toàn bộ schema vào DB hiện có để thay migration.
2. Cấu hình `CRM_DB_URL`, `CRM_DB_USERNAME`, `CRM_DB_PASSWORD` theo môi trường.
3. Đặt `CRM_AVATAR_DIR` thành đường dẫn thư mục bền vững ngoài WAR, ví dụ
   `C:/crm-data/avatars`. Nếu không đặt: `${catalina.base}/data/avatars`.
   Tài khoản chạy Tomcat cần quyền tạo/đọc/ghi/xóa file tại đây; thư mục này
   phải do server quản lý, không cho người dùng khác sửa storage key/symlink.
4. Mở terminal tại `backend`, chạy `mvn clean package`.
5. Dừng Tomcat của môi trường cần triển khai, deploy `target/ROOT.war` theo
   quy trình hiện có, rồi khởi động lại bằng `bin/catalina.bat run` (Windows).
   Nếu dùng context khác ROOT, URL frontend vẫn dùng context path tương ứng.
6. Đăng nhập bằng `/login`, chọn **Đổi ảnh đại diện** trên header hoặc truy cập
   `/profile/avatar`.

## Kiểm thử và kết quả

Đã chạy ngày 2026-09-29: Maven build thành công, **16 unit tests pass**
(11 mới + 5 có sẵn). Triển khai WAR trên Tomcat **10.1.60**, JDK **21**,
MySQL **8.0.46** riêng: **57 kiểm tra HTTP/JDBC pass**.

| Trường hợp | Kết quả mong đợi / đã kiểm tra |
|---|---|
| JPG/JPEG và PNG dưới 2 MiB | Accept; unit kiểm tra cả đuôi JPEG viết hoa |
| Đúng 2 MiB / hơn 1 byte | Accept / HTTP 413 |
| PDF, GIF, EXE | HTTP 400 |
| File giả `.jpg`, GIF đổi đuôi | HTTP 400 |
| Ảnh ngang / dọc / vuông | 512×512 và 128×128 |
| Crop giữa thay vì kéo méo | Unit kiểm tra màu ở hai góc ảnh |
| File rỗng / PNG hỏng / quá 16 triệu pixel | Reject |
| Đường dẫn DB | Hai file tồn tại, UUID server sinh |
| Upload thay thế | Chỉ còn cặp ảnh hiện tại |
| Người chưa đăng nhập / CSRF sai | 401 / 403 |
| Người khác thêm `?userId=...` | Không đọc được avatar của tài khoản khác |
| Lỗi DB thật trong upload | 500, giữ ảnh cũ, dọn file mới |
| Storage lỗi / tài khoản inactive / commit không rõ | Unit kiểm tra từng nhánh |
| JSP trước/sau upload, form POST | JSP biên dịch; POST redirect đúng |

Harness tích hợp **chỉ chạy trên database tạm dùng để test**: tạo user synthetic,
đổi tên tạm bảng để giả lập lỗi SQL. Không chạy trên DB dùng chung/production.
Sau khi deploy WAR vào Tomcat thử nghiệm và tạo schema, tại `backend`:

```powershell
javac -encoding UTF-8 -cp 'target/classes;target/ROOT/WEB-INF/lib/*' -d target/crm36-check tests/CRM36AvatarHttpCheck.java
java -cp 'target/crm36-check;target/classes;target/ROOT/WEB-INF/lib/*' CRM36AvatarHttpCheck http://127.0.0.1:8086 'jdbc:mysql://127.0.0.1:3308/crm_db?useSSL=false&allowPublicKeyRetrieval=true' root 'C:/test/avatars'
```

Thay base URL/JDBC/user/thư mục cho đúng môi trường riêng của bạn;
`CRM36_TEST_DB_PASSWORD` cung cấp mật khẩu JDBC cho harness nếu cần.

Debug: xem HTTP status/message trước, tiếp theo log Tomcat (`Avatar upload
failed`), kiểm tra migration, quyền storage, rồi DB config. 413 ở reverse proxy
hoặc Tomcat trước Servlet cần chỉnh giới hạn request của hạ tầng tương ứng.
Không trả stack trace hoặc đường dẫn filesystem cho client.

## Git

Branch: `feature/CRM-36-avatar-upload`, tạo từ `origin/develop`.
Commit: `feat(CRM-36): implement avatar upload and thumbnail`.
PR: base `develop`, compare `feature/CRM-36-avatar-upload`.
Chỉ merge sau khi team review; không merge trực tiếp vào main.
