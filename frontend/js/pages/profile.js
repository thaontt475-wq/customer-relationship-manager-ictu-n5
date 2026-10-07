"use strict";

const API_BASE = "http://localhost:8080/crm";
const BACKEND_ORIGIN = "http://localhost:8080";
const MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB

/* =========================================================
   STATE
========================================================= */

let sourceImage = null;
let currentProfile = null;

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

const fileInput = document.getElementById("avatarFile");
const canvas = document.getElementById("cropCanvas");
const ctx = canvas?.getContext("2d");
const zoomInput = document.getElementById("cropZoom");
const profileForm = document.getElementById("profileForm");
const profileMessage = document.getElementById("profileMessage");
const profileAlert = document.getElementById("profileAlert");
const alertIcon = document.getElementById("alertIcon");
const alertText = document.getElementById("alertText");
const phoneError = document.getElementById("phoneError");
const avatarError = document.getElementById("avatarError");

/* =========================================================
   INITIALIZATION
========================================================= */

document.addEventListener("DOMContentLoaded", init);

async function init() {
    bindEvents();
    try {
        await loadProfile();
    } catch (error) {
        console.error("Profile init error:", error);
        showAlert(error.message || "Không thể tải hồ sơ cá nhân.", "danger");
    }
}

/* =========================================================
   API CLIENT
========================================================= */

async function api(path, options = {}) {
    const config = {
        credentials: "include",
        headers: {
            "Accept": "application/json",
            ...(options.body && !(options.body instanceof FormData)
                ? { "Content-Type": "application/json" }
                : {}),
            ...(options.headers || {})
        },
        ...options
    };

    const response = await fetch(API_BASE + path, config);

    let result = null;
    try {
        result = await response.json();
    } catch (_) {
        result = null;
    }

    if (response.status === 401) {
        localStorage.removeItem("crm_ui_session");
        window.location.href = "login.html";
        throw new Error("Phiên đăng nhập đã hết hạn.");
    }

    if (!response.ok || !result?.success) {
        const error = new Error(result?.message || `HTTP ${response.status}`);
        error.status = response.status;
        throw error;
    }

    return result.data;
}

/* =========================================================
   LOAD PROFILE
========================================================= */

async function loadProfile() {
    clearAlert();
    const data = await api("/api/profile");
    currentProfile = data || {};

    // Populate inputs
    setValue("profileName", currentProfile.fullName || "");
    setValue("profilePhone", currentProfile.phone || "");
    setValue("profileEmail", currentProfile.email || "");
    setValue("profileTeam", currentProfile.teamName || "Chưa phân nhóm");
    setValue("profileRoles", currentProfile.roleNames || "Chưa phân vai trò");
    setValue("profileSignature", currentProfile.emailSignature || "");

    // Update Hero Banner
    updateHeroBanner(currentProfile);

    // Update Initials
    updateInitials(currentProfile.fullName);

    // Update Live Signature Preview
    updateLiveSignaturePreview();

    // Render Avatar
    renderAvatar(currentProfile.avatarUrl, currentProfile.avatarThumbnailUrl);

    clearErrors();
}

function getInitialLetter(name) {
    if (!name || !name.trim()) return "U";
    const parts = name.trim().split(/\s+/);
    const lastWord = parts[parts.length - 1];
    return lastWord.charAt(0).toUpperCase();
}

function updateInitials(name) {
    const initial = getInitialLetter(name);
    const heroInitial = document.getElementById("heroAvatarEmpty");
    const profileInitial = document.getElementById("profileAvatarEmpty");
    const topAvatar = document.getElementById("topAvatar");

    if (heroInitial) heroInitial.textContent = initial;
    if (profileInitial) profileInitial.textContent = initial;
    if (topAvatar && !currentProfile?.avatarUrl) topAvatar.textContent = initial;
}

function updateHeroBanner(profile) {
    const heroFullName = document.getElementById("heroFullName");
    const heroEmailChip = document.getElementById("heroEmailChip");
    const heroPhoneChip = document.getElementById("heroPhoneChip");
    const heroRoleText = document.getElementById("heroRoleText");
    const heroTeamText = document.getElementById("heroTeamText");
    const heroIdText = document.getElementById("heroIdText");

    if (heroFullName) heroFullName.textContent = profile.fullName || "Hồ sơ người dùng";
    if (heroEmailChip) heroEmailChip.textContent = profile.email || "email@company.com";
    if (heroPhoneChip) heroPhoneChip.textContent = profile.phone || "Chưa cập nhật SĐT";
    if (heroRoleText) heroRoleText.textContent = profile.roleNames || "Thành viên";
    if (heroTeamText) heroTeamText.textContent = profile.teamName || "Chưa phân nhóm";
    if (heroIdText) heroIdText.textContent = profile.id ? `#${profile.id}` : "--";
}

/* =========================================================
   LIVE SIGNATURE PREVIEW
========================================================= */

function updateLiveSignaturePreview() {
    const fullName = value("profileName") || currentProfile?.fullName || "Người dùng";
    const phone = value("profilePhone") || currentProfile?.phone || "Chưa có SĐT";
    const email = value("profileEmail") || currentProfile?.email || "email@company.com";
    const role = currentProfile?.roleNames || "Chuyên viên CRM";
    const team = currentProfile?.teamName ? ` · ${currentProfile.teamName}` : "";
    const signature = value("profileSignature");

    const mockupName = document.getElementById("mockupName");
    const mockupRole = document.getElementById("mockupRole");
    const mockupPhone = document.getElementById("mockupPhone");
    const mockupEmail = document.getElementById("mockupEmail");
    const mockupCustomText = document.getElementById("mockupCustomText");

    if (mockupName) mockupName.textContent = fullName;
    if (mockupRole) mockupRole.textContent = `${role}${team} | Corporate CRM`;
    if (mockupPhone) mockupPhone.textContent = `Hotline: ${phone}`;
    if (mockupEmail) mockupEmail.textContent = `Email: ${email}`;
    if (mockupCustomText) {
        mockupCustomText.textContent = signature || "Trân trọng cảm ơn!";
    }
}

/* =========================================================
   SAVE PROFILE
========================================================= */

async function saveProfile(event) {
    event.preventDefault();
    clearErrors();
    clearAlert();

    const fullName = value("profileName");
    const phone = value("profilePhone");
    const emailSignature = value("profileSignature");

    if (!fullName) {
        showAlert("Họ và tên là trường bắt buộc.", "danger");
        document.getElementById("profileName")?.focus();
        return;
    }

    if (phone && !isVietnamPhone(phone)) {
        if (phoneError) {
            phoneError.textContent = "Số điện thoại không hợp lệ. Ví dụ: 0912345678 hoặc +84912345678.";
        }
        document.getElementById("profilePhone")?.focus();
        return;
    }

    const submitBtn = document.getElementById("saveProfileBtn");
    const originalText = submitBtn?.innerHTML || "";

    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = `
            <span class="crm-inline-spinner" style="width:14px;height:14px;border:2px solid #fff;border-top-color:transparent;border-radius:50%;display:inline-block;animation:spin 0.6s linear infinite;margin-right:6px;"></span>
            Đang lưu thay đổi...
        `;
    }

    try {
        const data = await api("/api/profile", {
            method: "PUT",
            body: JSON.stringify({
                fullName,
                phone: phone || null,
                emailSignature: emailSignature || null
            })
        });

        currentProfile = data;

        // Re-sync values
        setValue("profileName", data.fullName || "");
        setValue("profilePhone", data.phone || "");
        setValue("profileEmail", data.email || "");
        setValue("profileSignature", data.emailSignature || "");

        // Update banners & initials
        updateHeroBanner(data);
        updateInitials(data.fullName);
        updateLiveSignaturePreview();

        showAlert("✓ Cập nhật hồ sơ cá nhân thành công!", "success");

        // Update shell username
        const shellUserName = document.querySelector(".crm-user-name");
        if (shellUserName && data.fullName) {
            shellUserName.textContent = data.fullName;
        }

    } catch (error) {
        console.error("Save profile error:", error);

        if (String(error.message).toLowerCase().includes("điện thoại")) {
            if (phoneError) {
                phoneError.textContent = error.message;
            }
        } else {
            showAlert(error.message || "Không thể lưu hồ sơ cá nhân.", "danger");
        }
    } finally {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        }
    }
}

/* =========================================================
   AVATAR UPLOAD & CROP
========================================================= */

function handleAvatarFile() {
    clearAvatarError();
    const file = fileInput?.files?.[0];
    if (!file) return;

    if (!["image/jpeg", "image/png"].includes(file.type)) {
        setAvatarError("Chỉ chấp nhận ảnh định dạng JPG hoặc PNG.");
        fileInput.value = "";
        return;
    }

    if (file.size > MAX_AVATAR_SIZE) {
        setAvatarError("Dung lượng ảnh không được vượt quá 2MB.");
        fileInput.value = "";
        return;
    }

    const reader = new FileReader();
    reader.onload = event => {
        const image = new Image();
        image.onload = () => {
            sourceImage = image;
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

            if (zoomInput) zoomInput.value = "1";

            const cropArea = document.getElementById("cropArea");
            if (cropArea) cropArea.hidden = false;

            drawCrop();
        };

        image.onerror = () => {
            setAvatarError("Không thể đọc tệp ảnh đã chọn.");
        };

        image.src = event.target.result;
    };

    reader.readAsDataURL(file);
}

function drawCrop() {
    if (!sourceImage || !canvas || !ctx) return;

    const size = canvas.width;
    ctx.clearRect(0, 0, size, size);

    const transform = calculateTransform(size);
    ctx.drawImage(sourceImage, transform.x, transform.y, transform.width, transform.height);

    ctx.save();
    ctx.fillStyle = "rgba(15, 23, 42, .58)";
    ctx.beginPath();
    ctx.rect(0, 0, size, size);
    ctx.arc(size / 2, size / 2, size * .405, 0, Math.PI * 2, true);
    ctx.fill("evenodd");

    ctx.beginPath();
    ctx.arc(size / 2, size / 2, size * .405, 0, Math.PI * 2);
    ctx.strokeStyle = "rgba(255,255,255,.95)";
    ctx.lineWidth = 3;
    ctx.stroke();
    ctx.restore();
}

function calculateTransform(size) {
    const baseScale = Math.max(size / sourceImage.width, size / sourceImage.height);
    const scale = baseScale * cropState.zoom;
    const width = sourceImage.width * scale;
    const height = sourceImage.height * scale;

    return {
        width,
        height,
        x: (size - width) / 2 + cropState.offsetX,
        y: (size - height) / 2 + cropState.offsetY
    };
}

function constrainOffset() {
    if (!sourceImage || !canvas) return;

    const transform = calculateTransform(canvas.width);
    const radius = canvas.width * .405;
    const minVisible = radius * 2;

    const maxX = Math.max(0, (transform.width - minVisible) / 2);
    const maxY = Math.max(0, (transform.height - minVisible) / 2);

    cropState.offsetX = Math.max(-maxX, Math.min(maxX, cropState.offsetX));
    cropState.offsetY = Math.max(-maxY, Math.min(maxY, cropState.offsetY));
}

function createCroppedCanvas() {
    const outputSize = 512;
    const output = document.createElement("canvas");
    output.width = outputSize;
    output.height = outputSize;

    const outputContext = output.getContext("2d");
    const cropRadius = canvas.width * .405;
    const cropDiameter = cropRadius * 2;
    const previewTransform = calculateTransform(canvas.width);
    const scale = outputSize / cropDiameter;

    outputContext.drawImage(
        sourceImage,
        (previewTransform.x - (canvas.width - cropDiameter) / 2) * scale,
        (previewTransform.y - (canvas.height - cropDiameter) / 2) * scale,
        previewTransform.width * scale,
        previewTransform.height * scale
    );

    return output;
}

function canvasToBlob(canvasElement, type, quality) {
    return new Promise(resolve => {
        canvasElement.toBlob(resolve, type, quality);
    });
}

async function createAvatarBlob() {
    const output = createCroppedCanvas();
    let quality = 0.92;
    let blob = await canvasToBlob(output, "image/jpeg", quality);

    while (blob && blob.size > MAX_AVATAR_SIZE && quality > 0.5) {
        quality -= 0.1;
        blob = await canvasToBlob(output, "image/jpeg", quality);
    }

    if (!blob) {
        throw new Error("Không thể tạo dữ liệu ảnh sau khi cắt.");
    }

    if (blob.size > MAX_AVATAR_SIZE) {
        throw new Error("Ảnh sau khi cắt vẫn vượt quá giới hạn 2MB.");
    }

    return blob;
}

async function uploadAvatar() {
    if (!sourceImage) return;
    clearAvatarError();

    const button = document.getElementById("applyAvatar");
    const originalText = button?.textContent || "";

    if (button) {
        button.disabled = true;
        button.textContent = "Đang tải ảnh lên...";
    }

    try {
        const blob = await createAvatarBlob();
        const formData = new FormData();
        formData.append("file", blob, "avatar.jpg");

        const data = await api("/api/profile/avatar", {
            method: "POST",
            body: formData
        });

        currentProfile = {
            ...(currentProfile || {}),
            avatarUrl: data.avatarUrl,
            avatarThumbnailUrl: data.thumbnailUrl
        };

        renderAvatar(data.avatarUrl, data.thumbnailUrl);

        const cropArea = document.getElementById("cropArea");
        if (cropArea) cropArea.hidden = true;

        sourceImage = null;
        if (fileInput) fileInput.value = "";

        showAlert("✓ Cập nhật ảnh đại diện thành công!", "success");
        window.crmUpdateGlobalAvatar?.();

    } catch (error) {
        console.error("Avatar upload error:", error);
        setAvatarError(error.message || "Không thể tải ảnh đại diện.");
    } finally {
        if (button) {
            button.disabled = false;
            button.textContent = originalText;
        }
    }
}

/* =========================================================
   RENDER AVATAR
========================================================= */

function renderAvatar(avatarUrl, thumbnailUrl) {
    const profileAvatar = document.getElementById("profileAvatarImage");
    const empty = document.getElementById("profileAvatarEmpty");
    const heroImage = document.getElementById("heroAvatarImage");
    const heroEmpty = document.getElementById("heroAvatarEmpty");
    const thumbnail = document.getElementById("avatarThumbnail");
    const thumbnailSection = document.getElementById("thumbnailSection");
    const topAvatar = document.getElementById("topAvatar");

    if (avatarUrl) {
        const fullUrl = absoluteUrl(avatarUrl) + "?v=" + Date.now();
        if (profileAvatar) {
            profileAvatar.src = fullUrl;
            profileAvatar.hidden = false;
        }
        if (empty) empty.hidden = true;

        if (heroImage) {
            heroImage.src = fullUrl;
            heroImage.hidden = false;
        }
        if (heroEmpty) heroEmpty.hidden = true;

        if (topAvatar) {
            topAvatar.innerHTML = `<img src="${fullUrl}" style="width:100%;height:100%;border-radius:50%;object-fit:cover;" alt="Avatar">`;
        }
    } else {
        if (profileAvatar) profileAvatar.hidden = true;
        if (empty) empty.hidden = false;

        if (heroImage) heroImage.hidden = true;
        if (heroEmpty) heroEmpty.hidden = false;
    }

    if (thumbnailUrl) {
        if (thumbnail) {
            thumbnail.src = absoluteUrl(thumbnailUrl) + "?v=" + Date.now();
        }
        if (thumbnailSection) thumbnailSection.hidden = false;
    } else if (thumbnailSection) {
        thumbnailSection.hidden = true;
    }
}

/* =========================================================
   EVENT BINDINGS
========================================================= */

function bindEvents() {
    // Select Avatar button & hover trigger
    document.getElementById("selectAvatar")?.addEventListener("click", () => fileInput?.click());
    document.getElementById("avatarHoverTrigger")?.addEventListener("click", () => fileInput?.click());
    fileInput?.addEventListener("change", handleAvatarFile);

    // Zoom
    zoomInput?.addEventListener("input", () => {
        cropState.zoom = Number(zoomInput.value);
        constrainOffset();
        drawCrop();
    });

    // Canvas pointer drag
    canvas?.addEventListener("pointerdown", event => {
        if (!sourceImage) return;
        cropState.dragging = true;
        cropState.startX = event.clientX;
        cropState.startY = event.clientY;
        cropState.originalX = cropState.offsetX;
        cropState.originalY = cropState.offsetY;
        canvas.setPointerCapture?.(event.pointerId);
        canvas.classList.add("dragging");
    });

    canvas?.addEventListener("pointermove", event => {
        if (!sourceImage || !cropState.dragging) return;
        const rect = canvas.getBoundingClientRect();
        const scaleX = canvas.width / rect.width;
        const scaleY = canvas.height / rect.height;

        cropState.offsetX = cropState.originalX + (event.clientX - cropState.startX) * scaleX;
        cropState.offsetY = cropState.originalY + (event.clientY - cropState.startY) * scaleY;
        constrainOffset();
        drawCrop();
    });

    canvas?.addEventListener("pointerup", stopDragging);
    canvas?.addEventListener("pointercancel", stopDragging);

    // Crop submit
    document.getElementById("applyAvatar")?.addEventListener("click", uploadAvatar);

    // Form submit
    profileForm?.addEventListener("submit", saveProfile);

    // Reset button
    document.getElementById("resetProfile")?.addEventListener("click", async event => {
        event.preventDefault();
        try {
            await loadProfile();
            showAlert("Đã khôi phục dữ liệu ban đầu.", "success");
        } catch (error) {
            showAlert(error.message || "Không thể tải lại hồ sơ.", "danger");
        }
    });

    // Real-time live signature preview
    document.getElementById("profileSignature")?.addEventListener("input", updateLiveSignaturePreview);
    document.getElementById("profileName")?.addEventListener("input", updateLiveSignaturePreview);
    document.getElementById("profilePhone")?.addEventListener("input", updateLiveSignaturePreview);

    // Signature templates
    document.getElementById("btnInsertTemplate")?.addEventListener("click", () => {
        const name = value("profileName") || currentProfile?.fullName || "Nguyễn Văn A";
        const phone = value("profilePhone") || currentProfile?.phone || "0912345678";
        const email = value("profileEmail") || currentProfile?.email || "user@company.com";
        const team = currentProfile?.teamName || "Khối Kinh doanh";

        const template = `Trân trọng / Best regards,\n${name}\n${team} | Corporate CRM\nHotline: ${phone} | Email: ${email}\nWebsite: https://crm.company.com.vn`;
        setValue("profileSignature", template);
        updateLiveSignaturePreview();
        showAlert("Đã chèn mẫu chữ ký doanh nghiệp chuẩn.", "success");
    });

    document.getElementById("btnClearSignature")?.addEventListener("click", () => {
        setValue("profileSignature", "");
        updateLiveSignaturePreview();
    });
}

function stopDragging() {
    cropState.dragging = false;
    canvas?.classList.remove("dragging");
}

/* =========================================================
   VALIDATION & ALERTS
========================================================= */

function isVietnamPhone(input) {
    const normalized = String(input).replace(/[\s().-]/g, "");
    return /^(0\d{9}|\+84\d{9})$/.test(normalized);
}

function absoluteUrl(url) {
    if (!url) return "";
    if (/^https?:\/\//i.test(url)) return url;
    return BACKEND_ORIGIN + (url.startsWith("/") ? url : "/" + url);
}

function value(id) {
    return (document.getElementById(id)?.value ?? "").toString().trim();
}

function setValue(id, newValue) {
    const element = document.getElementById(id);
    if (element) element.value = newValue ?? "";
}

function clearErrors() {
    if (phoneError) phoneError.textContent = "";
    clearAvatarError();
}

function clearAvatarError() {
    if (avatarError) avatarError.textContent = "";
}

function setAvatarError(message) {
    if (avatarError) avatarError.textContent = message || "";
}

function showAlert(message, type = "success") {
    if (profileAlert) {
        profileAlert.hidden = false;
        profileAlert.className = `profile-alert alert-${type}`;

        if (alertIcon) {
            alertIcon.innerHTML = type === "success"
                ? `<svg viewBox="0 0 24 24"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>`
                : `<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`;
        }

        if (alertText) alertText.textContent = message;
    }

    if (profileMessage) {
        profileMessage.textContent = message;
        profileMessage.style.color = type === "success" ? "#059669" : "#dc2626";
    }
}

function clearAlert() {
    if (profileAlert) profileAlert.hidden = true;
    if (profileMessage) profileMessage.textContent = "";
}

// Backwards compatibility
function setMessage(message, error = false) {
    if (!message) {
        clearAlert();
        return;
    }
    showAlert(message, error ? "danger" : "success");
}