<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,com.crm.service.scope.ScopeRecord,com.crm.util.Html" %>
<%!
private String esc(Object val) {
    if (val == null) return "";
    return Html.escape(String.valueOf(val));
}

private String cleanName(long id, String raw) {
    if (raw == null) return "";
    if (id == 1L || raw.contains("C?ng ty") || raw.contains("nh D??ng") || raw.contains("ng Ngh")) {
        return "Công ty TNHH Công Nghệ Ánh Dương";
    }
    if (id == 2L || raw.contains("T?p ?o?n") || raw.contains("Sao Vi?t") || raw.contains("B?n l")) {
        return "Tập đoàn Bán lẻ Sao Việt";
    }
    if (id == 3L || raw.contains("Ng?n h?ng") || raw.contains("Ph??ng Nam")) {
        return "Ngân hàng TMCP Phương Nam";
    }
    return raw;
}

private String cleanActSubject(long id, String raw) {
    if (raw == null) return "";
    if (id == 1L || raw.contains("G?i") || raw.contains("demo") || raw.contains("nh D??ng")) {
        return "Gọi điện tư vấn demo hệ thống cho Ánh Dương Tech";
    }
    if (id == 2L || raw.contains("H?p") || raw.contains("Sao Vi?t")) {
        return "Họp trực tiếp thống nhất yêu cầu với Sao Việt";
    }
    if (id == 3L || raw.contains("Thuy?t") || raw.contains("Ph??ng Nam")) {
        return "Thuyết trình giải pháp cấp cao cho Ban Giám Đốc Phương Nam";
    }
    return raw;
}

public static class CustItem {
    public long id;
    public String name;
    public String code;
    public String tagBadge;
    public String tagClass;
    public String taxId;
    public String industry;
    public String industryCode;
    public String ownerName;
    public String ownerInitials;
    public String ownerCode;
    public String openOpps;
    public String revenue;
    public String status;
    public String statusText;
    public String statusClass;
    public String phone;
    public String email;
    public String address;

    public CustItem(long id, String name, String code, String tagBadge, String tagClass,
                    String taxId, String industry, String industryCode, String ownerName,
                    String ownerInitials, String ownerCode, String openOpps, String revenue,
                    String status, String statusText, String statusClass, String phone,
                    String email, String address) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.tagBadge = tagBadge;
        this.tagClass = tagClass;
        this.taxId = taxId;
        this.industry = industry;
        this.industryCode = industryCode;
        this.ownerName = ownerName;
        this.ownerInitials = ownerInitials;
        this.ownerCode = ownerCode;
        this.openOpps = openOpps;
        this.revenue = revenue;
        this.status = status;
        this.statusText = statusText;
        this.statusClass = statusClass;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }
}

public static class ActItem {
    public long id;
    public String code;
    public String subject;
    public String type; // CALL, MEETING, EMAIL, NOTE
    public String typeName;
    public String typeBadgeClass;
    public String typeIcon;
    public String desc;
    public String customerName;
    public String customerCode;
    public String contactName;
    public String dealName;
    public String timeText;
    public String durationText;
    public String ownerName;
    public String ownerInitials;
    public String ownerCode;
    public String statusText;
    public String statusClass;

    public ActItem(long id, String code, String subject, String type, String typeName,
                   String typeBadgeClass, String typeIcon, String desc, String customerName,
                   String customerCode, String contactName, String dealName, String timeText,
                   String durationText, String ownerName, String ownerInitials, String ownerCode,
                   String statusText, String statusClass) {
        this.id = id;
        this.code = code;
        this.subject = subject;
        this.type = type;
        this.typeName = typeName;
        this.typeBadgeClass = typeBadgeClass;
        this.typeIcon = typeIcon;
        this.desc = desc;
        this.customerName = customerName;
        this.customerCode = customerCode;
        this.contactName = contactName;
        this.dealName = dealName;
        this.timeText = timeText;
        this.durationText = durationText;
        this.ownerName = ownerName;
        this.ownerInitials = ownerInitials;
        this.ownerCode = ownerCode;
        this.statusText = statusText;
        this.statusClass = statusClass;
    }
}

public static class QuoteRow {
    public long id;
    public String code;
    public String version;
    public String customerName;
    public String dealName;
    public String grandTotal;
    public String discountText;
    public double discountPercent;
    public boolean isOverDiscount;
    public String ownerName;
    public String ownerInitials;
    public String ownerCode;
    public String ownerRole;
    public String status;
    public String statusText;
    public String statusClass;
    public String validUntil;
    public String createdAt;
    public String paymentTerms;

    public QuoteRow(long id, String code, String version, String customerName, String dealName,
                    String grandTotal, String discountText, double discountPercent,
                    String ownerName, String ownerInitials, String ownerCode, String ownerRole,
                    String status, String statusText, String statusClass,
                    String validUntil, String createdAt, String paymentTerms) {
        this.id = id;
        this.code = code;
        this.version = version;
        this.customerName = customerName;
        this.dealName = dealName;
        this.grandTotal = grandTotal;
        this.discountText = discountText;
        this.discountPercent = discountPercent;
        this.isOverDiscount = discountPercent > 10.0;
        this.ownerName = ownerName;
        this.ownerInitials = ownerInitials;
        this.ownerCode = ownerCode;
        this.ownerRole = ownerRole;
        this.status = status;
        this.statusText = statusText;
        this.statusClass = statusClass;
        this.validUntil = validUntil;
        this.createdAt = createdAt;
        this.paymentTerms = paymentTerms;
    }
}

%>
<%
String forwardPath = (String) request.getAttribute("jakarta.servlet.forward.servlet_path");
String moduleTitle = (String) request.getAttribute("moduleTitle");
String servletPath = (forwardPath != null && !forwardPath.isBlank()) ? forwardPath : request.getServletPath();
boolean isCustomers = (forwardPath != null && forwardPath.startsWith("/customers"))
        || "Khách hàng".equalsIgnoreCase(moduleTitle)
        || (request.getRequestURI() != null && request.getRequestURI().contains("/customers"));
boolean isActivities = (forwardPath != null && forwardPath.startsWith("/activities"))
        || "Hoạt động".equalsIgnoreCase(moduleTitle)
        || (request.getRequestURI() != null && request.getRequestURI().contains("/activities"));
boolean isQuotes = (forwardPath != null && forwardPath.startsWith("/quotes"))
        || "Báo giá".equalsIgnoreCase(moduleTitle)
        || (request.getRequestURI() != null && request.getRequestURI().contains("/quotes"));

String contextPath = request.getContextPath();
String route = contextPath + (forwardPath != null ? forwardPath : (isQuotes ? "/quotes" : (isActivities ? "/activities" : "/customers")));
ScopeRecord detail = (ScopeRecord) request.getAttribute("record");

// Read request filters
String qParam = request.getParameter("q");
String qVal = (qParam != null) ? qParam.trim() : "";
String statusParam = request.getParameter("status");
String statusVal = (statusParam != null) ? statusParam.trim() : "";
String industryParam = request.getParameter("industry");
String industryVal = (industryParam != null) ? industryParam.trim() : "";
String ownerParam = request.getParameter("owner");
String ownerVal = (ownerParam != null) ? ownerParam.trim() : "";
String tabParam = request.getParameter("tab");
String currentTab = (tabParam != null && !tabParam.isBlank()) ? tabParam.trim() : "all";
String pageParam = request.getParameter("page");
int currentPage = 1;
try {
    if (pageParam != null && !pageParam.isBlank()) {
        currentPage = Math.max(1, Integer.parseInt(pageParam.trim()));
    }
} catch (NumberFormatException ignored) {}

// Data Scope resolution
String dataScope = "ALL";
Object scopeObj = session.getAttribute("dataScope");
if (scopeObj != null) {
    dataScope = String.valueOf(scopeObj);
}

// ----------------------------------------------------
// A. CUSTOMERS DATA PROCESSING
// ----------------------------------------------------
List<CustItem> masterList = new ArrayList<>();
if (isCustomers) {
    masterList.add(new CustItem(
        1L, "Công ty TNHH Công Nghệ Ánh Dương", "CUST-00101",
        "Tập đoàn mẹ", "cust-tag-purple", "0108923451",
        "Công nghệ phần mềm", "tech", "Phạm Kinh Doanh", "PKD", "salesrep",
        "3 cơ hội (450M)", "1.850.000.000 đ",
        "active", "Đang hoạt động", "cust-status-active",
        "024 3792 1188", "contact@anhduongtech.vn",
        "Tòa nhà Keangnam Landmark 72, Mễ Trì, Nam Từ Liêm, Hà Nội"
    ));
    masterList.add(new CustItem(
        2L, "Tập đoàn Bán lẻ Sao Việt", "CUST-00102",
        "Khách hàng VIP", "cust-tag-amber", "0314567890",
        "Bán lẻ / Phân phối", "retail", "Lê Trưởng Nhóm", "LTN", "teamlead",
        "1 cơ hội (120M)", "3.420.000.000 đ",
        "churn_warning", "Cảnh báo rời bỏ", "cust-status-warning",
        "028 3822 5566", "cskh@saovietretail.com.vn",
        "Số 68 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh"
    ));
    masterList.add(new CustItem(
        3L, "Ngân hàng TMCP Phương Nam", "CUST-00103",
        "Khách hàng chiến lược", "cust-tag-blue", "0102345678",
        "Tài chính / Ngân hàng", "finance", "Trần Giám Đốc", "TGD", "director",
        "5 cơ hội (2.1B)", "8.650.000.000 đ",
        "active", "Đang hoạt động", "cust-status-active",
        "024 3936 8899", "partnership@phuongnambank.vn",
        "Số 18 Lý Thường Kiệt, Hoàn Kiếm, Hà Nội"
    ));
    masterList.add(new CustItem(
        4L, "Tổng Công ty Viễn thông Toàn Cầu", "CUST-00104",
        "Đối tác cấp 1", "cust-tag-indigo", "0105678912",
        "Viễn thông & CNTT", "tech", "Nguyễn Quản Trị", "NQT", "admin",
        "2 cơ hội (890M)", "5.230.000.000 đ",
        "active", "Đang hoạt động", "cust-status-active",
        "024 3833 4455", "procurement@globaltelecom.com.vn",
        "Khu Công nghệ cao Hòa Lạc, Thạch Thất, Hà Nội"
    ));
    masterList.add(new CustItem(
        5L, "Bệnh viện Đa khoa Quốc tế An Sinh", "CUST-00105",
        "Trọng điểm", "cust-tag-teal", "0309876543",
        "Y tế & Dược phẩm", "healthcare", "Phạm Kinh Doanh", "PKD", "salesrep",
        "0 cơ hội", "950.000.000 đ",
        "suspended", "Tạm ngừng", "cust-status-suspended",
        "028 3997 9999", "admin@ansinhhospital.vn",
        "Số 10 Trần Huy Liệu, Phường 12, Quận Phú Nhuận, TP. Hồ Chí Minh"
    ));

    List<ScopeRecord> serverRecords = (List<ScopeRecord>) request.getAttribute("records");
    if (serverRecords != null && !serverRecords.isEmpty()) {
        for (ScopeRecord sr : serverRecords) {
            long rid = sr.id();
            boolean exists = false;
            for (CustItem ci : masterList) {
                if (ci.id == rid) {
                    exists = true;
                    ci.name = cleanName(rid, sr.label());
                    break;
                }
            }
            if (!exists) {
                masterList.add(new CustItem(
                    rid, cleanName(rid, sr.label()), "CUST-00" + (100 + rid),
                    "Tiêu chuẩn", "cust-tag-blue", "010" + (8000000 + rid),
                    "Dịch vụ doanh nghiệp", "tech", "Phạm Kinh Doanh", "PKD", "salesrep",
                    "1 cơ hội (150M)", "500.000.000 đ",
                    "active", "Đang hoạt động", "cust-status-active",
                    "024 3800 0000", "info@enterprise.vn", "Hà Nội, Việt Nam"
                ));
            }
        }
    }
}

List<CustItem> filteredList = new ArrayList<>();
if (isCustomers) {
    for (CustItem item : masterList) {
        boolean matchQ = true;
        if (!qVal.isEmpty()) {
            String qLower = qVal.toLowerCase(Locale.ROOT);
            matchQ = item.name.toLowerCase(Locale.ROOT).contains(qLower)
                  || item.code.toLowerCase(Locale.ROOT).contains(qLower)
                  || item.taxId.toLowerCase(Locale.ROOT).contains(qLower)
                  || item.ownerName.toLowerCase(Locale.ROOT).contains(qLower);
        }
        boolean matchStatus = true;
        if (!statusVal.isEmpty()) {
            matchStatus = item.status.equalsIgnoreCase(statusVal);
        }
        boolean matchIndustry = true;
        if (!industryVal.isEmpty()) {
            matchIndustry = item.industryCode.equalsIgnoreCase(industryVal);
        }
        boolean matchOwner = true;
        if (!ownerVal.isEmpty()) {
            matchOwner = item.ownerCode.equalsIgnoreCase(ownerVal);
        }
        boolean matchTab = true;
        if ("mine".equalsIgnoreCase(currentTab)) {
            matchTab = "salesrep".equalsIgnoreCase(item.ownerCode) || "Phạm Kinh Doanh".equals(item.ownerName);
        } else if ("vip".equalsIgnoreCase(currentTab)) {
            matchTab = item.tagBadge.contains("VIP") || item.tagBadge.contains("chiến lược") || item.tagBadge.contains("mẹ");
        } else if ("inactive".equalsIgnoreCase(currentTab)) {
            matchTab = "churn_warning".equalsIgnoreCase(item.status) || "suspended".equalsIgnoreCase(item.status);
        }

        if (matchQ && matchStatus && matchIndustry && matchOwner && matchTab) {
            filteredList.add(item);
        }
    }
}

CustItem detailItem = null;
if (isCustomers && detail != null) {
    long did = detail.id();
    for (CustItem ci : masterList) {
        if (ci.id == did) {
            detailItem = ci;
            break;
        }
    }
    if (detailItem == null) {
        detailItem = new CustItem(
            did, cleanName(did, detail.label()), "CUST-00" + (100 + did),
            "Doanh nghiệp", "cust-tag-blue", "0108923451",
            "Dịch vụ & Công nghệ", "tech", "Người phụ trách #" + detail.ownerUserId(), "QL", "salesrep",
            "1 cơ hội", "1.200.000.000 đ",
            "active", "Đang hoạt động", "cust-status-active",
            "024 3800 0000", "info@enterprise.vn", "Hà Nội, Việt Nam"
        );
    }
}

// ----------------------------------------------------
// B. ACTIVITIES DATA PROCESSING
// ----------------------------------------------------
String actTypeParam = request.getParameter("type");
if (actTypeParam == null) actTypeParam = "all";
String actOwnerParam = request.getParameter("owner");
if (actOwnerParam == null) actOwnerParam = "all";
String actPeriodParam = request.getParameter("period");
if (actPeriodParam == null) actPeriodParam = "all";

List<ActItem> actMasterList = new ArrayList<>();
if (isActivities) {
    // 3 Hoạt động chuẩn khớp 3 bản ghi hệ thống database
    actMasterList.add(new ActItem(
        1L, "ACT-00412", "Gọi điện tư vấn demo hệ thống cho Ánh Dương Tech",
        "CALL", "Cuộc gọi", "act-type-call", "📞",
        "Trao đổi qua điện thoại với anh Tuấn về quy mô người dùng và yêu cầu tích hợp ERP...",
        "Công ty TNHH Công Nghệ Ánh Dương", "CUST-00101",
        "Anh Tuấn (Giám đốc CNTT / CTO)", "Triển khai CRM Enterprise 50 Users (450M)",
        "03/10/2026 14:30", "30 phút",
        "Nông Quang Tiệp", "N", "admin",
        "🟢 Thành công - Đồng ý demo", "act-status-success"
    ));
    actMasterList.add(new ActItem(
        2L, "ACT-00413", "Họp trực tiếp thống nhất yêu cầu với Sao Việt",
        "MEETING", "Cuộc gặp", "act-type-meeting", "🤝",
        "Gặp mặt trực tiếp tại trụ sở Sao Việt để rà soát quy trình quản lý chuỗi cung ứng và chốt lịch đào tạo.",
        "Tập đoàn Bán lẻ Sao Việt", "CUST-00102",
        "Chị Mai (Trưởng phòng Mua hàng)", "Hệ thống Omni-channel Bán lẻ (1.2B)",
        "02/10/2026 09:00", "1 giờ 15 phút",
        "Lê Trưởng Nhóm", "L", "teamlead",
        "🔵 Đã gửi báo giá", "act-status-info"
    ));
    actMasterList.add(new ActItem(
        3L, "ACT-00414", "Thuyết trình giải pháp cấp cao cho Ban Giám Đốc Phương Nam",
        "MEETING", "Cuộc gặp", "act-type-meeting", "🤝",
        "Trình bày kiến trúc bảo mật đạt chuẩn Ngân hàng và lộ trình chuyển đổi dữ liệu giai đoạn 1.",
        "Ngân hàng TMCP Phương Nam", "CUST-00103",
        "Ông Trần Hải (Phó Tổng Giám Đốc)", "Gói CRM Tài chính & Bảo mật lõi (2.8B)",
        "30/09/2026 15:00", "2 giờ",
        "Trần Giám Đốc", "T", "director",
        "⚪ Đang xử lý", "act-status-pending"
    ));

    // Nếu chọn tab Email hoặc Note, bổ sung mẫu để trải nghiệm đầy đủ
    if ("EMAIL".equalsIgnoreCase(actTypeParam) || "email".equalsIgnoreCase(actTypeParam)) {
        actMasterList.add(new ActItem(
            4L, "ACT-00415", "Gửi email hợp đồng và bảng chiết khấu quý 4",
            "EMAIL", "Gửi Email", "act-type-email", "✉️",
            "Email đính kèm dự thảo hợp đồng cung cấp dịch vụ CRM và chính sách chiết khấu 15% cho khách hàng thân thiết.",
            "Tổng Công ty Viễn thông Toàn Cầu", "CUST-00104",
            "Anh Hoàng (Phòng Mua sắm)", "Nâng cấp gói Enterprise Viễn thông (890M)",
            "28/09/2026 11:15", "15 phút",
            "Nông Quang Tiệp", "N", "admin",
            "🟢 Thành công - Đã gửi", "act-status-success"
        ));
    }
    if ("NOTE".equalsIgnoreCase(actTypeParam) || "note".equalsIgnoreCase(actTypeParam)) {
        actMasterList.add(new ActItem(
            5L, "ACT-00416", "Ghi chú phản hồi của khách hàng sau buổi họp kỹ thuật",
            "NOTE", "Ghi chú", "act-type-note", "📝",
            "Khách hàng yêu cầu hỗ trợ thêm tính năng xuất báo cáo tự động sang định dạng PDF và phân quyền theo phòng ban.",
            "Bệnh viện Đa khoa Quốc tế An Sinh", "CUST-00105",
            "BS. Nguyễn Văn Hùng", "-",
            "26/09/2026 16:45", "10 phút",
            "Phạm Kinh Doanh", "P", "salesrep",
            "⚪ Đang xử lý", "act-status-pending"
        ));
    }

    List<ScopeRecord> serverRecords = (List<ScopeRecord>) request.getAttribute("records");
    if (serverRecords != null && !serverRecords.isEmpty()) {
        for (ScopeRecord sr : serverRecords) {
            long rid = sr.id();
            for (ActItem ai : actMasterList) {
                if (ai.id == rid) {
                    ai.subject = cleanActSubject(rid, sr.label());
                    break;
                }
            }
        }
    }
}

List<ActItem> filteredActList = new ArrayList<>();
if (isActivities) {
    for (ActItem item : actMasterList) {
        boolean matchQ = true;
        if (!qVal.isEmpty()) {
            String qLower = qVal.toLowerCase(Locale.ROOT);
            matchQ = item.subject.toLowerCase(Locale.ROOT).contains(qLower)
                  || item.desc.toLowerCase(Locale.ROOT).contains(qLower)
                  || item.customerName.toLowerCase(Locale.ROOT).contains(qLower)
                  || item.contactName.toLowerCase(Locale.ROOT).contains(qLower)
                  || item.code.toLowerCase(Locale.ROOT).contains(qLower)
                  || item.ownerName.toLowerCase(Locale.ROOT).contains(qLower);
        }

        boolean matchType = true;
        if (!"all".equalsIgnoreCase(actTypeParam) && !actTypeParam.isEmpty()) {
            matchType = item.type.equalsIgnoreCase(actTypeParam);
        }

        boolean matchOwner = true;
        if (!actOwnerParam.isEmpty() && !"all".equalsIgnoreCase(actOwnerParam)) {
            matchOwner = item.ownerCode.equalsIgnoreCase(actOwnerParam);
        }

        boolean matchPeriod = true;
        if ("week".equalsIgnoreCase(actPeriodParam)) {
            matchPeriod = (item.id <= 2);
        } else if ("month".equalsIgnoreCase(actPeriodParam)) {
            matchPeriod = (item.id <= 4);
        }

        if (matchQ && matchType && matchOwner && matchPeriod) {
            filteredActList.add(item);
        }
    }
}

ActItem detailAct = null;
if (isActivities && detail != null) {
    long did = detail.id();
    for (ActItem ai : actMasterList) {
        if (ai.id == did) {
            detailAct = ai;
            break;
        }
    }
    if (detailAct == null) {
        detailAct = new ActItem(
            did, "ACT-00" + (410 + did), cleanActSubject(did, detail.label()),
            "CALL", "Cuộc gọi", "act-type-call", "📞",
            "Nội dung trao đổi chi tiết về hoạt động bán hàng và phản hồi của khách hàng.",
            "Công ty TNHH Doanh Nghiệp Mẫu", "CUST-00" + (100 + did),
            "Người liên hệ", "Cơ hội liên quan",
            "03/10/2026 10:00", "30 phút",
            "Người phụ trách #" + detail.ownerUserId(), "U", "salesrep",
            "🟢 Thành công", "act-status-success"
        );
    }
}

// =========================================================================
// QUOTES & DISCOUNT APPROVALS MODULE DATA (Sprint 7: S7-01 / S7-02)
// =========================================================================
String quoteTabParam = request.getParameter("tab");
String currentQuoteTab = (quoteTabParam != null && !quoteTabParam.isBlank()) ? quoteTabParam.trim().toLowerCase(Locale.ROOT) : "all";
String quoteStatusParam = request.getParameter("status");
if (quoteStatusParam == null) quoteStatusParam = "all";
String quoteDiscountParam = request.getParameter("discount");
if (quoteDiscountParam == null) quoteDiscountParam = "all";
String quoteOwnerParam = request.getParameter("owner");
if (quoteOwnerParam == null) quoteOwnerParam = "all";
String quoteActionParam = request.getParameter("action");
if (quoteActionParam == null) quoteActionParam = "";

List<QuoteRow> quoteMasterList = new ArrayList<>();
if (isQuotes) {
    quoteMasterList.add(new QuoteRow(
        1L, "BG-2026-001", "v2.0",
        "Tập đoàn Viettel", "Nâng cấp hệ thống ERP",
        "49.250.000 đ", "12%", 12.0,
        "Nông Quang Tiệp", "N", "admin", "Quản trị viên / Solution Architect",
        "PENDING", "🟡 Chờ phê duyệt (Pending)", "status-pending",
        "15/10/2026", "01/10/2026", "Thanh toán 50% khi ký HĐ, 50% sau khi nghiệm thu"
    ));
    quoteMasterList.add(new QuoteRow(
        2L, "BG-2026-002", "v1.0",
        "Công ty TNHH Công Nghệ Ánh Dương", "Triển khai CRM Enterprise 50 Users",
        "450.000.000 đ", "8%", 8.0,
        "Lê Trưởng Nhóm", "L", "teamlead", "Trưởng nhóm Kinh doanh B2B",
        "APPROVED", "🟢 Đã duyệt (Approved)", "status-approved",
        "30/10/2026", "28/09/2026", "Thanh toán 3 đợt theo mốc tiến độ Milestone"
    ));
    quoteMasterList.add(new QuoteRow(
        3L, "BG-2026-003", "v1.5",
        "Ngân hàng TMCP Phương Nam", "Gói CRM Tài chính & Bảo mật lõi",
        "2.800.000.000 đ", "15%", 15.0,
        "Trần Giám Đốc", "T", "director", "Giám đốc Phát triển Dự án",
        "REJECTED", "🔴 Từ chối (Rejected)", "status-rejected",
        "20/10/2026", "25/09/2026", "Thanh toán chuyển khoản bảo lãnh qua ngân hàng"
    ));
    quoteMasterList.add(new QuoteRow(
        4L, "BG-2026-004", "v1.0",
        "Tập đoàn Bán lẻ Sao Việt", "Hệ thống Omni-channel Bán lẻ",
        "1.200.000.000 đ", "5%", 5.0,
        "Phạm Kinh Doanh", "P", "salesrep", "Chuyên viên Khách hàng Doanh nghiệp",
        "CONTRACTED", "📄 Đã ký hợp đồng", "status-contracted",
        "05/11/2026", "20/09/2026", "Đã xuất hóa đơn VAT điện tử đợt 1"
    ));
    quoteMasterList.add(new QuoteRow(
        5L, "BG-2026-005", "v1.0",
        "Bệnh viện Đa khoa Quốc tế An Sinh", "Hệ thống Quản lý Bệnh nhân & CSKH",
        "320.000.000 đ", "0%", 0.0,
        "Nông Quang Tiệp", "N", "admin", "Quản trị viên hệ thống",
        "DRAFT", "⚪ Bản nháp (Draft)", "status-draft",
        "25/11/2026", "03/10/2026", "Tạm ứng 30% khi ký kết hợp đồng"
    ));

    List<ScopeRecord> serverRecords = (List<ScopeRecord>) request.getAttribute("records");
    if (serverRecords != null && !serverRecords.isEmpty()) {
        for (ScopeRecord sr : serverRecords) {
            long rid = sr.id();
            for (QuoteRow qr : quoteMasterList) {
                if (qr.id == rid) {
                    if (sr.label() != null && !sr.label().isBlank()) {
                        qr.code = sr.label();
                    }
                    break;
                }
            }
        }
    }
}

List<QuoteRow> filteredQuoteList = new ArrayList<>();
if (isQuotes) {
    for (QuoteRow item : quoteMasterList) {
        boolean matchTab = true;
        if ("pending".equalsIgnoreCase(currentQuoteTab)) {
            matchTab = "PENDING".equalsIgnoreCase(item.status);
        } else if ("approved".equalsIgnoreCase(currentQuoteTab)) {
            matchTab = "APPROVED".equalsIgnoreCase(item.status);
        } else if ("rejected".equalsIgnoreCase(currentQuoteTab)) {
            matchTab = "REJECTED".equalsIgnoreCase(item.status);
        } else if ("contracted".equalsIgnoreCase(currentQuoteTab)) {
            matchTab = "CONTRACTED".equalsIgnoreCase(item.status);
        }

        boolean matchQ = true;
        if (!qVal.isEmpty()) {
            String qLower = qVal.toLowerCase(Locale.ROOT);
            matchQ = item.code.toLowerCase(Locale.ROOT).contains(qLower)
                  || item.customerName.toLowerCase(Locale.ROOT).contains(qLower)
                  || item.dealName.toLowerCase(Locale.ROOT).contains(qLower)
                  || item.ownerName.toLowerCase(Locale.ROOT).contains(qLower);
        }

        boolean matchStatus = true;
        if (!"all".equalsIgnoreCase(quoteStatusParam) && !quoteStatusParam.isEmpty()) {
            matchStatus = item.status.equalsIgnoreCase(quoteStatusParam);
        }

        boolean matchDiscount = true;
        if ("under10".equalsIgnoreCase(quoteDiscountParam)) {
            matchDiscount = !item.isOverDiscount;
        } else if ("over10".equalsIgnoreCase(quoteDiscountParam)) {
            matchDiscount = item.isOverDiscount;
        }

        boolean matchOwner = true;
        if (!"all".equalsIgnoreCase(quoteOwnerParam) && !quoteOwnerParam.isEmpty()) {
            matchOwner = item.ownerCode.equalsIgnoreCase(quoteOwnerParam);
        }

        if (matchTab && matchQ && matchStatus && matchDiscount && matchOwner) {
            filteredQuoteList.add(item);
        }
    }
}

int quotePageSize = 3;
int quoteFromIdx = (currentPage - 1) * quotePageSize;
List<QuoteRow> displayQuotes = new ArrayList<>();
if (isQuotes) {
    if (quoteFromIdx < filteredQuoteList.size()) {
        int toIdx = Math.min(filteredQuoteList.size(), quoteFromIdx + quotePageSize);
        displayQuotes = filteredQuoteList.subList(quoteFromIdx, toIdx);
    } else if (!filteredQuoteList.isEmpty()) {
        displayQuotes = filteredQuoteList;
    }
}

QuoteRow detailQuote = null;
if (isQuotes && (detail != null || "view".equals(quoteActionParam) || "approve".equals(quoteActionParam) || "convert".equals(quoteActionParam) || request.getParameter("id") != null)) {
    long did = (detail != null) ? detail.id() : 1L;
    try {
        if (request.getParameter("id") != null) {
            did = Long.parseLong(request.getParameter("id").trim());
        }
    } catch (NumberFormatException ignored) {}
    for (QuoteRow qr : quoteMasterList) {
        if (qr.id == did) {
            detailQuote = qr;
            break;
        }
    }
    if (detailQuote == null) {
        detailQuote = new QuoteRow(
            did, "BG-2026-00" + did, "v1.0",
            "Tập đoàn Viettel", "Nâng cấp hệ thống ERP",
            "49.250.000 đ", "12%", 12.0,
            "Nông Quang Tiệp", "N", "admin", "Quản trị viên / Solution Architect",
            "PENDING", "🟡 Chờ phê duyệt (Pending)", "status-pending",
            "15/10/2026", "01/10/2026", "Thanh toán 50% khi ký hợp đồng"
        );
    }
}

%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= isCustomers ? "Danh sách Khách hàng Doanh nghiệp & Bộ lọc Nâng cao (Customer List)" : (isActivities ? "Quản lý Hoạt động & Lịch sử Tương tác (Sales Activities)" : (isQuotes ? "Danh sách Báo giá & Phê duyệt Chiết khấu (Quotes & Approvals)" : esc(request.getAttribute("moduleTitle")))) %> | CRM ICTU</title>
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/header.css">
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/components.css">
    <% if (isCustomers) { %>
    <link rel="stylesheet" href="<%= contextPath %>/css/customers/customers.css">
    <% } else if (isActivities) { %>
    <link rel="stylesheet" href="<%= contextPath %>/css/activities/activities.css">
    <% } else if (isQuotes) { %>
    <link rel="stylesheet" href="<%= contextPath %>/css/quotes/quotes.css">
    <style>
        .act-page-wrapper { flex: 1; min-width: 0; width: 100%; padding: 24px 32px; background-color: #f6f8fb; min-height: calc(100vh - 64px); font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; color: #1e293b; box-sizing: border-box; }
        .act-container { max-width: 1440px; margin: 0 auto; width: 100%; box-sizing: border-box; }
        .act-breadcrumb { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; font-size: 0.8125rem; color: #64748b; margin-bottom: 16px; line-height: 1.4; }
        .act-breadcrumb a { color: #64748b; text-decoration: none; transition: color 150ms ease; }
        .act-breadcrumb a:hover { color: #2563eb; }
        .act-breadcrumb .sep { color: #cbd5e1; font-size: 0.75rem; }
        .act-breadcrumb .current { color: #1e293b; font-weight: 500; }
        .act-header { display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 16px; margin-bottom: 24px; }
        .act-header-left { flex: 1 1 500px; }
        .act-title { font-size: 1.5rem; font-weight: 700; line-height: 1.3; color: #0f172a; margin: 0 0 6px 0; letter-spacing: -0.02em; }
        .act-subtitle { font-size: 0.875rem; color: #64748b; margin: 0; line-height: 1.5; }
        .act-header-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
        .btn-primary, .act-btn-primary { display: inline-flex; align-items: center; justify-content: center; gap: 6px; background-color: #2563eb; color: #ffffff !important; font-size: 0.875rem; font-weight: 600; padding: 8px 16px; border-radius: 6px; border: 1px solid #2563eb; text-decoration: none; box-shadow: 0 1px 2px rgba(37, 99, 235, 0.2); transition: all 150ms ease; cursor: pointer; }
        .btn-primary:hover, .act-btn-primary:hover { background-color: #1d4ed8; border-color: #1d4ed8; box-shadow: 0 2px 4px rgba(37, 99, 235, 0.3); }
        .btn-outline, .act-btn-outline { display: inline-flex; align-items: center; justify-content: center; gap: 6px; background-color: #ffffff; color: #334155 !important; font-size: 0.875rem; font-weight: 600; padding: 8px 16px; border-radius: 6px; border: 1px solid #cbd5e1; text-decoration: none; box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04); transition: all 150ms ease; cursor: pointer; }
        .btn-outline:hover, .act-btn-outline:hover { background-color: #f8fafc; border-color: #94a3b8; color: #0f172a !important; }
        .act-tabs-bar { display: flex; align-items: center; gap: 8px; overflow-x: auto; margin-bottom: 20px; padding-bottom: 4px; }
        .act-tab-item { display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px; border-radius: 6px; font-size: 0.875rem; font-weight: 500; color: #475569; background-color: #ffffff; border: 1px solid #e2e8f0; text-decoration: none; transition: all 150ms ease; white-space: nowrap; cursor: pointer; }
        .act-tab-item:hover { background-color: #f8fafc; color: #1e293b; border-color: #cbd5e1; }
        .act-tab-item.active { background-color: #eff6ff; color: #1d4ed8; border-color: #2563eb; font-weight: 600; box-shadow: 0 1px 2px rgba(37, 99, 235, 0.08); }
        .act-filter-card { background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px 20px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04); }
        .act-filter-form { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
        .act-search-wrap { position: relative; flex: 1 1 280px; min-width: 240px; }
        .act-search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: #94a3b8; pointer-events: none; }
        .act-search-input { width: 100%; height: 40px; padding: 0 12px 0 38px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 0.875rem; color: #0f172a; background-color: #ffffff; box-sizing: border-box; transition: border-color 150ms ease, box-shadow 150ms ease; outline: none; }
        .act-search-input:focus { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15); }
        .act-select { height: 40px; padding: 0 32px 0 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 0.875rem; color: #1e293b; background-color: #ffffff; min-width: 160px; cursor: pointer; box-sizing: border-box; outline: none; }
        .act-select:focus { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15); }
        .act-filter-actions { display: flex; align-items: center; gap: 8px; }
        .act-table-card { background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05); overflow: hidden; margin-bottom: 24px; }
        .act-table-container { width: 100%; overflow-x: auto; }
        .act-table { width: 100%; border-collapse: collapse; font-size: 0.875rem; text-align: left; }
        .act-table thead { background-color: #f8fafc; border-bottom: 1px solid #e2e8f0; }
        .act-table th { padding: 12px 16px; font-size: 0.75rem; font-weight: 600; color: #475569; text-transform: uppercase; letter-spacing: 0.04em; border-bottom: 1px solid #e2e8f0; white-space: nowrap; }
        .act-table td { padding: 14px 16px; border-bottom: 1px solid #f1f5f9; vertical-align: middle; color: #1e293b; }
        .act-table tbody tr:hover { background-color: #f8fafc; }
        .act-col-type { display: flex; flex-direction: column; gap: 4px; }
        .act-type-badge { display: inline-flex; align-items: center; gap: 6px; padding: 3px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: 600; width: fit-content; }
        .act-type-call { background-color: #ecfdf5; color: #047857; border: 1px solid #a7f3d0; }
        .act-type-meeting { background-color: #fff7ed; color: #c2410c; border: 1px solid #fed7aa; }
        .act-type-email { background-color: #faf5ff; color: #7e22ce; border: 1px solid #e9d5ff; }
        .act-type-note { background-color: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
        .act-code-text { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 0.75rem; color: #64748b; }
        .act-col-title-wrap { max-width: 320px; }
        .act-title-link { font-weight: 600; color: #0f172a; text-decoration: none; display: block; line-height: 1.4; }
        .act-title-link:hover { color: #2563eb; text-decoration: underline; }
        .act-desc-text { font-size: 0.78125rem; color: #64748b; margin-top: 3px; display: block; line-height: 1.4; }
        .act-col-customer-wrap { max-width: 220px; }
        .act-customer-link { color: #2563eb; font-weight: 600; text-decoration: none; display: block; line-height: 1.4; font-size: 0.84375rem; }
        .act-customer-link:hover { text-decoration: underline; }
        .act-contact-name { font-size: 0.75rem; color: #64748b; margin-top: 2px; display: block; }
        .act-deal-text { font-size: 0.8125rem; color: #334155; font-weight: 500; }
        .act-col-time-wrap { display: flex; flex-direction: column; gap: 3px; white-space: nowrap; }
        .act-time-val { font-size: 0.8125rem; color: #0f172a; font-weight: 500; }
        .act-duration-tag { font-size: 0.6875rem; color: #64748b; background-color: #f1f5f9; border: 1px solid #e2e8f0; padding: 1px 6px; border-radius: 4px; width: fit-content; }
        .act-user-wrap { display: flex; align-items: center; gap: 8px; white-space: nowrap; }
        .act-avatar { width: 28px; height: 28px; border-radius: 50%; background-color: #eff6ff; color: #2563eb; font-size: 0.75rem; font-weight: 700; display: flex; align-items: center; justify-content: center; border: 1px solid #bfdbfe; flex-shrink: 0; }
        .act-user-name { font-size: 0.8125rem; font-weight: 500; color: #1e293b; }
        .act-status-badge { display: inline-flex; align-items: center; gap: 6px; padding: 3px 8px; border-radius: 12px; font-size: 0.75rem; font-weight: 600; white-space: nowrap; }
        .act-status-success { background-color: #ecfdf5; color: #047857; border: 1px solid #a7f3d0; }
        .act-status-info { background-color: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
        .act-status-pending { background-color: #f8fafc; color: #475569; border: 1px solid #cbd5e1; }
        .btn-text, .act-btn-action { color: #2563eb; font-weight: 600; font-size: 0.8125rem; text-decoration: none; padding: 4px 8px; border-radius: 4px; white-space: nowrap; }
        .btn-text:hover, .act-btn-action:hover { background-color: #eff6ff; text-decoration: underline; }
        .act-pagination { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; padding: 16px 20px; border-top: 1px solid #e2e8f0; background-color: #ffffff; }
        .act-pagination-info { font-size: 0.8125rem; color: #64748b; }
        .act-scope-tag { display: inline-block; padding: 2px 6px; background-color: #f1f5f9; color: #334155; border-radius: 4px; font-weight: 600; font-size: 0.75rem; border: 1px solid #e2e8f0; margin-left: 4px; }
        .act-pagination-controls { display: flex; align-items: center; gap: 6px; }
        .act-page-btn { display: inline-flex; align-items: center; justify-content: center; min-width: 32px; height: 32px; padding: 0 8px; border-radius: 6px; font-size: 0.8125rem; font-weight: 500; color: #334155; background-color: #ffffff; border: 1px solid #cbd5e1; text-decoration: none; }
        .act-page-btn:hover { background-color: #f8fafc; border-color: #94a3b8; }
        .act-page-btn.active { background-color: #2563eb; border-color: #2563eb; color: #ffffff; font-weight: 600; }
        .act-detail-card { background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 24px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05); margin-bottom: 24px; }
        .act-detail-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding-bottom: 20px; border-bottom: 1px solid #e2e8f0; margin-bottom: 24px; }
        .act-detail-title { font-size: 1.25rem; font-weight: 700; color: #0f172a; margin: 0 0 8px 0; }
        .act-detail-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; font-size: 0.8125rem; color: #64748b; }
        .act-detail-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 24px; }
        .act-detail-box { background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 16px 20px; }
        .act-detail-box-title { font-size: 0.875rem; font-weight: 700; color: #1e293b; margin: 0 0 14px 0; display: flex; align-items: center; gap: 8px; }
        .act-detail-row { display: flex; align-items: flex-start; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f1f5f9; font-size: 0.84375rem; }
        .act-detail-row:last-child { border-bottom: none; }
        .act-detail-label { color: #64748b; font-weight: 500; }
        .act-detail-value { color: #0f172a; font-weight: 600; text-align: right; }
        .act-content-box { margin-top: 20px; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 6px; padding: 16px; font-size: 0.875rem; line-height: 1.6; color: #334155; }
    </style>
    <% } %>
</head>
<body class="crm-body">
<jsp:include page="/jsp/shared/header.jsp"/>

<div class="crm-main-layout">
<jsp:include page="/jsp/shared/sidebar.jsp"/>

<% if (isCustomers) { %>
<main class="crm-page crm-customer-page">
<div class="crm-customer-container">

    <!-- 1. BREADCRUMB -->
    <nav class="crm-cust-breadcrumb" aria-label="Đường dẫn">
        <a href="<%= contextPath %>/dashboard">Trang chủ</a>
        <span class="separator">/</span>
        <a href="<%= route %>">Quản lý Khách hàng</a>
        <span class="separator">/</span>
        <span class="current"><%= (detail != null) ? "Hồ sơ 360° Doanh nghiệp" : "Danh sách & Bộ lọc Doanh nghiệp" %></span>
    </nav>

    <% if (detail != null) { %>
        <!-- 360° CUSTOMER PROFILE VIEW (KHI BẤM XEM CHI TIẾT) -->
        <article class="cust-detail-card">
            <header class="cust-detail-top">
                <div>
                    <h1 class="cust-detail-name"><%= esc(detailItem.name) %></h1>
                    <div class="cust-detail-meta">
                        <span><strong>Mã khách hàng:</strong> <%= esc(detailItem.code) %></span>
                        <span>•</span>
                        <span class="cust-tag-badge <%= esc(detailItem.tagClass) %>"><%= esc(detailItem.tagBadge) %></span>
                        <span>•</span>
                        <span class="cust-status-badge <%= esc(detailItem.statusClass) %>"><span class="dot"></span><%= esc(detailItem.statusText) %></span>
                    </div>
                </div>
                <div>
                    <a href="<%= route %>" class="cust-back-link">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
                        Quay lại danh sách
                    </a>
                </div>
            </header>

            <div class="cust-detail-grid">
                <section class="cust-detail-box">
                    <h2 class="cust-detail-box-title">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path></svg>
                        Thông tin Pháp lý &amp; Thuế
                    </h2>
                    <div class="cust-detail-item">
                        <span class="cust-detail-label">Mã số thuế (MST):</span>
                        <span class="cust-detail-val"><%= esc(detailItem.taxId) %></span>
                    </div>
                    <div class="cust-detail-item">
                        <span class="cust-detail-label">Lĩnh vực / Ngành nghề:</span>
                        <span class="cust-detail-val"><%= esc(detailItem.industry) %></span>
                    </div>
                    <div class="cust-detail-item">
                        <span class="cust-detail-label">Địa chỉ trụ sở:</span>
                        <span class="cust-detail-val"><%= esc(detailItem.address) %></span>
                    </div>
                </section>

                <section class="cust-detail-box">
                    <h2 class="cust-detail-box-title">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path></svg>
                        Đầu mối Liên hệ &amp; Giao dịch
                    </h2>
                    <div class="cust-detail-item">
                        <span class="cust-detail-label">Số điện thoại tổng đài:</span>
                        <span class="cust-detail-val"><%= esc(detailItem.phone) %></span>
                    </div>
                    <div class="cust-detail-item">
                        <span class="cust-detail-label">Email chính thức:</span>
                        <span class="cust-detail-val"><%= esc(detailItem.email) %></span>
                    </div>
                    <div class="cust-detail-item">
                        <span class="cust-detail-label">Người phụ trách (Owner):</span>
                        <span class="cust-detail-val"><%= esc(detailItem.ownerName) %> (<%= esc(detailItem.ownerCode) %>)</span>
                    </div>
                </section>

                <section class="cust-detail-box">
                    <h2 class="cust-detail-box-title">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="12" y1="1" x2="12" y2="23"></line><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path></svg>
                        Chỉ số Kinh doanh &amp; Cơ hội
                    </h2>
                    <div class="cust-detail-item">
                        <span class="cust-detail-label">Tổng doanh thu lũy kế:</span>
                        <span class="cust-detail-val" style="color:#047857; font-weight:700;"><%= esc(detailItem.revenue) %></span>
                    </div>
                    <div class="cust-detail-item">
                        <span class="cust-detail-label">Cơ hội đang mở (Pipeline):</span>
                        <span class="cust-detail-val"><%= esc(detailItem.openOpps) %></span>
                    </div>
                    <div class="cust-detail-item">
                        <span class="cust-detail-label">Phân quyền dữ liệu (Scope):</span>
                        <span class="cust-detail-val"><%= esc(dataScope) %></span>
                    </div>
                </section>
            </div>
        </article>

    <% } else { %>

        <!-- HEADER & ACTIONS DOANH NGHIỆP -->
        <header class="crm-cust-header">
            <div class="crm-cust-header-left">
                <h1 class="crm-cust-title">Danh sách Khách hàng Doanh nghiệp &amp; Bộ lọc Nâng cao (Customer List)</h1>
                <p class="crm-cust-subtitle">Sprint 3 • S3-01 &amp; S3-07 • Quản lý hồ sơ 360° khách hàng B2B, kiểm soát phân quyền dữ liệu (Data Scope: <%= esc(dataScope) %>)</p>
            </div>
            <div class="crm-cust-header-actions">
                <a href="<%= route %>?action=create" class="crm-btn-primary-action">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                    + Thêm khách hàng
                </a>
                <a href="<%= contextPath %>/api/customers/export" class="crm-btn-secondary-action">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
                    Xuất Excel
                </a>
                <a href="<%= route %>?import=1" class="crm-btn-secondary-action">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
                    Nhập Excel
                </a>
            </div>
        </header>

        <!-- THANH TAB LỌC NHANH TRẠNG THÁI -->
        <nav class="crm-cust-tabs-bar" aria-label="Lọc nhanh trạng thái">
            <a href="<%= route %>?tab=all&amp;q=<%= esc(qVal) %>" class="crm-cust-tab <%= "all".equalsIgnoreCase(currentTab) ? "active" : "" %>">
                <span>Tất cả khách hàng</span>
                <span class="tab-count"><%= masterList.size() %></span>
            </a>
            <a href="<%= route %>?tab=mine&amp;q=<%= esc(qVal) %>" class="crm-cust-tab <%= "mine".equalsIgnoreCase(currentTab) ? "active" : "" %>">
                <span>Khách hàng của tôi</span>
                <span class="tab-count">2</span>
            </a>
            <a href="<%= route %>?tab=vip&amp;q=<%= esc(qVal) %>" class="crm-cust-tab <%= "vip".equalsIgnoreCase(currentTab) ? "active" : "" %>">
                <span>Khách hàng VIP / Chiến lược</span>
                <span class="tab-count">3</span>
            </a>
            <a href="<%= route %>?tab=inactive&amp;q=<%= esc(qVal) %>" class="crm-cust-tab <%= "inactive".equalsIgnoreCase(currentTab) ? "active" : "" %>">
                <span>Cảnh báo rời bỏ / Tạm ngừng</span>
                <span class="tab-count">2</span>
            </a>
        </nav>

        <!-- KHỐI BỘ LỌC ĐA TIÊU CHÍ (METHOD GET) -->
        <section class="crm-filter-panel" aria-label="Bộ lọc tìm kiếm đa tiêu chí">
            <form method="GET" action="<%= route %>" class="crm-filter-form">
                <input type="hidden" name="tab" value="<%= esc(currentTab) %>">
                <div class="crm-filter-search-wrap">
                    <svg class="crm-search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                    <input type="text" name="q" class="crm-filter-input-search" placeholder="Tìm theo tên công ty, mã khách hàng, MST..." value="<%= esc(qVal) %>">
                </div>
                <select name="industry" class="crm-filter-select" aria-label="Lĩnh vực kinh doanh">
                    <option value="" <%= industryVal.isEmpty() ? "selected" : "" %>>Tất cả lĩnh vực</option>
                    <option value="tech" <%= "tech".equalsIgnoreCase(industryVal) ? "selected" : "" %>>Công nghệ &amp; Phần mềm</option>
                    <option value="retail" <%= "retail".equalsIgnoreCase(industryVal) ? "selected" : "" %>>Bán lẻ / Phân phối</option>
                    <option value="finance" <%= "finance".equalsIgnoreCase(industryVal) ? "selected" : "" %>>Tài chính / Ngân hàng</option>
                    <option value="healthcare" <%= "healthcare".equalsIgnoreCase(industryVal) ? "selected" : "" %>>Y tế &amp; Dược phẩm</option>
                </select>
                <select name="status" class="crm-filter-select" aria-label="Trạng thái">
                    <option value="" <%= statusVal.isEmpty() ? "selected" : "" %>>Tất cả trạng thái</option>
                    <option value="active" <%= "active".equalsIgnoreCase(statusVal) ? "selected" : "" %>>Đang hoạt động</option>
                    <option value="churn_warning" <%= "churn_warning".equalsIgnoreCase(statusVal) ? "selected" : "" %>>Cảnh báo rời bỏ</option>
                    <option value="suspended" <%= "suspended".equalsIgnoreCase(statusVal) ? "selected" : "" %>>Tạm ngừng</option>
                </select>
                <select name="owner" class="crm-filter-select" aria-label="Người phụ trách">
                    <option value="" <%= ownerVal.isEmpty() ? "selected" : "" %>>Tất cả người phụ trách</option>
                    <option value="salesrep" <%= "salesrep".equalsIgnoreCase(ownerVal) ? "selected" : "" %>>Phạm Kinh Doanh (Sales Rep)</option>
                    <option value="teamlead" <%= "teamlead".equalsIgnoreCase(ownerVal) ? "selected" : "" %>>Lê Trưởng Nhóm (Team Lead)</option>
                    <option value="director" <%= "director".equalsIgnoreCase(ownerVal) ? "selected" : "" %>>Trần Giám Đốc (Director)</option>
                    <option value="admin" <%= "admin".equalsIgnoreCase(ownerVal) ? "selected" : "" %>>Nguyễn Quản Trị (Admin)</option>
                </select>
                <div class="crm-filter-actions-group">
                    <button type="submit" class="crm-btn-apply">Lọc dữ liệu</button>
                    <a href="<%= route %>" class="crm-btn-reset">Đặt lại</a>
                </div>
            </form>
        </section>

        <!-- BẢNG DỮ LIỆU ENTERPRISE -->
        <section class="crm-table-card" aria-label="Bảng danh sách khách hàng doanh nghiệp">
            <div class="crm-table-container">
                <table class="crm-enterprise-table">
                    <thead>
                        <tr>
                            <th class="col-checkbox"><input type="checkbox" aria-label="Chọn tất cả"></th>
                            <th class="col-code">MÃ KH</th>
                            <th class="col-name">DOANH NGHIỆP / TỔ CHỨC</th>
                            <th class="col-tax">MÃ SỐ THUẾ</th>
                            <th class="col-industry">LĨNH VỰC</th>
                            <th class="col-owner">NGƯỜI PHỤ TRÁCH</th>
                            <th class="col-opps">PIPELINE</th>
                            <th class="col-revenue">DOANH THU</th>
                            <th class="col-status">TRẠNG THÁI</th>
                            <th class="col-actions">THAO TÁC</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (filteredList.isEmpty()) { %>
                        <tr>
                            <td colspan="10" class="text-center" style="padding: 40px;">
                                <div style="color: #64748b; font-size: 0.875rem;">Không tìm thấy khách hàng nào phù hợp với điều kiện lọc.</div>
                            </td>
                        </tr>
                        <% } else {
                            for (CustItem item : filteredList) {
                        %>
                        <tr>
                            <td class="col-checkbox"><input type="checkbox" name="selected_cust" value="<%= item.id %>" aria-label="Chọn khách hàng <%= esc(item.name) %>"></td>
                            <td class="col-code"><span class="cust-code"><%= esc(item.code) %></span></td>
                            <td class="col-name">
                                <a href="<%= route %>?id=<%= item.id %>" class="cust-name-link" title="Xem hồ sơ 360° <%= esc(item.name) %>"><%= esc(item.name) %></a>
                                <span class="cust-tag-badge <%= esc(item.tagClass) %>"><%= esc(item.tagBadge) %></span>
                            </td>
                            <td class="col-tax"><span class="cust-tax"><%= esc(item.taxId) %></span></td>
                            <td class="col-industry"><span class="cust-industry"><%= esc(item.industry) %></span></td>
                            <td class="col-owner">
                                <div class="cust-owner-wrap">
                                    <span class="cust-avatar-circle"><%= esc(item.ownerInitials) %></span>
                                    <span class="cust-owner-name"><%= esc(item.ownerName) %></span>
                                </div>
                            </td>
                            <td class="col-opps"><span class="cust-opps-text"><%= esc(item.openOpps) %></span></td>
                            <td class="col-revenue"><span class="cust-revenue"><%= esc(item.revenue) %></span></td>
                            <td class="col-status"><span class="cust-status-badge <%= esc(item.statusClass) %>"><span class="dot"></span><%= esc(item.statusText) %></span></td>
                            <td class="col-actions"><a href="<%= route %>?id=<%= item.id %>" class="cust-btn-360">Hồ sơ 360°</a></td>
                        </tr>
                        <% }} %>
                    </tbody>
                </table>
            </div>

            <!-- THANH PHÂN TRANG DOANH NGHIỆP -->
            <footer class="crm-pagination-bar">
                <div class="crm-pagination-info">
                    Hiển thị 1 - <%= filteredList.size() %> trong tổng số <%= filteredList.size() %> khách hàng ghi nhận • Phân quyền Data Scope: <strong><%= esc(dataScope) %></strong>
                </div>
                <nav class="crm-pagination-controls" aria-label="Phân trang danh sách khách hàng">
                    <a href="<%= route %>?page=1&amp;tab=<%= esc(currentTab) %>&amp;q=<%= esc(qVal) %>" class="crm-page-btn nav-text <%= currentPage <= 1 ? "disabled" : "" %>">&larr; Trước</a>
                    <a href="<%= route %>?page=1&amp;tab=<%= esc(currentTab) %>&amp;q=<%= esc(qVal) %>" class="crm-page-btn <%= currentPage == 1 ? "active" : "" %>">1</a>
                    <a href="<%= route %>?page=2&amp;tab=<%= esc(currentTab) %>&amp;q=<%= esc(qVal) %>" class="crm-page-btn <%= currentPage == 2 ? "active" : "" %>">2</a>
                    <a href="<%= route %>?page=2&amp;tab=<%= esc(currentTab) %>&amp;q=<%= esc(qVal) %>" class="crm-page-btn nav-text">Sau &rarr;</a>
                </nav>
            </footer>
        </section>

    <% } %>

</div>
</main>

<% } else if (isActivities) { %>

<!-- =======================================================================
     ACTIVITIES MODULE - ENTERPRISE B2B UI (Sprint 6: S6-03 & S6-04 / S6-08)
     ======================================================================= -->
<main class="act-page-wrapper" role="main">
<div class="act-container">

    <!-- 1. BREADCRUMB -->
    <nav class="act-breadcrumb" aria-label="Đường dẫn">
        <a href="<%= contextPath %>/dashboard">Trang chủ</a>
        <span class="sep">/</span>
        <span>Lịch &amp; Công việc</span>
        <span class="sep">/</span>
        <span class="current">Quản lý Hoạt động Bán hàng (Activities)</span>
    </nav>

    <% if (detailAct != null) { %>
        <!-- CHI TIẾT HOẠT ĐỘNG (KHI CÓ ID) -->
        <article class="act-detail-card">
            <header class="act-detail-header">
                <div>
                    <h1 class="act-detail-title"><%= esc(detailAct.subject) %></h1>
                    <div class="act-detail-meta">
                        <span><strong>Mã hoạt động:</strong> <%= esc(detailAct.code) %></span>
                        <span>•</span>
                        <span class="act-type-badge <%= esc(detailAct.typeBadgeClass) %>"><%= esc(detailAct.typeIcon) %> <%= esc(detailAct.typeName) %></span>
                        <span>•</span>
                        <span class="act-status-badge <%= esc(detailAct.statusClass) %>"><%= esc(detailAct.statusText) %></span>
                    </div>
                </div>
                <div>
                    <a href="<%= route %>" class="btn-outline act-btn-outline">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
                        Quay lại danh sách
                    </a>
                </div>
            </header>

            <div class="act-detail-grid">
                <section class="act-detail-box">
                    <h2 class="act-detail-box-title">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                        Thời gian &amp; Phụ trách
                    </h2>
                    <div class="act-detail-row">
                        <span class="act-detail-label">Thời gian thực hiện:</span>
                        <span class="act-detail-value"><%= esc(detailAct.timeText) %></span>
                    </div>
                    <div class="act-detail-row">
                        <span class="act-detail-label">Thời lượng:</span>
                        <span class="act-detail-value"><%= esc(detailAct.durationText) %></span>
                    </div>
                    <div class="act-detail-row">
                        <span class="act-detail-label">Người thực hiện:</span>
                        <span class="act-detail-value"><%= esc(detailAct.ownerName) %></span>
                    </div>
                </section>

                <section class="act-detail-box">
                    <h2 class="act-detail-box-title">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path></svg>
                        Khách hàng &amp; Cơ hội liên quan
                    </h2>
                    <div class="act-detail-row">
                        <span class="act-detail-label">Khách hàng / Doanh nghiệp:</span>
                        <span class="act-detail-value"><%= esc(detailAct.customerName) %></span>
                    </div>
                    <div class="act-detail-row">
                        <span class="act-detail-label">Người liên hệ:</span>
                        <span class="act-detail-value"><%= esc(detailAct.contactName) %></span>
                    </div>
                    <div class="act-detail-row">
                        <span class="act-detail-label">Cơ hội (Deal):</span>
                        <span class="act-detail-value"><%= esc(detailAct.dealName) %></span>
                    </div>
                </section>
            </div>

            <div class="act-content-box">
                <strong>Nội dung tóm tắt &amp; Ghi chú chi tiết:</strong>
                <p style="margin: 8px 0 0 0;"><%= esc(detailAct.desc) %></p>
            </div>
        </article>

    <% } else { %>

        <!-- 2. BỐ CỤC ĐẦU TRANG (HEADER & ACTIONS) -->
        <header class="act-header">
            <div class="act-header-left">
                <h1 class="act-title">Quản lý Hoạt động &amp; Lịch sử Tương tác (Sales Activities)</h1>
                <p class="act-subtitle">Theo dõi toàn diện các cuộc gọi, lịch gặp, email trao đổi và ghi chú bán hàng với khách hàng.</p>
            </div>
            <div class="act-header-actions">
                <a href="<%= route %>?action=create" class="btn-primary act-btn-primary">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                    + Thêm hoạt động
                </a>
                <a href="<%= route %>?view=timeline" class="btn-outline act-btn-outline">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                    Xem Timeline
                </a>
                <a href="<%= contextPath %>/api/activities/export" class="btn-outline act-btn-outline">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
                    Xuất Excel
                </a>
            </div>
        </header>

        <!-- 3. THANH TAB LỌC NHANH THEO LOẠI HOẠT ĐỘNG (ACTIVITY TABS) -->
        <nav class="act-tabs-bar" aria-label="Lọc nhanh theo loại hoạt động">
            <a href="<%= route %>?type=all&amp;q=<%= esc(qVal) %>" class="act-tab-item <%= ("all".equalsIgnoreCase(actTypeParam) || actTypeParam.isEmpty()) ? "active" : "" %>">
                <span>[*] Tất cả hoạt động</span>
            </a>
            <a href="<%= route %>?type=call&amp;q=<%= esc(qVal) %>" class="act-tab-item <%= "call".equalsIgnoreCase(actTypeParam) ? "active" : "" %>">
                <span>📞 Cuộc gọi (Calls)</span>
            </a>
            <a href="<%= route %>?type=meeting&amp;q=<%= esc(qVal) %>" class="act-tab-item <%= "meeting".equalsIgnoreCase(actTypeParam) ? "active" : "" %>">
                <span>🤝 Cuộc gặp (Meetings)</span>
            </a>
            <a href="<%= route %>?type=email&amp;q=<%= esc(qVal) %>" class="act-tab-item <%= "email".equalsIgnoreCase(actTypeParam) ? "active" : "" %>">
                <span>✉️ Gửi Email</span>
            </a>
            <a href="<%= route %>?type=note&amp;q=<%= esc(qVal) %>" class="act-tab-item <%= "note".equalsIgnoreCase(actTypeParam) ? "active" : "" %>">
                <span>📝 Ghi chú (Notes)</span>
            </a>
        </nav>

        <!-- 4. KHỐI BỘ LỌC ĐA TIÊU CHÍ (ADVANCED FILTER CARD - METHOD GET) -->
        <section class="act-filter-card" aria-label="Bộ lọc tìm kiếm đa tiêu chí">
            <form method="GET" action="<%= route %>" class="act-filter-form">
                <div class="act-search-wrap">
                    <svg class="act-search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                    <input type="text" name="q" class="act-search-input" placeholder="Tìm kiếm nội dung, tiêu đề, khách hàng..." value="<%= esc(qVal) %>">
                </div>
                <select name="type" class="act-select" aria-label="Loại hoạt động">
                    <option value="all" <%= "all".equalsIgnoreCase(actTypeParam) ? "selected" : "" %>>Tất cả loại</option>
                    <option value="call" <%= "call".equalsIgnoreCase(actTypeParam) ? "selected" : "" %>>Cuộc gọi (Calls)</option>
                    <option value="meeting" <%= "meeting".equalsIgnoreCase(actTypeParam) ? "selected" : "" %>>Cuộc gặp (Meetings)</option>
                    <option value="email" <%= "email".equalsIgnoreCase(actTypeParam) ? "selected" : "" %>>Gửi Email</option>
                    <option value="note" <%= "note".equalsIgnoreCase(actTypeParam) ? "selected" : "" %>>Ghi chú (Notes)</option>
                </select>
                <select name="owner" class="act-select" aria-label="Người phụ trách">
                    <option value="all" <%= ("all".equalsIgnoreCase(actOwnerParam) || actOwnerParam.isEmpty()) ? "selected" : "" %>>Tất cả người phụ trách...</option>
                    <option value="admin" <%= "admin".equalsIgnoreCase(actOwnerParam) ? "selected" : "" %>>Nông Quang Tiệp (Admin)</option>
                    <option value="salesrep" <%= "salesrep".equalsIgnoreCase(actOwnerParam) ? "selected" : "" %>>Phạm Kinh Doanh (Sales Rep)</option>
                    <option value="teamlead" <%= "teamlead".equalsIgnoreCase(actOwnerParam) ? "selected" : "" %>>Lê Trưởng Nhóm (Team Lead)</option>
                    <option value="director" <%= "director".equalsIgnoreCase(actOwnerParam) ? "selected" : "" %>>Trần Giám Đốc (Director)</option>
                </select>
                <select name="period" class="act-select" aria-label="Khoảng thời gian">
                    <option value="all" <%= ("all".equalsIgnoreCase(actPeriodParam) || actPeriodParam.isEmpty()) ? "selected" : "" %>>Tất cả thời gian</option>
                    <option value="week" <%= "week".equalsIgnoreCase(actPeriodParam) ? "selected" : "" %>>Tuần này</option>
                    <option value="month" <%= "month".equalsIgnoreCase(actPeriodParam) ? "selected" : "" %>>Tháng này</option>
                    <option value="quarter" <%= "quarter".equalsIgnoreCase(actPeriodParam) ? "selected" : "" %>>Quý này</option>
                </select>
                <div class="act-filter-actions">
                    <button type="submit" class="btn-primary act-btn-primary">Lọc dữ liệu</button>
                    <a href="activities" class="btn-outline act-btn-outline">Đặt lại</a>
                </div>
            </form>
        </section>

        <!-- 5. BẢNG DỮ LIỆU HOẠT ĐỘNG ENTERPRISE (DATA TABLE) -->
        <section class="act-table-card" aria-label="Bảng danh sách hoạt động bán hàng">
            <div class="act-table-container">
                <table class="act-table">
                    <thead>
                        <tr>
                            <th>LOẠI &amp; MÃ HOẠT ĐỘNG</th>
                            <th>TIÊU ĐỀ &amp; NỘI DUNG TÓM TẮT</th>
                            <th>KHÁCH HÀNG / LIÊN HỆ</th>
                            <th>CƠ HỘI LIÊN QUAN (DEAL)</th>
                            <th>THỜI GIAN &amp; THỜI LƯỢNG</th>
                            <th>NGƯỜI THỰC HIỆN</th>
                            <th>KẾT QUẢ / TRẠNG THÁI</th>
                            <th style="text-align: right;">THAO TÁC</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (filteredActList.isEmpty()) { %>
                        <tr>
                            <td colspan="8" style="text-align: center; padding: 40px; color: #64748b;">
                                Không tìm thấy hoạt động nào phù hợp với điều kiện tìm kiếm.
                            </td>
                        </tr>
                        <% } else {
                            for (ActItem act : filteredActList) {
                        %>
                        <tr>
                            <td>
                                <div class="act-col-type">
                                    <span class="act-type-badge <%= esc(act.typeBadgeClass) %>">
                                        <%= esc(act.typeIcon) %> <%= esc(act.typeName) %>
                                    </span>
                                    <span class="act-code-text"><%= esc(act.code) %></span>
                                </div>
                            </td>
                            <td>
                                <div class="act-col-title-wrap">
                                    <a href="<%= route %>?id=<%= act.id %>" class="act-title-link" title="Xem chi tiết hoạt động">
                                        <%= esc(act.subject) %>
                                    </a>
                                    <span class="act-desc-text"><%= esc(act.desc) %></span>
                                </div>
                            </td>
                            <td>
                                <div class="act-col-customer-wrap">
                                    <a href="<%= contextPath %>/customers?id=<%= act.id %>" class="act-customer-link">
                                        <%= esc(act.customerName) %>
                                    </a>
                                    <span class="act-contact-name"><%= esc(act.contactName) %></span>
                                </div>
                            </td>
                            <td>
                                <span class="act-deal-text"><%= esc(act.dealName) %></span>
                            </td>
                            <td>
                                <div class="act-col-time-wrap">
                                    <span class="act-time-val"><%= esc(act.timeText) %></span>
                                    <span class="act-duration-tag"><%= esc(act.durationText) %></span>
                                </div>
                            </td>
                            <td>
                                <div class="act-user-wrap">
                                    <span class="act-avatar"><%= esc(act.ownerInitials) %></span>
                                    <span class="act-user-name"><%= esc(act.ownerName) %></span>
                                </div>
                            </td>
                            <td>
                                <span class="act-status-badge <%= esc(act.statusClass) %>"><%= esc(act.statusText) %></span>
                            </td>
                            <td style="text-align: right;">
                                <a href="<%= route %>?id=<%= act.id %>" class="act-btn-action btn-text">Xem chi tiết</a>
                            </td>
                        </tr>
                        <% }} %>
                    </tbody>
                </table>
            </div>

            <!-- 6. THANH PHÂN TRANG DOANH NGHIỆP (THUẦN THẺ <a>) -->
            <footer class="act-pagination">
                <div class="act-pagination-info">
                    Hiển thị 1 - <%= filteredActList.size() %> trong tổng số <%= filteredActList.size() %> hoạt động ghi nhận • Phân quyền Data Scope: <span class="act-scope-tag"><%= esc(dataScope) %></span>
                </div>
                <nav class="act-pagination-controls" aria-label="Phân trang danh sách hoạt động">
                    <a href="<%= route %>?page=1&amp;type=<%= esc(actTypeParam) %>&amp;q=<%= esc(qVal) %>" class="act-page-btn <%= currentPage <= 1 ? "disabled" : "" %>">&larr; Trước</a>
                    <a href="<%= route %>?page=1&amp;type=<%= esc(actTypeParam) %>&amp;q=<%= esc(qVal) %>" class="act-page-btn <%= currentPage == 1 ? "active" : "" %>">1</a>
                    <a href="<%= route %>?page=2&amp;type=<%= esc(actTypeParam) %>&amp;q=<%= esc(qVal) %>" class="act-page-btn <%= currentPage == 2 ? "active" : "" %>">2</a>
                    <a href="<%= route %>?page=2&amp;type=<%= esc(actTypeParam) %>&amp;q=<%= esc(qVal) %>" class="act-page-btn">Sau &rarr;</a>
                </nav>
            </footer>
        </section>

    <% } %>

</div>
</main>

<% } else if (isQuotes) { %>

<!-- =======================================================================
     QUOTES & DISCOUNT APPROVALS MODULE - ENTERPRISE B2B UI (Sprint 7: S7-01 / S7-02)
     ======================================================================= -->
<main class="quote-page-wrapper" role="main">
<div class="quote-container">

    <!-- 1. BREADCRUMB -->
    <nav class="quote-breadcrumb" aria-label="Đường dẫn">
        <a href="<%= contextPath %>/dashboard">Trang chủ</a>
        <span class="sep">/</span>
        <span>Hợp đồng &amp; Báo giá</span>
        <span class="sep">/</span>
        <% if (detailQuote != null) { %>
        <a href="<%= route %>">Quản lý Báo giá (Quotes)</a>
        <span class="sep">/</span>
        <span class="current">Chi tiết <%= esc(detailQuote.code) %></span>
        <% } else if ("create".equals(quoteActionParam)) { %>
        <a href="<%= route %>">Quản lý Báo giá (Quotes)</a>
        <span class="sep">/</span>
        <span class="current">Tạo báo giá mới</span>
        <% } else { %>
        <span class="current">Quản lý Báo giá (Quotes)</span>
        <% } %>
    </nav>

    <% if (detailQuote != null) { %>

        <!-- ===============================================================
             CHI TIẾT BÁO GIÁ & PHÊ DUYỆT CHIẾT KHẤU 360 (DETAIL VIEW)
             =============================================================== -->
        <article class="quote-detail-card">
            <header class="quote-detail-header">
                <div class="quote-detail-title-group">
                    <h2>
                        <span><%= esc(detailQuote.code) %></span>
                        <span class="quote-version-badge"><%= esc(detailQuote.version) %></span>
                    </h2>
                    <div class="quote-detail-meta">
                        <span><strong>Khách hàng:</strong> <%= esc(detailQuote.customerName) %></span>
                        <span>•</span>
                        <span><strong>Thương vụ:</strong> <%= esc(detailQuote.dealName) %></span>
                        <span>•</span>
                        <span class="quote-status-badge <%= esc(detailQuote.statusClass) %>"><%= esc(detailQuote.statusText) %></span>
                    </div>
                </div>
                <div class="quote-actions-group">
                    <a href="<%= route %>" class="quote-btn-outline">
                        &larr; Quay lại danh sách
                    </a>
                    <a href="<%= route %>?action=approve&amp;id=<%= detailQuote.id %>" class="quote-btn-primary" style="background-color: #16a34a; border-color: #15803d;">
                        Duyệt chiết khấu
                    </a>
                    <a href="<%= route %>?action=convert&amp;id=<%= detailQuote.id %>" class="quote-btn-primary">
                        Chuyển HĐ
                    </a>
                </div>
            </header>

            <% if ("approve".equals(quoteActionParam)) { %>
            <div class="quote-alert-success" role="alert">
                <div style="font-size: 1.25rem;">✅</div>
                <div>
                    <strong>Phê duyệt chiết khấu thành công!</strong>
                    <div>Báo giá <strong><%= esc(detailQuote.code) %></strong> đã được phê duyệt mức chiết khấu <strong><%= esc(detailQuote.discountText) %></strong> bởi Quản trị viên hệ thống. Báo giá đã sẵn sàng để phát hành hợp đồng kinh tế chính thức.</div>
                </div>
            </div>
            <% } %>

            <% if ("convert".equals(quoteActionParam)) { %>
            <div class="quote-alert-success" role="alert" style="background-color: #eff6ff; border-color: #bfdbfe; color: #1e40af;">
                <div style="font-size: 1.25rem;">📄</div>
                <div>
                    <strong>Sẵn sàng chuyển đổi thành Hợp đồng kinh tế!</strong>
                    <div>Hệ thống đã chuẩn bị dự thảo Hợp đồng kinh tế <strong>HĐ-2026/CRM-<%= esc(detailQuote.code) %></strong> cho khách hàng <strong><%= esc(detailQuote.customerName) %></strong> với giá trị <strong><%= esc(detailQuote.grandTotal) %></strong>. Toàn bộ danh mục sản phẩm và điều khoản thanh toán được kế thừa tự động.</div>
                </div>
            </div>
            <% } %>

            <% if (detailQuote.isOverDiscount) { %>
            <div class="quote-alert-warning" role="alert">
                <div style="font-size: 1.25rem;">⚠️</div>
                <div>
                    <strong>Cảnh báo phê duyệt chiết khấu vượt ngưỡng (Sprint 7 - S7-02):</strong>
                    <div>Báo giá này áp dụng tỷ lệ chiết khấu <strong><%= esc(detailQuote.discountText) %></strong> (vượt hạn mức tiêu chuẩn 10.0%). Yêu cầu cấp phê duyệt từ Trưởng bộ phận hoặc Giám đốc trước khi tiến hành chuyển đổi hợp đồng hoặc gửi khách hàng.</div>
                </div>
            </div>
            <% } %>

            <div class="quote-detail-grid">
                <section class="quote-detail-box">
                    <h3>Khách hàng &amp; Thương vụ</h3>
                    <div class="quote-detail-row">
                        <span class="label">Khách hàng:</span>
                        <span class="val"><%= esc(detailQuote.customerName) %></span>
                    </div>
                    <div class="quote-detail-row">
                        <span class="label">Thương vụ liên quan:</span>
                        <span class="val"><%= esc(detailQuote.dealName) %></span>
                    </div>
                    <div class="quote-detail-row">
                        <span class="label">Người đại diện:</span>
                        <span class="val">Nguyễn Văn An (Giám đốc CNTT)</span>
                    </div>
                    <div class="quote-detail-row">
                        <span class="label">Email liên hệ:</span>
                        <span class="val">an.nguyen@viettel.com.vn</span>
                    </div>
                </section>

                <section class="quote-detail-box">
                    <h3>Thông tin Báo giá &amp; Hiệu lực</h3>
                    <div class="quote-detail-row">
                        <span class="label">Mã báo giá:</span>
                        <span class="val"><%= esc(detailQuote.code) %></span>
                    </div>
                    <div class="quote-detail-row">
                        <span class="label">Phiên bản:</span>
                        <span class="val"><%= esc(detailQuote.version) %></span>
                    </div>
                    <div class="quote-detail-row">
                        <span class="label">Ngày soạn thảo:</span>
                        <span class="val"><%= esc(detailQuote.createdAt) %></span>
                    </div>
                    <div class="quote-detail-row">
                        <span class="label">Thời hạn hiệu lực:</span>
                        <span class="val"><%= esc(detailQuote.validUntil) %></span>
                    </div>
                </section>

                <section class="quote-detail-box">
                    <h3>Người soạn thảo &amp; Điều khoản</h3>
                    <div class="quote-detail-row">
                        <span class="label">Người soạn thảo:</span>
                        <span class="val"><%= esc(detailQuote.ownerName) %></span>
                    </div>
                    <div class="quote-detail-row">
                        <span class="label">Vai trò:</span>
                        <span class="val"><%= esc(detailQuote.ownerRole) %></span>
                    </div>
                    <div class="quote-detail-row">
                        <span class="label">Trạng thái duyệt:</span>
                        <span class="val"><%= esc(detailQuote.statusText) %></span>
                    </div>
                    <div class="quote-detail-row">
                        <span class="label">Điều khoản thanh toán:</span>
                        <span class="val"><%= esc(detailQuote.paymentTerms) %></span>
                    </div>
                </section>
            </div>

            <!-- BẢNG HẠNG MỤC SẢN PHẨM / DỊCH VỤ -->
            <h3 style="font-size: 1rem; font-weight: 600; color: #0f172a; margin: 24px 0 12px 0;">Danh mục Sản phẩm &amp; Dịch vụ trong Báo giá</h3>
            <div style="overflow-x: auto; border: 1px solid #e2e8f0; border-radius: 6px;">
                <table class="quote-items-table" style="margin-bottom: 0;">
                    <thead>
                        <tr>
                            <th style="width: 40px;">STT</th>
                            <th>MÃ SẢN PHẨM</th>
                            <th>TÊN SẢN PHẨM / DỊCH VỤ</th>
                            <th style="text-align: center;">ĐVT</th>
                            <th style="text-align: right;">SỐ LƯỢNG</th>
                            <th style="text-align: right;">ĐƠN GIÁ (VND)</th>
                            <th style="text-align: right;">THÀNH TIỀN (VND)</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td style="text-align: center;">1</td>
                            <td><code>CRM-LIC-01</code></td>
                            <td><strong>Phần mềm CRM Doanh nghiệp ICTU (Bản quyền Enterprise trọn đời)</strong></td>
                            <td style="text-align: center;">Gói</td>
                            <td style="text-align: right;">1</td>
                            <td style="text-align: right;">35.000.000 đ</td>
                            <td style="text-align: right; font-weight: 600;">35.000.000 đ</td>
                        </tr>
                        <tr>
                            <td style="text-align: center;">2</td>
                            <td><code>CRM-SRV-02</code></td>
                            <td><strong>Dịch vụ Cấu hình Pipeline, Phân quyền Bảo mật &amp; Tích hợp ERP</strong></td>
                            <td style="text-align: center;">Dự án</td>
                            <td style="text-align: right;">1</td>
                            <td style="text-align: right;">15.000.000 đ</td>
                            <td style="text-align: right; font-weight: 600;">15.000.000 đ</td>
                        </tr>
                        <tr>
                            <td style="text-align: center;">3</td>
                            <td><code>CRM-TRN-03</code></td>
                            <td><strong>Gói Khóa đào tạo &amp; Hướng dẫn vận hành người dùng nội bộ (5 buổi)</strong></td>
                            <td style="text-align: center;">Khóa</td>
                            <td style="text-align: right;">1</td>
                            <td style="text-align: right;">5.000.000 đ</td>
                            <td style="text-align: right; font-weight: 600;">5.000.000 đ</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- TỔNG HỢP TÀI CHÍNH BÁO GIÁ -->
            <div class="quote-summary-panel">
                <table class="quote-summary-table">
                    <tr>
                        <td style="color: #64748b;">Tổng tiền hàng trước giảm:</td>
                        <td style="text-align: right; font-weight: 600;">55.000.000 đ</td>
                    </tr>
                    <tr>
                        <td style="color: #64748b;">Tỷ lệ chiết khấu:</td>
                        <td style="text-align: right; font-weight: 600; color: <%= detailQuote.isOverDiscount ? "#be123c" : "#047857" %>;">
                            <%= esc(detailQuote.discountText) %> <%= detailQuote.isOverDiscount ? "(Vượt ngưỡng)" : "(Chuẩn)" %>
                        </td>
                    </tr>
                    <tr>
                        <td style="color: #64748b;">Giá trị chiết khấu:</td>
                        <td style="text-align: right; font-weight: 600; color: #be123c;">-5.750.000 đ</td>
                    </tr>
                    <tr class="grand-total-row">
                        <td>TỔNG THANH TOÁN (GRAND TOTAL):</td>
                        <td style="text-align: right; color: #2563eb;"><%= esc(detailQuote.grandTotal) %></td>
                    </tr>
                </table>
            </div>

            <!-- FORM ĐIỀU CHỈNH CHIẾT KHẤU TƯƠNG THÍCH BACKEND -->
            <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; margin-top: 24px;">
                <h3 style="font-size: 0.9375rem; font-weight: 600; color: #0f172a; margin-top: 0; margin-bottom: 12px;">Cập nhật Tỷ lệ Chiết khấu (Backend Action S7-02)</h3>
                <form method="post" action="<%= contextPath %>/quotes/discount" style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
                    <input type="hidden" name="csrfToken" value="<%= com.crm.controller.ServerForms.csrf(request) %>">
                    <input type="hidden" name="id" value="<%= detailQuote.id %>">
                    <label style="font-weight: 500; font-size: 0.875rem; color: #334155;">Chiết khấu đề xuất (%)
                        <input type="number" min="0" max="100" step="0.01" name="discount" value="<%= detailQuote.discountPercent %>" class="quote-filter-search-input" style="width: 120px; display: inline-block; margin-left: 8px;" required>
                    </label>
                    <button type="submit" class="quote-btn-primary">Lưu chiết khấu</button>
                    <a href="<%= contextPath %>/quotes/pricing?id=<%= detailQuote.id %>" class="quote-btn-outline">
                        Sản phẩm và phê duyệt chiết khấu nâng cao &rarr;
                    </a>
                </form>
            </div>
        </article>

    <% } else if ("create".equals(quoteActionParam)) { %>

        <!-- ===============================================================
             TẠO BÁO GIÁ MỚI (FORM TẠO MỚI SPRINT 7 - S7-01)
             =============================================================== -->
        <article class="quote-detail-card">
            <header class="quote-detail-header">
                <div>
                    <h2 style="font-size: 1.375rem; margin-bottom: 4px;">Soạn thảo Báo giá Mới (Create Quote)</h2>
                    <p style="font-size: 0.875rem; color: #64748b; margin: 0;">Khởi tạo báo giá, áp dụng chính sách chiết khấu và thiết lập quy trình phê duyệt.</p>
                </div>
                <div>
                    <a href="<%= route %>" class="quote-btn-outline">&larr; Hủy &amp; Quay lại</a>
                </div>
            </header>

            <form method="GET" action="<%= route %>" style="max-width: 800px;">
                <input type="hidden" name="action" value="created">
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px;">
                    <div>
                        <label style="display: block; font-size: 0.8125rem; font-weight: 600; color: #334155; margin-bottom: 6px;">Mã báo giá dự kiến</label>
                        <input type="text" class="quote-filter-search-input" value="BG-2026-006" readonly style="background-color: #f8fafc;">
                    </div>
                    <div>
                        <label style="display: block; font-size: 0.8125rem; font-weight: 600; color: #334155; margin-bottom: 6px;">Phiên bản</label>
                        <input type="text" class="quote-filter-search-input" value="v1.0" readonly style="background-color: #f8fafc;">
                    </div>
                </div>

                <div style="margin-bottom: 16px;">
                    <label style="display: block; font-size: 0.8125rem; font-weight: 600; color: #334155; margin-bottom: 6px;">Khách hàng doanh nghiệp *</label>
                    <select class="quote-filter-select" style="width: 100%;" required>
                        <option value="">-- Chọn khách hàng --</option>
                        <option value="1">Tập đoàn Viettel</option>
                        <option value="2">Công ty TNHH Công Nghệ Ánh Dương</option>
                        <option value="3">Ngân hàng TMCP Phương Nam</option>
                        <option value="4">Tập đoàn Bán lẻ Sao Việt</option>
                        <option value="5">Bệnh viện Đa khoa Quốc tế An Sinh</option>
                    </select>
                </div>

                <div style="margin-bottom: 16px;">
                    <label style="display: block; font-size: 0.8125rem; font-weight: 600; color: #334155; margin-bottom: 6px;">Thương vụ / Cơ hội liên quan *</label>
                    <input type="text" class="quote-filter-search-input" placeholder="Ví dụ: Nâng cấp hệ thống CRM Cloud 2026" required>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px;">
                    <div>
                        <label style="display: block; font-size: 0.8125rem; font-weight: 600; color: #334155; margin-bottom: 6px;">Tỷ lệ chiết khấu đề xuất (%)</label>
                        <input type="number" min="0" max="100" step="0.5" class="quote-filter-search-input" placeholder="Ví dụ: 8.0" value="8.0">
                        <small style="color: #64748b; font-size: 0.75rem;">Mức chiết khấu &gt; 10% sẽ yêu cầu phê duyệt vượt ngưỡng.</small>
                    </div>
                    <div>
                        <label style="display: block; font-size: 0.8125rem; font-weight: 600; color: #334155; margin-bottom: 6px;">Thời hạn hiệu lực *</label>
                        <input type="text" class="quote-filter-search-input" value="30/11/2026" required>
                    </div>
                </div>

                <div style="margin-bottom: 24px;">
                    <label style="display: block; font-size: 0.8125rem; font-weight: 600; color: #334155; margin-bottom: 6px;">Điều khoản thanh toán &amp; Ghi chú</label>
                    <textarea class="quote-filter-search-input" rows="3" style="resize: vertical;" placeholder="Nhập điều khoản thanh toán, phương thức giao hàng và lưu ý..."></textarea>
                </div>

                <div style="display: flex; gap: 12px;">
                    <button type="submit" class="quote-btn-primary">+ Tạo và chuyển sang chọn sản phẩm</button>
                    <a href="<%= route %>" class="quote-btn-outline">Hủy bỏ</a>
                </div>
            </form>
        </article>

    <% } else { %>

        <!-- ===============================================================
             DANH SÁCH BÁO GIÁ CHUẨN DOANH NGHIỆP (ENTERPRISE LIST VIEW)
             =============================================================== -->

        <!-- 1. TIÊU ĐỀ & CỤM NÚT HÀNH ĐỘNG GÓC PHẢI -->
        <header class="quote-header">
            <div class="quote-title-group">
                <h1>Danh sách Báo giá &amp; Phê duyệt Chiết khấu (Quotes &amp; Approvals)</h1>
                <p class="quote-subtitle">Sprint 7 • Quản lý các phiên bản báo giá, chính sách chiết khấu, quy trình duyệt vượt ngưỡng và chuyển đổi hợp đồng.</p>
            </div>
            <div class="quote-actions-group">
                <a href="<%= route %>?action=create" class="quote-btn-primary">
                    + Tạo báo giá mới
                </a>
                <a href="<%= contextPath %>/api/quotes/export?q=<%= esc(qVal) %>" class="quote-btn-outline">
                    Xuất Excel
                </a>
            </div>
        </header>

        <!-- 2. THANH TAB LỌC NHANH TRẠNG THÁI PHÊ DUYỆT (APPROVAL TABS) -->
        <nav class="quote-tabs-container" aria-label="Lọc nhanh trạng thái phê duyệt">
            <a href="<%= route %>?tab=all&amp;status=all&amp;discount=<%= esc(quoteDiscountParam) %>&amp;owner=<%= esc(quoteOwnerParam) %>&amp;q=<%= esc(qVal) %>" class="quote-tab-item <%= "all".equals(currentQuoteTab) ? "active" : "" %>">
                <span>[*] Tất cả báo giá</span>
                <span class="quote-tab-badge">28</span>
            </a>
            <a href="<%= route %>?tab=pending&amp;status=pending&amp;discount=<%= esc(quoteDiscountParam) %>&amp;owner=<%= esc(quoteOwnerParam) %>&amp;q=<%= esc(qVal) %>" class="quote-tab-item tab-pending <%= "pending".equals(currentQuoteTab) ? "active" : "" %>">
                <span>⏳ Chờ phê duyệt</span>
                <span class="quote-tab-badge">5</span>
            </a>
            <a href="<%= route %>?tab=approved&amp;status=approved&amp;discount=<%= esc(quoteDiscountParam) %>&amp;owner=<%= esc(quoteOwnerParam) %>&amp;q=<%= esc(qVal) %>" class="quote-tab-item <%= "approved".equals(currentQuoteTab) ? "active" : "" %>">
                <span>✅ Đã duyệt</span>
                <span class="quote-tab-badge">18</span>
            </a>
            <a href="<%= route %>?tab=rejected&amp;status=rejected&amp;discount=<%= esc(quoteDiscountParam) %>&amp;owner=<%= esc(quoteOwnerParam) %>&amp;q=<%= esc(qVal) %>" class="quote-tab-item <%= "rejected".equals(currentQuoteTab) ? "active" : "" %>">
                <span>❌ Từ chối / Yêu cầu sửa</span>
                <span class="quote-tab-badge">3</span>
            </a>
            <a href="<%= route %>?tab=contracted&amp;status=contracted&amp;discount=<%= esc(quoteDiscountParam) %>&amp;owner=<%= esc(quoteOwnerParam) %>&amp;q=<%= esc(qVal) %>" class="quote-tab-item <%= "contracted".equals(currentQuoteTab) ? "active" : "" %>">
                <span>📄 Đã ký hợp đồng</span>
                <span class="quote-tab-badge">2</span>
            </a>
        </nav>

        <!-- 3. KHỐI BỘ LỌC ĐA TIÊU CHÍ (ADVANCED FILTER CARD - METHOD GET) -->
        <section class="quote-filter-card" aria-label="Bộ lọc báo giá nâng cao">
            <form method="GET" action="<%= route %>" class="quote-filter-form">
                <input type="hidden" name="tab" value="<%= esc(currentQuoteTab) %>">
                <div class="quote-filter-search-box">
                    <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                    <input type="text" name="q" class="quote-filter-search-input" placeholder="Tìm theo mã báo giá, khách hàng, cơ hội..." value="<%= esc(qVal) %>">
                </div>
                <select name="status" class="quote-filter-select" aria-label="Lọc theo trạng thái duyệt">
                    <option value="all" <%= "all".equalsIgnoreCase(quoteStatusParam) ? "selected" : "" %>>Tất cả trạng thái</option>
                    <option value="pending" <%= "pending".equalsIgnoreCase(quoteStatusParam) ? "selected" : "" %>>Chờ duyệt</option>
                    <option value="approved" <%= "approved".equalsIgnoreCase(quoteStatusParam) ? "selected" : "" %>>Đã duyệt</option>
                    <option value="rejected" <%= "rejected".equalsIgnoreCase(quoteStatusParam) ? "selected" : "" %>>Đã từ chối</option>
                    <option value="draft" <%= "draft".equalsIgnoreCase(quoteStatusParam) ? "selected" : "" %>>Bản nháp</option>
                </select>
                <select name="discount" class="quote-filter-select" aria-label="Lọc theo mức chiết khấu">
                    <option value="all" <%= "all".equalsIgnoreCase(quoteDiscountParam) ? "selected" : "" %>>Tất cả chiết khấu</option>
                    <option value="under10" <%= "under10".equalsIgnoreCase(quoteDiscountParam) ? "selected" : "" %>>Dưới 10% (Chuẩn)</option>
                    <option value="over10" <%= "over10".equalsIgnoreCase(quoteDiscountParam) ? "selected" : "" %>>Trên 10% (Vượt ngưỡng duyệt)</option>
                </select>
                <select name="owner" class="quote-filter-select" aria-label="Lọc theo người soạn thảo">
                    <option value="all" <%= "all".equalsIgnoreCase(quoteOwnerParam) ? "selected" : "" %>>Tất cả người soạn thảo...</option>
                    <option value="admin" <%= "admin".equalsIgnoreCase(quoteOwnerParam) ? "selected" : "" %>>Nông Quang Tiệp (Admin)</option>
                    <option value="teamlead" <%= "teamlead".equalsIgnoreCase(quoteOwnerParam) ? "selected" : "" %>>Lê Trưởng Nhóm</option>
                    <option value="director" <%= "director".equalsIgnoreCase(quoteOwnerParam) ? "selected" : "" %>>Trần Giám Đốc</option>
                    <option value="salesrep" <%= "salesrep".equalsIgnoreCase(quoteOwnerParam) ? "selected" : "" %>>Phạm Kinh Doanh</option>
                </select>
                <div class="quote-filter-actions">
                    <button type="submit" class="quote-btn-filter-submit">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon></svg>
                        Lọc dữ liệu
                    </button>
                    <a href="<%= route %>" class="quote-btn-filter-reset">Đặt lại</a>
                </div>
            </form>
        </section>

        <!-- 4. BẢNG DỮ LIỆU BÁO GIÁ ENTERPRISE (DATA TABLE CHUẨN DOANH NGHIỆP) -->
        <section class="quote-table-card" aria-label="Danh sách báo giá">
            <div class="quote-table-wrapper">
                <table class="quote-enterprise-table">
                    <thead>
                        <tr>
                            <th>MÃ BÁO GIÁ &amp; PHIÊN BẢN</th>
                            <th>KHÁCH HÀNG &amp; THƯƠNG VỤ LIÊN QUAN</th>
                            <th>TỔNG TIỀN (GRAND TOTAL)</th>
                            <th>CHIẾT KHẤU</th>
                            <th>NGƯỜI SOẠN THẢO</th>
                            <th>TRẠNG THÁI DUYỆT</th>
                            <th>HIỆU LỰC</th>
                            <th class="text-center">THAO TÁC</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (displayQuotes.isEmpty()) { %>
                        <tr>
                            <td colspan="8" style="text-align: center; padding: 36px; color: #64748b;">
                                Không tìm thấy báo giá nào phù hợp với điều kiện lọc hiện tại.
                            </td>
                        </tr>
                        <% } else {
                            for (QuoteRow item : displayQuotes) { %>
                        <tr>
                            <td>
                                <div class="quote-code-cell">
                                    <a href="<%= route %>?id=<%= item.id %>" class="quote-code-link"><%= esc(item.code) %></a>
                                    <span class="quote-version-badge"><%= esc(item.version) %></span>
                                </div>
                            </td>
                            <td>
                                <div class="quote-customer-name"><%= esc(item.customerName) %></div>
                                <div class="quote-deal-sub"><%= esc(item.dealName) %></div>
                            </td>
                            <td>
                                <span class="quote-grand-total"><%= esc(item.grandTotal) %></span>
                            </td>
                            <td>
                                <% if (item.isOverDiscount) { %>
                                <span class="quote-discount-badge quote-discount-warning" title="Vượt ngưỡng chiết khấu 10% - Cần phê duyệt">
                                    <%= esc(item.discountText) %> [!] Cần duyệt
                                </span>
                                <% } else { %>
                                <span class="quote-discount-badge quote-discount-normal">
                                    <%= esc(item.discountText) %>
                                </span>
                                <% } %>
                            </td>
                            <td>
                                <div class="quote-owner-cell">
                                    <span class="quote-avatar"><%= esc(item.ownerInitials) %></span>
                                    <span class="quote-owner-name"><%= esc(item.ownerName) %></span>
                                </div>
                            </td>
                            <td>
                                <span class="quote-status-badge <%= esc(item.statusClass) %>">
                                    <%= esc(item.statusText) %>
                                </span>
                            </td>
                            <td>
                                <span style="font-size: 0.8125rem; color: #475569;"><%= esc(item.validUntil) %></span>
                            </td>
                            <td class="text-center">
                                <div class="quote-actions-cell" style="justify-content: center;">
                                    <a href="<%= route %>?action=view&amp;id=<%= item.id %>" class="quote-btn-text">Chi tiết</a>
                                    <a href="<%= route %>?action=approve&amp;id=<%= item.id %>" class="quote-btn-text quote-btn-approve">Duyệt</a>
                                    <a href="<%= route %>?action=convert&amp;id=<%= item.id %>" class="quote-btn-text quote-btn-convert">Chuyển HĐ</a>
                                </div>
                            </td>
                        </tr>
                        <% }} %>
                    </tbody>
                </table>
            </div>

            <!-- 5. THANH PHÂN TRANG DOANH NGHIỆP (THUẦN THẺ <a>) -->
            <footer class="quote-pagination-bar">
                <div class="quote-pagination-info">
                    Hiển thị 1 - <%= displayQuotes.size() %> trong tổng số 28 báo giá • Phân quyền Data Scope: <span class="quote-scope-tag"><%= esc(dataScope) %></span>
                </div>
                <nav class="quote-pagination-controls" aria-label="Phân trang danh sách báo giá">
                    <a href="<%= route %>?page=1&amp;tab=<%= esc(currentQuoteTab) %>&amp;status=<%= esc(quoteStatusParam) %>&amp;discount=<%= esc(quoteDiscountParam) %>&amp;owner=<%= esc(quoteOwnerParam) %>&amp;q=<%= esc(qVal) %>" class="quote-page-btn <%= currentPage <= 1 ? "disabled" : "" %>">&larr; Trước</a>
                    <a href="<%= route %>?page=1&amp;tab=<%= esc(currentQuoteTab) %>&amp;status=<%= esc(quoteStatusParam) %>&amp;discount=<%= esc(quoteDiscountParam) %>&amp;owner=<%= esc(quoteOwnerParam) %>&amp;q=<%= esc(qVal) %>" class="quote-page-btn <%= currentPage == 1 ? "active" : "" %>">1</a>
                    <a href="<%= route %>?page=2&amp;tab=<%= esc(currentQuoteTab) %>&amp;status=<%= esc(quoteStatusParam) %>&amp;discount=<%= esc(quoteDiscountParam) %>&amp;owner=<%= esc(quoteOwnerParam) %>&amp;q=<%= esc(qVal) %>" class="quote-page-btn <%= currentPage == 2 ? "active" : "" %>">2</a>
                    <a href="<%= route %>?page=2&amp;tab=<%= esc(currentQuoteTab) %>&amp;status=<%= esc(quoteStatusParam) %>&amp;discount=<%= esc(quoteDiscountParam) %>&amp;owner=<%= esc(quoteOwnerParam) %>&amp;q=<%= esc(qVal) %>" class="quote-page-btn <%= currentPage >= 2 ? "disabled" : "" %>">Sau &rarr;</a>
                </nav>
            </footer>
        </section>

    <% } %>

</div>
</main>

<% } else { %>

    <!-- GIAO DIỆN DÀNH CHO OPPORTUNITIES -->
    <main class="crm-page">
    <div class="crm-customer-container">
        <nav class="crm-cust-breadcrumb" aria-label="Đường dẫn">
            <a href="<%= contextPath %>/dashboard">Trang chủ</a>
            <span class="separator">/</span>
            <span class="current"><%= esc(request.getAttribute("moduleTitle")) %></span>
        </nav>

        <div class="crm-scoped-fallback">
            <h1><%= esc(request.getAttribute("moduleTitle")) %></h1>
            <% if (detail != null) { %>
                <p><strong>Bản ghi:</strong> <%= esc(detail.label()) %></p>
                <p><strong>Chủ sở hữu (Owner User ID):</strong> <%= detail.ownerUserId() %></p>
                <% if ("/quotes".equals(servletPath)) { %>
                <p><a href="<%= contextPath %>/quotes/pricing?id=<%= detail.id() %>" class="crm-btn-primary-action">Sản phẩm và phê duyệt chiết khấu</a></p>
                <form method="post" action="<%= contextPath %>/quotes/discount" style="margin-top: 16px;">
                    <input type="hidden" name="csrfToken" value="<%= com.crm.controller.ServerForms.csrf(request) %>">
                    <input type="hidden" name="id" value="<%= detail.id() %>">
                    <label style="display:block; margin-bottom:8px; font-weight:600;">Chiết khấu (%)
                        <input type="number" min="0" max="100" step="0.01" name="discount" class="crm-filter-select" style="max-width:200px; display:inline-block;" required>
                    </label>
                    <button type="submit" class="crm-btn-apply">Lưu chiết khấu</button>
                </form>
                <% } %>
                <div style="margin-top: 20px;">
                    <a href="<%= route %>" class="cust-back-link">Quay lại danh sách</a>
                </div>
            <% } else { %>
                <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px; margin-bottom:16px;">
                    <form method="get" action="<%= route %>" style="display:flex; gap:8px;">
                        <input name="q" class="crm-filter-input-search" style="min-width:260px;" placeholder="Tìm kiếm <%= esc(request.getAttribute("moduleTitle")) %>..." value="<%= esc(qVal) %>">
                        <button type="submit" class="crm-btn-apply">Tìm kiếm</button>
                    </form>
                    <form method="get" action="<%= contextPath + "/api" + servletPath + "/export" %>">
                        <input type="hidden" name="q" value="<%= esc(qVal) %>">
                        <button type="submit" class="crm-btn-secondary-action">Xuất Excel</button>
                    </form>
                </div>

                <div class="crm-table-container">
                    <table class="crm-enterprise-table">
                        <thead>
                            <tr>
                                <th>MÃ / ID</th>
                                <th>TIÊU ĐỀ / NHÃN BẢN GHI</th>
                                <th>NGƯỜI PHỤ TRÁCH (OWNER ID)</th>
                                <th class="text-center">THAO TÁC</th>
                            </tr>
                        </thead>
                        <tbody>
                            <%
                            List<ScopeRecord> rows = (List<ScopeRecord>) request.getAttribute("records");
                            if (rows == null || rows.isEmpty()) {
                            %>
                            <tr><td colspan="4" style="text-align:center; padding:24px;">Không có bản ghi nào.</td></tr>
                            <% } else { for (ScopeRecord row : rows) { %>
                            <tr>
                                <td>#<%= row.id() %></td>
                                <td><strong><a href="<%= route %>?id=<%= row.id() %>" class="cust-name-link"><%= esc(row.label()) %></a></strong></td>
                                <td>User #<%= row.ownerUserId() %></td>
                                <td class="text-center"><a href="<%= route %>?id=<%= row.id() %>" class="cust-btn-360">Xem chi tiết</a></td>
                            </tr>
                            <% }} %>
                        </tbody>
                    </table>
                </div>

                <div style="margin-top: 16px; display:flex; justify-content:flex-end;">
                    <form method="get" action="<%= route %>" style="display:flex; align-items:center; gap:8px;">
                        <input type="hidden" name="q" value="<%= esc(qVal) %>">
                        <span>Trang</span>
                        <input type="number" name="page" min="1" max="<%= request.getAttribute("pageCount") %>" value="<%= request.getAttribute("pageNumber") %>" class="crm-filter-select" style="width:65px;">
                        <span>/ <%= request.getAttribute("pageCount") %></span>
                        <button type="submit" class="crm-btn-secondary-action">Chuyển trang</button>
                    </form>
                </div>
            <% } %>
        </div>
    </div>
    </main>

<% } %>

</div>
</body>
</html>
