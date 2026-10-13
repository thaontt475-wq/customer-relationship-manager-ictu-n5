// campaigns.js - Frontend controller for S4-03 Campaigns & Lead Association
const API_BASE = "http://localhost:8080/crm/api";

let currentPage = 1;
let currentSize = 10;
let currentKeyword = "";
let currentStatus = "";
let activeCampaignId = null;

document.addEventListener("DOMContentLoaded", () => {
    loadCampaigns();

    // Search & Filter listeners
    const searchInput = document.getElementById("searchCampaignInput");
    const statusSelect = document.getElementById("statusFilterSelect");
    const btnSearch = document.getElementById("btnSearchCampaign");

    btnSearch?.addEventListener("click", () => {
        currentKeyword = searchInput ? searchInput.value.trim() : "";
        currentStatus = statusSelect ? statusSelect.value : "";
        currentPage = 1;
        loadCampaigns();
    });

    statusSelect?.addEventListener("change", () => {
        currentStatus = statusSelect.value;
        currentPage = 1;
        loadCampaigns();
    });

    // Modal Create / Edit listeners
    document.getElementById("btnOpenCreateModal")?.addEventListener("click", () => {
        openCampaignModal();
    });

    document.getElementById("campaignForm")?.addEventListener("submit", handleSaveCampaign);

    // Modal Link Leads listeners
    document.getElementById("btnAddLeadToCampaign")?.addEventListener("click", handleAddLeadToCampaign);
});

async function apiFetch(url, options = {}) {
    options.credentials = "include";
    if (options.body && typeof options.body === "object" && !(options.body instanceof FormData)) {
        options.headers = {
            ...options.headers,
            "Content-Type": "application/json"
        };
        options.body = JSON.stringify(options.body);
    }
    const response = await fetch(API_BASE + url, options);
    const data = await response.json();
    if (!response.ok || !data.success) {
        throw new Error(data.message || "Đã xảy ra lỗi khi gọi máy chủ");
    }
    return data;
}

async function loadCampaigns() {
    const tableBody = document.getElementById("campaignTableBody");
    if (!tableBody) return;
    tableBody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding: 2rem;">Đang tải dữ liệu chiến dịch...</td></tr>`;

    try {
        let url = `/campaigns?page=${currentPage}&size=${currentSize}`;
        if (currentKeyword) url += `&keyword=${encodeURIComponent(currentKeyword)}`;
        if (currentStatus) url += `&status=${encodeURIComponent(currentStatus)}`;

        const res = await apiFetch(url);
        const { items, pagination } = res.data;

        if (!items || items.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding: 2rem; color: #64748b;">Không có chiến dịch nào phù hợp.</td></tr>`;
            return;
        }

        tableBody.innerHTML = items.map(c => `
            <tr>
                <td><strong>${escapeHtml(c.code)}</strong></td>
                <td>${escapeHtml(c.name)}</td>
                <td><span class="status-badge status-${c.status}">${c.status}</span></td>
                <td>${c.type || '-'}</td>
                <td>${formatCurrency(c.budget)}</td>
                <td><span class="badge" style="background:#e2e8f0; padding:2px 8px; border-radius:12px;">${c.leadCount || 0} Leads</span></td>
                <td>${c.startDate || '-'} ~ ${c.endDate || '-'}</td>
                <td style="text-align: right; white-space: nowrap;">
                    <button class="btn btn-secondary btn-sm" onclick="openLeadModal(${c.id}, '${escapeHtml(c.name)}')">👥 Leads</button>
                    <button class="btn btn-secondary btn-sm" onclick="openEditModal(${c.id})">✏️ Sửa</button>
                    <button class="btn btn-danger btn-sm" onclick="handleDeleteCampaign(${c.id})">🗑️</button>
                </td>
            </tr>
        `).join("");

        renderPagination(pagination);
    } catch (err) {
        tableBody.innerHTML = `<tr><td colspan="8" style="text-align:center; color: #ef4444; padding: 2rem;">Lỗi: ${escapeHtml(err.message)}</td></tr>`;
    }
}

function renderPagination(pg) {
    const container = document.getElementById("campaignPagination");
    if (!container || !pg) return;
    container.innerHTML = `
        <span style="font-size:0.875rem; color:#64748b;">Trang ${pg.page} / ${pg.totalPages || 1} (Tổng: ${pg.totalItems} chiến dịch)</span>
        <div style="display:flex; gap:0.5rem;">
            <button class="btn btn-secondary btn-sm" ${pg.page <= 1 ? 'disabled' : ''} onclick="changePage(${pg.page - 1})">Trước</button>
            <button class="btn btn-secondary btn-sm" ${pg.page >= pg.totalPages ? 'disabled' : ''} onclick="changePage(${pg.page + 1})">Sau</button>
        </div>
    `;
}

function changePage(p) {
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
        title.innerText = "Chỉnh sửa Chiến dịch";
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
        document.getElementById("campaignCode").value = "CAMP-" + Math.floor(1000 + Math.random() * 9000);
    }

    modal.classList.add("active");
}

function closeCampaignModal() {
    document.getElementById("campaignModal")?.classList.remove("active");
}

async function openEditModal(id) {
    try {
        const res = await apiFetch(`/campaigns/${id}`);
        openCampaignModal(res.data);
    } catch (err) {
        alert("Lỗi tải thông tin chiến dịch: " + err.message);
    }
}

async function handleSaveCampaign(e) {
    e.preventDefault();
    const id = document.getElementById("campaignId").value;
    const payload = {
        code: document.getElementById("campaignCode").value.trim(),
        name: document.getElementById("campaignName").value.trim(),
        type: document.getElementById("campaignType").value,
        status: document.getElementById("campaignStatus").value,
        startDate: document.getElementById("campaignStartDate").value || null,
        endDate: document.getElementById("campaignEndDate").value || null,
        budget: parseFloat(document.getElementById("campaignBudget").value) || 0,
        expectedRevenue: parseFloat(document.getElementById("campaignExpectedRevenue").value) || 0,
        description: document.getElementById("campaignDescription").value.trim()
    };

    try {
        if (id) {
            await apiFetch(`/campaigns/${id}`, { method: "PUT", body: payload });
            alert("Cập nhật chiến dịch thành công!");
        } else {
            await apiFetch(`/campaigns`, { method: "POST", body: payload });
            alert("Tạo chiến dịch mới thành công!");
        }
        closeCampaignModal();
        loadCampaigns();
    } catch (err) {
        alert("Lỗi: " + err.message);
    }
}

async function handleDeleteCampaign(id) {
    if (!confirm("Bạn có chắc chắn muốn xóa chiến dịch này?")) return;
    try {
        await apiFetch(`/campaigns/${id}`, { method: "DELETE" });
        alert("Đã xóa chiến dịch thành công!");
        loadCampaigns();
    } catch (err) {
        alert("Lỗi xóa chiến dịch: " + err.message);
    }
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

    listContainer.innerHTML = `<tr><td colspan="5" style="text-align:center;">Đang tải danh sách Lead...</td></tr>`;
    try {
        const res = await apiFetch(`/campaigns/${activeCampaignId}/leads`);
        const leads = res.data;
        if (!leads || leads.length === 0) {
            listContainer.innerHTML = `<tr><td colspan="5" style="text-align:center; color:#64748b;">Chưa có Lead nào trong chiến dịch này.</td></tr>`;
            return;
        }

        listContainer.innerHTML = leads.map(l => `
            <tr>
                <td><strong>${escapeHtml(l.leadName)}</strong></td>
                <td>${escapeHtml(l.companyName || '-')}</td>
                <td>${escapeHtml(l.phone || l.email || '-')}</td>
                <td>
                    <select onchange="handleUpdateLeadStatus(${l.leadId}, this.value)" style="padding:2px 6px; border-radius:4px; font-size:0.75rem;">
                        <option value="SENT" ${l.status === 'SENT' ? 'selected' : ''}>SENT</option>
                        <option value="OPENED" ${l.status === 'OPENED' ? 'selected' : ''}>OPENED</option>
                        <option value="RESPONDED" ${l.status === 'RESPONDED' ? 'selected' : ''}>RESPONDED</option>
                        <option value="CONVERTED" ${l.status === 'CONVERTED' ? 'selected' : ''}>CONVERTED</option>
                        <option value="BOUNCED" ${l.status === 'BOUNCED' ? 'selected' : ''}>BOUNCED</option>
                    </select>
                </td>
                <td style="text-align:right;">
                    <button class="btn btn-danger btn-sm" onclick="handleUnlinkLead(${l.leadId})">Gỡ</button>
                </td>
            </tr>
        `).join("");
    } catch (err) {
        listContainer.innerHTML = `<tr><td colspan="5" style="color:red; text-align:center;">Lỗi: ${err.message}</td></tr>`;
    }
}

async function loadAvailableLeadsDropdown() {
    const select = document.getElementById("availableLeadsSelect");
    if (!select || !activeCampaignId) return;

    select.innerHTML = `<option value="">-- Đang tải Lead khả dụng --</option>`;
    try {
        const res = await apiFetch(`/campaigns/${activeCampaignId}/available-leads`);
        const list = res.data;
        if (!list || list.length === 0) {
            select.innerHTML = `<option value="">(Không còn Lead khả dụng)</option>`;
            return;
        }
        select.innerHTML = `<option value="">-- Chọn Lead để thêm vào chiến dịch --</option>` +
            list.map(l => `<option value="${l.id}">${escapeHtml(l.fullName)} (${escapeHtml(l.companyName || l.phone || 'Lead')})</option>`).join("");
    } catch (err) {
        select.innerHTML = `<option value="">Lỗi tải Lead khả dụng</option>`;
    }
}

async function handleAddLeadToCampaign() {
    const select = document.getElementById("availableLeadsSelect");
    const leadId = select ? select.value : null;
    if (!leadId) {
        alert("Vui lòng chọn Lead cần thêm!");
        return;
    }

    try {
        await apiFetch(`/campaigns/${activeCampaignId}/leads`, {
            method: "POST",
            body: { leadIds: [parseInt(leadId)], status: "SENT" }
        });
        alert("Đã thêm Lead vào chiến dịch!");
        loadLinkedLeads();
        loadAvailableLeadsDropdown();
    } catch (err) {
        alert("Lỗi thêm Lead: " + err.message);
    }
}

async function handleUpdateLeadStatus(leadId, newStatus) {
    try {
        await apiFetch(`/campaigns/${activeCampaignId}/leads/${leadId}`, {
            method: "PUT",
            body: { status: newStatus }
        });
    } catch (err) {
        alert("Lỗi cập nhật trạng thái Lead: " + err.message);
        loadLinkedLeads();
    }
}

async function handleUnlinkLead(leadId) {
    if (!confirm("Gỡ Lead này khỏi chiến dịch?")) return;
    try {
        await apiFetch(`/campaigns/${activeCampaignId}/leads/${leadId}`, {
            method: "DELETE"
        });
        loadLinkedLeads();
        loadAvailableLeadsDropdown();
    } catch (err) {
        alert("Lỗi: " + err.message);
    }
}

function escapeHtml(str) {
    if (!str) return "";
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function formatCurrency(val) {
    if (!val) return "0 đ";
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
}
