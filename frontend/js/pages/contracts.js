"use strict";

const API_BASE = (window.CRM_BACKEND_BASE || `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`).replace(/\/$/, "");

let contractsData = [];
let pendingDeleteId = null;

document.addEventListener("DOMContentLoaded", () => {
    initContracts();

    document.getElementById("btnSearch")?.addEventListener("click", () => loadContracts());
    document.getElementById("statusFilter")?.addEventListener("change", () => loadContracts());
    document.getElementById("searchInput")?.addEventListener("keydown", (e) => {
        if (e.key === "Enter") loadContracts();
    });
    document.getElementById("globalSearch")?.addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
            const val = e.target.value.trim();
            const searchInput = document.getElementById("searchInput");
            if (searchInput) searchInput.value = val;
            loadContracts();
        }
    });
    document.getElementById("btnReset")?.addEventListener("click", () => {
        const searchInput = document.getElementById("searchInput");
        const statusFilter = document.getElementById("statusFilter");
        if (searchInput) searchInput.value = "";
        if (statusFilter) statusFilter.value = "";
        loadContracts();
    });
});

async function initContracts() {
    await loadContracts();
}

async function loadContracts() {
    const tableBody = document.getElementById("contractsTableBody");
    const emptyBox = document.getElementById("emptyContracts");
    const searchInput = document.getElementById("searchInput");
    const statusFilter = document.getElementById("statusFilter");

    const keyword = searchInput ? searchInput.value.trim() : "";
    const status = statusFilter ? statusFilter.value : "";

    if (tableBody) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="10" style="text-align:center; padding: 40px; color: #64748b;">
                    Đang tải danh sách hợp đồng...
                </td>
            </tr>
        `;
    }

    try {
        const url = new URL(`${API_BASE}/api/contracts`);
        if (keyword) url.searchParams.set("keyword", keyword);
        if (status) url.searchParams.set("status", status);

        const res = await fetch(url.toString(), {
            method: "GET",
            credentials: "include",
            headers: { "Accept": "application/json" }
        });

        if (res.status === 401) {
            window.location.href = "login.html";
            return;
        }

        const json = await res.json();
        if (!json || !json.success) {
            throw new Error(json?.message || "Không thể tải danh sách hợp đồng");
        }

        contractsData = json.data || [];
        renderContractsTable(contractsData);
        updateStats(contractsData);
    } catch (err) {
        console.error("Load contracts error:", err);
        if (tableBody) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="10" style="text-align:center; padding: 30px; color: #ef4444;">
                        Lỗi khi tải dữ liệu: ${escapeHtml(err.message)}
                    </td>
                </tr>
            `;
        }
        showToast("Lỗi tải danh sách hợp đồng: " + err.message, true);
    }
}

function renderContractsTable(list) {
    const tableBody = document.getElementById("contractsTableBody");
    const emptyBox = document.getElementById("emptyContracts");
    if (!tableBody) return;

    if (!list || list.length === 0) {
        tableBody.innerHTML = "";
        if (emptyBox) emptyBox.style.display = "flex";
        return;
    }

    if (emptyBox) emptyBox.style.display = "none";
    tableBody.innerHTML = "";

    list.forEach(ct => {
        const tr = document.createElement("tr");

        const statusClass = (ct.status || "draft").toLowerCase();
        const statusText = getStatusLabel(ct.status);
        const signedDate = ct.signedDate ? formatDate(ct.signedDate) : "—";
        const dateRange = (ct.startDate || ct.endDate)
            ? `${ct.startDate ? formatDate(ct.startDate) : "?"} - ${ct.endDate ? formatDate(ct.endDate) : "?"}`
            : "—";

        tr.innerHTML = `
            <td>
                <span class="quote-code" style="cursor:pointer;" onclick="openViewModal(${ct.id})">${escapeHtml(ct.contractNumber)}</span>
            </td>
            <td>
                <strong style="color:var(--crm-text, #1e293b);">${escapeHtml(ct.title)}</strong>
            </td>
            <td>
                <span style="font-weight:500;">${escapeHtml(ct.customerName || "—")}</span>
            </td>
            <td>
                ${ct.quoteNumber ? `<a href="quotations.html" style="color:#0284c7; text-decoration:none;">${escapeHtml(ct.quoteNumber)}</a>` : "—"}
            </td>
            <td style="text-align:right; font-weight:700; color:#0f172a;">
                ${formatMoney(ct.totalAmount)}
            </td>
            <td>${signedDate}</td>
            <td><small style="color:#64748b;">${dateRange}</small></td>
            <td>
                <span class="contract-badge ${statusClass}">${statusText}</span>
            </td>
            <td>
                <span style="color:#475569;">${escapeHtml(ct.ownerName || "—")}</span>
            </td>
            <td style="text-align:right;">
                <div class="btn-action-group">
                    <button class="btn-icon-sm" title="Xem chi tiết" onclick="openViewModal(${ct.id})"><i class="fa-solid fa-eye"></i> Xem</button>
                    <button class="btn-icon-sm edit" title="Chỉnh sửa" onclick="openEditModal(${ct.id})"><i class="fa-solid fa-pen-to-square"></i> Sửa</button>
                    <button class="btn-icon-sm delete" title="Xóa" onclick="openDeleteModal(${ct.id}, '${escapeHtml(ct.contractNumber)}')"><i class="fa-solid fa-trash-can"></i> Xóa</button>
                </div>
            </td>
        `;
        tableBody.appendChild(tr);
    });
}

function updateStats(list) {
    const totalCount = list.length;
    let activeCount = 0;
    let draftCount = 0;
    let totalAmount = 0;

    list.forEach(item => {
        const s = (item.status || "").toUpperCase();
        if (s === "ACTIVE") activeCount++;
        else if (s === "DRAFT") draftCount++;
        totalAmount += Number(item.totalAmount || 0);
    });

    const elTotal = document.getElementById("statTotalCount");
    const elActive = document.getElementById("statActiveCount");
    const elDraft = document.getElementById("statDraftCount");
    const elAmount = document.getElementById("statTotalAmount");

    if (elTotal) elTotal.textContent = totalCount;
    if (elActive) elActive.textContent = activeCount;
    if (elDraft) elDraft.textContent = draftCount;
    if (elAmount) elAmount.textContent = formatMoney(totalAmount);
}

function getStatusLabel(status) {
    const s = (status || "").toUpperCase();
    switch (s) {
        case "ACTIVE": return "Hiệu lực";
        case "DRAFT": return "Bản thảo";
        case "COMPLETED": return "Hoàn tất";
        case "EXPIRED": return "Hết hạn";
        case "CANCELLED": return "Đã hủy";
        default: return s || "Bản thảo";
    }
}

// Modal Xem
window.openViewModal = function(id) {
    const ct = contractsData.find(c => String(c.id) === String(id));
    if (!ct) return;

    document.getElementById("viewContractNumber").textContent = ct.contractNumber || "—";
    document.getElementById("viewTitle").textContent = ct.title || "—";
    document.getElementById("viewCustomerName").textContent = ct.customerName || "—";
    document.getElementById("viewQuoteNumber").textContent = ct.quoteNumber || "—";
    document.getElementById("viewOpportunityTitle").textContent = ct.opportunityTitle || "—";
    document.getElementById("viewTotalAmount").textContent = formatMoney(ct.totalAmount);
    document.getElementById("viewSignedDate").textContent = ct.signedDate ? formatDate(ct.signedDate) : "—";
    document.getElementById("viewStartDate").textContent = ct.startDate ? formatDate(ct.startDate) : "—";
    document.getElementById("viewEndDate").textContent = ct.endDate ? formatDate(ct.endDate) : "—";
    document.getElementById("viewStatus").innerHTML = `<span class="contract-badge ${(ct.status || 'draft').toLowerCase()}">${getStatusLabel(ct.status)}</span>`;
    document.getElementById("viewPaymentTerms").textContent = ct.paymentTerms || "Không có";
    document.getElementById("viewNotes").textContent = ct.notes || "Không có";
    document.getElementById("viewOwnerName").textContent = ct.ownerName || "—";
    document.getElementById("viewCreatedAt").textContent = ct.createdAt ? formatDate(ct.createdAt, true) : "—";

    const editBtn = document.getElementById("viewBtnEdit");
    if (editBtn) {
        editBtn.onclick = () => {
            closeViewModal();
            openEditModal(id);
        };
    }

    document.getElementById("viewModal")?.classList.add("open");
};

window.closeViewModal = function() {
    document.getElementById("viewModal")?.classList.remove("open");
};

// Modal Sửa
window.openEditModal = function(id) {
    const ct = contractsData.find(c => String(c.id) === String(id));
    if (!ct) return;

    document.getElementById("editContractId").value = ct.id;
    document.getElementById("editTitle").value = ct.title || "";
    document.getElementById("editContractNumber").value = ct.contractNumber || "";
    document.getElementById("editCustomerId").value = ct.customerId || "";
    document.getElementById("editCustomerDisplay").value = ct.customerName || "Khách hàng #" + ct.customerId;
    document.getElementById("editQuoteId").value = ct.quoteId || "";
    document.getElementById("editOpportunityId").value = ct.opportunityId || "";
    document.getElementById("editTotalAmount").value = ct.totalAmount || 0;
    document.getElementById("editStatus").value = (ct.status || "DRAFT").toUpperCase();
    document.getElementById("editSignedDate").value = ct.signedDate ? ct.signedDate.substring(0, 10) : "";
    document.getElementById("editStartDate").value = ct.startDate ? ct.startDate.substring(0, 10) : "";
    document.getElementById("editEndDate").value = ct.endDate ? ct.endDate.substring(0, 10) : "";
    document.getElementById("editPaymentTerms").value = ct.paymentTerms || "";
    document.getElementById("editNotes").value = ct.notes || "";

    document.getElementById("editModal")?.classList.add("open");
};

window.closeEditModal = function() {
    document.getElementById("editModal")?.classList.remove("open");
};

window.submitEditContract = async function() {
    const id = document.getElementById("editContractId").value;
    const title = document.getElementById("editTitle").value.trim();
    const contractNumber = document.getElementById("editContractNumber").value.trim();
    const customerId = document.getElementById("editCustomerId").value;
    const quoteId = document.getElementById("editQuoteId").value || null;
    const opportunityId = document.getElementById("editOpportunityId").value || null;
    const totalAmount = document.getElementById("editTotalAmount").value || 0;
    const status = document.getElementById("editStatus").value;
    const signedDate = document.getElementById("editSignedDate").value || null;
    const startDate = document.getElementById("editStartDate").value || null;
    const endDate = document.getElementById("editEndDate").value || null;
    const paymentTerms = document.getElementById("editPaymentTerms").value.trim();
    const notes = document.getElementById("editNotes").value.trim();

    if (!title) {
        showToast("Vui lòng nhập tên hợp đồng", true);
        return;
    }
    if (!contractNumber) {
        showToast("Vui lòng nhập số hợp đồng", true);
        return;
    }

    const payload = {
        title,
        contractNumber,
        customerId: Number(customerId),
        quoteId: quoteId ? Number(quoteId) : null,
        opportunityId: opportunityId ? Number(opportunityId) : null,
        totalAmount: Number(totalAmount),
        status,
        signedDate,
        startDate,
        endDate,
        paymentTerms,
        notes
    };

    const saveBtn = document.getElementById("btnSaveEdit");
    if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.textContent = "Đang lưu...";
    }

    try {
        const res = await fetch(`${API_BASE}/api/contracts/${id}`, {
            method: "PUT",
            credentials: "include",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify(payload)
        });

        const json = await res.json();
        if (!res.ok || !json.success) {
            throw new Error(json?.message || "Lỗi khi cập nhật hợp đồng");
        }

        showToast("Cập nhật hợp đồng thành công!");
        closeEditModal();
        await loadContracts();
    } catch (err) {
        showToast(err.message, true);
    } finally {
        if (saveBtn) {
            saveBtn.disabled = false;
            saveBtn.textContent = "Lưu thay đổi";
        }
    }
};

// Modal Xóa
window.openDeleteModal = function(id, contractNumber) {
    pendingDeleteId = id;
    const nameEl = document.getElementById("deleteContractName");
    if (nameEl) nameEl.textContent = contractNumber || `#${id}`;
    document.getElementById("deleteModal")?.classList.add("open");
};

window.closeDeleteModal = function() {
    pendingDeleteId = null;
    document.getElementById("deleteModal")?.classList.remove("open");
};

window.confirmDeleteContract = async function() {
    if (!pendingDeleteId) return;

    const delBtn = document.getElementById("btnConfirmDelete");
    if (delBtn) {
        delBtn.disabled = true;
        delBtn.textContent = "Đang xóa...";
    }

    try {
        const res = await fetch(`${API_BASE}/api/contracts/${pendingDeleteId}`, {
            method: "DELETE",
            credentials: "include",
            headers: { "Accept": "application/json" }
        });

        const json = await res.json();
        if (!res.ok || !json.success) {
            throw new Error(json?.message || "Lỗi khi xóa hợp đồng");
        }

        showToast("Đã xóa hợp đồng thành công!");
        closeDeleteModal();
        await loadContracts();
    } catch (err) {
        showToast(err.message, true);
    } finally {
        if (delBtn) {
            delBtn.disabled = false;
            delBtn.textContent = "Xác nhận Xóa";
        }
    }
};

// Helper utilities
function showToast(message, isError = false) {
    const toast = document.getElementById("toastMessage");
    if (!toast) return;
    toast.textContent = message;
    toast.className = `toast-msg ${isError ? 'error' : 'success'}`;
    toast.style.display = "block";
    setTimeout(() => {
        toast.style.display = "none";
    }, 3500);
}

function formatMoney(amount) {
    return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
        maximumFractionDigits: 0
    }).format(Number(amount) || 0);
}

function formatDate(dateStr, includeTime = false) {
    if (!dateStr) return "—";
    try {
        const d = new Date(dateStr);
        if (isNaN(d.getTime())) return dateStr;
        const day = String(d.getDate()).padStart(2, '0');
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const year = d.getFullYear();
        if (includeTime) {
            const h = String(d.getHours()).padStart(2, '0');
            const m = String(d.getMinutes()).padStart(2, '0');
            return `${day}/${month}/${year} ${h}:${m}`;
        }
        return `${day}/${month}/${year}`;
    } catch {
        return dateStr;
    }
}

function escapeHtml(str) {
    if (str == null) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
