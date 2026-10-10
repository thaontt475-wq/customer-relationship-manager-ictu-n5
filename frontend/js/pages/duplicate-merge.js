/**
 * ===================================================================
 * ENTERPRISE DUPLICATE DETECTION & DATA MERGE ENGINE (CRM-64)
 * - Tự động phát hiện trùng lặp dựa trên 3 tiêu chí:
 *   1. Mã số thuế (MST trùng 100% sau chuẩn hóa)
 *   2. Tên công ty gần giống (Fuzzy comparison / substring / token match)
 *   3. Website trùng lặp (Domain normalization)
 * - Quản lý Modal so sánh 2 cột song song (Side-by-Side Comparison)
 * - Chọn Hồ sơ gốc (Master Record) và chọn từng trường thông tin ưu tiên
 * - Cơ chế gộp dữ liệu: chuyển giao toàn bộ liên hệ, deals, activities
 * - Phân quyền: Trưởng nhóm kinh doanh (Team Lead / Admin) được phép gộp;
 *   Nhân viên Sale chỉ được xem đối chiếu.
 * - Lưu trữ bền vững Client-side: CRM_CUSTOMERS_DATA & CRM_DUPLICATE_MERGE_LOGS
 * ===================================================================
 */

"use strict";

(function (root, factory) {
    if (typeof define === "function" && define.amd) {
        define([], factory);
    } else if (typeof module === "object" && module.exports) {
        module.exports = factory();
    } else {
        root.DuplicateMergeEngine = factory();
    }
})(typeof self !== "undefined" ? self : this, function () {

    const STORAGE_KEY_CUSTOMERS = "CRM_CUSTOMERS_DATA";
    const STORAGE_KEY_MERGE_LOGS = "CRM_DUPLICATE_MERGE_LOGS";
    const STORAGE_KEY_ACTIVE_ROLE = "CRM_SIMULATED_ROLE"; // TEAM_LEAD | SALE

    /* =========================================================
       1. INITIAL SEED DATA WITH DELIBERATE DUPLICATES (FOR CRM-64)
    ========================================================= */
    const DEFAULT_SEED_CUSTOMERS = [
        // --- Cặp trùng 1: Trùng 100% Mã số thuế (MST: 0101234567) ---
        {
            id: 1,
            name: "Công ty Cổ phần Công nghệ FPT",
            companyName: "Công ty Cổ phần Công nghệ FPT",
            taxCode: "0101234567",
            status: "TIEM_NANG",
            industryId: 1,
            companySizeId: 13,
            email: "contact@fpt.com.vn",
            phone: "02473007300",
            website: "https://fpt.com.vn",
            address: "Tòa nhà FPT, Phố Duy Tân, Dịch Vọng Hậu, Cầu Giấy, Hà Nội",
            ownerUserId: 101,
            ownerName: "Nguyễn Văn An",
            revenue: "50 tỷ VNĐ",
            contacts: [
                { id: 11, name: "Nguyễn Văn Khoa", role: "Tổng Giám Đốc", phone: "0903123456", email: "khoanv@fpt.com.vn" },
                { id: 12, name: "Trương Gia Bình", role: "Chủ tịch HĐQT", phone: "0903999888", email: "binhtg@fpt.com.vn" }
            ],
            deals: [
                { id: 101, title: "Hệ thống Quản lý Khách hàng Doanh nghiệp Cloud 2026", amount: 450000000, stage: "Đàm phán", probability: 70 }
            ],
            activities: [
                { id: 201, type: "Cuộc gọi", summary: "Trao đổi sơ bộ về quy mô triển khai và tích hợp SSO", date: "2026-10-02", user: "Nguyễn Văn An" }
            ],
            tickets: [
                { id: 301, subject: "Yêu cầu tài liệu API & kiểm thử môi trường Sandbox", priority: "Trung bình", status: "Đã xử lý" }
            ]
        },
        {
            id: 2,
            name: "FPT Software - Chi nhánh Cầu Giấy",
            companyName: "FPT Software - Chi nhánh Cầu Giấy",
            taxCode: "0101234567", // Trùng 100% MST
            status: "DANG_GIAO_DICH",
            industryId: 1,
            companySizeId: 13,
            email: "sales@fpt-software.com",
            phone: "02473008888",
            website: "https://fpt-software.com",
            address: "Tầng 6, FPT Tower, Số 10 Phạm Văn Bạch, Cầu Giấy, Hà Nội",
            ownerUserId: 102,
            ownerName: "Trần Thị Mai",
            revenue: "45 tỷ VNĐ",
            contacts: [
                { id: 13, name: "Phạm Minh Tuấn", role: "Phó Tổng Giám Đốc", phone: "0912345678", email: "tuanpm@fpt-software.com" }
            ],
            deals: [
                { id: 102, title: "Gói Dịch vụ DevOps & Microservices Migration", amount: 320000000, stage: "Đề xuất giải pháp", probability: 55 }
            ],
            activities: [
                { id: 202, type: "Gặp mặt", summary: "Demo giải pháp kiến trúc tại văn phòng FPT", date: "2026-10-04", user: "Trần Thị Mai" }
            ],
            tickets: []
        },

        // --- Cặp trùng 2: Trùng Tên miền Website (viettel.vn) & Tên tương tự ---
        {
            id: 3,
            name: "Tập đoàn Công nghiệp - Viễn thông Quân đội (Viettel)",
            companyName: "Tập đoàn Công nghiệp - Viễn thông Quân đội (Viettel)",
            taxCode: "0100109106",
            status: "CHINH_THUC",
            industryId: 1,
            companySizeId: 13,
            email: "info@viettel.vn",
            phone: "02462556789",
            website: "https://viettel.vn",
            address: "Số 1 Trần Hữu Dực, Mỹ Đình 2, Nam Từ Liêm, Hà Nội",
            ownerUserId: 103,
            ownerName: "Lê Hoàng Nam",
            revenue: "120 tỷ VNĐ",
            contacts: [
                { id: 14, name: "Tào Đức Thắng", role: "Chủ tịch kiêm Tổng Giám Đốc", phone: "0988111222", email: "thangtd@viettel.vn" }
            ],
            deals: [
                { id: 103, title: "Hợp đồng Cung cấp Hạ tầng Cloud & Server B2B", amount: 1200000000, stage: "Thành công", probability: 100 }
            ],
            activities: [
                { id: 203, type: "Email", summary: "Gửi biên bản nghiệm thu hợp đồng giai đoạn 1", date: "2026-09-28", user: "Lê Hoàng Nam" }
            ],
            tickets: [
                { id: 302, subject: "Cấu hình mở rộng băng thông IP tĩnh", priority: "Cao", status: "Đang xử lý" }
            ]
        },
        {
            id: 4,
            name: "Tổng Công ty Viễn thông Viettel - Viettel Telecom",
            companyName: "Tổng Công ty Viễn thông Viettel - Viettel Telecom",
            taxCode: "0100109106-001",
            status: "TIEM_NANG",
            industryId: 1,
            companySizeId: 13,
            email: "cskh@viettel.vn",
            phone: "02462660198",
            website: "http://www.viettel.vn", // Trùng normalized domain viettel.vn
            address: "Tòa nhà Viettel, Số 1 Giang Văn Minh, Ba Đình, Hà Nội",
            ownerUserId: 104,
            ownerName: "Phạm Thu Thảo",
            revenue: "110 tỷ VNĐ",
            contacts: [
                { id: 15, name: "Nguyễn Trọng Tính", role: "Phó Tổng Giám Đốc", phone: "0989222333", email: "tinhnt@viettel.vn" }
            ],
            deals: [
                { id: 104, title: "Dự án Nâng cấp Hệ thống Billing Doanh nghiệp", amount: 680000000, stage: "Liên hệ ban đầu", probability: 30 }
            ],
            activities: [
                { id: 204, type: "Cuộc gọi", summary: "Khảo sát nhu cầu sử dụng dịch vụ thanh toán", date: "2026-10-06", user: "Phạm Thu Thảo" }
            ],
            tickets: []
        },

        // --- Cặp trùng 3: Tên tương đồng cao (Fuzzy Name match & Website con) ---
        {
            id: 5,
            name: "Công ty Cổ phần MISA",
            companyName: "Công ty Cổ phần MISA",
            taxCode: "0101243150",
            status: "DANG_GIAO_DICH",
            industryId: 1,
            companySizeId: 12,
            email: "contact@misa.vn",
            phone: "02437959595",
            website: "misa.vn",
            address: "Tầng 9, Tòa nhà Technosoft, Phố Duy Tân, Dịch Vọng Hậu, Cầu Giấy, Hà Nội",
            ownerUserId: 100,
            ownerName: "Nông Quang Tiệp (Trưởng nhóm)",
            revenue: "30 tỷ VNĐ",
            contacts: [
                { id: 16, name: "Lữ Thành Long", role: "Chủ tịch HĐQT", phone: "0913222111", email: "longlt@misa.vn" }
            ],
            deals: [
                { id: 105, title: "Hợp tác Tích hợp Hóa đơn điện tử meInvoice", amount: 180000000, stage: "Đề xuất", probability: 60 }
            ],
            activities: [
                { id: 205, type: "Họp trực tuyến", summary: "Thống nhất phạm vi tích hợp API với nền tảng CRM", date: "2026-10-03", user: "Nông Quang Tiệp" }
            ],
            tickets: []
        },
        {
            id: 6,
            name: "Công ty Cổ phần Phần mềm Kế toán MISA",
            companyName: "Công ty Cổ phần Phần mềm Kế toán MISA",
            taxCode: "0101243150-002",
            status: "TIEM_NANG",
            industryId: 1,
            companySizeId: 12,
            email: "sales@misa.vn",
            phone: "02437959599",
            website: "https://www.misa.vn/phan-mem-ke-toan", // Trùng normalized domain misa.vn
            address: "Tòa nhà MISA, Lô 5 Công viên phần mềm Quang Trung, Quận 12, TP.HCM",
            ownerUserId: 105,
            ownerName: "Vũ Đức Hùng",
            revenue: "28 tỷ VNĐ",
            contacts: [
                { id: 17, name: "Đinh Thị Huyền", role: "Trưởng phòng Kinh doanh", phone: "0934112233", email: "huyendt@misa.vn" }
            ],
            deals: [
                { id: 106, title: "Triển khai phần mềm ERP cho chuỗi cửa hàng", amount: 210000000, stage: "Khảo sát", probability: 40 }
            ],
            activities: [
                { id: 206, type: "Cuộc gọi", summary: "Tư vấn gói giải pháp doanh nghiệp cho chi nhánh phía Nam", date: "2026-10-07", user: "Vũ Đức Hùng" }
            ],
            tickets: []
        },

        // --- Các khách hàng bình thường (Không trùng) ---
        {
            id: 7,
            name: "Tập đoàn Vingroup - CTCP",
            companyName: "Tập đoàn Vingroup - CTCP",
            taxCode: "0101245486",
            status: "CHINH_THUC",
            industryId: 5,
            companySizeId: 13,
            email: "info@vingroup.net",
            phone: "02439749999",
            website: "https://vingroup.net",
            address: "Số 7 Đường Bằng Lăng 1, KĐT Vinhomes Riverside, Long Biên, Hà Nội",
            ownerUserId: 100,
            ownerName: "Nông Quang Tiệp",
            revenue: "200 tỷ VNĐ",
            contacts: [{ id: 18, name: "Phạm Nhật Vượng", role: "Chủ tịch HĐQT", phone: "0903000001", email: "vuongpn@vingroup.net" }],
            deals: [{ id: 107, title: "Dự án Quản trị quan hệ khách hàng VinFast", amount: 2500000000, stage: "Thành công", probability: 100 }],
            activities: [],
            tickets: []
        },
        {
            id: 8,
            name: "Công ty Cổ phần Tập đoàn Masan",
            companyName: "Công ty Cổ phần Tập đoàn Masan",
            taxCode: "0303576603",
            status: "DANG_GIAO_DICH",
            industryId: 3,
            companySizeId: 13,
            email: "info@msn.masangroup.com",
            phone: "02862563862",
            website: "https://masangroup.com",
            address: "Tầng 8, Tòa nhà Central Plaza, 17 Lê Duẩn, Quận 1, TP.HCM",
            ownerUserId: 101,
            ownerName: "Nguyễn Văn An",
            revenue: "85 tỷ VNĐ",
            contacts: [],
            deals: [],
            activities: [],
            tickets: []
        },
        {
            id: 9,
            name: "Công ty Cổ phần Đầu tư Thế Giới Di Động",
            companyName: "Công ty Cổ phần Đầu tư Thế Giới Di Động",
            taxCode: "0303217354",
            status: "CHINH_THUC",
            industryId: 2,
            companySizeId: 13,
            email: "contact@mwg.vn",
            phone: "02835100100",
            website: "https://mwg.vn",
            address: "128 Trần Quang Khải, Tân Định, Quận 1, TP.HCM",
            ownerUserId: 102,
            ownerName: "Trần Thị Mai",
            revenue: "150 tỷ VNĐ",
            contacts: [],
            deals: [],
            activities: [],
            tickets: []
        },
        {
            id: 10,
            name: "Công ty Cổ phần Tập đoàn Công nghệ CMC",
            companyName: "Công ty Cổ phần Tập đoàn Công nghệ CMC",
            taxCode: "0100244112",
            status: "TIEM_NANG",
            industryId: 1,
            companySizeId: 12,
            email: "info@cmc.com.vn",
            phone: "02437958668",
            website: "https://cmc.com.vn",
            address: "Tòa nhà CMC, Duy Tân, Dịch Vọng Hậu, Cầu Giấy, Hà Nội",
            ownerUserId: 103,
            ownerName: "Lê Hoàng Nam",
            revenue: "35 tỷ VNĐ",
            contacts: [],
            deals: [],
            activities: [],
            tickets: []
        }
    ];

    /* =========================================================
       2. NORMALIZATION & DUPLICATE DETECTION ALGORITHMS
    ========================================================= */

    /**
     * Chuẩn hóa Mã số thuế: Bỏ khoảng trắng, dấu gạch ngang, ký tự đặc biệt
     */
    function cleanTaxCode(tax) {
        if (!tax) return "";
        return String(tax).replace(/[^0-9A-Za-z]/g, "").toUpperCase();
    }

    /**
     * Chuẩn hóa Website: Lấy domain gốc, bỏ giao thức http/https, www, query param, path
     */
    function cleanWebsite(url) {
        if (!url) return "";
        let clean = String(url).trim().toLowerCase();
        // Xóa giao thức
        clean = clean.replace(/^(https?:\/\/)?(www\.)?/, "");
        // Xóa port, path, trailing slash
        clean = clean.split("/")[0].split(":")[0];
        // Bỏ domain quá ngắn hoặc rỗng
        if (clean.length < 3 || !clean.includes(".")) return "";
        return clean;
    }

    /**
     * Chuẩn hóa Tên công ty để so sánh Fuzzy:
     * - Chuyển chữ thường
     * - Loại bỏ các định dạng loại hình doanh nghiệp phổ biến ở Việt Nam
     * - Loại bỏ dấu câu
     */
    function cleanCompanyName(name) {
        if (!name) return "";
        let clean = String(name).toLowerCase();

        // Bỏ dấu tiếng Việt để so sánh phụ trợ
        clean = removeVietnameseTones(clean);

        // Danh sách từ khóa loại hình công ty cần lược bỏ
        const legalTerms = [
            "cong ty co phan",
            "cong ty tnhh mtv",
            "cong ty tnhh",
            "cong ty",
            "tap doan",
            "tong cong ty",
            "chi nhanh",
            "cty cp",
            "cty tnhh",
            "co phan",
            "tnhh",
            "group",
            "corp",
            "jsc",
            "ltd"
        ];

        legalTerms.forEach(term => {
            const regex = new RegExp(`\\b${term}\\b`, "gi");
            clean = clean.replace(regex, " ");
        });

        // Bỏ dấu câu, khoảng trắng thừa
        clean = clean.replace(/[^a-z0-9\s]/g, " ").replace(/\s+/g, " ").trim();
        return clean;
    }

    function removeVietnameseTones(str) {
        return str
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .replace(/đ/g, "d")
            .replace(/Đ/g, "D");
    }

    /**
     * Tính toán độ tương đồng giữa hai chuỗi tên (Token Jaccard & Substring)
     */
    function calculateNameSimilarity(nameA, nameB) {
        const cleanA = cleanCompanyName(nameA);
        const cleanB = cleanCompanyName(nameB);

        if (!cleanA || !cleanB) return 0;
        if (cleanA === cleanB) return 1.0;

        // Nếu một trong hai chuỗi bao hàm chuỗi kia và có độ dài đáng kể
        if (cleanA.length >= 3 && cleanB.length >= 3) {
            if (cleanA.includes(cleanB) || cleanB.includes(cleanA)) {
                const shorter = Math.min(cleanA.length, cleanB.length);
                const longer = Math.max(cleanA.length, cleanB.length);
                if (shorter / longer >= 0.4) return 0.88;
            }
        }

        // So sánh theo tập từ khóa (Token Jaccard Similarity)
        const tokensA = new Set(cleanA.split(/\s+/).filter(w => w.length >= 2));
        const tokensB = new Set(cleanB.split(/\s+/).filter(w => w.length >= 2));

        if (tokensA.size === 0 || tokensB.size === 0) return 0;

        let intersectionCount = 0;
        tokensA.forEach(token => {
            if (tokensB.has(token)) intersectionCount++;
        });

        const unionSize = new Set([...tokensA, ...tokensB]).size;
        return unionSize === 0 ? 0 : intersectionCount / unionSize;
    }

    /**
     * Kiểm tra một khách hàng xem có trùng lặp với khách hàng nào khác trong danh sách không
     * Trả về danh sách các đối tượng trùng cùng lý do chi tiết
     */
    function detectDuplicatesForCustomer(candidate, allCustomers) {
        if (!candidate || candidate.status === "DA_GOP" || candidate.isMerged) {
            return { isDuplicate: false, matches: [] };
        }

        const candidateTax = cleanTaxCode(candidate.taxCode);
        const candidateWeb = cleanWebsite(candidate.website);
        const candidateName = candidate.companyName || candidate.name || "";

        const matches = [];

        for (const other of allCustomers) {
            // Không tự so sánh với chính mình và bỏ qua bản ghi đã gộp
            if (Number(other.id) === Number(candidate.id)) continue;
            if (other.status === "DA_GOP" || other.isMerged) continue;

            const otherTax = cleanTaxCode(other.taxCode);
            const otherWeb = cleanWebsite(other.website);
            const otherName = other.companyName || other.name || "";

            const reasons = [];
            let isMatched = false;

            // 1. Tiêu chí 1: Mã số thuế (MST trùng 100% hoặc trùng 10 số đầu)
            if (candidateTax && otherTax) {
                if (candidateTax === otherTax) {
                    isMatched = true;
                    reasons.push(`Trùng 100% Mã số thuế (MST: ${candidate.taxCode})`);
                } else if (candidateTax.length >= 10 && otherTax.length >= 10 && candidateTax.slice(0, 10) === otherTax.slice(0, 10)) {
                    isMatched = true;
                    reasons.push(`Trùng mã số thuế gốc chi nhánh (MST: ${candidate.taxCode} & ${other.taxCode})`);
                }
            }

            // 2. Tiêu chí 2: Website trùng lặp (Domain)
            if (candidateWeb && otherWeb && candidateWeb === otherWeb) {
                isMatched = true;
                reasons.push(`Trùng địa chỉ Website (${candidateWeb})`);
            }

            // 3. Tiêu chí 3: Tên công ty gần giống (Fuzzy Search / Tương đồng cao)
            const similarity = calculateNameSimilarity(candidateName, otherName);
            if (similarity >= 0.65) {
                isMatched = true;
                const percent = Math.round(similarity * 100);
                reasons.push(`Tên công ty tương đồng ${percent}% ("${otherName}")`);
            }

            if (isMatched) {
                matches.push({
                    record: other,
                    reasons: reasons,
                    similarityScore: similarity
                });
            }
        }

        return {
            isDuplicate: matches.length > 0,
            matches: matches
        };
    }

    /**
     * Quét toàn bộ danh sách khách hàng và trả về Map các bản ghi trùng lặp
     */
    function scanAllDuplicates(customers) {
        const resultMap = new Map();
        customers.forEach(cust => {
            const res = detectDuplicatesForCustomer(cust, customers);
            if (res.isDuplicate) {
                resultMap.set(Number(cust.id), res);
            }
        });
        return resultMap;
    }

    /* =========================================================
       3. STORAGE MANAGEMENT (CRM_CUSTOMERS_DATA & MERGE LOGS)
    ========================================================= */

    function getStoredCustomers() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY_CUSTOMERS);
            if (raw === null || raw === undefined) {
                localStorage.setItem(STORAGE_KEY_CUSTOMERS, JSON.stringify(DEFAULT_SEED_CUSTOMERS));
                return DEFAULT_SEED_CUSTOMERS;
            }
            const parsed = JSON.parse(raw);
            if (!Array.isArray(parsed)) {
                localStorage.setItem(STORAGE_KEY_CUSTOMERS, JSON.stringify(DEFAULT_SEED_CUSTOMERS));
                return DEFAULT_SEED_CUSTOMERS;
            }
            return parsed;
        } catch (e) {
            console.error("Lỗi đọc CRM_CUSTOMERS_DATA:", e);
            return DEFAULT_SEED_CUSTOMERS;
        }
    }

    function saveStoredCustomers(customers) {
        try {
            localStorage.setItem(STORAGE_KEY_CUSTOMERS, JSON.stringify(customers));
        } catch (e) {
            console.error("Lỗi lưu CRM_CUSTOMERS_DATA:", e);
        }
    }

    function getMergeLogs() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY_MERGE_LOGS);
            return raw ? JSON.parse(raw) : [];
        } catch (e) {
            return [];
        }
    }

    function appendMergeLog(logEntry) {
        try {
            const logs = getMergeLogs();
            logs.unshift(logEntry);
            localStorage.setItem(STORAGE_KEY_MERGE_LOGS, JSON.stringify(logs));
        } catch (e) {
            console.error("Lỗi lưu CRM_DUPLICATE_MERGE_LOGS:", e);
        }
    }

    /* =========================================================
       4. ROLE MANAGEMENT & PERMISSION CHECK
    ========================================================= */

    function getSimulatedRole() {
        return localStorage.getItem(STORAGE_KEY_ACTIVE_ROLE) || "TEAM_LEAD";
    }

    function setSimulatedRole(role) {
        localStorage.setItem(STORAGE_KEY_ACTIVE_ROLE, role);
    }

    function canExecuteMerge(userRole) {
        const role = (userRole || getSimulatedRole()).toUpperCase();
        return role.includes("LEAD") || role.includes("ADMIN") || role.includes("MANAGER");
    }

    /* =========================================================
       5. COMPREHENSIVE DATA MERGE EXECUTION
    ========================================================= */

    /**
     * Thực hiện gộp 2 bản ghi:
     * - Master Record: Giữ lại, nhận thông tin các trường đã chọn
     * - Secondary Record: Chuyển sang trạng thái "Đã gộp" (DA_GOP)
     * - Chuyển toàn bộ contacts, deals, activities, tickets sang Master Record
     * - Ghi log vào CRM_DUPLICATE_MERGE_LOGS
     */
    function executeMerge({ masterId, secondaryId, fieldOverrides, matchedReasons, operatorUser }) {
        const customers = getStoredCustomers();

        const masterIndex = customers.findIndex(c => Number(c.id) === Number(masterId));
        const secondaryIndex = customers.findIndex(c => Number(c.id) === Number(secondaryId));

        if (masterIndex === -1 || secondaryIndex === -1) {
            throw new Error("Không tìm thấy bản ghi khách hàng cần gộp trong hệ thống.");
        }

        const master = { ...customers[masterIndex] };
        const secondary = { ...customers[secondaryIndex] };

        // 1. Ghi đè các trường thông tin ưu tiên đã chọn
        Object.keys(fieldOverrides || {}).forEach(key => {
            master[key] = fieldOverrides[key];
        });

        // 2. Chuyển giao toàn bộ Người liên hệ (Contacts)
        const masterContacts = Array.isArray(master.contacts) ? [...master.contacts] : [];
        const secondaryContacts = Array.isArray(secondary.contacts) ? [...secondary.contacts] : [];
        // Gắn cờ nguồn cho các liên hệ chuyển sang
        const transferredContacts = secondaryContacts.map(c => ({
            ...c,
            note: `Chuyển từ hồ sơ đã gộp [${secondary.companyName || secondary.name}]`
        }));
        master.contacts = [...masterContacts, ...transferredContacts];

        // 3. Chuyển giao toàn bộ Cơ hội bán hàng (Deals/Opportunities)
        const masterDeals = Array.isArray(master.deals) ? [...master.deals] : [];
        const secondaryDeals = Array.isArray(secondary.deals) ? [...secondary.deals] : [];
        const transferredDeals = secondaryDeals.map(d => ({
            ...d,
            note: `Cơ hội chuyển giao khi gộp từ [${secondary.companyName || secondary.name}]`
        }));
        master.deals = [...masterDeals, ...transferredDeals];

        // 4. Chuyển giao toàn bộ Lịch sử hoạt động (Activities)
        const masterActs = Array.isArray(master.activities) ? [...master.activities] : [];
        const secondaryActs = Array.isArray(secondary.activities) ? [...secondary.activities] : [];
        const transferredActs = secondaryActs.map(a => ({
            ...a,
            summary: `[Hoạt động gộp] ${a.summary || ""}`
        }));

        // Bổ sung hoạt động ghi vết việc gộp bản ghi
        const mergeActivity = {
            id: Date.now(),
            type: "Gộp khách hàng (CRM-64)",
            summary: `Đã hợp nhất dữ liệu từ bản ghi phụ "${secondary.companyName || secondary.name}" (MST: ${secondary.taxCode || "N/A"}) vào bản ghi này.`,
            date: new Date().toISOString().slice(0, 10),
            user: operatorUser?.fullName || "Trưởng nhóm kinh doanh"
        };

        master.activities = [mergeActivity, ...masterActs, ...transferredActs];

        // 5. Chuyển giao Phiếu hỗ trợ (Tickets) nếu có
        const masterTickets = Array.isArray(master.tickets) ? [...master.tickets] : [];
        const secondaryTickets = Array.isArray(secondary.tickets) ? [...secondary.tickets] : [];
        master.tickets = [...masterTickets, ...secondaryTickets];

        // Cập nhật nhãn ghi nhớ trên Master
        master.isMergedMaster = true;
        master.lastMergedAt = new Date().toISOString();

        // 6. Cập nhật bản ghi phụ (Secondary) thành trạng thái ĐÃ GỘP
        secondary.status = "DA_GOP";
        secondary.isMerged = true;
        secondary.mergedIntoId = master.id;
        secondary.mergedIntoName = master.companyName || master.name;
        secondary.mergedAt = new Date().toISOString();
        secondary.mergedBy = operatorUser?.fullName || "Trưởng nhóm kinh doanh";

        // Lưu lại vào danh sách
        customers[masterIndex] = master;
        customers[secondaryIndex] = secondary;
        saveStoredCustomers(customers);

        // 7. Tạo Audit Log ghi vết vào CRM_DUPLICATE_MERGE_LOGS
        const logEntry = {
            id: "MERGE-" + Date.now(),
            timestamp: new Date().toISOString(),
            operator: operatorUser?.fullName || "Trưởng nhóm kinh doanh",
            role: operatorUser?.role || getSimulatedRole(),
            masterRecord: {
                id: master.id,
                name: master.companyName || master.name,
                taxCode: master.taxCode
            },
            secondaryRecord: {
                id: secondary.id,
                name: secondary.companyName || secondary.name,
                taxCode: secondary.taxCode
            },
            matchedReasons: matchedReasons || [],
            fieldOverridesApplied: fieldOverrides,
            transferredDataSummary: {
                contactsCount: transferredContacts.length,
                dealsCount: transferredDeals.length,
                activitiesCount: transferredActs.length,
                ticketsCount: secondaryTickets.length
            }
        };

        appendMergeLog(logEntry);

        return {
            success: true,
            master,
            secondary,
            logEntry
        };
    }

    /* =========================================================
       PUBLIC API EXPORTS
    ========================================================= */
    return {
        cleanTaxCode,
        cleanWebsite,
        cleanCompanyName,
        calculateNameSimilarity,
        detectDuplicatesForCustomer,
        scanAllDuplicates,
        getStoredCustomers,
        saveStoredCustomers,
        getMergeLogs,
        appendMergeLog,
        getSimulatedRole,
        setSimulatedRole,
        canExecuteMerge,
        executeMerge,
        DEFAULT_SEED_CUSTOMERS
    };
});
