// campaigns.js - Frontend controller for S4-03 Campaigns & Lead Association
// Phụ trách: Hoàng Văn Thắng (S4-03-FE & S4-03-INT)

const API_BASE = (window.CRM_BACKEND_BASE || `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`).replace(/\/$/, "");

let currentPage = 1;
let currentSize = 10;
let currentKeyword = "";
let currentStatus = "";
let currentType = "";
let activeCampaignId = null;
let isOfflineMode = false;
let currentCampaignsList = [];

// Mock storage keys
const STORAGE_CAMPAIGNS = "crm_mock_campaigns_s4";
const STORAGE_LEADS = "crm_mock_leads_s4";
const STORAGE_CAMPAIGN_LEADS = "crm_mock_campaign_leads_s4";

// Initial Mock Seed Data (Aligned with S4-03 Schema & Sprint 4 DB)
const DEFAULT_CAMPAIGNS = [
    {
        id: 1,
        code: "CAMP-2026-001",
        name: "Hội thảo Chuyển đổi số Doanh nghiệp 2026",
        type: "EVENT",
        status: "ACTIVE",
        startDate: "2026-10-01",
        endDate: "2026-10-30",
        budget: 150000000,
        expectedRevenue: 500000000,
        description: "Chuỗi hội thảo giới thiệu giải pháp CRM và chuyển đổi số cho khối doanh nghiệp SME.",
        leadCount: 4
    },
    {
        id: 2,
        code: "CAMP-2026-002",
        name: "Chiến dịch Email Marketing Quý 4",
        type: "EMAIL",
        status: "ACTIVE",
        startDate: "2026-10-05",
        endDate: "2026-11-15",
        budget: 35000000,
        expectedRevenue: 120000000,
        description: "Gửi email cá nhân hóa tri ân khách hàng cũ và chào gói ưu đãi gia hạn.",
        leadCount: 5
    },
    {
        id: 3,
        code: "CAMP-2026-003",
        name: "Quảng cáo Google Search & Display Ads",
        type: "ADS",
        status: "PLANNING",
        startDate: "2026-11-01",
        endDate: "2026-12-31",
        budget: 80000000,
        expectedRevenue: 300000000,
        description: "Chiến dịch chạy quảng cáo từ khóa giải pháp quản trị khách hàng B2B.",
        leadCount: 2
    },
    {
        id: 4,
        code: "CAMP-2026-004",
        name: "Telesales Chăm Sóc Khách Hàng Thân Thiết",
        type: "TELEMARKETING",
        status: "PAUSED",
        startDate: "2026-09-15",
        endDate: "2026-10-10",
        budget: 20000000,
        expectedRevenue: 60000000,
        description: "Đội ngũ Telesales gọi điện tư vấn nhu cầu mở rộng chi nhánh.",
        leadCount: 3
    },
    {
        id: 5,
        code: "CAMP-2026-005",
        name: "Triển lãm Công nghệ Quốc tế TechExpo 2026",
        type: "EVENT",
        status: "COMPLETED",
        startDate: "2026-08-01",
        endDate: "2026-08-05",
        budget: 200000000,
        expectedRevenue: 850000000,
        description: "Gian hàng demo trực tiếp sản phẩm tại Trung tâm Hội chợ & Triển lãm Quốc tế.",
        leadCount: 8
    }
];

const DEFAULT_LEADS = [
    { id: 101, fullName: "Nguyễn Hoàng Nam", companyName: "Tập đoàn An Phát", email: "nam.nh@anphat.vn", phone: "0987 111 222" },
    { id: 102, fullName: "Vũ Thu Trang", companyName: "Công ty CP Công Nghệ Alpha", email: "trang.vt@alphatech.com", phone: "0912 345 678" },
    { id: 103, fullName: "Phạm Minh Đức", companyName: "Công ty Vận Tải Sao Vàng", email: "duc.pm@saovang.vn", phone: "0903 456 789" },
    { id: 104, fullName: "Đặng Quốc Hưng", companyName: "Bất Động Sản Thịnh Vượng", email: "hung.dq@thinhvuong.com", phone: "0978 999 888" },
    { id: 105, fullName: "Hoàng Thị Mai", companyName: "Du Lịch Sen Việt", email: "mai.ht@senviet.com", phone: "0934 567 890" },
    { id: 106, fullName: "Trần Anh Tuấn", companyName: "Logistics Toàn Cầu", email: "tuan.ta@globallogistics.vn", phone: "0966 222 333" },
    { id: 107, fullName: "Lê Ngọc Bích", companyName: "Dược Phẩm Minh Tâm", email: "bich.ln@minhtampharma.com", phone: "0945 678 123" },
    { id: 108, fullName: "Nguyễn Văn Dũng", companyName: "Cơ Khí Nam Định", email: "dung.nv@namdinhmech.vn", phone: "0936 888 777" }
];

const DEFAULT_CAMPAIGN_LEADS = [
    { campaignId: 1, leadId: 101, status: "CONVERTED", notes: "Đã ký hợp đồng dịch vụ" },
    { campaignId: 1, leadId: 102, status: "RESPONDED", notes: "Quan tâm gói Enterprise" },
    { campaignId: 1, leadId: 103, status: "OPENED", notes: "Đã xem thư mời" },
    { campaignId: 1, leadId: 104, status: "SENT", notes: "Mới gửi thư" },
    { campaignId: 2, leadId: 102, status: "RESPONDED", notes: "Hẹn demo tuần tới" },
    { campaignId: 2, leadId: 105, status: "OPENED", notes: "Đã click link" },
    { campaignId: 3, leadId: 106, status: "SENT", notes: "Từ chiến dịch Ads" }
];

// Helper: Toast notification
function notify(message, type = "info") {
    if (typeof window.crmToast === "function") {
        window.crmToast(message, type);
    } else {
        alert(message);
    }
}

// Local mock data store management
function getMockCampaigns() {
    try {
        const raw = localStorage.getItem(STORAGE_CAMPAIGNS);
        if (!raw) {
            localStorage.setItem(STORAGE_CAMPAIGNS, JSON.stringify(DEFAULT_CAMPAIGNS));
            return [...DEFAULT_CAMPAIGNS];
        }
        return JSON.parse(raw);
    } catch {
        return [...DEFAULT_CAMPAIGNS];
    }
}

function saveMockCampaigns(items) {
    try {
        localStorage.setItem(STORAGE_CAMPAIGNS, JSON.stringify(items));
    } catch (e) {
        console.error("Failed to save campaigns to localStorage", e);
    }
}

function getMockLeads() {
    try {
        const raw = localStorage.getItem(STORAGE_LEADS);
        if (!raw) {
            localStorage.setItem(STORAGE_LEADS, JSON.stringify(DEFAULT_LEADS));
            return [...DEFAULT_LEADS];
        }
        return JSON.parse(raw);
    } catch {
        return [...DEFAULT_LEADS];
    }
}

function getMockCampaignLeads() {
    try {
        const raw = localStorage.getItem(STORAGE_CAMPAIGN_LEADS);
        if (!raw) {
            localStorage.setItem(STORAGE_CAMPAIGN_LEADS, JSON.stringify(DEFAULT_CAMPAIGN_LEADS));
            return [...DEFAULT_CAMPAIGN_LEADS];
        }
        return JSON.parse(raw);
    } catch {
        return [...DEFAULT_CAMPAIGN_LEADS];
    }
}

function saveMockCampaignLeads(items) {
    try {
        localStorage.setItem(STORAGE_CAMPAIGN_LEADS, JSON.stringify(items));
    } catch (e) {
        console.error("Failed to save campaign leads to localStorage", e);
    }
}

// Update UI Banner showing mode
function updateModeBanner(offline, errorMsg = "") {
    isOfflineMode = offline;
    const banner = document.getElementById("campaignModeBanner");
    if (!banner) return;

    if (offline) {
        banner.className = "campaign-mode-banner mock-mode";
        banner.innerHTML = `
            <div>
                <strong>💡 Chế độ Xem trước Giao diện (Mock Data - Story S4-03-FE)</strong>
                <div style="font-size: 0.8rem; margin-top: 2px;">
                    ${errorMsg ? `(Không kết nối được API: ${escapeHtml(errorMsg)} — Sử dụng dữ liệu mô phỏng)` : 'Hệ thống đang hoạt động với dữ liệu mẫu đầy đủ để thử nghiệm Form, Bảng, Lọc và Quản lý Lead.'}
                </div>
            </div>
            <button class="btn btn-secondary btn-sm" onclick="retryConnectApi()">
                <i class="fa-solid fa-arrows-rotate"></i> Thử kết nối API
            </button>
        `;
    } else {
        banner.className = "campaign-mode-banner online-mode";
        banner.innerHTML = `
            <div>
                <strong>🟢 Kết nối API Backend thành công (Story S4-03-INT)</strong>
                <div style="font-size: 0.8rem; margin-top: 2px;">Endpoint: ${escapeHtml(API_BASE)}/api/campaigns (Đã đồng bộ Cơ sở dữ liệu)</div>
            </div>
            <span class="badge" style="background:#dcfce7; color:#15803d; padding:4px 8px; border-radius:4px; font-weight:600;">LIVE API</span>
        `;
    }
}

window.retryConnectApi = function() {
    isOfflineMode = false;
    notify("Đang kiểm tra kết nối API Backend...", "info");
    loadCampaigns();
};

document.addEventListener("DOMContentLoaded", () => {
    loadCampaigns();

    // Search & Filter listeners
    const searchInput = document.getElementById("searchCampaignInput");
    const statusSelect = document.getElementById("statusFilterSelect");
    const typeSelect = document.getElementById("typeFilterSelect");
    const btnSearch = document.getElementById("btnSearchCampaign");
    const btnReset = document.getElementById("btnResetFilter");
    const btnRefresh = document.getElementById("btnRefreshCampaigns");
    const btnExport = document.getElementById("btnExportCampaigns");

    btnSearch?.addEventListener("click", () => {
        applyFilters();
    });

    searchInput?.addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
            applyFilters();
        }
    });

    statusSelect?.addEventListener("change", () => {
        applyFilters();
    });

    typeSelect?.addEventListener("change", () => {
        applyFilters();
    });

    btnReset?.addEventListener("click", () => {
        if (searchInput) searchInput.value = "";
        if (statusSelect) statusSelect.value = "";
        if (typeSelect) typeSelect.value = "";
        currentKeyword = "";
        currentStatus = "";
        currentType = "";
        currentPage = 1;
        loadCampaigns();
        notify("Đã đặt lại bộ lọc tìm kiếm", "info");
    });

    btnRefresh?.addEventListener("click", () => {
        loadCampaigns();
        notify("Đã làm mới danh sách chiến dịch", "info");
    });

    btnExport?.addEventListener("click", () => {
        exportCampaignsToCSV();
    });

    // KPI Card Clickable Filters
    document.querySelectorAll(".customer-kpi-card[data-filter-status]").forEach(card => {
        card.addEventListener("click", () => {
            const status = card.getAttribute("data-filter-status");
            if (statusSelect) statusSelect.value = status;
            currentStatus = status;
            currentPage = 1;
            loadCampaigns();
        });
    });

    // Modal Create / Edit listeners
    document.getElementById("btnOpenCreateModal")?.addEventListener("click", () => {
        openCampaignModal();
    });

    document.getElementById("btnAutoGenerateCode")?.addEventListener("click", () => {
        const rand = Math.floor(100 + Math.random() * 900);
        const codeInput = document.getElementById("campaignCode");
        if (codeInput) {
            codeInput.value = `CAMP-2026-${rand}`;
            notify(`Đã tạo mã gợi ý: ${codeInput.value}`, "info");
        }
    });

    document.getElementById("campaignForm")?.addEventListener("submit", handleSaveCampaign);

    // Modal Link Leads listeners
    document.getElementById("btnAddLeadToCampaign")?.addEventListener("click", handleAddLeadToCampaign);

    // Confirmation dialog listeners
    document.getElementById("btnCancelConfirm")?.addEventListener("click", closeConfirmDialog);
    document.getElementById("btnAcceptConfirm")?.addEventListener("click", () => {
        if (typeof pendingConfirmCallback === "function") {
            const cb = pendingConfirmCallback;
            closeConfirmDialog();
            cb();
        }
    });

    const confirmModal = document.getElementById("confirmDeleteModal");
    if (confirmModal) {
        confirmModal.addEventListener("click", (e) => {
            if (e.target === confirmModal) {
                closeConfirmDialog();
            }
        });
    }

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            const cm = document.getElementById("confirmDeleteModal");
            if (cm && cm.classList.contains("active")) {
                closeConfirmDialog();
            }
        }
    });
});

let pendingConfirmCallback = null;

function showConfirmDialog({ title, message, confirmText = "Xác nhận xóa", onConfirm }) {
    const modal = document.getElementById("confirmDeleteModal");
    const titleEl = document.getElementById("confirmDialogTitle");
    const msgEl = document.getElementById("confirmDialogMessage");
    const confirmBtn = document.getElementById("btnAcceptConfirm");

    if (!modal) {
        if (confirm(message.replace(/<[^>]+>/g, ""))) onConfirm();
        return;
    }

    if (titleEl) titleEl.innerText = title;
    if (msgEl) msgEl.innerHTML = message;
    if (confirmBtn) {
        confirmBtn.innerHTML = `<i class="fa-solid fa-trash-can"></i> ${escapeHtml(confirmText)}`;
    }

    pendingConfirmCallback = onConfirm;
    modal.classList.add("active");
}

function closeConfirmDialog() {
    const modal = document.getElementById("confirmDeleteModal");
    if (modal) modal.classList.remove("active");
    pendingConfirmCallback = null;
}

function applyFilters() {
    const searchInput = document.getElementById("searchCampaignInput");
    const statusSelect = document.getElementById("statusFilterSelect");
    const typeSelect = document.getElementById("typeFilterSelect");

    currentKeyword = searchInput ? searchInput.value.trim() : "";
    currentStatus = statusSelect ? statusSelect.value : "";
    currentType = typeSelect ? typeSelect.value : "";
    currentPage = 1;
    loadCampaigns();
}

async function apiFetch(url, options = {}) {
    options.credentials = "include";
    if (options.body && typeof options.body === "object" && !(options.body instanceof FormData)) {
        options.headers = {
            ...options.headers,
            "Content-Type": "application/json"
        };
        options.body = JSON.stringify(options.body);
    }
    const cleanUrl = url.startsWith("/") ? url : "/" + url;
    const response = await fetch(`${API_BASE}/api${cleanUrl}`, options);
    const data = await response.json().catch(() => ({ success: false, message: "Phản hồi máy chủ không đúng định dạng JSON" }));
    if (!response.ok || !data.success) {
        throw new Error(data.message || `Lỗi yêu cầu máy chủ (${response.status})`);
    }
    return data;
}

// Update KPI cards
function updateKpiCards(allCampaigns, allCampaignLeads) {
    const totalCount = allCampaigns.length;
    const activeCount = allCampaigns.filter(c => c.status === "ACTIVE").length;
    const planningCount = allCampaigns.filter(c => c.status === "PLANNING").length;
    const totalBudget = allCampaigns.reduce((sum, c) => sum + (Number(c.budget) || 0), 0);
    const totalLeads = allCampaignLeads ? allCampaignLeads.length : allCampaigns.reduce((sum, c) => sum + (c.leadCount || 0), 0);

    const elTotal = document.getElementById("kpiTotalCampaigns");
    const elActive = document.getElementById("kpiActiveCampaigns");
    const elPlanning = document.getElementById("kpiPlanningCampaigns");
    const elBudget = document.getElementById("kpiTotalBudget");
    const elLeads = document.getElementById("kpiTotalLeads");

    if (elTotal) elTotal.innerText = totalCount;
    if (elActive) elActive.innerText = activeCount;
    if (elPlanning) elPlanning.innerText = planningCount;
    if (elBudget) elBudget.innerText = formatCurrency(totalBudget);
    if (elLeads) elLeads.innerText = totalLeads;
}

async function loadCampaigns() {
    const tableBody = document.getElementById("campaignTableBody");
    if (!tableBody) return;
    tableBody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding: 2.5rem; color: var(--text-secondary, #64748b);">
        <i class="fa-solid fa-spinner fa-spin" style="font-size: 1.5rem; margin-bottom: 0.5rem; display: block; color: var(--color-primary, #2563eb);"></i>
        Đang tải dữ liệu chiến dịch...
    </td></tr>`;

    // Try real API first
    if (!isOfflineMode) {
        try {
            let url = `/campaigns?page=${currentPage}&size=${currentSize}`;
            if (currentKeyword) url += `&keyword=${encodeURIComponent(currentKeyword)}`;
            if (currentStatus) url += `&status=${encodeURIComponent(currentStatus)}`;

            const res = await apiFetch(url);
            updateModeBanner(false);
            const { items, pagination } = res.data;

            // Apply client-side type filter if selected
            let filteredItems = items || [];
            if (currentType) {
                filteredItems = filteredItems.filter(c => c.type === currentType);
            }
            currentCampaignsList = filteredItems;

            // Also fetch all campaigns for KPI metrics calculation if possible
            const allRes = await apiFetch(`/campaigns?page=1&size=1000`).catch(() => null);
            if (allRes && allRes.data && allRes.data.items) {
                updateKpiCards(allRes.data.items, null);
            } else {
                updateKpiCards(filteredItems, null);
            }

            renderCampaignTable(filteredItems, pagination);
            return;
        } catch (err) {
            console.warn("API Backend offline or error. Falling back to local mock data:", err.message);
            updateModeBanner(true, err.message);
        }
    }

    // Mock mode execution
    loadMockCampaigns();
}

function loadMockCampaigns() {
    const campaigns = getMockCampaigns();
    const campaignLeads = getMockCampaignLeads();

    // Calculate lead count dynamically
    campaigns.forEach(c => {
        c.leadCount = campaignLeads.filter(cl => cl.campaignId === c.id).length;
    });

    updateKpiCards(campaigns, campaignLeads);

    // Filter
    let filtered = campaigns.filter(c => {
        let matchKw = true;
        let matchSt = true;
        let matchTp = true;

        if (currentKeyword) {
            const kw = currentKeyword.toLowerCase();
            matchKw = (c.name && c.name.toLowerCase().includes(kw)) ||
                      (c.code && c.code.toLowerCase().includes(kw));
        }
        if (currentStatus) {
            matchSt = c.status === currentStatus;
        }
        if (currentType) {
            matchTp = c.type === currentType;
        }
        return matchKw && matchSt && matchTp;
    });

    const totalItems = filtered.length;
    const totalPages = Math.ceil(totalItems / currentSize) || 1;
    if (currentPage > totalPages) currentPage = totalPages;

    const startIdx = (currentPage - 1) * currentSize;
    const pageItems = filtered.slice(startIdx, startIdx + currentSize);

    const pagination = {
        page: currentPage,
        size: currentSize,
        totalItems: totalItems,
        totalPages: totalPages
    };

    renderCampaignTable(pageItems, pagination);
}

function getTypeBadge(type) {
    switch (type) {
        case "EMAIL":
            return `<span class="type-badge"><i class="fa-solid fa-envelope"></i> Email</span>`;
        case "EVENT":
            return `<span class="type-badge"><i class="fa-solid fa-calendar-star"></i> Hội thảo</span>`;
        case "TELEMARKETING":
            return `<span class="type-badge"><i class="fa-solid fa-phone-volume"></i> Telesales</span>`;
        case "ADS":
            return `<span class="type-badge"><i class="fa-solid fa-rectangle-ad"></i> Quảng cáo</span>`;
        default:
            return `<span class="type-badge"><i class="fa-solid fa-tag"></i> Khác</span>`;
    }
}

function renderCampaignTable(items, pagination) {
    const tableBody = document.getElementById("campaignTableBody");
    if (!tableBody) return;

    if (!items || items.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding: 3rem; color: var(--text-secondary, #64748b);">
            <div style="font-size: 2rem; margin-bottom: 0.5rem;"><i class="fa-regular fa-folder-open"></i></div>
            Không tìm thấy chiến dịch nào phù hợp với bộ lọc tìm kiếm.
        </td></tr>`;
        renderPagination(pagination || { page: 1, totalPages: 1, totalItems: 0 });
        return;
    }

    tableBody.innerHTML = items.map(c => `
        <tr>
            <td class="td-code">
                <span class="campaign-code-tag">${escapeHtml(c.code)}</span>
            </td>
            <td class="td-name">
                <div class="campaign-name-title" title="${escapeHtml(c.name)}">${escapeHtml(c.name)}</div>
                ${c.description ? `<div class="campaign-desc-subtitle" title="${escapeHtml(c.description)}">${escapeHtml(c.description)}</div>` : ''}
            </td>
            <td class="td-status"><span class="status-badge status-${c.status}">${c.status}</span></td>
            <td class="td-type">${getTypeBadge(c.type)}</td>
            <td class="td-budget">
                <div class="budget-main">${formatCurrency(c.budget)}</div>
                ${c.expectedRevenue ? `<div class="budget-sub">Kỳ vọng: ${formatCurrency(c.expectedRevenue)}</div>` : ''}
            </td>
            <td class="td-leads" style="text-align: center;">
                <button type="button" class="lead-pill-btn" onclick="openLeadModal(${c.id}, '${escapeHtml(c.name)}')" title="Xem danh sách Lead (${c.leadCount || 0})">
                    <i class="fa-solid fa-users"></i>
                    <strong>${c.leadCount || 0}</strong>
                </button>
            </td>
            <td class="td-date">
                <div class="date-range-wrap">
                    <span>${c.startDate ? c.startDate : '...'}</span>
                    <span class="date-sep">~</span>
                    <span>${c.endDate ? c.endDate : '...'}</span>
                </div>
            </td>
            <td class="td-actions" style="text-align: right;">
                <div class="action-btn-group">
                    <button class="crm-btn crm-btn-secondary action-icon-btn" onclick="openLeadModal(${c.id}, '${escapeHtml(c.name)}')" title="Quản lý Leads">
                        <i class="fa-solid fa-users"></i>
                    </button>
                    <button class="crm-btn crm-btn-secondary action-icon-btn" onclick="openEditModal(${c.id})" title="Chỉnh sửa">
                        <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                    <button class="crm-btn crm-btn-secondary action-icon-btn btn-delete-action" onclick="handleDeleteCampaign(${c.id})" title="Xóa">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join("");

    renderPagination(pagination);
}

function renderPagination(pg) {
    const container = document.getElementById("campaignPagination");
    if (!container || !pg) return;
    container.innerHTML = `
        <span style="font-size:0.875rem; color: var(--text-secondary, #64748b);">
            Hiển thị trang <strong>${pg.page}</strong> / <strong>${pg.totalPages || 1}</strong> (Tổng số: <strong>${pg.totalItems}</strong> chiến dịch)
        </span>
        <div style="display:flex; gap:0.5rem;">
            <button class="crm-btn crm-btn-secondary btn-sm" ${pg.page <= 1 ? 'disabled style="opacity:0.5;cursor:not-allowed;"' : ''} onclick="changePage(${pg.page - 1})">
                <i class="fa-solid fa-angle-left"></i> Trang trước
            </button>
            <button class="crm-btn crm-btn-secondary btn-sm" ${pg.page >= pg.totalPages ? 'disabled style="opacity:0.5;cursor:not-allowed;"' : ''} onclick="changePage(${pg.page + 1})">
                Trang sau <i class="fa-solid fa-angle-right"></i>
            </button>
        </div>
    `;
}

function changePage(p) {
    if (p < 1) return;
    currentPage = p;
    loadCampaigns();
}

function openCampaignModal(c = null) {
    const modal = document.getElementById("campaignModal");
    const title = document.getElementById("campaignModalTitle");
    const form = document.getElementById("campaignForm");

    if (!modal || !form) return;
    form.reset();

    if (c) {
        title.innerText = "Chỉnh sửa Chiến dịch: " + c.code;
        document.getElementById("campaignId").value = c.id;
        document.getElementById("campaignCode").value = c.code;
        document.getElementById("campaignName").value = c.name;
        document.getElementById("campaignType").value = c.type;
        document.getElementById("campaignStatus").value = c.status;
        document.getElementById("campaignStartDate").value = c.startDate || "";
        document.getElementById("campaignEndDate").value = c.endDate || "";
        document.getElementById("campaignBudget").value = c.budget || 0;
        document.getElementById("campaignExpectedRevenue").value = c.expectedRevenue || 0;
        document.getElementById("campaignDescription").value = c.description || "";
    } else {
        title.innerText = "Tạo mới Chiến dịch";
        document.getElementById("campaignId").value = "";
        const rand = Math.floor(100 + Math.random() * 900);
        document.getElementById("campaignCode").value = `CAMP-2026-${rand}`;
        document.getElementById("campaignType").value = "EMAIL";
        document.getElementById("campaignStatus").value = "PLANNING";
        document.getElementById("campaignBudget").value = 10000000;
        document.getElementById("campaignExpectedRevenue").value = 50000000;
    }

    modal.classList.add("active");
}

function closeCampaignModal() {
    document.getElementById("campaignModal")?.classList.remove("active");
}

async function openEditModal(id) {
    if (!isOfflineMode) {
        try {
            const res = await apiFetch(`/campaigns/${id}`);
            openCampaignModal(res.data);
            return;
        } catch (err) {
            console.error("API error fetching detail:", err.message);
            notify("Không thể tải chi tiết chiến dịch: " + err.message, "error");
            return;
        }
    }

    const campaigns = getMockCampaigns();
    const c = campaigns.find(item => item.id === id);
    if (c) {
        openCampaignModal(c);
    } else {
        notify("Không tìm thấy thông tin chiến dịch!", "error");
    }
}

async function handleSaveCampaign(e) {
    e.preventDefault();
    const id = document.getElementById("campaignId").value;
    const code = document.getElementById("campaignCode").value.trim();
    const name = document.getElementById("campaignName").value.trim();
    const type = document.getElementById("campaignType").value;
    const status = document.getElementById("campaignStatus").value;
    const startDate = document.getElementById("campaignStartDate").value || null;
    const endDate = document.getElementById("campaignEndDate").value || null;
    const budget = parseFloat(document.getElementById("campaignBudget").value) || 0;
    const expectedRevenue = parseFloat(document.getElementById("campaignExpectedRevenue").value) || 0;
    const description = document.getElementById("campaignDescription").value.trim();

    // Validation (Sprint 4 Business Rules)
    if (!code) {
        notify("Mã chiến dịch không được để trống!", "error");
        return;
    }
    if (!name || name.length < 3) {
        notify("Tên chiến dịch phải có ít nhất 3 ký tự!", "error");
        return;
    }
    if (budget < 0) {
        notify("Ngân sách không được là số âm!", "error");
        return;
    }
    if (startDate && endDate && new Date(endDate) < new Date(startDate)) {
        notify("Ngày kết thúc không được trước ngày bắt đầu!", "error");
        return;
    }

    const payload = {
        code,
        name,
        type,
        status,
        startDate,
        endDate,
        budget,
        expectedRevenue,
        description
    };

    if (!isOfflineMode) {
        try {
            if (id) {
                await apiFetch(`/campaigns/${id}`, { method: "PUT", body: payload });
                notify("Cập nhật chiến dịch thành công!", "success");
            } else {
                await apiFetch(`/campaigns`, { method: "POST", body: payload });
                notify("Tạo mới chiến dịch thành công!", "success");
            }
            closeCampaignModal();
            loadCampaigns();
            return;
        } catch (err) {
            console.error("API save failed:", err.message);
            notify(err.message, "error");
            return;
        }
    }

    // Mock save
    const campaigns = getMockCampaigns();
    if (id) {
        const idx = campaigns.findIndex(c => c.id === parseInt(id));
        if (idx !== -1) {
            campaigns[idx] = { ...campaigns[idx], ...payload, id: parseInt(id) };
            saveMockCampaigns(campaigns);
            notify("Đã cập nhật chiến dịch!", "success");
        }
    } else {
        // Check duplicate code
        if (campaigns.some(c => c.code.toLowerCase() === code.toLowerCase())) {
            notify("Mã chiến dịch đã tồn tại! Vui lòng chọn mã khác.", "error");
            return;
        }
        const newId = Math.max(0, ...campaigns.map(c => c.id)) + 1;
        const newCampaign = { ...payload, id: newId, leadCount: 0 };
        campaigns.unshift(newCampaign);
        saveMockCampaigns(campaigns);
        notify("Đã tạo mới chiến dịch thành công!", "success");
    }

    closeCampaignModal();
    loadCampaigns();
}

function handleDeleteCampaign(id) {
    const campaigns = getMockCampaigns();
    const c = campaigns.find(item => item.id === id);
    const campaignName = c ? c.name : `Chiến dịch #${id}`;
    const campaignCode = c ? c.code : "";

    showConfirmDialog({
        title: "Xác nhận xóa chiến dịch",
        message: `Bạn có chắc chắn muốn xóa chiến dịch <strong>"${escapeHtml(campaignName)}"</strong>${campaignCode ? ` (<span style="color:var(--crm-primary);font-family:monospace;">${escapeHtml(campaignCode)}</span>)` : ""}? Thao tác này sẽ gỡ toàn bộ Lead liên kết và không thể hoàn tác.`,
        confirmText: "Xóa chiến dịch",
        onConfirm: async () => {
            if (!isOfflineMode) {
                try {
                    await apiFetch(`/campaigns/${id}`, { method: "DELETE" });
                    notify("Đã xóa chiến dịch thành công!", "success");
                    loadCampaigns();
                    return;
                } catch (err) {
                    console.error("API delete failed:", err.message);
                    notify(err.message, "error");
                    return;
                }
            }

            // Mock delete
            let currentList = getMockCampaigns();
            currentList = currentList.filter(item => item.id !== id);
            saveMockCampaigns(currentList);

            // Also remove linked leads
            let cl = getMockCampaignLeads();
            cl = cl.filter(item => item.campaignId !== id);
            saveMockCampaignLeads(cl);

            notify("Đã xóa chiến dịch thành công!", "success");
            loadCampaigns();
        }
    });
}

// ================= LEAD ASSOCIATION (S4-03-INT) =================
async function openLeadModal(campaignId, campaignName) {
    activeCampaignId = campaignId;
    document.getElementById("leadModalCampaignTitle").innerText = campaignName;
    document.getElementById("campaignLeadModal").classList.add("active");

    await loadLinkedLeads();
    await loadAvailableLeadsDropdown();
}

function closeLeadModal() {
    document.getElementById("campaignLeadModal")?.classList.remove("active");
    activeCampaignId = null;
    loadCampaigns();
}

async function loadLinkedLeads() {
    const listContainer = document.getElementById("linkedLeadsList");
    if (!listContainer || !activeCampaignId) return;

    listContainer.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 1.5rem; color: var(--text-secondary, #64748b);">
        <i class="fa-solid fa-spinner fa-spin"></i> Đang tải danh sách Lead...
    </td></tr>`;

    if (!isOfflineMode) {
        try {
            const res = await apiFetch(`/campaigns/${activeCampaignId}/leads`);
            renderLinkedLeads(res.data);
            return;
        } catch (err) {
            console.error("API leads error:", err.message);
            listContainer.innerHTML = `<tr><td colspan="5" style="text-align:center; padding: 1.5rem; color: #ef4444;">
                <i class="fa-solid fa-triangle-exclamation"></i> Lỗi tải danh sách Lead: ${escapeHtml(err.message)}
            </td></tr>`;
            return;
        }
    }

    // Mock leads
    const allLeads = getMockLeads();
    const clList = getMockCampaignLeads().filter(cl => cl.campaignId === activeCampaignId);
    const leads = clList.map(cl => {
        const lead = allLeads.find(l => l.id === cl.leadId) || { fullName: "Lead #" + cl.leadId, companyName: "-", phone: "-" };
        return {
            leadId: cl.leadId,
            leadName: lead.fullName,
            companyName: lead.companyName,
            phone: lead.phone,
            email: lead.email,
            status: cl.status,
            notes: cl.notes
        };
    });

    renderLinkedLeads(leads);
}

function getInitials(name) {
    if (!name) return "L";
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[parts.length - 2][0] + parts[parts.length - 1][0]).toUpperCase();
}

function getAvatarColor(name) {
    const gradients = [
        "linear-gradient(135deg, #3b82f6, #1d4ed8)",
        "linear-gradient(135deg, #8b5cf6, #6d28d9)",
        "linear-gradient(135deg, #10b981, #047857)",
        "linear-gradient(135deg, #f59e0b, #d97706)",
        "linear-gradient(135deg, #ec4899, #be185d)",
        "linear-gradient(135deg, #06b6d4, #0e7490)"
    ];
    let hash = 0;
    for (let i = 0; i < (name || "").length; i++) hash += name.charCodeAt(i);
    return gradients[Math.abs(hash) % gradients.length];
}

function renderLinkedLeads(leads) {
    const listContainer = document.getElementById("linkedLeadsList");
    const summaryText = document.getElementById("leadModalSummaryText");
    if (!listContainer) return;

    if (!leads || leads.length === 0) {
        listContainer.innerHTML = `<tr><td colspan="5" class="leads-empty-state">
            <div class="empty-icon"><i class="fa-regular fa-folder-open"></i></div>
            <div class="empty-text">Chưa có Khách hàng tiềm năng (Lead) nào trong chiến dịch này.</div>
            <div class="empty-subtext">Hãy chọn Lead từ danh sách phía trên và nhấn "Gán Lead" để bắt đầu.</div>
        </td></tr>`;
        if (summaryText) {
            summaryText.innerHTML = `
                <div class="lead-stats-container">
                    <span class="kpi-chip"><i class="fa-solid fa-users"></i> Tổng số: <strong>0 Leads</strong></span>
                </div>
            `;
        }
        return;
    }

    const convertedCount = leads.filter(l => l.status === "CONVERTED").length;
    const conversionRate = leads.length > 0 ? ((convertedCount / leads.length) * 100).toFixed(1) : 0;
    if (summaryText) {
        summaryText.innerHTML = `
            <div class="lead-stats-container">
                <span class="kpi-chip"><i class="fa-solid fa-users"></i> Tổng số: <strong>${leads.length} Leads</strong></span>
                <span class="kpi-chip kpi-chip-success"><i class="fa-solid fa-circle-check"></i> Đã chuyển đổi: <strong>${convertedCount} (${conversionRate}%)</strong></span>
            </div>
        `;
    }

    listContainer.innerHTML = leads.map(l => {
        const initials = getInitials(l.leadName);
        const avatarBg = getAvatarColor(l.leadName);
        return `
        <tr class="lead-row">
            <td>
                <div class="lead-cell-name">
                    <div class="lead-avatar" style="background: ${avatarBg};">${initials}</div>
                    <div class="lead-name-meta">
                        <div class="lead-fullname">${escapeHtml(l.leadName)}</div>
                        ${l.email ? `<div class="lead-email"><i class="fa-regular fa-envelope"></i> ${escapeHtml(l.email)}</div>` : ''}
                    </div>
                </div>
            </td>
            <td>
                <div class="lead-company">
                    <i class="fa-regular fa-building"></i>
                    <span>${escapeHtml(l.companyName || 'Khách vãng lai')}</span>
                </div>
            </td>
            <td>
                <div class="lead-phone">
                    <i class="fa-solid fa-phone"></i>
                    <span>${escapeHtml(l.phone || '-')}</span>
                </div>
            </td>
            <td>
                <div class="lead-status-wrapper">
                    <select class="lead-status-select select-status-${l.status}" onchange="handleUpdateLeadStatus(${l.leadId}, this.value, this)">
                        <option value="SENT" ${l.status === 'SENT' ? 'selected' : ''}>📨 SENT (Đã gửi)</option>
                        <option value="OPENED" ${l.status === 'OPENED' ? 'selected' : ''}>👀 OPENED (Đã mở)</option>
                        <option value="RESPONDED" ${l.status === 'RESPONDED' ? 'selected' : ''}>💬 RESPONDED (Phản hồi)</option>
                        <option value="CONVERTED" ${l.status === 'CONVERTED' ? 'selected' : ''}>🎯 CONVERTED (Chuyển đổi)</option>
                        <option value="BOUNCED" ${l.status === 'BOUNCED' ? 'selected' : ''}>⚠️ BOUNCED (Thất bại)</option>
                    </select>
                </div>
            </td>
            <td style="text-align: center;">
                <button class="btn-icon-danger" onclick="handleUnlinkLead(${l.leadId})" title="Gỡ Lead khỏi chiến dịch">
                    <i class="fa-solid fa-trash-can"></i>
                </button>
            </td>
        </tr>`;
    }).join("");
}

async function loadAvailableLeadsDropdown() {
    const select = document.getElementById("availableLeadsSelect");
    if (!select || !activeCampaignId) return;

    select.innerHTML = `<option value="">-- Đang tải danh sách Lead khả dụng... --</option>`;

    if (!isOfflineMode) {
        try {
            const res = await apiFetch(`/campaigns/${activeCampaignId}/available-leads`);
            renderAvailableLeadsDropdown(res.data);
            return;
        } catch (err) {
            console.error("API available leads error:", err.message);
            select.innerHTML = `<option value="">(Lỗi tải danh sách Lead: ${escapeHtml(err.message)})</option>`;
            return;
        }
    }

    // Mock available leads
    const allLeads = getMockLeads();
    const linkedLeadIds = getMockCampaignLeads()
        .filter(cl => cl.campaignId === activeCampaignId)
        .map(cl => cl.leadId);

    const available = allLeads.filter(l => !linkedLeadIds.includes(l.id));
    renderAvailableLeadsDropdown(available);
}

function renderAvailableLeadsDropdown(list) {
    const select = document.getElementById("availableLeadsSelect");
    if (!select) return;

    if (!list || list.length === 0) {
        select.innerHTML = `<option value="">(Tất cả Lead đã được gán vào chiến dịch này)</option>`;
        return;
    }

    select.innerHTML = `<option value="">-- Chọn Lead để thêm vào chiến dịch --</option>` +
        list.map(l => `<option value="${l.id}">${escapeHtml(l.fullName)} — ${escapeHtml(l.companyName || l.phone || 'Lead')}</option>`).join("");
}

async function handleAddLeadToCampaign() {
    const select = document.getElementById("availableLeadsSelect");
    const statusSelect = document.getElementById("initialLeadStatusSelect");
    const leadId = select ? select.value : null;
    const initialStatus = statusSelect ? statusSelect.value : "SENT";

    if (!leadId) {
        notify("Vui lòng chọn một Lead từ danh sách trước khi gán!", "error");
        return;
    }

    if (!isOfflineMode) {
        try {
            await apiFetch(`/campaigns/${activeCampaignId}/leads`, {
                method: "POST",
                body: { leadIds: [parseInt(leadId)], status: initialStatus }
            });
            notify("Đã thêm Lead vào chiến dịch thành công!", "success");
            loadLinkedLeads();
            loadAvailableLeadsDropdown();
            return;
        } catch (err) {
            console.error("API link lead failed:", err.message);
            notify(err.message, "error");
            return;
        }
    }

    // Mock link
    const cl = getMockCampaignLeads();
    cl.push({
        campaignId: activeCampaignId,
        leadId: parseInt(leadId),
        status: initialStatus,
        notes: "Gán vào chiến dịch"
    });
    saveMockCampaignLeads(cl);

    notify("Đã thêm Lead vào chiến dịch!", "success");
    loadLinkedLeads();
    loadAvailableLeadsDropdown();
}

async function handleUpdateLeadStatus(leadId, newStatus, selectEl) {
    if (selectEl) {
        selectEl.className = `lead-status-select select-status-${newStatus}`;
    }

    if (!isOfflineMode) {
        try {
            await apiFetch(`/campaigns/${activeCampaignId}/leads/${leadId}`, {
                method: "PUT",
                body: { status: newStatus }
            });
            notify("Đã cập nhật trạng thái tương tác của Lead!", "success");
            loadLinkedLeads();
            return;
        } catch (err) {
            console.error("API update lead status failed:", err.message);
            notify(err.message, "error");
            return;
        }
    }

    // Mock update status
    const cl = getMockCampaignLeads();
    const item = cl.find(i => i.campaignId === activeCampaignId && i.leadId === leadId);
    if (item) {
        item.status = newStatus;
        saveMockCampaignLeads(cl);
        notify("Đã cập nhật trạng thái tương tác Lead!", "success");
        loadLinkedLeads();
    }
}

function handleUnlinkLead(leadId) {
    const allLeads = getMockLeads();
    const lead = allLeads.find(l => l.id === leadId);
    const leadName = lead ? lead.fullName : `Lead #${leadId}`;

    showConfirmDialog({
        title: "Gỡ Lead khỏi chiến dịch",
        message: `Bạn có chắc chắn muốn gỡ khách hàng tiềm năng <strong>"${escapeHtml(leadName)}"</strong> khỏi chiến dịch này không?`,
        confirmText: "Gỡ Lead",
        onConfirm: async () => {
            if (!isOfflineMode) {
                try {
                    await apiFetch(`/campaigns/${activeCampaignId}/leads/${leadId}`, {
                        method: "DELETE"
                    });
                    notify("Đã gỡ Lead khỏi chiến dịch!", "success");
                    loadLinkedLeads();
                    loadAvailableLeadsDropdown();
                    return;
                } catch (err) {
                    console.error("API unlink lead failed:", err.message);
                    notify(err.message, "error");
                    return;
                }
            }

            // Mock unlink
            let cl = getMockCampaignLeads();
            cl = cl.filter(i => !(i.campaignId === activeCampaignId && i.leadId === leadId));
            saveMockCampaignLeads(cl);

            notify("Đã gỡ Lead khỏi chiến dịch!", "success");
            loadLinkedLeads();
            loadAvailableLeadsDropdown();
        }
    });
}

// Export Campaigns to CSV
function exportCampaignsToCSV() {
    const campaigns = (currentCampaignsList && currentCampaignsList.length > 0) ? currentCampaignsList : getMockCampaigns();
    if (!campaigns || campaigns.length === 0) {
        notify("Không có dữ liệu chiến dịch để xuất CSV!", "error");
        return;
    }

    const headers = ["Mã chiến dịch", "Tên chiến dịch", "Trạng thái", "Hình thức", "Ngày bắt đầu", "Ngày kết thúc", "Ngân sách", "Doanh thu kỳ vọng", "Mô tả"];
    const rows = campaigns.map(c => [
        `"${(c.code || '').replace(/"/g, '""')}"`,
        `"${(c.name || '').replace(/"/g, '""')}"`,
        `"${(c.status || '').replace(/"/g, '""')}"`,
        `"${(c.type || '').replace(/"/g, '""')}"`,
        `"${c.startDate || ''}"`,
        `"${c.endDate || ''}"`,
        c.budget || 0,
        c.expectedRevenue || 0,
        `"${(c.description || '').replace(/"/g, '""')}"`
    ]);

    const csvContent = "\uFEFF" + [headers.join(","), ...rows.map(r => r.join(","))].join("\r\n");
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `danh_sach_chien_dich_${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    notify("Đã xuất danh sách chiến dịch ra file CSV!", "success");
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function formatCurrency(val) {
    if (!val && val !== 0) return "0 đ";
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}

// Global window mappings for inline event handlers
window.handleDeleteCampaign = handleDeleteCampaign;
window.showConfirmDialog = showConfirmDialog;
window.closeConfirmDialog = closeConfirmDialog;
window.openCampaignModal = openCampaignModal;
window.closeCampaignModal = closeCampaignModal;
window.openEditModal = openEditModal;
window.openLeadModal = openLeadModal;
window.closeLeadModal = closeLeadModal;
window.handleUnlinkLead = handleUnlinkLead;
window.handleUpdateLeadStatus = handleUpdateLeadStatus;
window.exportCampaignsToCSV = exportCampaignsToCSV;
window.changePage = changePage;
window.loadCampaigns = loadCampaigns;

