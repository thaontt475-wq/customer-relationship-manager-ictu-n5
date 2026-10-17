/**
 * ExcelExportHelper - Thư viện xuất tệp Excel Doanh nghiệp Cao cấp (Corporate Enterprise Excel)
 * Tạo tệp bảng tính Microsoft Excel chuẩn XML SpreadsheetML với đầy đủ màu sắc, border, font chữ,
 * định dạng tiền tệ và độ rộng cột tự động mà không bị vỡ font UTF-8 tiếng Việt.
 */
(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        define([], factory);
    } else if (typeof module === 'object' && module.exports) {
        module.exports = factory();
    } else {
        root.ExcelExportHelper = factory();
    }
}(typeof self !== 'undefined' ? self : this, function () {

    /**
     * Escape ký tự đặc biệt cho XML
     */
    function escapeXml(unsafe) {
        if (unsafe === null || unsafe === undefined) return "";
        return String(unsafe)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&apos;');
    }

    /**
     * Tự động tải file về máy người dùng
     */
    function triggerDownload(content, filename, mimeType) {
        const blob = new Blob([content], { type: mimeType || 'application/vnd.ms-excel;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(() => URL.revokeObjectURL(url), 1500);
    }

    /**
     * Xuất Bảng tính Excel có định dạng chuyên nghiệp
     * @param {Object} options
     * - title: Tiêu đề báo cáo
     * - filename: Tên file khi tải về (kèm đuôi .xls)
     * - meta: Dòng thông tin phụ (Ngày xuất, người xuất)
     * - columns: Mảng cột [{ label, key, width, align, isMoney }]
     * - data: Mảng dữ liệu các dòng
     * - summary: Cấu hình dòng tổng cộng (tùy chọn)
     */
    function exportStyledExcel(options) {
        const title = options.title || "BÁO CÁO DỮ LIỆU NOVACRM";
        const filename = options.filename || "Bao_Cao_NovaCRM.xls";
        const meta = options.meta || `Xuất ngày: ${new Date().toLocaleDateString('vi-VN')} | Hệ thống NovaCRM Enterprise`;
        const columns = options.columns || [];
        const data = options.data || [];
        const summary = options.summary || null;

        const xmlHeader = `<?xml version="1.0" encoding="UTF-8"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <DocumentProperties xmlns="urn:schemas-microsoft-com:office:office">
  <Author>NovaCRM Enterprise</Author>
  <Created>${new Date().toISOString()}</Created>
 </DocumentProperties>
 <Styles>
  <Style ss:ID="Default" ss:Name="Normal">
   <Alignment ss:Vertical="Center"/>
   <Borders/>
   <Font ss:FontName="Segoe UI" ss:Size="10" ss:Color="#0F172A"/>
   <Interior/>
   <NumberFormat/>
   <Protection/>
  </Style>
  <!-- Title Style -->
  <Style ss:ID="sTitle">
   <Alignment ss:Horizontal="Left" ss:Vertical="Center"/>
   <Font ss:FontName="Segoe UI" ss:Size="14" ss:Bold="1" ss:Color="#1E3A8A"/>
  </Style>
  <!-- Subtitle Meta -->
  <Style ss:ID="sMeta">
   <Alignment ss:Horizontal="Left" ss:Vertical="Center"/>
   <Font ss:FontName="Segoe UI" ss:Size="9.5" ss:Italic="1" ss:Color="#64748B"/>
  </Style>
  <!-- Column Header Style -->
  <Style ss:ID="sColHeader">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center" ss:WrapText="1"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
   </Borders>
   <Font ss:FontName="Segoe UI" ss:Size="10" ss:Bold="1" ss:Color="#FFFFFF"/>
   <Interior ss:Color="#1E3A8A" ss:Pattern="Solid"/>
  </Style>
  <!-- Data Cells Left -->
  <Style ss:ID="sDataLeft">
   <Alignment ss:Horizontal="Left" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Segoe UI" ss:Size="10"/>
  </Style>
  <!-- Data Cells Center -->
  <Style ss:ID="sDataCenter">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Segoe UI" ss:Size="10"/>
  </Style>
  <!-- Data Cells Money Right -->
  <Style ss:ID="sDataMoney">
   <Alignment ss:Horizontal="Right" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Segoe UI" ss:Size="10"/>
   <NumberFormat ss:Format="#,##0\ &quot;₫&quot;"/>
  </Style>
  <!-- Zebra Striping -->
  <Style ss:ID="sZebraLeft">
   <Alignment ss:Horizontal="Left" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Segoe UI" ss:Size="10"/>
   <Interior ss:Color="#F8FAFC" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="sZebraCenter">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Segoe UI" ss:Size="10"/>
   <Interior ss:Color="#F8FAFC" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="sZebraMoney">
   <Alignment ss:Horizontal="Right" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Segoe UI" ss:Size="10"/>
   <Interior ss:Color="#F8FAFC" ss:Pattern="Solid"/>
   <NumberFormat ss:Format="#,##0\ &quot;₫&quot;"/>
  </Style>
  <!-- Summary Row Style -->
  <Style ss:ID="sTotalRow">
   <Alignment ss:Horizontal="Right" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Double" ss:Weight="3" ss:Color="#4F46E5"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#4F46E5"/>
   </Borders>
   <Font ss:FontName="Segoe UI" ss:Size="10.5" ss:Bold="1" ss:Color="#1E3A8A"/>
   <Interior ss:Color="#EEF2FF" ss:Pattern="Solid"/>
   <NumberFormat ss:Format="#,##0\ &quot;₫&quot;"/>
  </Style>
  <Style ss:ID="sTotalLabel">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Double" ss:Weight="3" ss:Color="#4F46E5"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#4F46E5"/>
   </Borders>
   <Font ss:FontName="Segoe UI" ss:Size="10.5" ss:Bold="1" ss:Color="#1E3A8A"/>
   <Interior ss:Color="#EEF2FF" ss:Pattern="Solid"/>
  </Style>
 </Styles>
 <Worksheet ss:Name="Báo Cáo">
  <Table ss:DefaultRowHeight="20">
`;

        // Column widths
        let colsXml = "";
        columns.forEach(col => {
            const w = col.width || 120;
            colsXml += `   <Column ss:Width="${w}"/>\n`;
        });

        // Rows construction
        let rowsXml = "";

        // Row 1: Title
        const colSpan = Math.max(columns.length - 1, 1);
        rowsXml += `   <Row ss:Height="28">\n`;
        rowsXml += `    <Cell ss:MergeAcross="${colSpan}" ss:StyleID="sTitle"><Data ss:Type="String">${escapeXml(title)}</Data></Cell>\n`;
        rowsXml += `   </Row>\n`;

        // Row 2: Subtitle
        rowsXml += `   <Row ss:Height="18">\n`;
        rowsXml += `    <Cell ss:MergeAcross="${colSpan}" ss:StyleID="sMeta"><Data ss:Type="String">${escapeXml(meta)}</Data></Cell>\n`;
        rowsXml += `   </Row>\n`;

        // Row 3: Spacer
        rowsXml += `   <Row ss:Height="8"/>\n`;

        // Row 4: Column Headers
        rowsXml += `   <Row ss:Height="24">\n`;
        columns.forEach(col => {
            rowsXml += `    <Cell ss:StyleID="sColHeader"><Data ss:Type="String">${escapeXml(col.label)}</Data></Cell>\n`;
        });
        rowsXml += `   </Row>\n`;

        // Data Rows
        data.forEach((item, rIdx) => {
            const isZebra = (rIdx % 2 === 1);
            rowsXml += `   <Row ss:Height="21">\n`;
            columns.forEach(col => {
                let val = item[col.key];
                if (val === undefined || val === null) val = "";

                if (col.isMoney) {
                    const num = typeof val === 'number' ? val : parseFloat(String(val).replace(/[^0-9.-]/g, '')) || 0;
                    const styleId = isZebra ? "sZebraMoney" : "sDataMoney";
                    rowsXml += `    <Cell ss:StyleID="${styleId}"><Data ss:Type="Number">${num}</Data></Cell>\n`;
                } else if (col.align === 'center') {
                    const styleId = isZebra ? "sZebraCenter" : "sDataCenter";
                    rowsXml += `    <Cell ss:StyleID="${styleId}"><Data ss:Type="String">${escapeXml(val)}</Data></Cell>\n`;
                } else {
                    const styleId = isZebra ? "sZebraLeft" : "sDataLeft";
                    rowsXml += `    <Cell ss:StyleID="${styleId}"><Data ss:Type="String">${escapeXml(val)}</Data></Cell>\n`;
                }
            });
            rowsXml += `   </Row>\n`;
        });

        // Summary Row (nếu có)
        if (summary) {
            rowsXml += `   <Row ss:Height="24">\n`;
            columns.forEach((col, cIdx) => {
                if (cIdx === 0) {
                    const merge = summary.mergeCols ? summary.mergeCols - 1 : 0;
                    rowsXml += `    <Cell ss:MergeAcross="${merge}" ss:StyleID="sTotalLabel"><Data ss:Type="String">${escapeXml(summary.label || "TỔNG CỘNG:")}</Data></Cell>\n`;
                } else if (summary.mergeCols && cIdx < summary.mergeCols) {
                    // Merged cell, skip
                } else if (summary.values && summary.values[col.key] !== undefined) {
                    const sumVal = summary.values[col.key];
                    rowsXml += `    <Cell ss:StyleID="sTotalRow"><Data ss:Type="Number">${sumVal}</Data></Cell>\n`;
                } else {
                    rowsXml += `    <Cell ss:StyleID="sTotalRow"><Data ss:Type="String"></Data></Cell>\n`;
                }
            });
            rowsXml += `   </Row>\n`;
        }

        const xmlFooter = `  </Table>
  <WorksheetOptions xmlns="urn:schemas-microsoft-com:office:excel">
   <FreezePanes/>
   <FrozenNoSplit/>
   <SplitPane>
    <SplitRow>4</SplitRow>
    <LeftColumn>0</LeftColumn>
    <ActivePane>2</ActivePane>
   </SplitPane>
   <Panes>
    <Pane>
     <Number>2</Number>
    </Pane>
   </Panes>
   <ProtectObjects>False</ProtectObjects>
   <ProtectScenarios>False</ProtectScenarios>
  </WorksheetOptions>
 </Worksheet>
</Workbook>`;

        const fullXml = xmlHeader + colsXml + rowsXml + xmlFooter;
        triggerDownload(fullXml, filename);
    }

    /**
     * Mở trung tâm mẫu Excel để người dùng xem và tải
     */
    function showTemplateModal() {
        let modal = document.getElementById("excelTemplatesModal");
        if (!modal) {
            modal = document.createElement("div");
            modal.id = "excelTemplatesModal";
            modal.className = "crm-modal-backdrop";
            modal.style.cssText = "position:fixed;inset:0;background:rgba(15,23,42,0.7);backdrop-filter:blur(4px);display:flex;align-items:center;justify-content:center;z-index:9999;";
            modal.innerHTML = `
                <div class="crm-modal" style="background:var(--crm-surface,#ffffff);border:1px solid var(--crm-border,#e2e8f0);border-radius:16px;max-width:760px;width:92%;padding:28px;box-shadow:0 25px 50px -12px rgba(0,0,0,0.25);color:var(--crm-text,#0f172a);max-height:90vh;overflow-y:auto;">
                    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px;border-bottom:1px solid var(--crm-border,#e2e8f0);padding-bottom:14px;">
                        <div style="display:flex;align-items:center;gap:12px;">
                            <div style="width:40px;height:40px;border-radius:10px;background:#10b981;color:#fff;display:flex;align-items:center;justify-content:center;font-size:18px;">
                                <i class="fa-solid fa-file-excel"></i>
                            </div>
                            <div>
                                <h3 style="margin:0;font-size:18px;font-weight:700;">Trung Tâm Tệp Mẫu Excel Chuẩn NovaCRM</h3>
                                <p style="margin:2px 0 0;font-size:12.5px;color:var(--crm-muted,#64748b);">Tải về các tệp mẫu Excel .xlsx chuẩn Enterprise định dạng sẵn cho từng nghiệp vụ</p>
                            </div>
                        </div>
                        <button type="button" id="btnCloseExcelModal" style="background:none;border:none;font-size:20px;cursor:pointer;color:var(--crm-muted,#64748b);">&times;</button>
                    </div>

                    <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:16px;">
                        
                        <!-- NHÓM 1: MẪU NHẬP (IMPORT) -->
                        <div style="border:1px solid var(--crm-border,#e2e8f0);border-radius:12px;padding:16px;background:var(--crm-card-bg,rgba(255,255,255,0.02));">
                            <h4 style="margin:0 0 12px;font-size:14px;color:#2563eb;display:flex;align-items:center;gap:8px;">
                                <i class="fa-solid fa-cloud-arrow-up"></i> Tệp Mẫu Nhập Dữ Liệu (.xlsx)
                            </h4>
                            <div style="display:flex;flex-direction:column;gap:10px;">
                                <a href="assets/templates/excel/Mau_Nhap_Khach_Hang_NovaCRM.xlsx" download style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:8px;background:var(--crm-surface-hover,#f8fafc);border:1px solid var(--crm-border,#e2e8f0);text-decoration:none;color:inherit;font-size:13px;font-weight:500;">
                                    <span><i class="fa-solid fa-file-excel" style="color:#10b981;margin-right:8px;"></i> Mẫu Nhập Khách Hàng</span>
                                    <span style="color:#2563eb;font-size:12px;font-weight:600;"><i class="fa-solid fa-download"></i> Tải về</span>
                                </a>
                                <a href="assets/templates/excel/Mau_Nhap_Lead_Tiem_Nang_NovaCRM.xlsx" download style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:8px;background:var(--crm-surface-hover,#f8fafc);border:1px solid var(--crm-border,#e2e8f0);text-decoration:none;color:inherit;font-size:13px;font-weight:500;">
                                    <span><i class="fa-solid fa-file-excel" style="color:#10b981;margin-right:8px;"></i> Mẫu Nhập Lead Tiềm Năng</span>
                                    <span style="color:#2563eb;font-size:12px;font-weight:600;"><i class="fa-solid fa-download"></i> Tải về</span>
                                </a>
                                <a href="assets/templates/excel/Mau_Nhap_Nguoi_Dung_NovaCRM.xlsx" download style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:8px;background:var(--crm-surface-hover,#f8fafc);border:1px solid var(--crm-border,#e2e8f0);text-decoration:none;color:inherit;font-size:13px;font-weight:500;">
                                    <span><i class="fa-solid fa-file-excel" style="color:#10b981;margin-right:8px;"></i> Mẫu Nhập Người Dùng</span>
                                    <span style="color:#2563eb;font-size:12px;font-weight:600;"><i class="fa-solid fa-download"></i> Tải về</span>
                                </a>
                            </div>
                        </div>

                        <!-- NHÓM 2: MẪU XUẤT BÁO CÁO (EXPORT) -->
                        <div style="border:1px solid var(--crm-border,#e2e8f0);border-radius:12px;padding:16px;background:var(--crm-card-bg,rgba(255,255,255,0.02));">
                            <h4 style="margin:0 0 12px;font-size:14px;color:#10b981;display:flex;align-items:center;gap:8px;">
                                <i class="fa-solid fa-cloud-arrow-down"></i> Tệp Mẫu Xuất Báo Cáo (.xlsx)
                            </h4>
                            <div style="display:flex;flex-direction:column;gap:10px;">
                                <a href="assets/templates/excel/Mau_Xuat_Bao_Gia_NovaCRM.xlsx" download style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:8px;background:var(--crm-surface-hover,#f8fafc);border:1px solid var(--crm-border,#e2e8f0);text-decoration:none;color:inherit;font-size:13px;font-weight:500;">
                                    <span><i class="fa-solid fa-file-invoice-dollar" style="color:#3b82f6;margin-right:8px;"></i> Mẫu Xuất Báo Giá (Quotes)</span>
                                    <span style="color:#10b981;font-size:12px;font-weight:600;"><i class="fa-solid fa-download"></i> Tải về</span>
                                </a>
                                <a href="assets/templates/excel/Mau_Xuat_Hop_Dong_NovaCRM.xlsx" download style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:8px;background:var(--crm-surface-hover,#f8fafc);border:1px solid var(--crm-border,#e2e8f0);text-decoration:none;color:inherit;font-size:13px;font-weight:500;">
                                    <span><i class="fa-solid fa-file-contract" style="color:#6366f1;margin-right:8px;"></i> Mẫu Xuất Hợp Đồng (Contracts)</span>
                                    <span style="color:#10b981;font-size:12px;font-weight:600;"><i class="fa-solid fa-download"></i> Tải về</span>
                                </a>
                                <a href="assets/templates/excel/Mau_Xuat_Khach_Hang_NovaCRM.xlsx" download style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:8px;background:var(--crm-surface-hover,#f8fafc);border:1px solid var(--crm-border,#e2e8f0);text-decoration:none;color:inherit;font-size:13px;font-weight:500;">
                                    <span><i class="fa-solid fa-building" style="color:#0ea5e9;margin-right:8px;"></i> Mẫu Xuất Khách Hàng (Customers)</span>
                                    <span style="color:#10b981;font-size:12px;font-weight:600;"><i class="fa-solid fa-download"></i> Tải về</span>
                                </a>
                                <a href="assets/templates/excel/Mau_Xuat_Nguoi_Lien_He_NovaCRM.xlsx" download style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-radius:8px;background:var(--crm-surface-hover,#f8fafc);border:1px solid var(--crm-border,#e2e8f0);text-decoration:none;color:inherit;font-size:13px;font-weight:500;">
                                    <span><i class="fa-solid fa-address-book" style="color:#f59e0b;margin-right:8px;"></i> Mẫu Xuất Người Liên Hệ (Contacts)</span>
                                    <span style="color:#10b981;font-size:12px;font-weight:600;"><i class="fa-solid fa-download"></i> Tải về</span>
                                </a>
                            </div>
                        </div>

                    </div>

                    <div style="margin-top:20px;text-align:right;">
                        <button type="button" class="crm-btn crm-btn-secondary" id="btnDismissExcelModal" style="padding:8px 18px;border-radius:8px;cursor:pointer;">Đóng</button>
                    </div>
                </div>
            `;
            document.body.appendChild(modal);

            document.getElementById("btnCloseExcelModal")?.addEventListener("click", () => modal.remove());
            document.getElementById("btnDismissExcelModal")?.addEventListener("click", () => modal.remove());
            modal.addEventListener("click", (e) => {
                if (e.target === modal) modal.remove();
            });
        }
    }

    return {
        exportStyledExcel: exportStyledExcel,
        showTemplateModal: showTemplateModal
    };
}));
