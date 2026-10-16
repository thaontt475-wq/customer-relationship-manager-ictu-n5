# BE S4-04 — Phát hiện và gộp Lead trùng (Lead Duplicate Detection and Merge)

- **Người thực hiện:** Nguyễn Đình Toàn (Senior Java Backend Engineer)
- **Nhánh:** `feature/S4-04-lead-duplicate-merge`
- **User Story:** Là nhân viên Marketing, tôi muốn được cảnh báo và gộp Lead trùng để tránh hai nhân viên cùng gọi một người.

---

## 1. Phân tích hiện trạng và Quyết định Thiết kế (Architecture & Contract Decisions)

### 1.1. Frontend Contract (`frontend/js/pages/lead-duplicates.js`)
- Frontend thực hiện gọi API:
  - `GET /api/leads/duplicates` để tải danh sách các cặp Lead nghi ngờ trùng.
  - `POST /api/leads/merge` để thực hiện gộp Lead side-by-side.
  - Frontend kiểm tra response bằng: `if (Array.isArray(data))` tại dòng 318 của `lead-duplicates.js`. Nếu bọc danh sách trong `{success: true, data: [...]}` mặc định của `ApiResponse`, Frontend sẽ coi là không hợp lệ và fallback về mock data.
  - **Quyết định Backend:**
    - `GET /api/leads/duplicates` mặc định trả về **Raw JSON Array** `List<LeadDuplicatePairDTO>` (HTTP 200) để hoàn toàn tương thích và khớp 100% với Frontend hiện tại mà không cần sửa bất kỳ file FE nào.
    - Hỗ trợ tham số `?envelope=true` nếu client muốn chuẩn hóa theo format `ApiResponse`.
    - Trả về mã HTTP 200 khi danh sách rỗng (`[]`).
- ID Format: Frontend có thể gửi ID số nguyên (`1`, `2`) hoặc ID định dạng chuỗi có tiền tố (`"LD-1"`, `"CUST-100"`). Backend xử lý tách tiền tố và parse an toàn về `Long`.
- Fallback LocalStorage: Frontend hiện có cơ chế fallback localStorage khi API lỗi. Đây là cơ chế của client và đã được Backend ghi nhận; Backend cung cấp đầy đủ API với response chuẩn xác để Frontend nhận diện dữ liệu thật từ database.

### 1.2. Bảo toàn lịch sử và Toàn vẹn dữ liệu (Data Integrity & AC3 Enforcement)
- **`retainHistory` là bắt buộc:** Yêu cầu AC3 nghiêm cấm `retainHistory=false` dẫn đến mất lịch sử. Nếu client gửi `retainHistory: false`, Service từ chối với ngoại lệ `400 Bad Request` ("Bảo toàn lịch sử là bắt buộc khi gộp Lead").
- **Audit Timestamp & Actor:** Client có thể gửi `mergedAt` trong request payload, nhưng Backend **không** dùng thời gian do client tự gửi. Toàn bộ `merged_at` được gán theo đồng hồ server (`Timestamp(System.currentTimeMillis())`) và `merged_by` lấy từ session người dùng đăng nhập (`SessionUser.getId()`).
- **Lưu trữ lịch sử:**
  - Lead phụ (`duplicateId`) được chuyển trạng thái `status = 'MERGED'`, gán `merged_into_id = masterId`, lưu `merged_at` và `merged_by`. **Tuyệt đối không xóa vật lý (No hard delete)**.
  - Ghi snapshot toàn bộ dữ liệu Lead chính trước gộp, Lead phụ trước gộp, và dữ liệu sau gộp vào bảng `lead_merge_history`.
  - Ghi nhật ký hệ thống vào bảng `audit_logs` (`action = 'MERGE'`, `entity_type = 'LEAD'`).
  - Toàn bộ ghi chú (`notes`) của cả hai Lead được tự động tổng hợp giữ lại đầy đủ theo cú pháp:
    `[Ghi chú Lead chính] | [Lịch sử gộp ngày dd/MM/yyyy HH:mm:ss từ Lead #<duplicateId> (<Tên> - <SĐT> - <Email>): <Ghi chú Lead phụ>]`.

### 1.3. Chống gộp đồng thời và Deadlock Prevention
- Sử dụng Database Transaction (`conn.setAutoCommit(false)`).
- Khóa bi-directional row bằng `SELECT ... FOR UPDATE` theo thứ tự ID tăng dần (`ORDER BY id ASC`). Việc khóa theo thứ tự ID nhất quán loại bỏ hoàn toàn nguy cơ deadlock khi hai user cùng lúc kích hoạt gộp hoán vị (`1 -> 2` và `2 -> 1`).
- Kiểm tra trạng thái đã gộp: Nếu Lead phụ hoặc Lead chính đã bị gộp trước đó (`merged_into_id IS NOT NULL` hoặc `status = 'MERGED'`), hệ thống từ chối ngay lập tức với `409 Conflict`.
- Kiểm tra gộp cùng một ID: Từ chối với `400 Bad Request`.

### 1.4. Phát hiện trùng và Chuẩn hóa (Normalization & Matching Logic - AC1 & AC2)
- **Số điện thoại:** Chuẩn hóa bỏ ký tự không phải số, quy đổi đầu số quốc tế `+84`, `84` thành `0`. So khớp chính xác 100% confidence.
- **Email:** Chuẩn hóa trim, lowercase. So khớp chính xác 100% confidence.
- **Tên công ty:**
  - Chuẩn hóa loại bỏ dấu tiếng Việt thông qua `Normalizer.normalize(..., Form.NFD)` và loại bỏ các ký tự dấu unicode.
  - Loại bỏ các hậu tố/tiền tố pháp lý phổ biến: `công ty`, `cổ phần`, `tnhh`, `trách nhiệm hữu hạn`, `doanh nghiệp`, `tập đoàn`, `corp`, `corporation`, `inc`, `ltd`, `jsc`.
  - Tính toán độ tương đồng từ vựng bằng Jaccard Similarity trên tập từ chuẩn hóa. Ngưỡng tương đồng >= 0.70 tính là nghi ngờ trùng.
- **Trùng với Customer (AC2):**
  - Quét đối chiếu tập Lead hợp lệ với danh sách Customer trong database.
  - Nếu trùng SĐT, Email hoặc Tên công ty với Customer, trả về cờ `isCustomerMatch = true`, đính kèm thông tin Customer và gợi ý hành động `LINK_CUSTOMER`.
  - Cung cấp API `POST /api/leads/attach-customer` (hoặc `/api/leads/link-customer`) để liên kết Lead vào Customer khi người dùng xác nhận.

---

## 2. Danh sách API Backend

### 2.1. GET `/api/leads/duplicates`
- **Mô tả:** Quét và trả về danh sách các cặp Lead nghi ngờ trùng lặp và Lead trùng với Customer trong phạm vi quyền hạn (Data Scope).
- **Quyền yêu cầu:** `lead.read` (hoặc `lead.merge`).
- **Scope hỗ trợ:** `ALL`, `TEAM`, `SELF`.
- **Response Format:** (HTTP 200 Raw JSON Array)
```json
[
  {
    "id": "pair-1-2",
    "masterLead": {
      "id": 1,
      "fullName": "Nguyễn Văn A",
      "companyName": "Công ty FPT",
      "email": "vana@example.com",
      "phone": "0912345678",
      "source": "EVENT",
      "notes": "Quan tâm gói Pro"
    },
    "duplicateLead": {
      "id": 2,
      "fullName": "Nguyễn Văn A (CN)",
      "companyName": "Công ty Cổ phần FPT",
      "email": "vana@example.com",
      "phone": "0912345678",
      "source": "WEBSITE",
      "notes": "Đăng ký từ landing page"
    },
    "similarityScore": 95,
    "reasons": [
      "Trùng email (vana@example.com)",
      "Trùng số điện thoại (0912345678)",
      "Tương đồng tên công ty"
    ],
    "matchedBy": "Hệ thống phát hiện trùng (Email, Số điện thoại)",
    "createdAt": "2026-10-15T19:00:00.000Z",
    "isCustomerMatch": false
  }
]
```

### 2.2. POST `/api/leads/merge`
- **Mô tả:** Gộp Lead phụ vào Lead chính side-by-side, cập nhật dữ liệu lựa chọn, bảo toàn lịch sử và khóa bản ghi.
- **Quyền yêu cầu:** `lead.merge`.
- **Request Body:**
```json
{
  "masterId": 1,
  "duplicateId": 2,
  "mergedData": {
    "id": 1,
    "fullName": "Nguyễn Văn A",
    "phone": "0912345678",
    "email": "vana@example.com",
    "companyName": "Công ty Cổ phần FPT",
    "title": "Trưởng phòng Mua sắm",
    "source": "EVENT",
    "notes": "Khách hàng VIP"
  },
  "retainHistory": true
}
```
- **Response Format:** (HTTP 200 ApiResponse)
```json
{
  "success": true,
  "message": "Gộp Lead thành công và đã bảo toàn toàn bộ lịch sử",
  "data": {
    "mergeHistoryId": 1,
    "masterId": 1,
    "duplicateId": 2,
    "mergedAt": "2026-10-15T19:30:00.000+07:00",
    "mergedBy": 5,
    "retainHistory": true
  }
}
```
- **HTTP Error Codes:**
  - `400 Bad Request`: `masterId` trùng `duplicateId`, thiếu ID, hoặc `retainHistory = false`.
  - `401 Unauthorized`: Chưa đăng nhập session.
  - `403 Forbidden`: Người dùng không có quyền `lead.merge` hoặc Lead nằm ngoài Data Scope (`SELF`/`TEAM`).
  - `404 Not Found`: Lead chính hoặc Lead phụ không tồn tại trong hệ thống.
  - `409 Conflict`: Lead phụ hoặc Lead chính đã được gộp từ trước (`already merged`).
  - `500 Internal Server Error`: Lỗi hệ thống/database (đã được rollback tự động).

### 2.3. POST `/api/leads/attach-customer` (hoặc `/api/leads/link-customer`)
- **Mô tả:** Liên kết Lead vào Khách hàng (Customer) đã tồn tại (AC2).
- **Quyền yêu cầu:** `lead.link_customer` (hoặc `lead.merge`).
- **Request Body:**
```json
{
  "leadId": 1,
  "customerId": 100,
  "notes": "Xác nhận Lead thuộc khách hàng FPT Telecom"
}
```
- **Response Format:** (HTTP 200 ApiResponse)
```json
{
  "success": true,
  "message": "Liên kết Lead với Khách hàng thành công",
  "data": {
    "linkId": 1,
    "leadId": 1,
    "customerId": 100
  }
}
```

---

## 3. Cơ sở dữ liệu và Migration

### 3.1. File Migration S4-04
- Đường dẫn: `backend/database/025_s4_04_lead_duplicate_merge.sql`.
- **Thứ tự thực thi:** Chạy sau migration `024_s4_02_lead_manual_excel.sql` (bảng `leads`).
- **Nội dung:**
  1. Thêm an toàn các cột phục vụ gộp và liên kết vào bảng `leads` thông qua procedure `EnsureS404LeadMergeColumns`:
     - `title VARCHAR(100)`: Chức danh liên hệ của Lead.
     - `merged_into_id BIGINT UNSIGNED`: ID của Lead chính đã gộp vào.
     - `merged_at DATETIME`: Thời điểm gộp.
     - `merged_by BIGINT UNSIGNED`: User ID thực hiện gộp.
     - `customer_id BIGINT UNSIGNED`: ID của Customer đã liên kết.
  2. Tạo bảng lưu lịch sử gộp `lead_merge_history`:
     - `id`, `master_lead_id`, `duplicate_lead_id`, `merged_by`, `merged_at`, `retain_history`, `master_snapshot_json`, `duplicate_snapshot_json`, `merged_result_json`, `notes`.
  3. Tạo bảng liên kết Lead - Customer `lead_customer_links`:
     - `id`, `lead_id`, `customer_id`, `linked_by`, `linked_at`, `notes`.
  4. Cấp quyền RBAC:
     - Thêm quyền `lead.read`, `lead.merge`, `lead.link_customer` vào `permissions`.
     - Phân quyền cho role `ADMIN`, `MANAGER`, `MARKETING`, `SALES` trong `role_permissions`.
     - Phân bổ Data Scope `ALL` cho `ADMIN`/`MANAGER`, `TEAM` cho `MARKETING`, `SELF` cho `SALES` trong `role_module_scopes`.

---

## 4. Danh sách các File Backend Triển khai

1. **Migration & DB:**
   - `backend/database/025_s4_04_lead_duplicate_merge.sql`
2. **DTO & Matching:**
   - `backend/src/main/java/com/crm/dto/leads/LeadMergeRequest.java`
   - `backend/src/main/java/com/crm/dto/leads/LeadAttachCustomerRequest.java`
   - `backend/src/main/java/com/crm/dto/leads/LeadDuplicatePairDTO.java`
   - `backend/src/main/java/com/crm/service/leads/LeadDuplicateMatcher.java`
3. **Data Access Layer (DAO):**
   - `backend/src/main/java/com/crm/dao/leads/LeadDuplicateMergeDAO.java`
4. **Service Layer:**
   - `backend/src/main/java/com/crm/service/leads/LeadDuplicateMergeService.java`
   - `backend/src/main/java/com/crm/service/permissions/DataScopeService.java` (Bổ sung module `"lead"`)
5. **Controller Layer & Utilities:**
   - `backend/src/main/java/com/crm/controller/leads/LeadDuplicateMergeServlet.java`
   - `backend/src/main/java/com/crm/util/ResponseUtil.java` (Bổ sung helper `rawJson`)
6. **Unit & Integration Tests:**
   - `backend/src/test/java/com/crm/service/leads/LeadDuplicateMatcherTest.java` (8 tests)
   - `backend/src/test/java/com/crm/controller/leads/LeadDuplicateMergeServletTest.java` (9 tests)
   - `backend/src/test/java/com/crm/service/leads/LeadDuplicateMergeJdbcTest.java` (17 tests)
7. **Documentation:**
   - `backend/docs/S4-04-lead-duplicate-merge.md`

---

## 5. Kết quả Kiểm thử (Test Suite Results)

- Toàn bộ test suite dự án đã chạy thành công:
  - **Tổng số test:** 161 tests.
  - **Failures:** 0.
  - **Errors:** 0.
  - **Skipped:** 0.
  - **BUILD SUCCESS.**
- 34 test cases mới dành riêng cho S4-04 bao phủ đầy đủ các kịch bản bắt buộc:
  1. Trùng email (chuẩn hóa trim, lowercase).
  2. Trùng số điện thoại (chuẩn hóa +84, 84, khoảng trắng).
  3. Trùng tên công ty (chuẩn hóa NFD bỏ dấu tiếng Việt, bỏ tiền tố/hậu tố pháp lý).
  4. Không trùng lặp.
  5. Gợi ý Customer phù hợp (AC2).
  6. Gộp thành công, cập nhật thông tin Lead chính.
  7. Lưu đầy đủ lịch sử sau gộp (`lead_merge_history` và `audit_logs`).
  8. Gộp cùng một ID -> Bị từ chối.
  9. Lead không tồn tại -> Trả về 404.
  10. Người dùng không có quyền -> Trả về 403.
  11. Người dùng không có quyền trên Lead của người khác (Data Scope: SELF) -> Bị chặn.
  12. Gộp lại Lead đã gộp -> Bị từ chối (409 Conflict).
  13. Rollback khi xảy ra lỗi trong transaction.
  14. Hai yêu cầu gộp đồng thời (chống race condition và deadlock với `ORDER BY id ASC FOR UPDATE`).
  15. SQL Injection & dữ liệu đầu vào không hợp lệ.
