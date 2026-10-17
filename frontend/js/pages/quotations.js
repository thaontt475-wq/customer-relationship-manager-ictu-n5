"use strict";

const API_BASE = `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;

let quotes = [];
let activeKpiFilter = "";

const quoteTableBody = document.getElementById("quoteTableBody");
const quoteEmpty = document.getElementById("quoteEmpty");
const search = document.getElementById("quoteSearch");
const statusFilter = document.getElementById("quoteStatusFilter");
const sortFilter = document.getElementById("quoteSortFilter");
const quoteCountBadge = document.getElementById("quoteCountBadge");

// KPI elements
const kpiTotalQuotes = document.getElementById("kpiTotalQuotes");
const kpiPendingQuotes = document.getElementById("kpiPendingQuotes");
const kpiApprovedQuotes = document.getElementById("kpiApprovedQuotes");
const kpiTotalValue = document.getElementById("kpiTotalValue");
const kpiApprovalRate = document.getElementById("kpiApprovalRate");

// Preview Modal elements
const quotePreviewModal = document.getElementById("quotePreviewModal");
const btnClosePreviewModal = document.getElementById("btnClosePreviewModal");
const btnClosePreviewBtn = document.getElementById("btnClosePreviewBtn");
const btnPrintModalDoc = document.getElementById("btnPrintModalDoc");

/* =========================================================
   API CLIENT
========================================================= */
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

/* =========================================================
   INITIALIZATION
========================================================= */
async function loadQuotes() {
    try {
        const data = await api("/api/quotes");
        if (Array.isArray(data)) {
            quotes = data.map(q => normalizeQuote(q));
            saveQuotesCache();
            updateKpis();
            render();
            return;
        }
    } catch (err) {
        console.warn("Could not load quotes from backend, falling back to cache:", err);
    }

    try {
        const cached = JSON.parse(localStorage.getItem("crm_ui_quotes"));
        quotes = Array.isArray(cached) ? cached : [];
    } catch {
        quotes = [];
    }
    updateKpis();
    render();
}

function normalizeQuote(q) {
    let st = (q.status || "DRAFT").toLowerCase();
    if (st === "pending_approval") st = "pending";

    return {
        id: q.id,
        code: q.quoteNumber || q.code || `BG-${q.id}`,
        title: q.title || "",
        customer: q.customerName || q.customer || "Khách hàng",
        customerId: q.customerId,
        total: Number(q.totalAmount || q.total || 0),
        subtotal: Number(q.subtotal || 0),
        discountPercent: Number(q.discountPercent || 0),
        status: st,
        requiresApproval: !!q.requiresApproval,
        validUntil: q.validUntil || (q.createdAt ? q.createdAt.split("T")[0] : "—"),
        createdAt: q.createdAt || "",
        lines: q.items || q.lines || []
    };
}

function saveQuotesCache() {
    try {
        localStorage.setItem("crm_ui_quotes", JSON.stringify(quotes));
    } catch (_) {}
}

/* =========================================================
   KPI METRICS CALCULATION
========================================================= */
function updateKpis() {
    const totalCount = quotes.length;
    let pendingCount = 0;
    let approvedCount = 0;
    let totalRevenue = 0;

    for (const q of quotes) {
        if (q.status === "pending" || (q.status === "draft" && q.requiresApproval)) {
            pendingCount++;
        } else if (q.status === "approved") {
            approvedCount++;
        }
        totalRevenue += Number(q.total || 0);
    }

    const rate = totalCount > 0 ? Math.round((approvedCount / totalCount) * 100) : 0;

    if (kpiTotalQuotes) kpiTotalQuotes.textContent = totalCount;
    if (kpiPendingQuotes) kpiPendingQuotes.textContent = pendingCount;
    if (kpiApprovedQuotes) kpiApprovedQuotes.textContent = approvedCount;
    if (kpiTotalValue) kpiTotalValue.textContent = money(totalRevenue);
    if (kpiApprovalRate) kpiApprovalRate.textContent = `${rate}%`;
}

/* =========================================================
   RENDER & FILTERING
========================================================= */
function render() {
    const query = search ? search.value.trim().toLowerCase() : "";
    const status = activeKpiFilter || (statusFilter ? statusFilter.value : "");
    const sort = sortFilter ? sortFilter.value : "newest";

    let filtered = quotes.filter(quote => {
        const text = `${quote.code || ""} ${quote.customer || ""} ${quote.title || ""}`.toLowerCase();
        const searchOk = !query || text.includes(query);
        const statusOk = !status || quote.status === status;
        return searchOk && statusOk;
    });

    // Sorting
    filtered.sort((a, b) => {
        if (sort === "price_desc") return (b.total || 0) - (a.total || 0);
        if (sort === "price_asc") return (a.total || 0) - (b.total || 0);
        if (sort === "oldest") return (a.id || 0) - (b.id || 0);
        return (b.id || 0) - (a.id || 0);
    });

    if (quoteCountBadge) quoteCountBadge.textContent = filtered.length;
    if (quoteTableBody) quoteTableBody.innerHTML = "";

    if (quoteEmpty) {
        quoteEmpty.style.display = filtered.length ? "none" : "flex";
    }

    for (const quote of filtered) {
        if (!quoteTableBody) continue;

        const tr = document.createElement("tr");

        // Customer Avatar Initials
        const custInitials = (quote.customer || "K")
            .split(" ")
            .filter(Boolean)
            .slice(0, 2)
            .map(w => w[0].toUpperCase())
            .join("");

        // Discount styling
        const disc = Number(quote.discountPercent || 0);
        const isDiscWarning = disc > 10;
        const discClass = isDiscWarning ? "warning" : "safe";

        // Floor price warning flag
        const hasFloorViolation = !!quote.requiresApproval;

        tr.innerHTML = `
            <td>
                <a href="quote-editor.html?id=${encodeURIComponent(quote.id)}" class="quote-code-badge" title="Mở chỉnh sửa">
                    <i class="fa-solid fa-hashtag" style="font-size:0.7rem; opacity:0.7;"></i>
                    ${escapeHtml(quote.code)}
                </a>
            </td>
            <td>
                <div class="quote-customer-cell">
                    <div class="quote-customer-avatar">${escapeHtml(custInitials)}</div>
                    <div>
                        <div class="quote-customer-name">${escapeHtml(quote.customer || "Khách hàng")}</div>
                        <div class="quote-customer-sub"><i class="fa-solid fa-building" style="font-size:0.7rem;"></i> Khách hàng doanh nghiệp</div>
                    </div>
                </div>
            </td>
            <td>
                <div class="quote-total-amount">${money(quote.total)}</div>
                <div class="quote-subtotal-info">Tạm tính: ${money(quote.subtotal || quote.total)}</div>
            </td>
            <td>
                <span class="quote-discount-pill ${discClass}">
                    <i class="fa-solid ${isDiscWarning ? 'fa-arrow-trend-up' : 'fa-percent'}" style="font-size:0.7rem;"></i>
                    ${disc.toFixed(1)}%
                </span>
            </td>
            <td>
                <span class="quote-status-pill ${quote.status}">
                    <span class="dot"></span>
                    ${statusLabel(quote.status)}
                </span>
                ${hasFloorViolation ? `
                    <div class="quote-floor-violation-tag" title="Giá chào bán thấp hơn giá sàn quy định, bắt buộc quản lý duyệt">
                        <i class="fa-solid fa-shield-halved"></i> Cần duyệt giá sàn
                    </div>
                ` : ''}
            </td>
            <td>
                <div class="quote-date-cell">
                    <i class="fa-regular fa-calendar"></i>
                    <span>${escapeHtml(quote.validUntil || "—")}</span>
                </div>
            </td>
            <td>
                <div class="quote-action-group">
                    <button type="button" class="quote-btn-action preview" data-preview-quote="${quote.id}" title="Xem trước bản in / PDF">
                        <i class="fa-solid fa-print"></i>
                    </button>
                    <a class="quote-btn-action" href="quote-editor.html?id=${encodeURIComponent(quote.id)}" title="Chỉnh sửa thông tin">
                        <i class="fa-solid fa-pen-to-square"></i> Sửa
                    </a>
                    ${
                        quote.status === "pending" || (quote.status === "draft" && quote.requiresApproval)
                        ? `<button type="button" class="quote-btn-action approve" data-approve-quote="${quote.id}" title="Phê duyệt báo giá này">
                            <i class="fa-solid fa-check"></i> Duyệt
                           </button>`
                        : ""
                    }
                    ${
                        quote.status === "approved"
                        ? `<a class="quote-btn-action contract" href="contract-create.html?id=${encodeURIComponent(quote.id)}" title="Chuyển đổi thành Hợp đồng kinh tế">
                            <i class="fa-solid fa-file-contract"></i> Tạo HĐ
                           </a>`
                        : ""
                    }
                    <button type="button" class="quote-btn-action delete" data-delete-quote="${quote.id}" title="Xóa báo giá">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </div>
            </td>
        `;
        quoteTableBody.appendChild(tr);
    }
}

/* =========================================================
   EVENT LISTENERS & ACTIONS
========================================================= */
document.addEventListener("click", async event => {
    // Delete Quote
    const delBtn = event.target.closest("[data-delete-quote]");
    if (delBtn) {
        const id = delBtn.dataset.deleteQuote;
        if (confirm("Bạn có chắc chắn muốn xóa báo giá này không? Hành động này không thể hoàn tác.")) {
            try {
                if (typeof Number(id) === "number" && !isNaN(Number(id))) {
                    await api(`/api/quotes/${id}`, { method: "DELETE" });
                }
                quotes = quotes.filter(q => String(q.id) !== String(id));
                saveQuotesCache();
                updateKpis();
                render();
            } catch (err) {
                alert("Lỗi khi xóa báo giá: " + err.message);
            }
        }
        return;
    }

    // Approve Quote
    const appBtn = event.target.closest("[data-approve-quote]");
    if (appBtn) {
        const id = appBtn.dataset.approveQuote;
        if (confirm("Xác nhận phê duyệt và phát hành báo giá này?")) {
            try {
                if (typeof Number(id) === "number" && !isNaN(Number(id))) {
                    await api(`/api/quotes/${id}/status`, {
                        method: "POST",
                        body: JSON.stringify({ status: "APPROVED" })
                    });
                }
                await loadQuotes();
            } catch (err) {
                alert("Lỗi khi duyệt báo giá: " + err.message);
            }
        }
        return;
    }

    // Preview Quote in Modal
    const prevBtn = event.target.closest("[data-preview-quote]");
    if (prevBtn) {
        const id = prevBtn.dataset.previewQuote;
        openQuotePreview(id);
        return;
    }

    // KPI Card Filter click
    const kpiCard = event.target.closest(".kpi-card-pro[data-filter-status]");
    if (kpiCard) {
        document.querySelectorAll(".kpi-card-pro").forEach(c => c.classList.remove("active-filter"));
        const targetStatus = kpiCard.dataset.filterStatus;
        if (activeKpiFilter === targetStatus) {
            activeKpiFilter = "";
            if (statusFilter) statusFilter.value = "";
        } else {
            activeKpiFilter = targetStatus;
            kpiCard.classList.add("active-filter");
            if (statusFilter) statusFilter.value = targetStatus;
        }
        render();
    }
});

// Search & Filter change
search?.addEventListener("input", render);
statusFilter?.addEventListener("change", () => {
    activeKpiFilter = statusFilter.value;
    document.querySelectorAll(".kpi-card-pro").forEach(c => {
        if (c.dataset.filterStatus === activeKpiFilter && activeKpiFilter !== "") {
            c.classList.add("active-filter");
        } else {
            c.classList.remove("active-filter");
        }
    });
    render();
});
sortFilter?.addEventListener("change", render);

// Reset filter
document.getElementById("btnResetFilter")?.addEventListener("click", () => {
    if (search) search.value = "";
    if (statusFilter) statusFilter.value = "";
    if (sortFilter) sortFilter.value = "newest";
    activeKpiFilter = "";
    document.querySelectorAll(".kpi-card-pro").forEach(c => c.classList.remove("active-filter"));
    render();
});

// Refresh button
document.getElementById("btnRefreshQuotes")?.addEventListener("click", async () => {
    const icon = document.querySelector("#btnRefreshQuotes i");
    if (icon) icon.classList.add("fa-spin");
    await loadQuotes();
    setTimeout(() => {
        if (icon) icon.classList.remove("fa-spin");
    }, 500);
});

// Export to CSV/Excel
document.getElementById("btnExportQuotes")?.addEventListener("click", () => {
    if (!quotes.length) {
        alert("Không có dữ liệu báo giá để xuất!");
        return;
    }

    let csvContent = "data:text/csv;charset=utf-8,\uFEFF";
    csvContent += "Mã báo giá,Khách hàng,Tổng tiền (VNĐ),Chiết khấu (%),Trạng thái,Cần duyệt giá sàn,Hiệu lực đến\n";

    quotes.forEach(q => {
        const row = [
            `"${q.code || ''}"`,
            `"${(q.customer || '').replace(/"/g, '""')}"`,
            q.total || 0,
            q.discountPercent || 0,
            `"${statusLabel(q.status)}"`,
            q.requiresApproval ? "CÓ" : "KHÔNG",
            `"${q.validUntil || ''}"`
        ];
        csvContent += row.join(",") + "\n";
    });

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `Bao_Gia_CRM_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
});

/* =========================================================
   QUOTATION PREVIEW MODAL LOGIC
========================================================= */
function openQuotePreview(id) {
    const quote = quotes.find(q => String(q.id) === String(id));
    if (!quote) return;

    document.getElementById("modalDocCode").textContent = quote.code || `BG-${quote.id}`;
    document.getElementById("modalDocDate").textContent = quote.createdAt ? quote.createdAt.split("T")[0] : new Date().toISOString().slice(0, 10);
    document.getElementById("modalDocValid").textContent = quote.validUntil || "—";
    document.getElementById("modalDocCustomer").textContent = quote.customer || "Khách hàng";
    document.getElementById("modalDocStatus").textContent = statusLabel(quote.status);

    const itemsTbody = document.getElementById("modalDocItemsBody");
    if (itemsTbody) {
        itemsTbody.innerHTML = "";
        const lines = quote.lines && quote.lines.length ? quote.lines : [
            { product: quote.title || "Gói giải pháp phần mềm CRM", quantity: 1, price: quote.total, discount: quote.discountPercent }
        ];

        let st = 0;
        lines.forEach((item, idx) => {
            const qty = Number(item.quantity || 1);
            const pr = Number(item.price || item.unitPrice || 0);
            const dc = Number(item.discount || item.discountPercent || 0);
            const amt = qty * pr * (1 - dc / 100);
            st += qty * pr;

            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td style="text-align:center;">${idx + 1}</td>
                <td><strong>${escapeHtml(item.product || item.productName || 'Sản phẩm')}</strong></td>
                <td style="text-align:center;">${qty}</td>
                <td style="text-align:right;">${money(pr)}</td>
                <td style="text-align:center;">${dc}%</td>
                <td style="text-align:right; font-weight:700;">${money(amt)}</td>
            `;
            itemsTbody.appendChild(tr);
        });

        const subtotal = quote.subtotal || st;
        const discountVal = (subtotal * (quote.discountPercent || 0)) / 100;
        const taxVal = (subtotal - discountVal) * 0.1;
        const grandTotal = (subtotal - discountVal) + taxVal;

        document.getElementById("modalDocSubtotal").textContent = money(subtotal);
        document.getElementById("modalDocDiscount").textContent = `-${money(discountVal)}`;
        document.getElementById("modalDocTax").textContent = money(taxVal);
        document.getElementById("modalDocTotal").textContent = money(quote.total || grandTotal);
    }

    if (quotePreviewModal) {
        quotePreviewModal.style.display = "flex";
    }
}

function closeQuotePreview() {
    if (quotePreviewModal) {
        quotePreviewModal.style.display = "none";
    }
}

btnClosePreviewModal?.addEventListener("click", closeQuotePreview);
btnClosePreviewBtn?.addEventListener("click", closeQuotePreview);
quotePreviewModal?.addEventListener("click", (e) => {
    if (e.target === quotePreviewModal) closeQuotePreview();
});

btnPrintModalDoc?.addEventListener("click", () => {
    window.print();
});

/* =========================================================
   HELPERS
========================================================= */
function statusLabel(status) {
    switch (status) {
        case "pending":
            return "Chờ duyệt";
        case "approved":
            return "Đã duyệt";
        case "rejected":
            return "Từ chối";
        default:
            return "Bản nháp";
    }
}

function money(value) {
    return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
        maximumFractionDigits: 0
    }).format(Number(value) || 0);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// Start
loadQuotes();