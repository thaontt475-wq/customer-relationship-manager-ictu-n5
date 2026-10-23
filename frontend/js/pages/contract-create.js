"use strict";

const API_BASE = (window.CRM_BACKEND_BASE || `${window.location.pathname.startsWith("/crm/") ? window.location.origin : window.location.protocol + "//" + (window.location.hostname || "localhost") + ":8080"}/crm`).replace(/\/$/, "");

const params = new URLSearchParams(window.location.search);
const quoteId = params.get("id") || params.get("quoteId");

let quote = null;
let selectedCustomerId = null;

const blocked = document.getElementById("contractBlocked");
const blockedMessage = document.getElementById("blockedMessage");
const createButton = document.getElementById("createContractButton");
const validation = document.getElementById("contractValidation");

document.addEventListener("DOMContentLoaded", () => {
    // Default today's date
    const today = new Date().toISOString().substring(0, 10);
    const signedInput = document.getElementById("signedDate");
    const effectiveInput = document.getElementById("effectiveDate");
    const expiryInput = document.getElementById("expiryDate");

    if (signedInput && !signedInput.value) signedInput.value = today;
    if (effectiveInput && !effectiveInput.value) effectiveInput.value = today;
    if (expiryInput && !expiryInput.value) {
        const nextYear = new Date();
        nextYear.setFullYear(nextYear.getFullYear() + 1);
        expiryInput.value = nextYear.toISOString().substring(0, 10);
    }

    // Default auto contract number
    const numInput = document.getElementById("contractNumber");
    if (numInput && !numInput.value) {
        numInput.value = `HD-${new Date().getFullYear()}-${Math.floor(1000 + Math.random() * 9000)}`;
    }

    // Date preset chips
    document.querySelectorAll(".date-chip[data-contract-months]").forEach(chip => {
        chip.addEventListener("click", () => {
            const months = Number(chip.dataset.contractMonths || 12);
            const eff = new Date(document.getElementById("effectiveDate").value || new Date());
            eff.setMonth(eff.getMonth() + months);
            if (expiryInput) expiryInput.value = eff.toISOString().substring(0, 10);
        });
    });

    // Terms preset chips
    document.querySelectorAll(".terms-chip[data-term-fill]").forEach(chip => {
        chip.addEventListener("click", () => {
            const fillText = chip.dataset.termFill;
            const termsArea = document.getElementById("contractPaymentTerms");
            if (termsArea) termsArea.value = fillText;
        });
    });

    initContractPage();
});

async function initContractPage() {
    if (quoteId) {
        await loadQuoteFromBackend(quoteId);
    } else {
        await setupManualMode();
    }
}

async function loadQuoteFromBackend(id) {
    const pageHeading = document.getElementById("pageHeading");
    const pageSubHeading = document.getElementById("pageSubHeading");
    if (pageHeading) {
        pageHeading.innerHTML = `<i class="fa-solid fa-file-signature" style="color:#10b981;"></i> Tạo Hợp đồng từ Báo giá`;
    }
    if (pageSubHeading) {
        pageSubHeading.textContent = "Dữ liệu khách hàng, danh mục sản phẩm và giá trị được kế thừa tự động từ Báo giá.";
    }

    const manualCust = document.getElementById("manualCustomerContainer");
    const manualTotal = document.getElementById("manualTotalContainer");
    const inheritSec = document.getElementById("inheritSection");

    if (manualCust) manualCust.style.display = "none";
    if (manualTotal) manualTotal.style.display = "none";
    if (inheritSec) inheritSec.style.display = "block";

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
            throw new Error(json?.message || "Không tìm thấy thông tin báo giá.");
        }

        const q = json.data;
        quote = {
            id: q.id,
            code: q.quoteNumber || `BG-${q.id}`,
            title: q.title || "",
            customer: q.customerName || "Khách hàng",
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
            titleInput.value = `Hợp đồng kinh tế - ${quote.customer} (${quote.code})`;
        }

        const termsInput = document.getElementById("contractPaymentTerms");
        if (termsInput && !termsInput.value && quote.paymentTerms) {
            termsInput.value = quote.paymentTerms;
        }

        // Notification if quote is not approved
        if (quote.status !== "APPROVED") {
            if (blocked) blocked.style.display = "flex";
            if (blockedMessage) {
                blockedMessage.innerHTML = `Báo giá <strong>${escapeHtml(quote.code)}</strong> đang ở trạng thái <em>${escapeHtml(quote.status)}</em> (chưa ở trạng thái <strong>Đã duyệt / APPROVED</strong>). Bạn vẫn có thể khởi tạo bản thảo hợp đồng để chuẩn bị trước.`;
            }
        }
    } catch (err) {
        console.warn("Load quote error, falling back to manual setup:", err);
        setupManualMode();
    }
}

function renderQuoteData(q) {
    const elCode = document.getElementById("contractQuoteCode");
    const elCust = document.getElementById("contractCustomer");
    const elOpp = document.getElementById("contractOpportunity");
    const elTotal = document.getElementById("contractTotal");
    const elLines = document.getElementById("contractLines");

    if (elCode) elCode.textContent = q.code;
    if (elCust) elCust.textContent = q.customer;
    if (elOpp) elOpp.textContent = q.opportunity;
    if (elTotal) elTotal.textContent = formatMoney(q.total);

    if (elLines) {
        elLines.innerHTML = "";
        const items = q.lines && q.lines.length ? q.lines : [
            { productName: q.title || "Gói giải pháp phần mềm", quantity: 1, unitPrice: q.total, discountPercent: 0, amount: q.total }
        ];

        items.forEach(it => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><strong>${escapeHtml(it.productName || it.product || 'Sản phẩm')}</strong></td>
                <td style="text-align:center;">${it.quantity || 1}</td>
                <td style="text-align:right;">${formatMoney(it.unitPrice || it.price || 0)}</td>
                <td style="text-align:center;">${it.discountPercent || it.discount || 0}%</td>
                <td style="text-align:right; font-weight:700;">${formatMoney(it.amount || ((it.quantity || 1) * (it.unitPrice || 0)))}</td>
            `;
            elLines.appendChild(tr);
        });
    }
}

async function setupManualMode() {
    const manualCust = document.getElementById("manualCustomerContainer");
    const manualTotal = document.getElementById("manualTotalContainer");
    const inheritSec = document.getElementById("inheritSection");

    if (manualCust) manualCust.style.display = "flex";
    if (manualTotal) manualTotal.style.display = "flex";
    if (inheritSec) inheritSec.style.display = "none";

    const custSelect = document.getElementById("contractCustomerSelect");
    if (!custSelect) return;

    try {
        const res = await fetch(`${API_BASE}/api/customers?page=1&size=100`, {
            method: "GET",
            credentials: "include",
            headers: { "Accept": "application/json" }
        });

        if (res.status === 401) {
            window.location.href = "login.html";
            return;
        }

        const json = await res.json();
        const list = Array.isArray(json?.data)
            ? json.data
            : Array.isArray(json?.data?.items)
                ? json.data.items
                : [];

        custSelect.innerHTML = '<option value="">-- Chọn khách hàng ký kết --</option>';
        list.forEach(c => {
            if (c.isDeleted !== true && c.is_deleted !== true) {
                const opt = document.createElement("option");
                opt.value = c.id;
                opt.textContent = `${c.name || 'Khách hàng'} ${c.phone ? ' - ' + c.phone : ''}`;
                custSelect.appendChild(opt);
            }
        });
    } catch (err) {
        console.error("Load customers error:", err);
        custSelect.innerHTML = '<option value="1">Công ty TNHH Công Nghệ Ánh Dương</option>';
    }
}

// Form Submission
document.getElementById("contractForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();

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

    if (!title) {
        showValidation("Vui lòng nhập tên hợp đồng.");
        return;
    }
    if (!number) {
        showValidation("Vui lòng nhập số hợp đồng.");
        return;
    }
    if (!customerId || isNaN(customerId) || customerId <= 0) {
        showValidation("Vui lòng chọn khách hàng cho hợp đồng.");
        return;
    }
    if (!signed || !effective) {
        showValidation("Vui lòng chọn ngày ký và ngày hiệu lực.");
        return;
    }

    hideValidation();

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

    if (createButton) {
        createButton.disabled = true;
        createButton.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang lưu vào MySQL...';
    }

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
            successText.textContent = `Hợp đồng "${title}" (Mã: ${number}) với giá trị ${formatMoney(totalAmount)} đã được lưu thành công vào MySQL!`;
        }

        document.getElementById("contractSuccess")?.classList.add("open");
    } catch (err) {
        console.error("Create contract error:", err);
        showValidation("Lỗi khi lưu hợp đồng: " + err.message);
    } finally {
        if (createButton) {
            createButton.disabled = false;
            createButton.innerHTML = '<i class="fa-solid fa-floppy-disk"></i> Lưu Hợp đồng vào MySQL';
        }
    }
});

function showValidation(msg) {
    if (validation) {
        validation.textContent = msg;
        validation.style.display = "block";
    }
}

function hideValidation() {
    if (validation) {
        validation.style.display = "none";
    }
}

function getValue(id) {
    const el = document.getElementById(id);
    return el ? el.value.trim() : "";
}

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