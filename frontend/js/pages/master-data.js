"use strict";

const API_BASE =
    "http://localhost:8080/crm";

let currentCategory =
    "industry";

let values = [];


const categoryLabels = {
    "industry":
        "Ngành nghề",

    "company-size":
        "Quy mô doanh nghiệp",

    "lead-source":
        "Nguồn Lead",

    "activity-type":
        "Loại hoạt động"
};


const drawer =
    document.getElementById(
        "masterDrawer"
    );

const overlay =
    document.getElementById(
        "masterOverlay"
    );


document
    .querySelectorAll(
        ".category-button"
    )
    .forEach(button => {

        button.addEventListener(
            "click",
            async () => {

                document
                    .querySelectorAll(
                        ".category-button"
                    )
                    .forEach(item => {

                        item.classList.remove(
                            "active"
                        );

                    });

                button.classList.add(
                    "active"
                );

                currentCategory =
                    button.dataset.category;

                document
                    .getElementById(
                        "categoryTitle"
                    )
                    .textContent =
                        categoryLabels[
                            currentCategory
                        ]
                        ||
                        currentCategory;

                await loadValues();
            }
        );
    });


document
    .getElementById(
        "addMasterItem"
    )
    ?.addEventListener(
        "click",
        () => openDrawer()
    );


document
    .getElementById(
        "closeMasterDrawer"
    )
    ?.addEventListener(
        "click",
        closeDrawer
    );


document
    .getElementById(
        "cancelMaster"
    )
    ?.addEventListener(
        "click",
        closeDrawer
    );


overlay
    ?.addEventListener(
        "click",
        closeDrawer
    );


document
    .getElementById(
        "masterForm"
    )
    ?.addEventListener(
        "submit",
        async event => {

            event.preventDefault();

            await saveForm();
        }
    );


document.addEventListener(
    "click",
    async event => {

        const edit =
            event.target.closest(
                "[data-edit-master]"
            );

        if (edit) {

            openDrawer(
                Number(
                    edit.dataset.editMaster
                )
            );

            return;
        }


        const up =
            event.target.closest(
                "[data-master-up]"
            );

        if (up) {

            await moveItem(
                Number(
                    up.dataset.masterUp
                ),
                -1
            );

            return;
        }


        const down =
            event.target.closest(
                "[data-master-down]"
            );

        if (down) {

            await moveItem(
                Number(
                    down.dataset.masterDown
                ),
                1
            );
        }
    }
);


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
        await response.json()
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


async function loadValues() {

    try {

        const result =
            await api(
                `/api/master-data/${encodeURIComponent(currentCategory)}`
            );

        values =
            Array.isArray(
                result.data
            )
            ?
            result.data
            :
            [];

        render();

    } catch (error) {

        console.error(
            "Load master data error:",
            error
        );

        values = [];

        render();

        alert(
            error.message
            ||
            "Không thể tải danh mục."
        );
    }
}


async function saveForm() {

    const code =
        value(
            "masterCode"
        );

    const name =
        value(
            "masterName"
        );

    const status =
        value(
            "masterStatus"
        );

    const errorBox =
        document.getElementById(
            "masterError"
        );

    if (errorBox) {
        errorBox.textContent = "";
    }


    if (
        !code
        ||
        !name
    ) {

        if (errorBox) {

            errorBox.textContent =
                "Mã và tên hiển thị là bắt buộc.";
        }

        return;
    }


    const editingId =
        value(
            "editingMasterId"
        );


    const existing =
        editingId
        ?
        values.find(
            item =>
                Number(item.id)
                ===
                Number(editingId)
        )
        :
        null;


    const body = {
        code,
        name,

        active:
            status ===
            "active",

        displayOrder:
            existing
            ?
            Number(
                existing.displayOrder
            )
            :
            undefined
    };


    try {

        if (editingId) {

            await api(
                `/api/master-data/${encodeURIComponent(currentCategory)}/${editingId}`,
                {
                    method:
                        "PUT",

                    body:
                        JSON.stringify(
                            body
                        )
                }
            );

        } else {

            await api(
                `/api/master-data/${encodeURIComponent(currentCategory)}`,
                {
                    method:
                        "POST",

                    body:
                        JSON.stringify(
                            body
                        )
                }
            );
        }


        closeDrawer();

        await loadValues();

    } catch (error) {

        console.error(
            "Save master data error:",
            error
        );

        if (errorBox) {

            errorBox.textContent =
                error.message
                ||
                "Không thể lưu danh mục.";
        }
    }
}


async function moveItem(
    id,
    direction
) {

    const index =
        values.findIndex(
            item =>
                Number(item.id)
                ===
                Number(id)
        );


    const targetIndex =
        index + direction;


    if (
        index < 0
        ||
        targetIndex < 0
        ||
        targetIndex >=
            values.length
    ) {

        return;
    }


    const current =
        values[index];

    const target =
        values[targetIndex];


    const currentOrder =
        Number(
            current.displayOrder
        );

    const targetOrder =
        Number(
            target.displayOrder
        );


    try {

        await api(
            `/api/master-data/${encodeURIComponent(currentCategory)}/${current.id}`,
            {
                method:
                    "PUT",

                body:
                    JSON.stringify({
                        code:
                            current.code,

                        name:
                            current.name,

                        active:
                            current.active,

                        displayOrder:
                            targetOrder
                    })
            }
        );


        await api(
            `/api/master-data/${encodeURIComponent(currentCategory)}/${target.id}`,
            {
                method:
                    "PUT",

                body:
                    JSON.stringify({
                        code:
                            target.code,

                        name:
                            target.name,

                        active:
                            target.active,

                        displayOrder:
                            currentOrder
                    })
            }
        );


        await loadValues();

    } catch (error) {

        console.error(
            "Move master data error:",
            error
        );

        alert(
            error.message
            ||
            "Không thể thay đổi thứ tự."
        );
    }
}


function render() {

    const list =
        document.getElementById(
            "masterList"
        );

    const empty =
        document.getElementById(
            "masterEmpty"
        );


    if (!list) {
        return;
    }


    list.innerHTML = "";


    if (empty) {

        empty.style.display =
            values.length
            ?
            "none"
            :
            "flex";
    }


    values.forEach(
        (
            item,
            index
        ) => {

            const row =
                document.createElement(
                    "article"
                );


            const status =
                item.active
                ?
                "active"
                :
                "inactive";


            row.className =
                "master-item";


            row.innerHTML = `

                <div class="master-order">

                    <button
                        type="button"
                        data-master-up="${item.id}"
                        ${index === 0 ? "disabled" : ""}
                    >
                        ↑
                    </button>

                    <button
                        type="button"
                        data-master-down="${item.id}"
                        ${
                            index ===
                            values.length - 1
                            ?
                            "disabled"
                            :
                            ""
                        }
                    >
                        ↓
                    </button>

                </div>

                <div class="master-code">
                    ${escapeHtml(item.code)}
                </div>

                <div class="master-name">
                    ${escapeHtml(item.name)}
                </div>

                <span class="master-status ${status}">
                    ${
                        item.active
                        ?
                        "Đang dùng"
                        :
                        "Ngừng dùng"
                    }
                </span>

                <div>
                    <button
                        type="button"
                        data-edit-master="${item.id}"
                    >
                        Sửa
                    </button>
                </div>
            `;


            list.appendChild(
                row
            );
        }
    );
}


function openDrawer(
    editingId = null
) {

    document
        .getElementById(
            "masterForm"
        )
        ?.reset();


    setValue(
        "editingMasterId",
        ""
    );


    if (
        document.getElementById(
            "masterError"
        )
    ) {

        document.getElementById(
            "masterError"
        ).textContent =
            "";
    }


    document
        .getElementById(
            "masterDrawerCategory"
        )
        .textContent =
            categoryLabels[
                currentCategory
            ]
            ||
            currentCategory;


    if (editingId !== null) {

        const item =
            values.find(
                record =>
                    Number(record.id)
                    ===
                    Number(editingId)
            );


        if (item) {

            setValue(
                "editingMasterId",
                item.id
            );

            setValue(
                "masterCode",
                item.code
            );

            setValue(
                "masterName",
                item.name
            );

            setValue(
                "masterStatus",
                item.active
                ?
                "active"
                :
                "inactive"
            );


            document
                .getElementById(
                    "masterDrawerTitle"
                )
                .textContent =
                    "Sửa giá trị";

        }

    } else {

        setValue(
            "masterStatus",
            "active"
        );


        document
            .getElementById(
                "masterDrawerTitle"
            )
            .textContent =
                "Thêm giá trị";
    }


    drawer
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


function closeDrawer() {

    drawer
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
        .replace(
            /&/g,
            "&amp;"
        )
        .replace(
            /</g,
            "&lt;"
        )
        .replace(
            />/g,
            "&gt;"
        )
        .replace(
            /"/g,
            "&quot;"
        )
        .replace(
            /'/g,
            "&#039;"
        );
}


loadValues();