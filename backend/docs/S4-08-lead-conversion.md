# BE S4-08 — Chuyển đổi Lead thành Khách hàng, Liên hệ & Cơ hội (3-in-1 Lead Conversion)

- **Người thực hiện:** Nguyễn Đình Toàn (Senior Java Backend Engineer)
- **Nhánh:** `feature/S4-08-lead-conversion`
- **User Story:** S4-08 – Chuyển Lead thành khách hàng và cơ hội (3 SP).
- **Mục tiêu:** Cho phép nhân viên kinh doanh và marketing chuyển đổi Lead tiềm năng (Qualified) thành Khách hàng doanh nghiệp (Account/Customer), Người liên hệ (Contact) và Cơ hội bán hàng (Deal/Opportunity) một cách nguyên tử (atomic), an toàn, có truy vết và tương thích hoàn toàn với giao diện Frontend hiện có (`frontend/lead-conversion.html` & `frontend/js/pages/lead-conversion.js`).

---

## 1. Phân tích hiện trạng và Quyết định Thiết kế (Architecture & Contract Decisions)

### 1.1. Frontend Contract (`frontend/js/pages/lead-conversion.js`)
- Frontend thực hiện các lời gọi API RESTful:
  - `GET /api/leads/{id}/check-convert-duplicates`: Kiểm tra phát hiện trùng lặp Mã số thuế hoặc Tên doanh nghiệp giữa Lead và các Khách hàng đã có trong hệ thống CRM.
  - `POST /api/leads/{id}/convert`: Kích hoạt thao tác chuyển đổi 3-trong-1 từ Lead sang Customer, Contact và Opportunity.
  - Hỗ trợ cả định dạng ID số (`1`, `2`) lẫn tiền tố chuỗi (`"LD-CONV-01"`, `"LEAD-2026-081"`, `"CUST-1001"`).
- **Quyết định Backend:**
  - Chuẩn hóa toàn bộ phản hồi theo cấu trúc `ApiResponse.success(message, data)` để hàm `window.CrmApi.request(...)` tự động unpack và trả về trực tiếp `result.data` cho client mà không phát sinh lỗi.
  - Hỗ trợ đầy đủ các trường mà hàm `openSuccessModal(result)` trên Frontend hiển thị:
    - `customerName`, `customerInfo`, `customerId`
    - `contactName`, `contactInfo`, `contactId`
    - `dealName`, `dealInfo`, `dealId`
    - `leadCode`, `convertedAt`

### 1.2. Tính Toàn vẹn Dữ liệu và Bảo toàn Lịch sử (Data Integrity & No-Loss Policy)
- **Tuyệt đối không xóa vật lý Lead (No hard delete):**
  - Sau khi chuyển đổi thành công, bản ghi Lead được cập nhật trạng thái `status = 'CONVERTED'`, gán cờ `is_locked = 1`, ghi nhận `converted_at`, `converted_by`, `converted_customer_id`, `converted_contact_id`, `converted_opportunity_id`.
  - Khóa chặt mọi yêu cầu chuyển đổi lại trên Lead đã chuyển đổi (`status = 'CONVERTED'` hoặc `is_locked = 1`), từ chối với mã `409 Conflict` ("Bản ghi Lead này đã được chuyển đổi trước đó và đang bị khóa.").
- **Lưu vết chi tiết vào bảng chuyên biệt `lead_conversion_history`:**
  - Lưu snapshot nguyên trạng của Lead trước khi chuyển đổi (`lead_snapshot JSON`).
  - Lưu payload yêu cầu chuyển đổi của client (`conversion_payload JSON`).
  - Lưu thông tin liên kết: `lead_id`, `customer_id`, `contact_id`, `opportunity_id`, `converted_by`, `customer_mode`, `converted_at`.
- **Ghi nhật ký hệ thống `audit_logs`:**
  - Ghi bản ghi audit `action = 'CONVERT'`, `entity_type = 'LEAD'`, `entity_id = leadId`.

### 1.3. Cơ chế Kiểm soát Giao tác và An toàn Đồng thời (Concurrency Safety & ACID)
- Sử dụng Database Transaction (`conn.setAutoCommit(false)` / `conn.commit()` / `conn.rollback()`).
- Sử dụng Row-level Lock `SELECT ... FROM leads WHERE id = ? FOR UPDATE` ngay khi bắt đầu tiến trình chuyển đổi.
- Khi hai tiến trình hoặc hai người dùng cùng thao tác chuyển đổi trên một Lead tại cùng một thời điểm:
  - Tiến trình đến trước sẽ chiếm row lock, hoàn tất gán `CONVERTED` / `is_locked = 1` và commit.
  - Tiến trình đến sau sau khi giải phóng lock sẽ đọc được trạng thái `CONVERTED` và ngay lập tức rollback, trả về `409 Conflict`.
  - Tuyệt đối không sinh ra bản ghi khách hàng hay cơ hội trùng lặp ngoài ý muốn.

### 1.4. Xử lý Linh hoạt Khách hàng (Customer Mode: CREATE_NEW vs MERGE_EXISTING)
- **`CREATE_NEW`:**
  - Tạo mới bản ghi khách hàng trong bảng `customers` (`status = 'TIEM_NANG'`).
  - Kiểm tra tránh trùng Mã số thuế với khách hàng đã có trên hệ thống (chặn duplicate key error).
  - Tự động map `industry` từ Lead sang `industry_id` trong `master_data`.
- **`MERGE_EXISTING`:**
  - Tận dụng khách hàng doanh nghiệp có sẵn đã được người dùng lựa chọn (`existingCustomerId`).
  - Kiểm tra sự tồn tại và quyền truy cập trên khách hàng đó.
  - Gắn Người liên hệ mới và Cơ hội mới vào Khách hàng có sẵn này mà không tạo bản ghi khách hàng thừa.

### 1.5. Xử lý Linh hoạt Cơ hội Bán hàng (Deal Creation Toggle)
- Cho phép người dùng bật/tắt tạo Cơ hội bán hàng ngay tại thời điểm chuyển đổi thông qua cờ `deal.createDeal`.
- Nếu `createDeal = true`: Kiểm tra bắt buộc tên cơ hội, giá trị hợp đồng (> 0đ), ngày dự kiến chốt hợp đồng (`YYYY-MM-DD`), quy trình bán hàng và giai đoạn ban đầu; tạo bản ghi `opportunities`.
- Nếu `createDeal = false`: Bỏ qua tạo cơ hội, trả về `dealId = null`, `dealInfo = "Không tạo cơ hội"`.

---

## 2. Danh sách API Backend S4-08

### 2.1. `GET /api/leads/{id}/check-convert-duplicates`
- **Mô tả:** Kiểm tra phát hiện trùng lặp Mã số thuế hoặc Tên công ty giữa Lead và Khách hàng trong database.
- **Quyền yêu cầu:** `lead.read` hoặc `lead.convert`.
- **Response Format:** (HTTP 200)
```json
{
  "success": true,
  "message": "Kiểm tra trùng lặp thành công",
  "data": {
    "hasDuplicate": true,
    "duplicateCustomer": {
      "id": 1001,
      "name": "Công ty Cổ phần Mediphar Quốc tế",
      "taxCode": "0107892341",
      "phone": "02438889999",
      "email": "contact@mediphar.vn",
      "address": "Tòa nhà Mediphar, Cầu Giấy, Hà Nội",
      "status": "CHINH_THUC"
    },
    "reason": "Trùng Mã số thuế (0107892341)"
  }
}
```

### 2.2. `POST /api/leads/{id}/convert`
- **Mô tả:** Thực hiện chuyển đổi nguyên tử Lead sang Customer, Contact và Opportunity.
- **Quyền yêu cầu:** `lead.convert` (kiểm tra DataScopeContext `SELF`, `TEAM`, `ALL` theo người phụ trách Lead).
- **Request Body:**
```json
{
  "customerMode": "CREATE_NEW",
  "existingCustomerId": null,
  "customer": {
    "name": "Công ty Cổ phần Công nghệ Cao Việt Tech",
    "taxCode": "0108923451",
    "industry": "CNTT & Viễn thông",
    "phone": "02437891234",
    "website": "https://viettech.com.vn",
    "address": "Tầng 8, Tòa nhà Keangnam, Hà Nội"
  },
  "contact": {
    "fullName": "Nguyễn Hoàng Nam",
    "title": "Tổng Giám đốc (CEO)",
    "buyingRole": "DECISION_MAKER",
    "phone": "0912345678",
    "email": "nam.nh@viettech.com.vn",
    "notes": "Quan tâm giải pháp CRM B2B"
  },
  "deal": {
    "createDeal": true,
    "name": "Việt Tech - Triển khai CRM 120 Users",
    "amount": 250000000,
    "closeDate": "2026-11-15",
    "pipeline": "B2B_STANDARD",
    "stageId": 2,
    "stageName": "Trình bày giải pháp / Demo",
    "probability": 40
  }
}
```
- **Response Format:** (HTTP 200)
```json
{
  "success": true,
  "message": "Chuyển đổi Lead thành công",
  "data": {
    "success": true,
    "customerId": 1004,
    "customerName": "Công ty Cổ phần Công nghệ Cao Việt Tech",
    "customerInfo": "MST: 0108923451 - CNTT & Viễn thông",
    "contactId": 5001,
    "contactName": "Nguyễn Hoàng Nam",
    "contactInfo": "Tổng Giám đốc (CEO) - 0912345678",
    "dealId": 8001,
    "dealName": "Việt Tech - Triển khai CRM 120 Users",
    "dealInfo": "250.000.000 ₫ - Dự kiến chốt: 2026-11-15",
    "leadCode": "LEAD-00001",
    "convertedAt": "2026-10-15T12:00:00Z"
  }
}
```

### 2.3. `GET /api/leads/{id}`
- **Mô tả:** Lấy thông tin chi tiết của Lead theo ID, bao gồm trạng thái chuyển đổi và liên kết đối tượng sau chuyển đổi.

### 2.4. `GET /api/leads`
- **Mô tả:** Lấy danh sách Lead trong phạm vi quyền hạn (hỗ trợ lọc theo `status`, `keyword`, `limit`, `offset`).

---

## 3. Cấu trúc Database & Migration (`026_s4_08_lead_conversion.sql`)

1. **Bảng `leads`:** Đảm bảo tồn tại và bổ sung các cột phục vụ chuyển đổi và thông tin hồ sơ:
   - `tax_code`, `address`, `website`, `company_phone`, `industry`, `score`, `tier`, `estimated_value`, `buying_role`.
   - `is_locked`: Cờ khóa bản ghi Lead sau khi chuyển đổi.
   - `converted_at`, `converted_by`: Thời gian và người thực hiện chuyển đổi.
   - `converted_customer_id`, `converted_contact_id`, `converted_opportunity_id`: Khóa ngoại liên kết đối tượng sinh ra.
2. **Bảng `lead_conversion_history`:**
   - Lưu trữ truy vết lịch sử gộp với `UNIQUE KEY (lead_id)` chống gộp lặp, `lead_snapshot JSON` và `conversion_payload JSON`.
3. **Phân quyền:**
   - Bổ sung quyền `lead.read`, `lead.convert`.
   - Gán quyền cho các vai trò: `ADMIN`, `DIRECTOR`, `TEAM_LEAD`, `SALES_REP`, `MARKETING`.
   - Cấu hình phân định phạm vi dữ liệu trong `role_module_scopes`.

---

## 4. Danh sách các File Thay đổi và Bổ sung

1. **Database Migration:**
   - `backend/database/026_s4_08_lead_conversion.sql`
2. **Data Transfer Objects (DTO):**
   - `backend/src/main/java/com/crm/dto/leads/LeadConversionRequest.java`
   - `backend/src/main/java/com/crm/dto/leads/LeadConversionResultDTO.java`
   - `backend/src/main/java/com/crm/dto/leads/LeadConvertDuplicateCheckDTO.java`
   - `backend/src/main/java/com/crm/dto/leads/LeadDTO.java`
3. **Data Access Object (DAO):**
   - `backend/src/main/java/com/crm/dao/leads/LeadConversionDAO.java`
4. **Service Layer:**
   - `backend/src/main/java/com/crm/service/leads/LeadConversionService.java`
   - `backend/src/main/java/com/crm/service/permissions/DataScopeService.java` (Bổ sung module `"lead"`)
5. **Controller Layer & Utility:**
   - `backend/src/main/java/com/crm/controller/leads/LeadConversionServlet.java`
   - `backend/src/main/java/com/crm/util/ResponseUtil.java` (Bổ sung helper `rawJson`)
6. **Unit & Integration Tests:**
   - `backend/src/test/java/com/crm/service/leads/LeadConversionServiceTest.java` (19 test cases)
   - `backend/src/test/java/com/crm/controller/leads/LeadConversionServletTest.java` (11 test cases)
   - `backend/src/test/java/com/crm/service/leads/LeadConversionJdbcTest.java` (7 test cases)
7. **Tài liệu:**
   - `backend/docs/S4-08-lead-conversion.md`

---

## 5. Kết quả Kiểm thử (Test Suite Results)

- Toàn bộ test suite dự án đã chạy thành công 100%:
  - **Tổng số test:** 164 tests.
  - **Failures:** 0.
  - **Errors:** 0.
  - **Skipped:** 0.
  - **BUILD SUCCESS.**
- 37 test cases mới dành riêng cho S4-08 bao phủ đầy đủ tất cả các kịch bản bắt buộc:
  1. Chuyển Lead hợp lệ tạo mới Customer, Contact, Opportunity thành công.
  2. Chuyển Lead liên kết Customer có sẵn (`MERGE_EXISTING`) không sinh trùng khách hàng.
  3. Chuyển đổi khi tắt tạo cơ hội (`createDeal = false`).
  4. Lead không tồn tại -> Báo lỗi 404.
  5. Người dùng không có quyền truy cập dữ liệu Lead (Data Scope: SELF) -> Báo lỗi 403.
  6. Người dùng không có quyền chuyển đổi -> Báo lỗi 403.
  7. Chuyển đổi Lead đã chuyển đổi trước đó -> Báo lỗi 409 Conflict.
  8. Chuyển đổi Lead đã bị khóa -> Báo lỗi 409 Conflict.
  9. Chuyển đổi Lead đã bị gộp (`MERGED`) -> Báo lỗi 409 Conflict.
  10. Dữ liệu khách hàng không hợp lệ (tên trống) -> Báo lỗi 400 Bad Request.
  11. Thiếu thông tin khách hàng gộp khi chọn `MERGE_EXISTING` -> Báo lỗi 400 Bad Request.
  12. Khách hàng gộp không tồn tại -> Báo lỗi 404 Not Found.
  13. Email người liên hệ không hợp lệ -> Báo lỗi 400 Bad Request.
  14. Giá trị cơ hội không hợp lệ (<= 0) -> Báo lỗi 400 Bad Request.
  15. Ngày chốt hợp đồng sai định dạng -> Báo lỗi 400 Bad Request.
  16. Trùng mã số thuế khi tạo khách hàng mới -> Báo lỗi 409 Conflict.
  17. Phát hiện trùng khách hàng theo Mã số thuế.
  18. Phát hiện trùng khách hàng theo Tên công ty.
  19. Không có trùng lặp.
  20. Kiểm tra an toàn giao tác đồng thời (Concurrency safety: đúng 1 thành công, 1 bị từ chối).
  21. Kiểm tra rollback giao tác khi lỗi cơ sở dữ liệu.
  22. Kiểm tra ghi nhận lịch sử chuyển đổi và nhật ký audit log.
  23. Kiểm tra các mã HTTP phản hồi từ Servlet (200, 400, 401, 403, 404, 405, 409, 500).
