<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh mục Lý do Thắng/Thua &amp; Đối thủ - CRM ICTU</title>

    <!-- CSS dùng chung -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS Module Users & Win/Loss -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/users/users.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/winloss/winloss.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Nội dung chính màn hình Win/Loss & Đối thủ -->
        <main class="winloss-page" id="winLossApp" role="main">
            <div class="winloss-container">

                <!-- Breadcrumb -->
                <nav class="winloss-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Kinh doanh</span>
                    <span class="separator">/</span>
                    <span class="active">Lý do thắng/thua &amp; Đối thủ cạnh tranh</span>
                </nav>

                <!-- Header màn hình -->
                <header class="winloss-header">
                    <div class="winloss-header-info">
                        <h1>Danh mục Lý do Thắng / Thua &amp; Đối thủ cạnh tranh</h1>
                        <p>Khai báo và quản lý tập trung các nguyên nhân chốt đơn thành công, lý do mất đơn hàng và danh sách đối thủ cạnh tranh trên thị trường.</p>
                    </div>
                    <div class="winloss-header-badges">
                        <span class="winloss-badge">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <circle cx="12" cy="8" r="7"></circle>
                                <polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88"></polyline>
                            </svg>
                            S2-10 / CRM-48
                        </span>
                    </div>
                </header>

                <p class="winloss-sprint-note">
                    Những danh mục này sẽ được sử dụng khi đóng Cơ hội ở Sprint 5.
                </p>

                <!-- Alerts -->
                <div class="user-alerts" id="wlAlertsArea" aria-live="polite">
                    <div class="user-alert user-alert-danger" id="wlErrorAlert" style="display: none;" role="alert">
                        <svg class="user-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                        </svg>
                        <div class="user-alert-content">
                            <div class="user-alert-title">Thông báo lỗi</div>
                            <div id="wlErrorMessage"></div>
                        </div>
                        <button type="button" class="user-alert-close" onclick="this.parentElement.style.display='none';">&times;</button>
                    </div>

                    <div class="user-alert user-alert-success" id="wlSuccessAlert" style="display: none;" role="status">
                        <svg class="user-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>
                        <div class="user-alert-content">
                            <div class="user-alert-title">Thành công</div>
                            <div id="wlSuccessMessage">Thực hiện thao tác thành công.</div>
                        </div>
                        <button type="button" class="user-alert-close" onclick="this.parentElement.style.display='none';">&times;</button>
                    </div>
                </div>

                <!-- Tab Navigation (AC 1, AC 2, AC 3, AC 5: Tách riêng danh sách) -->
                <div class="winloss-tabs" role="tablist">
                    <button type="button" class="winloss-tab-btn is-active" id="tabBtnWin" onclick="switchTab('WIN')">
                        🏆 Lý do Thắng (Win) <span class="winloss-tab-badge" id="badgeCountWin">0</span>
                    </button>
                    <button type="button" class="winloss-tab-btn" id="tabBtnLoss" onclick="switchTab('LOSS')">
                        ❌ Lý do Thua (Loss) <span class="winloss-tab-badge" id="badgeCountLoss">0</span>
                    </button>
                    <button type="button" class="winloss-tab-btn" id="tabBtnCompetitor" onclick="switchTab('COMPETITOR')">
                        🏢 Đối thủ cạnh tranh <span class="winloss-tab-badge" id="badgeCountComp">0</span>
                    </button>
                </div>

                <!-- Section 1 & 2: Lý do Thắng / Thua -->
                <section class="winloss-card" id="paneReasons" style="display: block;">
                    <div class="winloss-card-header">
                        <div>
                            <h2 class="winloss-card-title" id="reasonsCardTitle">Danh sách Lý do Thắng</h2>
                            <p style="font-size: 0.88rem; color: #64748b; margin: 4px 0 0 0;" id="reasonsCardSubtitle">
                                Danh mục các nguyên nhân thành công khi ký kết hợp đồng.
                            </p>
                        </div>
                        <button type="button" class="btn btn-primary" id="btnOpenAddReasonModal">
                            + Thêm lý do mới
                        </button>
                    </div>

                    <div class="winloss-table-responsive">
                        <table class="winloss-table">
                            <thead>
                                <tr>
                                    <th style="width: 60px;">ID</th>
                                    <th>Nội dung lý do</th>
                                    <th style="width: 140px;">Phân loại</th>
                                    <th>Mô tả chi tiết</th>
                                    <th style="width: 120px;">Trạng thái</th>
                                    <th style="width: 140px; text-align: center;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody id="reasonsTableBody">
                                <!-- Rendered dynamically -->
                            </tbody>
                        </table>
                    </div>
                </section>

                <!-- Section 3: Đối thủ cạnh tranh -->
                <section class="winloss-card" id="paneCompetitors" style="display: none;">
                    <div class="winloss-card-header">
                        <div>
                            <h2 class="winloss-card-title">Danh sách Đối thủ cạnh tranh</h2>
                            <p style="font-size: 0.88rem; color: #64748b; margin: 4px 0 0 0;">
                                Quản lý thông tin điểm mạnh, điểm yếu và liên hệ của các đối thủ trên thị trường.
                            </p>
                        </div>
                        <button type="button" class="btn btn-primary" id="btnOpenAddCompetitorModal">
                            + Thêm đối thủ mới
                        </button>
                    </div>

                    <div class="winloss-table-responsive">
                        <table class="winloss-table">
                            <thead>
                                <tr>
                                    <th style="width: 60px;">ID</th>
                                    <th style="width: 220px;">Tên đối thủ</th>
                                    <th>Điểm mạnh (Strengths)</th>
                                    <th>Điểm yếu (Weaknesses)</th>
                                    <th style="width: 180px;">Website</th>
                                    <th style="width: 140px; text-align: center;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody id="competitorsTableBody">
                                <!-- Rendered dynamically -->
                            </tbody>
                        </table>
                    </div>
                </section>

            </div>
        </main>
    </div>

    <!-- Modal Thêm/Sửa Lý do -->
    <div class="user-modal-overlay" id="reasonModal" role="dialog" aria-modal="true">
        <div class="user-modal-card" style="max-width: 520px;">
            <header class="user-modal-header">
                <h3 class="user-modal-title" id="reasonModalTitle">Thêm lý do mới</h3>
                <button type="button" class="user-modal-close-btn" onclick="closeReasonModal()">&times;</button>
            </header>
            <form id="reasonForm" novalidate onsubmit="handleSaveReason(event)">
                <input type="hidden" id="reasonId">
                <input type="hidden" id="reasonType">

                <div class="user-modal-body">
                    <div class="modal-field">
                        <label class="modal-label" for="reasonText">Nội dung lý do <span class="modal-required">*</span></label>
                        <input type="text" id="reasonText" class="modal-input" placeholder="Ví dụ: Giá cạnh tranh, Thiếu tính năng..." required>
                        <div class="modal-field-feedback" id="feedbackReasonText" style="display:block; color:#dc2626; font-size:0.83rem; margin-top:4px;"></div>
                    </div>
                    <div class="modal-field">
                        <label class="modal-label" for="reasonDesc">Mô tả chi tiết</label>
                        <textarea id="reasonDesc" class="modal-input" rows="3" placeholder="Giải thích ngữ cảnh áp dụng lý do này..."></textarea>
                    </div>
                </div>
                <footer class="user-modal-footer">
                    <button type="button" class="btn btn-secondary" onclick="closeReasonModal()">Hủy bỏ</button>
                    <button type="submit" class="btn btn-primary" id="btnSaveReason">Lưu lý do</button>
                </footer>
            </form>
        </div>
    </div>

    <!-- Modal Thêm/Sửa Đối thủ -->
    <div class="user-modal-overlay" id="competitorModal" role="dialog" aria-modal="true">
        <div class="user-modal-card" style="max-width: 540px;">
            <header class="user-modal-header">
                <h3 class="user-modal-title" id="compModalTitle">Thêm đối thủ cạnh tranh</h3>
                <button type="button" class="user-modal-close-btn" onclick="closeCompModal()">&times;</button>
            </header>
            <form id="compForm" novalidate onsubmit="handleSaveCompetitor(event)">
                <input type="hidden" id="compId">

                <div class="user-modal-body">
                    <div class="modal-field">
                        <label class="modal-label" for="compName">Tên đối thủ <span class="modal-required">*</span></label>
                        <input type="text" id="compName" class="modal-input" placeholder="Ví dụ: Công ty Apex CRM..." required>
                        <div class="modal-field-feedback" id="feedbackCompName" style="display:block; color:#dc2626; font-size:0.83rem; margin-top:4px;"></div>
                    </div>
                    <div class="modal-field">
                        <label class="modal-label" for="compStrengths">Điểm mạnh (Strengths)</label>
                        <textarea id="compStrengths" class="modal-input" rows="2" placeholder="Điểm mạnh nổi trội của đối thủ..."></textarea>
                    </div>
                    <div class="modal-field">
                        <label class="modal-label" for="compWeaknesses">Điểm yếu (Weaknesses)</label>
                        <textarea id="compWeaknesses" class="modal-input" rows="2" placeholder="Điểm yếu cần khai thác..."></textarea>
                    </div>
                    <div class="modal-field">
                        <label class="modal-label" for="compWebsite">Địa chỉ Website</label>
                        <input type="url" id="compWebsite" class="modal-input" placeholder="https://example.com">
                    </div>
                </div>
                <footer class="user-modal-footer">
                    <button type="button" class="btn btn-secondary" onclick="closeCompModal()">Hủy bỏ</button>
                    <button type="submit" class="btn btn-primary" id="btnSaveComp">Lưu đối thủ</button>
                </footer>
            </form>
        </div>
    </div>

    <!-- Footer dùng chung -->
    <jsp:include page="/jsp/shared/footer.jsp" />

    <script>
    var contextPath = '${pageContext.request.contextPath}';
    var activeTab = 'WIN'; // 'WIN', 'LOSS', 'COMPETITOR'

    var winReasons = [];
    var lossReasons = [];
    var competitors = [];

    function escapeHtml(str) {
        if (!str) return '';
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function showAlert(isSuccess, msg) {
        var successAlert = document.getElementById('wlSuccessAlert');
        var errorAlert = document.getElementById('wlErrorAlert');
        if (isSuccess) {
            document.getElementById('wlSuccessMessage').textContent = msg;
            successAlert.style.display = 'flex';
            errorAlert.style.display = 'none';
            setTimeout(function () { successAlert.style.display = 'none'; }, 4000);
        } else {
            document.getElementById('wlErrorMessage').textContent = msg;
            errorAlert.style.display = 'flex';
            successAlert.style.display = 'none';
        }
    }

    async function loadReasons() {
        try {
            // BE CONTRACT NEEDED: /api/winloss/reasons.
            var rRes = await fetch(contextPath + '/api/winloss/reasons', { headers: { 'Accept': 'application/json' } });
            var rData = await rRes.json();
            if (!rRes.ok || !rData || rData.success === false || !rData.data) {
                throw new Error((rData && rData.message) ? rData.message : 'API Reasons chưa khả dụng.');
            }

            winReasons = Array.isArray(rData.data.winReasons) ? rData.data.winReasons : [];
            lossReasons = Array.isArray(rData.data.lossReasons) ? rData.data.lossReasons : [];
            return null;
        } catch (error) {
            winReasons = [];
            lossReasons = [];
            console.error('Lỗi khi tải Win/Loss Reasons:', error);
            return 'Không thể tải danh mục lý do thắng/thua.';
        }
    }

    async function loadCompetitors() {
        try {
            // BE CONTRACT NEEDED: /api/winloss/competitors.
            var cRes = await fetch(contextPath + '/api/winloss/competitors', { headers: { 'Accept': 'application/json' } });
            var cData = await cRes.json();
            if (!cRes.ok || !cData || cData.success === false || !Array.isArray(cData.data)) {
                throw new Error((cData && cData.message) ? cData.message : 'API Competitors chưa khả dụng.');
            }

            competitors = cData.data;
            return null;
        } catch (error) {
            competitors = [];
            console.error('Lỗi khi tải Competitors:', error);
            return 'Không thể tải danh mục đối thủ cạnh tranh.';
        }
    }

    async function loadData() {
        var errors = await Promise.all([loadReasons(), loadCompetitors()]);

        document.getElementById('badgeCountWin').textContent = winReasons.length;
        document.getElementById('badgeCountLoss').textContent = lossReasons.length;
        document.getElementById('badgeCountComp').textContent = competitors.length;

        renderActiveTab();

        var errorMessages = errors.filter(function (message) { return Boolean(message); });
        if (errorMessages.length > 0) {
            showAlert(false, errorMessages.join(' '));
        }
    }

    function switchTab(tab) {
        activeTab = tab;
        document.getElementById('tabBtnWin').classList.toggle('is-active', tab === 'WIN');
        document.getElementById('tabBtnLoss').classList.toggle('is-active', tab === 'LOSS');
        document.getElementById('tabBtnCompetitor').classList.toggle('is-active', tab === 'COMPETITOR');

        document.getElementById('paneReasons').style.display = (tab === 'WIN' || tab === 'LOSS') ? 'block' : 'none';
        document.getElementById('paneCompetitors').style.display = (tab === 'COMPETITOR') ? 'block' : 'none';

        if (tab === 'WIN') {
            document.getElementById('reasonsCardTitle').textContent = 'Danh sách Lý do Thắng (Win Reasons)';
            document.getElementById('reasonsCardSubtitle').textContent = 'Danh mục các nguyên nhân thành công khi ký kết hợp đồng.';
        } else if (tab === 'LOSS') {
            document.getElementById('reasonsCardTitle').textContent = 'Danh sách Lý do Thua (Loss Reasons)';
            document.getElementById('reasonsCardSubtitle').textContent = 'Danh mục các nguyên nhân mất khách hàng hoặc thua thầu đối thủ.';
        }

        renderActiveTab();
    }

    function renderActiveTab() {
        if (activeTab === 'WIN') {
            renderReasonsTable(winReasons, 'WIN');
        } else if (activeTab === 'LOSS') {
            renderReasonsTable(lossReasons, 'LOSS');
        } else if (activeTab === 'COMPETITOR') {
            renderCompetitorsTable(competitors);
        }
    }

    function renderReasonsTable(list, type) {
        var tbody = document.getElementById('reasonsTableBody');
        tbody.innerHTML = '';

        if (!list || list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:#64748b; padding:24px;">Chưa có lý do nào. Hãy bấm nút "+ Thêm lý do mới" để tạo.</td></tr>';
            return;
        }

        list.forEach(function (item) {
            var badge = item.type === 'WIN' ? '<span class="badge-win">THẮNG (WIN)</span>' : '<span class="badge-loss">THUA (LOSS)</span>';
            var tr = document.createElement('tr');
            tr.innerHTML =
                '<td>#' + escapeHtml(item.id) + '</td>' +
                '<td><strong>' + escapeHtml(item.reasonText) + '</strong></td>' +
                '<td>' + badge + '</td>' +
                '<td style="color:#64748b; font-size:0.85rem;">' + escapeHtml(item.description || '---') + '</td>' +
                '<td><span class="status-badge status-badge--active"><span class="status-dot"></span>Hoạt động</span></td>' +
                '<td style="text-align:center;">' +
                    '<button type="button" class="btn btn-sm btn-secondary" onclick="openEditReason(' + item.id + ', \'' + item.type + '\')" style="margin-right:6px;">Sửa</button>' +
                    '<button type="button" class="btn btn-sm btn-outline-danger" onclick="deleteReason(' + item.id + ')">Xóa</button>' +
                '</td>';
            tbody.appendChild(tr);
        });
    }

    function renderCompetitorsTable(list) {
        var tbody = document.getElementById('competitorsTableBody');
        tbody.innerHTML = '';

        if (!list || list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:#64748b; padding:24px;">Chưa có đối thủ nào. Hãy bấm nút "+ Thêm đối thủ mới" để tạo.</td></tr>';
            return;
        }

        list.forEach(function (c) {
            var websiteLink = c.website ? '<a href="' + escapeHtml(c.website) + '" target="_blank" rel="noopener noreferrer" style="color:#2563eb; text-decoration:none;">' + escapeHtml(c.website) + '</a>' : '---';
            var tr = document.createElement('tr');
            tr.innerHTML =
                '<td>#' + escapeHtml(c.id) + '</td>' +
                '<td><strong>' + escapeHtml(c.name) + '</strong></td>' +
                '<td style="color:#166534; font-size:0.85rem;">' + escapeHtml(c.strengths || '---') + '</td>' +
                '<td style="color:#991b1b; font-size:0.85rem;">' + escapeHtml(c.weaknesses || '---') + '</td>' +
                '<td>' + websiteLink + '</td>' +
                '<td style="text-align:center;">' +
                    '<button type="button" class="btn btn-sm btn-secondary" onclick="openEditComp(' + c.id + ')" style="margin-right:6px;">Sửa</button>' +
                    '<button type="button" class="btn btn-sm btn-outline-danger" onclick="deleteCompetitor(' + c.id + ')">Xóa</button>' +
                '</td>';
            tbody.appendChild(tr);
        });
    }

    // Modal Reason Handlers
    document.getElementById('btnOpenAddReasonModal').addEventListener('click', function () {
        document.getElementById('reasonId').value = '';
        document.getElementById('reasonType').value = (activeTab === 'LOSS') ? 'LOSS' : 'WIN';
        document.getElementById('reasonText').value = '';
        document.getElementById('reasonDesc').value = '';
        document.getElementById('feedbackReasonText').textContent = '';
        document.getElementById('reasonModalTitle').textContent = (activeTab === 'LOSS') ? 'Thêm lý do Thua mới' : 'Thêm lý do Thắng mới';
        document.getElementById('reasonModal').classList.add('is-open');
    });

    function closeReasonModal() {
        document.getElementById('reasonModal').classList.remove('is-open');
    }

    function openEditReason(id, type) {
        var list = (type === 'WIN') ? winReasons : lossReasons;
        var r = list.find(function (item) { return item.id === id; });
        if (!r) return;
        document.getElementById('reasonId').value = r.id;
        document.getElementById('reasonType').value = r.type;
        document.getElementById('reasonText').value = r.reasonText || '';
        document.getElementById('reasonDesc').value = r.description || '';
        document.getElementById('feedbackReasonText').textContent = '';
        document.getElementById('reasonModalTitle').textContent = 'Chỉnh sửa lý do ' + (r.type === 'WIN' ? 'Thắng' : 'Thua');
        document.getElementById('reasonModal').classList.add('is-open');
    }

    async function handleSaveReason(e) {
        e.preventDefault();
        var id = document.getElementById('reasonId').value;
        var type = document.getElementById('reasonType').value || 'WIN';
        var text = document.getElementById('reasonText').value.trim();
        var desc = document.getElementById('reasonDesc').value.trim();

        if (!text) {
            document.getElementById('feedbackReasonText').textContent = 'Vui lòng nhập nội dung lý do.';
            return;
        }

        var isEdit = Boolean(id);
        var url = contextPath + '/api/winloss/reasons';
        var method = isEdit ? 'PUT' : 'POST';
        var payload = { id: isEdit ? Number(id) : null, reasonText: text, type: type, description: desc };

        try {
            var res = await fetch(url, {
                method: method,
                headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                body: JSON.stringify(payload)
            });
            var data = await res.json();
            if (res.ok && data.success) {
                closeReasonModal();
                showAlert(true, isEdit ? 'Cập nhật lý do thành công.' : 'Thêm mới lý do thành công.');
                loadData();
            } else {
                showAlert(false, (data && data.message) ? data.message : 'Không thể lưu lý do.');
            }
        } catch (err) {
            console.error(err);
            showAlert(false, 'Lỗi kết nối máy chủ.');
        }
    }

    async function deleteReason(id) {
        if (!confirm('Bạn có chắc chắn muốn xóa lý do #' + id + ' không?')) return;
        try {
            var res = await fetch(contextPath + '/api/winloss/reasons?id=' + id, { method: 'DELETE', headers: { 'Accept': 'application/json' } });
            var data = await res.json();
            if (res.ok && data.success) {
                showAlert(true, 'Đã xóa lý do thành công.');
                loadData();
            } else {
                showAlert(false, (data && data.message) ? data.message : 'Không thể xóa lý do.');
            }
        } catch (err) {
            showAlert(false, 'Lỗi kết nối máy chủ.');
        }
    }

    // Modal Competitor Handlers
    document.getElementById('btnOpenAddCompetitorModal').addEventListener('click', function () {
        document.getElementById('compId').value = '';
        document.getElementById('compName').value = '';
        document.getElementById('compStrengths').value = '';
        document.getElementById('compWeaknesses').value = '';
        document.getElementById('compWebsite').value = '';
        document.getElementById('feedbackCompName').textContent = '';
        document.getElementById('compModalTitle').textContent = 'Thêm đối thủ cạnh tranh mới';
        document.getElementById('competitorModal').classList.add('is-open');
    });

    function closeCompModal() {
        document.getElementById('competitorModal').classList.remove('is-open');
    }

    function openEditComp(id) {
        var c = competitors.find(function (item) { return item.id === id; });
        if (!c) return;
        document.getElementById('compId').value = c.id;
        document.getElementById('compName').value = c.name || '';
        document.getElementById('compStrengths').value = c.strengths || '';
        document.getElementById('compWeaknesses').value = c.weaknesses || '';
        document.getElementById('compWebsite').value = c.website || '';
        document.getElementById('feedbackCompName').textContent = '';
        document.getElementById('compModalTitle').textContent = 'Chỉnh sửa đối thủ cạnh tranh';
        document.getElementById('competitorModal').classList.add('is-open');
    }

    async function handleSaveCompetitor(e) {
        e.preventDefault();
        var id = document.getElementById('compId').value;
        var name = document.getElementById('compName').value.trim();
        var strengths = document.getElementById('compStrengths').value.trim();
        var weaknesses = document.getElementById('compWeaknesses').value.trim();
        var website = document.getElementById('compWebsite').value.trim();

        if (!name) {
            document.getElementById('feedbackCompName').textContent = 'Vui lòng nhập tên đối thủ cạnh tranh.';
            return;
        }

        var isEdit = Boolean(id);
        var url = contextPath + '/api/winloss/competitors';
        var method = isEdit ? 'PUT' : 'POST';
        var payload = { id: isEdit ? Number(id) : null, name: name, strengths: strengths, weaknesses: weaknesses, website: website };

        try {
            var res = await fetch(url, {
                method: method,
                headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                body: JSON.stringify(payload)
            });
            var data = await res.json();
            if (res.ok && data.success) {
                closeCompModal();
                showAlert(true, isEdit ? 'Cập nhật đối thủ thành công.' : 'Thêm mới đối thủ thành công.');
                loadData();
            } else {
                showAlert(false, (data && data.message) ? data.message : 'Không thể lưu đối thủ.');
            }
        } catch (err) {
            showAlert(false, 'Lỗi kết nối máy chủ.');
        }
    }

    async function deleteCompetitor(id) {
        if (!confirm('Bạn có chắc chắn muốn xóa đối thủ #' + id + ' không?')) return;
        try {
            var res = await fetch(contextPath + '/api/winloss/competitors?id=' + id, { method: 'DELETE', headers: { 'Accept': 'application/json' } });
            var data = await res.json();
            if (res.ok && data.success) {
                showAlert(true, 'Đã xóa đối thủ thành công.');
                loadData();
            } else {
                showAlert(false, (data && data.message) ? data.message : 'Không thể xóa đối thủ.');
            }
        } catch (err) {
            showAlert(false, 'Lỗi kết nối máy chủ.');
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        loadData();
    });
    </script>
</body>
</html>
