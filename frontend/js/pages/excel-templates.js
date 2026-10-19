/**
 * ===================================================================
 * TRUNG TÂM TỆP MẪU EXCEL & BÁO CÁO DOANH NGHIỆP (EXCEL TEMPLATES HUB)
 * ===================================================================
 */

(function () {
    "use strict";

    // Metadata & Mockup Data for Floating Preview
    const TEMPLATE_PREVIEWS = {
        "customers-import": {
            title: "Mẫu Nhập Khách Hàng (Customers)",
            subtitle: "Tệp nhập hàng loạt dữ liệu doanh nghiệp B2B với mã số thuế & phân ngành",
            type: "import",
            filename: "Mau_Nhap_Khach_Hang_NovaCRM.xlsx",
            downloadUrl: "api/templates/excel/customers-import",
            headers: ["name*", "taxCode", "status*", "email", "phone", "website", "address", "industryId", "companySizeId"],
            rows: [
                ["Công ty CP Công Nghệ ICTU", "0108998877", "ACTIVE", "contact@ictu.vn", "0243888999", "https://ictu.vn", "Q. Cầu Giấy, Hà Nội", "1", "3"],
                ["Tập đoàn Bán lẻ Á Châu", "0305667788", "PROSPECT", "info@retail-asia.com", "0283999888", "https://retail-asia.com", "Q. 1, TP. HCM", "2", "4"],
                ["Công ty TNHH Cơ Khí Hải Phòng", "0201223344", "ACTIVE", "office@haiphong-eng.vn", "02253888777", "https://haiphong-eng.vn", "Hồng Bàng, Hải Phòng", "3", "2"]
            ],
            rules: [
                "Cột <code>name</code> (Tên doanh nghiệp) và <code>status</code> (Trạng thái) là BẮT BUỘC.",
                "Trạng thái hợp lệ: <code>PROSPECT</code> (Tiềm năng), <code>ACTIVE</code> (Đang giao dịch), <code>INACTIVE</code> (Ngừng hoạt động).",
                "Mã số thuế <code>taxCode</code> phải gồm 10 hoặc 13 chữ số, hệ thống tự động kiểm tra trùng lặp.",
                "Định dạng tệp: <strong>Microsoft Excel (.xlsx)</strong>, mã hóa UTF-8 tiếng Việt có dấu."
            ]
        },
        "leads-import": {
            title: "Mẫu Nhập Lead Tiềm Năng (Leads)",
            subtitle: "Thu thập danh sách khách hàng tiềm năng từ Hội thảo, Marketing, Sự kiện",
            type: "import",
            filename: "Mau_Nhap_Lead_Tiem_Nang_NovaCRM.xlsx",
            downloadUrl: "api/templates/excel/leads-import",
            headers: ["fullName*", "companyName", "email", "phone*", "sourceCode*", "notes"],
            rows: [
                ["Nguyễn Văn An", "Tập đoàn Viễn Thông VTC", "an.nv@vtc.com.vn", "0912345678", "EVENT", "Gặp tại Triển lãm TechExpo 2026"],
                ["Trần Thị Mai", "Công ty CP Dược Phẩm Bắc Nam", "mai.tran@bacnampharma.vn", "0987654321", "FACEBOOK", "Quan tâm giải pháp CRM Doanh nghiệp"],
                ["Lê Hoàng Long", "Logistics Song Mã", "long.lh@songma-log.com", "0903112233", "WEBSITE", "Cần tư vấn gói Enterprise 50 Users"]
            ],
            rules: [
                "Cột <code>fullName</code> (Họ tên), <code>phone</code> (Số điện thoại) và <code>sourceCode</code> (Nguồn) là BẮT BUỘC.",
                "Nguồn hợp lệ: <code>WEBSITE</code>, <code>FACEBOOK</code>, <code>EVENT</code>, <code>REFERRAL</code>, <code>COLD_CALL</code>.",
                "Số điện thoại được tự động chuẩn hóa định dạng số Việt Nam (+84 hoặc 0x).",
                "Hệ thống sẽ chạy thuật toán so khớp (CRM-118) để cảnh báo Lead bị trùng số điện thoại hoặc email."
            ]
        },
        "users-import": {
            title: "Mẫu Nhập Người Dùng (Users)",
            subtitle: "Dùng cho Quản trị viên nhập danh sách tài khoản nhân viên vào hệ thống",
            type: "import",
            filename: "Mau_Nhap_Nguoi_Dung_NovaCRM.xlsx",
            downloadUrl: "api/templates/excel/users-import",
            headers: ["fullName*", "email*", "password*", "status*"],
            rows: [
                ["Đặng Minh Quân", "quan.dm@company.com", "NovaPass@2026", "ACTIVE"],
                ["Phạm Thùy Linh", "linh.pt@company.com", "NovaPass@2026", "ACTIVE"],
                ["Vũ Quốc Dũng", "dung.vq@company.com", "NovaPass@2026", "ACTIVE"]
            ],
            rules: [
                "Cả 4 cột <code>fullName</code>, <code>email</code>, <code>password</code>, <code>status</code> đều là BẮT BUỘC.",
                "Email phải là duy nhất chưa từng đăng ký trong hệ thống CRM.",
                "Mật khẩu ban đầu tối thiểu 8 ký tự, khuyến nghị bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.",
                "Trạng thái hợp lệ: <code>ACTIVE</code> (Kích hoạt), <code>INACTIVE</code> (Khóa tạm thời)."
            ]
        },
        "quotes-export": {
            title: "Mẫu Xuất Báo Giá (Quotations Report)",
            subtitle: "Báo cáo bảng kê danh sách báo giá thương mại, tổng hợp doanh thu và thuế VAT",
            type: "export",
            filename: "Mau_Xuat_Bao_Gia_NovaCRM.xlsx",
            downloadUrl: "api/templates/excel/quotes-export",
            headers: ["STT", "Mã Báo Giá", "Tên Báo Giá", "Khách Hàng", "Giá Chưa Thuế", "Thuế VAT (10%)", "Tổng Tiền (VNĐ)", "Trạng Thái"],
            rows: [
                ["1", "BG-2026-001", "Gói Bản Quyền Cloud CRM", "Công ty CP Công Nghệ ICTU", "120,000,000", "12,000,000", "132,000,000", "APPROVED"],
                ["2", "BG-2026-002", "Triển khai Server On-Premise", "Tập đoàn Bán lẻ Á Châu", "350,000,000", "35,000,000", "385,000,000", "DRAFT"],
                ["Tổng cộng", "-", "-", "-", "470,000,000", "47,000,000", "517,000,000", "-"]
            ],
            rules: [
                "Được định dạng chuyên nghiệp với dòng Header xanh Navy và chữ trắng đậm.",
                "Cột số tiền được định dạng kiểu số kế toán (Financial Currency Format #,##0 VNĐ).",
                "Có dòng SUM tự động bằng công thức Excel <code>=SUM(...)</code> ở chân bảng báo cáo."
            ]
        },
        "contracts-export": {
            title: "Mẫu Xuất Hợp Đồng (Contracts Report)",
            subtitle: "Báo cáo tổng hợp tình trạng thực hiện hợp đồng kinh tế và cam kết tài chính",
            type: "export",
            filename: "Mau_Xuat_Hop_Dong_NovaCRM.xlsx",
            downloadUrl: "api/templates/excel/contracts-export",
            headers: ["STT", "Số Hợp Đồng", "Tên Hợp Đồng", "Khách Hàng", "Giá Trị (VNĐ)", "Hiệu Lực Từ", "Đến Ngày", "Trạng Thái"],
            rows: [
                ["1", "HD-2026/01", "Hợp đồng Cung cấp Dịch vụ CRM", "Công ty CP Công Nghệ ICTU", "132,000,000", "01/01/2026", "31/12/2026", "ACTIVE"],
                ["2", "HD-2026/02", "Hợp đồng Bảo trì Phần mềm", "Tập đoàn Bán lẻ Á Châu", "45,000,000", "15/02/2026", "14/02/2027", "PENDING"],
                ["Tổng", "-", "-", "-", "177,000,000", "-", "-", "-"]
            ],
            rules: [
                "Phân loại tình trạng hiệu lực: <code>ACTIVE</code> (Đang hiệu lực), <code>EXPIRED</code> (Hết hạn), <code>PENDING</code> (Chờ ký).",
                "Định dạng ngày tháng chuẩn <code>dd/MM/yyyy</code> thân thiện với chuẩn kế toán Việt Nam.",
                "Tự động tính lũy kế tổng giá trị cam kết thương mại."
            ]
        },
        "customers-export": {
            title: "Mẫu Xuất Hồ Sơ Khách Hàng (Customers)",
            subtitle: "Danh bạ hồ sơ doanh nghiệp B2B, mã số thuế, quy mô nhân sự và doanh số đã ký",
            type: "export",
            filename: "Mau_Xuat_Khach_Hang_NovaCRM.xlsx",
            downloadUrl: "api/templates/excel/customers-export",
            headers: ["STT", "Mã KH", "Tên Doanh Nghiệp", "Mã Số Thuế", "Ngành Nghề", "Quy Mô", "Doanh Số Đã Ký (VNĐ)", "Trạng Thái"],
            rows: [
                ["1", "KH-00102", "Công ty CP Công Nghệ ICTU", "0108998877", "Công nghệ phần mềm", "50-100", "132,000,000", "ACTIVE"],
                ["2", "KH-00103", "Tập đoàn Bán lẻ Á Châu", "0305667788", "Bán lẻ & Chuỗi siêu thị", "500-1000", "385,000,000", "PROSPECT"]
            ],
            rules: [
                "Báo cáo tổng quan 360 độ về hệ sinh thái khách hàng doanh nghiệp.",
                "Phục vụ đối soát doanh thu, trích xuất dữ liệu cho ban giám đốc và cơ quan thuế."
            ]
        },
        "contacts-export": {
            title: "Mẫu Xuất Người Liên Hệ (Contacts)",
            subtitle: "Danh bạ lãnh đạo, chức danh và vai trò ra quyết định mua (CRM-62)",
            type: "export",
            filename: "Mau_Xuat_Nguoi_Lien_He_NovaCRM.xlsx",
            downloadUrl: "api/templates/excel/contacts-export",
            headers: ["STT", "Họ và Tên", "Doanh Nghiệp", "Chức Vụ", "Vai Trò Quyết Định", "Email", "Điện Thoại", "Đầu Mối Chính"],
            rows: [
                ["1", "Nguyễn Văn Hùng", "Công ty CP Công Nghệ ICTU", "Giám đốc CNTT (CIO)", "Decision Maker", "hung.nv@ictu.vn", "0912113355", "CÓ"],
                ["2", "Lê Thu Hằng", "Tập đoàn Bán lẻ Á Châu", "Kế toán trưởng", "Financial Buyer", "hang.lt@retail-asia.com", "0988776655", "KHÔNG"]
            ],
            rules: [
                "Vai trò ra quyết định chuẩn B2B: Decision Maker, Champion, Influencer, End User, Financial Buyer.",
                "Hỗ trợ phân nhóm đầu mối liên hệ phục vụ chiến dịch Telesales và Email Marketing."
            ]
        }
    };

    // DOM Elements
    const tabButtons = document.querySelectorAll(".excel-tab-btn");
    const searchInput = document.getElementById("excelSearchInput");
    const cards = document.querySelectorAll(".excel-card");
    const previewModal = document.getElementById("excelPreviewModal");
    const modalOverlay = document.getElementById("excelModalOverlay");
    const modalCloseBtn = document.getElementById("excelModalClose");
    const modalCancelBtn = document.getElementById("excelModalCancel");
    const modalDownloadBtn = document.getElementById("excelModalDownload");

    let activeFilter = "all";

    // 1. Tab Filtering
    tabButtons.forEach(btn => {
        btn.addEventListener("click", () => {
            tabButtons.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            activeFilter = btn.dataset.filter || "all";
            applyFilterAndSearch();
        });
    });

    // 2. Real-time Search
    if (searchInput) {
        searchInput.addEventListener("input", () => {
            applyFilterAndSearch();
        });
    }

    function applyFilterAndSearch() {
        const query = (searchInput ? searchInput.value : "").trim().toLowerCase();

        cards.forEach(card => {
            const cardType = card.dataset.type; // 'import' or 'export'
            const cardText = card.textContent.toLowerCase();

            const matchType = (activeFilter === "all") || (cardType === activeFilter);
            const matchQuery = !query || cardText.includes(query);

            if (matchType && matchQuery) {
                card.style.display = "";
            } else {
                card.style.display = "none";
            }
        });
    }

    // 3. Floating Preview Modal
    function openPreview(templateKey) {
        const data = TEMPLATE_PREVIEWS[templateKey];
        if (!data) return;

        // Set title & subtitle
        document.getElementById("modalPreviewTitle").textContent = data.title;
        document.getElementById("modalPreviewSubtitle").textContent = data.subtitle;

        // Set icon & badge
        const badge = document.getElementById("modalPreviewBadge");
        if (badge) {
            if (data.type === "import") {
                badge.className = "excel-modal-icon-badge import";
                badge.innerHTML = '<i class="fa-solid fa-cloud-arrow-up" style="color:#60a5fa;"></i>';
            } else {
                badge.className = "excel-modal-icon-badge export";
                badge.innerHTML = '<i class="fa-solid fa-cloud-arrow-down" style="color:#34d399;"></i>';
            }
        }

        // Render Table Mockup
        const table = document.getElementById("modalPreviewTable");
        let tableHtml = "<thead><tr>";
        data.headers.forEach(h => {
            tableHtml += `<th>${h}</th>`;
        });
        tableHtml += "</tr></thead><tbody>";

        data.rows.forEach(row => {
            tableHtml += "<tr>";
            row.forEach(cell => {
                tableHtml += `<td>${cell}</td>`;
            });
            tableHtml += "</tr>";
        });
        tableHtml += "</tbody>";
        table.innerHTML = tableHtml;

        // Render Rules
        const rulesList = document.getElementById("modalPreviewRules");
        rulesList.innerHTML = data.rules.map(r => `<li>${r}</li>`).join("");

        // Set Download Button href
        if (modalDownloadBtn) {
            modalDownloadBtn.href = data.downloadUrl;
            modalDownloadBtn.setAttribute("download", data.filename);
        }

        // Show Modal
        if (modalOverlay) modalOverlay.classList.add("open");
        if (previewModal) previewModal.classList.add("open");
        document.body.classList.add("modal-open");
    }

    function closeModal() {
        if (modalOverlay) modalOverlay.classList.remove("open");
        if (previewModal) previewModal.classList.remove("open");
        document.body.classList.remove("modal-open");
    }

    // Attach click events for Preview buttons
    document.querySelectorAll("[data-preview]").forEach(btn => {
        btn.addEventListener("click", () => {
            const key = btn.dataset.preview;
            openPreview(key);
        });
    });

    if (modalCloseBtn) modalCloseBtn.addEventListener("click", closeModal);
    if (modalCancelBtn) modalCancelBtn.addEventListener("click", closeModal);
    if (modalOverlay) modalOverlay.addEventListener("click", closeModal);

    // Close on Escape key
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && previewModal && previewModal.classList.contains("open")) {
            closeModal();
        }
    });

    // Cleanup hook for SPA
    if (typeof window.crmAddPageCleanup === "function") {
        window.crmAddPageCleanup(closeModal);
    }

})();
