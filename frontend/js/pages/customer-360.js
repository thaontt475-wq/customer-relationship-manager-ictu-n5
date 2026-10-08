"use strict";

/**
 * CUSTOMER 360 CONTROLLER (CRM-63)
 * Full Enterprise Client Hub: Overview, Financial KPIs, Contacts with Buying Roles,
 * Deals (Open vs Closed), Activity Timeline (Optimized <1.5s for 500+ items), Attachments.
 * Synchronized with LocalStorage key: CRM_CUSTOMER_360_DATA.
 */

const API_BASE = "http://localhost:8080/crm";
const STORAGE_KEY_360 = "CRM_CUSTOMER_360_DATA";
const STORAGE_KEY_CUSTOMERS = "CRM_CUSTOMERS_DATA";

// Global URL parameters
const urlParams = new URLSearchParams(window.location.search);
const currentCustomerId = Number(urlParams.get("id")) || 1;

// Active in-memory state
let active360Record = null;
let currentTimelineFilter = "all";
let timelineRenderLimit = 20;

/* =========================================================
   1. DEFAULT SEED DATA FOR CRM_CUSTOMER_360_DATA
   ========================================================= */
const DEFAULT_SEED_360_DATA = {
    1: {
        id: 1,
        companyName: "Tập đoàn Công nghệ FPT",
        taxCode: "0101234567",
        industry: "Công nghệ thông tin & Viễn thông",
        companySize: "Trên 1.000 nhân sự",
        phone: "02473007300",
        website: "https://fpt.com.vn",
        address: "Tòa nhà FPT, Số 10 Phạm Văn Bạch, Cầu Giấy, Hà Nội",
        ownerName: "Nông Quang Tiệp (Trưởng phòng KD)",
        ownerEmail: "tiepnq@crm.vn",
        ownerPhone: "0912.888.999",
        status: "CHINH_THUC",
        lifecycleStatus: "Chính thức",
        annualRevenue: "55.000 tỷ VNĐ",
        healthScore: "Tốt (96/100)",
        contacts: [
            {
                id: 101,
                fullName: "Nguyễn Văn Khoa",
                title: "Tổng Giám Đốc (CEO)",
                email: "khoanv@fpt.com.vn",
                phone: "0903.123.456",
                buyingRole: "DECISION_MAKER",
                isPrimary: true
            },
            {
                id: 102,
                fullName: "Trần Đăng Hòa",
                title: "Phó Tổng Giám Đốc phụ trách Kỹ thuật",
                email: "hoatd@fpt.com.vn",
                phone: "0912.345.678",
                buyingRole: "INFLUENCER",
                isPrimary: false
            },
            {
                id: 103,
                fullName: "Lê Bích Phượng",
                title: "Trưởng phòng Chuyển đổi số & Vận hành",
                email: "phuonglb@fpt.com.vn",
                phone: "0988.777.666",
                buyingRole: "END_USER",
                isPrimary: false
            },
            {
                id: 104,
                fullName: "Phạm Quốc Toàn",
                title: "Giám đốc Kiểm soát Tuân thủ & Mua sắm",
                email: "toanpq@fpt.com.vn",
                phone: "0977.555.444",
                buyingRole: "BLOCKER",
                isPrimary: false
            }
        ],
        deals: [
            {
                id: 201,
                name: "Hệ thống Quản lý Khách hàng Doanh nghiệp Cloud 2026",
                amount: 650000000,
                stage: "Đàm phán hợp đồng",
                probability: 80,
                status: "OPEN",
                expectedCloseDate: "2026-11-15",
                owner: "Nông Quang Tiệp"
            },
            {
                id: 202,
                name: "Gói Dịch vụ DevOps CI/CD & Microservices Migration",
                amount: 320000000,
                stage: "Báo giá & Demo",
                probability: 60,
                status: "OPEN",
                expectedCloseDate: "2026-12-01",
                owner: "Nguyễn Văn An"
            },
            {
                id: 203,
                name: "Tích hợp Cổng Định danh Tập trung SSO & Keycloak",
                amount: 180000000,
                stage: "Khảo sát nhu cầu",
                probability: 30,
                status: "OPEN",
                expectedCloseDate: "2026-12-25",
                owner: "Trần Thị Mai"
            },
            {
                id: 204,
                name: "Hợp đồng Bản quyền CRM Core Edition Phase 1",
                amount: 1250000000,
                stage: "Thành công (Won)",
                probability: 100,
                status: "CLOSED_WON",
                closeDate: "2026-08-20",
                winReason: "Đáp ứng chuẩn bảo mật và tích hợp ERP SAP sẵn có."
            },
            {
                id: 205,
                name: "Gói Đào tạo Quản trị viên & Chuyển giao công nghệ",
                amount: 150000000,
                stage: "Thành công (Won)",
                probability: 100,
                status: "CLOSED_WON",
                closeDate: "2026-09-05",
                winReason: "Hỗ trợ onsite chuyên sâu."
            },
            {
                id: 206,
                name: "Thí điểm AI Chatbot Tự động CSKH Khối Bán lẻ",
                amount: 220000000,
                stage: "Thất bại (Lost)",
                probability: 0,
                status: "CLOSED_LOST",
                closeDate: "2026-07-10",
                lostReason: "Khách hàng ưu tiên xây dựng in-house module."
            }
        ],
        activities: [
            {
                id: 301,
                type: "call",
                subject: "Trao đổi tiến độ hợp đồng giai đoạn 2",
                text: "Đã gọi điện cho anh Nguyễn Văn Khoa (CEO) chốt các điều khoản thanh toán 30-40-30 cho gói CRM Cloud 2026. Anh Khoa đồng ý gửi dự thảo cho ban kiểm soát rà soát.",
                author: "Nông Quang Tiệp",
                time: new Date(Date.now() - 1000 * 60 * 45) // 45 phút trước
            },
            {
                id: 302,
                type: "email",
                subject: "Gửi báo giá cập nhật & Phụ lục SLA kỹ thuật",
                text: "Đã gửi bản chào giá số BG-2026-FPT-03 đính kèm cam kết SLA 99.9% uptime và hồ sơ năng lực đội ngũ triển khai.",
                author: "Nông Quang Tiệp",
                time: new Date(Date.now() - 1000 * 60 * 60 * 5) // 5 giờ trước
            },
            {
                id: 303,
                type: "meeting",
                subject: "Họp Demo kiến trúc Microservices & SSO tại FPT Tower",
                text: "Buổi họp kỹ thuật với anh Trần Đăng Hòa và chị Lê Bích Phượng. Đội kỹ thuật FPT đánh giá cao giải pháp bảo mật phân quyền Role/Permission và đồng bộ webhook.",
                author: "Nguyễn Văn An",
                time: new Date(Date.now() - 1000 * 60 * 60 * 28) // 1 ngày trước
            },
            {
                id: 304,
                type: "note",
                subject: "Ghi chú chiến lược tiếp cận ban kiểm soát",
                text: "Lưu ý anh Phạm Quốc Toàn (Blocker) rất chặt chẽ về điều khoản phạt tiến độ. Cần chuẩn bị kỹ biên bản cam kết giải ngân và nghiệm thu theo sprint.",
                author: "Nông Quang Tiệp",
                time: new Date(Date.now() - 1000 * 60 * 60 * 52) // 2 ngày trước
            },
            {
                id: 305,
                type: "call",
                subject: "Khảo sát yêu cầu tích hợp SAP ERP",
                text: "Gọi điện làm rõ giao thức REST API và format dữ liệu hóa đơn điện tử với đại diện phòng kế toán tài chính FPT.",
                author: "Trần Thị Mai",
                time: new Date(Date.now() - 1000 * 60 * 60 * 96) // 4 ngày trước
            }
        ],
        attachments: [
            {
                id: 401,
                fileName: "Hop_dong_nguyen_tac_CRM_Cloud_2026_signed.pdf",
                fileType: "Hợp đồng kinh tế",
                fileSize: "3.4 MB",
                uploader: "Nông Quang Tiệp",
                uploadedAt: "05/10/2026",
                icon: "📄"
            },
            {
                id: 402,
                fileName: "Bao_gia_giai_phap_DevOps_CI_CD_v3.xlsx",
                fileType: "Báo giá giải pháp",
                fileSize: "820 KB",
                uploader: "Nguyễn Văn An",
                uploadedAt: "04/10/2026",
                icon: "📊"
            },
            {
                id: 403,
                fileName: "Thoa_thuan_bao_mat_thong_tin_NDA_FPT_Corp.pdf",
                fileType: "Thỏa thuận NDA",
                fileSize: "1.2 MB",
                uploader: "Nông Quang Tiệp",
                uploadedAt: "28/09/2026",
                icon: "🔒"
            },
            {
                id: 404,
                fileName: "Yeu_cau_ky_thuat_va_Kien_truc_RFP_v2.docx",
                fileType: "Yêu cầu RFP",
                fileSize: "4.8 MB",
                uploader: "Trần Thị Mai",
                uploadedAt: "20/09/2026",
                icon: "📑"
            }
        ]
    },
    2: {
        id: 2,
        companyName: "Tập đoàn Công nghiệp - Viễn thông Quân đội (Viettel)",
        taxCode: "0100109106",
        industry: "Viễn thông & Hạ tầng số",
        companySize: "Trên 1.000 nhân sự",
        phone: "02462556789",
        website: "https://viettel.vn",
        address: "Số 1 Trần Hữu Dực, Mỹ Đình 2, Nam Từ Liêm, Hà Nội",
        ownerName: "Lê Hoàng Nam",
        ownerEmail: "namlh@crm.vn",
        ownerPhone: "0988.111.222",
        status: "CHINH_THUC",
        lifecycleStatus: "Chính thức",
        annualRevenue: "160.000 tỷ VNĐ",
        healthScore: "Tốt (92/100)",
        contacts: [
            {
                id: 105,
                fullName: "Tào Đức Thắng",
                title: "Chủ tịch kiêm Tổng Giám Đốc",
                email: "thangtd@viettel.vn",
                phone: "0988.111.333",
                buyingRole: "DECISION_MAKER",
                isPrimary: true
            },
            {
                id: 106,
                fullName: "Nguyễn Vũ Hà",
                title: "Tổng Giám đốc Viettel High Tech",
                email: "hanv@viettel.vn",
                phone: "0983.444.555",
                buyingRole: "INFLUENCER",
                isPrimary: false
            }
        ],
        deals: [
            {
                id: 207,
                name: "Cung cấp Hạ tầng Cloud B2B & Bảo mật Trung tâm dữ liệu",
                amount: 1850000000,
                stage: "Thành công (Won)",
                probability: 100,
                status: "CLOSED_WON",
                closeDate: "2026-09-12",
                winReason: "Đáp ứng tiêu chuẩn an toàn thông tin cấp độ 4."
            },
            {
                id: 208,
                name: "Mở rộng 500 Agent CSKH Đa kênh Omni-channel",
                amount: 450000000,
                stage: "Đàm phán hợp đồng",
                probability: 80,
                status: "OPEN",
                expectedCloseDate: "2026-11-20",
                owner: "Lê Hoàng Nam"
            }
        ],
        activities: [
            {
                id: 306,
                type: "meeting",
                subject: "Nghiệm thu triển khai giai đoạn 1 tại Viettel Post",
                text: "Hai bên ký kết biên bản nghiệm thu hạ tầng hệ thống và bàn giao tài liệu kỹ thuật.",
                author: "Lê Hoàng Nam",
                time: new Date(Date.now() - 1000 * 60 * 60 * 24)
            }
        ],
        attachments: [
            {
                id: 405,
                fileName: "Bien_ban_nghiem_thu_Cloud_Viettel_2026.pdf",
                fileType: "Biên bản nghiệm thu",
                fileSize: "2.1 MB",
                uploader: "Lê Hoàng Nam",
                uploadedAt: "25/09/2026",
                icon: "📄"
            }
        ]
    }
};

/* =========================================================
   2. IN-MEMORY DATA STORE & CLIENT ADAPTER (CRM-63)
   ========================================================= */
let IN_MEMORY_360_STORE = (() => {
    try {
        const raw = localStorage.getItem(STORAGE_KEY_360);
        if (raw) {
            const parsed = JSON.parse(raw);
            if (parsed && typeof parsed === "object") return parsed;
        }
    } catch (_) {}
    return JSON.parse(JSON.stringify(DEFAULT_SEED_360_DATA));
})();

function getAllStored360Data() {
    return IN_MEMORY_360_STORE;
}

function saveAllStored360Data(data) {
    IN_MEMORY_360_STORE = data;
    try {
        localStorage.setItem(STORAGE_KEY_360, JSON.stringify(data));
    } catch (_) {}
}

function getStoredCustomer360(cId) {
    const all = getAllStored360Data();
    if (all[cId]) return all[cId];

    // If customer not yet in 360 store, try finding in CRM_CUSTOMERS_DATA
    let baseCustomer = null;
    try {
        const rawCust = localStorage.getItem(STORAGE_KEY_CUSTOMERS);
        if (rawCust) {
            const list = JSON.parse(rawCust);
            baseCustomer = list.find(c => Number(c.id) === Number(cId));
        }
    } catch (_) { }

    // Fallback template for any new/unseeded customer
    const newRecord = {
        id: cId,
        companyName: baseCustomer?.companyName || baseCustomer?.name || `Khách hàng Doanh nghiệp #${cId}`,
        taxCode: baseCustomer?.taxCode || "0109988776",
        industry: baseCustomer?.industry || "Thương mại & Dịch vụ",
        companySize: baseCustomer?.companySize || "50 - 200 nhân sự",
        phone: baseCustomer?.phone || "0243.999.8888",
        website: baseCustomer?.website || "https://doanhnghiep.vn",
        address: baseCustomer?.address || "Hà Nội, Việt Nam",
        ownerName: baseCustomer?.ownerName || "Nông Quang Tiệp (Trưởng phòng KD)",
        ownerEmail: "tiepnq@crm.vn",
        ownerPhone: "0912.888.999",
        status: baseCustomer?.status || "CHINH_THUC",
        lifecycleStatus: "Chính thức",
        annualRevenue: "25 tỷ VNĐ",
        healthScore: "Tốt (90/100)",
        contacts: [
            {
                id: Date.now(),
                fullName: baseCustomer?.contactName || "Nguyễn Văn Đại diện",
                title: "Giám đốc điều hành",
                email: baseCustomer?.email || "contact@doanhnghiep.vn",
                phone: baseCustomer?.phone || "0912.333.444",
                buyingRole: "DECISION_MAKER",
                isPrimary: true
            }
        ],
        deals: [
            {
                id: Date.now() + 1,
                name: `Gói Triển khai Dịch vụ B2B - KH #${cId}`,
                amount: 350000000,
                stage: "Đề xuất giải pháp",
                probability: 60,
                status: "OPEN",
                expectedCloseDate: "2026-11-30",
                owner: "Nông Quang Tiệp"
            }
        ],
        activities: [
            {
                id: Date.now() + 2,
                type: "call",
                subject: "Liên hệ giới thiệu giải pháp",
                text: "Đã liên hệ trao đổi sơ bộ nhu cầu số hóa quy trình kinh doanh và gửi tài liệu giới thiệu giải pháp.",
                author: "Nông Quang Tiệp",
                time: new Date()
            }
        ],
        attachments: [
            {
                id: Date.now() + 3,
                fileName: "Ho_so_gioi_thieu_nang_luc.pdf",
                fileType: "Hồ sơ năng lực",
                fileSize: "1.5 MB",
                uploader: "Nông Quang Tiệp",
                uploadedAt: new Date().toLocaleDateString("vi-VN"),
                icon: "📄"
            }
        ]
    };

    all[cId] = newRecord;
    saveAllStored360Data(all);
    return newRecord;
}

function updateActiveRecord(mutatorFn) {
    if (!active360Record) return;
    mutatorFn(active360Record);
    const all = getAllStored360Data();
    all[active360Record.id] = active360Record;
    saveAllStored360Data(all);
}

/* =========================================================
   3. API CLIENT (GRACEFUL INTEGRATION)
   ========================================================= */
async function fetchApi(path, options = {}) {
    try {
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
        if (!response.ok) return null;
        const res = await response.json();
        return res?.data || null;
    } catch (_) {
        return null;
    }
}

/* =========================================================
   4. INITIALIZATION & DATA ORCHESTRATION
   ========================================================= */
async function initCustomer360() {
    // 1. Load from LocalStorage / Seed store first
    active360Record = getStoredCustomer360(currentCustomerId);

    // 2. IMMEDIATE SYNCHRONOUS RENDER (Cache-First) - Instant 0ms display
    renderHeaderAndProfile();
    renderFinancialKpis();
    renderContactsList();
    renderDeals();
    renderTimeline();
    renderAttachments();
    renderCustomerChurnRisk360();
    renderCustomerTickets360();

    // 3. Bind UI interaction listeners immediately
    bindTabEvents();
    bindComposerEvents();
    bindModalEvents();
    bindQuickActionEvents();

    // 4. Background asynchronous sync with Backend API if online (Non-blocking)
    try {
        const apiData = await fetchApi(`/api/customers/${currentCustomerId}/360`);
        if (apiData && apiData.customer) {
            // Synchronize backend data into our 360 record
            active360Record.companyName = apiData.customer.companyName || apiData.customer.name || active360Record.companyName;
            active360Record.taxCode = apiData.customer.taxCode || active360Record.taxCode;
            active360Record.industry = apiData.customer.industry || active360Record.industry;
            active360Record.phone = apiData.customer.phone || active360Record.phone;
            active360Record.website = apiData.customer.website || active360Record.website;
            active360Record.address = apiData.customer.address || active360Record.address;

            if (Array.isArray(apiData.contacts) && apiData.contacts.length > 0) {
                active360Record.contacts = apiData.contacts.map(c => ({
                    id: c.id,
                    fullName: c.fullName || c.name,
                    title: c.title || "Người liên hệ",
                    email: c.email || "—",
                    phone: c.phone || "—",
                    buyingRole: c.buyingRole || "INFLUENCER",
                    isPrimary: !!c.isPrimary
                }));
            }
            updateActiveRecord(() => { });
            // Re-render updated information
            renderHeaderAndProfile();
            renderContactsList();
        }
    } catch (e) {
        console.warn("Backend 360 API not reached, using persistent LocalStorage record:", e);
    }
}

/* =========================================================
   5. RENDER HEADER, METRICS & COMPANY PROFILE (AC1)
   ========================================================= */
function renderHeaderAndProfile() {
    if (!active360Record) return;

    // Header Top Overview
    writeText("headerCompanyTitle", active360Record.companyName);
    writeText("headCustomerId", `#KH-${active360Record.id}`);
    writeText("headCompanyTax", active360Record.taxCode || "—");
    writeText("headCompanyOwner", active360Record.ownerName || "Chưa phân công");

    // Lifecycle badge
    const badgeEl = document.getElementById("company360LifecycleBadge");
    const statusBadgeEl = document.getElementById("company360StatusBadge");
    const statusText = active360Record.lifecycleStatus || "Chính thức";
    if (badgeEl) badgeEl.textContent = statusText.toUpperCase();
    if (statusBadgeEl) statusBadgeEl.textContent = statusText;

    // Left Column Detail list (Preserving all original IDs!)
    writeText("company360Name", active360Record.companyName);
    writeText("company360Tax", active360Record.taxCode || "—");
    writeText("company360Industry", active360Record.industry || "—");
    writeText("company360Size", active360Record.companySize || "—");
    writeText("company360Phone", active360Record.phone || "—");
    writeText("company360Website", active360Record.website || "—");
    writeText("company360Address", active360Record.address || "—");
    writeText("company360Owner", active360Record.ownerName || "—");
}

function renderFinancialKpis() {
    if (!active360Record) return;

    const deals = active360Record.deals || [];

    // 1. Total Won Revenue (Giá trị đã ký)
    const wonDeals = deals.filter(d => d.status === "CLOSED_WON");
    const totalWonRevenue = wonDeals.reduce((sum, d) => sum + (Number(d.amount) || 0), 0);
    writeText("kpiSignedRevenue", formatMoney(totalWonRevenue));
    writeText("kpiSignedSubtext", `${wonDeals.length} hợp đồng đã chốt thành công`);

    // 2. Open Pipeline Value (Giá trị cơ hội đang mở)
    const openDeals = deals.filter(d => d.status === "OPEN" || !d.status);
    const totalOpenPipeline = openDeals.reduce((sum, d) => sum + (Number(d.amount) || 0), 0);
    writeText("kpiOpenPipeline", formatMoney(totalOpenPipeline));
    writeText("kpiOpenSubtext", `${openDeals.length} thương vụ đang theo đuổi`);

    // 3. Deals Counts & Win Rate
    const closedDeals = deals.filter(d => d.status === "CLOSED_WON" || d.status === "CLOSED_LOST");
    const totalFinished = closedDeals.length;
    const winRate = totalFinished > 0 ? Math.round((wonDeals.length / totalFinished) * 100) : (wonDeals.length > 0 ? 100 : 0);

    writeText("kpiDealCounts", `${openDeals.length} Mở · ${totalFinished} Đã đóng`);
    writeText("kpiWinRate", `Tỷ lệ thắng (Win Rate): ${winRate}%`);

    // 4. Account Health
    writeText("kpiAccountHealth", active360Record.healthScore || "Tốt (95/100)");
}

/* =========================================================
   6. RENDER CONTACTS & BUYING ROLES (CRM-62 INTEGRATION)
   ========================================================= */
function renderContactsList() {
    const container = document.getElementById("contactsPanel");
    const badge = document.getElementById("contactCountBadge");
    if (!container || !active360Record) return;

    const contacts = active360Record.contacts || [];
    if (badge) badge.textContent = String(contacts.length);

    if (contacts.length === 0) {
        container.innerHTML = `<div class="empty-small">Chưa có người liên hệ nào được lưu.</div>`;
        return;
    }

    // Sort: Primary contact first
    const sorted = [...contacts].sort((a, b) => (b.isPrimary ? 1 : 0) - (a.isPrimary ? 1 : 0));

    container.innerHTML = sorted.map(c => {
        const roleLabel = getBuyingRoleLabel(c.buyingRole);
        const roleClass = getBuyingRoleClass(c.buyingRole);

        return `
            <div class="contact-card-item">
                <div class="contact-card-header">
                    <span class="contact-name">${escapeHtml(c.fullName)}</span>
                    <div class="contact-badges-row">
                        ${c.isPrimary ? `<span class="badge-primary-star">★ Đầu mối chính</span>` : ""}
                        <span class="buying-role-pill ${roleClass}">${roleLabel}</span>
                    </div>
                </div>
                <div class="contact-meta-row">
                    <span style="font-weight:600; color:var(--crm-text);">${escapeHtml(c.title || "Chức danh")}</span>
                    <span>📧 <a href="mailto:${escapeHtml(c.email)}" class="contact-quick-link">${escapeHtml(c.email)}</a></span>
                    <span>☎ <a href="tel:${escapeHtml(c.phone)}" class="contact-quick-link">${escapeHtml(c.phone)}</a></span>
                </div>
                <div class="contact-action-bar">
                    <span style="font-size:11px; color:var(--crm-muted);">${c.isPrimary ? "Đầu mối hiện tại" : "Thành viên"}</span>
                    ${!c.isPrimary ? `
                        <button type="button" class="btn-set-primary" onclick="setPrimaryContact(${c.id})">
                            Đặt làm đầu mối chính
                        </button>
                    ` : ""}
                </div>
            </div>
        `;
    }).join("");
}

window.setPrimaryContact = function (contactId) {
    updateActiveRecord(rec => {
        (rec.contacts || []).forEach(c => {
            c.isPrimary = (Number(c.id) === Number(contactId));
        });
    });
    renderContactsList();
};

function getBuyingRoleLabel(role) {
    switch (role) {
        case "DECISION_MAKER": return "Người quyết định";
        case "INFLUENCER": return "Người ảnh hưởng";
        case "END_USER": return "Người dùng cuối";
        case "BLOCKER": return "Người cản trở";
        default: return "Người ảnh hưởng";
    }
}

function getBuyingRoleClass(role) {
    switch (role) {
        case "DECISION_MAKER": return "role-decision-maker";
        case "INFLUENCER": return "role-influencer";
        case "END_USER": return "role-end-user";
        case "BLOCKER": return "role-blocker";
        default: return "role-influencer";
    }
}

/* =========================================================
   7. RENDER DEALS (OPEN VS CLOSED - AC2)
   ========================================================= */
function renderDeals() {
    const openListEl = document.getElementById("openDealsList");
    const closedListEl = document.getElementById("closedDealsList");
    const tabBadge = document.getElementById("tabDealsBadge");
    if (!openListEl || !closedListEl || !active360Record) return;

    const deals = active360Record.deals || [];
    if (tabBadge) tabBadge.textContent = String(deals.length);

    const openDeals = deals.filter(d => d.status === "OPEN" || !d.status);
    const closedDeals = deals.filter(d => d.status === "CLOSED_WON" || d.status === "CLOSED_LOST");

    const totalOpen = openDeals.reduce((sum, d) => sum + (Number(d.amount) || 0), 0);
    const totalClosedWon = deals.filter(d => d.status === "CLOSED_WON").reduce((sum, d) => sum + (Number(d.amount) || 0), 0);

    writeText("dealsOpenTotalAmount", formatMoney(totalOpen));
    writeText("dealsClosedTotalAmount", formatMoney(totalClosedWon));
    writeText("dealsSummarySubtext", `${openDeals.length} cơ hội mở · ${closedDeals.length} đã đóng`);

    // 1. Render Open Deals
    if (openDeals.length === 0) {
        openListEl.innerHTML = `<div class="empty-small">Chưa có cơ hội bán hàng nào đang mở.</div>`;
    } else {
        openListEl.innerHTML = openDeals.map(d => {
            const prob = Number(d.probability) || 50;
            return `
                <div class="deal-item-row">
                    <div class="deal-item-top">
                        <span class="deal-item-name">${escapeHtml(d.name)}</span>
                        <span class="deal-item-amount">${formatMoney(d.amount)}</span>
                    </div>
                    <div class="deal-progress-bar-bg" title="Xác suất thành công: ${prob}%">
                        <div class="deal-progress-bar-fill" style="width: ${prob}%;"></div>
                    </div>
                    <div class="deal-item-bottom">
                        <span class="deal-stage-pill">📌 ${escapeHtml(d.stage || "Đang xử lý")} (${prob}%)</span>
                        <span>Dự kiến: ${escapeHtml(d.expectedCloseDate || "Chưa xác định")}</span>
                    </div>
                </div>
            `;
        }).join("");
    }

    // 2. Render Closed Deals
    if (closedDeals.length === 0) {
        closedListEl.innerHTML = `<div class="empty-small">Chưa có cơ hội nào đã đóng.</div>`;
    } else {
        closedListEl.innerHTML = closedDeals.map(d => {
            const isWon = d.status === "CLOSED_WON";
            return `
                <div class="deal-item-row" style="background: ${isWon ? "rgba(22,163,74,0.03)" : "rgba(220,38,38,0.03)"};">
                    <div class="deal-item-top">
                        <span class="deal-item-name">${escapeHtml(d.name)}</span>
                        <span class="deal-item-amount" style="color:${isWon ? "var(--crm-success)" : "var(--crm-muted)"};">
                            ${formatMoney(d.amount)}
                        </span>
                    </div>
                    <div class="deal-item-bottom">
                        <span class="${isWon ? "deal-won-badge" : "deal-lost-badge"}">
                            ${isWon ? "✓ THÀNH CÔNG (WON)" : "✗ THẤT BẠI (LOST)"}
                        </span>
                        <span>Đóng ngày: ${escapeHtml(d.closeDate || "Gần đây")}</span>
                    </div>
                    ${d.winReason || d.lostReason ? `
                        <div style="font-size:11.5px; color:var(--crm-muted); margin-top:2px; font-style:italic;">
                            Lý do: ${escapeHtml(d.winReason || d.lostReason)}
                        </div>
                    ` : ""}
                </div>
            `;
        }).join("");
    }
}

/* =========================================================
   8. ACTIVITY TIMELINE & 500+ PERFORMANCE OPTIMIZATION (AC3)
   ========================================================= */
function renderTimeline() {
    const startTime = performance.now();

    const listEl = document.getElementById("timelineList");
    const emptyEl = document.getElementById("timelineEmpty");
    const tabBadge = document.getElementById("tabTimelineBadge");
    const loadMoreContainer = document.getElementById("timelineLoadMoreContainer");
    const loadMoreProgress = document.getElementById("loadMoreProgress");
    if (!listEl || !active360Record) return;

    const allActivities = active360Record.activities || [];
    if (tabBadge) tabBadge.textContent = String(allActivities.length);

    // Apply Filter
    let filtered = allActivities;
    if (currentTimelineFilter !== "all") {
        filtered = allActivities.filter(a => a.type === currentTimelineFilter);
    }

    // Sort descending by time
    filtered.sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime());

    // Lazy Batching: Only render up to timelineRenderLimit items for DOM speed!
    const itemsToRender = filtered.slice(0, timelineRenderLimit);

    listEl.querySelectorAll(".timeline-item").forEach(el => el.remove());

    if (itemsToRender.length === 0) {
        if (emptyEl) emptyEl.style.display = "block";
        if (loadMoreContainer) loadMoreContainer.style.display = "none";
    } else {
        if (emptyEl) emptyEl.style.display = "none";

        const fragment = document.createDocumentFragment();
        itemsToRender.forEach(item => {
            const el = document.createElement("article");
            el.className = `timeline-item item-${item.type}`;
            const timeObj = new Date(item.time);
            const timeFormatted = formatTimelineTime(timeObj);

            el.innerHTML = `
                <div class="timeline-item-head">
                    <span class="timeline-type">${typeLabel(item.type)} · <strong>${escapeHtml(item.subject || "Hoạt động")}</strong></span>
                    <time class="timeline-time" title="${timeObj.toLocaleString("vi-VN")}">${timeFormatted}</time>
                </div>
                <div class="timeline-author">Người thực hiện: ${escapeHtml(item.author || "Nông Quang Tiệp")}</div>
                <div class="timeline-text">${escapeHtml(item.text)}</div>
            `;
            fragment.appendChild(el);
        });
        listEl.appendChild(fragment);

        // Manage Load More button
        if (loadMoreContainer) {
            if (filtered.length > timelineRenderLimit) {
                loadMoreContainer.style.display = "block";
                if (loadMoreProgress) {
                    loadMoreProgress.textContent = `${itemsToRender.length}/${filtered.length}`;
                }
            } else {
                loadMoreContainer.style.display = "none";
            }
        }
    }

    // Benchmark performance calculation
    const elapsedMs = (performance.now() - startTime).toFixed(1);
    const speedEl = document.getElementById("perfSpeedText");
    if (speedEl) {
        speedEl.textContent = `Đã tải ${itemsToRender.length} / ${allActivities.length} mục (${elapsedMs}ms)`;
    }
}

function formatTimelineTime(d) {
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return "Vừa xong";
    if (diffMins < 60) return `${diffMins} phút trước`;
    if (diffHours < 24) return `${diffHours} giờ trước`;
    if (diffDays === 1) return "Hôm qua " + d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
    if (diffDays < 7) return `${diffDays} ngày trước`;
    return d.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
}

/* =========================================================
   9. RENDER ATTACHMENTS (AC2)
   ========================================================= */
function renderAttachments() {
    const tbody = document.getElementById("attachmentsTableBody");
    const countSummary = document.getElementById("attachmentsCountText");
    const tabBadge = document.getElementById("tabAttachmentsBadge");
    if (!tbody || !active360Record) return;

    const attachments = active360Record.attachments || [];
    if (countSummary) countSummary.textContent = `${attachments.length} tệp đính kèm`;
    if (tabBadge) tabBadge.textContent = String(attachments.length);

    if (attachments.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-muted" style="padding:28px;">
                    Chưa có tài liệu đính kèm nào cho khách hàng này.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = attachments.map(a => `
        <tr>
            <td>
                <div class="file-name-cell">
                    <span class="file-icon">${a.icon || "📄"}</span>
                    <span>${escapeHtml(a.fileName)}</span>
                </div>
            </td>
            <td><span class="text-muted">${escapeHtml(a.fileType || "Tài liệu")}</span></td>
            <td><strong>${escapeHtml(a.fileSize || "1.0 MB")}</strong></td>
            <td>${escapeHtml(a.uploader || "Nông Quang Tiệp")}</td>
            <td>${escapeHtml(a.uploadedAt || "Gần đây")}</td>
            <td style="text-align:right;">
                <button type="button" class="file-action-btn" onclick="downloadAttachmentDemo('${escapeHtml(a.fileName)}')">
                    Tải xuống
                </button>
                <button type="button" class="file-action-btn file-delete-btn" onclick="deleteAttachment(${a.id})">
                    Xóa
                </button>
            </td>
        </tr>
    `).join("");
}

window.downloadAttachmentDemo = function (filename) {
    alert(`[Demo CRM-63] Đang tải xuống tệp: "${filename}".`);
};

window.deleteAttachment = function (attId) {
    if (!confirm("Bạn có chắc chắn muốn xóa tài liệu này?")) return;
    updateActiveRecord(rec => {
        rec.attachments = (rec.attachments || []).filter(a => Number(a.id) !== Number(attId));
    });
    renderAttachments();
};

/* =========================================================
   10. EVENT HANDLERS & MODAL MANAGEMENT
   ========================================================= */
function bindTabEvents() {
    document.querySelectorAll(".right-tab").forEach(tab => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".right-tab").forEach(t => t.classList.remove("active"));
            tab.classList.add("active");

            const target = tab.dataset.tab;
            const pTimeline = document.getElementById("timelinePanel");
            const pOpportunities = document.getElementById("opportunitiesPanel");
            const pAttachments = document.getElementById("attachmentsPanel");

            if (pTimeline) pTimeline.hidden = (target !== "timeline");
            if (pOpportunities) pOpportunities.hidden = (target !== "opportunities");
            if (pAttachments) pAttachments.hidden = (target !== "attachments");
        });
    });

    // Timeline Filter Buttons
    document.querySelectorAll(".filter-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".filter-btn").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            currentTimelineFilter = btn.dataset.activityFilter || "all";
            timelineRenderLimit = 20; // reset limit on filter change
            renderTimeline();
        });
    });

    // Timeline Load More
    document.getElementById("btnLoadMoreActivities")?.addEventListener("click", () => {
        timelineRenderLimit += 20;
        renderTimeline();
    });

    // 500+ Items Simulation (AC3 Demo)
    document.getElementById("btnSimulate500")?.addEventListener("click", () => {
        if (!active360Record) return;
        const types = ["call", "email", "meeting", "note"];
        const subjects = [
            "Trao đổi kỹ thuật", "Gửi tài liệu giải pháp", "Họp rà soát tiến độ",
            "Ghi chú bổ sung", "Demo module báo cáo", "Chốt điều khoản bảo trì"
        ];
        const newSimulated = [];
        const baseNow = Date.now();

        for (let i = 1; i <= 500; i++) {
            const randType = types[i % types.length];
            const randSub = subjects[i % subjects.length];
            newSimulated.push({
                id: baseNow + i,
                type: randType,
                subject: `${randSub} #${i}`,
                text: `Dòng nhật ký kiểm thử tải trang mượt mà #${i} cho khách hàng ${active360Record.companyName}.`,
                author: i % 2 === 0 ? "Nông Quang Tiệp" : "Nguyễn Văn An",
                time: new Date(baseNow - i * 1000 * 60 * 30) // cách nhau 30 phút
            });
        }

        updateActiveRecord(rec => {
            rec.activities = [...newSimulated, ...(rec.activities || [])];
        });

        timelineRenderLimit = 20;
        renderTimeline();
        alert("Đã tạo thành công 500+ hoạt động mô phỏng! Kiểm tra thời gian render mượt mà ngay trên thanh trạng thái.");
    });
}

function bindComposerEvents() {
    const form = document.getElementById("activityComposer");
    const noteEl = document.getElementById("activityNote");
    const typeEl = document.getElementById("activityType");

    form?.addEventListener("submit", async event => {
        event.preventDefault();
        const text = noteEl.value.trim();
        if (!text) {
            noteEl.focus();
            return;
        }

        const type = typeEl.value || "note";
        const subject = text.length > 50 ? text.substring(0, 47) + "..." : text;

        // Try POSTing to backend API if available
        fetchApi("/api/activities", {
            method: "POST",
            body: JSON.stringify({
                customerId: currentCustomerId,
                type: type.toUpperCase(),
                subject: subject,
                description: text,
                status: "COMPLETED"
            })
        }).catch(() => { });

        // Save to LocalStorage store
        updateActiveRecord(rec => {
            rec.activities = rec.activities || [];
            rec.activities.unshift({
                id: Date.now(),
                type: type,
                subject: subject,
                text: text,
                author: "Nông Quang Tiệp",
                time: new Date()
            });
        });

        noteEl.value = "";
        timelineRenderLimit = 20;
        renderTimeline();
    });
}

function bindModalEvents() {
    // 1. Company Edit Modal
    document.getElementById("editCompanyButton")?.addEventListener("click", openCompanyModal);
    document.getElementById("closeCompanyModal")?.addEventListener("click", closeCompanyModal);
    document.getElementById("cancelCompanyEdit")?.addEventListener("click", closeCompanyModal);
    document.getElementById("companyModalOverlay")?.addEventListener("click", closeCompanyModal);

    document.getElementById("company360Form")?.addEventListener("submit", event => {
        event.preventDefault();
        const name = value("edit360Name");
        const tax = value("edit360Tax");
        const ind = value("edit360Industry");
        const size = value("edit360Size");
        const phone = value("edit360Phone");
        const website = value("edit360Website");
        const address = value("edit360Address");
        const status = value("edit360Status");

        updateActiveRecord(rec => {
            rec.companyName = name || rec.companyName;
            rec.taxCode = tax;
            rec.industry = ind;
            rec.companySize = size;
            rec.phone = phone;
            rec.website = website;
            rec.address = address;
            rec.status = status;
            rec.lifecycleStatus = status === "CHINH_THUC" ? "Chính thức" : (status === "TIEM_NANG" ? "Tiềm năng" : "Đang giao dịch");
        });

        // HTTP Fetch API update
        fetchApi(`/api/customers/${active360Record.id}`, {
            method: "PUT",
            body: {
                companyName: name,
                taxCode: tax,
                industry: ind,
                companySize: size,
                phone: phone,
                website: website,
                address: address,
                status: status
            }
        }).catch(() => {});

        renderHeaderAndProfile();
        closeCompanyModal();
    });

    // 2. Contact Modal
    document.getElementById("addContact")?.addEventListener("click", () => openModal("contactModal", "contactModalOverlay"));
    document.getElementById("closeContactModal")?.addEventListener("click", () => closeModal("contactModal", "contactModalOverlay"));
    document.getElementById("cancelContactModal")?.addEventListener("click", () => closeModal("contactModal", "contactModalOverlay"));
    document.getElementById("contactModalOverlay")?.addEventListener("click", () => closeModal("contactModal", "contactModalOverlay"));

    document.getElementById("contactForm")?.addEventListener("submit", event => {
        event.preventDefault();
        const fullName = value("contactFullName");
        const title = value("contactTitle");
        const email = value("contactEmail");
        const phone = value("contactPhone");
        const buyingRole = value("contactBuyingRole");
        const isPrimary = document.getElementById("contactIsPrimary")?.checked || false;

        updateActiveRecord(rec => {
            rec.contacts = rec.contacts || [];
            if (isPrimary) {
                rec.contacts.forEach(c => c.isPrimary = false);
            }
            rec.contacts.push({
                id: Date.now(),
                fullName,
                title,
                email,
                phone,
                buyingRole,
                isPrimary
            });
        });

        // HTTP Fetch API contact creation
        fetchApi(`/api/customers/${active360Record.id}/contacts`, {
            method: "POST",
            body: {
                fullName,
                title,
                email,
                phone,
                buyingRole,
                isPrimary
            }
        }).catch(() => {});

        renderContactsList();
        closeModal("contactModal", "contactModalOverlay");
        document.getElementById("contactForm").reset();
    });

    // 3. Deal Modal
    document.getElementById("btnAddOpportunity")?.addEventListener("click", () => openModal("dealModal", "dealModalOverlay"));
    document.getElementById("closeDealModal")?.addEventListener("click", () => closeModal("dealModal", "dealModalOverlay"));
    document.getElementById("cancelDealModal")?.addEventListener("click", () => closeModal("dealModal", "dealModalOverlay"));
    document.getElementById("dealModalOverlay")?.addEventListener("click", () => closeModal("dealModal", "dealModalOverlay"));

    document.getElementById("dealForm")?.addEventListener("submit", event => {
        event.preventDefault();
        const name = value("dealNameInput");
        const amount = Number(value("dealAmountInput")) || 0;
        const probability = Number(value("dealProbInput")) || 50;
        const stage = value("dealStageSelect");
        const closeDate = value("dealCloseDateInput");
        const isWon = stage.includes("Won");
        const isLost = stage.includes("Lost");

        updateActiveRecord(rec => {
            rec.deals = rec.deals || [];
            rec.deals.push({
                id: Date.now(),
                name,
                amount,
                probability,
                stage,
                status: isWon ? "CLOSED_WON" : (isLost ? "CLOSED_LOST" : "OPEN"),
                expectedCloseDate: closeDate,
                closeDate: (isWon || isLost) ? closeDate : null,
                owner: "Nông Quang Tiệp"
            });
        });

        // HTTP Fetch API deal creation
        fetchApi(`/api/customers/${active360Record.id}/deals`, {
            method: "POST",
            body: {
                name,
                amount,
                probability,
                stage,
                expectedCloseDate: closeDate
            }
        }).catch(() => {});

        renderDeals();
        renderFinancialKpis();
        closeModal("dealModal", "dealModalOverlay");
        document.getElementById("dealForm").reset();
    });

    // 4. Attachment Modal
    document.getElementById("btnUploadAttachment")?.addEventListener("click", () => openModal("attachmentModal", "attachmentModalOverlay"));
    document.getElementById("closeAttachmentModal")?.addEventListener("click", () => closeModal("attachmentModal", "attachmentModalOverlay"));
    document.getElementById("cancelAttachmentModal")?.addEventListener("click", () => closeModal("attachmentModal", "attachmentModalOverlay"));
    document.getElementById("attachmentModalOverlay")?.addEventListener("click", () => closeModal("attachmentModal", "attachmentModalOverlay"));

    document.getElementById("attachmentForm")?.addEventListener("submit", event => {
        event.preventDefault();
        const fileName = value("attachmentNameInput");
        const fileType = value("attachmentTypeSelect");
        const fileSize = value("attachmentSizeInput") || "2.0 MB";

        updateActiveRecord(rec => {
            rec.attachments = rec.attachments || [];
            rec.attachments.unshift({
                id: Date.now(),
                fileName,
                fileType,
                fileSize,
                uploader: "Nông Quang Tiệp",
                uploadedAt: new Date().toLocaleDateString("vi-VN"),
                icon: fileName.endsWith(".pdf") ? "📄" : (fileName.endsWith(".xlsx") ? "📊" : "📑")
            });
        });

        renderAttachments();
        closeModal("attachmentModal", "attachmentModalOverlay");
        document.getElementById("attachmentForm").reset();
    });
}

function bindQuickActionEvents() {
    document.querySelectorAll("[data-quick]").forEach(btn => {
        btn.addEventListener("click", () => {
            const type = btn.dataset.quick;
            const selectEl = document.getElementById("activityType");
            const noteEl = document.getElementById("activityNote");
            if (selectEl) selectEl.value = type;
            if (noteEl) {
                noteEl.focus();
                noteEl.scrollIntoView({ behavior: "smooth", block: "center" });
            }
        });
    });

    document.getElementById("btnQuickNewDeal")?.addEventListener("click", () => {
        openModal("dealModal", "dealModalOverlay");
    });
}

/* =========================================================
   11. CRM-68: CHURN RISK & SALES ALERT BANNER (PRESERVED)
   ========================================================= */
function getCustomerRecord(cId) {
    if (active360Record && Number(active360Record.id) === Number(cId)) return active360Record;
    return getStoredCustomer360(cId);
}

function renderCustomerChurnRisk360() {
    const bannerContainer = document.getElementById("customer360ChurnBanner");
    if (!bannerContainer) return;

    let churnInfo = null;
    if (window.SupportTicketsManager && typeof window.SupportTicketsManager.getChurnRiskStatus === "function") {
        churnInfo = window.SupportTicketsManager.getChurnRiskStatus(Number(currentCustomerId));
    } else {
        try {
            const rawChurn = localStorage.getItem("CRM_CHURN_RISK_DATA");
            const churnFlags = rawChurn ? JSON.parse(rawChurn) : {};
            const manualFlag = churnFlags[Number(currentCustomerId)];

            const rawTickets = localStorage.getItem("CRM_SUPPORT_TICKETS_DATA");
            const allTickets = rawTickets ? JSON.parse(rawTickets) : [];
            const custTickets = allTickets.filter(t => Number(t.customerId) === Number(currentCustomerId) && t.status !== "CLOSED" && t.status !== "RESOLVED");

            const isManual = !!manualFlag;
            const hasOverdueUrgent = custTickets.some(t => t.priority === "URGENT" || t.priority === "HIGH");
            const hasMultipleOpen = custTickets.length >= 2;
            const isRisk = isManual || hasOverdueUrgent || hasMultipleOpen;

            const reasons = [];
            if (isManual) reasons.push(manualFlag.manualReason || "CSKH gắn cờ thủ công rủi ro rời bỏ.");
            if (hasOverdueUrgent) reasons.push("Có yêu cầu mức độ Khẩn cấp/Cao chưa giải quyết dứt điểm.");
            if (hasMultipleOpen) reasons.push(`Có ${custTickets.length} phiếu khiếu nại đang tồn đọng mở.`);

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
    const customer = getCustomerRecord(currentCustomerId);
    const salesRepName = customer?.ownerName || "Nông Quang Tiệp (Trưởng phòng KD)";
    const salesRepEmail = customer?.ownerEmail || "tiepnq@crm.vn";
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
    if (!listEl) return;

    let tickets = [];
    if (window.SupportTicketsManager && typeof window.SupportTicketsManager.getTicketsByCustomer === "function") {
        tickets = window.SupportTicketsManager.getTicketsByCustomer(Number(currentCustomerId));
    } else {
        try {
            const raw = localStorage.getItem("CRM_SUPPORT_TICKETS_DATA");
            if (raw) {
                const parsed = JSON.parse(raw);
                tickets = parsed.filter(t => Number(t.customerId) === Number(currentCustomerId));
            }
        } catch (_) { }
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

        return `
            <div style="padding:10px; border-bottom:1px solid var(--crm-border); display:flex; flex-direction:column; gap:4px;">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <a href="support-tickets.html" style="font-weight:700; font-size:12px; color:var(--crm-primary); text-decoration:none;">
                        ${escapeHtml(t.ticketCode)}
                    </a>
                    <span class="priority-badge ${priorityClass}" style="font-size:10px; padding:1px 6px;">
                        ${priorityLabel}
                    </span>
                </div>
                <div style="font-size:12.5px; font-weight:600; color:var(--crm-text); line-height:1.35;">
                    ${escapeHtml(t.title)}
                </div>
                <div style="display:flex; justify-content:space-between; align-items:center; font-size:11px; color:var(--crm-muted);">
                    <span>${statusLabel}</span>
                    <span>👤 ${escapeHtml(t.assigneeName || "CSKH")}</span>
                </div>
            </div>
        `;
    }).join("");
}

/* =========================================================
   12. HELPER UTILITIES
   ========================================================= */
function openCompanyModal() {
    if (!active360Record) return;
    setValue("edit360Name", active360Record.companyName);
    setValue("edit360Tax", active360Record.taxCode);
    setValue("edit360Industry", active360Record.industry);
    setValue("edit360Size", active360Record.companySize || "50 - 200 nhân sự");
    setValue("edit360Phone", active360Record.phone);
    setValue("edit360Website", active360Record.website);
    setValue("edit360Address", active360Record.address);
    setValue("edit360Status", active360Record.status || "CHINH_THUC");

    openModal("companyModal", "companyModalOverlay");
}

function closeCompanyModal() {
    closeModal("companyModal", "companyModalOverlay");
}

function openModal(modalId, overlayId) {
    document.getElementById(modalId)?.classList.add("open");
    document.getElementById(overlayId)?.classList.add("open");
}

function closeModal(modalId, overlayId) {
    document.getElementById(modalId)?.classList.remove("open");
    document.getElementById(overlayId)?.classList.remove("open");
}

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

function setValue(id, val) {
    const el = document.getElementById(id);
    if (el) el.value = val || "";
}

function writeText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val || "—";
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

// Global escape key listener for all modals
document.addEventListener("keydown", event => {
    if (event.key === "Escape") {
        document.querySelectorAll(".company-modal.open").forEach(m => m.classList.remove("open"));
        document.querySelectorAll(".modal-overlay.open").forEach(o => o.classList.remove("open"));
    }
});

// Boot customer 360 controller
if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initCustomer360);
} else {
    initCustomer360();
}