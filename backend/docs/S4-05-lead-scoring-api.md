# S4-05 — Backend chấm điểm Lead và tích hợp Frontend

Ngày bàn giao kỹ thuật: 2026-10-14 (UTC+7). Người phụ trách BE/Integration: Nguyễn Văn Thắng. Nhánh thực hiện: `feature/S4-05-lead-scoring`.

**Trạng thái: đã triển khai và kiểm chứng kỹ thuật trên MySQL/Tomcat test; chưa đánh dấu DONE nghiệp vụ. Contract bên dưới là PROPOSED, chờ xác nhận AC và quy tắc chính thức.**

## 1. Khảo sát, quyền tích hợp và quyết định còn chờ

- HEAD ban đầu: `39b3a47b7d6156927cabe372d47f389d9c24b861`. Commit này có HTML/CSS/JS chấm điểm và menu; chưa có BE S4-05. JS ban đầu dùng 12 Lead mẫu, 19 quy tắc mẫu và localStorage; bootstrap không tải dữ liệu API thật.
- Đã giữ nguyên hai thay đổi có sẵn ở `backend/pom.xml` (Surefire 3.5.4, ép JDBC test vào `crm_sprint1_test`) và `MasterDataServlet.java`. Không sửa hai file này trong công việc S4-05.
- Backend S4-02 chưa nằm trên HEAD hoặc `develop` được lưu cục bộ. Đã tìm bản triển khai tại `feature/S4-02-lead-manual-excel`, commit `8ee328d`. **Người dùng đã cho phép tích hợp chọn lọc**, nên đưa 15 file mới của BE/DAO/DTO/import/test/migration/docs vào nhánh hiện tại. Không merge/cherry-pick, không đổi nhánh. Thay đổi dùng chung duy nhất cần thiết là cho phép module `lead` trong `DataScopeService`; không mang thay đổi `CustomerExcelReader` từ nhánh khác sang.
- Đã thử đọc Linear `CRM-119` và tìm `S4-05`/`Chấm điểm Lead`; connector hiện tại không trả issue hoặc AC. Repository hiện tại không có AC S4-05 chính thức. Không khẳng định các quy tắc FE là quy tắc đã được duyệt.
- `S4-05-proposed-configuration.json` lưu nguyên 19 quy tắc của FE để duyệt. File này **không được ứng dụng tự tải hoặc tự seed**.

Cần xác nhận trước khi nghiệm thu nghiệp vụ:

1. AC chính thức và vai trò được quản lý quy tắc. Hiện đề xuất chỉ ADMIN được sửa cấu hình toàn hệ thống; ADMIN/MARKETING có quyền chấm lại mặc định.
2. Có dùng 19 quy tắc FE và các điểm cộng/trừ hiện tại không? Các trường chưa có trong S4-02 sẽ lấy từ hồ sơ chấm điểm, không được suy diễn từ ghi chú.
3. Ngưỡng HOT/WARM có phải 70/40 không?
4. `SUM_ALL` cộng tất cả quy tắc khớp, kể cả chồng trong một nhóm, hay `MAX_PER_GROUP` lấy quy tắc có điểm cao nhất mỗi nhóm? Với FE hiện tại, nhân sự >=100 khớp cả quy tắc >=100 và >=20. FE cũng dùng tên `GREATER_THAN`/`LESS_THAN` để chỉ >=/<=, cần xác nhận ranh giới này.
5. Có chặn điểm tối thiểu 0 và không giới hạn trần như FE hiện tại, hay chặn 0–100? Điểm sau giới hạn và điểm thô được trả riêng để giải thích.
6. Các giá trị so sánh nguồn phải khớp danh mục MySQL thực tế. `source` là tên nguồn trong `master_data`, không phải code; ví dụ tên COLD_CALL được seed là “Telesales / Tiếp cận trực tiếp”, có thể không khớp từ khóa “Cold Call” của đề xuất FE.
7. Bộ mặc định được phê duyệt cho chức năng “Khôi phục mặc định”. Hiện nút bị vô hiệu hóa và giải thích chưa có bộ mặc định; không reset âm thầm sang quy tắc mẫu.

Migration mới khởi tạo `configured=false`, ngưỡng/policy null, rules rỗng. Trong trạng thái này Lead đọc được với `score=null`, `classification="UNCONFIGURED"`; chấm lại hoặc lưu riêng rules trả 409. Tạo/import Lead vẫn lưu dữ liệu thật, chờ cấu hình trước khi tính điểm. ADMIN phải khai báo rõ policy/ngưỡng/giới hạn qua API cấu hình, sau khi nghiệp vụ được xác nhận.

## 2. API Contract đề xuất

Base thông thường: `/crm/api`. Tất cả endpoint dùng session hiện có, Fetch `credentials: "include"`. Chuẩn JSON `{success,message,data}`; JSON response có `Cache-Control: no-store`. S4-05 dùng namespace riêng `/api/lead-scoring/*`, tránh chiếm `/api/leads/*` của S4-02. Không thay endpoint các module Sprint 1–3.

- `GET /lead-scoring/config`: đọc cấu hình, quyền `lead.read` và scope hợp lệ.
- `PUT /lead-scoring/config`: thay toàn bộ cấu hình, quyền `lead.scoring.manage`; tính lại toàn bộ Lead trong cùng transaction.
- `POST /lead-scoring/rules`: thay danh sách tiêu chí, giữ nguyên policy/ngưỡng/giới hạn; quyền `lead.scoring.manage`. Hỗ trợ thêm/sửa/bật/tắt/xóa bằng thay danh sách với `version` hiện tại.
- `GET /lead-scoring/leads?page=1&size=100`: Lead kèm điểm/chi tiết trong scope `lead.read`; page >=1, size 1..100. Trả `items,page,size,total,totalPages,configured,version`. FE tải từng trang, không giới hạn ngầm 100 Lead.
- `GET /lead-scoring/leads/{id}`: đọc điểm, phân loại và breakdown của một Lead trong scope `lead.read`; không tồn tại/ngoài scope đều 404.
- `PUT /lead-scoring/leads/{id}/profile`: cập nhật các thuộc tính chấm điểm trong scope `lead.update`, lưu profile và điểm cùng transaction.
- `POST /lead-scoring/re-score`: body `{}`, quyền/scope `lead.score`; tính lại Lead trong phạm vi người gọi, trả `count,version`.
- `PUT /leads/{id}`: mở rộng S4-02 để thay toàn bộ các trường cơ bản `fullName,companyName,email,phone,sourceId,notes`; quyền/scope `lead.update`. Trường bắt buộc được kiểm tra như tạo Lead. Lưu dữ liệu cơ bản và điểm trong cùng transaction, giữ owner/status do hệ thống quản lý. Đọc điểm bằng endpoint scoring riêng.

Giữ nguyên các endpoint tạo Lead, lấy nguồn, lấy hồ sơ, Excel template/preview/confirm của S4-02. Tạo Lead và confirm import được nối hook chấm điểm trong transaction hiện có.

### Đọc cấu hình chưa được kích hoạt

```json
{
  "success": true,
  "message": "Xử lý chấm điểm Lead thành công",
  "data": {
    "configured": false,
    "version": 0,
    "thresholds": {"hot": null, "warm": null},
    "aggregation": null,
    "minScore": null,
    "maxScore": null,
    "rules": []
  }
}
```

### Ghi cấu hình đầy đủ

Ví dụ cú pháp dưới đây là **minh họa**, không phải quy tắc được duyệt. Dùng `version` từ GET mới nhất; null ở min/max nghĩa là không giới hạn phía đó.

```json
{
  "version": 0,
  "thresholds": {"hot": 70, "warm": 40},
  "aggregation": "SUM_ALL",
  "minScore": 0,
  "maxScore": null,
  "rules": [
    {
      "id": "SOURCE-REFERRAL",
      "group": "SOURCE",
      "name": "Nguồn giới thiệu",
      "field": "source",
      "operator": "CONTAINS",
      "value": "Giới thiệu",
      "points": 25,
      "description": "Ví dụ để duyệt",
      "isActive": true
    }
  ]
}
```

Response thành công trả cấu hình mới, `version` tăng 1 và `reScoredCount`. POST rules nhận `{ "version": <version hiện tại>, "rules": [...] }`, kể cả danh sách rỗng để xóa toàn bộ tiêu chí. Hai người sửa cùng một version: một yêu cầu thành công, yêu cầu còn lại 409; không ghi đè cấu hình mới.

### Cập nhật thuộc tính Lead

```json
{
  "industry": "CNTT & Viễn thông",
  "employeeCount": 100,
  "title": "Giám đốc",
  "recentActivity": "Yêu cầu Demo"
}
```

Đây là cập nhật từng trường; trường bỏ qua giữ nguyên, null xóa dữ liệu trường. `source` lấy từ quan hệ `leads.source_id -> master_data`; đổi nguồn dùng `PUT /leads/{id}`. Không cho gửi `score`, `classification`, `breakdown`, `ownerUserId`, `configVersion` hay trường ngoài whitelist.

### Response chi tiết điểm

Ví dụ một rule khớp:

```json
{
  "success": true,
  "message": "Xử lý chấm điểm Lead thành công",
  "data": {
    "id": 123,
    "fullName": "Lead thực tế",
    "source": "Giới thiệu từ đối tác / khách hàng",
    "rawScore": 25,
    "score": 25,
    "classification": "COLD",
    "configVersion": 1,
    "scoredAt": "2026-10-14T20:00:00",
    "breakdown": [
      {"ruleId":"SOURCE-REFERRAL","ruleName":"Nguồn giới thiệu","group":"SOURCE","points":25,"actualValue":"Giới thiệu từ đối tác / khách hàng","description":"Ví dụ để duyệt"}
    ]
  }
}
```

Response thật còn các trường cơ bản, owner, sourceId/sourceCode và các thuộc tính chấm điểm. Ngày giờ chuyển thành chuỗi trước JSON để tương thích Java 21; không trả `Timestamp`/`LocalDateTime` cho Gson phản chiếu.

### Validation và mã HTTP

- 200: thao tác scoring thành công; 201: tạo Lead S4-02 thành công.
- 400: JSON/type/trường/ID/policy/tiêu chí/ngưỡng không hợp lệ.
- 401: không có session hoặc session bị hết hiệu lực qua AuthenticationFilter.
- 403: thiếu quyền hành động hoặc scope không hợp lệ.
- 404: không tìm thấy route/Lead trong phạm vi người dùng.
- 405: method không hỗ trợ, có header `Allow`.
- 409: cấu hình chưa kích hoạt, version cũ hoặc transaction xung đột.
- 413: scoring JSON >262144 ký tự; các giới hạn upload/JSON S4-02 vẫn giữ nguyên.
- 500: lỗi DB/hệ thống, transaction rollback; HTTP không trả nội dung SQL hoặc thông tin kết nối.

Giới hạn kỹ thuật: tối đa 200 rules; id duy nhất `[A-Za-z0-9_-]{1,64}`; name <=150, value <=255, description <=1000 ký tự; points nguyên -1000..1000. Nhóm: INDUSTRY, COMPANY_SIZE, SOURCE, INTENT, CUSTOM. Trường: industry, employeeCount, source, recentActivity, title. employeeCount nguyên 0..10000000 hoặc null; industry <=255, title <=150, recentActivity <=1000. Ngưỡng nguyên `0 <= warm < hot <= 100000`; min/max nếu có nằm trong -100000..100000, min<=max và max>=hot. Những giới hạn này bảo vệ API, không thay thế trọng số/ngưỡng nghiệp vụ cần duyệt.

## 3. MySQL và thứ tự migration

Repository dùng migration SQL thủ công, không có Flyway/Liquibase tự áp dụng. Đã có hai file số 014 và hai file số 020; không đổi số hoặc bỏ một file. Chuẩn bị schema Sprint 1–3 theo 001..023, bao gồm các file trùng prefix hiện có, rồi:

1. `024_s4_02_lead_manual_excel.sql`: dependency Lead, import receipt, nguồn và grants.
2. `025_s4_05_lead_scoring.sql`: cấu hình, hồ sơ chấm điểm, điểm và grants mới.

Migration 025 không ALTER/DROP/TRUNCATE Lead cũ, không seed Lead mẫu hoặc điểm mẫu. Có thể chạy lại nhờ CREATE TABLE IF NOT EXISTS và INSERT IGNORE; không ghi đè cấu hình đang dùng.

- `lead_scoring_config`: singleton `id=1`; version BIGINT, hot/warm INT nullable, aggregation VARCHAR(30), min_score/max_score INT nullable, rules_json JSON; updated_by FK users và updated_at DATETIME(6). Mỗi lần cấu hình được lưu version tăng một.
- `lead_scoring_profiles`: PK/FK lead_id -> leads; industry VARCHAR(255), employee_count INT nullable với CHECK không âm, title VARCHAR(150), recent_activity VARCHAR(1000); updated_by FK users, updated_at DATETIME(6). Phân biệt dữ liệu thiếu và số 0.
- `lead_scores`: PK/FK lead_id -> leads; raw_score/score INT, classification HOT/WARM/COLD có CHECK, breakdown_json JSON, config_version BIGINT, input_hash CHAR(64), scored_at DATETIME(6). Index `(classification,score)`.
- Nguồn/owner vẫn do `leads` quản lý; không sao chép chúng sang profile làm nguồn dữ liệu thứ hai.

Quyền mới: `lead.score` được cấp ADMIN/MARKETING; `lead.scoring.manage` mặc định ADMIN. Scope lead được bổ sung nếu vai trò đã có quyền Lead, dùng ALL cho ADMIN/DIRECTOR, TEAM cho TEAM_LEAD và SELF cho các vai trò khác; không ghi đè scope đã tồn tại. Phải gán quyền phù hợp cho vai trò khác qua cơ chế quản lý quyền hiện có nếu AC yêu cầu.

### Áp dụng trên môi trường chạy dự án sau khi nghiệm thu

**Trong phiên này chưa chạy migration trên `crm_db`.** Các lệnh dưới đây là hướng dẫn cho người vận hành; chọn DB đích rõ ràng, kiểm tra schema nền đầy đủ và sao lưu trước khi áp dụng:

```sql
USE crm_db;
SOURCE C:/Users/thang/Projects/customer-relationship-manager-ictu-n5/backend/database/024_s4_02_lead_manual_excel.sql;
SOURCE C:/Users/thang/Projects/customer-relationship-manager-ictu-n5/backend/database/025_s4_05_lead_scoring.sql;
```

Khởi động WAR mới sau migration 024–025; tạo/import production hiện có hook scoring nên cần cả hai migration. Cấu hình DB bằng CRM_DB_URL/USERNAME/PASSWORD hoặc db.properties theo cơ chế sẵn có. Sau khi quy tắc được duyệt, đăng nhập ADMIN, GET config và PUT cấu hình với version hiện tại. Kiểm tra lại điểm bằng API scoring và SELECT trên DB đích. Không tự kích hoạt file cấu hình đề xuất khi chưa duyệt.

## 4. Thuật toán và transaction

`LeadScoringEngine` là hàm xác định từ input và config, mỗi lần bắt đầu tổng từ 0. Không cộng thêm vào điểm cũ.

1. Bỏ rule inactive; input null/không có không khớp, kể cả numeric comparison. Không đổi input thiếu hoặc không phải số thành 0.
2. EQUALS và CONTAINS dùng chuỗi trim, không phân biệt hoa/thường với Locale.ROOT.
3. GREATER_THAN và LESS_THAN theo nhãn FE là >= và <=; chỉ hỗ trợ employeeCount với giá trị số hợp lệ. Không chạy expression/SQL tùy ý.
4. SUM_ALL lấy mọi rule khớp. MAX_PER_GROUP lấy rule có points cao nhất trong mỗi nhóm; nếu bằng điểm thì chọn rule xuất hiện trước. Khi cả nhóm chỉ khớp rule âm, điểm âm đó vẫn được tính.
5. rawScore là tổng rule được chọn; áp dụng minScore/maxScore nếu khai báo để có score. Phân loại HOT nếu score>=hot; WARM nếu score>=warm; còn lại COLD.
6. Lưu snapshot kết quả, breakdown, hash các input thật và version cấu hình bằng JDBC prepared statements.

Luồng chính: **FE Fetch -> AuthenticationFilter -> LeadScoringServlet -> LeadScoringService -> LeadScoringDAO -> MySQL commit -> JSON -> FE**. FE không tự tính điểm/phân loại và không persist chúng trong localStorage.

Transaction và thứ tự khóa: config FOR SHARE/FOR UPDATE -> Lead FOR UPDATE -> profile/score. Cấu hình mới khóa singleton để kiểm tra version rồi thay rules/ngưỡng và tính lại toàn bộ Lead trong transaction; xử lý từng trang 100 Lead nhưng commit một lần, rollback toàn bộ nếu lỗi. Người có quyền quản lý cấu hình có thể ảnh hưởng Lead toàn hệ thống nên không chỉ tính lại Lead của họ.

Tạo/import Lead mặc định gọi hook `beforeCreate` trước khóa nguồn/Lead và `scoreCreated` trước commit. Cập nhật cơ bản và profile cũng tính/lưu điểm trước commit. Nếu thiếu cấu hình, không invent score. Constructors nhận DAO/resolver/hook phục vụ test dependency riêng; constructors production luôn bật hook.

GET scoring kiểm tra input hash/config version và cập nhật snapshot nếu dữ liệu bị thay đổi bởi luồng ngoài service (ví dụ một module khác sửa tên nguồn). Đây là cập nhật khi đọc đối với các luồng ngoài service; không tuyên bố có scheduler hoặc trigger tính ngay cho mọi câu SQL ngoài ứng dụng. Các module S4 khác về tương tác/đổi owner nếu được tích hợp sau này cần dùng cùng hook/service trong transaction.

Scope được suy ra từ users ACTIVE, user_roles, role_permissions và role_module_scopes; không nhận owner/scope từ FE. TEAM bao gồm đội hiện tại và các đội con active qua recursive CTE. List, detail, update và re-score dùng predicate owner có tham số; ngoài scope trả 404 cho record.

## 5. Tích hợp Frontend

Giữ nguyên `lead-scoring.html` và `lead-scoring.css` so với commit FE. JS giữ cấu trúc render, tabs, modal và bộ lọc; thay phần mock/localStorage/tính điểm bằng API thật.

- Bootstrap chờ session, tải config rồi tải tất cả trang Lead thật; không render mock fallback khi API hỏng.
- Lấy breakdown trực tiếp từ GET detail khi mở modal. Hiển thị điểm thô/điểm sau giới hạn, null là “—”; chưa cấu hình không hiển thị khuyến nghị Cold giả.
- Thêm/sửa/bật/tắt/xóa rule gọi POST rules; lưu ngưỡng gọi PUT config, giữ policy đã được cấu hình. Không đổi state thành công trước khi BE lưu.
- Sau thao tác thành công tải lại config và Lead. Nếu DB đã lưu nhưng refresh API lỗi, thông báo phân biệt hai trạng thái và cung cấp nút tải lại.
- Loading, khóa nút khi đang ghi, validation form/type/ngưỡng, toast lỗi HTTP và retry khi tải thất bại. 409 tự tải cấu hình mới; 401 dùng hành vi login có sẵn của CrmApi.
- Marketing/role đọc chỉ xem rules, không sửa cấu hình nếu thiếu `lead.scoring.manage`. Chấm lại cần `lead.score`. Shared shell chỉ thêm menu khi session có `lead.read`.
- CrmApi giữ nguyên endpoint/cơ chế credentials cho module khác. Chỉ chỉnh lời gọi ở trang scoring và điều kiện menu.
- Bỏ thông báo “chuyển đổi thành công” giả của nút cơ hội; S4-08 chưa tích hợp thì báo đúng trạng thái. Nút gọi dùng liên kết tel, không tự tuyên bố hệ thống đã gọi.
- localStorage theme của HTML vẫn giữ nguyên; không còn localStorage score/rules/leads làm nguồn kết quả. Kiểm thử đã chủ động đặt mock cũ vào storage để chứng minh trang bỏ qua chúng.

## 6. Kết quả kiểm thử và bằng chứng

Môi trường: Java 21, Maven 3.9.16, MySQL 8.4, Apache Tomcat 10.1.60. Không chạy test ghi lên `crm_db`.

- `mvn -B -ntp -f backend/pom.xml clean test`: **170 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
- `mvn -B -ntp -f backend/pom.xml clean package`: **170 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**, tạo `backend/target/crm.war`.
- JDBC Maven dùng `crm_sprint1_test`, connection-local TEMPORARY tables để không đụng dữ liệu ứng dụng. Guard riêng của test scoring kiểm tra tên DB chính xác.
- HTTP/UI dùng riêng `crm_s4_05_test_20261014`, clone cấu trúc từ database test nền và seed account/data QA. Có FK thật ở migration 024/025. Tomcat riêng dưới `.local-qa/s4-05/tomcat`, HTTP 18085; hash WAR triển khai khớp WAR vừa build.
- **17 ca HTTP logic đạt**: 15 ca đầu đạt ngay; ca hồi quy HTTP16 lỗi thiếu bảng notifications ở schema QA đã sửa và rerun đạt; HTTP17 kiểm tra import rollback bổ sung đạt. Bao phủ 200/201/400/401/403/404/405/409/500, điểm/chi tiết khớp MySQL, input null, nguồn đổi, min/max, scope SELF/TEAM, quy tắc bật/tắt/xóa, ngưỡng, version stale và ghi đồng thời, rollback config/profile/manual/import, retry import, chống replay, migration lặp giữ dữ liệu, API Sprint trước.
- **6 ca Chrome headless đạt**, 0 lỗi runtime JS: FE đọc điểm thật khớp DB, bỏ mock storage cũ; add/edit/toggle/delete rule và refresh; lưu ngưỡng/re-score/detail; lỗi HTTP không fake thành công; loading lỗi + retry; Marketing SELF và khóa sửa rule/ngưỡng. Các request bình thường đều tới Servlet/MySQL; chỉ các ca lỗi mạng dùng abort có chủ đích để kiểm tra UI.

Các test mới được lưu trong `src/test/java/com/crm/service/scoring/{LeadScoringEngineTest,LeadScoringJdbcTest}.java` và `controller/scoring/LeadScoringServletTest.java`. Test S4-02 được tích hợp nguyên từ nhánh dependency, không đổi assertion để lấy PASS.

Bằng chứng local (gitignored, chứa test fixture/session, không commit):

- `.local-qa/s4-05/maven-clean-test.log`, `maven-clean-package.log`.
- `.local-qa/s4-05/http-results.json`, `http-followup-results.json`, `browser-results.json`; log các lần chạy giữ lại thông tin lỗi rồi sửa.
- Snapshot MySQL before/after rollback, rule/config changes và migration; `ranking.png`, `breakdown.png`.
- Harness JDBC/HTTP/browser và test Tomcat trong `.local-qa/s4-05/`. Mật khẩu/cookie QA chỉ nằm ở thư mục gitignored; không in hoặc đưa vào tài liệu/repository.

## 7. Lỗi phát hiện và khắc phục

1. FE dùng dữ liệu mẫu và localStorage, có catch API lỗi rồi giả lập thành công: thay bằng CrmApi, state từ server, xử lý lỗi và retry thật.
2. FE khởi tạo giao diện mà không gọi API: bootstrap async tải config/Lead thực tế; đọc theo pagination và kiểm tra version nhất quán.
3. `DataScopeService` chưa hỗ trợ Lead: bổ sung module lead, tái sử dụng resolver/predicate SELF/TEAM/ALL, không thêm owner do client tự chọn.
4. Test mới phát hiện JDBC DATETIME có thể trả `LocalDateTime`, Gson Java 21 không serialize phản chiếu an toàn: DAO chuyển cả Timestamp và TemporalAccessor sang chuỗi. Giữ assertion kiểm tra chuỗi, chạy lại toàn bộ Maven đạt.
5. HTTP hồi quy báo notifications 500 vì schema test nền không có bảng S3-08 thường dùng TEMPORARY trong test: áp dụng phần DDL có sẵn của migration 016 vào **schema QA**, rerun đúng assertion đạt; không sửa endpoint/code notification.
6. Harness browser ban đầu cố bấm checkbox input bị CSS ẩn nên timeout: sửa harness bấm label toggle đúng cách người dùng thao tác, không sửa CSS/logic hoặc bỏ assertion; rerun add/edit/toggle/delete đạt.
7. Các quy tắc quy mô mẫu cộng chồng, source label có thể không khớp, ngưỡng chưa có AC: không seed mặc định; engine yêu cầu policy rõ ràng và cấu hình đề xuất để duyệt.

## 8. Bàn giao và trạng thái Git

Tất cả thay đổi ở working tree của `feature/S4-05-lead-scoring`. Không tạo nhánh/PR, không merge, không commit/push, không reset/clean. HTML/CSS của FE nguyên vẹn. Hai thay đổi chưa commit ban đầu được đối chiếu lại và giữ nguyên; warning whitespace ở MasterDataServlet là dòng có sẵn trước tác vụ.

Chưa DONE vì thiếu xác nhận AC/quy tắc nghiệp vụ và bộ default được phê duyệt. Cấu hình synthetic QA không phải cấu hình chạy dự án. Chưa áp dụng migration hoặc cấu hình trên `crm_db`. Sau khi xác nhận, cần kích hoạt cấu hình đã duyệt và nghiệm thu lại ca điểm/ngưỡng thực tế; các phần BE/FE/DB độc lập đã triển khai và kiểm chứng như trên.
