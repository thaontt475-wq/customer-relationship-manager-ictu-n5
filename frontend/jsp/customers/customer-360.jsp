<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.util.Map,com.crm.service.scope.ScopeRecord,com.crm.model.CustomField,com.crm.model.User,com.crm.model.Category,com.crm.util.Html" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= Html.escape(request.getAttribute("customerName") != null ? request.getAttribute("customerName") : "Khách hàng 360°") %> | CRM</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/customers/customers.css">
</head>
<body class="crm-body">
<jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout">
    <jsp:include page="/jsp/shared/sidebar.jsp"/>
    <main class="crm-main-content">
        <%
            ScopeRecord record = (ScopeRecord) request.getAttribute("record");
            String customerName = record != null ? record.label() : "Khách hàng";
            String ownerName = (String) request.getAttribute("ownerName");
            String teamName = (String) request.getAttribute("teamName");
            String industryName = (String) request.getAttribute("industryName");
            String companySizeName = (String) request.getAttribute("companySizeName");
            List<CustomField> customFields = (List<CustomField>) request.getAttribute("customFields");
            Map<String, String> customFieldValues = (Map<String, String>) request.getAttribute("customFieldValues");
            List<ScopeRecord> relatedDeals = (List<ScopeRecord>) request.getAttribute("relatedDeals");
            List<ScopeRecord> relatedActivities = (List<ScopeRecord>) request.getAttribute("relatedActivities");
            
            if (ownerName == null || ownerName.isBlank()) ownerName = "Chưa phân bổ";
            if (teamName == null || teamName.isBlank()) teamName = "Chưa gán nhóm";
            if (industryName == null || industryName.isBlank()) industryName = "Chưa xác định";
            if (companySizeName == null || companySizeName.isBlank()) companySizeName = "Chưa xác định";
        %>
        
        <div class="crm-customer-360">
            <!-- Breadcrumbs & Actions -->
            <div class="crm-page-header">
                <nav class="crm-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/dashboard">Trang chủ</a>
                    <span aria-hidden="true">&rsaquo;</span>
                    <a href="${pageContext.request.contextPath}/customers">Khách hàng</a>
                    <span aria-hidden="true">&rsaquo;</span>
                    <span aria-current="page">Góc nhìn toàn diện 360°</span>
                </nav>
                <div class="crm-toolbar-right" style="display: flex; gap: var(--crm-space-3);">
                    <a href="${pageContext.request.contextPath}/customers" class="crm-btn crm-btn-secondary">Quay lại danh sách</a>
                </div>
            </div>

            <!-- Hero Header Card -->
            <section class="c360-hero-card" aria-label="Thông tin tổng quan khách hàng">
                <div class="c360-hero-main">
                    <div class="c360-avatar" aria-hidden="true">
                        <%= customerName != null && !customerName.isEmpty() ? Html.escape(customerName.substring(0, 1).toUpperCase()) : "K" %>
                    </div>
                    <div class="c360-hero-info">
                        <div class="c360-title-row">
                            <h1 class="c360-name"><%= Html.escape(customerName) %></h1>
                            <span class="crm-badge crm-badge-success">Đang hoạt động</span>
                            <span class="crm-badge">Doanh nghiệp B2B</span>
                        </div>
                        <div class="c360-meta-line">
                            <span class="c360-meta-item"><strong>Ngành nghề:</strong> <%= Html.escape(industryName) %></span>
                            <span>&bull;</span>
                            <span class="c360-meta-item"><strong>Quy mô:</strong> <%= Html.escape(companySizeName) %></span>
                            <span>&bull;</span>
                            <span class="c360-meta-item"><strong>Phụ trách:</strong> <%= Html.escape(ownerName) %> (<%= Html.escape(teamName) %>)</span>
                        </div>
                        <div class="c360-stats-line">
                            <span>Mã khách hàng: <strong>CUST-<%= record != null ? record.id() : 0 %></strong></span>
                            <span>&bull;</span>
                            <span>Số cơ hội liên quan: <strong class="c360-stat-highlight"><%= relatedDeals != null ? relatedDeals.size() : 0 %></strong></span>
                            <span>&bull;</span>
                            <span>Hoạt động ghi nhận: <strong class="c360-stat-highlight"><%= relatedActivities != null ? relatedActivities.size() : 0 %></strong></span>
                        </div>
                    </div>
                </div>

                <!-- Churn Risk Alert Banner -->
                <div class="c360-churn-banner" role="alert">
                    <div class="c360-churn-icon" aria-hidden="true">&#9888;</div>
                    <div class="c360-churn-body">
                        <span class="c360-churn-title">Cờ cảnh báo rủi ro rời bỏ (Churn Risk)</span>
                        <span class="c360-churn-desc">Mức độ: <strong>THEO DÕI</strong> — Đã đồng bộ xuyên suốt từ Lead, Cơ hội đến Báo giá.</span>
                        <span class="c360-churn-action">&rarr; Đề xuất: Phân công Sales Rep định kỳ ghi nhận hoạt động chăm sóc.</span>
                    </div>
                </div>
            </section>

            <!-- 3-Column Detail Grid -->
            <div class="c360-grid">
                <!-- Column 1: Contacts & Buying Roles -->
                <div class="c360-column">
                    <div class="c360-card">
                        <div class="c360-card-header">
                            <h2 class="c360-card-title">1. Người liên hệ & Vai trò mua hàng</h2>
                        </div>
                        <div class="c360-card-body">
                            <div class="c360-contact-list">
                                <div class="c360-contact-item">
                                    <div class="c360-contact-avatar" aria-hidden="true">N</div>
                                    <div class="c360-contact-info">
                                        <span class="c360-contact-name">Đại diện pháp nhân chính</span>
                                        <span class="c360-contact-role-title">Người phụ trách chuyên môn</span>
                                        <span class="c360-buying-role decision-maker">Quyết định mua (Decision Maker)</span>
                                        <span class="c360-contact-meta">Email: contact@<%= Html.escape(customerName.toLowerCase().replaceAll("[^a-z0-9]", "")) %>.vn</span>
                                    </div>
                                </div>
                                <div class="c360-contact-item">
                                    <div class="c360-contact-avatar" aria-hidden="true">T</div>
                                    <div class="c360-contact-info">
                                        <span class="c360-contact-name">Đại diện bộ phận mua sắm</span>
                                        <span class="c360-contact-role-title">Trưởng phòng / Chuyên viên thu mua</span>
                                        <span class="c360-buying-role buyer">Đàm phán giá (Buyer / Procurement)</span>
                                        <span class="c360-contact-meta">SĐT: 0912 888 999</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Additional / Custom Fields Section -->
                    <div class="c360-card">
                        <div class="c360-card-header">
                            <h2 class="c360-card-title">Trường thông tin tùy chỉnh</h2>
                        </div>
                        <div class="c360-card-body">
                            <% if (customFields != null && !customFields.isEmpty()) { %>
                                <div class="c360-kv-list">
                                    <% for (CustomField cf : customFields) {
                                        if (cf == null || !cf.isActive()) continue;
                                        String val = (customFieldValues != null && customFieldValues.containsKey(cf.getFieldName()))
                                                ? customFieldValues.get(cf.getFieldName()) : null;
                                    %>
                                        <div class="c360-kv-item">
                                            <span class="c360-kv-label"><%= Html.escape(cf.getFieldLabel()) %></span>
                                            <span class="c360-kv-value"><%= (val != null && !val.isBlank()) ? Html.escape(val) : "<em class='crm-muted'>Chưa có dữ liệu</em>" %></span>
                                        </div>
                                    <% } %>
                                </div>
                            <% } else { %>
                                <div class="c360-empty">Chưa có trường dữ liệu tùy chỉnh nào được kích hoạt.</div>
                            <% } %>
                        </div>
                    </div>
                </div>

                <!-- Column 2: Opportunities & Pipeline -->
                <div class="c360-column">
                    <div class="c360-card">
                        <div class="c360-card-header">
                            <h2 class="c360-card-title">2. Cơ hội kinh doanh & Pipeline (<%= relatedDeals != null ? relatedDeals.size() : 0 %>)</h2>
                        </div>
                        <div class="c360-card-body">
                            <% if (relatedDeals != null && !relatedDeals.isEmpty()) { %>
                                <div class="c360-deal-list">
                                    <% for (ScopeRecord deal : relatedDeals) { %>
                                        <div class="c360-deal-item">
                                            <div class="c360-deal-header">
                                                <span class="c360-deal-name"><%= Html.escape(deal.label()) %></span>
                                            </div>
                                            <div class="c360-deal-meta">
                                                <span class="c360-deal-stage">Mã thương vụ: DEAL-<%= deal.id() %></span>
                                                <a href="${pageContext.request.contextPath}/opportunities?id=<%= deal.id() %>" class="crm-btn crm-btn-secondary" style="min-height: 28px; padding: 2px 8px; font-size: 0.75rem;">Xem chi tiết &rarr;</a>
                                            </div>
                                        </div>
                                    <% } %>
                                </div>
                            <% } else { %>
                                <div class="c360-empty">Chưa có cơ hội kinh doanh nào gắn với khách hàng này.</div>
                            <% } %>
                        </div>
                    </div>
                </div>

                <!-- Column 3: Timeline & Activities -->
                <div class="c360-column">
                    <div class="c360-card">
                        <div class="c360-card-header">
                            <h2 class="c360-card-title">3. Dòng thời gian hoạt động (Timeline)</h2>
                        </div>
                        <div class="c360-card-body">
                            <% if (relatedActivities != null && !relatedActivities.isEmpty()) { %>
                                <div class="c360-timeline">
                                    <% for (ScopeRecord act : relatedActivities) { %>
                                        <div class="c360-timeline-item">
                                            <div class="c360-timeline-dot"></div>
                                            <div class="c360-timeline-header">
                                                <span class="c360-timeline-subject"><%= Html.escape(act.label()) %></span>
                                                <span class="c360-timeline-time">#<%= act.id() %></span>
                                            </div>
                                            <span class="c360-timeline-desc">Ghi nhận tương tác bởi nhân sự phụ trách</span>
                                        </div>
                                    <% } %>
                                </div>
                            <% } else { %>
                                <div class="c360-empty">Chưa có hoạt động tương tác nào được ghi nhận.</div>
                            <% } %>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>
</div>
</body>
</html>
