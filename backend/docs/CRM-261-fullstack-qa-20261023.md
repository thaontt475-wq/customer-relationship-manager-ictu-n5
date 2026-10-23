# NovaCRM CRM-261 — FE, tích hợp, triển khai và hiệu năng (23/10/2026)

## Phạm vi và bảo toàn
Nhánh ban đầu và hiện tại: main; HEAD f3284a8f7f5414569869a1139858f38fd59a7564. Không stage/commit/push/PR/merge hoặc chuyển nhánh. Các sửa backend và báo cáo ở phiên trước được giữ nguyên. Không ghi crm_db; chỉ dùng crm_sprint1_test_crm261_20261023, đã xác minh catalog bằng kết nối thật, MySQL 8.4.9, nguồn active và cột scored_at.

## Đã sửa
- frontend/leads.html: nạp crm-api.js trước leads.js; đổi phiên bản cache của cả hai JS.
- frontend/js/pages/leads.js: modal lấy nguồn thật từ /api/leads/sources; gửi đúng fullName/companyName/email/phone/sourceId/notes; kiểm tra tên/contact/nguồn/notes; khóa nút khi đang lưu; giữ modal/dữ liệu khi lỗi; không tạo Lead giả trong localStorage. Lỗi tải danh sách từ server được truyền lên thay vì dùng cache giả để che lỗi.
- Chức danh/khu vực/ngành/ngưỡng nhiệt độ do người dùng nhập được giữ trong notes với nhãn; nhiệt độ này chỉ là đề xuất, không ghi đè điểm/tier backend. Không mở rộng contract backend. Notes giới hạn 2000 ký tự bao gồm thông tin bổ sung.
- frontend/js/crm-api.js: giữ data của response lỗi để modal hiển thị lỗi theo field. Toast dùng textContent để không diễn giải HTML trong thông báo server.
- frontend/tests/lead-capture.test.mjs: bổ sung closest('.form-group')/querySelector cho DOM adapter; không sửa assertion cũ. Đây là lỗi harness, không phải thiếu DOM API trên trình duyệt thật.
- frontend/tests/leads-modal.test.mjs: 3 regression test cho payload/notes, HTTP400 giữ modal, nguồn/contact sai không gửi request.
- performance/leads-read.js và README: k6 loopback-only; 1/5/10/25/50 VU; 30 hoặc60 giây; tối đa3000 GET/lượt; mỗiVU nghỉ1giây; timeout5giây; lỗi>1% thì dừng sau khoảng đánh giá10giây. Auth lấy credential từ env, không ghi credential/token vào source hoặc summary.

## Kiểm thử và deploy
- node --test frontend/tests/lead-capture.test.mjs: 5/5 PASS.
- node --test frontend/tests/*.test.mjs: 8/8 PASS, 0 FAIL/SKIP. Log phase2-fe.log.
- mvn clean verify: 355/355 PASS, 0 failure/error/skip. Log phase2-verify.log.
- mvn clean package: 355/355 PASS, BUILD SUCCESS. Log phase2-package.log. Các lần package lại sau sửa thông báo/cache cũng chạy đầy đủ355 test, không skip: phase2-package-final.log.
- Tomcat 10.1.60 được phát hiện tại C:/Tools/apache-tomcat-10.1.60. Java Temurin21, Maven3.9.16. Instance mới nằm dưới .local-qa/CRM-261-20261023/tomcat; Connector chỉ bind127.0.0.1:18085, shutdown port tắt. Không thay deployment khác; không sử dụng deployment tại8080. Bản WAR triển khai ban đầu đã được backup trước thay thế bằng WAR cuối.
- Health thực: GET http://127.0.0.1:18085/crm/api/leads/health trả200 với success:true.
- Startup phát hiện thiếu lead_assignment_rules khi scheduler chạy. Đã áp dụng migration029 có sẵn vào QA; không xóa lỗi cũ khỏi log, không tạo rule mẫu. Các bảng tạm trong unit/JDBC tests giải thích tại sao355 test trước đó không phát hiện cấu trúc runtime này thiếu.
- HTTP smoke từ script có sẵn:14 PASS/0FAIL, gồm public health, session401, CORS, content type,413 và rate limit429. Log phase2-http-smoke.log.
- Trình duyệt thật: đăng nhập tài khoản QA mới, mở trangLead/modal, nhận6 nguồn, điền dữ liệu và tạo Lead#807; tìm lại bằng tên trong danh sách. MySQL xác nhận tên/company/email/phone/source_id435/owner và notes có QA Director. Không suy luận persistence từ mock.
- Tên151ký tự nhận400; modal vẫn mở và hiện “fullName…tối đa150ký tự”. Đã phát hiện trình duyệt cache JS cũ và thêm cache-busting trước khi kiểm tra lại.
- 7 kiểm tra HTTP có xác thực PASS: login, sources, danh sách tìm Lead#807, detail và notes,400 owner spoof,400 contact sai,401 khôngcookie. Thêm3 kiểm tra permission/session PASS: userQA khôngrole bị403 ở list/sources; cookie sai bị401. Evidence phase2-integration.json, phase2-permission.json.
- Tài khoản kiểm thử được tạo mới chỉ trong QA, không reset mật khẩu tài khoản hiện có. Credential/session không nằm trong báo cáo. Không gửi email.

## Bằng chứng cục bộ
Thư mục bằng chứng: C:/Users/thang/Projects/customer-relationship-manager-ictu-n5/.local-qa/CRM-261-20261023/ (gitignored).
- phase2-fe.log; phase2-verify.log; phase2-package.log; phase2-package-final.log.
- phase2-http-smoke.log; phase2-integration.json; phase2-permission.json; browser-db-evidence.txt.
- browser-created-lead.png; browser-validation-error.png; browser-validation-error.txt.
- tomcat/logs/stdout.log, stderr.log và localhost_access_log*.txt (có HTTP201/400 thực).
- k6-health-*.json, k6-authenticated-*.json và log tương ứng; resources.jsonl (CPU cumulative và working set processTomcat, không phải CPU%).
Không commit .local-qa: chứa credential QA, dump, WARbackup, dữ liệu test và log runtime. Không commit backend/target hoặc db.properties.

## PowerPoint A-10
Không tìm được file.pptx trong workspace, C:/Users/thang/Projects, Desktop, Documents hoặc Downloads đã kiểm tra. Không tạo PPTX giả thay bản gốc. Nội dung cập nhật riêng nằm ở performance/A-10-content-20261023.md; cần bản gốc để sửa đúng font/màu/vị trí và xuất bản PPTX mới.

## Review và danh sách đề nghị commit
Nhánh đề xuất: codex/fix-crm261-lead-contract-qa. Commit message: fix(CRM-261): align lead form contract and verify QA deployment. PR target: develop. Chưa thực hiện Git mutation, chờ duyệt phạm vi.

Đưa vào commit sau khi duyệt (chỉ hunk liên quan CRM-261):
- backend/src/main/java/com/crm/controller/leads/LeadServlet.java (chỉ fallbackPOST/PUT; không gộp hunk dọnimport cũ không liên quan).
- backend/src/main/java/com/crm/service/leads/LeadAutoAssignmentService.java.
- backend/src/test/java/com/crm/controller/leads/LeadServletTest.java.
- frontend/leads.html; frontend/js/crm-api.js; frontend/js/pages/leads.js.
- frontend/tests/lead-capture.test.mjs; frontend/tests/leads-modal.test.mjs.
- performance/leads-read.js; performance/README.md; performance/results-20261023.json; performance/A-10-content-20261023.md.
- backend/docs/CRM-261-backend-fix-qa-20261023.md và báo cáo này.

Không đưa vào commit vì có từ trước và ngoài phạm vi: PasswordResetService.java, LeadSlaMonitorService.java, MailService.java, ExcelTemplateGenerator.java, LeadCaptureJdbcTest.java, backend/database/031_full_qa_test_data.sql. Không thể kết luận tác giả chỉ từ workingtree; cần chủ thay đổi review riêng. Không commit secrets, binaries, dump hoặc credential QA.

## Giới hạn và việc cần duyệt
- Chưa có PowerPoint gốc để sửa A-10.
- Dữ liệu legacyQA có chuỗi tiếng Việt đã lỗi encoding từ trước; không tự sửa dữ liệu đó.
- Danh sách legacy dùng trường source/sourceName, trong khi contract manual lưu sourceId; bản ghi mới có thể hiện nhãn WEB_FORM legacy dù sourceId chính xác. Không sửa backend để chấp nhận payload sai; nên thống nhất chuẩn hóa readmodel/source catalog cho toàn bộ màn hình trong task tiếp theo.
- Màn hình còn nút/dữ liệu mẫu của thiết kế cũ ngoài luồng tạoLead đã sửa; không coi đó là bằng chứng dữ liệu server. Luồng tạoLead được kiểm thử dùng server thật.
- Kết quả tải là snapshot trên máy dev dùng chung, có thời gian build/test Maven trùng một số lượt đầu; không phải đo capacity trong môi trường cách ly. Không suy rộng số request thành số người dùng production.
- API manualPOST chưa có idempotency; không tựretry khi lỗi mạng sau commit.
- Cần duyệt danh sách file/hunk, tên nhánh và target develop trước commit/push/PR. Không tự merge.

## Kết quả k6
Các số đo cuối được thêm bên dưới từ JSON thực; tổngHTTP của chế độauthenticated bao gồm một request login trong setup. successful_requests chỉ đếm GET được check. Duration cấu hình không bao gồm thời gian setup; throughput của k6 dùng thời gian đo của chính k6. Các lượt kế tiếp chỉ chạy khi lượt trước exit0, health200 và RAM trống>=512MiB.
- health, 1 VU, 30s cấu hình: 30 HTTP, 30 thành công, 0 lỗi (0%); avg=2.96ms, p50=2.2ms, p95=9.91ms, p99=11.12ms, throughput=0.99req/s; PASS
- health, 5 VU, 60s cấu hình: 300 HTTP, 300 thành công, 0 lỗi (0%); avg=1.94ms, p50=1.43ms, p95=5.38ms, p99=11.51ms, throughput=4.98req/s; PASS
- health, 10 VU, 60s cấu hình: 600 HTTP, 600 thành công, 0 lỗi (0%); avg=1.69ms, p50=1.11ms, p95=4.01ms, p99=13.22ms, throughput=9.97req/s; PASS
- health, 25 VU, 60s cấu hình: 1500 HTTP, 1500 thành công, 0 lỗi (0%); avg=1.15ms, p50=0.73ms, p95=3.7ms, p99=7.2ms, throughput=24.94req/s; PASS
- health, 50 VU, 60s cấu hình: 3000 HTTP, 3000 thành công, 0 lỗi (0%); avg=2.15ms, p50=1.01ms, p95=7.86ms, p99=17.64ms, throughput=49.63req/s; PASS
- authenticated, 1 VU, 30s cấu hình: 30 HTTP, 30 thành công, 0 lỗi (0%); avg=61.69ms, p50=34.37ms, p95=220.47ms, p99=370.27ms, throughput=0.97req/s; PASS
- authenticated, 5 VU, 60s cấu hình: 292 HTTP, 292 thành công, 0 lỗi (0%); avg=33.2ms, p50=27.1ms, p95=57.85ms, p99=139.23ms, throughput=4.78req/s; PASS
- authenticated, 10 VU, 60s cấu hình: 591 HTTP, 591 thành công, 0 lỗi (0%); avg=29.99ms, p50=25.42ms, p95=59.44ms, p99=79.99ms, throughput=9.65req/s; PASS
- authenticated, 25 VU, 60s cấu hình: 1450 HTTP, 1450 thành công, 0 lỗi (0%); avg=39.02ms, p50=32.53ms, p95=81.28ms, p99=169.15ms, throughput=23.65req/s; PASS
- authenticated, 50 VU, 60s cấu hình: 1517 HTTP, 1465 thành công, 52 lỗi (3.4278%); avg=58.01ms, p50=41.19ms, p95=140.45ms, p99=349.92ms, throughput=47.41req/s; FAIL, tự dừng khoảng32s

50VU authenticated FAIL: 52/1517 HTTP lỗi (3,4278%); checkGET1464PASS/52FAIL. Không tăng tải hoặc sửa threshold. Tomcat ghi CommunicationsException với nguyên nhân BindException: Address already in use: connect. Kiểm tra Windows sau lượt tải:13210TCP TIME_WAIT,8059 về MySQL3306, dải cổng động49152–65535 (16384cổng). Kết hợp với DatabaseConfig gọi DriverManager.getConnection cho mỗi lần mở DAO, bằng chứng phù hợp với áp lực cổng/socket từ nhiều kết nối JDBC ngắn; không khẳng định MySQL hết max_connections khi chưa có bằng chứng đó.

Package chạy trùng tải cuối đã FAIL do lỗi socketJDBC; log được giữ tại phase2-package-during-load-failed.log. Các lượt cleanverify/cleanpackage trước đó PASS355. Cần đợi socket hồi phục rồi build lạiWAR cuối; không được xem kết quả package trùng tải là PASS. Khuyến nghị task riêng: cấu hình pool JDBC/TomcatDataSource có giới hạn và lifecycle rõ, đo lại trên máy cách ly sau khi xác minh transaction/rollback. Không tự sửa dải cổng Windows, tắt giới hạn hoặc thêm retry để che lỗi. Health sau tải vẫn200.

Tài nguyên lấy mẫu trước mức cuối:CPUhost71%, RAMtrống3405MiB; snapshotCPU trước đó52–63%. Đây là snapshot, không phải peak/average theo từng mức. resources.jsonl ghiCPUcumulative vàWorkingSetTomcat; không diễn giải CPUcumulative thành phần trăm.

## Bổ sung cuối sau kiểm tra trình duyệt
- frontend/css/pages/leads.css: nâng z-index vùng toast vượt modal99999 để thông báo lỗi không bị che; đổi versionCSS để tránhcache.
- frontend/js/pages/leads.js: sửa mapping PENDING_ASSIGNMENT/PENDING_RESPONSE; không gắn nhãn “Đóng/Mất” cho trạng thái chưa biết. Phát hiện khi chụp bản ghiQA sau scheduler; không sửa dữ liệu để làm đẹp ảnh.
- Bổ sung CSS này, performance/evidence/*.png và performance/A-10-content-20261023.md vào danh sách file chờduyệt. Ảnh01/02 là trích xuất từ log/JSON; ảnh03/04/05 là ảnh chụp trình duyệt thật.
- Package sau khi socket hồi phục đã PASS355/355; giữ logFAIL trước đó (354PASS,1FAIL,0ERROR/SKIP). Lỗi500 phần kiểm tra session trong thời gian chạytrùngtải không được tính làPASS.
- Redeploy có một health404 tạm thời khicontextđangnạp lại. Sau log“Deployment...finished”, health200 và10kiểmtraHTTPnghiệpvụ/quyềnPASS. Không coi404tạmthời làtriểnkhaithànhcông.

## Trạng thái cuối và bộ ảnh cho slide
Frontend cuối:9/9PASS (thêm regression mapping trạng thái), backendpackagecuối:355/355PASS,0FAIL/ERROR/SKIP. Chỉ dùng ảnh01/02(tổnghợp từ nguồnthật),03(health trìnhduyệt),04(bản ghiQA trìnhduyệt) cho slide. Các ảnhmodal trước đó không dùng làm ảnh minh chứng thông báo vì không bắt được toast thoáng qua trong khung ảnh; nội dung lỗi có chứng cứ AX và HTTP400. Không chỉnh sửa ảnh trìnhduyệt để thêm thông báo không chụp được.

Ảnh người dùng gửi xác định slideA-10. Chưa có PPTXgốc nên chưa thay nội dung trong filePowerPoint; nội dung thay thế đúng hàngTriểnkhai/hiệunăng nằm trongperformance/A-10-content-20261023.md. Giữ nguyên172/172,7/7,130/130,Người dùng thử:Chưađo và các báo cáoPR#163 chưa được kiểmtra lại.

## Git status/diffstat cuối

```text
 M backend/src/main/java/com/crm/controller/leads/LeadServlet.java
 M backend/src/main/java/com/crm/service/auth/PasswordResetService.java
 M backend/src/main/java/com/crm/service/leads/LeadAutoAssignmentService.java
 M backend/src/main/java/com/crm/service/leads/LeadSlaMonitorService.java
 M backend/src/main/java/com/crm/service/mail/MailService.java
 M backend/src/main/java/com/crm/util/ExcelTemplateGenerator.java
 M backend/src/test/java/com/crm/controller/leads/LeadCaptureJdbcTest.java
 M backend/src/test/java/com/crm/controller/leads/LeadServletTest.java
 M frontend/css/pages/leads.css
 M frontend/js/crm-api.js
 M frontend/js/pages/leads.js
 M frontend/leads.html
 M frontend/tests/lead-capture.test.mjs
?? backend/database/031_full_qa_test_data.sql
?? backend/docs/CRM-261-backend-fix-qa-20261023.md
?? backend/docs/CRM-261-fullstack-qa-20261023.md
?? frontend/tests/leads-modal.test.mjs
?? performance/

 .../java/com/crm/controller/leads/LeadServlet.java |  68 +------
 .../com/crm/service/auth/PasswordResetService.java |   3 +-
 .../service/leads/LeadAutoAssignmentService.java   |   5 -
 .../crm/service/leads/LeadSlaMonitorService.java   |   9 +-
 .../java/com/crm/service/mail/MailService.java     |  16 +-
 .../java/com/crm/util/ExcelTemplateGenerator.java  |   2 -
 .../crm/controller/leads/LeadCaptureJdbcTest.java  |   1 -
 .../com/crm/controller/leads/LeadServletTest.java  | 206 ++++++++++++++++++++-
 frontend/css/pages/leads.css                       |   3 +
 frontend/js/crm-api.js                             |   1 +
 frontend/js/pages/leads.js                         | 120 +++++-------
 frontend/leads.html                                |   7 +-
 frontend/tests/lead-capture.test.mjs               |   3 +-
 13 files changed, 289 insertions(+), 155 deletions(-)

```
Branch main; HEAD f3284a8f7f5414569869a1139858f38fd59a7564. Không commit/push/PR. Các fileuntracked không nằm trongdiffstat; ảnh/JSON/docs mới cầnreviewriêng. SHA-256cáctệpkhôngliênquan giữnguyên.

