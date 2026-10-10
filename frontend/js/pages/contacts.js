/**
 * ===================================================================
 * ENTERPRISE B2B CONTACT MANAGEMENT & DECISION ROLES CONTROLLER (CRM-62)
 * Pure HTML5 / CSS3 / ES6+ JavaScript client-side module.
 * - Quản lý hồ sơ người liên hệ đa tầng (1-N với khách hàng doanh nghiệp)
 * - Đánh dấu vai trò quyết định mua hàng (Decision Maker, Influencer, End User, Blocker)
 * - Chỉ định đầu mối liên hệ chính (Primary Contact) với cơ chế tự động cập nhật
 * - Điều chuyển người liên hệ sang công ty mới & bảo toàn lịch sử chuyển công tác
 * - Bền vững dữ liệu 100% qua LocalStorage: CRM_CONTACTS_DATA & CRM_CONTACT_HISTORY_DATA
 * ===================================================================
 */

"use strict";

(function () {
    // Required LocalStorage Keys as mandated by Acceptance Criteria
    const STORAGE_KEY_CONTACTS = "CRM_CONTACTS_DATA";
    const STORAGE_KEY_HISTORY = "CRM_CONTACT_HISTORY_DATA";
    const STORAGE_KEY_CUSTOMERS = "CRM_CUSTOMERS_DATA";

    // Decision Roles Definition (CRM-62)
    const DECISION_ROLES = {
        DECISION_MAKER: {
            code: "DECISION_MAKER",
            label: "Người quyết định",
            englishLabel: "Decision Maker",
            icon: "👑",
            cssClass: "role-decision-maker",
            accentClass: "accent-decision-maker",
            kpiClass: "kpi-purple",
            desc: "Nắm ngân sách & ký duyệt hợp đồng cuối cùng. Trọng tâm chốt deal."
        },
        INFLUENCER: {
            code: "INFLUENCER",
            label: "Người ảnh hưởng",
            englishLabel: "Influencer",
            icon: "⚡",
            cssClass: "role-influencer",
            accentClass: "accent-influencer",
            kpiClass: "kpi-blue",
            desc: "Chuyên môn cao, định hướng tiêu chuẩn kỹ thuật & tác động đến lựa chọn."
        },
        END_USER: {
            code: "END_USER",
            label: "Người dùng cuối",
            englishLabel: "End User",
            icon: "👤",
            cssClass: "role-end-user",
            accentClass: "accent-end-user",
            kpiClass: "kpi-green",
            desc: "Trực tiếp vận hành hằng ngày, phản hồi trải nghiệm & tính năng thực tế."
        },
        BLOCKER: {
            code: "BLOCKER",
            label: "Người cản trở",
            englishLabel: "Blocker / Gatekeeper",
            icon: "⚠️",
            cssClass: "role-blocker",
            accentClass: "accent-blocker",
            kpiClass: "kpi-red",
            desc: "Cảnh báo rủi ro: Có xu hướng trì hoãn, thắt chặt ngân sách hoặc chặn đàm phán."
        }
    };

    // Color palette for contact avatars
    const AVATAR_GRADIENTS = [
        "linear-gradient(135deg, #6366f1, #4f46e5)",
        "linear-gradient(135deg, #3b82f6, #1d4ed8)",
        "linear-gradient(135deg, #0ea5e9, #0284c7)",
        "linear-gradient(135deg, #10b981, #059669)",
        "linear-gradient(135deg, #f59e0b, #d97706)",
        "linear-gradient(135deg, #8b5cf6, #6d28d9)",
        "linear-gradient(135deg, #ec4899, #be185d)"
    ];

    // Seed Mock Contacts (Robust Enterprise Data across B2B clients)
    const DEFAULT_MOCK_CONTACTS = [];
    const _UNUSED_DEFAULT_MOCK_CONTACTS = [
        {
            id: 1,
            customerId: 1,
            companyName: "Tập đoàn Công nghệ FPT",
            fullName: "Trương Gia Bình",
            jobTitle: "Chủ tịch HĐQT",
            department: "Ban Quản trị & Điều hành",
            email: "binhtg@fpt.com.vn",
            phone: "0913222333",
            decisionRole: "DECISION_MAKER",
            isPrimary: true,
            status: "ACTIVE",
            notes: "Người phê duyệt ngân sách cấp cao nhất cho các dự án chuyển đổi số toàn tập đoàn.",
            createdAt: "2026-01-15T08:30:00",
            updatedAt: "2026-03-20T10:00:00"
        },
        {
            id: 2,
            customerId: 1,
            companyName: "Tập đoàn Công nghệ FPT",
            fullName: "Nguyễn Văn Khoa",
            jobTitle: "Tổng Giám đốc (CEO)",
            department: "Ban Điều hành",
            email: "khoanv@fpt.com.vn",
            phone: "0903444555",
            decisionRole: "INFLUENCER",
            isPrimary: false,
            status: "ACTIVE",
            notes: "Ủng hộ ứng dụng công nghệ hiện đại, quan tâm sâu sát đến chỉ số ROI và hiệu suất triển khai.",
            createdAt: "2026-01-16T09:15:00",
            updatedAt: "2026-03-20T10:00:00"
        },
        {
            id: 3,
            customerId: 1,
            companyName: "Tập đoàn Công nghệ FPT",
            fullName: "Đỗ Cao Bảo",
            jobTitle: "Giám đốc CNTT (CIO)",
            department: "Khối Công nghệ & Hạ tầng",
            email: "baodc@fpt.com.vn",
            phone: "0912555666",
            decisionRole: "BLOCKER",
            isPrimary: false,
            status: "ACTIVE",
            notes: "Yêu cầu kiểm định an toàn thông tin khắt khe; ưu tiên giải pháp nội bộ tự phát triển nếu nhà cung cấp không chứng minh được SLA.",
            createdAt: "2026-01-20T14:00:00",
            updatedAt: "2026-03-20T10:00:00"
        },
        {
            id: 4,
            customerId: 1,
            companyName: "Tập đoàn Công nghệ FPT",
            fullName: "Phạm Minh Thắng",
            jobTitle: "Trưởng phòng Mua sắm & Đấu thầu",
            department: "Phòng Mua sắm",
            email: "thangpm@fpt.com.vn",
            phone: "0987654321",
            decisionRole: "END_USER",
            isPrimary: false,
            status: "ACTIVE",
            notes: "Vận hành trực tiếp hệ thống đối chiếu đơn hàng và nghiệm thu dịch vụ định kỳ.",
            createdAt: "2026-02-01T11:00:00",
            updatedAt: "2026-03-20T10:00:00"
        },
        {
            id: 5,
            customerId: 2,
            companyName: "Công ty Cổ phần VNG",
            fullName: "Lê Hồng Minh",
            jobTitle: "Chủ tịch & Sáng lập",
            department: "Ban Lãnh đạo",
            email: "minhlh@vng.com.vn",
            phone: "0908111222",
            decisionRole: "DECISION_MAKER",
            isPrimary: true,
            status: "ACTIVE",
            notes: "Quyết định toàn diện các gói giải pháp hạ tầng máy chủ và bảo mật dữ liệu khách hàng.",
            createdAt: "2026-02-10T10:00:00",
            updatedAt: "2026-03-21T09:00:00"
        },
        {
            id: 6,
            customerId: 2,
            companyName: "Công ty Cổ phần VNG",
            fullName: "Vương Quang Khải",
            jobTitle: "Phó Tổng Giám đốc Zalo",
            department: "Khối Nền tảng & AI",
            email: "khaivq@vng.com.vn",
            phone: "0918333444",
            decisionRole: "INFLUENCER",
            isPrimary: false,
            status: "ACTIVE",
            notes: "Tác động lớn đến kiến trúc tích hợp hệ thống tin nhắn OTT và API chăm sóc khách hàng.",
            createdAt: "2026-02-12T14:30:00",
            updatedAt: "2026-03-21T09:00:00"
        },
        {
            id: 7,
            customerId: 2,
            companyName: "Công ty Cổ phần VNG",
            fullName: "Trần Anh Dũng",
            jobTitle: "Giám đốc Kiểm soát Tài chính",
            department: "Phòng Tài chính - Kế toán",
            email: "dungta@vng.com.vn",
            phone: "0909666777",
            decisionRole: "BLOCKER",
            isPrimary: false,
            status: "ACTIVE",
            notes: "Chặt chẽ về điều khoản thanh toán và phạt trễ hạn; thường yêu cầu đàm phán chiết khấu thêm 15%.",
            createdAt: "2026-02-15T16:00:00",
            updatedAt: "2026-03-21T09:00:00"
        },
        {
            id: 8,
            customerId: 3,
            companyName: "Công ty TNHH Phần mềm MISA",
            fullName: "Lữ Thành Long",
            jobTitle: "Chủ tịch HĐQT",
            department: "Hội đồng Quản trị",
            email: "longlt@misa.vn",
            phone: "0913999888",
            decisionRole: "DECISION_MAKER",
            isPrimary: true,
            status: "ACTIVE",
            notes: "Đại diện thẩm quyền tối cao, quan tâm đến quan hệ hợp tác chiến lược B2B lâu dài.",
            createdAt: "2026-03-01T08:00:00",
            updatedAt: "2026-03-25T11:00:00"
        },
        {
            id: 9,
            customerId: 3,
            companyName: "Công ty TNHH Phần mềm MISA",
            fullName: "Đinh Thị Thúy",
            jobTitle: "Tổng Giám đốc",
            department: "Ban Điều hành",
            email: "thuydt@misa.vn",
            phone: "0982555444",
            decisionRole: "INFLUENCER",
            isPrimary: false,
            status: "ACTIVE",
            notes: "Theo dõi sát sao tiến độ bàn giao và đảm bảo quy chuẩn đào tạo nhân sự.",
            createdAt: "2026-03-02T09:30:00",
            updatedAt: "2026-03-25T11:00:00"
        },
        {
            id: 10,
            customerId: 3,
            companyName: "Công ty TNHH Phần mềm MISA",
            fullName: "Hoàng Nam",
            jobTitle: "Trưởng nhóm Hỗ trợ Kỹ thuật",
            department: "Trung tâm Chăm sóc Khách hàng",
            email: "namh@misa.vn",
            phone: "0976123456",
            decisionRole: "END_USER",
            isPrimary: false,
            status: "ACTIVE",
            notes: "Người dùng trực tiếp giải quyết khiếu nại và đánh giá độ tiện dụng của giao diện.",
            createdAt: "2026-03-05T15:00:00",
            updatedAt: "2026-03-25T11:00:00"
        },
        {
            id: 11,
            customerId: 4,
            companyName: "Tập đoàn Bưu chính Viễn thông VNPT",
            fullName: "Tô Dũng Thái",
            jobTitle: "Chủ tịch Hội đồng Thành viên",
            department: "Ban Lãnh đạo",
            email: "thaitd@vnpt.vn",
            phone: "0913555777",
            decisionRole: "DECISION_MAKER",
            isPrimary: true,
            status: "ACTIVE",
            notes: "Ký kết phê duyệt các dự án đầu tư phần mềm trọng điểm cấp bộ ngành.",
            createdAt: "2026-01-10T09:00:00",
            updatedAt: "2026-03-22T14:00:00"
        },
        {
            id: 12,
            customerId: 4,
            companyName: "Tập đoàn Bưu chính Viễn thông VNPT",
            fullName: "Lê Quốc Hùng",
            jobTitle: "Trưởng phòng Pháp chế & Rủi ro",
            department: "Khối Pháp chế",
            email: "hunglq@vnpt.vn",
            phone: "0918777999",
            decisionRole: "BLOCKER",
            isPrimary: false,
            status: "ACTIVE",
            notes: "Soát xét hợp đồng rất kỹ lưỡng, yêu cầu tuân thủ đầy đủ nghị định bảo vệ dữ liệu cá nhân 13/2023/NĐ-CP.",
            createdAt: "2026-01-18T10:00:00",
            updatedAt: "2026-03-22T14:00:00"
        },
        {
            id: 13,
            customerId: 5,
            companyName: "Tập đoàn Công nghiệp - Viễn thông Quân đội (Viettel)",
            fullName: "Tào Đức Thắng",
            jobTitle: "Chủ tịch kiêm Tổng Giám đốc",
            department: "Ban Tổng Giám đốc",
            email: "thangtd@viettel.com.vn",
            phone: "0983111000",
            decisionRole: "DECISION_MAKER",
            isPrimary: true,
            status: "ACTIVE",
            notes: "Đòi hỏi kỷ luật triển khai cao, phương án dự phòng sự cố 24/7.",
            createdAt: "2026-02-01T08:00:00",
            updatedAt: "2026-03-24T16:00:00"
        },
        {
            id: 14,
            customerId: 8,
            companyName: "Công ty Cổ phần Tập đoàn Hòa Phát",
            fullName: "Trần Đình Long",
            jobTitle: "Chủ tịch HĐQT",
            department: "Ban Quản trị",
            email: "longtd@hoaphat.com.vn",
            phone: "0903888777",
            decisionRole: "DECISION_MAKER",
            isPrimary: true,
            status: "ACTIVE",
            notes: "Tập trung giải pháp quản lý chuỗi cung ứng thép và kho vận bãi cảng.",
            createdAt: "2026-01-25T10:00:00",
            updatedAt: "2026-03-20T10:00:00"
        }
    ];

    // Seed Mock Career History (Audit Trail of Contact Transfers)
    const DEFAULT_MOCK_HISTORY = [];
    const _UNUSED_DEFAULT_MOCK_HISTORY = [
        {
            id: 101,
            contactId: 3,
            contactName: "Đỗ Cao Bảo",
            fromCustomerId: 4,
            fromCompanyName: "Tập đoàn Bưu chính Viễn thông VNPT",
            toCustomerId: 1,
            toCompanyName: "Tập đoàn Công nghệ FPT",
            oldJobTitle: "Phó Giám đốc Trung tâm Kỹ thuật",
            newJobTitle: "Giám đốc CNTT (CIO)",
            transferDate: "2025-11-15",
            reason: "Chuyển công tác sang FPT phụ trách chỉ đạo chuyển đổi số hạ tầng cloud toàn tập đoàn.",
            loggedAt: "2025-11-15T09:30:00",
            loggedBy: "Nông Quang Tiệp (Admin)"
        },
        {
            id: 102,
            contactId: 7,
            contactName: "Trần Anh Dũng",
            fromCustomerId: 3,
            fromCompanyName: "Công ty TNHH Phần mềm MISA",
            toCustomerId: 2,
            toCompanyName: "Công ty Cổ phần VNG",
            oldJobTitle: "Trưởng phòng Kế toán Quản trị",
            newJobTitle: "Giám đốc Kiểm soát Tài chính",
            transferDate: "2025-08-01",
            reason: "Chuyển sang VNG phụ trách bộ phận kiểm toán nội bộ và quản trị rủi ro dòng tiền.",
            loggedAt: "2025-08-01T14:15:00",
            loggedBy: "Nông Quang Tiệp (Admin)"
        }
    ];

    // Helper: Escape HTML
    function escapeHtml(str) {
        if (str === null || str === undefined) return "";
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    // Helper: Toast notifications
    function showToast(message, type = "success") {
        let region = document.querySelector(".crm-toast-region");
        if (!region) {
            region = document.createElement("div");
            region.className = "crm-toast-region";
            document.body.appendChild(region);
        }

        const toast = document.createElement("div");
        toast.className = `crm-toast crm-toast-${type}`;
        toast.innerHTML = `
            <span class="crm-toast-icon">
                <svg viewBox="0 0 24 24">
                    ${type === "success" 
                        ? '<path d="M20 6L9 17l-5-5"/>' 
                        : (type === "error" 
                            ? '<circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>' 
                            : '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>')}
                </svg>
            </span>
            <span class="crm-toast-copy">${escapeHtml(message)}</span>
            <button class="crm-toast-close" type="button" aria-label="Đóng">✕</button>
            <div class="crm-toast-progress"></div>
        `;

        toast.querySelector(".crm-toast-close").addEventListener("click", () => toast.remove());
        region.appendChild(toast);

        setTimeout(() => {
            if (toast.parentNode) {
                toast.style.opacity = "0";
                toast.style.transform = "translateX(20px)";
                toast.style.transition = "all 200ms ease";
                setTimeout(() => toast.remove(), 200);
            }
        }, 4000);
    }

    // Helper: Avatar background gradient by name hash
    function getAvatarGradient(name = "") {
        let hash = 0;
        for (let i = 0; i < name.length; i++) {
            hash = name.charCodeAt(i) + ((hash << 5) - hash);
        }
        const index = Math.abs(hash) % AVATAR_GRADIENTS.length;
        return AVATAR_GRADIENTS[index];
    }

    // Helper: Initials
    function getInitials(name = "") {
        const parts = name.trim().split(/\s+/);
        if (parts.length === 0 || !parts[0]) return "U";
        if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
        return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    // Helper: Validate Vietnamese Mobile Phone
    function isValidVietnamesePhone(phone) {
        if (!phone) return false;
        const cleaned = phone.replace(/[\s.-]/g, "");
        // Standard VN mobile: 10 digits starting with 03, 05, 07, 08, 09; or landline 024, 028...
        const regex = /^(0)(3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$/;
        const landlineRegex = /^(02)[0-9]{9}$/;
        return regex.test(cleaned) || landlineRegex.test(cleaned);
    }

    // Helper: Validate Email
    function isValidEmail(email) {
        if (!email) return false;
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
    }

    // ===================================================================
    // CONTACTS CORE DATA MANAGER (PERSISTENCE LAYER)
    // ===================================================================
    class ContactsStore {
        constructor() {
            this.initStore();
        }

        initStore() {
            // Contacts table persistence
            try {
                const raw = localStorage.getItem(STORAGE_KEY_CONTACTS);
                if (!raw) {
                    localStorage.setItem(STORAGE_KEY_CONTACTS, JSON.stringify(DEFAULT_MOCK_CONTACTS));
                }
            } catch (e) {
                console.error("Error accessing localStorage for contacts:", e);
            }

            // History table persistence
            try {
                const rawHistory = localStorage.getItem(STORAGE_KEY_HISTORY);
                if (!rawHistory) {
                    localStorage.setItem(STORAGE_KEY_HISTORY, JSON.stringify(DEFAULT_MOCK_HISTORY));
                }
            } catch (e) {
                console.error("Error accessing localStorage for history:", e);
            }
        }

        getAllContacts() {
            try {
                const raw = localStorage.getItem(STORAGE_KEY_CONTACTS);
                return raw ? JSON.parse(raw) : [...DEFAULT_MOCK_CONTACTS];
            } catch (_) {
                return [...DEFAULT_MOCK_CONTACTS];
            }
        }

        saveContacts(list) {
            try {
                localStorage.setItem(STORAGE_KEY_CONTACTS, JSON.stringify(list));
            } catch (e) {
                console.error("Failed to save contacts to localStorage:", e);
            }
        }

        getAllHistory() {
            try {
                const raw = localStorage.getItem(STORAGE_KEY_HISTORY);
                return raw ? JSON.parse(raw) : [...DEFAULT_MOCK_HISTORY];
            } catch (_) {
                return [...DEFAULT_MOCK_HISTORY];
            }
        }

        saveHistory(list) {
            try {
                localStorage.setItem(STORAGE_KEY_HISTORY, JSON.stringify(list));
            } catch (e) {
                console.error("Failed to save history to localStorage:", e);
            }
        }

        getAvailableCustomers() {
            try {
                const raw = localStorage.getItem(STORAGE_KEY_CUSTOMERS);
                if (raw) {
                    const parsed = JSON.parse(raw);
                    if (Array.isArray(parsed) && parsed.length) return parsed;
                }
            } catch (_) {}

            // Fallback enterprise customers
            return [
                { id: 1, companyName: "Tập đoàn Công nghệ FPT", taxCode: "0101248141" },
                { id: 2, companyName: "Công ty Cổ phần VNG", taxCode: "0303579890" },
                { id: 3, companyName: "Công ty TNHH Phần mềm MISA", taxCode: "0100779774" },
                { id: 4, companyName: "Tập đoàn Bưu chính Viễn thông VNPT", taxCode: "0100684378" },
                { id: 5, companyName: "Tập đoàn Công nghiệp - Viễn thông Quân đội (Viettel)", taxCode: "0100109106" },
                { id: 6, companyName: "Công ty Cổ phần Sữa Việt Nam (Vinamilk)", taxCode: "0300588569" },
                { id: 7, companyName: "Công ty Cổ phần Đầu tư Thế Giới Di Động", taxCode: "0303270651" },
                { id: 8, companyName: "Công ty Cổ phần Tập đoàn Hòa Phát", taxCode: "0900189284" }
            ];
        }

        getCustomerById(customerId) {
            const list = this.getAvailableCustomers();
            return list.find(c => Number(c.id) === Number(customerId)) || null;
        }

        getContactsByCustomer(customerId) {
            const all = this.getAllContacts();
            return all.filter(c => Number(c.customerId) === Number(customerId));
        }

        getContactById(id) {
            const all = this.getAllContacts();
            return all.find(c => Number(c.id) === Number(id)) || null;
        }

        getContactHistory(contactId) {
            const allHistory = this.getAllHistory();
            return allHistory
                .filter(h => Number(h.contactId) === Number(contactId))
                .sort((a, b) => new Date(b.transferDate) - new Date(a.transferDate));
        }

        /**
         * Add a new Contact.
         * If marked isPrimary = true, unset other contacts of the same customer.
         */
        addContact(payload) {
            const all = this.getAllContacts();
            const newId = all.length ? Math.max(...all.map(c => Number(c.id) || 0)) + 1 : 1;

            const custId = Number(payload.customerId);
            const customer = this.getCustomerById(custId);
            const compName = customer ? (customer.companyName || customer.name) : (payload.companyName || "Chưa xác định");

            const isPrimary = !!payload.isPrimary;

            // If new contact is primary, clear primary flag on sibling contacts
            if (isPrimary) {
                all.forEach(c => {
                    if (Number(c.customerId) === custId) {
                        c.isPrimary = false;
                    }
                });
            }

            const newContact = {
                id: newId,
                customerId: custId,
                companyName: compName,
                fullName: payload.fullName.trim(),
                jobTitle: payload.jobTitle.trim(),
                department: (payload.department || "").trim(),
                email: payload.email.trim(),
                phone: payload.phone.trim(),
                decisionRole: payload.decisionRole || "END_USER",
                isPrimary: isPrimary,
                status: payload.status || "ACTIVE",
                notes: (payload.notes || "").trim(),
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString()
            };

            all.unshift(newContact);
            this.saveContacts(all);
            return newContact;
        }

        /**
         * Update an existing contact.
         */
        updateContact(id, payload) {
            const all = this.getAllContacts();
            const index = all.findIndex(c => Number(c.id) === Number(id));
            if (index === -1) throw new Error("Không tìm thấy người liên hệ cần cập nhật.");

            const old = all[index];
            const custId = Number(payload.customerId || old.customerId);
            const isPrimary = payload.isPrimary !== undefined ? !!payload.isPrimary : old.isPrimary;

            // If becoming primary, unset siblings
            if (isPrimary && !old.isPrimary) {
                all.forEach(c => {
                    if (Number(c.customerId) === custId && Number(c.id) !== Number(id)) {
                        c.isPrimary = false;
                    }
                });
            }

            const customer = this.getCustomerById(custId);
            const compName = customer ? (customer.companyName || customer.name) : (payload.companyName || old.companyName);

            all[index] = {
                ...old,
                customerId: custId,
                companyName: compName,
                fullName: payload.fullName !== undefined ? payload.fullName.trim() : old.fullName,
                jobTitle: payload.jobTitle !== undefined ? payload.jobTitle.trim() : old.jobTitle,
                department: payload.department !== undefined ? (payload.department || "").trim() : old.department,
                email: payload.email !== undefined ? payload.email.trim() : old.email,
                phone: payload.phone !== undefined ? payload.phone.trim() : old.phone,
                decisionRole: payload.decisionRole || old.decisionRole,
                isPrimary: isPrimary,
                status: payload.status || old.status,
                notes: payload.notes !== undefined ? (payload.notes || "").trim() : old.notes,
                updatedAt: new Date().toISOString()
            };

            this.saveContacts(all);
            return all[index];
        }

        /**
         * Delete a contact
         */
        deleteContact(id) {
            let all = this.getAllContacts();
            const target = all.find(c => Number(c.id) === Number(id));
            if (!target) return false;

            all = all.filter(c => Number(c.id) !== Number(id));

            // If deleted contact was primary, automatically assign primary to the first active sibling
            if (target.isPrimary) {
                const sibling = all.find(c => Number(c.customerId) === Number(target.customerId) && c.status === "ACTIVE");
                if (sibling) {
                    sibling.isPrimary = true;
                }
            }

            this.saveContacts(all);
            return true;
        }

        /**
         * Set Primary Contact (Đầu mối chính)
         * Only 1 contact is primary per company. Automatically unset others.
         */
        setPrimaryContact(contactId) {
            const all = this.getAllContacts();
            const target = all.find(c => Number(c.id) === Number(contactId));
            if (!target) throw new Error("Không tìm thấy người liên hệ.");

            const custId = Number(target.customerId);

            all.forEach(c => {
                if (Number(c.customerId) === custId) {
                    c.isPrimary = Number(c.id) === Number(contactId);
                    c.updatedAt = new Date().toISOString();
                }
            });

            this.saveContacts(all);
            return target;
        }

        /**
         * Re-assign Contact to a new Company (Điều chuyển công tác)
         * Creates an audit trail record in CRM_CONTACT_HISTORY_DATA.
         */
        transferContact(contactId, { toCustomerId, newJobTitle, newDepartment, transferDate, reason, setAsPrimaryInNewComp }) {
            const all = this.getAllContacts();
            const target = all.find(c => Number(c.id) === Number(contactId));
            if (!target) throw new Error("Không tìm thấy người liên hệ để điều chuyển.");

            const fromCustomerId = Number(target.customerId);
            const fromCompanyName = target.companyName;
            const oldJobTitle = target.jobTitle;

            const targetCustId = Number(toCustomerId);
            const newCustomer = this.getCustomerById(targetCustId);
            if (!newCustomer) throw new Error("Không tìm thấy khách hàng doanh nghiệp đích.");
            const toCompanyName = newCustomer.companyName || newCustomer.name;

            if (fromCustomerId === targetCustId) {
                throw new Error("Người liên hệ đã thuộc doanh nghiệp này. Vui lòng chọn doanh nghiệp khác.");
            }

            // 1. Create Career History Trail
            const allHistory = this.getAllHistory();
            const newHistoryId = allHistory.length ? Math.max(...allHistory.map(h => Number(h.id) || 0)) + 1 : 101;

            const historyRecord = {
                id: newHistoryId,
                contactId: Number(contactId),
                contactName: target.fullName,
                fromCustomerId: fromCustomerId,
                fromCompanyName: fromCompanyName,
                toCustomerId: targetCustId,
                toCompanyName: toCompanyName,
                oldJobTitle: oldJobTitle,
                newJobTitle: (newJobTitle || target.jobTitle).trim(),
                transferDate: transferDate || new Date().toISOString().split("T")[0],
                reason: (reason || "Điều chuyển công tác sang đơn vị mới.").trim(),
                loggedAt: new Date().toISOString(),
                loggedBy: "Nông Quang Tiệp (Quản trị viên)"
            };

            allHistory.unshift(historyRecord);
            this.saveHistory(allHistory);

            // 2. Handle Primary Flag at Old Company (promote another sibling if available)
            if (target.isPrimary) {
                const oldSibling = all.find(c => Number(c.customerId) === fromCustomerId && Number(c.id) !== Number(contactId) && c.status === "ACTIVE");
                if (oldSibling) {
                    oldSibling.isPrimary = true;
                }
            }

            // 3. Handle Primary Flag at New Company
            const isNewPrimary = !!setAsPrimaryInNewComp;
            if (isNewPrimary) {
                all.forEach(c => {
                    if (Number(c.customerId) === targetCustId) {
                        c.isPrimary = false;
                    }
                });
            }

            // 4. Update Contact Profile
            target.customerId = targetCustId;
            target.companyName = toCompanyName;
            target.jobTitle = (newJobTitle || target.jobTitle).trim();
            if (newDepartment) target.department = newDepartment.trim();
            target.isPrimary = isNewPrimary;
            target.updatedAt = new Date().toISOString();

            this.saveContacts(all);

            return { contact: target, history: historyRecord };
        }
    }

    // Instantiate Store
    const store = new ContactsStore();

    // ===================================================================
    // UI RENDERING & CONTROLLER LAYER
    // ===================================================================

    // State for contacts.html page
    let pageState = {
        searchTerm: "",
        roleFilter: "ALL", // ALL | DECISION_MAKER | INFLUENCER | END_USER | BLOCKER | PRIMARY
        customerFilter: "ALL",
        statusFilter: "ALL",
        viewMode: "CARD" // CARD | TABLE
    };

    /**
     * Build Badge HTML for Decision Role
     */
    function renderDecisionRoleBadge(roleCode) {
        const role = DECISION_ROLES[roleCode] || DECISION_ROLES.END_USER;
        return `
            <span class="role-badge ${role.cssClass}" title="${escapeHtml(role.desc)}">
                <span class="badge-icon">${role.icon}</span>
                <span>${escapeHtml(role.label)}</span>
            </span>
        `;
    }

    /**
     * Build Primary Contact Badge HTML
     */
    function renderPrimaryBadge(isPrimary) {
        if (!isPrimary) return "";
        return `
            <span class="primary-contact-badge" title="Đầu mối giao dịch và liên lạc trọng yếu của doanh nghiệp">
                <span class="star-icon">⭐</span>
                <span>Đầu mối chính</span>
            </span>
        `;
    }

    /**
     * Calculate KPI metrics
     */
    function getMetrics() {
        const contacts = store.getAllContacts();
        return {
            total: contacts.length,
            decisionMakers: contacts.filter(c => c.decisionRole === "DECISION_MAKER").length,
            influencers: contacts.filter(c => c.decisionRole === "INFLUENCER").length,
            blockers: contacts.filter(c => c.decisionRole === "BLOCKER").length,
            primary: contacts.filter(c => c.isPrimary).length,
            endUsers: contacts.filter(c => c.decisionRole === "END_USER").length
        };
    }

    /**
     * Filter contacts according to current state
     */
    function getFilteredContacts() {
        const all = store.getAllContacts();
        return all.filter(c => {
            // Role Filter
            if (pageState.roleFilter === "PRIMARY") {
                if (!c.isPrimary) return false;
            } else if (pageState.roleFilter !== "ALL") {
                if (c.decisionRole !== pageState.roleFilter) return false;
            }

            // Customer Filter
            if (pageState.customerFilter !== "ALL") {
                if (Number(c.customerId) !== Number(pageState.customerFilter)) return false;
            }

            // Status Filter
            if (pageState.statusFilter !== "ALL") {
                if (c.status !== pageState.statusFilter) return false;
            }

            // Search Term
            if (pageState.searchTerm) {
                const term = pageState.searchTerm.toLowerCase();
                const matchName = (c.fullName || "").toLowerCase().includes(term);
                const matchEmail = (c.email || "").toLowerCase().includes(term);
                const matchPhone = (c.phone || "").toLowerCase().includes(term);
                const matchTitle = (c.jobTitle || "").toLowerCase().includes(term);
                const matchComp = (c.companyName || "").toLowerCase().includes(term);
                const matchDept = (c.department || "").toLowerCase().includes(term);
                if (!matchName && !matchEmail && !matchPhone && !matchTitle && !matchComp && !matchDept) {
                    return false;
                }
            }

            return true;
        });
    }

    /**
     * Render KPI Summary Cards
     */
    function renderKpiCards() {
        const metrics = getMetrics();

        const map = [
            { id: "kpiTotal", value: metrics.total, filter: "ALL" },
            { id: "kpiDecision", value: metrics.decisionMakers, filter: "DECISION_MAKER" },
            { id: "kpiInfluencer", value: metrics.influencers, filter: "INFLUENCER" },
            { id: "kpiBlocker", value: metrics.blockers, filter: "BLOCKER" },
            { id: "kpiPrimary", value: metrics.primary, filter: "PRIMARY" }
        ];

        map.forEach(item => {
            const el = document.getElementById(item.id);
            if (el) el.textContent = item.value;
        });

        // Update counts inside role pills
        const pillDecision = document.getElementById("pillCountDecision");
        if (pillDecision) pillDecision.textContent = metrics.decisionMakers;
        const pillInfluencer = document.getElementById("pillCountInfluencer");
        if (pillInfluencer) pillInfluencer.textContent = metrics.influencers;
        const pillEndUser = document.getElementById("pillCountEndUser");
        if (pillEndUser) pillEndUser.textContent = metrics.endUsers;
        const pillBlocker = document.getElementById("pillCountBlocker");
        if (pillBlocker) pillBlocker.textContent = metrics.blockers;
        const pillPrimary = document.getElementById("pillCountPrimary");
        if (pillPrimary) pillPrimary.textContent = metrics.primary;
        const pillAll = document.getElementById("pillCountAll");
        if (pillAll) pillAll.textContent = metrics.total;
    }

    /**
     * Render Contact Card HTML
     */
    function renderContactCardHtml(contact) {
        const role = DECISION_ROLES[contact.decisionRole] || DECISION_ROLES.END_USER;
        const gradient = getAvatarGradient(contact.fullName);
        const initials = getInitials(contact.fullName);
        const historyList = store.getContactHistory(contact.id);
        const latestHistory = historyList.length ? historyList[0] : null;

        return `
            <div class="contact-card ${role.accentClass}" data-contact-id="${contact.id}">
                <div class="contact-card-header">
                    <div class="contact-card-profile">
                        <div class="contact-avatar" style="background:${gradient};">
                            ${initials}
                        </div>
                        <div class="contact-name-info">
                            <h4 class="contact-full-name" title="${escapeHtml(contact.fullName)}">
                                ${escapeHtml(contact.fullName)}
                            </h4>
                            <p class="contact-job-title" title="${escapeHtml(contact.jobTitle)} - ${escapeHtml(contact.department || 'Đơn vị')}">
                                ${escapeHtml(contact.jobTitle)}${contact.department ? ` · ${escapeHtml(contact.department)}` : ""}
                            </p>
                        </div>
                    </div>
                </div>

                <div class="contact-card-badges">
                    ${renderDecisionRoleBadge(contact.decisionRole)}
                    ${renderPrimaryBadge(contact.isPrimary)}
                    <span class="status-pill ${contact.status === 'ACTIVE' ? 'status-active' : 'status-inactive'}">
                        ● ${contact.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng'}
                    </span>
                </div>

                <div class="contact-company-row">
                    <svg viewBox="0 0 24 24"><path d="M3 21h18M5 21V7l8-4v18M13 7l6 4v10M9 9v.01M9 13v.01M9 17v.01M17 13v.01M17 17v.01"/></svg>
                    <a href="customer-360.html?id=${contact.customerId}" class="contact-company-link" title="Xem hồ sơ 360° ${escapeHtml(contact.companyName)}">
                        ${escapeHtml(contact.companyName)}
                    </a>
                </div>

                <div class="contact-comms-list">
                    <div class="contact-comm-item" title="Email công việc">
                        <svg viewBox="0 0 24 24"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
                        <a href="mailto:${escapeHtml(contact.email)}">${escapeHtml(contact.email)}</a>
                    </div>
                    <div class="contact-comm-item" title="Số điện thoại di động">
                        <svg viewBox="0 0 24 24"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
                        <a href="tel:${escapeHtml(contact.phone)}">${escapeHtml(contact.phone)}</a>
                    </div>
                </div>

                ${contact.notes ? `
                    <div class="contact-strategy-note">
                        <strong>Chiến lược tiếp cận:</strong> ${escapeHtml(contact.notes)}
                    </div>
                ` : ""}

                ${latestHistory ? `
                    <div class="contact-history-badge">
                        <span>🔄</span>
                        <span>Từng là <em>${escapeHtml(latestHistory.oldJobTitle)}</em> tại <strong>${escapeHtml(latestHistory.fromCompanyName)}</strong></span>
                    </div>
                ` : ""}

                <div class="contact-card-actions">
                    <div style="display:flex; gap:6px;">
                        ${!contact.isPrimary ? `
                            <button class="contact-action-btn btn-set-primary" type="button" data-action="set-primary" data-id="${contact.id}" title="Chỉ định làm Đầu mối liên hệ chính của công ty">
                                ⭐ Đặt đầu mối chính
                            </button>
                        ` : ""}
                        <button class="contact-action-btn btn-transfer" type="button" data-action="transfer" data-id="${contact.id}" title="Chuyển sang doanh nghiệp khác khi chuyển công tác">
                            🔄 Chuyển công ty
                        </button>
                    </div>

                    <div style="display:flex; gap:4px;">
                        ${historyList.length ? `
                            <button class="contact-action-btn" type="button" data-action="view-history" data-id="${contact.id}" title="Xem lịch sử điều chuyển công tác (${historyList.length})">
                                📜 Lịch sử
                            </button>
                        ` : ""}
                        <button class="contact-action-btn" type="button" data-action="edit" data-id="${contact.id}" title="Chỉnh sửa thông tin liên hệ">
                            ✏️ Sửa
                        </button>
                        <button class="contact-action-btn" type="button" data-action="delete" data-id="${contact.id}" title="Xóa người liên hệ" style="color:var(--crm-danger);">
                            🗑️
                        </button>
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * Render Table Row HTML
     */
    function renderContactTableRowHtml(contact) {
        const gradient = getAvatarGradient(contact.fullName);
        const initials = getInitials(contact.fullName);
        const historyList = store.getContactHistory(contact.id);

        return `
            <tr data-contact-id="${contact.id}">
                <td>
                    <div style="display:flex; align-items:center; gap:10px;">
                        <div class="contact-avatar" style="width:34px; height:34px; font-size:13px; background:${gradient};">
                            ${initials}
                        </div>
                        <div>
                            <div style="font-weight:700; color:var(--crm-text);">${escapeHtml(contact.fullName)}</div>
                            <div style="font-size:12px; color:var(--crm-muted);">${escapeHtml(contact.email)} · ${escapeHtml(contact.phone)}</div>
                        </div>
                    </div>
                </td>
                <td>
                    <a href="customer-360.html?id=${contact.customerId}" style="color:var(--crm-primary); font-weight:600; text-decoration:none;">
                        ${escapeHtml(contact.companyName)}
                    </a>
                </td>
                <td>
                    <div style="font-weight:600;">${escapeHtml(contact.jobTitle)}</div>
                    <div style="font-size:12px; color:var(--crm-muted);">${escapeHtml(contact.department || "—")}</div>
                </td>
                <td>
                    ${renderDecisionRoleBadge(contact.decisionRole)}
                </td>
                <td>
                    ${contact.isPrimary 
                        ? `<span class="primary-contact-badge">⭐ Đầu mối chính</span>` 
                        : `<button class="contact-action-btn btn-set-primary" style="padding:2px 8px; font-size:11px;" data-action="set-primary" data-id="${contact.id}">+ Đặt làm chính</button>`
                    }
                </td>
                <td>
                    <span class="status-pill ${contact.status === 'ACTIVE' ? 'status-active' : 'status-inactive'}">
                        ● ${contact.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng'}
                    </span>
                </td>
                <td>
                    <div style="display:flex; gap:6px; align-items:center;">
                        <button class="contact-action-btn btn-transfer" style="padding:3px 8px; font-size:11.5px;" data-action="transfer" data-id="${contact.id}" title="Điều chuyển sang công ty mới">
                            🔄 Chuyển
                        </button>
                        ${historyList.length ? `
                            <button class="contact-action-btn" style="padding:3px 8px; font-size:11.5px;" data-action="view-history" data-id="${contact.id}" title="Xem lịch sử chuyển công tác">
                                📜 (${historyList.length})
                            </button>
                        ` : ""}
                        <button class="contact-action-btn" style="padding:3px 8px; font-size:11.5px;" data-action="edit" data-id="${contact.id}" title="Sửa">
                            ✏️
                        </button>
                        <button class="contact-action-btn" style="padding:3px 8px; font-size:11.5px; color:var(--crm-danger);" data-action="delete" data-id="${contact.id}" title="Xóa">
                            🗑️
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }

    /**
     * Render the main contacts list on contacts.html
     */
    function renderMainContactsList() {
        const container = document.getElementById("contactsListContainer");
        if (!container) return;

        const filtered = getFilteredContacts();
        renderKpiCards();

        const countDisplay = document.getElementById("txtFilteredCount");
        if (countDisplay) {
            countDisplay.textContent = `Hiển thị ${filtered.length} người liên hệ`;
        }

        if (filtered.length === 0) {
            container.innerHTML = `
                <div style="background:var(--crm-surface); border:1px dashed var(--crm-border-strong); border-radius:12px; padding:48px 20px; text-align:center;">
                    <div style="font-size:36px; margin-bottom:12px;">👥</div>
                    <h3 style="margin:0 0 8px; font-size:18px; color:var(--crm-text);">Không tìm thấy người liên hệ nào</h3>
                    <p style="margin:0 auto 20px; color:var(--crm-muted); font-size:14px; max-width:440px;">
                        Không có kết quả khớp với tiêu chí tìm kiếm hoặc bộ lọc vai trò hiện tại. Vui lòng thử lại với từ khóa khác hoặc đặt lại bộ lọc.
                    </p>
                    <button id="btnResetSearch" class="crm-btn crm-btn-secondary" type="button">
                        Đặt lại toàn bộ bộ lọc
                    </button>
                </div>
            `;
            const btnReset = container.querySelector("#btnResetSearch");
            if (btnReset) {
                btnReset.addEventListener("click", () => {
                    pageState.searchTerm = "";
                    pageState.roleFilter = "ALL";
                    pageState.customerFilter = "ALL";
                    pageState.statusFilter = "ALL";
                    const inputSearch = document.getElementById("inputContactSearch");
                    if (inputSearch) inputSearch.value = "";
                    const selectCust = document.getElementById("selectCustomerFilter");
                    if (selectCust) selectCust.value = "ALL";
                    const selectStat = document.getElementById("selectStatusFilter");
                    if (selectStat) selectStat.value = "ALL";
                    document.querySelectorAll(".role-filter-pill").forEach(p => {
                        p.classList.toggle("active", p.dataset.role === "ALL");
                    });
                    renderMainContactsList();
                });
            }
            return;
        }

        if (pageState.viewMode === "CARD") {
            container.innerHTML = `
                <div class="contacts-cards-grid">
                    ${filtered.map(renderContactCardHtml).join("")}
                </div>
            `;
        } else {
            container.innerHTML = `
                <div class="contacts-table-wrapper">
                    <table class="contacts-table">
                        <thead>
                            <tr>
                                <th>Người liên hệ</th>
                                <th>Doanh nghiệp</th>
                                <th>Chức danh / Phòng ban</th>
                                <th>Vai trò quyết định</th>
                                <th>Đầu mối chính</th>
                                <th>Trạng thái</th>
                                <th>Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${filtered.map(renderContactTableRowHtml).join("")}
                        </tbody>
                    </table>
                </div>
            `;
        }

        // Attach event handlers for actions inside rendered cards/table
        attachActionListeners(container);
    }

    /**
     * Attach Action Listeners (Set Primary, Transfer, Edit, Delete, View History)
     */
    function attachActionListeners(parent) {
        // Set Primary
        parent.querySelectorAll('[data-action="set-primary"]').forEach(btn => {
            btn.addEventListener("click", e => {
                e.stopPropagation();
                const id = btn.dataset.id;
                try {
                    const updated = store.setPrimaryContact(id);
                    showToast(`Đã chỉ định "${updated.fullName}" làm Đầu mối liên hệ chính của ${updated.companyName}.`);
                    renderMainContactsList();
                    // Also trigger customer-360 update if present
                    dispatchContactsUpdatedEvent();
                } catch (err) {
                    showToast(err.message, "error");
                }
            });
        });

        // Transfer Contact Modal
        parent.querySelectorAll('[data-action="transfer"]').forEach(btn => {
            btn.addEventListener("click", e => {
                e.stopPropagation();
                openTransferModal(btn.dataset.id);
            });
        });

        // View History Modal
        parent.querySelectorAll('[data-action="view-history"]').forEach(btn => {
            btn.addEventListener("click", e => {
                e.stopPropagation();
                openHistoryModal(btn.dataset.id);
            });
        });

        // Edit Contact Modal
        parent.querySelectorAll('[data-action="edit"]').forEach(btn => {
            btn.addEventListener("click", e => {
                e.stopPropagation();
                openEditModal(btn.dataset.id);
            });
        });

        // Delete Contact
        parent.querySelectorAll('[data-action="delete"]').forEach(btn => {
            btn.addEventListener("click", e => {
                e.stopPropagation();
                const id = btn.dataset.id;
                const contact = store.getContactById(id);
                if (!contact) return;

                if (confirm(`Bạn có chắc chắn muốn xóa người liên hệ "${contact.fullName}" (${contact.jobTitle} - ${contact.companyName})?`)) {
                    store.deleteContact(id);
                    showToast(`Đã xóa người liên hệ "${contact.fullName}".`);
                    renderMainContactsList();
                    dispatchContactsUpdatedEvent();
                }
            });
        });
    }

    function dispatchContactsUpdatedEvent() {
        document.dispatchEvent(new CustomEvent("crm:contacts-updated"));
    }

    // ===================================================================
    // MODALS MANAGEMENT (ADD/EDIT, TRANSFER, HISTORY)
    // ===================================================================

    let activeContactId = null;

    /**
     * Open Modal: Add New Contact
     */
    function openAddModal(preselectedCustomerId = null) {
        activeContactId = null;
        const modal = document.getElementById("contactFormModal");
        if (!modal) return;

        document.getElementById("modalContactTitle").textContent = "Thêm mới người liên hệ B2B";
        document.getElementById("btnSubmitContact").textContent = "Lưu người liên hệ";

        // Reset form
        const form = document.getElementById("contactForm");
        if (form) form.reset();

        // Populate Customers dropdown
        const selectCust = document.getElementById("contactCustomerId");
        if (selectCust) {
            const customers = store.getAvailableCustomers();
            selectCust.innerHTML = customers.map(c => `
                <option value="${c.id}" ${preselectedCustomerId && Number(c.id) === Number(preselectedCustomerId) ? "selected" : ""}>
                    ${escapeHtml(c.companyName || c.name)} (MST: ${escapeHtml(c.taxCode || "N/A")})
                </option>
            `).join("");
        }

        // Set default role
        const defaultRoleRadio = form?.querySelector('input[name="decisionRole"][value="END_USER"]');
        if (defaultRoleRadio) defaultRoleRadio.checked = true;
        updateRoleRadioCards();

        document.getElementById("contactIsPrimary").checked = false;
        document.getElementById("contactStatus").value = "ACTIVE";

        modal.classList.add("open");
    }

    /**
     * Open Modal: Edit Contact
     */
    function openEditModal(contactId) {
        const contact = store.getContactById(contactId);
        if (!contact) return;
        activeContactId = contact.id;

        const modal = document.getElementById("contactFormModal");
        if (!modal) return;

        document.getElementById("modalContactTitle").textContent = "Chỉnh sửa hồ sơ người liên hệ";
        document.getElementById("btnSubmitContact").textContent = "Cập nhật thay đổi";

        // Populate Customers dropdown
        const selectCust = document.getElementById("contactCustomerId");
        if (selectCust) {
            const customers = store.getAvailableCustomers();
            selectCust.innerHTML = customers.map(c => `
                <option value="${c.id}" ${Number(c.id) === Number(contact.customerId) ? "selected" : ""}>
                    ${escapeHtml(c.companyName || c.name)} (MST: ${escapeHtml(c.taxCode || "N/A")})
                </option>
            `).join("");
        }

        document.getElementById("contactFullName").value = contact.fullName;
        document.getElementById("contactJobTitle").value = contact.jobTitle;
        document.getElementById("contactDepartment").value = contact.department || "";
        document.getElementById("contactEmail").value = contact.email;
        document.getElementById("contactPhone").value = contact.phone;
        document.getElementById("contactNotes").value = contact.notes || "";
        document.getElementById("contactStatus").value = contact.status || "ACTIVE";
        document.getElementById("contactIsPrimary").checked = !!contact.isPrimary;

        const roleRadio = document.querySelector(`input[name="decisionRole"][value="${contact.decisionRole}"]`);
        if (roleRadio) roleRadio.checked = true;
        updateRoleRadioCards();

        modal.classList.add("open");
    }

    function closeFormModal() {
        const modal = document.getElementById("contactFormModal");
        if (modal) modal.classList.remove("open");
        activeContactId = null;
    }

    function updateRoleRadioCards() {
        document.querySelectorAll(".role-radio-card").forEach(card => {
            const radio = card.querySelector('input[type="radio"]');
            card.classList.toggle("selected", !!(radio && radio.checked));
        });
    }

    /**
     * Open Modal: Transfer Contact (Chuyển công tác sang công ty mới)
     */
    function openTransferModal(contactId) {
        const contact = store.getContactById(contactId);
        if (!contact) return;
        activeContactId = contact.id;

        const modal = document.getElementById("transferContactModal");
        if (!modal) return;

        document.getElementById("transferContactName").textContent = contact.fullName;
        document.getElementById("transferCurrentCompany").textContent = contact.companyName;
        document.getElementById("transferCurrentJobTitle").textContent = contact.jobTitle;

        // Populate Destination Customers (excluding current)
        const selectNewCust = document.getElementById("transferNewCustomerId");
        if (selectNewCust) {
            const customers = store.getAvailableCustomers().filter(c => Number(c.id) !== Number(contact.customerId));
            selectNewCust.innerHTML = customers.map(c => `
                <option value="${c.id}">${escapeHtml(c.companyName || c.name)}</option>
            `).join("");
        }

        document.getElementById("transferNewJobTitle").value = contact.jobTitle;
        document.getElementById("transferNewDepartment").value = contact.department || "";
        document.getElementById("transferDate").value = new Date().toISOString().split("T")[0];
        document.getElementById("transferReason").value = `Chuyển công tác từ ${contact.companyName} sang đơn vị mới từ tháng ${new Date().getMonth() + 1}/${new Date().getFullYear()}.`;
        document.getElementById("transferSetPrimary").checked = false;

        modal.classList.add("open");
    }

    function closeTransferModal() {
        const modal = document.getElementById("transferContactModal");
        if (modal) modal.classList.remove("open");
        activeContactId = null;
    }

    /**
     * Open Modal: History Timeline
     */
    function openHistoryModal(contactId = null) {
        const modal = document.getElementById("contactHistoryModal");
        if (!modal) return;

        const historyContainer = document.getElementById("historyTimelineContainer");
        const titleEl = document.getElementById("historyModalTitle");

        let historyList = [];
        if (contactId) {
            const contact = store.getContactById(contactId);
            if (titleEl) titleEl.textContent = `Lịch sử chuyển công tác: ${contact ? contact.fullName : ""}`;
            historyList = store.getContactHistory(contactId);
        } else {
            if (titleEl) titleEl.textContent = "Toàn bộ lịch sử điều chuyển nhân sự B2B (Audit Trail)";
            historyList = store.getAllHistory();
        }

        if (historyContainer) {
            if (historyList.length === 0) {
                historyContainer.innerHTML = `
                    <div style="text-align:center; padding:32px 16px; color:var(--crm-muted);">
                        <div style="font-size:28px; margin-bottom:8px;">📜</div>
                        <div>Chưa có dữ liệu lịch sử chuyển công tác nào được ghi nhận.</div>
                    </div>
                `;
            } else {
                historyContainer.innerHTML = `
                    <div class="contact-history-timeline">
                        ${historyList.map(h => {
                            const dateObj = new Date(h.transferDate);
                            const formattedDate = !isNaN(dateObj) ? dateObj.toLocaleDateString("vi-VN") : h.transferDate;
                            return `
                                <div class="history-timeline-node">
                                    <div class="history-timeline-dot"></div>
                                    <div class="history-timeline-date">Ngày điều chuyển: ${escapeHtml(formattedDate)}</div>
                                    <div class="history-timeline-content">
                                        <div class="history-timeline-title">
                                            👤 ${escapeHtml(h.contactName)}: 
                                            <span style="color:var(--crm-muted); text-decoration:line-through;">${escapeHtml(h.fromCompanyName)}</span>
                                            ➔ <span style="color:var(--crm-primary); font-weight:800;">${escapeHtml(h.toCompanyName)}</span>
                                        </div>
                                        <div class="history-timeline-meta">
                                            Vị trí cũ: <strong>${escapeHtml(h.oldJobTitle)}</strong> ➔ Vị trí mới: <strong>${escapeHtml(h.newJobTitle)}</strong>
                                        </div>
                                        <div class="history-timeline-reason">
                                            <strong>Ghi chú lý do:</strong> ${escapeHtml(h.reason || "Không có")}
                                            <div style="font-size:11px; color:var(--crm-muted); margin-top:4px;">
                                                Ghi nhận bởi: ${escapeHtml(h.loggedBy || "Hệ thống")} (${new Date(h.loggedAt).toLocaleString("vi-VN")})
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            `;
                        }).join("")}
                    </div>
                `;
            }
        }

        modal.classList.add("open");
    }

    function closeHistoryModal() {
        const modal = document.getElementById("contactHistoryModal");
        if (modal) modal.classList.remove("open");
    }

    // ===================================================================
    // FORM SUBMISSIONS & VALIDATIONS
    // ===================================================================

    function setupFormListeners() {
        // Role Radio Card clicks
        document.querySelectorAll(".role-radio-card").forEach(card => {
            card.addEventListener("click", () => {
                const radio = card.querySelector('input[type="radio"]');
                if (radio) {
                    radio.checked = true;
                    updateRoleRadioCards();
                }
            });
        });

        // Contact Form Submit (Add/Edit)
        const contactForm = document.getElementById("contactForm");
        if (contactForm) {
            contactForm.addEventListener("submit", e => {
                e.preventDefault();

                const customerId = document.getElementById("contactCustomerId").value;
                const fullName = document.getElementById("contactFullName").value.trim();
                const jobTitle = document.getElementById("contactJobTitle").value.trim();
                const department = document.getElementById("contactDepartment").value.trim();
                const email = document.getElementById("contactEmail").value.trim();
                const phone = document.getElementById("contactPhone").value.trim();
                const notes = document.getElementById("contactNotes").value.trim();
                const status = document.getElementById("contactStatus").value;
                const isPrimary = document.getElementById("contactIsPrimary").checked;

                const roleRadio = contactForm.querySelector('input[name="decisionRole"]:checked');
                const decisionRole = roleRadio ? roleRadio.value : "END_USER";

                // Validations
                if (!fullName) {
                    alert("Vui lòng nhập Họ và tên người liên hệ.");
                    document.getElementById("contactFullName").focus();
                    return;
                }
                if (!jobTitle) {
                    alert("Vui lòng nhập Chức danh / Vị trí công tác.");
                    document.getElementById("contactJobTitle").focus();
                    return;
                }
                if (!email || !isValidEmail(email)) {
                    alert("Vui lòng nhập định dạng Email hợp lệ (ví dụ: contact@company.com).");
                    document.getElementById("contactEmail").focus();
                    return;
                }
                if (!phone || !isValidVietnamesePhone(phone)) {
                    alert("Vui lòng nhập Số điện thoại Việt Nam hợp lệ (10 chữ số, bắt đầu bằng 03, 05, 07, 08, 09 hoặc máy bàn 02).");
                    document.getElementById("contactPhone").focus();
                    return;
                }

                try {
                    if (activeContactId) {
                        // Update
                        const updated = store.updateContact(activeContactId, {
                            customerId,
                            fullName,
                            jobTitle,
                            department,
                            email,
                            phone,
                            decisionRole,
                            isPrimary,
                            status,
                            notes
                        });
                        showToast(`Đã cập nhật thông tin "${updated.fullName}".`);
                    } else {
                        // Add
                        const created = store.addContact({
                            customerId,
                            fullName,
                            jobTitle,
                            department,
                            email,
                            phone,
                            decisionRole,
                            isPrimary,
                            status,
                            notes
                        });
                        showToast(`Đã thêm mới người liên hệ "${created.fullName}" thành công!`);
                    }

                    closeFormModal();
                    renderMainContactsList();
                    dispatchContactsUpdatedEvent();
                } catch (err) {
                    alert("Lỗi: " + err.message);
                }
            });
        }

        // Transfer Form Submit
        const transferForm = document.getElementById("transferForm");
        if (transferForm) {
            transferForm.addEventListener("submit", e => {
                e.preventDefault();
                if (!activeContactId) return;

                const toCustomerId = document.getElementById("transferNewCustomerId").value;
                const newJobTitle = document.getElementById("transferNewJobTitle").value.trim();
                const newDepartment = document.getElementById("transferNewDepartment").value.trim();
                const transferDate = document.getElementById("transferDate").value;
                const reason = document.getElementById("transferReason").value.trim();
                const setAsPrimaryInNewComp = document.getElementById("transferSetPrimary").checked;

                if (!toCustomerId) {
                    alert("Vui lòng chọn doanh nghiệp đích.");
                    return;
                }
                if (!newJobTitle) {
                    alert("Vui lòng nhập chức danh mới tại đơn vị chuyển đến.");
                    return;
                }
                if (!reason) {
                    alert("Vui lòng nhập ghi chú / lý do điều chuyển công tác.");
                    return;
                }

                try {
                    const result = store.transferContact(activeContactId, {
                        toCustomerId,
                        newJobTitle,
                        newDepartment,
                        transferDate,
                        reason,
                        setAsPrimaryInNewComp
                    });

                    showToast(`Đã điều chuyển "${result.contact.fullName}" sang "${result.contact.companyName}" thành công!`);
                    closeTransferModal();
                    renderMainContactsList();
                    dispatchContactsUpdatedEvent();
                } catch (err) {
                    alert("Lỗi điều chuyển: " + err.message);
                }
            });
        }

        // Modal Close Buttons
        document.querySelectorAll("[data-close-modal]").forEach(btn => {
            btn.addEventListener("click", () => {
                closeFormModal();
                closeTransferModal();
                closeHistoryModal();
            });
        });

        // Close on escape
        document.addEventListener("keydown", e => {
            if (e.key === "Escape") {
                closeFormModal();
                closeTransferModal();
                closeHistoryModal();
            }
        });
    }

    // ===================================================================
    // INTEGRATION WITH CUSTOMER 360 RIGHT PANEL (#contactsPanel)
    // ===================================================================
    function renderCustomer360Contacts(container, customerId) {
        if (!container || !customerId) return;

        const contacts = store.getContactsByCustomer(customerId);

        if (contacts.length === 0) {
            container.innerHTML = `
                <div class="empty-small" style="padding:16px; text-align:center; color:var(--crm-muted);">
                    Chưa có người liên hệ nào cho doanh nghiệp này.
                </div>
                <button id="btn360AddContact" class="crm-btn crm-btn-secondary full-button" type="button" style="width:100%; margin-top:8px;">
                    + Thêm người liên hệ
                </button>
            `;
            const btn = container.querySelector("#btn360AddContact");
            if (btn) btn.addEventListener("click", () => openAddModal(customerId));
            return;
        }

        container.innerHTML = `
            <div style="padding:4px 0 10px 0; display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid var(--crm-border); margin-bottom:10px;">
                <span style="font-size:12.5px; font-weight:700; color:var(--crm-text-2);">
                    Người liên hệ (${contacts.length})
                </span>
                <a href="contacts.html?customerId=${customerId}" class="crm-btn crm-btn-secondary" style="font-size:11.5px; padding:3px 8px; text-decoration:none;">
                    Quản lý toàn diện
                </a>
            </div>

            <div class="customer-360-contacts-list" style="max-height:420px; overflow-y:auto; padding-right:2px;">
                ${contacts.map(c => {
                    const gradient = getAvatarGradient(c.fullName);
                    const initials = getInitials(c.fullName);
                    return `
                        <div class="customer-360-contact-item" data-contact-id="${c.id}">
                            <div class="c360-contact-head">
                                <div class="c360-contact-main">
                                    <div class="c360-contact-avatar" style="background:${gradient};">
                                        ${initials}
                                    </div>
                                    <div style="min-width:0;">
                                        <div class="c360-contact-name" title="${escapeHtml(c.fullName)}">
                                            ${escapeHtml(c.fullName)}
                                        </div>
                                        <div class="c360-contact-title">
                                            ${escapeHtml(c.jobTitle)}
                                        </div>
                                    </div>
                                </div>
                                <div>
                                    ${c.isPrimary ? '<span title="Đầu mối chính" style="font-size:16px;">⭐</span>' : ''}
                                </div>
                            </div>

                            <div style="display:flex; flex-wrap:wrap; gap:4px;">
                                ${renderDecisionRoleBadge(c.decisionRole)}
                            </div>

                            <div style="font-size:12px; color:var(--crm-muted); display:flex; flex-direction:column; gap:2px;">
                                <div>✉ <a href="mailto:${escapeHtml(c.email)}" style="color:inherit; text-decoration:none;">${escapeHtml(c.email)}</a></div>
                                <div>☎ <a href="tel:${escapeHtml(c.phone)}" style="color:inherit; text-decoration:none;">${escapeHtml(c.phone)}</a></div>
                            </div>

                            <div class="c360-contact-actions">
                                ${!c.isPrimary ? `
                                    <button class="contact-action-btn btn-set-primary" style="padding:2px 6px; font-size:11px;" data-action="set-primary" data-id="${c.id}" title="Đặt làm đầu mối chính">
                                        ⭐ Đầu mối chính
                                    </button>
                                ` : ""}
                                <button class="contact-action-btn btn-transfer" style="padding:2px 6px; font-size:11px;" data-action="transfer" data-id="${c.id}" title="Chuyển công ty">
                                    🔄 Chuyển
                                </button>
                                <button class="contact-action-btn" style="padding:2px 6px; font-size:11px;" data-action="edit" data-id="${c.id}" title="Sửa">
                                    ✏️
                                </button>
                            </div>
                        </div>
                    `;
                }).join("")}
            </div>

            <button id="btn360AddContact" class="crm-btn crm-btn-secondary full-button" type="button" style="width:100%; margin-top:8px;">
                + Thêm người liên hệ
            </button>
        `;

        // Attach action handlers
        attachActionListeners(container);

        const btnAdd = container.querySelector("#btn360AddContact");
        if (btnAdd) btnAdd.addEventListener("click", () => openAddModal(customerId));
    }

    // ===================================================================
    // PAGE INITIALIZATION (IF ON contacts.html)
    // ===================================================================
    function initContactsPage() {
        const container = document.getElementById("contactsListContainer");
        if (!container) return; // Not on contacts.html

        // Check URL params for pre-filter
        const urlParams = new URLSearchParams(window.location.search);
        const qCust = urlParams.get("customerId");
        if (qCust) {
            pageState.customerFilter = qCust;
        }

        // Setup Customer Filter dropdown
        const selectCustomer = document.getElementById("selectCustomerFilter");
        if (selectCustomer) {
            const customers = store.getAvailableCustomers();
            selectCustomer.innerHTML = `
                <option value="ALL">Tất cả doanh nghiệp (${customers.length})</option>
                ${customers.map(c => `
                    <option value="${c.id}" ${pageState.customerFilter === String(c.id) ? "selected" : ""}>
                        ${escapeHtml(c.companyName || c.name)}
                    </option>
                `).join("")}
            `;
            selectCustomer.addEventListener("change", e => {
                pageState.customerFilter = e.target.value;
                renderMainContactsList();
            });
        }

        // Setup Status Filter dropdown
        const selectStatus = document.getElementById("selectStatusFilter");
        if (selectStatus) {
            selectStatus.addEventListener("change", e => {
                pageState.statusFilter = e.target.value;
                renderMainContactsList();
            });
        }

        // Setup Search input with debounce
        const inputSearch = document.getElementById("inputContactSearch");
        if (inputSearch) {
            let timer = null;
            inputSearch.addEventListener("input", e => {
                clearTimeout(timer);
                timer = setTimeout(() => {
                    pageState.searchTerm = e.target.value.trim();
                    renderMainContactsList();
                }, 200);
            });
        }

        // Setup Role Filter Pills
        document.querySelectorAll(".role-filter-pill").forEach(pill => {
            pill.addEventListener("click", () => {
                document.querySelectorAll(".role-filter-pill").forEach(p => p.classList.remove("active"));
                pill.classList.add("active");
                pageState.roleFilter = pill.dataset.role;
                renderMainContactsList();
            });
        });

        // Setup KPI Card clicks to filter directly
        document.querySelectorAll(".contacts-kpi-card").forEach(card => {
            card.addEventListener("click", () => {
                const targetFilter = card.dataset.filter;
                if (!targetFilter) return;

                pageState.roleFilter = targetFilter;
                document.querySelectorAll(".role-filter-pill").forEach(p => {
                    p.classList.toggle("active", p.dataset.role === targetFilter);
                });
                renderMainContactsList();
            });
        });

        // Setup View Switcher (Card vs Table)
        const btnCardView = document.getElementById("btnCardView");
        const btnTableView = document.getElementById("btnTableView");
        if (btnCardView && btnTableView) {
            btnCardView.addEventListener("click", () => {
                btnCardView.classList.add("active");
                btnTableView.classList.remove("active");
                pageState.viewMode = "CARD";
                renderMainContactsList();
            });
            btnTableView.addEventListener("click", () => {
                btnTableView.classList.add("active");
                btnCardView.classList.remove("active");
                pageState.viewMode = "TABLE";
                renderMainContactsList();
            });
        }

        // Setup Add Contact Button on Header
        const btnAddMain = document.getElementById("btnAddNewContact");
        if (btnAddMain) {
            btnAddMain.addEventListener("click", () => openAddModal());
        }

        // Setup View All History Button on Header
        const btnAllHistory = document.getElementById("btnViewAllHistory");
        if (btnAllHistory) {
            btnAllHistory.addEventListener("click", () => openHistoryModal());
        }

        // Setup Export CSV / Excel Button
        const btnExport = document.getElementById("btnExportContacts");
        if (btnExport) {
            btnExport.addEventListener("click", exportContactsToCsv);
        }

        // Render initial data
        renderMainContactsList();
    }

    /**
     * Export Contacts to CSV with Vietnamese UTF-8 BOM
     */
    function exportContactsToCsv() {
        const contacts = store.getAllContacts();
        if (!contacts.length) {
            alert("Không có dữ liệu người liên hệ để xuất.");
            return;
        }

        const headers = ["ID", "Họ và tên", "Doanh nghiệp", "Chức danh", "Phòng ban", "Email", "Số điện thoại", "Vai trò quyết định", "Đầu mối chính", "Trạng thái", "Ghi chú chiến lược"];
        const rows = contacts.map(c => [
            c.id,
            `"${(c.fullName || "").replace(/"/g, '""')}"`,
            `"${(c.companyName || "").replace(/"/g, '""')}"`,
            `"${(c.jobTitle || "").replace(/"/g, '""')}"`,
            `"${(c.department || "").replace(/"/g, '""')}"`,
            c.email || "",
            c.phone || "",
            DECISION_ROLES[c.decisionRole]?.label || c.decisionRole,
            c.isPrimary ? "Có (Đầu mối chính)" : "Không",
            c.status === "ACTIVE" ? "Hoạt động" : "Tạm dừng",
            `"${(c.notes || "").replace(/"/g, '""')}"`
        ]);

        const csvContent = "\uFEFF" + [headers.join(","), ...rows.map(r => r.join(","))].join("\r\n");
        const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.setAttribute("href", url);
        link.setAttribute("download", `Danh_sach_nguoi_lien_he_CRM62_${new Date().toISOString().split("T")[0]}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        showToast("Đã xuất danh sách người liên hệ thành công!");
    }

    // ===================================================================
    // PUBLIC API EXPOSURE ON WINDOW
    // ===================================================================
    window.ContactsManager = {
        store,
        DECISION_ROLES,
        getAllContacts: () => store.getAllContacts(),
        getContactsByCustomer: id => store.getContactsByCustomer(id),
        getContactById: id => store.getContactById(id),
        addContact: payload => store.addContact(payload),
        updateContact: (id, payload) => store.updateContact(id, payload),
        deleteContact: id => store.deleteContact(id),
        setPrimaryContact: id => store.setPrimaryContact(id),
        transferContact: (id, payload) => store.transferContact(id, payload),
        getContactHistory: id => store.getContactHistory(id),
        getAllHistory: () => store.getAllHistory(),
        openAddContactModal: preselectedCustomerId => openAddModal(preselectedCustomerId),
        openEditContactModal: id => openEditModal(id),
        openTransferModal: id => openTransferModal(id),
        openHistoryModal: id => openHistoryModal(id),
        renderCustomer360Contacts: (container, customerId) => renderCustomer360Contacts(container, customerId),
        renderDecisionRoleBadge: role => renderDecisionRoleBadge(role),
        renderPrimaryBadge: isPrimary => renderPrimaryBadge(isPrimary),
        refresh: () => {
            renderMainContactsList();
        }
    };

    // Auto-init on DOMContentLoaded
    document.addEventListener("DOMContentLoaded", () => {
        setupFormListeners();
        initContactsPage();
    });

})();
