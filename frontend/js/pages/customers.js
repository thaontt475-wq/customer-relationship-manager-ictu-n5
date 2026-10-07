"use strict";

const API_BASE = "http://localhost:8080/crm";

let records = [];

const tableBody = document.getElementById("customerTableBody");
const desktopEmpty = document.getElementById("customerEmpty");
const mobileList = document.getElementById("customerMobileList");
const drawer = document.getElementById("customerDrawer");
const drawerOverlay = document.getElementById("customerDrawerOverlay");
const openDrawerButton = document.getElementById("openCustomerDrawer");
const closeDrawerButton = document.getElementById("closeCustomerDrawer");
const cancelButton = document.getElementById("cancelCustomer");
const form = document.getElementById("customerForm");
const searchInput = document.getElementById("customerSearch");
const statusFilter = document.getElementById("statusFilter");

let searchDebounceTimer = null;

async function api(path, options = {}) {
    const config = {
        credentials: "include",
        headers: {
            "Accept": "application/json",
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...(options.headers || {})
        },
        ...options
    };

    const response = await fetch(API_BASE + path, config);
    let result = null;
    try {
        result = await response.json();
    } catch (_) {
        result = null;
    }

    if (response.status === 401) {
        localStorage.removeItem("crm_ui_session");
        window.location.href = "login.html";
        throw new Error("Phiên đăng nhập đã hết hạn.");
    }

    if (!response.ok || !result?.success) {
        const error = new Error(result?.message || `HTTP ${response.status}`);
        error.status = response.status;
        error.data = result?.data;
        throw error;
    }

    return result.data;
}

openDrawerButton?.addEventListener("click", () => openDrawer());
closeDrawerButton?.addEventListener("click", closeDrawer);
cancelButton?.addEventListener("click", closeDrawer);
drawerOverlay?.addEventListener("click", closeDrawer);

document.addEventListener("keydown", event => {
    if (event.key === "Escape") {
        closeDrawer();
    }
});

form?.addEventListener("submit", async event => {
    event.preventDefault();

    const companyName = value("companyName");
    const error = document.getElementById("companyNameError");

    if (!companyName) {
        error.textContent = "Vui lòng nhập tên công ty.";
        return;
    }

    error.textContent = "";

    const payload = {
        companyName,
        name: companyName,
        taxCode: value("taxCode"),
        status: value("customerStatus") || "TIEM_NANG",
        email: value("customerEmail"),
        phone: value("customerPhone"),
        industryId: value("industry") ? Number(value("industry")) : null,
        companySizeId: value("companySize") ? Number(value("companySize")) : null,
        website: value("website"),
        address: value("address")
    };

    const editingIndex = document.getElementById("editingIndex").value;
    const submitBtn = form.querySelector('button[type="submit"]');
    const originalText = submitBtn ? submitBtn.textContent : "";
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = "Đang lưu...";
    }

    try {
        if (editingIndex === "") {
            await api("/api/customers", {
                method: "POST",
                body: JSON.stringify(payload)
            });
        } else {
            const currentRecord = records[Number(editingIndex)];
            if (currentRecord?.id) {
                await api(`/api/customers/${currentRecord.id}`, {
                    method: "PUT",
                    body: JSON.stringify(payload)
                });
            }
        }

        closeDrawer();
        await loadCustomers();
    } catch (err) {
        alert(err.message || "Không thể lưu thông tin khách hàng.");
    } finally {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = originalText;
        }
    }
});

searchInput?.addEventListener("input", () => {
    clearTimeout(searchDebounceTimer);
    searchDebounceTimer = setTimeout(() => {
        loadCustomers();
    }, 300);
});

statusFilter?.addEventListener("change", () => {
    loadCustomers();
});

async function loadCustomers() {
    try {
        const query = searchInput ? searchInput.value.trim() : "";
        const status = statusFilter ? statusFilter.value : "";
        let url = `/api/customers?size=100`;
        if (query) url += `&keyword=${encodeURIComponent(query)}`;
        if (status) url += `&status=${encodeURIComponent(status)}`;

        const data = await api(url);
        records = data?.items || [];
        render();
    } catch (error) {
        console.error("Lỗi tải danh sách khách hàng:", error);
        tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:red; padding:20px;">${escapeHtml(error.message)}</td></tr>`;
    }
}

function render() {
    tableBody.innerHTML = "";
    mobileList.innerHTML = "";

    desktopEmpty.style.display = records.length ? "none" : "flex";

    if (!records.length) {
        const empty = document.createElement("div");
        empty.className = "customer-mobile-empty";
        empty.textContent = "Không có bản ghi phù hợp.";
        mobileList.appendChild(empty);
        return;
    }

    for (let index = 0; index < records.length; index++) {
        const record = records[index];
        renderDesktopRow(record, index);
        renderMobileCard(record, index);
    }
}

function renderDesktopRow(record, index) {
    const tr = document.createElement("tr");

    tr.innerHTML = `
        <td>
            <input type="checkbox">
        </td>
        <td>
            <a class="customer-name" href="customer-360.html?id=${record.id}">
                ${escapeHtml(record.companyName || record.name)}
            </a>
            <span class="customer-sub">
                ${escapeHtml(record.taxCode || "—")}
            </span>
        </td>
        <td>
            ${escapeHtml(record.email || "—")}
            <span class="customer-sub">
                ${escapeHtml(record.phone || "—")}
            </span>
        </td>
        <td>
            <span class="status-pill ${statusClass(record.status)}">
                ${escapeHtml(record.status)}
            </span>
        </td>
        <td>
            ${escapeHtml(record.ownerName || record.owner || "—")}
        </td>
        <td class="action-col">
            <div class="row-action-wrap">
                <button class="row-action-button" type="button" data-action-menu="${index}">
                    ⋮
                </button>
                <div class="row-action-menu" data-menu="${index}">
                    <a href="customer-360.html?id=${record.id}">
                        Xem chi tiết
                    </a>
                    <button type="button" data-edit="${index}">
                        Sửa
                    </button>
                    <button type="button" class="danger" data-delete="${index}">
                        Xóa
                    </button>
                </div>
            </div>
        </td>
    `;

    tableBody.appendChild(tr);
}

function renderMobileCard(record, index) {
    const card = document.createElement("article");
    card.className = "customer-mobile-card";

    card.innerHTML = `
        <div class="mobile-card-head">
            <div>
                <strong>
                    ${escapeHtml(record.companyName || record.name)}
                </strong>
                <div style="margin-top:6px">
                    <span class="status-pill ${statusClass(record.status)}">
                        ${escapeHtml(record.status)}
                    </span>
                </div>
            </div>
            <button class="row-action-button" type="button" data-edit="${index}">
                ⋮
            </button>
        </div>
        <div class="mobile-card-contact">
            <span>✉ ${escapeHtml(record.email || "—")}</span>
            <span>☎ ${escapeHtml(record.phone || "—")}</span>
            <span>Người sở hữu: ${escapeHtml(record.ownerName || record.owner || "—")}</span>
        </div>
        <a href="customer-360.html?id=${record.id}" class="crm-btn crm-btn-secondary" style="margin-top:12px; width:100%; text-decoration:none;">
            Xem chi tiết
        </a>
    `;

    mobileList.appendChild(card);
}

document.addEventListener("click", async event => {
    const actionButton = event.target.closest("[data-action-menu]");
    if (actionButton) {
        const index = actionButton.dataset.actionMenu;
        document.querySelectorAll(".row-action-menu").forEach(menu => {
            if (menu.dataset.menu !== index) {
                menu.classList.remove("open");
            }
        });
        document.querySelector(`[data-menu="${index}"]`)?.classList.toggle("open");
        return;
    }

    const editButton = event.target.closest("[data-edit]");
    if (editButton) {
        openDrawer(Number(editButton.dataset.edit));
        return;
    }

    const deleteButton = event.target.closest("[data-delete]");
    if (deleteButton) {
        const index = Number(deleteButton.dataset.delete);
        const record = records[index];
        if (record && confirm(`Bạn có chắc chắn muốn xóa khách hàng "${record.companyName || record.name}"?`)) {
            try {
                await api(`/api/customers/${record.id}`, { method: "DELETE" });
                await loadCustomers();
            } catch (err) {
                alert(err.message || "Không thể xóa khách hàng.");
            }
        }
        return;
    }

    if (!event.target.closest(".row-action-wrap")) {
        document.querySelectorAll(".row-action-menu").forEach(menu => {
            menu.classList.remove("open");
        });
    }
});

function openDrawer(index = null) {
    resetForm();

    if (index !== null && records[index]) {
        const record = records[index];
        document.getElementById("drawerTitle").textContent = "Sửa Khách hàng / Lead";
        document.getElementById("editingIndex").value = String(index);

        setValue("companyName", record.companyName || record.name);
        setValue("taxCode", record.taxCode);
        setValue("customerStatus", record.status);
        setValue("customerEmail", record.email);
        setValue("customerPhone", record.phone);
        setValue("industry", record.industryId || record.industry);
        setValue("companySize", record.companySizeId || record.companySize);
        setValue("website", record.website);
        setValue("address", record.address);
        setValue("owner", record.ownerName || record.owner);
    }

    drawer?.classList.add("open");
    drawerOverlay?.classList.add("open");
}

function closeDrawer() {
    drawer?.classList.remove("open");
    drawerOverlay?.classList.remove("open");
}

function resetForm() {
    form?.reset();
    document.getElementById("editingIndex").value = "";
    document.getElementById("drawerTitle").textContent = "Thêm mới Khách hàng / Lead";
    document.getElementById("companyNameError").textContent = "";
}

function statusClass(status) {
    switch (status) {
        case "Đang giao dịch":
        case "DANG_GIAO_DICH":
            return "status-dealing";
        case "Khách hàng":
        case "CHINH_THUC":
            return "status-customer";
        case "Ngừng hợp tác":
        case "NGUNG_HOP_TAC":
            return "status-inactive";
        default:
            return "status-prospect";
    }
}

function value(id) {
    const el = document.getElementById(id);
    return el ? el.value.trim() : "";
}

function setValue(id, val) {
    const el = document.getElementById(id);
    if (el) el.value = val || "";
}

function escapeHtml(val) {
    return String(val || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// Bind Export button
document.getElementById("exportCustomerExcel")?.addEventListener("click", () => {
    if (!records.length) {
        alert("Không có dữ liệu để xuất Excel.");
        return;
    }
    const headers = ["ID", "Tên công ty", "Mã số thuế", "Trạng thái", "Email", "Điện thoại", "Người sở hữu"];
    const rows = records.map(r => [
        r.id,
        `"${(r.companyName || r.name || "").replace(/"/g, '""')}"`,
        `"${(r.taxCode || "").replace(/"/g, '""')}"`,
        `"${(r.status || "").replace(/"/g, '""')}"`,
        `"${(r.email || "").replace(/"/g, '""')}"`,
        `"${(r.phone || "").replace(/"/g, '""')}"`,
        `"${(r.ownerName || r.owner || "").replace(/"/g, '""')}"`
    ]);
    const csvContent = "\uFEFF" + [headers.join(","), ...rows.map(e => e.join(","))].join("\n");
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `Danh_sach_khach_hang_${new Date().toISOString().slice(0,10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
});

// Initial load
document.addEventListener("DOMContentLoaded", loadCustomers);
if (document.readyState === "complete" || document.readyState === "interactive") {
    loadCustomers();
}