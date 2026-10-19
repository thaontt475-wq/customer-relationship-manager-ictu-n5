"use strict";

const API_BASE = (window.CRM_BACKEND_BASE || `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`).replace(/\/$/, "");

let contractsData = [];
let pendingDeleteId = null;
let activeKpiFilter = "";

document.addEventListener("DOMContentLoaded", () => {
    initContracts();

    document.getElementById("btnSearch")?.addEventListener("click", () => render());
    document.getElementById("statusFilter")?.addEventListener("change", (e) => {
        activeKpiFilter = e.target.value;
        updateKpiActiveCard();
        render();
    });
    document.getElementById("sortFilter")?.addEventListener("change", () => render());
    document.getElementById("searchInput")?.addEventListener("input", () => render());
    document.getElementById("globalSearch")?.addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
            const val = e.target.value.trim();
            const searchInput = document.getElementById("searchInput");
            if (searchInput) searchInput.value = val;
            render();
        }
    });

    document.getElementById("btnReset")?.addEventListener("click", () => {
        const searchInput = document.getElementById("searchInput");
        const statusFilter = document.getElementById("statusFilter");
        const sortFilter = document.getElementById("sortFilter");
        if (searchInput) searchInput.value = "";
        if (statusFilter) statusFilter.value = "";
        if (sortFilter) sortFilter.value = "newest";
        activeKpiFilter = "";
        updateKpiActiveCard();
        render();
    });

    document.getElementById("btnRefreshContracts")?.addEventListener("click", async () => {
        const icon = document.querySelector("#btnRefreshContracts i");
        if (icon) icon.classList.add("fa-spin");
        await loadContracts();
        setTimeout(() => {
            if (icon) icon.classList.remove("fa-spin");
        }, 500);
    });

    // KPI Card Click Filter
    document.querySelectorAll(".kpi-card-contract[data-filter-status]").forEach(card => {
        card.addEventListener("click", () => {
            const st = card.dataset.filterStatus;
            if (activeKpiFilter === st) {
                activeKpiFilter = "";
                document.getElementById("statusFilter").value = "";
            } else {
                activeKpiFilter = st;
                document.getElementById("statusFilter").value = st;
            }
            updateKpiActiveCard();
            render();
        });
    });

    // Export Excel/CSV
    document.getElementById("btnExportContracts")?.addEventListener("click", exportContractsCSV);

    // Print Modal Events
    document.getElementById("btnCloseContractPrint")?.addEventListener("click", closeContractPrint);
    document.getElementById("btnCloseContractPrintBtn")?.addEventListener("click", closeContractPrint);
    document.getElementById("btnExecutePrintDoc")?.addEventListener("click", () => window.print());
    document.getElementById("viewBtnPrint")?.addEventListener("click", () => {
        const currentViewId = document.getElementById("viewBtnEdit")?.dataset.editId;
        if (currentViewId) {
            closeViewModal();
            openContractPrint(currentViewId);
        }
    });
});

async function initContracts() {
    await loadContracts();
}

async function loadContracts() {
    const tableBody = document.getElementById("contractsTableBody");

    if (tableBody) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align:center; padding: 40px; color: var(--crm-muted);">
                    <i class="fa-solid fa-circle-notch fa-spin"></i> Đang tải danh sách hợp đồng từ MySQL...
                </td>
            </tr>
        `;
    }

    try {
        const res = await fetch(`${API_BASE}/api/contracts`, {
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
        updateStats(contractsData);
        render();
    } catch (err) {
        console.error("Load contracts error:", err);
        if (tableBody) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align:center; padding: 30px; color: #ef4444;">
                        Lỗi khi tải dữ liệu từ máy chủ: ${escapeHtml(err.message)}
                    </td>
                </tr>
            `;
        }
        showToast("Lỗi tải danh sách hợp đồng: " + err.message, true);
    }
}

function updateKpiActiveCard() {
    document.querySelectorAll(".kpi-card-contract").forEach(c => {
        if (c.dataset.filterStatus === activeKpiFilter && activeKpiFilter !== "") {
            c.classList.add("active-filter");
        } else {
            c.classList.remove("active-filter");
        }
    });
}

function updateStats(list) {
    const totalCount = list.length;
    let activeCount = 0;
    let draftCount = 0;
    let totalAmount = 0;
    let completedCount = 0;

    for (const item of list) {
        const st = (item.status || "").toUpperCase();
        if (st === "ACTIVE") activeCount++;
        else if (st === "DRAFT") draftCount++;
        else if (st === "COMPLETED") completedCount++;

        totalAmount += Number(item.total_amount || item.totalAmount || 0);
    }

    const rate = totalCount > 0 ? Math.round(((activeCount + completedCount) / totalCount) * 100) : 0;

    const elTotal = document.getElementById("statTotalCount");
    const elActive = document.getElementById("statActiveCount");
    const elDraft = document.getElementById("statDraftCount");
    const elAmount = document.getElementById("statTotalAmount");
    const elRate = document.getElementById("statCompletionRate");

    if (elTotal) elTotal.textContent = totalCount;
    if (elActive) elActive.textContent = activeCount;
    if (elDraft) elDraft.textContent = draftCount;
    if (elAmount) elAmount.textContent = formatMoney(totalAmount);
    if (elRate) elRate.textContent = `${rate}%`;
}

function render() {
    const tableBody = document.getElementById("contractsTableBody");
    const emptyBox = document.getElementById("emptyContracts");
    const searchInput = document.getElementById("searchInput");
    const statusSelect = document.getElementById("statusFilter");
    const sortSelect = document.getElementById("sortFilter");
    const countBadge = document.getElementById("contractCountBadge");

    const keyword = searchInput ? searchInput.value.trim().toLowerCase() : "";
    const status = activeKpiFilter || (statusSelect ? statusSelect.value : "");
    const sort = sortSelect ? sortSelect.value : "newest";

    let filtered = contractsData.filter(item => {
        const text = `${item.contract_number || ''} ${item.title || ''} ${item.customer_name || ''} ${item.quote_number || ''}`.toLowerCase();
        const matchesKey = !keyword || text.includes(keyword);
        const matchesStatus = !status || (item.status || "").toUpperCase() === status.toUpperCase();
        return matchesKey && matchesStatus;
    });

    // Sorting
    filtered.sort((a, b) => {
        const amtA = Number(a.total_amount || a.totalAmount || 0);
        const amtB = Number(b.total_amount || b.totalAmount || 0);
        if (sort === "amount_desc") return amtB - amtA;
        if (sort === "amount_asc") return amtA - amtB;
        if (sort === "oldest") return (a.id || 0) - (b.id || 0);
        return (b.id || 0) - (a.id || 0);
    });

    if (countBadge) countBadge.textContent = filtered.length;

    if (!filtered || filtered.length === 0) {
        if (tableBody) tableBody.innerHTML = "";
        if (emptyBox) emptyBox.style.display = "flex";
        return;
    }

    if (emptyBox) emptyBox.style.display = "none";
    if (!tableBody) return;

    tableBody.innerHTML = "";

    filtered.forEach(c => {
        const tr = document.createElement("tr");

        const cNum = c.contract_number || c.contractNumber || `HD-${c.id}`;
        const title = c.title || "Hợp đồng kinh tế";
        const custName = c.customer_name || c.customerName || "Khách hàng";
        const quoteNum = c.quote_number || c.quoteNumber || "";
        const amount = Number(c.total_amount || c.totalAmount || 0);
        const status = (c.status || "DRAFT").toUpperCase();
        const startDate = c.start_date || c.startDate || "";
        const endDate = c.end_date || c.endDate || "";

        // Avatar Initials
        const initials = custName.split(" ").filter(Boolean).slice(0, 2).map(w => w[0].toUpperCase()).join("");

        tr.innerHTML = `
            <td>
                <span class="contract-number-badge" title="Mã hợp đồng">
                    <i class="fa-solid fa-file-signature" style="font-size:0.75rem;"></i>
                    ${escapeHtml(cNum)}
                </span>
            </td>
            <td>
                <div style="font-weight:700; color:var(--crm-text); line-height:1.3;">
                    ${escapeHtml(title)}
                </div>
                ${c.notes ? `<div style="font-size:0.75rem; color:var(--crm-muted); margin-top:2px;">${escapeHtml(c.notes)}</div>` : ''}
            </td>
            <td>
                <div class="quote-customer-cell">
                    <div class="quote-customer-avatar" style="background:linear-gradient(135deg, #10b981, #059669);">${escapeHtml(initials)}</div>
                    <div>
                        <div class="quote-customer-name">${escapeHtml(custName)}</div>
                        <div class="quote-customer-sub"><i class="fa-solid fa-building" style="font-size:0.7rem;"></i> Khách hàng DN</div>
                    </div>
                </div>
            </td>
            <td>
                ${quoteNum ? `
                    <a href="quote-editor.html?id=${encodeURIComponent(c.quote_id || c.quoteId || '')}" class="quote-link-tag" title="Xem báo giá nguồn">
                        <i class="fa-solid fa-file-invoice-dollar"></i> ${escapeHtml(quoteNum)}
                    </a>
                ` : '<span style="color:var(--crm-muted); font-size:0.78rem;">Tạo trực tiếp</span>'}
            </td>
            <td style="text-align:right;">
                <div style="font-weight:800; font-size:0.95rem; color:#10b981; font-family:'JetBrains Mono', monospace;">
                    ${formatMoney(amount)}
                </div>
            </td>
            <td>
                <div class="quote-date-cell">
                    <i class="fa-regular fa-calendar-days"></i>
                    <span>${escapeHtml(startDate || '—')} → ${escapeHtml(endDate || '—')}</span>
                </div>
            </td>
            <td>
                <span class="contract-status-pill ${status}">
                    <span class="dot"></span>
                    ${renderStatusLabel(status)}
                </span>
            </td>
            <td>
                <div class="contract-action-group">
                    <button class="contract-btn-action view" onclick="openViewModal(${c.id})" title="Xem chi tiết">
                        <i class="fa-solid fa-eye"></i>
                    </button>
                    <button class="contract-btn-action print" onclick="openContractPrint(${c.id})" title="Xem trước bản in / PDF">
                        <i class="fa-solid fa-print"></i>
                    </button>
                    <button class="contract-btn-action" onclick="openEditModal(${c.id})" title="Chỉnh sửa">
                        <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                    <button class="contract-btn-action delete" onclick="openDeleteModal(${c.id}, '${escapeHtml(cNum)}')" title="Xóa hợp đồng">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </div>
            </td>
        `;
        tableBody.appendChild(tr);
    });
}

function renderStatusLabel(st) {
    switch (st) {
        case "ACTIVE": return "Hiệu lực";
        case "DRAFT": return "Bản thảo";
        case "COMPLETED": return "Hoàn tất";
        case "EXPIRED": return "Hết hạn";
        case "CANCELLED": return "Đã hủy";
        default: return st;
    }
}

/* =========================================================
   VIEW DETAIL MODAL
========================================================= */
function openViewModal(id) {
    const c = contractsData.find(item => Number(item.id) === Number(id));
    if (!c) return;

    document.getElementById("viewContractNumber").textContent = c.contract_number || c.contractNumber || "—";
    document.getElementById("viewTitle").textContent = c.title || "—";
    document.getElementById("viewCustomerName").textContent = c.customer_name || c.customerName || "—";
    document.getElementById("viewQuoteNumber").textContent = c.quote_number || c.quoteNumber || "Không có (Tạo thủ công)";
    document.getElementById("viewTotalAmount").textContent = formatMoney(c.total_amount || c.totalAmount || 0);
    document.getElementById("viewSignedDate").textContent = c.signed_date || c.signedDate || "—";
    document.getElementById("viewStartDate").textContent = c.start_date || c.startDate || "—";
    document.getElementById("viewEndDate").textContent = c.end_date || c.endDate || "—";
    document.getElementById("viewPaymentTerms").textContent = c.payment_terms || c.paymentTerms || "Không có ghi chú điều khoản";
    document.getElementById("viewNotes").textContent = c.notes || "Không có ghi chú nội bộ";
    document.getElementById("viewOwnerName").textContent = c.owner_name || c.ownerName || "Quản trị viên";
    document.getElementById("viewCreatedAt").textContent = c.created_at || c.createdAt || "—";

    const st = (c.status || "DRAFT").toUpperCase();
    const stEl = document.getElementById("viewStatus");
    if (stEl) {
        stEl.innerHTML = `<span class="contract-status-pill ${st}"><span class="dot"></span> ${renderStatusLabel(st)}</span>`;
    }

    const editBtn = document.getElementById("viewBtnEdit");
    if (editBtn) {
        editBtn.dataset.editId = c.id;
        editBtn.onclick = () => {
            closeViewModal();
            openEditModal(c.id);
        };
    }

    document.getElementById("viewModal")?.classList.add("open");
}

function closeViewModal() {
    document.getElementById("viewModal")?.classList.remove("open");
}

/* =========================================================
   PRINT / PDF CONTRACT MODAL
========================================================= */
function openContractPrint(id) {
    const c = contractsData.find(item => Number(item.id) === Number(id));
    if (!c) return;

    document.getElementById("printContractNumber").textContent = c.contract_number || c.contractNumber || `HD-${c.id}`;
    document.getElementById("printSignedDate").textContent = c.signed_date || c.signedDate || new Date().toISOString().slice(0, 10);
    document.getElementById("printDateDay").textContent = c.signed_date || new Date().toLocaleDateString("vi-VN");
    document.getElementById("printCustomerName").textContent = c.customer_name || c.customerName || "Khách hàng Doanh Nghiệp";
    document.getElementById("printContractTitle").textContent = c.title || "Hợp đồng kinh tế";
    document.getElementById("printContractAmount").textContent = formatMoney(c.total_amount || c.totalAmount || 0);
    document.getElementById("printStartDate").textContent = c.start_date || c.startDate || "Ngày ký";
    document.getElementById("printEndDate").textContent = c.end_date || c.endDate || "Sau 01 năm";
    document.getElementById("printPaymentTerms").textContent = c.payment_terms || c.paymentTerms || "Thanh toán 100% sau khi ký hợp đồng và nghiệm thu.";

    document.getElementById("contractPrintModal").style.display = "flex";
}

function closeContractPrint() {
    document.getElementById("contractPrintModal").style.display = "none";
}

/* =========================================================
   EDIT CONTRACT MODAL
========================================================= */
function openEditModal(id) {
    const c = contractsData.find(item => Number(item.id) === Number(id));
    if (!c) return;

    document.getElementById("editContractId").value = c.id;
    document.getElementById("editCustomerId").value = c.customer_id || c.customerId || "";
    document.getElementById("editQuoteId").value = c.quote_id || c.quoteId || "";
    document.getElementById("editOpportunityId").value = c.opportunity_id || c.opportunityId || "";

    document.getElementById("editTitle").value = c.title || "";
    document.getElementById("editContractNumber").value = c.contract_number || c.contractNumber || "";
    document.getElementById("editCustomerDisplay").value = c.customer_name || c.customerName || "";
    document.getElementById("editTotalAmount").value = Number(c.total_amount || c.totalAmount || 0);
    document.getElementById("editStatus").value = (c.status || "DRAFT").toUpperCase();
    document.getElementById("editSignedDate").value = c.signed_date || c.signedDate || "";
    document.getElementById("editStartDate").value = c.start_date || c.startDate || "";
    document.getElementById("editEndDate").value = c.end_date || c.endDate || "";
    document.getElementById("editPaymentTerms").value = c.payment_terms || c.paymentTerms || "";
    document.getElementById("editNotes").value = c.notes || "";

    document.getElementById("editModal")?.classList.add("open");
}

function closeEditModal() {
    document.getElementById("editModal")?.classList.remove("open");
}

async function submitEditContract() {
    const id = document.getElementById("editContractId").value;
    const title = document.getElementById("editTitle").value.trim();
    const contractNumber = document.getElementById("editContractNumber").value.trim();
    const customerId = document.getElementById("editCustomerId").value;
    const quoteId = document.getElementById("editQuoteId").value;
    const opportunityId = document.getElementById("editOpportunityId").value;
    const totalAmount = document.getElementById("editTotalAmount").value;
    const status = document.getElementById("editStatus").value;
    const signedDate = document.getElementById("editSignedDate").value;
    const startDate = document.getElementById("editStartDate").value;
    const endDate = document.getElementById("editEndDate").value;
    const paymentTerms = document.getElementById("editPaymentTerms").value.trim();
    const notes = document.getElementById("editNotes").value.trim();

    if (!title) {
        showToast("Vui lòng nhập tên hợp đồng", true);
        return;
    }
    if (!contractNumber) {
        showToast("Vui lòng nhập mã hợp đồng", true);
        return;
    }

    const payload = {
        title,
        contractNumber,
        customerId: customerId ? Number(customerId) : null,
        quoteId: quoteId ? Number(quoteId) : null,
        opportunityId: opportunityId ? Number(opportunityId) : null,
        totalAmount: totalAmount ? Number(totalAmount) : 0,
        status,
        signedDate: signedDate || null,
        startDate: startDate || null,
        endDate: endDate || null,
        paymentTerms,
        notes
    };

    const saveBtn = document.getElementById("btnSaveEdit");
    if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang lưu...';
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

        if (res.status === 401) {
            window.location.href = "login.html";
            return;
        }

        const json = await res.json();
        if (!res.ok || !json.success) {
            throw new Error(json?.message || "Không thể cập nhật hợp đồng");
        }

        showToast("Cập nhật hợp đồng thành công!");
        closeEditModal();
        await loadContracts();
    } catch (err) {
        console.error("Update contract error:", err);
        showToast("Lỗi: " + err.message, true);
    } finally {
        if (saveBtn) {
            saveBtn.disabled = false;
            saveBtn.innerHTML = '<i class="fa-solid fa-floppy-disk"></i> Lưu thay đổi';
        }
    }
}

/* =========================================================
   DELETE CONTRACT MODAL
========================================================= */
function openDeleteModal(id, code) {
    pendingDeleteId = id;
    const nameEl = document.getElementById("deleteContractName");
    if (nameEl) nameEl.textContent = code;
    document.getElementById("deleteModal")?.classList.add("open");
}

function closeDeleteModal() {
    pendingDeleteId = null;
    document.getElementById("deleteModal")?.classList.remove("open");
}

async function confirmDeleteContract() {
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

        if (res.status === 401) {
            window.location.href = "login.html";
            return;
        }

        const json = await res.json();
        if (!res.ok || !json.success) {
            throw new Error(json?.message || "Không thể xóa hợp đồng");
        }

        showToast("Xóa hợp đồng thành công!");
        closeDeleteModal();
        await loadContracts();
    } catch (err) {
        console.error("Delete contract error:", err);
        showToast("Lỗi khi xóa: " + err.message, true);
    } finally {
        if (delBtn) {
            delBtn.disabled = false;
            delBtn.innerHTML = '<i class="fa-solid fa-trash-can"></i> Xác nhận Xóa';
        }
    }
}

/* =========================================================
   CSV EXPORT
========================================================= */
function exportContractsCSV() {
    if (!contractsData.length) {
        showToast("Không có dữ liệu hợp đồng để xuất!", true);
        return;
    }

    if (window.ExcelExportHelper) {
        const columns = [
            { label: "STT", key: "stt", width: 45, align: "center" },
            { label: "Số Hợp Đồng", key: "contractNumber", width: 95, align: "center" },
            { label: "Tên Hợp Đồng", key: "title", width: 260, align: "left" },
            { label: "Khách Hàng", key: "customerName", width: 220, align: "left" },
            { label: "Giá Trị HĐ (VNĐ)", key: "totalAmount", width: 140, isMoney: true },
            { label: "Báo Giá Gốc", key: "quoteNumber", width: 95, align: "center" },
            { label: "Trạng Thái", key: "statusName", width: 120, align: "center" },
            { label: "Ngày Hiệu Lực", key: "startDate", width: 100, align: "center" },
            { label: "Ngày Hết Hạn", key: "endDate", width: 100, align: "center" }
        ];

        let sumTotal = 0;
        const exportData = contractsData.map((c, idx) => {
            const amount = Number(c.total_amount || c.totalAmount) || 0;
            sumTotal += amount;
            return {
                stt: idx + 1,
                contractNumber: c.contract_number || c.contractNumber || `HD-${c.id}`,
                title: c.title || "Hợp đồng kinh tế",
                customerName: c.customer_name || c.customerName || "Khách hàng",
                totalAmount: amount,
                quoteNumber: c.quote_number || c.quoteNumber || "—",
                statusName: renderStatusLabel((c.status || '').toUpperCase()),
                startDate: c.start_date || c.startDate || "—",
                endDate: c.end_date || c.endDate || "—"
            };
        });

        window.ExcelExportHelper.exportStyledExcel({
            title: "NOVACRM ENTERPRISE - BÁO CÁO TỔNG HỢP HỢP ĐỒNG KINH TẾ",
            filename: `Hop_Dong_NovaCRM_${new Date().toISOString().slice(0, 10)}.xls`,
            meta: `Ngày xuất: ${new Date().toLocaleDateString('vi-VN')} | Tổng số hợp đồng: ${contractsData.length} | Đơn vị: VNĐ`,
            columns: columns,
            data: exportData,
            summary: {
                label: "TỔNG GIÁ TRỊ HỢP ĐỒNG:",
                mergeCols: 4,
                values: { totalAmount: sumTotal }
            }
        });
        showToast("Đã xuất tệp Excel danh sách hợp đồng thành công!");
        return;
    }

    // Fallback CSV
    let csvContent = "data:text/csv;charset=utf-8,\uFEFF";
    csvContent += "Mã hợp đồng,Tên hợp đồng,Khách hàng,Giá trị (VNĐ),Báo giá liên kết,Trạng thái,Ngày ký,Ngày hiệu lực,Ngày hết hạn\n";

    contractsData.forEach(c => {
        const row = [
            `"${c.contract_number || c.contractNumber || ''}"`,
            `"${(c.title || '').replace(/"/g, '""')}"`,
            `"${(c.customer_name || c.customerName || '').replace(/"/g, '""')}"`,
            Number(c.total_amount || c.totalAmount || 0),
            `"${c.quote_number || c.quoteNumber || ''}"`,
            `"${renderStatusLabel((c.status || '').toUpperCase())}"`,
            `"${c.signed_date || c.signedDate || ''}"`,
            `"${c.start_date || c.startDate || ''}"`,
            `"${c.end_date || c.endDate || ''}"`
        ];
        csvContent += row.join(",") + "\n";
    });

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `Hop_Dong_CRM_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

/* =========================================================
   HELPERS & TOAST
========================================================= */
function formatMoney(num) {
    return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
        maximumFractionDigits: 0
    }).format(Number(num) || 0);
}

function escapeHtml(str) {
    if (str === null || str === undefined) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function showToast(msg, isError = false) {
    const toast = document.getElementById("toastMessage");
    if (!toast) return;
    toast.textContent = msg;
    toast.className = `toast-msg ${isError ? 'error' : 'success'}`;
    toast.style.display = "block";
    setTimeout(() => {
        toast.style.display = "none";
    }, 3500);
}
