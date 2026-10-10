/**
 * ===================================================================
 * ENTERPRISE B2B CUSTOMER MANAGEMENT CONTROLLER (CRM-61 & CRM-64)
 * - Quản lý hồ sơ khách hàng doanh nghiệp, tra cứu MST, vòng đời
 * - CRM-64: Phát hiện và cảnh báo trùng lặp (MST, Website, Tên fuzzy)
 * - CRM-64: Giao diện so sánh đối chiếu 2 cột (Side-by-Side Merge Modal)
 * - CRM-64: Phân quyền gộp dữ liệu: Trưởng nhóm kinh doanh / Admin vs Sale Rep
 * - Bền vững dữ liệu qua LocalStorage: CRM_CUSTOMERS_DATA & CRM_DUPLICATE_MERGE_LOGS
 * ===================================================================
 */

"use strict";

(function () {
    const API_BASE = "http://localhost:8080/crm";

    // Master Catalogues (Categories)
    const INDUSTRIES = [
        { id: 1, name: "Công nghệ thông tin & Viễn thông", code: "IT" },
        { id: 2, name: "Bán lẻ & Thương mại điện tử", code: "RETAIL" },
        { id: 3, name: "Sản xuất & Chế tạo", code: "MANUFACTURING" },
        { id: 4, name: "Tài chính - Ngân hàng - Bảo hiểm", code: "FINANCE" },
        { id: 5, name: "Bất động sản & Xây dựng", code: "REAL_ESTATE" },
        { id: 6, name: "Giáo dục & Đào tạo", code: "EDUCATION" },
        { id: 7, name: "Y tế & Dược phẩm", code: "HEALTHCARE" },
        { id: 8, name: "Ngành nghề khác", code: "OTHER" }
    ];

    const COMPANY_SIZES = [
        { id: 9, name: "Dưới 10 nhân sự (Siêu nhỏ)", code: "MICRO" },
        { id: 10, name: "10 - 50 nhân sự (Nhỏ)", code: "SMALL" },
        { id: 11, name: "51 - 200 nhân sự (Vừa)", code: "MEDIUM" },
        { id: 12, name: "201 - 500 nhân sự (Lớn)", code: "LARGE" },
        { id: 13, name: "Trên 500 nhân sự (Tập đoàn)", code: "ENTERPRISE" }
    ];

    const STORAGE_KEY_CUSTOMERS = "CRM_CUSTOMERS_DATA";

    const DEFAULT_MOCK_CUSTOMERS = [
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
            createdAt: "2026-01-05T08:00:00"
        },
        {
            id: 7,
            companyName: "Công ty CP Dược phẩm Imexpharm",
            name: "Công ty CP Dược phẩm Imexpharm",
            taxCode: "1400384433",
            status: "TIEM_NANG",
            industryId: 7,
            companySizeId: 11,
            email: "imexpharm@imexpharm.com",
            phone: "02773851941",
            website: "https://imexpharm.com",
            address: "Số 04 Đường 30/4, Phường 1, TP. Cao Lãnh, Đồng Tháp",
            ownerUserId: 1,
            ownerName: "Nông Quang Tiệp",
            owner: "Nông Quang Tiệp",
            createdAt: "2026-03-12T16:10:00"
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
            phone: "02462848666",
            website: "https://hoaphat.com.vn",
            address: "KCN Phố Nối A, Xã Giai Phạm, Huyện Yên Mỹ, Hưng Yên",
            ownerUserId: 2,
            ownerName: "Nguyễn Văn An",
            owner: "Nguyễn Văn An",
            createdAt: "2026-02-01T13:30:00"
        }
    ];

    function getLocalCustomers() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY_CUSTOMERS);
            if (raw) {
                const parsed = JSON.parse(raw);
                if (Array.isArray(parsed) && parsed.length > 0) return parsed;
            }
        } catch (_) {}
        saveLocalCustomers(DEFAULT_MOCK_CUSTOMERS);
        return [...DEFAULT_MOCK_CUSTOMERS];
    }

    function saveLocalCustomers(list) {
        try {
            localStorage.setItem(STORAGE_KEY_CUSTOMERS, JSON.stringify(list));
        } catch (_) {}
    }

    // State
    let customersList = [];
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
        const config = {
            credentials: "include",
            headers: {
                "Accept": "application/json",
                ...(options.body ? { "Content-Type": "application/json" } : {}),
                ...(options.headers || {})
            },
            ...options
        };

        let response;
        try {
            response = await fetch(API_BASE + path, config);
        } catch (netErr) {
            console.warn("Network error calling Backend, falling back to LocalStorage:", netErr);
            throw new Error("Không thể kết nối Backend CRM (Sử dụng dữ liệu LocalStorage).");
        }

        let result = null;
        try {
            result = await response.json();
        } catch (_) {
            result = null;
        }

        if (response.status === 401) {
            console.warn("Chưa đăng nhập Backend, tiếp tục với Mock Session người dùng.");
            throw new Error("Chưa đăng nhập");
        }

        if (!response.ok || !result?.success) {
            const err = new Error(result?.message || `Lỗi máy chủ HTTP ${response.status}`);
            err.status = response.status;
            err.data = result?.data;
            throw err;
        }

        return result.data;
    }

    /* =========================================================
       2. INITIALIZATION (CACHE-FIRST INSTANT RENDER)
    ========================================================= */
    function init() {
        populateSelectOptions();
        bindEvents();

        // 1. Render Cache-First tức thì (0ms) từ LocalStorage
        if (window.DuplicateMergeEngine) {
            customersList = window.DuplicateMergeEngine.getStoredCustomers();
        } else {
            customersList = getLocalCustomers();
        }

        state.totalItems = Number(customersList.length);
        state.totalPages = Math.max(1, Math.ceil(customersList.length / state.pageSize));
        currentScope = "ALL";

        scanDuplicates();
        filterAndRenderTable();
        updateKpiStats();

        // 2. Chạy ngầm session & users và sync backend (non-blocking)
        loadSession().catch(() => {});
        loadUsers().catch(() => {});
        syncBackendCustomers().catch(() => {});
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
        try {
            currentSessionUser = await apiRequest("/api/auth/session");
        } catch (e) {
            // Default demo user: Nông Quang Tiệp (Trưởng nhóm kinh doanh / Admin)
            currentSessionUser = {
                id: 100,
                fullName: "Nông Quang Tiệp",
                email: "tiepnq@corporate-crm.vn",
                roles: [{ id: 1, name: "Trưởng nhóm kinh doanh (Team Lead)" }],
                role: "TEAM_LEAD",
                teamId: 10,
                teamName: "Kinh doanh B2B Miền Bắc"
            };
        }

        if (currentSessionUser) {
            const topUserName = document.getElementById("topUserName");
            const topUserRole = document.getElementById("topUserRole");
            const topAvatar = document.getElementById("topAvatar");
            if (topUserName) topUserName.textContent = currentSessionUser.fullName || "Người dùng";
            if (topUserRole) topUserRole.textContent = (currentSessionUser.roles || []).map(r => r.name).join(", ") || "Trưởng nhóm kinh doanh";
            if (topAvatar) topAvatar.textContent = (currentSessionUser.fullName || "N").charAt(0).toUpperCase();
        }
    }

    async function loadUsers() {
        try {
            const data = await apiRequest("/api/users?size=100");
            usersList = data?.items || [];
        } catch (e) {
            // Fallback mock users
            usersList = [
                { id: 100, fullName: "Nông Quang Tiệp", email: "tiepnq@crm.vn", teamName: "Kinh doanh B2B (Trưởng nhóm)", role: "TEAM_LEAD" },
                { id: 101, fullName: "Nguyễn Văn An", email: "annv@crm.vn", teamName: "Nhóm B2B Hà Nội", role: "SALE" },
                { id: 102, fullName: "Trần Thị Mai", email: "maitt@crm.vn", teamName: "Nhóm B2B Hà Nội", role: "SALE" },
                { id: 103, fullName: "Lê Hoàng Nam", email: "namlh@crm.vn", teamName: "Nhóm Khách hàng Doanh nghiệp", role: "SALE" },
                { id: 104, fullName: "Phạm Thu Thảo", email: "thaopt@crm.vn", teamName: "Nhóm Khách hàng Doanh nghiệp", role: "SALE" },
                { id: 105, fullName: "Vũ Đức Hùng", email: "hungvd@crm.vn", teamName: "Nhóm Khách hàng Miền Nam", role: "SALE" }
            ];
        }

        const ownerSelect = document.getElementById("ownerSelect");
        if (ownerSelect) {
            ownerSelect.innerHTML = '<option value="">-- Mặc định: Bản thân tôi --</option>';
            usersList.forEach(u => {
                const opt = document.createElement("option");
                opt.value = String(u.id);
                opt.textContent = `${u.fullName || u.email} (${u.teamName || "Chưa phân nhóm"})`;
                ownerSelect.appendChild(opt);
            });
        }

        // Đồng bộ sang dropdown filter người phụ trách của CRM-67
        if (window.CustomerFilterManager?.populateOwners) {
            window.CustomerFilterManager.populateOwners(usersList);
        }
    }

    /* =========================================================
       3. LOAD & RENDER CUSTOMERS (WITH DUPLICATE SCANNING)
    ========================================================= */
    async function syncBackendCustomers() {
        try {
            const params = new URLSearchParams();
            params.set("page", String(state.page));
            params.set("size", String(state.pageSize));
            if (state.keyword) params.set("keyword", state.keyword);
            if (state.status) params.set("status", state.status);
            const data = await apiRequest(`/api/customers?${params.toString()}`);
            if (data && Array.isArray(data.items) && data.items.length > 0) {
                customersList = data.items;
                saveLocalCustomers(customersList);
                state.totalItems = Number(customersList.length);
                state.totalPages = Math.max(1, Math.ceil(customersList.length / state.pageSize));
                scanDuplicates();
                filterAndRenderTable();
                updateKpiStats();
            }
        } catch (_) {
            // Không kết nối được API Backend -> tiếp tục sử dụng dữ liệu bền vững từ LocalStorage
        }
    }

    async function loadCustomers() {
        setLoading(true);
        try {
            // 1. Nạp từ LocalStorage CRM_CUSTOMERS_DATA
            if (window.DuplicateMergeEngine) {
                customersList = window.DuplicateMergeEngine.getStoredCustomers();
            } else {
                customersList = getLocalCustomers();
            }

            state.totalItems = Number(customersList.length);
            state.totalPages = Math.max(1, Math.ceil(customersList.length / state.pageSize));
            currentScope = "ALL";

            // Update Global Scope Badge
            const scopeGlobalBadge = document.getElementById("scopeGlobalBadge");
            if (scopeGlobalBadge) {
                scopeGlobalBadge.textContent = `Phạm vi: ${currentScope}`;
                scopeGlobalBadge.className = `scope-badge scope-${currentScope.toLowerCase()}`;
            }

            // Quét phát hiện trùng lặp tự động (CRM-64)
            scanDuplicates();

            filterAndRenderTable();
            updateKpiStats();

            // Thử đồng bộ Backend nếu trực tuyến
            await syncBackendCustomers();
        } catch (err) {
            console.error("Lỗi tải khách hàng:", err);
            customersList = getLocalCustomers();
            filterAndRenderTable();
            updateKpiStats();
        } finally {
            setLoading(false);
        }
    }

    /**
     * Tự động quét và phát hiện trùng lặp trên toàn bộ danh sách khách hàng
     */
    function scanDuplicates() {
        if (!window.DuplicateMergeEngine) return;
        duplicatesMap = window.DuplicateMergeEngine.scanAllDuplicates(customersList);

        // Cập nhật số lượng lên Badge nút xem nhanh trên thanh công cụ
        const dupCount = duplicatesMap.size;
        if (badgeDuplicatesCount) {
            badgeDuplicatesCount.textContent = String(dupCount);
        }
    }

    function filterAndRenderTable() {
        // Tái quét trùng lặp để dữ liệu luôn phản ánh mới nhất
        scanDuplicates();

        let displayList = [...customersList];

        // 0. Bộ lọc Khách hàng trùng lặp tiềm ẩn (CRM-64)
        if (isDuplicateOnlyFilter) {
            displayList = displayList.filter(item => duplicatesMap.has(Number(item.id)));
        }

        // 1. Client-side Smart Search (Tên công ty, MST, SĐT, Email)
        if (state.keyword) {
            const kw = state.keyword.toLowerCase().trim();
            displayList = displayList.filter(item => {
                const nameMatch = (item.companyName || item.name || "").toLowerCase().includes(kw);
                const taxMatch = (item.taxCode || "").toLowerCase().includes(kw);
                const phoneMatch = (item.phone || "").toLowerCase().includes(kw);
                const emailMatch = (item.email || "").toLowerCase().includes(kw);
                return nameMatch || taxMatch || phoneMatch || emailMatch;
            });
        }

        // 2. Client-side Status filter
        if (state.status) {
            displayList = displayList.filter(item => normalizeStatus(item.status) === state.status);
        }

        // 3. Client-side Industry filter
        if (state.industryId) {
            displayList = displayList.filter(item => String(item.industryId) === String(state.industryId));
        }

        // 4. Client-side Company Size filter (CRM-67)
        if (state.companySizeId) {
            displayList = displayList.filter(item => String(item.companySizeId) === String(state.companySizeId));
        }

        // 5. Client-side Geographical Region filter (CRM-67)
        if (state.region) {
            displayList = displayList.filter(item => {
                const addr = (item.address || "").toLowerCase();
                if (state.region === "MB") {
                    return addr.includes("hà nội") || addr.includes("hải phòng") || addr.includes("quảng ninh") ||
                           addr.includes("thái nguyên") || addr.includes("bắc ninh") || addr.includes("vĩnh phúc") ||
                           addr.includes("hải dương") || addr.includes("hưng yên") || addr.includes("nam định");
                } else if (state.region === "MT") {
                    return addr.includes("đà nẵng") || addr.includes("huế") || addr.includes("khánh hòa") ||
                           addr.includes("nha trang") || addr.includes("nghệ an") || addr.includes("hà tĩnh") ||
                           addr.includes("quảng nam") || addr.includes("bình định");
                } else if (state.region === "MN") {
                    return addr.includes("hồ chí minh") || addr.includes("tp.hcm") || addr.includes("tphcm") ||
                           addr.includes("bình dương") || addr.includes("đồng nai") || addr.includes("cần thơ") ||
                           addr.includes("long an") || addr.includes("bà rịa") || addr.includes("vũng tàu");
                }
                return true;
            });
        }

        // 6. Client-side Owner User filter (CRM-67)
        if (state.ownerUserId) {
            displayList = displayList.filter(item => String(item.ownerUserId) === String(state.ownerUserId));
        }

        // 7. Client-side Scope filter (SELF vs TEAM)
        if (state.scopeFilter === "SELF" && currentSessionUser?.id) {
            displayList = displayList.filter(item => Number(item.ownerUserId) === Number(currentSessionUser.id));
        } else if (state.scopeFilter === "TEAM" && currentSessionUser?.teamId) {
            displayList = displayList.filter(item => Number(item.ownerUserId) !== Number(currentSessionUser.id));
        }

        // Cập nhật số lượng tìm thấy lên Badge (CRM-67)
        if (window.CustomerFilterManager?.updateMatchCount) {
            window.CustomerFilterManager.updateMatchCount(displayList.length, customersList.length);
        }

        // Sorting
        displayList.sort((a, b) => {
            let valA = a[state.sortField] || "";
            let valB = b[state.sortField] || "";

            if (typeof valA === "string") valA = valA.toLowerCase();
            if (typeof valB === "string") valB = valB.toLowerCase();

            if (valA < valB) return state.sortOrder === "asc" ? -1 : 1;
            if (valA > valB) return state.sortOrder === "asc" ? 1 : -1;
            return 0;
        });

        // Phân trang
        const total = displayList.length;
        state.totalItems = total;
        state.totalPages = Math.max(1, Math.ceil(total / state.pageSize));
        if (state.page > state.totalPages) state.page = 1;

        const startIndex = (state.page - 1) * state.pageSize;
        const pageItems = displayList.slice(startIndex, startIndex + state.pageSize);

        renderTableRows(pageItems);
        renderPagination();
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
                : `<span class="scope-badge scope-team">Nhóm (TEAM)</span>`;

            // CRM-68: Churn Risk Flagging
            let churnInfo = null;
            if (window.SupportTicketsManager && typeof window.SupportTicketsManager.getChurnRiskStatus === "function") {
                churnInfo = window.SupportTicketsManager.getChurnRiskStatus(record.id);
            } else {
                try {
                    const rawTickets = localStorage.getItem("CRM_SUPPORT_TICKETS_DATA");
                    const allTickets = rawTickets ? JSON.parse(rawTickets) : [];
                    const openTickets = allTickets.filter(t => Number(t.customerId) === Number(record.id) && t.status !== "RESOLVED" && t.status !== "CLOSED");
                    const hasSevere = openTickets.some(t => t.priority === "URGENT" || t.priority === "HIGH");
                    const hasMulti = openTickets.length >= 2;
                    const rawChurn = localStorage.getItem("CRM_CHURN_RISK_DATA");
                    const churnFlags = rawChurn ? JSON.parse(rawChurn) : {};
                    const isManual = !!churnFlags[record.id];
                    if (hasSevere || hasMulti || isManual) {
                        churnInfo = { isRisk: true, reasons: ["Khách hàng có nguy cơ rời bỏ cao"] };
                    }
                } catch (_) {}
            }
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

        if (statTotal) statTotal.textContent = String(customersList.length);

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
            setFieldError(groupTax, errorTax, "Mã số thuế doanh nghiệp là bắt buộc.");
            isValid = false;
        } else {
            const cleanTax = taxVal.replace(/[^0-9-]/g, "");
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
            const urlRegex = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/i;
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

        if (!validateForm()) {
            return;
        }

        const id = getVal("customerId");
        const companyName = getVal("companyName");
        const taxCode = getVal("taxCode").replace(/[^0-9-]/g, "");
        const status = getVal("customerStatus") || "TIEM_NANG";
        const email = getVal("customerEmail");
        const phone = getVal("customerPhone");
        const website = getVal("website");
        const address = getVal("address");
        const industryId = getVal("industrySelect") ? Number(getVal("industrySelect")) : 1;
        const companySizeId = getVal("companySizeSelect") ? Number(getVal("companySizeSelect")) : 11;
        const ownerUserId = getVal("ownerSelect") ? Number(getVal("ownerSelect")) : (currentSessionUser?.id || 100);

        const assignedUser = usersList.find(u => Number(u.id) === Number(ownerUserId));
        const ownerName = assignedUser?.fullName || currentSessionUser?.fullName || "Nông Quang Tiệp";

        const submitBtn = document.getElementById("btnSaveCustomer");
        const origText = submitBtn.innerHTML;
        submitBtn.disabled = true;
        submitBtn.innerHTML = `<span class="crm-spinner" style="width:16px;height:16px;margin-right:6px;"></span> Đang lưu...`;

        try {
            if (!id) {
                // CREATE NEW CUSTOMER
                const newId = customersList.length ? Math.max(...customersList.map(c => Number(c.id) || 0)) + 1 : 1;
                const newCustomer = {
                    id: newId,
                    name: companyName,
                    companyName: companyName,
                    taxCode: taxCode,
                    status: status,
                    email: email,
                    phone: phone,
                    website: website,
                    address: address,
                    industryId: industryId,
                    companySizeId: companySizeId,
                    ownerUserId: ownerUserId,
                    ownerName: ownerName,
                    revenue: "Chưa cập nhật",
                    contacts: [],
                    deals: [],
                    activities: [
                        {
                            id: Date.now(),
                            type: "Khởi tạo hồ sơ",
                            summary: "Khai báo mới hồ sơ doanh nghiệp trên hệ thống CRM",
                            date: new Date().toISOString().slice(0, 10),
                            user: currentSessionUser?.fullName || "Nông Quang Tiệp"
                        }
                    ],
                    tickets: []
                };

                customersList.unshift(newCustomer);
                window.DuplicateMergeEngine?.saveStoredCustomers(customersList);
                showToast("Tạo mới hồ sơ khách hàng doanh nghiệp thành công!", "success");
            } else {
                // UPDATE CUSTOMER
                const idx = customersList.findIndex(c => Number(c.id) === Number(id));
                if (idx !== -1) {
                    customersList[idx] = {
                        ...customersList[idx],
                        name: companyName,
                        companyName: companyName,
                        taxCode: taxCode,
                        status: status,
                        email: email,
                        phone: phone,
                        website: website,
                        address: address,
                        industryId: industryId,
                        companySizeId: companySizeId,
                        ownerUserId: ownerUserId,
                        ownerName: ownerName
                    };
                    window.DuplicateMergeEngine?.saveStoredCustomers(customersList);
                    showToast("Cập nhật thông tin khách hàng thành công!", "success");
                }
            }

            // CRM-65: Synchronize Parent Association
            const parentSelect = document.getElementById("customerParentSelect");
            if (parentSelect && window.CorporateHierarchyManager) {
                const targetCustId = id ? Number(id) : (customersList[0]?.id || 1);
                const pVal = parentSelect.value ? Number(parentSelect.value) : null;
                try {
                    await window.CorporateHierarchyManager.apiUpdateParent(targetCustId, pVal);
                } catch (_) {}
            }

            closeDrawer();
            filterAndRenderTable();
            updateKpiStats();
        } catch (err) {
            console.error("Save customer error:", err);
            alert(err.message || "Không thể lưu thông tin khách hàng.");
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = origText;
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
        const newStatus = quickModalSelect.value;

        const idx = customersList.findIndex(c => Number(c.id) === Number(quickActionCustomer.id));
        if (idx !== -1) {
            customersList[idx].status = newStatus;
            window.DuplicateMergeEngine?.saveStoredCustomers(customersList);
            showToast(`Đã chuyển trạng thái sang "${getStatusLabel(newStatus)}"`, "success");
        }

        closeQuickStatus();
        filterAndRenderTable();
        updateKpiStats();
    }

    async function handleDeleteCustomer(customerId) {
        const customer = customersList.find(c => Number(c.id) === Number(customerId));
        if (!customer) return;

        const confirmed = window.confirm(`Bạn có chắc chắn muốn xóa khách hàng "${customer.companyName || customer.name}" khỏi hệ thống?`);
        if (!confirmed) return;

        customersList = customersList.filter(c => Number(c.id) !== Number(customerId));
        window.DuplicateMergeEngine?.saveStoredCustomers(customersList);
        showToast(`Đã xóa khách hàng "${customer.companyName || customer.name}" thành công!`, "success");

        filterAndRenderTable();
        updateKpiStats();
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

    function openMergeModal(idA, idB) {
        if (!window.DuplicateMergeEngine) {
            alert("Công cụ gộp khách hàng đang tải, vui lòng thử lại.");
            return;
        }

        const recordA = customersList.find(c => Number(c.id) === Number(idA));
        const recordB = customersList.find(c => Number(c.id) === Number(idB));

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
                taxCode: recordA.taxCode || "",
                website: recordA.website || "",
                industryId: recordA.industryId,
                companySizeId: recordA.companySizeId,
                ownerUserId: recordA.ownerUserId,
                email: recordA.email || "",
                phone: recordA.phone || "",
                address: recordA.address || "",
                status: recordA.status || "TIEM_NANG",
                revenue: recordA.revenue || "Chưa cập nhật"
            },
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
        const countContactsA = (recordA.contacts || []).length;
        const countContactsB = (recordB.contacts || []).length;
        setElText("txtContactsCount", `${countContactsA} + ${countContactsB} = ${countContactsA + countContactsB} liên hệ`);

        const countDealsA = (recordA.deals || []).length;
        const countDealsB = (recordB.deals || []).length;
        setElText("txtDealsCount", `${countDealsA} + ${countDealsB} = ${countDealsA + countDealsB} cơ hội`);

        const countActsA = (recordA.activities || []).length;
        const countActsB = (recordB.activities || []).length;
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
                valA: recordA.taxCode || "",
                valB: recordB.taxCode || "",
                displayValA: recordA.taxCode || "—",
                displayValB: recordB.taxCode || "—"
            },
            {
                key: "website",
                label: "Website doanh nghiệp",
                valA: recordA.website || "",
                valB: recordB.website || "",
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
                key: "revenue",
                label: "Doanh thu / Quy mô tài chính",
                valA: recordA.revenue || "Chưa cập nhật",
                valB: recordB.revenue || "Chưa cập nhật",
                displayValA: recordA.revenue || "Chưa cập nhật",
                displayValB: recordB.revenue || "Chưa cập nhật"
            },
            {
                key: "phone",
                label: "Số điện thoại / Hotline",
                valA: recordA.phone || "",
                valB: recordB.phone || "",
                displayValA: recordA.phone || "—",
                displayValB: recordB.phone || "—"
            },
            {
                key: "email",
                label: "Email đại diện",
                valA: recordA.email || "",
                valB: recordB.email || "",
                displayValA: recordA.email || "—",
                displayValB: recordB.email || "—"
            },
            {
                key: "address",
                label: "Địa chỉ trụ sở văn phòng",
                valA: recordA.address || "",
                valB: recordB.address || "",
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
            const isAChecked = String(currentChoiceValue) === String(f.valA);
            const isBChecked = !isAChecked && String(currentChoiceValue) === String(f.valB);

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
                    renderSideBySideTable();
                });
            });

            tbody.appendChild(tr);
        });
    }

    function switchMasterRecord(newMaster) {
        if (!currentMergeSession) return;
        currentMergeSession.masterKey = newMaster;

        const targetMasterRecord = newMaster === "A" ? currentMergeSession.recordA : currentMergeSession.recordB;
        currentMergeSession.fieldOverrides = {
            companyName: targetMasterRecord.companyName || targetMasterRecord.name,
            taxCode: targetMasterRecord.taxCode || "",
            website: targetMasterRecord.website || "",
            industryId: targetMasterRecord.industryId,
            companySizeId: targetMasterRecord.companySizeId,
            ownerUserId: targetMasterRecord.ownerUserId,
            email: targetMasterRecord.email || "",
            phone: targetMasterRecord.phone || "",
            address: targetMasterRecord.address || "",
            status: targetMasterRecord.status || "TIEM_NANG",
            revenue: targetMasterRecord.revenue || "Chưa cập nhật"
        };

        renderMergeModal();
    }

    function updateRolePermissionUI() {
        const role = window.DuplicateMergeEngine?.getSimulatedRole() || "TEAM_LEAD";
        if (roleSimulatorSelect) roleSimulatorSelect.value = role;

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

    function handleConfirmMerge() {
        if (!currentMergeSession) return;
        const role = window.DuplicateMergeEngine?.getSimulatedRole() || "TEAM_LEAD";

        if (!window.DuplicateMergeEngine?.canExecuteMerge(role)) {
            alert("Bạn không có quyền thực hiện gộp khách hàng. Vui lòng chuyển vai trò sang 'Trưởng nhóm' để kiểm thử.");
            return;
        }

        const { recordA, recordB, masterKey, fieldOverrides, reasons } = currentMergeSession;
        const master = masterKey === "A" ? recordA : recordB;
        const secondary = masterKey === "A" ? recordB : recordA;

        const confirmed = window.confirm(
            `Xác nhận gộp khách hàng trùng lặp?\n\n` +
            `• Bản ghi chính giữ lại: "${master.companyName || master.name}"\n` +
            `• Bản ghi phụ sẽ gộp: "${secondary.companyName || secondary.name}"\n` +
            `• Toàn bộ người liên hệ, cơ hội bán hàng và lịch sử hoạt động sẽ được chuyển giao về bản ghi chính.\n` +
            `• Bản ghi phụ sẽ chuyển trạng thái "Đã gộp" (MERGED).\n\n` +
            `Bạn có chắc chắn muốn tiến hành?`
        );
        if (!confirmed) return;

        try {
            const result = window.DuplicateMergeEngine.executeMerge({
                masterId: master.id,
                secondaryId: secondary.id,
                fieldOverrides: fieldOverrides,
                matchedReasons: reasons,
                operatorUser: currentSessionUser || { fullName: "Nông Quang Tiệp (Trưởng nhóm)", role: role }
            });

            if (result.success) {
                showToast(`Gộp thành công! Đã hợp nhất dữ liệu vào "${result.master.companyName || result.master.name}".`, "success");
                closeMergeModal();
                // Nạp lại danh sách mới nhất từ LocalStorage
                customersList = window.DuplicateMergeEngine.getStoredCustomers();
                filterAndRenderTable();
                updateKpiStats();
            }
        } catch (err) {
            console.error("Lỗi khi thực hiện gộp khách hàng:", err);
            alert(err.message || "Đã xảy ra lỗi khi gộp khách hàng.");
        }
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

        // Role Simulator Select
        roleSimulatorSelect?.addEventListener("change", (e) => {
            window.DuplicateMergeEngine?.setSimulatedRole(e.target.value);
            updateRolePermissionUI();
        });

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
