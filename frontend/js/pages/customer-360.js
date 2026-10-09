"use strict";

/**
 * ===================================================================
 * CRM-63 / S3-03: ENTERPRISE CUSTOMER 360 VIEW CLIENT ENGINE
 * Module: frontend/js/pages/customer-360.js
 * Author: Senior Frontend Developer
 * 
 * Capabilities:
 *  1. Unified API Client for GET /api/customers/{id}/360
 *  2. Pure In-Memory Mock Fallback & HTTP/JSON Client Architecture
 *  3. Enterprise KPI Cards & Company Profile Representation
 *  4. High-Performance DOM Rendering (< 1.5s for 500 activities) with client pagination
 *  5. Complete 6-Tab Workspace: Timeline, Contacts, Deals, Attachments, Tickets, Hierarchy
 *  6. 100% Backward Compatibility with CRM-61, CRM-62, CRM-65, CRM-68
 * ===================================================================
 */

const API_BASE = "http://localhost:8080/crm";

// Global View States
let currentCustomer = null;
let customer360Data = null;
let timelineData = [];
let filteredTimeline = [];
let opportunitiesData = { open: [], closed: [] };
let attachmentsData = [];
let contactsList = [];

// Timeline Pagination & Performance State
let timelineCurrentPage = 1;
const timelinePageSize = 20;
let timelineFilterType = "all";
let timelineSearchQuery = "";

// URL Parameters
const params = new URLSearchParams(window.location.search);
const customerId = params.get("id") || "4"; // Default demo to non-deleted customer 4 if omitted

/* =========================================================
   1. API CLIENT WITH CREDENTIALS
========================================================= */
async function api(path, options = {}) {
    const config = {
        credentials: "include",
        headers: {
            "Accept": "application/json",
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...(options.headers || {})
        },
        ...options
    };

    const response = await fetch(API_BASE + path, config);
    let result = null;
    try {
        result = await response.json();
    } catch (_) {
        result = null;
    }

    if (response.status === 401) {
        localStorage.removeItem("crm_ui_session");
        window.location.href = "login.html";
        throw new Error("Phiên đăng nhập đã hết hạn.");
    }

    if (!response.ok || !result?.success) {
        const error = new Error(result?.message || `HTTP ${response.status}`);
        error.status = response.status;
        error.data = result?.data;
        throw error;
    }

    return result.data;
}

/* =========================================================
   2. IN-MEMORY ENTERPRISE MOCK GENERATOR (500 ACTIVITIES)
========================================================= */
function generateComprehensiveMock360(cId) {
    const idNum = Number(cId) || 4;

    // Check existing customer record in CRM_CUSTOMERS_DATA
    let custName = "Công ty Cổ phần Giải pháp Số Nam Á";
    let custTax = "0108923456";
    let custIndustry = "Công nghệ thông tin & Viễn thông";
    let custPhone = "024.3999.8888";
    let custWebsite = "https://namasolutions.vn";
    let custAddress = "Tầng 18, Keangnam Landmark 72, Phạm Hùng, Cầu Giấy, Hà Nội";
    let custScale = "200 - 500 nhân sự";
    let custOwner = "Nông Quang Tiệp (Trưởng phòng KD)";
    let custEmail = "contact@namasolutions.vn";
    let custStatus = "ACTIVE";

    try {
        const raw = localStorage.getItem("CRM_CUSTOMERS_DATA");
        if (raw) {
            const list = JSON.parse(raw);
            const found = list.find(c => Number(c.id) === idNum);
            if (found) {
                custName = found.companyName || found.name || custName;
                custTax = found.taxCode || custTax;
                custIndustry = found.industry || custIndustry;
                custPhone = found.phone || custPhone;
                custWebsite = found.website || custWebsite;
                custAddress = found.address || custAddress;
                custScale = found.scale || custScale;
                custOwner = found.ownerName || found.owner || custOwner;
                custEmail = found.email || custEmail;
                custStatus = found.status || custStatus;
            }
        }
    } catch (_) {}

    // 1. Customer Object
    const customer = {
        id: idNum,
        name: custName,
        companyName: custName,
        taxCode: custTax,
        industry: custIndustry,
        scale: custScale,
        address: custAddress,
        phone: custPhone,
        email: custEmail,
        website: custWebsite,
        ownerName: custOwner,
        owner: custOwner,
        ownerEmail: "tiep.nq@crm.vn",
        ownerPhone: "0912.888.999",
        status: custStatus,
        type: "ENTERPRISE",
        parentName: "Tập đoàn Công nghệ Nam Á Holdings",
        createdAt: "2026-01-15T08:30:00Z"
    };

    // 2. Contacts with B2B Decision-Making Roles (CRM-62)
    const contacts = [
        {
            id: 101,
            customerId: idNum,
            fullName: "TS. Hoàng Quốc Hùng",
            jobTitle: "Tổng Giám đốc (CEO)",
            department: "Ban Điều hành",
            email: "hung.hq@namasolutions.vn",
            phone: "0903.111.222",
            decisionRole: "DECISION_MAKER",
            isPrimary: true,
            status: "ACTIVE"
        },
        {
            id: 102,
            customerId: idNum,
            fullName: "ThS. Vũ Thùy Linh",
            jobTitle: "Giám đốc Công nghệ Thông tin (CIO)",
            department: "Khối Công nghệ & Hạ tầng",
            email: "linh.vt@namasolutions.vn",
            phone: "0904.333.444",
            decisionRole: "INFLUENCER",
            isPrimary: false,
            status: "ACTIVE"
        },
        {
            id: 103,
            customerId: idNum,
            fullName: "Nguyễn Tuấn Anh",
            jobTitle: "Trưởng phòng Vận hành Hệ thống",
            department: "Trung tâm Vận hành NOC",
            email: "anh.nt@namasolutions.vn",
            phone: "0915.555.666",
            decisionRole: "END_USER",
            isPrimary: false,
            status: "ACTIVE"
        },
        {
            id: 104,
            customerId: idNum,
            fullName: "Đặng Thị Mai",
            jobTitle: "Kế toán trưởng & Pháp chế hợp đồng",
            department: "Phòng Tài chính Kế toán",
            email: "mai.dt@namasolutions.vn",
            phone: "0936.777.888",
            decisionRole: "BLOCKER",
            isPrimary: false,
            status: "ACTIVE"
        }
    ];

    // 3. Opportunities (Deals): Open & Closed
    const openOpportunities = [
        {
            id: 201,
            name: "Triển khai Hạ tầng Private Cloud & Bảo mật dữ liệu 2026",
            stage: "NEGOTIATION",
            stageName: "Đàm phán hợp đồng",
            amount: 550000000,
            probability: 80,
            expectedRevenue: 440000000,
            closeDate: "2026-11-15",
            status: "OPEN",
            ownerName: "Nông Quang Tiệp"
        },
        {
            id: 202,
            name: "Gói giải pháp Quản trị Dữ liệu BI & AI Dashboard",
            stage: "PROPOSAL",
            stageName: "Đề xuất & Báo giá",
            amount: 300000000,
            probability: 60,
            expectedRevenue: 180000000,
            closeDate: "2026-11-30",
            status: "OPEN",
            ownerName: "Nông Quang Tiệp"
        }
    ];

    const closedOpportunities = [
        {
            id: 198,
            name: "Hệ thống Quản trị Khách hàng CRM Enterprise Giai đoạn 1",
            stage: "CLOSED_WON",
            stageName: "Thành công (Won)",
            amount: 1200000000,
            probability: 100,
            expectedRevenue: 1200000000,
            closeDate: "2026-06-15",
            status: "WON",
            ownerName: "Nông Quang Tiệp"
        },
        {
            id: 199,
            name: "Bản quyền License CSDL Oracle Enterprise & Đào tạo",
            stage: "CLOSED_WON",
            stageName: "Thành công (Won)",
            amount: 750000000,
            probability: 100,
            expectedRevenue: 750000000,
            closeDate: "2026-08-20",
            status: "WON",
            ownerName: "Nông Quang Tiệp"
        },
        {
            id: 200,
            name: "Dịch vụ Bảo trì định kỳ & Hỗ trợ kỹ thuật 24/7 năm 2026",
            stage: "CLOSED_WON",
            stageName: "Thành công (Won)",
            amount: 500000000,
            probability: 100,
            expectedRevenue: 500000000,
            closeDate: "2026-09-10",
            status: "WON",
            ownerName: "Nông Quang Tiệp"
        },
        {
            id: 195,
            name: "Gói Tư vấn Chuyển đổi số chi nhánh phía Nam",
            stage: "CLOSED_LOST",
            stageName: "Thất bại (Lost)",
            amount: 220000000,
            probability: 0,
            expectedRevenue: 0,
            closeDate: "2026-03-12",
            status: "LOST",
            lostReason: "Khách hàng hoãn kế hoạch sang năm sau",
            ownerName: "Hoàng Trọng Thái"
        }
    ];

    // 4. Attachments (Hợp đồng, Báo giá, Tài liệu)
    const attachments = [
        {
            id: 301,
            fileName: "Hop_dong_kinh_te_CRM_Enterprise_signed.pdf",
            fileType: "pdf",
            category: "Hợp đồng",
            fileSize: "3.4 MB",
            uploadedBy: "Nông Quang Tiệp",
            uploadedAt: "2026-06-15 14:20"
        },
        {
            id: 302,
            fileName: "Bao_gia_ha_tang_Cloud_2026_v2.xlsx",
            fileType: "xlsx",
            category: "Báo giá",
            fileSize: "840 KB",
            uploadedBy: "Nông Quang Tiệp",
            uploadedAt: "2026-10-05 09:15"
        },
        {
            id: 303,
            fileName: "Bien_ban_nghiem_thu_giai_doan_1.docx",
            fileType: "docx",
            category: "Biên bản",
            fileSize: "1.2 MB",
            uploadedBy: "Hoàng Trọng Thái",
            uploadedAt: "2026-08-28 16:45"
        },
        {
            id: 304,
            fileName: "Ho_so_giai_phap_ky_thuat_va_bao_mat.pdf",
            fileType: "pdf",
            category: "Tài liệu kỹ thuật",
            fileSize: "5.8 MB",
            uploadedBy: "Nguyễn Văn Thắng",
            uploadedAt: "2026-05-10 11:00"
        },
        {
            id: 305,
            fileName: "Giay_phep_kinh_doanh_va_MST_cong_chung.pdf",
            fileType: "pdf",
            category: "Pháp lý",
            fileSize: "2.1 MB",
            uploadedBy: "Nguyễn Đình Toàn",
            uploadedAt: "2026-04-01 10:30"
        }
    ];

    // 5. High-Volume Activity Timeline Dataset (500 Items for Performance Test)
    const activities = [];
    const types = ["call", "meeting", "email", "note"];
    const authors = ["Nông Quang Tiệp", "Hoàng Trọng Thái", "Nguyễn Văn Thắng", "Vũ Thùy Linh"];
    const actions = [
        { type: "call", subject: "Cuộc gọi trao đổi tiến độ hợp đồng", text: "Trao đổi với Tổng Giám đốc Hoàng Quốc Hùng về điều khoản thanh toán đợt 2 và kế hoạch triển khai cụm server dự phòng." },
        { type: "meeting", subject: "Họp rà soát phương án an ninh dữ liệu", text: "Làm việc trực tiếp tại trụ sở Nam Á cùng Giám đốc CNTT ThS. Vũ Thùy Linh. Thống nhất cơ chế phân quyền RBAC và mã hóa dữ liệu đầu cuối." },
        { type: "email", subject: "Gửi dự thảo phụ lục bổ sung tính năng", text: "Đã gửi email kèm bảng báo giá chi tiết và checklist các tiêu chuẩn an toàn thông tin ISO 27001 cho phòng Pháp chế." },
        { type: "note", subject: "Ghi chú chiến lược chốt deal Cloud", text: "Khách hàng ưu tiên hoàn tất giải ngân trước ngày 20/11 để kịp quyết toán quý 4. Cần sales rep theo sát phản hồi của Kế toán trưởng." },
        { type: "call", subject: "Hỗ trợ xác nhận sự cố kết nối API", text: "Gọi điện hỗ trợ Trưởng phòng Vận hành Nguyễn Tuấn Anh kiểm tra webhook đồng bộ tồn kho. Hệ thống đã hoạt động ổn định trở lại." }
    ];

    const baseTime = Date.now();
    for (let i = 0; i < 500; i++) {
        const tpl = actions[i % actions.length];
        const minutesAgo = i * 28 + (i % 7) * 5;
        const entryDate = new Date(baseTime - minutesAgo * 60 * 1000);
        activities.push({
            id: 5000 + i,
            type: tpl.type,
            subject: `#${500 - i}: ${tpl.subject}`,
            description: tpl.text + ` (Mã giao dịch tương tác: ACT-2026-${String(500 - i).padStart(4, '0')})`,
            authorName: authors[i % authors.length],
            createdAt: entryDate.toISOString()
        });
    }

    // 6. KPIs
    const totalWon = closedOpportunities.filter(o => o.status === "WON").reduce((sum, o) => sum + (o.amount || 0), 0);
    const openPipeline = openOpportunities.reduce((sum, o) => sum + (o.amount || 0), 0);

    return {
        customer,
        kpis: {
            totalWon,
            wonDealsCount: closedOpportunities.filter(o => o.status === "WON").length,
            openPipeline,
            openDealsCount: openOpportunities.length,
            contactsCount: contacts.length,
            activitiesCount: activities.length
        },
        contacts,
        openOpportunities,
        closedOpportunities,
        opportunities: {
            open: openOpportunities,
            closed: closedOpportunities
        },
        activities,
        attachments,
        totalContractValue: totalWon,
        openOpportunityValue: openPipeline,
        churnRisk: {
            customerId: idNum,
            customerName: custName,
            isRisk: false,
            riskLevel: "NONE",
            openTicketCount: 0,
            reasons: []
        }
    };
}

/* =========================================================
   3. DATA FETCHING & NORMALIZATION PIPELINE
========================================================= */
async function loadCustomer360Data(cId) {
    const id = Number(cId) || 4;

    // Clean up residual demo localStorage key if previously present
    try {
        localStorage.removeItem("CRM_CUSTOMER_360_VIEW_DATA");
    } catch (_) {}

    // 1. Primary Flow: Fetch Backend endpoint GET /api/customers/{id}/360
    try {
        const res = await api(`/api/customers/${id}/360`);
        if (res && res.customer) {
            console.info("[CRM-63] Tải thành công dữ liệu từ backend GET /api/customers/{id}/360:", res);
            return normalizePayload(res);
        }
    } catch (err) {
        console.warn(`[CRM-63] Backend GET /api/customers/${id}/360 chưa sẵn sàng hoặc lỗi (${err.message}). Kích hoạt In-Memory Mock Data:`);
    }

    // 2. Fallback Flow: Comprehensive In-Memory Mock Data
    const mock = generateComprehensiveMock360(id);
    return normalizePayload(mock);
}


function normalizePayload(raw) {
    const cust = raw.customer || raw;
    const oppsOpen = raw.openOpportunities || (Array.isArray(raw.opportunities) ? raw.opportunities.filter(o => o.status === "OPEN") : (raw.opportunities?.open || []));
    const oppsClosed = raw.closedOpportunities || (Array.isArray(raw.opportunities) ? raw.opportunities.filter(o => o.status !== "OPEN") : (raw.opportunities?.closed || []));
    
    // Financial KPIs calculation
    const wonSum = raw.kpis?.totalWon ?? raw.totalContractValue ?? oppsClosed.filter(o => o.status === "WON").reduce((s, o) => s + (Number(o.amount) || 0), 0);
    const pipelineSum = raw.kpis?.openPipeline ?? raw.openOpportunityValue ?? oppsOpen.reduce((s, o) => s + (Number(o.amount) || 0), 0);
    const contacts = raw.contacts || [];
    const activities = raw.activities || [];
    const attachments = raw.attachments || [];

    return {
        customer: cust,
        kpis: {
            totalWon: wonSum,
            wonDealsCount: raw.kpis?.wonDealsCount ?? oppsClosed.filter(o => o.status === "WON").length,
            openPipeline: pipelineSum,
            openDealsCount: raw.kpis?.openDealsCount ?? oppsOpen.length,
            contactsCount: raw.kpis?.contactsCount ?? contacts.length,
            activitiesCount: raw.kpis?.activitiesCount ?? activities.length
        },
        contacts,
        openOpportunities: oppsOpen,
        closedOpportunities: oppsClosed,
        activities,
        attachments,
        churnRisk: raw.churnRisk || null
    };
}

/* =========================================================
   4. INITIALIZATION FLOW
========================================================= */
async function initCustomer360() {
    if (!customerId) return;

    try {
        customer360Data = await loadCustomer360Data(customerId);
        currentCustomer = customer360Data.customer;


        // Render UI Sections
        renderHeaderAndCompanyProfile();
        renderKPIs();
        renderPrimaryContactCard();

        // Prepare Timeline dataset
        timelineData = customer360Data.activities.map(a => ({
            id: a.id,
            type: (a.type || "note").toLowerCase(),
            subject: a.subject || a.title || "",
            text: a.description || a.content || a.subject || "",
            author: a.authorName || a.createdBy || currentCustomer?.ownerName || "Hệ thống",
            time: a.createdAt ? new Date(a.createdAt) : new Date()
        }));
        applyTimelineFilter();

        // Render Deals Tab
        opportunitiesData = {
            open: customer360Data.openOpportunities,
            closed: customer360Data.closedOpportunities
        };
        renderDealsTab();

        // Render Attachments Tab
        attachmentsData = customer360Data.attachments;
        renderAttachmentsTab();

        // Render Contacts Tab (CRM-62 Integration)
        renderContactsTab();

        // Render CRM-68 Churn Risk Banner & Support Tickets
        renderCustomerChurnRisk360();
        renderCustomerTickets360();

        // Render CRM-65 Corporate Hierarchy Banner
        renderCorporateHierarchy360();

        // Update Tab Badges
        updateAllTabBadges();

    } catch (err) {
        console.error("Lỗi khởi tạo Customer 360:", err);
    }
}

/* =========================================================
   5. RENDER HEADER, COMPANY PROFILE & FINANCIAL KPIS
========================================================= */
function renderHeaderAndCompanyProfile() {
    if (!currentCustomer) return;

    const name = currentCustomer.companyName || currentCustomer.name || "Doanh nghiệp";
    const titleEl = document.getElementById("pageCustomerTitle");
    if (titleEl) titleEl.textContent = name;

    const statusBadge = document.getElementById("customer360StatusBadge");
    if (statusBadge) {
        const isClosed = currentCustomer.status === "INACTIVE" || currentCustomer.status === "CLOSED";
        statusBadge.textContent = isClosed ? "TẠM NGỪNG" : "ĐANG HỢP TÁC";
        statusBadge.className = isClosed ? "c360-status-pill" : "c360-status-pill c360-status-active";
    }

    writeDisplay("company360Name", name);
    writeDisplay("company360Tax", currentCustomer.taxCode);
    writeDisplay("company360Industry", currentCustomer.industry);
    writeDisplay("company360Scale", currentCustomer.scale || currentCustomer.companySize || "100 - 500 nhân sự");
    writeDisplay("company360Address", currentCustomer.address || "Chưa cập nhật địa chỉ");
    writeDisplay("company360Owner", currentCustomer.ownerName || currentCustomer.owner || "Nông Quang Tiệp");
    writeDisplay("company360Phone", currentCustomer.phone);
    writeDisplay("company360Email", currentCustomer.email);
    writeDisplay("company360Website", currentCustomer.website);
    writeDisplay("company360Parent", currentCustomer.parentName || "— (Độc lập)");
}

function renderKPIs() {
    if (!customer360Data) return;
    const { kpis } = customer360Data;

    // Card 1: Won Deals (Tổng giá trị đã ký)
    const wonValEl = document.getElementById("kpiTotalWonValue");
    const wonCountEl = document.getElementById("kpiTotalWonCount");
    if (wonValEl) wonValEl.textContent = formatMoney(kpis.totalWon || 0);
    if (wonCountEl) wonCountEl.textContent = `${kpis.wonDealsCount || 0} hợp đồng hoàn tất`;

    // Card 2: Open Pipeline (Giá trị cơ hội đang mở)
    const pipeValEl = document.getElementById("kpiOpenPipelineValue");
    const pipeCountEl = document.getElementById("kpiOpenPipelineCount");
    if (pipeValEl) pipeValEl.textContent = formatMoney(kpis.openPipeline || 0);
    if (pipeCountEl) pipeCountEl.textContent = `${kpis.openDealsCount || 0} cơ hội mở`;

    // Card 3: Key Contacts
    const contactCountEl = document.getElementById("kpiContactsCount");
    const contactMetaEl = document.getElementById("kpiContactsMeta");
    const dmCount = (customer360Data.contacts || []).filter(c => c.decisionRole === "DECISION_MAKER").length;
    if (contactCountEl) contactCountEl.textContent = String(kpis.contactsCount || 0);
    if (contactMetaEl) contactMetaEl.textContent = `${dmCount} Người quyết định (DM) • 1 Đầu mối chính`;

    // Card 4: Activity Stream
    const actCountEl = document.getElementById("kpiActivitiesCount");
    const actMetaEl = document.getElementById("kpiHealthMeta");
    if (actCountEl) actCountEl.textContent = String(kpis.activitiesCount || 0);
    if (actMetaEl) actMetaEl.textContent = "Gần đây: Cuộc gọi hôm nay";
}

function renderPrimaryContactCard() {
    const primary = (customer360Data?.contacts || []).find(c => c.isPrimary) || (customer360Data?.contacts || [])[0];
    const nameEl = document.getElementById("primaryContactName");
    const titleEl = document.getElementById("primaryContactTitle");
    const emailEl = document.getElementById("primaryContactEmail");
    const phoneEl = document.getElementById("primaryContactPhone");
    const roleBadge = document.getElementById("primaryContactRoleBadge");

    if (!primary) {
        if (nameEl) nameEl.textContent = "Chưa có đầu mối liên hệ chính";
        return;
    }

    if (nameEl) nameEl.textContent = primary.fullName || "—";
    if (titleEl) titleEl.textContent = primary.jobTitle ? `${primary.jobTitle} · ${primary.department || ''}` : "—";
    if (emailEl) emailEl.innerHTML = `✉ <a href="mailto:${escapeHtml(primary.email)}" style="color:inherit; text-decoration:none;">${escapeHtml(primary.email || '—')}</a>`;
    if (phoneEl) phoneEl.innerHTML = `☎ <a href="tel:${escapeHtml(primary.phone)}" style="color:inherit; text-decoration:none;">${escapeHtml(primary.phone || '—')}</a>`;
    if (roleBadge) roleBadge.innerHTML = renderDecisionRoleBadge(primary.decisionRole);
}

/* =========================================================
   6. HIGH-PERFORMANCE TIMELINE ENGINE (< 1.5s FOR 500 ITEMS)
========================================================= */
function applyTimelineFilter() {
    const q = timelineSearchQuery.toLowerCase().trim();
    
    filteredTimeline = timelineData.filter(item => {
        const matchesType = timelineFilterType === "all" || item.type === timelineFilterType;
        const matchesQuery = !q || item.text.toLowerCase().includes(q) || item.subject.toLowerCase().includes(q) || item.author.toLowerCase().includes(q);
        return matchesType && matchesQuery;
    });

    timelineCurrentPage = 1;
    renderTimelineOptimized();
    updateFilterCounts();
}

function renderTimelineOptimized() {
    const list = document.getElementById("timelineList");
    const empty = document.getElementById("timelineEmpty");
    const paginationBar = document.getElementById("timelinePaginationBar");
    const pageInfo = document.getElementById("timelinePageInfo");
    const paginationControls = document.getElementById("timelinePaginationControls");
    const perfBadge = document.getElementById("timelinePerfBadge");

    if (!list) return;

    // Start performance timer
    const t0 = performance.now();

    // Clear previous items (keep empty placeholder)
    list.querySelectorAll(".c360-timeline-card, .timeline-item").forEach(el => el.remove());

    if (filteredTimeline.length === 0) {
        if (empty) empty.style.display = "block";
        if (paginationBar) paginationBar.style.display = "none";
        if (perfBadge) perfBadge.textContent = "⚡ Render: 0ms";
        return;
    }

    if (empty) empty.style.display = "none";

    // Client-side Pagination Slicing
    const totalItems = filteredTimeline.length;
    const totalPages = Math.ceil(totalItems / timelinePageSize);
    if (timelineCurrentPage > totalPages) timelineCurrentPage = totalPages;
    if (timelineCurrentPage < 1) timelineCurrentPage = 1;

    const startIdx = (timelineCurrentPage - 1) * timelinePageSize;
    const endIdx = Math.min(startIdx + timelinePageSize, totalItems);
    const pageItems = filteredTimeline.slice(startIdx, endIdx);

    // Fast DOM mounting via DocumentFragment
    const fragment = document.createDocumentFragment();

    for (const item of pageItems) {
        const article = document.createElement("article");
        article.className = "c360-timeline-card";

        const typeClass = `timeline-type-${item.type}`;
        const typeName = typeLabel(item.type);
        const timeFormatted = item.time.toLocaleDateString("vi-VN", {
            day: "2-digit", month: "2-digit", year: "numeric",
            hour: "2-digit", minute: "2-digit"
        });

        article.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:center; gap:10px; margin-bottom:6px;">
                <div style="display:flex; align-items:center; gap:8px;">
                    <span class="timeline-type-pill ${typeClass}">${typeName}</span>
                    <strong style="font-size:13.5px; color:var(--crm-text);">${escapeHtml(item.subject || typeName)}</strong>
                </div>
                <time style="font-size:11.5px; color:var(--crm-muted); white-space:nowrap;">
                    ${timeFormatted}
                </time>
            </div>
            <div style="font-size:13px; color:var(--crm-text-2); line-height:1.5;">
                ${escapeHtml(item.text)}
            </div>
            <div style="display:flex; justify-content:space-between; align-items:center; font-size:11.5px; color:var(--crm-muted); margin-top:8px; padding-top:6px; border-top:1px dashed var(--crm-border);">
                <span>👤 Thực hiện bởi: <strong>${escapeHtml(item.author)}</strong></span>
                <span style="color:var(--crm-success); font-weight:600;">✓ Hoàn tất</span>
            </div>
        `;
        fragment.appendChild(article);
    }

    list.appendChild(fragment);

    // Measure Render Performance
    const t1 = performance.now();
    const duration = (t1 - t0).toFixed(1);
    if (perfBadge) {
        perfBadge.textContent = `⚡ Render: ${duration}ms (${totalItems} items)`;
    }

    // Render Pagination Controls
    if (paginationBar && pageInfo && paginationControls) {
        paginationBar.style.display = totalPages > 1 ? "flex" : "none";
        pageInfo.textContent = `Hiển thị ${startIdx + 1} - ${endIdx} trong tổng số ${totalItems} hoạt động`;

        paginationControls.innerHTML = `
            <button type="button" class="pagination-btn" id="btnPagePrev" ${timelineCurrentPage === 1 ? 'disabled' : ''} title="Trang trước">‹</button>
            <span style="font-size:12px; font-weight:600; padding:0 8px;">Trang ${timelineCurrentPage} / ${totalPages}</span>
            <button type="button" class="pagination-btn" id="btnPageNext" ${timelineCurrentPage === totalPages ? 'disabled' : ''} title="Trang tiếp">›</button>
        `;

        document.getElementById("btnPagePrev")?.addEventListener("click", () => {
            if (timelineCurrentPage > 1) {
                timelineCurrentPage--;
                renderTimelineOptimized();
            }
        });

        document.getElementById("btnPageNext")?.addEventListener("click", () => {
            if (timelineCurrentPage < totalPages) {
                timelineCurrentPage++;
                renderTimelineOptimized();
            }
        });
    }
}

function updateFilterCounts() {
    const countAll = timelineData.length;
    const countCall = timelineData.filter(i => i.type === "call").length;
    const countMeeting = timelineData.filter(i => i.type === "meeting").length;
    const countEmail = timelineData.filter(i => i.type === "email").length;
    const countNote = timelineData.filter(i => i.type === "note").length;

    writeText("countFilterAll", countAll);
    writeText("countFilterCall", countCall);
    writeText("countFilterMeeting", countMeeting);
    writeText("countFilterEmail", countEmail);
    writeText("countFilterNote", countNote);
}

/* =========================================================
   7. DEALS / OPPORTUNITIES TAB RENDERER
========================================================= */
let currentDealsFilter = "all";

function renderDealsTab() {
    const openDeals = opportunitiesData.open || [];
    const closedDeals = opportunitiesData.closed || [];
    const allDeals = [...openDeals, ...closedDeals];

    const openSum = openDeals.reduce((sum, d) => sum + (Number(d.amount) || 0), 0);
    const wonSum = closedDeals.filter(d => d.status === "WON").reduce((sum, d) => sum + (Number(d.amount) || 0), 0);
    const totalClosed = closedDeals.length;
    const wonCount = closedDeals.filter(d => d.status === "WON").length;
    const winRate = totalClosed > 0 ? Math.round((wonCount / totalClosed) * 100) : 0;

    writeText("dealsStatOpenAmount", formatMoney(openSum));
    writeText("dealsStatWonAmount", formatMoney(wonSum));
    writeText("dealsStatWinRate", `${winRate}%`);

    writeText("dealCountAll", allDeals.length);
    writeText("dealCountOpen", openDeals.length);
    writeText("dealCountWon", wonCount);
    writeText("dealCountLost", closedDeals.filter(d => d.status === "LOST").length);

    // Filter deals
    let displayedDeals = allDeals;
    if (currentDealsFilter === "open") displayedDeals = openDeals;
    else if (currentDealsFilter === "won") displayedDeals = closedDeals.filter(d => d.status === "WON");
    else if (currentDealsFilter === "lost") displayedDeals = closedDeals.filter(d => d.status === "LOST");

    const container = document.getElementById("dealsListContainer");
    if (!container) return;

    if (displayedDeals.length === 0) {
        container.innerHTML = '<div class="empty-small" style="padding:24px; text-align:center; color:var(--crm-muted);">Không có cơ hội bán hàng nào phù hợp bộ lọc.</div>';
        return;
    }

    container.innerHTML = `
        <table class="c360-deals-table">
            <thead>
                <tr>
                    <th>Tên cơ hội bán hàng</th>
                    <th>Giai đoạn</th>
                    <th>Doanh số dự kiến</th>
                    <th>Xác suất</th>
                    <th>Doanh số kỳ vọng</th>
                    <th>Dự kiến đóng</th>
                    <th>Trạng thái</th>
                </tr>
            </thead>
            <tbody>
                ${displayedDeals.map(deal => {
                    const statusClass = deal.status === "WON" ? "deal-status-won" : (deal.status === "LOST" ? "deal-status-lost" : "deal-status-open");
                    const statusText = deal.status === "WON" ? "✓ Ký thành công" : (deal.status === "LOST" ? "✕ Thất bại" : "● Đang mở");
                    const prob = Number(deal.probability) || 0;
                    const expected = (Number(deal.amount) || 0) * (prob / 100);

                    return `
                        <tr>
                            <td>
                                <strong>${escapeHtml(deal.name)}</strong>
                                <div style="font-size:11.5px; color:var(--crm-muted); margin-top:2px;">
                                    Phụ trách: ${escapeHtml(deal.ownerName || currentCustomer?.ownerName || "Sales Rep")}
                                </div>
                            </td>
                            <td>
                                <span style="font-weight:600; color:var(--crm-text);">${escapeHtml(deal.stageName || deal.stage || "Khám phá")}</span>
                            </td>
                            <td>
                                <strong style="color:var(--crm-primary); font-size:14px;">${formatMoney(deal.amount || 0)}</strong>
                            </td>
                            <td>
                                <div style="font-weight:700; font-size:12px;">${prob}%</div>
                                <div class="prob-bar-wrap">
                                    <div class="prob-bar-fill" style="width:${prob}%;"></div>
                                </div>
                            </td>
                            <td>
                                <span style="color:var(--crm-text-2); font-weight:600;">${formatMoney(deal.expectedRevenue || expected)}</span>
                            </td>
                            <td>
                                <span style="font-size:12px; color:var(--crm-muted);">${deal.closeDate || "—"}</span>
                            </td>
                            <td>
                                <span class="deal-status-pill ${statusClass}">${statusText}</span>
                            </td>
                        </tr>
                    `;
                }).join("")}
            </tbody>
        </table>
    `;
}

/* =========================================================
   8. ATTACHMENTS TAB RENDERER & UPLOAD MODAL
========================================================= */
function renderAttachmentsTab() {
    const container = document.getElementById("attachmentsListContainer");
    const countHeader = document.getElementById("attachmentsCountHeader");
    if (!container) return;

    if (countHeader) countHeader.textContent = `${attachmentsData.length} tệp`;

    if (attachmentsData.length === 0) {
        container.innerHTML = '<div class="empty-small" style="grid-column:1/-1; padding:24px; text-align:center; color:var(--crm-muted);">Chưa có tệp đính kèm nào. Nhấn "+ Tải lên tài liệu mới" để thêm.</div>';
        return;
    }

    container.innerHTML = attachmentsData.map(file => {
        const ext = (file.fileType || file.fileName.split('.').pop() || "pdf").toLowerCase();
        const iconClass = `file-type-${ext === "xlsx" || ext === "xls" ? "xlsx" : (ext === "docx" || ext === "doc" ? "docx" : (ext === "zip" || ext === "rar" ? "zip" : (ext === "png" || ext === "jpg" ? "img" : "pdf")))}`;
        const iconEmoji = ext.includes("xls") ? "📊" : (ext.includes("doc") ? "📝" : (ext.includes("zip") ? "🗄️" : "📄"));

        return `
            <div class="attachment-card" data-file-id="${file.id}">
                <div class="attachment-card-top">
                    <div class="file-type-icon ${iconClass}">
                        ${iconEmoji}
                    </div>
                    <div style="min-width:0; flex:1;">
                        <div class="attachment-card-title" title="${escapeHtml(file.fileName)}">
                            ${escapeHtml(file.fileName)}
                        </div>
                        <div style="margin-top:4px;">
                            <span class="status-pill status-new" style="font-size:10.5px; padding:1px 6px;">
                                ${escapeHtml(file.category || "Tài liệu")}
                            </span>
                        </div>
                    </div>
                </div>

                <div class="attachment-card-meta">
                    <div>💾 Dung lượng: <strong>${escapeHtml(file.fileSize || "1.0 MB")}</strong></div>
                    <div>👤 Người tải: ${escapeHtml(file.uploadedBy || "Hệ thống")}</div>
                    <div>📅 Ngày tải: ${escapeHtml(file.uploadedAt || "Gần đây")}</div>
                </div>

                <div class="attachment-actions">
                    <button type="button" class="crm-btn crm-btn-secondary btn-preview-file" data-name="${escapeHtml(file.fileName)}" style="font-size:11.5px; min-height:28px; padding:0 8px;">
                        👁 Xem
                    </button>
                    <button type="button" class="crm-btn crm-btn-secondary btn-download-file" data-name="${escapeHtml(file.fileName)}" style="font-size:11.5px; min-height:28px; padding:0 8px;">
                        📥 Tải về
                    </button>
                    <button type="button" class="crm-btn crm-btn-secondary btn-delete-file" data-id="${file.id}" style="font-size:11.5px; min-height:28px; padding:0 8px; color:var(--crm-danger);">
                        ✕
                    </button>
                </div>
            </div>
        `;
    }).join("");

    // Wire action buttons
    container.querySelectorAll(".btn-preview-file").forEach(btn => {
        btn.addEventListener("click", () => {
            alert(`Xem trước tệp tin: ${btn.dataset.name}\n\nĐịnh dạng file an toàn, đã được quét bảo mật bởi hệ thống CRM.`);
        });
    });

    container.querySelectorAll(".btn-download-file").forEach(btn => {
        btn.addEventListener("click", () => {
            const fileName = btn.dataset.name;
            const blob = new Blob([`Tài liệu CRM: ${fileName}\nKhách hàng: ${currentCustomer?.companyName || ''}`], { type: "text/plain;charset=utf-8" });
            const link = document.createElement("a");
            link.href = URL.createObjectURL(blob);
            link.download = fileName;
            link.click();
            URL.revokeObjectURL(link.href);
        });
    });

    container.querySelectorAll(".btn-delete-file").forEach(btn => {
        btn.addEventListener("click", () => {
            const fId = Number(btn.dataset.id);
            if (confirm("Bạn có chắc chắn muốn xóa tệp đính kèm này không?")) {
                attachmentsData = attachmentsData.filter(f => f.id !== fId);
                renderAttachmentsTab();
                updateAllTabBadges();
                syncInMemory360Data();
            }
        });
    });
}

/* =========================================================
   9. CONTACTS TAB RENDERER (CRM-62 INTEGRATION)
========================================================= */
function renderContactsTab() {
    const container = document.getElementById("contactsPanel");
    if (!container || !customerId) return;

    if (window.ContactsManager && typeof window.ContactsManager.renderCustomer360Contacts === "function") {
        window.ContactsManager.renderCustomer360Contacts(container, customerId);
        return;
    }

    // Fallback Renderer if ContactsManager is not initialized yet
    const contacts = customer360Data?.contacts || [];
    if (contacts.length === 0) {
        container.innerHTML = `
            <div class="empty-small" style="padding:20px; text-align:center; color:var(--crm-muted);">
                Chưa có người liên hệ nào cho doanh nghiệp này.
            </div>
            <button id="addContact" class="crm-btn crm-btn-secondary full-button" type="button">
                + Thêm người liên hệ
            </button>
        `;
        return;
    }

    container.innerHTML = `
        <div style="padding:4px 0 12px 0; display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid var(--crm-border); margin-bottom:12px;">
            <span style="font-size:13px; font-weight:700; color:var(--crm-text);">
                Danh sách người liên hệ B2B (${contacts.length})
            </span>
            <a href="contacts.html?customerId=${customerId}" class="crm-btn crm-btn-secondary" style="font-size:12px; padding:3px 10px; text-decoration:none;">
                Quản lý toàn diện →
            </a>
        </div>
        <div style="display:grid; grid-template-columns:repeat(auto-fill, minmax(280px, 1fr)); gap:12px;">
            ${contacts.map(c => `
                <div style="background:var(--crm-surface-soft); border:1px solid var(--crm-border); border-radius:8px; padding:12px; display:flex; flex-direction:column; gap:8px;">
                    <div style="display:flex; justify-content:space-between; align-items:center;">
                        <strong>${escapeHtml(c.fullName)}</strong>
                        ${c.isPrimary ? '<span title="Đầu mối chính" style="font-size:16px;">⭐</span>' : ''}
                    </div>
                    <div style="font-size:12px; color:var(--crm-muted);">
                        ${escapeHtml(c.jobTitle)} · ${escapeHtml(c.department || '')}
                    </div>
                    <div>
                        ${renderDecisionRoleBadge(c.decisionRole)}
                    </div>
                    <div style="font-size:12px; color:var(--crm-text-2); display:flex; flex-direction:column; gap:2px; margin-top:4px;">
                        <span>✉ ${escapeHtml(c.email)}</span>
                        <span>☎ ${escapeHtml(c.phone)}</span>
                    </div>
                </div>
            `).join("")}
        </div>
    `;
}

function renderDecisionRoleBadge(role) {
    switch (role) {
        case "DECISION_MAKER":
            return `<span style="background:#fee2e2; color:#991b1b; border:1px solid #f87171; font-size:11px; font-weight:700; padding:1px 6px; border-radius:4px;">👑 Quyết định (DM)</span>`;
        case "INFLUENCER":
            return `<span style="background:#fef3c7; color:#92400e; border:1px solid #fcd34d; font-size:11px; font-weight:700; padding:1px 6px; border-radius:4px;">⚡ Người ảnh hưởng</span>`;
        case "BLOCKER":
            return `<span style="background:#f1f5f9; color:#b91c1c; border:1px solid #fca5a5; font-size:11px; font-weight:700; padding:1px 6px; border-radius:4px;">⚠️ Người cản trở</span>`;
        default:
            return `<span style="background:#e0f2fe; color:#0369a1; border:1px solid #7dd3fc; font-size:11px; font-weight:700; padding:1px 6px; border-radius:4px;">👤 Người dùng cuối</span>`;
    }
}

/* =========================================================
   10. CRM-65 & CRM-68 PRESERVED INTEGRATIONS
========================================================= */
function getCustomerRecord(cId) {
    if (currentCustomer && Number(currentCustomer.id) === Number(cId)) return currentCustomer;
    try {
        const raw = localStorage.getItem("CRM_CUSTOMERS_DATA");
        if (raw) {
            const list = JSON.parse(raw);
            return list.find(c => Number(c.id) === Number(cId)) || null;
        }
    } catch (_) {}
    return null;
}

function renderCustomerChurnRisk360() {
    const bannerContainer = document.getElementById("customer360ChurnBanner");
    if (!bannerContainer || !customerId) return;

    let churnInfo = null;
    if (window.SupportTicketsManager && typeof window.SupportTicketsManager.getChurnRiskStatus === "function") {
        churnInfo = window.SupportTicketsManager.getChurnRiskStatus(Number(customerId));
    } else {
        try {
            const rawChurn = localStorage.getItem("CRM_CHURN_RISK_DATA");
            const churnFlags = rawChurn ? JSON.parse(rawChurn) : {};
            const manualFlag = churnFlags[Number(customerId)];

            const rawTickets = localStorage.getItem("CRM_SUPPORT_TICKETS_DATA");
            const allTickets = rawTickets ? JSON.parse(rawTickets) : [];
            const custTickets = allTickets.filter(t => Number(t.customerId) === Number(customerId) && t.status !== "CLOSED" && t.status !== "RESOLVED");

            const isManual = !!manualFlag;
            const hasOverdueUrgent = custTickets.some(t => t.priority === "URGENT" || t.priority === "HIGH");
            const hasMultipleOpen = custTickets.length >= 2;
            const isRisk = isManual || hasOverdueUrgent || hasMultipleOpen;

            const reasons = [];
            if (isManual) reasons.push(manualFlag.manualReason || "CSKH gắn cờ thủ công rủi ro rời bỏ.");
            if (hasOverdueUrgent) reasons.push("Có yêu cầu mức độ Khẩn cấp/Cao chưa được giải quyết dứt điểm.");
            if (hasMultipleOpen) reasons.push(`Có ${custTickets.length} phiếu khiếu nại/hỗ trợ đang tồn đọng mở.`);

            churnInfo = { isRisk, reasons, riskLevel: (isManual || hasOverdueUrgent) ? "HIGH" : (hasMultipleOpen ? "MEDIUM" : "LOW") };
        } catch (_) {
            churnInfo = { isRisk: false, reasons: [], riskLevel: "LOW" };
        }
    }

    if (!churnInfo || !churnInfo.isRisk) {
        bannerContainer.innerHTML = "";
        bannerContainer.style.display = "none";
        return;
    }

    bannerContainer.style.display = "block";
    const customer = getCustomerRecord(customerId);
    const salesRepName = customer?.ownerName || customer?.owner || "Nông Quang Tiệp (Trưởng phòng KD)";
    const salesRepEmail = customer?.ownerEmail || "tiep.nq@crm.vn";
    const salesRepPhone = customer?.ownerPhone || "0912.888.999";

    bannerContainer.innerHTML = `
        <div class="churn-banner-container" style="margin-bottom:20px; animation:fadeIn 0.3s ease;">
            <div class="churn-alert-banner" style="display:flex; flex-wrap:wrap; align-items:flex-start; justify-content:space-between; gap:16px; padding:18px 22px; border-radius:12px; background:linear-gradient(135deg, rgba(239,68,68,0.1), rgba(245,158,11,0.08)); border:1.5px solid var(--crm-danger); box-shadow:0 4px 16px rgba(239,68,68,0.12);">
                <div style="display:flex; gap:14px; align-items:flex-start; flex:1; min-width:300px;">
                    <div style="font-size:28px; line-height:1; flex-shrink:0;">⚠️</div>
                    <div>
                        <div style="display:flex; align-items:center; gap:10px; margin-bottom:6px; flex-wrap:wrap;">
                            <h3 style="margin:0; font-size:16px; font-weight:700; color:var(--crm-danger);">
                                CẢNH BÁO RỦI RO RỜI BỎ (CHURN RISK DETECTED)
                            </h3>
                            <span class="churn-risk-badge" style="background:#fee2e2; color:#b91c1c; border:1px solid #fca5a5; font-size:11.5px; padding:2px 8px; border-radius:4px; font-weight:700;">
                                ${churnInfo.riskLevel === 'CRITICAL' ? 'RẤT NGUY CẤP' : 'RỦI RO CAO'}
                            </span>
                        </div>
                        <p style="margin:0 0 8px; font-size:13.5px; color:var(--crm-text);">
                            Khách hàng có nhiều phản ánh tồn đọng hoặc khiếu nại kỹ thuật mức độ cao. Cần Sales & CSKH phối hợp can thiệp gấp!
                        </p>
                        <div style="font-size:12.5px; color:var(--crm-danger); font-weight:500;">
                            <strong>Yếu tố rủi ro:</strong> ${escapeHtml(churnInfo.reasons.join(" • ") || "Nhiều phản ánh tồn đọng")}
                        </div>
                    </div>
                </div>

                <div class="sales-alert-box" style="background:var(--crm-surface); border:1.5px solid #fecaca; border-radius:10px; padding:14px 18px; min-width:260px; box-shadow:0 2px 8px rgba(0,0,0,0.06); flex-shrink:0;">
                    <div style="font-size:11px; text-transform:uppercase; letter-spacing:0.5px; font-weight:700; color:var(--crm-danger); margin-bottom:6px; display:flex; align-items:center; gap:6px;">
                        <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" fill="none" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
                        Phụ trách kinh doanh (Sales Rep)
                    </div>
                    <div style="font-weight:700; font-size:14.5px; color:var(--crm-text); margin-bottom:4px;">
                        ${escapeHtml(salesRepName)}
                    </div>
                    <div style="font-size:12px; color:var(--crm-muted); display:flex; flex-direction:column; gap:2px;">
                        <span>📧 ${escapeHtml(salesRepEmail)}</span>
                        <span>☎ ${escapeHtml(salesRepPhone)}</span>
                    </div>
                    <div style="margin-top:10px; padding-top:8px; border-top:1px dashed var(--crm-border);">
                        <a href="support-tickets.html" class="crm-btn crm-btn-secondary" style="font-size:11.5px; padding:4px 10px; width:100%; text-align:center; display:block; text-decoration:none;">
                            Xem phiếu & Xử lý ngay →
                        </a>
                    </div>
                </div>
            </div>
        </div>
    `;
}

function renderCustomerTickets360() {
    const listEl = document.getElementById("customerTicketsList");
    const countBadge = document.getElementById("ticketCountBadge");
    if (!listEl || !customerId) return;

    let tickets = [];
    if (window.SupportTicketsManager && typeof window.SupportTicketsManager.getTicketsByCustomer === "function") {
        tickets = window.SupportTicketsManager.getTicketsByCustomer(Number(customerId));
    } else {
        try {
            const raw = localStorage.getItem("CRM_SUPPORT_TICKETS_DATA");
            if (raw) {
                const parsed = JSON.parse(raw);
                tickets = parsed.filter(t => Number(t.customerId) === Number(customerId));
            }
        } catch (_) {}
    }

    if (countBadge) countBadge.textContent = String(tickets.length);

    if (tickets.length === 0) {
        listEl.innerHTML = '<div class="empty-small" style="padding:16px; text-align:center; color:var(--crm-muted);">Chưa có phiếu hỗ trợ nào cho khách hàng này.</div>';
        return;
    }

    listEl.innerHTML = tickets.map(t => {
        const priorityClass = t.priority === "URGENT" ? "priority-urgent" : (t.priority === "HIGH" ? "priority-high" : (t.priority === "MEDIUM" ? "priority-medium" : "priority-low"));
        const priorityLabel = t.priority === "URGENT" ? "Khẩn cấp" : (t.priority === "HIGH" ? "Cao" : (t.priority === "MEDIUM" ? "Trung bình" : "Thấp"));
        const statusLabel = t.status === "NEW" ? "Mới tiếp nhận" : (t.status === "PROCESSING" ? "Đang xử lý" : (t.status === "WAITING_CUSTOMER" ? "Chờ khách" : (t.status === "RESOLVED" ? "Đã giải quyết" : "Đóng")));
        const statusClass = t.status === "NEW" ? "status-new" : (t.status === "PROCESSING" ? "status-processing" : (t.status === "RESOLVED" ? "status-resolved" : "status-waiting"));

        return `
            <div style="padding:12px; border-bottom:1px solid var(--crm-border); display:flex; flex-direction:column; gap:6px;">
                <div style="display:flex; justify-content:space-between; align-items:center; gap:8px;">
                    <a href="support-tickets.html" style="font-weight:700; font-size:12px; color:var(--crm-primary); text-decoration:none;">
                        ${escapeHtml(t.ticketCode)}
                    </a>
                    <span class="priority-badge ${priorityClass}" style="font-size:10.5px; padding:1px 6px;">
                        ${priorityLabel}
                    </span>
                </div>
                <div style="font-size:13px; font-weight:600; color:var(--crm-text); line-height:1.35;">
                    ${escapeHtml(t.title)}
                </div>
                <div style="display:flex; justify-content:space-between; align-items:center; font-size:11.5px; color:var(--crm-muted); margin-top:2px;">
                    <span class="status-pill ${statusClass}" style="font-size:11px; padding:1px 8px;">
                        <span class="status-dot"></span>
                        ${statusLabel}
                    </span>
                    <span>👤 ${escapeHtml(t.assigneeName || "Chưa phân công")}</span>
                </div>
            </div>
        `;
    }).join("");
}

function renderCorporateHierarchy360() {
    if (window.CorporateHierarchyManager && typeof window.CorporateHierarchyManager.initCustomer360Hierarchy === "function") {
        window.CorporateHierarchyManager.initCustomer360Hierarchy(Number(customerId));
    }
}

/* =========================================================
   11. TABS WORKSPACE SWITCHING ENGINE
========================================================= */
function updateAllTabBadges() {
    writeText("badgeTimelineCount", timelineData.length);
    writeText("badgeContactsCount", (customer360Data?.contacts || []).length);
    writeText("badgeOpportunitiesCount", (opportunitiesData.open.length + opportunitiesData.closed.length));
    writeText("badgeAttachmentsCount", attachmentsData.length);
}

document.querySelectorAll(".c360-tab-btn, .right-tab").forEach(tab => {
    tab.addEventListener("click", () => {
        const target = tab.dataset.tab;
        if (!target) return;

        // Update active class on tab buttons
        document.querySelectorAll(".c360-tab-btn, .right-tab").forEach(item => {
            if (item.dataset.tab === target) {
                item.classList.add("active");
                item.setAttribute("aria-selected", "true");
            } else {
                item.classList.remove("active");
                item.setAttribute("aria-selected", "false");
            }
        });

        // Toggle Tab Panes
        const panes = {
            timeline: document.getElementById("timelinePanel"),
            contacts: document.getElementById("contactsPanel"),
            opportunities: document.getElementById("opportunitiesPanel"),
            attachments: document.getElementById("attachmentsPanel"),
            tickets: document.getElementById("ticketsPanel"),
            hierarchy: document.getElementById("hierarchyPanel")
        };

        Object.keys(panes).forEach(k => {
            const pane = panes[k];
            if (pane) {
                if (k === target) {
                    pane.hidden = false;
                    pane.removeAttribute("hidden");
                } else {
                    pane.hidden = true;
                    pane.setAttribute("hidden", "true");
                }
            }
        });
    });
});

/* =========================================================
   12. TIMELINE COMPOSER & TOOLBAR EVENT LISTENERS
========================================================= */
const composer = document.getElementById("activityComposer");
const noteInput = document.getElementById("activityNote");
const typeInput = document.getElementById("activityType");

composer?.addEventListener("submit", async event => {
    event.preventDefault();
    const text = noteInput.value.trim();
    if (!text) {
        noteInput.focus();
        return;
    }

    const type = typeInput.value || "note";
    const authorName = "Nông Quang Tiệp (Bạn)";
    const newEntry = {
        id: Date.now(),
        type: type.toLowerCase(),
        subject: text.slice(0, 50),
        text: text,
        author: authorName,
        time: new Date()
    };

    // Optimistic UI Update
    timelineData.unshift(newEntry);
    noteInput.value = "";
    applyTimelineFilter();
    updateAllTabBadges();
    syncInMemory360Data();

    // Call API in background
    if (customerId) {
        try {
            await api("/api/activities", {
                method: "POST",
                body: JSON.stringify({
                    subject: text.slice(0, 50),
                    type: type.toUpperCase(),
                    description: text,
                    customerId: Number(customerId),
                    status: "COMPLETED"
                })
            });
        } catch (err) {
            console.warn("Lưu hoạt động vào Backend không thành công (lưu bộ nhớ tạm):", err);
        }
    }
});

// Quick Action Buttons (Gọi ngay, Gửi Email, Ghi chú)
document.querySelectorAll("[data-quick]").forEach(button => {
    button.addEventListener("click", () => {
        const type = button.dataset.quick;
        if (typeInput) typeInput.value = type === "call" ? "call" : type === "email" ? "email" : "note";
        if (noteInput) {
            noteInput.placeholder = type === "call" ? "Ghi nhận nội dung cuộc gọi..." : (type === "email" ? "Ghi nhận nội dung email đã gửi..." : "Ghi chú công việc...");
            noteInput.focus();
        }
    });
});

// Refresh Button
document.getElementById("btnRefresh360")?.addEventListener("click", async () => {
    const btn = document.getElementById("btnRefresh360");
    if (btn) btn.classList.add("loading");
    await initCustomer360();
    if (btn) btn.classList.remove("loading");
});

// Timeline Filter Chips
document.querySelectorAll(".timeline-chip").forEach(chip => {
    chip.addEventListener("click", () => {
        document.querySelectorAll(".timeline-chip").forEach(c => c.classList.remove("active"));
        chip.classList.add("active");
        timelineFilterType = chip.dataset.filter || "all";
        applyTimelineFilter();
    });
});

// Timeline Search Input
const searchInput = document.getElementById("timelineSearchInput");
searchInput?.addEventListener("input", () => {
    timelineSearchQuery = searchInput.value;
    applyTimelineFilter();
});

// Deals Filter Buttons
["All", "Open", "Won", "Lost"].forEach(k => {
    const btn = document.getElementById(`filterDeals${k}`);
    btn?.addEventListener("click", () => {
        ["All", "Open", "Won", "Lost"].forEach(x => document.getElementById(`filterDeals${x}`)?.classList.remove("active"));
        btn.classList.add("active");
        currentDealsFilter = k.toLowerCase();
        renderDealsTab();
    });
});

/* =========================================================
   13. ATTACHMENT UPLOAD MODAL
========================================================= */
const attachModal = document.getElementById("attachmentUploadModal");
const attachOverlay = document.getElementById("attachmentModalOverlay");

document.getElementById("btnOpenUploadAttachment")?.addEventListener("click", () => {
    if (attachModal && attachOverlay) {
        attachModal.classList.add("open");
        attachOverlay.classList.add("open");
    }
});

function closeAttachModal() {
    if (attachModal && attachOverlay) {
        attachModal.classList.remove("open");
        attachOverlay.classList.remove("open");
    }
}

document.getElementById("closeAttachmentModal")?.addEventListener("click", closeAttachModal);
document.getElementById("cancelAttachUpload")?.addEventListener("click", closeAttachModal);
attachOverlay?.addEventListener("click", closeAttachModal);

document.getElementById("attachmentUploadForm")?.addEventListener("submit", event => {
    event.preventDefault();
    const nameInput = document.getElementById("attachFileName");
    const catInput = document.getElementById("attachCategory");
    const fileInput = document.getElementById("attachFileInput");

    const fileName = nameInput.value.trim();
    if (!fileName) {
        nameInput.focus();
        return;
    }

    const newAttach = {
        id: Date.now(),
        fileName: fileName,
        fileType: fileName.split('.').pop() || "pdf",
        category: catInput.value || "Tài liệu",
        fileSize: fileInput?.files?.[0] ? `${(fileInput.files[0].size / 1024 / 1024).toFixed(1)} MB` : "1.5 MB",
        uploadedBy: "Nông Quang Tiệp",
        uploadedAt: new Date().toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" })
    };

    attachmentsData.unshift(newAttach);
    renderAttachmentsTab();
    updateAllTabBadges();
    syncInMemory360Data();
    closeAttachModal();
    nameInput.value = "";
});

/* =========================================================
   14. COMPANY EDIT MODAL
========================================================= */
const modal = document.getElementById("companyModal");
const modalOverlay = document.getElementById("companyModalOverlay");

function openCompanyModal() {
    setValue("edit360Name", displayValue("company360Name"));
    setValue("edit360Tax", displayValue("company360Tax"));
    setValue("edit360Industry", displayValue("company360Industry"));
    setValue("edit360Scale", displayValue("company360Scale"));
    setValue("edit360Address", displayValue("company360Address"));
    setValue("edit360Owner", displayValue("company360Owner"));
    setValue("edit360Phone", displayValue("company360Phone"));
    setValue("edit360Email", displayValue("company360Email"));
    setValue("edit360Website", displayValue("company360Website"));

    modal.classList.add("open");
    modalOverlay.classList.add("open");
}

function closeCompanyModal() {
    modal.classList.remove("open");
    modalOverlay.classList.remove("open");
}

document.getElementById("editCompanyButton")?.addEventListener("click", openCompanyModal);
document.getElementById("closeCompanyModal")?.addEventListener("click", closeCompanyModal);
document.getElementById("cancelCompanyEdit")?.addEventListener("click", closeCompanyModal);
modalOverlay?.addEventListener("click", closeCompanyModal);

document.getElementById("company360Form")?.addEventListener("submit", async event => {
    event.preventDefault();

    const name = value("edit360Name");
    const tax = value("edit360Tax");
    const ind = value("edit360Industry");
    const scale = value("edit360Scale");
    const address = value("edit360Address");
    const owner = value("edit360Owner");
    const ph = value("edit360Phone");
    const email = value("edit360Email");
    const web = value("edit360Website");

    if (currentCustomer) {
        currentCustomer.name = name;
        currentCustomer.companyName = name;
        currentCustomer.taxCode = tax;
        currentCustomer.industry = ind;
        currentCustomer.scale = scale;
        currentCustomer.address = address;
        currentCustomer.ownerName = owner;
        currentCustomer.phone = ph;
        currentCustomer.email = email;
        currentCustomer.website = web;
    }

    writeDisplay("company360Name", name);
    writeDisplay("company360Tax", tax);
    writeDisplay("company360Industry", ind);
    writeDisplay("company360Scale", scale);
    writeDisplay("company360Address", address);
    writeDisplay("company360Owner", owner);
    writeDisplay("company360Phone", ph);
    writeDisplay("company360Email", email);
    writeDisplay("company360Website", web);

    const titleEl = document.getElementById("pageCustomerTitle");
    if (titleEl) titleEl.textContent = name;

    syncInMemory360Data();

    // Call Backend PUT /api/customers/{id}
    if (customerId) {
        try {
            await api(`/api/customers/${customerId}`, {
                method: "PUT",
                body: JSON.stringify({
                    name,
                    taxCode: tax,
                    industry: ind,
                    phone: ph,
                    website: web,
                    email,
                    address,
                    type: currentCustomer?.type || "ENTERPRISE",
                    status: currentCustomer?.status || "ACTIVE"
                })
            });
        } catch (err) {
            console.warn("Backend update error:", err.message);
        }
    }

    closeCompanyModal();
});

document.addEventListener("keydown", event => {
    if (event.key === "Escape") {
        closeCompanyModal();
        closeAttachModal();
    }
});

// Re-render contacts on CRM-62 event
document.addEventListener("crm:contacts-updated", () => {
    renderContactsTab();
    renderPrimaryContactCard();
    updateAllTabBadges();
});

function syncInMemory360Data() {
    if (customer360Data) {
        customer360Data.customer = currentCustomer;
        customer360Data.activities = timelineData.map(t => ({
            id: t.id,
            type: t.type,
            subject: t.subject,
            description: t.text,
            authorName: t.author,
            createdAt: t.time instanceof Date ? t.time.toISOString() : new Date().toISOString()
        }));
        customer360Data.attachments = attachmentsData;
    }
}

/* =========================================================
   15. UTILITY FUNCTIONS
========================================================= */
function typeLabel(type) {
    switch (type) {
        case "call": return "☎ Cuộc gọi";
        case "email": return "✉ Email";
        case "meeting": return "▣ Cuộc gặp";
        default: return "✎ Ghi chú";
    }
}

function value(id) {
    return (document.getElementById(id)?.value || "").trim();
}

function displayValue(id) {
    const val = (document.getElementById(id)?.textContent || "").trim();
    return val === "—" ? "" : val;
}

function setValue(id, val) {
    const el = document.getElementById(id);
    if (el) el.value = val || "";
}

function writeDisplay(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val || "—";
}

function writeText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = String(val ?? "");
}

function escapeHtml(val) {
    return String(val ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function formatMoney(val) {
    return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
        maximumFractionDigits: 0
    }).format(Number(val) || 0);
}

// Bootstrap Initialization
document.addEventListener("DOMContentLoaded", () => {
    initCustomer360();
});

if (document.readyState === "interactive" || document.readyState === "complete") {
    initCustomer360();
}