/**
 * ===================================================================
 * ENTERPRISE BULK CUSTOMER IMPORT ENGINE (CRM-66 / TASK S3-06)
 * Pure HTML5 / CSS3 / ES6+ JavaScript Client Module
 * - Tải tệp mẫu Excel: GET /api/customers/import/template
 * - Khu vực Upload Kéo thả Drag & Drop (.xlsx)
 * - Xem trước dữ liệu & Báo lỗi từng dòng: POST /api/customers/import/preview
 * - Phát hiện & Xử lý trùng lặp (Duplicate Detection & SKIP/UPDATE resolution)
 * - Xác nhận nhập liệu & Báo cáo kết quả: POST /api/customers/import/confirm
 * - Chuẩn hóa Fetch API với dữ liệu Backend đã kiểm tra quyền
 * ===================================================================
 */

"use strict";

(function () {
    const API_BASE = (window.CRM_BACKEND_BASE || `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`);

    let currentStep=1, selectedFile=null, previewData=null, currentFilter="ALL", searchQuery="", globalDuplicateMode="SKIP";
    function escapeHtml(str) {
        return String(str || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function formatFileSize(bytes) {
        if (!bytes || bytes === 0) return "0 B";
        const k = 1024;
        const sizes = ["B", "KB", "MB", "GB"];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + " " + sizes[i];
    }

    function showToast(message, type = "info") {
        if (typeof window.showToast === "function") {
            window.showToast(message, type);
            return;
        }
        // Fallback UI toast
        const existing = document.getElementById("crmGlobalToast");
        if (existing) existing.remove();

        const toast = document.createElement("div");
        toast.id = "crmGlobalToast";
        toast.style.cssText = `
            position: fixed;
            bottom: 24px;
            right: 24px;
            background: ${type === "danger" ? "#dc2626" : type === "success" ? "#16a34a" : type === "warning" ? "#d97706" : "#2563eb"};
            color: #ffffff;
            padding: 12px 20px;
            border-radius: 8px;
            box-shadow: 0 10px 25px rgba(0,0,0,0.2);
            font-size: 13.5px;
            font-weight: 600;
            z-index: 100000;
            display: flex;
            align-items: center;
            gap: 8px;
            animation: crmFadeSlideUp 200ms ease;
        `;
        toast.innerHTML = `<span>${type === "success" ? "✓" : type === "danger" ? "✕" : "ℹ"}</span> <span>${escapeHtml(message)}</span>`;
        document.body.appendChild(toast);
        setTimeout(() => {
            toast.style.opacity = "0";
            toast.style.transition = "opacity 300ms ease";
            setTimeout(() => toast.remove(), 300);
        }, 3500);
    }



    /* =========================================================
       2. CLIENT-SIDE FALLBACK PARSER & VALIDATOR
    ========================================================= */


    /* =========================================================
       3. API CLIENT CALLS
    ========================================================= */
    /**
     * 1. GET /api/customers/import/template
     */
    async function apiDownloadTemplate() {
        try {
            const response=await fetch(`${CrmApi.base}/api/customers/import/template`,{credentials:"include"});
            if (!response.ok) { const error=await response.json(); throw new Error(error.message || `HTTP ${response.status}`); }
            const url=URL.createObjectURL(await response.blob());
            const link=document.createElement("a"); link.href=url; link.download="customer-import-template.xlsx"; link.click();
            setTimeout(()=>URL.revokeObjectURL(url),1000);
        } catch(error) { showToast(error.message,"danger"); }
    }

    /**
     * 2. POST /api/customers/import/preview (multipart/form-data)
     */
    async function apiPreviewFile(file) {
        previewData=null;
        if (!file || !/\.xlsx$/i.test(file.name)) throw new Error("Vui lòng chọn tệp Excel .xlsx.");
        const form=new FormData(); form.append("file",file);
        const data=await CrmApi.request("/api/customers/import/preview",{method:"POST",body:form});
        return {...data,fileName:file.name,rows:[...data.validRows,...data.errorRows].sort((a,b)=>a.row-b.row).map(row=>({...row,action:row.duplicate ? globalDuplicateMode : (row.valid ? "INSERT":"ERROR")}))};
    }

    /**
     * 3. POST /api/customers/import/confirm
     */
    async function apiExecuteImport(batchToken, duplicateMode) {
        if (!batchToken) throw new Error("Chưa có bản xem trước hợp lệ. Vui lòng tải lại tệp.");
        return CrmApi.request("/api/customers/import/confirm",{method:"POST",body:JSON.stringify({batchToken,duplicateMode})});
    }

    /* =========================================================
       4. UI RENDERING & STEPPER CONTROLLER
    ========================================================= */
    function setStep(step) {
        currentStep = step;
        const modal = document.getElementById("customerImportModal");
        if (!modal) return;

        // Update Stepper Bar
        modal.querySelectorAll(".import-step-item").forEach(item => {
            const s = Number(item.dataset.step);
            item.classList.remove("active", "completed");
            if (s === step) item.classList.add("active");
            else if (s < step) item.classList.add("completed");
        });

        modal.querySelectorAll(".import-step-divider").forEach((div, idx) => {
            div.classList.toggle("active", idx + 1 < step);
        });

        // Update Panes
        modal.querySelectorAll(".import-step-pane").forEach(pane => {
            pane.classList.remove("active");
        });
        const activePane = modal.querySelector(`.import-step-pane[data-step="${step}"]`);
        if (activePane) activePane.classList.add("active");

        // Update Footer Buttons
        const btnBack = modal.querySelector("#btnImportBack");
        const btnNext = modal.querySelector("#btnImportNext");
        const btnExecute = modal.querySelector("#btnImportExecute");
        const btnFinish = modal.querySelector("#btnImportFinish");

        if (btnBack) btnBack.style.display = (step === 2) ? "inline-flex" : "none";
        if (btnNext) btnNext.style.display = (step === 1 && selectedFile) ? "inline-flex" : "none";
        if (btnExecute) btnExecute.style.display = (step === 2) ? "inline-flex" : "none";
        if (btnFinish) btnFinish.style.display = (step === 3) ? "inline-flex" : "none";

        // Step 2 entry setup
        if (step === 2 && previewData) {
            renderPreviewGrid();
        }
    }

    function renderPreviewGrid() {
        if (!previewData) return;
        const modal = document.getElementById("customerImportModal");
        if (!modal) return;

        // 1. KPI Stats
        modal.querySelector("#previewStatTotal").textContent = previewData.totalRows;
        modal.querySelector("#previewStatValid").textContent = previewData.validCount;
        modal.querySelector("#previewStatError").textContent = previewData.errorCount;
        modal.querySelector("#previewStatDuplicate").textContent = previewData.duplicateCount;

        // 2. Filter Rows
        let filtered = previewData.rows || [];
        if (currentFilter === "VALID") {
            filtered = filtered.filter(r => r.valid && !r.duplicate);
        } else if (currentFilter === "ERROR") {
            filtered = filtered.filter(r => !r.valid);
        } else if (currentFilter === "DUPLICATE") {
            filtered = filtered.filter(r => r.duplicate);
        }

        if (searchQuery) {
            const q = searchQuery.toLowerCase();
            filtered = filtered.filter(r =>
                (r.name && r.name.toLowerCase().includes(q)) ||
                (r.taxCode && r.taxCode.toLowerCase().includes(q)) ||
                (r.email && r.email.toLowerCase().includes(q))
            );
        }

        // 3. Render Table Rows
        const tbody = modal.querySelector("#importPreviewTbody");
        if (!tbody) return;

        if (filtered.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align:center; padding:32px; color:var(--crm-muted);">
                        Không có dữ liệu phù hợp với bộ lọc hiện tại.
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = filtered.map(r => {
            const isError = !r.valid;
            const isDup = !!r.duplicate;
            const rowClass = isError ? "row-error" : (isDup ? "row-duplicate" : "");

            // Name cell error
            const hasNameError = isError && (!r.name || !r.name.trim() || r.name.length > 150);
            const nameTooltip = hasNameError ? (r.errors.find(e => e.includes("Tên")) || "Lỗi tên khách hàng") : "";

            // Tax cell error
            const hasTaxError = isError && r.errors.some(e => e.includes("Mã số thuế"));
            const taxTooltip = hasTaxError ? r.errors.find(e => e.includes("Mã số thuế")) : "";

            // Email cell error
            const hasEmailError = isError && r.errors.some(e => e.includes("Email"));
            const emailTooltip = hasEmailError ? r.errors.find(e => e.includes("Email")) : "";

            // Status Badge
            let statusBadge = `<span class="import-badge import-badge-valid">✓ Hợp lệ</span>`;
            if (isError) {
                statusBadge = `<span class="import-badge import-badge-error">✕ ${r.errors.length} Lỗi</span>`;
            } else if (isDup) {
                const srcLabel = r.duplicateSource === "DATABASE" ? "Trùng DB" : "Trùng tệp";
                statusBadge = `<span class="import-badge import-badge-duplicate">⚠️ ${srcLabel}</span>`;
            }

            // Conflict Action Dropdown
            let conflictCell = `<span style="color:var(--crm-muted); font-size:12px;">Thêm mới</span>`;
            if (isError) {
                conflictCell = `<span style="color:var(--crm-danger); font-size:12px; font-weight:600;">Không nhập (Lỗi)</span>`;
            } else if (isDup) {
                const currentAction = r.action || globalDuplicateMode;
                conflictCell = `<span>${globalDuplicateMode === "UPDATE" ? "Cập nhật" : "Bỏ qua"}</span>`;
            }

            return `
                <tr class="${rowClass}">
                    <td style="font-weight:600; color:var(--crm-muted);">${r.row}</td>
                    <td class="${hasNameError ? 'cell-error' : ''}">
                        <div class="crm-error-cell-wrap">
                            <span>${escapeHtml(r.name || '— Trống —')}</span>
                            ${hasNameError ? `<span class="crm-cell-warn-icon">⚠️<span class="crm-cell-tooltip">${escapeHtml(nameTooltip)}</span></span>` : ''}
                        </div>
                    </td>
                    <td class="${hasTaxError ? 'cell-error' : ''}">
                        <div class="crm-error-cell-wrap">
                            <code>${escapeHtml(r.taxCode || '—')}</code>
                            ${hasTaxError ? `<span class="crm-cell-warn-icon">⚠️<span class="crm-cell-tooltip">${escapeHtml(taxTooltip)}</span></span>` : ''}
                        </div>
                    </td>
                    <td><span class="tax-badge">${escapeHtml(r.status || 'TIEM_NANG')}</span></td>
                    <td class="${hasEmailError ? 'cell-error' : ''}">
                        <div class="crm-error-cell-wrap">
                            <span>${escapeHtml(r.email || '—')}</span>
                            ${hasEmailError ? `<span class="crm-cell-warn-icon">⚠️<span class="crm-cell-tooltip">${escapeHtml(emailTooltip)}</span></span>` : ''}
                        </div>
                    </td>
                    <td>${escapeHtml(r.phone || '—')}</td>
                    <td>${statusBadge}</td>
                    <td>${conflictCell}</td>
                </tr>
            `;
        }).join("");

        // Attach change listeners for row conflict selectors
        tbody.querySelectorAll(".crm-row-conflict-select").forEach(sel => {
            sel.addEventListener("change", e => {
                const rowNum = Number(e.target.dataset.row);
                const targetRow = previewData.rows.find(r => r.row === rowNum);
                if (targetRow) {
                    targetRow.action = e.target.value;
                    e.target.classList.toggle("select-update", e.target.value === "UPDATE");
                }
            });
        });

        // Update execute button label
        const btnExecute = modal.querySelector("#btnImportExecute");
        if (btnExecute) {
            const executableCount = previewData.rows.filter(r => r.valid).length;
            btnExecute.innerHTML = `
                <svg class="crm-inline-icon" viewBox="0 0 24 24"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
                Tiến hành nhập dữ liệu (${executableCount} dòng hợp lệ)
            `;
            btnExecute.disabled = executableCount === 0;
        }
    }

    async function runImportExecution() {
        if(!previewData)return;
        setStep(3);
        const finish=document.getElementById("btnImportFinish");if(finish){finish.disabled=true;finish.style.display="none";}
        const modal = document.getElementById("customerImportModal");
        if (!modal) return;

        const progressWrapper = modal.querySelector("#importProgressWrapper");
        const progressBar = modal.querySelector("#importProgressBar");
        const progressStatus = modal.querySelector("#importProgressStatus");
        const reportCard = modal.querySelector("#importReportCard");

        if (progressWrapper) progressWrapper.style.display = "flex";
        if (reportCard) reportCard.style.display = "none";

        if (progressStatus) progressStatus.textContent="Đang nhập dữ liệu...";
        try {
            const report = await apiExecuteImport(previewData?.batchToken, globalDuplicateMode);
            previewData=null;
            window.dispatchEvent(new CustomEvent("crm:customers-imported"));

            if (progressBar) progressBar.style.width = "100%";
            if (progressStatus) progressStatus.textContent = "Hoàn tất xử lý nhập dữ liệu!";

            if (progressWrapper) progressWrapper.style.display = "none";
            if (reportCard) {
                reportCard.style.display = "block";
                renderSummaryReport(report);
                if(finish){finish.disabled=false;finish.style.display="inline-flex";}
            }
        } catch (err) {
            if (progressStatus) progressStatus.textContent = "Lỗi khi nhập: " + err.message;
            showToast(err.message || "Lỗi xử lý import", "danger");
        }
    }

    function renderSummaryReport(report) {
        const modal = document.getElementById("customerImportModal");
        if (!modal || !report) return;

        modal.querySelector("#reportTotalCount").textContent = report.totalRows || 0;
        modal.querySelector("#reportCreatedCount").textContent = report.created || 0;
        modal.querySelector("#reportUpdatedCount").textContent = report.updated || 0;
        modal.querySelector("#reportSkippedCount").textContent = report.skipped || 0;
        modal.querySelector("#reportErrorCount").textContent = report.errorCount || 0;

        const errorSection = modal.querySelector("#reportErrorSection");
        const errorTbody = modal.querySelector("#reportErrorTbody");

        if (report.errorCount > 0 && Array.isArray(report.errors) && report.errors.length > 0) {
            if (errorSection) errorSection.style.display = "block";
            if (errorTbody) {
                errorTbody.innerHTML = report.errors.map(err => `
                    <tr>
                        <td style="font-weight:700;">Dòng ${err.row}</td>
                        <td>${escapeHtml(err.name || '—')}</td>
                        <td><code>${escapeHtml(err.taxCode || '—')}</code></td>
                        <td style="color:var(--crm-danger); font-size:12.5px;">${escapeHtml(Array.isArray(err.errors) ? err.errors.join("; ") : err.errors)}</td>
                    </tr>
                `).join("");
            }

            // Export error CSV button
            const btnDownloadErrorLog = modal.querySelector("#btnDownloadErrorLog");
            if (btnDownloadErrorLog) {
                btnDownloadErrorLog.onclick = () => {
                    const csvRows = [
                        ["Dong", "Ten_Khach_Hang", "Ma_So_Thue", "Nguyen_Nhan_Loi"].join(",")
                    ];
                    report.errors.forEach(e => {
                        const errText = Array.isArray(e.errors) ? e.errors.join("; ") : e.errors;
                        csvRows.push([
                            e.row,
                            `"${(e.name || '').replace(/"/g, '""')}"`,
                            `"${(e.taxCode || '').replace(/"/g, '""')}"`,
                            `"${errText.replace(/"/g, '""')}"`
                        ].join(","));
                    });
                    const blob = new Blob(["\uFEFF" + csvRows.join("\n")], { type: "text/csv;charset=utf-8;" });
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement("a");
                    a.href = url;
                    a.download = `danh_sach_dong_loi_import_${Date.now()}.csv`;
                    document.body.appendChild(a);
                    a.click();
                    a.remove();
                    window.URL.revokeObjectURL(url);
                };
            }
        } else {
            if (errorSection) errorSection.style.display = "none";
        }
    }

    /* =========================================================
       5. MODAL LIFECYCLE & EVENT WIRING
    ========================================================= */
    function openModal() {
        clearSelectedFile();
        const overlay = document.getElementById("customerImportModalOverlay");
        if (!overlay) return;

        overlay.classList.add("active");
        setStep(1);


    }

    function closeModal() {
        const overlay = document.getElementById("customerImportModalOverlay");
        if (overlay) overlay.classList.remove("active");
    }

    function showSelectedFileInfo(file) {
        const modal = document.getElementById("customerImportModal");
        if (!modal || !file) return;

        selectedFile = file;
        const card = modal.querySelector("#importFileCard");
        if (card) {
            card.classList.add("active");
            card.querySelector("#importFileName").textContent = file.name;
            card.querySelector("#importFileSize").textContent = formatFileSize(file.size);
        }

        const btnNext = modal.querySelector("#btnImportNext");
        if (btnNext) btnNext.style.display = "inline-flex";
    }

    function clearSelectedFile() {
        selectedFile = null;
        previewData = null;

        const modal = document.getElementById("customerImportModal");
        if (!modal) return;

        const card = modal.querySelector("#importFileCard");
        if (card) card.classList.remove("active");

        const fileInput = modal.querySelector("#importFileInput");
        if (fileInput) fileInput.value = "";

        const btnNext = modal.querySelector("#btnImportNext");
        if (btnNext) btnNext.style.display = "none";
    }

    function initEventListeners() {
        const modal = document.getElementById("customerImportModal");
        const overlay = document.getElementById("customerImportModalOverlay");
        if (!modal || !overlay) return;

        // Close triggers
        modal.querySelectorAll(".crm-import-close-trigger").forEach(btn => {
            btn.addEventListener("click", closeModal);
        });

        overlay.addEventListener("click", e => {
            if (e.target === overlay) closeModal();
        });

        document.addEventListener("keydown", e => {
            if (e.key === "Escape" && overlay.classList.contains("active")) {
                closeModal();
            }
        });

        // Template Download
        modal.querySelector("#btnDownloadTemplate")?.addEventListener("click", apiDownloadTemplate);

        // File Input & Dropzone
        const dropZone = modal.querySelector("#importDropZone");
        const fileInput = modal.querySelector("#importFileInput");

        if (dropZone && fileInput) {
            dropZone.addEventListener("click", () => fileInput.click());

            dropZone.addEventListener("dragover", e => {
                e.preventDefault();
                dropZone.classList.add("dragover");
            });

            dropZone.addEventListener("dragleave", () => {
                dropZone.classList.remove("dragover");
            });

            dropZone.addEventListener("drop", async e => {
                e.preventDefault();
                dropZone.classList.remove("dragover");
                if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                    const f = e.dataTransfer.files[0];
                    showSelectedFileInfo(f);
                    try {
                        const data=await apiPreviewFile(f);
                        if(selectedFile===f)previewData=data;
                    } catch (err) {
                        showToast(err.message, "danger");
                    }
                }
            });

            fileInput.addEventListener("change", async e => {
                if (e.target.files && e.target.files[0]) {
                    const f = e.target.files[0];
                    showSelectedFileInfo(f);
                    try {
                        const data=await apiPreviewFile(f);
                        if(selectedFile===f)previewData=data;
                    } catch (err) {
                        showToast(err.message, "danger");
                    }
                }
            });
        }

        // File remove button
        modal.querySelector("#btnRemoveFile")?.addEventListener("click", e => {
            e.stopPropagation();
            clearSelectedFile();
        });

        for (const id of ["btnLoadValidDemo","btnLoadMixedDemo"]) modal.querySelector("#"+id)?.remove();
        const input=modal.querySelector("#importFileInput"); if (input) input.accept=".xlsx";
        // Stepper Navigation Buttons
        modal.querySelector("#btnImportNext")?.addEventListener("click", async () => {
            if (!previewData && selectedFile) {
                try {
                    previewData = await apiPreviewFile(selectedFile);
                } catch (err) {
                    showToast(err.message, "danger");
                    return;
                }
            }
            if (previewData) {
                setStep(2);
            }
        });

        modal.querySelector("#btnImportBack")?.addEventListener("click", () => {
            setStep(1);
        });

        modal.querySelector("#btnImportExecute")?.addEventListener("click", () => {
            runImportExecution();
        });

        modal.querySelector("#btnImportFinish")?.addEventListener("click", () => {
            closeModal();
            if (!document.getElementById("btnImportFinish").disabled) showToast("Nhập dữ liệu khách hàng hoàn tất!", "success");
        });

        // Filter Pills in Step 2
        modal.querySelectorAll(".crm-pill-btn").forEach(btn => {
            btn.addEventListener("click", () => {
                modal.querySelectorAll(".crm-pill-btn").forEach(b => b.classList.remove("active"));
                btn.classList.add("active");
                currentFilter = btn.dataset.filter || "ALL";
                renderPreviewGrid();
            });
        });

        // KPI Cards in Step 2
        modal.querySelectorAll(".crm-import-kpi-card").forEach(card => {
            card.addEventListener("click", () => {
                const f = card.dataset.filter;
                if (!f) return;
                currentFilter = f;
                modal.querySelectorAll(".crm-pill-btn").forEach(b => {
                    b.classList.toggle("active", b.dataset.filter === f);
                });
                renderPreviewGrid();
            });
        });

        // Search Input in Step 2
        modal.querySelector("#importSearchInput")?.addEventListener("input", e => {
            searchQuery = e.target.value.trim();
            renderPreviewGrid();
        });

        // Global Conflict Resolution Dropdown in Step 2
        modal.querySelector("#globalConflictSelect")?.addEventListener("change", e => {
            globalDuplicateMode = e.target.value;
            if (previewData && Array.isArray(previewData.rows)) {
                previewData.rows.forEach(r => {
                    if (r.duplicate) {
                        r.action = globalDuplicateMode;
                    }
                });
                renderPreviewGrid();
            }
        });
    }

    function init() {
        // Bind to trigger button on Customers page
        const btnImportExcel = document.getElementById("btnImportExcel");
        if (btnImportExcel) {
            btnImportExcel.addEventListener("click", e => {
                e.preventDefault();
                openModal();
            });
        }

        initEventListeners();
    }

    // Export Global API
    window.CustomerImportEngine = {
        open: openModal,
        close: closeModal,
        setStep,
        apiDownloadTemplate,
        apiPreviewFile,
        apiExecuteImport
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
