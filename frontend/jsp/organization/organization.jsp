<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  ==========================================================================
  FEATURE S2-06: Cây tổ chức bán hàng (Sales Organization Tree)
  Phụ trách Frontend/View: Tiệp
  
  BE CONTRACT NEEDED:
    1. GET /sales-org/teams
       - Query params: search, geography, status
       - Response dự kiến:
         {
           "teams": [
             {
               "id": 1,
               "code": "TEAM-HN",
               "name": "Team Hà Nội",
               "leader": { "id": 10, "employeeCode": "NV001", "fullName": "Nguyễn Văn A" },
               "geography": "NORTH",
               "status": "ACTIVE",
               "memberCount": 5
             }
           ]
         }

    2. GET /sales-org/teams/{teamId}/members
       - Response dự kiến:
         {
           "teamId": 1,
           "members": [
             {
               "id": 11,
               "employeeCode": "NV002",
               "fullName": "Nguyễn Văn B",
               "role": "SALES_REP",
               "teamId": 1,
               "geography": "NORTH",
               "status": "ACTIVE"
             }
           ]
         }

    3. GET /sales-org/employees/{employeeId}
       - Response dự kiến:
         {
           "id": 11,
           "employeeCode": "NV002",
           "fullName": "Nguyễn Văn B",
           "role": "SALES_REP",
           "team": { "id": 1, "name": "Team Hà Nội" },
           "leader": { "id": 10, "fullName": "Nguyễn Văn A" },
           "geography": "NORTH",
           "status": "ACTIVE"
         }
  ==========================================================================
--%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cây tổ chức bán hàng - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng biệt của module Sales Organization Tree (S2-06) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization/organization.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Cây tổ chức bán hàng -->
        <main class="org-page" id="salesOrgApp" role="main">
            <div class="org-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="org-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Hệ thống</span>
                    <span class="separator">/</span>
                    <span class="active">Cây tổ chức bán hàng</span>
                </nav>

                <!-- Header màn hình -->
                <header class="org-header">
                    <div class="org-header-info">
                        <h1>Cây tổ chức bán hàng</h1>
                        <p>Quản lý đội ngũ, trưởng nhóm và phạm vi khu vực bán hàng.</p>
                    </div>
                    <div class="org-header-badges">
                        <span class="org-badge-tag">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                <circle cx="9" cy="7" r="4"></circle>
                                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                            </svg>
                            S2-06 / Sales Org Tree
                        </span>
                        <span class="org-badge-tag" style="background: #f0fdf4; color: #16a34a; border-color: #bbf7d0;">
                            Phân cấp khu vực
                        </span>
                    </div>
                </header>

                <!-- Thống kê sơ bộ (KPI Cards) -->
                <section class="org-stats-grid" aria-label="Thống kê tổng quan tổ chức bán hàng">
                    <div class="org-stat-card">
                        <div class="org-stat-icon org-stat-icon--blue" aria-hidden="true">👥</div>
                        <div class="org-stat-data">
                            <span class="org-stat-val" id="statTotalTeams">5</span>
                            <span class="org-stat-lbl">Tổng số Đội ngũ (Teams)</span>
                        </div>
                    </div>
                    <div class="org-stat-card">
                        <div class="org-stat-icon org-stat-icon--green" aria-hidden="true">👤</div>
                        <div class="org-stat-data">
                            <span class="org-stat-val" id="statTotalEmployees">24</span>
                            <span class="org-stat-lbl">Tổng số nhân sự</span>
                        </div>
                    </div>
                    <div class="org-stat-card">
                        <div class="org-stat-icon org-stat-icon--purple" aria-hidden="true">👑</div>
                        <div class="org-stat-data">
                            <span class="org-stat-val" id="statTotalLeaders">5</span>
                            <span class="org-stat-lbl">Trưởng nhóm (Team Leaders)</span>
                        </div>
                    </div>
                    <div class="org-stat-card">
                        <div class="org-stat-icon org-stat-icon--amber" aria-hidden="true">📍</div>
                        <div class="org-stat-data">
                            <span class="org-stat-val" id="statTotalGeographies">3</span>
                            <span class="org-stat-lbl">Khu vực địa lý (Geography)</span>
                        </div>
                    </div>
                </section>

                <!-- Khung chính hiển thị Cây tổ chức và Bộ lọc -->
                <div class="org-card">

                    <!-- Toolbar tìm kiếm và bộ lọc đa tiêu chí -->
                    <div class="org-toolbar">
                        <div class="org-toolbar-left">
                            <!-- Ô tìm kiếm -->
                            <div class="org-search-wrap">
                                <svg class="org-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                </svg>
                                <input type="search" id="orgSearchInput" class="org-search-input"
                                       placeholder="Tìm theo tên NV, mã NV hoặc tên Team..."
                                       aria-label="Tìm kiếm nhân viên hoặc team">
                            </div>

                            <!-- Lọc theo Team -->
                            <select id="filterTeam" class="org-filter-select" aria-label="Lọc theo Team">
                                <option value="">Tất cả Đội ngũ (Teams)</option>
                                <option value="TEAM-HN">Team Hà Nội</option>
                                <option value="TEAM-TN">Team Thái Nguyên</option>
                                <option value="TEAM-HP">Team Hải Phòng</option>
                                <option value="TEAM-DN">Team Đà Nẵng</option>
                                <option value="TEAM-HCM">Team TP. Hồ Chí Minh</option>
                            </select>

                            <!-- Lọc theo Geography -->
                            <select id="filterGeo" class="org-filter-select" aria-label="Lọc theo khu vực địa lý">
                                <option value="">Tất cả khu vực</option>
                                <option value="NORTH">Miền Bắc</option>
                                <option value="CENTRAL">Miền Trung</option>
                                <option value="SOUTH">Miền Nam</option>
                            </select>

                            <!-- Lọc theo Role -->
                            <select id="filterRole" class="org-filter-select" aria-label="Lọc theo vai trò">
                                <option value="">Tất cả vai trò</option>
                                <option value="TEAM_LEAD">Trưởng nhóm (Team Lead)</option>
                                <option value="SALES_REP">Nhân viên kinh doanh (Sales Rep)</option>
                                <option value="SENIOR_SALES_REP">Senior Sales Rep</option>
                            </select>

                            <!-- Lọc theo Status -->
                            <select id="filterStatus" class="org-filter-select" aria-label="Lọc theo trạng thái">
                                <option value="">Tất cả trạng thái</option>
                                <option value="ACTIVE">Đang hoạt động (Active)</option>
                                <option value="INACTIVE">Tạm ngưng (Inactive)</option>
                            </select>

                            <!-- Nút Đặt lại bộ lọc -->
                            <button type="button" class="btn-org btn-org-secondary" id="btnResetFilter" onclick="resetFilters()">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="1 4 1 10 7 10"></polyline>
                                    <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                                </svg>
                                <span>Đặt lại</span>
                            </button>
                        </div>

                        <div class="org-toolbar-right">
                            <button type="button" class="btn-org btn-org-secondary" onclick="expandAllTeams()">
                                <span>Mở rộng tất cả</span>
                            </button>
                            <button type="button" class="btn-org btn-org-secondary" onclick="collapseAllTeams()">
                                <span>Thu gọn tất cả</span>
                            </button>
                        </div>
                    </div>

                    <!-- Khu vực hiển thị Cây tổ chức (Tree & Hierarchy Cards) -->
                    <div class="org-tree-wrapper">

                        <!-- LEVEL 1: ROOT NODE - Sales Organization -->
                        <div class="org-root-card">
                            <div class="org-root-content">
                                <div class="org-root-icon" aria-hidden="true">🏢</div>
                                <div>
                                    <h2 class="org-root-title">Khối Kinh Doanh (Sales Organization)</h2>
                                    <p class="org-root-desc">Tổng công ty CRM ICTU • Điều hành toàn diện hoạt động kinh doanh đa vùng</p>
                                </div>
                            </div>
                            <div class="org-root-badges">
                                <span class="org-root-badge">Cấp độ 1 • Toàn quốc</span>
                            </div>
                        </div>

                        <!-- Đường nhánh liên kết (Connector) -->
                        <div class="org-tree-line" aria-hidden="true"></div>

                        <!-- LEVEL 2 & 3: DANH SÁCH TEAMS VÀ MEMBERS -->
                        <div class="org-teams-list" id="orgTeamsContainer">
                            <!-- Sẽ được render động qua JavaScript từ Mock Data / API -->
                        </div>

                        <!-- Empty State khi không có kết quả lọc -->
                        <div class="org-empty-state" id="orgEmptyState" style="display: none;">
                            <div class="org-empty-icon" aria-hidden="true">🔍</div>
                            <div class="org-empty-title">Không tìm thấy đội ngũ hoặc nhân sự nào</div>
                            <p class="org-empty-desc">Không có kết quả phù hợp với từ khóa hoặc điều kiện lọc hiện tại. Hãy thử thay đổi bộ lọc.</p>
                            <button type="button" class="btn-org btn-org-secondary" onclick="resetFilters()">Đặt lại bộ lọc</button>
                        </div>

                    </div>
                </div>

            </div>
        </main>
    </div>

    <!-- MODAL 1: CHI TIẾT ĐỘI NGŨ (TEAM DETAIL MODAL) -->
    <div class="org-modal-overlay" id="modalTeamDetail" role="dialog" aria-modal="true" aria-labelledby="teamDetailTitle">
        <div class="org-modal">
            <div class="org-modal-header">
                <h3 class="org-modal-title" id="teamDetailTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="9" cy="7" r="4"></circle>
                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                    </svg>
                    Chi tiết Đội ngũ bán hàng
                </h3>
                <button type="button" class="org-modal-close" onclick="closeTeamDetailModal()" aria-label="Đóng">&times;</button>
            </div>
            <div class="org-modal-body">
                <div class="org-detail-grid">
                    <span class="org-detail-label">Tên Đội ngũ:</span>
                    <span class="org-detail-val" id="mdlTeamName" style="font-weight: 700; color: var(--org-primary); font-size: 1.05rem;"></span>

                    <span class="org-detail-label">Mã Đội ngũ:</span>
                    <span class="org-detail-val" id="mdlTeamCode" style="font-weight: 600;"></span>

                    <span class="org-detail-label">Khu vực (Geo):</span>
                    <span class="org-detail-val" id="mdlTeamGeo"></span>

                    <span class="org-detail-label">Trưởng nhóm:</span>
                    <span class="org-detail-val" id="mdlTeamLeader" style="font-weight: 700; color: #b45309;"></span>

                    <span class="org-detail-label">Số thành viên:</span>
                    <span class="org-detail-val" id="mdlTeamCount" style="font-weight: 600;"></span>

                    <span class="org-detail-label">Trạng thái:</span>
                    <span class="org-detail-val" id="mdlTeamStatus"></span>
                </div>

                <div style="margin-top: 10px;">
                    <h4 style="font-size: 0.95rem; margin-bottom: 8px; color: var(--org-text-main);">Danh sách thành viên thuộc Team:</h4>
                    <div style="max-height: 240px; overflow-y: auto; border: 1px solid var(--org-border); border-radius: 8px;">
                        <table class="org-modal-table">
                            <thead>
                                <tr>
                                    <th>Mã NV</th>
                                    <th>Họ và tên</th>
                                    <th>Vai trò</th>
                                    <th>Khu vực</th>
                                    <th style="text-align: center;">Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody id="mdlTeamMembersList">
                                <!-- Render danh sách thành viên -->
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            <div class="org-modal-footer">
                <button type="button" class="btn-org btn-org-secondary" onclick="closeTeamDetailModal()">Đóng</button>
            </div>
        </div>
    </div>

    <!-- MODAL 2: CHI TIẾT NHÂN VIÊN (EMPLOYEE DETAIL MODAL) -->
    <div class="org-modal-overlay" id="modalEmployeeDetail" role="dialog" aria-modal="true" aria-labelledby="empDetailTitle">
        <div class="org-modal" style="max-width: 500px;">
            <div class="org-modal-header">
                <h3 class="org-modal-title" id="empDetailTitle">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="7" r="4"></circle>
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    </svg>
                    Thông tin nhân viên bán hàng
                </h3>
                <button type="button" class="org-modal-close" onclick="closeEmployeeDetailModal()" aria-label="Đóng">&times;</button>
            </div>
            <div class="org-modal-body">
                <div style="display: flex; align-items: center; gap: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--org-border-subtle);">
                    <div class="org-member-avatar" id="mdlEmpAvatar" style="width: 52px; height: 52px; font-size: 1.2rem;"></div>
                    <div>
                        <div id="mdlEmpName" style="font-size: 1.15rem; font-weight: 700; color: var(--org-text-main);"></div>
                        <div id="mdlEmpRole" style="margin-top: 4px;"></div>
                    </div>
                </div>

                <div class="org-detail-grid">
                    <span class="org-detail-label">Mã nhân viên:</span>
                    <span class="org-detail-val" id="mdlEmpCode" style="font-weight: 700; color: var(--org-primary);"></span>

                    <span class="org-detail-label">Đội ngũ (Team):</span>
                    <span class="org-detail-val" id="mdlEmpTeam" style="font-weight: 600;"></span>

                    <span class="org-detail-label">Trưởng nhóm:</span>
                    <span class="org-detail-val" id="mdlEmpLeader" style="font-weight: 600; color: #b45309;"></span>

                    <span class="org-detail-label">Khu vực:</span>
                    <span class="org-detail-val" id="mdlEmpGeo"></span>

                    <span class="org-detail-label">Trạng thái:</span>
                    <span class="org-detail-val" id="mdlEmpStatus"></span>

                    <span class="org-detail-label">Email:</span>
                    <span class="org-detail-val" id="mdlEmpEmail" style="color: var(--org-text-sub);"></span>

                    <span class="org-detail-label">Số điện thoại:</span>
                    <span class="org-detail-val" id="mdlEmpPhone" style="color: var(--org-text-sub);"></span>
                </div>
            </div>
            <div class="org-modal-footer">
                <button type="button" class="btn-org btn-org-secondary" onclick="closeEmployeeDetailModal()">Đóng</button>
            </div>
        </div>
    </div>

    <!-- JAVASCRIPT XỬ LÝ FRONTEND MOCK DATA & CÂY TỔ CHỨC (S2-06) -->
    <script>
        // Dữ liệu Mock chuẩn đặc tả S2-06
        // Khi BE triển khai các endpoint /sales-org/teams, dữ liệu sẽ được liên kết đồng bộ
        const salesOrgData = {
            teams: [
                {
                    id: 1,
                    code: "TEAM-HN",
                    name: "Team Hà Nội",
                    geography: "NORTH",
                    status: "ACTIVE",
                    leader: {
                        id: 101,
                        employeeCode: "NV001",
                        fullName: "Nguyễn Văn A",
                        role: "TEAM_LEAD",
                        email: "nguyenvana@crm.vn",
                        phone: "0912 345 678",
                        status: "ACTIVE"
                    },
                    members: [
                        { id: 102, employeeCode: "NV002", fullName: "Nguyễn Văn B", role: "SALES_REP", email: "nguyenvanb@crm.vn", phone: "0912 345 679", status: "ACTIVE" },
                        { id: 103, employeeCode: "NV003", fullName: "Trần Văn C", role: "SALES_REP", email: "tranvanc@crm.vn", phone: "0912 345 680", status: "ACTIVE" },
                        { id: 104, employeeCode: "NV004", fullName: "Lê Văn D", role: "SENIOR_SALES_REP", email: "levand@crm.vn", phone: "0912 345 681", status: "ACTIVE" },
                        { id: 105, employeeCode: "NV005", fullName: "Hoàng Thị Mai", role: "SALES_REP", email: "hoangthimai@crm.vn", phone: "0912 345 682", status: "INACTIVE" }
                    ]
                },
                {
                    id: 2,
                    code: "TEAM-TN",
                    name: "Team Thái Nguyên",
                    geography: "NORTH",
                    status: "ACTIVE",
                    leader: {
                        id: 106,
                        employeeCode: "NV006",
                        fullName: "Phạm Văn E",
                        role: "TEAM_LEAD",
                        email: "phamvane@crm.vn",
                        phone: "0912 345 683",
                        status: "ACTIVE"
                    },
                    members: [
                        { id: 107, employeeCode: "NV007", fullName: "Đỗ Quốc Bảo", role: "SALES_REP", email: "doquocbao@crm.vn", phone: "0912 345 684", status: "ACTIVE" },
                        { id: 108, employeeCode: "NV008", fullName: "Trịnh Thùy Linh", role: "SALES_REP", email: "trinhthuylinh@crm.vn", phone: "0912 345 685", status: "ACTIVE" },
                        { id: 109, employeeCode: "NV009", fullName: "Ngô Quang Huy", role: "SALES_REP", email: "ngoquanghuy@crm.vn", phone: "0912 345 686", status: "ACTIVE" }
                    ]
                },
                {
                    id: 3,
                    code: "TEAM-HP",
                    name: "Team Hải Phòng",
                    geography: "NORTH",
                    status: "ACTIVE",
                    leader: {
                        id: 110,
                        employeeCode: "NV010",
                        fullName: "Vũ Thị H",
                        role: "TEAM_LEAD",
                        email: "vuthih@crm.vn",
                        phone: "0912 345 687",
                        status: "ACTIVE"
                    },
                    members: [
                        { id: 111, employeeCode: "NV011", fullName: "Bùi Minh Khôi", role: "SENIOR_SALES_REP", email: "buiminhkhoi@crm.vn", phone: "0912 345 688", status: "ACTIVE" },
                        { id: 112, employeeCode: "NV012", fullName: "Lâm Hải Yến", role: "SALES_REP", email: "lamhaiyen@crm.vn", phone: "0912 345 689", status: "ACTIVE" },
                        { id: 113, employeeCode: "NV013", fullName: "Dương Tuấn Anh", role: "SALES_REP", email: "duongtuananh@crm.vn", phone: "0912 345 690", status: "ACTIVE" }
                    ]
                },
                {
                    id: 4,
                    code: "TEAM-DN",
                    name: "Team Đà Nẵng",
                    geography: "CENTRAL",
                    status: "ACTIVE",
                    leader: {
                        id: 114,
                        employeeCode: "NV014",
                        fullName: "Hoàng Văn M",
                        role: "TEAM_LEAD",
                        email: "hoangvanm@crm.vn",
                        phone: "0912 345 691",
                        status: "ACTIVE"
                    },
                    members: [
                        { id: 115, employeeCode: "NV015", fullName: "Phan Đình Trọng", role: "SALES_REP", email: "phandinhtrong@crm.vn", phone: "0912 345 692", status: "ACTIVE" },
                        { id: 116, employeeCode: "NV016", fullName: "Lê Thị Cẩm Tú", role: "SENIOR_SALES_REP", email: "lethicamtu@crm.vn", phone: "0912 345 693", status: "ACTIVE" },
                        { id: 117, employeeCode: "NV017", fullName: "Võ Thành Nam", role: "SALES_REP", email: "vothanhnam@crm.vn", phone: "0912 345 694", status: "ACTIVE" },
                        { id: 118, employeeCode: "NV018", fullName: "Nguyễn Hải Đăng", role: "SALES_REP", email: "nguyenhaidang@crm.vn", phone: "0912 345 695", status: "ACTIVE" }
                    ]
                },
                {
                    id: 5,
                    code: "TEAM-HCM",
                    name: "Team TP. Hồ Chí Minh",
                    geography: "SOUTH",
                    status: "ACTIVE",
                    leader: {
                        id: 119,
                        employeeCode: "NV019",
                        fullName: "Lê Quốc T",
                        role: "TEAM_LEAD",
                        email: "lequoct@crm.vn",
                        phone: "0912 345 696",
                        status: "ACTIVE"
                    },
                    members: [
                        { id: 120, employeeCode: "NV020", fullName: "Trương Mỹ Nhân", role: "SENIOR_SALES_REP", email: "truongmynhan@crm.vn", phone: "0912 345 697", status: "ACTIVE" },
                        { id: 121, employeeCode: "NV021", fullName: "Đặng Hữu Phúc", role: "SALES_REP", email: "danghuuphuc@crm.vn", phone: "0912 345 698", status: "ACTIVE" },
                        { id: 122, employeeCode: "NV022", fullName: "Nguyễn Kim Ngân", role: "SALES_REP", email: "nguyenkimngan@crm.vn", phone: "0912 345 699", status: "ACTIVE" },
                        { id: 123, employeeCode: "NV023", fullName: "Hồ Hoàng Long", role: "SALES_REP", email: "hohoanglong@crm.vn", phone: "0912 345 700", status: "ACTIVE" },
                        { id: 124, employeeCode: "NV024", fullName: "Tạ Thị Thanh Thảo", role: "SALES_REP", email: "tathithanhthao@crm.vn", phone: "0912 345 701", status: "INACTIVE" }
                    ]
                }
            ]
        };

        // Format Geography Badge
        function getGeoBadge(geo) {
            switch(geo) {
                case "NORTH":
                    return '<span class="badge-geo badge-geo-north">📍 Miền Bắc</span>';
                case "CENTRAL":
                    return '<span class="badge-geo badge-geo-central">📍 Miền Trung</span>';
                case "SOUTH":
                    return '<span class="badge-geo badge-geo-south">📍 Miền Nam</span>';
                default:
                    return '<span class="badge-geo">' + geo + '</span>';
            }
        }

        // Format Status Badge
        function getStatusBadge(status) {
            return status === "ACTIVE"
                ? '<span class="badge-status badge-status-active">● Hoạt động</span>'
                : '<span class="badge-status badge-status-inactive">○ Tạm ngưng</span>';
        }

        // Format Role Badge
        function getRoleBadge(role) {
            switch(role) {
                case "TEAM_LEAD":
                    return '<span class="badge-status badge-role-lead">👑 Trưởng nhóm</span>';
                case "SENIOR_SALES_REP":
                    return '<span class="badge-status" style="background:#f3e8ff; color:#7e22ce; border:1px solid #d8b4fe;">⭐ Senior Sales Rep</span>';
                default:
                    return '<span class="badge-status badge-role-rep">💼 Sales Rep</span>';
            }
        }

        // Lấy chữ cái viết tắt avatar (e.g. "Nguyễn Văn B" -> "NB")
        function getInitials(fullName) {
            if (!fullName) return "NV";
            const parts = fullName.trim().split(" ");
            if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
            return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
        }

        // Cập nhật thống kê sơ bộ
        function updateKPIs() {
            const teams = salesOrgData.teams;
            let totalEmployees = 0;
            let totalLeaders = teams.length;
            const geos = new Set();

            teams.forEach(t => {
                totalEmployees += (1 + t.members.length); // 1 leader + members
                geos.add(t.geography);
            });

            document.getElementById("statTotalTeams").textContent = teams.length;
            document.getElementById("statTotalEmployees").textContent = totalEmployees;
            document.getElementById("statTotalLeaders").textContent = totalLeaders;
            document.getElementById("statTotalGeographies").textContent = geos.size;
        }

        // Render Cây tổ chức
        function renderOrgTree() {
            const container = document.getElementById("orgTeamsContainer");
            const emptyState = document.getElementById("orgEmptyState");

            const searchKw = (document.getElementById("orgSearchInput").value || "").trim().toLowerCase();
            const filterTeamVal = document.getElementById("filterTeam").value;
            const filterGeoVal = document.getElementById("filterGeo").value;
            const filterRoleVal = document.getElementById("filterRole").value;
            const filterStatusVal = document.getElementById("filterStatus").value;

            let visibleTeamsCount = 0;
            let html = "";

            salesOrgData.teams.forEach(t => {
                // Kiểm tra filter theo Team
                if (filterTeamVal && t.code !== filterTeamVal) return;

                // Kiểm tra filter theo Geography
                if (filterGeoVal && t.geography !== filterGeoVal) return;

                // Tập hợp tất cả nhân sự của Team (Leader + Members)
                const allPersonsInTeam = [
                    { ...t.leader, isLeader: true },
                    ...t.members.map(m => ({ ...m, isLeader: false }))
                ];

                // Lọc theo search, role, status
                const matchedPersons = allPersonsInTeam.filter(p => {
                    const matchKw = !searchKw || 
                        p.fullName.toLowerCase().includes(searchKw) || 
                        p.employeeCode.toLowerCase().includes(searchKw) ||
                        t.name.toLowerCase().includes(searchKw) ||
                        t.code.toLowerCase().includes(searchKw);

                    const matchRole = !filterRoleVal || p.role === filterRoleVal;
                    const matchStatus = !filterStatusVal || p.status === filterStatusVal;

                    return matchKw && matchRole && matchStatus;
                });

                // Nếu có tiêu chí lọc nhân sự mà Team không có ai khớp -> ẩn Team
                if (matchedPersons.length === 0) return;

                visibleTeamsCount++;
                const isAutoExpand = !!(searchKw || filterTeamVal || filterGeoVal || filterRoleVal || filterStatusVal);
                const totalMembersCount = 1 + t.members.length;

                html += `
                    <div class="org-team-card ${isAutoExpand ? 'expanded' : ''}" id="teamCard-${t.id}">
                        <!-- Header Đội ngũ -->
                        <div class="org-team-header" onclick="toggleTeamExpand(${t.id})">
                            <div class="org-team-left">
                                <button type="button" class="org-toggle-btn" aria-label="Đóng mở danh sách thành viên">
                                    ▶
                                </button>
                                <div class="org-team-info">
                                    <div class="org-team-name">
                                        <span>${t.name}</span>
                                        <span style="font-size: 0.78rem; font-weight: 600; color: var(--org-primary); background: var(--org-primary-light); padding: 2px 6px; border-radius: 4px;">${t.code}</span>
                                        ${getGeoBadge(t.geography)}
                                        ${getStatusBadge(t.status)}
                                    </div>
                                    <div class="org-team-meta">
                                        <span>Phụ trách khu vực ${t.geography === 'NORTH' ? 'Miền Bắc' : t.geography === 'CENTRAL' ? 'Miền Trung' : 'Miền Nam'}</span>
                                    </div>
                                </div>
                            </div>

                            <!-- Hộp nổi bật Trưởng nhóm -->
                            <div class="org-team-leader-box" onclick="event.stopPropagation(); openEmployeeDetail(${t.leader.id}, ${t.id})" title="Xem chi tiết Trưởng nhóm">
                                <span class="org-leader-crown" aria-hidden="true">👑</span>
                                <div class="org-leader-info">
                                    <span class="org-leader-title">Trưởng nhóm (Team Lead)</span>
                                    <span class="org-leader-name">${t.leader.fullName} (${t.leader.employeeCode})</span>
                                </div>
                            </div>

                            <div class="org-team-right">
                                <span class="org-member-count-badge">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                        <circle cx="9" cy="7" r="4"></circle>
                                    </svg>
                                    ${totalMembersCount} thành viên
                                </span>
                                <button type="button" class="btn-org btn-org-secondary" style="padding: 5px 10px; font-size: 0.8rem;" onclick="event.stopPropagation(); openTeamDetail(${t.id})">
                                    Chi tiết Team
                                </button>
                            </div>
                        </div>

                        <!-- Danh sách thành viên cấp 3 -->
                        <div class="org-team-body">
                            <div style="font-size: 0.8rem; font-weight: 600; color: var(--org-text-muted); margin-bottom: 10px; text-transform: uppercase; letter-spacing: 0.03em;">
                                Thành viên thuộc đội ngũ (${matchedPersons.length} người hiển thị)
                            </div>
                            <div class="org-members-grid">
                                ${matchedPersons.map(p => `
                                    <div class="org-member-card ${p.isLeader ? 'org-member-card--leader' : ''}" onclick="openEmployeeDetail(${p.id}, ${t.id})">
                                        <div class="org-member-avatar">${getInitials(p.fullName)}</div>
                                        <div class="org-member-details">
                                            <div class="org-member-name" title="${p.fullName}">${p.fullName}</div>
                                            <div class="org-member-meta">
                                                <span>${p.employeeCode}</span>
                                                <span>•</span>
                                                <span>${p.phone}</span>
                                            </div>
                                            <div class="org-member-badges">
                                                ${getRoleBadge(p.role)}
                                                ${getStatusBadge(p.status)}
                                            </div>
                                        </div>
                                    </div>
                                `).join('')}
                            </div>
                        </div>
                    </div>
                `;
            });

            if (visibleTeamsCount === 0) {
                container.innerHTML = "";
                emptyState.style.display = "flex";
            } else {
                emptyState.style.display = "none";
                container.innerHTML = html;
            }
        }

        // Đóng mở từng Team Card
        function toggleTeamExpand(teamId) {
            const card = document.getElementById("teamCard-" + teamId);
            if (card) {
                card.classList.toggle("expanded");
            }
        }

        // Mở rộng tất cả
        function expandAllTeams() {
            document.querySelectorAll(".org-team-card").forEach(c => c.classList.add("expanded"));
        }

        // Thu gọn tất cả
        function collapseAllTeams() {
            document.querySelectorAll(".org-team-card").forEach(c => c.classList.remove("expanded"));
        }

        // Đặt lại bộ lọc
        function resetFilters() {
            document.getElementById("orgSearchInput").value = "";
            document.getElementById("filterTeam").value = "";
            document.getElementById("filterGeo").value = "";
            document.getElementById("filterRole").value = "";
            document.getElementById("filterStatus").value = "";
            renderOrgTree();
        }

        // Modal Chi tiết Team
        function openTeamDetail(teamId) {
            const team = salesOrgData.teams.find(t => t.id === teamId);
            if (!team) return;

            document.getElementById("mdlTeamName").textContent = team.name;
            document.getElementById("mdlTeamCode").textContent = team.code;
            document.getElementById("mdlTeamGeo").innerHTML = getGeoBadge(team.geography);
            document.getElementById("mdlTeamLeader").textContent = team.leader.fullName + " (" + team.leader.employeeCode + ")";
            document.getElementById("mdlTeamCount").textContent = (1 + team.members.length) + " nhân sự";
            document.getElementById("mdlTeamStatus").innerHTML = getStatusBadge(team.status);

            const allMembers = [
                { ...team.leader, isLeader: true },
                ...team.members.map(m => ({ ...m, isLeader: false }))
            ];

            let rows = "";
            allMembers.forEach(m => {
                rows += `
                    <tr style="cursor: pointer;" onclick="closeTeamDetailModal(); openEmployeeDetail(${m.id}, ${team.id});">
                        <td style="font-weight: 700; color: var(--org-primary);">${m.employeeCode}</td>
                        <td style="font-weight: 600;">${m.fullName} ${m.isLeader ? '<span style="color:#b45309;">(Leader)</span>' : ''}</td>
                        <td>${getRoleBadge(m.role)}</td>
                        <td>${getGeoBadge(team.geography)}</td>
                        <td style="text-align: center;">${getStatusBadge(m.status)}</td>
                    </tr>
                `;
            });
            document.getElementById("mdlTeamMembersList").innerHTML = rows;

            document.getElementById("modalTeamDetail").classList.add("active");
        }

        function closeTeamDetailModal() {
            document.getElementById("modalTeamDetail").classList.remove("active");
        }

        // Modal Chi tiết Nhân viên
        function openEmployeeDetail(empId, teamId) {
            const team = salesOrgData.teams.find(t => t.id === teamId);
            if (!team) return;

            let emp = null;
            if (team.leader.id === empId) {
                emp = { ...team.leader, isLeader: true };
            } else {
                const found = team.members.find(m => m.id === empId);
                if (found) emp = { ...found, isLeader: false };
            }

            if (!emp) return;

            document.getElementById("mdlEmpAvatar").textContent = getInitials(emp.fullName);
            document.getElementById("mdlEmpName").textContent = emp.fullName;
            document.getElementById("mdlEmpRole").innerHTML = getRoleBadge(emp.role);
            document.getElementById("mdlEmpCode").textContent = emp.employeeCode;
            document.getElementById("mdlEmpTeam").textContent = team.name + " (" + team.code + ")";
            document.getElementById("mdlEmpLeader").textContent = team.leader.fullName + " (" + team.leader.employeeCode + ")";
            document.getElementById("mdlEmpGeo").innerHTML = getGeoBadge(team.geography);
            document.getElementById("mdlEmpStatus").innerHTML = getStatusBadge(emp.status);
            document.getElementById("mdlEmpEmail").textContent = emp.email || "chưa cập nhật";
            document.getElementById("mdlEmpPhone").textContent = emp.phone || "chưa cập nhật";

            document.getElementById("modalEmployeeDetail").classList.add("active");
        }

        function closeEmployeeDetailModal() {
            document.getElementById("modalEmployeeDetail").classList.remove("active");
        }

        // Lắng nghe sự kiện tìm kiếm và bộ lọc thời gian thực
        document.getElementById("orgSearchInput").addEventListener("input", renderOrgTree);
        document.getElementById("filterTeam").addEventListener("change", renderOrgTree);
        document.getElementById("filterGeo").addEventListener("change", renderOrgTree);
        document.getElementById("filterRole").addEventListener("change", renderOrgTree);
        document.getElementById("filterStatus").addEventListener("change", renderOrgTree);

        // Đóng modal khi bấm phím Escape
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") {
                closeTeamDetailModal();
                closeEmployeeDetailModal();
            }
        });

        // Khởi tạo
        window.addEventListener("DOMContentLoaded", () => {
            updateKPIs();
            renderOrgTree();
        });
    </script>
</body>
</html>