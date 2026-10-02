<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,java.net.URLEncoder,java.nio.charset.StandardCharsets,com.crm.model.Organization,com.crm.model.User,com.crm.controller.ServerForms" %>
<%!
private String esc(Object value) {
    if (value == null) return "";
    return value.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
}
private String encoded(Object value) {
    return URLEncoder.encode(value == null ? "" : value.toString(), StandardCharsets.UTF_8);
}
private String regionName(String value) {
    if (value == null) return "Chưa xác định";
    return switch(value.trim().toUpperCase(Locale.ROOT)) {
        case "NORTH" -> "Miền Bắc";
        case "CENTRAL" -> "Miền Trung";
        case "SOUTH" -> "Miền Nam";
        case "NATIONAL" -> "Toàn quốc";
        case "OVERSEAS" -> "Quốc tế";
        default -> value;
    };
}
private String avatarInitials(String name) {
    if (name == null || name.isBlank()) return "NV";
    String[] parts = name.trim().split("\\s+");
    if (parts.length == 1) {
        return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
    }
    return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
}
private boolean isDescendant(long targetId, long candidateId, Map<Long, Organization> unitMap) {
    if (targetId == candidateId) return true;
    Organization current = unitMap.get(candidateId);
    while (current != null && current.getParentId() != null) {
        if (current.getParentId() == targetId) return true;
        current = unitMap.get(current.getParentId());
    }
    return false;
}
%>
<%
if (request.getAttribute("units") == null) {
    response.sendRedirect(request.getContextPath() + "/organization/page");
    return;
}
List<Organization> units = (List<Organization>) request.getAttribute("units");
List<Organization> filtered = (List<Organization>) request.getAttribute("filtered");
Organization selectedUnit = (Organization) request.getAttribute("selectedUnit");
boolean isCreateMode = Boolean.TRUE.equals(request.getAttribute("isCreateMode"));
List<User> allUsers = (List<User>) request.getAttribute("allUsers");
if (allUsers == null) allUsers = List.of();
boolean canManage = Boolean.TRUE.equals(request.getAttribute("canManage"));
String prefix = request.getContextPath();
String q = (String) request.getAttribute("keyword");
String region = (String) request.getAttribute("regionFilter");
String notice = (String) request.getAttribute("notice");
String error = (String) request.getAttribute("error");

Map<Long, Organization> unitMap = new LinkedHashMap<>();
for (Organization u : units) {
    unitMap.put(u.getId(), u);
}
Map<Long, List<Organization>> childMap = new LinkedHashMap<>();
for (Organization u : units) {
    if (u.getParentId() != null) {
        childMap.computeIfAbsent(u.getParentId(), k -> new ArrayList<>()).add(u);
    }
}
%>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Cơ cấu tổ chức kinh doanh - CRM</title>
    <link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/common.css">
    <link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/layout.css">
    <link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/header.css">
    <link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/components.css">
    <link rel="stylesheet" href="<%=esc(prefix)%>/css/organization/organization.css">
</head>
<body class="crm-body">
<jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout">
    <jsp:include page="/jsp/shared/sidebar.jsp"/>
    <main class="crm-page org-shell" id="mainContent">
        <!-- Breadcrumb Navigation -->
        <nav class="crm-breadcrumb" aria-label="Breadcrumb" style="margin-bottom: 12px; font-size: 0.8125rem; color: #64748b;">
            <a href="<%=esc(prefix)%>/dashboard" style="color: #64748b; text-decoration: none;">Quản trị hệ thống</a> /
            <span style="color: #0f172a; font-weight: 500;">Cơ cấu tổ chức kinh doanh</span>
        </nav>

        <!-- Top Header & Action Buttons -->
        <div class="org-header-bar">
            <div class="org-header-title-wrap">
                <h1 class="org-page-title">Cơ cấu Tổ chức kinh doanh &amp; Phân bổ Địa bàn</h1>
                <div class="org-subtitle-row">
                    <span class="crm-badge crm-badge-primary">Sprint 2 • S2-06 (CRM-42)</span>
                    <span class="org-subtitle-text">Sơ đồ cây phân cấp quản lý nhóm, gán trưởng nhóm, phân bổ nhân sự và khu vực địa lý</span>
                </div>
            </div>
            <div class="org-header-actions">
                <% if (canManage) { %>
                <a href="<%=esc(prefix)%>/organization/page?mode=create" class="crm-btn crm-btn-primary" id="btnCreateUnit">+ Thêm nhóm mới</a>
                <% } %>
                <a href="<%=esc(prefix)%>/api/organization/units" download="co-cau-to-chuc.json" class="crm-btn crm-btn-secondary" id="btnExportUnits" title="Xuất dữ liệu cơ cấu tổ chức dưới dạng JSON">Xuất cơ cấu</a>
            </div>
        </div>

        <!-- Feedback Messages -->
        <% if (notice != null && !notice.isBlank()) { %>
        <div class="crm-alert crm-alert-success" role="status">
            <span>&#10003;</span> <span><%=esc(notice)%></span>
        </div>
        <% } %>
        <% if (error != null && !error.isBlank()) { %>
        <div class="crm-alert crm-alert-danger" role="alert">
            <span>&#9888;</span> <span><%=esc(error)%></span>
        </div>
        <% } %>

        <!-- Filter & Search Toolbar -->
        <section class="org-toolbar" aria-label="Bộ lọc tìm kiếm">
            <form class="org-filter-form" method="get" action="<%=esc(prefix)%>/organization/page">
                <div class="org-search-box">
                    <svg class="org-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    </svg>
                    <input type="search" name="q" class="org-search-input" placeholder="Tìm kiếm đơn vị, phòng ban..." value="<%=esc(q)%>" maxlength="150" aria-label="Tìm kiếm đơn vị">
                </div>
                <select name="region" class="org-region-select" aria-label="Lọc theo khu vực">
                    <option value="">Tất cả khu vực</option>
                    <% for (String r : new String[]{"NORTH", "CENTRAL", "SOUTH", "NATIONAL", "OVERSEAS"}) { %>
                    <option value="<%=r%>" <%=r.equals(region) ? "selected" : ""%>><%=esc(regionName(r))%></option>
                    <% } %>
                </select>
                <button type="submit" class="crm-btn crm-btn-primary org-filter-btn">Lọc</button>
                <a href="<%=esc(prefix)%>/organization/page" class="org-reset-link">Làm mới</a>
            </form>
        </section>

        <!-- Two-Column Workspace -->
        <div class="org-workspace">
            <!-- Left Column: Organization Tree View -->
            <section class="org-tree-panel" aria-label="Sơ đồ cây tổ chức">
                <div class="org-panel-header">
                    <h2>Sơ đồ Cây Tổ chức (<%=filtered.size()%> đơn vị)</h2>
                </div>
                <div class="org-tree-body">
                    <% if (filtered.isEmpty()) { %>
                    <div class="org-empty" role="status">
                        <div class="org-empty-icon">&#127970;</div>
                        <h3 style="font-size: 1rem; margin-bottom: 4px;">Không tìm thấy đơn vị kinh doanh nào</h3>
                        <p style="font-size: 0.8125rem;">Vui lòng thử tìm kiếm với từ khóa hoặc khu vực khác.</p>
                    </div>
                    <% } else {
                        /* Identify root units: parentId is null OR parent not in current filtered list */
                        Set<Long> filteredIds = new HashSet<>();
                        for (Organization u : filtered) filteredIds.add(u.getId());

                        List<Organization> roots = new ArrayList<>();
                        for (Organization u : filtered) {
                            if (u.getParentId() == null || !filteredIds.contains(u.getParentId())) {
                                roots.add(u);
                            }
                        }
                    %>
                    <div class="org-tree-root">
                        <% for (Organization root : roots) {
                            List<Organization> children = childMap.get(root.getId());
                            boolean hasChildren = children != null && !children.isEmpty();
                            boolean isSel = selectedUnit != null && selectedUnit.getId() == root.getId();
                        %>
                        <% if (hasChildren) { %>
                        <details open class="org-tree-details">
                            <summary class="org-tree-summary">
                                <a href="<%=esc(prefix)%>/organization/page?selected=<%=root.getId()%>&q=<%=esc(encoded(q))%>&region=<%=esc(encoded(region))%>" class="org-node-card <%=isSel ? "is-selected" : ""%>">
                                    <div class="org-node-top">
                                        <span class="org-node-title">
                                            <span class="org-node-indicator">[-]</span>
                                            <%=esc(root.getName())%>
                                        </span>
                                        <span class="crm-badge <%=root.isActive() ? "crm-badge-success" : "crm-badge-danger"%>"><%=root.isActive() ? "Hoạt động" : "Ngừng hoạt động"%></span>
                                    </div>
                                    <div class="org-node-meta-line">
                                        <span><strong><%=root.getManagerName() != null ? esc(root.getManagerName()) : "Chưa có Lead"%></strong></span>
                                        <span>•</span>
                                        <span><%=root.getMemberCount()%> nhân sự</span>
                                        <span>•</span>
                                        <span class="crm-badge crm-badge-info"><%=esc(regionName(root.getRegion()))%></span>
                                    </div>
                                </a>
                            </summary>
                            <div class="org-children-container">
                                <% for (Organization child : children) {
                                    if (!filteredIds.contains(child.getId())) continue;
                                    List<Organization> grandChildren = childMap.get(child.getId());
                                    boolean hasGrandChildren = grandChildren != null && !grandChildren.isEmpty();
                                    boolean isChildSel = selectedUnit != null && selectedUnit.getId() == child.getId();
                                %>
                                <% if (hasGrandChildren) { %>
                                <details open class="org-tree-details">
                                    <summary class="org-tree-summary">
                                        <a href="<%=esc(prefix)%>/organization/page?selected=<%=child.getId()%>&q=<%=esc(encoded(q))%>&region=<%=esc(encoded(region))%>" class="org-node-card <%=isChildSel ? "is-selected" : ""%>">
                                            <div class="org-node-top">
                                                <span class="org-node-title">
                                                    <span class="org-node-indicator">[-]</span>
                                                    <%=esc(child.getName())%>
                                                </span>
                                                <span class="crm-badge <%=child.isActive() ? "crm-badge-success" : "crm-badge-danger"%>"><%=child.isActive() ? "Hoạt động" : "Ngừng hoạt động"%></span>
                                            </div>
                                            <div class="org-node-meta-line">
                                                <span><strong><%=child.getManagerName() != null ? esc(child.getManagerName()) : "Chưa có Lead"%></strong></span>
                                                <span>•</span>
                                                <span><%=child.getMemberCount()%> nhân sự</span>
                                                <span>•</span>
                                                <span class="crm-badge crm-badge-info"><%=esc(regionName(child.getRegion()))%></span>
                                            </div>
                                        </a>
                                    </summary>
                                    <div class="org-children-container">
                                        <% for (Organization grandChild : grandChildren) {
                                            if (!filteredIds.contains(grandChild.getId())) continue;
                                            boolean isGrandSel = selectedUnit != null && selectedUnit.getId() == grandChild.getId();
                                        %>
                                        <a href="<%=esc(prefix)%>/organization/page?selected=<%=grandChild.getId()%>&q=<%=esc(encoded(q))%>&region=<%=esc(encoded(region))%>" class="org-node-card <%=isGrandSel ? "is-selected" : ""%>">
                                            <div class="org-node-top">
                                                <span class="org-node-title">
                                                    <span class="org-node-indicator">•</span>
                                                    <%=esc(grandChild.getName())%>
                                                </span>
                                                <span class="crm-badge <%=grandChild.isActive() ? "crm-badge-success" : "crm-badge-danger"%>"><%=grandChild.isActive() ? "Hoạt động" : "Ngừng hoạt động"%></span>
                                            </div>
                                            <div class="org-node-meta-line">
                                                <span><strong><%=grandChild.getManagerName() != null ? esc(grandChild.getManagerName()) : "Chưa có Lead"%></strong></span>
                                                <span>•</span>
                                                <span><%=grandChild.getMemberCount()%> nhân sự</span>
                                                <span>•</span>
                                                <span class="crm-badge crm-badge-info"><%=esc(regionName(grandChild.getRegion()))%></span>
                                            </div>
                                        </a>
                                        <% } %>
                                    </div>
                                </details>
                                <% } else { %>
                                <a href="<%=esc(prefix)%>/organization/page?selected=<%=child.getId()%>&q=<%=esc(encoded(q))%>&region=<%=esc(encoded(region))%>" class="org-node-card <%=isChildSel ? "is-selected" : ""%>">
                                    <div class="org-node-top">
                                        <span class="org-node-title">
                                            <span class="org-node-indicator">•</span>
                                            <%=esc(child.getName())%>
                                        </span>
                                        <span class="crm-badge <%=child.isActive() ? "crm-badge-success" : "crm-badge-danger"%>"><%=child.isActive() ? "Hoạt động" : "Ngừng hoạt động"%></span>
                                    </div>
                                    <div class="org-node-meta-line">
                                        <span><strong><%=child.getManagerName() != null ? esc(child.getManagerName()) : "Chưa có Lead"%></strong></span>
                                        <span>•</span>
                                        <span><%=child.getMemberCount()%> nhân sự</span>
                                        <span>•</span>
                                        <span class="crm-badge crm-badge-info"><%=esc(regionName(child.getRegion()))%></span>
                                    </div>
                                </a>
                                <% } %>
                                <% } %>
                            </div>
                        </details>
                        <% } else { %>
                        <a href="<%=esc(prefix)%>/organization/page?selected=<%=root.getId()%>&q=<%=esc(encoded(q))%>&region=<%=esc(encoded(region))%>" class="org-node-card <%=isSel ? "is-selected" : ""%>">
                            <div class="org-node-top">
                                <span class="org-node-title">
                                    <span class="org-node-indicator">•</span>
                                    <%=esc(root.getName())%>
                                </span>
                                <span class="crm-badge <%=root.isActive() ? "crm-badge-success" : "crm-badge-danger"%>"><%=root.isActive() ? "Hoạt động" : "Ngừng hoạt động"%></span>
                            </div>
                            <div class="org-node-meta-line">
                                <span><strong><%=root.getManagerName() != null ? esc(root.getManagerName()) : "Chưa có Lead"%></strong></span>
                                <span>•</span>
                                <span><%=root.getMemberCount()%> nhân sự</span>
                                <span>•</span>
                                <span class="crm-badge crm-badge-info"><%=esc(regionName(root.getRegion()))%></span>
                            </div>
                        </a>
                        <% } %>
                        <% } %>
                    </div>
                    <% } %>
                </div>
            </section>

            <!-- Right Column: Unit Detail & Actions -->
            <section class="org-detail-card" aria-label="Chi tiết đơn vị">
                <% if (isCreateMode) { %>
                <!-- CREATE MODE FORM -->
                <div class="org-detail-top">
                    <div>
                        <h2 class="org-detail-title">Thêm Đơn vị / Nhóm kinh doanh mới</h2>
                        <p style="font-size: 0.8125rem; color: #64748b; margin: 4px 0 0;">Khai báo đơn vị kinh doanh mới trong hệ thống phân cấp CRM</p>
                    </div>
                    <span class="crm-badge crm-badge-info">TẠO MỚI</span>
                </div>

                <form method="post" action="<%=esc(prefix)%>/organization/page" id="formCreateUnit">
                    <input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>">
                    <input type="hidden" name="action" value="create">

                    <div class="org-form-grid">
                        <div class="org-form-group">
                            <label for="createName">Tên Đơn vị / Nhóm kinh doanh *</label>
                            <input id="createName" name="name" maxlength="150" required placeholder="Ví dụ: Nhóm Sales Miền Bắc 01">
                        </div>
                        <div class="org-form-group">
                            <label for="createParentId">Đơn vị cấp cha trực thuộc (Parent Unit)</label>
                            <select id="createParentId" name="parentId">
                                <option value="">-- Không có (Đơn vị cấp gốc / HO) --</option>
                                <% for (Organization candidate : units) { %>
                                <option value="<%=candidate.getId()%>"><%=esc(candidate.getName())%></option>
                                <% } %>
                            </select>
                        </div>
                        <div class="org-form-group">
                            <label for="createManagerId">Trưởng nhóm phụ trách (Team Lead / Manager) *</label>
                            <select id="createManagerId" name="managerId" required>
                                <option value="">-- Chọn Trưởng nhóm phụ trách --</option>
                                <% for (User u : allUsers) {
                                    if (!u.isActive()) continue;
                                    String uLabel = (u.getDisplayName() != null && !u.getDisplayName().isBlank()) ? u.getDisplayName() : u.getFullName();
                                    if (uLabel == null || uLabel.isBlank()) uLabel = u.getUsername();
                                %>
                                <option value="<%=u.getId()%>"><%=esc(uLabel)%> (<%=esc(u.getEmail())%>)</option>
                                <% } %>
                            </select>
                            <span class="org-field-badge">&#10003; Tự động kích hoạt Scope TEAM &amp; Quyền duyệt chiết khấu</span>
                        </div>
                        <div class="org-form-group">
                            <label for="createRegion">Khu vực địa lý phụ trách (Territory Scope) *</label>
                            <select id="createRegion" name="region" required>
                                <% for (String r : new String[]{"NORTH", "CENTRAL", "SOUTH", "NATIONAL", "OVERSEAS"}) { %>
                                <option value="<%=r%>"><%=esc(regionName(r))%></option>
                                <% } %>
                            </select>
                        </div>
                        <div class="org-form-group">
                            <label for="createActive">Trạng thái hoạt động</label>
                            <select id="createActive" name="active">
                                <option value="true" selected>Hoạt động</option>
                                <option value="false">Ngừng hoạt động</option>
                            </select>
                        </div>
                    </div>

                    <div class="org-form-actions-row">
                        <div class="org-action-left-group">
                            <button type="submit" class="crm-btn crm-btn-primary" id="btnSubmitCreate">Tạo nhóm kinh doanh</button>
                            <a href="<%=esc(prefix)%>/organization/page" class="crm-btn crm-btn-secondary">Hủy</a>
                        </div>
                    </div>
                </form>

                <% } else if (selectedUnit != null) {
                    Organization parentUnit = null;
                    if (selectedUnit.getParentId() != null) {
                        parentUnit = unitMap.get(selectedUnit.getParentId());
                    }
                %>
                <!-- UNIT DETAIL / EDIT MODE -->
                <div class="org-detail-top">
                    <div>
                        <h2 class="org-detail-title">Chi tiết Đơn vị: <%=esc(selectedUnit.getName())%></h2>
                        <p style="font-size: 0.8125rem; color: #64748b; margin: 4px 0 0;">
                            Cấp cha: <%=parentUnit != null ? esc(parentUnit.getName()) : "Đơn vị gốc (HO)"%>
                        </p>
                    </div>
                    <span class="crm-badge crm-badge-primary">TEAM-<%=selectedUnit.getId()%></span>
                </div>

                <% if (canManage) { %>
                <!-- Form Update Unit Configuration -->
                <form method="post" action="<%=esc(prefix)%>/organization/page" id="formUpdateUnit">
                    <input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>">
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" name="id" value="<%=selectedUnit.getId()%>">

                    <div class="org-form-grid">
                        <div class="org-form-group">
                            <label for="unitName">Tên Đơn vị / Nhóm kinh doanh *</label>
                            <input id="unitName" name="name" maxlength="150" required value="<%=esc(selectedUnit.getName())%>">
                        </div>
                        <div class="org-form-group">
                            <label for="unitParentId">Đơn vị cấp cha trực thuộc (Parent Unit)</label>
                            <select id="unitParentId" name="parentId">
                                <option value="">-- Không có (Đơn vị cấp gốc / HO) --</option>
                                <% for (Organization candidate : units) {
                                    if (isDescendant(selectedUnit.getId(), candidate.getId(), unitMap)) continue;
                                %>
                                <option value="<%=candidate.getId()%>" <%=selectedUnit.getParentId() != null && selectedUnit.getParentId().equals(candidate.getId()) ? "selected" : ""%>>
                                    <%=esc(candidate.getName())%>
                                </option>
                                <% } %>
                            </select>
                        </div>
                        <div class="org-form-group">
                            <label for="unitManagerId">Trưởng nhóm phụ trách (Team Lead / Manager) *</label>
                            <select id="unitManagerId" name="managerId" required>
                                <% for (User u : allUsers) {
                                    if (!u.isActive()) continue;
                                    String uLabel = (u.getDisplayName() != null && !u.getDisplayName().isBlank()) ? u.getDisplayName() : u.getFullName();
                                    if (uLabel == null || uLabel.isBlank()) uLabel = u.getUsername();
                                    boolean isCurrentMgr = selectedUnit.getManagerId() != null && selectedUnit.getManagerId() == u.getId();
                                %>
                                <option value="<%=u.getId()%>" <%=isCurrentMgr ? "selected" : ""%>>
                                    <%=esc(uLabel)%> (<%=esc(u.getEmail())%>)
                                </option>
                                <% } %>
                            </select>
                            <span class="org-field-badge">&#10003; Tự động kích hoạt Scope TEAM &amp; Quyền duyệt chiết khấu</span>
                        </div>
                        <div class="org-form-group">
                            <label for="unitRegion">Khu vực địa lý phụ trách (Territory Scope) *</label>
                            <select id="unitRegion" name="region" required>
                                <% for (String r : new String[]{"NORTH", "CENTRAL", "SOUTH", "NATIONAL", "OVERSEAS"}) { %>
                                <option value="<%=r%>" <%=r.equals(selectedUnit.getRegion()) ? "selected" : ""%>><%=esc(regionName(r))%></option>
                                <% } %>
                            </select>
                        </div>
                        <div class="org-form-group">
                            <label for="unitActive">Trạng thái hoạt động</label>
                            <select id="unitActive" name="active">
                                <option value="true" <%=selectedUnit.isActive() ? "selected" : ""%>>Hoạt động</option>
                                <option value="false" <%=!selectedUnit.isActive() ? "selected" : ""%>>Ngừng hoạt động</option>
                            </select>
                        </div>
                    </div>

                    <div class="org-form-actions-row">
                        <div class="org-action-left-group">
                            <button type="submit" class="crm-btn crm-btn-primary" id="btnSaveUnit">Lưu cấu hình nhóm</button>
                            <a href="#addMemberCard" class="crm-btn crm-btn-secondary">+ Thêm thành viên</a>
                        </div>
                    </div>
                </form>

                <!-- Form Dissolve / Deactivate Unit -->
                <form method="post" action="<%=esc(prefix)%>/organization/page" id="formDeactivateUnit" style="margin-top: 12px; text-align: right;">
                    <input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>">
                    <input type="hidden" name="action" value="deactivate">
                    <input type="hidden" name="id" value="<%=selectedUnit.getId()%>">
                    <button type="submit" class="crm-btn crm-btn-danger-outline" id="btnDeactivateUnit" title="Vô hiệu hóa đơn vị (soft delete)">Giải thể đơn vị</button>
                </form>
                <% } else { %>
                <!-- Read-only View for non-admin -->
                <div class="org-form-grid" style="margin-bottom: 20px;">
                    <div><strong>Tên đơn vị:</strong> <span><%=esc(selectedUnit.getName())%></span></div>
                    <div><strong>Cấp cha:</strong> <span><%=parentUnit != null ? esc(parentUnit.getName()) : "Đơn vị gốc (HO)"%></span></div>
                    <div><strong>Trưởng nhóm:</strong> <span><%=selectedUnit.getManagerName() != null ? esc(selectedUnit.getManagerName()) : "Chưa có"%> (<%=selectedUnit.getManagerRole() != null ? esc(selectedUnit.getManagerRole()) : "Manager"%>)</span></div>
                    <div><strong>Khu vực địa lý:</strong> <span><%=esc(regionName(selectedUnit.getRegion()))%></span></div>
                    <div><strong>Trạng thái:</strong> <span class="crm-badge <%=selectedUnit.isActive() ? "crm-badge-success" : "crm-badge-danger"%>"><%=selectedUnit.isActive() ? "Hoạt động" : "Ngừng hoạt động"%></span></div>
                </div>
                <% } %>

                <!-- Members Table Section -->
                <div class="org-members-section">
                    <div class="org-section-header">
                        <h3>Danh sách Nhân viên kinh doanh phân bổ trong nhóm (<%=selectedUnit.getMembers().size()%> thành viên)</h3>
                    </div>
                    <div class="org-scroll">
                        <table class="org-table">
                            <thead>
                                <tr>
                                    <th>Nhân viên (Sales Rep)</th>
                                    <th>Email công ty</th>
                                    <th>Chỉ tiêu Q4</th>
                                    <th>Phạm vi dữ liệu</th>
                                    <% if (canManage) { %><th>Thao tác</th><% } %>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (selectedUnit.getMembers().isEmpty()) { %>
                                <tr>
                                    <td colspan="<%=canManage ? 5 : 4%>" style="text-align: center; color: #64748b; padding: 24px;">
                                        Chưa có nhân viên nào được phân bổ trong nhóm này.
                                    </td>
                                </tr>
                                <% } else {
                                    for (Organization.Member m : selectedUnit.getMembers()) {
                                        boolean isLeader = selectedUnit.getManagerId() != null && selectedUnit.getManagerId() == m.getId();
                                %>
                                <tr>
                                    <td>
                                        <div class="org-member-profile">
                                            <span class="org-avatar"><%=esc(avatarInitials(m.getName()))%></span>
                                            <div>
                                                <strong><%=esc(m.getName())%></strong>
                                                <div class="org-member-role"><%=esc(m.getRole() != null ? m.getRole() : "Sales Rep")%></div>
                                            </div>
                                        </div>
                                    </td>
                                    <td><%=esc(m.getEmail())%></td>
                                    <td>500.000.000 đ</td>
                                    <td>
                                        <span class="crm-badge <%=isLeader ? "crm-badge-primary" : "crm-badge-neutral"%>">
                                            <%=isLeader ? "CỦA NHÓM" : "CỦA TÔI"%>
                                        </span>
                                    </td>
                                    <% if (canManage) { %>
                                    <td>
                                        <% if (!isLeader) { %>
                                        <form method="post" action="<%=esc(prefix)%>/organization/page" style="display: inline;">
                                            <input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>">
                                            <input type="hidden" name="action" value="remove_member">
                                            <input type="hidden" name="unitId" value="<%=selectedUnit.getId()%>">
                                            <input type="hidden" name="userId" value="<%=m.getId()%>">
                                            <button type="submit" class="crm-btn crm-btn-danger-outline" style="padding: 4px 8px; font-size: 0.75rem;" title="Gỡ nhân viên khỏi nhóm">Xóa khỏi nhóm</button>
                                        </form>
                                        <% } else { %>
                                        <span class="crm-badge crm-badge-info">Trưởng nhóm</span>
                                        <% } %>
                                    </td>
                                    <% } %>
                                </tr>
                                <% } } %>
                            </tbody>
                        </table>
                    </div>

                    <!-- Add Member Section (If Admin) -->
                    <% if (canManage) {
                        Set<Long> existingMemberIds = new HashSet<>();
                        for (Organization.Member m : selectedUnit.getMembers()) {
                            existingMemberIds.add(m.getId());
                        }
                    %>
                    <div class="org-add-member-card" id="addMemberCard">
                        <form method="post" action="<%=esc(prefix)%>/organization/page" class="org-add-member-form">
                            <input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>">
                            <input type="hidden" name="action" value="assign_member">
                            <input type="hidden" name="unitId" value="<%=selectedUnit.getId()%>">

                            <label for="assignUserId">
                                Phân bổ thêm nhân viên vào nhóm:
                                <select id="assignUserId" name="userId" required>
                                    <option value="">-- Chọn nhân sự để gán vào nhóm --</option>
                                    <% for (User u : allUsers) {
                                        if (!u.isActive() || existingMemberIds.contains(u.getId())) continue;
                                        String uLabel = (u.getDisplayName() != null && !u.getDisplayName().isBlank()) ? u.getDisplayName() : u.getFullName();
                                        if (uLabel == null || uLabel.isBlank()) uLabel = u.getUsername();
                                    %>
                                    <option value="<%=u.getId()%>"><%=esc(uLabel)%> (<%=esc(u.getEmail())%>)</option>
                                    <% } %>
                                </select>
                            </label>
                            <button type="submit" class="crm-btn crm-btn-secondary" id="btnAssignMember">+ Thêm vào nhóm</button>
                        </form>
                    </div>
                    <% } %>
                </div>
                <% } else { %>
                <div class="org-empty" role="status">
                    <div class="org-empty-icon">&#128065;</div>
                    <h3>Chưa chọn đơn vị kinh doanh</h3>
                    <p>Vui lòng chọn một đơn vị từ cây tổ chức bên trái để xem và quản lý chi tiết.</p>
                </div>
                <% } %>
            </section>
        </div>

        <!-- Spec Feature Cards (Bottom Section from image11.png) -->
        <section class="org-spec-cards" aria-label="Đặc tả quy chuẩn nghiệp vụ">
            <div class="org-spec-card">
                <h3>1. Sơ đồ cây phân cấp (Tree Hierarchy)</h3>
                <p>Quản lý đa cấp (Công ty &rarr; Vùng &rarr; Nhóm). Kiểm tra chống lặp vòng cây (Cycle Prevention) tự động khi cập nhật đơn vị cấp trên trực thuộc.</p>
            </div>
            <div class="org-spec-card">
                <h3>2. Gán Trưởng nhóm &amp; Quyền duyệt (Team Lead Role)</h3>
                <p>Tự động kích hoạt quyền Scope TEAM cho Trưởng nhóm. Được phép kiểm duyệt chiết khấu và theo dõi tiến độ của các thành viên trực thuộc nhóm.</p>
            </div>
            <div class="org-spec-card">
                <h3>3. Phân bổ Nhân viên &amp; Địa bàn (Territory Scope)</h3>
                <p>Gán Sales Rep vào nhóm quản lý, định nghĩa vùng địa lý (Bắc, Trung, Nam, Toàn quốc, Quốc tế) để đồng bộ hóa phạm vi dữ liệu khách hàng &amp; cơ hội.</p>
            </div>
            <div class="org-spec-card">
                <h3>4. Đồng bộ Master Sidebar &amp; API (System Unity)</h3>
                <p>Đồng bộ menu tại <em>Cơ cấu tổ chức kinh doanh</em> (<code>/organization</code>). Hỗ trợ đầy đủ REST API: <code>GET/POST /api/organization/units</code>, <code>PUT/DELETE /api/organization/units/{id}</code>.</p>
            </div>
        </section>
    </main>
</div>
</body>
</html>
