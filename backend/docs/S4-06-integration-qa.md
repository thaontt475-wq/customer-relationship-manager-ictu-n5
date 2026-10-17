# NovaCRM — tiếp tục QA Sprint 4 ngày 17/10/2026

## Git và phạm vi

- PR S4-01: https://github.com/thaontt475-wq/customer-relationship-manager-ictu-n5/pull/154, head `fix/s401-full-browser-qa-20261017`, base `develop`.
- Commit S4-01: `57c3f82b740fa0d035d683381a2658751f3bff1f`. Đã push; không merge, không push vào develop.
- Sửa S4-06/S4-07 nằm riêng tại nhánh `fix/s406-integration-qa-20261017`, worktree `integration-source`. Không nằm trong PR 154.
- Checkout gốc vẫn có đúng sáu file chưa commit ban đầu. SHA256 binary diff trước/sau bằng nhau: `CA320A2ED1599AC53BE742ABA8CB112BFEA7333DF68DB00567430DF536378808`.
- Không chỉnh sửa `crm_db`; toàn bộ mutation ứng dụng dùng Tomcat 18086, schema `crm_sprint1_test_s401_full_20261017`, MySQL 8.4.9. Chín scoring JDBC tests dùng bảng tạm kết nối trong schema riêng `crm_sprint1_test` do test yêu cầu chính xác tên đó.

## Hai BLOCKED của S4-01

- **TC-S4-01-03 — BLOCKED:** tìm thấy cửa sổ Chrome thật. Khi đọc trạng thái cửa sổ, Computer Use dừng vì không xác định chắc chắn URL để thực thi chính sách. Không tiếp tục điều khiển UI hoặc dùng cách khác vượt qua cơ chế dừng. Không có bằng chứng clipboard hệ điều hành paste khớp mã gốc.
- **TC-S4-01-12 — BLOCKED:** đã kiểm tra đăng nhập/đăng xuất/đổi Marketing bằng cookie jar HTTP độc lập, gửi form và kiểm tra DB. Đây không phải browser/profile độc lập. Hồi quy liên Story còn lỗi S4-09 và S4-08; không đánh PASS.
- Kết quả 12 case S4-01 trước đó giữ nguyên **10 PASS / 0 FAIL / 2 BLOCKED**. Không cộng lại các lần chạy thành số case mới.

## S4-06: nguyên nhân, backup và xử lý

1. QA thiếu `lead_assignment_rules`. Migration cũ `025_sprint4_lead_assignment_rules.sql` có `USE crm_db`, tạo/bổ sung bảng/cột, permission và demo seeds cố định users 101–106, rules/history/leads. Không chạy file này trực tiếp; không có DROP bảng nhưng demo seeds và database đích không phù hợp QA.
2. Full dump trước thay đổi: `continuation-evidence/qa-before-s406.sql`, SHA256 `B2F6DBCFDF891A34B06C4343EF7CA27C563E4E20DE5FF7BA093BBECB9BDF695D`. Backup chỉ lưu cục bộ, không đưa vào Git.
3. Thêm migration `029_s4_06_assignment_sla_integration.sql`: không USE, không DROP, không demo seeds, không UPDATE dữ liệu khách hàng. Chỉ tạo rules/history nếu thiếu và thêm quyền. Cần schema Lead/S4-07 hiện hữu; phải chọn database đích trước khi chạy. `last_assigned_user_id` có FK users. Với history đã tồn tại, CREATE IF NOT EXISTS không tự thêm FK mới; đây không phải migration siết toàn bộ legacy constraints.
4. Leads 1–4 tham chiếu `USR-2`/2 và Lead 6 tham chiếu `USR-9999`/9999, nhưng users tương ứng không tồn tại. `assigned_to` legacy là VARCHAR, trong khi S4-07 dùng `assigned_to_user_id` BIGINT. LeadDAO còn đọc legacy bằng getLong và trả sáu Sales hardcode; service tự chọn nhân viên đầu tiên khi ID rule không tồn tại.
5. Chỉ trong QA, sau backup, năm orphan references được xóa assignee và đưa về PENDING_ASSIGNMENT, giữ nguyên nội dung Lead. Không tạo users 2/9999 giả để hợp thức hóa dữ liệu. Quy tắc chuyển `USR-n` sang numeric chỉ hợp lệ nếu users.id=n thực sự tồn tại.
6. Catalog mới chỉ trả users ACTIVE có role SALES_REP hoặc TEAM_LEAD. Hỗ trợ alias numeric/USR-n đúng định dạng, tính workload từ canonical/legacy hợp lệ, loại converted/merged/rejected. Không còn fallback sang Sales không thuộc rule.
7. Phân bổ ghi đồng thời hai cột assignee, `PENDING_RESPONSE`, assigned_at và SLA deadline 120 phút mặc định; cập nhật Lead/rule pointer/history trong transaction. Điều kiện UPDATE và queue loại Lead đã nhận, khóa, chuyển đổi hoặc đã có assignee, tránh ghi đè bởi scheduler.
8. GET rules/reps/queue/history yêu cầu quyền phù hợp. Bằng chứng trước sửa: denied account nhận HTTP 200; sau sửa 403. Anonymous vẫn 401; POST không quyền 403; rate limit public vẫn 429.
9. S4-07 có lỗi truy vấn ancestor: duyệt tất cả teams rồi JOIN lại team hiện tại làm mất leader nhóm cha. Đã tái hiện 0 alert cho Sales nhóm con, sửa CTE bắt đầu đúng team của Sales rồi đi lên cha. Sau sửa có đúng một notification `LEAD_SLA_OVERDUE:752:4269`; kiểm tra lặp không trùng. Log backend xác nhận phát cảnh báo.

QA còn thiếu tiền đề S4-04/S4-08: đã đọc đầy đủ và áp dụng `025_s4_04_lead_duplicate_merge.sql`, `026_s4_08_lead_conversion.sql` vào QA được chọn, sau backup. Hai file không USE; DROP chỉ dành cho helper procedures; ALTER additive và grants/scope được kiểm tra. Không chạy `026_sprint4_lead_filters_sla.sql` vì có UPDATE và UPSERT demo Leads 201–208; QA đã có cấu trúc lọc cần thiết.

## Kiểm thử S4-06/S4-07 thực tế

- **PASS — khu vực/ngành/ưu tiên:** rules NORTH + ngành có marker riêng ưu tiên 1 chọn rep 4272; rule ưu tiên 2 chọn 4273 không thắng rule đầu. CENTRAL/ngành không khớp giữ hàng chờ.
- **PASS — round robin:** hai Lead SOUTH lần lượt phân cho hai users thật 4272/4273, có rule dùng alias `USR-4273`.
- **PASS — load balanced:** rule chọn nhân viên có workload nhỏ nhất trong danh sách cấu hình, đối chiếu API trước và SQL sau.
- **PASS — hàng chờ/ID lỗi:** rule trỏ 99999999 không phân cho người đầu tiên; manual `bogus-4272` bị 400. Queue không chứa Lead đã phân bổ/chuyển đổi.
- **PASS — thời gian:** scheduler thật, không gọi process-queue để tạo kết quả đo ban đầu, phân Leads 748–751 sau 9 giây; lần sau 782–785 sau 1–2 giây. Đây là các mẫu chức năng nhỏ, chưa phải kiểm thử tải hoặc cam kết mọi trường hợp dưới 5 phút.
- **PASS — tiếp nhận:** đúng Sales nhận Lead -> CONTACTING, accepted_at, ON_TIME; Sales khác bị 403.
- **PASS — từ chối/phân bổ lại:** xóa hai assignee, về PENDING_ASSIGNMENT, lưu lý do/history; phân bổ lại đặt deadline mới và xóa rejected_at/sla_result cũ.
- **PASS — trễ hạn/cảnh báo:** đặt deadline quá hạn chỉ trên Lead fixture mới để kiểm tra cơ chế; lưu OVERDUE, nhận trễ vẫn OVERDUE; cảnh báo tới trưởng nhóm cha và dedup. Không tuyên bố đã chờ hết SLA 120 phút thật.
- **PASS — bảo vệ lifecycle:** JDBC production DAO không ghi đè assignment đã có hoặc converted; chạy process-queue thực tế không thay đổi Lead CONVERTED.
- **BLOCKED — FE/profile:** không có kiểm thử UI mới sau khi Computer Use dừng; các kết quả trên là API/JDBC/SQL.

## Luồng liên Story: kết quả API/SQL theo từng chức năng

Luồng cuối dùng Lead **782**, duplicate **783**, RR **784/785**, unmatched **786**; marker `QA4FOLLOW1792210174284`. Dữ liệu được tạo bằng API form thật với UUID requestId mới, không chèn bản ghi thành công thay API.

1. **S4-01 — PASS:** form do Marketing 4270 đăng ký; public POST 201; SQL WEB_FORM, nguồn WEBSITE, owner/form/submission thật; Marketing khác 403. 429 và logout/session invalidation vẫn hoạt động.
2. **S4-04 — PASS:** duplicate API tìm đúng hai Lead gửi form trùng email/phone. Kiểm tra phát hiện trùng; không tuyên bố đã merge trong luồng này. Các merge JDBC tests được chạy riêng.
3. **S4-05 — PASS cơ chế với fixture QA:** ban đầu config UNCONFIGURED, API trả score null, không có score row — đúng trạng thái thiếu cấu hình, không gọi PASS. Sau khi đặt rõ fixture 40 điểm employeeCount>=100 +20 title CEO, thresholds HOT50/WARM20, API tính 60/HOT và SQL lead_scores lưu đúng. Fixture không xác nhận bộ quy tắc/ngưỡng nghiệp vụ S4-05; phần nghiệm thu business rules **BLOCKED**.
4. **S4-06 — PASS API/SQL:** phân bổ đúng rule/real users, assignee canonical, PENDING_RESPONSE, deadline và history; scheduler đáp ứng mẫu thời gian nêu trên.
5. **S4-07 — PASS API/SQL sau sửa:** Sales nhận/từ chối, trả hàng chờ, phân lại, ON_TIME/OVERDUE, cảnh báo trưởng nhóm cha được xác nhận API + SQL + backend log.
6. **S4-09 — FAIL:** GET lọc CONTACTING của Sales trả cùng Lead 782 **hai lần**. SQL JOIN users bằng OR của assigned_to_user_id/owner_user_id nối cả Marketing lẫn Sales thành hai hàng. Cầu nối scoring cũng FAIL: lead_scores lưu **60/HOT** nhưng leads.score/tier vẫn **0/WARM**, bộ lọc HOT không tìm thấy Lead 782. Saved filter riêng vẫn lưu/đọc được. Không sửa truy vấn/chính sách scoring-filter S4-09 trong nhánh này.
7. **S4-08 — FAIL khi bàn giao Sales:** Sales 4272 được phân và đã nhận Lead 782 nhưng GET `/782/check-convert-duplicates` trả 403 vì service xét owner_user_id=Marketing 4270. Admin có scope ALL thực hiện conversion 200, tạo Customer 97/Contact 2/history FK hợp lệ và khóa Lead; lần chuyển đổi tiếp 409. Nhánh Admin PASS không biến luồng Sales thành PASS. Không sửa chính sách ownership của S4-08 trong nhánh này.

Tổng theo bảy chức năng API/SQL trên: **5 PASS / 2 FAIL / 0 BLOCKED**, riêng PASS S4-05 chỉ cho cơ chế fixture. FE của cả bảy bước **BLOCKED** trong phiên này; business rules S4-05 còn BLOCKED. Không gộp các lớp hoặc đếm một request thành một Story.

## Maven, frontend, MySQL và log

- S4-01 trước commit: **67 Maven PASS**, WAR BUILD SUCCESS; **5 frontend PASS**. Bằng chứng 44 HTTP +14 smoke từ phiên trước được giữ nguyên, không tuyên bố chạy lại những lượt đó.
- S4-06/S4-07 focused cuối: **27 PASS**, không fail/error/skip; WAR build và deploy QA thành công. Bao gồm 10 assignment, 10 SLA service, 4 SLA monitor, 3 hierarchy JDBC.
- Full regression cuối trên schema QA: **333 tests, 332 PASS / 1 FAIL / 0 ERROR / 0 SKIP**. Thêm **9 scoring JDBC PASS** trên connection-local tables của crm_sprint1_test. Tổng distinct Maven: **341 PASS / 1 FAIL** trong 342 tests, không cộng lượt focused vào tổng.
- FAIL còn lại: `DataScopeIntegrationTest.teamLeadAndAdminRulesAreEnforcedBeforeReplacingRoles:129` giả định fixture Admin là Admin cuối cùng nhưng QA có Admin khác. Đã tái hiện cùng FAIL ở commit S4-01 chưa sửa backend; không phải chứng cứ regression S4-06. Không đổi quyền hay xóa Admin để làm test xanh.
- Khi scoring được bật, lần full đầu gặp lỗi cleanup FK lead_scores trong test Capture. Sửa cleanup xóa scores/profiles **chỉ thuộc owner fixture do test tạo** trước khi xóa Lead. Full cuối không còn lỗi cleanup. File test nằm trên nhánh tích hợp, không thêm vào PR S4-01.
- Frontend cuối: **5 PASS / 0 FAIL**; đây là behavioral tests bằng DOM adapter, không thay thế browser độc lập.
- MySQL cuối: không còn canonical assignee hoặc rule.last_assigned_user_id trỏ user thiếu; kiểm tra FK rules/conversion/merge/notification/users/teams. Bảng leads legacy vẫn chưa có FK assignee; không tuyên bố đã siết FK toàn hệ thống.
- HTTP suite cuối raw: 52 PASS /3 FAIL. Một FAIL catalog là assertion fixture sai: Admin QA đã được gán thêm TEAM_LEAD để kiểm tra SLA nên hợp lệ trong sales catalog. Recheck đúng role PASS. Sau thay thế assertion này: **53 PASS /2 FAIL** cho 55 checks, hai FAIL thật là S4-09/S4-08. Không sửa/xóa file raw; có catalog-recheck riêng.
- Thêm một check độc lập scoring HOT -> filter HOT **FAIL**, tổng 56 checks chức năng hợp lệ của lượt cuối và check bổ sung là **53 PASS /3 FAIL**. Các test bổ sung load balancing/SLA trước đó không được cộng trùng vào tổng này.
- Raw lượt đầu còn có `/conversion-profile` 404 do harness dùng endpoint không tồn tại; lượt cuối dùng đúng `/check-convert-duplicates` và tái hiện 403. Không tính 404 của harness là lỗi sản phẩm.
- Lưu SQL, HTTP request/response, Tomcat access logs, stderr/stdout. Log có cảnh báo MySQL cleanup thread khi hot-redeploy; không coi đó là failure request nghiệp vụ, cũng không khẳng định log sạch hoàn toàn.

## Bằng chứng cục bộ và trạng thái nghiệm thu

Tất cả bằng chứng tại `C:/Users/thang/Projects/novacrm-s401-full-qa-20261017/continuation-evidence/`:

- `s406-baseline-http.json`, `qa-schema-before-s406.txt`, `s406-migration-repair.txt`, `qa-before-s406.sql`.
- `integration-http.json`, `integration-followup.json`, `integration-final-http.json`, `catalog-recheck.json`, `sla-after-fix.json`, `mysql-final-verification.txt`.
- `scoring-filter-handoff.json`, `scoring-filter-sql.txt` xác nhận lệch dữ liệu S4-05/S4-09.
- `s401-maven.log`, `s401-frontend.log`, `s406-s407-final-focused.log`, `integration-final-after-cleanup-maven.log`, `integration-scoring-jdbc.log`, `integration-frontend.log`, `s401-baseline-admin-fixture.log`.
- Tomcat logs tại `../evidence/tomcat-stderr.log`, `../evidence/tomcat-stdout.log`, `../tomcat/logs/localhost_access_log.*`.

**S4-01 chưa đủ điều kiện nghiệm thu FULL:** PR sẵn để review nhưng Clipboard/profile và hồi quy liên Story chưa đạt.

**S4-06 chưa đủ điều kiện nghiệm thu toàn bộ:** backend assignment/SLA đã kiểm chứng và sửa; FE chưa chạy lại, luồng Sales sang S4-08/S4-09 còn FAIL, full Maven vẫn có một FAIL fixture đã được ghi nhận. Không tự động merge hoặc triển khai vào crm_db.

## Các file thay đổi

Commit S4-01: `frontend/js/shared-shell.js`, `frontend/css/pages/lead-capture.css`, `frontend/js/pages/lead-capture.js`, `frontend/lead-capture-form.html`, `frontend/lead-capture-manager.html`; thêm `backend/docs/S4-01-browser-qa.md` và ba ảnh QA dưới `backend/docs/qa/s4-01/`.

Nhánh tích hợp, so với S4-01:

- `backend/database/029_s4_06_assignment_sla_integration.sql`.
- `backend/src/main/java/com/crm/controller/leads/LeadServlet.java`.
- `backend/src/main/java/com/crm/dao/leads/LeadDAO.java`.
- `backend/src/main/java/com/crm/service/leads/LeadAutoAssignmentService.java`.
- `backend/src/main/java/com/crm/service/leads/LeadSlaMonitorService.java`.
- `backend/src/test/java/com/crm/service/leads/LeadAutoAssignmentJdbcTest.java`.
- `backend/src/test/java/com/crm/service/leads/LeadSlaMonitorJdbcTest.java`.
- `backend/src/test/java/com/crm/controller/leads/LeadCaptureJdbcTest.java`.
- Báo cáo này và các JSON bằng chứng đã chọn dưới `backend/docs/qa/s4-06/`.

Hai migration tiền đề S4-04/S4-08 chỉ được áp dụng vào QA, không chỉnh sửa source. Không sửa production S4-08/S4-09 hoặc các file checkout gốc.
