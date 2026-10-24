"use strict";

const API_BASE = `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;

let lines = [];
let currentId = null;
let currentStatus = "draft";
let availableProducts = [];
let availableCustomers = [];

const lineBody = document.getElementById("quoteLineBody");
const lineEmpty = document.getElementById("lineEmpty");
const lineCountBadge = document.getElementById("lineCountBadge");
const editorStatusBadge = document.getElementById("editorStatusBadge");

// Modals
const modalProductCatalog = document.getElementById("modalProductCatalog");
const btnOpenCatalogModal = document.getElementById("btnOpenCatalogModal");
const btnCloseCatalogModal = document.getElementById("btnCloseCatalogModal");
const catalogSearchInput = document.getElementById("catalogSearchInput");
const catalogTableBody = document.getElementById("catalogTableBody");

const quotePreviewModal = document.getElementById("quotePreviewModal");
const btnPreviewPdfTop = document.getElementById("btnPreviewPdfTop");
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
const params = new URLSearchParams(window.location.search);
currentId = params.get("id");

async function initQuoteEditor() {
    // Default valid date (+15 days)
    const validUntilInput = document.getElementById("quoteValidUntil");
    if (validUntilInput && !validUntilInput.value) {
        const d = new Date();
        d.setDate(d.getDate() + 15);
        validUntilInput.value = formatDateForInput(d);
    }

    // Default code if new
    const codeInput = document.getElementById("quoteCode");
    if (codeInput && !currentId && !codeInput.value) {
        codeInput.value = `BG-${new Date().getFullYear()}-${String(Math.floor(Math.random() * 9000) + 1000)}`;
    }

    // Default terms
    const payTerms = document.getElementById("paymentTerms");
    if (payTerms && !payTerms.value) {
        payTerms.value = "Thanh toán 50% ngay sau khi ký hợp đồng, 50% còn lại trong vòng 07 ngày sau khi hoàn thành nghiệm thu bàn giao.";
    }
    const delTerms = document.getElementById("deliveryTerms");
    if (delTerms && !delTerms.value) {
        delTerms.value = "Bàn giao tài khoản và kích hoạt dịch vụ trong vòng 03 ngày làm việc kể từ ngày nhận được tiền tạm ứng.";
    }

    await Promise.all([loadProducts(), loadCustomers()]);

    if (currentId) {
        await loadExistingQuote(currentId);
    } else {
        if (!lines.length) {
            const first = availableProducts[0];
            lines.push({
                productId: first?.id || null,
                product: first?.name || "Gói giải pháp CRM Tiêu chuẩn",
                unit: first?.unit || "Gói",
                quantity: 1,
                price: Number(first?.listPrice || 15000000),
                floorPrice: Number(first?.floorPrice || 12000000),
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
        opt.dataset.unit = p.unit || "Chiếc";
        dl.appendChild(opt);
    });
}

async function loadCustomers() {
    try {
        const data = await api("/api/customers?page=1&size=100");
        const customerList = Array.isArray(data)
            ? data
            : Array.isArray(data?.items)
                ? data.items
                : [];

        availableCustomers = customerList.filter(
            c => c && c.isDeleted !== true && c.is_deleted !== true
        );
        buildCustomerDatalist();
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
   EVENT HANDLERS & USER INTERACTIONS
========================================================= */
document.getElementById("addQuoteLine")?.addEventListener("click", () => {
    const defaultProd = availableProducts[0];
    lines.push({
        productId: defaultProd?.id || null,
        product: "",
        unit: defaultProd?.unit || "Gói",
        quantity: 1,
        price: Number(defaultProd?.listPrice || 0),
        floorPrice: Number(defaultProd?.floorPrice || 0),
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

// Line input changes
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
            lines[index].unit = matched.unit || "Gói";
            renderLines();
            return;
        }
    } else if (field === "unit") {
        lines[index][field] = input.value;
    } else {
        lines[index][field] = Math.max(0, Number(input.value) || 0);
        if (field === "discount") {
            lines[index][field] = Math.min(100, lines[index][field]);
        }
    }

    updateSummary();
    updateLineTotal(index);
});

// Remove line
lineBody?.addEventListener("click", event => {
    const remove = event.target.closest("[data-remove-line]");
    if (!remove) return;
    lines.splice(Number(remove.dataset.removeLine), 1);
    renderLines();
});

// Preset date chips
document.querySelectorAll(".date-chip").forEach(chip => {
    chip.addEventListener("click", () => {
        const days = Number(chip.dataset.days || 15);
        const d = new Date();
        d.setDate(d.getDate() + days);
        const input = document.getElementById("quoteValidUntil");
        if (input) input.value = formatDateForInput(d);
    });
});

// Preset terms chips
document.querySelectorAll(".terms-chip").forEach(chip => {
    chip.addEventListener("click", () => {
        const presetType = chip.dataset.preset;
        const text = chip.dataset.text;
        if (presetType === "payment") {
            const el = document.getElementById("paymentTerms");
            if (el) el.value = text;
        } else if (presetType === "delivery") {
            const el = document.getElementById("deliveryTerms");
            if (el) el.value = text;
        }
    });
});

/* =========================================================
   RENDER & CPQ LOGIC
========================================================= */
function renderLines() {
    if (!lineBody) return;
    lineBody.innerHTML = "";

    if (lineEmpty) lineEmpty.style.display = lines.length ? "none" : "flex";
    if (lineCountBadge) lineCountBadge.textContent = `${lines.length} dòng`;

    lines.forEach((line, index) => {
        const tr = document.createElement("tr");
        const isBelowFloor = line.floorPrice > 0 && line.price < line.floorPrice;

        tr.innerHTML = `
            <td style="text-align:center; color:var(--crm-muted); font-weight:600;">
                ${index + 1}
            </td>
            <td>
                <input
                    type="text"
                    class="line-product-input"
                    list="quoteProductDatalist"
                    value="${escapeAttribute(line.product)}"
                    placeholder="Tìm hoặc nhập sản phẩm / dịch vụ..."
                    data-line-field="product"
                    data-index="${index}"
                >
                ${isBelowFloor ? `
                    <div class="line-floor-alert">
                        <i class="fa-solid fa-triangle-exclamation"></i>
                        Dưới giá sàn (${money(line.floorPrice)}) - Cần duyệt GĐ
                    </div>
                ` : ''}
            </td>
            <td style="text-align:center;">
                <input
                    type="text"
                    style="width:65px; text-align:center;"
                    value="${escapeAttribute(line.unit || 'Gói')}"
                    data-line-field="unit"
                    data-index="${index}"
                >
            </td>
            <td style="text-align:center;">
                <input
                    type="number"
                    class="line-qty-input"
                    min="1"
                    value="${line.quantity}"
                    data-line-field="quantity"
                    data-index="${index}"
                >
            </td>
            <td style="text-align:right;">
                <input
                    type="number"
                    class="line-price-input"
                    min="0"
                    step="100000"
                    value="${line.price}"
                    data-line-field="price"
                    data-index="${index}"
                    style="${isBelowFloor ? 'border-color:#ef4444; color:#ef4444; font-weight:700;' : ''}"
                >
            </td>
            <td style="text-align:center;">
                <input
                    type="number"
                    class="line-discount-input"
                    min="0"
                    max="100"
                    value="${line.discount}"
                    data-line-field="discount"
                    data-index="${index}"
                >
            </td>
            <td class="line-total-cell" id="lineTotal${index}">
                ${money(lineAmount(line))}
            </td>
            <td style="text-align:center;">
                <button type="button" class="btn-remove-line" data-remove-line="${index}" title="Xóa dòng này">
                    <i class="fa-solid fa-trash-can"></i>
                </button>
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
    let totalAfterDiscount = 0;
    let hasFloorPriceViolation = false;

    for (const line of lines) {
        const raw = Number(line.quantity || 0) * Number(line.price || 0);
        subtotal += raw;
        totalAfterDiscount += lineAmount(line);
        if (line.floorPrice > 0 && line.price < line.floorPrice) {
            hasFloorPriceViolation = true;
        }
    }

    const discount = subtotal - totalAfterDiscount;
    const discountPercent = subtotal > 0 ? (discount / subtotal) * 100 : 0;
    const tax = totalAfterDiscount * 0.1; // VAT 10%
    const grandTotal = totalAfterDiscount + tax;

    const subEl = document.getElementById("subtotalText");
    const discEl = document.getElementById("discountText");
    const taxEl = document.getElementById("taxText");
    const totEl = document.getElementById("totalText");
    const alertEl = document.getElementById("approvalPolicyAlert");
    const badgeStateEl = document.getElementById("approvalBadgeState");
    const stepCheckFloor = document.getElementById("stepCheckFloor");

    if (subEl) subEl.textContent = money(subtotal);
    if (discEl) discEl.textContent = discount > 0 ? `-${money(discount)} (${discountPercent.toFixed(1)}%)` : "0 ₫";
    if (taxEl) taxEl.textContent = money(tax);
    if (totEl) totEl.textContent = money(grandTotal);

    // CPQ Approval Policy Check (Discount > 15% OR Price < Floor price)
    const needsApproval = discountPercent > 15 || hasFloorPriceViolation;

    if (stepCheckFloor) {
        if (hasFloorPriceViolation) {
            stepCheckFloor.className = "cpq-step active";
            stepCheckFloor.innerHTML = `<span class="cpq-step-num" style="background:#f59e0b; color:#fff; border-color:#f59e0b;"><i class="fa-solid fa-triangle-exclamation"></i></span><span>3. Cần duyệt giá sàn</span>`;
        } else {
            stepCheckFloor.className = "cpq-step completed";
            stepCheckFloor.innerHTML = `<span class="cpq-step-num"><i class="fa-solid fa-check"></i></span><span>3. Giá sàn chuẩn</span>`;
        }
    }

    if (alertEl) {
        if (needsApproval) {
            alertEl.className = "approval-policy-alert violation";
            let reasons = [];
            if (hasFloorPriceViolation) reasons.push("Có sản phẩm bán DƯỚI GIÁ SÀN quy định");
            if (discountPercent > 15) reasons.push(`Mức chiết khấu (${discountPercent.toFixed(1)}%) vượt hạn mức nhân viên (15%)`);

            alertEl.innerHTML = `
                <i class="fa-solid fa-shield-halved" style="color:#f59e0b; margin-top:2px;"></i>
                <div>
                    <strong>Cảnh báo chính sách giá:</strong> ${reasons.join(". ")}.
                    Bắt buộc phải có Giám đốc/Quản lý phê duyệt trước khi phát hành.
                </div>
            `;
            if (badgeStateEl) {
                badgeStateEl.className = "quote-status-badge pending";
                badgeStateEl.textContent = "Cần phê duyệt";
            }
        } else {
            alertEl.className = "approval-policy-alert safe";
            alertEl.innerHTML = `
                <i class="fa-solid fa-circle-check" style="color:#10b981; margin-top:2px;"></i>
                <div>
                    <strong>Chính sách chuẩn:</strong> Đơn giá hợp lệ và chiết khấu trong hạn mức cho phép. Đủ điều kiện duyệt tự động.
                </div>
            `;
            if (badgeStateEl) {
                badgeStateEl.className = "quote-status-badge approved";
                badgeStateEl.textContent = "Hợp lệ";
            }
        }
    }
}

function calculateSummary() {
    let subtotal = 0;
    let totalAfterDiscount = 0;
    let hasFloorViolation = false;

    for (const line of lines) {
        const raw = Number(line.quantity || 0) * Number(line.price || 0);
        subtotal += raw;
        totalAfterDiscount += lineAmount(line);
        if (line.floorPrice > 0 && line.price < line.floorPrice) {
            hasFloorViolation = true;
        }
    }

    const discount = subtotal - totalAfterDiscount;
    const tax = totalAfterDiscount * 0.1;
    const grandTotal = totalAfterDiscount + tax;

    return {
        subtotal,
        total: grandTotal,
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
    if (!matchedCustomer) {
        alert("Khách hàng không hợp lệ. Vui lòng chọn khách hàng có sẵn trong hệ thống.");
        return;
    }

    const customerId = matchedCustomer.id;

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
            alert(`Báo giá ${code} đã được tạo!\nDo giá bán thấp hơn giá sàn hoặc chiết khấu > 15%, trạng thái là CHỜ DUYỆT (PENDING_APPROVAL).`);
        } else {
            alert(`Báo giá ${code} đã được lưu thành công!`);
        }

        // Cache update
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

        document.getElementById("editorPageTitle").textContent = `Chỉnh sửa Báo giá #${quote.quoteNumber || quote.code}`;
        setValue("quoteCode", quote.quoteNumber || quote.code);
        setValue("quoteCustomer", quote.customerName || quote.customer);
        setValue("quoteOpportunity", quote.opportunityId || quote.opportunity);
        setValue("quoteValidUntil", quote.validUntil || (quote.createdAt ? quote.createdAt.split("T")[0] : ""));
        setValue("paymentTerms", quote.paymentTerms);
        setValue("deliveryTerms", quote.deliveryTerms);

        currentStatus = (quote.status || "draft").toLowerCase();
        if (editorStatusBadge) {
            editorStatusBadge.className = `quote-status-badge ${currentStatus}`;
            editorStatusBadge.textContent = statusLabel(currentStatus);
        }

        if (Array.isArray(quote.items) && quote.items.length) {
            lines = quote.items.map(it => ({
                productId: it.productId,
                product: it.productName || it.productCode || "",
                unit: it.unit || "Gói",
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
   PRODUCT CATALOG PICKER MODAL
========================================================= */
btnOpenCatalogModal?.addEventListener("click", () => {
    renderCatalogList();
    if (modalProductCatalog) modalProductCatalog.style.display = "flex";
});

btnCloseCatalogModal?.addEventListener("click", () => {
    if (modalProductCatalog) modalProductCatalog.style.display = "none";
});

modalProductCatalog?.addEventListener("click", (e) => {
    if (e.target === modalProductCatalog) modalProductCatalog.style.display = "none";
});

catalogSearchInput?.addEventListener("input", () => {
    renderCatalogList();
});

function renderCatalogList() {
    if (!catalogTableBody) return;
    catalogTableBody.innerHTML = "";
    const term = (catalogSearchInput?.value || "").toLowerCase().trim();

    const filtered = availableProducts.filter(p => {
        const text = `${p.name || ''} ${p.code || ''} ${p.category || ''}`.toLowerCase();
        return !term || text.includes(term);
    });

    if (!filtered.length) {
        catalogTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding:20px; color:var(--crm-muted);">Không tìm thấy sản phẩm phù hợp.</td></tr>`;
        return;
    }

    filtered.forEach(p => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><code>${escapeHtml(p.code || 'PRD')}</code></td>
            <td><strong>${escapeHtml(p.name)}</strong></td>
            <td><span style="font-size:0.75rem; color:var(--crm-muted);">${escapeHtml(p.category || 'Mặc định')}</span></td>
            <td style="text-align:right; font-weight:700;">${money(p.listPrice)}</td>
            <td style="text-align:right; color:#ef4444; font-size:0.8rem;">${money(p.floorPrice)}</td>
            <td style="text-align:center;">
                <button type="button" class="crm-btn crm-btn-secondary" style="padding:4px 8px; font-size:0.75rem;" data-add-catalog-item="${p.id}">
                    <i class="fa-solid fa-plus"></i> Chọn
                </button>
            </td>
        `;
        catalogTableBody.appendChild(tr);
    });
}

catalogTableBody?.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-add-catalog-item]");
    if (!btn) return;
    const id = btn.dataset.addCatalogItem;
    const prod = availableProducts.find(p => String(p.id) === String(id));
    if (prod) {
        lines.push({
            productId: prod.id,
            product: prod.name,
            unit: prod.unit || "Gói",
            quantity: 1,
            price: Number(prod.listPrice || 0),
            floorPrice: Number(prod.floorPrice || 0),
            discount: 0
        });
        renderLines();
        if (modalProductCatalog) modalProductCatalog.style.display = "none";
    }
});

/* =========================================================
   PRINT & PDF PREVIEW MODAL
========================================================= */
btnPreviewPdfTop?.addEventListener("click", () => {
    openQuotePreviewModal();
});

btnClosePreviewModal?.addEventListener("click", () => {
    if (quotePreviewModal) quotePreviewModal.style.display = "none";
});

btnClosePreviewBtn?.addEventListener("click", () => {
    if (quotePreviewModal) quotePreviewModal.style.display = "none";
});

quotePreviewModal?.addEventListener("click", (e) => {
    if (e.target === quotePreviewModal) quotePreviewModal.style.display = "none";
});

btnPrintModalDoc?.addEventListener("click", () => {
    window.print();
});

function openQuotePreviewModal() {
    const code = value("quoteCode") || "BG-DRAFT";
    const cust = value("quoteCustomer") || "Quý Khách hàng";
    const valid = value("quoteValidUntil") || todayString();
    const pay = value("paymentTerms") || "Theo thỏa thuận hợp đồng";
    const del = value("deliveryTerms") || "Theo tiến độ thực hiện";

    document.getElementById("modalDocCode").textContent = code;
    document.getElementById("modalDocDate").textContent = todayString();
    document.getElementById("modalDocValid").textContent = valid;
    document.getElementById("modalDocCustomer").textContent = cust;
    document.getElementById("modalDocStatus").textContent = statusLabel(currentStatus);
    document.getElementById("modalDocPaymentTerms").textContent = pay;
    document.getElementById("modalDocDeliveryTerms").textContent = del;

    const itemsTbody = document.getElementById("modalDocItemsBody");
    if (itemsTbody) {
        itemsTbody.innerHTML = "";
        let st = 0;
        lines.forEach((item, idx) => {
            const qty = Number(item.quantity || 1);
            const pr = Number(item.price || 0);
            const dc = Number(item.discount || 0);
            const amt = qty * pr * (1 - dc / 100);
            st += qty * pr;

            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td style="text-align:center;">${idx + 1}</td>
                <td><strong>${escapeHtml(item.product || 'Sản phẩm')}</strong></td>
                <td style="text-align:center;">${qty}</td>
                <td style="text-align:right;">${money(pr)}</td>
                <td style="text-align:center;">${dc}%</td>
                <td style="text-align:right; font-weight:700;">${money(amt)}</td>
            `;
            itemsTbody.appendChild(tr);
        });

        const summary = calculateSummary();
        document.getElementById("modalDocSubtotal").textContent = money(summary.subtotal);
        document.getElementById("modalDocDiscount").textContent = `-${money(summary.discount)}`;
        document.getElementById("modalDocTax").textContent = money((summary.subtotal - summary.discount) * 0.1);
        document.getElementById("modalDocTotal").textContent = money(summary.total);
    }

    if (quotePreviewModal) quotePreviewModal.style.display = "flex";
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

function escapeHtml(val) {
    return String(val ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function escapeAttribute(val) {
    return String(val ?? "")
        .replace(/&/g, "&amp;")
        .replace(/"/g, "&quot;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}

function todayString() {
    return formatDateForInput(new Date());
}

function formatDateForInput(d) {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

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

// Start
initQuoteEditor();