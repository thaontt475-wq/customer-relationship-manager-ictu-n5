/**
 * ===================================================================
 * CRM-123 / S4-09: XEM DANH SÁCH LEAD VỚI BỘ LỌC & BỘ LỌC LƯU SẴN
 * Author: Senior Frontend Developer (Nông Quang Tiệp)
 * Scope: HTML5, CSS3, Vanilla ES6+ - Zero JSP Policy - No Backend Modification
 * RESTful Endpoints Ready:
 *   - GET    /api/leads?search={kw}&status={st}&source={src}&tier={hot|warm|cold}&owner_id={id}&from={d1}&to={d2}&page={p}&size={s}
 *   - GET    /api/leads/saved-filters
 *   - POST   /api/leads/saved-filters
 *   - DELETE /api/leads/saved-filters/{id}
 * ===================================================================
 */

(function () {
    "use strict";

    // 1. STORAGE KEYS & CONSTANTS
    const STORAGE_KEY_LEADS = "crm_leads_data_s409";
    const STORAGE_KEY_SAVED_FILTERS = "crm_leads_saved_filters";
    const STORAGE_KEY_DEFAULT_FILTER_ID = "crm_leads_default_filter_id";

    const CURRENT_USER_NAME = "Nông Quang Tiệp";

    // 2. MOCK DATA INITIALIZATION
    const nowMs = Date.now();

    const DEFAULT_LEADS = [
        // Lead 1: 🔴 OVERDUE SLA - Hot Lead
        {
            id: "LD-2026-101",
            code: "LEAD-2026-101",
            fullName: "Trần Quang Khải",
            title: "Tổng Giám đốc (CEO)",
            companyName: "Công ty Cổ phần Công nghệ Delta Smart",
            industry: "CNTT & Viễn thông",
            taxCode: "0109988112",
            phone: "0912111222",
            email: "khai.tq@deltasmart.vn",
            source: "WEBSITE",
            sourceName: "Website Form",
            tier: "HOT",
            score: 95,
            status: "NEW",
            owner: CURRENT_USER_NAME,
            estimatedValue: 350000000,
            createdAt: new Date(nowMs - 6 * 3600 * 1000).toISOString(), // 6h ago
            slaDeadline: nowMs - 3 * 3600 * 1000, // Overdue by 3h
            slaMinutesTotal: 120,
            notes: "Khách hàng đăng ký gói Enterprise 150 người dùng qua form website, cần liên hệ gấp trước 10h sáng."
        },
        // Lead 2: 🔴 OVERDUE SLA - Warm Lead
        {
            id: "LD-2026-102",
            code: "LEAD-2026-102",
            fullName: "Đoàn Minh Trí",
            title: "Giám đốc Mua sắm & Cung ứng",
            companyName: "Chuỗi Cửa hàng Tiện lợi VinMart+",
            industry: "Bán lẻ & Chuỗi F&B",
            taxCode: "0108777666",
            phone: "0988333444",
            email: "tri.dm@vincommerce.vn",
            source: "EVENT",
            sourceName: "Hội thảo Chuyển đổi số Bán lẻ",
            tier: "WARM",
            score: 76,
            status: "NEW",
            owner: "Trần Anh Quân",
            estimatedValue: 220000000,
            createdAt: new Date(nowMs - 5 * 3600 * 1000).toISOString(),
            slaDeadline: nowMs - 90 * 60 * 1000, // Overdue by 90m
            slaMinutesTotal: 120,
            notes: "Gặp tại hội thảo, quan tâm phân hệ Quản lý Hợp đồng và Chăm sóc Khách hàng thân thiết."
        },
        // Lead 3: 🟢 SAFE SLA - Hot Lead (My Lead - In Progress)
        {
            id: "LD-2026-103",
            code: "LEAD-2026-103",
            fullName: "Nguyễn Hoàng Nam",
            title: "Giám đốc Công nghệ (CTO)",
            companyName: "Công ty Cổ phần Công nghệ Cao Việt Tech",
            industry: "CNTT & Viễn thông",
            taxCode: "0108923451",
            phone: "0912345678",
            email: "nam.nh@viettech.com.vn",
            source: "CAMPAIGN",
            sourceName: "Chiến dịch CRM 2026",
            tier: "HOT",
            score: 92,
            status: "IN_PROGRESS",
            owner: CURRENT_USER_NAME,
            estimatedValue: 250000000,
            createdAt: new Date(nowMs - 2 * 3600 * 1000).toISOString(),
            slaDeadline: nowMs + 45 * 60 * 1000, // 45m left
            slaMinutesTotal: 120,
            notes: "Đã gọi điện thoại lần 1, khách hàng yêu cầu gửi tài liệu demo kỹ thuật và bảng báo giá."
        },
        // Lead 4: 🟢 SAFE SLA - Unassigned New Lead
        {
            id: "LD-2026-104",
            code: "LEAD-2026-104",
            fullName: "Vũ Thị Bích Ngọc",
            title: "Trưởng phòng Kế hoạch Đầu tư",
            companyName: "Tập đoàn Dược phẩm Quốc tế Mediphar",
            industry: "Sản xuất & Chế tạo",
            taxCode: "0107892341",
            phone: "0988776655",
            email: "ngoc.vb@mediphar.vn",
            source: "WEBSITE",
            sourceName: "Website Form",
            tier: "HOT",
            score: 88,
            status: "NEW",
            owner: null, // Unassigned
            estimatedValue: 420000000,
            createdAt: new Date(nowMs - 20 * 60 * 1000).toISOString(), // 20m ago
            slaDeadline: nowMs + 100 * 60 * 1000, // 100m left
            slaMinutesTotal: 120,
            notes: "Nhu cầu tích hợp hệ thống CRM với phần mềm quản lý sản xuất thuốc GMP."
        },
        // Lead 5: Qualified Lead (My Lead)
        {
            id: "LD-2026-105",
            code: "LEAD-2026-105",
            fullName: "Phạm Quốc Dũng",
            title: "Phó Tổng Giám đốc Kinh doanh",
            companyName: "Công ty Cổ phần Địa ốc & Xây dựng Đất Việt",
            industry: "Bất động sản & Xây dựng",
            taxCode: "0309876543",
            phone: "0933555777",
            email: "dung.pq@datvietland.vn",
            source: "REFERRAL",
            sourceName: "Giới thiệu / Đối tác",
            tier: "HOT",
            score: 86,
            status: "QUALIFIED",
            owner: CURRENT_USER_NAME,
            estimatedValue: 350000000,
            createdAt: new Date(nowMs - 24 * 3600 * 1000).toISOString(),
            slaDeadline: null,
            slaMinutesTotal: 120,
            notes: "Đã thẩm định nhu cầu đầy đủ, đạt tiêu chuẩn chuyển đổi sang Khách hàng & Cơ hội."
        },
        // Lead 6: In Progress - Warm Lead
        {
            id: "LD-2026-106",
            code: "LEAD-2026-106",
            fullName: "Đinh Tuấn Anh",
            title: "Giám đốc Chuyển đổi số",
            companyName: "Chuỗi Bán lẻ Thời trang An Phước",
            industry: "Bán lẻ & Chuỗi F&B",
            taxCode: "0106543219",
            phone: "0904123987",
            email: "anh.dt@anphuocfashion.com",
            source: "EXCEL",
            sourceName: "Nhập Excel",
            tier: "WARM",
            score: 79,
            status: "IN_PROGRESS",
            owner: "Hoàng Mai Linh",
            estimatedValue: 180000000,
            createdAt: new Date(nowMs - 48 * 3600 * 1000).toISOString(),
            slaDeadline: null,
            slaMinutesTotal: 120,
            notes: "Đang hẹn gặp trực tiếp tại văn phòng Hai Bà Trưng để demo giao diện mobile app."
        },
        // Lead 7: Converted Lead
        {
            id: "LD-2026-107",
            code: "LEAD-2026-107",
            fullName: "Trần Trọng Nghĩa",
            title: "Tổng Giám đốc",
            companyName: "Tập đoàn Logistics Toàn Cầu VinaTrans",
            industry: "CNTT & Viễn thông",
            taxCode: "0301122334",
            phone: "0908112233",
            email: "nghia.tt@vinatrans.com.vn",
            source: "EVENT",
            sourceName: "Sự kiện / Hội thảo",
            tier: "HOT",
            score: 96,
            status: "CONVERTED",
            owner: CURRENT_USER_NAME,
            estimatedValue: 500000000,
            createdAt: new Date(nowMs - 72 * 3600 * 1000).toISOString(),
            slaDeadline: null,
            slaMinutesTotal: 120,
            notes: "Đã chuyển đổi thành công thành Khách hàng doanh nghiệp và Cơ hội bán hàng trong Sprint 4."
        },
        // Lead 8: Cold Lead - Unassigned
        {
            id: "LD-2026-108",
            code: "LEAD-2026-108",
            fullName: "Lê Văn Hùng",
            title: "Chuyên viên Mua sắm",
            companyName: "Công ty TNHH Cơ khí Tiến Phát",
            industry: "Sản xuất & Chế tạo",
            taxCode: "0103344556",
            phone: "0966112233",
            email: "hung.lv@tienphatmech.vn",
            source: "WEBSITE",
            sourceName: "Website Form",
            tier: "COLD",
            score: 42,
            status: "NEW",
            owner: null,
            estimatedValue: 60000000,
            createdAt: new Date(nowMs - 3 * 3600 * 1000).toISOString(),
            slaDeadline: nowMs + 30 * 60 * 1000, // 30m left
            slaMinutesTotal: 120,
            notes: "Khách hàng cá nhân tự đăng ký thử nghiệm gói Starter 5 người dùng."
        },
        // Lead 9: Lost Lead
        {
            id: "LD-2026-109",
            code: "LEAD-2026-109",
            fullName: "Hoàng Văn Thái",
            title: "Trưởng phòng Hành chính",
            companyName: "Công ty Cổ phần Xây lắp Số 1",
            industry: "Bất động sản & Xây dựng",
            taxCode: "0105566778",
            phone: "0944778899",
            email: "thai.hv@xaylap1.vn",
            source: "EXCEL",
            sourceName: "Nhập Excel",
            tier: "COLD",
            score: 35,
            status: "LOST",
            owner: "Đặng Quốc Việt",
            estimatedValue: 80000000,
            createdAt: new Date(nowMs - 96 * 3600 * 1000).toISOString(),
            slaDeadline: null,
            slaMinutesTotal: 120,
            notes: "Khách hàng tạm hoãn kế hoạch chuyển đổi số do cắt giảm ngân sách quý 4."
        },
        // Lead 10: In Progress - Hot Lead
        {
            id: "LD-2026-110",
            code: "LEAD-2026-110",
            fullName: "Lê Hoàng Yến",
            title: "Giám đốc Tài chính (CFO)",
            companyName: "Ngân hàng TMCP Thịnh Vượng Capital",
            industry: "Tài chính & Ngân hàng",
            taxCode: "0109988776",
            phone: "0977889900",
            email: "yen.lh@thinhvuongcapital.vn",
            source: "REFERRAL",
            sourceName: "Giới thiệu / Đối tác",
            tier: "HOT",
            score: 91,
            status: "IN_PROGRESS",
            owner: CURRENT_USER_NAME,
            estimatedValue: 600000000,
            createdAt: new Date(nowMs - 12 * 3600 * 1000).toISOString(),
            slaDeadline: null,
            slaMinutesTotal: 120,
            notes: "Dự án CRM cho Khối Khách hàng Doanh nghiệp, cần ký biên bản bảo mật NDA."
        }
    ];

    // Default System Quick Views / Saved Filters
    const SYSTEM_SAVED_FILTERS = [
        {
            id: "view_all",
            name: "📋 Tất cả Lead",
            isSystem: true,
            filter: {
                keyword: "",
                status: "ALL",
                tier: "ALL",
                source: "ALL",
                owner: "ALL",
                dateRange: "ALL",
                sort: "SLA_URGENT"
            }
        },
        {
            id: "view_hot_today",
            name: "🔥 Lead nóng",
            isSystem: true,
            filter: {
                keyword: "",
                status: "ACTIVE", // NEW or IN_PROGRESS
                tier: "HOT",
                source: "ALL",
                owner: "ALL",
                dateRange: "ALL",
                sort: "SCORE_DESC"
            }
        },
        {
            id: "view_sla_overdue",
            name: "⚠️ Quá hạn SLA",
            isSystem: true,
            isUrgentBadge: true,
            filter: {
                keyword: "",
                status: "ALL",
                tier: "ALL",
                source: "ALL",
                owner: "ALL",
                dateRange: "ALL",
                slaOverdueOnly: true,
                sort: "SLA_URGENT"
            }
        },
        {
            id: "view_unassigned",
            name: "🆕 Chưa phân bổ",
            isSystem: true,
            filter: {
                keyword: "",
                status: "NEW",
                tier: "ALL",
                source: "ALL",
                owner: "UNASSIGNED",
                dateRange: "ALL",
                sort: "DATE_DESC"
            }
        },
        {
            id: "view_my_leads",
            name: "👤 Lead của tôi",
            isSystem: true,
            filter: {
                keyword: "",
                status: "IN_PROGRESS",
                tier: "ALL",
                source: "ALL",
                owner: "CURRENT_USER",
                dateRange: "ALL",
                sort: "SLA_URGENT"
            }
        }
    ];

    // 3. SERVICE LAYER (REST API & LOCALSTORAGE ABSTRACTION)
    const LeadsService = {
        init() {
            if (!localStorage.getItem(STORAGE_KEY_LEADS)) {
                localStorage.setItem(STORAGE_KEY_LEADS, JSON.stringify(DEFAULT_LEADS));
            }
            if (!localStorage.getItem(STORAGE_KEY_SAVED_FILTERS)) {
                localStorage.setItem(STORAGE_KEY_SAVED_FILTERS, JSON.stringify([]));
            }
        },

        getLeads() {
            this.init();
            try {
                return JSON.parse(localStorage.getItem(STORAGE_KEY_LEADS)) || DEFAULT_LEADS;
            } catch (e) {
                return DEFAULT_LEADS;
            }
        },

        saveLeads(leads) {
            localStorage.setItem(STORAGE_KEY_LEADS, JSON.stringify(leads));
        },

        getLeadById(id) {
            return this.getLeads().find(l => l.id === id || l.code === id) || null;
        },

        createLead(data) {
            const leads = this.getLeads();
            const now = new Date();
            const idNumber = Math.floor(111 + Math.random() * 888);
            const newLead = {
                id: `LD-2026-${idNumber}`,
                code: `LEAD-2026-${idNumber}`,
                fullName: data.fullName,
                title: data.title || "Người đại diện",
                companyName: data.companyName,
                industry: data.industry || "B2B",
                taxCode: data.taxCode || "",
                phone: data.phone,
                email: data.email,
                source: data.source || "WEBSITE",
                sourceName: data.sourceName || "Website Form",
                tier: data.tier || "WARM",
                score: data.tier === "HOT" ? 85 : (data.tier === "WARM" ? 65 : 40),
                status: "NEW",
                owner: null,
                estimatedValue: 150000000,
                createdAt: now.toISOString(),
                slaDeadline: Date.now() + 120 * 60 * 1000, // 2 hours SLA
                slaMinutesTotal: 120,
                notes: data.notes || ""
            };
            leads.unshift(newLead);
            this.saveLeads(leads);
            return newLead;
        },

        /**
         * Endpoint: GET /api/leads/saved-filters
         */
        getSavedFilters() {
            this.init();
            try {
                const customs = JSON.parse(localStorage.getItem(STORAGE_KEY_SAVED_FILTERS)) || [];
                return [...SYSTEM_SAVED_FILTERS, ...customs];
            } catch (e) {
                return SYSTEM_SAVED_FILTERS;
            }
        },

        /**
         * Endpoint: POST /api/leads/saved-filters
         */
        saveCustomFilter(name, filterCriteria, setAsDefault = false) {
            this.init();
            const customs = JSON.parse(localStorage.getItem(STORAGE_KEY_SAVED_FILTERS)) || [];
            const newId = `custom_filter_${Date.now()}`;
            const newFilter = {
                id: newId,
                name: name.trim(),
                isSystem: false,
                filter: { ...filterCriteria }
            };
            customs.push(newFilter);
            localStorage.setItem(STORAGE_KEY_SAVED_FILTERS, JSON.stringify(customs));

            if (setAsDefault) {
                localStorage.setItem(STORAGE_KEY_DEFAULT_FILTER_ID, newId);
            }
            return newFilter;
        },

        /**
         * Endpoint: DELETE /api/leads/saved-filters/{id}
         */
        deleteCustomFilter(filterId) {
            this.init();
            let customs = JSON.parse(localStorage.getItem(STORAGE_KEY_SAVED_FILTERS)) || [];
            customs = customs.filter(f => f.id !== filterId);
            localStorage.setItem(STORAGE_KEY_SAVED_FILTERS, JSON.stringify(customs));

            if (localStorage.getItem(STORAGE_KEY_DEFAULT_FILTER_ID) === filterId) {
                localStorage.removeItem(STORAGE_KEY_DEFAULT_FILTER_ID);
            }
        },

        getDefaultFilterId() {
            return localStorage.getItem(STORAGE_KEY_DEFAULT_FILTER_ID) || "view_all";
        },

        resetSampleData() {
            localStorage.setItem(STORAGE_KEY_LEADS, JSON.stringify(DEFAULT_LEADS));
            localStorage.setItem(STORAGE_KEY_SAVED_FILTERS, JSON.stringify([]));
            localStorage.removeItem(STORAGE_KEY_DEFAULT_FILTER_ID);
        }
    };

    window.LeadsService = LeadsService;

    // 4. UI STATE & FILTER DEFINITION
    let activeFilterTabId = "view_all";

    let currentCriteria = {
        keyword: "",
        status: "ALL",
        tier: "ALL",
        source: "ALL",
        owner: "ALL",
        dateRange: "ALL",
        customFrom: "",
        customTo: "",
        slaOverdueOnly: false,
        sort: "SLA_URGENT"
    };

    let currentPage = 1;
    let pageSize = 10;

    // Avatar Color Palette
    const AVATAR_GRADIENTS = [
        "linear-gradient(135deg, #6366f1, #4f46e5)",
        "linear-gradient(135deg, #3b82f6, #1d4ed8)",
        "linear-gradient(135deg, #0ea5e9, #0284c7)",
        "linear-gradient(135deg, #10b981, #059669)",
        "linear-gradient(135deg, #f59e0b, #d97706)",
        "linear-gradient(135deg, #8b5cf6, #6d28d9)",
        "linear-gradient(135deg, #ec4899, #be185d)"
    ];

    function getAvatarGradient(name = "") {
        let hash = 0;
        for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
        return AVATAR_GRADIENTS[Math.abs(hash) % AVATAR_GRADIENTS.length];
    }

    function getInitials(name = "") {
        const parts = name.trim().split(/\s+/);
        if (!parts.length || !parts[0]) return "U";
        if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
        return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    function formatCurrency(val) {
        return (Number(val) || 0).toLocaleString("vi-VN");
    }

    function formatDateTimeVi(dateStr) {
        if (!dateStr) return "--";
        const d = new Date(dateStr);
        if (isNaN(d.getTime())) return dateStr;
        return d.toLocaleString("vi-VN", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        });
    }

    function isSlaOverdue(lead) {
        if (!lead.slaDeadline) return false;
        if (lead.status === "CONVERTED" || lead.status === "LOST") return false;
        return Date.now() > lead.slaDeadline;
    }

    function getSlaRemainingText(lead) {
        if (!lead.slaDeadline) return null;
        if (lead.status === "CONVERTED" || lead.status === "LOST") return null;

        const diffMs = lead.slaDeadline - Date.now();
        if (diffMs < 0) {
            const overdueMin = Math.abs(Math.floor(diffMs / 60000));
            const hours = Math.floor(overdueMin / 60);
            const mins = overdueMin % 60;
            return {
                isOverdue: true,
                text: hours > 0 ? `Quá hạn ${hours}h ${mins}m` : `Quá hạn ${mins} phút`
            };
        } else {
            const leftMin = Math.floor(diffMs / 60000);
            const hours = Math.floor(leftMin / 60);
            const mins = leftMin % 60;
            return {
                isOverdue: false,
                text: hours > 0 ? `Còn ${hours}h ${mins}m` : `Còn ${mins} phút`
            };
        }
    }

    // 5. TOAST NOTIFICATIONS
    function showToast(message, type = "success") {
        let region = document.getElementById("crmToastContainer");
        if (!region) {
            region = document.createElement("div");
            region.id = "crmToastContainer";
            region.className = "crm-toast-region";
            document.body.appendChild(region);
        }

        const toast = document.createElement("div");
        toast.className = `crm-toast crm-toast-${type}`;

        let iconHtml = '<i class="fa-solid fa-circle-check"></i>';
        if (type === "error") iconHtml = '<i class="fa-solid fa-circle-xmark"></i>';
        if (type === "warning") iconHtml = '<i class="fa-solid fa-triangle-exclamation"></i>';

        toast.innerHTML = `
            <div class="crm-toast-icon">${iconHtml}</div>
            <div class="crm-toast-copy">${message}</div>
            <button class="crm-toast-close" type="button" aria-label="Đóng"><i class="fa-solid fa-xmark"></i></button>
        `;

        toast.querySelector(".crm-toast-close").addEventListener("click", () => toast.remove());
        region.appendChild(toast);

        setTimeout(() => {
            if (toast.parentNode) {
                toast.style.opacity = "0";
                toast.style.transform = "translateX(20px)";
                toast.style.transition = "all 0.2s ease";
                setTimeout(() => toast.remove(), 200);
            }
        }, 4000);
    }

    // 6. RENDER LOGIC
    function refreshData() {
        const allLeads = LeadsService.getLeads();

        // 6.1 Update KPIs
        const total = allLeads.length;
        const hotCount = allLeads.filter(l => l.tier === "HOT").length;
        const overdueCount = allLeads.filter(l => isSlaOverdue(l)).length;
        const progressCount = allLeads.filter(l => l.status === "IN_PROGRESS").length;

        document.getElementById("statTotalLeads").textContent = total;
        document.getElementById("statHotLeads").textContent = hotCount;
        document.getElementById("statOverdueSla").textContent = overdueCount;
        document.getElementById("statInProgress").textContent = progressCount;

        // 6.2 Render Saved Filter Tabs
        renderSavedFilterTabs(allLeads);

        // 6.3 Apply Filtering
        let filtered = allLeads.filter(lead => {
            // Full-text search
            if (currentCriteria.keyword) {
                const kw = currentCriteria.keyword.toLowerCase();
                const matchName = (lead.fullName || "").toLowerCase().includes(kw);
                const matchComp = (lead.companyName || "").toLowerCase().includes(kw);
                const matchCode = (lead.code || "").toLowerCase().includes(kw);
                const matchPhone = (lead.phone || "").toLowerCase().includes(kw);
                const matchEmail = (lead.email || "").toLowerCase().includes(kw);
                if (!matchName && !matchComp && !matchCode && !matchPhone && !matchEmail) {
                    return false;
                }
            }

            // Status Filter
            if (currentCriteria.status === "ACTIVE") {
                if (lead.status !== "NEW" && lead.status !== "IN_PROGRESS") return false;
            } else if (currentCriteria.status !== "ALL") {
                if (lead.status !== currentCriteria.status) return false;
            }

            // Temperature / Tier Filter
            if (currentCriteria.tier !== "ALL") {
                if (lead.tier !== currentCriteria.tier) return false;
            }

            // Source Filter
            if (currentCriteria.source !== "ALL") {
                if (lead.source !== currentCriteria.source) return false;
            }

            // Owner Filter
            if (currentCriteria.owner === "CURRENT_USER") {
                if (lead.owner !== CURRENT_USER_NAME) return false;
            } else if (currentCriteria.owner === "UNASSIGNED") {
                if (lead.owner !== null && lead.owner !== "") return false;
            } else if (currentCriteria.owner !== "ALL") {
                if (lead.owner !== currentCriteria.owner) return false;
            }

            // SLA Overdue Only
            if (currentCriteria.slaOverdueOnly) {
                if (!isSlaOverdue(lead)) return false;
            }

            // Date Range
            if (currentCriteria.dateRange !== "ALL") {
                const createdTime = new Date(lead.createdAt).getTime();
                const now = new Date();
                if (currentCriteria.dateRange === "TODAY") {
                    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
                    if (createdTime < startOfToday) return false;
                } else if (currentCriteria.dateRange === "LAST_7_DAYS") {
                    const sevenDaysAgo = now.getTime() - 7 * 24 * 3600 * 1000;
                    if (createdTime < sevenDaysAgo) return false;
                } else if (currentCriteria.dateRange === "THIS_MONTH") {
                    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).getTime();
                    if (createdTime < startOfMonth) return false;
                } else if (currentCriteria.dateRange === "CUSTOM") {
                    if (currentCriteria.customFrom) {
                        const fromTime = new Date(currentCriteria.customFrom).getTime();
                        if (createdTime < fromTime) return false;
                    }
                    if (currentCriteria.customTo) {
                        const toTime = new Date(currentCriteria.customTo).getTime() + 24 * 3600 * 1000;
                        if (createdTime > toTime) return false;
                    }
                }
            }

            return true;
        });

        // 6.4 Apply Sorting
        filtered.sort((a, b) => {
            if (currentCriteria.sort === "SLA_URGENT") {
                const aOver = isSlaOverdue(a);
                const bOver = isSlaOverdue(b);
                if (aOver && !bOver) return -1;
                if (!aOver && bOver) return 1;
                return (b.score || 0) - (a.score || 0);
            }
            if (currentCriteria.sort === "SCORE_DESC") {
                return (b.score || 0) - (a.score || 0);
            }
            if (currentCriteria.sort === "DATE_DESC") {
                return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
            }
            if (currentCriteria.sort === "COMPANY_ASC") {
                return (a.companyName || "").localeCompare(b.companyName || "");
            }
            return 0;
        });

        // 6.5 Render Active Filter Tags
        renderActiveFilterTags();

        // 6.6 Render Table
        renderTable(filtered);
    }

    function renderSavedFilterTabs(allLeads) {
        const container = document.getElementById("savedFilterTabs");
        const filters = LeadsService.getSavedFilters();

        let html = "";
        filters.forEach(f => {
            const isActive = f.id === activeFilterTabId;

            // Compute count for tab
            let count = 0;
            if (f.id === "view_all") {
                count = allLeads.length;
            } else if (f.id === "view_hot_today") {
                count = allLeads.filter(l => l.tier === "HOT" && (l.status === "NEW" || l.status === "IN_PROGRESS")).length;
            } else if (f.id === "view_sla_overdue") {
                count = allLeads.filter(l => isSlaOverdue(l)).length;
            } else if (f.id === "view_unassigned") {
                count = allLeads.filter(l => l.owner === null || l.owner === "").length;
            } else if (f.id === "view_my_leads") {
                count = allLeads.filter(l => l.owner === CURRENT_USER_NAME && l.status === "IN_PROGRESS").length;
            } else {
                // Custom filter count
                count = allLeads.filter(l => {
                    const fc = f.filter;
                    if (fc.status && fc.status !== "ALL" && l.status !== fc.status) return false;
                    if (fc.tier && fc.tier !== "ALL" && l.tier !== fc.tier) return false;
                    if (fc.owner === "CURRENT_USER" && l.owner !== CURRENT_USER_NAME) return false;
                    return true;
                }).length;
            }

            const badgeClass = f.isUrgentBadge && count > 0 ? "tab-badge-urgent" : "";
            const deleteBtn = !f.isSystem
                ? `<i class="fa-solid fa-xmark btn-delete-custom-tab" data-delete-filter="${f.id}" title="Xóa bộ lọc này"></i>`
                : "";

            html += `
                <button type="button" class="filter-tab ${isActive ? "active" : ""}" data-filter-id="${f.id}">
                    <span>${f.name}</span>
                    <span class="filter-tab-count ${badgeClass}">${count}</span>
                    ${deleteBtn}
                </button>
            `;
        });

        container.innerHTML = html;

        // Attach tab click handlers
        container.querySelectorAll(".filter-tab").forEach(tab => {
            tab.addEventListener("click", (e) => {
                if (e.target.classList.contains("btn-delete-custom-tab")) {
                    e.stopPropagation();
                    const delId = e.target.getAttribute("data-delete-filter");
                    if (confirm("Bạn có chắc chắn muốn xóa bộ lọc tùy chỉnh này?")) {
                        LeadsService.deleteCustomFilter(delId);
                        if (activeFilterTabId === delId) activeFilterTabId = "view_all";
                        refreshData();
                        showToast("Đã xóa bộ lọc tùy chỉnh!", "success");
                    }
                    return;
                }

                const filterId = tab.getAttribute("data-filter-id");
                applySavedFilter(filterId);
            });
        });
    }

    function applySavedFilter(filterId) {
        activeFilterTabId = filterId;
        const filters = LeadsService.getSavedFilters();
        const found = filters.find(f => f.id === filterId);

        if (found) {
            currentCriteria = {
                keyword: "",
                status: "ALL",
                tier: "ALL",
                source: "ALL",
                owner: "ALL",
                dateRange: "ALL",
                customFrom: "",
                customTo: "",
                slaOverdueOnly: false,
                sort: "SLA_URGENT",
                ...(found.filter || {})
            };

            // Sync controls with criteria
            document.getElementById("leadSearchInput").value = currentCriteria.keyword || "";
            document.getElementById("filterStatus").value = currentCriteria.status === "ACTIVE" ? "ALL" : currentCriteria.status;
            document.getElementById("filterTier").value = currentCriteria.tier || "ALL";
            document.getElementById("filterSource").value = currentCriteria.source || "ALL";
            document.getElementById("filterOwner").value = currentCriteria.owner || "ALL";
            document.getElementById("filterDateRange").value = currentCriteria.dateRange || "ALL";
            document.getElementById("filterSort").value = currentCriteria.sort || "SLA_URGENT";

            document.getElementById("customDateRangeWrap").style.display = currentCriteria.dateRange === "CUSTOM" ? "block" : "none";
            document.getElementById("tableTitle").textContent = found.name;

            currentPage = 1;
            refreshData();
        }
    }

    function renderActiveFilterTags() {
        const container = document.getElementById("activeFilterTags");
        let tags = [];

        if (currentCriteria.keyword) {
            tags.push({ label: `Tìm: "${currentCriteria.keyword}"`, key: "keyword" });
        }
        if (currentCriteria.status !== "ALL" && currentCriteria.status !== "ACTIVE") {
            const statusMap = { NEW: "Mới", IN_PROGRESS: "Đang chăm sóc", QUALIFIED: "Đủ điều kiện", CONVERTED: "Đã chuyển đổi", LOST: "Đóng/Mất" };
            tags.push({ label: `Trạng thái: ${statusMap[currentCriteria.status] || currentCriteria.status}`, key: "status" });
        }
        if (currentCriteria.tier !== "ALL") {
            const tierMap = { HOT: "🔥 Nóng", WARM: "🌤️ Ấm", COLD: "❄️ Lạnh" };
            tags.push({ label: `Nhiệt độ: ${tierMap[currentCriteria.tier]}`, key: "tier" });
        }
        if (currentCriteria.source !== "ALL") {
            tags.push({ label: `Nguồn: ${currentCriteria.source}`, key: "source" });
        }
        if (currentCriteria.owner !== "ALL") {
            const ownerLabel = currentCriteria.owner === "CURRENT_USER" ? "Của tôi" : (currentCriteria.owner === "UNASSIGNED" ? "Chưa phân bổ" : currentCriteria.owner);
            tags.push({ label: `Phụ trách: ${ownerLabel}`, key: "owner" });
        }
        if (currentCriteria.slaOverdueOnly) {
            tags.push({ label: `⚠️ Quá hạn SLA`, key: "slaOverdueOnly" });
        }
        if (currentCriteria.dateRange !== "ALL") {
            const dateMap = { TODAY: "Hôm nay", LAST_7_DAYS: "7 ngày qua", THIS_MONTH: "Tháng này", CUSTOM: "Tùy chọn ngày" };
            tags.push({ label: `Thời gian: ${dateMap[currentCriteria.dateRange] || currentCriteria.dateRange}`, key: "dateRange" });
        }

        if (!tags.length) {
            container.innerHTML = "";
            return;
        }

        container.innerHTML = tags.map(t => `
            <span class="filter-pill">
                ${t.label}
                <i class="fa-solid fa-xmark" data-remove-tag="${t.key}" title="Bỏ điều kiện này"></i>
            </span>
        `).join("");

        container.querySelectorAll("[data-remove-tag]").forEach(el => {
            el.addEventListener("click", () => {
                const k = el.getAttribute("data-remove-tag");
                if (k === "keyword") {
                    currentCriteria.keyword = "";
                    document.getElementById("leadSearchInput").value = "";
                } else if (k === "status") {
                    currentCriteria.status = "ALL";
                    document.getElementById("filterStatus").value = "ALL";
                } else if (k === "tier") {
                    currentCriteria.tier = "ALL";
                    document.getElementById("filterTier").value = "ALL";
                } else if (k === "source") {
                    currentCriteria.source = "ALL";
                    document.getElementById("filterSource").value = "ALL";
                } else if (k === "owner") {
                    currentCriteria.owner = "ALL";
                    document.getElementById("filterOwner").value = "ALL";
                } else if (k === "slaOverdueOnly") {
                    currentCriteria.slaOverdueOnly = false;
                } else if (k === "dateRange") {
                    currentCriteria.dateRange = "ALL";
                    document.getElementById("filterDateRange").value = "ALL";
                    document.getElementById("customDateRangeWrap").style.display = "none";
                }
                currentPage = 1;
                refreshData();
            });
        });
    }

    function renderTable(leads) {
        const tbody = document.getElementById("leadsTableBody");
        const emptyState = document.getElementById("tableEmptyState");
        const recordCountEl = document.getElementById("tableRecordCount");

        recordCountEl.textContent = `${leads.length} bản ghi`;

        if (!leads.length) {
            tbody.innerHTML = "";
            emptyState.style.display = "block";
            updatePagination(0, 0, 0, 0);
            return;
        }

        emptyState.style.display = "none";

        // Pagination calculations
        const totalRecords = leads.length;
        const totalPages = Math.ceil(totalRecords / pageSize);
        if (currentPage > totalPages) currentPage = totalPages || 1;

        const startIndex = (currentPage - 1) * pageSize;
        const endIndex = Math.min(startIndex + pageSize, totalRecords);
        const pageItems = leads.slice(startIndex, endIndex);

        let html = "";
        pageItems.forEach(lead => {
            const overdue = isSlaOverdue(lead);
            const slaRemaining = getSlaRemainingText(lead);

            // Row Class: SLA Overdue Highlighting (MANDATORY REQUIREMENT)
            const rowClass = overdue ? "row-sla-overdue" : "";

            // Temperature Tier Badge
            let tierBadge = "";
            if (lead.tier === "HOT") {
                tierBadge = `<span class="badge-tier tier-hot"><i class="fa-solid fa-fire"></i> Nóng</span>`;
            } else if (lead.tier === "WARM") {
                tierBadge = `<span class="badge-tier tier-warm"><i class="fa-solid fa-sun"></i> Ấm</span>`;
            } else {
                tierBadge = `<span class="badge-tier tier-cold"><i class="fa-solid fa-snowflake"></i> Lạnh</span>`;
            }

            // Status Badge
            let statusBadge = "";
            if (lead.status === "NEW") {
                statusBadge = `<span class="badge-lead-status status-new"><i class="fa-solid fa-sparkles"></i> Mới</span>`;
            } else if (lead.status === "IN_PROGRESS") {
                statusBadge = `<span class="badge-lead-status status-progress"><i class="fa-solid fa-spinner"></i> Đang chăm sóc</span>`;
            } else if (lead.status === "QUALIFIED") {
                statusBadge = `<span class="badge-lead-status status-qualified"><i class="fa-solid fa-circle-check"></i> Đủ điều kiện</span>`;
            } else if (lead.status === "CONVERTED") {
                statusBadge = `<span class="badge-lead-status status-converted"><i class="fa-solid fa-lock"></i> Đã chuyển đổi</span>`;
            } else {
                statusBadge = `<span class="badge-lead-status status-lost"><i class="fa-solid fa-ban"></i> Đóng / Mất</span>`;
            }

            // SLA Tag
            let slaTagHtml = "";
            if (slaRemaining) {
                if (slaRemaining.isOverdue) {
                    slaTagHtml = `<span class="sla-tag sla-overdue"><i class="fa-solid fa-triangle-exclamation"></i> ${slaRemaining.text}</span>`;
                } else {
                    slaTagHtml = `<span class="sla-tag sla-safe"><i class="fa-solid fa-stopwatch"></i> ${slaRemaining.text}</span>`;
                }
            }

            // Owner Cell
            const ownerHtml = lead.owner
                ? `<div class="owner-cell"><i class="fa-solid fa-user-tie text-muted"></i> <span>${lead.owner}</span></div>`
                : `<span class="owner-unassigned"><i class="fa-solid fa-user-slash"></i> Chưa phân bổ</span>`;

            html += `
                <tr class="${rowClass}">
                    <td>
                        <div class="lead-code-wrap">
                            <span class="lead-code">${lead.code}</span>
                            ${tierBadge}
                        </div>
                    </td>
                    <td>
                        <div class="lead-person-col">
                            <div class="person-avt" style="background: ${getAvatarGradient(lead.fullName)}">
                                ${getInitials(lead.fullName)}
                            </div>
                            <div>
                                <span class="person-name">${lead.fullName}</span>
                                <span class="person-title">${lead.title || "Người đại diện"}</span>
                            </div>
                        </div>
                    </td>
                    <td>
                        <span class="company-title">${lead.companyName}</span>
                        <div class="company-sub">
                            <span>${lead.industry || "B2B"}</span>
                            ${lead.taxCode ? `<span>• MST: ${lead.taxCode}</span>` : ""}
                        </div>
                    </td>
                    <td>
                        <div class="contact-line">
                            <i class="fa-solid fa-phone"></i>
                            <a href="tel:${lead.phone}">${lead.phone}</a>
                        </div>
                        <div class="contact-line">
                            <i class="fa-solid fa-envelope"></i>
                            <a href="mailto:${lead.email}">${lead.email}</a>
                        </div>
                    </td>
                    <td>
                        <span class="source-badge"><i class="fa-solid fa-share-nodes"></i> ${lead.sourceName || lead.source}</span>
                        <span class="created-date">${formatDateTimeVi(lead.createdAt)}</span>
                    </td>
                    <td>
                        ${ownerHtml}
                    </td>
                    <td>
                        <div class="status-sla-cell">
                            ${statusBadge}
                            ${slaTagHtml}
                        </div>
                    </td>
                    <td style="text-align: center;">
                        <div class="action-row">
                            <button type="button" class="btn-table-icon btn-view-lead" data-lead-id="${lead.id}" title="Xem chi tiết Lead">
                                <i class="fa-solid fa-eye"></i>
                            </button>
                            <a href="lead-conversion.html" class="btn-table-icon" title="Chuyển đổi 3-trong-1 (CRM-122)">
                                <i class="fa-solid fa-arrows-split-up-and-left"></i>
                            </a>
                        </div>
                    </td>
                </tr>
            `;
        });

        tbody.innerHTML = html;

        // Attach action handlers
        tbody.querySelectorAll(".btn-view-lead").forEach(btn => {
            btn.addEventListener("click", () => {
                const id = btn.getAttribute("data-lead-id");
                openLeadDetailModal(id);
            });
        });

        updatePagination(startIndex + 1, endIndex, totalRecords, totalPages);
    }

    function updatePagination(start, end, total, totalPages = 1) {
        document.getElementById("pageStartRecord").textContent = total === 0 ? 0 : start;
        document.getElementById("pageEndRecord").textContent = end;
        document.getElementById("pageTotalRecord").textContent = total;

        const controls = document.getElementById("paginationControls");
        if (total === 0) {
            controls.innerHTML = "";
            return;
        }

        let pagesHtml = "";
        pagesHtml += `
            <button type="button" class="page-btn" id="btnPrevPage" ${currentPage === 1 ? "disabled" : ""} title="Trang trước">
                <i class="fa-solid fa-chevron-left"></i>
            </button>
        `;

        for (let p = 1; p <= totalPages; p++) {
            pagesHtml += `
                <button type="button" class="page-btn ${p === currentPage ? "active" : ""}" data-page="${p}">${p}</button>
            `;
        }

        pagesHtml += `
            <button type="button" class="page-btn" id="btnNextPage" ${currentPage === totalPages ? "disabled" : ""} title="Trang sau">
                <i class="fa-solid fa-chevron-right"></i>
            </button>
        `;

        controls.innerHTML = pagesHtml;

        controls.querySelectorAll("button[data-page]").forEach(b => {
            b.addEventListener("click", () => {
                currentPage = Number(b.getAttribute("data-page"));
                refreshData();
            });
        });

        const prevBtn = document.getElementById("btnPrevPage");
        if (prevBtn) {
            prevBtn.addEventListener("click", () => {
                if (currentPage > 1) {
                    currentPage--;
                    refreshData();
                }
            });
        }

        const nextBtn = document.getElementById("btnNextPage");
        if (nextBtn) {
            nextBtn.addEventListener("click", () => {
                if (currentPage < totalPages) {
                    currentPage++;
                    refreshData();
                }
            });
        }
    }

    // 7. MODAL: SAVE CURRENT FILTER
    function openSaveFilterModal() {
        const previewEl = document.getElementById("filterCriteriaPreview");
        let criteriaList = [];

        if (currentCriteria.keyword) criteriaList.push(`Từ khóa: "${currentCriteria.keyword}"`);
        if (currentCriteria.status !== "ALL") criteriaList.push(`Trạng thái: ${currentCriteria.status}`);
        if (currentCriteria.tier !== "ALL") criteriaList.push(`Nhiệt độ: ${currentCriteria.tier}`);
        if (currentCriteria.source !== "ALL") criteriaList.push(`Nguồn: ${currentCriteria.source}`);
        if (currentCriteria.owner !== "ALL") criteriaList.push(`Phụ trách: ${currentCriteria.owner}`);
        if (currentCriteria.dateRange !== "ALL") criteriaList.push(`Thời gian: ${currentCriteria.dateRange}`);

        if (!criteriaList.length) {
            criteriaList.push("Tất cả tiêu chuẩn (Không giới hạn điều kiện)");
        }

        previewEl.innerHTML = criteriaList.map(c => `<span class="filter-pill">${c}</span>`).join(" ");

        document.getElementById("filterNameInput").value = "";
        document.getElementById("checkSetAsDefault").checked = false;

        const modal = document.getElementById("saveFilterModal");
        modal.style.display = "flex";
        modal.setAttribute("aria-hidden", "false");
        document.getElementById("filterNameInput").focus();
    }

    function closeSaveFilterModal() {
        const modal = document.getElementById("saveFilterModal");
        modal.style.display = "none";
        modal.setAttribute("aria-hidden", "true");
    }

    function handleConfirmSaveFilter() {
        const nameInput = document.getElementById("filterNameInput");
        const name = nameInput.value.trim();
        if (!name) {
            showToast("Vui lòng nhập tên cho bộ lọc!", "warning");
            nameInput.focus();
            return;
        }

        const isDefault = document.getElementById("checkSetAsDefault").checked;
        const newFilter = LeadsService.saveCustomFilter(name, currentCriteria, isDefault);

        closeSaveFilterModal();
        activeFilterTabId = newFilter.id;
        refreshData();
        showToast(`Đã lưu bộ lọc "${name}" thành công!`, "success");
    }

    // 8. MODAL: LEAD DETAIL VIEW
    function openLeadDetailModal(leadId) {
        const lead = LeadsService.getLeadById(leadId);
        if (!lead) return;

        document.getElementById("detailLeadFullName").textContent = lead.fullName;
        document.getElementById("detailLeadCompany").textContent = `${lead.companyName} (${lead.code})`;

        document.getElementById("dtTitle").textContent = lead.title || "Người đại diện";
        document.getElementById("dtPhone").textContent = lead.phone;
        document.getElementById("dtEmail").textContent = lead.email;

        document.getElementById("dtCompany").textContent = lead.companyName;
        document.getElementById("dtTax").textContent = lead.taxCode || "N/A";
        document.getElementById("dtIndustry").textContent = lead.industry || "B2B";
        document.getElementById("dtAddress").textContent = lead.address || "Việt Nam";

        document.getElementById("dtTier").textContent = lead.tier === "HOT" ? "🔥 Nóng (Hot)" : (lead.tier === "WARM" ? "🌤️ Ấm (Warm)" : "❄️ Lạnh (Cold)");
        document.getElementById("dtScore").textContent = `${lead.score} điểm`;
        document.getElementById("dtSource").textContent = lead.sourceName || lead.source;
        document.getElementById("dtValue").textContent = `${formatCurrency(lead.estimatedValue)} ₫`;

        document.getElementById("dtStatus").textContent = lead.status;
        document.getElementById("dtOwner").textContent = lead.owner || "Chưa phân bổ";

        const slaRemaining = getSlaRemainingText(lead);
        document.getElementById("dtSla").textContent = slaRemaining ? slaRemaining.text : "Không áp dụng";
        document.getElementById("dtNotes").textContent = lead.notes || "Chưa có ghi chú bổ sung.";

        const modal = document.getElementById("leadDetailModal");
        modal.style.display = "flex";
        modal.setAttribute("aria-hidden", "false");
    }

    function closeLeadDetailModal() {
        const modal = document.getElementById("leadDetailModal");
        modal.style.display = "none";
        modal.setAttribute("aria-hidden", "true");
    }

    // 9. MODAL: CREATE NEW LEAD
    function openCreateLeadModal() {
        document.getElementById("createLeadForm").reset();
        const modal = document.getElementById("createLeadModal");
        modal.style.display = "flex";
        modal.setAttribute("aria-hidden", "false");
        document.getElementById("newFullName").focus();
    }

    function closeCreateLeadModal() {
        const modal = document.getElementById("createLeadModal");
        modal.style.display = "none";
        modal.setAttribute("aria-hidden", "true");
    }

    function handleCreateLeadSubmit() {
        const fullName = document.getElementById("newFullName").value.trim();
        const phone = document.getElementById("newPhone").value.trim();
        const email = document.getElementById("newEmail").value.trim();
        const companyName = document.getElementById("newCompanyName").value.trim();

        if (!fullName || !phone || !email || !companyName) {
            showToast("Vui lòng điền đầy đủ các thông tin bắt buộc (*)", "warning");
            return;
        }

        const newLead = LeadsService.createLead({
            fullName,
            title: document.getElementById("newTitle").value.trim(),
            phone,
            email,
            companyName,
            industry: document.getElementById("newIndustry").value,
            source: document.getElementById("newSource").value,
            tier: document.getElementById("newTier").value,
            notes: document.getElementById("newNotes").value.trim()
        });

        closeCreateLeadModal();
        currentPage = 1;
        refreshData();
        showToast(`Đã thêm Lead mới (${newLead.code}) thành công!`, "success");
    }

    // 10. EXPORT TO CSV / EXCEL
    function exportLeadsToExcel() {
        const allLeads = LeadsService.getLeads();
        if (!allLeads.length) {
            showToast("Không có dữ liệu để xuất!", "warning");
            return;
        }

        const headers = ["Mã Lead", "Họ tên", "Chức danh", "Công ty", "Ngành nghề", "Mã số thuế", "Điện thoại", "Email", "Nhiệt độ", "Điểm", "Nguồn", "Trạng thái", "Người phụ trách", "Ngày tạo"];
        const rows = allLeads.map(l => [
            `"${l.code}"`,
            `"${l.fullName}"`,
            `"${l.title || ""}"`,
            `"${l.companyName}"`,
            `"${l.industry || ""}"`,
            `"${l.taxCode || ""}"`,
            `"${l.phone}"`,
            `"${l.email}"`,
            `"${l.tier}"`,
            l.score,
            `"${l.sourceName || l.source}"`,
            `"${l.status}"`,
            `"${l.owner || "Chưa phân bổ"}"`,
            `"${formatDateTimeVi(l.createdAt)}"`
        ]);

        const csvContent = "\uFEFF" + [headers.join(","), ...rows.map(r => r.join(","))].join("\r\n");
        const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
        const link = document.createElement("a");
        link.href = URL.createObjectURL(blob);
        link.setAttribute("download", `Danh_Sach_Lead_${new Date().toISOString().split("T")[0]}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        showToast("Đã xuất danh sách Lead thành công!", "success");
    }

    // 11. ATTACH EVENT LISTENERS
    function initEventHandlers() {
        // Search Input (Debounced)
        const searchInput = document.getElementById("leadSearchInput");
        let searchTimer = null;
        searchInput.addEventListener("input", (e) => {
            clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                currentCriteria.keyword = e.target.value.trim();
                currentPage = 1;
                refreshData();
            }, 300);
        });

        // Dropdown Filters
        document.getElementById("filterStatus").addEventListener("change", (e) => {
            currentCriteria.status = e.target.value;
            currentPage = 1;
            refreshData();
        });

        document.getElementById("filterTier").addEventListener("change", (e) => {
            currentCriteria.tier = e.target.value;
            currentPage = 1;
            refreshData();
        });

        document.getElementById("filterSource").addEventListener("change", (e) => {
            currentCriteria.source = e.target.value;
            currentPage = 1;
            refreshData();
        });

        document.getElementById("filterOwner").addEventListener("change", (e) => {
            currentCriteria.owner = e.target.value;
            currentPage = 1;
            refreshData();
        });

        document.getElementById("filterDateRange").addEventListener("change", (e) => {
            currentCriteria.dateRange = e.target.value;
            const customWrap = document.getElementById("customDateRangeWrap");
            if (e.target.value === "CUSTOM") {
                customWrap.style.display = "block";
            } else {
                customWrap.style.display = "none";
                currentCriteria.customFrom = "";
                currentCriteria.customTo = "";
                currentPage = 1;
                refreshData();
            }
        });

        document.getElementById("btnApplyCustomDate").addEventListener("click", () => {
            currentCriteria.customFrom = document.getElementById("customDateFrom").value;
            currentCriteria.customTo = document.getElementById("customDateTo").value;
            currentPage = 1;
            refreshData();
        });

        document.getElementById("filterSort").addEventListener("change", (e) => {
            currentCriteria.sort = e.target.value;
            refreshData();
        });

        document.getElementById("pageSizeSelect").addEventListener("change", (e) => {
            pageSize = Number(e.target.value) || 10;
            currentPage = 1;
            refreshData();
        });

        // Reset All Filters
        const resetAllFilters = () => {
            activeFilterTabId = "view_all";
            currentCriteria = {
                keyword: "",
                status: "ALL",
                tier: "ALL",
                source: "ALL",
                owner: "ALL",
                dateRange: "ALL",
                customFrom: "",
                customTo: "",
                slaOverdueOnly: false,
                sort: "SLA_URGENT"
            };

            document.getElementById("leadSearchInput").value = "";
            document.getElementById("filterStatus").value = "ALL";
            document.getElementById("filterTier").value = "ALL";
            document.getElementById("filterSource").value = "ALL";
            document.getElementById("filterOwner").value = "ALL";
            document.getElementById("filterDateRange").value = "ALL";
            document.getElementById("customDateRangeWrap").style.display = "none";
            document.getElementById("filterSort").value = "SLA_URGENT";
            document.getElementById("tableTitle").textContent = "Tất cả Lead";

            currentPage = 1;
            refreshData();
            showToast("Đã khôi phục toàn bộ tiêu chí lọc về mặc định!", "success");
        };

        document.getElementById("btnResetAllFilters").addEventListener("click", resetAllFilters);
        document.getElementById("btnClearFiltersFromEmpty").addEventListener("click", resetAllFilters);

        // Header Action Buttons
        document.getElementById("btnRefreshLeads").addEventListener("click", () => {
            refreshData();
            showToast("Đã làm mới danh sách Lead!", "success");
        });

        document.getElementById("btnResetMockData").addEventListener("click", () => {
            if (confirm("Khôi phục toàn bộ dữ liệu mẫu chuẩn Sprint 4? Các thay đổi cục bộ sẽ được hoàn tác.")) {
                LeadsService.resetSampleData();
                resetAllFilters();
                showToast("Đã khôi phục dữ liệu mẫu chuẩn Sprint 4!", "success");
            }
        });

        document.getElementById("btnExportExcel").addEventListener("click", exportLeadsToExcel);

        // Quick KPI Card Filters
        document.querySelectorAll(".leads-kpi-card[data-quick-filter]").forEach(card => {
            card.addEventListener("click", () => {
                const qf = card.getAttribute("data-quick-filter");
                if (qf === "ALL") {
                    applySavedFilter("view_all");
                } else if (qf === "HOT") {
                    applySavedFilter("view_hot_today");
                } else if (qf === "OVERDUE") {
                    applySavedFilter("view_sla_overdue");
                } else if (qf === "IN_PROGRESS") {
                    applySavedFilter("view_my_leads");
                }
            });
        });

        // Save Filter Modal Controls
        document.getElementById("btnOpenSaveFilterModal").addEventListener("click", openSaveFilterModal);
        document.getElementById("btnCloseSaveFilterModal").addEventListener("click", closeSaveFilterModal);
        document.getElementById("btnCancelSaveFilter").addEventListener("click", closeSaveFilterModal);
        document.getElementById("btnConfirmSaveFilter").addEventListener("click", handleConfirmSaveFilter);

        // Lead Detail Modal Controls
        document.getElementById("btnCloseDetailModal").addEventListener("click", closeLeadDetailModal);
        document.getElementById("btnDismissDetailModal").addEventListener("click", closeLeadDetailModal);

        // Create Lead Modal Controls
        document.getElementById("btnOpenCreateLead").addEventListener("click", openCreateLeadModal);
        document.getElementById("btnCloseCreateModal").addEventListener("click", closeCreateLeadModal);
        document.getElementById("btnCancelCreateModal").addEventListener("click", closeCreateLeadModal);
        document.getElementById("btnSubmitCreateLead").addEventListener("click", handleCreateLeadSubmit);

        // Backdrop click to close modals
        document.querySelectorAll(".crm-modal-backdrop").forEach(backdrop => {
            backdrop.addEventListener("click", (e) => {
                if (e.target === backdrop) {
                    closeSaveFilterModal();
                    closeLeadDetailModal();
                    closeCreateLeadModal();
                }
            });
        });

        // Keyboard Escape
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") {
                closeSaveFilterModal();
                closeLeadDetailModal();
                closeCreateLeadModal();
            }
        });
    }

    // 12. BOOTSTRAP ON DOM READY
    document.addEventListener("DOMContentLoaded", () => {
        LeadsService.init();
        initEventHandlers();

        // Check if there is a default saved filter configured
        const defaultFilterId = LeadsService.getDefaultFilterId();
        if (defaultFilterId && defaultFilterId !== "view_all") {
            applySavedFilter(defaultFilterId);
        } else {
            refreshData();
        }
    });

})();
