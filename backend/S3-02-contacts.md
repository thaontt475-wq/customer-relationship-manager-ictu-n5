# BE S3-02 — Người liên hệ & vai trò mua
Người thực hiện: Nguyễn Văn Thắng.

## Source và phạm vi
Tái sử dụng bảng contacts từ database/012_sprint3_customer_foundation.sql.
Customer360DAO đã đọc các trường camelCase của bảng này; không sửa module Customer hay FE.
AuthenticationFilter trong web.xml đã bảo vệ /api/* (kiểm tra tài khoản ACTIVE và sessionVersion).
Các file bổ sung: controller/contacts/ContactServlet, service/contacts/ContactService,
dao/contacts/ContactDAO, dto/contacts/ContactWriteRequest và ContactTransferRequest.
ApiResponse và ResponseUtil được tái sử dụng.

## API
Tất cả response dùng ApiResponse {success, message, data}.
- GET /api/contacts: danh sách Contact trong phạm vi Customer.
- GET /api/contacts?customerId={id}: danh sách theo Customer.
- GET /api/contacts/{id}: chi tiết.
- POST /api/contacts: tạo, trả 201.
- PUT /api/contacts/{id}: cập nhật thông tin, không chuyển công ty.
- DELETE /api/contacts/{id}: soft delete, bỏ primary, giữ lịch sử.
- PUT /api/contacts/{id}/primary: chỉ định đầu mối chính.
- POST /api/contacts/{id}/transfer: chuyển Customer và ghi lịch sử.
- GET /api/contacts/{id}/history: lịch sử chuyển, mới nhất trước.

JSON tạo/cập nhật: customerId, fullName, title, email, phone, buyingRole, isPrimary.
fullName/title/email/phone/buyingRole bắt buộc khi tạo hoặc PUT.
customerId bắt buộc khi tạo; khi PUT có thể bỏ hoặc giữ nguyên.
isPrimary bỏ khi PUT sẽ giữ trạng thái hiện tại; false bỏ chỉ định.
buyingRole: DECISION_MAKER (quyết định), INFLUENCER (ảnh hưởng),
END_USER (người dùng cuối), BLOCKER (cản trở).
JSON transfer: customerId (đích, bắt buộc), note (tối đa 1000 ký tự), isPrimary.
Chuyển mặc định bỏ primary; isPrimary=true sẽ thay primary tại Customer đích.
Không tự chỉ định người khác khi xóa/chuyển/bỏ primary.

## Quyền và transaction
contact.read/create/update/delete bắt buộc theo thao tác.
Phạm vi SELF/TEAM/ALL dùng DataScopeService trên module customer.
Read yêu cầu customer.read; create/update/delete Contact yêu cầu customer.update.
Transfer kiểm tra customer.update và scope ở cả Customer cũ/mới trong transaction.
History kiểm tra Customer hiện tại và cả hai Customer của từng lần chuyển
(kể cả Customer đã soft delete); trả 403 nếu bất kỳ Customer nào ngoài phạm vi.
Create/update/primary/transfer/delete đều commit hoặc rollback trọn vẹn.
Khóa Contact và Customer bằng FOR UPDATE; unique generated column ở DB chặn nhiều
primary đang hoạt động cho cùng Customer. Deadlock/lock timeout trả 409 để thử lại.
Chuyển Contact và INSERT lịch sử cùng transaction; lỗi lịch sử rollback chuyển.
Không ghi dữ liệu mẫu, không trả thành công nếu Contact không tồn tại.

## Migration và vận hành
Chạy database/014_s3_02_contacts.sql một lần sau 012 và 013 trên MySQL 8.
Không chạy lại toàn bộ migration cũ chỉ để triển khai S3-02.
Trước khi chạy, kiểm tra:
SELECT customer_id, COUNT(*) FROM contacts
WHERE is_primary=1 AND is_deleted=0
GROUP BY customer_id HAVING COUNT(*)>1;
Nếu có trùng, trao đổi với chủ dữ liệu để chọn primary; migration không tự sửa dữ liệu.
014 bổ sung unique primary và contact_company_history, không tạo lại contacts.
DDL MySQL không rollback toàn bộ file; nếu chạy dở phải kiểm tra schema trước retry.
ADMIN được cấp đủ contact.*; các role khác giữ quyền hiện có.
contact.delete mới phải được cấp rõ ràng cho role cần xóa.
Dùng MySQL client từ PowerShell (password nhập qua prompt):
mysql -u root -p crm_db
Sau đó trong mysql:
SOURCE C:/Users/thang/Projects/customer-relationship-manager-ictu-n5/backend/database/014_s3_02_contacts.sql;

Build tại backend:
mvn clean package -DskipTests
Nếu Maven dùng sai C:\.m2:
mvn "-Dmaven.repo.local=C:\Users\thang\.m2\repository" clean package -DskipTests
WAR: backend/target/crm.war, deploy vào Tomcat 10.1 theo quy trình dự án.
Kiểm thử riêng S3-02 không ghi DB:
mvn "-Dmaven.repo.local=C:\Users\thang\.m2\repository" -Dtest=ContactValidationTest test

## Kết quả và phần chưa xác nhận
AC 1–7 đã có logic Backend trong source.
Build WAR thành công; 4 kiểm thử validation/HTTP đạt.
Chưa áp dụng migration lên DB đang chạy; chưa xác nhận integration MySQL/Tomcat,
scope thực tế theo từng role hoặc tranh chấp primary bằng request đồng thời.
Cần kiểm tra các luồng này trên DB kiểm thử trước khi đánh dấu nghiệm thu runtime.
Không triển khai S3-06.

## Commit và push (người dùng thực hiện)
Từ thư mục repository:
git switch -c feature/BE-S3-02-contacts
Nếu nhánh đã tồn tại: git switch feature/BE-S3-02-contacts
git add -- backend/database/014_s3_02_contacts.sql backend/src/main/java/com/crm/controller/contacts backend/src/main/java/com/crm/service/contacts backend/src/main/java/com/crm/dao/contacts backend/src/main/java/com/crm/dto/contacts backend/src/test/java/com/crm/contacts/ContactValidationTest.java backend/S3-02-contacts.md
git diff --cached --stat
git diff --cached --check
git commit -m "feat(BE-S3-02): implement contact management and buying roles"
git push -u origin feature/BE-S3-02-contacts
Kiểm tra git config user.name/user.email là danh tính Git của Nguyễn Văn Thắng trước commit.
Không dùng git add . để tránh gom thay đổi ngoài S3-02.

## Comment Jira/PR — chỉ dùng sau khi push thành công
Người thực hiện: Nguyễn Văn Thắng.
Đã push Backend S3-02 trên feature/BE-S3-02-contacts: CRUD Contact, danh sách theo
Customer, bốn vai trò mua, primary duy nhất, chuyển công ty cùng lịch sử và API
truy xuất lịch sử. Đã áp dụng kiểm tra đăng nhập, contact permissions, scope Customer
SELF/TEAM/ALL; transfer kiểm tra cả công ty cũ/mới. Dùng PreparedStatement,
transaction/rollback, soft delete và ApiResponse. Tái sử dụng contacts; bổ sung
migration 014_s3_02_contacts.sql. Không sửa FE.
Validation: build WAR thành công; 4 kiểm thử validation/HTTP đạt.
Còn cần chạy migration và integration MySQL/Tomcat để xác nhận nghiệm thu runtime.
