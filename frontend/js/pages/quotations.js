"use strict";

const API_BASE = "http://localhost:8080/crm";

let quotes = [];

const quoteTableBody = document.getElementById("quoteTableBody");
const quoteEmpty = document.getElementById("quoteEmpty");
const mobileList = document.getElementById("quoteMobileList");
const search = document.getElementById("quoteSearch");
const statusFilter = document.getElementById("quoteStatusFilter");

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
        lines: q.items || []
    };
}

function saveQuotesCache() {
    try {
        localStorage.setItem("crm_ui_quotes", JSON.stringify(quotes));
    } catch (_) {}
}

/* =========================================================
   RENDER
========================================================= */
function render() {
    const query = search ? search.value.trim().toLowerCase() : "";
    const status = statusFilter ? statusFilter.value : "";

    const filtered = quotes.filter(quote => {
        const text = `${quote.code || ""} ${quote.customer || ""} ${quote.title || ""}`.toLowerCase();
        const searchOk = !query || text.includes(query);
        const statusOk = !status || quote.status === status;
        return searchOk && statusOk;
    });

    if (quoteTableBody) quoteTableBody.innerHTML = "";
    if (mobileList) mobileList.innerHTML = "";

    if (quoteEmpty) {
        quoteEmpty.style.display = filtered.length ? "none" : "flex";
    }

    for (const quote of filtered) {
        if (quoteTableBody) {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td class="quote-code">
                    ${escapeHtml(quote.code)}
                </td>
                <td>
                    ${escapeHtml(quote.customer || "—")}
                </td>
                <td>
                    ${money(quote.total)}
                </td>
                <td>
                    ${Number(quote.discountPercent || 0).toFixed(1)}%
                </td>
                <td>
                    <span class="quote-status ${quote.status}">
                        ${statusLabel(quote.status)}
                    </span>
                    ${quote.requiresApproval ? '<span style="font-size:10px;color:#ef4444;display:block;">(Cần duyệt giá sàn)</span>' : ''}
                </td>
                <td>
                    ${escapeHtml(quote.validUntil || "—")}
                </td>
                <td>
                    <div class="quote-actions">
                        <a class="quote-action" href="quote-editor.html?id=${encodeURIComponent(quote.id)}">
                            Xem / Sửa
                        </a>
                        ${
                            quote.status === "pending" || (quote.status === "draft" && quote.requiresApproval)
                            ? `<button type="button" class="quote-action" style="color:#10b981;cursor:pointer;" data-approve-quote="${quote.id}">Duyệt</button>`
                            : ""
                        }
                        ${
                            quote.status === "approved"
                            ? `<a class="quote-action contract" href="contract-create.html?id=${encodeURIComponent(quote.id)}">Tạo HĐ</a>`
                            : ""
                        }
                        <button type="button" class="quote-action" style="color:#ef4444;cursor:pointer;" data-delete-quote="${quote.id}">
                            Xóa
                        </button>
                    </div>
                </td>
            `;
            quoteTableBody.appendChild(tr);
        }

        if (mobileList) {
            const card = document.createElement("article");
            card.className = "quote-mobile-card";
            card.innerHTML = `
                <div class="quote-mobile-head">
                    <div>
                        <strong>${escapeHtml(quote.code)}</strong>
                        <div style="margin-top:5px">${escapeHtml(quote.customer || "—")}</div>
                    </div>
                    <span class="quote-status ${quote.status}">
                        ${statusLabel(quote.status)}
                    </span>
                </div>
                <div class="quote-mobile-info">
                    <span>Tổng: ${money(quote.total)}</span>
                    <span>Chiết khấu: ${Number(quote.discountPercent || 0).toFixed(1)}%</span>
                    <span>Hiệu lực: ${escapeHtml(quote.validUntil || "—")}</span>
                </div>
                <div class="quote-mobile-actions">
                    <a class="crm-btn crm-btn-secondary" style="text-decoration:none" href="quote-editor.html?id=${encodeURIComponent(quote.id)}">
                        Xem / Sửa
                    </a>
                    ${
                        quote.status === "pending" || (quote.status === "draft" && quote.requiresApproval)
                        ? `<button type="button" class="crm-btn crm-btn-secondary" style="color:#10b981" data-approve-quote="${quote.id}">Duyệt</button>`
                        : ""
                    }
                    ${
                        quote.status === "approved"
                        ? `<a class="crm-btn crm-btn-primary" style="text-decoration:none" href="contract-create.html?id=${encodeURIComponent(quote.id)}">Tạo Hợp đồng</a>`
                        : `<button class="crm-btn crm-btn-secondary" disabled>Chưa duyệt</button>`
                    }
                    <button type="button" class="crm-btn crm-btn-secondary" style="color:#ef4444" data-delete-quote="${quote.id}">Xóa</button>
                </div>
            `;
            mobileList.appendChild(card);
        }
    }
}

/* =========================================================
   ACTIONS
========================================================= */
document.addEventListener("click", async event => {
    const delBtn = event.target.closest("[data-delete-quote]");
    if (delBtn) {
        const id = delBtn.dataset.deleteQuote;
        if (confirm("Bạn có chắc chắn muốn xóa báo giá này không?")) {
            try {
                if (typeof Number(id) === "number" && !isNaN(Number(id))) {
                    await api(`/api/quotes/${id}`, { method: "DELETE" });
                }
                quotes = quotes.filter(q => String(q.id) !== String(id));
                saveQuotesCache();
                render();
            } catch (err) {
                alert("Lỗi khi xóa báo giá: " + err.message);
            }
        }
        return;
    }

    const appBtn = event.target.closest("[data-approve-quote]");
    if (appBtn) {
        const id = appBtn.dataset.approveQuote;
        if (confirm("Xác nhận phê duyệt báo giá này?")) {
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
    }
});

search?.addEventListener("input", render);
statusFilter?.addEventListener("change", render);

function statusLabel(status) {
    switch (status) {
        case "pending":
            return "Chờ duyệt";
        case "approved":
            return "Đã duyệt";
        case "rejected":
            return "Từ chối";
        default:
            return "Nháp";
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
        .replace(/&/g,"&amp;")
        .replace(/</g,"&lt;")
        .replace(/>/g,"&gt;")
        .replace(/"/g,"&quot;")
        .replace(/'/g,"&#039;");
}

// Start
loadQuotes();