/**
 * ===================================================================
 * CRM-116 / S4-02: TẠO LEAD THỦ CÔNG & NHẬP LEAD HÀNG LOẠT TỪ EXCEL
 * Phụ trách: Senior Frontend Developer (Nông Quang Tiệp)
 * 
 * - Tạo Lead đơn lẻ thủ công (Hội thảo, Danh thiếp, Giới thiệu...)
 * - Nhập hàng loạt từ Excel / CSV (.xlsx, .xls, .csv)
 * - Tự động đối soát & Báo lỗi chi tiết từng dòng (Row-by-Row Error)
 * - Sandbox 1-Click nạp mẫu Hợp lệ & Có lỗi phục vụ kiểm thử UAT
 * - Tải file mẫu chuẩn Excel (.xlsx) và CSV với UTF-8 tiếng Việt
 * - Tích hợp chuyển đổi Lead thành Khách hàng CRM
 * ===================================================================
 */

"use strict";

(function () {
    const STORAGE_KEY_LEADS = "crm_captured_leads";
    const STORAGE_KEY_CUSTOMERS = "crm_ui_customers";
    const API_ENDPOINT = (window.CRM_BACKEND_BASE || `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`) + "/api/leads";

    // Regex Validation Standards
    const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    const PHONE_REGEX = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;

    // State
    let allLeads = [];
    let recentManualLeads = [];
    let previewRows = [];
    let selectedFile = null;
    let currentStep = 1;

    // DOM References
    const dom = {
        // Tabs
        tabBtnManual: document.getElementById("tabBtnManual"),
        tabBtnExcel: document.getElementById("tabBtnExcel"),
        tabBtnList: document.getElementById("tabBtnList"),
        paneManual: document.getElementById("paneManual"),
        paneExcel: document.getElementById("paneExcel"),
        paneList: document.getElementById("paneList"),
        badgeTotalLeads: document.getElementById("badgeTotalLeads"),

        // Template Download
        btnDownloadExcelTemplate: document.getElementById("btnDownloadExcelTemplate"),
        btnDownloadCsvTemplate: document.getElementById("btnDownloadCsvTemplate"),

        // Manual Form
        formManualLead: document.getElementById("formManualLead"),
        manualFullName: document.getElementById("manualFullName"),
        manualCompany: document.getElementById("manualCompany"),
        manualPhone: document.getElementById("manualPhone"),
        manualEmail: document.getElementById("manualEmail"),
        manualSource: document.getElementById("manualSource"),
        manualTitle: document.getElementById("manualTitle"),
        manualCompanySize: document.getElementById("manualCompanySize"),
        manualEstimatedBudget: document.getElementById("manualEstimatedBudget"),
        manualNotes: document.getElementById("manualNotes"),
        btnFillDemoManualLead: document.getElementById("btnFillDemoManualLead"),
        btnSubmitManualLead: document.getElementById("btnSubmitManualLead"),
        btnSubmitText: document.getElementById("btnSubmitText"),
        recentManualLeadsList: document.getElementById("recentManualLeadsList"),

        // Stepper Nodes
        stepNode1: document.getElementById("stepNode1"),
        stepNode2: document.getElementById("stepNode2"),
        stepNode3: document.getElementById("stepNode3"),
        stepBox1: document.getElementById("stepBox1"),
        stepBox2: document.getElementById("stepBox2"),
        stepBox3: document.getElementById("stepBox3"),

        // Step 1: Dropzone
        dropzoneContainer: document.getElementById("dropzoneContainer"),
        excelFileInput: document.getElementById("excelFileInput"),
        selectedFileCard: document.getElementById("selectedFileCard"),
        selectedFileName: document.getElementById("selectedFileName"),
        selectedFileSize: document.getElementById("selectedFileSize"),
        btnRemoveSelectedFile: document.getElementById("btnRemoveSelectedFile"),
        btnLoadValidDemo: document.getElementById("btnLoadValidDemo"),
        btnLoadInvalidDemo: document.getElementById("btnLoadInvalidDemo"),

        // Step 2: Preview
        statTotalRows: document.getElementById("statTotalRows"),
        statValidRows: document.getElementById("statValidRows"),
        statErrorRows: document.getElementById("statErrorRows"),
        previewSearchInput: document.getElementById("previewSearchInput"),
        previewFilterMode: document.getElementById("previewFilterMode"),
        chkOnlyValid: document.getElementById("chkOnlyValid"),
        previewTableBody: document.getElementById("previewTableBody"),
        btnBackToStep1: document.getElementById("btnBackToStep1"),
        btnCancelPreview: document.getElementById("btnCancelPreview"),
        btnExecuteImport: document.getElementById("btnExecuteImport"),
        btnExecuteImportText: document.getElementById("btnExecuteImportText"),

        // Step 3: Result
        resSuccessCount: document.getElementById("resSuccessCount"),
        resSkippedCount: document.getElementById("resSkippedCount"),
        btnImportAnotherFile: document.getElementById("btnImportAnotherFile"),
        btnGoToLeadsList: document.getElementById("btnGoToLeadsList"),

        // Tab 3: List
        listSearchInput: document.getElementById("listSearchInput"),
        listFilterSource: document.getElementById("listFilterSource"),
        listFilterStatus: document.getElementById("listFilterStatus"),
        btnResetListFilters: document.getElementById("btnResetListFilters"),
        listTableBody: document.getElementById("listTableBody"),

        // Toast Container
        toastContainer: document.getElementById("leadManualToastContainer")
    };

    /* ===================================================================
       1. INITIALIZATION
       =================================================================== */

    function init() {
        loadExistingLeads();
        bindTabs();
        bindManualFormEvents();
        bindTemplateDownloadEvents();
        bindExcelImportEvents();
        bindListEvents();

        renderRecentManualLeads();
        renderLeadsListTable();
    }

    function loadExistingLeads() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY_LEADS);
            if (raw) {
                allLeads = JSON.parse(raw);
            } else {
                // Seed initial data if empty
                allLeads = [
                    {
                        id: "LEAD-201",
                        fullName: "Nguyễn Minh Quang",
                        phone: "0987123456",
                        email: "quang.nm@digitech.vn",
                        companyName: "Công ty CP Công nghệ DigiTech VN",
                        source: "Hội thảo / Sự kiện chuyên ngành",
                        notes: "Gặp tại TechExpo 2026, quan tâm CRM B2B cho 40 sales.",
                        status: "NEW",
                        createdAt: new Date(Date.now() - 3600000).toISOString()
                    },
                    {
                        id: "LEAD-202",
                        fullName: "Trần Thu Hà",
                        phone: "0912345678",
                        email: "ha.tran@hoalinh.vn",
                        companyName: "Tập đoàn Dược phẩm Hoa Linh",
                        source: "Gặp trực tiếp / Trao đổi Danh thiếp",
                        notes: "Trao đổi danh thiếp tại hội nghị khách hàng doanh nghiệp.",
                        status: "CONTACTED",
                        createdAt: new Date(Date.now() - 7200000).toISOString()
                    }
                ];
                saveLeadsLocally();
            }
        } catch (e) {
            allLeads = [];
        }
        updateBadgeTotal();
    }

    function saveLeadsLocally() {
        try {
            localStorage.setItem(STORAGE_KEY_LEADS, JSON.stringify(allLeads));
            updateBadgeTotal();
        } catch (e) {
            console.error("Lỗi ghi localStorage:", e);
        }
    }

    function updateBadgeTotal() {
        if (dom.badgeTotalLeads) dom.badgeTotalLeads.textContent = allLeads.length;
    }

    /* ===================================================================
       2. TABS SWITCHING
       =================================================================== */

    function bindTabs() {
        if (dom.tabBtnManual) dom.tabBtnManual.addEventListener("click", () => switchTab("manual"));
        if (dom.tabBtnExcel) dom.tabBtnExcel.addEventListener("click", () => switchTab("excel"));
        if (dom.tabBtnList) dom.tabBtnList.addEventListener("click", () => switchTab("list"));
    }

    function switchTab(tabId) {
        [dom.tabBtnManual, dom.tabBtnExcel, dom.tabBtnList].forEach(b => b.classList.remove("active"));
        [dom.paneManual, dom.paneExcel, dom.paneList].forEach(p => p.style.display = "none");

        if (tabId === "manual") {
            dom.tabBtnManual.classList.add("active");
            dom.paneManual.style.display = "block";
            renderRecentManualLeads();
        } else if (tabId === "excel") {
            dom.tabBtnExcel.classList.add("active");
            dom.paneExcel.style.display = "block";
        } else {
            dom.tabBtnList.classList.add("active");
            dom.paneList.style.display = "block";
            renderLeadsListTable();
        }
    }

    /* ===================================================================
       3. TAB 1: MANUAL LEAD CREATION LOGIC
       =================================================================== */

    function bindManualFormEvents() {
        // Quick Fill Demo button
        if (dom.btnFillDemoManualLead) {
            dom.btnFillDemoManualLead.addEventListener("click", fillRandomManualDemo);
        }

        // Live validation on blur/input
        if (dom.manualFullName) {
            dom.manualFullName.addEventListener("input", () => validateField(dom.manualFullName, dom.manualFullName.value.trim().length >= 2));
        }
        if (dom.manualPhone) {
            dom.manualPhone.addEventListener("input", () => {
                const clean = dom.manualPhone.value.replace(/\s+/g, "");
                validateField(dom.manualPhone, PHONE_REGEX.test(clean));
            });
        }
        if (dom.manualEmail) {
            dom.manualEmail.addEventListener("input", () => validateField(dom.manualEmail, EMAIL_REGEX.test(dom.manualEmail.value.trim())));
        }
        if (dom.manualSource) {
            dom.manualSource.addEventListener("change", () => validateField(dom.manualSource, !!dom.manualSource.value));
        }

        // Form Submit
        if (dom.formManualLead) {
            dom.formManualLead.addEventListener("submit", handleManualSubmit);
        }
    }

    function validateField(inputEl, isValid) {
        if (!inputEl) return false;
        if (isValid) {
            inputEl.classList.remove("form-input-error");
        } else {
            inputEl.classList.add("form-input-error");
        }
        return isValid;
    }

    function fillRandomManualDemo() {
        const demoLeads = [
            {
                name: "Trịnh Đình Khôi",
                company: "Công ty CP Dịch vụ Viễn thông Toàn Cầu",
                phone: "0982345678",
                email: "khoi.td@globaltelecom.vn",
                source: "Hội thảo / Sự kiện chuyên ngành",
                title: "Giám đốc Phát triển Kinh doanh",
                size: "50 - 200 nhân sự",
                budget: "50 - 150 triệu",
                notes: "Gặp gỡ tại Hội nghị Chuyển đổi số 2026. Khách hàng có nhu cầu trang bị CRM cho 60 sales."
            },
            {
                name: "Bùi Phương Linh",
                company: "Tập đoàn Bất động sản SunLand",
                phone: "0918765432",
                email: "linh.bp@sunlandgroup.com.vn",
                source: "Gặp trực tiếp / Trao đổi Danh thiếp",
                title: "Trưởng phòng Marketing",
                size: "Trên 200 nhân sự",
                budget: "Trên 150 triệu",
                notes: "Trao đổi danh thiếp tại Cafe Doanh nhân. Cần tích hợp quản lý lead từ nhiều nguồn dự án."
            },
            {
                name: "Đặng Tiến Dũng",
                company: "Công ty TNHH Giải pháp Logistic Á Châu",
                phone: "0903456789",
                email: "dung.dt@achau-logistics.vn",
                source: "Người quen / Đối tác giới thiệu",
                title: "Phó Tổng Giám Đốc",
                size: "10 - 50 nhân sự",
                budget: "20 - 50 triệu",
                notes: "Anh Tuấn (Giám đốc Techcorp) giới thiệu sang. Cần triển khai CRM trong tháng 11/2026."
            }
        ];

        const pick = demoLeads[Math.floor(Math.random() * demoLeads.length)];
        dom.manualFullName.value = pick.name;
        dom.manualCompany.value = pick.company;
        dom.manualPhone.value = pick.phone;
        dom.manualEmail.value = pick.email;
        dom.manualSource.value = pick.source;
        dom.manualTitle.value = pick.title;
        dom.manualCompanySize.value = pick.size;
        dom.manualEstimatedBudget.value = pick.budget;
        dom.manualNotes.value = pick.notes;

        // Reset errors
        [dom.manualFullName, dom.manualPhone, dom.manualEmail, dom.manualSource].forEach(el => el.classList.remove("form-input-error"));

        showToast("Đã điền thông tin Lead mẫu chuẩn doanh nghiệp!", "info");
    }

    async function handleManualSubmit(e) {
        e.preventDefault();

        const nameVal = dom.manualFullName.value.trim();
        const phoneClean = dom.manualPhone.value.replace(/\s+/g, "");
        const emailVal = dom.manualEmail.value.trim();
        const sourceVal = dom.manualSource.value;

        const isNameOk = validateField(dom.manualFullName, nameVal.length >= 2);
        const isPhoneOk = validateField(dom.manualPhone, PHONE_REGEX.test(phoneClean));
        const isEmailOk = validateField(dom.manualEmail, EMAIL_REGEX.test(emailVal));
        const isSourceOk = validateField(dom.manualSource, !!sourceVal);

        if (!isNameOk || !isPhoneOk || !isEmailOk || !isSourceOk) {
            showToast("Vui lòng kiểm tra lại các trường dữ liệu bắt buộc bị sai hoặc bỏ trống.", "danger");
            return;
        }

        const newLeadPayload = {
            id: `LD-${Date.now().toString().slice(-6)}`,
            fullName: nameVal,
            companyName: dom.manualCompany.value.trim(),
            phone: phoneClean,
            email: emailVal,
            source: sourceVal,
            title: dom.manualTitle.value.trim(),
            companySize: dom.manualCompanySize.value,
            budget: dom.manualEstimatedBudget.value,
            notes: dom.manualNotes.value.trim(),
            status: "NEW",
            createdAt: new Date().toISOString()
        };

        // Loading state
        dom.btnSubmitManualLead.disabled = true;
        dom.btnSubmitText.innerHTML = '<span class="btn-spinner"></span> Đang lưu Lead...';

        try {
            // Real API Call (POST /api/leads)
            const resp = await fetch(API_ENDPOINT, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify(newLeadPayload)
            });

            if (resp.ok) {
                const data = await resp.json().catch(() => ({}));
                if (data.id) newLeadPayload.id = data.id;
            }
        } catch (err) {
            console.warn("Backend API /api/leads không phản hồi, lưu cục bộ localStorage:", err);
        }

        // Save locally to storage
        allLeads.unshift(newLeadPayload);
        recentManualLeads.unshift(newLeadPayload);
        saveLeadsLocally();

        setTimeout(() => {
            dom.btnSubmitManualLead.disabled = false;
            dom.btnSubmitText.innerHTML = 'Lưu Khách hàng tiềm năng';
            dom.formManualLead.reset();

            renderRecentManualLeads();
            showToast(`Đã tạo thành công Lead "${newLeadPayload.fullName}"!`, "success");
        }, 300);
    }

    function renderRecentManualLeads() {
        if (!dom.recentManualLeadsList) return;

        const recent = recentManualLeads.length > 0 
            ? recentManualLeads.slice(0, 5) 
            : allLeads.filter(l => l.source !== "Website Form" && l.source !== "Nhập hàng loạt từ Excel").slice(0, 5);

        if (recent.length === 0) {
            dom.recentManualLeadsList.innerHTML = `
                <div style="font-size: 12px; color: var(--crm-muted); text-align: center; padding: 16px 0;">
                    Chưa có Lead nào được tạo thủ công gần đây.
                </div>
            `;
            return;
        }

        let html = "";
        recent.forEach(lead => {
            const initials = getInitials(lead.fullName);
            html += `
                <div class="recent-lead-item">
                    <div class="recent-lead-avatar">${escapeHtml(initials)}</div>
                    <div class="recent-lead-info">
                        <div class="recent-lead-name">${escapeHtml(lead.fullName)}</div>
                        <div class="recent-lead-sub">
                            ${escapeHtml(lead.companyName || "Chưa có cty")} &bull; ${escapeHtml(lead.phone)}
                        </div>
                    </div>
                    <span class="row-status-badge valid" style="font-size:10px;">${escapeHtml(lead.source)}</span>
                </div>
            `;
        });

        dom.recentManualLeadsList.innerHTML = html;
    }

    /* ===================================================================
       4. TEMPLATE GENERATION & DOWNLOADS (.XLSX & .CSV)
       =================================================================== */

    function bindTemplateDownloadEvents() {
        if (dom.btnDownloadExcelTemplate) {
            dom.btnDownloadExcelTemplate.addEventListener("click", downloadExcelTemplate);
        }
        if (dom.btnDownloadCsvTemplate) {
            dom.btnDownloadCsvTemplate.addEventListener("click", downloadCsvTemplate);
        }
    }

    /**
     * Tải file mẫu Excel (.xlsx) chuẩn định dạng tiếng Việt
     */
    function downloadExcelTemplate() {
        const templateData = [
            {
                "Họ và tên *": "Nguyễn Minh Quang",
                "Số điện thoại *": "0987123456",
                "Email *": "quang.nm@digitech.vn",
                "Tên Công ty / Tổ chức": "Công ty Cổ phần Công nghệ DigiTech VN",
                "Nguồn Lead *": "Hội thảo / Sự kiện chuyên ngành",
                "Nhu cầu / Ghi chú": "Cần báo giá và tư vấn triển khai CRM cho 40 Sales B2B"
            },
            {
                "Họ và tên *": "Trần Thu Hà",
                "Số điện thoại *": "0912345678",
                "Email *": "ha.tran@hoalinh.vn",
                "Tên Công ty / Tổ chức": "Tập đoàn Dược phẩm Hoa Linh",
                "Nguồn Lead *": "Gặp trực tiếp / Trao đổi Danh thiếp",
                "Nhu cầu / Ghi chú": "Quan tâm đến chữ ký số và phân quyền chi nhánh"
            },
            {
                "Họ và tên *": "Lê Hoàng Cường",
                "Số điện thoại *": "0903456789",
                "Email *": "cuong.lh@vinasmart.vn",
                "Tên Công ty / Tổ chức": "Công ty Giải pháp Thông minh VinaSmart",
                "Nguồn Lead *": "Người quen / Đối tác giới thiệu",
                "Nhu cầu / Ghi chú": "Cần tích hợp quản lý cơ hội và báo giá hợp đồng"
            }
        ];

        if (window.XLSX && XLSX.utils && XLSX.writeFile) {
            const worksheet = XLSX.utils.json_to_sheet(templateData);
            // Auto column widths
            worksheet["!cols"] = [
                { wch: 22 }, // Họ và tên
                { wch: 16 }, // SĐT
                { wch: 25 }, // Email
                { wch: 38 }, // Công ty
                { wch: 30 }, // Nguồn
                { wch: 45 }  // Ghi chú
            ];
            const workbook = XLSX.utils.book_new();
            XLSX.utils.book_append_sheet(workbook, worksheet, "DanhSachLead");
            XLSX.writeFile(workbook, "mau_nhap_lead_hang_loat.xlsx");
            showToast("Đã tải xuống tệp mẫu Excel: mau_nhap_lead_hang_loat.xlsx", "success");
        } else {
            // Fallback to CSV if SheetJS library is not yet loaded
            downloadCsvTemplate();
        }
    }

    /**
     * Tải file mẫu CSV với UTF-8 BOM chuẩn
     */
    function downloadCsvTemplate() {
        const headers = ["Họ và tên *", "Số điện thoại *", "Email *", "Tên Công ty / Tổ chức", "Nguồn Lead *", "Nhu cầu / Ghi chú"];
        const rows = [
            ["Nguyễn Minh Quang", "0987123456", "quang.nm@digitech.vn", "Công ty Cổ phần Công nghệ DigiTech VN", "Hội thảo / Sự kiện chuyên ngành", "Cần báo giá và tư vấn triển khai CRM cho 40 Sales"],
            ["Trần Thu Hà", "0912345678", "ha.tran@hoalinh.vn", "Tập đoàn Dược phẩm Hoa Linh", "Gặp trực tiếp / Trao đổi Danh thiếp", "Quan tâm đến chữ ký số và phân quyền chi nhánh"],
            ["Lê Hoàng Cường", "0903456789", "cuong.lh@vinasmart.vn", "Công ty VinaSmart", "Người quen / Đối tác giới thiệu", "Cần tích hợp quản lý cơ hội và báo giá hợp đồng"]
        ];

        let csvContent = "\uFEFF"; // UTF-8 BOM for Excel
        csvContent += headers.map(h => `"${h}"`).join(",") + "\r\n";
        rows.forEach(r => {
            csvContent += r.map(c => `"${c.replace(/"/g, '""')}"`).join(",") + "\r\n";
        });

        const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = "mau_nhap_lead_hang_loat.csv";
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);

        showToast("Đã tải xuống tệp mẫu CSV: mau_nhap_lead_hang_loat.csv", "success");
    }

    /* ===================================================================
       5. TAB 2: EXCEL IMPORT WIZARD & PARSER
       =================================================================== */

    function bindExcelImportEvents() {
        // Dropzone click
        if (dom.dropzoneContainer && dom.excelFileInput) {
            dom.dropzoneContainer.addEventListener("click", () => dom.excelFileInput.click());
            dom.excelFileInput.addEventListener("change", handleFileSelect);

            // Drag and drop events
            dom.dropzoneContainer.addEventListener("dragover", (e) => {
                e.preventDefault();
                dom.dropzoneContainer.classList.add("dragover");
            });

            dom.dropzoneContainer.addEventListener("dragleave", () => {
                dom.dropzoneContainer.classList.remove("dragover");
            });

            dom.dropzoneContainer.addEventListener("drop", (e) => {
                e.preventDefault();
                dom.dropzoneContainer.classList.remove("dragover");
                if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
                    processIncomingFile(e.dataTransfer.files[0]);
                }
            });
        }

        // Remove selected file
        if (dom.btnRemoveSelectedFile) {
            dom.btnRemoveSelectedFile.addEventListener("click", resetStep1);
        }

        // Sandbox 1-Click Loaders
        if (dom.btnLoadValidDemo) {
            dom.btnLoadValidDemo.addEventListener("click", loadValidDemoSet);
        }
        if (dom.btnLoadInvalidDemo) {
            dom.btnLoadInvalidDemo.addEventListener("click", loadInvalidDemoSet);
        }

        // Step 2 toolbar events
        if (dom.previewSearchInput) {
            dom.previewSearchInput.addEventListener("input", debounce(renderPreviewTable, 200));
        }
        if (dom.previewFilterMode) {
            dom.previewFilterMode.addEventListener("change", renderPreviewTable);
        }

        // Back to Step 1
        if (dom.btnBackToStep1) dom.btnBackToStep1.addEventListener("click", () => setStepperStep(1));
        if (dom.btnCancelPreview) dom.btnCancelPreview.addEventListener("click", resetStep1);

        // Execute Import
        if (dom.btnExecuteImport) {
            dom.btnExecuteImport.addEventListener("click", executeImportProcess);
        }

        // Step 3 actions
        if (dom.btnImportAnotherFile) {
            dom.btnImportAnotherFile.addEventListener("click", () => {
                resetStep1();
                setStepperStep(1);
            });
        }
        if (dom.btnGoToLeadsList) {
            dom.btnGoToLeadsList.addEventListener("click", () => switchTab("list"));
        }
    }

    function handleFileSelect(e) {
        if (e.target.files && e.target.files.length > 0) {
            processIncomingFile(e.target.files[0]);
        }
    }

    function processIncomingFile(file) {
        const ext = file.name.split(".").pop().toLowerCase();
        if (!["xlsx", "xls", "csv"].includes(ext)) {
            showToast("Định dạng tệp không được hỗ trợ. Vui lòng chọn tệp .xlsx, .xls hoặc .csv.", "danger");
            return;
        }

        if (file.size > 5 * 1024 * 1024) {
            showToast("Kích thước tệp vượt quá 5MB cho phép.", "danger");
            return;
        }

        selectedFile = file;
        dom.selectedFileName.textContent = file.name;
        dom.selectedFileSize.textContent = `${(file.size / 1024).toFixed(1)} KB · Sẵn sàng đối soát dữ liệu`;
        dom.selectedFileCard.style.display = "flex";

        showToast(`Đã nhận tệp: ${file.name}. Đang tiến hành phân tích đối soát dữ liệu...`, "info");

        // Parse file
        parseFileContent(file);
    }

    function resetStep1() {
        selectedFile = null;
        if (dom.excelFileInput) dom.excelFileInput.value = "";
        dom.selectedFileCard.style.display = "none";
        previewRows = [];
        setStepperStep(1);
    }

    function setStepperStep(step) {
        currentStep = step;
        [dom.stepNode1, dom.stepNode2, dom.stepNode3].forEach((node, idx) => {
            node.classList.remove("active", "completed");
            if (idx + 1 === step) node.classList.add("active");
            if (idx + 1 < step) node.classList.add("completed");
        });

        dom.stepBox1.style.display = step === 1 ? "block" : "none";
        dom.stepBox2.style.display = step === 2 ? "block" : "none";
        dom.stepBox3.style.display = step === 3 ? "block" : "none";
    }

    /**
     * Đọc tệp Excel hoặc CSV qua SheetJS
     */
    function parseFileContent(file) {
        const reader = new FileReader();

        reader.onload = function (e) {
            try {
                const data = new Uint8Array(e.target.result);
                if (window.XLSX && XLSX.read) {
                    const workbook = XLSX.read(data, { type: "array" });
                    const firstSheetName = workbook.SheetNames[0];
                    const worksheet = workbook.Sheets[firstSheetName];
                    const jsonRows = XLSX.utils.sheet_to_json(worksheet, { header: 1 });

                    processRawRows(jsonRows);
                } else {
                    // Fallback simple line parsing for CSV
                    fallbackCsvParse(new TextDecoder().decode(data));
                }
            } catch (err) {
                console.error("Lỗi phân tích tệp Excel:", err);
                showToast("Không thể phân tích dữ liệu tệp. Vui lòng kiểm tra lại định dạng bảng tính.", "danger");
            }
        };

        reader.readAsArrayBuffer(file);
    }

    function fallbackCsvParse(text) {
        const lines = text.split(/\r?\n/).filter(l => l.trim().length > 0);
        const rows = lines.map(line => line.split(",").map(cell => cell.replace(/^"(.*)"$/, "$1").trim()));
        processRawRows(rows);
    }

    /**
     * Chuẩn hóa và kiểm tra từng dòng dữ liệu (Row-by-Row Validation)
     */
    function processRawRows(matrix) {
        if (!matrix || matrix.length < 2) {
            showToast("Tệp không chứa dữ liệu hoặc thiếu dòng tiêu đề.", "danger");
            return;
        }

        // Header mapping
        const headers = matrix[0].map(h => String(h || "").trim().toLowerCase());
        
        const colIdx = {
            fullName: headers.findIndex(h => h.includes("họ và tên") || h.includes("họ tên") || h.includes("fullname") || h.includes("tên")),
            phone: headers.findIndex(h => h.includes("số điện thoại") || h.includes("sđt") || h.includes("phone")),
            email: headers.findIndex(h => h.includes("email") || h.includes("thư")),
            company: headers.findIndex(h => h.includes("công ty") || h.includes("doanh nghiệp") || h.includes("tổ chức") || h.includes("company")),
            source: headers.findIndex(h => h.includes("nguồn") || h.includes("source")),
            notes: headers.findIndex(h => h.includes("nhu cầu") || h.includes("ghi chú") || h.includes("notes"))
        };

        const parsedRows = [];

        for (let i = 1; i < matrix.length; i++) {
            const rawRow = matrix[i];
            if (!rawRow || rawRow.length === 0 || rawRow.every(c => !c || String(c).trim() === "")) {
                continue; // Skip empty lines
            }

            const fullName = colIdx.fullName !== -1 ? String(rawRow[colIdx.fullName] || "").trim() : String(rawRow[0] || "").trim();
            const phone = colIdx.phone !== -1 ? String(rawRow[colIdx.phone] || "").trim().replace(/\s+/g, "") : String(rawRow[1] || "").trim().replace(/\s+/g, "");
            const email = colIdx.email !== -1 ? String(rawRow[colIdx.email] || "").trim() : String(rawRow[2] || "").trim();
            const company = colIdx.company !== -1 ? String(rawRow[colIdx.company] || "").trim() : String(rawRow[3] || "").trim();
            const source = colIdx.source !== -1 ? String(rawRow[colIdx.source] || "").trim() : String(rawRow[4] || "").trim();
            const notes = colIdx.notes !== -1 ? String(rawRow[colIdx.notes] || "").trim() : String(rawRow[5] || "").trim();

            const errors = [];

            // Validation rules
            if (!fullName || fullName.length < 2) {
                errors.push("Thiếu Họ và tên hoặc họ tên quá ngắn (< 2 ký tự)");
            }

            if (!phone) {
                errors.push("Thiếu số điện thoại bắt buộc");
            } else if (!PHONE_REGEX.test(phone)) {
                errors.push(`Số điện thoại không đúng chuẩn 10 chữ số (${phone})`);
            }

            if (!email) {
                errors.push("Thiếu địa chỉ email bắt buộc");
            } else if (!EMAIL_REGEX.test(email)) {
                errors.push(`Email không đúng định dạng RFC chuẩn (${email})`);
            }

            if (!source) {
                errors.push("Thiếu thông tin Nguồn Lead bắt buộc");
            }

            parsedRows.push({
                rowNum: i + 1,
                fullName,
                phone,
                email,
                companyName: company || "Chưa cung cấp",
                source: source || "Nhập hàng loạt từ Excel",
                notes: notes || "—",
                isValid: errors.length === 0,
                errors: errors
            });
        }

        previewRows = parsedRows;
        setStepperStep(2);
        renderPreviewStats();
        renderPreviewTable();
    }

    /**
     * Nạp dữ liệu mẫu HỢP LỆ (Sandbox Test 1)
     */
    function loadValidDemoSet() {
        dom.selectedFileName.textContent = "mau_lead_hop_le_chuan.xlsx";
        dom.selectedFileSize.textContent = "4.2 KB · Dữ liệu kiểm thử Hợp lệ 100%";
        dom.selectedFileCard.style.display = "flex";

        previewRows = [
            {
                rowNum: 2,
                fullName: "Nguyễn Minh Quang",
                phone: "0987123456",
                email: "quang.nm@digitech.vn",
                companyName: "Công ty CP Công nghệ DigiTech VN",
                source: "Hội thảo / Sự kiện chuyên ngành",
                notes: "Cần báo giá CRM cho 40 Sales",
                isValid: true,
                errors: []
            },
            {
                rowNum: 3,
                fullName: "Trần Thu Hà",
                phone: "0912345678",
                email: "ha.tran@hoalinh.vn",
                companyName: "Tập đoàn Dược phẩm Hoa Linh",
                source: "Gặp trực tiếp / Trao đổi Danh thiếp",
                notes: "Quan tâm đến chữ ký số và phân quyền chi nhánh",
                isValid: true,
                errors: []
            },
            {
                rowNum: 4,
                fullName: "Lê Hoàng Cường",
                phone: "0903456789",
                email: "cuong.lh@vinasmart.vn",
                companyName: "Công ty CP VinaSmart Global",
                source: "Người quen / Đối tác giới thiệu",
                notes: "Cần tích hợp quản lý cơ hội và báo giá",
                isValid: true,
                errors: []
            },
            {
                rowNum: 5,
                fullName: "Phạm Thu Dung",
                phone: "0933221100",
                email: "dung.pt@bachhoasach.vn",
                companyName: "Chuỗi Bách Hóa Sạch Miền Bắc",
                source: "Tư vấn qua điện thoại (Telesales)",
                notes: "Muốn đặt lịch demo quản lý 5.000 khách hàng",
                isValid: true,
                errors: []
            },
            {
                rowNum: 6,
                fullName: "Vũ Đức Thắng",
                phone: "0977889900",
                email: "thang.vd@ecohome.com.vn",
                companyName: "Kiến trúc Nội thất EcoHome",
                source: "Hội thảo / Sự kiện chuyên ngành",
                notes: "Cần phần mềm theo dõi tiến độ hợp đồng thiết kế",
                isValid: true,
                errors: []
            }
        ];

        setStepperStep(2);
        renderPreviewStats();
        renderPreviewTable();
        showToast("✨ Đã nạp thành công 5 dòng dữ liệu mẫu HỢP LỆ chuẩn để kiểm thử!", "success");
    }

    /**
     * Nạp dữ liệu mẫu CÓ LỖI (Sandbox Test 2)
     */
    function loadInvalidDemoSet() {
        dom.selectedFileName.textContent = "mau_kiem_thu_co_loi_demo.xlsx";
        dom.selectedFileSize.textContent = "5.1 KB · Dữ liệu kiểm thử lỗi quy chuẩn";
        dom.selectedFileCard.style.display = "flex";

        previewRows = [
            {
                rowNum: 2,
                fullName: "Nguyễn Văn Hợp Lệ",
                phone: "0988112233",
                email: "hople.nguyen@saoviet.vn",
                companyName: "Công ty Sao Việt",
                source: "Hội thảo / Sự kiện chuyên ngành",
                notes: "Dòng này hoàn toàn hợp lệ",
                isValid: true,
                errors: []
            },
            {
                rowNum: 3,
                fullName: "",
                phone: "0912233445",
                email: "thieuten@company.com",
                companyName: "Công ty Alpha",
                source: "Gặp trực tiếp / Trao đổi Danh thiếp",
                notes: "Bị thiếu họ tên",
                isValid: false,
                errors: ["Thiếu Họ và tên hoặc họ tên quá ngắn (< 2 ký tự)"]
            },
            {
                rowNum: 4,
                fullName: "Trần Sai SĐT",
                phone: "09123",
                email: "saisdt@beta.vn",
                companyName: "Công ty Beta",
                source: "Người quen / Đối tác giới thiệu",
                notes: "SĐT chỉ có 5 chữ số",
                isValid: false,
                errors: ["Số điện thoại không đúng chuẩn 10 chữ số (09123)"]
            },
            {
                rowNum: 5,
                fullName: "Lê Thiếu Email",
                phone: "0934567890",
                email: "",
                companyName: "Tập đoàn Gamma",
                source: "Tư vấn qua điện thoại (Telesales)",
                notes: "Bỏ trống trường email",
                isValid: false,
                errors: ["Thiếu địa chỉ email bắt buộc"]
            },
            {
                rowNum: 6,
                fullName: "Phạm Sai Email",
                phone: "0976543210",
                email: "email_khong_hop_le",
                companyName: "Công ty Delta",
                source: "Hội thảo / Sự kiện chuyên ngành",
                notes: "Email thiếu ký tự @ và domain",
                isValid: false,
                errors: ["Email không đúng định dạng RFC chuẩn (email_khong_hop_le)"]
            },
            {
                rowNum: 7,
                fullName: "Hoàng Minh Tuấn",
                phone: "0909988776",
                email: "tuan.hm@omega.vn",
                companyName: "Tập đoàn Omega",
                source: "Gặp trực tiếp / Trao đổi Danh thiếp",
                notes: "Dòng này hợp lệ",
                isValid: true,
                errors: []
            },
            {
                rowNum: 8,
                fullName: "Vũ Thiếu Nguồn",
                phone: "0981122334",
                email: "thieunguon@vina.com",
                companyName: "Công ty VinaCorp",
                source: "",
                notes: "Bị thiếu cột nguồn tiếp nhận",
                isValid: false,
                errors: ["Thiếu thông tin Nguồn Lead bắt buộc"]
            },
            {
                rowNum: 9,
                fullName: "Đỗ Đa Lỗi Cùng Lúc",
                phone: "0999abc",
                email: "sai_cuc_bo",
                companyName: "Công ty Zeta",
                source: "",
                notes: "Vi phạm SĐT, Email và Nguồn",
                isValid: false,
                errors: [
                    "Số điện thoại không đúng chuẩn 10 chữ số (0999abc)",
                    "Email không đúng định dạng RFC chuẩn (sai_cuc_bo)",
                    "Thiếu thông tin Nguồn Lead bắt buộc"
                ]
            }
        ];

        setStepperStep(2);
        renderPreviewStats();
        renderPreviewTable();
        showToast("⚡ Đã nạp thành công 8 dòng dữ liệu mẫu KIỂM THỬ LỖI!", "warning");
    }

    function renderPreviewStats() {
        const total = previewRows.length;
        const valid = previewRows.filter(r => r.isValid).length;
        const errors = total - valid;

        if (dom.statTotalRows) dom.statTotalRows.textContent = total;
        if (dom.statValidRows) dom.statValidRows.textContent = valid;
        if (dom.statErrorRows) dom.statErrorRows.textContent = errors;

        // Button activate condition
        if (dom.btnExecuteImport) {
            dom.btnExecuteImport.disabled = valid === 0;
            if (valid === 0) {
                dom.btnExecuteImport.title = "Không có dòng dữ liệu hợp lệ nào để import.";
            } else {
                dom.btnExecuteImport.title = `Nhập ${valid} dòng hợp lệ vào hệ thống`;
            }
        }
    }

    function renderPreviewTable() {
        if (!dom.previewTableBody) return;

        const query = dom.previewSearchInput ? dom.previewSearchInput.value.trim().toLowerCase() : "";
        const filterMode = dom.previewFilterMode ? dom.previewFilterMode.value : "ALL";

        const filtered = previewRows.filter(row => {
            if (filterMode === "VALID" && !row.isValid) return false;
            if (filterMode === "ERROR" && row.isValid) return false;

            if (query) {
                const matchName = (row.fullName || "").toLowerCase().includes(query);
                const matchPhone = (row.phone || "").toLowerCase().includes(query);
                const matchEmail = (row.email || "").toLowerCase().includes(query);
                const matchCompany = (row.companyName || "").toLowerCase().includes(query);
                if (!matchName && !matchPhone && !matchEmail && !matchCompany) return false;
            }

            return true;
        });

        if (filtered.length === 0) {
            dom.previewTableBody.innerHTML = `
                <tr>
                    <td colspan="9" style="text-align: center; padding: 24px; color: var(--crm-muted);">
                        Không tìm thấy dòng dữ liệu nào khớp với bộ lọc hiện tại.
                    </td>
                </tr>
            `;
            return;
        }

        let html = "";
        filtered.forEach(row => {
            const rowClass = row.isValid ? "" : "row-error";
            const badgeHtml = row.isValid
                ? `<span class="row-status-badge valid"><i class="fa-solid fa-circle-check"></i> Hợp lệ</span>`
                : `<span class="row-status-badge error"><i class="fa-solid fa-triangle-exclamation"></i> Có lỗi</span>`;

            const errorsHtml = row.errors.length > 0
                ? `<div class="row-error-list">${row.errors.map(err => `<div>&bull; ${escapeHtml(err)}</div>`).join("")}</div>`
                : `<span style="color: #16a34a; font-size: 11.5px;"><i class="fa-solid fa-check"></i> Đạt chuẩn</span>`;

            html += `
                <tr class="${rowClass}">
                    <td style="text-align: center; font-weight: 700; color: var(--crm-muted);">${row.rowNum}</td>
                    <td><strong>${escapeHtml(row.fullName || "—")}</strong></td>
                    <td>${escapeHtml(row.phone || "—")}</td>
                    <td>${escapeHtml(row.email || "—")}</td>
                    <td>${escapeHtml(row.companyName || "—")}</td>
                    <td><span class="row-status-badge valid" style="font-size:11px;">${escapeHtml(row.source || "—")}</span></td>
                    <td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${escapeHtml(row.notes)}">
                        ${escapeHtml(row.notes)}
                    </td>
                    <td>${badgeHtml}</td>
                    <td>${errorsHtml}</td>
                </tr>
            `;
        });

        dom.previewTableBody.innerHTML = html;
    }

    /**
     * Tiến hành Import Lead vào hệ thống (Step 2 -> Step 3)
     */
    async function executeImportProcess() {
        const onlyValid = dom.chkOnlyValid ? dom.chkOnlyValid.checked : true;
        const validRows = previewRows.filter(r => r.isValid);

        if (validRows.length === 0) {
            showToast("Không có dòng hợp lệ nào để tiến hành nhập dữ liệu.", "danger");
            return;
        }

        dom.btnExecuteImport.disabled = true;
        dom.btnExecuteImportText.innerHTML = '<span class="btn-spinner"></span> Đang nhập dữ liệu vào CRM...';

        const importedLeads = validRows.map(r => ({
            id: `LD-${Date.now().toString().slice(-4)}-${r.rowNum}`,
            fullName: r.fullName,
            phone: r.phone,
            email: r.email,
            companyName: r.companyName,
            source: r.source || "Nhập hàng loạt từ Excel",
            notes: r.notes !== "—" ? r.notes : "Nhập hàng loạt qua tệp Excel",
            status: "NEW",
            createdAt: new Date().toISOString()
        }));

        try {
            // Call REST API: POST /api/leads/import
            const formData = new FormData();
            if (selectedFile) formData.append("file", selectedFile);
            formData.append("leads", JSON.stringify(importedLeads));

            const resp = await fetch((window.CRM_BACKEND_BASE || "") + "/api/leads/import", {
                method: "POST",
                body: formData
            });

            if (resp.ok) {
                console.log("Backend import lead success");
            }
        } catch (err) {
            console.warn("Backend API /api/leads/import offline, lưu an toàn vào localStorage:", err);
        }

        // Save imported leads locally
        allLeads = [...importedLeads, ...allLeads];
        saveLeadsLocally();

        setTimeout(() => {
            dom.btnExecuteImport.disabled = false;
            dom.btnExecuteImportText.innerHTML = 'Tiến hành Import Lead vào hệ thống';

            const skippedCount = previewRows.length - validRows.length;
            if (dom.resSuccessCount) dom.resSuccessCount.textContent = validRows.length;
            if (dom.resSkippedCount) dom.resSkippedCount.textContent = skippedCount;

            setStepperStep(3);
            showToast(`✓ Đã nhập thành công ${validRows.length} khách hàng tiềm năng vào CRM!`, "success");
        }, 600);
    }

    /* ===================================================================
       6. TAB 3: COMBINED LEADS LIST TABLE & CONVERT TO CUSTOMER
       =================================================================== */

    function bindListEvents() {
        if (dom.listSearchInput) dom.listSearchInput.addEventListener("input", debounce(renderLeadsListTable, 200));
        if (dom.listFilterSource) dom.listFilterSource.addEventListener("change", renderLeadsListTable);
        if (dom.listFilterStatus) dom.listFilterStatus.addEventListener("change", renderLeadsListTable);

        if (dom.btnResetListFilters) {
            dom.btnResetListFilters.addEventListener("click", () => {
                if (dom.listSearchInput) dom.listSearchInput.value = "";
                if (dom.listFilterSource) dom.listFilterSource.value = "ALL";
                if (dom.listFilterStatus) dom.listFilterStatus.value = "ALL";
                renderLeadsListTable();
                showToast("Đã đặt lại bộ lọc danh sách Lead", "info");
            });
        }
    }

    function renderLeadsListTable() {
        if (!dom.listTableBody) return;

        const query = dom.listSearchInput ? dom.listSearchInput.value.trim().toLowerCase() : "";
        const filterSource = dom.listFilterSource ? dom.listFilterSource.value : "ALL";
        const filterStatus = dom.listFilterStatus ? dom.listFilterStatus.value : "ALL";

        const filtered = allLeads.filter(lead => {
            if (filterSource !== "ALL" && !lead.source.includes(filterSource)) return false;
            if (filterStatus !== "ALL" && lead.status !== filterStatus) return false;

            if (query) {
                const matchName = (lead.fullName || "").toLowerCase().includes(query);
                const matchPhone = (lead.phone || "").toLowerCase().includes(query);
                const matchEmail = (lead.email || "").toLowerCase().includes(query);
                const matchCompany = (lead.companyName || "").toLowerCase().includes(query);
                if (!matchName && !matchPhone && !matchEmail && !matchCompany) return false;
            }

            return true;
        });

        if (filtered.length === 0) {
            dom.listTableBody.innerHTML = `
                <tr>
                    <td colspan="9" style="text-align: center; padding: 32px; color: var(--crm-muted);">
                        Chưa có dữ liệu Lead nào khớp với điều kiện tìm kiếm.
                    </td>
                </tr>
            `;
            return;
        }

        let html = "";
        filtered.forEach((lead, idx) => {
            const statusBadge = getStatusBadge(lead.status);
            const formattedDate = formatDate(lead.createdAt);

            html += `
                <tr>
                    <td style="text-align: center; color: var(--crm-muted); font-weight: 600;">${idx + 1}</td>
                    <td>
                        <strong style="color: var(--crm-text);">${escapeHtml(lead.fullName)}</strong>
                        <div style="font-size: 11px; color: var(--crm-muted);">Mã: ${escapeHtml(lead.id)}</div>
                    </td>
                    <td>
                        <div><a href="tel:${escapeHtml(lead.phone)}" style="color: var(--crm-primary); text-decoration: none;"><i class="fa-solid fa-phone"></i> ${escapeHtml(lead.phone)}</a></div>
                        <div style="font-size: 11.5px;"><a href="mailto:${escapeHtml(lead.email)}" style="color: var(--crm-muted); text-decoration: none;"><i class="fa-solid fa-envelope"></i> ${escapeHtml(lead.email)}</a></div>
                    </td>
                    <td><strong>${escapeHtml(lead.companyName || "Chưa cung cấp")}</strong></td>
                    <td>
                        <span class="row-status-badge valid" style="font-size: 11px;">
                            ${escapeHtml(lead.source || "Thủ công")}
                        </span>
                    </td>
                    <td style="max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${escapeHtml(lead.notes || "")}">
                        ${escapeHtml(lead.notes || "—")}
                    </td>
                    <td>
                        <span class="row-status-badge ${statusBadge.cls}">
                            <i class="fa-solid ${statusBadge.icon}"></i> ${statusBadge.label}
                        </span>
                    </td>
                    <td style="font-size: 12px; color: var(--crm-muted); white-space: nowrap;">${formattedDate}</td>
                    <td style="text-align: right;">
                        ${lead.status !== "CONVERTED" ? `
                            <button type="button" class="crm-btn crm-btn-secondary crm-btn-sm btn-convert-lead" data-id="${escapeHtml(lead.id)}" title="Chuyển thành Khách hàng CRM">
                                <i class="fa-solid fa-user-check text-success"></i> Chuyển KH
                            </button>
                        ` : `
                            <span style="font-size: 11.5px; color: #16a34a; font-weight: 600;"><i class="fa-solid fa-check-double"></i> Đã vào CRM</span>
                        `}
                    </td>
                </tr>
            `;
        });

        dom.listTableBody.innerHTML = html;

        // Bind Convert action buttons
        dom.listTableBody.querySelectorAll(".btn-convert-lead").forEach(btn => {
            btn.addEventListener("click", (e) => {
                const leadId = e.currentTarget.dataset.id;
                const targetLead = allLeads.find(l => l.id === leadId);
                if (targetLead) handleConvertLeadToCustomer(targetLead);
            });
        });
    }

    /**
     * Chuyển đổi Lead thành Khách hàng chính thức trong kho dữ liệu CRM
     */
    function handleConvertLeadToCustomer(lead) {
        if (!confirm(`Bạn có chắc chắn muốn chuyển đổi Lead "${lead.fullName}" (${lead.companyName || "Cá nhân"}) thành Khách hàng chính thức trong CRM?`)) {
            return;
        }

        lead.status = "CONVERTED";
        saveLeadsLocally();

        try {
            let customers = [];
            const rawCustomers = localStorage.getItem(STORAGE_KEY_CUSTOMERS);
            if (rawCustomers) customers = JSON.parse(rawCustomers);

            const newCustomer = {
                id: `CUST-${Date.now().toString().slice(-4)}`,
                code: `KH-${Date.now().toString().slice(-4)}`,
                name: lead.companyName || lead.fullName,
                contactPerson: lead.fullName,
                phone: lead.phone,
                email: lead.email,
                address: "Chưa cập nhật",
                taxCode: "",
                industry: "Công nghệ / Dịch vụ",
                source: lead.source || "Tạo thủ công (CRM-116)",
                status: "ACTIVE",
                notes: `Chuyển đổi từ Lead ngày ${new Date().toLocaleDateString("vi-VN")}. Nhu cầu: ${lead.notes || "Không"}`,
                createdAt: new Date().toISOString()
            };

            customers.unshift(newCustomer);
            localStorage.setItem(STORAGE_KEY_CUSTOMERS, JSON.stringify(customers));
        } catch (e) {
            console.warn("Lưu khách hàng CRM:", e);
        }

        renderLeadsListTable();
        showToast(`Đã chuyển đổi thành công Khách hàng "${lead.companyName || lead.fullName}"!`, "success");
    }

    /* ===================================================================
       7. UTILITIES
       =================================================================== */

    function getInitials(name) {
        if (!name) return "L";
        const parts = name.trim().split(/\s+/);
        if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
        return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    function getStatusBadge(status) {
        switch (status) {
            case "NEW": return { cls: "valid", label: "Mới", icon: "fa-circle-dot" };
            case "CONTACTED": return { cls: "warning", label: "Đã liên hệ", icon: "fa-phone" };
            case "CONVERTED": return { cls: "valid", label: "Đã chuyển đổi", icon: "fa-circle-check" };
            default: return { cls: "valid", label: "Mới", icon: "fa-circle-dot" };
        }
    }

    function formatDate(isoStr) {
        if (!isoStr) return "—";
        try {
            const d = new Date(isoStr);
            return d.toLocaleDateString("vi-VN", {
                day: "2-digit",
                month: "2-digit",
                year: "numeric"
            });
        } catch (_) {
            return isoStr;
        }
    }

    function debounce(fn, delay) {
        let timer = null;
        return function (...args) {
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(this, args), delay);
        };
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

    function showToast(message, type = "info") {
        if (!dom.toastContainer) return;

        const toast = document.createElement("div");
        toast.className = `crm-lead-toast ${type}`;

        let iconCls = "fa-circle-info";
        if (type === "success") iconCls = "fa-circle-check";
        if (type === "warning") iconCls = "fa-triangle-exclamation";
        if (type === "danger") iconCls = "fa-circle-xmark";

        toast.innerHTML = `
            <i class="fa-solid ${iconCls}"></i>
            <span>${escapeHtml(message)}</span>
        `;

        dom.toastContainer.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = "toastOut 0.3s forwards ease";
            setTimeout(() => toast.remove(), 300);
        }, 3500);
    }

    // Run on ready
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
