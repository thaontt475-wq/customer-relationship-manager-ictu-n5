"use strict";

const API_BASE =
    `${window.location.pathname.startsWith("/crm/") ? window.location.origin : window.location.protocol + "//" + (window.location.hostname || "localhost") + ":8080"}/crm`;

let products = [];

let searchTimer = null;


function normalizeProductType(val) {
    if (!val) return "one-time";
    const s = String(val).toLowerCase().replace(/_/g, "-");
    return s.includes("sub") ? "subscription" : "one-time";
}

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

const discontinueModal =
    document.getElementById(
        "discontinueModal"
    );

const discontinueOverlay =
    document.getElementById(
        "discontinueOverlay"
    );

const activateModal =
    document.getElementById(
        "activateModal"
    );

const activateOverlay =
    document.getElementById(
        "activateOverlay"
    );

let pendingDiscontinueId = null;
let pendingActivateId = null;
let globalAllProducts = [];


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

        // Synchronize status tabs active state
        const curStatus = status || "";
        document.querySelectorAll(".status-tab-btn").forEach(btn => {
            if (btn.dataset.statusTab === curStatus) {
                btn.classList.add("active");
            } else {
                btn.classList.remove("active");
            }
        });

        // Fetch or cache globalAllProducts for accurate counters
        if (!status && !keyword) {
            globalAllProducts = products;
        } else if (!globalAllProducts.length) {
            api("/api/products")
                .then(res => {
                    if (Array.isArray(res?.items)) {
                        globalAllProducts = res.items;
                        updateKpiMetrics();
                    }
                })
                .catch(() => {});
        }


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


    document
        .getElementById(
            "productCode"
        )
        ?.addEventListener(
            "input",
            event => {
                const enteredCode = event.target.value.trim().toUpperCase();
                const curEdit = value("editingProduct");
                if (!curEdit && enteredCode) {
                    const matched = products.find(
                        p => String(p.code || "").trim().toUpperCase() === enteredCode
                    );
                    if (matched) {
                        setValue("editingProduct", matched.id);
                        setValue("productName", matched.name);
                        setValue("productType", normalizeProductType(matched.type));
                        setValue("productUnit", matched.unit || "");
                        setValue("listPrice", matched.listPrice ?? 0);
                        setValue("floorPrice", matched.floorPrice ?? 0);
                        setValue("costPrice", matched.costPrice ?? 0);
                        setValue("productStatus", matched.active ? "active" : "inactive");

                        const titleEl = document.getElementById("productDrawerTitle");
                        const submitBtn = document.querySelector("#productForm button[type='submit']");
                        if (titleEl) titleEl.textContent = "Sửa sản phẩm: " + matched.code;
                        if (submitBtn) submitBtn.textContent = "Cập nhật";
                    }
                }
            }
        );

    // Status tabs click event
    document.querySelectorAll(".status-tab-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".status-tab-btn").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            const tabStatus = btn.dataset.statusTab || "";
            const statusFilter = document.getElementById("productStatusFilter");
            if (statusFilter) {
                statusFilter.value = tabStatus;
            }
            loadProducts();
        });
    });

    // Discontinue modal controls
    document.getElementById("closeDiscontinueModal")?.addEventListener("click", closeDiscontinueModal);
    document.getElementById("cancelDiscontinueBtn")?.addEventListener("click", closeDiscontinueModal);
    document.getElementById("confirmDiscontinueBtn")?.addEventListener("click", confirmDiscontinue);
    discontinueOverlay?.addEventListener("click", closeDiscontinueModal);

    // Reactivate modal controls
    document.getElementById("closeActivateModal")?.addEventListener("click", closeActivateModal);
    document.getElementById("cancelActivateBtn")?.addEventListener("click", closeActivateModal);
    document.getElementById("confirmActivateBtn")?.addEventListener("click", confirmActivate);
    activateOverlay?.addEventListener("click", closeActivateModal);

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
                openDiscontinueModal(id);
                return;
            }

            const activate =
                event.target.closest(
                    "[data-activate-product]"
                );

            if (activate) {
                const id =
                    Number(
                        activate.dataset
                            .activateProduct
                    );
                openActivateModal(id);
                return;
            }

            // Clicking anywhere on a table row opens the edit drawer
            const tr = event.target.closest("tr[data-product-id]");
            if (tr && !event.target.closest("button") && !event.target.closest("a")) {
                openDrawer(Number(tr.dataset.productId));
                return;
            }
        }
    );

    // ESC key closes any open modal
    document.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            if (drawer?.classList.contains("open")) closeDrawer();
            if (discontinueModal?.classList.contains("open")) closeDiscontinueModal();
            if (activateModal?.classList.contains("open")) closeActivateModal();
        }
    });
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


    const rawType = value("productType");

    const payload = {
        code,
        name,
        type: rawType === "subscription" ? "SUBSCRIPTION" : "ONE_TIME",
        unit: value("productUnit") || null,
        listPrice,
        floorPrice,
        costPrice,
        active: status !== "inactive" && status !== "discontinued"
    };

    let editing = value("editingProduct");

    // If editing is not set, but the entered code matches an existing product:
    if (!editing && code) {
        const existing = products.find(
            p => String(p.code || "").trim().toUpperCase() === code.trim().toUpperCase()
        );
        if (existing) {
            editing = String(existing.id);
            setValue("editingProduct", existing.id);
        }
    }

    try {
        if (editing) {
            await api(
                `/api/products/${editing}`,
                {
                    method: "PUT",
                    body: JSON.stringify(payload)
                }
            );
        } else {
            await api(
                "/api/products",
                {
                    method: "POST",
                    body: JSON.stringify(payload)
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
   DISCONTINUE (NGỪNG KINH DOANH) MODAL
========================================================= */

function openDiscontinueModal(id) {
    const product = products.find(item => Number(item.id) === Number(id))
        || globalAllProducts.find(item => Number(item.id) === Number(id));

    if (!product) {
        return;
    }

    pendingDiscontinueId = product.id;

    const elCode = document.getElementById("discontinueProductCode");
    const elName = document.getElementById("discontinueProductName");
    const elUnit = document.getElementById("discontinueProductUnit");
    const elListPrice = document.getElementById("discontinueProductListPrice");
    const elFloorPrice = document.getElementById("discontinueProductFloorPrice");

    if (elCode) elCode.textContent = product.code;
    if (elName) elName.textContent = product.name;
    if (elUnit) elUnit.textContent = product.unit || "—";
    if (elListPrice) elListPrice.textContent = money(product.listPrice);
    if (elFloorPrice) elFloorPrice.textContent = money(product.floorPrice);

    discontinueModal?.classList.add("open");
    discontinueOverlay?.classList.add("open");
    discontinueOverlay?.classList.add("show");
    document.body.classList.add("modal-open");
}


function closeDiscontinueModal() {
    pendingDiscontinueId = null;
    discontinueModal?.classList.remove("open");
    discontinueOverlay?.classList.remove("open");
    discontinueOverlay?.classList.remove("show");
    document.body.classList.remove("modal-open");
}


async function confirmDiscontinue() {
    if (!pendingDiscontinueId) {
        return;
    }

    const btn = document.getElementById("confirmDiscontinueBtn");

    try {
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> <span>Đang xử lý...</span>';
        }

        await api(
            `/api/products/${pendingDiscontinueId}`,
            {
                method: "DELETE"
            }
        );

        closeDiscontinueModal();
        globalAllProducts = [];
        await loadProducts();

    } catch (error) {
        console.error("Discontinue product error:", error);
        alert(error.message || "Không thể ngừng kinh doanh sản phẩm.");
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<i class="fa-solid fa-power-off"></i> <span>Xác nhận ngừng bán</span>';
        }
    }
}


/* =========================================================
   REACTIVATE (MỞ BÁN LẠI) MODAL
========================================================= */

function openActivateModal(id) {
    const product = products.find(item => Number(item.id) === Number(id))
        || globalAllProducts.find(item => Number(item.id) === Number(id));

    if (!product) {
        return;
    }

    pendingActivateId = product.id;

    const elCode = document.getElementById("activateProductCode");
    const elName = document.getElementById("activateProductName");
    const elUnit = document.getElementById("activateProductUnit");
    const elListPrice = document.getElementById("activateProductListPrice");

    if (elCode) elCode.textContent = product.code;
    if (elName) elName.textContent = product.name;
    if (elUnit) elUnit.textContent = product.unit || "—";
    if (elListPrice) elListPrice.textContent = money(product.listPrice);

    activateModal?.classList.add("open");
    activateOverlay?.classList.add("open");
    activateOverlay?.classList.add("show");
    document.body.classList.add("modal-open");
}


function closeActivateModal() {
    pendingActivateId = null;
    activateModal?.classList.remove("open");
    activateOverlay?.classList.remove("open");
    activateOverlay?.classList.remove("show");
    document.body.classList.remove("modal-open");
}


async function confirmActivate() {
    if (!pendingActivateId) {
        return;
    }

    const product = products.find(item => Number(item.id) === Number(pendingActivateId))
        || globalAllProducts.find(item => Number(item.id) === Number(pendingActivateId));

    if (!product) {
        return;
    }

    const btn = document.getElementById("confirmActivateBtn");

    try {
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> <span>Đang kích hoạt...</span>';
        }

        const payload = {
            code: product.code,
            name: product.name,
            type: normalizeProductType(product.type) === "subscription" ? "SUBSCRIPTION" : "ONE_TIME",
            unit: product.unit || null,
            listPrice: Number(product.listPrice) || 0,
            floorPrice: Number(product.floorPrice) || 0,
            costPrice: Number(product.costPrice) || 0,
            active: true
        };

        await api(
            `/api/products/${product.id}`,
            {
                method: "PUT",
                body: JSON.stringify(payload)
            }
        );

        closeActivateModal();
        globalAllProducts = [];
        await loadProducts();

    } catch (error) {
        console.error("Reactivate product error:", error);
        alert(error.message || "Không thể mở bán lại sản phẩm.");
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<i class="fa-solid fa-rotate-left"></i> <span>Mở bán lại ngay</span>';
        }
    }
}


/* =========================================================
   DRAWER / MODAL
========================================================= */

function openDrawer(
    id = null
) {
    document.getElementById("productForm")?.reset();
    setValue("editingProduct", "");

    const titleEl = document.getElementById("productDrawerTitle");
    const submitBtn = document.querySelector("#productForm button[type='submit']");

    if (document.getElementById("productError")) {
        document.getElementById("productError").textContent = "";
    }

    if (id !== null) {
        const product = products.find(item => Number(item.id) === Number(id))
            || globalAllProducts.find(item => Number(item.id) === Number(id));

        if (product) {
            setValue("editingProduct", product.id);
            setValue("productCode", product.code);
            setValue("productName", product.name);
            setValue("productType", normalizeProductType(product.type));
            setValue("productUnit", product.unit || "");
            setValue("listPrice", product.listPrice ?? 0);
            setValue("floorPrice", product.floorPrice ?? 0);
            setValue("costPrice", product.costPrice ?? 0);
            setValue("productStatus", product.active ? "active" : "inactive");

            if (titleEl) titleEl.textContent = "Sửa sản phẩm: " + product.code;
            if (submitBtn) {
                submitBtn.innerHTML = '<i class="fa-solid fa-check"></i> <span>Cập nhật</span>';
            }
        }
    } else {
        if (titleEl) titleEl.textContent = "Thêm sản phẩm mới";
        if (submitBtn) {
            submitBtn.innerHTML = '<i class="fa-solid fa-check"></i> <span>Lưu thông tin</span>';
        }
        setValue("productStatus", "active");
        setValue("productType", "one-time");
    }

    drawer?.classList.add("open");
    overlay?.classList.add("open");
    overlay?.classList.add("show");
    document.body.classList.add("modal-open");
    document.body.classList.add("drawer-open");

    setTimeout(() => {
        const codeInput = document.getElementById("productCode");
        if (codeInput && !id) codeInput.focus();
    }, 120);
}


function closeDrawer() {
    drawer?.classList.remove("open");
    overlay?.classList.remove("open");
    overlay?.classList.remove("show");
    document.body.classList.remove("modal-open");
    document.body.classList.remove("drawer-open");
}


/* =========================================================
   RENDER & KPI METRICS
========================================================= */

function updateKpiMetrics() {
    const dataSource = globalAllProducts.length ? globalAllProducts : products;

    const totalCount = dataSource.length;
    const activeCount = dataSource.filter(p => p.active).length;
    const inactiveCount = dataSource.filter(p => !p.active).length;
    const subCount = dataSource.filter(p => normalizeProductType(p.type) === "subscription").length;
    const sumListPrice = dataSource.reduce((acc, p) => acc + (Number(p.listPrice) || 0), 0);
    const avgListPrice = totalCount > 0 ? (sumListPrice / totalCount) : 0;
    const activeRate = totalCount > 0 ? Math.round((activeCount / totalCount) * 100) : 0;

    const elTotal = document.getElementById("kpiTotalProducts");
    const elActive = document.getElementById("kpiActiveProducts");
    const elActiveRate = document.getElementById("kpiActiveRate");
    const elAvgPrice = document.getElementById("kpiAvgListPrice");
    const elSub = document.getElementById("kpiSubscriptionCount");

    if (elTotal) elTotal.textContent = totalCount.toLocaleString("vi-VN");
    if (elActive) elActive.textContent = activeCount.toLocaleString("vi-VN");
    if (elActiveRate) elActiveRate.textContent = `${activeRate}% danh mục sẵn sàng bán (${inactiveCount} ngừng bán)`;
    if (elAvgPrice) elAvgPrice.textContent = money(avgListPrice);
    if (elSub) elSub.textContent = `${subCount.toLocaleString("vi-VN")} dịch vụ`;

    // Status Tab count badges
    const tabAll = document.getElementById("tabCountAll");
    const tabAct = document.getElementById("tabCountActive");
    const tabInact = document.getElementById("tabCountInactive");
    if (tabAll) tabAll.textContent = totalCount;
    if (tabAct) tabAct.textContent = activeCount;
    if (tabInact) tabInact.textContent = inactiveCount;
}


function render() {
    // Update KPI metrics on top cards & status tabs
    updateKpiMetrics();

    if (!body) {
        return;
    }

    const type = value("productTypeFilter");

    const filtered = products.filter(product => {
        if (type && normalizeProductType(product.type) !== type) {
            return false;
        }
        return true;
    });

    body.innerHTML = "";

    if (empty) {
        if (!filtered.length) {
            empty.style.display = "flex";
            const curStatus = value("productStatusFilter");
            const strongEl = empty.querySelector("strong");
            const spanEl = empty.querySelector("span");

            if (curStatus === "inactive") {
                if (strongEl) strongEl.textContent = "Không có sản phẩm nào ngừng bán";
                if (spanEl) spanEl.textContent = "Hiện tại toàn bộ sản phẩm trong danh mục đều đang hoạt động kinh doanh.";
            } else {
                if (strongEl) strongEl.textContent = "Chưa có sản phẩm phù hợp";
                if (spanEl) spanEl.textContent = "Không tìm thấy sản phẩm nào theo bộ lọc hiện tại. Bấm \"Thêm sản phẩm\" để khởi tạo.";
            }
        } else {
            empty.style.display = "none";
        }
    }

    for (const product of filtered) {
        const tr = document.createElement("tr");
        tr.className = "product-row-clickable";
        tr.dataset.productId = product.id;
        tr.title = "Bấm để sửa sản phẩm: " + product.code;

        const status = product.active ? "active" : "inactive";
        const isSub = normalizeProductType(product.type) === "subscription";

        tr.innerHTML = `
            <td>
                <span class="product-code-badge">
                    <i class="fa-solid fa-cube"></i>
                    ${escapeHtml(product.code)}
                </span>
            </td>
            <td>
                <div class="product-name-cell">
                    ${escapeHtml(product.name)}
                </div>
            </td>
            <td>
                <span class="product-type-badge ${isSub ? "subscription" : "one-time"}">
                    <i class="${isSub ? "fa-solid fa-arrows-rotate" : "fa-solid fa-box"}"></i>
                    ${isSub ? "Thuê bao" : "Một lần"}
                </span>
            </td>

            <td>
                <span class="product-unit-tag">
                    ${escapeHtml(product.unit || "—")}
                </span>
            </td>

            <td style="text-align: right;">
                <span class="price-list">${money(product.listPrice)}</span>
            </td>

            <td style="text-align: right;">
                <span class="price-floor">${money(product.floorPrice)}</span>
            </td>

            <td style="text-align: right;">
                ${
                    product.costPrice === undefined
                        ? `<span class="price-restricted"><i class="fa-solid fa-lock"></i> Giới hạn RBAC</span>`
                        : `<span class="price-cost">${money(product.costPrice)}</span>`
                }
            </td>

            <td style="text-align: center;">
                <span class="status-pill ${status}">
                    <span class="status-dot"></span>
                    ${product.active ? "Đang kinh doanh" : "Ngừng bán"}
                </span>
            </td>

            <td style="text-align: right;">
                <div class="action-btn-group">
                    <button
                        type="button"
                        class="action-btn btn-edit"
                        data-edit-product="${product.id}"
                        title="Chỉnh sửa sản phẩm"
                    >
                        <i class="fa-solid fa-pen-to-square"></i>
                        <span>Sửa</span>
                    </button>

                    ${
                        product.active
                            ? `
                            <button
                                type="button"
                                class="action-btn btn-discontinue"
                                data-discontinue-product="${product.id}"
                                title="Chuyển sang trạng thái ngừng kinh doanh"
                            >
                                <i class="fa-solid fa-power-off"></i>
                                <span>Ngừng bán</span>
                            </button>
                            `
                            : `
                            <button
                                type="button"
                                class="action-btn btn-activate"
                                data-activate-product="${product.id}"
                                title="Kích hoạt mở bán lại sản phẩm này"
                            >
                                <i class="fa-solid fa-rotate-left"></i>
                                <span>Mở bán lại</span>
                            </button>
                            `
                    }
                </div>
            </td>
        `;

        body.appendChild(tr);
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