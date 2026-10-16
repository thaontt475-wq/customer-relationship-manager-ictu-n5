# S4-02 — Tạo Lead thủ công và Import Excel

Contract: **PROPOSED** — đề xuất của Backend Nguyễn Văn Thắng, chưa được FE Nguyễn Thị Thu Thảo phê duyệt. Ngày 2026-10-14. Nhánh `feature/S4-02-lead-manual-excel`.

## Khảo sát và mục tiêu

Source trước triển khai không có Lead Servlet/Service/DAO/DTO, endpoint `/api/leads` hoặc trang FE Lead. Metadata MySQL cũng chưa có bảng Lead. Có danh mục `master_data` type `lead-source` trong migration 022 nhưng schema crm_db được audit chưa có row nguồn Lead; migration 024 bổ sung các code chuẩn bằng INSERT IGNORE. Permission `lead.read/create/update/assign` (007), AuthenticationFilter/session và JSON `{success,message,data}` đã có; DataScopeService trước triển khai chưa cho module lead. Apache POI 5.4.1 có sẵn; không thêm dependency/framework.

AC1: Marketing có `lead.create` tạo Lead thủ công, validate nhận diện/liên hệ/nguồn, lưu MySQL. AC2: template và upload `.xlsx` giới hạn dung lượng/dòng, reject macro và formula. AC3: preview từng dòng và lỗi; **không có thao tác ghi DB**, kể cả bảng batch. AC4: confirm riêng, chỉ nhập dòng hợp lệ, transaction và chống confirm lặp. AC5: lỗi có số dòng Excel/field/message cho FE.

## Endpoint đề xuất trước triển khai

Base `/crm/api`; Fetch `credentials:"include"`. Giữ nguyên endpoint/field của các module Sprint 1–3. Các endpoint Lead dưới đây là mới và đang **PROPOSED**:

- `POST /leads`: tạo thủ công; quyền `lead.create`; HTTP 201.
- `GET /leads/{id}`: đọc lại Lead theo `lead.read` + SELF/TEAM/ALL; 200/404 (không lộ bản ghi ngoài scope).
- `GET /leads/sources`: danh mục nguồn active thật; quyền `lead.create`; 200.
- `GET /leads/import/template`: tải file `lead_import_template.xlsx`; quyền `lead.create`; 200 binary.
- `POST /leads/import/preview`: multipart field `file`; quyền `lead.create`; 200 JSON.
- `POST /leads/import/confirm`: JSON `{"batchToken":"<token từ preview>"}`; quyền `lead.create`; 200 JSON. Không gửi lại rows/owner/status từ client khi confirm.

## Request/validation

Manual request:

```json
{"fullName":"Nguyễn Minh An","companyName":"Công ty An","email":"an@example.invalid","phone":"0901234567","sourceId":12,"notes":"Danh thiếp tại hội thảo"}
```

`sourceId:12` chỉ là ví dụ; FE lấy ID thật từ `/leads/sources`. Các field đúng kiểu, unknown field bị từ chối 400 để không âm thầm bỏ qua owner/status. `fullName` bắt buộc, trim, tối đa 150 codepoints; companyName 255, email 255, phone 50, notes 2000. Cần ít nhất email hoặc phone. Email theo quy tắc Customer hiện có: không whitespace, một @ và domain có dấu chấm. Phone theo quy tắc dự án: chữ số và `+() .-`, 6–20 chữ số. `sourceId` số nguyên dương chính xác Long, tồn tại `master_data.type='lead-source' AND active=1`. NULL/blank source hoặc inactive/wrong type không hợp lệ. Lead luôn `status=NEW`, owner/createdBy là user của session, không nhận ID chủ sở hữu từ client.

Trùng email/phone không tự gộp/xóa/cập nhật Lead cũ; dedup Lead thuộc Story khác. Chống import lặp áp dụng cho **cùng batch token**, không dùng email để bỏ dòng tùy tiện.

## Excel mẫu, preview và confirm

Một worksheet `Leads`, header chính xác (có thể đổi thứ tự): `fullName,companyName,email,phone,sourceCode,notes`. Dòng 1 header; dòng 2 trở đi dữ liệu. `sourceCode` là code active lấy `/leads/sources`, ví dụ `EVENT`, `OTHER`; nguồn và fullName bắt buộc. Phone phải định dạng **Text** để giữ số 0 đầu; numeric phone là lỗi dòng. Bỏ dòng trắng, header-only/all-blank là 400.

Giới hạn: 5 MiB file, multipart request 5.5 MB; 1000 dòng dữ liệu theo chỉ số dòng, tối đa 32 cột, 4096 ký tự/ô, 1 triệu ký tự/file; ZIP tối đa 1000 entries, tổng giải nén 64 MiB, mỗi entry 20 MiB. Chỉ `.xlsx`, kiểm tra ZIP/OOXML chứ không chỉ filename/MIME. Không chạy macro/formula/external links: macro/external-link archive bị reject toàn file; formula/error cell bị lỗi dòng, header formula bị reject file. Không sử dụng FormulaEvaluator.

Preview chỉ đọc catalog nguồn, validate từng dòng và giữ immutable batch trong RAM. TTL 15 phút; tối đa 5 preview/user, 64 preview/application; vượt 429. Restart/backend instance khác làm mất preview chưa confirm: FE phải upload lại. Không tuyên bố hỗ trợ shared preview giữa nhiều node.

Preview success (dữ liệu minh họa contract, không phải evidence test):

```json
{"success":true,"message":"Preview Lead thành công","data":{"batchToken":"<opaque token>","expiresInSeconds":900,"totalRows":2,"validCount":1,"errorCount":1,"validRows":[{"row":2,"lead":{"fullName":"Nguyễn Minh An","companyName":null,"email":"an@example.invalid","phone":null,"sourceId":12,"notes":null},"errors":[]}],"errorRows":[{"row":3,"lead":{"fullName":"Bình","companyName":null,"email":"bad","phone":null,"sourceId":null,"notes":null},"errors":[{"field":"sourceCode","message":"Thiếu nguồn Lead"},{"field":"email","message":"email không đúng định dạng"}]}]}}
```

Chính sách **VALID_ONLY_ATOMIC**: bỏ các dòng invalid từ preview (vẫn báo failureCount/lỗi), import toàn bộ dòng valid trong **một transaction**. Catalog nguồn được revalidate/lock tại confirm. Nếu một dòng từng valid không còn nguồn active hoặc gặp lỗi SQL, rollback toàn bộ dòng valid; HTTP 400 cho nguồn thay đổi / 500 cho lỗi DB, successCount=0, lỗi theo dòng; không partial commit.

Confirm success:

```json
{"success":true,"message":"Import Lead hoàn tất","data":{"totalRows":2,"successCount":1,"failureCount":1,"leadIds":[81],"errors":[{"row":3,"errors":[{"field":"sourceCode","message":"Thiếu nguồn Lead"}]}]}}
```

Mỗi preview có token opaque, chỉ session user tạo batch được confirm; user khác 403, token thiếu/expired/restart 400. Synchronize trên batch để hai confirm đồng thời không tạo hai giao dịch. Chỉ lưu **SHA-256 token** trong `lead_import_batches` cùng owner và counts tại confirm; receipt và Leads commit atomically. Unique receipt bền vững chống cùng token được confirm lại sau restart/timeout mạng: 409, không thêm Lead. Trước confirm tra receipt; lỗi SQL rollback cho phép retry token còn TTL. HTTP failure DB/changed source có data counts/error rows rollback; SQL/credentials không đưa ra response.

## Auth, HTTP và lỗi

AuthenticationFilter `/api/*` giữ nguyên: cookie session còn hiệu lực/ACTIVE; không tin role/owner từ client. DataScopeService cho module `lead`, quyền theo role_permissions và scope role_module_scopes; Marketing SELF, Team Lead TEAM, Admin/Director ALL theo migration mới, không nâng quyền các module cũ. Create chỉ owner=session ngay cả ALL.

Manual success data có `id,fullName,companyName,email,phone,sourceId,sourceCode,sourceName,notes,status,ownerUserId,createdBy,createdAt`. Ngày giờ projection ISO. Validation failure HTTP400: `{"success":false,"message":"Dữ liệu Lead không hợp lệ","data":{"errors":[{"field":"sourceId","message":"Nguồn Lead là bắt buộc"}]}}`.

Status: 200 read/template/preview/confirm; 201 manual; 400 validation/file/header/empty/token; 401 unauthenticated; 403 permission/other-owner preview; 404 unknown route hoặc detail ngoài scope; 405 wrong method với `Allow`; 409 confirmed token; 413 upload/JSON limit; 429 preview quota; 500 SQL/internal safe JSON. Manual JSON tối đa 16 KiB ký tự; confirm 8 KiB ký tự. Template binary MIME `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.

## Database và file triển khai dự kiến

Migration `backend/database/024_s4_02_lead_manual_excel.sql`: tạo `leads`, `lead_import_batches`, FK/index và scope lead; bổ sung danh mục nguồn chuẩn bằng INSERT IGNORE; không DROP hoặc sửa/xóa Lead/Customer cũ. Chạy trên schema đã áp dụng migrations 001–023; script dùng schema đang chọn, không có `USE crm_db`. Backend không tự migration trong request.

Package mới: controller/leads/LeadServlet; dto/leads/LeadWriteRequest và LeadValidationException; service/leads/LeadValidation, LeadService, LeadExcelReader, LeadImportService; dao/leads/LeadDAO. Reuse bounded OOXML helper của Customer import, JsonUtil/ResponseUtil, DatabaseConfig, DataScopeService. Chỉnh DataScopeService chỉ thêm `lead` vào module whitelist; CustomerExcelReader chỉ dùng helper shared validation archive để tránh sao chép chức năng.

## FE integration cho Nguyễn Thị Thu Thảo

1. GET sources, hiển thị name, manual gửi sourceId thật; Excel dùng sourceCode.
2. Manual JSON gửi 6 field hỗ trợ, Content-Type application/json, credentials include; chỉ hiện thành công sau 201.
3. Upload `const body=new FormData(); body.append('file',file)`; Fetch POST preview credentials include, **không tự set multipart Content-Type**.
4. Hiển thị totalRows/validCount/errorCount, validRows/errorRows; dùng `row` đúng vị trí Excel và `errors[].field/message`; escape text trước render HTML.
5. Khi xác nhận POST token từ response vào `/leads/import/confirm`; disable nút khi pending; xử lý 409 đã confirm và 400 hết TTL bằng thông báo/upload lại. Token thuộc owner, không dùng localStorage để giả success.
6. 500/400 có rollback report không được hiển thị nhập thành công; yêu cầu retry sau lỗi DB khi token còn hạn. Không chỉnh file Excel rows ở client rồi gửi thẳng confirm.
7. Source list API và đề xuất field/endpoint cần FE–BE xác nhận; Contract giữ PROPOSED đến khi có phê duyệt thật. Không gửi tin nhắn cho FE hoặc tạo PR tự động trong phiên này.

## Test bắt buộc và bàn giao

Tạo hợp lệ/thiếu nguồn/email/phone/owner spoof; Excel valid/mixed/empty/not-XLSX/oversize/row limit/macro/formula; preview không ghi DB; confirm đúng counts, repeat/concurrent confirm, owner khác/permission denied, expired token, source inactive giữa preview-confirm, SQL failure rollback/retry; GET scope SELF/TEAM/ALL; regression Sprint 1–3. Chạy đúng backend: `mvn clean test` và `mvn clean package` (không skipTests). Test ghi chỉ MySQL riêng, không crm_db.

## Kết quả thực thi ngày 2026-10-14

**Backend hoàn thành AC1–AC5 và kiểm tra bắt buộc; Contract vẫn PROPOSED, chưa có FE APPROVED.**

- `mvn -B -o clean test`: **150 PASS, 0 failure/error/skipped**, BUILD SUCCESS. `-o` chỉ dùng dependencies sẵn có, không skip test.
- `mvn -B -o clean package`: **150 PASS, 0 failure/error/skipped**, BUILD SUCCESS; WAR `backend/target/crm.war` được deploy Tomcat 10.1.60 test riêng.
- 23 test S4-02 mới: LeadValidationTest 5; LeadExcelReaderTest 6; LeadImportJdbcTest 8; LeadServletTest 4. 127 test hiện có cho Sprint 1–3 vẫn PASS, gồm auth/data scope, customer/profile/search/merge/hierarchy, Contact, Customer Excel import, support/risk, contract/team.
- HTTP thực **20/20 PASS**: login session Marketing/Admin/Accountant/Team Lead/Outside, manual create/DB/ISO createdAt, validation, scope SELF/TEAM/ALL, source/template binary, valid/mixed preview/confirm, preview không ghi DB, replay409/concurrent200+409, empty/corrupt XLSX400, upload thực 5MiB+1 trả413 JSON, formula invalid row, quyền401/403, nguồn bị disable400 rollback, trigger SQL500 rollback + retry200, method405/unknown404.
- Runtime API test: `http://localhost:18084/crm/api`, database `crm_s4_02_test`. JDBC regression Maven: `crm_sprint1_test`, fixture Lead dùng TEMPORARY InnoDB trong session; không ghi test trên crm_db. Migration 024 được áp dụng/replay trên DB S4 test, chưa áp dụng production.
- Lỗi runtime phát hiện và sửa: MySQL Connector/J trả DATETIME dạng LocalDateTime, Gson Java21 không serialize được; DAO chuẩn hóa TemporalAccessor/Timestamp sang ISO string, thêm assertion JSON serialization thực trong JDBC regression. Lượt HTTP FAIL trước sửa được giữ nguyên log/JSON; không sửa thành PASS.
- Helper QA ban đầu tạo Statement trước đổi catalog gây “No database selected”; đã sửa helper tạo Statement sau chọn DB. Một lỗi checked exception khi compile test mới đã sửa declaration, không giảm assertion. Log các lần thử giữ trong QA.

Evidence ở `C:/Users/thang/Projects/novacrm-s4-02-qa-20261014/`: `maven-clean-test.log`, `maven-clean-package.log`, `http-acceptance.json` (method/endpoint/status/response, token che), `http-acceptance.log`, snapshots `db-*.json`, `schema-audit.log`, Tomcat logs, workbook mẫu/fixture và script tái kiểm thử. Báo cáo chi tiết `test-results.md`; bản nháp PR `pull-request-draft.md`. Không cookie/password/token plaintext trong báo cáo.

## File Backend thực tế (17 file)

Tạo mới:

- `backend/database/024_s4_02_lead_manual_excel.sql`
- `backend/docs/S4-02-lead-manual-excel.md`
- `backend/src/main/java/com/crm/controller/leads/LeadServlet.java`
- `backend/src/main/java/com/crm/dao/leads/LeadDAO.java`
- `backend/src/main/java/com/crm/dto/leads/LeadWriteRequest.java`
- `backend/src/main/java/com/crm/dto/leads/LeadValidationException.java`
- `backend/src/main/java/com/crm/service/importer/BoundedXlsx.java`
- `backend/src/main/java/com/crm/service/leads/LeadValidation.java`
- `backend/src/main/java/com/crm/service/leads/LeadService.java`
- `backend/src/main/java/com/crm/service/leads/LeadExcelReader.java`
- `backend/src/main/java/com/crm/service/leads/LeadImportService.java`
- `backend/src/test/java/com/crm/controller/leads/LeadServletTest.java`
- `backend/src/test/java/com/crm/service/leads/LeadValidationTest.java`
- `backend/src/test/java/com/crm/service/leads/LeadExcelReaderTest.java`
- `backend/src/test/java/com/crm/service/leads/LeadImportJdbcTest.java`

Sửa nhỏ module shared:

- `backend/src/main/java/com/crm/service/permissions/DataScopeService.java`: thêm whitelist `lead`; giữ nguyên cách resolve quyền/scope và các module cũ.
- `backend/src/main/java/com/crm/service/importer/CustomerExcelReader.java`: tái sử dụng BoundedXlsx (guards cũ được giữ, thêm reject external links); giữ header/DTO/validation/transaction/API Customer import hiện có.

FE, Maven dependency, endpoint/table/JSON field Sprint 1–3 không đổi. Working tree ban đầu sạch, HEAD `550a993`; stash `backup-before-pull-20261011` giữ nguyên. Chỉ làm việc trên nhánh feature đã yêu cầu, không stage/commit/push/tạo PR.

## Vận hành và việc còn lại

Nguồn canonical: WEBSITE/REFERRAL/COLD_CALL/SOCIAL_MEDIA/EVENT/OTHER. Danh thiếp có thể chọn OTHER và ghi notes, không tự tạo Lead mẫu. Tham số nguồn phải lấy catalog thật. Không có unique email/phone trong S4-02; không coi import retry bằng token khác là duplicate cùng batch. Không tự merge Lead.

Preview sống trong RAM một node 15 phút; restart hoặc đổi node trước confirm cần upload lại. Receipt confirm sống trong MySQL, token hash unique; replay không tạo row thêm. Cần retention policy sau này cho receipt nếu có lưu lượng lớn; đợt triển khai này không xóa receipt/dữ liệu cũ. Những giới hạn này là thiết kế được công khai trong PROPOSED Contract, không ngụy tạo hỗ trợ multi-node.

Chưa tích hợp giao diện Lead của FE/chưa có FE phê duyệt Contract; chưa production deployment, chưa load-test multi-node. Regression Sprint 1–3 là toàn bộ automated Backend suite, không tuyên bố chạy lại tất cả luồng UI Sprint 1–3 trong phiên S4-02. Email/password DB lấy env; QA không gửi email bên ngoài.

Chạy migration bằng MySQL client trên **schema đã chọn và đã backup theo quy trình nhóm** sau khi review; DDL MySQL auto-commit và CREATE IF NOT EXISTS không sửa bảng cùng tên/schema khác:

```sql
SOURCE C:/Users/thang/Projects/customer-relationship-manager-ictu-n5/backend/database/024_s4_02_lead_manual_excel.sql;
```

Kiểm tra tables/FK/index và lead scopes trước deploy WAR. SQL không có USE crm_db/DROP, Backend không tự chạy migration trong request.

## Commit/push và Pull Request đề xuất (chưa thực hiện)

Commit message: `feat(S4-02): add manual leads and safe Excel preview-confirm import`.

Sau khi bạn review và tự quyết định commit, tại repository:

```powershell
git branch --show-current
git status --short
git add -- backend/database/024_s4_02_lead_manual_excel.sql backend/docs/S4-02-lead-manual-excel.md backend/src/main/java/com/crm/controller/leads backend/src/main/java/com/crm/dao/leads backend/src/main/java/com/crm/dto/leads backend/src/main/java/com/crm/service/leads backend/src/main/java/com/crm/service/importer/BoundedXlsx.java backend/src/main/java/com/crm/service/importer/CustomerExcelReader.java backend/src/main/java/com/crm/service/permissions/DataScopeService.java backend/src/test/java/com/crm/controller/leads backend/src/test/java/com/crm/service/leads
git diff --cached --check
git diff --cached --stat
git commit -m "feat(S4-02): add manual leads and safe Excel preview-confirm import"
git push -u origin feature/S4-02-lead-manual-excel
```

Xác nhận Git identity của Nguyễn Văn Thắng trước commit. PR đề xuất **base develop**, head feature/S4-02-lead-manual-excel; draft title/body và evidence ở `novacrm-s4-02-qa-20261014/pull-request-draft.md`. Không tạo PR tự động. Giữ Contract PROPOSED đến khi FE–BE thật sự thống nhất field/endpoint/chính sách import.
