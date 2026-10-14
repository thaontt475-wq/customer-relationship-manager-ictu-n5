/**
 * ===================================================================
 * CRM-119 / S4-05: Cấu hình Chấm điểm Lead theo Tiêu chí khai báo
 * Author: Senior Frontend Developer (Nông Quang Tiệp)
 * Scope: HTML5, CSS3, Vanilla ES6+ - Zero JSP Policy - No Backend Modification
 * RESTful Endpoints Ready:
 *   - GET  /api/leads/scoring-rules
 *   - POST /api/leads/scoring-rules
 *   - POST /api/leads/re-score
 *   - GET  /api/leads?include_score=true
 * ===================================================================
 */

(function () {
    "use strict";

    // 1. STORAGE KEYS & CONSTANTS
    const STORAGE_KEY_RULES = "crm_lead_scoring_rules";
    const STORAGE_KEY_LEADS = "crm_leads_scoring_data";
    const STORAGE_KEY_THRESHOLDS = "crm_lead_scoring_thresholds";

    const DEFAULT_THRESHOLDS = {
        hot: 70,
        warm: 40
    };

    const DEFAULT_RULES = [
        // NHÓM 1: NGÀNH NGHỀ PHÙ HỢP (INDUSTRY FIT)
        {
            id: "R-IND-01",
            group: "INDUSTRY",
            name: "Công nghệ thông tin & Viễn thông",
            field: "industry",
            operator: "CONTAINS",
            value: "CNTT",
            points: 20,
            description: "Ngành mục tiêu chiến lược của phần mềm CRM B2B.",
            isActive: true
        },
        {
            id: "R-IND-02",
            group: "INDUSTRY",
            name: "Bất động sản & Xây dựng",
            field: "industry",
            operator: "CONTAINS",
            value: "Bất động sản",
            points: 15,
            description: "Nhu cầu quản lý sàn giao dịch và dữ liệu khách mua cao.",
            isActive: true
        },
        {
            id: "R-IND-03",
            group: "INDUSTRY",
            name: "Dược phẩm & Y tế",
            field: "industry",
            operator: "CONTAINS",
            value: "Dược phẩm",
            points: 15,
            description: "Doanh nghiệp phân phối thuốc và thiết bị y tế có đội ngũ Sales lớn.",
            isActive: true
        },
        {
            id: "R-IND-04",
            group: "INDUSTRY",
            name: "Bán lẻ & Chuỗi F&B",
            field: "industry",
            operator: "CONTAINS",
            value: "Bán lẻ",
            points: 10,
            description: "Phục vụ chương trình khách hàng thân thiết và tích điểm.",
            isActive: true
        },
        {
            id: "R-IND-05",
            group: "INDUSTRY",
            name: "Sản xuất & Công nghiệp",
            field: "industry",
            operator: "CONTAINS",
            value: "Sản xuất",
            points: 10,
            description: "Mô hình B2B truyền thống, chu kỳ bán hàng dài.",
            isActive: true
        },
        {
            id: "R-IND-06",
            group: "INDUSTRY",
            name: "Giáo dục & Đào tạo",
            field: "industry",
            operator: "CONTAINS",
            value: "Giáo dục",
            points: 5,
            description: "Tuyển sinh và tư vấn khóa học.",
            isActive: true
        },

        // NHÓM 2: QUY MÔ DOANH NGHIỆP (COMPANY SIZE)
        {
            id: "R-SIZE-01",
            group: "COMPANY_SIZE",
            name: "Doanh nghiệp lớn (> 100 nhân sự)",
            field: "employeeCount",
            operator: "GREATER_THAN",
            value: "100",
            points: 20,
            description: "Ngân sách phần mềm dồi dào, cần phân quyền sâu theo phòng ban.",
            isActive: true
        },
        {
            id: "R-SIZE-02",
            group: "COMPANY_SIZE",
            name: "Doanh nghiệp vừa (20 - 100 nhân sự)",
            field: "employeeCount",
            operator: "GREATER_THAN",
            value: "20",
            points: 10,
            description: "Quy mô chuẩn, thời gian triển khai nhanh gọn từ 2-4 tuần.",
            isActive: true
        },
        {
            id: "R-SIZE-03",
            group: "COMPANY_SIZE",
            name: "Doanh nghiệp nhỏ (< 20 nhân sự)",
            field: "employeeCount",
            operator: "LESS_THAN",
            value: "20",
            points: 5,
            description: "Chỉ cần gói Starter cơ bản, tỷ lệ chốt thấp hơn.",
            isActive: true
        },

        // NHÓM 3: NGUỒN TIẾP CẬN LEAD (LEAD SOURCE)
        {
            id: "R-SRC-01",
            group: "SOURCE",
            name: "Khách hàng cũ hoặc Đối tác giới thiệu (Referral)",
            field: "source",
            operator: "CONTAINS",
            value: "Giới thiệu",
            points: 25,
            description: "Độ tin cậy cực cao, tỷ lệ chuyển đổi thành công trên 45%.",
            isActive: true
        },
        {
            id: "R-SRC-02",
            group: "SOURCE",
            name: "Website Form / Landing Page trực tuyến",
            field: "source",
            operator: "CONTAINS",
            value: "Website",
            points: 15,
            description: "Khách hàng chủ động tìm kiếm giải pháp và để lại thông tin.",
            isActive: true
        },
        {
            id: "R-SRC-03",
            group: "SOURCE",
            name: "Hội thảo / Sự kiện chuyên ngành (Event/Expo)",
            field: "source",
            operator: "CONTAINS",
            value: "Sự kiện",
            points: 10,
            description: "Khách hàng đã gặp mặt trực tiếp đội ngũ chuyên viên.",
            isActive: true
        },
        {
            id: "R-SRC-04",
            group: "SOURCE",
            name: "Tiếp thị cuộc gọi lạnh (Cold Call)",
            field: "source",
            operator: "CONTAINS",
            value: "Cold Call",
            points: 5,
            description: "Khách hàng thụ động, cần nhiều thời gian thuyết phục.",
            isActive: true
        },
        {
            id: "R-SRC-05",
            group: "SOURCE",
            name: "Danh sách mua ngoài / Chưa thẩm định",
            field: "source",
            operator: "CONTAINS",
            value: "Mua ngoài",
            points: -10,
            description: "Data thô chất lượng kém, thường sai thông tin người đại diện.",
            isActive: true
        },

        // NHÓM 4: MỨC ĐỘ QUAN TÂM & HÀNH VI (ENGAGEMENT & INTENT)
        {
            id: "R-INT-01",
            group: "INTENT",
            name: "Yêu cầu Demo giải pháp / Báo giá gấp",
            field: "recentActivity",
            operator: "CONTAINS",
            value: "Demo",
            points: 30,
            description: "Tín hiệu mua hàng mạnh mẽ nhất, cần liên hệ trong vòng 30 phút.",
            isActive: true
        },
        {
            id: "R-INT-02",
            group: "INTENT",
            name: "Tải Bảng giá & Hồ sơ năng lực (Profile)",
            field: "recentActivity",
            operator: "CONTAINS",
            value: "Tải bảng giá",
            points: 10,
            description: "Khách hàng đang trong giai đoạn khảo giá và so sánh nhà cung cấp.",
            isActive: true
        },
        {
            id: "R-INT-03",
            group: "INTENT",
            name: "Mở Email & Click link tài liệu 3+ lần",
            field: "recentActivity",
            operator: "CONTAINS",
            value: "Click link",
            points: 10,
            description: "Mức độ quan tâm cao đối với nội dung chuyên môn.",
            isActive: true
        },
        {
            id: "R-INT-04",
            group: "INTENT",
            name: "Điền khảo sát nhu cầu sử dụng CRM",
            field: "recentActivity",
            operator: "CONTAINS",
            value: "Khảo sát",
            points: 5,
            description: "Cung cấp đầy đủ thông tin về quy mô và nỗi đau nghiệp vụ.",
            isActive: true
        },
        {
            id: "R-INT-05",
            group: "INTENT",
            name: "Không phản hồi / Không nghe máy trong 14 ngày",
            field: "recentActivity",
            operator: "CONTAINS",
            value: "Không phản hồi",
            points: -15,
            description: "Tương tác nguội lạnh, cần chuyển sang danh sách nuôi dưỡng dài hạn.",
            isActive: true
        }
    ];

    // 2. MOCK LEADS DATA (RICH B2B REALISTIC ENTERPRISE DATA)
    const DEFAULT_LEADS = [
        {
            id: "LD-901",
            fullName: "Trịnh Đình Trọng",
            title: "Giám đốc Vận hành (COO)",
            email: "trong.td@vinatech.vn",
            phone: "0912345678",
            companyName: "Công ty CP Tập đoàn Công nghệ VinaTech",
            industry: "CNTT & Viễn thông",
            employeeCount: 180,
            source: "Website Form / Đăng ký dùng thử",
            recentActivity: "Yêu cầu Demo trực tiếp cho 45 Sales",
            avatar: "TĐ",
            createdAt: "2026-10-14T08:30:00Z"
        },
        {
            id: "LD-902",
            fullName: "Nguyễn Thị Phương Thảo",
            title: "Trưởng phòng Kinh doanh B2B",
            email: "thao.ntp@hoanggiagroup.com",
            phone: "0987654321",
            companyName: "Tập đoàn Địa ốc Hoàng Gia Land",
            industry: "Bất động sản & Xây dựng",
            employeeCount: 250,
            source: "Khách hàng cũ giới thiệu (Referral)",
            recentActivity: "Yêu cầu Demo giải pháp và báo giá gói Enterprise",
            avatar: "PT",
            createdAt: "2026-10-14T09:15:00Z"
        },
        {
            id: "LD-903",
            fullName: "Vũ Hải Đăng",
            title: "Phó Giám đốc Kỹ thuật",
            email: "dang.vh@vinasmart.vn",
            phone: "0908889999",
            companyName: "Công ty Cổ phần VinaSmart Global",
            industry: "CNTT & Phần mềm Nhúng",
            employeeCount: 65,
            source: "Hội thảo / Sự kiện TechExpo 2026",
            recentActivity: "Tải Bảng giá & Tài liệu API tích hợp",
            avatar: "HĐ",
            createdAt: "2026-10-13T14:20:00Z"
        },
        {
            id: "LD-904",
            fullName: "Lê Hoàng Cường",
            title: "Tổng Giám đốc (CEO)",
            email: "cuong.lh@duochoalinh.vn",
            phone: "0903456789",
            companyName: "Công ty Dược phẩm Hoa Linh Pharma",
            industry: "Dược phẩm & Y tế",
            employeeCount: 120,
            source: "Website Form trực tuyến",
            recentActivity: "Yêu cầu Demo module quản lý Trình Dược viên",
            avatar: "HC",
            createdAt: "2026-10-13T11:00:00Z"
        },
        {
            id: "LD-905",
            fullName: "Trần Thu Hà",
            title: "Trưởng phòng Mua sắm & CNTT",
            email: "ha.tran@bachhoasach.vn",
            phone: "0912345679",
            companyName: "Chuỗi Bách Hóa Sạch Miền Bắc",
            industry: "Bán lẻ & Chuỗi F&B",
            employeeCount: 85,
            source: "Đối tác giới thiệu (Referral)",
            recentActivity: "Điền khảo sát nhu cầu tích điểm CRM",
            avatar: "TH",
            createdAt: "2026-10-12T16:45:00Z"
        },
        {
            id: "LD-906",
            fullName: "Phạm Thu Dung",
            title: "Giám đốc Marketing (CMO)",
            email: "dung.pt@sunshinemedia.vn",
            phone: "0933221100",
            companyName: "Sunshine Media & Communications",
            industry: "CNTT & Truyền thông số",
            employeeCount: 45,
            source: "Website Form / Tải Ebook",
            recentActivity: "Mở Email & Click link xem tính năng Pipeline 4 lần",
            avatar: "TD",
            createdAt: "2026-10-12T10:10:00Z"
        },
        {
            id: "LD-907",
            fullName: "Đỗ Minh Tuấn",
            title: "Chủ tịch HĐQT",
            email: "tuan.dm@anphatplastic.com",
            phone: "0944556677",
            companyName: "Nhựa Công nghiệp An Phát Holdings",
            industry: "Sản xuất & Công nghiệp",
            employeeCount: 320,
            source: "Gặp trực tiếp tại Triển lãm Quốc tế",
            recentActivity: "Tải Bảng giá và yêu cầu gửi hồ sơ năng lực công ty",
            avatar: "MT",
            createdAt: "2026-10-11T15:30:00Z"
        },
        {
            id: "LD-908",
            fullName: "Nguyễn Văn Hùng",
            title: "Trưởng ban Đào tạo",
            email: "hung.nv@eduplus.vn",
            phone: "0977889900",
            companyName: "Học viện Kỹ năng Doanh nghiệp EduPlus",
            industry: "Giáo dục & Đào tạo",
            employeeCount: 25,
            source: "Website Form tư vấn",
            recentActivity: "Điền form khảo sát thông tin",
            avatar: "VH",
            createdAt: "2026-10-11T09:00:00Z"
        },
        {
            id: "LD-909",
            fullName: "Bùi Hoàng Nam",
            title: "Chuyên viên Mua hàng",
            email: "nam.bh@danhsachthotrangvang.vn",
            phone: "0966112233",
            companyName: "Cửa hàng Thiết bị Văn phòng Nam Phát",
            industry: "Bán lẻ & Phân phối nhỏ",
            employeeCount: 8,
            source: "Danh sách mua ngoài / Data thô",
            recentActivity: "Không phản hồi sau 3 cuộc gọi liên hệ",
            avatar: "HN",
            createdAt: "2026-10-10T14:00:00Z"
        },
        {
            id: "LD-910",
            fullName: "Hoàng Gia Bảo",
            title: "Nhân viên Bán lẻ",
            email: "bao.hg@coldcalllead.com",
            phone: "0922334455",
            companyName: "Đại lý Tạp hóa Hoàng Bảo",
            industry: "Khác",
            employeeCount: 5,
            source: "Tiếp thị cuộc gọi lạnh (Cold Call)",
            recentActivity: "Không phản hồi, báo nhầm máy",
            avatar: "GB",
            createdAt: "2026-10-09T16:20:00Z"
        },
        {
            id: "LD-911",
            fullName: "Lâm Quốc Cường",
            title: "Giám đốc Kinh doanh",
            email: "cuong.lq@megalogistics.vn",
            phone: "0918765432",
            companyName: "Công ty Tiếp vận Quốc tế Mega Logistics",
            industry: "Sản xuất & Vận tải Logistics",
            employeeCount: 140,
            source: "Website Form trực tuyến",
            recentActivity: "Tải Bảng giá & Yêu cầu Demo cho 15 nhân viên",
            avatar: "QC",
            createdAt: "2026-10-14T10:00:00Z"
        },
        {
            id: "LD-912",
            fullName: "Dương Minh Châu",
            title: "Phụ trách Nhân sự & Vận hành",
            email: "chau.dm@fintechviet.com",
            phone: "0909123888",
            companyName: "Công ty Cổ phần Giải pháp Tài chính FinTech Việt",
            industry: "CNTT & Dịch vụ Tài chính",
            employeeCount: 55,
            source: "Đối tác chiến lược giới thiệu (Referral)",
            recentActivity: "Yêu cầu Demo giải pháp tích hợp SSO & ERP",
            avatar: "MC",
            createdAt: "2026-10-14T07:45:00Z"
        }
    ];

    // 3. STATE MANAGEMENT
    let scoringRules = [];
    let scoringThresholds = { ...DEFAULT_THRESHOLDS };
    let scoredLeads = [];
    let currentTab = "ranking"; // 'ranking' | 'rules'
    let editingCriteriaId = null;

    // DOM Elements Cache
    let dom = {};

    function initDom() {
        dom = {
            // KPIs
            kpiTotalLeads: document.getElementById("kpiTotalLeads"),
            kpiAvgScore: document.getElementById("kpiAvgScore"),
            kpiHotLeads: document.getElementById("kpiHotLeads"),
            kpiWarmLeads: document.getElementById("kpiWarmLeads"),
            kpiColdLeads: document.getElementById("kpiColdLeads"),
            kpiHotThresholdBadge: document.getElementById("kpiHotThresholdBadge"),
            kpiWarmThresholdBadge: document.getElementById("kpiWarmThresholdBadge"),
            kpiColdThresholdBadge: document.getElementById("kpiColdThresholdBadge"),

            // Tabs
            tabRankingBtn: document.getElementById("tabRankingBtn"),
            tabRulesBtn: document.getElementById("tabRulesBtn"),
            paneRanking: document.getElementById("paneRanking"),
            paneRules: document.getElementById("paneRules"),
            tabLeadsCount: document.getElementById("tabLeadsCount"),
            tabRulesCount: document.getElementById("tabRulesCount"),

            // Tab 1: Filters & Table
            scoringSearchInput: document.getElementById("scoringSearchInput"),
            filterClassification: document.getElementById("filterClassification"),
            filterIndustry: document.getElementById("filterIndustry"),
            sortOrder: document.getElementById("sortOrder"),
            btnResetFilters: document.getElementById("btnResetFilters"),
            leadScoringTableBody: document.getElementById("leadScoringTableBody"),
            scoringEmptyState: document.getElementById("scoringEmptyState"),

            // Tab 2: Thresholds & Rules
            inputHotThreshold: document.getElementById("inputHotThreshold"),
            inputWarmThreshold: document.getElementById("inputWarmThreshold"),
            displayColdThreshold: document.getElementById("displayColdThreshold"),
            btnSaveThresholds: document.getElementById("btnSaveThresholds"),
            spectrumWarmMark: document.getElementById("spectrumWarmMark"),
            spectrumHotMark: document.getElementById("spectrumHotMark"),
            spectrumColdPart: document.getElementById("spectrumColdPart"),
            spectrumWarmPart: document.getElementById("spectrumWarmPart"),
            spectrumHotPart: document.getElementById("spectrumHotPart"),
            ruleGroupsGrid: document.getElementById("ruleGroupsGrid"),
            btnResetDefaultRules: document.getElementById("btnResetDefaultRules"),
            btnOpenAddCriteriaGroup: document.getElementById("btnOpenAddCriteriaGroup"),

            // Header Actions
            btnAddCriteriaBtn: document.getElementById("btnAddCriteriaBtn"),
            btnReScoreAll: document.getElementById("btnReScoreAll"),

            // Modals
            criteriaModal: document.getElementById("criteriaModal"),
            criteriaModalTitle: document.getElementById("criteriaModalTitle"),
            criteriaForm: document.getElementById("criteriaForm"),
            criteriaId: document.getElementById("criteriaId"),
            criteriaGroup: document.getElementById("criteriaGroup"),
            criteriaName: document.getElementById("criteriaName"),
            criteriaField: document.getElementById("criteriaField"),
            criteriaOperator: document.getElementById("criteriaOperator"),
            criteriaValue: document.getElementById("criteriaValue"),
            criteriaPoints: document.getElementById("criteriaPoints"),
            pointsHintBadge: document.getElementById("pointsHintBadge"),
            criteriaDescription: document.getElementById("criteriaDescription"),
            criteriaIsActive: document.getElementById("criteriaIsActive"),
            btnCloseCriteriaModal: document.getElementById("btnCloseCriteriaModal"),
            btnCancelCriteria: document.getElementById("btnCancelCriteria"),
            btnSaveCriteria: document.getElementById("btnSaveCriteria"),

            breakdownModal: document.getElementById("breakdownModal"),
            breakdownModalBody: document.getElementById("breakdownModalBody"),
            btnCloseBreakdownModal: document.getElementById("btnCloseBreakdownModal"),
            btnCloseBreakdownBtn: document.getElementById("btnCloseBreakdownBtn"),
            btnActionFromBreakdown: document.getElementById("btnActionFromBreakdown"),

            toastContainer: document.getElementById("leadScoringToastContainer")
        };
    }

    // 4. BUSINESS LOGIC: SCORING ENGINE
    function calculateLeadScore(lead, rules, thresholds) {
        let totalScore = 0;
        const breakdown = [];

        // Match against active rules
        rules.forEach(rule => {
            if (!rule.isActive) return;

            let matched = false;
            const fieldValue = lead[rule.field];

            if (fieldValue !== undefined && fieldValue !== null) {
                if (rule.operator === "EQUALS") {
                    matched = String(fieldValue).trim().toLowerCase() === String(rule.value).trim().toLowerCase();
                } else if (rule.operator === "CONTAINS") {
                    matched = String(fieldValue).toLowerCase().includes(String(rule.value).toLowerCase());
                } else if (rule.operator === "GREATER_THAN") {
                    const numField = parseFloat(fieldValue) || 0;
                    const numTarget = parseFloat(rule.value) || 0;
                    matched = numField >= numTarget;
                } else if (rule.operator === "LESS_THAN") {
                    const numField = parseFloat(fieldValue) || 0;
                    const numTarget = parseFloat(rule.value) || 0;
                    matched = numField <= numTarget;
                }
            }

            if (matched) {
                totalScore += rule.points;
                breakdown.push({
                    ruleId: rule.id,
                    ruleName: rule.name,
                    group: rule.group,
                    points: rule.points,
                    actualValue: fieldValue,
                    description: rule.description
                });
            }
        });

        // Minimum score floor at 0
        totalScore = Math.max(0, totalScore);

        // Classify
        let classification = "COLD";
        if (totalScore >= thresholds.hot) {
            classification = "HOT";
        } else if (totalScore >= thresholds.warm) {
            classification = "WARM";
        } else {
            classification = "COLD";
        }

        return {
            ...lead,
            score: totalScore,
            classification: classification,
            breakdown: breakdown
        };
    }

    function recalculateAllLeads() {
        scoredLeads = scoredLeads.map(lead => calculateLeadScore(lead, scoringRules, scoringThresholds));
        saveLeadsToStorage();
        updateKpis();
        renderLeadsTable();
    }

    // 5. DATA PERSISTENCE & RESTful SERVICE LAYER
    function loadInitialData() {
        // Load Thresholds
        try {
            const rawThresh = localStorage.getItem(STORAGE_KEY_THRESHOLDS);
            scoringThresholds = rawThresh ? JSON.parse(rawThresh) : { ...DEFAULT_THRESHOLDS };
        } catch (_) {
            scoringThresholds = { ...DEFAULT_THRESHOLDS };
        }

        // Load Rules
        try {
            const rawRules = localStorage.getItem(STORAGE_KEY_RULES);
            scoringRules = rawRules ? JSON.parse(rawRules) : [...DEFAULT_RULES];
        } catch (_) {
            scoringRules = [...DEFAULT_RULES];
        }

        // Load Leads
        try {
            const rawLeads = localStorage.getItem(STORAGE_KEY_LEADS);
            scoredLeads = rawLeads ? JSON.parse(rawLeads) : [...DEFAULT_LEADS];
        } catch (_) {
            scoredLeads = [...DEFAULT_LEADS];
        }

        // Sync and score
        recalculateAllLeads();
    }

    function saveRulesToStorage() {
        try {
            localStorage.setItem(STORAGE_KEY_RULES, JSON.stringify(scoringRules));
        } catch (e) {
            console.error("Failed to save rules to localStorage", e);
        }
    }

    function saveThresholdsToStorage() {
        try {
            localStorage.setItem(STORAGE_KEY_THRESHOLDS, JSON.stringify(scoringThresholds));
        } catch (e) {
            console.error("Failed to save thresholds to localStorage", e);
        }
    }

    function saveLeadsToStorage() {
        try {
            localStorage.setItem(STORAGE_KEY_LEADS, JSON.stringify(scoredLeads));
        } catch (e) {
            console.error("Failed to save leads to localStorage", e);
        }
    }

    // RESTful API Connector (Contract with Nguyễn Văn Thắng)
    window.LeadScoringService = {
        async getScoringRules() {
            try {
                if (window.CrmApi) {
                    const data = await window.CrmApi.request("/api/leads/scoring-rules");
                    if (data && Array.isArray(data)) {
                        scoringRules = data;
                        saveRulesToStorage();
                        return data;
                    }
                }
            } catch (err) {
                console.warn("[CRM-119 API] /api/leads/scoring-rules unavailable, using local rules.", err);
            }
            return scoringRules;
        },

        async saveScoringRules(rules) {
            scoringRules = rules;
            saveRulesToStorage();
            try {
                if (window.CrmApi) {
                    await window.CrmApi.request("/api/leads/scoring-rules", {
                        method: "POST",
                        body: JSON.stringify(rules)
                    });
                }
            } catch (err) {
                console.warn("[CRM-119 API] POST /api/leads/scoring-rules unavailable, saved locally.", err);
            }
            return scoringRules;
        },

        async reScoreAll() {
            try {
                if (window.CrmApi) {
                    const res = await window.CrmApi.request("/api/leads/re-score", { method: "POST" });
                    if (res && Array.isArray(res.items)) {
                        scoredLeads = res.items;
                        saveLeadsToStorage();
                        return res;
                    }
                }
            } catch (err) {
                console.warn("[CRM-119 API] POST /api/leads/re-score unavailable, re-scoring client-side.", err);
            }
            recalculateAllLeads();
            return { success: true, count: scoredLeads.length };
        },

        async getScoredLeads(filter = {}) {
            try {
                if (window.CrmApi) {
                    const query = new URLSearchParams({ include_score: "true", ...filter });
                    const res = await window.CrmApi.request(`/api/leads?${query}`);
                    if (res && Array.isArray(res.items)) {
                        scoredLeads = res.items;
                        saveLeadsToStorage();
                        return res.items;
                    }
                }
            } catch (err) {
                console.warn("[CRM-119 API] GET /api/leads?include_score=true unavailable, using local leads.", err);
            }
            return scoredLeads;
        }
    };

    // 6. UI RENDERING: KPIS & SPECTRUM
    function updateKpis() {
        const total = scoredLeads.length;
        const hotCount = scoredLeads.filter(l => l.classification === "HOT").length;
        const warmCount = scoredLeads.filter(l => l.classification === "WARM").length;
        const coldCount = scoredLeads.filter(l => l.classification === "COLD").length;

        const sumScore = scoredLeads.reduce((acc, cur) => acc + (cur.score || 0), 0);
        const avgScore = total > 0 ? (sumScore / total).toFixed(1) : 0;

        if (dom.kpiTotalLeads) dom.kpiTotalLeads.textContent = total;
        if (dom.kpiAvgScore) dom.kpiAvgScore.textContent = `Điểm trung bình: ${avgScore} điểm`;
        if (dom.kpiHotLeads) dom.kpiHotLeads.textContent = `${hotCount} (${total > 0 ? ((hotCount / total) * 100).toFixed(0) : 0}%)`;
        if (dom.kpiWarmLeads) dom.kpiWarmLeads.textContent = `${warmCount} (${total > 0 ? ((warmCount / total) * 100).toFixed(0) : 0}%)`;
        if (dom.kpiColdLeads) dom.kpiColdLeads.textContent = `${coldCount} (${total > 0 ? ((coldCount / total) * 100).toFixed(0) : 0}%)`;

        if (dom.kpiHotThresholdBadge) dom.kpiHotThresholdBadge.textContent = `≥ ${scoringThresholds.hot}đ`;
        if (dom.kpiWarmThresholdBadge) dom.kpiWarmThresholdBadge.textContent = `${scoringThresholds.warm} - ${scoringThresholds.hot - 1}đ`;
        if (dom.kpiColdThresholdBadge) dom.kpiColdThresholdBadge.textContent = `< ${scoringThresholds.warm}đ`;

        if (dom.tabLeadsCount) dom.tabLeadsCount.textContent = total;
        if (dom.tabRulesCount) dom.tabRulesCount.textContent = scoringRules.length;

        updateSpectrumDisplay();
    }

    function updateSpectrumDisplay() {
        const hot = scoringThresholds.hot;
        const warm = scoringThresholds.warm;

        if (dom.inputHotThreshold) dom.inputHotThreshold.value = hot;
        if (dom.inputWarmThreshold) dom.inputWarmThreshold.value = warm;
        if (dom.displayColdThreshold) dom.displayColdThreshold.textContent = warm;

        if (dom.spectrumWarmMark) dom.spectrumWarmMark.textContent = `Ấm: ${warm}đ`;
        if (dom.spectrumHotMark) dom.spectrumHotMark.textContent = `Nóng: ${hot}đ`;

        // Calculate visual widths (out of 100)
        const coldWidth = Math.min(Math.max(warm, 15), 60);
        const warmWidth = Math.min(Math.max(hot - warm, 15), 60);
        const hotWidth = Math.max(100 - (coldWidth + warmWidth), 15);

        if (dom.spectrumColdPart) dom.spectrumColdPart.style.width = `${coldWidth}%`;
        if (dom.spectrumWarmPart) dom.spectrumWarmPart.style.width = `${warmWidth}%`;
        if (dom.spectrumHotPart) dom.spectrumHotPart.style.width = `${hotWidth}%`;
    }

    // 7. UI RENDERING: TAB 1 (LEADS TABLE)
    function getFilteredLeads() {
        const query = dom.scoringSearchInput ? dom.scoringSearchInput.value.trim().toLowerCase() : "";
        const classification = dom.filterClassification ? dom.filterClassification.value : "ALL";
        const industry = dom.filterIndustry ? dom.filterIndustry.value : "ALL";
        const sort = dom.sortOrder ? dom.sortOrder.value : "score_desc";

        return scoredLeads.filter(lead => {
            if (classification !== "ALL" && lead.classification !== classification) return false;
            if (industry !== "ALL" && !lead.industry.toLowerCase().includes(industry.toLowerCase())) return false;

            if (query) {
                const searchStr = `${lead.fullName} ${lead.phone} ${lead.email} ${lead.companyName} ${lead.title} ${lead.source}`.toLowerCase();
                if (!searchStr.includes(query)) return false;
            }
            return true;
        }).sort((a, b) => {
            if (sort === "score_desc") return b.score - a.score;
            if (sort === "score_asc") return a.score - b.score;
            if (sort === "name_asc") return a.fullName.localeCompare(b.fullName);
            if (sort === "newest") return new Date(b.createdAt) - new Date(a.createdAt);
            return 0;
        });
    }

    function renderLeadsTable() {
        if (!dom.leadScoringTableBody) return;

        const filtered = getFilteredLeads();

        if (filtered.length === 0) {
            dom.leadScoringTableBody.innerHTML = "";
            if (dom.scoringEmptyState) dom.scoringEmptyState.style.display = "block";
            return;
        }

        if (dom.scoringEmptyState) dom.scoringEmptyState.style.display = "none";

        let html = "";
        filtered.forEach((lead, idx) => {
            const classBadge = getClassificationBadge(lead.classification);
            const scorePercent = Math.min(Math.round((lead.score / 100) * 100), 100);
            const classColor = lead.classification.toLowerCase();

            html += `
                <tr data-lead-id="${escapeHtml(lead.id)}">
                    <td class="col-seq">${idx + 1}</td>

                    <!-- Lead Identity -->
                    <td class="col-lead">
                        <div class="lead-person-cell">
                            <div class="lead-avatar-bubble">${escapeHtml(lead.avatar || "LD")}</div>
                            <div>
                                <div class="lead-person-name">
                                    <strong>${escapeHtml(lead.fullName)}</strong>
                                </div>
                                <div class="lead-person-title">${escapeHtml(lead.title || "Khách hàng")}</div>
                                <div class="lead-contact-line">
                                    <span><i class="fa-solid fa-phone text-primary"></i> ${escapeHtml(lead.phone)}</span>
                                    <span><i class="fa-solid fa-envelope text-primary"></i> ${escapeHtml(lead.email)}</span>
                                </div>
                            </div>
                        </div>
                    </td>

                    <!-- Company & Size -->
                    <td class="col-company">
                        <div class="company-cell-name">${escapeHtml(lead.companyName)}</div>
                        <div class="company-cell-meta">
                            <span><i class="fa-solid fa-building text-purple"></i> ${escapeHtml(lead.industry)}</span>
                            <span>&bull;</span>
                            <span><i class="fa-solid fa-users text-primary"></i> ${lead.employeeCount} nhân sự</span>
                        </div>
                    </td>

                    <!-- Source & Activity -->
                    <td class="col-source">
                        <div style="font-weight: 600; font-size: 12.5px; color: var(--crm-text);">
                            <i class="fa-solid fa-bullhorn text-warning"></i> ${escapeHtml(lead.source)}
                        </div>
                        <div style="font-size: 11.5px; color: var(--crm-muted); margin-top: 3px;">
                            ${escapeHtml(lead.recentActivity)}
                        </div>
                    </td>

                    <!-- Quality Score -->
                    <td class="col-score">
                        <div class="score-meter-wrap">
                            <div class="score-meter-head">
                                <span class="score-value-bold">${lead.score} <small style="font-size: 11px; font-weight: 600;">điểm</small></span>
                                <span class="score-scale-max">/ 100+</span>
                            </div>
                            <div class="score-progress-bar">
                                <div class="score-progress-fill ${classColor}" style="width: ${scorePercent}%;"></div>
                            </div>
                        </div>
                    </td>

                    <!-- Classification -->
                    <td class="col-class">
                        ${classBadge}
                    </td>

                    <!-- Score Breakdown Details -->
                    <td class="col-breakdown">
                        <button type="button" class="btn-view-breakdown" data-lead-id="${escapeHtml(lead.id)}" title="Xem lý do cộng/trừ điểm">
                            <i class="fa-solid fa-magnifying-glass-chart"></i> Chi tiết
                        </button>
                    </td>

                    <!-- Actions -->
                    <td class="col-actions">
                        <div class="table-lead-actions">
                            <button type="button" class="btn-call" data-lead-id="${escapeHtml(lead.id)}" data-phone="${escapeHtml(lead.phone)}" title="Gọi điện cho Lead ngay">
                                <i class="fa-solid fa-phone"></i>
                            </button>
                            <button type="button" class="btn-convert" data-lead-id="${escapeHtml(lead.id)}" title="Tạo cơ hội bán hàng từ Lead này">
                                <i class="fa-solid fa-bullseye"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        });

        dom.leadScoringTableBody.innerHTML = html;
        attachTableEvents();
    }

    function getClassificationBadge(classification) {
        if (classification === "HOT") {
            return `<span class="class-badge hot"><i class="fa-solid fa-fire-flame-curved"></i> Hot Lead</span>`;
        }
        if (classification === "WARM") {
            return `<span class="class-badge warm"><i class="fa-solid fa-cloud-sun"></i> Warm Lead</span>`;
        }
        return `<span class="class-badge cold"><i class="fa-solid fa-snowflake"></i> Cold Lead</span>`;
    }

    function attachTableEvents() {
        // View Breakdown
        const breakdownButtons = dom.leadScoringTableBody.querySelectorAll(".btn-view-breakdown");
        breakdownButtons.forEach(btn => {
            btn.addEventListener("click", () => {
                const leadId = btn.getAttribute("data-lead-id");
                openBreakdownModal(leadId);
            });
        });

        // Quick Call
        const callButtons = dom.leadScoringTableBody.querySelectorAll(".btn-call");
        callButtons.forEach(btn => {
            btn.addEventListener("click", () => {
                const phone = btn.getAttribute("data-phone");
                showToast(`📞 Đang kích hoạt cuộc gọi thoại tới số ${phone}...`, "info");
            });
        });

        // Convert to opportunity
        const convertButtons = dom.leadScoringTableBody.querySelectorAll(".btn-convert");
        convertButtons.forEach(btn => {
            btn.addEventListener("click", () => {
                const leadId = btn.getAttribute("data-lead-id");
                const targetLead = scoredLeads.find(l => l.id === leadId);
                showToast(`🎯 Chuyển đổi thành công Lead "${targetLead?.fullName}" sang Cơ hội bán hàng!`, "success");
            });
        });
    }

    // 8. UI RENDERING: TAB 2 (CRITERIA RULES GROUPS)
    const GROUP_CONFIGS = {
        INDUSTRY: {
            title: "Ngành nghề phù hợp (Industry Fit)",
            icon: "fa-building",
            description: "Cộng điểm ưu tiên cho các khách hàng thuộc lĩnh vực then chốt."
        },
        COMPANY_SIZE: {
            title: "Quy mô Doanh nghiệp (Company Size)",
            icon: "fa-users",
            description: "Đánh giá mức độ tiềm năng theo số lượng nhân sự."
        },
        SOURCE: {
            title: "Nguồn tiếp cận Lead (Lead Source)",
            icon: "fa-bullhorn",
            description: "Thưởng điểm cho kênh giới thiệu, phạt điểm cho data mua ngoài."
        },
        INTENT: {
            title: "Mức độ quan tâm & Hành vi (Engagement & Intent)",
            icon: "fa-bolt",
            description: "Ghi nhận mức độ khẩn thiết qua việc yêu cầu demo, khảo giá hoặc bỏ tương tác."
        },
        CUSTOM: {
            title: "Tiêu chí mở rộng khác (Custom Criteria)",
            icon: "fa-sliders",
            description: "Các quy tắc nghiệp vụ bổ sung theo chiến dịch bán hàng."
        }
    };

    function renderRulesGrid() {
        if (!dom.ruleGroupsGrid) return;

        // Group rules by group
        const grouped = {
            INDUSTRY: [],
            COMPANY_SIZE: [],
            SOURCE: [],
            INTENT: [],
            CUSTOM: []
        };

        scoringRules.forEach(rule => {
            const grp = rule.group || "CUSTOM";
            if (!grouped[grp]) grouped[grp] = [];
            grouped[grp].push(rule);
        });

        let html = "";
        Object.keys(GROUP_CONFIGS).forEach(groupKey => {
            const config = GROUP_CONFIGS[groupKey];
            const rules = grouped[groupKey] || [];
            if (groupKey === "CUSTOM" && rules.length === 0) return; // Hide custom if empty

            html += `
                <div class="group-card" data-group="${groupKey}">
                    <div class="group-card-head">
                        <div class="group-card-title">
                            <i class="fa-solid ${config.icon}"></i>
                            ${config.title}
                            <span class="tab-badge" style="font-size: 11px;">${rules.filter(r => r.isActive).length}/${rules.length} kích hoạt</span>
                        </div>
                        <button type="button" class="btn-add-to-group" data-group="${groupKey}">
                            <i class="fa-solid fa-plus"></i> Thêm tiêu chí
                        </button>
                    </div>

                    <div class="group-criteria-list">
                        ${rules.length === 0 ? `
                            <p style="font-size: 12px; color: var(--crm-muted); text-align: center; margin: 16px 0;">Chưa có tiêu chí nào trong nhóm này.</p>
                        ` : ""}

                        ${rules.map(rule => {
                            const isPositive = rule.points > 0;
                            const isZero = rule.points === 0;
                            const pillClass = isZero ? "zero" : (isPositive ? "positive" : "negative");
                            const pointsText = isPositive ? `+${rule.points}đ` : `${rule.points}đ`;

                            return `
                                <div class="criteria-item-row ${rule.isActive ? "" : "inactive"}" data-rule-id="${escapeHtml(rule.id)}">
                                    <div class="criteria-item-left">
                                        <div class="criteria-item-name" title="${escapeHtml(rule.name)}">${escapeHtml(rule.name)}</div>
                                        <div class="criteria-item-condition">${rule.field} ${rule.operator} "${escapeHtml(rule.value)}"</div>
                                    </div>
                                    <div class="criteria-item-right">
                                        <span class="points-pill ${pillClass}">${pointsText}</span>
                                        
                                        <label class="toggle-switch-label" title="${rule.isActive ? "Đang bật" : "Đang tắt"}">
                                            <input type="checkbox" class="rule-active-toggle" data-rule-id="${escapeHtml(rule.id)}" ${rule.isActive ? "checked" : ""}>
                                            <span class="toggle-slider"></span>
                                        </label>

                                        <button type="button" class="btn-criteria-action edit btn-edit-rule" data-rule-id="${escapeHtml(rule.id)}" title="Sửa tiêu chí">
                                            <i class="fa-solid fa-pen-to-square"></i>
                                        </button>
                                        <button type="button" class="btn-criteria-action delete btn-delete-rule" data-rule-id="${escapeHtml(rule.id)}" title="Xóa tiêu chí">
                                            <i class="fa-solid fa-trash-can"></i>
                                        </button>
                                    </div>
                                </div>
                            `;
                        }).join("")}
                    </div>
                </div>
            `;
        });

        dom.ruleGroupsGrid.innerHTML = html;
        attachRuleEvents();
    }

    function attachRuleEvents() {
        // Add to group
        const addGroupButtons = dom.ruleGroupsGrid.querySelectorAll(".btn-add-to-group");
        addGroupButtons.forEach(btn => {
            btn.addEventListener("click", () => {
                const group = btn.getAttribute("data-group");
                openCriteriaModal(null, group);
            });
        });

        // Toggle active status
        const toggleCheckboxes = dom.ruleGroupsGrid.querySelectorAll(".rule-active-toggle");
        toggleCheckboxes.forEach(chk => {
            chk.addEventListener("change", () => {
                const ruleId = chk.getAttribute("data-rule-id");
                const rule = scoringRules.find(r => r.id === ruleId);
                if (rule) {
                    rule.isActive = chk.checked;
                    saveRulesToStorage();
                    recalculateAllLeads();
                    renderRulesGrid();
                    showToast(`Đã ${rule.isActive ? "kích hoạt" : "vô hiệu hóa"} tiêu chí "${rule.name}"`, "info");
                }
            });
        });

        // Edit rule
        const editButtons = dom.ruleGroupsGrid.querySelectorAll(".btn-edit-rule");
        editButtons.forEach(btn => {
            btn.addEventListener("click", () => {
                const ruleId = btn.getAttribute("data-rule-id");
                openCriteriaModal(ruleId);
            });
        });

        // Delete rule
        const deleteButtons = dom.ruleGroupsGrid.querySelectorAll(".btn-delete-rule");
        deleteButtons.forEach(btn => {
            btn.addEventListener("click", () => {
                const ruleId = btn.getAttribute("data-rule-id");
                const rule = scoringRules.find(r => r.id === ruleId);
                if (confirm(`Bạn có chắc chắn muốn xóa tiêu chí "${rule?.name}" không?`)) {
                    scoringRules = scoringRules.filter(r => r.id !== ruleId);
                    saveRulesToStorage();
                    recalculateAllLeads();
                    renderRulesGrid();
                    showToast(`Đã xóa tiêu chí "${rule?.name}" thành công!`, "success");
                }
            });
        });
    }

    // 9. MODALS: CRITERIA MODAL (ADD / EDIT)
    function openCriteriaModal(ruleId = null, prefillGroup = null) {
        editingCriteriaId = ruleId;

        if (ruleId) {
            const rule = scoringRules.find(r => r.id === ruleId);
            if (!rule) return;
            if (dom.criteriaModalTitle) dom.criteriaModalTitle.innerHTML = `<i class="fa-solid fa-pen-to-square text-primary"></i> Chỉnh sửa tiêu chí chấm điểm`;
            if (dom.criteriaId) dom.criteriaId.value = rule.id;
            if (dom.criteriaGroup) dom.criteriaGroup.value = rule.group;
            if (dom.criteriaName) dom.criteriaName.value = rule.name;
            if (dom.criteriaField) dom.criteriaField.value = rule.field;
            if (dom.criteriaOperator) dom.criteriaOperator.value = rule.operator;
            if (dom.criteriaValue) dom.criteriaValue.value = rule.value;
            if (dom.criteriaPoints) dom.criteriaPoints.value = rule.points;
            if (dom.criteriaDescription) dom.criteriaDescription.value = rule.description || "";
            if (dom.criteriaIsActive) dom.criteriaIsActive.checked = rule.isActive !== false;
        } else {
            if (dom.criteriaModalTitle) dom.criteriaModalTitle.innerHTML = `<i class="fa-solid fa-plus-circle text-primary"></i> Thêm tiêu chí chấm điểm mới`;
            if (dom.criteriaForm) dom.criteriaForm.reset();
            if (dom.criteriaId) dom.criteriaId.value = "";
            if (dom.criteriaGroup) dom.criteriaGroup.value = prefillGroup || "INDUSTRY";
            if (dom.criteriaPoints) dom.criteriaPoints.value = "15";
            if (dom.criteriaIsActive) dom.criteriaIsActive.checked = true;
        }

        updatePointsHint();
        if (dom.criteriaModal) dom.criteriaModal.classList.add("active");
    }

    function closeCriteriaModal() {
        if (dom.criteriaModal) dom.criteriaModal.classList.remove("active");
        editingCriteriaId = null;
    }

    function updatePointsHint() {
        if (!dom.criteriaPoints || !dom.pointsHintBadge) return;
        const val = parseInt(dom.criteriaPoints.value, 10) || 0;
        dom.pointsHintBadge.textContent = val >= 0 ? `+${val}đ` : `${val}đ`;
        dom.pointsHintBadge.style.color = val > 0 ? "#15803d" : (val < 0 ? "#b91c1c" : "var(--crm-text)");
    }

    function saveCriteriaFromModal() {
        const name = dom.criteriaName ? dom.criteriaName.value.trim() : "";
        const group = dom.criteriaGroup ? dom.criteriaGroup.value : "INDUSTRY";
        const field = dom.criteriaField ? dom.criteriaField.value : "industry";
        const operator = dom.criteriaOperator ? dom.criteriaOperator.value : "EQUALS";
        const value = dom.criteriaValue ? dom.criteriaValue.value.trim() : "";
        const points = parseInt(dom.criteriaPoints.value, 10) || 0;
        const description = dom.criteriaDescription ? dom.criteriaDescription.value.trim() : "";
        const isActive = dom.criteriaIsActive ? dom.criteriaIsActive.checked : true;

        if (!name || !value) {
            alert("Vui lòng điền đầy đủ Tên tiêu chí và Giá trị so sánh!");
            return;
        }

        if (editingCriteriaId) {
            const rule = scoringRules.find(r => r.id === editingCriteriaId);
            if (rule) {
                rule.name = name;
                rule.group = group;
                rule.field = field;
                rule.operator = operator;
                rule.value = value;
                rule.points = points;
                rule.description = description;
                rule.isActive = isActive;
            }
            showToast(`✨ Đã cập nhật tiêu chí "${name}" thành công!`, "success");
        } else {
            const newRule = {
                id: `R-CUST-${Date.now().toString().slice(-5)}`,
                name,
                group,
                field,
                operator,
                value,
                points,
                description,
                isActive
            };
            scoringRules.push(newRule);
            showToast(`✨ Đã thêm tiêu chí mới "${name}" thành công!`, "success");
        }

        saveRulesToStorage();
        closeCriteriaModal();
        recalculateAllLeads();
        renderRulesGrid();
    }

    // 10. MODALS: SCORE BREAKDOWN MODAL
    function openBreakdownModal(leadId) {
        const lead = scoredLeads.find(l => l.id === leadId);
        if (!lead || !dom.breakdownModalBody) return;

        const classBadge = getClassificationBadge(lead.classification);
        const recommendation = lead.classification === "HOT"
            ? "🔥 <strong>Khuyến nghị Sales:</strong> Lead Nóng có độ quan tâm và tính phù hợp rất cao. Hãy gọi điện thoại trực tiếp trong ca làm việc để chốt cuộc hẹn demo hoặc ký hợp đồng."
            : (lead.classification === "WARM"
                ? "🌤️ <strong>Khuyến nghị Sales:</strong> Lead Ấm có tiềm năng tốt nhưng cần thêm tài liệu. Hãy gửi email case study, hồ sơ giải pháp và xếp lịch follow-up sau 3 ngày."
                : "❄️ <strong>Khuyến nghị Sales:</strong> Lead Lạnh chưa có nhu cầu cấp bách. Tạm thời đưa vào chuỗi email marketing nuôi dưỡng tự động hàng tuần.");

        let breakdownRows = "";
        if (lead.breakdown && lead.breakdown.length > 0) {
            lead.breakdown.forEach(item => {
                const isPos = item.points > 0;
                const pillClass = item.points === 0 ? "zero" : (isPos ? "positive" : "negative");
                const pointsStr = isPos ? `+${item.points}đ` : `${item.points}đ`;
                const groupTitle = GROUP_CONFIGS[item.group] ? GROUP_CONFIGS[item.group].title.split("(")[0].trim() : item.group;

                breakdownRows += `
                    <tr>
                        <td><span style="font-size: 11px; font-weight: 700; color: var(--crm-primary);">${escapeHtml(groupTitle)}</span></td>
                        <td>
                            <strong>${escapeHtml(item.ruleName)}</strong>
                            <div style="font-size: 11px; color: var(--crm-muted);">${escapeHtml(item.description || "")}</div>
                        </td>
                        <td><code>${escapeHtml(String(item.actualValue || ""))}</code></td>
                        <td style="text-align: right;"><span class="points-pill ${pillClass}">${pointsStr}</span></td>
                    </tr>
                `;
            });
        } else {
            breakdownRows = `
                <tr>
                    <td colspan="4" style="text-align: center; color: var(--crm-muted); padding: 18px;">
                        Lead này chưa khớp với bất kỳ tiêu chí cộng điểm nào đang được kích hoạt.
                    </td>
                </tr>
            `;
        }

        dom.breakdownModalBody.innerHTML = `
            <div class="breakdown-lead-header">
                <div class="breakdown-lead-identity">
                    <div class="lead-avatar-bubble" style="width: 44px; height: 44px; font-size: 15px;">${escapeHtml(lead.avatar || "LD")}</div>
                    <div>
                        <div style="font-size: 15px; font-weight: 800; color: var(--crm-text);">${escapeHtml(lead.fullName)}</div>
                        <div style="font-size: 12px; color: var(--crm-muted);">${escapeHtml(lead.title)} &bull; ${escapeHtml(lead.companyName)}</div>
                        <div style="font-size: 11.5px; color: var(--crm-muted); margin-top: 2px;">
                            <i class="fa-solid fa-phone text-primary"></i> ${escapeHtml(lead.phone)} | <i class="fa-solid fa-envelope text-primary"></i> ${escapeHtml(lead.email)}
                        </div>
                    </div>
                </div>

                <div class="breakdown-score-badge-large">
                    <div style="font-size: 28px; font-weight: 800; color: var(--crm-text); line-height: 1;">${lead.score} <small style="font-size: 13px;">điểm</small></div>
                    <div style="margin-top: 6px;">${classBadge}</div>
                </div>
            </div>

            <div style="margin-bottom: 12px; font-size: 13px; color: var(--crm-text-2);">
                Chi tiết các điều kiện tiêu chuẩn và điểm số được tính lũy kế:
            </div>

            <table class="breakdown-table">
                <thead>
                    <tr>
                        <th style="width: 130px;">Nhóm tiêu chí</th>
                        <th>Quy tắc khớp</th>
                        <th style="width: 140px;">Dữ liệu của Lead</th>
                        <th style="width: 80px; text-align: right;">Điểm</th>
                    </tr>
                </thead>
                <tbody>
                    ${breakdownRows}
                </tbody>
                <tfoot>
                    <tr class="breakdown-total-row">
                        <td colspan="3" style="text-align: right; padding: 12px;">TỔNG ĐIỂM CHẤT LƯỢNG LEAD:</td>
                        <td style="text-align: right; font-size: 15px; color: var(--crm-primary); padding: 12px;">${lead.score}đ</td>
                    </tr>
                </tfoot>
            </table>

            <div style="margin-top: 14px; padding: 12px 14px; border-radius: var(--crm-radius-sm); background: var(--crm-surface-soft); border-left: 4px solid var(--crm-primary); font-size: 12.5px; line-height: 1.5;">
                ${recommendation}
            </div>
        `;

        if (dom.btnActionFromBreakdown) {
            dom.btnActionFromBreakdown.onclick = () => {
                showToast(`📞 Đang quay số gọi tới ${lead.fullName} (${lead.phone})...`, "info");
                closeBreakdownModal();
            };
        }

        if (dom.breakdownModal) dom.breakdownModal.classList.add("active");
    }

    function closeBreakdownModal() {
        if (dom.breakdownModal) dom.breakdownModal.classList.remove("active");
    }

    // 11. TOAST NOTIFICATION UTILITY
    function showToast(message, type = "info") {
        if (!dom.toastContainer) return;

        const toast = document.createElement("div");
        toast.className = `scoring-toast ${type}`;

        const iconClass = type === "success"
            ? "fa-circle-check"
            : (type === "warning" ? "fa-triangle-exclamation" : (type === "error" ? "fa-circle-xmark" : "fa-circle-info"));

        toast.innerHTML = `
            <i class="fa-solid ${iconClass}"></i>
            <span>${escapeHtml(message)}</span>
        `;

        dom.toastContainer.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = "0";
            toast.style.transform = "translateY(10px)";
            toast.style.transition = "all 0.25s ease";
            setTimeout(() => toast.remove(), 250);
        }, 3500);
    }

    function escapeHtml(str) {
        if (!str) return "";
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    // 12. EVENT LISTENERS SETUP
    function setupEventListeners() {
        // Tab switching
        if (dom.tabRankingBtn && dom.tabRulesBtn) {
            dom.tabRankingBtn.addEventListener("click", () => switchTab("ranking"));
            dom.tabRulesBtn.addEventListener("click", () => switchTab("rules"));
        }

        // Filters in Tab 1
        if (dom.scoringSearchInput) {
            dom.scoringSearchInput.addEventListener("input", () => renderLeadsTable());
        }
        if (dom.filterClassification) {
            dom.filterClassification.addEventListener("change", () => renderLeadsTable());
        }
        if (dom.filterIndustry) {
            dom.filterIndustry.addEventListener("change", () => renderLeadsTable());
        }
        if (dom.sortOrder) {
            dom.sortOrder.addEventListener("change", () => renderLeadsTable());
        }
        if (dom.btnResetFilters) {
            dom.btnResetFilters.addEventListener("click", () => {
                if (dom.scoringSearchInput) dom.scoringSearchInput.value = "";
                if (dom.filterClassification) dom.filterClassification.value = "ALL";
                if (dom.filterIndustry) dom.filterIndustry.value = "ALL";
                if (dom.sortOrder) dom.sortOrder.value = "score_desc";
                renderLeadsTable();
                showToast("Đã đặt lại toàn bộ bộ lọc!", "info");
            });
        }

        // Save Thresholds in Tab 2
        if (dom.btnSaveThresholds) {
            dom.btnSaveThresholds.addEventListener("click", () => {
                const hotVal = parseInt(dom.inputHotThreshold?.value, 10) || 70;
                const warmVal = parseInt(dom.inputWarmThreshold?.value, 10) || 40;

                if (warmVal >= hotVal) {
                    alert("Ngưỡng điểm Lead Ấm phải nhỏ hơn ngưỡng Lead Nóng!");
                    return;
                }

                scoringThresholds.hot = hotVal;
                scoringThresholds.warm = warmVal;
                saveThresholdsToStorage();
                recalculateAllLeads();
                showToast(`💾 Đã lưu cấu hình ngưỡng điểm: Nóng ≥ ${hotVal}đ, Ấm: ${warmVal} - ${hotVal - 1}đ!`, "success");
            });
        }

        // Reset default rules
        if (dom.btnResetDefaultRules) {
            dom.btnResetDefaultRules.addEventListener("click", () => {
                if (confirm("Bạn có chắc chắn muốn khôi phục danh sách tiêu chí về mặc định của hệ thống không?")) {
                    scoringRules = [...DEFAULT_RULES];
                    scoringThresholds = { ...DEFAULT_THRESHOLDS };
                    saveRulesToStorage();
                    saveThresholdsToStorage();
                    recalculateAllLeads();
                    renderRulesGrid();
                    showToast("✨ Đã khôi phục toàn bộ tiêu chí chuẩn!", "success");
                }
            });
        }

        // Open Add Criteria Modal buttons
        if (dom.btnAddCriteriaBtn) {
            dom.btnAddCriteriaBtn.addEventListener("click", () => openCriteriaModal());
        }
        if (dom.btnOpenAddCriteriaGroup) {
            dom.btnOpenAddCriteriaGroup.addEventListener("click", () => openCriteriaModal());
        }

        // Criteria Modal close & save
        if (dom.btnCloseCriteriaModal) dom.btnCloseCriteriaModal.addEventListener("click", closeCriteriaModal);
        if (dom.btnCancelCriteria) dom.btnCancelCriteria.addEventListener("click", closeCriteriaModal);
        if (dom.btnSaveCriteria) dom.btnSaveCriteria.addEventListener("click", saveCriteriaFromModal);
        if (dom.criteriaPoints) dom.criteriaPoints.addEventListener("input", updatePointsHint);

        // Breakdown Modal close
        if (dom.btnCloseBreakdownModal) dom.btnCloseBreakdownModal.addEventListener("click", closeBreakdownModal);
        if (dom.btnCloseBreakdownBtn) dom.btnCloseBreakdownBtn.addEventListener("click", closeBreakdownModal);

        // Backdrop click to close
        if (dom.criteriaModal) {
            dom.criteriaModal.addEventListener("click", (e) => {
                if (e.target === dom.criteriaModal) closeCriteriaModal();
            });
        }
        if (dom.breakdownModal) {
            dom.breakdownModal.addEventListener("click", (e) => {
                if (e.target === dom.breakdownModal) closeBreakdownModal();
            });
        }

        // Re-score All (Simulation & API call)
        if (dom.btnReScoreAll) {
            dom.btnReScoreAll.addEventListener("click", handleReScoreAll);
        }

        // Escape key to close modals
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") {
                closeCriteriaModal();
                closeBreakdownModal();
            }
        });
    }

    function switchTab(tabName) {
        currentTab = tabName;
        if (tabName === "ranking") {
            if (dom.tabRankingBtn) dom.tabRankingBtn.classList.add("active");
            if (dom.tabRulesBtn) dom.tabRulesBtn.classList.remove("active");
            if (dom.paneRanking) dom.paneRanking.classList.add("active");
            if (dom.paneRules) dom.paneRules.classList.remove("active");
            renderLeadsTable();
        } else {
            if (dom.tabRankingBtn) dom.tabRankingBtn.classList.remove("active");
            if (dom.tabRulesBtn) dom.tabRulesBtn.classList.add("active");
            if (dom.paneRanking) dom.paneRanking.classList.remove("active");
            if (dom.paneRules) dom.paneRules.classList.add("active");
            renderRulesGrid();
            updateSpectrumDisplay();
        }
    }

    async function handleReScoreAll() {
        if (!dom.btnReScoreAll) return;
        const btn = dom.btnReScoreAll;
        const originalHtml = btn.innerHTML;

        btn.disabled = true;
        btn.innerHTML = `<i class="fa-solid fa-circle-notch fa-spin"></i> Đang tính toán lại...`;

        try {
            // Call API Service
            await window.LeadScoringService.reScoreAll();
            setTimeout(() => {
                btn.disabled = false;
                btn.innerHTML = originalHtml;
                showToast(`⚡ Đã hoàn tất chấm điểm tự động cho ${scoredLeads.length} Lead theo quy tắc mới nhất!`, "success");
            }, 600);
        } catch (err) {
            btn.disabled = false;
            btn.innerHTML = originalHtml;
            showToast("Có lỗi khi chấm điểm lại Lead!", "error");
        }
    }

    // 13. BOOTSTRAP INITIALIZATION
    document.addEventListener("DOMContentLoaded", () => {
        initDom();
        loadInitialData();
        renderRulesGrid();
        renderLeadsTable();
        setupEventListeners();
    });

})();
