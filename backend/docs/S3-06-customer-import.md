# Backend S3-06 — Import khách hàng Excel

Người thực hiện: Nguyễn Văn Thắng.
Nhánh: feature/BE-S3-06-import-customers.

## Source đã kiểm tra và phạm vi
- pom.xml đã có poi-ooxml 5.4.1; không thêm dependency.
- CustomerServlet, CustomerService, CustomerDAO, CustomerWriteRequest và schema 009/012.
- AuthenticationFilter (ACTIVE/sessionVersion), AuthorizationService và DataScopeService.
- UserImportServlet/UserImportService để giữ quy ước template → preview → confirm.
Không có API Contract import Customer trong các file được Git quản lý tại thời điểm triển khai.
Do đó các route import được bổ sung theo quy ước hiện hữu, dưới prefix /api/customers.
Không sửa endpoint/JSON CRUD Customer, source Customer của thành viên khác hoặc Frontend.
Không tạo mapping trùng: /api/customers/import/* cụ thể hơn /api/customers/*.
Servlet container chọn mapping cụ thể hơn cho các request import.

## API triển khai
1. GET /api/customers/import/template
   Trả application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,
   Content-Disposition: attachment; filename="customer_import_template.xlsx".
2. POST /api/customers/import/preview
   multipart/form-data với Part "file", file .xlsx.
3. POST /api/customers/import/confirm
   application/json:
   {"batchToken":"token-do-preview-tra-ve","duplicateMode":"SKIP"}
   duplicateMode bắt buộc là SKIP hoặc UPDATE; không ngầm UPDATE.

Response preview/confirm và tất cả lỗi dùng ApiResponse chung:
{"success":true|false,"message":"...","data":...}.

Preview data:
- batchToken, expiresInSeconds (900).
- totalRows, validCount, errorCount, duplicateCount.
- validRows, errorRows.
Mỗi dòng: row (số dòng Excel bắt đầu từ 2), name, taxCode, status, email,
phone, website, address, industryId, companySizeId, valid, errors (mảng thông báo),
duplicate, duplicateSource (DATABASE/FILE/null), canUpdate.
customerId chỉ xuất hiện khi Customer trùng nằm trong scope customer.read.
Không trả dữ liệu Customer hiện có nằm ngoài scope.
Một dòng có thể vừa bị trùng vừa có lỗi validation.

Confirm data:
- totalRows, created, updated, skipped, errorCount.
- errors: [{"row":2,"errors":["thông báo cụ thể"]}].
Bất biến: totalRows = created + updated + skipped + errorCount.
success=true cho biết đã xử lý yêu cầu báo cáo; không có nghĩa tất cả dòng thành công.
Các dòng validation lỗi từ preview vẫn được giữ trong báo cáo confirm.

HTTP: 400 dữ liệu/file/token không hợp lệ; 401 chưa đăng nhập; 403 không có quyền;
404 route không tồn tại; 405 sai method (kèm Allow); 413 quá dung lượng;
429 vượt giới hạn preview; 500 lỗi DB/cấu hình hoặc máy chủ.
Lỗi ghi riêng từng dòng được đưa vào báo cáo, không lộ thông báo SQL/driver.

## Excel và validation
Một sheet; tối đa 1.000 dòng dữ liệu, 32 cột vật lý, file tối đa 5MB.
Giới hạn giải nén tổng 64MB, mỗi thành phần 20MB, tối đa 1.000 thành phần,
tổng nội dung các ô tối đa 1 triệu ký tự; không macro, formula hoặc ô lỗi.
Không có dữ liệu khách hàng mẫu được tạo trong template hoặc database.
Các cột bắt buộc có trong header, cho phép đổi thứ tự:
name, taxCode, status, email, phone, website, address, industryId, companySizeId.
name là trường dữ liệu bắt buộc; các cột khác có thể trống theo schema.
status trống mặc định TIEM_NANG, nhận TIEM_NANG/DANG_GIAO_DICH/CHINH_THUC.
taxCode: 10 chữ số hoặc 13 chữ số chi nhánh (có thể ngăn bằng "-").
taxCode và phone phải lưu dạng Text để không mất số 0 đầu/không bị làm tròn.
Email, phone, URL http/https, ID nguyên dương và độ dài theo schema được kiểm tra.
industryId/companySizeId phải tồn tại, active và đúng type trong master_data.
Dòng trống bỏ qua; lỗi được tích lũy theo từng dòng.
Mã số thuế trùng trong file: tất cả các dòng có cùng mã bị báo lỗi, không chọn
tùy tiện dòng đầu/cuối. Dạng mã chi nhánh có/không "-" được coi là cùng một mã.
taxCode trống được lưu NULL; không suy đoán bản ghi trùng bằng tên hoặc email.

## Quyền, duplicate và transaction
- Template: customer.create.
- Preview: customer.create và customer.read; customer.update quyết định canUpdate.
- Confirm SKIP: customer.create.
- Confirm UPDATE: customer.create và customer.update.
Các quyền và SELF/TEAM/ALL được lấy bằng DataScopeService, không nhận scope từ client.
Customer mới luôn thuộc người import. UPDATE giữ owner_user_id, parent,
custom fields, contacts và các thông tin ngoài 9 cột Excel.
UPDATE thay thế 9 cột Excel; ô tùy chọn trống sẽ xóa giá trị cột đó,
status trống dùng mặc định. Đây là lựa chọn UPDATE rõ ràng của người dùng.

Preview không ghi DB. Token giữ snapshot phía server, thuộc người upload,
hết hạn sau 15 phút, dùng một lần, tối đa 3 preview/người và 64 preview/server.
Không chấp nhận client gửi lại rows hoặc tự thay owner qua confirm.
Quyền được kiểm tra lại lúc confirm; Customer trùng được đọc lại và khóa FOR UPDATE.
UPDATE kiểm tra scope theo owner hiện tại tại thời điểm xử lý từng dòng.
Không tự khôi phục Customer đã soft delete.
SKIP không ghi đè Customer trùng, kể cả ngoài scope UPDATE.
Nếu một Customer trùng mới xuất hiện/đổi ID sau preview, UPDATE báo lỗi và yêu cầu
preview lại, tránh ghi đè dữ liệu chưa từng được preview.
Mỗi dòng hợp lệ có một transaction: commit xong mới cộng bộ đếm; lỗi rollback dòng,
các dòng còn lại tiếp tục. Các danh mục tham chiếu được kiểm tra lại trong transaction.
Khóa unique tại DB chặn race giữa các request import hoặc CRUD tạo cùng mã số thuế.
Nếu request bị ngắt/restart khi đang confirm, các dòng đã commit vẫn tồn tại;
token ở RAM mất khi redeploy/restart. Hãy preview lại, không tự retry UPDATE mù.

## Migration và triển khai
File mới: database/015_s3_06_customer_import.sql.
Thực thi trên đúng database có schema Customer sau 009/012.
Migration thêm generated column tax_code_import_key và unique index;
không sửa/xóa dữ liệu cũ, không thay đổi permission hoặc role.
Schema 012 đã có UNIQUE tax_code nhưng chưa chuẩn hóa mã chi nhánh;
schema nâng cấp bằng 009 còn có thể thiếu UNIQUE.
Migration dừng nếu dữ liệu cũ có mã trùng sau chuẩn hóa. Chủ dữ liệu phải
quyết định cách xử lý; không tự gộp hay xóa.
Kiểm tra trước bằng:
SELECT NULLIF(REPLACE(TRIM(tax_code),'-',''),'') AS tax_key, COUNT(*) AS total
FROM customers
WHERE NULLIF(REPLACE(TRIM(tax_code),'-',''),'') IS NOT NULL
GROUP BY tax_key HAVING COUNT(*) > 1;

PowerShell:
mysql -u root -p crm_db
Trong MySQL:
SOURCE C:/Users/thang/Projects/customer-relationship-manager-ictu-n5/backend/database/015_s3_06_customer_import.sql;

Migration có thể chạy lại; DDL MySQL auto-commit nên luôn kiểm tra schema khi có lỗi.
Service kiểm tra unique key trước preview/confirm, không import nếu thiếu migration.
Chưa áp dụng migration lên bảng CRM hiện có trong quá trình triển khai này.
Deploy backend/target/crm.war lên Tomcat 10.1 sau migration theo quy trình dự án.

## Kiểm thử và build
Kiểm thử S3-06:
mvn "-Dmaven.repo.local=C:\Users\thang\.m2\repository" "-Dtest=CustomerExcelReaderTest,CustomerImportServiceTest,CustomerImportJdbcTest,CustomerImportServletTest" test
JDBC test cần kết nối MySQL theo DatabaseConfig; tạo TEMPORARY TABLE chỉ sống
trong session để kiểm thử SQL thật, không sửa schema/bản ghi CRM hiện hữu.
Kiểm thử scope SELF/TEAM/ALL dùng dữ liệu unit test trong bộ nhớ.
File .xlsx được đọc lại bằng Apache POI để xác nhận cấu trúc hợp lệ.

Build:
mvn clean package -DskipTests
Nếu Maven cache mặc định sai quyền trên máy Windows:
mvn "-Dmaven.repo.local=C:\Users\thang\.m2\repository" clean package -DskipTests

AC 1–6 có logic đầy đủ trong Backend và kiểm thử liên quan.
Chưa kiểm thử end-to-end trên Tomcat hay mở file trực tiếp bằng ứng dụng Excel.
Không tự commit/push; người dùng kiểm tra danh sách file trước.

## File S3-06 để kiểm tra và commit
- backend/database/015_s3_06_customer_import.sql
- backend/src/main/java/com/crm/controller/customers/CustomerImportServlet.java
- backend/src/main/java/com/crm/dao/customers/CustomerImportDAO.java
- backend/src/main/java/com/crm/dto/customers/CustomerImportConfirmRequest.java
- backend/src/main/java/com/crm/service/importer/CustomerExcelReader.java
- backend/src/main/java/com/crm/service/importer/CustomerImportService.java
- backend/src/test/java/com/crm/service/importer/CustomerExcelReaderTest.java
- backend/src/test/java/com/crm/service/importer/CustomerImportServiceTest.java
- backend/src/test/java/com/crm/service/importer/CustomerImportJdbcTest.java
- backend/src/test/java/com/crm/controller/customers/CustomerImportServletTest.java
- backend/docs/S3-06-customer-import.md

Sau khi kiểm tra, từ thư mục repository:
git branch --show-current
git add -- backend/database/015_s3_06_customer_import.sql backend/src/main/java/com/crm/controller/customers/CustomerImportServlet.java backend/src/main/java/com/crm/dao/customers/CustomerImportDAO.java backend/src/main/java/com/crm/dto/customers/CustomerImportConfirmRequest.java backend/src/main/java/com/crm/service/importer/CustomerExcelReader.java backend/src/main/java/com/crm/service/importer/CustomerImportService.java backend/src/test/java/com/crm/service/importer/CustomerExcelReaderTest.java backend/src/test/java/com/crm/service/importer/CustomerImportServiceTest.java backend/src/test/java/com/crm/service/importer/CustomerImportJdbcTest.java backend/src/test/java/com/crm/controller/customers/CustomerImportServletTest.java backend/docs/S3-06-customer-import.md
git diff --cached --stat
git diff --cached --check
git commit -m "feat(BE-S3-06): implement customer excel import"
git push -u origin feature/BE-S3-06-import-customers

Đảm bảo Git user.name/user.email là danh tính của Nguyễn Văn Thắng trước commit.
Không dùng git add . để tránh gom file ngoài phạm vi S3-06.
