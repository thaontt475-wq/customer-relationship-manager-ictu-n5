"use strict";

const params =
    new URLSearchParams(
        window.location.search
    );

const quoteId =
    params.get("id");


let quote = getQuotes().find(item => String(item.id) === String(quoteId));

const blocked = document.getElementById("contractBlocked");
const createButton = document.getElementById("createContractButton");

async function initContractPage() {
    if (!quote && quoteId) {
        try {
            const res = await fetch(`http://localhost:8080/crm/api/quotes/${quoteId}`, { credentials: "include" });
            const json = await res.json();
            if (json?.success && json.data) {
                const q = json.data;
                quote = {
                    id: q.id,
                    code: q.quoteNumber || `BG-${q.id}`,
                    customer: q.customerName || "Khách hàng",
                    total: Number(q.totalAmount || 0),
                    status: (q.status || "").toLowerCase()
                };
            }
        } catch (_) {}
    }

    if (!quote) {
        blocked.hidden = false;
        blocked.innerHTML = "Không tìm thấy báo giá được chọn.";
        createButton.disabled = true;
    } else {
        renderQuoteData(quote);
        if (quote.status !== "approved") {
            blocked.hidden = false;
            blocked.innerHTML = "Báo giá chưa được phê duyệt nên không thể tạo hợp đồng.";
            createButton.disabled = true;
        } else {
            blocked.hidden = true;
            createButton.disabled = false;
        }
    }
}

initContractPage();


document
    .getElementById(
        "contractForm"
    )
    .addEventListener(
        "submit",
        event => {

            event.preventDefault();


            if (
                !quote
                ||
                quote.status
                !== "approved"
            ) {
                return;
            }


            const number =
                value(
                    "contractNumber"
                );

            const signed =
                value(
                    "signedDate"
                );

            const effective =
                value(
                    "effectiveDate"
                );


            const validation =
                document.getElementById(
                    "contractValidation"
                );


            if (
                !number
                ||
                !signed
                ||
                !effective
            ) {

                validation.textContent =
                    "Vui lòng nhập số hợp đồng, ngày ký và ngày hiệu lực.";

                return;
            }


            validation.textContent = "";


            /*
             * Lưu tạm FE để test thao tác.
             */
            const contracts =
                getContracts();


            contracts.push({
                id:
                    "contract_"
                    +
                    Date.now(),

                contractNumber:
                    number,

                signedDate:
                    signed,

                effectiveDate:
                    effective,

                expiryDate:
                    value(
                        "expiryDate"
                    ),

                paymentTerms:
                    value(
                        "contractPaymentTerms"
                    ),

                quoteId:
                    quote.id,

                quoteCode:
                    quote.code,

                customer:
                    quote.customer,

                opportunity:
                    quote.opportunity,

                total:
                    quote.total,

                lines:
                    quote.lines
            });


            localStorage.setItem(
                "crm_ui_contracts",
                JSON.stringify(
                    contracts
                )
            );


            document
                .getElementById(
                    "contractSuccess"
                )
                .classList
                .add("open");

        }
    );


function renderQuoteData(quote) {

    text(
        "contractQuoteCode",
        quote.code
    );

    text(
        "contractCustomer",
        quote.customer || "—"
    );

    text(
        "contractOpportunity",
        quote.opportunity || "—"
    );

    text(
        "contractTotal",
        money(
            quote.total
        )
    );


    document
        .getElementById(
            "contractPaymentTerms"
        )
        .value =
            quote.paymentTerms || "";


    const body =
        document.getElementById(
            "contractLines"
        );


    body.innerHTML = "";


    for (
        const line
        of quote.lines || []
    ) {

        const raw =
            Number(
                line.quantity || 0
            )
            *
            Number(
                line.price || 0
            );


        const total =
            raw
            *
            (
                1
                -
                Number(
                    line.discount || 0
                )
                / 100
            );


        const tr =
            document.createElement(
                "tr"
            );


        tr.innerHTML = `
            <td>
                ${escapeHtml(line.product || "—")}
            </td>

            <td>
                ${Number(line.quantity || 0)}
            </td>

            <td>
                ${money(line.price)}
            </td>

            <td>
                ${Number(line.discount || 0)}%
            </td>

            <td>
                ${money(total)}
            </td>
        `;


        body.appendChild(
            tr
        );
    }

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


function getContracts() {

    try {

        return JSON.parse(
            localStorage.getItem(
                "crm_ui_contracts"
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


function text(
    id,
    value
) {

    document
        .getElementById(id)
        .textContent =
            value;
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