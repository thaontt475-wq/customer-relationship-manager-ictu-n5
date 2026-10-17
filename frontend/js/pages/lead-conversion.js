/**
 * ===================================================================
 * CRM-122 / S4-08: CHUYỂN ĐỔI LEAD 3-TRONG-1 (LEAD CONVERSION CONTROLLER)
 * Author: Senior Frontend Developer (Nông Quang Tiệp)
 * Scope: HTML5, CSS3, Vanilla ES6+ - Zero JSP Policy - No Backend Modification
 * RESTful Endpoints Ready:
 *   - GET  /api/leads/{id}/check-convert-duplicates
 *   - POST /api/leads/{id}/convert
 * ===================================================================
 */

(function () {
    "use strict";

    // 1. STORAGE KEYS & CONSTANTS
    const STORAGE_KEY_LEADS = "crm_leads_conversion_data";
    const STORAGE_KEY_CUSTOMERS = "crm_customers";
    const STORAGE_KEY_CONTACTS = "crm_contacts";
    const STORAGE_KEY_OPPORTUNITIES = "crm_ui_opportunities";

    // 2. DEFAULT MOCK DATA FOR SPRINT 4
    const DEFAULT_EXISTING_CUSTOMERS = [
        {
            id: 1001,
            name: "Công ty Cổ phần Mediphar Quốc tế",
            taxCode: "0107892341",
            phone: "02438889999",
            email: "contact@mediphar.vn",
            address: "Tòa nhà Mediphar, Cầu Giấy, Hà Nội",
            industry: "Y tế & Dược phẩm",
            status: "CHINH_THUC"
        },
        {
            id: 1002,
            name: "Ngân hàng TMCP Thịnh Vượng Capital",
            taxCode: "0109988776",
            phone: "02439991111",
            email: "b2b@thinhvuongcapital.vn",
            address: "Tháp Capital, Hoàn Kiếm, Hà Nội",
            industry: "Tài chính & Ngân hàng",
            status: "DANG_GIAO_DICH"
        },
        {
            id: 1003,
            name: "Tập đoàn Logistics Toàn Cầu VinaTrans",
            taxCode: "0301122334",
            phone: "02838887777",
            email: "sales@vinatrans.com.vn",
            address: "Cảng Cát Lái, Quận 2, TP.HCM",
            industry: "Dịch vụ & Đào tạo",
            status: "CHINH_THUC"
        }
    ];

    const DEFAULT_LEADS = [
        // Lead 1: Qualified - Việt Tech (CNTT)
        {
            id: "LD-CONV-01",
            code: "LEAD-2026-081",
            fullName: "Nguyễn Hoàng Nam",
            title: "Tổng Giám đốc (CEO)",
            buyingRole: "DECISION_MAKER",
            phone: "0912345678",
            email: "nam.nh@viettech.com.vn",
            companyName: "Công ty Cổ phần Công nghệ Cao Việt Tech",
            industry: "CNTT & Viễn thông",
            taxCode: "0108923451",
            companyPhone: "02437891234",
            address: "Tầng 8, Tòa nhà Keangnam Landmark 72, Mễ Trì, Nam Từ Liêm, Hà Nội",
            website: "https://viettech.com.vn",
            score: 94,
            tier: "TIER_A",
            estimatedValue: 250000000,
            qualifiedAt: "2026-10-12T14:30:00Z",
            assignedSales: "Nông Quang Tiệp (Senior Sales)",
            notes: "Khách hàng cần triển khai phần mềm CRM cho 120 nhân sự sales B2B. Đã qua vòng demo giải pháp và đồng ý chốt hợp đồng trong tháng.",
            status: "QUALIFIED",
            isLocked: false,
            convertedAt: null,
            convertedBy: null,
            convertedTo: null
        },
        // Lead 2: Qualified - Mediphar (Potential Duplicate with existing customer!)
        {
            id: "LD-CONV-02",
            code: "LEAD-2026-082",
            fullName: "Vũ Thị Bích Ngọc",
            title: "Giám đốc Mua sắm & Cung ứng",
            buyingRole: "INFLUENCER",
            phone: "0988776655",
            email: "ngoc.vb@mediphar.vn",
            companyName: "Công ty Cổ phần Mediphar Quốc tế",
            industry: "Y tế & Dược phẩm",
            taxCode: "0107892341", // Matches KH-1001
            companyPhone: "02438889999",
            address: "Tòa nhà Mediphar, Cầu Giấy, Hà Nội",
            website: "https://mediphar.vn",
            score: 88,
            tier: "TIER_A",
            estimatedValue: 420000000,
            qualifiedAt: "2026-10-13T09:15:00Z",
            assignedSales: "Nông Quang Tiệp (Senior Sales)",
            notes: "Dự án mở rộng module Quản lý Hợp đồng & Chăm sóc Khách hàng Bệnh viện. Cần tích hợp với hệ thống ERP SAP hiện tại.",
            status: "QUALIFIED",
            isLocked: false,
            convertedAt: null,
            convertedBy: null,
            convertedTo: null
        },
        // Lead 3: Qualified - Chuỗi Thời trang An Phước (Bán lẻ)
        {
            id: "LD-CONV-03",
            code: "LEAD-2026-083",
            fullName: "Đinh Tuấn Anh",
            title: "Giám đốc Chuyển đổi số (CDO)",
            buyingRole: "DECISION_MAKER",
            phone: "0904123987",
            email: "anh.dt@anphuocfashion.com",
            companyName: "Chuỗi Bán lẻ Thời trang An Phước",
            industry: "Bán lẻ & Chuỗi F&B",
            taxCode: "0106543219",
            companyPhone: "02435678910",
            address: "Số 158 Phố Huế, Hai Bà Trưng, Hà Nội",
            website: "https://anphuocfashion.com",
            score: 79,
            tier: "TIER_B",
            estimatedValue: 180000000,
            qualifiedAt: "2026-10-11T16:45:00Z",
            assignedSales: "Nông Quang Tiệp (Senior Sales)",
            notes: "Cần phần mềm quản lý quan hệ khách hàng VIP và tích điểm thành viên đa kênh.",
            status: "QUALIFIED",
            isLocked: false,
            convertedAt: null,
            convertedBy: null,
            convertedTo: null
        },
        // Lead 4: Qualified - Địa ốc Đất Việt (Bất động sản)
        {
            id: "LD-CONV-04",
            code: "LEAD-2026-084",
            fullName: "Phạm Quốc Dũng",
            title: "Phó Tổng Giám đốc Kinh doanh",
            buyingRole: "DECISION_MAKER",
            phone: "0933555777",
            email: "dung.pq@datvietland.vn",
            companyName: "Công ty Cổ phần Địa ốc & Xây dựng Đất Việt",
            industry: "Bất động sản & Xây dựng",
            taxCode: "0309876543",
            companyPhone: "02839112233",
            address: "Tòa tháp Landmark 81, Bình Thạnh, TP.HCM",
            website: "https://datvietland.vn",
            score: 86,
            tier: "TIER_A",
            estimatedValue: 350000000,
            qualifiedAt: "2026-10-10T11:20:00Z",
            assignedSales: "Nông Quang Tiệp (Senior Sales)",
            notes: "Nhu cầu quản lý bảng hàng, giỏ căn hộ và hoa hồng môi giới bất động sản.",
            status: "QUALIFIED",
            isLocked: false,
            convertedAt: null,
            convertedBy: null,
            convertedTo: null
        },
        // Lead 5: Qualified - Ngân hàng Thịnh Vượng (Tài chính - Duplicate test)
        {
            id: "LD-CONV-05",
            code: "LEAD-2026-085",
            fullName: "Lê Hoàng Yến",
            title: "Giám đốc Khối Khách hàng Doanh nghiệp",
            buyingRole: "DECISION_MAKER",
            phone: "0977889900",
            email: "yen.lh@thinhvuongcapital.vn",
            companyName: "Ngân hàng TMCP Thịnh Vượng Capital",
            industry: "Tài chính & Ngân hàng",
            taxCode: "0109988776", // Matches KH-1002
            companyPhone: "02439991111",
            address: "Tháp Capital, Hoàn Kiếm, Hà Nội",
            website: "https://thinhvuongcapital.vn",
            score: 92,
            tier: "TIER_A",
            estimatedValue: 600000000,
            qualifiedAt: "2026-10-09T08:30:00Z",
            assignedSales: "Nông Quang Tiệp (Senior Sales)",
            notes: "Dự án nâng cấp hệ thống CRM ngân hàng thương mại, phê duyệt khoản vay doanh nghiệp SME.",
            status: "QUALIFIED",
            isLocked: false,
            convertedAt: null,
            convertedBy: null,
            convertedTo: null
        },
        // Lead 6: Converted & Locked - VinaTrans (Demonstrates initial converted state)
        {
            id: "LD-CONV-06",
            code: "LEAD-2026-086",
            fullName: "Trần Trọng Nghĩa",
            title: "Tổng Giám đốc (CEO)",
            buyingRole: "DECISION_MAKER",
            phone: "0908112233",
            email: "nghia.tt@vinatrans.com.vn",
            companyName: "Tập đoàn Logistics Toàn Cầu VinaTrans",
            industry: "Dịch vụ & Đào tạo",
            taxCode: "0301122334",
            companyPhone: "02838887777",
            address: "Cảng Cát Lái, Quận 2, TP.HCM",
            website: "https://vinatrans.com.vn",
            score: 95,
            tier: "TIER_A",
            estimatedValue: 500000000,
            qualifiedAt: "2026-10-08T10:00:00Z",
            assignedSales: "Nông Quang Tiệp (Senior Sales)",
            notes: "Lead đã hoàn thành chuyển đổi 3-trong-1 thành công, bản ghi được khóa an toàn.",
            status: "CONVERTED",
            isLocked: true,
            convertedAt: "2026-10-13T16:00:00Z",
            convertedBy: "Nông Quang Tiệp",
            convertedTo: {
                customerId: 1003,
                customerName: "Tập đoàn Logistics Toàn Cầu VinaTrans",
                contactId: "CT-1003",
                contactName: "Trần Trọng Nghĩa",
                dealId: "OPP-1003",
                dealName: "VinaTrans - Phần mềm Quản trị Quan hệ Vận tải & Kho bãi",
                dealAmount: 500000000
            }
        }
    ];

    // 3. SERVICE LAYER (REST API & LOCALSTORAGE ABSTRACTION)
    const LeadConversionService = {
        init() {
            if (!localStorage.getItem(STORAGE_KEY_LEADS)) {
                localStorage.setItem(STORAGE_KEY_LEADS, JSON.stringify(DEFAULT_LEADS));
            }
            if (!localStorage.getItem(STORAGE_KEY_CUSTOMERS)) {
                localStorage.setItem(STORAGE_KEY_CUSTOMERS, JSON.stringify(DEFAULT_EXISTING_CUSTOMERS));
            }
        },

        getLeads() {
            this.init();
            try {
                return JSON.parse(localStorage.getItem(STORAGE_KEY_LEADS)) || [];
            } catch (e) {
                console.error("Error reading leads:", e);
                return DEFAULT_LEADS;
            }
        },

        saveLeads(leads) {
            localStorage.setItem(STORAGE_KEY_LEADS, JSON.stringify(leads));
        },

        getLeadById(id) {
            const leads = this.getLeads();
            return leads.find(l => String(l.id) === String(id) || l.code === id) || null;
        },

        async getLeadByIdAsync(id) {
            let lead = this.getLeadById(id);
            if (!lead && window.CrmApi) {
                try {
                    const res = await window.CrmApi.request(`/api/leads/${id}`);
                    if (res) {
                        const data = res.data || res;
                        lead = {
                            id: data.id,
                            code: data.code || `LEAD-2026-${data.id}`,
                            fullName: data.fullName || data.name || "Khách hàng",
                            title: data.title || "Người đại diện",
                            buyingRole: "DECISION_MAKER",
                            phone: data.phone || "",
                            email: data.email || "",
                            companyName: data.companyName || data.name || "Doanh nghiệp",
                            industry: data.industry || "CNTT & Viễn thông",
                            taxCode: data.taxCode || "",
                            companyPhone: data.phone || "",
                            address: data.address || "",
                            website: data.website || "",
                            score: data.score || 85,
                            tier: data.tier || "TIER_A",
                            estimatedValue: data.estimatedValue || 150000000,
                            assignedSales: data.owner || "Nông Quang Tiệp",
                            notes: data.notes || "",
                            status: data.status || "QUALIFIED",
                            isLocked: data.isLocked || data.status === "CONVERTED"
                        };
                        const list = this.getLeads();
                        list.unshift(lead);
                        this.saveLeads(list);
                    }
                } catch (e) {
                    console.warn("Could not fetch lead from backend:", e);
                }
            }
            return lead;
        },

        getExistingCustomers() {
            this.init();
            try {
                return JSON.parse(localStorage.getItem(STORAGE_KEY_CUSTOMERS)) || DEFAULT_EXISTING_CUSTOMERS;
            } catch (e) {
                return DEFAULT_EXISTING_CUSTOMERS;
            }
        },

        saveExistingCustomer(cust) {
            const list = this.getExistingCustomers();
            const idx = list.findIndex(c => c.id === cust.id || (cust.taxCode && c.taxCode === cust.taxCode));
            if (idx >= 0) {
                list[idx] = { ...list[idx], ...cust };
            } else {
                list.unshift(cust);
            }
            localStorage.setItem(STORAGE_KEY_CUSTOMERS, JSON.stringify(list));
        },

        /**
         * Endpoint: GET /api/leads/{id}/check-convert-duplicates
         * Checks for duplicate company name or tax code among existing customers
         */
        async checkConvertDuplicates(leadId) {
            const lead = this.getLeadById(leadId);
            if (!lead) return { hasDuplicate: false };

            if (window.CrmApi && typeof window.CrmApi.request === "function") {
                const res = await window.CrmApi.request(`/api/leads/${leadId}/check-convert-duplicates`);
                if (res) {
                    return (res && res.data) ? res.data : res;
                }
            }

            const customers = this.getExistingCustomers();
            const cleanTax = (lead.taxCode || "").trim().toLowerCase();
            const cleanName = (lead.companyName || "").trim().toLowerCase();

            const found = customers.find(c => {
                const cTax = (c.taxCode || "").trim().toLowerCase();
                const cName = (c.name || "").trim().toLowerCase();
                return (cleanTax && cTax === cleanTax) || (cleanName && cName === cleanName);
            });

            if (found) {
                return {
                    hasDuplicate: true,
                    duplicateCustomer: found,
                    reason: cleanTax && (found.taxCode || "").trim().toLowerCase() === cleanTax
                        ? `Trùng Mã số thuế (${found.taxCode})`
                        : `Trùng Tên doanh nghiệp (${found.name})`
                };
            }
            return { hasDuplicate: false };
        },

        /**
         * Endpoint: POST /api/leads/{id}/convert
         * Performs 3-in-1 conversion into Customer, Contact, and Opportunity
         */
        async convertLead(leadId, payload) {
            const lead = this.getLeadById(leadId);
            if (!lead) throw new Error("Không tìm thấy thông tin Lead để chuyển đổi.");
            if (lead.isLocked || lead.status === "CONVERTED") {
                throw new Error("Bản ghi Lead này đã được chuyển đổi trước đó và đang bị khóa.");
            }

            let serverResult = null;
            if (window.CrmApi && typeof window.CrmApi.request === "function") {
                const apiRes = await window.CrmApi.request(`/api/leads/${leadId}/convert`, {
                    method: "POST",
                    body: JSON.stringify(payload)
                });
                serverResult = (apiRes && apiRes.data) ? apiRes.data : apiRes;
                if (apiRes && apiRes.success === false) {
                    throw new Error(apiRes.message || "Chuyển đổi Lead thất bại từ máy chủ.");
                }
            }

            const now = new Date();
            const nowIso = now.toISOString();

            let customerId = serverResult?.customerId ?? null;
            let customerName = serverResult?.customerName ?? "";
            let customerInfo = serverResult?.customerInfo ?? "";
            let contactId = serverResult?.contactId ?? null;
            let contactName = serverResult?.contactName ?? payload.contact.fullName.trim();
            let contactTitle = payload.contact.title || "Người đại diện";
            let contactInfo = serverResult?.contactInfo ?? `${contactTitle} - ${payload.contact.phone}`;
            let dealId = serverResult?.dealId !== undefined ? serverResult.dealId : null;
            let dealName = serverResult?.dealName ?? (payload.deal?.createDeal ? payload.deal.name.trim() : "");
            let dealAmount = Number(payload.deal?.amount) || 0;
            let dealInfo = serverResult?.dealInfo ?? (dealId ? `${formatCurrency(dealAmount)} ₫ - Dự kiến chốt: ${payload.deal.closeDate}` : "Không tạo cơ hội");
            let leadCode = serverResult?.leadCode || lead.code;
            let convertedAt = serverResult?.convertedAt || nowIso;

            if (!serverResult) {
                if (payload.customerMode === "MERGE_EXISTING" && payload.existingCustomerId) {
                    const existing = this.getExistingCustomers().find(c => String(c.id) === String(payload.existingCustomerId));
                    if (existing) {
                        customerId = existing.id;
                        customerName = existing.name;
                        customerInfo = `Gộp vào khách hàng có sẵn (MST: ${existing.taxCode || "N/A"})`;
                    } else {
                        throw new Error("Không tìm thấy khách hàng được chọn để gộp.");
                    }
                } else {
                    customerId = Date.now();
                    customerName = payload.customer.name.trim();
                    customerInfo = `MST: ${payload.customer.taxCode || "N/A"} - ${payload.customer.industry || "B2B"}`;

                    const newCustomer = {
                        id: customerId,
                        name: customerName,
                        taxCode: payload.customer.taxCode || "",
                        phone: payload.customer.phone || "",
                        email: payload.customer.email || lead.email,
                        address: payload.customer.address || "",
                        industry: payload.customer.industry || "",
                        website: payload.customer.website || "",
                        status: "TIEM_NANG",
                        source: `Chuyển đổi từ Lead (${lead.code})`,
                        createdAt: nowIso
                    };
                    this.saveExistingCustomer(newCustomer);
                }

                contactId = `CT-${customerId}-${Math.floor(100 + Math.random() * 900)}`;
                if (payload.deal && payload.deal.createDeal) {
                    dealId = `OPP-${customerId}-${Math.floor(100 + Math.random() * 900)}`;
                    dealName = payload.deal.name.trim();
                    dealInfo = `${formatCurrency(dealAmount)} ₫ - Dự kiến chốt: ${payload.deal.closeDate}`;
                } else {
                    dealId = null;
                    dealName = "";
                    dealInfo = "Không tạo cơ hội";
                }
            } else {
                if (payload.customerMode !== "MERGE_EXISTING") {
                    const newCustomer = {
                        id: customerId,
                        name: customerName,
                        taxCode: payload.customer?.taxCode || "",
                        phone: payload.customer?.phone || "",
                        email: payload.customer?.email || lead.email,
                        address: payload.customer?.address || "",
                        industry: payload.customer?.industry || "",
                        website: payload.customer?.website || "",
                        status: "TIEM_NANG",
                        source: `Chuyển đổi từ Lead (${lead.code})`,
                        createdAt: convertedAt
                    };
                    this.saveExistingCustomer(newCustomer);
                }
            }

            // 2. Process Contact
            const newContact = {
                id: contactId,
                customerId: customerId,
                companyName: customerName,
                fullName: contactName,
                title: contactTitle,
                phone: payload.contact.phone,
                email: payload.contact.email,
                buyingRole: payload.contact.buyingRole || "DECISION_MAKER",
                isPrimary: true,
                notes: payload.contact.notes || lead.notes || "",
                createdAt: convertedAt
            };
            try {
                let contacts = JSON.parse(localStorage.getItem(STORAGE_KEY_CONTACTS)) || [];
                contacts.unshift(newContact);
                localStorage.setItem(STORAGE_KEY_CONTACTS, JSON.stringify(contacts));
            } catch (_) {}

            // 3. Process Deal / Opportunity
            if (dealId) {
                const newOpportunity = {
                    id: dealId,
                    customerId: customerId,
                    customerName: customerName,
                    contactId: contactId,
                    name: dealName,
                    amount: dealAmount,
                    pipeline: payload.deal?.pipeline || "B2B_STANDARD",
                    stageId: Number(payload.deal?.stageId) || 2,
                    stageName: payload.deal?.stageName || "Giải pháp / Demo",
                    probability: Number(payload.deal?.probability) || 40,
                    expectedCloseDate: payload.deal?.closeDate || convertedAt.split("T")[0],
                    leadCode: lead.code,
                    createdAt: convertedAt
                };
                try {
                    let opps = JSON.parse(localStorage.getItem(STORAGE_KEY_OPPORTUNITIES)) || [];
                    opps.unshift(newOpportunity);
                    localStorage.setItem(STORAGE_KEY_OPPORTUNITIES, JSON.stringify(opps));
                } catch (_) {}
            }

            // 4. Update and LOCK the Lead record ONLY after successful conversion
            const leads = this.getLeads();
            const targetIndex = leads.findIndex(l => String(l.id) === String(leadId) || l.code === lead.code);
            if (targetIndex >= 0) {
                leads[targetIndex].status = "CONVERTED";
                leads[targetIndex].isLocked = true;
                leads[targetIndex].convertedAt = convertedAt;
                leads[targetIndex].convertedBy = serverResult?.convertedBy || "Nông Quang Tiệp (Senior Sales)";
                leads[targetIndex].convertedTo = {
                    customerId: customerId,
                    customerName: customerName,
                    contactId: contactId,
                    contactName: contactName,
                    dealId: dealId,
                    dealName: dealName,
                    dealAmount: dealAmount
                };
                this.saveLeads(leads);
            }

            return {
                success: true,
                customerId,
                customerName,
                customerInfo,
                contactId,
                contactName,
                contactInfo,
                dealId,
                dealName,
                dealInfo,
                leadCode,
                convertedAt
            };
        },

        resetSampleData() {
            localStorage.setItem(STORAGE_KEY_LEADS, JSON.stringify(DEFAULT_LEADS));
            localStorage.setItem(STORAGE_KEY_CUSTOMERS, JSON.stringify(DEFAULT_EXISTING_CUSTOMERS));
        }
    };

    window.LeadConversionService = LeadConversionService;

    // 4. UI CONTROLLER & STATE
    let currentFilter = {
        keyword: "",
        status: "QUALIFIED",
        tier: "ALL",
        industry: "ALL",
        sort: "SCORE_DESC"
    };

    let currentPage = 1;
    const pageSize = 5;
    let activeLeadForConversion = null;

    // Avatar color gradients
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
        const num = Number(val) || 0;
        return num.toLocaleString("vi-VN");
    }

    function parseCurrency(str) {
        if (!str) return 0;
        return Number(String(str).replace(/[^\d]/g, "")) || 0;
    }

    function formatDateVi(dateStr) {
        if (!dateStr) return "--";
        const d = new Date(dateStr);
        if (isNaN(d.getTime())) return dateStr;
        return d.toLocaleDateString("vi-VN", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric"
        });
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

    // 5. TOAST NOTIFICATIONS
    function showToast(message, type = "success", links = null) {
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

        let linksHtml = "";
        if (links && Array.isArray(links)) {
            linksHtml = `
                <div class="toast-links">
                    ${links.map(l => `<a href="${l.href}" class="toast-link-btn" target="_blank">${l.label} <i class="fa-solid fa-arrow-up-right-from-square"></i></a>`).join(" ")}
                </div>
            `;
        }

        toast.innerHTML = `
            <div class="crm-toast-icon">${iconHtml}</div>
            <div class="crm-toast-copy">
                <div>${message}</div>
                ${linksHtml}
            </div>
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
        }, 5000);
    }

    // 6. RENDER LOGIC
    function refreshData() {
        const allLeads = LeadConversionService.getLeads();

        // Update KPIs
        const qualifiedCount = allLeads.length;
        const convertedLeads = allLeads.filter(l => l.status === "CONVERTED");
        const convertedCount = convertedLeads.length;
        const conversionRate = qualifiedCount > 0 ? Math.round((convertedCount / qualifiedCount) * 100) : 0;
        const totalRevenue = convertedLeads.reduce((acc, curr) => acc + (curr.convertedTo?.dealAmount || curr.estimatedValue || 0), 0);

        document.getElementById("statQualifiedTotal").textContent = qualifiedCount;
        document.getElementById("statConvertedCount").textContent = convertedCount;
        document.getElementById("statConversionRate").textContent = `${conversionRate}%`;
        document.getElementById("kpiProgressFill").style.width = `${Math.min(conversionRate, 100)}%`;
        document.getElementById("statPipelineRevenue").textContent = `${formatCurrency(totalRevenue)} ₫`;

        // Apply Filters
        let filtered = allLeads.filter(lead => {
            // Keyword
            if (currentFilter.keyword) {
                const kw = currentFilter.keyword.toLowerCase();
                const matchName = (lead.fullName || "").toLowerCase().includes(kw);
                const matchComp = (lead.companyName || "").toLowerCase().includes(kw);
                const matchCode = (lead.code || "").toLowerCase().includes(kw);
                const matchTax = (lead.taxCode || "").toLowerCase().includes(kw);
                const matchPhone = (lead.phone || "").toLowerCase().includes(kw);
                const matchEmail = (lead.email || "").toLowerCase().includes(kw);
                if (!matchName && !matchComp && !matchCode && !matchTax && !matchPhone && !matchEmail) {
                    return false;
                }
            }
            // Status
            if (currentFilter.status !== "ALL") {
                if (lead.status !== currentFilter.status) return false;
            }
            // Tier
            if (currentFilter.tier !== "ALL") {
                if (lead.tier !== currentFilter.tier) return false;
            }
            // Industry
            if (currentFilter.industry !== "ALL") {
                if (lead.industry !== currentFilter.industry) return false;
            }
            return true;
        });

        // Apply Sort
        filtered.sort((a, b) => {
            if (currentFilter.sort === "SCORE_DESC") return (b.score || 0) - (a.score || 0);
            if (currentFilter.sort === "VALUE_DESC") return (b.estimatedValue || 0) - (a.estimatedValue || 0);
            if (currentFilter.sort === "DATE_DESC") return new Date(b.qualifiedAt || 0) - new Date(a.qualifiedAt || 0);
            if (currentFilter.sort === "COMPANY_ASC") return (a.companyName || "").localeCompare(b.companyName || "");
            return 0;
        });

        // Render Table & Pagination
        renderTable(filtered);
    }

    function renderTable(leads) {
        const tbody = document.getElementById("conversionTableBody");
        const emptyState = document.getElementById("tableEmptyState");
        const recordCountEl = document.getElementById("tableRecordCount");

        recordCountEl.textContent = `${leads.length} bản ghi`;

        if (!leads.length) {
            tbody.innerHTML = "";
            emptyState.style.display = "block";
            updatePagination(0, 0, 0);
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
            const isConverted = lead.status === "CONVERTED";
            const rowClass = isConverted ? "row-converted" : "";

            const statusBadge = isConverted
                ? `<span class="badge-status badge-converted"><i class="fa-solid fa-lock"></i> ĐÃ CHUYỂN ĐỔI</span>`
                : `<span class="badge-status badge-qualified"><i class="fa-solid fa-circle-check"></i> ĐỦ ĐIỀU KIỆN</span>`;

            let tierClass = "badge-tier-b";
            let tierName = "Hạng B (Tiềm năng)";
            if (lead.tier === "TIER_A") {
                tierClass = "badge-tier-a";
                tierName = "Hạng A (Rất cao)";
            } else if (lead.tier === "TIER_C") {
                tierClass = "badge-tier-c";
                tierName = "Hạng C (Tiêu chuẩn)";
            }

            // Action column
            let actionHtml = "";
            if (isConverted) {
                actionHtml = `
                    <div class="action-btn-group">
                        <span class="btn-locked-action" title="Bản ghi Lead đã được chuyển đổi và khóa an toàn">
                            <i class="fa-solid fa-lock"></i> Đã khóa
                        </span>
                        <button type="button" class="btn-view-converted-action" data-lead-id="${lead.id}" title="Xem Khách hàng, Liên hệ & Cơ hội đã tạo">
                            <i class="fa-solid fa-eye"></i>
                        </button>
                    </div>
                `;
            } else {
                actionHtml = `
                    <div class="action-btn-group">
                        <button type="button" class="btn-convert-action btn-open-convert" data-lead-id="${lead.id}">
                            <i class="fa-solid fa-arrows-split-up-and-left"></i>
                            <span>Chuyển đổi</span>
                        </button>
                    </div>
                `;
            }

            html += `
                <tr class="${rowClass}">
                    <td>
                        <div class="lead-code-cell">
                            <span class="lead-code">${lead.code}</span>
                            ${statusBadge}
                        </div>
                    </td>
                    <td>
                        <div class="lead-contact-person">
                            <div class="person-avatar" style="background: ${getAvatarGradient(lead.fullName)}">
                                ${getInitials(lead.fullName)}
                            </div>
                            <div class="person-info">
                                <span class="person-name">${lead.fullName}</span>
                                <span class="person-title">${lead.title || "Người đại diện"}</span>
                            </div>
                        </div>
                    </td>
                    <td>
                        <span class="company-name">${lead.companyName}</span>
                        <div class="company-meta">
                            <span><i class="fa-regular fa-building"></i> ${lead.industry || "B2B"}</span>
                            ${lead.taxCode ? `<span>• MST: ${lead.taxCode}</span>` : ""}
                        </div>
                    </td>
                    <td>
                        <div class="contact-meta-item">
                            <i class="fa-solid fa-phone"></i>
                            <a href="tel:${lead.phone}">${lead.phone}</a>
                        </div>
                        <div class="contact-meta-item">
                            <i class="fa-solid fa-envelope"></i>
                            <a href="mailto:${lead.email}">${lead.email}</a>
                        </div>
                    </td>
                    <td style="text-align: center;">
                        <div class="score-badge-wrap">
                            <span class="badge-score-pill ${tierClass}">${lead.score}đ</span>
                            <span class="tier-label">${tierName}</span>
                        </div>
                    </td>
                    <td style="text-align: right;">
                        <span class="estimated-value">${formatCurrency(lead.estimatedValue)} ₫</span>
                    </td>
                    <td style="text-align: center;">
                        ${actionHtml}
                    </td>
                </tr>
            `;
        });

        tbody.innerHTML = html;

        // Attach action handlers
        tbody.querySelectorAll(".btn-open-convert").forEach(btn => {
            btn.addEventListener("click", () => {
                const leadId = btn.getAttribute("data-lead-id");
                openConversionModal(leadId);
            });
        });

        tbody.querySelectorAll(".btn-view-converted-action").forEach(btn => {
            btn.addEventListener("click", () => {
                const leadId = btn.getAttribute("data-lead-id");
                openConvertedDetailsModal(leadId);
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

    // 7. MODAL 1: OPEN & PRE-FILL 3-IN-1 CONVERSION MODAL
    async function openConversionModal(leadId) {
        const lead = await LeadConversionService.getLeadByIdAsync(leadId);
        if (!lead) {
            showToast("Không tìm thấy Lead!", "error");
            return;
        }
        if (lead.isLocked) {
            showToast("Lead này đã chuyển đổi và bị khóa!", "warning");
            return;
        }

        activeLeadForConversion = lead;

        // Context Ribbon
        document.getElementById("modalLeadCode").textContent = lead.code;
        document.getElementById("modalLeadName").textContent = lead.fullName;
        document.getElementById("modalLeadScore").textContent = `${lead.score}đ`;
        document.getElementById("modalLeadOwner").textContent = lead.assignedSales || "Nông Quang Tiệp";

        // Pre-fill CARD 1: Customer (Account)
        document.getElementById("radioCreateNew").checked = true;
        document.getElementById("radioMergeExisting").checked = false;
        document.getElementById("existingCustomerSelectorRow").style.display = "none";
        document.getElementById("customerFieldsContainer").style.display = "flex";

        document.getElementById("custName").value = lead.companyName || "";
        document.getElementById("custTaxCode").value = lead.taxCode || "";
        document.getElementById("custIndustry").value = lead.industry || "CNTT & Viễn thông";
        document.getElementById("custPhone").value = lead.companyPhone || lead.phone || "";
        document.getElementById("custWebsite").value = lead.website || "";
        document.getElementById("custAddress").value = lead.address || "";

        // Populate Existing Customers Dropdown
        const existingSelect = document.getElementById("selectExistingCustomer");
        const existingCustomers = LeadConversionService.getExistingCustomers();
        existingSelect.innerHTML = `<option value="">-- Chọn khách hàng trong hệ thống --</option>` +
            existingCustomers.map(c => `<option value="${c.id}">${c.name} (MST: ${c.taxCode || "N/A"})</option>`).join("");

        // Pre-fill CARD 2: Contact
        document.getElementById("contactFullName").value = lead.fullName || "";
        document.getElementById("contactTitle").value = lead.title || "Người đại diện";
        document.getElementById("contactBuyingRole").value = lead.buyingRole || "DECISION_MAKER";
        document.getElementById("contactPhone").value = lead.phone || "";
        document.getElementById("contactEmail").value = lead.email || "";
        document.getElementById("contactNotes").value = lead.notes || "";

        // Pre-fill CARD 3: Deal / Opportunity
        document.getElementById("checkCreateDeal").checked = true;
        document.getElementById("dealFieldsContainer").style.display = "flex";
        document.getElementById("dealName").value = `${lead.companyName} - Dự án Giải pháp CRM`;
        document.getElementById("dealAmount").value = formatCurrency(lead.estimatedValue || 150000000);

        // Date preset (+30 days)
        const d = new Date();
        d.setDate(d.getDate() + 30);
        document.getElementById("dealCloseDate").value = d.toISOString().split("T")[0];

        document.getElementById("dealPipeline").value = "B2B_STANDARD";
        document.getElementById("dealStage").value = "2"; // Demo

        // Check Duplicates Asynchronously
        const duplicateBanner = document.getElementById("duplicateAlertBanner");
        const duplicateText = document.getElementById("duplicateAlertText");
        const btnMergeSuggestion = document.getElementById("btnApplyMergeSuggestion");
        duplicateBanner.style.display = "none";

        const dupResult = await LeadConversionService.checkConvertDuplicates(lead.id);
        if (dupResult && dupResult.hasDuplicate) {
            duplicateBanner.style.display = "flex";
            duplicateText.innerHTML = `Hệ thống phát hiện công ty trùng lặp: <strong>${dupResult.duplicateCustomer.name}</strong> (${dupResult.reason}). Bạn có muốn gộp Lead này vào hồ sơ khách hàng đã có?`;

            btnMergeSuggestion.onclick = () => {
                document.getElementById("radioMergeExisting").checked = true;
                handleCustomerModeChange("MERGE_EXISTING");
                existingSelect.value = dupResult.duplicateCustomer.id;
                showToast("Đã chọn gộp vào Khách hàng có sẵn!", "success");
            };
        }

        // Show Modal
        const modal = document.getElementById("conversionModal");
        modal.style.display = "flex";
        modal.setAttribute("aria-hidden", "false");
    }

    function closeConversionModal() {
        const modal = document.getElementById("conversionModal");
        modal.style.display = "none";
        modal.setAttribute("aria-hidden", "true");
        activeLeadForConversion = null;
    }

    function handleCustomerModeChange(mode) {
        const mergeRow = document.getElementById("existingCustomerSelectorRow");
        const customerFields = document.getElementById("customerFieldsContainer");

        if (mode === "MERGE_EXISTING") {
            mergeRow.style.display = "block";
            customerFields.style.opacity = "0.5";
            customerFields.querySelectorAll("input, select").forEach(el => el.disabled = true);
        } else {
            mergeRow.style.display = "none";
            customerFields.style.opacity = "1";
            customerFields.querySelectorAll("input, select").forEach(el => el.disabled = false);
        }
    }

    // 8. EXECUTE 3-IN-1 CONVERSION
    async function submitConversion() {
        if (!activeLeadForConversion) return;

        const customerMode = document.querySelector('input[name="customerMode"]:checked').value;
        const existingCustomerId = document.getElementById("selectExistingCustomer").value;

        // Validate Card 1
        let customerData = {};
        if (customerMode === "MERGE_EXISTING") {
            if (!existingCustomerId) {
                showToast("Vui lòng chọn một Khách hàng doanh nghiệp đã có để gộp!", "warning");
                document.getElementById("selectExistingCustomer").focus();
                return;
            }
        } else {
            const custName = document.getElementById("custName").value.trim();
            if (!custName) {
                showToast("Vui lòng nhập Tên công ty / Doanh nghiệp!", "warning");
                document.getElementById("custName").focus();
                return;
            }
            customerData = {
                name: custName,
                taxCode: document.getElementById("custTaxCode").value.trim(),
                industry: document.getElementById("custIndustry").value,
                phone: document.getElementById("custPhone").value.trim(),
                website: document.getElementById("custWebsite").value.trim(),
                address: document.getElementById("custAddress").value.trim()
            };
        }

        // Validate Card 2: Contact
        const contactFullName = document.getElementById("contactFullName").value.trim();
        const contactPhone = document.getElementById("contactPhone").value.trim();
        const contactEmail = document.getElementById("contactEmail").value.trim();

        if (!contactFullName) {
            showToast("Vui lòng nhập Họ tên Người liên hệ!", "warning");
            document.getElementById("contactFullName").focus();
            return;
        }
        if (!contactPhone) {
            showToast("Vui lòng nhập Số điện thoại người liên hệ!", "warning");
            document.getElementById("contactPhone").focus();
            return;
        }
        if (!contactEmail) {
            showToast("Vui lòng nhập Email công vụ của người liên hệ!", "warning");
            document.getElementById("contactEmail").focus();
            return;
        }

        const contactData = {
            fullName: contactFullName,
            title: document.getElementById("contactTitle").value.trim(),
            buyingRole: document.getElementById("contactBuyingRole").value,
            phone: contactPhone,
            email: contactEmail,
            notes: document.getElementById("contactNotes").value.trim()
        };

        // Validate Card 3: Deal
        const createDeal = document.getElementById("checkCreateDeal").checked;
        let dealData = { createDeal: false };

        if (createDeal) {
            const dealName = document.getElementById("dealName").value.trim();
            const dealAmount = parseCurrency(document.getElementById("dealAmount").value);
            const dealCloseDate = document.getElementById("dealCloseDate").value;

            if (!dealName) {
                showToast("Vui lòng nhập Tên cơ hội bán hàng!", "warning");
                document.getElementById("dealName").focus();
                return;
            }
            if (dealAmount <= 0) {
                showToast("Vui lòng nhập Giá trị hợp đồng dự kiến hợp lệ (> 0đ)!", "warning");
                document.getElementById("dealAmount").focus();
                return;
            }
            if (!dealCloseDate) {
                showToast("Vui lòng chọn Ngày dự kiến chốt hợp đồng!", "warning");
                document.getElementById("dealCloseDate").focus();
                return;
            }

            const stageSelect = document.getElementById("dealStage");
            const selectedOpt = stageSelect.options[stageSelect.selectedIndex];
            const prob = selectedOpt.getAttribute("data-prob") || 40;

            dealData = {
                createDeal: true,
                name: dealName,
                amount: dealAmount,
                closeDate: dealCloseDate,
                pipeline: document.getElementById("dealPipeline").value,
                stageId: stageSelect.value,
                stageName: selectedOpt.text.split("(")[0].trim(),
                probability: Number(prob)
            };
        }

        const payload = {
            customerMode,
            existingCustomerId: customerMode === "MERGE_EXISTING" && existingCustomerId ? (Number(existingCustomerId) || null) : null,
            customer: customerData,
            contact: contactData,
            deal: dealData
        };

        const confirmBtn = document.getElementById("btnConfirmConversion");
        confirmBtn.disabled = true;
        confirmBtn.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> <span>Đang chuyển đổi...</span>`;

        try {
            const result = await LeadConversionService.convertLead(activeLeadForConversion.id, payload);

            // Close conversion modal
            closeConversionModal();

            // Refresh table & KPIs
            refreshData();

            // Open Success Modal
            openSuccessModal(result);

            // Rich Toast Notification with Clickable Links
            showToast(
                `🎉 Chuyển đổi Lead thành công! Khách hàng: <strong>${result.customerName}</strong>, Cơ hội: <strong>${result.dealName || "N/A"}</strong>`,
                "success",
                [
                    { label: "Xem Khách hàng", href: "customers.html" },
                    { label: "Xem Cơ hội", href: "pipeline.html" }
                ]
            );

        } catch (err) {
            showToast(err.message || "Lỗi trong quá trình chuyển đổi Lead!", "error");
        } finally {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = `<i class="fa-solid fa-arrows-split-up-and-left"></i> <span>Xác nhận chuyển đổi 3-trong-1</span>`;
        }
    }

    // 9. MODAL 2: SUCCESS CELEBRATION MODAL
    function openSuccessModal(result) {
        document.getElementById("successCustomerName").textContent = result.customerName;
        document.getElementById("successCustomerInfo").textContent = result.customerInfo;

        document.getElementById("successContactName").textContent = result.contactName;
        document.getElementById("successContactInfo").textContent = result.contactInfo;

        const dealRow = document.getElementById("successDealRow");
        if (result.dealId) {
            dealRow.style.display = "flex";
            document.getElementById("successDealName").textContent = result.dealName;
            document.getElementById("successDealInfo").textContent = result.dealInfo;
        } else {
            dealRow.style.display = "none";
        }

        const modal = document.getElementById("conversionSuccessModal");
        modal.style.display = "flex";
        modal.setAttribute("aria-hidden", "false");
    }

    function closeSuccessModal() {
        const modal = document.getElementById("conversionSuccessModal");
        modal.style.display = "none";
        modal.setAttribute("aria-hidden", "true");
    }

    // 10. MODAL 3: VIEW CONVERTED DETAILS (LOCKED AUDIT)
    function openConvertedDetailsModal(leadId) {
        const lead = LeadConversionService.getLeadById(leadId);
        if (!lead || !lead.convertedTo) {
            showToast("Không tìm thấy thông tin chuyển đổi của Lead!", "warning");
            return;
        }

        const conv = lead.convertedTo;
        document.getElementById("detailsConvertedTimestamp").textContent =
            `Chuyển đổi vào ngày ${formatDateTimeVi(lead.convertedAt)} bởi ${lead.convertedBy || "Nông Quang Tiệp"}`;

        const container = document.getElementById("linkedEntitiesContainer");
        container.innerHTML = `
            <div class="success-entity-item">
                <div class="item-icon-col icon-customer">
                    <i class="fa-solid fa-building"></i>
                </div>
                <div class="item-details-col">
                    <span class="item-tag">Khách hàng Doanh nghiệp</span>
                    <strong class="item-title">${conv.customerName || lead.companyName}</strong>
                    <span class="item-sub">Mã khách hàng: KH-${conv.customerId}</span>
                </div>
                <a href="customers.html" class="crm-btn crm-btn-secondary btn-sm" target="_blank">
                    <span>Xem Khách hàng</span>
                    <i class="fa-solid fa-arrow-up-right-from-square"></i>
                </a>
            </div>

            <div class="success-entity-item">
                <div class="item-icon-col icon-contact">
                    <i class="fa-solid fa-address-book"></i>
                </div>
                <div class="item-details-col">
                    <span class="item-tag">Người liên hệ chính</span>
                    <strong class="item-title">${conv.contactName || lead.fullName}</strong>
                    <span class="item-sub">Mã liên hệ: ${conv.contactId || "CT-Primary"}</span>
                </div>
                <a href="contacts.html" class="crm-btn crm-btn-secondary btn-sm" target="_blank">
                    <span>Xem Liên hệ</span>
                    <i class="fa-solid fa-arrow-up-right-from-square"></i>
                </a>
            </div>

            ${conv.dealName ? `
            <div class="success-entity-item">
                <div class="item-icon-col icon-deal">
                    <i class="fa-solid fa-bullseye"></i>
                </div>
                <div class="item-details-col">
                    <span class="item-tag">Cơ hội bán hàng</span>
                    <strong class="item-title">${conv.dealName}</strong>
                    <span class="item-sub">Giá trị hợp đồng: ${formatCurrency(conv.dealAmount || lead.estimatedValue)} ₫</span>
                </div>
                <a href="pipeline.html" class="crm-btn crm-btn-primary btn-sm" target="_blank">
                    <span>Mở Pipeline</span>
                    <i class="fa-solid fa-arrow-up-right-from-square"></i>
                </a>
            </div>` : ""}
        `;

        const modal = document.getElementById("convertedDetailsModal");
        modal.style.display = "flex";
        modal.setAttribute("aria-hidden", "false");
    }

    function closeConvertedDetailsModal() {
        const modal = document.getElementById("convertedDetailsModal");
        modal.style.display = "none";
        modal.setAttribute("aria-hidden", "true");
    }

    // 11. EXPORT REPORT TO CSV
    function exportConversionReport() {
        const leads = LeadConversionService.getLeads();
        if (!leads.length) {
            showToast("Không có dữ liệu để xuất báo cáo!", "warning");
            return;
        }

        const headers = ["Mã Lead", "Người đại diện", "Chức danh", "Công ty", "Ngành nghề", "Điểm", "Giá trị dự kiến", "Trạng thái", "Ngày chuyển đổi", "Người chuyển đổi"];
        const rows = leads.map(l => [
            `"${l.code}"`,
            `"${l.fullName}"`,
            `"${l.title || ""}"`,
            `"${l.companyName}"`,
            `"${l.industry || ""}"`,
            l.score,
            l.estimatedValue,
            `"${l.status === "CONVERTED" ? "Đã chuyển đổi (Đã khóa)" : "Đủ điều kiện"}"`,
            `"${l.convertedAt ? formatDateTimeVi(l.convertedAt) : ""}"`,
            `"${l.convertedBy || ""}"`
        ]);

        const csvContent = "\uFEFF" + [headers.join(","), ...rows.map(r => r.join(","))].join("\r\n");
        const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
        const link = document.createElement("a");
        link.href = URL.createObjectURL(blob);
        link.setAttribute("download", `Bao_Cao_Chuyen_Doi_Lead_${new Date().toISOString().split("T")[0]}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        showToast("Đã xuất báo cáo chuyển đổi Lead thành công!", "success");
    }

    // 12. ATTACH ALL DOM EVENT LISTENERS
    function initEventHandlers() {
        // Search & Filter
        const searchInput = document.getElementById("leadSearchInput");
        let searchTimer = null;
        searchInput.addEventListener("input", (e) => {
            clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                currentFilter.keyword = e.target.value.trim();
                currentPage = 1;
                refreshData();
            }, 300);
        });

        document.getElementById("filterStatus").addEventListener("change", (e) => {
            currentFilter.status = e.target.value;
            currentPage = 1;
            refreshData();
        });

        document.getElementById("filterTier").addEventListener("change", (e) => {
            currentFilter.tier = e.target.value;
            currentPage = 1;
            refreshData();
        });

        document.getElementById("filterIndustry").addEventListener("change", (e) => {
            currentFilter.industry = e.target.value;
            currentPage = 1;
            refreshData();
        });

        document.getElementById("sortLeads").addEventListener("change", (e) => {
            currentFilter.sort = e.target.value;
            refreshData();
        });

        // Reset Filter
        const resetFilters = () => {
            currentFilter = { keyword: "", status: "ALL", tier: "ALL", industry: "ALL", sort: "SCORE_DESC" };
            document.getElementById("leadSearchInput").value = "";
            document.getElementById("filterStatus").value = "ALL";
            document.getElementById("filterTier").value = "ALL";
            document.getElementById("filterIndustry").value = "ALL";
            document.getElementById("sortLeads").value = "SCORE_DESC";
            currentPage = 1;
            refreshData();
            showToast("Đã đặt lại bộ lọc tìm kiếm!", "success");
        };

        document.getElementById("btnResetFilter").addEventListener("click", resetFilters);
        document.getElementById("btnClearFiltersFromEmpty").addEventListener("click", resetFilters);

        // Header Actions
        document.getElementById("btnRefreshLeads").addEventListener("click", () => {
            refreshData();
            showToast("Đã cập nhật danh sách Lead mới nhất!", "success");
        });

        document.getElementById("btnResetMockData").addEventListener("click", () => {
            if (confirm("Khôi phục bộ dữ liệu mẫu chuẩn Sprint 4? Toàn bộ trạng thái chuyển đổi sẽ được đặt lại.")) {
                LeadConversionService.resetSampleData();
                currentPage = 1;
                refreshData();
                showToast("Đã khôi phục dữ liệu mẫu chuẩn Sprint 4!", "success");
            }
        });

        document.getElementById("btnExportReport").addEventListener("click", exportConversionReport);

        // Customer Mode Radios
        document.querySelectorAll('input[name="customerMode"]').forEach(r => {
            r.addEventListener("change", (e) => handleCustomerModeChange(e.target.value));
        });

        // Deal Toggle
        document.getElementById("checkCreateDeal").addEventListener("change", (e) => {
            const container = document.getElementById("dealFieldsContainer");
            container.style.display = e.target.checked ? "flex" : "none";
        });

        // Deal Amount Currency Formatter & Presets
        const dealAmountInput = document.getElementById("dealAmount");
        dealAmountInput.addEventListener("input", (e) => {
            const num = parseCurrency(e.target.value);
            e.target.value = formatCurrency(num);
        });

        document.querySelectorAll(".btn-preset").forEach(btn => {
            btn.addEventListener("click", () => {
                const amt = Number(btn.getAttribute("data-amount")) || 0;
                dealAmountInput.value = formatCurrency(amt);
            });
        });

        // Modal 1 controls
        document.getElementById("btnCloseConversionModal").addEventListener("click", closeConversionModal);
        document.getElementById("btnCancelConversion").addEventListener("click", closeConversionModal);
        document.getElementById("btnConfirmConversion").addEventListener("click", submitConversion);

        // Modal 2 controls
        document.getElementById("btnCloseSuccessModal").addEventListener("click", closeSuccessModal);

        // Modal 3 controls
        document.getElementById("btnCloseDetailsModal").addEventListener("click", closeConvertedDetailsModal);
        document.getElementById("btnDismissDetailsModal").addEventListener("click", closeConvertedDetailsModal);

        // Close on backdrop click
        document.querySelectorAll(".crm-modal-backdrop").forEach(backdrop => {
            backdrop.addEventListener("click", (e) => {
                if (e.target === backdrop) {
                    closeConversionModal();
                    closeSuccessModal();
                    closeConvertedDetailsModal();
                }
            });
        });

        // Keyboard Escape
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") {
                closeConversionModal();
                closeSuccessModal();
                closeConvertedDetailsModal();
            }
        });
    }

    // 13. DOM READY BOOTSTRAP
    document.addEventListener("DOMContentLoaded", async () => {
        LeadConversionService.init();
        initEventHandlers();
        refreshData();

        // Check if redirected from other screens with ?leadId=...
        const urlParams = new URLSearchParams(window.location.search);
        const leadIdParam = urlParams.get("leadId");
        if (leadIdParam) {
            await openConversionModal(leadIdParam);
        }
    });

})();
