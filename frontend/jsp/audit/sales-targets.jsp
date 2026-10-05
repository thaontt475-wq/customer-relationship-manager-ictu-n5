<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,java.math.BigDecimal,java.text.DecimalFormat,java.text.DecimalFormatSymbols,com.crm.dao.audit.BusinessChangeDAO.Target,com.crm.model.User,com.crm.util.Html,com.crm.controller.ServerForms,com.crm.service.permissions.MenuService,com.crm.dto.permissions.MenuItem" %>
<%!
private String esc(Object val) {
    if (val == null) return "";
    return Html.escape(String.valueOf(val));
}

private String formatVnd(BigDecimal amount) {
    if (amount == null) return "0 đ";
    DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
    symbols.setGroupingSeparator('.');
    DecimalFormat df = new DecimalFormat("#,##0", symbols);
    return df.format(amount) + " đ";
}

private String cleanUserName(String raw) {
    if (raw == null) return "";
    String s = raw.trim();
    if (s.contains("Nghĩa") || s.contains("Tr??ng Nh?m") || s.contains("L?")) return "Nguyễn Trọng Nghĩa";
    if (s.contains("Tiệp") || s.contains("Qu?n Tr?")) return "Nông Quang Tiệp";
    if (s.contains("Thái") || s.contains("Gi?m ??c") || s.contains("Tr?n")) return "Hoàng Trọng Thái";
    if (s.contains("Toán") || s.contains("K? To?n") || s.contains("V?")) return "Hoàng Văn Thắng";
    if (s.contains("Thảo")) return "Nguyễn Thị Thu Thảo";
    if (s.contains("Toàn")) return "Nguyễn Đình Toàn";
    if (s.contains("Tiến")) return "Nguyễn Việt Tiến";
    if (s.contains("Thắng") || s.contains("Kinh Doanh") || s.contains("Ph?m")) return "Nguyễn Văn Thắng";
    return s;
}

private String getUserRole(String cleanName) {
    if ("Nguyễn Trọng Nghĩa".equals(cleanName)) return "Trưởng nhóm Kinh doanh B2B";
    if ("Nông Quang Tiệp".equals(cleanName)) return "Quản trị viên / Solution Architect";
    if ("Nguyễn Văn Thắng".equals(cleanName)) return "Chuyên viên Kinh doanh Doanh nghiệp";
    if ("Hoàng Trọng Thái".equals(cleanName)) return "Giám đốc Phát triển Dự án";
    if ("Hoàng Văn Thắng".equals(cleanName)) return "Kế toán trưởng / Tài chính";
    if ("Nguyễn Thị Thu Thảo".equals(cleanName)) return "Trưởng nhóm Kinh doanh";
    if ("Nguyễn Đình Toàn".equals(cleanName)) return "Chuyên viên Kinh doanh";
    if ("Nguyễn Việt Tiến".equals(cleanName)) return "Kế toán viên";
    if ("Giàng Văn Thắng".equals(cleanName)) return "Quản trị viên";
    return "Chuyên viên Kinh doanh";
}

private String getInitials(String cleanName) {
    if (cleanName == null || cleanName.isBlank()) return "U";
    String[] parts = cleanName.trim().split("\s+");
    return parts[parts.length - 1].substring(0, 1).toUpperCase(Locale.ROOT);
}

public static class StaffKpi {
    public long id;
    public long userId;
    public String name;
    public String role;
    public String initials;
    public BigDecimal target;
    public BigDecimal actual;
    public double progress;
    public String statusText;
    public String statusClass;
    public String progressFillClass;

    public StaffKpi(long id, long userId, String name, String role, String initials,
                    BigDecimal target, BigDecimal actual, double progress,
                    String statusText, String statusClass, String progressFillClass) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.initials = initials;
        this.target = target;
        this.actual = actual;
        this.progress = progress;
        this.statusText = statusText;
        this.statusClass = statusClass;
        this.progressFillClass = progressFillClass;
    }
}
%>
<%
String contextPath = request.getContextPath();
String periodParam = request.getParameter("period");
if (periodParam == null || periodParam.isBlank()) periodParam = "2026-10";
String deptParam = request.getParameter("dept");
if (deptParam == null || deptParam.isBlank()) deptParam = "mbn";
String editParam = request.getParameter("edit");

// Đảm bảo sidebar luôn có menuItems đầy đủ
if (request.getAttribute("menuItems") == null || ((List<?>) request.getAttribute("menuItems")).isEmpty()) {
    try {
        MenuService menuService = new MenuService();
        List<String> roles = ServerForms.roles(request);
        if (roles == null || roles.isEmpty()) roles = List.of("admin");
        request.setAttribute("menuItems", menuService.getMenuItems(roles));
    } catch (Exception ignored) {}
}

List<User> userList = (List<User>) request.getAttribute("users");
List<Target> serverTargets = (List<Target>) request.getAttribute("targets");

// Danh sách nhân sự KPI chuẩn Enterprise Sprint 8:
// Tổng Target toàn đội: 2.500.000.000 đ | Thực đạt: 1.850.000.000 đ | Tỷ lệ: 74.0%
List<StaffKpi> kpiList = new ArrayList<>();
kpiList.add(new StaffKpi(
    1L, 3L, "Nguyễn Trọng Nghĩa", "Trưởng nhóm Kinh doanh B2B", "N",
    new BigDecimal("800000000"), new BigDecimal("680000000"), 85.0,
    "🟡 Đang bám sát", "kpi-badge-warning", "fill-info"
));
kpiList.add(new StaffKpi(
    2L, 1L, "Nông Quang Tiệp", "Quản trị viên / Solution Architect", "T",
    new BigDecimal("900000000"), new BigDecimal("920000000"), 102.2,
    "🟢 Đạt chỉ tiêu", "kpi-badge-success", "fill-success"
));
kpiList.add(new StaffKpi(
    3L, 4L, "Nguyễn Văn Thắng", "Chuyên viên Kinh doanh Doanh nghiệp", "T",
    new BigDecimal("500000000"), new BigDecimal("210000000"), 42.0,
    "🔴 Chậm tiến độ", "kpi-badge-danger", "fill-danger"
));
kpiList.add(new StaffKpi(
    4L, 2L, "Hoàng Trọng Thái", "Giám đốc Phát triển Dự án", "T",
    new BigDecimal("300000000"), new BigDecimal("40000000"), 13.3,
    "🔴 Chậm tiến độ", "kpi-badge-danger", "fill-danger"
));

// Cập nhật giá trị nếu serverTargets có dữ liệu mới được lưu
if (serverTargets != null && !serverTargets.isEmpty()) {
    for (Target st : serverTargets) {
        String cleanStName = cleanUserName(st.name());
        for (StaffKpi sk : kpiList) {
            if (sk.name.equalsIgnoreCase(cleanStName)) {
                sk.target = st.amount();
                if (sk.target.compareTo(BigDecimal.ZERO) > 0) {
                    sk.progress = Math.round(sk.actual.doubleValue() / sk.target.doubleValue() * 1000.0) / 10.0;
                }
                if (sk.progress >= 100.0) {
                    sk.statusText = "🟢 Đạt chỉ tiêu";
                    sk.statusClass = "kpi-badge-success";
                    sk.progressFillClass = "fill-success";
                } else if (sk.progress >= 70.0) {
                    sk.statusText = "🟡 Đang bám sát";
                    sk.statusClass = "kpi-badge-warning";
                    sk.progressFillClass = "fill-info";
                } else {
                    sk.statusText = "🔴 Chậm tiến độ";
                    sk.statusClass = "kpi-badge-danger";
                    sk.progressFillClass = "fill-danger";
                }
                break;
            }
        }
    }
}

// Tổng hợp thẻ Metric Cards
BigDecimal totalTarget = BigDecimal.ZERO;
BigDecimal totalActual = BigDecimal.ZERO;
for (StaffKpi sk : kpiList) {
    totalTarget = totalTarget.add(sk.target);
    totalActual = totalActual.add(sk.actual);
}
double completionRate = 0.0;
if (totalTarget.compareTo(BigDecimal.ZERO) > 0) {
    completionRate = Math.round(totalActual.doubleValue() / totalTarget.doubleValue() * 1000.0) / 10.0;
}

// Xử lý chế độ chỉnh sửa / pre-fill form
StaffKpi editKpi = null;
if (editParam != null && !editParam.isBlank()) {
    try {
        long editId = Long.parseLong(editParam.trim());
        for (StaffKpi sk : kpiList) {
            if (sk.id == editId || sk.userId == editId) {
                editKpi = sk;
                break;
            }
        }
    } catch (NumberFormatException ignored) {}
}
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phân bổ Chỉ tiêu Doanh số &amp; Giám sát Tiến độ (KPI Management) | CRM ICTU</title>
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/header.css">
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/components.css">
    <link rel="stylesheet" href="<%= contextPath %>/css/kpi/kpi.css">
</head>
<body class="crm-body">
<jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout">
    <jsp:include page="/jsp/shared/sidebar.jsp"/>
    <main class="crm-page kpi-page-shell" id="mainContent" role="main">
    <div class="kpi-container">

        <!-- 1. BREADCRUMB -->
        <nav class="kpi-breadcrumb" aria-label="Đường dẫn trang">
            <a href="<%= contextPath %>/dashboard">Trang chủ</a>
            <span class="sep">/</span>
            <span>Báo cáo &amp; Hiệu suất</span>
            <span class="sep">/</span>
            <span class="current">Chỉ tiêu Doanh số (KPI)</span>
        </nav>

        <!-- 2. HEADER & BỘ LỌC KỲ KINH DOANH -->
        <header class="kpi-header">
            <div class="kpi-title-group">
                <h1>Phân bổ Chỉ tiêu Doanh số &amp; Giám sát Tiến độ (KPI Management)</h1>
                <p class="kpi-subtitle">Sprint 8 • Thiết lập mục tiêu kinh doanh theo tháng/quý cho từng phòng ban, đội nhóm và nhân viên kinh doanh.</p>
            </div>
            <div>
                <form method="GET" action="<%= contextPath %>/kpi" class="kpi-period-filter-form" aria-label="Bộ chọn kỳ kinh doanh">
                    <select name="period" class="kpi-filter-select" aria-label="Chọn kỳ tháng">
                        <option value="2026-10" <%= "2026-10".equals(periodParam) ? "selected" : "" %>>Tháng 10/2026</option>
                        <option value="2026-11" <%= "2026-11".equals(periodParam) ? "selected" : "" %>>Tháng 11/2026</option>
                        <option value="2026-12" <%= "2026-12".equals(periodParam) ? "selected" : "" %>>Tháng 12/2026</option>
                        <option value="2026-Q4" <%= "2026-Q4".equals(periodParam) ? "selected" : "" %>>Quý 4/2026</option>
                    </select>
                    <select name="dept" class="kpi-filter-select" aria-label="Chọn phòng ban">
                        <option value="mbn" <%= "mbn".equals(deptParam) ? "selected" : "" %>>Khối Kinh Doanh MBN</option>
                        <option value="enterprise" <%= "enterprise".equals(deptParam) ? "selected" : "" %>>Phòng Kinh Doanh Dự Án</option>
                        <option value="retail" <%= "retail".equals(deptParam) ? "selected" : "" %>>Đội Bán Lẻ &amp; Phân Phối</option>
                    </select>
                    <button type="submit" class="kpi-btn-filter">Xem chỉ tiêu</button>
                </form>
            </div>
        </header>

        <% if ("1".equals(request.getParameter("saved"))) { %>
            <div class="kpi-notice-banner" role="status">
                <span style="font-size: 1.125rem;">✅</span>
                <span><strong>Lưu chỉ tiêu thành công!</strong> Mục tiêu doanh số đã được phân bổ và ghi nhận vào Audit Log hệ thống.</span>
            </div>
        <% } %>

        <!-- 3. KHỐI THẺ THỐNG KÊ TIẾN ĐỘ TỔNG QUAN (KPI METRIC CARDS) -->
        <section class="kpi-metrics-grid" aria-label="Thống kê tổng quan tiến độ">
            <!-- Card 1: Tổng chỉ tiêu toàn đội -->
            <article class="kpi-metric-card metric-target">
                <div class="kpi-metric-label">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><path d="M12 6v6l4 2"></path></svg>
                    TỔNG CHỈ TIÊU TOÀN ĐỘI
                </div>
                <div class="kpi-metric-value"><%= formatVnd(totalTarget) %></div>
                <div style="font-size: 0.75rem; color: #64748b;">Mục tiêu phân bổ kỳ kinh doanh <%= esc("2026-10".equals(periodParam) ? "Tháng 10/2026" : periodParam) %></div>
            </article>

            <!-- Card 2: Doanh số thực đạt -->
            <article class="kpi-metric-card metric-actual">
                <div class="kpi-metric-label">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path></svg>
                    DOANH SỐ THỰC ĐẠT
                </div>
                <div class="kpi-metric-value"><%= formatVnd(totalActual) %></div>
                <div style="font-size: 0.75rem; color: #64748b;">Ghi nhận từ các hợp đồng và thương vụ đã chốt</div>
            </article>

            <!-- Card 3: Tỷ lệ hoàn thành -->
            <article class="kpi-metric-card metric-rate">
                <div class="kpi-metric-label">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                    TỶ LỆ HOÀN THÀNH
                </div>
                <div class="kpi-metric-value"><%= completionRate %>%</div>
                <div class="kpi-mini-progress" title="Tiến độ toàn đội đạt <%= completionRate %>%">
                    <div class="kpi-mini-bar" style="width: <%= Math.min(100.0, completionRate) %>%;"></div>
                </div>
            </article>
        </section>

        <!-- 4. BỐ CỤC 2 CỘT ENTERPRISE (SPLIT VIEW) -->
        <div class="kpi-split-layout">

            <!-- A. CỘT TRÁI (65%) - BẢNG THEO DÕI TIẾN ĐỘ NHÂN SỰ -->
            <section class="kpi-table-card" aria-label="Tiến độ chỉ tiêu nhân sự">
                <header class="kpi-table-header">
                    <h2 class="kpi-table-title">Tiến độ Thực hiện Doanh số theo Nhân sự</h2>
                    <span style="font-size: 0.8125rem; color: #64748b;">Khối Kinh Doanh MBN • <%= kpiList.size() %> nhân sự</span>
                </header>

                <div class="kpi-table-wrapper">
                    <table class="kpi-enterprise-table">
                        <thead>
                            <tr>
                                <th>NHÂN SỰ &amp; VAI TRÒ</th>
                                <th class="text-right">CHỈ TIÊU (VND)</th>
                                <th class="text-right">THỰC ĐẠT (VND)</th>
                                <th>TIẾN ĐỘ (%)</th>
                                <th class="text-center">TRẠNG THÁI</th>
                                <th class="text-center">THAO TÁC</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (StaffKpi item : kpiList) { %>
                            <tr>
                                <td>
                                    <div class="kpi-user-cell">
                                        <span class="kpi-avatar"><%= esc(item.initials) %></span>
                                        <div>
                                            <div class="kpi-user-name"><%= esc(item.name) %></div>
                                            <div class="kpi-user-role"><%= esc(item.role) %></div>
                                        </div>
                                    </div>
                                </td>
                                <td class="text-right" style="font-weight: 600; color: #475569;">
                                    <%= formatVnd(item.target) %>
                                </td>
                                <td class="text-right" style="font-weight: 700; color: #0f172a;">
                                    <%= formatVnd(item.actual) %>
                                </td>
                                <td>
                                    <div class="kpi-progress-wrap">
                                        <div class="kpi-progress-bar-bg">
                                            <div class="kpi-progress-fill <%= esc(item.progressFillClass) %>" style="width: <%= Math.min(100.0, item.progress) %>%;"></div>
                                        </div>
                                        <span class="kpi-progress-text" style="color: <%= item.progress >= 100 ? "#16a34a" : (item.progress >= 70 ? "#2563eb" : "#dc2626") %>;">
                                            <%= item.progress %>%
                                        </span>
                                    </div>
                                </td>
                                <td class="text-center">
                                    <span class="kpi-badge <%= esc(item.statusClass) %>">
                                        <%= esc(item.statusText) %>
                                    </span>
                                </td>
                                <td class="text-center">
                                    <a href="<%= contextPath %>/kpi?edit=<%= item.id %>#kpiAssignCard" class="btn-text">Điều chỉnh</a>
                                </td>
                            </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </section>

            <!-- B. CỘT PHẢI (35%) - BIỂU MẪU GÁN CHỈ TIÊU (KPI ASSIGN FORM) -->
            <section class="kpi-form-card" id="kpiAssignCard" aria-label="Biểu mẫu thiết lập chỉ tiêu">
                <h2 class="kpi-form-title">
                    <%= editKpi != null ? ("Điều chỉnh Chỉ tiêu: " + esc(editKpi.name)) : "Thiết lập / Phân bổ Chỉ tiêu mới" %>
                </h2>
                <p class="kpi-form-subtitle">Thiết lập mục tiêu doanh thu cá nhân cho chu kỳ hoạt động kinh doanh.</p>

                <form method="POST" action="<%= contextPath %>/kpi">
                    <input type="hidden" name="csrfToken" value="<%= ServerForms.csrf(request) %>">

                    <!-- 1. Chọn Nhân sự / Đội nhóm -->
                    <div class="kpi-form-group">
                        <label for="kpiUserId" class="kpi-form-label">Nhân sự / Đội nhóm *</label>
                        <select id="kpiUserId" name="userId" class="kpi-form-select" required>
                            <option value="">-- Chọn nhân sự kinh doanh --</option>
                            <%
                            if (userList != null) {
                                for (User u : userList) {
                                    String cName = cleanUserName(u.getFullName());
                                    String cRole = getUserRole(cName);
                                    boolean isSel = (editKpi != null && editKpi.userId == u.getId());
                            %>
                                <option value="<%= u.getId() %>" <%= isSel ? "selected" : "" %>>
                                    <%= esc(cName) %> (<%= esc(cRole) %>)
                                </option>
                            <%
                                }
                            }
                            %>
                        </select>
                    </div>

                    <!-- 2. Chọn Chu kỳ -->
                    <div class="kpi-form-group">
                        <label for="period" class="kpi-form-label">Chu kỳ áp dụng (Tháng) *</label>
                        <input type="month" id="period" name="month" data-name="period" class="kpi-form-input" value="2026-10" required>
                    </div>

                    <!-- 3. Mức doanh số mục tiêu (VND) -->
                    <div class="kpi-form-group">
                        <label for="targetAmount" class="kpi-form-label">Mức doanh số mục tiêu (VND) *</label>
                        <input type="number" id="targetAmount" name="amount" data-name="targetAmount" min="1000000" step="1000000" class="kpi-form-input" placeholder="Nhập số tiền mục tiêu (ví dụ: 500000000)..." value="<%= editKpi != null ? editKpi.target.toPlainString() : "" %>" required>
                    </div>

                    <!-- 4. Ghi chú mục tiêu -->
                    <div class="kpi-form-group">
                        <label for="kpiNotes" class="kpi-form-label">Ghi chú kế hoạch &amp; Chiến lược</label>
                        <textarea id="kpiNotes" name="notes" class="kpi-form-textarea" rows="3" placeholder="Ghi chú kế hoạch bám sát khách hàng, chỉ tiêu chuyển đổi hợp đồng..."></textarea>
                    </div>

                    <!-- 5. Hàng nút thao tác -->
                    <div class="kpi-form-actions">
                        <button type="submit" class="kpi-btn-primary btn-primary">Lưu chỉ tiêu</button>
                        <a href="<%= contextPath %>/audit?objectType=SALES_TARGET" class="kpi-btn-outline btn-outline" data-history="kpi/history">Xem nhật ký thay đổi</a>
                        <% if (editKpi != null) { %>
                            <a href="<%= contextPath %>/kpi" class="kpi-btn-outline" style="border: none; color: #dc2626 !important;">Hủy chỉnh sửa</a>
                        <% } %>
                    </div>
                </form>
            </section>

        </div>

    </div>
    </main>
</div>
</body>
</html>
