"use strict";

const API_BASE = "http://localhost:8080/crm";

let organizations = [];
let selectedId = null;

const tree =
    document.getElementById(
        "organizationTree"
    );

const empty =
    document.getElementById(
        "organizationEmpty"
    );

const drawer =
    document.getElementById(
        "orgDrawer"
    );

const overlay =
    document.getElementById(
        "orgOverlay"
    );


document
    .getElementById(
        "addRootOrg"
    )
    ?.addEventListener(
        "click",
        () => openDrawer(
            null,
            null
        )
    );


document
    .getElementById(
        "addChildOrg"
    )
    ?.addEventListener(
        "click",
        () => {

            if (!selectedId) {
                return;
            }

            openDrawer(
                null,
                selectedId
            );
        }
    );


document
    .getElementById(
        "editOrg"
    )
    ?.addEventListener(
        "click",
        () => {

            if (!selectedId) {
                return;
            }

            openDrawer(
                selectedId,
                null
            );
        }
    );


document
    .getElementById(
        "deleteOrg"
    )
    ?.addEventListener(
        "click",
        async () => {

            if (!selectedId) {
                return;
            }

            const item =
                organizations.find(
                    org =>
                        Number(org.id)
                        ===
                        Number(selectedId)
                );

            if (!item) {
                return;
            }

            const hasChildren =
                organizations.some(
                    org =>
                        Number(org.parentId)
                        ===
                        Number(selectedId)
                );

            if (hasChildren) {

                alert(
                    "Không thể ngừng hoạt động đơn vị đang có đơn vị con."
                );

                return;
            }

            if (
                !confirm(
                    "Ngừng hoạt động đơn vị này?"
                )
            ) {
                return;
            }

            try {

                await api(
                    `/api/organization/units/${item.id}`,
                    {
                        method: "PUT",
                        body: JSON.stringify({
                            code: item.code,
                            name: item.name,
                            type:
                                item.type
                                ||
                                "department",
                            parentId:
                                item.parentId
                                ??
                                null,
                            managerId:
                                item.managerId
                                ??
                                null,
                            description:
                                item.description
                                ||
                                "",
                            active: false
                        })
                    }
                );

                selectedId = null;

                await loadOrganizations();

            } catch (error) {

                alert(
                    error.message
                    ||
                    "Không thể cập nhật đơn vị."
                );
            }
        }
    );


document
    .getElementById(
        "closeOrgDrawer"
    )
    ?.addEventListener(
        "click",
        closeDrawer
    );


document
    .getElementById(
        "cancelOrg"
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
        "orgForm"
    )
    ?.addEventListener(
        "submit",
        async event => {

            event.preventDefault();

            const error =
                document.getElementById(
                    "orgFormError"
                );

            error.textContent = "";

            const code =
                value(
                    "orgCode"
                );

            const name =
                value(
                    "orgName"
                );

            if (
                !code
                ||
                !name
            ) {

                error.textContent =
                    "Mã đơn vị và tên đơn vị là bắt buộc.";

                return;
            }

            const editingId =
                value(
                    "editingOrgId"
                );

            const parentIdValue =
                value(
                    "parentOrgId"
                );

            const managerValue =
                value(
                    "orgManager"
                );

            const existing =
                organizations.find(
                    item =>
                        Number(item.id)
                        ===
                        Number(editingId)
                );

            const body = {
                code,
                name,
                type:
                    value(
                        "orgType"
                    )
                    ||
                    "department",
                parentId:
                    parentIdValue
                    ?
                    Number(parentIdValue)
                    :
                    null,
                managerId:
                    parseManagerId(
                        managerValue
                    ),
                description:
                    value(
                        "orgDescription"
                    ),
                active:
                    existing
                    ?
                    existing.active !== false
                    :
                    true
            };

            try {

                let result;

                if (editingId) {

                    result =
                        await api(
                            `/api/organization/units/${editingId}`,
                            {
                                method: "PUT",
                                body:
                                    JSON.stringify(
                                        body
                                    )
                            }
                        );

                } else {

                    result =
                        await api(
                            "/api/organization/units",
                            {
                                method: "POST",
                                body:
                                    JSON.stringify(
                                        body
                                    )
                            }
                        );
                }

                selectedId =
                    Number(
                        result.data.id
                    );

                closeDrawer();

                await loadOrganizations();

            } catch (apiError) {

                error.textContent =
                    apiError.message
                    ||
                    "Không thể lưu đơn vị.";
            }
        }
    );


tree
    ?.addEventListener(
        "click",
        event => {

            const expand =
                event.target.closest(
                    "[data-expand-org]"
                );

            if (expand) {

                event.stopPropagation();

                const id =
                    Number(
                        expand.dataset.expandOrg
                    );

                const item =
                    organizations.find(
                        org =>
                            Number(org.id)
                            ===
                            id
                    );

                if (item) {

                    item.expanded =
                        item.expanded
                        === false;
                }

                render();

                return;
            }

            const row =
                event.target.closest(
                    "[data-select-org]"
                );

            if (row) {

                selectedId =
                    Number(
                        row.dataset.selectOrg
                    );

                render();
                renderDetail();
            }
        }
    );


async function loadOrganizations() {

    try {

        const result =
            await api(
                "/api/organization/units"
            );

        const oldExpanded =
            new Map(
                organizations.map(
                    item => [
                        Number(item.id),
                        item.expanded
                    ]
                )
            );

        organizations =
            Array.isArray(result.data)
            ?
            result.data.map(
                item => ({
                    ...item,
                    id:
                        Number(item.id),
                    parentId:
                        item.parentId == null
                        ?
                        null
                        :
                        Number(item.parentId),
                    managerId:
                        item.managerId == null
                        ?
                        null
                        :
                        Number(item.managerId),
                    expanded:
                        oldExpanded.get(
                            Number(item.id)
                        )
                        ??
                        true
                })
            )
            :
            [];

        if (
            selectedId
            &&
            !organizations.some(
                item =>
                    Number(item.id)
                    ===
                    Number(selectedId)
            )
        ) {
            selectedId = null;
        }

        render();
        renderDetail();

    } catch (error) {

        console.error(
            "Load organization error:",
            error
        );

        if (empty) {
            empty.style.display =
                "flex";

            empty.textContent =
                error.message
                ||
                "Không tải được cơ cấu tổ chức.";
        }
    }
}


function render() {

    if (!tree) {
        return;
    }

    tree.innerHTML = "";

    const roots =
        organizations.filter(
            item =>
                item.parentId == null
        );

    if (empty) {

        empty.style.display =
            roots.length
            ?
            "none"
            :
            "flex";
    }

    for (
        const root
        of roots
    ) {

        tree.appendChild(
            createNode(
                root
            )
        );
    }
}


function createNode(item) {

    const wrapper =
        document.createElement(
            "div"
        );

    wrapper.className =
        "org-tree-node";

    const children =
        organizations.filter(
            child =>
                Number(child.parentId)
                ===
                Number(item.id)
        );

    const row =
        document.createElement(
            "div"
        );

    row.className =
        "org-node-row"
        +
        (
            Number(selectedId)
            ===
            Number(item.id)
            ?
            " selected"
            :
            ""
        );

    row.dataset.selectOrg =
        item.id;

    row.innerHTML = `
        <button
            class="org-expand"
            type="button"
            data-expand-org="${item.id}"
            ${children.length ? "" : "disabled"}
        >
            ${
                children.length
                ?
                (
                    item.expanded === false
                    ?
                    "›"
                    :
                    "⌄"
                )
                :
                ""
            }
        </button>

        <div class="org-node-icon">
            ${typeIcon(item.type)}
        </div>

        <div class="org-node-copy">
            <strong>
                ${escapeHtml(item.name)}
            </strong>

            <span>
                ${escapeHtml(item.code || "—")}
                ·
                ${typeLabel(item.type)}
                ${
                    item.active === false
                    ?
                    " · Ngừng hoạt động"
                    :
                    ""
                }
            </span>
        </div>
    `;

    wrapper.appendChild(
        row
    );

    if (
        children.length
        &&
        item.expanded !== false
    ) {

        const childrenBox =
            document.createElement(
                "div"
            );

        childrenBox.className =
            "org-children";

        for (
            const child
            of children
        ) {

            childrenBox.appendChild(
                createNode(
                    child
                )
            );
        }

        wrapper.appendChild(
            childrenBox
        );
    }

    return wrapper;
}


function renderDetail() {

    const item =
        organizations.find(
            org =>
                Number(org.id)
                ===
                Number(selectedId)
        );

    const emptyDetail =
        document.getElementById(
            "orgDetailEmpty"
        );

    const detail =
        document.getElementById(
            "orgDetail"
        );

    if (emptyDetail) {
        emptyDetail.hidden =
            Boolean(item);
    }

    if (detail) {
        detail.hidden =
            !item;
    }

    if (!item) {
        return;
    }

    text(
        "detailName",
        item.name
    );

    text(
        "detailCode",
        item.code
        ||
        "—"
    );

    text(
        "detailManager",
        item.manager
        ||
        "—"
    );

    text(
        "detailDescription",
        item.description
        ||
        "—"
    );

    const parent =
        organizations.find(
            org =>
                Number(org.id)
                ===
                Number(item.parentId)
        );

    text(
        "detailParent",
        parent
        ?
        parent.name
        :
        "Đơn vị gốc"
    );

    text(
        "detailType",
        typeLabel(
            item.type
        )
    );
}


function openDrawer(
    editingId = null,
    parentId = null
) {

    document
        .getElementById(
            "orgForm"
        )
        ?.reset();

    setValue(
        "editingOrgId",
        ""
    );

    setValue(
        "parentOrgId",
        parentId
        ??
        ""
    );

    const error =
        document.getElementById(
            "orgFormError"
        );

    if (error) {
        error.textContent = "";
    }

    const title =
        document.getElementById(
            "orgDrawerTitle"
        );

    if (title) {

        title.textContent =
            parentId
            ?
            "Thêm đơn vị con"
            :
            "Thêm đơn vị";
    }

    if (editingId !== null) {

        const item =
            organizations.find(
                org =>
                    Number(org.id)
                    ===
                    Number(editingId)
            );

        if (item) {

            setValue(
                "editingOrgId",
                item.id
            );

            setValue(
                "parentOrgId",
                item.parentId
                ??
                ""
            );

            setValue(
                "orgCode",
                item.code
                ||
                ""
            );

            setValue(
                "orgName",
                item.name
                ||
                ""
            );

            setValue(
                "orgType",
                item.type
                ||
                "department"
            );

            setValue(
                "orgManager",
                item.managerId
                ??
                ""
            );

            setValue(
                "orgDescription",
                item.description
                ||
                ""
            );

            if (title) {
                title.textContent =
                    "Sửa đơn vị";
            }
        }
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

    let json = null;

    try {
        json =
            await response.json();
    } catch {
        json = null;
    }

    if (!response.ok) {

        throw new Error(
            json?.message
            ||
            `HTTP ${response.status}`
        );
    }

    return json;
}


function parseManagerId(
    value
) {

    if (!value) {
        return null;
    }

    const number =
        Number(value);

    return Number.isInteger(number)
        &&
        number > 0
        ?
        number
        :
        null;
}


function typeLabel(type) {

    switch (type) {

        case "sales-team":
            return "Nhóm kinh doanh";

        case "division":
            return "Khối / Bộ phận";

        default:
            return "Phòng ban";
    }
}


function typeIcon(type) {

    switch (type) {

        case "sales-team":
            return "♟";

        case "division":
            return "◦";

        default:
            return "⌂";
    }
}


function value(id) {

    const element =
        document.getElementById(id);

    return element
        ?
        element.value.trim()
        :
        "";
}


function setValue(
    id,
    newValue
) {

    const element =
        document.getElementById(id);

    if (element) {
        element.value =
            newValue
            ??
            "";
    }
}


function text(
    id,
    newValue
) {

    const element =
        document.getElementById(id);

    if (element) {
        element.textContent =
            newValue;
    }
}


function escapeHtml(value) {

    return String(
        value
        ??
        ""
    )
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}


loadOrganizations();