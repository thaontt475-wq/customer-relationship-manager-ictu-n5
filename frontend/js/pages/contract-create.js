"use strict";

const API_BASE = (window.CRM_BACKEND_BASE || `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`).replace(/\/$/, "");

const params = new URLSearchParams(window.location.search);
const quoteId = params.get("id") || params.get("quoteId");

let quote = null;
let selectedCustomerId = null;

const blocked = document.getElementById("contractBlocked");
const createButton = document.getElementById("createContractButton");
const validation = document.getElementById("contractValidation");

document.addEventListener("DOMContentLoaded", () => {
    // Default today's date
    const today = new Date().toISOString().substring(0, 10);
    const signedInput = document.getElementById("signedDate");
    const effectiveInput = document.getElementById("effectiveDate");
    if (signedInput && !signedInput.value) signedInput.value = today;
    if (effectiveInput && !effectiveInput.value) effectiveInput.value = today;

    // Default auto contract number
    const numInput = document.getElementById("contractNumber");
    if (numInput && !numInput.value) {
        numInput.value = "HD-" + Math.floor(100000 + Math.random() * 900000);
    }

    initContractPage();
});

async function initContractPage() {
    if (quoteId) {
        // Mode 1: Tạo từ Báo giá
        await loadQuoteFromBackend(quoteId);
    } else {
        // Mode 2: Tạo hợp đồng thủ công trực tiếp
        setupManualMode();
    }
}

async function loadQuoteFromBackend(id) {
    const pageHeading = document.getElementById("pageHeading");
    const pageSubHeading = document.getElementById("pageSubHeading");
    if (pageHeading) pageHeading.textContent = "Tạo Hợp đồng từ Báo giá";
    if (pageSubHeading) pageSubHeading.textContent = "Dữ liệu khách hàng và giá trị được kế thừa từ Báo giá đã phê duyệt.";

    document.getElementById("manualCustomerContainer").style.display = "none";
    document.getElementById("manualTotalContainer").style.display = "none";
    document.getElementById("inheritSection").style.display = "block";

    try {
        const res = await fetch(`${API_BASE}/api/quotes/${id}`, {
            method: "GET",
            credentials: "include",
            headers: { "Accept": "application/json" }
        });

        if (res.status === 401) {
            window.location.href = "login.html";
            return;
        }

        const json = await res.json();
        if (!json || !json.success || !json.data) {
            throw new Error(json?.message || "Không tìm thấy báo giá chỉ định.");
        }

        const q = json.data;
        quote = {
            id: q.id,
            code: q.quoteNumber || `BG-${q.id}`,
            title: q.title || "",
            customer: q.customerName || "—",
            customerId: q.customerId,
            opportunity: q.opportunityTitle || "—",
            opportunityId: q.opportunityId,
            total: Number(q.totalAmount || 0),
            status: (q.status || "").toUpperCase(),
            paymentTerms: q.paymentTerms || "",
            lines: q.items || []
        };

        selectedCustomerId = quote.customerId;
        renderQuoteData(quote);

        const titleInput = document.getElementById("contractTitle");
        if (titleInput && !titleInput.value) {
            titleInput.value = `Hợp đồng - ${quote.title || quote.code}`;
        }

        const termsInput = document.getElementById("contractPaymentTerms");
        if (termsInput && !termsInput.value && quote.paymentTerms) {
            termsInput.value = quote.paymentTerms;
        }

        // Kiểm tra nghiệp vụ: Báo giá tạo hợp đồng phải ĐÃ DUYỆT (APPROVED)
        if (quote.status !== "APPROVED") {
            blocked.hidden = false;
            blocked.innerHTML = `Báo giá <strong>${escapeHtml(quote.code)}</strong> hiện ở trạng thái <em>${escapeHtml(quote.status)}</em> (chưa ở trạng thái <strong>Đã duyệt / APPROVED</strong>). Cần duyệt báo giá trước khi lập hợp đồng từ báo giá này.`;
            createButton.disabled = true;
            createButton.style.opacity = "0.5";
            createButton.style.cursor = "not-allowed";
        } else {
            blocked.hidden = true;
            createButton.disabled = false;
            createButton.style.opacity = "1";
            createButton.style.cursor = "pointer";
        }

    } catch (err) {
        console.error("Error loading quote:", err);
        blocked.hidden = false;
        blocked.innerHTML = "Không thể tải dữ liệu báo giá: " + escapeHtml(err.message);
        createButton.disabled = true;
    }
}

async function setupManualMode() {
    const pageHeading = document.getElementById("pageHeading");
    const pageSubHeading = document.getElementById("pageSubHeading");
    if (pageHeading) pageHeading.textContent = "Tạo Hợp đồng Mới";
    if (pageSubHeading) pageSubHeading.textContent = "Nhập thông tin hợp đồng để lưu vào hệ thống.";

    // Ẩn phần kế thừa báo giá, hiển thị dropdown chọn khách hàng và tổng tiền
    document.getElementById("inheritSection").style.display = "none";
    document.getElementById("manualCustomerContainer").style.display = "block";
    document.getElementById("manualTotalContainer").style.display = "block";
    blocked.hidden = true;
    createButton.disabled = false;

    // Tải danh sách khách hàng từ backend
    const customerSelect = document.getElementById("contractCustomerSelect");
    try {
        const res = await fetch(`${API_BASE}/api/customers?size=200`, {
            method: "GET",
            credentials: "include",
            headers: { "Accept": "application/json" }
        });
        if (res.status === 401) {
            window.location.href = "login.html";
            return;
        }
        const json = await res.json();
        const customers = json?.data?.items || json?.data || [];

        customerSelect.innerHTML = `<option value="">-- Chọn Khách hàng (*) --</option>`;
        customers.forEach(c => {
            const opt = document.createElement("option");
            opt.value = c.id;
            opt.textContent = `${c.name} ${c.taxCode ? `(MST: ${c.taxCode})` : ""}`;
            customerSelect.appendChild(opt);
        });

        customerSelect.addEventListener("change", (e) => {
            selectedCustomerId = e.target.value ? Number(e.target.value) : null;
        });
    } catch (err) {
        console.error("Error loading customers:", err);
        customerSelect.innerHTML = `<option value="">Không thể tải khách hàng: ${escapeHtml(err.message)}</option>`;
    }
}

function renderQuoteData(q) {
    setText("contractQuoteCode", q.code);
    setText("contractCustomer", q.customer);
    setText("contractOpportunity", q.opportunity);
    setText("contractTotal", formatMoney(q.total));

    const body = document.getElementById("contractLines");
    if (!body) return;
    body.innerHTML = "";

    if (!q.lines || q.lines.length === 0) {
        body.innerHTML = `<tr><td colspan="5" style="text-align:center; color:#64748b;">Không có dòng sản phẩm chi tiết</td></tr>`;
        return;
    }

    q.lines.forEach(line => {
        const raw = Number(line.quantity || 1) * Number(line.unitPrice || line.price || 0);
        const disc = Number(line.discountPercent || line.discount || 0);
        const total = Number(line.amount || (raw * (1 - disc / 100)));

        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${escapeHtml(line.productName || line.product || "Sản phẩm")}</td>
            <td>${Number(line.quantity || 1)}</td>
            <td>${formatMoney(line.unitPrice || line.price || 0)}</td>
            <td>${disc}%</td>
            <td>${formatMoney(total)}</td>
        `;
        body.appendChild(tr);
    });
}

// Submit xử lý form tạo hợp đồng
document.getElementById("contractForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    if (quote && quote.status !== "APPROVED") {
        validation.textContent = "Báo giá chưa được phê duyệt nên không thể tạo hợp đồng.";
        return;
    }

    const title = getValue("contractTitle");
    const number = getValue("contractNumber");
    const signed = getValue("signedDate");
    const effective = getValue("effectiveDate");
    const expiry = getValue("expiryDate");
    const paymentTerms = getValue("contractPaymentTerms");
    const notes = getValue("contractNotes");
    const status = getValue("contractStatus") || "ACTIVE";

    let customerId = selectedCustomerId;
    let totalAmount = 0;

    if (quote) {
        customerId = quote.customerId;
        totalAmount = quote.total;
    } else {
        const custSelect = document.getElementById("contractCustomerSelect");
        customerId = custSelect ? Number(custSelect.value) : null;
        totalAmount = Number(getValue("manualTotalAmount") || 0);
    }

    // Validation
    if (!title) {
        validation.textContent = "Vui lòng nhập tên hợp đồng.";
        return;
    }
    if (!number) {
        validation.textContent = "Vui lòng nhập số hợp đồng.";
        return;
    }
    if (!customerId || isNaN(customerId) || customerId <= 0) {
        validation.textContent = "Vui lòng chọn khách hàng cho hợp đồng.";
        return;
    }
    if (!signed || !effective) {
        validation.textContent = "Vui lòng chọn ngày ký và ngày hiệu lực.";
        return;
    }

    validation.textContent = "";

    const payload = {
        title,
        contractNumber: number,
        customerId: Number(customerId),
        quoteId: quote ? Number(quote.id) : null,
        opportunityId: quote?.opportunityId ? Number(quote.opportunityId) : null,
        totalAmount: Number(totalAmount),
        signedDate: signed,
        startDate: effective,
        endDate: expiry || null,
        paymentTerms: paymentTerms || "",
        status: status,
        notes: notes || ""
    };

    createButton.disabled = true;
    createButton.textContent = "Đang lưu hợp đồng...";

    try {
        const res = await fetch(`${API_BASE}/api/contracts`, {
            method: "POST",
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
            throw new Error(json?.message || "Không thể tạo hợp đồng");
        }

        const successText = document.getElementById("successDetailText");
        if (successText) {
            successText.textContent = `Hợp đồng "${title}" (Mã: ${number}) đã được lưu thành công vào MySQL!`;
        }

        document.getElementById("contractSuccess").classList.add("open");
    } catch (err) {
        console.error("Create contract error:", err);
        validation.textContent = "Lỗi: " + err.message;
        createButton.disabled = false;
        createButton.textContent = "Lưu Hợp đồng vào Hệ thống";
    }
});

function getValue(id) {
    const el = document.getElementById(id);
    return el ? el.value.trim() : "";
}

function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
}

function formatMoney(amount) {
    return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
        maximumFractionDigits: 0
    }).format(Number(amount) || 0);
}

function escapeHtml(val) {
    return String(val ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}