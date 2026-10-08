/**
 * Customer 360 Full Interactive Engine (S3-03 / CRM-63)
 * Author: Hoàng Văn Thắng (Frontend Lead)
 * 
 * Includes:
 * 1. Customer360API: Client-side mock / REST integration
 * 2. Parent Company Dropdown & Subsidiaries Management
 * 3. Group Total Value Card (Hệ sinh thái & tổng giá trị tập đoàn)
 * 4. Company Hierarchy Visual Tree on Customer 360
 * 5. Contacts (Người liên hệ), Opportunities (Mở & Đóng), Timeline & Attachments
 */

// ==========================================
// 1. DỮ LIỆU MOCK & API CUSTOMER 360
// ==========================================
const Customer360DB = {
    'CUST-001': {
        id: 'CUST-001',
        code: 'CUST-2026-089',
        name: 'Tập đoàn Công nghệ VNG',
        shortName: 'VNG',
        avatar: 'VNG',
        badge: 'Khách hàng VIP',
        industry: 'Công nghệ & Game',
        employees: '4.500 Nhân sự',
        owner: 'Hoàng Văn Thắng',
        phone: '028 3962 3888',
        email: 'contact@vng.com.vn',
        taxCode: '0303538466',
        address: 'Z06 Đường số 13, KCX Tân Thuận, Quận 7, TP. HCM',
        website: 'https://vng.com.vn',
        parentCompanyId: 'NONE', // Holding Corp
        parentCompanyName: '-- Độc lập / Là công ty Mẹ (Holding) --',
        revenue: 1250000000,
        healthScore: 95,
        subsidiaries: [
            {
                id: 'CUST-002',
                code: 'CUST-2026-090',
                name: 'VNG Cloud Solutions',
                shortName: 'VNG CLOUD',
                industry: 'Điện toán đám mây & Hạ tầng',
                representative: 'Trần Minh Quân',
                revenue: 1850000000,
                dealCount: 6,
                contractCount: 3
            },
            {
                id: 'CUST-003',
                code: 'CUST-2026-091',
                name: 'ZaloPay - Zion JSC',
                shortName: 'ZALOPAY',
                industry: 'Fintech & Ví điện tử',
                representative: 'Phạm Hương Ly',
                revenue: 1680000000,
                dealCount: 5,
                contractCount: 2
            },
            {
                id: 'CUST-006',
                code: 'CUST-2026-092',
                name: 'TrueData AI Lab',
                shortName: 'TRUEDATA',
                industry: 'Trí tuệ nhân tạo (AI/ML)',
                representative: 'Lê Hoàng Long',
                revenue: 700000000,
                dealCount: 3,
                contractCount: 1
            }
        ],
        contacts: [
            { id: 1, name: 'Nguyễn Văn An', role: 'Giám đốc Công nghệ (CTO)', phone: '0903 112 233', email: 'an.nv@vng.com.vn', decisionRole: 'Người quyết định chính (Decision Maker)', avatarColor: '#e0e7ff', textColor: '#4338ca', initials: 'AN' },
            { id: 2, name: 'Trần Lê Hương', role: 'Trưởng phòng Thu mua & Hợp đồng', phone: '0918 334 556', email: 'huong.tl@vng.com.vn', decisionRole: 'Ký hợp đồng & Thanh toán (Finance/Legal)', avatarColor: '#fef3c7', textColor: '#b45309', initials: 'LH' },
            { id: 3, name: 'Vũ Đức Thịnh', role: 'Kỹ sư trưởng Hệ thống (Lead Architect)', phone: '0977 445 667', email: 'thinh.vd@vng.com.vn', decisionRole: 'Người đánh giá kỹ thuật (Technical Evaluator)', avatarColor: '#dcfce7', textColor: '#15803d', initials: 'DT' }
        ],
        deals: [
            { id: 101, name: 'Nâng cấp Cloud Server Tier 4', stage: 'Đàm phán hợp đồng', status: 'OPEN', value: 450000000, probability: 80, closeDate: '15/10/2026', note: 'Đã hoàn tất vòng POC, đang chốt điều khoản SLA' },
            { id: 102, name: 'Gói bảo trì phần mềm 24/7', stage: 'Đề xuất giải pháp', status: 'OPEN', value: 120000000, probability: 60, closeDate: '25/10/2026', note: 'Chờ duyệt ngân sách quý 4' },
            { id: 103, name: 'Bản quyền bảo mật EndPoint', stage: 'Tư vấn ban đầu', status: 'OPEN', value: 85000000, probability: 40, closeDate: '30/11/2026', note: 'Đã demo giải pháp cho đội ngũ SecOps' },
            { id: 104, name: 'Triển khai C-CRM Enterprise Giai đoạn 1', stage: 'Closed Won', status: 'CLOSED_WON', value: 800000000, probability: 100, closeDate: '15/01/2026', note: 'Nghiệm thu thành công 100%, thanh toán đúng hạn' },
            { id: 105, name: 'Giải pháp tổng đài Call Center cũ', stage: 'Closed Lost', status: 'CLOSED_LOST', value: 150000000, probability: 0, closeDate: '20/03/2026', note: 'Thua đối thủ do đối thủ có tích hợp sẵn phần cứng viễn thông' }
        ],
        contracts: [
            { id: 'HD-2025/VNG-01', name: 'Hợp đồng dịch vụ hạ tầng mạng năm 2025-2026', value: 800000000, signedDate: '10/01/2025', expiryDate: '31/12/2026', status: 'Đang hiệu lực' },
            { id: 'HD-2026/VNG-02', name: 'Hợp đồng tư vấn chuyển đổi số quy trình', value: 450000000, signedDate: '01/06/2026', expiryDate: '30/06/2027', status: 'Đang hiệu lực' }
        ],
        timeline: [
            { type: 'meeting', title: 'Họp trao đổi nâng cấp Server Cloud', author: 'Hoàng Văn Thắng', time: 'Hôm nay, 14:30', body: 'Khách hàng đồng ý tiến hành thử nghiệm POC hệ thống cụm Server chuyên dụng trong 14 ngày.' },
            { type: 'call', title: 'Cuộc gọi chăm sóc định kỳ', author: 'Hoàng Văn Thắng', time: 'Hôm qua, 09:15', body: 'Liên hệ chị Hương để kiểm tra tình trạng sử dụng phần mềm. Phản hồi rất hài lòng.' },
            { type: 'email', title: 'Gửi báo giá đề xuất gói Cloud Tier 4', author: 'Hoàng Văn Thắng', time: '04/10/2026, 16:00', body: 'Đã gửi file proposal kèm chính sách chiết khấu 10% cho khách hàng VIP.' },
            { type: 'note', title: 'Ghi chú phê duyệt cấp tín dụng', author: 'Hoàng Văn Thắng', time: '28/09/2026, 11:20', body: 'Bộ phận tài chính duyệt hạn mức công nợ 60 ngày đối với tập đoàn VNG.' }
        ],
        attachments: [
            { id: 'ATT-1', name: 'Hop_dong_nguyen_tac_VNG_2026.pdf', type: 'pdf', size: '3.4 MB', uploader: 'Hoàng Văn Thắng', date: '15/01/2026' },
            { id: 'ATT-2', name: 'Bao_gia_giai_phap_Cloud_Tier4.docx', type: 'doc', size: '1.2 MB', uploader: 'Hoàng Văn Thắng', date: '04/10/2026' },
            { id: 'ATT-3', name: 'Bien_ban_nghiem_thu_Giai_doan_1.pdf', type: 'pdf', size: '2.8 MB', uploader: 'Hoàng Văn Thắng', date: '20/06/2026' },
            { id: 'ATT-4', name: 'Bang_ke_chi_tiet_tai_nguyen_Cloud.xlsx', type: 'xls', size: '850 KB', uploader: 'Hoàng Văn Thắng', date: '02/10/2026' }
        ]
    },

    'CUST-002': {
        id: 'CUST-002',
        code: 'CUST-2026-090',
        name: 'VNG Cloud Solutions',
        shortName: 'VNG CLOUD',
        avatar: 'VC',
        badge: 'Công ty con',
        industry: 'Điện toán đám mây & Hạ tầng',
        employees: '850 Nhân sự',
        owner: 'Hoàng Văn Thắng',
        phone: '028 7300 3988',
        email: 'sales@vngcloud.vn',
        taxCode: '0303538466-001',
        address: 'Tầng 12, Tòa nhà VNG Campus, KCX Tân Thuận, TP. HCM',
        website: 'https://vngcloud.vn',
        parentCompanyId: 'VNG',
        parentCompanyName: 'Tập đoàn Công nghệ VNG (VNG Corp)',
        revenue: 1850000000,
        healthScore: 92,
        subsidiaries: [],
        contacts: [
            { id: 21, name: 'Trần Minh Quân', role: 'Giám đốc Kinh doanh Cloud', phone: '0908 556 778', email: 'quan.tm@vngcloud.vn', decisionRole: 'Người quyết định chính (Decision Maker)', avatarColor: '#dbeafe', textColor: '#1e40af', initials: 'MQ' }
        ],
        deals: [
            { id: 201, name: 'Mở rộng cụm CDN Đa vùng 2026', stage: 'Đàm phán hợp đồng', status: 'OPEN', value: 650000000, probability: 85, closeDate: '20/10/2026', note: 'Khách hàng duyệt thiết kế kỹ thuật' }
        ],
        contracts: [
            { id: 'HD-CLOUD-2026-01', name: 'Hợp đồng điện toán đám mây Private Cloud', value: 1200000000, signedDate: '15/02/2026', expiryDate: '15/02/2028', status: 'Đang hiệu lực' }
        ],
        timeline: [
            { type: 'call', title: 'Trao đổi kỹ thuật mở rộng cụm CDN', author: 'Hoàng Văn Thắng', time: '05/10/2026, 10:00', body: 'Họp online với anh Quân thống nhất lịch nghiệm thu CDN.' }
        ],
        attachments: [
            { id: 'ATT-201', name: 'SLA_Dich_vu_Cloud_Enterprise.pdf', type: 'pdf', size: '1.9 MB', uploader: 'Hoàng Văn Thắng', date: '15/02/2026' }
        ]
    },

    'CUST-003': {
        id: 'CUST-003',
        code: 'CUST-2026-091',
        name: 'ZaloPay - Zion JSC',
        shortName: 'ZALOPAY',
        avatar: 'ZP',
        badge: 'Công ty con',
        industry: 'Fintech & Ví điện tử',
        employees: '1.200 Nhân sự',
        owner: 'Hoàng Văn Thắng',
        phone: '1900 54 54 36',
        email: 'hotro@zalopay.vn',
        taxCode: '0101538920',
        address: 'Tầng 5, VNG Campus, Tân Thuận, Quận 7, TP. HCM',
        website: 'https://zalopay.vn',
        parentCompanyId: 'VNG',
        parentCompanyName: 'Tập đoàn Công nghệ VNG (VNG Corp)',
        revenue: 1680000000,
        healthScore: 89,
        subsidiaries: [],
        contacts: [
            { id: 31, name: 'Phạm Hương Ly', role: 'Head of Merchant Partnerships', phone: '0988 112 990', email: 'ly.ph@zalopay.vn', decisionRole: 'Người quyết định chính (Decision Maker)', avatarColor: '#fce7f3', textColor: '#9d174d', initials: 'HL' }
        ],
        deals: [
            { id: 301, name: 'Cổng thanh toán QR Đa năng cho chuỗi bán lẻ', stage: 'Chờ ký kết', status: 'OPEN', value: 420000000, probability: 90, closeDate: '18/10/2026', note: 'Hợp đồng pháp lý đã rà soát xong' }
        ],
        contracts: [
            { id: 'HD-ZP-2026-01', name: 'Hợp đồng cổng thanh toán trực tuyến', value: 850000000, signedDate: '12/03/2026', expiryDate: '12/03/2027', status: 'Đang hiệu lực' }
        ],
        timeline: [
            { type: 'meeting', title: 'Ký kết phụ lục tích hợp giải pháp QR', author: 'Hoàng Văn Thắng', time: '02/10/2026, 14:00', body: 'Gặp chị Ly tại VNG Campus trao đổi phụ lục thanh toán.' }
        ],
        attachments: [
            { id: 'ATT-301', name: 'Hop_dong_ZaloPay_Merchant.pdf', type: 'pdf', size: '2.1 MB', uploader: 'Hoàng Văn Thắng', date: '12/03/2026' }
        ]
    },

    'CUST-004': {
        id: 'CUST-004',
        code: 'CUST-2026-050',
        name: 'Tập đoàn Bán lẻ Masan Group',
        shortName: 'MASAN',
        avatar: 'MSN',
        badge: 'Khách hàng VIP',
        industry: 'Bán lẻ & Tiêu dùng (FMCG)',
        employees: '12.000 Nhân sự',
        owner: 'Hoàng Văn Thắng',
        phone: '028 6256 3862',
        email: 'info@masangroup.com',
        taxCode: '0303576603',
        address: 'Tầng 8, Central Plaza, 17 Lê Duẩn, Quận 1, TP. HCM',
        website: 'https://masangroup.com',
        parentCompanyId: 'NONE',
        parentCompanyName: '-- Độc lập / Là công ty Mẹ (Holding) --',
        revenue: 3400000000,
        healthScore: 90,
        subsidiaries: [
            { id: 'CUST-041', code: 'CUST-2026-051', name: 'WinCommerce (Chuỗi WinMart)', shortName: 'WINCOMMERCE', industry: 'Chuỗi siêu thị', representative: 'Nguyễn Tiến Nam', revenue: 2100000000, dealCount: 4, contractCount: 2 },
            { id: 'CUST-042', code: 'CUST-2026-052', name: 'Masan MEATLife', shortName: 'MEATLIFE', industry: 'Thực phẩm sạch', representative: 'Vũ Thu Nga', revenue: 1300000000, dealCount: 3, contractCount: 1 }
        ],
        contacts: [
            { id: 41, name: 'Nguyễn Tiến Nam', role: 'Phó Tổng Giám Đốc Công Nghệ', phone: '0903 999 888', email: 'nam.nt@masangroup.com', decisionRole: 'Người quyết định chính (Decision Maker)', avatarColor: '#fef3c7', textColor: '#92400e', initials: 'TN' }
        ],
        deals: [
            { id: 401, name: 'Hệ thống Loyalty đa kênh WinMart', stage: 'Đàm phán hợp đồng', status: 'OPEN', value: 1200000000, probability: 75, closeDate: '25/11/2026', note: 'Tích hợp điểm thưởng toàn quốc' }
        ],
        contracts: [
            { id: 'HD-MSN-2026', name: 'Hợp đồng phần mềm CRM khối Bán lẻ', value: 2200000000, signedDate: '10/02/2026', expiryDate: '10/02/2028', status: 'Đang hiệu lực' }
        ],
        timeline: [
            { type: 'meeting', title: 'Họp giao ban tiến độ Loyalty WinMart', author: 'Hoàng Văn Thắng', time: '01/10/2026', body: 'Ban giám đốc Masan hài lòng về tiến độ demo.' }
        ],
        attachments: [
            { id: 'ATT-401', name: 'Bao_cao_nghiem_thu_Loyalty.pdf', type: 'pdf', size: '4.2 MB', uploader: 'Hoàng Văn Thắng', date: '01/10/2026' }
        ]
    },

    'CUST-005': {
        id: 'CUST-005',
        code: 'CUST-2026-012',
        name: 'Tổng công ty Viễn thông Viettel',
        shortName: 'VIETTEL',
        avatar: 'VTL',
        badge: 'Khách hàng VIP',
        industry: 'Viễn thông & Công nghệ',
        employees: '25.000 Nhân sự',
        owner: 'Hoàng Văn Thắng',
        phone: '1800 8098',
        email: 'cskh@viettel.com.vn',
        taxCode: '0100109106',
        address: 'Số 1 Giang Văn Minh, Ba Đình, Hà Nội',
        website: 'https://viettel.vn',
        parentCompanyId: 'NONE',
        parentCompanyName: '-- Độc lập / Là công ty Mẹ (Holding) --',
        revenue: 4100000000,
        healthScore: 96,
        subsidiaries: [
            { id: 'CUST-051', code: 'CUST-2026-013', name: 'Viettel Solutions', shortName: 'VTL SOLUTIONS', industry: 'Giải pháp Doanh nghiệp', representative: 'Lê Tuấn Anh', revenue: 2500000000, dealCount: 5, contractCount: 3 },
            { id: 'CUST-052', code: 'CUST-2026-014', name: 'Viettel Post', shortName: 'VTL POST', industry: 'Logistics & Bưu chính', representative: 'Trần Bích Thủy', revenue: 1600000000, dealCount: 4, contractCount: 2 }
        ],
        contacts: [
            { id: 51, name: 'Lê Tuấn Anh', role: 'Phó Giám Đốc Trung Tâm CNTT', phone: '0983 222 111', email: 'anhlt@viettel.com.vn', decisionRole: 'Người quyết định chính (Decision Maker)', avatarColor: '#fee2e2', textColor: '#991b1b', initials: 'TA' }
        ],
        deals: [
            { id: 501, name: 'Nền tảng Quản trị Trải nghiệm KH 5G', stage: 'Đề xuất giải pháp', status: 'OPEN', value: 1500000000, probability: 70, closeDate: '10/12/2026', note: 'Thiết kế riêng cho hệ thống thuê bao 5G' }
        ],
        contracts: [
            { id: 'HD-VTL-2026', name: 'Hợp đồng khung triển khai CRM 2026', value: 2600000000, signedDate: '05/01/2026', expiryDate: '05/01/2028', status: 'Đang hiệu lực' }
        ],
        timeline: [
            { type: 'call', title: 'Điện đàm cấp cao cùng Viettel Solutions', author: 'Hoàng Văn Thắng', time: '03/10/2026', body: 'Thống nhất tiến độ kết nối API hệ thống đo lường chất lượng mạng.' }
        ],
        attachments: [
            { id: 'ATT-501', name: 'Viettel_5G_Technical_Blueprint.pdf', type: 'pdf', size: '5.1 MB', uploader: 'Hoàng Văn Thắng', date: '03/10/2026' }
        ]
    }
};

// API Object hỗ trợ gọi và cập nhật động
const Customer360API = {
    getCurrentId() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('id') || 'CUST-001';
    },

    getCustomer(id) {
        return Customer360DB[id] || Customer360DB['CUST-001'];
    },

    updateParentCompany(customerId, parentCompanyId) {
        const cust = this.getCustomer(customerId);
        if (!cust) return false;

        cust.parentCompanyId = parentCompanyId;
        const parentMap = {
            'NONE': '-- Độc lập / Là công ty Mẹ (Holding) --',
            'VNG': 'Tập đoàn Công nghệ VNG (VNG Corp)',
            'MASAN': 'Tập đoàn Masan Group',
            'VIETTEL': 'Tập đoàn Công nghiệp - Viễn thông Viettel',
            'FPT': 'Tập đoàn FPT Corporation'
        };
        cust.parentCompanyName = parentMap[parentCompanyId] || parentCompanyId;
        return true;
    },

    addInteraction(customerId, interaction) {
        const cust = this.getCustomer(customerId);
        if (!cust) return false;
        cust.timeline.unshift(interaction);
        return true;
    },

    addContact(customerId, contact) {
        const cust = this.getCustomer(customerId);
        if (!cust) return false;
        contact.id = Date.now();
        cust.contacts.push(contact);
        return true;
    },

    addDeal(customerId, deal) {
        const cust = this.getCustomer(customerId);
        if (!cust) return false;
        deal.id = Date.now();
        cust.deals.unshift(deal);
        return true;
    },

    addAttachment(customerId, attachment) {
        const cust = this.getCustomer(customerId);
        if (!cust) return false;
        attachment.id = 'ATT-' + Date.now();
        cust.attachments.unshift(attachment);
        return true;
    }
};

// ==========================================
// 2. KHỞI TẠO VÀ EVENT LISTENERS
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    const currentId = Customer360API.getCurrentId();
    loadCustomer360View(currentId);

    initTabs();
    initQuickLog();
    initCustomerSwitcher();
    initParentCompanySelector();
    initModals();
    initDealFilters();
});

// Format VND currency
function formatVND(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

// ==========================================
// 3. LOAD TOÀN BỘ GIAO DIỆN CUSTOMER 360
// ==========================================
function loadCustomer360View(customerId) {
    const cust = Customer360API.getCustomer(customerId);
    if (!cust) return;

    // Header & Meta Info
    document.getElementById('breadcrumbCustName').textContent = cust.name;
    document.getElementById('custName').textContent = cust.name;
    document.getElementById('custAvatar').textContent = cust.avatar;
    document.getElementById('custCode').textContent = cust.code;
    document.getElementById('custIndustry').textContent = cust.industry;
    document.getElementById('custEmployees').textContent = cust.employees;
    document.getElementById('custBadge').textContent = cust.badge;

    // Dropdown Switcher Sync
    const switcher = document.getElementById('customerSwitcher');
    if (switcher) switcher.value = cust.id;

    // Parent Company Selector Sync
    const parentSelect = document.getElementById('parentCompanySelect');
    if (parentSelect) parentSelect.value = cust.parentCompanyId;

    // Side Profile Details
    document.getElementById('sidePhone').textContent = cust.phone;
    document.getElementById('sideEmail').textContent = cust.email;
    document.getElementById('sideTaxCode').textContent = cust.taxCode;
    document.getElementById('sideAddress').textContent = cust.address;
    document.getElementById('sideWebsite').innerHTML = `<a href="${cust.website}" target="_blank">${cust.website}</a>`;

    // Financial KPIs (Riêng đơn vị)
    document.getElementById('kpiRevenue').textContent = formatVND(cust.revenue);
    const openDeals = cust.deals.filter(d => d.status === 'OPEN');
    const openDealsVal = openDeals.reduce((acc, d) => acc + d.value, 0);
    document.getElementById('kpiOpenDeals').textContent = `${openDeals.length} Deals (${formatVND(openDealsVal)})`;
    document.getElementById('kpiHealthScore').textContent = `${cust.healthScore} / 100`;

    // Render Components
    renderGroupKpiCard(cust);
    renderHierarchyTree(cust);
    renderSubsidiariesTable(cust);
    renderSideSubsidiaries(cust);
    renderContacts(cust);
    renderDeals(cust, 'ALL');
    renderTimeline(cust);
    renderAttachments(cust);
    renderContracts(cust);
}

// ==========================================
// 4. CARD TỔNG GIÁ TRỊ TẬP ĐOÀN (Group Total Value Card)
// ==========================================
function renderGroupKpiCard(cust) {
    // Tính tổng giá trị tập đoàn: Đơn vị hiện tại + các công ty con
    let totalSubsidiariesRevenue = cust.subsidiaries.reduce((sum, s) => sum + s.revenue, 0);
    let groupTotalValue = cust.revenue + totalSubsidiariesRevenue;

    // Nếu công ty này là công ty con trực thuộc VNG, lấy tổng toàn bộ VNG
    if (cust.parentCompanyId === 'VNG' && cust.id !== 'CUST-001') {
        const vngHolding = Customer360DB['CUST-001'];
        groupTotalValue = vngHolding.revenue + vngHolding.subsidiaries.reduce((s, item) => s + item.revenue, 0);
    }

    const pct = groupTotalValue > 0 ? ((cust.revenue / groupTotalValue) * 100).toFixed(1) : 100;

    document.getElementById('groupTotalValueText').textContent = formatVND(groupTotalValue);
    document.getElementById('subsidiaryCountBadge').textContent = cust.subsidiaries.length;
    document.getElementById('groupContributionVal').innerHTML = `${formatVND(cust.revenue)} (<span id="groupContributionPct">${pct}%</span>)`;
    document.getElementById('groupContributionBar').style.width = `${pct}%`;
    document.getElementById('groupSubsidiariesTotal').textContent = `1 Mẹ + ${cust.subsidiaries.length} Công ty con`;
}

// ==========================================
// 5. SƠ ĐỒ HIERARCHY CÔNG TY MẸ - CON
// ==========================================
function renderHierarchyTree(cust) {
    const tree = document.getElementById('hierarchyTree');
    if (!tree) return;

    let isHolding = (cust.parentCompanyId === 'NONE' || cust.id === 'CUST-001');
    let parentName = isHolding ? cust.name : cust.parentCompanyName;

    let subTreeHtml = '';
    if (cust.subsidiaries && cust.subsidiaries.length > 0) {
        subTreeHtml = cust.subsidiaries.map(sub => `
            <li class="c360-tree-sub-item">
                <div class="c360-tree-card" onclick="switchToCustomer('${sub.id}')" style="cursor: pointer;">
                    <div class="c360-tree-card-info">
                        <div class="c360-tree-icon" style="background: #e0f2fe; color: #0369a1;">🏢</div>
                        <div>
                            <div style="font-weight: 700; font-size: 0.9rem; color: var(--text-primary);">${sub.name}</div>
                            <div style="font-size: 0.78rem; color: var(--text-muted);">
                                <span>Mã: ${sub.code}</span> • <span>Lĩnh vực: ${sub.industry}</span> • <span>Đại diện: ${sub.representative}</span>
                            </div>
                        </div>
                    </div>
                    <div style="text-align: right;">
                        <span class="badge badge-purple" style="font-size: 0.8rem; font-weight: 700;">${formatVND(sub.revenue)}</span>
                        <div style="font-size: 0.72rem; color: var(--primary); font-weight: 600; margin-top: 2px;">Xem hồ sơ 360 ➔</div>
                    </div>
                </div>
            </li>
        `).join('');
    } else {
        subTreeHtml = `
            <li class="c360-tree-sub-item">
                <div style="padding: 12px; background: #ffffff; border: 1px dashed var(--border-color); border-radius: 6px; font-size: 0.82rem; color: var(--text-muted);">
                    Đơn vị này hiện là công ty thành viên trực thuộc, chưa có công ty con cấp dưới.
                </div>
            </li>
        `;
    }

    tree.innerHTML = `
        <li class="c360-tree-node">
            <!-- Node Công ty Mẹ -->
            <div class="c360-tree-card ${isHolding ? 'active' : ''}">
                <div class="c360-tree-card-info">
                    <div class="c360-tree-icon" style="background: #e0e7ff; color: #4338ca;">🏛️</div>
                    <div>
                        <div style="font-weight: 800; font-size: 1rem; color: var(--text-primary);">
                            ${parentName} 
                            ${isHolding ? '<span class="badge badge-success" style="margin-left: 8px;">Đang xem (Mẹ)</span>' : '<span class="badge badge-secondary" style="margin-left: 8px;">Công ty Mẹ</span>'}
                        </div>
                        <div style="font-size: 0.8rem; color: var(--text-muted);">
                            <span>Tập đoàn Holding</span> • <span>Hệ thống hợp nhất doanh thu & chỉ số</span>
                        </div>
                    </div>
                </div>
                <div style="text-align: right;">
                    <span class="badge badge-success" style="font-size: 0.82rem;">Cấp cao nhất</span>
                </div>
            </div>

            <!-- Nhánh các công ty con -->
            <ul class="c360-tree-branch">
                ${subTreeHtml}
            </ul>
        </li>
    `;

    document.getElementById('tabBadgeHierarchy').textContent = (cust.subsidiaries.length + 1);
}

// Bảng chi tiết danh sách công ty con
function renderSubsidiariesTable(cust) {
    const tbody = document.getElementById('subsidiaryTableBody');
    if (!tbody) return;

    if (!cust.subsidiaries || cust.subsidiaries.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; color: var(--text-muted); padding: 24px;">
                    Không có công ty con trực thuộc.
                </td>
            </tr>
        `;
        document.getElementById('subsidiaryListTableCount').textContent = '0 Công ty';
        return;
    }

    const totalRev = cust.revenue + cust.subsidiaries.reduce((s, c) => s + c.revenue, 0);

    tbody.innerHTML = cust.subsidiaries.map(sub => {
        const pct = totalRev > 0 ? ((sub.revenue / totalRev) * 100).toFixed(1) : 0;
        return `
            <tr>
                <td><strong>${sub.name}</strong></td>
                <td><code>${sub.code}</code></td>
                <td>${sub.industry}</td>
                <td>👤 ${sub.representative}</td>
                <td><strong style="color: var(--primary);">${formatVND(sub.revenue)}</strong></td>
                <td><span class="badge badge-info">${pct}%</span></td>
                <td>
                    <button type="button" class="btn btn-secondary btn-sm" onclick="switchToCustomer('${sub.id}')">
                        Xem 360
                    </button>
                </td>
            </tr>
        `;
    }).join('');

    document.getElementById('subsidiaryListTableCount').textContent = `${cust.subsidiaries.length} Công ty`;
}

// Render Card công ty con ở thanh bên trái
function renderSideSubsidiaries(cust) {
    const container = document.getElementById('sideSubsidiariesList');
    if (!container) return;

    if (!cust.subsidiaries || cust.subsidiaries.length === 0) {
        container.innerHTML = `<div style="font-size: 0.8rem; color: var(--text-muted);">Không có công ty con.</div>`;
        document.getElementById('sideSubsidiaryCountBadge').textContent = '0 Đơn vị';
        return;
    }

    document.getElementById('sideSubsidiaryCountBadge').textContent = `${cust.subsidiaries.length} Đơn vị`;
    container.innerHTML = cust.subsidiaries.map(sub => `
        <div style="display: flex; justify-content: space-between; align-items: center; padding: 8px 10px; background: #f8fafc; border: 1px solid var(--border-color); border-radius: 6px; cursor: pointer;" onclick="switchToCustomer('${sub.id}')">
            <div>
                <div style="font-weight: 700; font-size: 0.82rem; color: var(--text-primary);">${sub.shortName}</div>
                <div style="font-size: 0.72rem; color: var(--text-muted);">${formatVND(sub.revenue)}</div>
            </div>
            <span style="font-size: 0.8rem; color: var(--primary);">➔</span>
        </div>
    `).join('');
}

// ==========================================
// 6. CONTACTS (Người liên hệ)
// ==========================================
function renderContacts(cust) {
    const list = document.getElementById('contactList');
    if (!list) return;

    document.getElementById('contactCountBadge').textContent = `${cust.contacts.length} Liên hệ`;
    list.innerHTML = cust.contacts.map(c => `
        <div class="c360-contact-item">
            <div class="c360-contact-avatar" style="background: ${c.avatarColor}; color: ${c.textColor};">
                ${c.initials || 'KH'}
            </div>
            <div class="c360-contact-info">
                <div class="c360-contact-name">${c.name}</div>
                <div class="c360-contact-role">${c.role}</div>
                <div style="font-size: 0.72rem; color: var(--primary); margin-top: 2px;">📞 ${c.phone}</div>
            </div>
            <div class="c360-contact-actions">
                <button type="button" class="c360-contact-action-btn" title="Gọi điện" onclick="quickCallContact('${c.name}', '${c.phone}')">📞</button>
                <button type="button" class="c360-contact-action-btn" title="Gửi email" onclick="quickEmailContact('${c.name}', '${c.email}')">✉️</button>
            </div>
        </div>
    `).join('');
}

// ==========================================
// 7. OPPORTUNITY MỞ & ĐÓNG (Deals)
// ==========================================
function renderDeals(cust, filterStatus = 'ALL') {
    const tbody = document.getElementById('dealsTableBody');
    if (!tbody) return;

    // Cập nhật số lượng
    const allCount = cust.deals.length;
    const openCount = cust.deals.filter(d => d.status === 'OPEN').length;
    const wonCount = cust.deals.filter(d => d.status === 'CLOSED_WON').length;
    const lostCount = cust.deals.filter(d => d.status === 'CLOSED_LOST').length;

    document.getElementById('dealCountAll').textContent = allCount;
    document.getElementById('dealCountOpen').textContent = openCount;
    document.getElementById('dealCountWon').textContent = wonCount;
    document.getElementById('dealCountLost').textContent = lostCount;
    document.getElementById('tabBadgeDeals').textContent = allCount;

    let filteredDeals = cust.deals;
    if (filterStatus !== 'ALL') {
        filteredDeals = cust.deals.filter(d => d.status === filterStatus);
    }

    if (filteredDeals.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 20px;">Không có cơ hội nào trong mục này.</td></tr>`;
        return;
    }

    tbody.innerHTML = filteredDeals.map(d => {
        let badgeClass = 'badge-purple';
        if (d.status === 'CLOSED_WON') badgeClass = 'badge-success';
        if (d.status === 'CLOSED_LOST') badgeClass = 'badge-danger';
        if (d.stage === 'Đàm phán hợp đồng') badgeClass = 'badge-info';

        return `
            <tr>
                <td><strong>${d.name}</strong></td>
                <td><span class="badge ${badgeClass}">${d.stage}</span></td>
                <td><strong style="color: var(--primary);">${formatVND(d.value)}</strong></td>
                <td>${d.probability}%</td>
                <td>${d.closeDate}</td>
                <td style="font-size: 0.82rem; color: var(--text-secondary);">${d.note || '—'}</td>
            </tr>
        `;
    }).join('');
}

function initDealFilters() {
    const filterButtons = document.querySelectorAll('.c360-deal-filter-btn');
    filterButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            filterButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            const status = btn.getAttribute('data-status');
            const cust = Customer360API.getCustomer(Customer360API.getCurrentId());
            renderDeals(cust, status);
        });
    });
}

// ==========================================
// 8. TIMELINE TƯƠNG TÁC & QUICK LOG
// ==========================================
function renderTimeline(cust) {
    const timeline = document.getElementById('activityTimeline');
    if (!timeline) return;

    document.getElementById('tabBadgeTimeline').textContent = cust.timeline.length;

    const iconMap = {
        meeting: '🤝',
        call: '📞',
        email: '✉️',
        note: '📝',
        care: '❤️'
    };

    timeline.innerHTML = cust.timeline.map(item => `
        <div class="c360-timeline-item">
            <div class="c360-timeline-icon">${iconMap[item.type] || '📝'}</div>
            <div class="c360-timeline-card">
                <div class="c360-timeline-header">
                    <span class="c360-timeline-title">${item.title} • <small style="color: var(--primary); font-weight: 700;">${item.author}</small></span>
                    <span class="c360-timeline-time">${item.time}</span>
                </div>
                <div class="c360-timeline-body">${escapeHtml(item.body)}</div>
            </div>
        </div>
    `).join('');
}

function initQuickLog() {
    const typeButtons = document.querySelectorAll('.c360-log-type-btn');
    const noteInput = document.getElementById('quickLogText');
    const submitBtn = document.getElementById('quickLogSubmit');

    let currentType = 'note';

    typeButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            typeButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            currentType = btn.getAttribute('data-type');
        });
    });

    if (submitBtn && noteInput) {
        submitBtn.addEventListener('click', () => {
            const content = noteInput.value.trim();
            if (!content) {
                noteInput.focus();
                return;
            }

            const metaConfig = {
                note: { title: 'Ghi chú nội bộ' },
                call: { title: 'Cuộc gọi điện thoại' },
                meeting: { title: 'Họp / Gặp gỡ trực tiếp' },
                email: { title: 'Email tương tác' },
                care: { title: 'Chăm sóc định kỳ sau bán' }
            };

            const config = metaConfig[currentType] || metaConfig.note;
            const now = new Date();
            const timeStr = `Hôm nay, ${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`;

            const custId = Customer360API.getCurrentId();
            Customer360API.addInteraction(custId, {
                type: currentType,
                title: config.title,
                author: 'Hoàng Văn Thắng',
                time: timeStr,
                body: content
            });

            noteInput.value = '';
            const cust = Customer360API.getCustomer(custId);
            renderTimeline(cust);
            showToast('✅ Đã lưu tương tác mới vào Timeline!');
        });
    }
}

// ==========================================
// 9. ATTACHMENTS (Tài liệu đính kèm)
// ==========================================
function renderAttachments(cust) {
    const grid = document.getElementById('attachmentGrid');
    if (!grid) return;

    document.getElementById('tabBadgeAttachments').textContent = cust.attachments.length;

    grid.innerHTML = cust.attachments.map(att => `
        <div class="c360-attachment-card">
            <div class="c360-attachment-icon ${att.type}">
                ${att.type === 'pdf' ? '📄' : att.type === 'doc' ? '📝' : att.type === 'xls' ? '📊' : '🖼️'}
            </div>
            <div class="c360-attachment-meta">
                <div class="c360-attachment-name" title="${att.name}">${att.name}</div>
                <div class="c360-attachment-sub">${att.size} • Tải lên: ${att.date}</div>
                <div class="c360-attachment-actions">
                    <button type="button" class="btn btn-secondary btn-sm" onclick="alert('Xem trước tài liệu: ${att.name}')">Xem</button>
                    <button type="button" class="btn btn-primary btn-sm" onclick="alert('Đang tải xuống: ${att.name}')">Tải về</button>
                </div>
            </div>
        </div>
    `).join('');
}

// ==========================================
// 10. CONTRACTS (Hợp đồng)
// ==========================================
function renderContracts(cust) {
    const tbody = document.getElementById('contractsTableBody');
    if (!tbody) return;

    document.getElementById('tabBadgeContracts').textContent = cust.contracts.length;

    tbody.innerHTML = cust.contracts.map(c => `
        <tr>
            <td><strong>${c.id}</strong></td>
            <td>${c.name}</td>
            <td><strong style="color: var(--primary);">${formatVND(c.value)}</strong></td>
            <td>${c.signedDate}</td>
            <td>${c.expiryDate}</td>
            <td><span class="badge badge-success">${c.status}</span></td>
        </tr>
    `).join('');
}

// ==========================================
// 11. SWITCHER, PARENT DROPDOWN & TABS
// ==========================================
function initCustomerSwitcher() {
    const switcher = document.getElementById('customerSwitcher');
    if (!switcher) return;

    switcher.addEventListener('change', (e) => {
        switchToCustomer(e.target.value);
    });
}

function switchToCustomer(customerId) {
    const url = new URL(window.location.href);
    url.searchParams.set('id', customerId);
    window.history.pushState({}, '', url);

    loadCustomer360View(customerId);
    showToast(`🔄 Đã chuyển sang xem hồ sơ 360: ${Customer360API.getCustomer(customerId).name}`);
}

function initParentCompanySelector() {
    const select = document.getElementById('parentCompanySelect');
    const notice = document.getElementById('parentSavedNotice');
    if (!select) return;

    select.addEventListener('change', () => {
        const custId = Customer360API.getCurrentId();
        const parentId = select.value;

        Customer360API.updateParentCompany(custId, parentId);
        const cust = Customer360API.getCustomer(custId);

        // Re-render hierarchy & group total card
        renderGroupKpiCard(cust);
        renderHierarchyTree(cust);

        if (notice) {
            notice.style.display = 'inline';
            setTimeout(() => { notice.style.display = 'none'; }, 2500);
        }
        showToast('🏛️ Đã cập nhật quan hệ công ty mẹ thành công!');
    });
}

function initTabs() {
    const tabButtons = document.querySelectorAll('.c360-tab-btn');
    const tabPanes = document.querySelectorAll('.c360-tab-pane');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetTab = btn.getAttribute('data-tab');

            tabButtons.forEach(b => b.classList.remove('active'));
            tabPanes.forEach(p => p.classList.remove('active'));

            btn.classList.add('active');
            const targetPane = document.getElementById(targetTab);
            if (targetPane) {
                targetPane.classList.add('active');
            }
        });
    });
}

// ==========================================
// 12. MODALS & FORMS
// ==========================================
function initModals() {
    // Open Contact Modal
    const btnOpenContact = document.getElementById('btnOpenNewContact');
    const btnSideAddContact = document.getElementById('btnSideAddContact');
    const modalContact = document.getElementById('modalContact');

    [btnOpenContact, btnSideAddContact].forEach(btn => {
        if (btn) btn.addEventListener('click', () => { modalContact.style.display = 'flex'; });
    });

    // Open Deal Modal
    const btnOpenDeal = document.getElementById('btnOpenNewDeal');
    const btnCreateNewDealTop = document.getElementById('btnCreateNewDealTop');
    const modalDeal = document.getElementById('modalDeal');

    [btnOpenDeal, btnCreateNewDealTop].forEach(btn => {
        if (btn) btn.addEventListener('click', () => { modalDeal.style.display = 'flex'; });
    });

    // Open Upload Attachment Modal
    const btnOpenAttach = document.getElementById('btnOpenUploadAttachment');
    const btnUploadAttachTrigger = document.getElementById('btnUploadAttachmentTrigger');
    const modalAttach = document.getElementById('modalAttachment');

    [btnOpenAttach, btnUploadAttachTrigger].forEach(btn => {
        if (btn) btn.addEventListener('click', () => { modalAttach.style.display = 'flex'; });
    });

    // Close Modals
    document.querySelectorAll('[data-close]').forEach(btn => {
        btn.addEventListener('click', () => {
            const targetModalId = btn.getAttribute('data-close');
            const target = document.getElementById(targetModalId);
            if (target) target.style.display = 'none';
        });
    });

    // Form Add Contact Submit
    const formContact = document.getElementById('formAddContact');
    if (formContact) {
        formContact.addEventListener('submit', (e) => {
            e.preventDefault();
            const name = document.getElementById('contactNameInput').value.trim();
            const role = document.getElementById('contactRoleInput').value.trim() || 'Người liên hệ';
            const phone = document.getElementById('contactPhoneInput').value.trim();
            const email = document.getElementById('contactEmailInput').value.trim() || 'contact@client.vn';
            const decisionRole = document.getElementById('contactDecisionRole').value;

            const initials = name.split(' ').map(w => w[0]).join('').slice(-2).toUpperCase();

            const custId = Customer360API.getCurrentId();
            Customer360API.addContact(custId, {
                name, role, phone, email, decisionRole,
                initials, avatarColor: '#e0e7ff', textColor: '#4338ca'
            });

            formContact.reset();
            modalContact.style.display = 'none';
            renderContacts(Customer360API.getCustomer(custId));
            showToast('👤 Đã thêm người liên hệ mới!');
        });
    }

    // Form Add Deal Submit
    const formDeal = document.getElementById('formAddDeal');
    if (formDeal) {
        formDeal.addEventListener('submit', (e) => {
            e.preventDefault();
            const name = document.getElementById('dealNameInput').value.trim();
            const value = parseFloat(document.getElementById('dealValueInput').value) || 0;
            const probability = parseInt(document.getElementById('dealProbabilityInput').value) || 50;
            const stage = document.getElementById('dealStageSelect').value;
            const closeDate = document.getElementById('dealCloseDateInput').value;

            let status = 'OPEN';
            if (stage === 'Closed Won') status = 'CLOSED_WON';
            if (stage === 'Closed Lost') status = 'CLOSED_LOST';

            const custId = Customer360API.getCurrentId();
            Customer360API.addDeal(custId, {
                name, value, probability, stage, status,
                closeDate, note: 'Tạo từ màn hình Customer 360'
            });

            formDeal.reset();
            modalDeal.style.display = 'none';
            const cust = Customer360API.getCustomer(custId);
            renderDeals(cust, 'ALL');
            showToast('💼 Đã tạo cơ hội bán hàng mới!');
        });
    }

    // Form Add Attachment Submit
    const formAttach = document.getElementById('formAddAttachment');
    if (formAttach) {
        formAttach.addEventListener('submit', (e) => {
            e.preventDefault();
            const name = document.getElementById('attachNameInput').value.trim();
            const type = document.getElementById('attachTypeSelect').value;
            const size = document.getElementById('attachSizeInput').value.trim() || '1.5 MB';

            const custId = Customer360API.getCurrentId();
            Customer360API.addAttachment(custId, {
                name, type, size, uploader: 'Hoàng Văn Thắng', date: 'Hôm nay'
            });

            formAttach.reset();
            modalAttach.style.display = 'none';
            renderAttachments(Customer360API.getCustomer(custId));
            showToast('📎 Đã tải lên tài liệu mới thành công!');
        });
    }

    // Quick Contact Action Helper
    window.quickCallContact = (name, phone) => {
        alert(`Bắt đầu cuộc gọi VOIP với: ${name} (${phone})`);
    };

    window.quickEmailContact = (name, email) => {
        alert(`Mở hộp thư gửi email tới: ${name} (${email})`);
    };

    // Quick Action Bar handlers
    const btnQuickCall = document.getElementById('btnQuickCall');
    if (btnQuickCall) {
        btnQuickCall.addEventListener('click', () => {
            alert('Đang kết nối tổng đài VOIP với người liên hệ chính của khách hàng...');
        });
    }

    const btnQuickEmail = document.getElementById('btnQuickEmail');
    if (btnQuickEmail) {
        btnQuickEmail.addEventListener('click', () => {
            alert('Mở trình soạn email gửi tới khách hàng...');
        });
    }
}

// Toast notification helper
function showToast(message) {
    const toast = document.getElementById('c360Toast');
    const msgEl = document.getElementById('c360ToastMsg');
    if (!toast || !msgEl) return;

    msgEl.textContent = message;
    toast.style.display = 'flex';

    setTimeout(() => {
        toast.style.display = 'none';
    }, 3000);
}

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
