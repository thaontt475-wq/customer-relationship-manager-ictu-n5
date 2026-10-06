"use strict";

const quoteTableBody =
    document.getElementById(
        "quoteTableBody"
    );

const quoteEmpty =
    document.getElementById(
        "quoteEmpty"
    );

const mobileList =
    document.getElementById(
        "quoteMobileList"
    );

const search =
    document.getElementById(
        "quoteSearch"
    );

const statusFilter =
    document.getElementById(
        "quoteStatusFilter"
    );


function loadQuotes() {

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


function render() {

    const quotes =
        loadQuotes();

    const query =
        search.value
            .trim()
            .toLowerCase();

    const status =
        statusFilter.value;


    const filtered =
        quotes.filter(quote => {

            const text =
                (
                    (quote.code || "")
                    + " "
                    + (quote.customer || "")
                )
                .toLowerCase();

            return (
                (
                    !query
                    ||
                    text.includes(query)
                )
                &&
                (
                    !status
                    ||
                    quote.status === status
                )
            );

        });


    quoteTableBody.innerHTML = "";
    mobileList.innerHTML = "";


    quoteEmpty.style.display =
        filtered.length
            ? "none"
            : "flex";


    for (const quote of filtered) {

        const tr =
            document.createElement("tr");

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
            </td>

            <td>
                ${escapeHtml(quote.validUntil || "—")}
            </td>

            <td>
                <div class="quote-actions">

                    <a
                        class="quote-action"
                        href="quote-editor.html?id=${encodeURIComponent(quote.id)}"
                    >
                        Xem / Sửa
                    </a>

                    ${
                        quote.status === "approved"
                        ? `
                        <a
                            class="quote-action contract"
                            href="contract-create.html?id=${encodeURIComponent(quote.id)}"
                        >
                            Tạo HĐ
                        </a>
                        `
                        : ""
                    }

                </div>
            </td>
        `;

        quoteTableBody.appendChild(tr);


        const card =
            document.createElement("article");

        card.className =
            "quote-mobile-card";

        card.innerHTML = `
            <div class="quote-mobile-head">

                <div>
                    <strong>
                        ${escapeHtml(quote.code)}
                    </strong>

                    <div style="margin-top:5px">
                        ${escapeHtml(quote.customer || "—")}
                    </div>
                </div>

                <span class="quote-status ${quote.status}">
                    ${statusLabel(quote.status)}
                </span>

            </div>

            <div class="quote-mobile-info">

                <span>
                    Tổng:
                    ${money(quote.total)}
                </span>

                <span>
                    Chiết khấu:
                    ${Number(quote.discountPercent || 0).toFixed(1)}%
                </span>

                <span>
                    Hiệu lực:
                    ${escapeHtml(quote.validUntil || "—")}
                </span>

            </div>

            <div class="quote-mobile-actions">

                <a
                    class="crm-btn crm-btn-secondary"
                    style="text-decoration:none"
                    href="quote-editor.html?id=${encodeURIComponent(quote.id)}"
                >
                    Xem / Sửa
                </a>

                ${
                    quote.status === "approved"
                    ? `
                    <a
                        class="crm-btn crm-btn-primary"
                        style="text-decoration:none"
                        href="contract-create.html?id=${encodeURIComponent(quote.id)}"
                    >
                        Tạo Hợp đồng
                    </a>
                    `
                    : `
                    <button
                        class="crm-btn crm-btn-secondary"
                        disabled
                    >
                        Chưa duyệt
                    </button>
                    `
                }

            </div>
        `;

        mobileList.appendChild(card);
    }

}


search.addEventListener(
    "input",
    render
);


statusFilter.addEventListener(
    "change",
    render
);


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


function escapeHtml(value) {

    return String(value ?? "")
        .replace(/&/g,"&amp;")
        .replace(/</g,"&lt;")
        .replace(/>/g,"&gt;")
        .replace(/"/g,"&quot;")
        .replace(/'/g,"&#039;");
}


render();