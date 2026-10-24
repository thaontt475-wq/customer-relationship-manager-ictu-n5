"use strict";

const API_BASE =
    `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;

const form =
    document.getElementById("forgotPasswordForm");

const emailInput =
    document.getElementById("forgotEmail");

const formState =
    document.getElementById("forgotFormState");

const successState =
    document.getElementById("successState");

const submitButton =
    form?.querySelector('button[type="submit"]');

const targetEmailDisplay =
    document.getElementById("targetEmailDisplay");

const btnResendEmail =
    document.getElementById("btnResendEmail");

function showError(message) {
    let error =
        document.getElementById("forgotError");

    if (!error) {
        error = document.createElement("div");
        error.id = "forgotError";
        error.className = "auth-alert-box alert-danger";
        form?.prepend(error);
    }

    error.textContent = message;
    error.hidden = false;
}

function clearError() {
    const error =
        document.getElementById("forgotError");

    if (error) {
        error.textContent = "";
        error.hidden = true;
    }
}

function setLoading(loading) {
    if (!submitButton) {
        return;
    }

    if (!submitButton.dataset.originalHtml) {
        submitButton.dataset.originalHtml = submitButton.innerHTML;
    }

    submitButton.disabled = loading;

    if (loading) {
        submitButton.innerHTML = `
            <span class="btn-text">Đang gửi yêu cầu...</span>
            <i class="fa-solid fa-spinner fa-spin btn-icon"></i>
        `;
    } else {
        submitButton.innerHTML = submitButton.dataset.originalHtml;
    }
}

form?.addEventListener(
    "submit",
    async event => {
        event.preventDefault();
        clearError();

        const email =
            emailInput?.value.trim() || "";

        if (!email) {
            showError("Vui lòng nhập địa chỉ email công vụ của bạn.");
            emailInput?.focus();
            return;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            showError("Định dạng email không hợp lệ. Ví dụ: name@corporate-crm.vn");
            emailInput?.focus();
            return;
        }

        setLoading(true);

        try {
            const response =
                await fetch(
                    `${API_BASE}/api/auth/forgot-password`,
                    {
                        method: "POST",
                        credentials: "include",
                        headers: {
                            "Content-Type": "application/json",
                            "Accept": "application/json"
                        },
                        body: JSON.stringify({ email })
                    }
                );

            let result = null;
            try {
                result = await response.json();
            } catch (_) {
                result = null;
            }

            if (!response.ok) {
                showError(
                    result?.message ||
                    "Không thể gửi yêu cầu đặt lại mật khẩu. Vui lòng kiểm tra lại email."
                );
                return;
            }

            if (targetEmailDisplay) {
                targetEmailDisplay.textContent = email;
            }

            formState.hidden = true;
            successState.hidden = false;

        } catch (error) {
            console.error("Forgot password API error:", error);
            showError("Không kết nối được tới máy chủ bảo mật. Vui lòng thử lại sau.");
        } finally {
            setLoading(false);
        }
    }
);

if (btnResendEmail) {
    btnResendEmail.addEventListener("click", () => {
        successState.hidden = true;
        formState.hidden = false;
        clearError();
        emailInput?.focus();
    });
}