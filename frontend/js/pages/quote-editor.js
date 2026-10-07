"use strict";

const API_BASE = "http://localhost:8080/crm";

let lines = [];
let currentId = null;
let currentStatus = "draft";
let availableProducts = [];
let availableCustomers = [];

const lineBody = document.getElementById("quoteLineBody");
const lineEmpty = document.getElementById("lineEmpty");

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
const params = new URLSearchParams(window.location.search);
currentId = params.get("id");

async function initQuoteEditor() {
    await Promise.all([loadProducts(), loadCustomers()]);

    if (currentId) {
        await loadExistingQuote(currentId);
    } else {
        if (!lines.length) {
            lines.push({
                productId: availableProducts[0]?.id || null,
                product: availableProducts[0]?.name || "",
                quantity: 1,
                price: Number(availableProducts[0]?.listPrice || 0),
                floorPrice: Number(availableProducts[0]?.floorPrice || 0),
                discount: 0
            });
            renderLines();
        }
    }
}

async function loadProducts() {
    try {
        const data = await api("/api/products");
        if (Array.isArray(data)) {
            availableProducts = data.filter(p => p.active !== false && p.isActive !== false);
            buildProductDatalist();
        }
    } catch (err) {
        console.warn("Could not load products from API:", err);
    }
}

function buildProductDatalist() {
    let dl = document.getElementById("quoteProductDatalist");
    if (!dl) {
        dl = document.createElement("datalist");
        dl.id = "quoteProductDatalist";
        document.body.appendChild(dl);
    }
    dl.innerHTML = "";
    availableProducts.forEach(p => {
        const opt = document.createElement("option");
        opt.value = p.name;
        opt.dataset.id = p.id;
        opt.dataset.price = p.listPrice || 0;
        opt.dataset.floor = p.floorPrice || 0;
        dl.appendChild(opt);
    });
}

async function loadCustomers() {
    try {
        const data = await api("/api/customers");
        if (Array.isArray(data)) {
            availableCustomers = data;
            buildCustomerDatalist();
        }
    } catch (err) {
        console.warn("Could not load customers:", err);
    }
}

function buildCustomerDatalist() {
    let dl = document.getElementById("quoteCustomerDatalist");
    if (!dl) {
        dl = document.createElement("datalist");
        dl.id = "quoteCustomerDatalist";
        document.body.appendChild(dl);
    }
    dl.innerHTML = "";
    availableCustomers.forEach(c => {
        const opt = document.createElement("option");
        opt.value = c.name;
        opt.dataset.id = c.id;
        dl.appendChild(opt);
    });
    const custInput = document.getElementById("quoteCustomer");
    if (custInput) custInput.setAttribute("list", "quoteCustomerDatalist");
}

/* =========================================================
   EVENT HANDLERS
========================================================= */
document.getElementById("addQuoteLine")?.addEventListener("click", () => {
    lines.push({
        productId: availableProducts[0]?.id || null,
        product: "",
        quantity: 1,
        price: 0,
        floorPrice: 0,
        discount: 0
    });
    renderLines();
});

document.getElementById("saveDraftButton")?.addEventListener("click", () => {
    currentStatus = "draft";
    saveQuote();
});

document.getElementById("submitApprovalButton")?.addEventListener("click", () => {
    currentStatus = "pending";
    saveQuote();
});

document.getElementById("approveQuote")?.addEventListener("click", async () => {
    currentStatus = "approved";
    if (currentId && !isNaN(Number(currentId))) {
        try {
            await api(`/api/quotes/${currentId}/status`, {
                method: "POST",
                body: JSON.stringify({ status: "APPROVED" })
            });
            alert("Báo giá đã được phê duyệt thành công!");
            window.location.href = "quotations.html";
            return;
        } catch (err) {
            alert("Lỗi duyệt báo giá: " + err.message);
            return;
        }
    }
    saveQuote();
});

document.getElementById("rejectQuote")?.addEventListener("click", async () => {
    currentStatus = "rejected";
    if (currentId && !isNaN(Number(currentId))) {
        try {
            await api(`/api/quotes/${currentId}/status`, {
                method: "POST",
                body: JSON.stringify({ status: "REJECTED" })
            });
            alert("Báo giá đã bị từ chối!");
            window.location.href = "quotations.html";
            return;
        } catch (err) {
            alert("Lỗi từ chối báo giá: " + err.message);
            return;
        }
    }
    saveQuote();
});

lineBody?.addEventListener("input", event => {
    const input = event.target.closest("[data-line-field]");
    if (!input) return;

    const index = Number(input.dataset.index);
    const field = input.dataset.lineField;
    if (!lines[index]) return;

    if (field === "product") {
        lines[index][field] = input.value;
        const matched = availableProducts.find(p => p.name.toLowerCase() === input.value.trim().toLowerCase());
        if (matched) {
            lines[index].productId = matched.id;
            lines[index].price = Number(matched.listPrice || 0);
            lines[index].floorPrice = Number(matched.floorPrice || 0);
            renderLines();
            return;
        }
    } else {
        lines[index][field] = Math.max(0, Number(input.value) || 0);
        if (field === "discount") {
            lines[index][field] = Math.min(100, lines[index][field]);
        }
    }

    updateSummary();
    updateLineTotal(index);
});

lineBody?.addEventListener("click", event => {
    const remove = event.target.closest("[data-remove-line]");
    if (!remove) return;
    lines.splice(Number(remove.dataset.removeLine), 1);
    renderLines();
});

/* =========================================================
   RENDER & SUMMARY
========================================================= */
function renderLines() {
    if (!lineBody) return;
    lineBody.innerHTML = "";
    if (lineEmpty) lineEmpty.style.display = lines.length ? "none" : "flex";

    lines.forEach((line, index) => {
        const tr = document.createElement("tr");
        const isBelowFloor = line.floorPrice > 0 && line.price < line.floorPrice;

        tr.innerHTML = `
            <td class="line-product">
                <input
                    type="text"
                    list="quoteProductDatalist"
                    value="${escapeAttribute(line.product)}"
                    placeholder="Chọn hoặc nhập sản phẩm..."
                    data-line-field="product"
                    data-index="${index}"
                >
                ${isBelowFloor ? `<small style="color:#ef4444;display:block;font-size:11px;">⚠️ Dưới giá sàn (${money(line.floorPrice)}) - Cần duyệt</small>` : ''}
            </td>
            <td class="line-qty">
                <input
                    type="number"
                    min="1"
                    value="${line.quantity}"
                    data-line-field="quantity"
                    data-index="${index}"
                >
            </td>
            <td class="line-price">
                <input
                    type="number"
                    min="0"
                    value="${line.price}"
                    data-line-field="price"
                    data-index="${index}"
                    style="${isBelowFloor ? 'border-color:#ef4444;color:#ef4444;' : ''}"
                >
            </td>
            <td class="line-discount">
                <input
                    type="number"
                    min="0"
                    max="100"
                    value="${line.discount}"
                    data-line-field="discount"
                    data-index="${index}"
                >
            </td>
            <td class="line-total" id="lineTotal${index}">
                ${money(lineAmount(line))}
            </td>
            <td>
                <button type="button" class="remove-line" data-remove-line="${index}">×</button>
            </td>
        `;
        lineBody.appendChild(tr);
    });

    updateSummary();
}

function updateLineTotal(index) {
    const cell = document.getElementById(`lineTotal${index}`);
    if (cell && lines[index]) {
        cell.textContent = money(lineAmount(lines[index]));
    }
}

function lineAmount(line) {
    const raw = Number(line.quantity || 0) * Number(line.price || 0);
    return raw * (1 - Number(line.discount || 0) / 100);
}

function updateSummary() {
    let subtotal = 0;
    let total = 0;
    let hasFloorPriceViolation = false;

    for (const line of lines) {
        const raw = Number(line.quantity || 0) * Number(line.price || 0);
        subtotal += raw;
        total += lineAmount(line);
        if (line.floorPrice > 0 && line.price < line.floorPrice) {
            hasFloorPriceViolation = true;
        }
    }

    const discount = subtotal - total;
    const discountPercent = subtotal > 0 ? (discount / subtotal) * 100 : 0;

    const subEl = document.getElementById("subtotalText");
    const discEl = document.getElementById("discountText");
    const totEl = document.getElementById("totalText");
    const warnEl = document.getElementById("approvalWarning");

    if (subEl) subEl.textContent = money(subtotal);
    if (discEl) discEl.textContent = money(discount);
    if (totEl) totEl.textContent = money(total);

    // Warning if discount > 15% OR price < floor price (CRM-44 & CRM-39)
    if (warnEl) {
        const needsApproval = discountPercent > 15 || hasFloorPriceViolation;
        warnEl.hidden = !needsApproval;
        if (hasFloorPriceViolation) {
            warnEl.textContent = "⚠️ Chú ý: Đơn giá thấp hơn Giá sàn. Báo giá bắt buộc phải có Giám đốc/Quản lý phê duyệt.";
        } else if (discountPercent > 15) {
            warnEl.textContent = "⚠️ Chú ý: Mức chiết khấu vượt quá 15%. Báo giá cần Quản lý phê duyệt.";
        }
    }
}

function calculateSummary() {
    let subtotal = 0;
    let total = 0;
    let hasFloorViolation = false;

    for (const line of lines) {
        const raw = Number(line.quantity || 0) * Number(line.price || 0);
        subtotal += raw;
        total += lineAmount(line);
        if (line.floorPrice > 0 && line.price < line.floorPrice) {
            hasFloorViolation = true;
        }
    }

    const discount = subtotal - total;
    return {
        subtotal,
        total,
        discount,
        discountPercent: subtotal ? (discount / subtotal) * 100 : 0,
        hasFloorViolation
    };
}

/* =========================================================
   SAVE QUOTE
========================================================= */
async function saveQuote() {
    const code = value("quoteCode") || `BG-${Date.now().toString().slice(-4)}`;
    const custName = value("quoteCustomer");

    if (!custName) {
        alert("Vui lòng nhập hoặc chọn khách hàng.");
        document.getElementById("quoteCustomer")?.focus();
        return;
    }

    if (!lines.length) {
        alert("Vui lòng thêm ít nhất một dòng sản phẩm.");
        return;
    }

    const summary = calculateSummary();
    const matchedCustomer = availableCustomers.find(c => c.name.toLowerCase() === custName.trim().toLowerCase());
    const customerId = matchedCustomer ? matchedCustomer.id : (availableCustomers[0]?.id || 1);

    const itemsPayload = lines.map(l => {
        let pId = l.productId;
        if (!pId) {
            const matched = availableProducts.find(p => p.name.toLowerCase() === (l.product || "").trim().toLowerCase());
            pId = matched ? matched.id : (availableProducts[0]?.id || 1);
        }
        return {
            productId: pId,
            quantity: Number(l.quantity || 1),
            unitPrice: Number(l.price || 0),
            discountPercent: Number(l.discount || 0)
        };
    });

    const payload = {
        title: `${code} - ${custName}`,
        customerId: customerId,
        discountPercent: summary.discountPercent,
        items: itemsPayload
    };

    try {
        const saved = await api("/api/quotes", {
            method: "POST",
            body: JSON.stringify(payload)
        });

        if (saved && saved.requiresApproval) {
            alert(`Báo giá ${code} đã được tạo! Do giá thấp hơn Giá sàn hoặc chiết khấu > 15%, trạng thái là CHỜ DUYỆT (PENDING_APPROVAL).`);
        } else {
            alert(`Báo giá ${code} đã được lưu thành công!`);
        }

        // Cache sync
        try {
            const raw = localStorage.getItem("crm_ui_quotes");
            const list = raw ? JSON.parse(raw) : [];
            list.unshift({
                id: saved.id,
                code: saved.quoteNumber || code,
                customer: custName,
                total: summary.total,
                subtotal: summary.subtotal,
                discountPercent: summary.discountPercent,
                status: saved.status ? saved.status.toLowerCase() : currentStatus,
                requiresApproval: saved.requiresApproval,
                validUntil: value("quoteValidUntil") || todayString(),
                lines
            });
            localStorage.setItem("crm_ui_quotes", JSON.stringify(list));
        } catch (_) {}

        window.location.href = "quotations.html";
    } catch (err) {
        alert("Lỗi khi lưu báo giá: " + err.message);
    }
}

/* =========================================================
   LOAD EXISTING QUOTE
========================================================= */
async function loadExistingQuote(id) {
    try {
        let quote = null;
        if (!isNaN(Number(id))) {
            quote = await api(`/api/quotes/${id}`);
        }

        if (!quote) {
            const cached = JSON.parse(localStorage.getItem("crm_ui_quotes") || "[]");
            quote = cached.find(item => String(item.id) === String(id));
        }

        if (!quote) return;

        document.getElementById("editorPageTitle").textContent = "Chỉnh sửa Báo giá";
        setValue("quoteCode", quote.quoteNumber || quote.code);
        setValue("quoteCustomer", quote.customerName || quote.customer);
        setValue("quoteOpportunity", quote.opportunityId || quote.opportunity);
        setValue("quoteValidUntil", quote.validUntil || (quote.createdAt ? quote.createdAt.split("T")[0] : ""));
        setValue("paymentTerms", quote.paymentTerms);
        setValue("deliveryTerms", quote.deliveryTerms);

        currentStatus = (quote.status || "draft").toLowerCase();

        if (Array.isArray(quote.items) && quote.items.length) {
            lines = quote.items.map(it => ({
                productId: it.productId,
                product: it.productName || it.productCode || "",
                quantity: it.quantity,
                price: Number(it.unitPrice || 0),
                floorPrice: Number(it.floorPrice || 0),
                discount: Number(it.discountPercent || 0)
            }));
        } else if (Array.isArray(quote.lines)) {
            lines = quote.lines;
        }

        renderLines();
    } catch (err) {
        console.warn("Could not load quote details:", err);
    }
}

/* =========================================================
   HELPERS
========================================================= */
function value(id) {
    return (document.getElementById(id)?.value || "").trim();
}

function setValue(id, val) {
    const el = document.getElementById(id);
    if (el) el.value = val || "";
}

function money(val) {
    return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
        maximumFractionDigits: 0
    }).format(Number(val) || 0);
}

function escapeAttribute(val) {
    return String(val ?? "")
        .replace(/&/g,"&amp;")
        .replace(/"/g,"&quot;")
        .replace(/</g,"&lt;")
        .replace(/>/g,"&gt;");
}

function todayString() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

// Start
initQuoteEditor();