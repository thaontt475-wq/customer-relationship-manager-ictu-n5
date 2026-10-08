"use strict";

const API_BASE =
    "http://localhost:8080/crm";

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

function showError(message) {

    let error =
        document.getElementById("forgotError");

    if (!error) {

        error =
            document.createElement("div");

        error.id =
            "forgotError";

        error.style.color =
            "#dc2626";

        error.style.marginTop =
            "10px";

        error.style.fontSize =
            "13px";

        form?.appendChild(error);
    }

    error.textContent =
        message;
}

function clearError() {

    const error =
        document.getElementById("forgotError");

    if (error) {
        error.textContent = "";
    }
}

function setLoading(loading) {

    if (!submitButton) {
        return;
    }

    if (!submitButton.dataset.originalText) {
        submitButton.dataset.originalText =
            submitButton.textContent.trim();
    }

    submitButton.disabled =
        loading;

    submitButton.textContent =
        loading
            ? "Đang gửi..."
            : submitButton.dataset.originalText;
}

form?.addEventListener(
    "submit",
    async event => {

        event.preventDefault();

        clearError();

        const email =
            emailInput?.value.trim() || "";

        if (!email) {

            showError(
                "Vui lòng nhập email."
            );

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
                            "Content-Type":
                                "application/json",
                            "Accept":
                                "application/json"
                        },
                        body:
                            JSON.stringify({
                                email
                            })
                    }
                );

            let result = null;

            try {
                result =
                    await response.json();
            } catch (_) {
                result = null;
            }

            if (!response.ok) {

                showError(
                    result?.message ||
                    "Không thể gửi yêu cầu đặt lại mật khẩu."
                );

                return;
            }

            formState.hidden =
                true;

            successState.hidden =
                false;

        } catch (error) {

            console.error(
                "Forgot password API error:",
                error
            );

            showError(
                "Không kết nối được tới máy chủ."
            );

        } finally {

            setLoading(false);

        }

    }
);