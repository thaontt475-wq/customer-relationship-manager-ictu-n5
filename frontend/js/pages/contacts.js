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

    // Decision Roles Definition (CRM-62)
    const DECISION_ROLES = {
        DECISION_MAKER: {
            code: "DECISION_MAKER",
            label: "Người quyết định",
            englishLabel: "Decision Maker",
            icon: '<i class="fa-solid fa-crown"></i>',
            cssClass: "role-decision-maker",
            accentClass: "accent-decision-maker",
            kpiClass: "kpi-purple",
            desc: "Nắm ngân sách & ký duyệt hợp đồng cuối cùng. Trọng tâm chốt deal."
        },
        INFLUENCER: {
            code: "INFLUENCER",
            label: "Người ảnh hưởng",
            englishLabel: "Influencer",
            icon: '<i class="fa-solid fa-bolt"></i>',
            cssClass: "role-influencer",
            accentClass: "accent-influencer",
            kpiClass: "kpi-blue",
            desc: "Chuyên môn cao, định hướng tiêu chuẩn kỹ thuật & tác động đến lựa chọn."
        },
        END_USER: {
            code: "END_USER",
            label: "Người dùng cuối",
            englishLabel: "End User",
            icon: '<i class="fa-solid fa-user"></i>',
            cssClass: "role-end-user",
            accentClass: "accent-end-user",
            kpiClass: "kpi-green",
            desc: "Trực tiếp vận hành hằng ngày, phản hồi trải nghiệm & tính năng thực tế."
        },
        BLOCKER: {
            code: "BLOCKER",
            label: "Người cản trở",
            englishLabel: "Blocker / Gatekeeper",
            icon: '<i class="fa-solid fa-triangle-exclamation"></i>',
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

    // Shared rendering helpers
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
                ${type === "success" 
                    ? '<i class="fa-solid fa-circle-check"></i>' 
                    : (type === "error" 
                        ? '<i class="fa-solid fa-circle-xmark"></i>' 
                        : '<i class="fa-solid fa-triangle-exclamation"></i>')}
            </span>
            <span class="crm-toast-copy">${escapeHtml(message)}</span>
            <button class="crm-toast-close" type="button" aria-label="Đóng"><i class="fa-solid fa-xmark"></i></button>
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
    // The in-memory view is refreshed after every committed API write.
    class ContactsStore {
        constructor() { this.contacts=[]; this.customers=[]; this.history=[]; }
        map(row) {
            const customer=this.getCustomerById(row.customerId);
            return {...row,jobTitle:row.title || "",decisionRole:row.buyingRole,isPrimary:!!row.isPrimary,status:"ACTIVE",companyName:customer?.name || `#${row.customerId}`};
        }
        async load() {
            this.customers=await CrmApi.customers();
            this.contacts=(await CrmApi.request("/api/contacts")).map(row=>this.map(row));
        }
        ingest(customer,contacts) {
            this.customers=[...this.customers.filter(c=>Number(c.id)!==Number(customer.id)),customer];
            this.contacts=[...this.contacts.filter(c=>Number(c.customerId)!==Number(customer.id)),...contacts.map(c=>this.map(c))];
        }
        getAllContacts() { return this.contacts; }
        getAvailableCustomers() { return this.customers; }
        getCustomerById(id) { return this.customers.find(c=>Number(c.id)===Number(id)); }
        getContactsByCustomer(id) { return this.contacts.filter(c=>Number(c.customerId)===Number(id)); }
        getContactById(id) { return this.contacts.find(c=>Number(c.id)===Number(id)); }
        getAllHistory() { return this.history; }
        getContactHistory(id) { return this.history.filter(h=>Number(h.contactId)===Number(id)); }
        async loadHistory(id) {
            const ids=id ? [id] : this.contacts.map(c=>c.id);
            const rows=(await Promise.all(ids.map(id=>CrmApi.request(`/api/contacts/${id}/history`)))).flat();
            this.history=[...this.history.filter(h=>!ids.some(id=>Number(id)===Number(h.contactId))),...rows.map(row=>({
                ...row,contactName:this.getContactById(row.contactId)?.fullName || `#${row.contactId}`,
                fromCompanyName:this.getCustomerById(row.fromCustomerId)?.name || `#${row.fromCustomerId}`,
                toCompanyName:this.getCustomerById(row.toCustomerId)?.name || `#${row.toCustomerId}`,
                transferDate:row.changedAt,reason:row.note,loggedAt:row.changedAt,loggedBy:`#${row.changedBy}`
            }))];
        }
        payload(p) { return {customerId:Number(p.customerId),fullName:p.fullName,title:p.jobTitle,email:p.email,phone:p.phone,buyingRole:p.decisionRole,isPrimary:!!p.isPrimary}; }
        async addContact(p) {
            const row=await CrmApi.request("/api/contacts",{method:"POST",body:JSON.stringify(this.payload(p))});
            await this.load(); return this.map(row);
        }
        async updateContact(id,p) {
            const row=await CrmApi.request(`/api/contacts/${id}`,{method:"PUT",body:JSON.stringify(this.payload(p))});
            await this.load(); return this.map(row);
        }
        async deleteContact(id) { await CrmApi.request(`/api/contacts/${id}`,{method:"DELETE"}); await this.load(); }
        async setPrimaryContact(id) {
            const row=await CrmApi.request(`/api/contacts/${id}/primary`,{method:"PUT"});
            await this.load(); return this.map(row);
        }
        async transferContact(id,p) {
            const row=await CrmApi.request(`/api/contacts/${id}/transfer`,{method:"POST",body:JSON.stringify({customerId:Number(p.toCustomerId),isPrimary:!!p.setAsPrimaryInNewComp,note:p.reason})});
            await this.load(); await this.loadHistory(id); return {contact:this.map(row)};
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
                <span class="star-icon"><i class="fa-solid fa-id-badge" style="color:var(--crm-primary); font-size:12px;"></i></span>
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
                    <i class="fa-solid fa-building" style="font-size:14px;"></i>
                    <a href="customer-360.html?id=${contact.customerId}" class="contact-company-link" title="Xem hồ sơ 360° ${escapeHtml(contact.companyName)}">
                        ${escapeHtml(contact.companyName)}
                    </a>
                </div>

                <div class="contact-comms-list">
                    <div class="contact-comm-item" title="Email công việc">
                        <i class="fa-solid fa-envelope" style="font-size:14px;"></i>
                        <a href="mailto:${escapeHtml(contact.email)}">${escapeHtml(contact.email)}</a>
                    </div>
                    <div class="contact-comm-item" title="Số điện thoại di động">
                        <i class="fa-solid fa-phone" style="font-size:14px;"></i>
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
                        <span><i class="fa-solid fa-right-left"></i></span>
                        <span>Từng là <em>${escapeHtml(latestHistory.oldJobTitle)}</em> tại <strong>${escapeHtml(latestHistory.fromCompanyName)}</strong></span>
                    </div>
                ` : ""}

                <div class="contact-card-actions">
                    <div style="display:flex; gap:6px;">
                        ${!contact.isPrimary ? `
                            <button class="contact-action-btn btn-set-primary" type="button" data-action="set-primary" data-id="${contact.id}" title="Chỉ định làm Đầu mối liên hệ chính của công ty">
                                <i class="fa-solid fa-id-badge" style="color:var(--crm-primary); margin-right:4px;"></i> Đặt đầu mối chính
                            </button>
                        ` : ""}
                        <button class="contact-action-btn btn-transfer" type="button" data-action="transfer" data-id="${contact.id}" title="Chuyển sang doanh nghiệp khác khi chuyển công tác">
                            <i class="fa-solid fa-right-left" style="margin-right:4px;"></i> Chuyển công ty
                        </button>
                    </div>

                    <div style="display:flex; gap:4px;">
                        ${historyList.length ? `
                            <button class="contact-action-btn" type="button" data-action="view-history" data-id="${contact.id}" title="Xem lịch sử điều chuyển công tác (${historyList.length})">
                                <i class="fa-solid fa-clock-rotate-left"></i> Lịch sử
                            </button>
                        ` : ""}
                        <button class="contact-action-btn" type="button" data-action="edit" data-id="${contact.id}" title="Chỉnh sửa thông tin liên hệ">
                            <i class="fa-solid fa-pen-to-square"></i> Sửa
                        </button>
                        <button class="contact-action-btn" type="button" data-action="delete" data-id="${contact.id}" title="Xóa người liên hệ" style="color:var(--crm-danger);">
                            <i class="fa-solid fa-trash-can"></i>
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
                        ? `<span class="primary-contact-badge"><i class="fa-solid fa-id-badge" style="color:var(--crm-primary); margin-right:4px;"></i> Đầu mối chính</span>` 
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
                            <i class="fa-solid fa-right-left" style="margin-right:4px;"></i> Chuyển
                        </button>
                        ${historyList.length ? `
                            <button class="contact-action-btn" style="padding:3px 8px; font-size:11.5px;" data-action="view-history" data-id="${contact.id}" title="Xem lịch sử chuyển công tác">
                                <i class="fa-solid fa-clock-rotate-left" style="margin-right:4px;"></i> (${historyList.length})
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
        if (!CrmApi.canSync("contact.update")) parent.querySelectorAll('[data-action="set-primary"],[data-action="transfer"],[data-action="edit"]').forEach(button => button.remove());
        if (!CrmApi.canSync("contact.delete")) parent.querySelectorAll('[data-action="delete"]').forEach(button => button.remove());
        // Set Primary
        parent.querySelectorAll('[data-action="set-primary"]').forEach(btn => {
            btn.addEventListener("click", async e => {
                e.stopPropagation();
                const id = btn.dataset.id;
                try {
                    const updated = await store.setPrimaryContact(id);
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
            btn.addEventListener("click", async e => {
                e.stopPropagation();
                openTransferModal(btn.dataset.id);
            });
        });

        // View History Modal
        parent.querySelectorAll('[data-action="view-history"]').forEach(btn => {
            btn.addEventListener("click", async e => {
                e.stopPropagation();
                openHistoryModal(btn.dataset.id);
            });
        });

        // Edit Contact Modal
        parent.querySelectorAll('[data-action="edit"]').forEach(btn => {
            btn.addEventListener("click", async e => {
                e.stopPropagation();
                openEditModal(btn.dataset.id);
            });
        });

        // Delete Contact
        parent.querySelectorAll('[data-action="delete"]').forEach(btn => {
            btn.addEventListener("click", async e => {
                e.stopPropagation();
                const id = btn.dataset.id;
                const contact = store.getContactById(id);
                if (!contact) return;

                if (confirm(`Bạn có chắc chắn muốn xóa người liên hệ "${contact.fullName}" (${contact.jobTitle} - ${contact.companyName})?`)) {
                    try { await store.deleteContact(id); } catch (error) { showToast(error.message,"error"); return; }
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
        if (!modal) { window.location.href="contacts.html?customerId="+encodeURIComponent(preselectedCustomerId || ""); return; }

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
    async function openTransferModal(contactId) {
        let contact = store.getContactById(contactId);
        if (!contact) return;
        activeContactId = contact.id;

        const modal = document.getElementById("transferContactModal");
        if (!modal) return;

        document.getElementById("transferContactName").textContent = contact.fullName;
        document.getElementById("transferCurrentCompany").textContent = contact.companyName;
        document.getElementById("transferCurrentJobTitle").textContent = contact.jobTitle;

        // Ensure available destination customers are loaded
        let availableCustomers = store.getAvailableCustomers();
        if (!availableCustomers || availableCustomers.length <= 1) {
            try {
                availableCustomers = await CrmApi.customers();
                store.customers = availableCustomers;
            } catch (err) {
                console.warn("Không thể tải danh sách khách hàng:", err);
            }
        }

        // Populate Destination Customers (excluding current)
        const selectNewCust = document.getElementById("transferNewCustomerId");
        if (selectNewCust) {
            const customers = (availableCustomers || []).filter(c => Number(c.id) !== Number(contact.customerId));
            if (customers.length === 0) {
                selectNewCust.innerHTML = `
                    <option value="" disabled selected>-- Không có doanh nghiệp khác để chuyển đến --</option>
                `;
            } else {
                selectNewCust.innerHTML = `
                    <option value="" disabled selected>-- Chọn doanh nghiệp chuyển đến --</option>
                    ${customers.map(c => `
                        <option value="${c.id}">${escapeHtml(c.companyName || c.name)}</option>
                    `).join("")}
                `;
            }
        }

        const jobEl = document.getElementById("transferNewJobTitle");
        if (jobEl) jobEl.value = contact.jobTitle || "";
        const deptEl = document.getElementById("transferNewDepartment");
        if (deptEl) deptEl.value = contact.department || "";
        const dateEl = document.getElementById("transferDate");
        if (dateEl) dateEl.value = new Date().toISOString().split("T")[0];
        const reasonEl = document.getElementById("transferReason");
        if (reasonEl) reasonEl.value = `Chuyển công tác từ ${contact.companyName} sang đơn vị mới từ tháng ${new Date().getMonth() + 1}/${new Date().getFullYear()}.`;
        const primaryEl = document.getElementById("transferSetPrimary");
        if (primaryEl) primaryEl.checked = false;

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
    async function openHistoryModal(contactId = null) {
        try { await store.loadHistory(contactId); } catch(error) { showToast(error.message,"error"); return; }
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
                                            <i class="fa-solid fa-user" style="margin-right:4px;"></i> ${escapeHtml(h.contactName)}: 
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
            contactForm.addEventListener("submit", async e => {
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
                        const updated = await store.updateContact(activeContactId, {
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
                        const created = await store.addContact({
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
            transferForm.addEventListener("submit", async e => {
                e.preventDefault();
                if (!activeContactId) return;

                const toCustomerId = document.getElementById("transferNewCustomerId")?.value;
                const newJobTitle = document.getElementById("transferNewJobTitle")?.value?.trim() || "";
                const newDepartment = document.getElementById("transferNewDepartment")?.value?.trim() || "";
                const transferDate = document.getElementById("transferDate")?.value || new Date().toISOString().split("T")[0];
                const reason = document.getElementById("transferReason")?.value?.trim() || "";
                const setAsPrimaryInNewComp = !!document.getElementById("transferSetPrimary")?.checked;

                if (!toCustomerId) {
                    alert("Vui lòng chọn doanh nghiệp chuyển đến.");
                    return;
                }
                if (!reason) {
                    alert("Vui lòng nhập ghi chú / lý do điều chuyển công tác.");
                    return;
                }

                try {
                    const result = await store.transferContact(activeContactId, {
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
                                    ${c.isPrimary ? '<span title="Đầu mối chính" style="font-size:14px; color:var(--crm-primary);"><i class="fa-solid fa-id-badge"></i></span>' : ''}
                                </div>
                            </div>

                            <div style="display:flex; flex-wrap:wrap; gap:4px;">
                                ${renderDecisionRoleBadge(c.decisionRole)}
                            </div>

                            <div style="font-size:12px; color:var(--crm-muted); display:flex; flex-direction:column; gap:2px;">
                                <div><i class="fa-solid fa-envelope" style="margin-right:4px;"></i> <a href="mailto:${escapeHtml(c.email)}" style="color:inherit; text-decoration:none;">${escapeHtml(c.email)}</a></div>
                                <div><i class="fa-solid fa-phone" style="margin-right:4px;"></i> <a href="tel:${escapeHtml(c.phone)}" style="color:inherit; text-decoration:none;">${escapeHtml(c.phone)}</a></div>
                            </div>

                            <div class="c360-contact-actions">
                                ${!c.isPrimary ? `
                                    <button class="contact-action-btn btn-set-primary" style="padding:2px 6px; font-size:11px;" data-action="set-primary" data-id="${c.id}" title="Đặt làm đầu mối chính">
                                        <i class="fa-solid fa-id-badge" style="color:var(--crm-primary); margin-right:4px;"></i> Đầu mối chính
                                    </button>
                                ` : ""}
                                <button class="contact-action-btn btn-transfer" style="padding:2px 6px; font-size:11px;" data-action="transfer" data-id="${c.id}" title="Chuyển công ty">
                                    <i class="fa-solid fa-right-left" style="margin-right:4px;"></i> Chuyển
                                </button>
                                <button class="contact-action-btn" style="padding:2px 6px; font-size:11px;" data-action="edit" data-id="${c.id}" title="Sửa">
                                    <i class="fa-solid fa-pen-to-square"></i>
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
        if (btnAdd) btnAdd.hidden = !CrmApi.canSync("contact.create");
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
            btnAddMain.addEventListener("click", () => openAddModal(new URLSearchParams(location.search).get("customerId")));
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
     * Export Contacts to Excel (Styled Spreadsheet) / CSV
     */
    function exportContactsToCsv() {
        const contacts = store.getAllContacts();
        if (!contacts.length) {
            alert("Không có dữ liệu người liên hệ để xuất.");
            return;
        }

        if (window.ExcelExportHelper) {
            const columns = [
                { label: "STT", key: "stt", width: 45, align: "center" },
                { label: "Mã LH", key: "code", width: 65, align: "center" },
                { label: "Họ và Tên", key: "fullName", width: 160, align: "left" },
                { label: "Doanh Nghiệp Trực Thuộc", key: "companyName", width: 220, align: "left" },
                { label: "Chức Danh", key: "jobTitle", width: 160, align: "left" },
                { label: "Phòng Ban", key: "department", width: 130, align: "left" },
                { label: "Email", key: "email", width: 170, align: "left" },
                { label: "Số Điện Thoại", key: "phone", width: 110, align: "center" },
                { label: "Vai Trò Quyết Định", key: "decisionRoleName", width: 180, align: "left" },
                { label: "Đầu Mối Chính", key: "primaryLabel", width: 100, align: "center" },
                { label: "Trạng Thái", key: "statusLabel", width: 100, align: "center" }
            ];

            const exportData = contacts.map((c, idx) => ({
                stt: idx + 1,
                code: `CT-${c.id}`,
                fullName: c.fullName || "—",
                companyName: c.companyName || "—",
                jobTitle: c.jobTitle || "—",
                department: c.department || "—",
                email: c.email || "—",
                phone: c.phone || "—",
                decisionRoleName: DECISION_ROLES[c.decisionRole]?.label || c.decisionRole || "—",
                primaryLabel: c.isPrimary ? "CÓ" : "KHÔNG",
                statusLabel: c.status === "ACTIVE" ? "HOẠT ĐỘNG" : "TẠM DỪNG"
            }));

            window.ExcelExportHelper.exportStyledExcel({
                title: "NOVACRM ENTERPRISE - DANH BẠ NGƯỜI LIÊN HỆ & ĐẦU MỐI DOANH NGHIỆP",
                filename: `Danh_Ba_Nguoi_Lien_He_NovaCRM_${new Date().toISOString().slice(0, 10)}.xls`,
                meta: `Ngày xuất: ${new Date().toLocaleDateString('vi-VN')} | Tổng số liên hệ: ${contacts.length} | Hệ thống NovaCRM Enterprise`,
                columns: columns,
                data: exportData
            });
            showToast("Đã xuất tệp Excel danh bạ người liên hệ thành công!");
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
        ingest: (customer,contacts) => store.ingest(customer,contacts),
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
    async function bootContacts() {
        setupFormListeners();
        for (const id of ["contactDepartment","contactNotes","contactStatus"]) {
            const input=document.getElementById(id);
            if (input) { input.required=false; input.disabled=true; input.closest(".form-field-group")?.setAttribute("hidden",""); }
        }
        try {
            const session=await CrmApi.session();
            document.getElementById("selectStatusFilter")?.setAttribute("hidden","");
            if (document.getElementById("contactsListContainer") || document.getElementById("customerTableBody")) {
                if (!session.permissions.includes("contact.read")) {
                    const container=document.getElementById("contactsListContainer");if(container)container.textContent="Bạn không có quyền xem người liên hệ.";
                    const add=document.getElementById("btnAddNewContact");if(add)add.style.display="none";
                    return;
                }
                await store.load();
                initContactsPage();
                const add=document.getElementById("btnAddNewContact");
                if (add) add.hidden=!session.permissions.includes("contact.create");
            }
        } catch(error) {
            const container=document.getElementById("contactsListContainer");
            if (container) container.textContent=error.message;
            showToast(error.message,"error");
        }
    }
    if(document.readyState==="loading") document.addEventListener("DOMContentLoaded",bootContacts);
    else bootContacts();

})();
