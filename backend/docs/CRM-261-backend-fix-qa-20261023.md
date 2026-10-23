# CRM-261 — Sửa backend và kiểm thử NovaCRM

Ngày thực hiện: 23/10/2026, múi giờ Asia/Bangkok. Workspace: `C:/Users/thang/Projects/customer-relationship-manager-ictu-n5`.

Backend đã chạy `mvn clean verify` thành công: **355 PASS, 0 failure, 0 error, 0 skip**. WAR được tạo tại `backend/target/crm.war`. Kiểm tra FE bổ sung có **1 PASS, 4 FAIL** do DOM adapter của test thiếu `closest()`; không tuyên bố toàn bộ FE hoặc kiểm thử trình duyệt đã PASS.

## 1. Nguyên nhân và cách sửa

### POST /api/leads và PUT /api/leads/{id}

`LeadWriteRequest.fromJson()` chỉ chấp nhận `fullName`, `companyName`, `email`, `phone`, `sourceId`, `notes`. Request `{"ownerUserId":2}` bị parser từ chối đúng. Tuy nhiên, servlet bắt mọi `Exception`, chuyển cùng JSON sang model `Lead`, rồi POST gọi `dao.insert(lead)` hoặc PUT gọi `leads.updateLead(lead, user)`.

Nhánh POST bỏ qua chuẩn hóa/validation, catalog nguồn, giao dịch và scoring của service. DAO legacy còn có thể trả `-1` khi SQL thất bại nhưng servlet vẫn trả 201. Ngoại lệ sau một INSERT đã commit cũng có thể kích hoạt INSERT lần thứ hai. Nhánh PUT có cùng nguy cơ bỏ qua validation và thay thế đường kiểm tra phạm vi dữ liệu của service bằng một đường cập nhật khác.

Đã bỏ cả hai nhánh dự phòng. Servlet chỉ parse DTO rồi gọi `LeadService.create()` hoặc `LeadService.update()` một lần. Chỉ ghi response thành công khi service hoàn tất. Bộ xử lý ngoại lệ chung hiện có tiếp tục trả:

- 400: JSON/kiểu dữ liệu/field/validation không hợp lệ; giữ cấu trúc `data.errors` của `LeadValidationException`.
- 401/403: phiên đăng nhập hoặc phân quyền.
- 404: không có Lead trong phạm vi cập nhật.
- 409: Lead đã chuyển đổi/bị khóa hoặc xung đột nghiệp vụ.
- 500: lỗi SQL hoặc lỗi hệ thống; ghi log, không đổi thành thành công và không ghi lại qua DAO legacy.

Không thay đổi DTO, contract, scoring hook, giao dịch của service, endpoint import, SLA, merge hoặc conversion.

### Auto Assignment

Test `missingConfiguredRepDoesNotFallBackToUnrelatedSalesUser` phát hiện lỗi chức năng thực: nếu nhân viên trong rule không có trong catalog Sales hợp lệ, `assignLead()` lấy một Sales bất kỳ từ `allReps`. Nhân viên này có thể không thuộc rule.

Đã bỏ nhánh chọn nhân viên ngoài rule. Các cơ chế ROUND_ROBIN, LOAD_BALANCED và DEDICATED vẫn chọn trong danh sách cấu hình; nếu không có đối tượng hợp lệ, giữ hành vi đưa Lead về hàng chờ. Test có sẵn kiểm tra không phân nhầm Sales và từ chối ID thủ công sai định dạng đã PASS; toàn bộ 10 test Auto Assignment cũng PASS.

### Lỗi môi trường JDBC

Biến môi trường ban đầu trỏ tới `crm_db`. Không chạy suite với URL đó. Các assertion chỉ kiểm tra chuỗi URL trong một số test không thay thế việc xác minh catalog thật; một số nhóm khác không có guard tương đương.

Database `crm_sprint1_test` tồn tại nhưng thiếu schema Sprint 4: chưa có `lead_capture_forms`/`lead_capture_submissions`, chưa có catalog active `type='lead-source'`, và `lead_scores` dùng tên cột legacy `calculated_at` thay cho `scored_at`. Đây là lỗi môi trường, không sửa assertion hoặc skip test để che lỗi.

## 2. File và phương thức thay đổi trong phiên này

- `backend/src/main/java/com/crm/controller/leads/LeadServlet.java` — `service()`: bỏ fallback POST và PUT. Giữ các thay đổi dọn import đã có từ trước phiên.
- `backend/src/main/java/com/crm/service/leads/LeadAutoAssignmentService.java` — `assignLead()`: bỏ lựa chọn Sales ngoài rule.
- `backend/src/test/java/com/crm/controller/leads/LeadServletTest.java` — thêm overload helper `call()` để inject servlet và 7 regression test. Không xóa test hoặc giảm assertion cũ.
- Báo cáo này là tài liệu mới.

Các regression mới kiểm tra request POST/PUT không hợp lệ không mở DAO; chuẩn hóa và scoring trên đường thành công; SQL/system/security/conflict giữ đúng HTTP status và rollback; lỗi đọc sau commit không kích hoạt INSERT khác; quyền bị từ chối không mở DB; Lead ngoài phạm vi hoặc đã chuyển đổi không được cập nhật; import preview/confirm giữ contract multipart và token, đồng thời xóa multipart Part sau preview.

Fixture servlet kiểm tra đường điều khiển bằng DAO/connection giả. Kiểm tra giao dịch, Excel và scoring thật trên MySQL được thực hiện bởi suite JDBC, không suy luận persistence từ mock.

## 3. An toàn database và cấu hình chạy

Đã tạo database riêng **`crm_sprint1_test_crm261_20261023`**, sao chép schema/dữ liệu từ `crm_sprint1_test` bằng `mysqldump --single-transaction --no-tablespaces --skip-add-drop-table --skip-triggers`. Đã xác minh catalog đích và 30 bảng sau khi sao chép. Không có trigger trong schema nguồn.

Chỉ trên bản sao mới này:

1. Áp dụng migration có sẵn `024_s4_02_lead_manual_excel.sql`.
2. Áp dụng migration có sẵn `025_s4_05_lead_scoring.sql`.
3. Áp dụng migration có sẵn `028_s4_01_lead_capture.sql`.
4. Do `CREATE TABLE IF NOT EXISTS` của 025 không nâng cấp bảng legacy đã có, đổi tên cột bằng `ALTER TABLE lead_scores RENAME COLUMN calculated_at TO scored_at`. Giữ nguyên giá trị timestamp; không xóa hoặc làm lại bảng.

Đây là chuẩn bị schema theo contract và migration của ứng dụng, không điều chỉnh kết quả nghiệp vụ để test PASS. Không chạy script `031_full_qa_test_data.sql` đang có sẵn trong working tree. Không chạy DROP/TRUNCATE trên database hiện có. Các INSERT/UPDATE/DELETE của fixture chỉ diễn ra trên bản sao kiểm thử hoặc bảng TEMPORARY của kết nối đó.

Mỗi tiến trình Maven nhận URL qua biến môi trường riêng:

```powershell
$env:CRM_DB_URL = 'jdbc:mysql://127.0.0.1:3306/crm_sprint1_test_crm261_20261023?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh'
```

Username/password dùng cấu hình môi trường sẵn có; không hardcode, in mật khẩu hoặc đưa mật khẩu vào tham số dòng lệnh. Không thay `db.properties`, không đổi biến môi trường người dùng toàn cục. Trước lần chạy sau cần chọn lại URL kiểm thử trong tiến trình chạy Maven và kiểm tra catalog, vì cấu hình mặc định của môi trường vẫn trỏ `crm_db`.

Bản dump schema/dữ liệu `crm_sprint1_test` trước và sau đối chiếu giống nhau sau khi loại dòng thời gian hoàn tất dump. Bản sao QA được giữ lại để review, không tự xóa database. Dump cục bộ nằm trong thư mục `.local-qa/` đã được gitignore; không đưa dump vào báo cáo hoặc commit.

## 4. Kết quả kiểm thử thực tế

Ba lệnh yêu cầu đã được chạy lại theo đúng thứ tự trên mã cuối cùng, từ thư mục `backend/`:

1. `mvn "-Dtest=LeadServletTest#malformedJsonContentTypeAndConfirmationRowsRejected" test` — **PASS**: 1 test, 1 PASS, 0 failure/error/skip. Log: `.local-qa/CRM-261-20261023/test1-final.log`.
2. `mvn "-Dtest=LeadServletTest" test` — **PASS**: 30 test, 30 PASS, 0 failure/error/skip. Log: `.local-qa/CRM-261-20261023/test2-final.log`.
3. `mvn clean verify` — **PASS / BUILD SUCCESS**: 355 test, 355 PASS, 0 failure/error/skip; đóng gói WAR thành công. Log: `.local-qa/CRM-261-20261023/verify-final.log`.

Test 1 và 2 thuộc suite 355 test; không cộng trùng thành số test riêng biệt.

Các lần chạy trước được giữ nguyên, không sửa log FAIL:

- Lượt test lỗi ban đầu sau sửa: 1/1 PASS (`test1.log`).
- Lượt servlet trước khi thêm test import thành công: 29/29 PASS (`test2.log`).
- `mvn clean verify` đầu tiên trên bản sao chưa bổ sung migration: **BUILD FAILURE**, 349 test, 344 PASS, 4 failure, 1 error, 0 skip (`verify-first.log`). Một failure là Auto Assignment; ba failure SLA thiếu nguồn; một error setup Lead Capture thiếu bảng.
- Chạy `LeadCaptureJdbcTest,LeadAutoAssignmentJdbcTest,LeadSlaMonitorJdbcTest` sau sửa assignment và bổ sung migration: **BUILD FAILURE**, 19 test, 18 PASS, 1 failure, 0 error/skip (`affected-first.log`). Failure còn lại là manual create gặp cột scoring legacy `scored_at` chưa có.
- Sau sửa schema legacy, chạy ba nhóm trên cùng `LeadServletTest`: **49/49 PASS**, 0 failure/error/skip (`affected-final.log`).

Số test ở lượt cuối tăng so với lượt đầu vì thêm một regression import và cả 6 test Lead Capture đã thực thi, thay vì dừng ở lỗi setup được Surefire ghi thành một ca lỗi cấp lớp.

Kiểm tra FE bổ sung: `node --test frontend/tests/lead-capture.test.mjs` — **FAIL**, 5 test, 1 PASS, 4 FAIL, 0 SKIP (`frontend-capture.log`). Bốn ca dừng ở `TypeError: input.closest is not a function`: `validateField()` dùng DOM API thật, trong khi adapter tại test chỉ giả lập một phần element và thiếu `closest()`. Chưa sửa FE/test FE vì ngoài phần backend được yêu cầu. Đây không phải kiểm thử trình duyệt thật và chưa chứng minh lỗi ở DOM trình duyệt hoặc API.

`git diff --check`: PASS.

## 5. Các luồng đã được bao phủ

- Tạo Lead thủ công và kiểm tra dữ liệu persisted: `LeadCaptureJdbcTest`, `LeadImportJdbcTest` cùng regression servlet.
- JSON sai, field ngoài contract, thiếu trường bắt buộc/nguồn, email/phone sai: servlet và validation/JDBC tests.
- Authentication, permission, owner scope, cập nhật Lead đã chuyển đổi: servlet, capture, DataScope integration và scoring scope tests.
- Excel preview/confirm: 6 `LeadExcelReaderTest`, 8 `LeadImportJdbcTest`, regression route servlet. Bao gồm preview không ghi dữ liệu, valid/error rows, rollback toàn batch, retry receipt/token, chủ sở hữu, hết hạn/quota và nguồn ngừng hoạt động.
- Cập nhật Lead và scoring khi tạo/cập nhật: `LeadScoringJdbcTest` chạy 9/9 PASS; kiểm tra score thật cùng transaction và rollback khi scoring thất bại.
- Auto Assignment: 10/10 PASS; SLA monitor JDBC: 3/3 PASS; SLA monitor service: 4/4 PASS; SLA response service: 10/10 PASS.
- Duplicate Merge và Conversion: các test servlet/service/JDBC đều thuộc lượt full verify PASS.

Không deploy Tomcat hoặc chạy E2E trình duyệt thật trong phiên này. Không tuyên bố đã đo tải/performance.

## 6. Ảnh hưởng Frontend và giới hạn còn lại

Contract 6 field giữ nguyên. `frontend/js/pages/lead-manual-import.js` gửi đúng `sourceId` lấy từ catalog, dùng multipart preview và `{batchToken}` khi confirm; phù hợp với backend sau sửa.

Đã xác minh lệch contract ở `frontend/js/pages/leads.js`, hàm `handleCreateLeadSubmit()` khoảng dòng 1353–1377: gửi thêm `title`, `region`, `industry`, `source`, `tier` và không gửi `sourceId`. Sau khi bỏ fallback, request này nhận 400 đúng contract thay vì được ghi qua đường không validation.

Cách xử lý đề xuất cho modal: lấy nguồn từ `/api/leads/sources`, gửi 6 field được hỗ trợ với `sourceId` dạng số; đưa thông tin bổ sung được phép vào `notes`, hoặc thống nhất mở rộng contract riêng với FE–BE nếu các field đó cần lưu độc lập. Không cho phép FE tự ghi owner/assignee/score và không khôi phục fallback. Không chỉnh sửa FE trong phiên này.

Backend full suite hiện không còn test thất bại. Vẫn còn 4 test FE bổ sung bị lỗi harness và lệch contract của modal nêu trên. Cấu hình/schema của database gốc không được tự nâng cấp: nếu triển khai ứng dụng ở môi trường khác, cần kiểm tra migration và cột scoring tương ứng trước.

Một giới hạn đã có ở `LeadService.create()`: service commit trước khi đọc response. Nếu bước đọc hoặc truyền response sau commit thất bại, client có thể nhận 500 dù Lead đã được lưu. Regression mới xác minh servlet không INSERT lần nữa trong cùng request. API manual create chưa có cơ chế idempotency key; không cam kết chống trùng khi client gửi một request POST mới sau lỗi mạng.

## 7. Git và bảo toàn công việc có sẵn

Nhánh đầu/cuối: `main`. HEAD đầu/cuối: `f3284a8f7f5414569869a1139858f38fd59a7564`.

Ban đầu có 6 tệp đã sửa và SQL 031 chưa tracked. Đã đối chiếu SHA-256, giữ nguyên hoàn toàn nội dung ban đầu của `PasswordResetService.java`, `LeadSlaMonitorService.java`, `MailService.java`, `ExcelTemplateGenerator.java`, `LeadCaptureJdbcTest.java` và `031_full_qa_test_data.sql`. Với `LeadServlet.java`, giữ phần dọn import cũ và chỉ sửa các nhánh POST/PUT được nêu ở trên.

`git status --short` cuối phiên:

```text
 M backend/src/main/java/com/crm/controller/leads/LeadServlet.java
 M backend/src/main/java/com/crm/service/auth/PasswordResetService.java
 M backend/src/main/java/com/crm/service/leads/LeadAutoAssignmentService.java
 M backend/src/main/java/com/crm/service/leads/LeadSlaMonitorService.java
 M backend/src/main/java/com/crm/service/mail/MailService.java
 M backend/src/main/java/com/crm/util/ExcelTemplateGenerator.java
 M backend/src/test/java/com/crm/controller/leads/LeadCaptureJdbcTest.java
 M backend/src/test/java/com/crm/controller/leads/LeadServletTest.java
?? backend/database/031_full_qa_test_data.sql
?? backend/docs/CRM-261-backend-fix-qa-20261023.md
```

`git diff --stat` (gồm cả các thay đổi đã có từ trước, chưa tính hai file untracked):

```text
 .../java/com/crm/controller/leads/LeadServlet.java |  68 +------
 .../com/crm/service/auth/PasswordResetService.java |   3 +-
 .../service/leads/LeadAutoAssignmentService.java   |   5 -
 .../crm/service/leads/LeadSlaMonitorService.java   |   9 +-
 .../java/com/crm/service/mail/MailService.java     |  16 +-
 .../java/com/crm/util/ExcelTemplateGenerator.java  |   2 -
 .../crm/controller/leads/LeadCaptureJdbcTest.java  |   1 -
 .../com/crm/controller/leads/LeadServletTest.java  | 206 ++++++++++++++++++++-
 8 files changed, 228 insertions(+), 82 deletions(-)
```

Không stage, commit, push, tạo PR, merge, đổi nhánh, reset, clean working tree hoặc sửa lịch sử Git.

## 8. Bước tiếp theo với k6

Deploy WAR này lên Tomcat 10.1 test riêng, gắn database QA đã migrate đầy đủ. Dùng tài khoản test và giữ cookie session trong từng VU; tránh chạy k6 vào `crm_db` hoặc môi trường đang dùng.

Đo baseline với 1 VU, tăng 5 rồi 10 VU: manual POST hợp lệ, JSON/validation 400, quyền 401/403, cập nhật 200/404/409, Excel preview rồi confirm bằng token của đúng phiên. Tách nhóm lỗi nghiệp vụ dự kiến khỏi lỗi 5xx để không tính sai error rate. Mỗi luồng thành công cần xác minh ID, số bản ghi và score; replay cùng token import phải 409 và không sinh thêm Lead.

Thu p50/p95/p99, throughput, error rate, thời gian preview/confirm theo kích thước workbook, CPU/heap/GC của Tomcat và connection/lock/slow query MySQL. Thống nhất ngưỡng SLO với nhóm trước khi dùng chúng làm điều kiện PASS/FAIL. Không tự retry manual POST khi chưa có idempotency hoặc đối chiếu kết quả đã lưu. Chưa chạy k6 trong phiên này.
