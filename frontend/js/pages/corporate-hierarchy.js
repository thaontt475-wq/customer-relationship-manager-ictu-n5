/**
 * ===================================================================
 * ENTERPRISE CORPORATE GROUP & HIERARCHY CONTROLLER (CRM-65 / S3-05)
 * Pure HTML5 / CSS3 / ES6+ JavaScript client-side module.
 * - Khai báo quan hệ Công ty Mẹ - Con (Parent-Child Association)
 * - Khóa ngoại chuẩn hóa: parent_customer_id
 * - Chặn tự gán chính mình (No Self-parent) & Chặn vòng lặp đệ quy (No Circular dependency)
 * - Thẻ KPI Tổng giá trị Hợp đồng Tập đoàn (Consolidated Group Revenue)
 * - Bảng phân rã doanh thu & Tỷ lệ đóng góp (%)
 * - Sơ đồ cây phân cấp tập đoàn trực quan (Expand/Collapse Hierarchy Tree)
 * - Chuẩn hóa Fetch API với In-Memory Mock Data dự phòng (Zero LocalStorage dependency)
 * ===================================================================
 */

"use strict";

(function () {
    const API_BASE = `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;

    // Default Seed Hierarchy Relationships (Mapping: childId -> parentId)
    const DEFAULT_HIERARCHY_MAPPING = {
        2: 1, // VNG is child of FPT
        3: 1, // MISA is child of FPT
        5: 1, // MWG is child of FPT
        7: 4  // Imexpharm is child of VNPT
    };

    // Default Mock Contract Values for Enterprises (VND)
    const DEFAULT_MOCK_CONTRACTS = {};
    const _UNUSED_DEFAULT_MOCK_CONTRACTS = {
        1: [
            { id: 101, code: "HD-FPT-01", name: "Hệ thống ERP Doanh nghiệp Toàn diện", amount: 4500000000, status: "ACTIVE", signedDate: "2026-01-20" }
        ],
        2: [
            { id: 102, code: "HD-VNG-01", name: "Hạ tầng Cloud Server & Datacenter", amount: 2200000000, status: "ACTIVE", signedDate: "2026-02-15" }
        ],
        3: [
            { id: 103, code: "HD-MISA-01", name: "Phần mềm Kế toán Tài chính Đa chi nhánh", amount: 1300000000, status: "ACTIVE", signedDate: "2026-03-05" }
        ],
        4: [
            { id: 104, code: "HD-VNPT-01", name: "Đường truyền Kênh thuê riêng & Bảo mật", amount: 5800000000, status: "ACTIVE", signedDate: "2026-01-18" }
        ],
        5: [
            { id: 105, code: "HD-MWG-01", name: "Nền tảng Bán lẻ Đa kênh POS Omni-channel", amount: 3500000000, status: "ACTIVE", signedDate: "2026-03-10" }
        ],
        6: [
            { id: 106, code: "HD-MB-01", name: "Cổng thanh toán & Tích hợp Ngân hàng Mở", amount: 6200000000, status: "ACTIVE", signedDate: "2026-02-28" }
        ],
        7: [
            { id: 107, code: "HD-IMEX-01", name: "Phần mềm Truy xuất Nguồn gốc Dược phẩm", amount: 1200000000, status: "ACTIVE", signedDate: "2026-03-14" }
        ],
        8: [
            { id: 108, code: "HD-HP-01", name: "Hệ thống Quản trị Sản xuất Thép MES", amount: 8500000000, status: "ACTIVE", signedDate: "2026-02-05" }
        ]
    };

    // Standard In-Memory Mock Master Customer Records
    const DEFAULT_ENTERPRISE_CUSTOMERS = [
        {
            id: 1,
            companyName: "Tập đoàn Công nghệ FPT",
            name: "Tập đoàn Công nghệ FPT",
            taxCode: "0101248141",
            status: "CHINH_THUC",
            industryId: 1,
            companySizeId: 13,
            email: "contact@fpt.com.vn",
            phone: "02473007300",
            website: "https://fpt.com.vn",
            address: "Số 10 Phạm Văn Bạch, Cầu Giấy, Hà Nội",
            ownerUserId: 1,
            ownerName: "Nông Quang Tiệp",
            owner: "Nông Quang Tiệp",
            parent_customer_id: null,
            createdAt: "2026-01-15T08:30:00"
        },
        {
            id: 2,
            companyName: "Công ty Cổ phần VNG",
            name: "Công ty Cổ phần VNG",
            taxCode: "0303579890",
            status: "DANG_GIAO_DICH",
            industryId: 1,
            companySizeId: 12,
            email: "partner@vng.com.vn",
            phone: "02839623888",
            website: "https://vng.com.vn",
            address: "Z06 Đường số 13, KCX Tân Thuận, Quận 7, TP.HCM",
            ownerUserId: 1,
            ownerName: "Nông Quang Tiệp",
            owner: "Nông Quang Tiệp",
            parent_customer_id: 1,
            createdAt: "2026-02-10T09:15:00"
        },
        {
            id: 3,
            companyName: "Công ty TNHH Phần mềm MISA",
            name: "Công ty TNHH Phần mềm MISA",
            taxCode: "0100779774",
            status: "TIEM_NANG",
            industryId: 1,
            companySizeId: 12,
            email: "contact@misa.vn",
            phone: "02437959595",
            website: "https://misa.vn",
            address: "Tòa nhà MISA, Lô 5, Công viên phần mềm Quang Trung, Quận 12, TP.HCM",
            ownerUserId: 2,
            ownerName: "Nguyễn Văn An",
            owner: "Nguyễn Văn An",
            parent_customer_id: 1,
            createdAt: "2026-03-01T10:00:00"
        },
        {
            id: 4,
            companyName: "Tập đoàn Bưu chính Viễn thông VNPT",
            name: "Tập đoàn Bưu chính Viễn thông VNPT",
            taxCode: "0100684378",
            status: "CHINH_THUC",
            industryId: 1,
            companySizeId: 13,
            email: "vanphong@vnpt.vn",
            phone: "02437741091",
            website: "https://vnpt.com.vn",
            address: "57 Huỳnh Thúc Kháng, Láng Hạ, Đống Đa, Hà Nội",
            ownerUserId: 1,
            ownerName: "Nông Quang Tiệp",
            owner: "Nông Quang Tiệp",
            parent_customer_id: null,
            createdAt: "2026-01-20T14:20:00"
        },
        {
            id: 5,
            companyName: "Công ty CP Đầu tư Thế Giới Di Động",
            name: "Công ty CP Đầu tư Thế Giới Di Động",
            taxCode: "0303274391",
            status: "DANG_GIAO_DICH",
            industryId: 2,
            companySizeId: 13,
            email: "lienhe@thegioididong.com",
            phone: "02838125960",
            website: "https://mwg.vn",
            address: "128 Trần Quang Khải, Tân Định, Quận 1, TP.HCM",
            ownerUserId: 2,
            ownerName: "Nguyễn Văn An",
            owner: "Nguyễn Văn An",
            parent_customer_id: 1,
            createdAt: "2026-02-18T11:45:00"
        },
        {
            id: 6,
            companyName: "Ngân hàng TMCP Quân Đội (MBBank)",
            name: "Ngân hàng TMCP Quân Đội (MBBank)",
            taxCode: "0100283873",
            status: "CHINH_THUC",
            industryId: 4,
            companySizeId: 13,
            email: "mb247@mbbank.com.vn",
            phone: "1900545426",
            website: "https://mbbank.com.vn",
            address: "Số 18 Lê Văn Lương, Trung Hòa, Cầu Giấy, Hà Nội",
            ownerUserId: 1,
            ownerName: "Nông Quang Tiệp",
            owner: "Nông Quang Tiệp",
            parent_customer_id: null,
            createdAt: "2026-01-05T08:00:00"
        },
        {
            id: 7,
            companyName: "Công ty CP Dược phẩm Imexpharm",
            name: "Công ty CP Dược phẩm Imexpharm",
            taxCode: "1400384433",
            status: "CHINH_THUC",
            industryId: 7,
            companySizeId: 12,
            email: "imexpharm@imexpharm.com",
            phone: "02773851941",
            website: "https://imexpharm.com",
            address: "Số 4, Đường 30/4, Phường 1, TP. Cao Lãnh, Đồng Tháp",
            ownerUserId: 2,
            ownerName: "Nguyễn Văn An",
            owner: "Nguyễn Văn An",
            parent_customer_id: 4,
            createdAt: "2026-03-12T16:00:00"
        },
        {
            id: 8,
            companyName: "Công ty Cổ phần Tập đoàn Hòa Phát",
            name: "Công ty Cổ phần Tập đoàn Hòa Phát",
            taxCode: "0900189284",
            status: "CHINH_THUC",
            industryId: 3,
            companySizeId: 13,
            email: "contact@hoaphat.com.vn",
            phone: "02462810999",
            website: "https://hoaphat.com.vn",
            address: "KCN Phố Nối A, Xã Giai Phạm, Huyện Yên Mỹ, Hưng Yên",
            ownerUserId: 1,
            ownerName: "Nông Quang Tiệp",
            owner: "Nông Quang Tiệp",
            parent_customer_id: null,
            createdAt: "2026-02-01T09:00:00"
        }
    ];

    /* =========================================================
       1. IN-MEMORY STATE STORE (FALLBACK WHEN API NOT READY)
    ========================================================= */
    const inMemoryState = {
        parents: { ...DEFAULT_HIERARCHY_MAPPING },
        contracts: { ...DEFAULT_MOCK_CONTRACTS },
        customers: DEFAULT_ENTERPRISE_CUSTOMERS.map(c => ({ ...c })),
        collapsedNodes: []
    };

    function getHierarchyStorage() {
        return inMemoryState;
    }

    function saveHierarchyStorage(data) {
        if (data && typeof data === "object") {
            if (data.parents) inMemoryState.parents = data.parents;
            if (data.contracts) inMemoryState.contracts = data.contracts;
            if (Array.isArray(data.collapsedNodes)) inMemoryState.collapsedNodes = data.collapsedNodes;
        }
    }

    function getLocalCustomersList() {
        return inMemoryState.customers;
    }

    function getCustomerById(id) {
        const list = getLocalCustomersList();
        const found = list.find(c => Number(c.id) === Number(id));
        if (found) {
            const parentId = inMemoryState.parents[Number(id)] !== undefined ? inMemoryState.parents[Number(id)] : (found.parent_customer_id || null);
            return { ...found, parent_customer_id: parentId ? Number(parentId) : null };
        }
        return null;
    }

    function syncCustomerParentInStorage(customerId, parentId) {
        const cId = Number(customerId);
        const pId = parentId ? Number(parentId) : null;
        if (pId === null) {
            delete inMemoryState.parents[cId];
        } else {
            inMemoryState.parents[cId] = pId;
        }

        const found = inMemoryState.customers.find(c => Number(c.id) === cId);
        if (found) {
            found.parent_customer_id = pId;
        }
    }

    /* =========================================================
       2. RECURSIVE CYCLE & VALIDATION HELPERS
    ========================================================= */
    /**
     * Recursively retrieves all descendant IDs of a customer
     */
    function getAllDescendantIds(customerId, hierarchyData) {
        const hier = hierarchyData || getHierarchyStorage();
        const parents = hier.parents || {};
        const descendants = new Set();

        function collect(parentId) {
            for (const [childIdStr, pId] of Object.entries(parents)) {
                if (Number(pId) === Number(parentId)) {
                    const childId = Number(childIdStr);
                    if (!descendants.has(childId)) {
                        descendants.add(childId);
                        collect(childId);
                    }
                }
            }
        }

        collect(customerId);
        return descendants;
    }

    /**
     * Validates if targetParentId can be set as parent of customerId
     * Prevents self-parenting and circular dependency.
     */
    function validateParentAssignment(customerId, targetParentId) {
        const cId = Number(customerId);
        const pId = targetParentId !== null && targetParentId !== undefined && targetParentId !== "" ? Number(targetParentId) : null;

        if (pId === null) {
            return { valid: true };
        }

        if (cId === pId) {
            return {
                valid: false,
                message: "Một công ty không thể tự chọn chính nó làm công ty mẹ (Self-parent prohibited)!"
            };
        }

        const descendants = getAllDescendantIds(cId);
        if (descendants.has(pId)) {
            const targetComp = getCustomerById(pId);
            const targetName = targetComp ? (targetComp.companyName || targetComp.name) : `ID ${pId}`;
            return {
                valid: false,
                message: `Không thể chọn "${targetName}" làm công ty mẹ vì đơn vị này đang là công ty con/cháu trong phân cấp của bạn (Tránh tạo vòng lặp đệ quy)!`
            };
        }

        return { valid: true };
    }

    /**
     * Finds the ultimate top root parent of a customer hierarchy
     */
    function getRootParentId(customerId, hierarchyData) {
        const hier = hierarchyData || getHierarchyStorage();
        const parents = hier.parents || {};
        let current = Number(customerId);
        const visited = new Set();

        while (parents[current]) {
            if (visited.has(current)) break; // cycle guard
            visited.add(current);
            current = Number(parents[current]);
        }
        return current;
    }

    /* =========================================================
       3. STANDARDIZED API CLIENT WITH FALLBACK
    ========================================================= */
    /**
     * 1. GET /api/customers?search={query}
     */
    async function apiSearchCustomers(query = "") {
        const cleanQuery = (query || "").trim();
        const url = `${API_BASE}/api/customers?search=${encodeURIComponent(cleanQuery)}`;

        try {
            const response = await fetch(url, {
                credentials: "include",
                headers: { "Accept": "application/json" }
            });
            if (response.ok) {
                const res = await response.json();
                if (res.success && Array.isArray(res.data)) {
                    return res.data;
                }
            }
        } catch (_) {}

        // Fallback: search in local customers list
        const all = getLocalCustomersList();
        if (!cleanQuery) return all.slice(0, 20);

        const qLower = cleanQuery.toLowerCase();
        return all.filter(c => {
            const name = (c.companyName || c.name || "").toLowerCase();
            const tax = (c.taxCode || "").toLowerCase();
            const phone = (c.phone || "").toLowerCase();
            const email = (c.email || "").toLowerCase();
            return name.includes(qLower) || tax.includes(qLower) || phone.includes(qLower) || email.includes(qLower);
        });
    }

    /**
     * 2. GET /api/customers/{id}/hierarchy
     */
    async function apiGetHierarchy(customerId) {
        const cId = Number(customerId);
        const url = `${API_BASE}/api/customers/${cId}/hierarchy`;

        try {
            const response = await fetch(url, {
                credentials: "include",
                headers: { "Accept": "application/json" }
            });
            if (response.ok) {
                const res = await response.json();
                if (res.success && res.data) {
                    return res.data;
                }
            }
        } catch (_) {}

        // Fallback: build tree locally
        const hier = getHierarchyStorage();
        const rootId = getRootParentId(cId, hier);
        return buildLocalHierarchyTree(rootId, hier);
    }

    /**
     * Recursive builder for tree node
     */
    function buildLocalHierarchyTree(nodeId, hier, depth = 0) {
        const customer = getCustomerById(nodeId);
        if (!customer) return null;

        const parents = hier.parents || {};
        const contracts = hier.contracts || {};

        // Find direct children
        const children = [];
        for (const [childIdStr, pId] of Object.entries(parents)) {
            if (Number(pId) === Number(nodeId)) {
                const childTree = buildLocalHierarchyTree(Number(childIdStr), hier, depth + 1);
                if (childTree) children.push(childTree);
            }
        }

        const myContracts = contracts[nodeId] || [];
        const contractValue = myContracts.reduce((sum, item) => sum + (Number(item.amount) || 0), 0);

        return {
            id: Number(nodeId),
            name: customer.companyName || customer.name || `Doanh nghiệp ${nodeId}`,
            taxCode: customer.taxCode || "—",
            ownerName: customer.ownerName || customer.owner || "Chưa phân công",
            parent_customer_id: parents[nodeId] ? Number(parents[nodeId]) : null,
            depth: depth,
            contractValue: contractValue,
            contractsCount: myContracts.length,
            childrenCount: children.length,
            status: customer.status || "CHINH_THUC",
            children: children
        };
    }

    /**
     * 3. PUT /api/customers/{id}/parent
     * Payload: { parent_customer_id: ... }
     */
    async function apiUpdateParent(customerId, parentCustomerId) {
        const cId = Number(customerId);
        const pId = parentCustomerId !== null && parentCustomerId !== undefined && parentCustomerId !== "" ? Number(parentCustomerId) : null;

        // Perform strict recursive validation
        const check = validateParentAssignment(cId, pId);
        if (!check.valid) {
            throw new Error(check.message);
        }

        const url = `${API_BASE}/api/customers/${cId}/parent`;
        let serverSuccess = false;

        try {
            const response = await fetch(url, {
                method: "PUT",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify({ parent_customer_id: pId })
            });
            if (response.ok) {
                const res = await response.json();
                if (res.success) {
                    serverSuccess = true;
                }
            }
        } catch (_) {}

        // Synchronize in-memory fallback state
        syncCustomerParentInStorage(cId, pId);

        // Notify app
        document.dispatchEvent(new CustomEvent("crm:hierarchy-updated", { detail: { customerId: cId, parentId: pId } }));
        document.dispatchEvent(new CustomEvent("crm:customers-updated"));

        return { success: true, parent_customer_id: pId, serverSynced: serverSuccess };
    }

    /**
     * 4. GET /api/contracts/group-summary?parent_id={id}
     */
    async function apiGetGroupSummary(parentId) {
        const pId = Number(parentId);
        const url = `${API_BASE}/api/contracts/group-summary?parent_id=${pId}`;

        try {
            const response = await fetch(url, {
                credentials: "include",
                headers: { "Accept": "application/json" }
            });
            if (response.ok) {
                const res = await response.json();
                if (res.success && res.data) {
                    return res.data;
                }
            }
        } catch (_) {}

        // Fallback: compute consolidated revenue locally
        const hier = getHierarchyStorage();
        const rootId = getRootParentId(pId, hier);
        const tree = buildLocalHierarchyTree(rootId, hier);

        const flatEntities = [];
        function flatten(node, role = "PARENT") {
            if (!node) return;
            flatEntities.push({
                id: node.id,
                name: node.name,
                taxCode: node.taxCode,
                ownerName: node.ownerName,
                role: role,
                roleLabel: role === "PARENT" ? "Công ty Mẹ" : (node.depth === 1 ? "Công ty Con" : "Công ty Cháu"),
                contractValue: node.contractValue || 0,
                contractsCount: node.contractsCount || 0,
                status: node.status || "CHINH_THUC"
            });
            (node.children || []).forEach(child => flatten(child, "SUBSIDIARY"));
        }

        flatten(tree, "PARENT");

        const totalGroupRevenue = flatEntities.reduce((sum, item) => sum + item.contractValue, 0);
        const parentEntity = flatEntities.find(e => e.role === "PARENT") || flatEntities[0];
        const parentRevenue = parentEntity ? parentEntity.contractValue : 0;
        const subsidiariesRevenue = totalGroupRevenue - parentRevenue;

        // Calculate contribution percentages
        const breakdown = flatEntities.map(item => {
            const pct = totalGroupRevenue > 0 ? ((item.contractValue / totalGroupRevenue) * 100) : 0;
            return {
                ...item,
                contributionPercentage: Number(pct.toFixed(2))
            };
        });

        // Sort breakdown: Parent first, then by contract value descending
        breakdown.sort((a, b) => {
            if (a.role === "PARENT") return -1;
            if (b.role === "PARENT") return 1;
            return b.contractValue - a.contractValue;
        });

        return {
            parentId: rootId,
            parentName: parentEntity ? parentEntity.name : "Tập đoàn",
            totalGroupRevenue: totalGroupRevenue,
            parentRevenue: parentRevenue,
            subsidiariesRevenue: subsidiariesRevenue,
            totalEntities: flatEntities.length,
            breakdown: breakdown
        };
    }

    /* =========================================================
       4. FORMATTING & UTILITIES
    ========================================================= */
    function formatCurrencyVND(amount) {
        const val = Number(amount) || 0;
        return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(val);
    }

    function formatShortMoney(amount) {
        const val = Number(amount) || 0;
        if (val >= 1000000000) {
            return (val / 1000000000).toFixed(2).replace(/\.00$/, "") + " tỷ ₫";
        }
        if (val >= 1000000) {
            return (val / 1000000).toFixed(1).replace(/\.0$/, "") + " tr ₫";
        }
        return formatCurrencyVND(val);
    }

    function escapeHtml(str) {
        return String(str || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function showToast(message, type = "success") {
        const existing = document.querySelector(".crm-toast");
        if (existing) existing.remove();

        const toast = document.createElement("div");
        toast.className = `crm-toast crm-toast-${type}`;
        toast.style.cssText = "position:fixed; bottom:24px; right:24px; z-index:1150; min-width:320px; padding:14px 20px; border-radius:10px; box-shadow:var(--crm-shadow); animation:fadeInUp 0.3s ease; display:flex; align-items:center; gap:12px; background:var(--crm-surface); border:1.5px solid var(--crm-border);";

        const icon = type === "success" ? "✓" : "⚠️";
        const color = type === "success" ? "var(--crm-success)" : "var(--crm-danger)";
        toast.innerHTML = `
            <span style="font-weight:900; color:${color}; font-size:18px;">${icon}</span>
            <span style="font-size:13.5px; font-weight:600; color:var(--crm-text);">${escapeHtml(message)}</span>
        `;

        document.body.appendChild(toast);
        setTimeout(() => {
            toast.style.opacity = "0";
            toast.style.transition = "opacity 0.3s ease";
            setTimeout(() => toast.remove(), 300);
        }, 3400);
    }

    /* =========================================================
       5. UI RENDERER: CUSTOMER 360 BANNER & DETAILS
    ========================================================= */
    async function renderCustomer360Hierarchy(container, customerId) {
        if (!container || !customerId) return;
        const cId = Number(customerId);

        const hier = getHierarchyStorage();
        const rootId = getRootParentId(cId, hier);
        const isRoot = rootId === cId;
        const hasParent = !!hier.parents[cId];
        const isChild = hasParent;
        const currentCust = getCustomerById(cId);

        // Check if this company or its group has multiple members
        const summary = await apiGetGroupSummary(rootId);
        const hasGroup = summary.totalEntities > 1;

        // Render Sidebar Info (Company Panel)
        const parentDisplayEl = document.getElementById("company360Parent");
        if (parentDisplayEl) {
            if (hasParent) {
                const parentCust = getCustomerById(hier.parents[cId]);
                const pName = parentCust ? (parentCust.companyName || parentCust.name) : `Công ty mẹ #${hier.parents[cId]}`;
                parentDisplayEl.innerHTML = `
                    <div style="display:flex; align-items:center; justify-content:space-between; gap:6px;">
                        <a href="customer-360.html?id=${hier.parents[cId]}" style="color:var(--crm-primary); font-weight:700; text-decoration:none;">
                            🏢 ${escapeHtml(pName)}
                        </a>
                        <button type="button" class="crm-btn crm-btn-secondary" onclick="window.CorporateHierarchyManager.openHierarchyModal(${cId})" style="padding:2px 6px; font-size:11px; height:auto;">
                            Đổi
                        </button>
                    </div>
                `;
            } else if (hasGroup && isRoot) {
                parentDisplayEl.innerHTML = `
                    <div style="display:flex; align-items:center; justify-content:space-between; gap:6px;">
                        <span style="color:#d97706; font-weight:700;">👑 Công ty Mẹ (${summary.totalEntities - 1} cty con)</span>
                        <button type="button" class="crm-btn crm-btn-secondary" onclick="window.CorporateHierarchyManager.openHierarchyModal(${cId})" style="padding:2px 6px; font-size:11px; height:auto;">
                            Quản lý
                        </button>
                    </div>
                `;
            } else {
                parentDisplayEl.innerHTML = `
                    <div style="display:flex; align-items:center; justify-content:space-between; gap:6px;">
                        <span style="color:var(--crm-muted);">Đơn vị độc lập</span>
                        <button type="button" class="crm-btn crm-btn-secondary" onclick="window.CorporateHierarchyManager.openHierarchyModal(${cId})" style="padding:2px 6px; font-size:11px; height:auto;">
                            + Gán Mẹ
                        </button>
                    </div>
                `;
            }
        }

        // Render KPI Group Banner
        const bannerContainer = document.getElementById("customer360HierarchyBanner") || container;
        if (!bannerContainer) return;

        // Update Tab Count Badge
        const tabBadge = document.getElementById("hierarchyBadgeCount");
        if (tabBadge) tabBadge.textContent = String(summary.totalEntities || 1);

        bannerContainer.innerHTML = `
            <div class="group-kpi-card">
                <div class="group-kpi-header">
                    <div class="group-kpi-title-box">
                        <div class="group-kpi-icon-badge">
                            <svg viewBox="0 0 24 24"><path d="M3 21h18M3 7v14M21 7v14M6 18h2v3H6zm8 0h2v3h-2zm-4 0h2v3h-2zm4-6h2v3h-2zm-4 0h2v3h-2zm-4 0h2v3H6zm12-5V3H6v4"/></svg>
                        </div>
                        <div>
                            <span class="group-kpi-tag">
                                🏢 CRM-65: TỔNG HỢP TẬP ĐOÀN & PHÂN CẤP DOANH NGHIỆP
                            </span>
                            <h2 class="group-kpi-main-title">
                                ${escapeHtml(summary.parentName)}
                                ${isChild ? `<span class="corporate-role-badge corporate-role-child">Thành viên trực thuộc</span>` : `<span class="corporate-role-badge corporate-role-parent">Công ty Mẹ / Trụ sở chính</span>`}
                            </h2>
                        </div>
                    </div>

                    <div class="group-kpi-actions">
                        <button type="button" class="crm-btn crm-btn-secondary" onclick="window.CorporateHierarchyManager.openHierarchyModal(${cId})">
                            <svg class="crm-inline-icon" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><path d="M12 8v8M8 12h8"/></svg>
                            Khai báo Quan hệ Mẹ - Con
                        </button>
                        <button type="button" class="crm-btn crm-btn-primary" onclick="window.CorporateHierarchyManager.openTreeModal(${rootId})">
                            <svg class="crm-inline-icon" viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><path d="M6.5 10v7H14"/></svg>
                            Xem Cây Phân cấp Toàn diện
                        </button>
                    </div>
                </div>

                <!-- 4 KPI Metrics -->
                <div class="group-kpi-metrics-grid">
                    <div class="group-metric-tile highlight">
                        <span class="group-metric-label">
                            <svg class="crm-inline-icon" viewBox="0 0 24 24"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
                            Tổng Giá Trị Hợp Đồng Tập Đoàn
                        </span>
                        <div class="group-metric-value">${formatCurrencyVND(summary.totalGroupRevenue)}</div>
                        <span class="group-metric-sub">Hợp đồng công ty mẹ + ${summary.totalEntities - 1} công ty con</span>
                    </div>

                    <div class="group-metric-tile">
                        <span class="group-metric-label">Số Pháp Nhân Thành Viên</span>
                        <div class="group-metric-value" style="color:var(--crm-primary);">${summary.totalEntities} <span style="font-size:14px;font-weight:500;">đơn vị</span></div>
                        <span class="group-metric-sub">1 Mẹ + ${summary.totalEntities - 1} Công ty con</span>
                    </div>

                    <div class="group-metric-tile">
                        <span class="group-metric-label">Doanh Thu Công Ty Mẹ</span>
                        <div class="group-metric-value">${formatShortMoney(summary.parentRevenue)}</div>
                        <span class="group-metric-sub">Trực tiếp từ hợp đồng mẹ</span>
                    </div>

                    <div class="group-metric-tile">
                        <span class="group-metric-label">Đóng Góp Các Công Ty Con</span>
                        <div class="group-metric-value" style="color:var(--crm-cyan);">${formatShortMoney(summary.subsidiariesRevenue)}</div>
                        <span class="group-metric-sub">${summary.totalGroupRevenue > 0 ? ((summary.subsidiariesRevenue / summary.totalGroupRevenue) * 100).toFixed(1) : 0}% trên tổng doanh thu</span>
                    </div>
                </div>

                <!-- Consolidated Breakdown Table -->
                <div class="corporate-breakdown-card" style="margin-bottom:0;">
                    <div class="corporate-card-head">
                        <h3>
                            <svg class="crm-inline-icon" viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
                            Bảng Phân Rã Giá Trị Hợp Đồng Từng Pháp Nhân Thành Viên
                        </h3>
                        <span style="font-size:12.5px; color:var(--crm-muted); font-weight:600;">
                            Được tổng hợp từ các hợp đồng đã ký kết
                        </span>
                    </div>

                    <div class="corporate-table-wrapper">
                        <table class="corporate-table">
                            <thead>
                                <tr>
                                    <th>Pháp nhân Doanh nghiệp</th>
                                    <th>Vai trò Tập đoàn</th>
                                    <th>Mã số thuế</th>
                                    <th>Phụ trách (Owner)</th>
                                    <th>Giá trị Hợp đồng (VND)</th>
                                    <th>Tỷ lệ Đóng góp (%)</th>
                                    <th style="text-align:right;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${summary.breakdown.map(item => {
                                    const isCurrentItem = item.id === cId;
                                    const roleClass = item.role === "PARENT" ? "corporate-role-parent" : "corporate-role-child";
                                    const roleIcon = item.role === "PARENT" ? "👑" : "🏢";

                                    return `
                                        <tr style="${isCurrentItem ? 'background:rgba(37,99,235,0.06); font-weight:600;' : ''}">
                                            <td>
                                                <div style="display:flex; align-items:center; gap:10px;">
                                                    <span style="font-size:16px;">${roleIcon}</span>
                                                    <div>
                                                        <a href="customer-360.html?id=${item.id}" style="color:var(--crm-text); text-decoration:none; font-weight:700;">
                                                            ${escapeHtml(item.name)}
                                                        </a>
                                                        ${isCurrentItem ? '<span style="margin-left:6px; font-size:11px; background:var(--crm-primary); color:#fff; padding:1px 6px; border-radius:4px;">Hiện tại</span>' : ''}
                                                    </div>
                                                </div>
                                            </td>
                                            <td>
                                                <span class="corporate-role-badge ${roleClass}">
                                                    ${item.roleLabel}
                                                </span>
                                            </td>
                                            <td><code>${escapeHtml(item.taxCode)}</code></td>
                                            <td>👤 ${escapeHtml(item.ownerName)}</td>
                                            <td style="font-weight:700; color:var(--crm-text);">
                                                ${formatCurrencyVND(item.contractValue)}
                                            </td>
                                            <td>
                                                <div class="contribution-progress-wrap">
                                                    <div class="contribution-progress-bar">
                                                        <div class="contribution-progress-fill" style="width: ${item.contributionPercentage}%;"></div>
                                                    </div>
                                                    <span class="contribution-percentage">${item.contributionPercentage}%</span>
                                                </div>
                                            </td>
                                            <td style="text-align:right;">
                                                <div style="display:inline-flex; gap:6px;">
                                                    <a href="customer-360.html?id=${item.id}" class="crm-btn crm-btn-secondary" style="padding:4px 8px; font-size:12px; height:auto; text-decoration:none;" title="Xem Hồ sơ 360">
                                                        👁️ 360
                                                    </a>
                                                    ${item.role !== "PARENT" ? `
                                                        <button type="button" class="crm-btn crm-btn-secondary" onclick="window.CorporateHierarchyManager.promptUnbindParent(${item.id})" style="padding:4px 8px; font-size:12px; height:auto; color:var(--crm-danger);" title="Gỡ khỏi tập đoàn">
                                                            ❌ Gỡ
                                                        </button>
                                                    ` : ''}
                                                </div>
                                            </td>
                                        </tr>
                                    `;
                                }).join("")}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;

        // Render Right-Panel Tab Hierarchy View
        renderRightTabHierarchy(document.getElementById("hierarchyPanel"), cId, summary, hier);
    }

    /**
     * Renders Right Column Tab (Hierarchy Panel)
     */
    function renderRightTabHierarchy(panelEl, currentCustId, summary, hier) {
        if (!panelEl) return;
        const cId = Number(currentCustId);
        const rootId = getRootParentId(cId, hier);

        panelEl.innerHTML = `
            <div style="padding:12px 14px; border-bottom:1px solid var(--crm-border); display:flex; justify-content:space-between; align-items:center;">
                <span style="font-size:13px; font-weight:700; color:var(--crm-text);">
                    Phân cấp Tập đoàn (${summary.totalEntities} thành viên)
                </span>
                <button type="button" class="crm-btn crm-btn-secondary" onclick="window.CorporateHierarchyManager.openHierarchyModal(${cId})" style="font-size:11.5px; padding:3px 8px; height:auto;">
                    + Thiết lập
                </button>
            </div>

            <div style="padding:14px; max-height:480px; overflow-y:auto;">
                <div class="hierarchy-side-list">
                    ${summary.breakdown.map(item => {
                        const isCurrent = item.id === cId;
                        const isParent = item.role === "PARENT";
                        return `
                            <div class="hierarchy-side-item ${isParent ? 'is-parent' : 'is-child'}" style="${isCurrent ? 'border-color:var(--crm-primary); background:rgba(37,99,235,0.04);' : ''}">
                                <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:4px;">
                                    <span class="corporate-role-badge ${isParent ? 'corporate-role-parent' : 'corporate-role-child'}" style="font-size:10px; padding:1px 6px;">
                                        ${isParent ? '👑 CÔNG TY MẸ' : '🏢 CÔNG TY CON'}
                                    </span>
                                    <span style="font-size:11.5px; font-weight:700; color:var(--crm-success);">
                                        ${formatShortMoney(item.contractValue)}
                                    </span>
                                </div>
                                <div style="font-weight:700; font-size:13px; color:var(--crm-text); margin-bottom:2px;">
                                    <a href="customer-360.html?id=${item.id}" style="color:inherit; text-decoration:none;">
                                        ${escapeHtml(item.name)}
                                    </a>
                                </div>
                                <div style="font-size:11.5px; color:var(--crm-muted); display:flex; justify-content:space-between;">
                                    <span>MST: ${escapeHtml(item.taxCode)}</span>
                                    <span>👤 ${escapeHtml(item.ownerName)}</span>
                                </div>
                            </div>
                        `;
                    }).join("")}
                </div>

                <div style="margin-top:14px;">
                    <button type="button" class="crm-btn crm-btn-secondary full-button" onclick="window.CorporateHierarchyManager.openTreeModal(${rootId})" style="width:100%;">
                        🌳 Mở Sơ đồ Cây Phân cấp Toàn diện
                    </button>
                </div>
            </div>
        `;
    }

    /* =========================================================
       6. UI RENDERER: INTERACTIVE HIERARCHY TREE VIEW
    ========================================================= */
    function renderHierarchyTreeHTML(treeNode, currentHighlightId, collapsedSet) {
        if (!treeNode) return "";
        const isCurrent = Number(treeNode.id) === Number(currentHighlightId);
        const hasChildren = Array.isArray(treeNode.children) && treeNode.children.length > 0;
        const isCollapsed = collapsedSet && collapsedSet.has(Number(treeNode.id));

        const roleClass = treeNode.depth === 0 ? "is-parent" : (treeNode.depth === 1 ? "is-child" : "is-grandchild");
        const roleBadge = treeNode.depth === 0 ? "👑 CÔNG TY MẸ" : (treeNode.depth === 1 ? "🏢 CÔNG TY CON CẤP 1" : "🏬 CÔNG TY CHÁU");
        const roleBadgeClass = treeNode.depth === 0 ? "corporate-role-parent" : (treeNode.depth === 1 ? "corporate-role-child" : "corporate-role-grandchild");

        let html = `
            <div class="tree-node-branch" data-node-id="${treeNode.id}">
                <!-- Node Card -->
                <div class="tree-node-card ${roleClass} ${isCurrent ? 'is-current' : ''}">
                    <div class="tree-node-head">
                        <span class="corporate-role-badge ${roleBadgeClass}" style="font-size:10px; padding:2px 7px;">
                            ${roleBadge}
                        </span>
                        <span style="font-size:11.5px; color:var(--crm-muted); font-weight:600;">
                            ID: ${treeNode.id}
                        </span>
                    </div>

                    <h4 class="tree-node-company-name" title="${escapeHtml(treeNode.name)}">
                        <a href="customer-360.html?id=${treeNode.id}">
                            ${escapeHtml(treeNode.name)}
                        </a>
                    </h4>

                    <div class="tree-node-meta-row">
                        <span>🏷️ MST: <strong>${escapeHtml(treeNode.taxCode)}</strong></span>
                    </div>
                    <div class="tree-node-meta-row">
                        <span>👤 Sale: ${escapeHtml(treeNode.ownerName)}</span>
                    </div>

                    <div class="tree-node-revenue-box">
                        <span style="font-size:11px; color:var(--crm-muted); text-transform:uppercase; font-weight:600;">Hợp đồng</span>
                        <span class="tree-node-revenue-val">${formatCurrencyVND(treeNode.contractValue)}</span>
                    </div>

                    <div class="tree-node-actions">
                        <a href="customer-360.html?id=${treeNode.id}" class="crm-btn crm-btn-secondary" style="flex:1; padding:3px 6px; font-size:11px; height:auto; text-align:center; text-decoration:none;">
                            👁️ 360
                        </a>
                        <button type="button" class="crm-btn crm-btn-secondary" onclick="window.CorporateHierarchyManager.openHierarchyModal(${treeNode.id})" style="padding:3px 6px; font-size:11px; height:auto;" title="Quản lý quan hệ">
                            ⚙️ Thiết lập
                        </button>
                    </div>

                    ${hasChildren ? `
                        <button type="button" class="tree-toggle-btn" onclick="window.CorporateHierarchyManager.toggleTreeNode(${treeNode.id})" title="${isCollapsed ? 'Mở rộng nhánh' : 'Thu gọn nhánh'}">
                            ${isCollapsed ? '+' : '−'}
                        </button>
                    ` : ''}
                </div>
        `;

        if (hasChildren && !isCollapsed) {
            html += `
                <div class="tree-stem-vertical"></div>
                <div class="tree-children-row ${treeNode.children.length === 1 ? 'single-child' : ''}">
                    ${treeNode.children.map(child => `
                        <div style="position:relative; display:flex; flex-direction:column; align-items:center;">
                            <div class="tree-child-stem"></div>
                            ${renderHierarchyTreeHTML(child, currentHighlightId, collapsedSet)}
                        </div>
                    `).join("")}
                </div>
            `;
        }

        html += `</div>`;
        return html;
    }

    /* =========================================================
       7. MODAL: PARENT-CHILD ASSOCIATION MANAGEMENT (CRM-65)
    ========================================================= */
    let currentModalTargetId = null;

    function buildHierarchyModalDOM() {
        if (document.getElementById("corporateHierarchyModal")) return;

        const overlay = document.createElement("div");
        overlay.id = "corporateHierarchyModalOverlay";
        overlay.className = "corporate-modal-overlay";

        const dialog = document.createElement("div");
        dialog.id = "corporateHierarchyModal";
        dialog.className = "corporate-modal-dialog";
        dialog.setAttribute("role", "dialog");
        dialog.setAttribute("aria-labelledby", "hierModalTitle");

        dialog.innerHTML = `
            <header class="corporate-modal-header">
                <div>
                    <span class="group-kpi-tag" style="margin-bottom:6px;">CRM-65 / S3-05: QUẢN LÝ QUAN HỆ CÔNG TY MẸ - CON</span>
                    <h2 id="hierModalTitle">Khai báo Phân cấp Tập đoàn Doanh nghiệp</h2>
                    <p>Thiết lập quan hệ pháp nhân mẹ - con, tổng hợp doanh thu và bảo toàn cấu trúc tổ chức.</p>
                </div>
                <button type="button" class="corporate-modal-close" onclick="window.CorporateHierarchyManager.closeHierarchyModal()" aria-label="Đóng">✕</button>
            </header>

            <div class="corporate-modal-body">
                <!-- Validation Cycle Warning Banner -->
                <div id="hierCycleAlert" class="cycle-warning-banner">
                    <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" fill="none" stroke-width="2" style="flex-shrink:0;"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                    <span id="hierCycleAlertText">Vòng lặp quan hệ phát hiện!</span>
                </div>

                <!-- Current Company Target Card -->
                <div style="background:var(--crm-surface-soft); border:1px solid var(--crm-border); border-radius:var(--crm-radius-md); padding:16px 20px; margin-bottom:20px;">
                    <div style="font-size:11.5px; text-transform:uppercase; font-weight:700; color:var(--crm-muted); margin-bottom:4px;">Doanh nghiệp đang thao tác:</div>
                    <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
                        <div>
                            <h3 id="modalTargetName" style="margin:0 0 2px; font-size:16px; font-weight:800; color:var(--crm-text);">—</h3>
                            <div style="font-size:12.5px; color:var(--crm-muted);">
                                MST: <strong id="modalTargetTax">—</strong> · Phụ trách: <strong id="modalTargetOwner">—</strong>
                            </div>
                        </div>
                        <span id="modalTargetRoleBadge" class="corporate-role-badge corporate-role-parent">—</span>
                    </div>
                </div>

                <!-- TAB 1: GÁN HOẶC ĐỔI CÔNG TY MẸ -->
                <div style="margin-bottom:28px;">
                    <h4 style="margin:0 0 10px; font-size:14.5px; font-weight:700; color:var(--crm-text); display:flex; align-items:center; gap:8px;">
                        <span>👑 1. Khai báo / Gán Công ty Mẹ (parent_customer_id)</span>
                    </h4>

                    <!-- Current Parent Display -->
                    <div id="currentParentDisplayBox" class="current-association-box">
                        <div class="current-assoc-info">
                            <div class="current-assoc-avatar">🏢</div>
                            <div>
                                <div style="font-size:11px; font-weight:700; color:var(--crm-muted); text-transform:uppercase;">Công ty mẹ hiện tại:</div>
                                <div id="currentParentName" style="font-weight:700; font-size:14px; color:var(--crm-text);">Chưa có công ty mẹ (Đơn vị độc lập)</div>
                                <div id="currentParentMeta" style="font-size:12px; color:var(--crm-muted);">—</div>
                            </div>
                        </div>
                        <button id="btnUnbindParent" type="button" class="crm-btn crm-btn-secondary" style="color:var(--crm-danger); border-color:var(--crm-danger); display:none;">
                            ❌ Gỡ bỏ công ty mẹ
                        </button>
                    </div>

                    <!-- Search Input to Choose New Parent -->
                    <div class="corporate-search-wrap">
                        <label style="display:block; font-size:12.5px; font-weight:600; color:var(--crm-text); margin-bottom:6px;">
                            Tìm kiếm và chọn Công ty mẹ mới:
                        </label>
                        <div class="corporate-search-input-box">
                            <svg class="corporate-search-icon" viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></svg>
                            <input id="inputSearchParent" class="crm-input" type="search" placeholder="Gõ tên công ty hoặc MST để tìm kiếm..." autocomplete="off">
                        </div>
                        <div id="dropdownParentResults" class="corporate-dropdown-results"></div>
                    </div>
                </div>

                <!-- TAB 2: QUẢN LÝ CÔNG TY CON TRỰC THUỘC -->
                <div>
                    <h4 style="margin:0 0 10px; font-size:14.5px; font-weight:700; color:var(--crm-text); display:flex; align-items:center; gap:8px;">
                        <span>🏢 2. Danh sách Công ty Con trực thuộc (<span id="modalChildrenCount">0</span>)</span>
                    </h4>

                    <div id="modalChildrenList" style="margin-bottom:14px;">
                        <!-- Rendered dynamically -->
                    </div>

                    <!-- Search to add new child -->
                    <div class="corporate-search-wrap">
                        <label style="display:block; font-size:12.5px; font-weight:600; color:var(--crm-text); margin-bottom:6px;">
                            Thêm công ty con vào dưới trướng doanh nghiệp này:
                        </label>
                        <div class="corporate-search-input-box">
                            <svg class="corporate-search-icon" viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></svg>
                            <input id="inputSearchChild" class="crm-input" type="search" placeholder="Tìm công ty để gán làm công ty con..." autocomplete="off">
                        </div>
                        <div id="dropdownChildResults" class="corporate-dropdown-results"></div>
                    </div>
                </div>
            </div>

            <footer class="corporate-modal-footer">
                <button type="button" class="crm-btn crm-btn-secondary" onclick="window.CorporateHierarchyManager.closeHierarchyModal()">
                    Đóng
                </button>
            </footer>
        `;

        document.body.appendChild(overlay);
        document.body.appendChild(dialog);

        // Bind Search Parent autocomplete
        const inputParent = dialog.querySelector("#inputSearchParent");
        const dropParent = dialog.querySelector("#dropdownParentResults");
        let debounceTimerP = null;

        inputParent?.addEventListener("input", () => {
            clearTimeout(debounceTimerP);
            debounceTimerP = setTimeout(async () => {
                const q = inputParent.value.trim();
                const results = await apiSearchCustomers(q);
                renderParentSearchResults(results, dropParent);
            }, 250);
        });

        inputParent?.addEventListener("focus", async () => {
            if (!dropParent.children.length) {
                const results = await apiSearchCustomers(inputParent.value.trim());
                renderParentSearchResults(results, dropParent);
            }
            dropParent.style.display = "block";
        });

        // Bind Search Child autocomplete
        const inputChild = dialog.querySelector("#inputSearchChild");
        const dropChild = dialog.querySelector("#dropdownChildResults");
        let debounceTimerC = null;

        inputChild?.addEventListener("input", () => {
            clearTimeout(debounceTimerC);
            debounceTimerC = setTimeout(async () => {
                const q = inputChild.value.trim();
                const results = await apiSearchCustomers(q);
                renderChildSearchResults(results, dropChild);
            }, 250);
        });

        inputChild?.addEventListener("focus", async () => {
            if (!dropChild.children.length) {
                const results = await apiSearchCustomers(inputChild.value.trim());
                renderChildSearchResults(results, dropChild);
            }
            dropChild.style.display = "block";
        });

        // Close dropdowns on click outside
        document.addEventListener("click", e => {
            if (!e.target.closest(".corporate-search-wrap")) {
                if (dropParent) dropParent.style.display = "none";
                if (dropChild) dropChild.style.display = "none";
            }
        });

        // Unbind parent button
        dialog.querySelector("#btnUnbindParent")?.addEventListener("click", async () => {
            if (!currentModalTargetId) return;
            try {
                await apiUpdateParent(currentModalTargetId, null);
                showToast("Đã gỡ bỏ công ty mẹ thành công!", "success");
                openHierarchyModal(currentModalTargetId);
            } catch (err) {
                showToast(err.message || "Lỗi gỡ bỏ công ty mẹ", "danger");
            }
        });

        overlay.addEventListener("click", closeHierarchyModal);
    }

    function renderParentSearchResults(list, dropdownEl) {
        if (!dropdownEl || !currentModalTargetId) return;
        dropdownEl.innerHTML = "";

        if (!list || !list.length) {
            dropdownEl.innerHTML = '<div style="padding:14px; text-align:center; color:var(--crm-muted); font-size:13px;">Không tìm thấy doanh nghiệp phù hợp.</div>';
            dropdownEl.style.display = "block";
            return;
        }

        const hier = getHierarchyStorage();
        const descendants = getAllDescendantIds(currentModalTargetId, hier);

        dropdownEl.innerHTML = list.map(c => {
            const isSelf = Number(c.id) === Number(currentModalTargetId);
            const isDescendant = descendants.has(Number(c.id));
            const isDisabled = isSelf || isDescendant;

            let reasonTag = "";
            if (isSelf) reasonTag = '<span style="color:var(--crm-danger); font-size:11px; font-weight:700;">(Chính công ty này - Chặn Self-parent)</span>';
            else if (isDescendant) reasonTag = '<span style="color:var(--crm-danger); font-size:11px; font-weight:700;">(Đang là cty con/cháu - Chặn tạo vòng lặp)</span>';

            return `
                <div class="corporate-dropdown-item ${isDisabled ? 'disabled' : ''}" data-cand-id="${c.id}" ${isDisabled ? 'title="Không thể chọn do vi phạm logic phân cấp"' : ''}>
                    <div>
                        <div class="corporate-item-title">
                            🏢 ${escapeHtml(c.companyName || c.name)} ${reasonTag}
                        </div>
                        <div class="corporate-item-meta">
                            <span>MST: <code>${escapeHtml(c.taxCode || '—')}</code></span>
                            <span>👤 ${escapeHtml(c.ownerName || c.owner || '—')}</span>
                        </div>
                    </div>
                    ${!isDisabled ? `
                        <button type="button" class="crm-btn crm-btn-primary" style="padding:3px 10px; font-size:12px; height:auto;">
                            Chọn làm Mẹ
                        </button>
                    ` : ''}
                </div>
            `;
        }).join("");

        dropdownEl.querySelectorAll(".corporate-dropdown-item:not(.disabled)").forEach(item => {
            item.addEventListener("click", async () => {
                const candId = Number(item.dataset.candId);
                try {
                    await apiUpdateParent(currentModalTargetId, candId);
                    showToast("Đã gán công ty mẹ thành công!", "success");
                    dropdownEl.style.display = "none";
                    openHierarchyModal(currentModalTargetId);
                } catch (err) {
                    showToast(err.message || "Lỗi khi gán công ty mẹ", "danger");
                }
            });
        });

        dropdownEl.style.display = "block";
    }

    function renderChildSearchResults(list, dropdownEl) {
        if (!dropdownEl || !currentModalTargetId) return;
        dropdownEl.innerHTML = "";

        if (!list || !list.length) {
            dropdownEl.innerHTML = '<div style="padding:14px; text-align:center; color:var(--crm-muted); font-size:13px;">Không tìm thấy doanh nghiệp phù compliance.</div>';
            dropdownEl.style.display = "block";
            return;
        }

        const hier = getHierarchyStorage();
        const rootOfCurrent = getRootParentId(currentModalTargetId, hier);

        dropdownEl.innerHTML = list.map(c => {
            const isSelf = Number(c.id) === Number(currentModalTargetId);
            // Candidate cannot be current company's ancestor
            const isAncestor = Number(c.id) === rootOfCurrent;
            const isCurrentChild = Number(hier.parents[c.id]) === Number(currentModalTargetId);
            const isDisabled = isSelf || isAncestor || isCurrentChild;

            let tag = "";
            if (isSelf) tag = '<span style="color:var(--crm-danger); font-size:11px; font-weight:700;">(Chính công ty này)</span>';
            else if (isAncestor) tag = '<span style="color:var(--crm-danger); font-size:11px; font-weight:700;">(Đang là cấp trên - Tránh lặp)</span>';
            else if (isCurrentChild) tag = '<span style="color:var(--crm-success); font-size:11px; font-weight:700;">(Đã là công ty con)</span>';

            return `
                <div class="corporate-dropdown-item ${isDisabled ? 'disabled' : ''}" data-child-id="${c.id}">
                    <div>
                        <div class="corporate-item-title">
                            🏢 ${escapeHtml(c.companyName || c.name)} ${tag}
                        </div>
                        <div class="corporate-item-meta">
                            <span>MST: <code>${escapeHtml(c.taxCode || '—')}</code></span>
                            <span>👤 ${escapeHtml(c.ownerName || c.owner || '—')}</span>
                        </div>
                    </div>
                    ${!isDisabled ? `
                        <button type="button" class="crm-btn crm-btn-secondary" style="padding:3px 10px; font-size:12px; height:auto;">
                            + Gán làm Con
                        </button>
                    ` : ''}
                </div>
            `;
        }).join("");

        dropdownEl.querySelectorAll(".corporate-dropdown-item:not(.disabled)").forEach(item => {
            item.addEventListener("click", async () => {
                const childId = Number(item.dataset.childId);
                try {
                    await apiUpdateParent(childId, currentModalTargetId);
                    showToast("Đã thêm công ty con thành công!", "success");
                    dropdownEl.style.display = "none";
                    openHierarchyModal(currentModalTargetId);
                } catch (err) {
                    showToast(err.message || "Lỗi khi gán công ty con", "danger");
                }
            });
        });

        dropdownEl.style.display = "block";
    }

    function openHierarchyModal(customerId) {
        buildHierarchyModalDOM();
        currentModalTargetId = Number(customerId);
        const dialog = document.getElementById("corporateHierarchyModal");
        const overlay = document.getElementById("corporateHierarchyModalOverlay");
        if (!dialog || !overlay) return;

        const cust = getCustomerById(currentModalTargetId);
        if (!cust) return;

        const hier = getHierarchyStorage();
        const parentId = hier.parents[currentModalTargetId];
        const isChild = !!parentId;

        // Populate Target Info
        dialog.querySelector("#modalTargetName").textContent = cust.companyName || cust.name;
        dialog.querySelector("#modalTargetTax").textContent = cust.taxCode || "—";
        dialog.querySelector("#modalTargetOwner").textContent = cust.ownerName || cust.owner || "—";

        const roleBadgeEl = dialog.querySelector("#modalTargetRoleBadge");
        if (roleBadgeEl) {
            roleBadgeEl.className = `corporate-role-badge ${isChild ? 'corporate-role-child' : 'corporate-role-parent'}`;
            roleBadgeEl.textContent = isChild ? "Công ty Con" : "Công ty Mẹ / Độc lập";
        }

        // Current Parent Box
        const pNameEl = dialog.querySelector("#currentParentName");
        const pMetaEl = dialog.querySelector("#currentParentMeta");
        const unbindBtn = dialog.querySelector("#btnUnbindParent");

        if (parentId) {
            const parentComp = getCustomerById(parentId);
            pNameEl.textContent = parentComp ? (parentComp.companyName || parentComp.name) : `Công ty Mẹ #${parentId}`;
            pMetaEl.textContent = `MST: ${parentComp?.taxCode || '—'} · Phụ trách: ${parentComp?.ownerName || '—'}`;
            if (unbindBtn) unbindBtn.style.display = "inline-flex";
        } else {
            pNameEl.textContent = "Chưa có công ty mẹ (Đang là Đơn vị độc lập / Trụ sở chính)";
            pMetaEl.textContent = "Bạn có thể tìm kiếm một công ty mẹ phía dưới để gán liên kết.";
            if (unbindBtn) unbindBtn.style.display = "none";
        }

        // Children List
        const childrenListEl = dialog.querySelector("#modalChildrenList");
        const countBadge = dialog.querySelector("#modalChildrenCount");
        const childIds = [];
        for (const [cIdStr, pId] of Object.entries(hier.parents)) {
            if (Number(pId) === Number(currentModalTargetId)) {
                childIds.push(Number(cIdStr));
            }
        }

        if (countBadge) countBadge.textContent = String(childIds.length);

        if (childIds.length === 0) {
            childrenListEl.innerHTML = '<div style="padding:12px; background:var(--crm-surface-soft); border-radius:8px; font-size:13px; color:var(--crm-muted); text-align:center;">Chưa có công ty con nào trực thuộc đơn vị này.</div>';
        } else {
            childrenListEl.innerHTML = childIds.map(chId => {
                const childComp = getCustomerById(chId);
                const name = childComp ? (childComp.companyName || childComp.name) : `Công ty con #${chId}`;
                const tax = childComp?.taxCode || "—";
                const owner = childComp?.ownerName || "—";

                return `
                    <div style="display:flex; justify-content:space-between; align-items:center; padding:10px 14px; background:var(--crm-surface); border:1px solid var(--crm-border); border-radius:8px; margin-bottom:8px;">
                        <div>
                            <div style="font-weight:700; font-size:13.5px; color:var(--crm-text);">🏢 ${escapeHtml(name)}</div>
                            <div style="font-size:12px; color:var(--crm-muted);">MST: ${escapeHtml(tax)} · Phụ trách: ${escapeHtml(owner)}</div>
                        </div>
                        <button type="button" class="crm-btn crm-btn-secondary" onclick="window.CorporateHierarchyManager.promptUnbindParent(${chId})" style="color:var(--crm-danger); font-size:12px; padding:3px 8px; height:auto;" title="Gỡ công ty con">
                            ❌ Gỡ liên kết
                        </button>
                    </div>
                `;
            }).join("");
        }

        // Reset Inputs
        const inputParent = dialog.querySelector("#inputSearchParent");
        const inputChild = dialog.querySelector("#inputSearchChild");
        if (inputParent) inputParent.value = "";
        if (inputChild) inputChild.value = "";
        dialog.querySelector("#dropdownParentResults").style.display = "none";
        dialog.querySelector("#dropdownChildResults").style.display = "none";
        dialog.querySelector("#hierCycleAlert").style.display = "none";

        overlay.style.display = "block";
        dialog.style.display = "flex";
    }

    function closeHierarchyModal() {
        const dialog = document.getElementById("corporateHierarchyModal");
        const overlay = document.getElementById("corporateHierarchyModalOverlay");
        if (dialog) dialog.style.display = "none";
        if (overlay) overlay.style.display = "none";
    }

    /* =========================================================
       8. MODAL: FULLSCREEN HIERARCHY TREE MODAL
    ========================================================= */
    const collapsedNodesSet = new Set();

    function buildTreeModalDOM() {
        if (document.getElementById("corporateTreeModal")) return;

        const overlay = document.createElement("div");
        overlay.id = "corporateTreeModalOverlay";
        overlay.className = "corporate-modal-overlay";

        const dialog = document.createElement("div");
        dialog.id = "corporateTreeModal";
        dialog.className = "corporate-modal-dialog modal-xl";
        dialog.setAttribute("role", "dialog");
        dialog.setAttribute("aria-labelledby", "treeModalTitle");

        dialog.innerHTML = `
            <header class="corporate-modal-header">
                <div>
                    <span class="group-kpi-tag" style="margin-bottom:6px;">SƠ ĐỒ PHÂN CẤP DOANH NGHIỆP TRỰC QUAN (CRM-65)</span>
                    <h2 id="treeModalTitle">Cây Cấu Trúc Tổ Chức Tập Đoàn</h2>
                    <p>Sơ đồ phân nhánh Mẹ - Con - Cháu kèm giá trị hợp đồng và người phụ trách từng pháp nhân.</p>
                </div>
                <button type="button" class="corporate-modal-close" onclick="window.CorporateHierarchyManager.closeTreeModal()" aria-label="Đóng">✕</button>
            </header>

            <div class="corporate-modal-body" style="background:var(--crm-bg); padding:20px;">
                <div class="corporate-tree-container">
                    <div id="treeModalCanvas" class="tree-view-wrapper">
                        <!-- Rendered dynamically -->
                    </div>
                </div>
            </div>

            <footer class="corporate-modal-footer">
                <button type="button" class="crm-btn crm-btn-secondary" onclick="window.CorporateHierarchyManager.expandAllNodes()">
                    ➕ Mở rộng tất cả nhánh
                </button>
                <button type="button" class="crm-btn crm-btn-primary" onclick="window.CorporateHierarchyManager.closeTreeModal()">
                    Đóng
                </button>
            </footer>
        `;

        document.body.appendChild(overlay);
        document.body.appendChild(dialog);
        overlay.addEventListener("click", closeTreeModal);
    }

    async function openTreeModal(rootCustomerId) {
        buildTreeModalDOM();
        const dialog = document.getElementById("corporateTreeModal");
        const overlay = document.getElementById("corporateTreeModalOverlay");
        const canvas = document.getElementById("treeModalCanvas");
        if (!dialog || !overlay || !canvas) return;

        const tree = await apiGetHierarchy(rootCustomerId);
        if (!tree) {
            canvas.innerHTML = '<div style="padding:40px; text-align:center; color:var(--crm-muted);">Không tìm thấy thông tin cấu trúc phân cấp.</div>';
        } else {
            canvas.innerHTML = renderHierarchyTreeHTML(tree, rootCustomerId, collapsedNodesSet);
        }

        overlay.style.display = "block";
        dialog.style.display = "flex";
    }

    function closeTreeModal() {
        const dialog = document.getElementById("corporateTreeModal");
        const overlay = document.getElementById("corporateTreeModalOverlay");
        if (dialog) dialog.style.display = "none";
        if (overlay) overlay.style.display = "none";
    }

    function toggleTreeNode(nodeId) {
        const id = Number(nodeId);
        if (collapsedNodesSet.has(id)) {
            collapsedNodesSet.delete(id);
        } else {
            collapsedNodesSet.add(id);
        }
        // Re-render open tree modal if open
        const modal = document.getElementById("corporateTreeModal");
        if (modal && modal.style.display === "flex") {
            const rootId = Number(modal.dataset.rootId || 1);
            openTreeModal(rootId);
        }
        // Refresh 360 page if tree is embedded
        const params = new URLSearchParams(window.location.search);
        const cId = params.get("id");
        if (cId) {
            renderCustomer360Hierarchy(document.getElementById("customer360HierarchyBanner"), cId);
        }
    }

    function expandAllNodes() {
        collapsedNodesSet.clear();
        const modal = document.getElementById("corporateTreeModal");
        if (modal && modal.style.display === "flex") {
            const rootId = Number(modal.dataset.rootId || 1);
            openTreeModal(rootId);
        }
    }

    async function promptUnbindParent(customerId) {
        const cust = getCustomerById(customerId);
        const name = cust ? (cust.companyName || cust.name) : `Khách hàng #${customerId}`;
        if (confirm(`Bạn có chắc chắn muốn gỡ bỏ "${name}" khỏi công ty mẹ / tập đoàn không?`)) {
            try {
                await apiUpdateParent(customerId, null);
                showToast(`Đã gỡ "${name}" khỏi tập đoàn thành công!`, "success");
                if (currentModalTargetId) openHierarchyModal(currentModalTargetId);
                const params = new URLSearchParams(window.location.search);
                const pageCustId = params.get("id");
                if (pageCustId) renderCustomer360Hierarchy(document.getElementById("customer360HierarchyBanner"), pageCustId);
            } catch (err) {
                showToast(err.message || "Lỗi khi gỡ quan hệ mẹ-con", "danger");
            }
        }
    }

    /* =========================================================
       9. INTEGRATION: CUSTOMERS LIST PAGE (CUSTOMERS.HTML)
    ========================================================= */
    function initCustomersPageIntegration() {
        // Wire up custom parent selector field in Customer Drawer form if present
        const formGroupOwner = document.getElementById("groupOwner");
        if (formGroupOwner && !document.getElementById("groupParentCompany")) {
            const parentFieldDiv = document.createElement("div");
            parentFieldDiv.id = "groupParentCompany";
            parentFieldDiv.className = "field-group form-col-span-2";
            parentFieldDiv.innerHTML = `
                <label class="field-label" for="customerParentSelect">
                    Công ty mẹ / Trực thuộc tập đoàn (CRM-65)
                </label>
                <div style="display:flex; gap:8px;">
                    <select id="customerParentSelect" class="crm-input" style="flex:1;">
                        <option value="">-- Đơn vị độc lập (Không có công ty mẹ) --</option>
                    </select>
                    <button type="button" id="btnOpenHierarchyFromDrawer" class="crm-btn crm-btn-secondary" style="white-space:nowrap;" title="Quản lý chi tiết cây phân cấp">
                        🌳 Cây phân cấp
                    </button>
                </div>
                <span class="field-helper">Chọn công ty mẹ để gộp doanh thu và quản lý cấu trúc tổ chức tập đoàn.</span>
            `;
            formGroupOwner.parentNode.insertBefore(parentFieldDiv, formGroupOwner.nextSibling);

            populateCustomerParentDropdown();

            document.getElementById("btnOpenHierarchyFromDrawer")?.addEventListener("click", () => {
                const currentEditId = window.currentEditingCustomerId || 1;
                openHierarchyModal(currentEditId);
            });
        }
    }

    function populateCustomerParentDropdown() {
        const select = document.getElementById("customerParentSelect");
        if (!select) return;

        const all = getLocalCustomersList();
        const currentEditId = window.currentEditingCustomerId ? Number(window.currentEditingCustomerId) : null;
        const hier = getHierarchyStorage();
        const descendants = currentEditId ? getAllDescendantIds(currentEditId, hier) : new Set();

        select.innerHTML = '<option value="">-- Đơn vị độc lập (Không có công ty mẹ) --</option>';
        all.forEach(c => {
            const cId = Number(c.id);
            if (currentEditId && (cId === currentEditId || descendants.has(cId))) {
                // Skip self or descendants to prevent cycle
                return;
            }
            const opt = document.createElement("option");
            opt.value = c.id;
            opt.textContent = `${c.companyName || c.name} (${c.taxCode || 'No MST'})`;
            select.appendChild(opt);
        });

        if (currentEditId && hier.parents[currentEditId]) {
            select.value = String(hier.parents[currentEditId]);
        }
    }

    /* =========================================================
       10. BOOTSTRAP & EVENT LISTENERS
    ========================================================= */
    function init() {
        // Detect Customer 360 page
        const isCustomer360 = window.location.pathname.includes("customer-360.html") || !!document.querySelector(".customer360");
        const params = new URLSearchParams(window.location.search);
        const cId = params.get("id");

        if (isCustomer360 && cId) {
            renderCustomer360Hierarchy(document.getElementById("customer360HierarchyBanner"), cId);
        }

        // Detect Customers List page
        const isCustomersPage = window.location.pathname.includes("customers.html") || !!document.querySelector(".customer-page");
        if (isCustomersPage) {
            initCustomersPageIntegration();
        }

        // Listen for hierarchy updates
        document.addEventListener("crm:hierarchy-updated", e => {
            if (isCustomer360 && cId) {
                renderCustomer360Hierarchy(document.getElementById("customer360HierarchyBanner"), cId);
            }
            if (isCustomersPage) {
                populateCustomerParentDropdown();
            }
        });
    }

    // Export Global API
    window.CorporateHierarchyManager = {
        apiSearchCustomers,
        apiGetHierarchy,
        apiUpdateParent,
        apiGetGroupSummary,
        validateParentAssignment,
        openHierarchyModal,
        closeHierarchyModal,
        openTreeModal,
        closeTreeModal,
        toggleTreeNode,
        expandAllNodes,
        promptUnbindParent,
        renderCustomer360Hierarchy,
        getHierarchyStorage
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
