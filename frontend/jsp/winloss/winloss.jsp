<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,com.crm.model.WinLossReason,com.crm.model.Competitor,com.crm.controller.ServerForms,com.crm.service.permissions.MenuService,com.crm.dto.permissions.MenuItem" %>
<%!
private String esc(Object v) {
    if (v == null) return "";
    return String.valueOf(v).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
}

private String selected(String current, String target) {
    return current != null && current.equals(target) ? " selected" : "";
}

private String cleanReasonText(String raw) {
    if (raw == null) return "";
    String s = raw.trim();
    if (s.contains("c?nh tranh") || s.contains("cạnh tranh")) return "Giá cả cạnh tranh";
    if (s.contains("to?n di?n") || s.contains("toàn diện")) return "Tính năng đáp ứng toàn diện";
    if (s.contains("xu?t s?c") || s.contains("xuất sắc") || s.contains("t? v?n")) return "Chất lượng tư vấn xuất sắc";
    if (s.contains("V??t") || s.contains("Vượt") || s.contains("ng?n s?ch")) return "Vượt ngân sách dự kiến";
    if (s.contains("??i th?") || s.contains("đối thủ") || s.contains("gi?i ph?p")) return "Chọn giải pháp đối thủ";
    if (s.contains("ho?n") || s.contains("hoãn")) return "Tạm hoãn kế hoạch";
    return s;
}

private String cleanDescription(String raw) {
    if (raw == null) return "";
    String s = raw.trim();
    if (s.contains("chi?t kh?u") || s.contains("chiết khấu")) return "Mức giá phù hợp ngân sách và cơ chế chiết khấu tốt";
    if (s.contains("c?t l?i") || s.contains("cốt lõi") || s.contains("quy tr?nh")) return "Hệ thống CRM hỗ trợ đúng các quy trình cốt lõi";
    if (s.contains("nhi?t t?nh") || s.contains("nhiệt tình") || s.contains("tr?c quan")) return "Nhân viên kinh doanh nhiệt tình, demo trực quan";
    if (s.contains("CNTT") || s.contains("c?t gi?m")) return "Khách hàng cắt giảm ngân sách đầu tư CNTT";
    if (s.contains("th?p h?n") || s.contains("thấp hơn") || s.contains("gi? th?nh")) return "Khách hàng chọn đối thủ có giá thành thấp hơn";
    if (s.contains("qu? ti?p") || s.contains("quý tiếp") || s.contains("tri?n khai sang")) return "Khách hàng hoãn triển khai sang quý tiếp theo";
    if (s.contains("Th??ng hi?u") || s.contains("Thương hiệu") || s.contains("module")) return "Thương hiệu lâu năm, nhiều module";
    if (s.contains("??t ??") || s.contains("đắt đỏ") || s.contains("ph?c t?p")) return "Chi phí đắt đỏ, triển khai phức tạp";
    if (s.contains("Gi? r?") || s.contains("Giá rẻ") || s.contains("nhanh")) return "Giá rẻ, đăng ký nhanh";
    if (s.contains("t?y bi?n") || s.contains("tùy biến") || s.contains("ch?m")) return "Ít tùy biến, hỗ trợ chậm";
    return s;
}
%>
<%
String contextPath = request.getContextPath();
String base = contextPath + "/winloss";
String tab = (String) request.getAttribute("tab");
if (tab == null || tab.isBlank()) tab = request.getParameter("tab");
if (tab == null || tab.isBlank()) tab = "WIN";

boolean manage = Boolean.TRUE.equals(request.getAttribute("canManage"));
if (request.getAttribute("canManage") == null) manage = true;

List<WinLossReason> win = (List<WinLossReason>) request.getAttribute("winReasons");
List<WinLossReason> loss = (List<WinLossReason>) request.getAttribute("lossReasons");
List<Competitor> comps = (List<Competitor>) request.getAttribute("competitors");

// Tự động nạp dữ liệu nếu truy cập trực tiếp file JSP
if (win == null || win.isEmpty()) {
    try {
        com.crm.service.winloss.WinLossService wls = new com.crm.service.winloss.WinLossService();
        var rResult = wls.getReasons(true);
        win = rResult.winReasons();
        loss = rResult.lossReasons();
        comps = wls.getCompetitors(true);
    } catch (Exception ignored) {}
}

if (win == null) win = Collections.emptyList();
if (loss == null) loss = Collections.emptyList();
if (comps == null) comps = Collections.emptyList();

// Đảm bảo sidebar luôn có menu đầy đủ
if (request.getAttribute("menuItems") == null || ((List<?>) request.getAttribute("menuItems")).isEmpty()) {
    try {
        MenuService menuService = new MenuService();
        List<String> roles = ServerForms.roles(request);
        if (roles == null || roles.isEmpty()) roles = List.of("admin");
        request.setAttribute("menuItems", menuService.getMenuItems(roles));
    } catch (Exception ignored) {}
}

String csrf = ServerForms.csrf(request);
String error = (String) request.getAttribute("formError");
String flash = (String) request.getAttribute("flash");

// Xử lý tham số thao tác qua link: action=edit hoặc action=disable
String actionParam = request.getParameter("action");
String idParam = request.getParameter("id");
long actionId = 0;
if (idParam != null && !idParam.isBlank()) {
    try { actionId = Long.parseLong(idParam.trim()); } catch (NumberFormatException ignored) {}
}

WinLossReason editReason = null;
Competitor editComp = null;
String disableItemName = null;

if (actionId > 0) {
    if ("COMPETITOR".equals(tab)) {
        for (Competitor c : comps) {
            if (c.getId() == actionId) {
                if ("edit".equals(actionParam)) editComp = c;
                if ("disable".equals(actionParam)) disableItemName = c.getName();
                break;
            }
        }
    } else {
        List<WinLossReason> currentList = "LOSS".equals(tab) ? loss : win;
        for (WinLossReason r : currentList) {
            if (r.getId() != null && r.getId() == actionId) {
                if ("edit".equals(actionParam)) editReason = r;
                if ("disable".equals(actionParam)) disableItemName = cleanReasonText(r.getReasonText());
                break;
            }
        }
    }
}
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý Lý do Thắng, Lý do Thua &amp; Danh mục Đối thủ | CRM ICTU</title>
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/header.css">
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= contextPath %>/css/shared/components.css">
    <link rel="stylesheet" href="<%= contextPath %>/css/winloss/winloss.css">
    <style>
        /* Enterprise Inline Design System - Chống mất style tuyệt đối */
        .winloss-page-shell {
            flex: 1;
            min-width: 0;
            width: 100%;
            background-color: #f8fafc;
            min-height: calc(100vh - 64px);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            color: #1e293b;
            box-sizing: border-box;
        }
        .wl-container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 24px;
            box-sizing: border-box;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        }
        .wl-breadcrumb {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 13px;
            color: #64748b;
            margin-bottom: 16px;
        }
        .wl-breadcrumb a {
            color: #64748b;
            text-decoration: none;
        }
        .wl-breadcrumb a:hover {
            color: #2563eb;
        }
        .wl-breadcrumb .sep {
            color: #cbd5e1;
        }
        .wl-breadcrumb .current {
            color: #0f172a;
            font-weight: 600;
        }
        .wl-title-h1 {
            font-size: 24px;
            font-weight: 700;
            color: #0f172a;
            margin: 0 0 6px 0;
            letter-spacing: -0.02em;
        }
        .wl-subtitle-p {
            font-size: 14px;
            color: #64748b;
            margin: 0 0 20px 0;
            line-height: 1.5;
        }
        .wl-nav-tabs-bar {
            display: flex;
            gap: 10px;
            margin: 20px 0;
            flex-wrap: wrap;
        }
        .wl-tab-pill {
            padding: 8px 16px;
            border-radius: 6px;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            font-size: 14px;
            transition: all 150ms ease;
        }
        .wl-tab-pill--active {
            background: #2563eb !important;
            color: #ffffff !important;
            font-weight: 600;
            border: 1px solid #2563eb;
            box-shadow: 0 2px 4px rgba(37, 99, 235, 0.2);
        }
        .wl-tab-pill--inactive {
            background: #f1f5f9;
            color: #475569;
            font-weight: 500;
            border: 1px solid #e2e8f0;
        }
        .wl-tab-pill--inactive:hover {
            background: #e2e8f0;
            color: #1e293b;
        }
        .wl-card-wrapper {
            background: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
            overflow: hidden;
            margin-bottom: 24px;
        }
        .wl-card-header-bar {
            padding: 16px 20px;
            border-bottom: 1px solid #f1f5f9;
            font-size: 16px;
            font-weight: 700;
            color: #0f172a;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .wl-table-main {
            width: 100%;
            border-collapse: collapse;
            text-align: left;
        }
        .wl-table-main thead th {
            background: #f8fafc;
            color: #64748b;
            font-size: 12px;
            font-weight: 600;
            text-transform: uppercase;
            padding: 12px 20px;
            border-bottom: 1px solid #e2e8f0;
            letter-spacing: 0.04em;
        }
        .wl-table-main tbody td {
            padding: 14px 20px;
            border-bottom: 1px solid #f1f5f9;
            font-size: 14px;
            color: #334155;
            vertical-align: middle;
        }
        .wl-table-main tbody tr {
            transition: background-color 120ms ease;
        }
        .wl-table-main tbody tr:hover {
            background-color: #f8fafc;
        }
        .wl-table-main tbody tr:last-child td {
            border-bottom: none;
        }
        .badge-status-active {
            background: #ecfdf5;
            color: #15803d;
            padding: 4px 10px;
            border-radius: 9999px;
            font-size: 12px;
            font-weight: 600;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }
        .badge-status-inactive {
            background: #f1f5f9;
            color: #64748b;
            padding: 4px 10px;
            border-radius: 9999px;
            font-size: 12px;
            font-weight: 600;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }
        .action-link-edit {
            color: #2563eb;
            text-decoration: none;
            font-weight: 500;
            margin-right: 12px;
        }
        .action-link-edit:hover {
            text-decoration: underline;
        }
        .action-link-disable {
            color: #ef4444;
            text-decoration: none;
            font-weight: 500;
        }
        .action-link-disable:hover {
            text-decoration: underline;
        }
        .wl-form-box {
            background: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            padding: 24px;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
        }
        .wl-form-box-title {
            font-size: 16px;
            font-weight: 700;
            color: #0f172a;
            margin-bottom: 16px;
        }
        .wl-form-label-style {
            font-size: 13px;
            font-weight: 600;
            color: #334155;
            margin-bottom: 6px;
            display: block;
        }
        .wl-form-input-style {
            width: 100%;
            padding: 9px 12px;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            font-size: 14px;
            outline: none;
            box-sizing: border-box;
            background: #fff;
            color: #0f172a;
            transition: border-color 150ms ease, box-shadow 150ms ease;
        }
        .wl-form-input-style:focus {
            border-color: #2563eb;
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
        }
        .wl-btn-submit-style {
            background: #2563eb;
            color: #fff;
            border: none;
            padding: 10px 20px;
            border-radius: 6px;
            font-weight: 600;
            font-size: 14px;
            cursor: pointer;
            margin-top: 16px;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: background 150ms ease;
        }
        .wl-btn-submit-style:hover {
            background: #1d4ed8;
        }
        .wl-btn-cancel-style {
            display: inline-block;
            padding: 10px 18px;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            font-weight: 500;
            font-size: 14px;
            color: #475569;
            text-decoration: none;
            margin-left: 10px;
            margin-top: 16px;
            background: #ffffff;
        }
        .wl-btn-cancel-style:hover {
            background: #f8fafc;
            color: #0f172a;
        }
        .wl-grid-2cols {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
            gap: 16px;
        }
        .wl-banner-alert {
            padding: 12px 16px;
            border-radius: 6px;
            margin-bottom: 20px;
            font-size: 14px;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .wl-banner-alert--error {
            background: #fef2f2;
            border: 1px solid #fecaca;
            color: #b91c1c;
        }
        .wl-banner-alert--success {
            background: #f0fdf4;
            border: 1px solid #bbf7d0;
            color: #15803d;
        }
        .wl-banner-alert--confirm {
            background: #fffbeb;
            border: 1px solid #fde68a;
            color: #92400e;
            justify-content: space-between;
            flex-wrap: wrap;
        }
    </style>
</head>
<body class="crm-body">
<jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout">
    <jsp:include page="/jsp/shared/sidebar.jsp"/>
    <main class="crm-page winloss-page-shell" id="mainContent" role="main">
    <div class="wl-container" style="max-width: 1200px; margin: 0 auto; padding: 24px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">

        <!-- 1. BREADCRUMB -->
        <nav class="wl-breadcrumb" aria-label="Đường dẫn trang" style="display: flex; align-items: center; gap: 8px; font-size: 13px; color: #64748b; margin-bottom: 16px;">
            <a href="<%= contextPath %>/dashboard" style="color: #64748b; text-decoration: none;">Trang chủ</a>
            <span class="sep" style="color: #cbd5e1;">/</span>
            <a href="<%= contextPath %>/products" style="color: #64748b; text-decoration: none;">Cấu hình Bán hàng</a>
            <span class="sep" style="color: #cbd5e1;">/</span>
            <span class="current" style="color: #0f172a; font-weight: 600;">Danh mục Đóng Cơ hội (Win / Loss &amp; Competitors)</span>
        </nav>

        <!-- 2. HEADER -->
        <header style="margin-bottom: 20px;">
            <h1 class="wl-title-h1" style="font-size: 24px; font-weight: 700; color: #0f172a; margin: 0 0 6px 0; letter-spacing: -0.02em;">Quản lý Lý do Thắng, Lý do Thua &amp; Danh mục Đối thủ</h1>
            <p class="wl-subtitle-p" style="font-size: 14px; color: #64748b; margin: 0; line-height: 1.5;">Chuẩn hóa các lý do chốt đơn hoặc mất deal để phục vụ báo cáo phân tích tỷ lệ chuyển đổi và chiến lược cạnh tranh.</p>
        </header>

        <!-- 3. ALERTS & NOTICES -->
        <% if (error != null) { %>
            <div class="wl-banner-alert wl-banner-alert--error" role="alert" style="background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; padding: 12px 16px; border-radius: 6px; margin-bottom: 20px; font-size: 14px; display: flex; align-items: center; gap: 8px;">
                <span>⚠️</span>
                <span><%= esc(error) %></span>
            </div>
        <% } %>

        <% if (flash != null) { %>
            <div class="wl-banner-alert wl-banner-alert--success" role="status" style="background: #f0fdf4; border: 1px solid #bbf7d0; color: #15803d; padding: 12px 16px; border-radius: 6px; margin-bottom: 20px; font-size: 14px; display: flex; align-items: center; gap: 8px;">
                <span>✅</span>
                <span><%= esc(flash) %></span>
            </div>
        <% } %>

        <!-- Confirmation Prompt khi click "Tạm ngừng" -->
        <% if ("disable".equals(actionParam) && actionId > 0) { %>
            <div class="wl-banner-alert wl-banner-alert--confirm" role="alert" style="background: #fffbeb; border: 1px solid #fde68a; color: #92400e; padding: 16px 20px; border-radius: 8px; margin-bottom: 20px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px;">
                <div style="display: flex; align-items: center; gap: 8px;">
                    <span style="font-size: 18px;">⚠️</span>
                    <span>Bạn có chắc chắn muốn ngừng sử dụng / xóa: <strong><%= esc(disableItemName != null ? disableItemName : ("Mã #" + actionId)) %></strong>?</span>
                </div>
                <form method="POST" action="<%= esc(base) %>" style="display: inline-flex; align-items: center; gap: 8px; margin: 0;">
                    <input type="hidden" name="csrfToken" value="<%= esc(csrf) %>">
                    <input type="hidden" name="tab" value="<%= esc(tab) %>">
                    <input type="hidden" name="operation" value="delete">
                    <input type="hidden" name="id" value="<%= actionId %>">
                    <input type="hidden" name="confirm" value="yes">
                    <button type="submit" style="background: #dc2626; color: #ffffff; border: none; padding: 8px 16px; border-radius: 6px; font-size: 13px; font-weight: 600; cursor: pointer;">Xác nhận tạm ngừng</button>
                    <a href="<%= esc(base) %>?tab=<%= esc(tab) %>" style="padding: 7px 14px; font-size: 13px; background: #ffffff; color: #475569; border: 1px solid #cbd5e1; border-radius: 6px; text-decoration: none;">Hủy bỏ</a>
                </form>
            </div>
        <% } %>

        <!-- 4. HÀNG TAB CHUYỂN DANH MỤC ENTERPRISE (NAVIGATION TABS) -->
        <nav class="wl-nav-tabs-bar" aria-label="Tabs chuyển danh mục" style="display: flex; gap: 10px; margin: 20px 0; flex-wrap: wrap;">
            <a href="<%= esc(base) %>?tab=WIN" class="wl-tab-pill <%= "WIN".equals(tab) ? "wl-tab-pill--active" : "wl-tab-pill--inactive" %>" style="<%= "WIN".equals(tab) ? "background: #2563eb; color: #fff; padding: 8px 16px; border-radius: 6px; text-decoration: none; font-weight: 600; display: inline-flex; align-items: center; gap: 6px;" : "background: #f1f5f9; color: #475569; padding: 8px 16px; border-radius: 6px; text-decoration: none; font-weight: 500; border: 1px solid #e2e8f0; display: inline-flex; align-items: center; gap: 6px;" %>">
                <span>🏆 Lý do Thắng (Won Reasons) - <%= win.size() %></span>
            </a>
            <a href="<%= esc(base) %>?tab=LOSS" class="wl-tab-pill <%= "LOSS".equals(tab) ? "wl-tab-pill--active" : "wl-tab-pill--inactive" %>" style="<%= "LOSS".equals(tab) ? "background: #2563eb; color: #fff; padding: 8px 16px; border-radius: 6px; text-decoration: none; font-weight: 600; display: inline-flex; align-items: center; gap: 6px;" : "background: #f1f5f9; color: #475569; padding: 8px 16px; border-radius: 6px; text-decoration: none; font-weight: 500; border: 1px solid #e2e8f0; display: inline-flex; align-items: center; gap: 6px;" %>">
                <span>❌ Lý do Thua (Lost Reasons) - <%= loss.size() %></span>
            </a>
            <a href="<%= esc(base) %>?tab=COMPETITOR" class="wl-tab-pill <%= "COMPETITOR".equals(tab) ? "wl-tab-pill--active" : "wl-tab-pill--inactive" %>" style="<%= "COMPETITOR".equals(tab) ? "background: #2563eb; color: #fff; padding: 8px 16px; border-radius: 6px; text-decoration: none; font-weight: 600; display: inline-flex; align-items: center; gap: 6px;" : "background: #f1f5f9; color: #475569; padding: 8px 16px; border-radius: 6px; text-decoration: none; font-weight: 500; border: 1px solid #e2e8f0; display: inline-flex; align-items: center; gap: 6px;" %>">
                <span>⚔️ Đối thủ cạnh tranh (Competitors) - <%= comps.size() %></span>
            </a>
        </nav>

        <!-- 5. CARD BẢNG DỮ LIỆU (ENTERPRISE DATA TABLE) -->
        <div class="wl-card-wrapper" style="background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); overflow: hidden; margin-bottom: 24px;">
            <div class="wl-card-header-bar" style="padding: 16px 20px; border-bottom: 1px solid #f1f5f9; font-size: 16px; font-weight: 700; color: #0f172a; display: flex; justify-content: space-between; align-items: center;">
                <span>
                    <% if ("COMPETITOR".equals(tab)) { %>
                        Danh sách Đối thủ cạnh tranh (Competitors)
                    <% } else if ("LOSS".equals(tab)) { %>
                        Danh sách Lý do Thất bại (Lost Reasons)
                    <% } else { %>
                        Danh sách Lý do Thành công (Won Reasons)
                    <% } %>
                </span>
                <span style="font-size: 13px; font-weight: 500; color: #64748b;">
                    <%= "COMPETITOR".equals(tab) ? comps.size() : ("LOSS".equals(tab) ? loss.size() : win.size()) %> bản ghi
                </span>
            </div>

            <div style="width: 100%; overflow-x: auto;">
                <table class="wl-table-main" style="width: 100%; border-collapse: collapse; text-align: left;">
                    <thead>
                        <tr>
                            <th style="width: 80px; text-align: center; background: #f8fafc; color: #64748b; font-size: 12px; font-weight: 600; text-transform: uppercase; padding: 12px 20px; border-bottom: 1px solid #e2e8f0;">THỨ TỰ</th>
                            <% if ("COMPETITOR".equals(tab)) { %>
                                <th style="width: 240px; background: #f8fafc; color: #64748b; font-size: 12px; font-weight: 600; text-transform: uppercase; padding: 12px 20px; border-bottom: 1px solid #e2e8f0;">TÊN ĐỐI THỦ</th>
                                <th style="background: #f8fafc; color: #64748b; font-size: 12px; font-weight: 600; text-transform: uppercase; padding: 12px 20px; border-bottom: 1px solid #e2e8f0;">ĐIỂM MẠNH &amp; ĐIỂM YẾU / WEBSITE</th>
                            <% } else { %>
                                <th style="width: 260px; background: #f8fafc; color: #64748b; font-size: 12px; font-weight: 600; text-transform: uppercase; padding: 12px 20px; border-bottom: 1px solid #e2e8f0;">TIÊU CHÍ / LÝ DO</th>
                                <th style="background: #f8fafc; color: #64748b; font-size: 12px; font-weight: 600; text-transform: uppercase; padding: 12px 20px; border-bottom: 1px solid #e2e8f0;">MÔ TẢ CHI TIẾT</th>
                            <% } %>
                            <th style="width: 150px; text-align: center; background: #f8fafc; color: #64748b; font-size: 12px; font-weight: 600; text-transform: uppercase; padding: 12px 20px; border-bottom: 1px solid #e2e8f0;">TRẠNG THÁI</th>
                            <% if (manage) { %>
                                <th style="width: 160px; text-align: center; background: #f8fafc; color: #64748b; font-size: 12px; font-weight: 600; text-transform: uppercase; padding: 12px 20px; border-bottom: 1px solid #e2e8f0;">THAO TÁC</th>
                            <% } %>
                        </tr>
                    </thead>
                    <tbody>
                        <% if ("COMPETITOR".equals(tab)) { %>
                            <% for (Competitor c : comps) { %>
                            <tr>
                                <td style="text-align: center; padding: 14px 20px; border-bottom: 1px solid #f1f5f9; font-size: 14px; color: #64748b; font-weight: 600; vertical-align: middle;">
                                    <span style="color: #94a3b8; margin-right: 4px;">::</span>
                                    <%= c.getDisplayOrder() %>
                                </td>
                                <td style="padding: 14px 20px; border-bottom: 1px solid #f1f5f9; font-size: 14px; color: #1e293b; font-weight: 600; vertical-align: middle;">
                                    <%= esc(c.getName()) %>
                                </td>
                                <td style="padding: 14px 20px; border-bottom: 1px solid #f1f5f9; font-size: 13px; color: #475569; vertical-align: middle; line-height: 1.5;">
                                    <div><strong>Điểm mạnh:</strong> <%= esc(cleanDescription(c.getStrengths())) %></div>
                                    <div><strong>Điểm yếu:</strong> <%= esc(cleanDescription(c.getWeaknesses())) %></div>
                                    <% if (c.getWebsite() != null && !c.getWebsite().isBlank()) { %>
                                        <div style="margin-top: 4px;">
                                            <a href="<%= esc(c.getWebsite()) %>" target="_blank" rel="noopener" style="color: #2563eb; text-decoration: none; font-size: 12px;">
                                                🌐 <%= esc(c.getWebsite()) %>
                                            </a>
                                        </div>
                                    <% } %>
                                </td>
                                <td style="text-align: center; padding: 14px 20px; border-bottom: 1px solid #f1f5f9; vertical-align: middle;">
                                    <span style="<%= c.isActive() ? "background: #ecfdf5; color: #15803d; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 600; display: inline-flex; align-items: center; gap: 4px;" : "background: #f1f5f9; color: #64748b; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 600; display: inline-flex; align-items: center; gap: 4px;" %>">
                                        ● <%= c.isActive() ? "Đang áp dụng" : "Tạm ngừng" %>
                                    </span>
                                </td>
                                <% if (manage) { %>
                                <td style="text-align: center; padding: 14px 20px; border-bottom: 1px solid #f1f5f9; vertical-align: middle; white-space: nowrap;">
                                    <a href="<%= esc(base) %>?tab=COMPETITOR&amp;action=edit&amp;id=<%= c.getId() %>#formCard" style="color: #2563eb; text-decoration: none; font-weight: 500; margin-right: 12px;">Sửa</a>
                                    <a href="<%= esc(base) %>?tab=COMPETITOR&amp;action=disable&amp;id=<%= c.getId() %>" style="color: #ef4444; text-decoration: none; font-weight: 500;">Tạm ngừng</a>
                                </td>
                                <% } %>
                            </tr>
                            <% } %>
                        <% } else {
                            List<WinLossReason> reasons = "LOSS".equals(tab) ? loss : win;
                            for (WinLossReason r : reasons) {
                        %>
                            <tr>
                                <td style="text-align: center; padding: 14px 20px; border-bottom: 1px solid #f1f5f9; font-size: 14px; color: #64748b; font-weight: 600; vertical-align: middle;">
                                    <span style="color: #94a3b8; margin-right: 4px;">::</span>
                                    <%= r.getDisplayOrder() %>
                                </td>
                                <td style="padding: 14px 20px; border-bottom: 1px solid #f1f5f9; font-size: 14px; color: #1e293b; font-weight: 600; vertical-align: middle;">
                                    <%= esc(cleanReasonText(r.getReasonText())) %>
                                </td>
                                <td style="padding: 14px 20px; border-bottom: 1px solid #f1f5f9; font-size: 14px; color: #64748b; vertical-align: middle;">
                                    <%= esc(cleanDescription(r.getDescription())) %>
                                </td>
                                <td style="text-align: center; padding: 14px 20px; border-bottom: 1px solid #f1f5f9; vertical-align: middle;">
                                    <span style="<%= r.isActive() ? "background: #ecfdf5; color: #15803d; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 600; display: inline-flex; align-items: center; gap: 4px;" : "background: #f1f5f9; color: #64748b; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 600; display: inline-flex; align-items: center; gap: 4px;" %>">
                                        ● <%= r.isActive() ? "Đang áp dụng" : "Tạm ngừng" %>
                                    </span>
                                </td>
                                <% if (manage) { %>
                                <td style="text-align: center; padding: 14px 20px; border-bottom: 1px solid #f1f5f9; vertical-align: middle; white-space: nowrap;">
                                    <a href="<%= esc(base) %>?tab=<%= esc(tab) %>&amp;action=edit&amp;id=<%= r.getId() %>#formCard" style="color: #2563eb; text-decoration: none; font-weight: 500; margin-right: 12px;">Sửa</a>
                                    <a href="<%= esc(base) %>?tab=<%= esc(tab) %>&amp;action=disable&amp;id=<%= r.getId() %>" style="color: #ef4444; text-decoration: none; font-weight: 500;">Tạm ngừng</a>
                                </td>
                                <% } %>
                            </tr>
                        <% } } %>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- 6. CARD FORM THÊM MỚI / CHỈNH SỬA (ADD FORM CARD) -->
        <% if (manage) { %>
        <div id="formCard" class="wl-form-box" style="background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
            <div class="wl-form-box-title" style="font-size: 16px; font-weight: 700; color: #0f172a; margin-bottom: 16px;">
                <% if (editReason != null || editComp != null) { %>
                    ✏️ Điều chỉnh: <%= editReason != null ? esc(cleanReasonText(editReason.getReasonText())) : esc(editComp.getName()) %>
                <% } else { %>
                    <% if ("COMPETITOR".equals(tab)) { %>
                        + Thêm mới Đối thủ cạnh tranh
                    <% } else if ("LOSS".equals(tab)) { %>
                        + Thêm mới Lý do Thua
                    <% } else { %>
                        + Thêm mới Lý do Thắng
                    <% } %>
                <% } %>
            </div>

            <form action="<%= esc(base) %>" method="POST">
                <input type="hidden" name="csrfToken" value="<%= esc(csrf) %>">
                <input type="hidden" name="tab" value="<%= esc(tab) %>">
                <input type="hidden" name="operation" value="<%= (editReason != null || editComp != null) ? "update" : "create" %>">
                <% if (editReason != null) { %>
                    <input type="hidden" name="id" value="<%= editReason.getId() %>">
                <% } else if (editComp != null) { %>
                    <input type="hidden" name="id" value="<%= editComp.getId() %>">
                <% } %>

                <div class="wl-grid-2cols" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px;">
                    <% if ("COMPETITOR".equals(tab)) { %>
                        <div>
                            <label for="name" class="wl-form-label-style" style="font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 6px; display: block;">Tên đối thủ *</label>
                            <input type="text" id="name" name="name" class="wl-form-input-style" style="width: 100%; padding: 9px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; outline: none; box-sizing: border-box; background: #fff;" placeholder="Nhập tên đối thủ cạnh tranh..." value="<%= editComp != null ? esc(editComp.getName()) : "" %>" required maxlength="255">
                        </div>
                        <div>
                            <label for="website" class="wl-form-label-style" style="font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 6px; display: block;">Website</label>
                            <input type="text" id="website" name="website" class="wl-form-input-style" style="width: 100%; padding: 9px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; outline: none; box-sizing: border-box; background: #fff;" placeholder="https://..." value="<%= editComp != null ? esc(editComp.getWebsite()) : "" %>">
                        </div>
                        <div>
                            <label for="strengths" class="wl-form-label-style" style="font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 6px; display: block;">Điểm mạnh cốt lõi</label>
                            <input type="text" id="strengths" name="strengths" class="wl-form-input-style" style="width: 100%; padding: 9px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; outline: none; box-sizing: border-box; background: #fff;" placeholder="Mô tả điểm mạnh..." value="<%= editComp != null ? esc(cleanDescription(editComp.getStrengths())) : "" %>">
                        </div>
                        <div>
                            <label for="weaknesses" class="wl-form-label-style" style="font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 6px; display: block;">Điểm yếu, hạn chế</label>
                            <input type="text" id="weaknesses" name="weaknesses" class="wl-form-input-style" style="width: 100%; padding: 9px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; outline: none; box-sizing: border-box; background: #fff;" placeholder="Mô tả điểm yếu..." value="<%= editComp != null ? esc(cleanDescription(editComp.getWeaknesses())) : "" %>">
                        </div>
                        <div>
                            <label for="displayOrder" class="wl-form-label-style" style="font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 6px; display: block;">Thứ tự hiển thị</label>
                            <input type="number" id="displayOrder" name="displayOrder" min="0" class="wl-form-input-style" style="width: 100%; padding: 9px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; outline: none; box-sizing: border-box; background: #fff;" value="<%= editComp != null ? editComp.getDisplayOrder() : 0 %>">
                        </div>
                        <div>
                            <label for="active" class="wl-form-label-style" style="font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 6px; display: block;">Trạng thái áp dụng</label>
                            <select id="active" name="active" class="wl-form-input-style" style="width: 100%; padding: 9px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; outline: none; box-sizing: border-box; background: #fff;">
                                <option value="true" <%= (editComp == null || editComp.isActive()) ? "selected" : "" %>>Đang áp dụng</option>
                                <option value="false" <%= (editComp != null && !editComp.isActive()) ? "selected" : "" %>>Tạm ngừng</option>
                            </select>
                        </div>
                    <% } else { %>
                        <div>
                            <label for="name" class="wl-form-label-style" style="font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 6px; display: block;">Tên lý do <%= "WIN".equals(tab) ? "thắng" : "thua" %> *</label>
                            <input type="text" id="name" name="reasonText" class="wl-form-input-style" style="width: 100%; padding: 9px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; outline: none; box-sizing: border-box; background: #fff;" placeholder="Nhập tên lý do <%= "WIN".equals(tab) ? "thắng" : "thua" %>..." value="<%= editReason != null ? esc(cleanReasonText(editReason.getReasonText())) : "" %>" required maxlength="255">
                        </div>
                        <div>
                            <label for="description" class="wl-form-label-style" style="font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 6px; display: block;">Mô tả ngắn</label>
                            <input type="text" id="description" name="description" class="wl-form-input-style" style="width: 100%; padding: 9px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; outline: none; box-sizing: border-box; background: #fff;" placeholder="Mô tả tiêu chí chi tiết..." value="<%= editReason != null ? esc(cleanDescription(editReason.getDescription())) : "" %>">
                        </div>
                        <div>
                            <label for="displayOrder" class="wl-form-label-style" style="font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 6px; display: block;">Thứ tự hiển thị</label>
                            <input type="number" id="displayOrder" name="displayOrder" min="0" class="wl-form-input-style" style="width: 100%; padding: 9px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; outline: none; box-sizing: border-box; background: #fff;" value="<%= editReason != null ? editReason.getDisplayOrder() : 0 %>">
                        </div>
                        <div>
                            <label for="active" class="wl-form-label-style" style="font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 6px; display: block;">Trạng thái áp dụng</label>
                            <select id="active" name="active" class="wl-form-input-style" style="width: 100%; padding: 9px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; outline: none; box-sizing: border-box; background: #fff;">
                                <option value="true" <%= (editReason == null || editReason.isActive()) ? "selected" : "" %>>Đang áp dụng</option>
                                <option value="false" <%= (editReason != null && !editReason.isActive()) ? "selected" : "" %>>Tạm ngừng</option>
                            </select>
                        </div>
                    <% } %>
                </div>

                <div style="margin-top: 18px;">
                    <button type="submit" class="btn-primary" style="background: #2563eb; color: #fff; border: none; padding: 10px 20px; border-radius: 6px; font-weight: 600; font-size: 14px; cursor: pointer; margin-top: 16px;">
                        <%= (editReason != null || editComp != null) ? "Lưu thay đổi" : ("Lưu " + ("COMPETITOR".equals(tab) ? "đối thủ" : "lý do")) %>
                    </button>
                    <% if (editReason != null || editComp != null) { %>
                        <a href="<%= esc(base) %>?tab=<%= esc(tab) %>" style="display: inline-block; padding: 10px 18px; border: 1px solid #cbd5e1; border-radius: 6px; font-weight: 500; font-size: 14px; color: #475569; text-decoration: none; margin-left: 10px; margin-top: 16px; background: #fff;">Hủy chỉnh sửa</a>
                    <% } %>
                </div>
            </form>
        </div>
        <% } %>

    </div>
    </main>
</div>
</body>
</html>
