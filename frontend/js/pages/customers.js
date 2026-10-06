"use strict";

/*
 * FE-only prototype.
 * Không gọi backend.
 * Không có dữ liệu mẫu được nạp sẵn.
 * Dữ liệu chỉ tồn tại trong RAM sau khi người dùng tự nhập.
 */

const records = [];

const tableBody =
    document.getElementById("customerTableBody");

const desktopEmpty =
    document.getElementById("customerEmpty");

const mobileList =
    document.getElementById("customerMobileList");

const drawer =
    document.getElementById("customerDrawer");

const drawerOverlay =
    document.getElementById("customerDrawerOverlay");

const openDrawerButton =
    document.getElementById("openCustomerDrawer");

const closeDrawerButton =
    document.getElementById("closeCustomerDrawer");

const cancelButton =
    document.getElementById("cancelCustomer");

const form =
    document.getElementById("customerForm");

const searchInput =
    document.getElementById("customerSearch");

const statusFilter =
    document.getElementById("statusFilter");


openDrawerButton.addEventListener(
    "click",
    () => openDrawer()
);

closeDrawerButton.addEventListener(
    "click",
    closeDrawer
);

cancelButton.addEventListener(
    "click",
    closeDrawer
);

drawerOverlay.addEventListener(
    "click",
    closeDrawer
);


document.addEventListener(
    "keydown",
    event => {

        if (event.key === "Escape") {
            closeDrawer();
        }

    }
);


form.addEventListener(
    "submit",
    event => {

        event.preventDefault();

        const companyName =
            value("companyName");

        const error =
            document.getElementById(
                "companyNameError"
            );

        if (!companyName) {

            error.textContent =
                "Vui lòng nhập tên công ty.";

            return;
        }

        error.textContent = "";

        const record = {
            companyName,
            taxCode: value("taxCode"),
            status: value("customerStatus"),
            email: value("customerEmail"),
            phone: value("customerPhone"),
            industry: value("industry"),
            companySize: value("companySize"),
            website: value("website"),
            address: value("address"),
            owner: value("owner")
        };

        const editingIndex =
            document.getElementById(
                "editingIndex"
            ).value;

        if (editingIndex === "") {

            records.push(record);

        } else {

            records[
                Number(editingIndex)
            ] = record;

        }

        closeDrawer();

        render();

    }
);


searchInput.addEventListener(
    "input",
    render
);

statusFilter.addEventListener(
    "change",
    render
);


function render() {

    const query =
        searchInput.value
            .trim()
            .toLowerCase();

    const status =
        statusFilter.value;

    const filtered =
        records
            .map((record,index) => ({
                record,
                index
            }))
            .filter(item => {

                const haystack = [
                    item.record.companyName,
                    item.record.email,
                    item.record.phone,
                    item.record.taxCode
                ]
                    .join(" ")
                    .toLowerCase();

                const matchesSearch =
                    !query
                    || haystack.includes(query);

                const matchesStatus =
                    !status
                    || item.record.status === status;

                return (
                    matchesSearch
                    && matchesStatus
                );

            });


    tableBody.innerHTML = "";
    mobileList.innerHTML = "";


    desktopEmpty.style.display =
        filtered.length
            ? "none"
            : "flex";


    if (!filtered.length) {

        const empty =
            document.createElement("div");

        empty.className =
            "customer-mobile-empty";

        empty.textContent =
            records.length
                ? "Không có bản ghi phù hợp với bộ lọc."
                : "Chưa có khách hàng.";

        mobileList.appendChild(empty);

        return;
    }


    for (const item of filtered) {

        renderDesktopRow(
            item.record,
            item.index
        );

        renderMobileCard(
            item.record,
            item.index
        );

    }
}


function renderDesktopRow(
    record,
    index
) {

    const tr =
        document.createElement("tr");

    tr.innerHTML = `
        <td>
            <input type="checkbox">
        </td>

        <td>
            <a
                class="customer-name"
                href="customer-360.html"
            >
                ${escapeHtml(record.companyName)}
            </a>

            <span class="customer-sub">
                ${escapeHtml(record.taxCode || "—")}
            </span>
        </td>

        <td>
            ${escapeHtml(record.email || "—")}

            <span class="customer-sub">
                ${escapeHtml(record.phone || "—")}
            </span>
        </td>

        <td>
            <span class="
                status-pill
                ${statusClass(record.status)}
            ">
                ${escapeHtml(record.status)}
            </span>
        </td>

        <td>
            ${escapeHtml(record.owner || "—")}
        </td>

        <td class="action-col">

            <div class="row-action-wrap">

                <button
                    class="row-action-button"
                    type="button"
                    data-action-menu="${index}"
                >
                    ⋮
                </button>

                <div
                    class="row-action-menu"
                    data-menu="${index}"
                >

                    <a href="customer-360.html">
                        Xem chi tiết
                    </a>

                    <button
                        type="button"
                        data-edit="${index}"
                    >
                        Sửa
                    </button>

                    <button
                        type="button"
                        class="danger"
                        data-delete="${index}"
                    >
                        Xóa khỏi giao diện
                    </button>

                </div>

            </div>

        </td>
    `;

    tableBody.appendChild(tr);

}


function renderMobileCard(
    record,
    index
) {

    const card =
        document.createElement("article");

    card.className =
        "customer-mobile-card";

    card.innerHTML = `
        <div class="mobile-card-head">

            <div>

                <strong>
                    ${escapeHtml(record.companyName)}
                </strong>

                <div style="margin-top:6px">
                    <span class="
                        status-pill
                        ${statusClass(record.status)}
                    ">
                        ${escapeHtml(record.status)}
                    </span>
                </div>

            </div>

            <button
                class="row-action-button"
                type="button"
                data-edit="${index}"
            >
                ⋮
            </button>

        </div>

        <div class="mobile-card-contact">

            <span>
                ✉ ${escapeHtml(record.email || "—")}
            </span>

            <span>
                ☎ ${escapeHtml(record.phone || "—")}
            </span>

            <span>
                Người sở hữu:
                ${escapeHtml(record.owner || "—")}
            </span>

        </div>

        <a
            href="customer-360.html"
            class="crm-btn crm-btn-secondary"
            style="
                margin-top:12px;
                width:100%;
                text-decoration:none;
            "
        >
            Xem chi tiết
        </a>
    `;

    mobileList.appendChild(card);

}


document.addEventListener(
    "click",
    event => {

        const actionButton =
            event.target.closest(
                "[data-action-menu]"
            );

        if (actionButton) {

            const index =
                actionButton.dataset.actionMenu;

            document
                .querySelectorAll(
                    ".row-action-menu"
                )
                .forEach(menu => {

                    if (
                        menu.dataset.menu
                        !== index
                    ) {
                        menu.classList.remove(
                            "open"
                        );
                    }

                });

            document
                .querySelector(
                    `[data-menu="${index}"]`
                )
                ?.classList
                .toggle("open");

            return;
        }


        const editButton =
            event.target.closest(
                "[data-edit]"
            );

        if (editButton) {

            openDrawer(
                Number(
                    editButton.dataset.edit
                )
            );

            return;
        }


        const deleteButton =
            event.target.closest(
                "[data-delete]"
            );

        if (deleteButton) {

            records.splice(
                Number(
                    deleteButton.dataset.delete
                ),
                1
            );

            render();

        }


        if (
            !event.target.closest(
                ".row-action-wrap"
            )
        ) {
            document
                .querySelectorAll(
                    ".row-action-menu"
                )
                .forEach(menu => {
                    menu.classList.remove(
                        "open"
                    );
                });
        }

    }
);


function openDrawer(index = null) {

    resetForm();

    if (
        index !== null
        && records[index]
    ) {

        const record =
            records[index];

        document.getElementById(
            "drawerTitle"
        ).textContent =
            "Sửa Khách hàng / Lead";

        document.getElementById(
            "editingIndex"
        ).value = String(index);

        setValue(
            "companyName",
            record.companyName
        );

        setValue(
            "taxCode",
            record.taxCode
        );

        setValue(
            "customerStatus",
            record.status
        );

        setValue(
            "customerEmail",
            record.email
        );

        setValue(
            "customerPhone",
            record.phone
        );

        setValue(
            "industry",
            record.industry
        );

        setValue(
            "companySize",
            record.companySize
        );

        setValue(
            "website",
            record.website
        );

        setValue(
            "address",
            record.address
        );

        setValue(
            "owner",
            record.owner
        );

    }

    drawer.classList.add(
        "open"
    );

    drawerOverlay.classList.add(
        "open"
    );

}


function closeDrawer() {

    drawer.classList.remove(
        "open"
    );

    drawerOverlay.classList.remove(
        "open"
    );

}


function resetForm() {

    form.reset();

    document.getElementById(
        "editingIndex"
    ).value = "";

    document.getElementById(
        "drawerTitle"
    ).textContent =
        "Thêm mới Khách hàng / Lead";

    document.getElementById(
        "companyNameError"
    ).textContent = "";

}


function statusClass(status) {

    switch (status) {

        case "Đang giao dịch":
            return "status-dealing";

        case "Khách hàng":
            return "status-customer";

        case "Ngừng hợp tác":
            return "status-inactive";

        default:
            return "status-prospect";

    }

}


function value(id) {

    return document
        .getElementById(id)
        .value
        .trim();

}


function setValue(id,value) {

    document
        .getElementById(id)
        .value = value || "";

}


function escapeHtml(value) {

    return String(value)
        .replace(/&/g,"&amp;")
        .replace(/</g,"&lt;")
        .replace(/>/g,"&gt;")
        .replace(/"/g,"&quot;")
        .replace(/'/g,"&#039;");

}


render();