"use strict";

const API_BASE =
    "http://localhost:8080/crm";

let winReasons = [];
let lossReasons = [];
let competitors = [];


const modal =
    document.getElementById(
        "itemModal"
    );

const overlay =
    document.getElementById(
        "itemOverlay"
    );


document.addEventListener(
    "DOMContentLoaded",
    async () => {

        bindEvents();

        await loadAll();
    }
);


function bindEvents() {

    document.addEventListener(
        "click",
        event => {

            const addButton =
                event.target.closest(
                    "[data-add-list]"
                );

            if (addButton) {

                openItemModal(
                    addButton.dataset.addList
                );

                return;
            }
        }
    );


    document
        .getElementById(
            "closeItemModal"
        )
        ?.addEventListener(
            "click",
            closeItemModal
        );


    document
        .getElementById(
            "cancelItem"
        )
        ?.addEventListener(
            "click",
            closeItemModal
        );


    overlay
        ?.addEventListener(
            "click",
            closeItemModal
        );


    document
        .getElementById(
            "itemForm"
        )
        ?.addEventListener(
            "submit",
            async event => {

                event.preventDefault();

                await saveItem();
            }
        );
}


async function api(
    path,
    options = {}
) {

    const response =
        await fetch(
            API_BASE + path,
            {
                credentials:
                    "include",

                headers: {
                    "Content-Type":
                        "application/json",

                    ...(options.headers || {})
                },

                ...options
            }
        );


    const result =
        await response
            .json()
            .catch(
                () => ({
                    success: false,
                    message:
                        "Phản hồi API không hợp lệ"
                })
            );


    if (!response.ok) {

        throw new Error(
            result.message
            ||
            "Có lỗi xảy ra"
        );
    }


    return result;
}


async function loadAll() {

    await Promise.all([
        loadReasons("WON"),
        loadReasons("LOST"),
        loadCompetitors()
    ]);

    renderAll();
}


async function loadReasons(type) {

    try {

        const result =
            await api(
                `/api/win-loss-reasons?type=${type}`
            );


        const data =
            Array.isArray(result.data)
            ?
            result.data
            :
            (
                result.data?.reasons
                ??
                []
            );


        if (type === "WON") {

            winReasons =
                data;

        } else {

            lossReasons =
                data;
        }

    } catch (error) {

        console.error(
            `Load ${type} reasons error:`,
            error
        );
    }
}


async function loadCompetitors() {

    try {

        const result =
            await api(
                "/api/competitors"
            );


        competitors =
            Array.isArray(result.data)
            ?
            result.data
            :
            (
                result.data?.competitors
                ??
                []
            );

    } catch (error) {

        console.error(
            "Load competitors error:",
            error
        );
    }
}


function openItemModal(type) {

    document
        .getElementById(
            "itemForm"
        )
        ?.reset();


    setValue(
        "editingItemId",
        ""
    );

    setValue(
        "itemType",
        type
    );


    const status =
        document.getElementById(
            "itemStatus"
        );

    if (status) {

        status.value =
            "active";
    }


    const error =
        document.getElementById(
            "itemError"
        );

    if (error) {

        error.textContent = "";
    }


    const title =
        document.getElementById(
            "itemModalTitle"
        );


    if (title) {

        if (type === "win") {

            title.textContent =
                "Thêm lý do thắng";

        } else if (type === "loss") {

            title.textContent =
                "Thêm lý do thua";

        } else {

            title.textContent =
                "Thêm đối thủ cạnh tranh";
        }
    }


    modal
        ?.classList
        .add(
            "open"
        );

    overlay
        ?.classList
        .add(
            "open"
        );
}


function closeItemModal() {

    modal
        ?.classList
        .remove(
            "open"
        );

    overlay
        ?.classList
        .remove(
            "open"
        );
}


async function saveItem() {

    const type =
        value(
            "itemType"
        );

    const name =
        value(
            "itemName"
        );

    const error =
        document.getElementById(
            "itemError"
        );


    if (error) {

        error.textContent = "";
    }


    if (!name) {

        if (error) {

            error.textContent =
                "Tên là bắt buộc.";
        }

        return;
    }


    try {

        if (
            type === "win"
            ||
            type === "loss"
        ) {

            await api(
                "/api/win-loss-reasons",
                {
                    method:
                        "POST",

                    body:
                        JSON.stringify({
                            type:
                                type === "win"
                                ?
                                "WON"
                                :
                                "LOST",

                            name
                        })
                }
            );

        } else if (
            type ===
            "competitor"
        ) {

            await api(
                "/api/competitors",
                {
                    method:
                        "POST",

                    body:
                        JSON.stringify({
                            name,
                            note: ""
                        })
                }
            );

        } else {

            throw new Error(
                "Loại dữ liệu không hợp lệ"
            );
        }


        closeItemModal();

        await loadAll();

    } catch (err) {

        console.error(
            "Save win/loss item error:",
            err
        );


        if (error) {

            error.textContent =
                err.message
                ||
                "Không thể lưu dữ liệu.";
        }
    }
}


function renderAll() {

    renderReasonList(
        "winReasonList",
        "winReasonEmpty",
        winReasons
    );

    renderReasonList(
        "lossReasonList",
        "lossReasonEmpty",
        lossReasons
    );

    renderCompetitors();
}


function renderReasonList(
    listId,
    emptyId,
    items
) {

    const list =
        document.getElementById(
            listId
        );

    const empty =
        document.getElementById(
            emptyId
        );


    if (!list) {
        return;
    }


    list.innerHTML = "";


    if (empty) {

        empty.style.display =
            items.length
            ?
            "none"
            :
            "";
    }


    for (
        const item
        of items
    ) {

        const row =
            document.createElement(
                "div"
            );


        row.className =
            "settings-item";


        row.innerHTML = `
            <div class="settings-item-main">

                <strong>
                    ${escapeHtml(item.name)}
                </strong>

            </div>

            <span class="settings-status">
                ${
                    item.active
                    ?
                    "Đang dùng"
                    :
                    "Ngừng dùng"
                }
            </span>
        `;


        list.appendChild(
            row
        );
    }
}


function renderCompetitors() {

    const list =
        document.getElementById(
            "competitorList"
        );

    const empty =
        document.getElementById(
            "competitorEmpty"
        );


    if (!list) {
        return;
    }


    list.innerHTML = "";


    if (empty) {

        empty.style.display =
            competitors.length
            ?
            "none"
            :
            "";
    }


    for (
        const item
        of competitors
    ) {

        const row =
            document.createElement(
                "div"
            );


        row.className =
            "settings-item";


        row.innerHTML = `
            <div class="settings-item-main">

                <strong>
                    ${escapeHtml(item.name)}
                </strong>

                ${
                    item.note
                    ?
                    `
                    <div class="settings-item-note">
                        ${escapeHtml(item.note)}
                    </div>
                    `
                    :
                    ""
                }

            </div>

            <span class="settings-status">
                ${
                    item.active
                    ?
                    "Đang dùng"
                    :
                    "Ngừng dùng"
                }
            </span>
        `;


        list.appendChild(
            row
        );
    }
}


function value(id) {

    return document
        .getElementById(id)
        ?.value
        ?.trim()
        ??
        "";
}


function setValue(
    id,
    newValue
) {

    const element =
        document.getElementById(
            id
        );

    if (element) {

        element.value =
            newValue ?? "";
    }
}


function escapeHtml(value) {

    return String(
        value ?? ""
    )
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}