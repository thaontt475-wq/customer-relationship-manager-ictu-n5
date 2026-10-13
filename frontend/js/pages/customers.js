/**
 * ===================================================================
 * ENTERPRISE B2B CUSTOMER MANAGEMENT CONTROLLER (CRM-61 & CRM-64)
 * - Quản lý hồ sơ khách hàng doanh nghiệp, tra cứu MST, vòng đời
 * - CRM-64: Phát hiện và cảnh báo trùng lặp (MST, Website, Tên fuzzy)
 * - CRM-64: Giao diện so sánh đối chiếu 2 cột (Side-by-Side Merge Modal)
 * - CRM-64: Phân quyền gộp dữ liệu: Trưởng nhóm kinh doanh / Admin vs Sale Rep
 * - Dữ liệu nghiệp vụ và phân quyền lấy từ Backend API
 * ===================================================================
 */

"use strict";

(function () {
    const API_BASE = (window.CRM_BACKEND_BASE || `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`);

    // Master Catalogues (Categories)
    const INDUSTRIES = [];
    const COMPANY_SIZES = [];

    const DEFAULT_INDUSTRIES = [
        { id: 1, code: "CNTT", name: "Công nghệ thông tin" },
        { id: 2, code: "RETAIL", name: "Bán lẻ / Thương mại" },
        { id: 3, code: "MANUFACTURING", name: "Sản xuất / Chế tạo" },
        { id: 4, code: "FINANCE", name: "Tài chính / Ngân hàng / Bảo hiểm" },
        { id: 5, code: "REAL_ESTATE", name: "Bất động sản / Xây dựng" },
        { id: 6, code: "LOGISTICS", name: "Vận tải / Kho bãi / Logistics" },
        { id: 7, code: "HEALTHCARE", name: "Y tế / Dược phẩm" },
        { id: 8, code: "EDUCATION", name: "Giáo dục / Đào tạo" },
        { id: 9, code: "FNB", name: "Thực phẩm / Đồ uống (F&B)" },
        { id: 10, code: "SERVICES", name: "Dịch vụ / Du lịch / Khách sạn" },
        { id: 11, code: "OTHER", name: "Ngành nghề khác" }
    ];

    const DEFAULT_COMPANY_SIZES = [
        { id: 1, code: "MICRO", name: "Dưới 10 nhân sự (Siêu nhỏ)" },
        { id: 2, code: "SMALL", name: "10 - 50 nhân sự (Nhỏ)" },
        { id: 3, code: "MEDIUM", name: "50 - 200 nhân sự (Vừa)" },
        { id: 4, code: "LARGE", name: "200 - 500 nhân sự (Lớn)" },
        { id: 5, code: "ENTERPRISE", name: "Trên 500 nhân sự (Tập đoàn)" }
    ];



    function getUserId(user) {
        if (!user) return null;
        return user.id || user.userId || null;
    }







    /**
     * Xác định phạm vi dữ liệu tối đa của người dùng: SELF, TEAM, hoặc ALL
     * Ngăn chặn hoàn toàn hiển thị dữ liệu vượt quyền
     */
    function resolveEffectiveScope() { return currentScope; }

    /**
     * Lọc danh sách khách hàng đảm bảo không vượt quá phạm vi SELF/TEAM/ALL của người dùng
     */


    /**
     * Đồng bộ giao diện phân quyền phạm vi: khóa các option vượt quyền
     */
    function applyUserScopePermissions(user) {
        const maxScope = resolveEffectiveScope(user);
        currentScope = maxScope;

        // Cập nhật thẻ hiển thị phạm vi toàn cục
        const scopeGlobalBadge = document.getElementById("scopeGlobalBadge");
        if (scopeGlobalBadge) {
            scopeGlobalBadge.textContent = `Phạm vi: ${maxScope}`;
            scopeGlobalBadge.className = `scope-badge scope-${maxScope.toLowerCase()}`;
        }

        // Khóa các tùy chọn phạm vi vượt quá quyền người dùng
        const scopeSelect = document.getElementById("scopeFilter");
        if (scopeSelect) {
            Array.from(scopeSelect.options).forEach(opt => {
                const val = opt.value;
                if (maxScope === "SELF") {
                    opt.disabled = val !== "SELF";
                } else if (maxScope === "TEAM") {
                    opt.disabled = val === "ALL";
                } else {
                    opt.disabled = val === "TEAM";
                }
            });

            if (maxScope === "SELF") {
                scopeSelect.value = "SELF";
                state.scopeFilter = "SELF";
            } else if (maxScope === "TEAM") {
                if (state.scopeFilter === "ALL") state.scopeFilter = "TEAM";
                scopeSelect.value = state.scopeFilter;
            } else {
                scopeSelect.value = state.scopeFilter || "ALL";
            }
        } else {
            if (maxScope === "SELF") state.scopeFilter = "SELF";
            else if (maxScope === "TEAM" && state.scopeFilter === "ALL") state.scopeFilter = "TEAM";
        }
    }

    // State
    let customersList = [];
    let searchSequence = 0;
    let usersList = [];
    let currentSessionUser = null;
    let currentScope = "ALL";

    // CRM-64 State
    let duplicatesMap = new Map();
    let isDuplicateOnlyFilter = false;
    let currentMergeSession = null;

    let state = {
        keyword: "",
        status: "",
        industryId: "",
        companySizeId: "",
        region: "",
        ownerUserId: "",
        scopeFilter: "ALL",
        page: 1,
        pageSize: 20,
        totalItems: 0,
        totalPages: 1,
        sortField: "name",
        sortOrder: "desc", // 'asc' or 'desc'
        selectedIds: new Set()
    };

    let debounceTimer = null;
    let quickActionCustomer = null;

    // Elements cache
    const tableBody = document.getElementById("customerTableBody");
    const emptyState = document.getElementById("customerEmpty");
    const loadingOverlay = document.getElementById("tableLoading");
    const searchInput = document.getElementById("customerSearch");
    const statusFilter = document.getElementById("statusFilter");
    const industryFilter = document.getElementById("industryFilter");
    const scopeFilter = document.getElementById("scopeFilter");
    const pageSizeSelect = document.getElementById("pageSizeSelect");
    const checkAllRows = document.getElementById("checkAllRows");
    const paginationControls = document.getElementById("paginationControls");
    const paginationInfo = document.getElementById("paginationInfo");

    const drawer = document.getElementById("customerDrawer");
    const drawerOverlay = document.getElementById("customerDrawerOverlay");
    const customerForm = document.getElementById("customerForm");
    const drawerTitle = document.getElementById("drawerTitle");
    const openDrawerBtn = document.getElementById("openCustomerDrawer");
    const closeDrawerBtn = document.getElementById("closeCustomerDrawer");
    const cancelDrawerBtn = document.getElementById("cancelCustomer");

    // Quick Status Modal
    const quickModal = document.getElementById("quickStatusModal");
    const quickModalOverlay = document.getElementById("quickStatusModalOverlay");
    const quickModalName = document.getElementById("quickModalCustomerName");
    const quickModalSelect = document.getElementById("quickModalStatusSelect");
    const btnCancelQuickStatus = document.getElementById("btnCancelQuickStatus");
    const btnConfirmQuickStatus = document.getElementById("btnConfirmQuickStatus");

    // CRM-64: Duplicate Merge Modal Elements
    const mergeModal = document.getElementById("mergeComparisonModal");
    const mergeModalOverlay = document.getElementById("mergeComparisonModalOverlay");
    const btnCloseMergeModal = document.getElementById("btnCloseMergeModal");
    const btnCancelMerge = document.getElementById("btnCancelMerge");
    const btnConfirmMerge = document.getElementById("btnConfirmMerge");
    const roleSimulatorSelect = document.getElementById("roleSimulatorSelect");
    const btnQuickDuplicates = document.getElementById("btnQuickDuplicates");
    const badgeDuplicatesCount = document.getElementById("badgeDuplicatesCount");

    const radioSelectMasterA = document.getElementById("radioSelectMasterA");
    const radioSelectMasterB = document.getElementById("radioSelectMasterB");
    const cardRecordA = document.getElementById("cardRecordA");
    const cardRecordB = document.getElementById("cardRecordB");

    /* =========================================================
       1. API HELPER & CLIENT-SIDE LOCALSTORAGE FALLBACK
    ========================================================= */
    async function apiRequest(path, options = {}) {
        return window.CrmApi.request(path, options);
    }

    /* =========================================================
       2. INITIALIZATION (SCOPE-AWARE INSTANT RENDER & SYNC)
    ========================================================= */
    async function init() {
        bindEvents();
        window.addEventListener("crm:customers-imported",loadCustomers);
        setLoading(true);
        try {
            await loadSession();
            await loadUsers();
            try { await window.DuplicateMergeEngine?.load(); } catch(error) { showToast(error.message,"error"); }
            await syncBackendCustomers();
        } catch (error) { renderErrorState(error.message); }
        finally { setLoading(false); }
        registerSpaCleanups();
    }

    function registerSpaCleanups() {
        const cleanupFn = () => {
            if (debounceTimer) {
                clearTimeout(debounceTimer);
                debounceTimer = null;
            }
            closeDrawer();
            closeQuickStatus();
            closeMergeModal();
        };

        window.__CRM_PAGE_CLEANUP__ = cleanupFn;
        if (typeof window.crmAddPageCleanup === "function") {
            window.crmAddPageCleanup(cleanupFn);
        }
        window.addEventListener("crmPageDestroy", cleanupFn, { once: true });
    }

    function populateSelectOptions() {
        // Industry Filter & Drawer Select
        const industrySelect = document.getElementById("industrySelect");
        if (industryFilter) {
            industryFilter.innerHTML = '<option value="">Tất cả ngành nghề</option>';
            INDUSTRIES.forEach(item => {
                const opt = document.createElement("option");
                opt.value = String(item.id);
                opt.textContent = item.name;
                industryFilter.appendChild(opt);
            });
        }
        if (industrySelect) {
            industrySelect.innerHTML = '<option value="">-- Chọn ngành nghề --</option>';
            INDUSTRIES.forEach(item => {
                const opt = document.createElement("option");
                opt.value = String(item.id);
                opt.textContent = item.name;
                industrySelect.appendChild(opt);
            });
        }

        // Company Size Select
        const companySizeSelect = document.getElementById("companySizeSelect");
        if (companySizeSelect) {
            companySizeSelect.innerHTML = '<option value="">-- Chọn quy mô nhân sự --</option>';
            COMPANY_SIZES.forEach(item => {
                const opt = document.createElement("option");
                opt.value = String(item.id);
                opt.textContent = item.name;
                companySizeSelect.appendChild(opt);
            });
        }
    }

    async function loadSession() {
        currentSessionUser = await window.CrmApi.session();
        applyUserScopePermissions(currentSessionUser);
        if (openDrawerBtn) openDrawerBtn.hidden = !currentSessionUser.permissions.includes("customer.create");
        const importButton = document.getElementById("btnImportExcel");
        if (importButton) importButton.hidden = !currentSessionUser.permissions.includes("customer.create");
        return currentSessionUser;
    }

    async function loadUsers() {
        const data = await apiRequest("/api/customer-lookups");
        const industries = (data.industries && data.industries.length > 0) ? data.industries : DEFAULT_INDUSTRIES;
        const companySizes = (data.companySizes && data.companySizes.length > 0) ? data.companySizes : DEFAULT_COMPANY_SIZES;
        INDUSTRIES.splice(0, INDUSTRIES.length, ...industries);
        COMPANY_SIZES.splice(0, COMPANY_SIZES.length, ...companySizes);
        usersList = data.owners || [];
        currentScope = data.scope || "ALL";
        applyUserScopePermissions(currentSessionUser);
        populateSelectOptions();
        const ownerSelect = document.getElementById("ownerSelect");
        if (ownerSelect) ownerSelect.innerHTML = '<option value="">Bản thân tôi</option>' + usersList.map(u => `<option value="${u.id}">${escapeHtml(u.fullName)}</option>`).join("");
        window.CustomerFilterManager?.populateOwners(usersList);
        window.CustomerFilterManager?.populateCatalogs?.({ ...data, industries, companySizes });
    }

    /* =========================================================
       3. LOAD & RENDER CUSTOMERS (WITH DUPLICATE SCANNING)
    ========================================================= */
    async function syncBackendCustomers() {
        const sequence = ++searchSequence;
        setLoading(true);
        try {
            const params = new URLSearchParams({page:state.page, size:state.pageSize});
            for (const [key,value] of Object.entries({keyword:state.keyword,status:state.status,industryId:state.industryId,companySizeId:state.companySizeId,region:state.region,ownerId:state.scopeFilter === "SELF" ? currentSessionUser.id : state.ownerUserId})) {
                if (value) params.set(key, value);
            }
            const data = await apiRequest(`/api/customers?${params}`);
            if (sequence !== searchSequence) return;
            customersList = data.items;
            state.totalItems = data.totalItems;
            state.totalPages = Math.max(1, data.totalPages);
            scanDuplicates();
            filterAndRenderTable(true);
            updateKpiStats();
        } catch (error) {
            if (sequence === searchSequence) { customersList = []; renderErrorState(error.message); }
        } finally { if (sequence === searchSequence) setLoading(false); }
    }

    async function loadCustomers() {
        try { await window.DuplicateMergeEngine?.load(); } catch(error) { showToast(error.message,"error"); }
        await syncBackendCustomers();
    }

    /**
     * Tự động quét và phát hiện trùng lặp trên toàn bộ danh sách khách hàng
     */
    function scanDuplicates() {
        if (!window.DuplicateMergeEngine) return;
        duplicatesMap = window.DuplicateMergeEngine.scanAllDuplicates(customersList);

        // Cập nhật số lượng lên Badge nút xem nhanh trên thanh công cụ
        const dupCount = customersList.filter(record => duplicatesMap.has(Number(record.id))).length;
        if (badgeDuplicatesCount) {
            badgeDuplicatesCount.textContent = String(dupCount);
        }
    }

    function filterAndRenderTable(renderOnly = false) {
        if (!renderOnly) { syncBackendCustomers(); return; }
        let displayList = customersList;
        if (isDuplicateOnlyFilter) displayList = displayList.filter(c => duplicatesMap.has(Number(c.id)));
        window.CustomerFilterManager?.updateMatchCount(isDuplicateOnlyFilter ? displayList.length : state.totalItems, state.totalItems);
        renderTableRows(displayList);
        if (!currentSessionUser.permissions.includes("customer.update")) tableBody.querySelectorAll(".btn-edit,[data-quick-status]").forEach(el=>{if(el.matches("button"))el.remove();else {el.removeAttribute("data-quick-status");el.style.cursor="default";}});
        if (!currentSessionUser.permissions.includes("customer.delete")) tableBody.querySelectorAll(".btn-delete").forEach(el=>el.remove());
        renderPagination();
        if (isDuplicateOnlyFilter && paginationInfo) paginationInfo.textContent = `Trang ${state.page}/${state.totalPages}: ${displayList.length} khách trùng lặp trong ${customersList.length} khách của trang (tổng bộ lọc ${state.totalItems}).`;
    }

    function renderTableRows(items) {
        if (!tableBody) return;
        tableBody.innerHTML = "";

        if (items.length === 0) {
            if (emptyState) emptyState.style.display = "block";
            return;
        }

        if (emptyState) emptyState.style.display = "none";

        items.forEach(record => {
            const tr = document.createElement("tr");
            const isSelected = state.selectedIds.has(record.id);
            if (isSelected) tr.classList.add("row-selected");

            // Resolve industry & size names
            const industry = INDUSTRIES.find(i => Number(i.id) === Number(record.industryId))?.name || "Chưa phân loại";
            const companySize = COMPANY_SIZES.find(s => Number(s.id) === Number(record.companySizeId))?.name || "Chưa xác định";

            // Determine Scope Tag (SELF vs TEAM)
            const isSelf = currentSessionUser?.id && Number(record.ownerUserId) === Number(currentSessionUser.id);
            const scopeTag = isSelf
                ? `<span class="scope-badge scope-self">Cá nhân (SELF)</span>`
                : `<span class="scope-badge scope-team">Phụ trách khác</span>`;

            // CRM-68: Churn Risk Flagging
            const churnInfo = record.churnRisk;
            const churnBadge = churnInfo?.isRisk
                ? `<span class="churn-risk-badge" title="${escapeHtml(churnInfo.reasons?.join(' • ') || 'Khách hàng có rủi ro rời bỏ')}" style="cursor:help;">
                     ⚠️ Rủi ro rời bỏ
                   </span>`
                : "";

// Kiểm tra trùng lặp cho bản ghi này (CRM-64)
            const dupInfo = duplicatesMap.get(Number(record.id));
            let duplicateBadgeHtml = "";
            let mergeActionBtnHtml = "";

            if (dupInfo && dupInfo.isDuplicate && dupInfo.matches.length > 0) {
                const primaryMatch = dupInfo.matches[0];
                const matchCompany = primaryMatch.record.companyName || primaryMatch.record.name;
                const matchReasonSummary = primaryMatch.reasons.join(" • ");

                duplicateBadgeHtml = `
                    <button type="button" class="duplicate-warning-badge btn-trigger-merge"
                        data-id-a="${record.id}" data-id-b="${primaryMatch.record.id}"
                        title="Dấu hiệu trùng lặp: ${escapeHtml(matchReasonSummary)}. Nhấp để đối chiếu & gộp ngay!">
                        <span class="duplicate-pulse-dot"></span>
                        <span>⚠️ Có dấu hiệu trùng lặp (${escapeHtml(matchCompany)})</span>
                    </button>
                `;

                mergeActionBtnHtml = `
                    <button type="button" class="action-icon-btn btn-merge-duplicate btn-trigger-merge"
                        data-id-a="${record.id}" data-id-b="${primaryMatch.record.id}"
                        title="Đối chiếu & Gộp khách hàng trùng (CRM-64)">
                        <svg viewBox="0 0 24 24"><path d="M8 7v10M8 12h8m0-5v10m-3-12 3 2 3-2m-6 14 3-2 3 2"/></svg>
                    </button>
                `;
            }

            // Xử lý bản ghi đã gộp
            let statusPillHtml = "";
            if (record.status === "DA_GOP" || record.isMerged) {
                statusPillHtml = `
                    <span class="status-pill status-merged" title="Hồ sơ đã được gộp vào: ${escapeHtml(record.mergedIntoName || 'Hồ sơ chính')}">
                        <span class="status-dot"></span>
                        Đã gộp (Merged)
                    </span>
                `;
            } else {
                statusPillHtml = `
                    <span class="status-pill ${getStatusPillClass(record.status)}" data-quick-status="${record.id}" style="cursor:pointer;" title="Bấm để đổi nhanh trạng thái">
                        <span class="status-dot"></span>
                        ${getStatusLabel(record.status)}
                    </span>
                `;
            }

            tr.innerHTML = `
                <td class="col-checkbox">
                    <input type="checkbox" class="row-checkbox" data-id="${record.id}" ${isSelected ? "checked" : ""}>
                </td>
                <td class="col-name">
                    <div style="display:flex; align-items:center; gap:6px; flex-wrap:wrap;">
                        <a href="customer-360.html?id=${record.id}" class="customer-name-link" title="Xem 360 độ khách hàng">
                            ${escapeHtml(record.companyName || record.name)}
                        </a>
                        ${(function() {
                            if (!window.CorporateHierarchyManager) return "";
                            const hier = window.CorporateHierarchyManager.getHierarchyStorage();
                            if (hier.parents && hier.parents[record.id]) {
                                return `<button type="button" class="corporate-role-badge corporate-role-child" style="cursor:pointer; border:none; font-size:10.5px; padding:2px 7px;" onclick="window.CorporateHierarchyManager.openHierarchyModal(${record.id})" title="Thuộc tập đoàn - Nhấp để xem cây phân cấp (CRM-65)">🏢 Công ty con</button>`;
                            }
                            const hasChildren = Object.values(hier.parents || {}).some(pId => Number(pId) === Number(record.id));
                            if (hasChildren) {
                                return `<button type="button" class="corporate-role-badge corporate-role-parent" style="cursor:pointer; border:none; font-size:10.5px; padding:2px 7px;" onclick="window.CorporateHierarchyManager.openHierarchyModal(${record.id})" title="Tập đoàn mẹ - Nhấp để xem cây phân cấp (CRM-65)">👑 Tập đoàn mẹ</button>`;
                            }
                            return "";
                        })()}
                        ${churnBadge}
    ${duplicateBadgeHtml}
</div>
                    <div class="customer-tax-wrap">
                        <span class="tax-badge" title="Mã số thuế doanh nghiệp">
                            <svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="16" rx="2"/><line x1="7" y1="8" x2="17" y2="8"/><line x1="7" y1="12" x2="13" y2="12"/><line x1="7" y1="16" x2="10" y2="16"/></svg>
                            MST: ${escapeHtml(record.taxCode || "Chưa cập nhật")}
                        </span>
                        ${record.website ? `
                            <a href="${formatUrl(record.website)}" target="_blank" rel="noopener" class="tax-badge" style="text-decoration:none;" title="Truy cập website">
                                <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
                                Website
                            </a>` : ""}
                    </div>
                </td>
                <td class="col-contact">
                    ${(function() {
                        if (window.ContactsManager) {
                            const custContacts = window.ContactsManager.getContactsByCustomer(record.id);
                            const primaryC = custContacts.find(c => c.isPrimary) || custContacts[0];
                            if (primaryC) {
                                return `
                                    <div style="margin-bottom:4px; font-weight:700; color:var(--crm-text); font-size:12.5px; display:flex; align-items:center; gap:4px; flex-wrap:wrap;">
                                        <span>⭐ ${escapeHtml(primaryC.fullName)}</span>
                                        ${window.ContactsManager.renderDecisionRoleBadge(primaryC.decisionRole)}
                                    </div>
                                    <div style="font-size:11.5px; color:var(--crm-muted); margin-bottom:4px;">
                                        ${escapeHtml(primaryC.jobTitle)} (${custContacts.length} liên hệ)
                                    </div>
                                    <div class="contact-item" title="Email liên hệ chính">
                                        <svg viewBox="0 0 24 24"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
                                        <span>${escapeHtml(primaryC.email || record.email || "—")}</span>
                                    </div>
                                    <div class="contact-item" title="Số điện thoại / Hotline">
                                        <svg viewBox="0 0 24 24"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
                                        <span>${escapeHtml(primaryC.phone || record.phone || "—")}</span>
                                    </div>
                                `;
                            }
                        }
                        return `
                            <div class="contact-item" title="Email liên hệ">
                                <svg viewBox="0 0 24 24"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
                                <span>${escapeHtml(record.email || "—")}</span>
                            </div>
                            <div class="contact-item" title="Số điện thoại / Hotline">
                                <svg viewBox="0 0 24 24"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
                                <span>${escapeHtml(record.phone || "—")}</span>
                            </div>
                        `;
                    })()}
                </td>
                <td class="col-meta">
                    <div class="meta-pill" title="Ngành nghề kinh doanh">
                        <svg viewBox="0 0 24 24"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>
                        ${escapeHtml(industry)}
                    </div>
                    <div class="meta-pill" style="margin-top:4px;" title="Quy mô doanh nghiệp">
                        <svg viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                        ${escapeHtml(companySize)}
                    </div>
                </td>
                <td>
                    ${statusPillHtml}
                </td>
                <td class="col-owner">
                    <div class="owner-cell">
                        <div class="owner-avatar">${getInitials(record.ownerName || record.owner || "U")}</div>
                        <div class="owner-details">
                            <span class="owner-name">${escapeHtml(record.ownerName || record.owner || "Chưa phân công")}</span>
                            ${scopeTag}
                        </div>
                    </div>
                </td>
                <td class="col-actions">
                    <div class="row-action-btn-group">
                        ${mergeActionBtnHtml}
                        <button type="button" class="action-icon-btn btn-corporate-tree" onclick="window.CorporateHierarchyManager && window.CorporateHierarchyManager.openHierarchyModal(${record.id})" title="Cây phân cấp & Tập đoàn Mẹ - Con (CRM-65)" style="color:#7c3aed;">
                            <svg viewBox="0 0 24 24"><path d="M3 21h18M3 7v14M21 7v14M6 18h2v3H6zm8 0h2v3h-2zm-4 0h2v3h-2zm4-6h2v3h-2zm-4 0h2v3h-2zm-4 0h2v3H6zm12-5V3H6v4"/></svg>
                        </button>
                        <a href="contacts.html?customerId=${record.id}" class="action-icon-btn" title="Quản lý Người liên hệ & Vai trò mua (CRM-62)" style="color:var(--crm-primary);">
                            <svg viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                        </a>
                        <a href="customer-360.html?id=${record.id}" class="action-icon-btn btn-view" title="Xem hồ sơ 360°">
                            <svg viewBox="0 0 24 24"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>
                        </a>
                        <button type="button" class="action-icon-btn btn-edit" data-id="${record.id}" title="Chỉnh sửa hồ sơ">
                            <svg viewBox="0 0 24 24"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/><path d="m15 5 4 4"/></svg>
                        </button>
                        <button type="button" class="action-icon-btn btn-danger btn-delete" data-id="${record.id}" title="Xóa khách hàng">
                            <svg viewBox="0 0 24 24"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
                        </button>
                    </div>
                </td>
            `;

            tableBody.appendChild(tr);
        });
    }

    function renderPagination() {
        if (!paginationControls || !paginationInfo) return;

        const start = state.totalItems === 0 ? 0 : (state.page - 1) * state.pageSize + 1;
        const end = Math.min(state.page * state.pageSize, state.totalItems);
        paginationInfo.textContent = `Hiển thị ${start} - ${end} trong tổng số ${state.totalItems} khách hàng`;

        paginationControls.innerHTML = "";

        // Prev Button
        const prevBtn = document.createElement("button");
        prevBtn.className = "page-btn";
        prevBtn.disabled = state.page <= 1;
        prevBtn.innerHTML = "‹ Trước";
        prevBtn.addEventListener("click", () => {
            if (state.page > 1) {
                state.page--;
                filterAndRenderTable();
            }
        });
        paginationControls.appendChild(prevBtn);

        // Page Numbers (render up to 5 buttons)
        const maxPagesToShow = 5;
        let startPage = Math.max(1, state.page - 2);
        let endPage = Math.min(state.totalPages, startPage + maxPagesToShow - 1);
        if (endPage - startPage < maxPagesToShow - 1) {
            startPage = Math.max(1, endPage - maxPagesToShow + 1);
        }

        for (let p = startPage; p <= endPage; p++) {
            const pageBtn = document.createElement("button");
            pageBtn.className = `page-btn ${p === state.page ? "active" : ""}`;
            pageBtn.textContent = String(p);
            pageBtn.addEventListener("click", () => {
                state.page = p;
                filterAndRenderTable();
            });
            paginationControls.appendChild(pageBtn);
        }

        // Next Button
        const nextBtn = document.createElement("button");
        nextBtn.className = "page-btn";
        nextBtn.disabled = state.page >= state.totalPages;
        nextBtn.innerHTML = "Sau ›";
        nextBtn.addEventListener("click", () => {
            if (state.page < state.totalPages) {
                state.page++;
                filterAndRenderTable();
            }
        });
        paginationControls.appendChild(nextBtn);
    }

    function updateKpiStats() {
        const statTotal = document.getElementById("statTotal");
        const statProspect = document.getElementById("statProspect");
        const statDealing = document.getElementById("statDealing");
        const statCustomer = document.getElementById("statCustomer");

        if (statTotal) statTotal.textContent = String(state.totalItems);

        let countProspect = 0, countDealing = 0, countCustomer = 0;
        customersList.forEach(c => {
            if (c.status === "DA_GOP" || c.isMerged) return;
            const s = (c.status || "").toUpperCase();
            if (s.includes("TIEM") || s.includes("LEAD")) countProspect++;
            else if (s.includes("GIAO_DICH") || s.includes("DANG")) countDealing++;
            else if (s.includes("CHINH") || s.includes("WON") || s.includes("KHACH")) countCustomer++;
        });

        if (statProspect) statProspect.textContent = String(countProspect);
        if (statDealing) statDealing.textContent = String(countDealing);
        if (statCustomer) statCustomer.textContent = String(countCustomer);
    }

    /* =========================================================
       4. FORM VALIDATION & MODAL DRAWER HANDLING
    ========================================================= */
    function openDrawer(customerId = null) {
        resetDrawerForm();

        if (customerId) {
            window.currentEditingCustomerId = Number(customerId);
            const customer = customersList.find(c => Number(c.id) === Number(customerId));
            if (customer) {
                if (drawerTitle) drawerTitle.textContent = "Chỉnh sửa Hồ sơ Khách hàng Doanh nghiệp";
                setVal("customerId", customer.id);
                setVal("companyName", customer.companyName || customer.name);
                setVal("taxCode", customer.taxCode);
                setVal("customerStatus", normalizeStatus(customer.status));
                setVal("industrySelect", customer.industryId);
                setVal("companySizeSelect", customer.companySizeId);
                setVal("customerEmail", customer.email);
                setVal("customerPhone", customer.phone);
                setVal("website", customer.website);
                setVal("address", customer.address);
                setVal("customerRegion", customer.region || "");
                setVal("ownerSelect", customer.ownerUserId);
            }
        } else {
            window.currentEditingCustomerId = null;
            if (drawerTitle) drawerTitle.textContent = "Khai báo Hồ sơ Khách hàng Doanh nghiệp";
            if (currentSessionUser?.id) {
                setVal("ownerSelect", currentSessionUser.id);
            }
        }

        // CRM-65: Sync Parent Selector in Drawer
        const parentSelect = document.getElementById("customerParentSelect");
        if (parentSelect && window.CorporateHierarchyManager) {
            const hier = window.CorporateHierarchyManager.getHierarchyStorage();
            parentSelect.value = (customerId && hier.parents && hier.parents[customerId]) ? String(hier.parents[customerId]) : "";
        }

        drawer?.classList.add("open");
        drawerOverlay?.classList.add("open");

        // Quét trùng lặp thời gian thực trong Drawer
        checkDrawerDuplicates();
    }

    function closeDrawer() {
        drawer?.classList.remove("open");
        drawerOverlay?.classList.remove("open");
        resetDrawerForm();
    }

    function resetDrawerForm() {
        customerForm?.reset();
        setVal("customerId", "");
        document.querySelectorAll(".field-group").forEach(el => el.classList.remove("has-error"));
        const drawerAlert = document.getElementById("drawerDuplicateAlert");
        if (drawerAlert) drawerAlert.style.display = "none";
    }

    /**
     * Real-time Enterprise Duplicate Scanner inside Customer Drawer (CRM-64)
     */
    function checkDrawerDuplicates() {
        const id = getVal("customerId");
        const name = getVal("companyName");
        const tax = getVal("taxCode");
        const web = getVal("website");

        const drawerAlert = document.getElementById("drawerDuplicateAlert");
        const dupReasonsEl = document.getElementById("drawerDupReasons");
        const dupSummaryEl = document.getElementById("drawerDupSummary");

        if (!name && !tax && !web) {
            if (drawerAlert) drawerAlert.style.display = "none";
            return;
        }

        if (!window.DuplicateMergeEngine) return;

        const candidate = {
            id: id ? Number(id) : 9999999,
            companyName: name,
            name: name,
            taxCode: tax,
            website: web
        };

        const dupResult = window.DuplicateMergeEngine.detectDuplicatesForCustomer(candidate, customersList);

        if (dupResult && dupResult.isDuplicate && dupResult.matches.length > 0) {
            const topMatch = dupResult.matches[0];
            const matchRec = topMatch.record;
            if (drawerAlert) drawerAlert.style.display = "block";
            if (dupSummaryEl) {
                dupSummaryEl.textContent = `Doanh nghiệp này trùng lặp với "${matchRec.companyName || matchRec.name}" (MST: ${matchRec.taxCode || 'N/A'}, Sale: ${matchRec.ownerName || 'Chưa rõ'}).`;
            }
            if (dupReasonsEl) {
                dupReasonsEl.innerHTML = topMatch.reasons.map(r => `<span class="drawer-dup-tag">⚠️ ${escapeHtml(r)}</span>`).join("");
            }

            const openMergeBtn = document.getElementById("btnDrawerOpenMerge");
            if (openMergeBtn) {
                openMergeBtn.onclick = () => {
                    closeDrawer();
                    const existingId = id ? Number(id) : matchRec.id;
                    const peerId = (existingId === matchRec.id && dupResult.matches[1]) ? dupResult.matches[1].record.id : matchRec.id;
                    openMergeModal(existingId, peerId);
                };
            }
        } else {
            if (drawerAlert) drawerAlert.style.display = "none";
        }
    }

    function validateForm() {
        let isValid = true;

        // 1. Company Name
        const nameVal = getVal("companyName");
        const groupName = document.getElementById("groupCompanyName");
        const errorName = document.getElementById("companyNameError");
        if (!nameVal) {
            setFieldError(groupName, errorName, "Tên công ty là bắt buộc và không được để trống.");
            isValid = false;
        } else {
            clearFieldError(groupName);
        }

        // 2. Tax Code (MST)
        const taxVal = getVal("taxCode");
        const groupTax = document.getElementById("groupTaxCode");
        const errorTax = document.getElementById("taxCodeError");

        if (!taxVal) {
            clearFieldError(groupTax);
        } else {
            const cleanTax = taxVal;
            const taxRegex = /^(\d{10}|\d{10}-\d{3}|\d{13})$/;
            if (!taxRegex.test(cleanTax)) {
                setFieldError(groupTax, errorTax, "MST phải gồm 10 số hoặc 13 số (chi nhánh).");
                isValid = false;
            } else {
                clearFieldError(groupTax);
            }
        }

        // 3. Email
        const emailVal = getVal("customerEmail");
        const groupEmail = document.getElementById("groupEmail");
        const errorEmail = document.getElementById("customerEmailError");
        if (emailVal) {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(emailVal)) {
                setFieldError(groupEmail, errorEmail, "Địa chỉ email không đúng định dạng.");
                isValid = false;
            } else {
                clearFieldError(groupEmail);
            }
        } else {
            clearFieldError(groupEmail);
        }

        // 4. Website
        const webVal = getVal("website");
        const groupWeb = document.getElementById("groupWebsite");
        const errorWeb = document.getElementById("websiteError");
        if (webVal) {
            const urlRegex = /^https?:\/\/[^\s]+$/i;
            if (!urlRegex.test(webVal)) {
                setFieldError(groupWeb, errorWeb, "Website không hợp lệ (Ví dụ: https://company.vn hoặc company.com).");
                isValid = false;
            } else {
                clearFieldError(groupWeb);
            }
        } else {
            clearFieldError(groupWeb);
        }

        // 5. Phone
        const phoneVal = getVal("customerPhone");
        const groupPhone = document.getElementById("groupPhone");
        const errorPhone = document.getElementById("customerPhoneError");
        if (phoneVal) {
            const phoneClean = phoneVal.replace(/[^0-9+]/g, "");
            if (phoneClean.length < 8 || phoneClean.length > 15) {
                setFieldError(groupPhone, errorPhone, "Số điện thoại phải từ 8 đến 15 chữ số.");
                isValid = false;
            } else {
                clearFieldError(groupPhone);
            }
        } else {
            clearFieldError(groupPhone);
        }

        // Realtime scan duplicate
        checkDrawerDuplicates();

        return isValid;
    }

    async function handleSaveCustomer(event) {
        event.preventDefault();
        if (!validateForm()) return;
        const id = getVal("customerId");
        const payload = { region:getVal("customerRegion") || null, name:getVal("companyName"), taxCode:getVal("taxCode") || null, status:getVal("customerStatus") || "TIEM_NANG", email:getVal("customerEmail"), phone:getVal("customerPhone"), website:getVal("website"), address:getVal("address"), industryId:Number(getVal("industrySelect")) || null, companySizeId:Number(getVal("companySizeSelect")) || null, ownerUserId:Number(getVal("ownerSelect")) || currentSessionUser.id };
        const button = document.getElementById("btnSaveCustomer");
        button.disabled = true;
        try {
            const saved = await apiRequest(`/api/customers${id ? "/"+id : ""}`, {method:id ? "PUT" : "POST",body:JSON.stringify(payload)});
            setVal("customerId", saved.id);
            const parent = document.getElementById("customerParentSelect");
            if (parent && Number(saved.parentCustomerId || 0) !== Number(parent.value || 0)) {
                try { await window.CorporateHierarchyManager.apiUpdateParent(saved.id, Number(parent.value) || null); }
                catch (error) { await syncBackendCustomers(); throw new Error("Hồ sơ đã lưu; gán công ty mẹ thất bại: " + error.message); }
            }
            closeDrawer();
            await loadCustomers();
            showToast("Đã lưu hồ sơ khách hàng.");
        } catch (error) { showToast(error.message,"error"); }
        finally { button.disabled = false; }
    }

    async function handleQuickAddCatalog(type, typeLabel) {
        const name = window.prompt(`Nhập tên ${typeLabel} mới:`);
        if (!name || !name.trim()) return;
        const trimmedName = name.trim();

        const targetList = (type === "industry") ? INDUSTRIES : COMPANY_SIZES;
        const existing = targetList.find(x => x.name && x.name.toLowerCase() === trimmedName.toLowerCase());
        if (existing) {
            showToast(`${typeLabel.charAt(0).toUpperCase() + typeLabel.slice(1)} "${trimmedName}" đã tồn tại trong danh sách.`, "info");
            const selectEl = document.getElementById(type === "industry" ? "industrySelect" : "companySizeSelect");
            if (selectEl) selectEl.value = String(existing.id);
            return;
        }

        const code = (type === "industry" ? "IND_" : "SIZE_") + Date.now().toString(36).toUpperCase();
        let newItem = { id: Date.now(), code: code, name: trimmedName };

        try {
            const res = await apiRequest(`/api/master-data/${type}`, {
                method: "POST",
                body: JSON.stringify({ code: code, name: trimmedName, active: true })
            });
            if (res && res.data && res.data.id) {
                newItem.id = res.data.id;
            }
            showToast(`Đã thêm ${typeLabel} "${trimmedName}" thành công!`, "success");
        } catch (err) {
            console.warn("Could not persist to master-data API:", err);
            showToast(`Đã thêm nhanh ${typeLabel} "${trimmedName}".`, "info");
        }

        targetList.push(newItem);
        populateSelectOptions();
        const selectEl = document.getElementById(type === "industry" ? "industrySelect" : "companySizeSelect");
        if (selectEl) {
            selectEl.value = String(newItem.id);
        }
    }

    /* =========================================================
       5. ACTIONS: QUICK STATUS, DELETE & EXPORT EXCEL
    ========================================================= */
    function openQuickStatus(customerId) {
        const customer = customersList.find(c => Number(c.id) === Number(customerId));
        if (!customer) return;

        quickActionCustomer = customer;
        if (quickModalName) quickModalName.textContent = `${customer.companyName || customer.name} (MST: ${customer.taxCode || "N/A"})`;
        if (quickModalSelect) quickModalSelect.value = normalizeStatus(customer.status);

        if (quickModal) quickModal.style.display = "block";
        if (quickModalOverlay) quickModalOverlay.classList.add("open");
    }

    function closeQuickStatus() {
        if (quickModal) quickModal.style.display = "none";
        if (quickModalOverlay) quickModalOverlay.classList.remove("open");
        quickActionCustomer = null;
    }

    async function handleConfirmQuickStatus() {
        if (!quickActionCustomer) return;
        try {
            const profile = await apiRequest(`/api/customers/${quickActionCustomer.id}`);
            await apiRequest(`/api/customers/${quickActionCustomer.id}`, {method:"PUT",body:JSON.stringify({...profile,status:quickModalSelect.value})});
            closeQuickStatus();
            await syncBackendCustomers();
            showToast("Đã cập nhật trạng thái.");
        } catch (error) { showToast(error.message,"error"); }
    }

    async function handleDeleteCustomer(customerId) {
        const customer = customersList.find(c => Number(c.id) === Number(customerId));
        if (!customer || !confirm(`Xóa khách hàng "${customer.name}"?`)) return;
        try {
            await apiRequest(`/api/customers/${customerId}`, {method:"DELETE"});
            await loadCustomers();
            showToast("Đã xóa khách hàng.");
        } catch (error) { showToast(error.message,"error"); }
    }

    function exportToExcel() {
        if (!customersList.length) {
            alert("Không có dữ liệu khách hàng để xuất Excel.");
            return;
        }

        const headers = [
            "Mã khách hàng",
            "Tên công ty / Doanh nghiệp",
            "Mã số thuế",
            "Trạng thái vòng đời",
            "Ngành nghề kinh doanh",
            "Quy mô",
            "Email",
            "Số điện thoại",
            "Website",
            "Địa chỉ",
            "Người phụ trách"
        ];

        const rows = customersList.map(c => {
            const industry = INDUSTRIES.find(i => Number(i.id) === Number(c.industryId))?.name || "";
            const companySize = COMPANY_SIZES.find(s => Number(s.id) === Number(c.companySizeId))?.name || "";
            return [
                c.id,
                `"${(c.companyName || c.name || "").replace(/"/g, '""')}"`,
                `"${(c.taxCode || "").replace(/"/g, '""')}"`,
                `"${getStatusLabel(c.status)}"`,
                `"${industry.replace(/"/g, '""')}"`,
                `"${companySize.replace(/"/g, '""')}"`,
                `"${(c.email || "").replace(/"/g, '""')}"`,
                `"${(c.phone || "").replace(/"/g, '""')}"`,
                `"${(c.website || "").replace(/"/g, '""')}"`,
                `"${(c.address || "").replace(/"/g, '""')}"`,
                `"${(c.ownerName || c.owner || "").replace(/"/g, '""')}"`
            ];
        });

        const csvContent = "\uFEFF" + [headers.join(","), ...rows.map(r => r.join(","))].join("\n");
        const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.setAttribute("href", url);
        link.setAttribute("download", `Danh_sach_khach_hang_doanh_nghiep_${new Date().toISOString().slice(0, 10)}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }

    /* =========================================================
       6. CRM-64: SIDE-BY-SIDE MERGE COMPARISON MODAL CONTROLLER
    ========================================================= */

    async function openMergeModal(idA, idB) {
        let comparison;
        try { comparison = await apiRequest(`/api/customers/compare?masterId=${idA}&secondaryId=${idB}`); }
        catch(error) { showToast(error.message,"error"); return; }
        if (!window.DuplicateMergeEngine) {
            alert("Công cụ gộp khách hàng đang tải, vui lòng thử lại.");
            return;
        }

        const recordA = comparison.master;
        const recordB = comparison.secondary;

        if (!recordA || !recordB) {
            alert("Không tìm thấy thông tin của 2 bản ghi để so sánh đối chiếu.");
            return;
        }

        const dupDetect = window.DuplicateMergeEngine.detectDuplicatesForCustomer(recordA, [recordB]);
        const matchedReasons = dupDetect.matches[0]?.reasons || [
            "Phát hiện dấu hiệu trùng lặp theo tiêu chí quản trị doanh nghiệp"
        ];

        currentMergeSession = {
            recordA: recordA,
            recordB: recordB,
            masterKey: "A", // 'A' | 'B'
            fieldOverrides: {
                companyName: recordA.companyName || recordA.name,
                taxCode: recordA.taxCode ?? null,
                website: recordA.website ?? null,
                industryId: recordA.industryId,
                companySizeId: recordA.companySizeId,
                ownerUserId: recordA.ownerUserId,
                email: recordA.email ?? null,
                phone: recordA.phone ?? null,
                address: recordA.address ?? null,
                status: recordA.status || "TIEM_NANG"
            },
            fieldChoices: {},
            relatedA: comparison.masterRelated, relatedB: comparison.secondaryRelated,
            reasons: matchedReasons
        };

        renderMergeModal();

        mergeModal?.classList.add("open");
        mergeModalOverlay?.classList.add("open");
    }

    function closeMergeModal() {
        mergeModal?.classList.remove("open");
        mergeModalOverlay?.classList.remove("open");
        currentMergeSession = null;
    }

    function renderMergeModal() {
        if (!currentMergeSession) return;
        const { recordA, recordB, masterKey, reasons } = currentMergeSession;

        // 1. Dấu hiệu trùng lặp
        const reasonTagsContainer = document.getElementById("mergeReasonTags");
        if (reasonTagsContainer) {
            reasonTagsContainer.innerHTML = reasons.map(r => `<span class="merge-reason-badge">⚠️ ${escapeHtml(r)}</span>`).join("");
        }

        // 2. Thẻ chọn Master Record
        if (radioSelectMasterA) radioSelectMasterA.checked = masterKey === "A";
        if (radioSelectMasterB) radioSelectMasterB.checked = masterKey === "B";

        if (cardRecordA) {
            if (masterKey === "A") cardRecordA.classList.add("selected");
            else cardRecordA.classList.remove("selected");
        }
        if (cardRecordB) {
            if (masterKey === "B") cardRecordB.classList.add("selected");
            else cardRecordB.classList.remove("selected");
        }

        const tagA = document.getElementById("tagMasterA");
        const tagB = document.getElementById("tagMasterB");
        if (tagA) {
            tagA.textContent = masterKey === "A" ? "MASTER RECORD" : "BẢN GHI PHỤ (SẼ GỘP)";
            tagA.className = `master-status-tag ${masterKey === "A" ? "is-master" : "is-secondary"}`;
        }
        if (tagB) {
            tagB.textContent = masterKey === "B" ? "MASTER RECORD" : "BẢN GHI PHỤ (SẼ GỘP)";
            tagB.className = `master-status-tag ${masterKey === "B" ? "is-master" : "is-secondary"}`;
        }

        setElText("cardACompanyName", recordA.companyName || recordA.name);
        setElText("cardATaxCode", recordA.taxCode || "Chưa có");
        setElText("cardAOwner", recordA.ownerName || recordA.owner || "Chưa phân công");
        setElText("cardAStatus", getStatusLabel(recordA.status));

        setElText("cardBCompanyName", recordB.companyName || recordB.name);
        setElText("cardBTaxCode", recordB.taxCode || "Chưa có");
        setElText("cardBOwner", recordB.ownerName || recordB.owner || "Chưa phân công");
        setElText("cardBStatus", getStatusLabel(recordB.status));

        const thA = document.getElementById("thRecordAName");
        const thB = document.getElementById("thRecordBName");
        if (thA) thA.textContent = `Hồ sơ A: ${recordA.companyName || recordA.name}`;
        if (thB) thB.textContent = `Hồ sơ B: ${recordB.companyName || recordB.name}`;

        // 3. Render bảng so sánh 2 cột song song (Side-by-Side Table)
        renderSideBySideTable();

        // 4. Cập nhật số liệu preview hợp nhất
        const countContactsA = Number(currentMergeSession.relatedA.contacts);
        const countContactsB = Number(currentMergeSession.relatedB.contacts);
        setElText("txtContactsCount", `${countContactsA} + ${countContactsB} = ${countContactsA + countContactsB} liên hệ`);

        const countDealsA = Number(currentMergeSession.relatedA["opportunities.customer_id"]);
        const countDealsB = Number(currentMergeSession.relatedB["opportunities.customer_id"]);
        setElText("txtDealsCount", `${countDealsA} + ${countDealsB} = ${countDealsA + countDealsB} cơ hội`);

        const countActsA = Number(currentMergeSession.relatedA["activities.customer_id"]);
        const countActsB = Number(currentMergeSession.relatedB["activities.customer_id"]);
        setElText("txtActivitiesCount", `Hợp nhất toàn bộ ${countActsA + countActsB} tương tác`);

        // 5. Cập nhật phân quyền nút Gộp
        updateRolePermissionUI();
    }

    function renderSideBySideTable() {
        const tbody = document.getElementById("sideBySideTableBody");
        if (!tbody || !currentMergeSession) return;
        tbody.innerHTML = "";

        const { recordA, recordB, fieldOverrides } = currentMergeSession;

        const fieldsToCompare = [
            {
                key: "companyName",
                label: "Tên công ty / Doanh nghiệp",
                valA: recordA.companyName || recordA.name || "",
                valB: recordB.companyName || recordB.name || "",
                displayValA: recordA.companyName || recordA.name || "—",
                displayValB: recordB.companyName || recordB.name || "—"
            },
            {
                key: "taxCode",
                label: "Mã số thuế (MST)",
                valA: recordA.taxCode ?? null,
                valB: recordB.taxCode ?? null,
                displayValA: recordA.taxCode || "—",
                displayValB: recordB.taxCode || "—"
            },
            {
                key: "website",
                label: "Website doanh nghiệp",
                valA: recordA.website ?? null,
                valB: recordB.website ?? null,
                displayValA: recordA.website || "—",
                displayValB: recordB.website || "—"
            },
            {
                key: "industryId",
                label: "Ngành nghề kinh doanh",
                valA: recordA.industryId,
                valB: recordB.industryId,
                displayValA: INDUSTRIES.find(i => Number(i.id) === Number(recordA.industryId))?.name || "Chưa phân loại",
                displayValB: INDUSTRIES.find(i => Number(i.id) === Number(recordB.industryId))?.name || "Chưa phân loại"
            },
            {
                key: "companySizeId",
                label: "Quy mô nhân sự",
                valA: recordA.companySizeId,
                valB: recordB.companySizeId,
                displayValA: COMPANY_SIZES.find(s => Number(s.id) === Number(recordA.companySizeId))?.name || "Chưa xác định",
                displayValB: COMPANY_SIZES.find(s => Number(s.id) === Number(recordB.companySizeId))?.name || "Chưa xác định"
            },
            {
                key: "ownerUserId",
                label: "Người phụ trách (Sale Owner)",
                valA: recordA.ownerUserId,
                valB: recordB.ownerUserId,
                displayValA: recordA.ownerName || recordA.owner || "Chưa phân công",
                displayValB: recordB.ownerName || recordB.owner || "Chưa phân công"
            },
            {
                key: "phone",
                label: "Số điện thoại / Hotline",
                valA: recordA.phone ?? null,
                valB: recordB.phone ?? null,
                displayValA: recordA.phone || "—",
                displayValB: recordB.phone || "—"
            },
            {
                key: "email",
                label: "Email đại diện",
                valA: recordA.email ?? null,
                valB: recordB.email ?? null,
                displayValA: recordA.email || "—",
                displayValB: recordB.email || "—"
            },
            {
                key: "address",
                label: "Địa chỉ trụ sở văn phòng",
                valA: recordA.address ?? null,
                valB: recordB.address ?? null,
                displayValA: recordA.address || "—",
                displayValB: recordB.address || "—"
            },
            {
                key: "status",
                label: "Trạng thái vòng đời",
                valA: recordA.status || "TIEM_NANG",
                valB: recordB.status || "TIEM_NANG",
                displayValA: getStatusLabel(recordA.status),
                displayValB: getStatusLabel(recordB.status)
            }
        ];

        fieldsToCompare.forEach(f => {
            const tr = document.createElement("tr");
            const isDifferent = String(f.valA) !== String(f.valB);
            if (isDifferent) tr.classList.add("row-different");

            const currentChoiceValue = fieldOverrides[f.key];
            const side = currentMergeSession.fieldChoices[f.key] || currentMergeSession.masterKey;
            const isAChecked = side === "A";
            const isBChecked = side === "B";

            tr.innerHTML = `
                <td class="field-name-cell">
                    ${isDifferent ? '<span class="field-diff-indicator" title="Có sự khác biệt giữa 2 hồ sơ"></span>' : ''}
                    ${escapeHtml(f.label)}
                </td>
                <td class="field-val-cell ${isAChecked ? 'is-selected' : ''}" data-side="A" data-field="${f.key}">
                    <div class="field-choice-row">
                        <input type="radio" class="field-choice-radio" name="override_${f.key}" value="A" ${isAChecked ? 'checked' : ''}>
                        <div class="field-val-content">${escapeHtml(f.displayValA)}</div>
                    </div>
                </td>
                <td class="field-val-cell ${isBChecked ? 'is-selected' : ''}" data-side="B" data-field="${f.key}">
                    <div class="field-choice-row">
                        <input type="radio" class="field-choice-radio" name="override_${f.key}" value="B" ${isBChecked ? 'checked' : ''}>
                        <div class="field-val-content">${escapeHtml(f.displayValB)}</div>
                    </div>
                </td>
            `;

            // Bắt sự kiện click chọn từng trường thông tin
            tr.querySelectorAll(".field-val-cell").forEach(cell => {
                cell.addEventListener("click", () => {
                    const side = cell.dataset.side;
                    const fieldKey = cell.dataset.field;
                    const chosenVal = side === "A" ? f.valA : f.valB;
                    fieldOverrides[fieldKey] = chosenVal;
                    currentMergeSession.fieldChoices[fieldKey] = side;
                    renderSideBySideTable();
                });
            });

            tbody.appendChild(tr);
        });
    }

    function switchMasterRecord(newMaster) {
        if (!currentMergeSession) return;
        currentMergeSession.masterKey = newMaster;
        currentMergeSession.fieldChoices={};

        const targetMasterRecord = newMaster === "A" ? currentMergeSession.recordA : currentMergeSession.recordB;
        currentMergeSession.fieldOverrides = {
            companyName: targetMasterRecord.companyName || targetMasterRecord.name,
            taxCode: targetMasterRecord.taxCode ?? null,
            website: targetMasterRecord.website ?? null,
            industryId: targetMasterRecord.industryId,
            companySizeId: targetMasterRecord.companySizeId,
            ownerUserId: targetMasterRecord.ownerUserId,
            email: targetMasterRecord.email ?? null,
            phone: targetMasterRecord.phone ?? null,
            address: targetMasterRecord.address ?? null,
            status: targetMasterRecord.status || "TIEM_NANG"
        };

        renderMergeModal();
    }

    function updateRolePermissionUI() {
        const role = window.DuplicateMergeEngine?.getSimulatedRole() || "";
        if (roleSimulatorSelect) roleSimulatorSelect.closest(".role-simulator")?.remove();
        if (roleSimulatorSelect) { roleSimulatorSelect.hidden=true; roleSimulatorSelect.disabled=true; }

        const canMerge = window.DuplicateMergeEngine?.canExecuteMerge(role);
        const permStatusEl = document.getElementById("mergePermissionStatus");

        if (canMerge) {
            if (permStatusEl) {
                permStatusEl.innerHTML = `
                    <div class="permission-granted">
                        <svg class="crm-inline-icon" viewBox="0 0 24 24" style="width:16px;height:16px;color:#16a34a;"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                        <span><strong>Quyền Trưởng nhóm:</strong> Được phép thực hiện gộp khách hàng (CRM-64).</span>
                    </div>
                `;
            }
            if (btnConfirmMerge) {
                btnConfirmMerge.disabled = false;
                btnConfirmMerge.title = "Xác nhận thực hiện gộp 2 hồ sơ khách hàng";
            }
        } else {
            if (permStatusEl) {
                permStatusEl.innerHTML = `
                    <div class="permission-denied">
                        <svg class="crm-inline-icon" viewBox="0 0 24 24" style="width:16px;height:16px;color:#d97706;"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                        <span><strong>Chỉ xem đối chiếu:</strong> Nhân viên Sale không có quyền gộp. Cần Trưởng nhóm phê duyệt.</span>
                    </div>
                `;
            }
            if (btnConfirmMerge) {
                btnConfirmMerge.disabled = true;
                btnConfirmMerge.title = "Chỉ Trưởng nhóm kinh doanh hoặc Quản trị viên mới có quyền thực hiện gộp khách hàng";
            }
        }
    }

    async function handleConfirmMerge() {
        if (!currentMergeSession || btnConfirmMerge.disabled) return;
        const {recordA,recordB,masterKey,fieldOverrides} = currentMergeSession;
        const master = masterKey === "A" ? recordA : recordB;
        const secondary = masterKey === "A" ? recordB : recordA;
        if (!confirm(`Gộp "${secondary.name}" vào "${master.name}" và chuyển toàn bộ dữ liệu liên quan?`)) return;
        btnConfirmMerge.disabled = true;
        try {
            const allowed = ["name","companyName","taxCode","status","email","phone","website","address","industryId","companySizeId","ownerUserId"];
            const overrides = Object.fromEntries(Object.entries(fieldOverrides).filter(([key]) => allowed.includes(key)));
            await apiRequest("/api/customers/merge",{method:"POST",body:JSON.stringify({masterId:master.id,secondaryId:secondary.id,fieldOverrides:overrides})});
            closeMergeModal();
            await loadCustomers();
            showToast("Đã hợp nhất khách hàng và dữ liệu liên quan.");
        } catch (error) { showToast(error.message,"error"); }
        finally { updateRolePermissionUI(); }
    }

    /* =========================================================
       7. EVENT BINDINGS
    ========================================================= */
    function bindEvents() {
        // Search Input Debounce
        searchInput?.addEventListener("input", () => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                state.keyword = searchInput.value.trim();
                state.page = 1;
                filterAndRenderTable();
            }, 300);
        });

        // Status Filter
        statusFilter?.addEventListener("change", () => {
            state.status = statusFilter.value;
            state.page = 1;
            filterAndRenderTable();
        });

        // Industry Filter
        industryFilter?.addEventListener("change", () => {
            state.industryId = industryFilter.value;
            state.page = 1;
            filterAndRenderTable();
        });

        // Scope Filter
        scopeFilter?.addEventListener("change", () => {
            state.scopeFilter = scopeFilter.value;
            state.page = 1;
            filterAndRenderTable();
        });

        // Page Size
        pageSizeSelect?.addEventListener("change", () => {
            state.pageSize = Number(pageSizeSelect.value) || 20;
            state.page = 1;
            filterAndRenderTable();
        });

        // Reset Filters Button
        document.getElementById("btnResetFilters")?.addEventListener("click", () => {
            searchInput.value = "";
            statusFilter.value = "";
            industryFilter.value = "";
            scopeFilter.value = "ALL";
            isDuplicateOnlyFilter = false;
            if (btnQuickDuplicates) {
                btnQuickDuplicates.classList.remove("crm-btn-primary");
                btnQuickDuplicates.classList.add("crm-btn-secondary");
            }
            state.keyword = "";
            state.status = "";
            state.industryId = "";
            state.companySizeId = "";
            state.region = "";
            state.ownerUserId = "";
            state.scopeFilter = "ALL";
            state.page = 1;
            filterAndRenderTable();
        });

        // CRM-64: Quick Filter for Duplicates Button
        btnQuickDuplicates?.addEventListener("click", () => {
            isDuplicateOnlyFilter = !isDuplicateOnlyFilter;
            if (isDuplicateOnlyFilter) {
                btnQuickDuplicates.classList.add("crm-btn-primary");
                btnQuickDuplicates.classList.remove("crm-btn-secondary");
            } else {
                btnQuickDuplicates.classList.remove("crm-btn-primary");
                btnQuickDuplicates.classList.add("crm-btn-secondary");
            }
            state.page = 1;
            filterAndRenderTable();
        });

        // Listen to CRM-67 Customer Filter Change Event
        window.addEventListener("customerFilterChange", (event) => {
            const criteria = event.detail || {};
            state.keyword = criteria.keyword || "";
            state.status = criteria.status || "";
            state.industryId = criteria.industryId || "";
            state.companySizeId = criteria.companySizeId || "";
            state.region = criteria.region || "";
            state.ownerUserId = criteria.ownerUserId || "";
            state.scopeFilter = criteria.scopeFilter || "ALL";
            applyUserScopePermissions(currentSessionUser);
            isDuplicateOnlyFilter = Boolean(criteria.isDuplicateOnly);

            if (btnQuickDuplicates) {
                if (isDuplicateOnlyFilter) {
                    btnQuickDuplicates.classList.add("crm-btn-primary");
                    btnQuickDuplicates.classList.remove("crm-btn-secondary");
                } else {
                    btnQuickDuplicates.classList.remove("crm-btn-primary");
                    btnQuickDuplicates.classList.add("crm-btn-secondary");
                }
            }

            state.page = 1;
            filterAndRenderTable();
        });

        // KPI Card click filter
        document.querySelectorAll(".customer-kpi-card[data-filter-status]").forEach(card => {
            card.addEventListener("click", () => {
                const targetStatus = card.dataset.filterStatus;
                if (statusFilter) statusFilter.value = targetStatus;
                state.status = targetStatus;
                state.page = 1;
                filterAndRenderTable();
            });
        });

        // Sorting
        document.querySelectorAll("th.sortable").forEach(th => {
            th.addEventListener("click", () => {
                const sortField = th.dataset.sort;
                if (state.sortField === sortField) {
                    state.sortOrder = state.sortOrder === "asc" ? "desc" : "asc";
                } else {
                    state.sortField = sortField;
                    state.sortOrder = "asc";
                }

                document.querySelectorAll("th.sortable").forEach(el => el.classList.remove("sorted-asc", "sorted-desc"));
                th.classList.add(state.sortOrder === "asc" ? "sorted-asc" : "sorted-desc");
                filterAndRenderTable();
            });
        });

        // Check All Rows
        checkAllRows?.addEventListener("change", () => {
            const checked = checkAllRows.checked;
            document.querySelectorAll(".row-checkbox").forEach(cb => {
                cb.checked = checked;
                const id = Number(cb.dataset.id);
                if (checked) state.selectedIds.add(id);
                else state.selectedIds.delete(id);
            });
            updateSelectionSummary();
        });

        // Table Delegate (Checkbox, Edit, Delete, Quick Status, Merge Modal)
        tableBody?.addEventListener("click", event => {
            // CRM-64: Trigger Merge Modal (From Badge or Action Button)
            const mergeTrigger = event.target.closest(".btn-trigger-merge");
            if (mergeTrigger) {
                const idA = Number(mergeTrigger.dataset.idA);
                const idB = Number(mergeTrigger.dataset.idB);
                openMergeModal(idA, idB);
                return;
            }

            // Checkbox
            const checkbox = event.target.closest(".row-checkbox");
            if (checkbox) {
                const id = Number(checkbox.dataset.id);
                if (checkbox.checked) state.selectedIds.add(id);
                else state.selectedIds.delete(id);
                updateSelectionSummary();
                return;
            }

            // Edit
            const editBtn = event.target.closest(".btn-edit");
            if (editBtn) {
                openDrawer(Number(editBtn.dataset.id));
                return;
            }

            // Delete
            const deleteBtn = event.target.closest(".btn-delete");
            if (deleteBtn) {
                handleDeleteCustomer(Number(deleteBtn.dataset.id));
                return;
            }

            // Quick Status Pill
            const statusPill = event.target.closest("[data-quick-status]");
            if (statusPill) {
                openQuickStatus(Number(statusPill.dataset.quickStatus));
                return;
            }
        });

        // Drawer
        openDrawerBtn?.addEventListener("click", () => openDrawer(null));
        closeDrawerBtn?.addEventListener("click", closeDrawer);
        cancelDrawerBtn?.addEventListener("click", closeDrawer);
        drawerOverlay?.addEventListener("click", closeDrawer);
        customerForm?.addEventListener("submit", handleSaveCustomer);
        document.getElementById("btnQuickAddIndustry")?.addEventListener("click", () => handleQuickAddCatalog("industry", "ngành nghề"));
        document.getElementById("btnQuickAddCompanySize")?.addEventListener("click", () => handleQuickAddCatalog("company-size", "quy mô doanh nghiệp"));

        // Realtime input validations & duplicate scanning
        document.getElementById("companyName")?.addEventListener("input", () => validateForm());
        document.getElementById("taxCode")?.addEventListener("input", () => validateForm());
        document.getElementById("customerEmail")?.addEventListener("input", () => validateForm());
        document.getElementById("website")?.addEventListener("input", () => validateForm());

        // Quick Status Modal
        btnCancelQuickStatus?.addEventListener("click", closeQuickStatus);
        btnConfirmQuickStatus?.addEventListener("click", handleConfirmQuickStatus);
        quickModalOverlay?.addEventListener("click", closeQuickStatus);

        // CRM-64: Merge Modal Events
        btnCloseMergeModal?.addEventListener("click", closeMergeModal);
        btnCancelMerge?.addEventListener("click", closeMergeModal);
        mergeModalOverlay?.addEventListener("click", closeMergeModal);
        btnConfirmMerge?.addEventListener("click", handleConfirmMerge);

        // Master Record Switcher
        cardRecordA?.addEventListener("click", () => switchMasterRecord("A"));
        cardRecordB?.addEventListener("click", () => switchMasterRecord("B"));
        radioSelectMasterA?.addEventListener("change", () => switchMasterRecord("A"));
        radioSelectMasterB?.addEventListener("change", () => switchMasterRecord("B"));

        // Export Excel
        document.getElementById("btnExportExcel")?.addEventListener("click", exportToExcel);
        document.getElementById("btnImportExcel")?.addEventListener("click", () => {
            if (window.CustomerImportEngine) {
                window.CustomerImportEngine.open();
            }
        });

        // Logout
        document.getElementById("btnLogout")?.addEventListener("click", async () => {
            try {
                await fetch(API_BASE + "/api/auth/logout", { method: "POST", credentials: "include" });
            } catch (_) {}
            localStorage.removeItem("crm_ui_session");
            window.location.href = "login.html";
        });

        // Escape Key
        document.addEventListener("keydown", e => {
            if (e.key === "Escape") {
                closeDrawer();
                closeQuickStatus();
                closeMergeModal();
            }
        });
    }

    /* =========================================================
       8. UTILITIES & HELPERS
    ========================================================= */
    function updateSelectionSummary() {
        const text = document.getElementById("selectedCountText");
        if (!text) return;
        const count = state.selectedIds.size;
        if (count > 0) {
            text.innerHTML = `Đã chọn <strong>${count}</strong> khách hàng`;
        } else {
            text.textContent = "Hiển thị danh sách khách hàng doanh nghiệp";
        }
    }

    function setLoading(isLoading) {
        if (loadingOverlay) {
            if (isLoading) loadingOverlay.classList.add("active");
            else loadingOverlay.classList.remove("active");
        }
    }

    function renderErrorState(message) {
        if (tableBody) {
            tableBody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:32px; color:var(--crm-danger); font-weight:600;">⚠️ ${escapeHtml(message)}</td></tr>`;
        }
    }

    function setFieldError(groupEl, msgEl, text) {
        if (groupEl) groupEl.classList.add("has-error");
        if (msgEl) msgEl.textContent = text;
    }

    function clearFieldError(groupEl) {
        if (groupEl) groupEl.classList.remove("has-error");
    }

    function getVal(id) {
        const el = document.getElementById(id);
        return el ? el.value.trim() : "";
    }

    function setVal(id, value) {
        const el = document.getElementById(id);
        if (el) el.value = value !== null && value !== undefined ? String(value) : "";
    }

    function setElText(id, text) {
        const el = document.getElementById(id);
        if (el) el.textContent = String(text ?? "—");
    }

    function getStatusLabel(status) {
        switch (normalizeStatus(status)) {
            case "TIEM_NANG": return "Tiềm năng (Lead)";
            case "DANG_GIAO_DICH": return "Đang giao dịch";
            case "CHINH_THUC": return "Khách hàng (Won)";
            case "NGUNG_HOP_TAC": return "Ngừng hợp tác";
            case "DA_GOP": return "Đã gộp (Merged)";
            default: return status || "Tiềm năng";
        }
    }

    function getStatusPillClass(status) {
        switch (normalizeStatus(status)) {
            case "TIEM_NANG": return "status-prospect";
            case "DANG_GIAO_DICH": return "status-dealing";
            case "CHINH_THUC": return "status-customer";
            case "NGUNG_HOP_TAC": return "status-inactive";
            case "DA_GOP": return "status-merged";
            default: return "status-prospect";
        }
    }

    function normalizeStatus(status) {
        const s = String(status || "").toUpperCase();
        if (s.includes("GOP") || s.includes("MERGE")) return "DA_GOP";
        if (s.includes("TIEM") || s.includes("LEAD") || s === "PROSPECT") return "TIEM_NANG";
        if (s.includes("GIAO_DICH") || s.includes("NEGOTIAT") || s === "ACTIVE") return "DANG_GIAO_DICH";
        if (s.includes("CHINH") || s.includes("KHACH") || s === "WON") return "CHINH_THUC";
        if (s.includes("NGUNG") || s.includes("CHURN") || s === "INACTIVE") return "NGUNG_HOP_TAC";
        return "TIEM_NANG";
    }

    function getInitials(name) {
        if (!name) return "U";
        const parts = name.trim().split(/\s+/);
        if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
        return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    function formatUrl(url) {
        if (!url) return "#";
        if (!/^https?:\/\//i.test(url)) return "https://" + url;
        return url;
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
        const toast = document.createElement("div");
        toast.className = `crm-toast crm-toast-${type}`;
        toast.style.cssText = "position:fixed; bottom:24px; right:24px; z-index:1100; min-width:300px; padding:14px 18px; border-radius:8px; box-shadow:var(--crm-shadow); animation:fadeInUp 0.3s ease; display:flex; align-items:center; gap:10px; background:var(--crm-surface); border:1px solid var(--crm-border);";

        const icon = type === "success" ? "✓" : "⚠️";
        toast.innerHTML = `<span style="font-weight:bold; color:var(--crm-${type}); font-size:16px;">${icon}</span> <span style="font-size:13.5px; color:var(--crm-text);">${escapeHtml(message)}</span>`;

        document.body.appendChild(toast);
        setTimeout(() => {
            toast.style.opacity = "0";
            toast.style.transition = "opacity 0.3s ease";
            setTimeout(() => toast.remove(), 300);
        }, 3200);
    }

    // Auto-bootstrap
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
