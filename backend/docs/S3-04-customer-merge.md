# BE S3-04 — Phát hiện và gộp khách hàng trùng
Người thực hiện: Hoàng Trọng Thái.
Nhánh: feature/BE-S3-04-customer-merge.

## Source và giả định Contract
Đã kiểm tra CustomerServlet/Service/DAO, Customer360DAO, CustomerRelationDAO,
OpportunityDAO, ActivityDAO, CustomFieldValueDAO, AuthenticationFilter,
AuthorizationService, DataScopeService, PermissionDAO, AuditLogDAO và migrations.
Nhánh này chưa có ContactDAO; bảng contacts đã có trong migration 012 và được
Customer360DAO đọc. Không cần thêm CRUD Contact hoặc lấy code từ Story khác.
Không tìm thấy Contract HTTP duplicate/compare/merge trong file Git quản lý.
FE đang dùng DuplicateMergeEngine local, không gọi HTTP, với masterId,
secondaryId, fieldOverrides. Backend giữ các tên đó và ghi rõ route bổ sung bên dưới.
Không sửa FE, URL/JSON CRUD Customer, hoặc các DAO của thành viên khác.

## API
- GET /api/customers/duplicates?customerId=123
  data: {isDuplicate, matches:[{record,reasons,similarityScore}]}.
  Bỏ customerId để quét cặp trong scope:
  data: {items:[{record,matches:[{record,reasons,similarityScore}]}],scanned}.
  Mỗi cặp chỉ xuất hiện một lần.
- GET /api/customers/compare?masterId=123&secondaryId=456
  data: {master,secondary,masterRelated,secondaryRelated,selectableFields}.
  master/secondary chứa trường Customer camelCase và customFields.
- POST /api/customers/merge
  application/json:
  {"masterId":123,"secondaryId":456,"fieldOverrides":{"companyName":"Tên lấy từ một trong hai Customer"}}
  data: {masterId,secondaryId,master,transferred,merged:true}.
  Bỏ fieldOverrides để giữ các trường của master.

Toàn bộ JSON dùng ApiResponse {success,message,data}.
master là bản ghi giữ lại; secondary là nguồn sẽ được đánh dấu đã gộp.
fieldOverrides chỉ chấp nhận:
name/companyName, taxCode, status, email, phone, website, address,
industryId, companySizeId, ownerUserId.
Giá trị phải lấy từ một trong hai Customer hiện tại, không chấp nhận giá trị giả,
trường tùy ý, chủ sở hữu thứ ba hoặc dữ liệu đã cũ không còn trong hai bản ghi.
name/companyName cùng là trường name; lựa chọn mâu thuẫn bị từ chối.
Actor lấy từ session; operatorUser/matchedReasons từ client không quyết định quyền/audit.
Custom fields không có ở master được sao chép; xung đột giữ master,
giá trị source vẫn được lưu trên source và trong snapshot lịch sử.

HTTP: 400 dữ liệu không hợp lệ; 401 chưa đăng nhập; 403 không có quyền;
404 Customer không tồn tại; 405 sai method; 409 đã merge/đã xóa, vòng lặp
phân cấp, khóa hoặc ràng buộc xung đột; 413 JSON vượt 16KB; 500 lỗi máy chủ/schema.
Không trả thông báo SQL/driver cho client.
Mapping Servlet là ba route chính xác, khác với mapping /api/customers và
/api/customers/* của CustomerServlet, nên không trùng mapping.

## Duplicate và compare
Mã số thuế chuẩn hóa bỏ khoảng trắng/dấu ngăn. So khớp toàn bộ mã;
cùng mã gốc 10 số cũng được đánh dấu lý do chi nhánh, chỉ là gợi ý.
Website so khớp domain, bỏ scheme, www, path và query; bỏ URL không hợp lệ.
Tên bỏ dấu tiếng Việt và một số cụm loại hình pháp lý; tính Jaccard theo
tập từ, gợi ý khi >= 0.65, giống ngưỡng engine FE hiện hữu.
Không tự động merge khi tên/domain/mã gốc giống nhau.
Quét toàn scope tối đa 1.000 Customer; theo một customerId tối đa 10.000.
Vượt giới hạn trả lỗi rõ ràng, không cắt dữ liệu rồi báo kết quả đầy đủ.
Compare dùng snapshot transaction chỉ đọc để hai bản ghi và số lượng liên kết
nhất quán trong cùng lần so sánh.

## Quyền và transaction
AuthenticationFilter hiện hữu bảo vệ /api/*, kiểm tra ACTIVE và sessionVersion.
Duplicate/compare yêu cầu customer.read và scope của cả hai Customer.
Merge yêu cầu customer.merge, customer.update, vai trò ADMIN/DIRECTOR/TEAM_LEAD,
đồng thời kiểm tra cả hai scope theo owner của source/master và công ty con cần chuyển.
Không dựa vào vai trò, scope hoặc người thực hiện do client gửi lên.
Role khác dù được cấp nhầm permission customer.merge vẫn bị từ chối.

Tất cả write đi qua một JDBC Connection với autoCommit=false.
Khóa customers theo thứ tự ID, đồng bộ với cơ chế khóa hierarchy hiện có.
Các bảng tham gia phải là InnoDB; thiếu migration/nontransactional table sẽ dừng
trước write. Lỗi bất kỳ bước nào, kể cả history hoặc audit, rollback toàn bộ.
Đổi primary: giữ primary đang active của master nếu có, nếu không giữ primary
active của source; nếu nhiều primary cũ, giữ ID nhỏ nhất theo thứ tự ưu tiên trên.
Không tự tạo primary khi cả hai chưa chỉ định; tất cả Contact giữ nguyên ID.
Chuyển cả dữ liệu đã soft delete, không bỏ Opportunity/Activity hoặc giá trị tiền.
Không sửa owner, amount, opportunity links hoặc lịch sử tương tác của các bảng con.
Trường hợp master nằm dưới source trong hierarchy: tách master khỏi nhánh nguồn
trước khi chuyển các công ty con, tránh tạo vòng lặp.
Nếu cây hiện tại bị vòng lặp hoặc công ty con ngoài scope, merge bị từ chối.

DAO khám phá FK thực tế tham chiếu customers.id và các cột customer_id trong
BASE TABLE của schema hiện tại. Do đó Opportunity, Activity, quotes,
customer_contracts, customer_attachments, support request nếu tồn tại, và các
liên kết cùng quy ước được chuyển mà không hardcode dữ liệu.
Identifier chỉ lấy từ metadata, kiểm tra whitelist ký tự; giá trị dùng PreparedStatement.
Nếu bảng liên kết có unique/composite constraint không thể chuyển, toàn merge
rollback và trả 409, không tự xóa bản ghi để giải quyết xung đột.
customer_merge_history và contact_company_history được giữ nguyên, vì chúng ghi
Customer tại thời điểm lịch sử. Audit cũ cũng không bị viết lại.

Sau chuyển xong, source soft delete, status DA_GOP, có merged_into_id/merged_at/merged_by.
Source không bị xóa vật lý. tax_code của source được giải phóng để ưu tiên tax_code
source cho master không vướng unique; giá trị tax_code gốc được lưu trong before_source.
Snapshot source/master, primary IDs, custom values, người/thời gian, trường lựa chọn
và số lượng chuyển được ghi vào customer_merge_history. Audit MERGE được insert
trong cùng transaction, không gọi AuditLogService mở Connection riêng.

## Migration và vận hành
File mới: database/014_s3_04_customer_merge.sql.
Tên riêng S3-04; không sửa migration cũ. Chạy trên đúng DB sau 007 (role/scope),
008 (audit compatibility), 012 (Customer foundation) và 013.
Bổ sung merged_into_id/merged_at/merged_by, customer_merge_history,
permission customer.merge cho ADMIN/DIRECTOR/TEAM_LEAD.
Không sửa dữ liệu Customer cũ. Có thể chạy lại, nhưng DDL MySQL auto-commit,
nên kiểm tra schema nếu chạy dở. Chưa áp dụng lên schema CRM đang chạy.

PowerShell:
mysql -u root -p crm_db
Trong mysql:
SOURCE C:/Users/thang/Projects/customer-relationship-manager-ictu-n5/backend/database/014_s3_04_customer_merge.sql;

Build tại backend:
mvn clean package -DskipTests
Cache Maven Windows nếu cần:
mvn "-Dmaven.repo.local=C:\Users\thang\.m2\repository" clean package -DskipTests
Deploy backend/target/crm.war lên Tomcat 10.1 sau migration.

## Acceptance Criteria và kiểm chứng
AC1: duplicate theo MST/domain/tên, có lý do; không merge tự động.
AC2: compare hai Customer, có fieldOverrides chọn trường và masterId chọn bản ghi chính.
AC3: chuyển Contact/Opportunity/Activity và các liên kết được khám phá, giữ giá trị tiền,
xử lý primary, hierarchy và custom fields.
AC4: authentication hiện có + role restriction + permissions + scope.
AC5: một transaction, rollback toàn bộ nếu lỗi.

15 kiểm thử S3-04 đạt: matcher, HTTP validation/mapping, MySQL JDBC merge,
primary, tiền, custom fields, scope, role, archive, hierarchy và rollback tại
5 bước (contacts/activity/target/archive/history).
JDBC dùng TEMPORARY TABLE chỉ tồn tại trong session, không sửa bảng/dữ liệu CRM.
Scope/role trong test được inject để kiểm tra các nhánh nghiệp vụ; chưa kiểm thử
end-to-end tài khoản/session thực tế trên Tomcat.
Maven WAR build thành công. Còn bước triển khai migration và kiểm thử Tomcat.
Không thực hiện S3-08; không commit/push.

## Danh sách file mới — chỉ S3-04
backend/src/main/java/com/crm/controller/customers/CustomerMergeServlet.java
backend/src/main/java/com/crm/service/customers/CustomerMergeService.java
backend/src/main/java/com/crm/service/customers/CustomerDuplicateMatcher.java
backend/src/main/java/com/crm/dao/customers/CustomerMergeDAO.java
backend/src/main/java/com/crm/dto/customers/CustomerMergeRequest.java
backend/database/014_s3_04_customer_merge.sql
backend/src/test/java/com/crm/service/customers/CustomerMergeJdbcTest.java
backend/src/test/java/com/crm/service/customers/CustomerDuplicateMatcherTest.java
backend/src/test/java/com/crm/controller/customers/CustomerMergeServletTest.java
backend/docs/S3-04-customer-merge.md

Sau khi người dùng kiểm tra và xác nhận:
git branch --show-current
git add -- backend/src/main/java/com/crm/controller/customers/CustomerMergeServlet.java backend/src/main/java/com/crm/service/customers/CustomerMergeService.java backend/src/main/java/com/crm/service/customers/CustomerDuplicateMatcher.java backend/src/main/java/com/crm/dao/customers/CustomerMergeDAO.java backend/src/main/java/com/crm/dto/customers/CustomerMergeRequest.java backend/database/014_s3_04_customer_merge.sql backend/src/test/java/com/crm/service/customers/CustomerMergeJdbcTest.java backend/src/test/java/com/crm/service/customers/CustomerDuplicateMatcherTest.java backend/src/test/java/com/crm/controller/customers/CustomerMergeServletTest.java backend/docs/S3-04-customer-merge.md
git diff --cached --stat
git diff --cached --check
git commit -m "feat(BE-S3-04): implement customer duplicate detection and merge"
git push -u origin feature/BE-S3-04-customer-merge
Xác nhận Git user.name/user.email là danh tính Hoàng Trọng Thái trước commit.
