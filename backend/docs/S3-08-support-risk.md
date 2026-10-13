# S3-08 – Hỗ trợ sau bán & cờ rủi ro

Người thực hiện: Hoàng Trọng Thái. Nhánh: `feature/BE-S3-08-support-risk`.

## Khảo sát và phạm vi

Đã đọc Customer Servlet/Service/DAO, Customer360, authentication/permission/DataScope và migration. Repository chưa có HTTP contract hoặc Backend Support/Risk/Notification; FE support-tickets hiện lưu localStorage. Backend giữ tên trường và các enum đang dùng trong source FE, không chỉnh Frontend. Các API dưới đây là phần Backend bổ sung; cần tích hợp FE riêng. Endpoint Customer hiện có giữ nguyên, Customer360 chỉ thêm `data.churnRisk`.

## API

Tất cả endpoint yêu cầu session đăng nhập hợp lệ và dùng ApiResponse chung (`success`, `message`, `data`). URL tính từ context path của WAR.

- `POST /api/support-requests`: tạo, HTTP 201.
- `GET /api/support-requests?customerId=123&status=NEW&page=1&size=20`: danh sách trong phạm vi; bộ lọc tùy chọn; `data` gồm `items`, `page`, `size`.
- `GET /api/support-requests/{id}`: chi tiết.
- `PUT /api/support-requests/{id}`: cập nhật một hoặc nhiều trường.
- `DELETE /api/support-requests/{id}`: xóa mềm và lưu audit.
- `GET /api/customers/{id}/360`: API hiện có, bổ sung `data.churnRisk`.
- `GET /api/notifications?page=1&size=20`: hộp thông báo riêng; `data` gồm `items`, `unreadCount`, `page`, `size`.
- `PUT /api/notifications/{id}/read`: đánh dấu đã đọc, idempotent.

POST/PUT support dùng `Content-Type: application/json`. Ví dụ cấu trúc request (ID phải lấy từ dữ liệu thật):

```json
{"customerId":123,"title":"Nội dung yêu cầu","description":"Chi tiết","priority":"HIGH","assigneeId":456,"status":"NEW"}
```

POST bắt buộc customerId/title/assigneeId; mặc định priority MEDIUM, status NEW. PUT giữ nguyên trường không gửi; `description:null` xóa mô tả. Không cho chuyển ticket sang Customer khác. Chỉ nhận sáu trường trên; ID là số nguyên dương, không nhận ID dạng chuỗi. Title tối đa 255 ký tự, description 10000, JSON tối đa 16384 ký tự. Trang >=1, size 1..100.

Priority: `URGENT`, `HIGH`, `MEDIUM`, `LOW`. Status: `NEW`, `PROCESSING`, `WAITING_CUSTOMER`, `RESOLVED`, `CLOSED`.

Ticket trả về id/customerId/title/description/priority/assigneeId/status và createdBy/updatedBy/createdAt/updatedAt. HTTP lỗi: 400 validation, 401 chưa đăng nhập, 403 thiếu quyền/phạm vi, 404 không tồn tại, 405 sai method, 409 xung đột database, 413 JSON vượt giới hạn, 500 lỗi server đã ẩn chi tiết SQL. Notifications không trả thông báo của người khác.

## Quyền và transaction

CRUD cần lần lượt `support.create/read/update/delete`. Đọc cần thêm Customer read scope; thay đổi cần Customer update scope. SELF/TEAM/ALL tái sử dụng DataScopeService, tính theo Sales Owner Customer. Người xử lý phải là tài khoản ACTIVE; người xử lý không quyết định phạm vi Customer. Customer đã xóa/gộp không được nhận ticket mới.

Mỗi mutation dùng JDBC PreparedStatement và transaction READ_COMMITTED. Khóa Customer bằng SELECT FOR UPDATE trước khi khóa ticket, ghi ticket, đếm lại, cập nhật risk và tạo notification trên cùng connection; lỗi bất kỳ bước nào rollback toàn bộ. Khóa Customer tuần tự hóa các writer support của cùng Customer. Xóa mềm không còn được tính vào risk.

## Risk và notification

Đếm các ticket chưa xóa ở trạng thái NEW/PROCESSING/WAITING_CUSTOMER. `openTicketCount >= N` bật cờ; dưới N hạ cờ. RESOLVED/CLOSED không tính. Không thay đổi trạng thái kinh doanh `customers.status`.

N mặc định 2 trong `src/main/resources/support-risk.properties`; môi trường `CRM_SUPPORT_RISK_THRESHOLD` ưu tiên hơn file, nhận số nguyên 1..10000. Config sai gây lỗi rõ ràng. Config tải khi service khởi tạo, cần restart/redeploy khi đổi.

```powershell
$env:CRM_SUPPORT_RISK_THRESHOLD = '2'
```

Đặt biến trong môi trường khởi chạy Tomcat; nếu Tomcat là Windows service, cấu hình môi trường service tương ứng. Đọc Customer360 cũng tính lại dưới cùng khóa, để đồng bộ sau đổi ngưỡng/Owner hoặc ticket được chuyển bởi thao tác gộp Customer hiện hữu. Không có background job quét toàn bộ Customer.

`data.churnRisk` chứa customerId/customerName/isRisk/openTicketCount/riskThreshold/riskLevel/reasons. riskLevel là MEDIUM khi đạt ngưỡng, NONE khi chưa đạt. Đây là quy tắc đếm ticket, chưa có quy tắc SLA hoặc cờ thủ công.

Mỗi lần chuyển từ không risk sang risk tăng episode và thông báo Sales Owner hiện tại. Unique event_key theo Customer/episode/recipient chống thông báo lặp trong cùng đợt. Nếu đổi Owner khi vẫn risk, lần xử lý support/đọc 360 tiếp theo thông báo Owner mới một lần; quay lại Owner đã được thông báo trong đợt đó không tạo bản sao. Hộp thư yêu cầu notification.read và Customer read scope, chỉ trả thông báo của chính người đăng nhập còn trong phạm vi. Thông báo là in-app lưu database; chưa có email/push.

## Migration và triển khai

Áp dụng `database/016_s3_08_support_risk.sql` sau các migration hiện hữu, trước deploy WAR mới. File thêm support_requests/notifications, bốn cột support_risk trên customers, index/FK/check/unique và quyền support.*. Các role ADMIN/DIRECTOR/TEAM_LEAD/SALES_REP/CUSTOMER_SUCCESS được cấp quyền mới; không sửa scope hoặc dữ liệu Customer cũ. Metadata database đang dùng chưa có hai bảng này. Migration chưa được áp dụng vào database CRM trong phiên làm việc này.

Trong PowerShell từ thư mục backend, chạy MySQL client và chọn đúng database:

```powershell
mysql -u <db_user> -p <crm_database>
```

Trong MySQL client (thay đường dẫn nếu checkout khác):

```sql
SOURCE C:/Users/thang/Projects/customer-relationship-manager-ictu-n5/backend/database/016_s3_08_support_risk.sql;
```

Migration dùng DELIMITER/procedure, cần client hỗ trợ và quyền CREATE/ALTER/CREATE ROUTINE. MySQL DDL không rollback chung như transaction CRUD. CREATE IF NOT EXISTS không tự sửa một bảng cùng tên có schema khác; kiểm tra schema trước áp dụng nếu môi trường deploy có bảng ngoài repository.

## File bàn giao

Java production mới:

- src/main/java/com/crm/config/SupportRiskConfig.java
- src/main/java/com/crm/dto/support/SupportRequestWriteRequest.java
- src/main/java/com/crm/controller/support/SupportRequestServlet.java
- src/main/java/com/crm/service/support/SupportRequestService.java
- src/main/java/com/crm/dao/support/SupportRequestDAO.java
- src/main/java/com/crm/controller/notifications/NotificationServlet.java
- src/main/java/com/crm/service/notifications/NotificationService.java
- src/main/java/com/crm/dao/notifications/NotificationDAO.java

Java sửa: src/main/java/com/crm/service/customers/Customer360Service.java (thêm service risk và trường churnRisk, hai dòng).

SQL mới: database/016_s3_08_support_risk.sql.

File khác mới:

- src/main/resources/support-risk.properties
- src/test/java/com/crm/service/support/SupportRiskJdbcTest.java
- src/test/java/com/crm/service/support/SupportValidationTest.java
- src/test/java/com/crm/controller/support/SupportServletTest.java
- docs/S3-08-support-risk.md

## Kiểm chứng và giới hạn

17 tests đã đạt: 10 JDBC MySQL dùng bảng TEMPORARY trong session riêng (không sửa schema/dữ liệu CRM), 3 validation, 4 Servlet HTTP/JSON/mapping. Kiểm tra CRUD, trạng thái tính risk, lên/hạ cờ, chống notification lặp, đổi Owner, scope SELF/TEAM, quyền bị từ chối, recipient riêng, rollback sau lỗi notification/risk, partial update và payload lỗi.

Build bàn giao: `mvn clean package -DskipTests`. Các AC đã có logic Backend tương ứng. Chưa chạy migration trên database thật, chưa deploy kiểm thử end-to-end trên Tomcat với session thật, chưa stress-test request đồng thời và chưa tích hợp FE. Test JDBC chạy thực với dữ liệu fixture TEMPORARY, không phải API production giả.

Không stage/commit/push trong phiên này. Commit dự kiến sau khi Hoàng Trọng Thái kiểm tra: `feat(BE-S3-08): implement support requests and risk flags`.
