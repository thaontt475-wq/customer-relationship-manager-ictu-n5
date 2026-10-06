"use strict";

let lines = [];
let currentId = null;
let currentStatus = "draft";

const lineBody =
    document.getElementById(
        "quoteLineBody"
    );

const lineEmpty =
    document.getElementById(
        "lineEmpty"
    );


const params =
    new URLSearchParams(
        window.location.search
    );

currentId =
    params.get("id");


if (currentId) {
    loadExistingQuote(
        currentId
    );
}


document
    .getElementById(
        "addQuoteLine"
    )
    .addEventListener(
        "click",
        () => {

            lines.push({
                product: "",
                quantity: 1,
                price: 0,
                discount: 0
            });

            renderLines();

        }
    );


document
    .getElementById(
        "saveDraftButton"
    )
    .addEventListener(
        "click",
        () => {

            currentStatus = "draft";

            saveQuote();

        }
    );


document
    .getElementById(
        "submitApprovalButton"
    )
    .addEventListener(
        "click",
        () => {

            currentStatus = "pending";

            saveQuote();

        }
    );


document
    .getElementById(
        "approveQuote"
    )
    .addEventListener(
        "click",
        () => {

            currentStatus = "approved";

            saveQuote();

        }
    );


document
    .getElementById(
        "rejectQuote"
    )
    .addEventListener(
        "click",
        () => {

            currentStatus = "rejected";

            saveQuote();

        }
    );


lineBody.addEventListener(
    "input",
    event => {

        const input =
            event.target.closest(
                "[data-line-field]"
            );

        if (!input) return;


        const index =
            Number(
                input.dataset.index
            );

        const field =
            input.dataset.lineField;


        if (!lines[index]) return;


        if (
            field === "product"
        ) {

            lines[index][field] =
                input.value;

        } else {

            lines[index][field] =
                Math.max(
                    0,
                    Number(
                        input.value
                    ) || 0
                );

            if (
                field === "discount"
            ) {

                lines[index][field] =
                    Math.min(
                        100,
                        lines[index][field]
                    );
            }

        }

        updateSummary();
        updateLineTotal(index);

    }
);


lineBody.addEventListener(
    "click",
    event => {

        const remove =
            event.target.closest(
                "[data-remove-line]"
            );

        if (!remove) return;


        lines.splice(
            Number(
                remove.dataset.removeLine
            ),
            1
        );

        renderLines();

    }
);


function renderLines() {

    lineBody.innerHTML = "";

    lineEmpty.style.display =
        lines.length
            ? "none"
            : "flex";


    lines.forEach(
        (line,index) => {

            const tr =
                document.createElement(
                    "tr"
                );

            tr.innerHTML = `
                <td class="line-product">

                    <input
                        type="text"
                        value="${escapeAttribute(line.product)}"
                        placeholder="Tên sản phẩm / dịch vụ"
                        data-line-field="product"
                        data-index="${index}"
                    >

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

                <td
                    class="line-total"
                    id="lineTotal${index}"
                >
                    ${money(lineAmount(line))}
                </td>

                <td>

                    <button
                        type="button"
                        class="remove-line"
                        data-remove-line="${index}"
                    >
                        ×
                    </button>

                </td>
            `;

            lineBody.appendChild(tr);

        }
    );


    updateSummary();

}


function updateLineTotal(index) {

    const cell =
        document.getElementById(
            `lineTotal${index}`
        );

    if (
        cell
        &&
        lines[index]
    ) {

        cell.textContent =
            money(
                lineAmount(
                    lines[index]
                )
            );

    }

}


function lineAmount(line) {

    const raw =
        Number(line.quantity || 0)
        *
        Number(line.price || 0);

    return raw
        *
        (
            1
            -
            Number(
                line.discount || 0
            )
            / 100
        );

}


function updateSummary() {

    let subtotal = 0;
    let total = 0;


    for (const line of lines) {

        const raw =
            Number(line.quantity || 0)
            *
            Number(line.price || 0);

        subtotal += raw;
        total += lineAmount(line);

    }


    const discount =
        subtotal - total;


    const discountPercent =
        subtotal > 0
            ? discount / subtotal * 100
            : 0;


    document
        .getElementById(
            "subtotalText"
        )
        .textContent =
            money(subtotal);


    document
        .getElementById(
            "discountText"
        )
        .textContent =
            money(discount);


    document
        .getElementById(
            "totalText"
        )
        .textContent =
            money(total);


    /*
     * Chỉ là ngưỡng UI mô phỏng.
     * Backend sẽ quyết định thật sau.
     */
    document
        .getElementById(
            "approvalWarning"
        )
        .hidden =
            discountPercent < 10;

}


function saveQuote() {

    const code =
        value("quoteCode");

    if (!code) {

        alert(
            "Vui lòng nhập mã báo giá."
        );

        document
            .getElementById(
                "quoteCode"
            )
            .focus();

        return;
    }


    if (!lines.length) {

        alert(
            "Vui lòng thêm ít nhất một dòng sản phẩm."
        );

        return;
    }


    const summary =
        calculateSummary();


    const quote = {
        id:
            currentId
            ||
            "quote_"
            +
            Date.now(),

        code,

        customer:
            value(
                "quoteCustomer"
            ),

        opportunity:
            value(
                "quoteOpportunity"
            ),

        validUntil:
            value(
                "quoteValidUntil"
            ),

        paymentTerms:
            value(
                "paymentTerms"
            ),

        deliveryTerms:
            value(
                "deliveryTerms"
            ),

        lines,

        subtotal:
            summary.subtotal,

        discount:
            summary.discount,

        discountPercent:
            summary.discountPercent,

        total:
            summary.total,

        status:
            currentStatus
    };


    const quotes =
        getQuotes();


    const existingIndex =
        quotes.findIndex(
            item =>
                item.id === quote.id
        );


    const existingQuote =
        existingIndex >= 0
            ? quotes[existingIndex]
            : null;

    const oldDiscountPercent =
        existingQuote
            ? (existingQuote.discountPercent || 0)
            : 0;

    const oldDiscountAmount =
        existingQuote
            ? (existingQuote.discount || 0)
            : 0;

    const newDiscountPercent =
        quote.discountPercent || 0;

    const newDiscountAmount =
        quote.discount || 0;

    if (!existingQuote && newDiscountPercent > 0) {
        recordAuditLog({
            entity: "DISCOUNT",
            entityId: quote.code,
            action: "UPDATE_DISCOUNT",
            description: `Thiết lập chiết khấu báo giá ${quote.code}`,
            beforeValue: "0% (0 đ)",
            afterValue: `${newDiscountPercent.toFixed(1)}% (${money(newDiscountAmount)})`
        });
    } else if (existingQuote && (Math.abs(oldDiscountPercent - newDiscountPercent) > 0.01 || Math.abs(oldDiscountAmount - newDiscountAmount) > 1)) {
        recordAuditLog({
            entity: "DISCOUNT",
            entityId: quote.code,
            action: "UPDATE_DISCOUNT",
            description: `Điều chỉnh chiết khấu báo giá ${quote.code}`,
            beforeValue: `${oldDiscountPercent.toFixed(1)}% (${money(oldDiscountAmount)})`,
            afterValue: `${newDiscountPercent.toFixed(1)}% (${money(newDiscountAmount)})`
        });
    }

    if (
        existingIndex >= 0
    ) {

        quotes[
            existingIndex
        ] = quote;

    } else {

        quotes.push(
            quote
        );

    }


    localStorage.setItem(
        "crm_ui_quotes",
        JSON.stringify(
            quotes
        )
    );


    currentId =
        quote.id;


    window.location.href =
        "quotations.html";

}


function calculateSummary() {

    let subtotal = 0;
    let total = 0;


    for (const line of lines) {

        const raw =
            Number(line.quantity || 0)
            *
            Number(line.price || 0);

        subtotal += raw;

        total +=
            lineAmount(line);

    }


    const discount =
        subtotal - total;


    return {
        subtotal,
        total,
        discount,
        discountPercent:
            subtotal
            ?
            discount / subtotal * 100
            :
            0
    };

}


function loadExistingQuote(id) {

    const quote =
        getQuotes()
            .find(
                item =>
                    item.id === id
            );


    if (!quote) return;


    document
        .getElementById(
            "editorPageTitle"
        )
        .textContent =
            "Chỉnh sửa Báo giá";


    setValue(
        "quoteCode",
        quote.code
    );

    setValue(
        "quoteCustomer",
        quote.customer
    );

    setValue(
        "quoteOpportunity",
        quote.opportunity
    );

    setValue(
        "quoteValidUntil",
        quote.validUntil
    );

    setValue(
        "paymentTerms",
        quote.paymentTerms
    );

    setValue(
        "deliveryTerms",
        quote.deliveryTerms
    );


    currentStatus =
        quote.status || "draft";


    lines =
        Array.isArray(
            quote.lines
        )
        ?
        quote.lines
        :
        [];


    renderLines();

}


function getQuotes() {

    try {

        return JSON.parse(
            localStorage.getItem(
                "crm_ui_quotes"
            )
        ) || [];

    } catch {

        return [];
    }

}


function value(id) {

    return document
        .getElementById(id)
        .value
        .trim();

}


function setValue(
    id,
    value
) {

    document
        .getElementById(id)
        .value =
            value || "";

}


function money(value) {

    return new Intl.NumberFormat(
        "vi-VN",
        {
            style: "currency",
            currency: "VND",
            maximumFractionDigits: 0
        }
    ).format(
        Number(value) || 0
    );

}


function escapeAttribute(value) {

    return String(value ?? "")
        .replace(/&/g,"&amp;")
        .replace(/"/g,"&quot;")
        .replace(/</g,"&lt;")
        .replace(/>/g,"&gt;");
}


renderLines();


/* =========================================================
   AUDIT LOG HELPER
========================================================= */

async function recordAuditLog(entry) {
    const API_BASE = "http://localhost:8080/crm";
    const timestamp = new Date().toISOString();

    let actorName = "Quản trị viên";
    let actorId = 1;
    let actorEmail = "admin@company.com";

    try {
        const sessionRaw = localStorage.getItem("crm_ui_session");
        if (sessionRaw) {
            const sess = JSON.parse(sessionRaw);
            if (sess?.fullName) actorName = sess.fullName;
            if (sess?.id) actorId = sess.id;
            if (sess?.email) actorEmail = sess.email;
        }
    } catch (_) {}

    try {
        await fetch(`${API_BASE}/api/audit-logs`, {
            method: "POST",
            credentials: "include",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify({
                entity: entry.entity,
                entityId: entry.entityId,
                action: entry.action,
                description: entry.description,
                beforeValue: entry.beforeValue,
                afterValue: entry.afterValue
            })
        });
    } catch (err) {
        console.warn("Backend audit log record warning:", err);
    }

    try {
        const raw = localStorage.getItem("crm_audit_logs");
        const list = raw ? JSON.parse(raw) : [];
        list.unshift({
            id: `local_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
            createdAt: timestamp,
            userId: actorId,
            userName: actorName,
            userEmail: actorEmail,
            entity: entry.entity,
            entityId: entry.entityId,
            action: entry.action,
            description: entry.description,
            beforeValue: entry.beforeValue,
            afterValue: entry.afterValue
        });
        if (list.length > 100) list.length = 100;
        localStorage.setItem("crm_audit_logs", JSON.stringify(list));
    } catch (_) {}
}