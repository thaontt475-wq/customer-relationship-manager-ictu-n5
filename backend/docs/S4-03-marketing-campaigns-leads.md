# Tài liệu Tích hợp & Kiểm thử S4-03: Chiến dịch Tiếp thị & Quản lý Lead (Marketing Campaigns & Leads)

## 1. Thông tin chung (Sprint 4)
- **Story ID**: S4-03
- **Tên Story**: Marketing Campaign & Lead Association
- **Phân công trách nhiệm**:
  - **S4-03-BE**: Giàng Văn Thắng (API CRUD chiến dịch, liên kết Lead, Service, DAO, SQL)
  - **S4-03-INT**: Hoàng Văn Thắng (Tích hợp FE - BE, kiểm tra định dạng JSON, xác nhận luồng dữ liệu E2E với DB, xử lý lỗi kết nối và dữ liệu nhất quán)

---

## 2. Thiết kế Cơ sở dữ liệu (Migration `024_sprint4_campaigns_leads.sql`)
1. **Bảng `campaigns`**:
   - `id` (BIGINT, PK, AUTO_INCREMENT)
   - `code` (VARCHAR(50), UNIQUE, NOT NULL): Mã định danh chiến dịch
   - `name` (VARCHAR(255), NOT NULL): Tên chiến dịch
   - `type` (VARCHAR(50)): `EMAIL`, `EVENT`, `TELEMARKETING`, `ADS`, `OTHER`
   - `status` (VARCHAR(50)): `PLANNING`, `ACTIVE`, `PAUSED`, `COMPLETED`, `CANCELLED`
   - `start_date`, `end_date` (DATE)
   - `budget`, `actual_cost`, `expected_revenue` (DECIMAL(15,2))
   - `description` (TEXT)
   - `owner_id` (BIGINT -> `users.id`)
   - `is_deleted` (TINYINT(1), default 0)
   - `created_at`, `updated_at` (DATETIME)

2. **Bảng `leads`**:
   - `id`, `full_name`, `company_name`, `email`, `phone`, `source`, `status`, `assigned_to`, `is_deleted`, `created_at`, `updated_at`

3. **Bảng liên kết `campaign_leads`**:
   - `campaign_id` (BIGINT -> `campaigns.id` ON DELETE CASCADE)
   - `lead_id` (BIGINT -> `leads.id` ON DELETE CASCADE)
   - `status` (VARCHAR(50)): `SENT`, `OPENED`, `RESPONDED`, `CONVERTED`, `BOUNCED`
   - `notes` (VARCHAR(500))
   - `created_at`, `updated_at` (DATETIME)
   - Primary Key: `(campaign_id, lead_id)`

---

## 3. Đặc tả Hợp đồng API (RESTful Endpoints)

Tất cả các API được bảo vệ bởi phiên đăng nhập (`userId` trong Session) và phản hồi theo chuẩn chung:
```json
{
  "success": true,
  "message": "Thông báo kết quả",
  "data": { ... }
}
```

| Phương thức | Đường dẫn | Tham số / Body | Mã HTTP | Mô tả |
|-------------|-----------|----------------|---------|-------|
| `GET` | `/api/campaigns` | `keyword`, `status`, `page`, `size` | 200 OK | Danh sách chiến dịch phân trang & tìm kiếm |
| `GET` | `/api/campaigns/{id}` | - | 200 OK | Chi tiết chiến dịch theo ID |
| `POST` | `/api/campaigns` | `CampaignWriteRequest` (JSON) | 201 Created | Tạo mới chiến dịch |
| `PUT` | `/api/campaigns/{id}` | `CampaignWriteRequest` (JSON) | 200 OK | Chỉnh sửa chiến dịch |
| `DELETE` | `/api/campaigns/{id}` | - | 200 OK | Xóa mềm chiến dịch (`is_deleted = 1`) |
| `GET` | `/api/campaigns/{id}/leads` | - | 200 OK | Danh sách Lead đã được gán vào chiến dịch |
| `GET` | `/api/campaigns/{id}/available-leads` | - | 200 OK | Danh sách Lead chưa thuộc chiến dịch này |
| `POST` | `/api/campaigns/{id}/leads` | `{"leadIds": [1, 2], "status": "SENT"}` | 201 Created | Gán 1 hoặc nhiều Lead vào chiến dịch |
| `PUT` | `/api/campaigns/{id}/leads/{leadId}` | `{"status": "RESPONDED", "notes": "..."}` | 200 OK | Cập nhật trạng thái tương tác của Lead |
| `DELETE` | `/api/campaigns/{id}/leads/{leadId}` | - | 200 OK | Gỡ Lead khỏi chiến dịch |

---

## 4. Các điểm tích hợp FE - BE đã chuẩn hóa (S4-03-INT)

1. **Chuẩn hóa cấu hình Base URL (`API_BASE`)**:
   - Sử dụng định tuyến động `window.CRM_BACKEND_BASE` hoặc origin hiện tại kết hợp port 8080/crm, tránh lỗi hardcode URL tĩnh.
2. **Khắc phục lỗi Serialization Date/Time trên Java 21**:
   - Bổ sung `TypeAdapter` chuyên dụng cho `LocalDate` (`yyyy-MM-dd`) và `LocalDateTime` (`yyyy-MM-dd HH:mm:ss`) trong `JsonUtil`. Khắc phục triệt để ngoại lệ `Failed making field 'java.time.LocalDate#year' accessible`.
3. **Tích hợp Shell toàn hệ thống (`shared-shell.js`)**:
   - Đã nhúng mục điều hướng **"Chiến dịch & Lead"** (`campaigns.html`) vào Sidebar động và bổ sung icon SVG `campaign`.
4. **Validation nhất quán giữa FE và BE**:
   - Tên chiến dịch bắt buộc $\ge 3$ ký tự.
   - Mã chiến dịch (`code`) bắt buộc không trùng lặp.
   - Ngân sách không âm.
   - Ngày kết thúc không được trước ngày bắt đầu.
   - Thao tác gán Lead yêu cầu ít nhất 1 `leadId`.

---

## 5. Kết quả Kiểm thử (Test Results)
Tất cả các bài kiểm thử tự động cho module Campaign đều vượt qua 100%:
- `CampaignValidationTest`: 6/6 tests PASS (kiểm tra toàn bộ nghiệp vụ kiểm tra dữ liệu đầu vào).
- `CampaignServletTest`: 12/12 tests PASS (kiểm tra đầy đủ hợp đồng API, phân quyền, HTTP status code và payload JSON).
# QA correction — 2026-10-16

Campaign requests now require action-specific database permissions and campaign SELF/TEAM/ALL scope. Collection search/count share the same owner predicate; detail and mutations check campaign ownership. Creating a campaign cannot spoof another owner.

Apply `027_s4_03_campaign_opportunities.sql` after migration 024 and the Opportunity foundation. It uses the selected schema, creates `campaign_opportunities(campaign_id, opportunity_id)`, and seeds campaign permissions/scopes without inserting demo business data.

- `GET /api/campaigns/{id}/opportunities`: linked opportunities in scope.
- `GET /api/campaigns/{id}/available-opportunities`: eligible opportunities in scope.
- `POST /api/campaigns/{id}/opportunities`: JSON `{"opportunityIds":[1,2]}`; requires `campaign.opportunity_manage`; returns `data.linkedCount` (201).
- `DELETE /api/campaigns/{id}/opportunities/{opportunityId}`: removes the relation (200).
- Campaign list/detail include `leadCount`, `opportunityCount`, and decimal `wonRevenue`. Only non-deleted Opportunities with the existing business status `WON` contribute to wonRevenue; OPEN/LOST and deleted rows do not.
- Linking locks the campaign and source opportunities in a transaction. Invalid, missing or out-of-scope IDs abort the whole batch; duplicate links cannot inflate counts.

Frontend exposes an Opportunity modal and reads these metrics directly from Campaign API responses. Regression covers permission denial, scoped list/count, aggregate correctness, duplicate linking, rollback and real browser/MySQL persistence. Existing migration 024 still contains historical demo seeds; review that separately when planning the combined Sprint 4 migration. No automatic merge is authorized.
