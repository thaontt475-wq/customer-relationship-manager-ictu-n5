"use strict";

const API_BASE =
    "http://localhost:8080/crm";

let fields = [];


const drawer =
    document.getElementById(
        "customDrawer"
    );

const overlay =
    document.getElementById(
        "customOverlay"
    );


initSelects();


document
    .getElementById(
        "addCustomField"
    )
    ?.addEventListener(
        "click",
        () => openDrawer()
    );


document
    .getElementById(
        "closeCustomDrawer"
    )
    ?.addEventListener(
        "click",
        closeDrawer
    );


document
    .getElementById(
        "cancelCustomField"
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
        "fieldType"
    )
    ?.addEventListener(
        "change",
        toggleOptions
    );


document
    .getElementById(
        "customFieldForm"
    )
    ?.addEventListener(
        "submit",
        async event => {

            event.preventDefault();

            await saveField();
        }
    );


document
    .getElementById(
        "moduleFilter"
    )
    ?.addEventListener(
        "change",
        loadFields
    );


document
    .getElementById(
        "fieldStatusFilter"
    )
    ?.addEventListener(
        "change",
        render
    );


document.addEventListener(
    "click",
    event => {

        const edit =
            event.target.closest(
                "[data-edit-field]"
            );

        if (edit) {

            openDrawer(
                Number(
                    edit.dataset.editField
                )
            );
        }
    }
);


function initSelects() {

    const moduleFilter =
        document.getElementById(
            "moduleFilter"
        );

    const fieldModule =
        document.getElementById(
            "fieldModule"
        );

    if (moduleFilter) {

        moduleFilter.innerHTML = `
            <option value="CUSTOMER">
                Khách hàng
            </option>

            <option value="OPPORTUNITY">
                Cơ hội
            </option>
        `;
    }


    if (fieldModule) {

        fieldModule.innerHTML = `
            <option value="CUSTOMER">
                Khách hàng
            </option>

            <option value="OPPORTUNITY">
                Cơ hội
            </option>
        `;
    }


    const fieldType =
        document.getElementById(
            "fieldType"
        );

    if (fieldType) {

        fieldType.innerHTML = `
            <option value="TEXT">
                Text
            </option>

            <option value="NUMBER">
                Number
            </option>

            <option value="DATE">
                Date
            </option>

            <option value="SELECT">
                Select
            </option>
        `;
    }
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
        await response.json()
            .catch(
                () => ({
                    success: false,
                    message:
                        "Phản hồi API không hợp lệ"
                })
            );


    if (!response.ok) {

        if (
            response.status === 401
            ||
            response.status === 403
        ) {

            throw new Error(
                result.message
                ||
                "Phiên đăng nhập không hợp lệ."
            );
        }

        throw new Error(
            result.message
            ||
            "Có lỗi xảy ra"
        );
    }


    return result;
}


async function loadFields() {

    const entityType =
        value(
            "moduleFilter"
        )
        ||
        "CUSTOMER";


    try {

        const result =
            await api(
                `/api/custom-fields?entityType=${encodeURIComponent(entityType)}`
            );


        fields =
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
            "Load custom fields error:",
            error
        );

        fields = [];

        render();

        alert(
            error.message
            ||
            "Không thể tải custom field."
        );
    }
}


async function saveField() {

    const entityType =
        value(
            "fieldModule"
        );

    const label =
        value(
            "fieldLabel"
        );

    const key =
        value(
            "fieldKey"
        );

    const fieldType =
        value(
            "fieldType"
        );

    const editingId =
        value(
            "editingFieldId"
        );

    const required =
        Boolean(
            document
                .getElementById(
                    "fieldRequired"
                )
                ?.checked
        );

    const active =
        value(
            "fieldStatus"
        )
        ===
        "active";

    const error =
        document.getElementById(
            "customFieldError"
        );


    if (error) {
        error.textContent = "";
    }


    if (
        !entityType
        ||
        !label
        ||
        !key
        ||
        !fieldType
    ) {

        if (error) {

            error.textContent =
                "Đối tượng, nhãn, mã trường và kiểu dữ liệu là bắt buộc.";
        }

        return;
    }


    let options = [];


    if (
        fieldType ===
        "SELECT"
    ) {

        options =
            value(
                "fieldOptions"
            )
            .split(/\r?\n|,/)
            .map(
                item =>
                    item.trim()
            )
            .filter(Boolean);


        if (
            options.length === 0
        ) {

            if (error) {

                error.textContent =
                    "Trường Select phải có ít nhất một lựa chọn.";
            }

            return;
        }
    }


    const body = {
        entityType,
        key,
        label,
        fieldType,
        options,
        required,
        active
    };


    try {

        if (editingId) {

            await api(
                `/api/custom-fields/${editingId}`,
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
                "/api/custom-fields",
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

        setValue(
            "moduleFilter",
            entityType
        );

        await loadFields();

    } catch (err) {

        console.error(
            "Save custom field error:",
            err
        );

        if (error) {

            error.textContent =
                err.message
                ||
                "Không thể lưu custom field.";
        }
    }
}


function render() {

    const body =
        document.getElementById(
            "customFieldBody"
        );

    const empty =
        document.getElementById(
            "customFieldEmpty"
        );

    const status =
        value(
            "fieldStatusFilter"
        );


    if (!body) {
        return;
    }


    let filtered =
        [...fields];


    if (status) {

        const wantedActive =
            status ===
            "active";


        filtered =
            filtered.filter(
                field =>
                    Boolean(
                        field.active
                    )
                    ===
                    wantedActive
            );
    }


    body.innerHTML =
        "";


    if (empty) {

        empty.style.display =
            filtered.length
            ?
            "none"
            :
            "flex";
    }


    for (
        const field
        of filtered
    ) {

        const tr =
            document.createElement(
                "tr"
            );

        const state =
            field.active
            ?
            "active"
            :
            "inactive";


        tr.innerHTML = `
            <td>
                ${moduleLabel(field.entityType)}
            </td>

            <td>
                ${escapeHtml(field.label)}
            </td>

            <td class="field-key">
                ${escapeHtml(field.key)}
            </td>

            <td>
                <span class="field-type">
                    ${typeLabel(field.fieldType)}
                </span>
            </td>

            <td>
                ${field.required ? "Có" : "Không"}
            </td>

            <td>
                <span class="field-state ${state}">
                    ${
                        field.active
                        ?
                        "Đang bật"
                        :
                        "Đang tắt"
                    }
                </span>
            </td>

            <td>
                <button
                    type="button"
                    data-edit-field="${field.id}"
                >
                    Sửa
                </button>
            </td>
        `;


        body.appendChild(
            tr
        );
    }
}


function openDrawer(
    editingId = null
) {

    document
        .getElementById(
            "customFieldForm"
        )
        ?.reset();


    setValue(
        "editingFieldId",
        ""
    );


    const error =
        document.getElementById(
            "customFieldError"
        );

    if (error) {

        error.textContent = "";
    }


    setValue(
        "fieldModule",
        value("moduleFilter")
        ||
        "CUSTOMER"
    );

    setValue(
        "fieldType",
        "TEXT"
    );

    setValue(
        "fieldStatus",
        "active"
    );


    const required =
        document.getElementById(
            "fieldRequired"
        );

    if (required) {

        required.checked =
            false;
    }


    if (editingId !== null) {

        const field =
            fields.find(
                item =>
                    Number(item.id)
                    ===
                    Number(editingId)
            );


        if (field) {

            setValue(
                "editingFieldId",
                field.id
            );

            setValue(
                "fieldModule",
                field.entityType
            );

            setValue(
                "fieldLabel",
                field.label
            );

            setValue(
                "fieldKey",
                field.key
            );

            setValue(
                "fieldType",
                field.fieldType
            );

            setValue(
                "fieldOptions",
                Array.isArray(
                    field.options
                )
                ?
                field.options.join(
                    "\n"
                )
                :
                ""
            );

            setValue(
                "fieldStatus",
                field.active
                ?
                "active"
                :
                "inactive"
            );


            if (required) {

                required.checked =
                    Boolean(
                        field.required
                    );
            }


            const title =
                document.getElementById(
                    "customDrawerTitle"
                );

            if (title) {

                title.textContent =
                    "Sửa trường tùy chỉnh";
            }
        }

    } else {

        setValue(
            "fieldOptions",
            ""
        );

        const title =
            document.getElementById(
                "customDrawerTitle"
            );

        if (title) {

            title.textContent =
                "Thêm trường tùy chỉnh";
        }
    }


    toggleOptions();


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


function toggleOptions() {

    const wrap =
        document.getElementById(
            "fieldOptionsWrap"
        );


    if (!wrap) {
        return;
    }


    wrap.style.display =
        value(
            "fieldType"
        )
        ===
        "SELECT"
        ?
        ""
        :
        "none";
}


function moduleLabel(
    module
) {

    switch (
        String(module)
            .toUpperCase()
    ) {

        case "CUSTOMER":
            return "Khách hàng";

        case "OPPORTUNITY":
            return "Cơ hội";

        default:
            return escapeHtml(
                module
            );
    }
}


function typeLabel(
    type
) {

    switch (
        String(type)
            .toUpperCase()
    ) {

        case "TEXT":
            return "Text";

        case "NUMBER":
            return "Number";

        case "DATE":
            return "Date";

        case "SELECT":
            return "Select";

        default:
            return escapeHtml(
                type
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


toggleOptions();
loadFields();