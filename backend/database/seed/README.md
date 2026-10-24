# HƯỚNG DẪN DỮ LIỆU MẪU (SEED DATA) — NOVACRM

Thư mục này chứa bộ script nạp dữ liệu mẫu (Seed Data) chuẩn hóa cho hệ thống **NovaCRM**, phục vụ việc demo tính năng cho Mentor và chạy kiểm thử tự động (E2E / QA / Regression).

---

## 1. Cấu trúc và Thứ tự thực thi

Các tệp SQL được thiết kế theo đúng thứ tự phụ thuộc khóa ngoại (Foreign Keys) và có tính chất idempotent (`INSERT ... ON DUPLICATE KEY UPDATE`):

| Thứ tự | Tệp tin SQL | Nội dung chính |
| :--- | :--- | :--- |
| **01** | `01_seed_master_data.sql` | Danh mục chuẩn: Ngành nghề, Quy mô công ty, Khu vực địa lý, Giai đoạn cơ hội, Lý do Thắng/Thua, Đối thủ cạnh tranh, Trường mở rộng. |
| **02** | `02_seed_users_roles.sql` | Cơ cấu phân quyền: Roles, Permissions, Role-Permission mappings, Phạm vi dữ liệu (SELF / TEAM / ALL), Phòng ban (Teams) và **13 tài khoản Demo chuẩn**. |
| **03** | `03_seed_customers_contacts.sql` | Khách hàng Doanh nghiệp (Hierarchy mẹ-con, Cờ rủi ro Churn, Chăm sóc định kỳ > 30 ngày, Cặp trùng lặp gộp) & Người liên hệ (4 Buying Roles). |
| **04** | `04_seed_products_opportunities.sql` | Danh mục Sản phẩm B2B, Cơ hội bán hàng theo Pipeline, Báo giá CPQ chi tiết từng dòng mặt hàng, Hợp đồng kinh doanh. |
| **05** | `05_seed_leads_campaigns.sql` | Chiến dịch Marketing, Biểu mẫu Web Form, Leads (HOT/WARM/COLD, Đúng hạn/Quá hạn SLA, Cặp trùng lặp), Quy tắc tự động gán & chấm điểm Lead. |
| **06** | `06_seed_activities.sql` | Dòng thời gian tương tác (Timeline cuộc gọi/họp/email), Phiếu hỗ trợ kỹ thuật (Support Requests), Tài liệu đính kèm, Thông báo hệ thống & Lịch sử chuyển đổi Lead. |
| **07** | `07_verify_seed.sql` | Script kiểm tra và xuất báo cáo tổng hợp: Số lượng bản ghi, tính toàn vẹn khóa ngoại (Orphan Check), tình trạng kịch bản đặc thù. |
| **08** | `08_cleanup_seed.sql` | Script dọn dẹp dữ liệu mẫu an toàn: Chạy Transaction, bảo vệ DB Production, chỉ xóa đúng dải ID seed, không dùng TRUNCATE. |

---

## 2. Danh sách Tài khoản Demo

Tất cả tài khoản demo đều được mã hóa bằng thuật toán **BCrypt** an toàn.

> **Mật khẩu dùng chung cho mọi tài khoản:** `Password123@`

| ID | Username | Họ và Tên | Vai trò (Role) | Phòng ban (Team) | Phạm vi dữ liệu | Mục đích kiểm thử / Demo |
| :---: | :--- | :--- | :--- | :--- | :---: | :--- |
| **100** | `admin` | Quản trị Hệ thống | ADMIN | Ban Giám đốc | ALL | Quản trị phân quyền, cấu hình hệ thống, audit log |
| **101** | `sales_bac_1` | Nguyễn Văn Thắng | SALES_EXECUTIVE | Phòng Kinh doanh Bắc 1 | SELF | Xem và quản lý khách hàng/lead do mình sở hữu |
| **102** | `sales_bac_2` | Trần Thị Mai | SALES_EXECUTIVE | Phòng Kinh doanh Bắc 1 | SELF | Phối hợp bán hàng, kiểm tra phân cách quyền SELF |
| **103** | `lead_bac` | Hoàng Văn Minh | SALES_LEAD | Phòng Kinh doanh Bắc 1 | TEAM | Quản lý team Bắc, xem toàn bộ dữ liệu của Team 1 |
| **104** | `sales_nam_1` | Lê Quốc Hưng | SALES_EXECUTIVE | Phòng Kinh doanh Nam | SELF | Thử nghiệm phân tách khu vực MB vs MN |
| **105** | `cskh_lead` | Phạm Thúy Nga | CSKH_LEAD | Chăm sóc Khách hàng | ALL | Quản lý Ticket hỗ trợ kỹ thuật, xử lý cờ rủi ro Churn |
| **106** | `cskh_member` | Đỗ Hoàng Nam | CSKH_MEMBER | Chăm sóc Khách hàng | TEAM | Tiếp nhận và cập nhật trạng thái phiếu hỗ trợ |
| **107** | `mkt_lead` | Vũ Hoài Phương | MARKETING_LEAD | Phòng Marketing | ALL | Tạo chiến dịch, cấu hình form web, luật chấm điểm lead |
| **108** | `mkt_member` | Nguyễn Đức Anh | MARKETING_MEMBER | Phòng Marketing | TEAM | Giám sát danh sách khách hàng tiềm năng thu thập |
| **109** | `director` | Bùi Tiến Dũng | DIRECTOR | Ban Giám đốc | ALL | Xem Dashboard tổng thể, báo cáo doanh thu & tỷ lệ chuyển đổi |
| **110** | `accountant` | Ngô Lan Anh | ACCOUNTANT | Kế toán - Tài chính | ALL | Quản lý hợp đồng đã ký và duyệt báo giá |
| **111** | `sales_ops` | Đinh Trọng Nghĩa | SALES_OPS | Vận hành Kinh doanh | ALL | Phân bổ lead, cài đặt luật gán tự động, phân vùng |
| **112** | `viewer` | Đặng Thu Thảo | VIEWER | Phòng Kinh doanh Bắc 1 | SELF | Kiểm tra quyền chỉ xem (Read-only), không chỉnh sửa |

---

## 3. Các kịch bản Kiểm thử Nghiệp vụ tiêu biểu

1. **Phân quyền truy cập 3 tầng (Data Visibility):**
   - Đăng nhập `sales_bac_1`: Chỉ thấy Khách hàng 1, 2, 3, 4. Không thấy Khách hàng 6 (của `sales_bac_2`) và Khách hàng 7, 8 (của `sales_nam_1`).
   - Đăng nhập `lead_bac`: Thấy tất cả Khách hàng 1, 2, 3, 4, 5, 6 thuộc Phòng Kinh doanh Bắc 1. Không thấy Khách hàng 7, 8 thuộc Team Nam.
   - Đăng nhập `director`: Thấy toàn bộ khách hàng trên toàn quốc.

2. **Cờ cảnh báo Rủi ro Churn (S3-08):**
   - Khách hàng 3 (*Công ty CP Bán lẻ SmartMart*) có `support_risk_active = 1` do có 2 ticket chưa đóng (#1 URGENT, #2 HIGH).
   - Kiểm tra thông báo cảnh báo đã gửi tới tài khoản `sales_bac_1`.

3. **Cảnh báo Chăm sóc định kỳ (S3-09):**
   - Khách hàng 7 (*Cơ khí Miền Nam*) có hợp đồng 180 triệu VNĐ nhưng lần tương tác cuối cùng cách đây **45 ngày (> 30 ngày)**.
   - Kiểm tra hiển thị cảnh báo cần tương tác lại trong Dashboard của `sales_nam_1`.

4. **Phân cấp Công ty Mẹ - Con (S3-05):**
   - Khách hàng 2 (*Alpha Solutions*) là công ty con trực thuộc Khách hàng 1 (*Alpha Holdings*).

5. **Phát hiện & Gộp Khách hàng / Lead trùng lặp (S3-04 & S4-04):**
   - Cặp khách hàng trùng lặp: Khách 4 & Khách 5 có cùng website `https://achau-logistics.vn`.
   - Cặp lead trùng lặp: Lead 3 & Lead 4 có cùng email `hoang.vt@ancu-realestate.vn`.

6. **Giám sát SLA Lead (S4-07):**
   - Lead 1: Trạng thái `IN_SLA` (còn thời hạn phản hồi).
   - Lead 3: Trạng thái `OVERDUE` (quá hạn phản hồi).

7. **Quy trình Chuyển đổi Lead 3-trong-1 (S4-08):**
   - Lead 5: Đã chuyển đổi thành công (`CONVERTED`), sinh ra liên kết với Khách hàng 8, Người liên hệ 12 và Cơ hội 6 trong `lead_conversion_history`.

---

## 4. Hướng dẫn Thực thi

### Cách 1: Sử dụng PowerShell Runner (Khuyến nghị)

```powershell
# Bước 1: Thiết lập mật khẩu MySQL qua biến môi trường
$env:CRM_DB_PASSWORD = "your_mysql_password"

# Bước 2: Nạp toàn bộ seed data vào database mong muốn
./backend/database/seed/run-seed.ps1 -Database crm_sprint1_test_final_20261023

# (Tùy chọn) Xóa dữ liệu cũ trước khi nạp mới:
./backend/database/seed/run-seed.ps1 -Database crm_sprint1_test_final_20261023 -CleanFirst

# (Tùy chọn) Chỉ kiểm tra số lượng và tính toàn vẹn:
./backend/database/seed/run-seed.ps1 -Database crm_sprint1_test_final_20261023 -VerifyOnly
```

### Cách 2: Chạy trực tiếp qua MySQL CLI

```bash
# Nạp theo thứ tự
mysql -u root -p crm_sprint1_test_final_20261023 < backend/database/seed/01_seed_master_data.sql
mysql -u root -p crm_sprint1_test_final_20261023 < backend/database/seed/02_seed_users_roles.sql
mysql -u root -p crm_sprint1_test_final_20261023 < backend/database/seed/03_seed_customers_contacts.sql
mysql -u root -p crm_sprint1_test_final_20261023 < backend/database/seed/04_seed_products_opportunities.sql
mysql -u root -p crm_sprint1_test_final_20261023 < backend/database/seed/05_seed_leads_campaigns.sql
mysql -u root -p crm_sprint1_test_final_20261023 < backend/database/seed/06_seed_activities.sql

# Kiểm tra báo cáo
mysql -u root -p crm_sprint1_test_final_20261023 < backend/database/seed/07_verify_seed.sql
```

### Dọn dẹp dữ liệu mẫu

Khi cần đưa database về trạng thái trắng (chỉ giữ schema chuẩn):
```powershell
$env:CRM_DB_PASSWORD = "your_mysql_password"
mysql -u root -p crm_sprint1_test_final_20261023 < backend/database/seed/08_cleanup_seed.sql
```
Script `08_cleanup_seed.sql` tích hợp cơ chế tự động từ chối chạy nếu tên database chứa `prod` hoặc `production`.
