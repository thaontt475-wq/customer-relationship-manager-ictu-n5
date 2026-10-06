"use strict";

const API_BASE =
    "http://localhost:8080/crm";

const MAX_FILE_SIZE =
    5 * 1024 * 1024;

let selectedFile =
    null;

let batchToken =
    null;

let previewRows =
    [];

const fileInput =
    document.getElementById(
        "importFile"
    );

const dropArea =
    document.getElementById(
        "dropArea"
    );

const selectedFileInfo =
    document.getElementById(
        "selectedFileInfo"
    );

const previewSection =
    document.getElementById(
        "previewSection"
    );

const previewBody =
    document.getElementById(
        "previewBody"
    );

const previewSummary =
    document.getElementById(
        "previewSummary"
    );

const importResult =
    document.getElementById(
        "importResult"
    );

const confirmButton =
    document.getElementById(
        "confirmImport"
    );


/* =========================================================
   INIT
========================================================= */

document.addEventListener(
    "DOMContentLoaded",
    () => {

        alignPreviewHeaders();
        bindEvents();

        if (confirmButton) {
            confirmButton.disabled =
                true;
        }
    }
);


/* =========================================================
   EVENTS
========================================================= */

function bindEvents() {

    document
        .getElementById(
            "downloadTemplate"
        )
        ?.addEventListener(
            "click",
            downloadTemplate
        );


    document
        .getElementById(
            "chooseImportFile"
        )
        ?.addEventListener(
            "click",
            () => fileInput?.click()
        );


    fileInput
        ?.addEventListener(
            "change",
            () => {

                const file =
                    fileInput.files?.[0];

                if (file) {
                    handleFile(file);
                }
            }
        );


    [
        "dragenter",
        "dragover"
    ].forEach(type => {

        dropArea
            ?.addEventListener(
                type,
                event => {

                    event.preventDefault();

                    dropArea.classList.add(
                        "drag"
                    );
                }
            );
    });


    [
        "dragleave",
        "drop"
    ].forEach(type => {

        dropArea
            ?.addEventListener(
                type,
                event => {

                    event.preventDefault();

                    dropArea.classList.remove(
                        "drag"
                    );
                }
            );
    });


    dropArea
        ?.addEventListener(
            "drop",
            event => {

                const file =
                    event.dataTransfer
                        ?.files?.[0];

                if (file) {
                    handleFile(file);
                }
            }
        );


    confirmButton
        ?.addEventListener(
            "click",
            confirmImport
        );
}


/* =========================================================
   API HELPER
========================================================= */

async function parseJsonResponse(
    response
) {

    let result = null;

    try {

        result =
            await response.json();

    } catch (_) {

        result =
            null;
    }


    if (
        response.status === 401
    ) {

        localStorage.removeItem(
            "crm_ui_session"
        );

        window.location.href =
            "login.html";

        throw new Error(
            "Phiên đăng nhập đã hết hạn."
        );
    }


    if (
        !response.ok ||
        !result?.success
    ) {

        const error =
            new Error(
                result?.message ||
                `HTTP ${response.status}`
            );

        error.status =
            response.status;

        throw error;
    }


    return result;
}


/* =========================================================
   DOWNLOAD TEMPLATE
========================================================= */

async function downloadTemplate() {

    setResult(
        "Đang tải file mẫu..."
    );

    try {

        const response =
            await fetch(
                `${API_BASE}/api/users/import/template`,
                {
                    method:
                        "GET",

                    credentials:
                        "include"
                }
            );


        if (
            response.status === 401
        ) {

            window.location.href =
                "login.html";

            return;
        }


        if (!response.ok) {

            let message =
                "Không thể tải file mẫu.";

            try {

                const body =
                    await response.json();

                message =
                    body?.message ||
                    message;

            } catch (_) {
            }

            throw new Error(
                message
            );
        }


        const blob =
            await response.blob();


        const url =
            URL.createObjectURL(
                blob
            );


        const link =
            document.createElement(
                "a"
            );

        link.href =
            url;

        link.download =
            "user-import-template.xlsx";


        document.body.appendChild(
            link
        );

        link.click();

        link.remove();


        URL.revokeObjectURL(
            url
        );


        setResult(
            "Đã tải file mẫu."
        );

    } catch (error) {

        console.error(
            "Download import template error:",
            error
        );

        setResult(
            error.message ||
            "Không thể tải file mẫu.",
            true
        );
    }
}


/* =========================================================
   SELECT FILE
========================================================= */

async function handleFile(
    file
) {

    resetPreview();

    const extension =
        file.name
            .split(".")
            .pop()
            ?.toLowerCase();


    if (extension !== "xlsx") {

        setResult(
            "Chỉ chấp nhận file .xlsx.",
            true
        );

        clearFileInput();

        return;
    }


    if (
        file.size <= 0
    ) {

        setResult(
            "File không có dữ liệu.",
            true
        );

        clearFileInput();

        return;
    }


    if (
        file.size >
        MAX_FILE_SIZE
    ) {

        setResult(
            "File không được vượt quá 5MB.",
            true
        );

        clearFileInput();

        return;
    }


    selectedFile =
        file;


    if (selectedFileInfo) {

        selectedFileInfo.hidden =
            false;

        selectedFileInfo.textContent =
            `${file.name} · ${(file.size / 1024).toFixed(1)} KB`;
    }


    await previewImport();
}


/* =========================================================
   PREVIEW API
========================================================= */

async function previewImport() {

    if (!selectedFile) {
        return;
    }


    setResult(
        "Đang kiểm tra file..."
    );

    setConfirmLoading(
        true,
        "Đang kiểm tra..."
    );


    try {

        const formData =
            new FormData();

        /*
         * Backend bắt buộc multipart
         * field tên chính xác là "file".
         */
        formData.append(
            "file",
            selectedFile
        );


        const response =
            await fetch(
                `${API_BASE}/api/users/import/preview`,
                {
                    method:
                        "POST",

                    credentials:
                        "include",

                    body:
                        formData
                }
            );


        const result =
            await parseJsonResponse(
                response
            );


        const data =
            result.data || {};


        batchToken =
            data.batchToken ||
            null;


        const validRows =
            Array.isArray(
                data.validRows
            )
                ? data.validRows
                : [];


        const errorRows =
            Array.isArray(
                data.errorRows
            )
                ? data.errorRows
                : [];


        previewRows = [

            ...validRows.map(
                row => ({
                    ...row,

                    valid:
                        true,

                    error:
                        ""
                })
            ),

            ...errorRows.map(
                row => ({
                    ...row,

                    valid:
                        false,

                    error:
                        row.error ||
                        "Dữ liệu không hợp lệ"
                })
            )
        ]
        .sort(
            (a,b) =>
                Number(a.row) -
                Number(b.row)
        );


        showPreview(
            previewRows
        );


        if (
            batchToken &&
            validRows.length > 0
        ) {

            confirmButton.disabled =
                false;

        } else {

            confirmButton.disabled =
                true;
        }


        setResult(
            `Đã kiểm tra ${previewRows.length} dòng. ${validRows.length} hợp lệ, ${errorRows.length} lỗi.`
        );

    } catch (error) {

        console.error(
            "Preview import error:",
            error
        );

        batchToken =
            null;

        previewRows =
            [];

        showPreview([]);

        setResult(
            error.message ||
            "Không thể kiểm tra file.",
            true
        );

    } finally {

        setConfirmLoading(
            false
        );
    }
}


/* =========================================================
   CONFIRM IMPORT
========================================================= */

async function confirmImport() {

    if (!batchToken) {

        setResult(
            "Chưa có batch import hợp lệ. Hãy chọn lại file.",
            true
        );

        return;
    }


    const validCount =
        previewRows.filter(
            row => row.valid
        ).length;


    if (
        validCount === 0
    ) {

        setResult(
            "Không có dòng hợp lệ để import.",
            true
        );

        return;
    }


    setConfirmLoading(
        true,
        "Đang import..."
    );


    try {

        const response =
            await fetch(
                `${API_BASE}/api/users/import/confirm`,
                {
                    method:
                        "POST",

                    credentials:
                        "include",

                    headers: {
                        "Content-Type":
                            "application/json",

                        "Accept":
                            "application/json"
                    },

                    body:
                        JSON.stringify({
                            batchToken
                        })
                }
            );


        const result =
            await parseJsonResponse(
                response
            );


        const data =
            result.data || {};


        /*
         * Batch token chỉ dùng một lần.
         */
        batchToken =
            null;

        confirmButton.disabled =
            true;


        const created =
            Number(
                data.created ??
                data.imported ??
                data.successCount ??
                validCount
            );


        const skipped =
            Number(
                data.skipped ??
                data.skippedCount ??
                0
            );


        const runtimeErrors =
            Array.isArray(
                data.errors
            )
                ? data.errors
                : [];


        const previewErrors =
            previewRows.filter(
                row => !row.valid
            ).length;


        let message =
            `Import hoàn tất: ${created} tạo thành công`;


        if (skipped > 0) {

            message +=
                ` · ${skipped} bỏ qua`;
        }


        if (previewErrors > 0) {

            message +=
                ` · ${previewErrors} dòng lỗi từ preview`;
        }


        if (
            runtimeErrors.length
        ) {

            message +=
                ` · ${runtimeErrors.length} lỗi khi tạo`;
        }


        setResult(
            message + "."
        );


        if (
            runtimeErrors.length
        ) {

            console.warn(
                "Import runtime errors:",
                runtimeErrors
            );
        }


    } catch (error) {

        console.error(
            "Confirm import error:",
            error
        );


        /*
         * Batch có thể đã hết 15 phút,
         * đã sử dụng hoặc không hợp lệ.
         */
        if (
            String(
                error.message
            )
                .toLowerCase()
                .includes("batch")
        ) {

            batchToken =
                null;

            confirmButton.disabled =
                true;
        }


        setResult(
            error.message ||
            "Không thể import người dùng.",
            true
        );

    } finally {

        setConfirmLoading(
            false
        );
    }
}


/* =========================================================
   PREVIEW RENDER
========================================================= */

function showPreview(
    rows
) {

    if (
        !previewSection ||
        !previewBody
    ) {
        return;
    }


    previewSection.hidden =
        false;

    previewBody.innerHTML =
        "";


    for (
        const row
        of rows
    ) {

        const tr =
            document.createElement(
                "tr"
            );


        tr.innerHTML = `

            <td>
                ${escapeHtml(row.row)}
            </td>

            <td>
                ${escapeHtml(row.fullName)}
            </td>

            <td>
                ${escapeHtml(row.email)}
            </td>

            <td>
                ${escapeHtml(row.status)}
            </td>

            <td>
                ${
                    row.valid
                    ? "—"
                    : escapeHtml(
                        row.error
                    )
                }
            </td>

            <td>

                ${
                    row.valid
                    ?
                    `
                    <span class="import-valid">
                        Hợp lệ
                    </span>
                    `
                    :
                    `
                    <span class="import-invalid">
                        Lỗi
                    </span>
                    `
                }

            </td>
        `;


        previewBody.appendChild(
            tr
        );
    }


    const valid =
        rows.filter(
            row => row.valid
        ).length;


    if (previewSummary) {

        previewSummary.textContent =
            `${rows.length} dòng · ${valid} hợp lệ · ${rows.length - valid} lỗi`;
    }
}


/* =========================================================
   ALIGN EXISTING TABLE HEADERS
========================================================= */

function alignPreviewHeaders() {

    const table =
        previewBody?.closest(
            "table"
        );

    const headers =
        table
            ?.querySelectorAll(
                "thead th"
            );


    if (
        !headers ||
        headers.length < 6
    ) {
        return;
    }


    const names = [
        "Dòng",
        "Họ tên",
        "Email",
        "Trạng thái",
        "Chi tiết lỗi",
        "Kết quả"
    ];


    names.forEach(
        (name,index) => {

            if (headers[index]) {

                headers[index]
                    .textContent =
                    name;
            }
        }
    );
}


/* =========================================================
   STATE
========================================================= */

function resetPreview() {

    selectedFile =
        null;

    batchToken =
        null;

    previewRows =
        [];


    if (confirmButton) {

        confirmButton.disabled =
            true;
    }


    if (previewBody) {

        previewBody.innerHTML =
            "";
    }


    if (previewSummary) {

        previewSummary.textContent =
            "";
    }


    if (importResult) {

        importResult.textContent =
            "";
    }
}


function clearFileInput() {

    selectedFile =
        null;

    batchToken =
        null;


    if (fileInput) {

        fileInput.value =
            "";
    }


    if (confirmButton) {

        confirmButton.disabled =
            true;
    }
}


function setConfirmLoading(
    loading,
    text = null
) {

    if (!confirmButton) {
        return;
    }


    if (
        !confirmButton
            .dataset
            .originalText
    ) {

        confirmButton
            .dataset
            .originalText =
                confirmButton.textContent
                    .trim();
    }


    if (loading) {

        confirmButton.disabled =
            true;

        confirmButton.textContent =
            text ||
            "Đang xử lý...";

    } else {

        confirmButton.textContent =
            confirmButton
                .dataset
                .originalText;


        if (batchToken) {

            confirmButton.disabled =
                previewRows
                    .filter(
                        row => row.valid
                    )
                    .length === 0;
        }
    }
}


function setResult(
    message,
    error = false
) {

    if (!importResult) {
        return;
    }


    importResult.textContent =
        message || "";

    importResult.style.color =
        error
            ? "#dc2626"
            : "";
}


/* =========================================================
   HELPERS
========================================================= */

function escapeHtml(value) {

    return String(
        value ?? ""
    )
        .replace(/&/g,"&amp;")
        .replace(/</g,"&lt;")
        .replace(/>/g,"&gt;")
        .replace(/"/g,"&quot;")
        .replace(/'/g,"&#039;");
}