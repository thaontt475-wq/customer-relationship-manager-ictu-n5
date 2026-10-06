"use strict";

const API_BASE =
    "http://localhost:8080/crm";

let products = [];

let searchTimer = null;


/* =========================================================
   DOM
========================================================= */

const body =
    document.getElementById(
        "productBody"
    );

const empty =
    document.getElementById(
        "productEmpty"
    );

const drawer =
    document.getElementById(
        "productDrawer"
    );

const overlay =
    document.getElementById(
        "productOverlay"
    );


/* =========================================================
   INIT
========================================================= */

document.addEventListener(
    "DOMContentLoaded",
    () => {

        bindEvents();

        loadProducts();
    }
);


/* =========================================================
   API
========================================================= */

async function api(
    path,
    options = {}
) {

    const config = {
        credentials:
            "include",

        headers: {
            "Accept":
                "application/json",

            ...(options.body
                ? {
                    "Content-Type":
                        "application/json"
                }
                : {})
        },

        ...options
    };


    const response =
        await fetch(
            API_BASE + path,
            config
        );


    let result;

    try {

        result =
            await response.json();

    } catch {

        throw new Error(
            `HTTP ${response.status}`
        );
    }


    if (
        response.status === 401
    ) {

        window.location.href =
            "login.html";

        throw new Error(
            "Phiên đăng nhập đã hết hạn"
        );
    }


    if (
        !response.ok ||
        !result.success
    ) {

        throw new Error(
            result.message ||
            `HTTP ${response.status}`
        );
    }


    return result.data;
}


/* =========================================================
   LOAD
========================================================= */

async function loadProducts() {

    try {

        const params =
            new URLSearchParams();


        const keyword =
            value(
                "productSearch"
            );


        const status =
            value(
                "productStatusFilter"
            );


        if (keyword) {

            params.set(
                "keyword",
                keyword
            );
        }


        if (
            status === "active"
        ) {

            params.set(
                "active",
                "true"
            );
        }


        if (
            status === "inactive" ||
            status === "discontinued"
        ) {

            params.set(
                "active",
                "false"
            );
        }


        const query =
            params.toString();


        const data =
            await api(
                "/api/products" +
                (
                    query
                        ? "?" + query
                        : ""
                )
            );


        products =
            Array.isArray(
                data?.items
            )
                ? data.items
                : [];


        render();


    } catch (error) {

        console.error(
            "Load products error:",
            error
        );


        products = [];

        render();


        if (empty) {

            empty.style.display =
                "flex";

            empty.textContent =
                error.message ||
                "Không thể tải sản phẩm.";
        }
    }
}


/* =========================================================
   EVENTS
========================================================= */

function bindEvents() {

    document
        .getElementById(
            "addProduct"
        )
        ?.addEventListener(
            "click",
            () => openDrawer()
        );


    document
        .getElementById(
            "closeProductDrawer"
        )
        ?.addEventListener(
            "click",
            closeDrawer
        );


    document
        .getElementById(
            "cancelProduct"
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
            "productSearch"
        )
        ?.addEventListener(
            "input",
            () => {

                clearTimeout(
                    searchTimer
                );


                searchTimer =
                    setTimeout(
                        loadProducts,
                        300
                    );
            }
        );


    document
        .getElementById(
            "productStatusFilter"
        )
        ?.addEventListener(
            "change",
            loadProducts
        );


    document
        .getElementById(
            "productTypeFilter"
        )
        ?.addEventListener(
            "change",
            render
        );


    document
        .getElementById(
            "productForm"
        )
        ?.addEventListener(
            "submit",
            async event => {

                event.preventDefault();

                await saveProductForm();
            }
        );


    document.addEventListener(
        "click",
        async event => {

            const edit =
                event.target.closest(
                    "[data-edit-product]"
                );


            if (edit) {

                openDrawer(
                    Number(
                        edit.dataset
                            .editProduct
                    )
                );

                return;
            }


            const discontinue =
                event.target.closest(
                    "[data-discontinue-product]"
                );


            if (discontinue) {

                const id =
                    Number(
                        discontinue.dataset
                            .discontinueProduct
                    );


                await discontinueProduct(
                    id
                );
            }
        }
    );
}


/* =========================================================
   SAVE
========================================================= */

async function saveProductForm() {

    const error =
        document.getElementById(
            "productError"
        );


    if (error) {

        error.textContent =
            "";
    }


    const code =
        value(
            "productCode"
        );

    const name =
        value(
            "productName"
        );


    if (
        !code ||
        !name
    ) {

        showError(
            "Mã sản phẩm và tên sản phẩm là bắt buộc."
        );

        return;
    }


    const listPrice =
        numberValue(
            "listPrice"
        );

    const floorPrice =
        numberValue(
            "floorPrice"
        );

    const costPrice =
        numberValue(
            "costPrice"
        );


    if (
        listPrice < 0 ||
        floorPrice < 0 ||
        costPrice < 0
    ) {

        showError(
            "Giá không được âm."
        );

        return;
    }


    if (
        floorPrice >
        listPrice
    ) {

        showError(
            "Giá sàn không được lớn hơn giá niêm yết."
        );

        return;
    }


    const status =
        value(
            "productStatus"
        );


    const payload = {

        code,

        name,

        type:
            value(
                "productType"
            ) || null,

        unit:
            value(
                "productUnit"
            ) || null,

        listPrice,

        floorPrice,

        costPrice,

        active:
            status !==
            "inactive" &&
            status !==
            "discontinued"
    };


    const editing =
        value(
            "editingProduct"
        );


    try {

        if (editing) {

            await api(
                `/api/products/${editing}`,
                {
                    method:
                        "PUT",

                    body:
                        JSON.stringify(
                            payload
                        )
                }
            );

        } else {

            await api(
                "/api/products",
                {
                    method:
                        "POST",

                    body:
                        JSON.stringify(
                            payload
                        )
                }
            );
        }


        closeDrawer();

        await loadProducts();


    } catch (error) {

        console.error(
            "Save product error:",
            error
        );


        showError(
            error.message
        );
    }
}


/* =========================================================
   DISCONTINUE
========================================================= */

async function discontinueProduct(
    id
) {

    const product =
        products.find(
            item =>
                Number(item.id)
                ===
                Number(id)
        );


    if (!product) {

        return;
    }


    const ok =
        window.confirm(
            `Ngừng kinh doanh sản phẩm "${product.name}"?`
        );


    if (!ok) {

        return;
    }


    try {

        await api(
            `/api/products/${id}`,
            {
                method:
                    "DELETE"
            }
        );


        await loadProducts();


    } catch (error) {

        console.error(
            "Discontinue product error:",
            error
        );


        alert(
            error.message ||
            "Không thể ngừng kinh doanh sản phẩm."
        );
    }
}


/* =========================================================
   DRAWER
========================================================= */

function openDrawer(
    id = null
) {

    document
        .getElementById(
            "productForm"
        )
        ?.reset();


    setValue(
        "editingProduct",
        ""
    );


    if (
        document.getElementById(
            "productError"
        )
    ) {

        document.getElementById(
            "productError"
        ).textContent =
            "";
    }


    if (id !== null) {

        const product =
            products.find(
                item =>
                    Number(item.id)
                    ===
                    Number(id)
            );


        if (product) {

            setValue(
                "editingProduct",
                product.id
            );

            setValue(
                "productCode",
                product.code
            );

            setValue(
                "productName",
                product.name
            );

            setValue(
                "productType",
                product.type || ""
            );

            setValue(
                "productUnit",
                product.unit || ""
            );

            setValue(
                "listPrice",
                product.listPrice ?? 0
            );

            setValue(
                "floorPrice",
                product.floorPrice ?? 0
            );

            setValue(
                "costPrice",
                product.costPrice ?? 0
            );

            setValue(
                "productStatus",
                product.active
                    ? "active"
                    : "inactive"
            );
        }

    } else {

        setValue(
            "productStatus",
            "active"
        );
    }


    drawer
        ?.classList
        .add(
            "open"
        );

    overlay
        ?.classList
        .add(
            "show"
        );

    document.body
        .classList
        .add(
            "drawer-open"
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
            "show"
        );

    document.body
        .classList
        .remove(
            "drawer-open"
        );
}


/* =========================================================
   RENDER
========================================================= */

function render() {

    if (!body) {

        return;
    }


    const type =
        value(
            "productTypeFilter"
        );


    const filtered =
        products.filter(
            product => {

                if (
                    type &&
                    product.type !==
                    type
                ) {

                    return false;
                }


                return true;
            }
        );


    body.innerHTML =
        "";


    if (empty) {

        empty.style.display =
            filtered.length
                ? "none"
                : "flex";
    }


    for (
        const product
        of filtered
    ) {

        const tr =
            document.createElement(
                "tr"
            );


        const status =
            product.active
                ? "active"
                : "inactive";


        tr.innerHTML = `

            <td class="product-code">
                ${escapeHtml(product.code)}
            </td>

            <td>
                ${escapeHtml(product.name)}
            </td>

            <td>
                ${
                    product.type ===
                    "subscription"

                    ? "Dịch vụ thuê bao"

                    : "Sản phẩm một lần"
                }
            </td>

            <td>
                ${escapeHtml(
                    product.unit ||
                    "—"
                )}
            </td>

            <td>
                ${money(
                    product.listPrice
                )}
            </td>

            <td>
                ${money(
                    product.floorPrice
                )}
            </td>

            <td>
                ${
                    product.costPrice ===
                    undefined

                    ? "Không có quyền xem"

                    : money(
                        product.costPrice
                    )
                }
            </td>

            <td>

                <span
                    class="product-status ${status}"
                >
                    ${
                        product.active
                            ? "Đang bán"
                            : "Ngừng kinh doanh"
                    }
                </span>

            </td>

            <td>

                <button
                    type="button"
                    class="product-action"
                    data-edit-product="${product.id}"
                >
                    Sửa
                </button>

                ${
                    product.active

                    ? `
                    <button
                        type="button"
                        class="product-action"
                        data-discontinue-product="${product.id}"
                    >
                        Ngừng bán
                    </button>
                    `

                    : ""
                }

            </td>
        `;


        body.appendChild(
            tr
        );
    }
}


/* =========================================================
   HELPERS
========================================================= */

function showError(
    message
) {

    const error =
        document.getElementById(
            "productError"
        );


    if (error) {

        error.textContent =
            message || "";
    }
}


function value(
    id
) {

    const element =
        document.getElementById(
            id
        );


    return (
        element?.value ??
        ""
    )
        .toString()
        .trim();
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


function numberValue(
    id
) {

    const raw =
        value(id);


    if (!raw) {

        return 0;
    }


    const number =
        Number(raw);


    return Number.isFinite(
        number
    )
        ? number
        : 0;
}


function money(
    value
) {

    const number =
        Number(
            value ?? 0
        );


    return new Intl.NumberFormat(
        "vi-VN",
        {
            style:
                "currency",

            currency:
                "VND",

            maximumFractionDigits:
                0
        }
    )
        .format(number);
}


function escapeHtml(
    value
) {

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