"use strict";

const API_BASE =
    "http://localhost:8080/crm";

const BACKEND_ORIGIN =
    "http://localhost:8080";

const MAX_AVATAR_SIZE =
    2 * 1024 * 1024;


/* =========================================================
   STATE
========================================================= */

let sourceImage =
    null;

let currentProfile =
    null;

let cropState = {
    zoom: 1,
    offsetX: 0,
    offsetY: 0,
    dragging: false,
    startX: 0,
    startY: 0,
    originalX: 0,
    originalY: 0
};


/* =========================================================
   ELEMENTS
========================================================= */

const fileInput =
    document.getElementById(
        "avatarFile"
    );

const canvas =
    document.getElementById(
        "cropCanvas"
    );

const ctx =
    canvas
        ?.getContext(
            "2d"
        );

const zoomInput =
    document.getElementById(
        "cropZoom"
    );

const profileForm =
    document.getElementById(
        "profileForm"
    );

const profileMessage =
    document.getElementById(
        "profileMessage"
    );

const phoneError =
    document.getElementById(
        "phoneError"
    );

const avatarError =
    document.getElementById(
        "avatarError"
    );


/* =========================================================
   INIT
========================================================= */

document.addEventListener(
    "DOMContentLoaded",
    init
);


async function init() {

    bindEvents();

    try {

        await loadProfile();

    } catch (error) {

        console.error(
            "Profile init error:",
            error
        );

        setMessage(
            error.message ||
            "Không thể tải hồ sơ.",
            true
        );
    }
}


/* =========================================================
   API
========================================================= */

async function api(
    path,
    options = {}
) {

    const config = {
        credentials:
            "include",

        headers: {
            "Accept":
                "application/json",

            ...(options.body &&
            !(options.body instanceof FormData)
                ? {
                    "Content-Type":
                        "application/json"
                }
                : {}),

            ...(options.headers || {})
        },

        ...options
    };


    const response =
        await fetch(
            API_BASE + path,
            config
        );


    let result =
        null;


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


    return result.data;
}


/* =========================================================
   LOAD PROFILE
========================================================= */

async function loadProfile() {

    setMessage(
        "Đang tải hồ sơ..."
    );


    const data =
        await api(
            "/api/profile"
        );


    currentProfile =
        data || {};


    setValue(
        "profileName",
        currentProfile.fullName
    );

    setValue(
        "profilePhone",
        currentProfile.phone
    );

    setValue(
        "profileEmail",
        currentProfile.email
    );

    setValue(
        "profileSignature",
        currentProfile.emailSignature
    );


    /*
     * Email không được tự sửa.
     */
    const emailInput =
        document.getElementById(
            "profileEmail"
        );

    if (emailInput) {

        emailInput.readOnly =
            true;
    }


    renderAvatar(
        currentProfile.avatarUrl,
        currentProfile.avatarThumbnailUrl
    );


    clearErrors();

    setMessage(
        ""
    );
}


/* =========================================================
   SAVE PROFILE
========================================================= */

async function saveProfile(
    event
) {

    event.preventDefault();

    clearErrors();


    const fullName =
        value(
            "profileName"
        );

    const phone =
        value(
            "profilePhone"
        );

    const emailSignature =
        value(
            "profileSignature"
        );


    if (!fullName) {

        setMessage(
            "Họ tên là bắt buộc.",
            true
        );

        return;
    }


    if (
        phone &&
        !isVietnamPhone(
            phone
        )
    ) {

        if (phoneError) {

            phoneError.textContent =
                "Số điện thoại không hợp lệ. Ví dụ: 0912345678 hoặc +84912345678.";
        }

        return;
    }


    const submit =
        profileForm
            ?.querySelector(
                'button[type="submit"]'
            );

    const originalText =
        submit?.textContent || "";


    if (submit) {

        submit.disabled =
            true;

        submit.textContent =
            "Đang lưu...";
    }


    try {

        const data =
            await api(
                "/api/profile",
                {
                    method:
                        "PUT",

                    body:
                        JSON.stringify({
                            fullName,
                            phone:
                                phone || null,
                            emailSignature:
                                emailSignature || null
                        })
                }
            );


        currentProfile =
            data;


        setValue(
            "profileName",
            data.fullName
        );

        setValue(
            "profilePhone",
            data.phone
        );

        setValue(
            "profileEmail",
            data.email
        );

        setValue(
            "profileSignature",
            data.emailSignature
        );


        /*
         * Không lưu profile vào localStorage.
         * Backend/MySQL là nguồn dữ liệu chính.
         */

        setMessage(
            "Đã lưu thay đổi."
        );


    } catch (error) {

        console.error(
            "Save profile error:",
            error
        );

        if (
            String(
                error.message
            )
                .toLowerCase()
                .includes("điện thoại")
        ) {

            if (phoneError) {

                phoneError.textContent =
                    error.message;
            }

        } else {

            setMessage(
                error.message ||
                "Không thể lưu hồ sơ.",
                true
            );
        }


    } finally {

        if (submit) {

            submit.disabled =
                false;

            submit.textContent =
                originalText;
        }
    }
}


/* =========================================================
   AVATAR FILE SELECT
========================================================= */

function handleAvatarFile() {

    clearAvatarError();


    const file =
        fileInput
            ?.files?.[0];


    if (!file) {
        return;
    }


    if (
        ![
            "image/jpeg",
            "image/png"
        ].includes(
            file.type
        )
    ) {

        setAvatarError(
            "Chỉ chấp nhận ảnh JPG hoặc PNG."
        );

        fileInput.value =
            "";

        return;
    }


    if (
        file.size >
        MAX_AVATAR_SIZE
    ) {

        setAvatarError(
            "Ảnh không được vượt quá 2MB."
        );

        fileInput.value =
            "";

        return;
    }


    const reader =
        new FileReader();


    reader.onload =
        event => {

            const image =
                new Image();


            image.onload =
                () => {

                    sourceImage =
                        image;


                    cropState = {
                        zoom: 1,
                        offsetX: 0,
                        offsetY: 0,
                        dragging: false,
                        startX: 0,
                        startY: 0,
                        originalX: 0,
                        originalY: 0
                    };


                    if (zoomInput) {

                        zoomInput.value =
                            "1";
                    }


                    const cropArea =
                        document.getElementById(
                            "cropArea"
                        );

                    if (cropArea) {

                        cropArea.hidden =
                            false;
                    }


                    drawCrop();
                };


            image.onerror =
                () => {

                    setAvatarError(
                        "Không thể đọc file ảnh."
                    );
                };


            image.src =
                event.target.result;
        };


    reader.readAsDataURL(
        file
    );
}


/* =========================================================
   CROP
========================================================= */

function drawCrop() {

    if (
        !sourceImage ||
        !canvas ||
        !ctx
    ) {
        return;
    }


    const size =
        canvas.width;


    ctx.clearRect(
        0,
        0,
        size,
        size
    );


    const transform =
        calculateTransform(
            size
        );


    ctx.drawImage(
        sourceImage,
        transform.x,
        transform.y,
        transform.width,
        transform.height
    );


    ctx.save();


    ctx.fillStyle =
        "rgba(15, 23, 42, .58)";


    ctx.beginPath();


    ctx.rect(
        0,
        0,
        size,
        size
    );


    ctx.arc(
        size / 2,
        size / 2,
        size * .405,
        0,
        Math.PI * 2,
        true
    );


    ctx.fill(
        "evenodd"
    );


    ctx.beginPath();


    ctx.arc(
        size / 2,
        size / 2,
        size * .405,
        0,
        Math.PI * 2
    );


    ctx.strokeStyle =
        "rgba(255,255,255,.95)";


    ctx.lineWidth =
        3;


    ctx.stroke();


    ctx.restore();
}


function calculateTransform(
    size
) {

    const baseScale =
        Math.max(
            size /
            sourceImage.width,

            size /
            sourceImage.height
        );


    const scale =
        baseScale *
        cropState.zoom;


    const width =
        sourceImage.width *
        scale;


    const height =
        sourceImage.height *
        scale;


    return {
        width,
        height,

        x:
            (
                size -
                width
            ) / 2 +
            cropState.offsetX,

        y:
            (
                size -
                height
            ) / 2 +
            cropState.offsetY
    };
}


function constrainOffset() {

    if (
        !sourceImage ||
        !canvas
    ) {
        return;
    }


    const transform =
        calculateTransform(
            canvas.width
        );


    const radius =
        canvas.width *
        .405;


    const minVisible =
        radius * 2;


    const maxX =
        Math.max(
            0,
            (
                transform.width -
                minVisible
            ) / 2
        );


    const maxY =
        Math.max(
            0,
            (
                transform.height -
                minVisible
            ) / 2
        );


    cropState.offsetX =
        Math.max(
            -maxX,
            Math.min(
                maxX,
                cropState.offsetX
            )
        );


    cropState.offsetY =
        Math.max(
            -maxY,
            Math.min(
                maxY,
                cropState.offsetY
            )
        );
}


/* =========================================================
   CREATE CROPPED IMAGE
========================================================= */

function createCroppedCanvas() {

    const outputSize =
        512;


    const output =
        document.createElement(
            "canvas"
        );


    output.width =
        outputSize;

    output.height =
        outputSize;


    const outputContext =
        output.getContext(
            "2d"
        );


    const cropRadius =
        canvas.width *
        .405;


    const cropDiameter =
        cropRadius * 2;


    const previewTransform =
        calculateTransform(
            canvas.width
        );


    const scale =
        outputSize /
        cropDiameter;


    outputContext.drawImage(
        sourceImage,

        (
            previewTransform.x -
            (
                canvas.width -
                cropDiameter
            ) / 2
        ) * scale,

        (
            previewTransform.y -
            (
                canvas.height -
                cropDiameter
            ) / 2
        ) * scale,

        previewTransform.width *
        scale,

        previewTransform.height *
        scale
    );


    return output;
}


/* =========================================================
   CANVAS → BLOB
========================================================= */

function canvasToBlob(
    canvasElement,
    type,
    quality
) {

    return new Promise(
        resolve => {

            canvasElement.toBlob(
                resolve,
                type,
                quality
            );
        }
    );
}


async function createAvatarBlob() {

    const output =
        createCroppedCanvas();


    /*
     * Upload ảnh đã crop thay vì ảnh gốc.
     * JPEG giúp giảm dung lượng.
     */

    let quality =
        0.92;

    let blob =
        await canvasToBlob(
            output,
            "image/jpeg",
            quality
        );


    while (
        blob &&
        blob.size >
        MAX_AVATAR_SIZE &&
        quality > 0.5
    ) {

        quality -=
            0.1;

        blob =
            await canvasToBlob(
                output,
                "image/jpeg",
                quality
            );
    }


    if (!blob) {

        throw new Error(
            "Không thể tạo ảnh sau khi crop."
        );
    }


    if (
        blob.size >
        MAX_AVATAR_SIZE
    ) {

        throw new Error(
            "Ảnh sau khi crop vẫn vượt quá 2MB."
        );
    }


    return blob;
}


/* =========================================================
   UPLOAD AVATAR
========================================================= */

async function uploadAvatar() {

    if (!sourceImage) {
        return;
    }


    clearAvatarError();


    const button =
        document.getElementById(
            "applyAvatar"
        );


    const originalText =
        button?.textContent || "";


    if (button) {

        button.disabled =
            true;

        button.textContent =
            "Đang tải lên...";
    }


    try {

        const blob =
            await createAvatarBlob();


        const formData =
            new FormData();


        formData.append(
            "file",
            blob,
            "avatar.jpg"
        );


        const data =
            await api(
                "/api/profile/avatar",
                {
                    method:
                        "POST",

                    body:
                        formData
                }
            );


        currentProfile = {
            ...(currentProfile || {}),
            avatarUrl:
                data.avatarUrl,
            avatarThumbnailUrl:
                data.thumbnailUrl
        };


        renderAvatar(
            data.avatarUrl,
            data.thumbnailUrl
        );


        const cropArea =
            document.getElementById(
                "cropArea"
            );

        if (cropArea) {

            cropArea.hidden =
                true;
        }


        sourceImage =
            null;


        if (fileInput) {

            fileInput.value =
                "";
        }


        setMessage(
            "Ảnh đại diện đã được cập nhật."
        );


        /*
         * Cho shared-shell cập nhật nếu
         * đang có hook tương ứng.
         */
        window.crmUpdateGlobalAvatar?.();


    } catch (error) {

        console.error(
            "Avatar upload error:",
            error
        );


        setAvatarError(
            error.message ||
            "Không thể tải ảnh đại diện."
        );


    } finally {

        if (button) {

            button.disabled =
                false;

            button.textContent =
                originalText;
        }
    }
}


/* =========================================================
   RENDER AVATAR
========================================================= */

function renderAvatar(
    avatarUrl,
    thumbnailUrl
) {

    const profileAvatar =
        document.getElementById(
            "profileAvatarImage"
        );

    const empty =
        document.getElementById(
            "profileAvatarEmpty"
        );

    const thumbnail =
        document.getElementById(
            "avatarThumbnail"
        );

    const thumbnailSection =
        document.getElementById(
            "thumbnailSection"
        );


    if (avatarUrl) {

        if (profileAvatar) {

            profileAvatar.src =
                absoluteUrl(
                    avatarUrl
                );

            profileAvatar.hidden =
                false;
        }


        if (empty) {

            empty.hidden =
                true;
        }

    } else {

        if (profileAvatar) {

            profileAvatar.hidden =
                true;
        }


        if (empty) {

            empty.hidden =
                false;
        }
    }


    if (thumbnailUrl) {

        if (thumbnail) {

            thumbnail.src =
                absoluteUrl(
                    thumbnailUrl
                );
        }


        if (thumbnailSection) {

            thumbnailSection.hidden =
                false;
        }

    } else if (
        thumbnailSection
    ) {

        thumbnailSection.hidden =
            true;
    }
}


/* =========================================================
   EVENTS
========================================================= */

function bindEvents() {

    document
        .getElementById(
            "selectAvatar"
        )
        ?.addEventListener(
            "click",
            () => fileInput?.click()
        );


    fileInput
        ?.addEventListener(
            "change",
            handleAvatarFile
        );


    zoomInput
        ?.addEventListener(
            "input",
            () => {

                cropState.zoom =
                    Number(
                        zoomInput.value
                    );

                constrainOffset();

                drawCrop();
            }
        );


    canvas
        ?.addEventListener(
            "pointerdown",
            event => {

                if (!sourceImage) {
                    return;
                }


                cropState.dragging =
                    true;


                cropState.startX =
                    event.clientX;

                cropState.startY =
                    event.clientY;


                cropState.originalX =
                    cropState.offsetX;

                cropState.originalY =
                    cropState.offsetY;


                canvas
                    .setPointerCapture?.(
                        event.pointerId
                    );


                canvas.classList.add(
                    "dragging"
                );
            }
        );


    canvas
        ?.addEventListener(
            "pointermove",
            event => {

                if (
                    !sourceImage ||
                    !cropState.dragging
                ) {
                    return;
                }


                const rect =
                    canvas
                        .getBoundingClientRect();


                const scaleX =
                    canvas.width /
                    rect.width;


                const scaleY =
                    canvas.height /
                    rect.height;


                cropState.offsetX =
                    cropState.originalX +
                    (
                        event.clientX -
                        cropState.startX
                    ) *
                    scaleX;


                cropState.offsetY =
                    cropState.originalY +
                    (
                        event.clientY -
                        cropState.startY
                    ) *
                    scaleY;


                constrainOffset();

                drawCrop();
            }
        );


    canvas
        ?.addEventListener(
            "pointerup",
            stopDragging
        );


    canvas
        ?.addEventListener(
            "pointercancel",
            stopDragging
        );


    document
        .getElementById(
            "applyAvatar"
        )
        ?.addEventListener(
            "click",
            uploadAvatar
        );


    profileForm
        ?.addEventListener(
            "submit",
            saveProfile
        );


    document
        .getElementById(
            "resetProfile"
        )
        ?.addEventListener(
            "click",
            async event => {

                event.preventDefault();

                try {

                    await loadProfile();

                    setMessage(
                        "Đã khôi phục dữ liệu từ hệ thống."
                    );

                } catch (error) {

                    setMessage(
                        error.message ||
                        "Không thể tải lại hồ sơ.",
                        true
                    );
                }
            }
        );
}


function stopDragging() {

    cropState.dragging =
        false;


    canvas
        ?.classList
        .remove(
            "dragging"
        );
}


/* =========================================================
   VALIDATION
========================================================= */

function isVietnamPhone(
    input
) {

    const normalized =
        String(input)
            .replace(
                /[\s().-]/g,
                ""
            );


    /*
     * 0xxxxxxxxx
     * +84xxxxxxxxx
     */
    return /^(0\d{9}|\+84\d{9})$/
        .test(
            normalized
        );
}


/* =========================================================
   HELPERS
========================================================= */

function absoluteUrl(
    url
) {

    if (!url) {
        return "";
    }


    if (
        /^https?:\/\//i.test(
            url
        )
    ) {

        return url;
    }


    return BACKEND_ORIGIN +
        (
            url.startsWith("/")
                ? url
                : "/" + url
        );
}


function value(
    id
) {

    return (
        document
            .getElementById(id)
            ?.value ??
        ""
    )
        .toString()
        .trim();
}


function setValue(
    id,
    newValue
) {

    const element =
        document.getElementById(
            id
        );


    if (element) {

        element.value =
            newValue ?? "";
    }
}


function clearErrors() {

    if (phoneError) {

        phoneError.textContent =
            "";
    }


    clearAvatarError();
}


function clearAvatarError() {

    if (avatarError) {

        avatarError.textContent =
            "";
    }
}


function setAvatarError(
    message
) {

    if (avatarError) {

        avatarError.textContent =
            message || "";
    }
}


function setMessage(
    message,
    error = false
) {

    if (!profileMessage) {
        return;
    }


    profileMessage.textContent =
        message || "";


    profileMessage.style.color =
        error
            ? "#dc2626"
            : "";
}