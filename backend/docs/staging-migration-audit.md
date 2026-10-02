# BÁO CÁO KIỂM TOÁN MIGRATION CƠ SỞ DỮ LIỆU TỪ MÃ NGUỒN THỰC TẾ
## (STAGING MIGRATION AUDIT FROM REAL SOURCE)

> **Phiên bản:** 3.1 (TASK 04A.3 — Final Staging Plan Validation & MySQL 8.4 Deterministic Compatibility)
> **Repository:** `thaontt475-wq/customer-relationship-manager-ictu-n5`
> **Baseline Commit:** `0479ea50109bb11fa28a751555ebc0be51ab75fe` (Khớp `origin/develop`)
> **Worktree:** `crm-staging-test-plan` | **Branch:** `docs/CRM-staging-test-plan`
> **Trạng thái:** **WAITING_FOR_APPROVAL** (Chỉ khảo sát và thẩm định; KHÔNG chạy SQL, KHÔNG tạo database, KHÔNG triển khai Tomcat)

---

## 1. Kiểm kê thực tế toàn bộ tệp SQL từ Git

Kiểm kê trực tiếp thông qua lệnh `git ls-files backend/database` trên branch `docs/CRM-staging-test-plan` tại baseline commit `0479ea50109bb11fa28a751555ebc0be51ab75fe`. Kho mã nguồn chứa đúng 17 tệp SQL (1 tệp schema hợp nhất, 1 tệp fixtures vai trò, và 15 tệp migration lịch sử trong thư mục `backend/database/migrations/`).

### Bảng kiểm kê chính xác 17 tệp SQL với mã băm SHA-256

| STT | Tệp SQL | Đường dẫn Git tương đối | Mã băm SHA-256 | Mục đích nghiệp vụ thực tế |
|---|---|---|---|---|
| **0** | `schema.sql` | `backend/database/schema.sql` | `48C27B0852DE957A4397703BF5CD18826CE8C974DB512052219CDB9CA0578AE8` | Schema hợp nhất cơ sở (26 bảng, quan hệ khóa ngoại, index, đã có `audit_logs`) |
| **0b** | `data.sql` | `backend/database/data.sql` | `5B6E429EC883A2102A0590EA29C21E4A7626D9CDBB556498BE0286CE46E599D4` | Fixtures khởi tạo 4 vai trò: Admin, Sales Rep, Accountant, Team Lead |
| **1** | `CRM-21-login.sql` | `backend/database/migrations/CRM-21-login.sql` | `BF84C65D6ACC11357A2CDAE905CC65556EBBBDEAFE6A116790C7DF7845F51683` | Bổ sung `display_name`, `active` cho `users`; tạo bảng `roles`, `user_roles` |
| **2** | `CRM-25-role-data-scope.sql` | `backend/database/migrations/CRM-25-role-data-scope.sql` | `E9EE71E1C0433453948862BDAD0FC8FDF6CE862A2690A72B2414BD5F5CF2EC91` | Bổ sung cột `data_scope` vào bảng `users` |
| **3** | `CRM-25-scoped-domain-records.sql` | `backend/database/migrations/CRM-25-scoped-domain-records.sql` | `F8F99A2FF9D9D4B696B83369C9E71DC7C8666E38BEDC1E6973A640E993FA2DE1` | Tạo 4 bảng nghiệp vụ phân quyền cơ sở: `customers`, `opportunities`, `activities`, `quotes` |
| **4** | `CRM-29-team-assignment.sql` | `backend/database/migrations/CRM-29-team-assignment.sql` | `26078A72DE4CE490403156238C8D32ECECC2C908FB5269C396782F83707F308B` | Tạo bảng `teams` cơ bản và thêm cột `team_id` vào `users` |
| **5** | `CRM-29-team-lead-role.sql` | `backend/database/migrations/CRM-29-team-lead-role.sql` | `E5BEBE5D25B4CCB059A40047CEB27C305B845CF4E8FD2537F58F8D10ABDBC48D` | Chèn bổ sung vai trò `'Team Lead'` vào bảng `roles` |
| **6** | `CRM-35-user-signature.sql` | `backend/database/migrations/CRM-35-user-signature.sql` | `6675D2B5ABC536EDA537E351181CA61647ACE436AD59E2358C5537FC6BB059D0` | Bổ sung cột `signature TEXT NULL` vào bảng `users` |
| **7** | `CRM-36-avatar.sql` | `backend/database/migrations/CRM-36-avatar.sql` | `107948CFF7719FE4D4AA6EEF5089B7B0A5A7F5510C929F4675F332A4E05C4695` | Tạo bảng lưu trữ đường dẫn ảnh đại diện `user_avatars` |
| **8** | `CRM-37-audit-logs.sql` | `backend/database/migrations/CRM-37-audit-logs.sql` | `5DA7D7C12FA99F276449DAC10586F1F684263E6211FA28CAAFA10BA8540140C8` | Tạo bảng lưu vết thay đổi nghiệp vụ bất biến `audit_logs` |
| **9** | `CRM-39-products.sql` | `backend/database/migrations/CRM-39-products.sql` | `C06F2EFA5CB71F7BA3BD5C33545210EB39BF66795E498506456DBA1B14BE5BBF` | Tạo 7 bảng sản phẩm, báo giá, cơ hội, hợp đồng, đơn hàng liên kết |
| **10** | `CRM-42-organization-structure.sql` | `backend/database/migrations/CRM-42-organization-structure.sql` | `25AC2EC09F5243CB29A166160883FBED1E8A88277B9A2D411149A1FFF195215E` | Mở rộng cấu trúc phân cấp cây tổ chức cho bảng `teams` (`parent_id`, `leader_user_id`, `region`,...) |
| **11** | `CRM-44-master-data.sql` | `backend/database/migrations/CRM-44-master-data.sql` | `51AE5DF131C4F0B30A36CF22CA0FFFF6690E53B1A37648B10959A37DD900C7B6` | Tạo bảng `categories`, `leads`, thêm trường vào `customers`, `activities` và nạp seed 27 danh mục |
| **12** | `CRM-46-custom-fields.sql` | `backend/database/migrations/CRM-46-custom-fields.sql` | `A4E880A0B0DAE74921ECEF04BFBB05C6299F0DD3A508052981C7896FB3E8CD25` | Tạo 3 bảng trường tùy chỉnh: `custom_field_definitions`, `custom_field_options`, `custom_field_values` |
| **13** | `CRM-47-pipeline.sql` | `backend/database/migrations/CRM-47-pipeline.sql` | `0754DB1B28A5073BC6ECC8B0AD443AC77782D6E63EC958978F4648D6B493EC39` | Tạo `pipeline_stages`, thêm trường `stage_id`, `amount`... vào `opportunities` và seed 6 giai đoạn bán hàng |
| **14** | `CRM-48-win-loss-competitors.sql` | `backend/database/migrations/CRM-48-win-loss-competitors.sql` | `0F00236CCF2C80E1EEE9FE630D8660AFAC263E33115C31977EF7AE1E2FEDDAA4` | Tạo bảng `win_loss_reasons` và bảng `competitors` |
| **15** | `CRM-49-reconcile-sprint2-mysql.sql` | `backend/database/migrations/CRM-49-reconcile-sprint2-mysql.sql` | `53EDA83EE6BCDB00CA30CF168487F2201461FBA157BCD8E989395BC2983A3255` | Script đồng bộ gia tăng Sprint 2 dùng Dynamic SQL (reconcile additive patch) |

> [!CAUTION]
> **Loại bỏ hoàn toàn các giả định sai về tên tệp:**
> Các tên tệp như `CRM-22-user-activity-log.sql`, `CRM-23-user-role.sql`, `CRM-24-user-permission.sql`, `CRM-28-pipeline-management.sql`, `CRM-30-user-lock-handover.sql`, `CRM-32-advanced-lead-filtering.sql` **HOÀN TOÀN KHÔNG TỒN TẠI** trong repository. Toàn bộ kết luận trước đây dựa trên các tên tệp này bị hủy bỏ 100%.

---

## 2. Xác nhận tình trạng bảng `audit_logs` trong `schema.sql`

Khảo sát trực tiếp tệp [`backend/database/schema.sql`](file:///c:/Users/thang/Projects/crm-staging-test-plan/backend/database/schema.sql) tại các dòng 370–382:

```sql
370: CREATE TABLE IF NOT EXISTS audit_logs (
371:     id BIGINT AUTO_INCREMENT PRIMARY KEY,
372:     actor_user_id BIGINT NOT NULL,
373:     action VARCHAR(50) NOT NULL,
374:     object_type VARCHAR(50) NOT NULL,
375:     object_id BIGINT NOT NULL,
376:     before_value JSON NOT NULL,
377:     after_value JSON NOT NULL,
378:     created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
379:     INDEX idx_audit_logs_actor_time (actor_user_id, created_at),
380:     INDEX idx_audit_logs_object_time (object_type, object_id, created_at),
381:     INDEX idx_audit_logs_created_at (created_at)
382: ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Kết luận xác minh:**
Bảng `audit_logs` **ĐÃ TỒN TẠI ĐẦY ĐỦ VÀ CHÍNH XÁC** trong `schema.sql`. Do đó:
- **TUYỆT ĐỐI KHÔNG CHẠY LẠI** `CRM-37-audit-logs.sql` trên schema hợp nhất (file này chứa `USE crm_db;`).
- **TUYỆT ĐỐI KHÔNG CHẠY LẠI** `CRM-49-reconcile-sprint2-mysql.sql` trên schema hợp nhất vì CRM-49 cố gắng tạo lại `audit_logs`, chứa 4 lệnh `USE crm_db;`, và chứa Dynamic SQL không an toàn.

---

## 3. Bảng đối chiếu chi tiết Schema hợp nhất và 15 Migration thật

| Tệp SQL | Đối tượng thay đổi | Đã có trong schema.sql | Cần áp dụng | Phụ thuộc | Rủi ro cú pháp / nghiệp vụ | Bằng chứng từ mã nguồn |
|---|---|---|---|---|---|---|
| `CRM-21-login.sql` | Thêm `display_name`, `active` vào `users`; tạo `roles`, `user_roles` | **ĐÃ CÓ ĐẦY ĐỦ** (`users`: dòng 30-31; `roles`: dòng 108-111; `user_roles`: dòng 113-119) | **KHÔNG** | Bảng `users` | Lỗi `ERROR 1060: Duplicate column name 'display_name'`; Chứa `USE crm_db;` | Ghi chú dòng 1-2: `-- Fresh databases use schema.sql instead; do not run both.` |
| `CRM-25-role-data-scope.sql` | Thêm cột `data_scope` vào bảng `users` | **ĐÃ CÓ ĐẦY ĐỦ** (`users`: dòng 36) | **KHÔNG** | Bảng `users` | Lỗi `ERROR 1060: Duplicate column name 'data_scope'`; Chứa `USE crm_db;` | Dòng 1-2: `-- Fresh databases use schema.sql instead; do not run both.` |
| `CRM-25-scoped-domain-records.sql` | Tạo 4 bảng `customers`, `opportunities`, `activities`, `quotes` | **ĐÃ CÓ ĐẦY ĐỦ** (dòng 126–164) | **KHÔNG** | Bảng `users` | Chứa `USE crm_db;` (dòng 2); Ghi đè vào production nếu chạy trực tiếp | Dòng 4–42 định nghĩa 4 bảng y hệt dòng 126–164 của `schema.sql` |
| `CRM-29-team-assignment.sql` | Tạo `teams` đơn giản; thêm `team_id` và FK vào `users` | **ĐÃ CÓ ĐẦY ĐỦ** (`teams`: dòng 6-21; `users`: dòng 35, 43, 44) | **KHÔNG** | Bảng `users`, `teams` | Lỗi `ERROR 1060: Duplicate column name 'team_id'`; Chứa `USE crm_db;` | Dòng 1-2: `-- Fresh databases use schema.sql instead; do not run both.` |
| `CRM-29-team-lead-role.sql` | Chèn vai trò `'Team Lead'` vào bảng `roles` | **ĐÃ CÓ TRONG DATA.SQL** (`data.sql` dòng 4) | **KHÔNG** | Bảng `roles` | Chứa `USE crm_db;` (dòng 2) | `data.sql:4` đã có `INSERT INTO roles VALUES ('Admin'), ('Sales Rep'), ('Accountant'), ('Team Lead')` |
| `CRM-35-user-signature.sql` | Thêm cột `signature TEXT NULL` vào `users` | **ĐÃ CÓ ĐẦY ĐỦ** (`users`: dòng 33) | **KHÔNG** | Bảng `users` | Chứa `USE crm_db;` (dòng 2) | `schema.sql:33` ghi rõ `signature TEXT NULL,` |
| `CRM-36-avatar.sql` | Tạo bảng `user_avatars` | **ĐÃ CÓ ĐẦY ĐỦ** (dòng 167–173) | **KHÔNG** | Bảng `users` | Thao tác thừa; bảng đã có sẵn | `schema.sql:167-173` định nghĩa chính xác bảng `user_avatars` |
| `CRM-37-audit-logs.sql` | Tạo bảng `audit_logs` | **ĐÃ CÓ ĐẦY ĐỦ** (dòng 370–382) | **KHÔNG** | Không phụ thuộc bảng khác | Chứa `USE crm_db;` (dòng 2); Ghi nhầm sang DB sản xuất | `schema.sql:370-382` định nghĩa chính xác cấu trúc `audit_logs` |
| `CRM-39-products.sql` | Tạo 7 bảng: `products`, `quote_items`, `opportunity_products`, `contracts`, `contract_items`, `orders`, `order_items` | **ĐÃ CÓ ĐẦY ĐỦ** (dòng 286–368) | **KHÔNG** | Bảng `users`, `quotes`, `opportunities` | Chứa `USE crm_db;` (dòng 3) | `schema.sql:286-368` chứa toàn bộ 7 bảng của CRM-39 |
| `CRM-42-organization-structure.sql` | Thêm các trường tổ chức phân cấp (`parent_id`, `leader_user_id`, `region`...) vào `teams` | **ĐÃ CÓ ĐẦY ĐỦ** (dòng 6–21 và dòng 48–63) | **KHÔNG** | Bảng `teams`, `users` | Lỗi `ERROR 1060: Duplicate column name 'parent_id'`; Chứa `USE crm_db;` | `schema.sql:6-21` đã tạo bảng `teams` với đầy đủ cấu trúc này |
| `CRM-44-master-data.sql` | Tạo `categories`, `leads`; thêm trường vào `customers`, `activities`; seed 27 danh mục | **CHỈ CÓ BẢNG CATEGORIES**; thiếu `leads`, các cột bổ sung và dữ liệu seed | **CẦN TRÍCH XUẤT** (Bổ sung phần còn thiếu vào staging-02) | Bảng `customers`, `activities`, `users` | Chứa `USE crm_db;` (dòng 3); Chứa `ADD COLUMN IF NOT EXISTS` không hợp lệ trong MySQL 8.4 | `CRM-44-master-data.sql` dòng 21-38, 40-74; `schema.sql` thiếu bảng `leads` |
| `CRM-46-custom-fields.sql` | Tạo 3 bảng `custom_field_definitions`, `custom_field_options`, `custom_field_values` | **ĐÃ CÓ ĐẦY ĐỦ** (dòng 240–283) | **KHÔNG** | Không có FK ngoại | Bảng đã được tạo từ `schema.sql` | `schema.sql:240-283` định nghĩa toàn bộ 3 bảng custom fields |
| `CRM-47-pipeline.sql` | Tạo `pipeline_stages`; thêm `stage_id`, `amount`... vào `opportunities`; seed 6 stages | **CHỈ CÓ BẢNG PIPELINE_STAGES**; thiếu các cột trên `opportunities` và seed stages | **CẦN TRÍCH XUẤT** (Bổ sung phần còn thiếu vào staging-02) | Bảng `opportunities` | Chứa `USE crm_db;` (dòng 2); Chứa `ADD COLUMN IF NOT EXISTS` không hợp lệ trong MySQL 8.4 | `CRM-47-pipeline.sql` dòng 23-38; `schema.sql:136-144` chỉ có các trường cơ bản |
| `CRM-48-win-loss-competitors.sql` | Tạo bảng `win_loss_reasons` và `competitors` | **ĐÃ CÓ ĐẦY ĐỦ** (dòng 211–237) | **KHÔNG** | Không có | Bảng đã được tạo từ `schema.sql` | `schema.sql:211-237` định nghĩa cả hai bảng |
| `CRM-49-reconcile-sprint2-mysql.sql` | Bản vá tổng hợp Sprint 2 dùng Dynamic SQL, thêm role `'Director'` | **MỘT PHẦN** (Chứa nhiều câu lệnh đã có trong `schema.sql`) | **KHÔNG CHẠY NGUYÊN BẢN** | Nhiều bảng Sprint 1 & 2 | **RỦI RO RẤT CAO:** Chứa 4 lệnh `USE crm_db;` (dòng 7, 10, 29, 152) và Dynamic SQL query `information_schema` | Có 4 dòng `USE crm_db;`; Dynamic SQL chuẩn bị bằng `PREPARE crm_stmt` |

---

## 4. Kiểm tra tính tương thích MySQL 8.4 và Phân tích SQL động (Dynamic SQL Audit)

### 4.1. Phân tích rủi ro cú pháp `ADD COLUMN IF NOT EXISTS` trên MySQL 8.4
Trong `CRM-44-master-data.sql` (dòng 21–23, 36–37) và `CRM-47-pipeline.sql` (dòng 24–29), các tác giả đã sử dụng cú pháp:
```sql
ALTER TABLE customers ADD COLUMN IF NOT EXISTS industry_id BIGINT NULL;
```
**Phát hiện kỹ thuật nghiêm ngặt:**
Cú pháp `ADD COLUMN IF NOT EXISTS` chỉ được hỗ trợ trên MariaDB, **hoàn toàn KHÔNG được hỗ trợ trên Oracle MySQL Server (kể cả phiên bản MySQL 8.0 và MySQL 8.4.9 LTS hiện tại)**. Nếu chạy lệnh này trên MySQL 8.4, hệ thống sẽ dừng ngay với lỗi cú pháp:
`ERROR 1064 (42000): You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near 'IF NOT EXISTS...'`

Đó chính là nguyên nhân `CRM-49-reconcile-sprint2-mysql.sql` phải sử dụng Dynamic SQL phức tạp (`information_schema.columns` checks) để khắc phục nhược điểm này.
Tuy nhiên, vì môi trường Staging được **khởi tạo từ cơ sở dữ liệu rỗng với trạng thái schema được xác định hoàn toàn từ trước**, ta không cần dynamic SQL hay `IF NOT EXISTS`. Thay vào đó, tệp `staging-02-reconcile.sql` được thiết kế bằng các câu lệnh DDL tất định (Deterministic DDL), đảm bảo tương thích 100% với MySQL 8.4:
```sql
ALTER TABLE customers ADD COLUMN industry_id BIGINT NULL, ADD COLUMN company_size_id BIGINT NULL;
ALTER TABLE activities ADD COLUMN activity_type_id BIGINT NULL;
ALTER TABLE opportunities ADD COLUMN stage_id BIGINT NULL, ADD COLUMN amount DECIMAL(15, 2) NULL DEFAULT 0.00, ADD COLUMN contact_name VARCHAR(255) NULL, ADD COLUMN lost_reason VARCHAR(500) NULL, ADD COLUMN probability INT NULL;
```

### 4.2. Kiểm toán toàn diện các đoạn SQL động (Dynamic SQL Audit)
Kiểm tra trực tiếp tất cả câu lệnh `PREPARE`, `EXECUTE`, `DEALLOCATE PREPARE` trong toàn bộ mã nguồn SQL:

1. **Trong `schema.sql` (dòng 48–63):**
   ```sql
   SET @teams_leader_fk_exists = (
       SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
       WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'teams'
         AND CONSTRAINT_NAME = 'fk_teams_leader' AND CONSTRAINT_TYPE = 'FOREIGN KEY'
   );
   SET @teams_leader_fk_sql = IF(@teams_leader_fk_exists = 0,
       'ALTER TABLE teams ADD CONSTRAINT fk_teams_leader FOREIGN KEY (leader_user_id) REFERENCES users(id) ON DELETE SET NULL',
       'SELECT 1'
   );
   PREPARE teams_leader_fk_stmt FROM @teams_leader_fk_sql;
   EXECUTE teams_leader_fk_stmt;
   DEALLOCATE PREPARE teams_leader_fk_stmt;
   ```
   - **Đánh giá an toàn:** Đoạn mã này sử dụng hàm `DATABASE()`. Khi tiến trình kết nối tới `crm_db_staging`, `DATABASE()` trả về chính xác `'crm_db_staging'`. Biến `@teams_leader_fk_sql` hoàn toàn không chứa tiền tố database cứng (`crm_db.`).
   - **Tối ưu hóa Staging:** Vì database staging khởi tạo rỗng, bảng `teams` chắc chắn chưa có khóa ngoại này, do đó trong bản staging có thể thực thi trực tiếp DDL mà không cần bọc trong câu lệnh PREPARE.

2. **Trong `CRM-49-reconcile-sprint2-mysql.sql`:**
   - Chứa 11 khối Dynamic SQL sử dụng `information_schema.columns` và `information_schema.TABLE_CONSTRAINTS` kết hợp `PREPARE crm_stmt`.
   - **Nguy cơ chí mạng:** Tệp này chứa 4 chỉ thị `USE crm_db;`. Nếu chạy tệp này, ngữ cảnh `DATABASE()` sẽ lập tức bị ép chuyển về `crm_db`, khiến toàn bộ Dynamic SQL phía sau tác động thẳng vào cơ sở dữ liệu sản xuất.
   - **Quyết định an toàn:** **TUYỆT ĐỐI LOẠI BỎ CRM-49** khỏi quy trình staging. Không dùng dynamic SQL để vá staging mà dùng tệp `staging-02-reconcile.sql` tĩnh, tất định.

---

## 5. Danh sách các thay đổi thực sự còn thiếu trong Schema hợp nhất

Sau khi đối chiếu tỉ mỉ từng dòng SQL, `schema.sql` là bản snapshot mạnh mẽ nhưng **chưa bao gồm toàn bộ thay đổi của Sprint 2**. Dưới đây là danh sách chính xác các đối tượng còn thiếu:

### 5.1. Bảng dữ liệu còn thiếu:
1. **Bảng `leads` (CRM-44 / CRM-49):**
   ```sql
   CREATE TABLE IF NOT EXISTS leads (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       name VARCHAR(255) NOT NULL,
       owner_user_id BIGINT NOT NULL,
       lead_source_id BIGINT NULL,
       industry_id BIGINT NULL,
       company_size_id BIGINT NULL,
       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT fk_leads_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
   ```

### 5.2. Các cột bổ sung còn thiếu trên bảng hiện hữu:
2. **Trên bảng `customers` (CRM-44 / CRM-49):**
   - `industry_id BIGINT NULL`
   - `company_size_id BIGINT NULL`
3. **Trên bảng `activities` (CRM-44 / CRM-49):**
   - `activity_type_id BIGINT NULL`
4. **Trên bảng `opportunities` (CRM-47 / CRM-49):**
   - `stage_id BIGINT NULL`
   - `amount DECIMAL(15, 2) NULL DEFAULT 0.00`
   - `contact_name VARCHAR(255) NULL`
   - `lost_reason VARCHAR(500) NULL`
   - `probability INT NULL`

### 5.3. Dữ liệu cấu hình & Master Data còn thiếu:
5. **Vai trò `'Director'` (CRM-49 dòng 239):**
   - Bảng `roles` trong `data.sql` chỉ có: Admin, Sales Rep, Accountant, Team Lead. Cần thêm role `Director` để phục vụ CRM-21 và CRM-42.
6. **27 danh mục Master Data mặc định (CRM-44 dòng 40–74):**
   - **8 danh mục `INDUSTRY`:** `IT`, `RETAIL`, `MANUFACTURING`, `FINANCE`, `REAL_ESTATE`, `EDUCATION`, `HEALTHCARE`, `OTHER`.
   - **5 danh mục `COMPANY_SIZE`:** `MICRO`, `SMALL`, `MEDIUM`, `LARGE`, `ENTERPRISE`.
   - **8 danh mục `LEAD_SOURCE`:** `WEBSITE`, `FACEBOOK`, `GOOGLE`, `REFERRAL`, `EVENT`, `HOTLINE`, `EMAIL`, `OTHER`.
   - **6 danh mục `ACTIVITY_TYPE`:** `CALL`, `MEETING`, `DEMO`, `EMAIL`, `TASK`, `LUNCH`.
   - *Tổng cộng chính xác: 8 + 5 + 8 + 6 = 27 bản ghi.*
7. **6 giai đoạn bán hàng mặc định (CRM-47 dòng 32–38):**
   - `PROSPECTING` (Thứ tự 1, Xác suất 10%)
   - `QUALIFICATION` (Thứ tự 2, Xác suất 25%)
   - `PROPOSAL` (Thứ tự 3, Xác suất 50%)
   - `NEGOTIATION` (Thứ tự 4, Xác suất 75%)
   - `CLOSED_WON` (Thứ tự 5, Xác suất 100%, is_won = TRUE)
   - `CLOSED_LOST` (Thứ tự 6, Xác suất 0%, is_lost = TRUE)

---

## 6. Quy trình phân quyền MySQL an toàn (Two-Tier Role Separation)

Để loại trừ hoàn toàn nguy cơ ứng dụng staging vô tình thay đổi hoặc phá hủy cấu trúc bảng, hệ thống phân chia rõ ràng hai tài khoản cơ sở dữ liệu:

```
+-----------------------------------------------------------------------------------+
|                        MÔ HÌNH PHÂN QUYỀN HAI TẦNG TRÊN MYSQL                     |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  [ TẦNG 1: TÀI KHOẢN KHỞI TẠO SCHEMA ]        [ TẦNG 2: TÀI KHOẢN JDBC RUNTIME ]  |
|  User: crm_staging_admin                      User: crm_staging_user              |
|                                                                                   |
|  Phạm vi: CSDL crm_db_staging duy nhất        Phạm vi: CSDL crm_db_staging duy nhất|
|  Quyền hạn DDL + DML:                         CHỈ CÓ QUYỀN DML:                   |
|  - CREATE, ALTER, DROP, INDEX, REFERENCES     - SELECT, INSERT, UPDATE, DELETE    |
|  - SELECT, INSERT, UPDATE, DELETE             TUYỆT ĐỐI KHÔNG CÓ DDL:             |
|                                               - CẤM CREATE, ALTER, DROP, INDEX    |
|  Sử dụng: Chỉ chạy bằng tay lúc khởi tạo,     Sử dụng: Cấu hình vào Tomcat qua    |
|  không cấu hình vào file ứng dụng             CRM_DB_USERNAME / CRM_DB_PASSWORD   |
|                                                                                   |
|  -------------------------------------------------------------------------------  |
|  CẢ HAI TÀI KHOẢN ĐỀU BỊ TƯỚC QUYỀN HOÀN TOÀN TRÊN crm_db (PRODUCTION):           |
|  REVOKE ALL PRIVILEGES ON crm_db.* FROM 'crm_staging_admin'@'localhost';          |
|  REVOKE ALL PRIVILEGES ON crm_db.* FROM 'crm_staging_user'@'localhost';           |
+-----------------------------------------------------------------------------------+
```

### Các câu lệnh phân quyền chuẩn xác (Chỉ chạy trong TASK 04B sau khi được phê duyệt):

```sql
-- 1. Tài khoản khởi tạo schema (Admin)
CREATE USER IF NOT EXISTS 'crm_staging_admin'@'localhost' IDENTIFIED BY '<STRONG_PASSWORD_1>';
GRANT CREATE, ALTER, DROP, INDEX, REFERENCES, SELECT, INSERT, UPDATE, DELETE ON crm_db_staging.* TO 'crm_staging_admin'@'localhost';
REVOKE ALL PRIVILEGES ON crm_db.* FROM 'crm_staging_admin'@'localhost';

-- 2. Tài khoản runtime của Tomcat (Least-Privilege DML Only)
CREATE USER IF NOT EXISTS 'crm_staging_user'@'localhost' IDENTIFIED BY '<STRONG_PASSWORD_2>';
GRANT SELECT, INSERT, UPDATE, DELETE ON crm_db_staging.* TO 'crm_staging_user'@'localhost';
REVOKE CREATE, ALTER, DROP, INDEX, REFERENCES ON crm_db_staging.* FROM 'crm_staging_user'@'localhost';
REVOKE ALL PRIVILEGES ON crm_db.* FROM 'crm_staging_user'@'localhost';

FLUSH PRIVILEGES;
```

---

## 7. Đề xuất quy trình khởi tạo Database Staging rỗng chuẩn hóa

Thay vì cố gắng chạy các tệp migration lịch sử chứa lệnh nguy hiểm `USE crm_db;`, thiết kế **đúng một quy trình khởi tạo duy nhất** gồm 3 tệp SQL staging tĩnh, rõ ràng, được kiểm tra trước:

```
[BƯỚC 1: DDL CƠ SỞ]             [BƯỚC 2: BÙ TRÙ SPRINT 2]          [BƯỚC 3: DỮ LIỆU KIỂM THỬ]
staging-01-schema.sql    --->  staging-02-reconcile.sql   --->  staging-03-fixtures.sql
(26 bảng từ schema.sql,        (leads, cột bổ sung,             (vai trò, 7 users, 3 teams,
 lược bỏ CREATE DB & USE)       27 categories, 6 stages)         4 customers, 2 opps, 2 prods)
```

### 7.1. Chi tiết 3 tệp SQL Staging độc lập:
1. **`staging-01-schema.sql`:**
   - Tạo từ `schema.sql`.
   - Lược bỏ dòng 1 (`CREATE DATABASE...`) và dòng 3 (`USE crm_db;`).
   - Khởi tạo 26 bảng nền tảng và ràng buộc `fk_teams_leader`.
2. **`staging-02-reconcile.sql`:**
   - Tạo mới độc lập, chứa toàn bộ các đối tượng còn thiếu của Sprint 2 đã xác định ở Mục 5.
   - Sử dụng cú pháp DDL thuần túy, tất định, tương thích 100% MySQL 8.4 (không dùng `IF NOT EXISTS` cho cột).
   - Tuyệt đối không chứa `USE crm_db;` hay Dynamic SQL.
3. **`staging-03-fixtures.sql`:**
   - Tạo từ `data.sql` (bỏ `USE crm_db;`) cộng với bộ dữ liệu Synthetic Test Fixtures (7 người dùng, 3 nhóm, khách hàng, sản phẩm).

### 7.2. Cú pháp thực thi MySQL CLI chuẩn xác (Verified Syntax):
- Trong MySQL CLI 8.4, cờ `-f, --force` dùng để tiếp tục khi gặp lỗi. **Mặc định khi không truyền `-f`, MySQL CLI luôn dừng ngay lập tức khi gặp lỗi (force = FALSE).**
- Lệnh thực thi chuẩn:
  ```powershell
  # Thực thi tuần tự bằng tài khoản crm_staging_admin (Fail-Fast: dừng ngay nếu có lỗi)
  & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" --host=localhost --port=3306 --user=crm_staging_admin -p crm_db_staging -e "source C:/Tools/crm-stg-artifacts/staging-01-schema.sql"
  & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" --host=localhost --port=3306 --user=crm_staging_admin -p crm_db_staging -e "source C:/Tools/crm-stg-artifacts/staging-02-reconcile.sql"
  & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" --host=localhost --port=3306 --user=crm_staging_admin -p crm_db_staging -e "source C:/Tools/crm-stg-artifacts/staging-03-fixtures.sql"
  ```

> [!IMPORTANT]
> **PHÂN BIỆT RÕ RÀNG TIẾN ĐỘ SQL STAGING (DESIGN VERIFIED VS PENDING RUNTIME):**
> - **Đã xác minh thiết kế xây dựng SQL staging:** `DESIGN VERIFIED` (Đã rà soát 15 migration thật, loại bỏ `ADD COLUMN IF NOT EXISTS`, đối chiếu 26 bảng nền tảng + bảng `leads`, và 27 danh mục Master Data).
> - **Chưa tạo ba tệp SQL staging vật lý:** `PENDING RUNTIME` (Chưa tạo tệp trên đĩa trong TASK 04A).
> - **Chưa xác minh toàn bộ nội dung SQL staging thực tế:** `PENDING RUNTIME` (Chưa quét regex trên tệp vật lý).
> - **Chưa thực thi trên engine MySQL 8.4:** `PENDING RUNTIME` (Chưa chạy script trên MySQL).
> - **Mọi kết luận về khả năng khởi tạo thành công database phải giữ ở trạng thái `PENDING RUNTIME`**; tuyệt đối không ghi nhận SQL staging đã vượt qua kiểm tra chỉ dựa trên thiết kế tĩnh.

---

## 8. Trạng thái các Migration ngoài luồng (CRM-50 và CRM-51)

- `CRM-50-audit-business-mutations.sql` và `CRM-51-quote-product-pricing.sql` chỉ tồn tại trong nhánh phát triển dở dang `[fix/sprint2-complete-remaining-ac]`, **chưa được merge vào `origin/develop`**.
- Cả hai file đều chứa lệnh cứng `USE crm_db;`.
- **Quyết định kiểm toán:** Giữ nguyên trạng thái **`NOT VERIFIED (Unmerged development code)`**, loại bỏ hoàn toàn khỏi kế hoạch khởi tạo và kiểm thử của TASK 04A và TASK 04B.
