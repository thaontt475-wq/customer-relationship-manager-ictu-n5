"use strict";

const API_BASE =
    "http://localhost:8080/crm";

const params =
    new URLSearchParams(
        window.location.search
    );

const token =
    params.get("token");

const form =
    document.getElementById(
        "resetPasswordForm"
    );

const newPassword =
    document.getElementById(
        "newPassword"
    );

const confirmPassword =
    document.getElementById(
        "confirmPassword"
    );

const errorBox =
    document.getElementById(
        "resetError"
    );

const successBox =
    document.getElementById(
        "resetSuccess"
    );

const button =
    form?.querySelector(
        'button[type="submit"]'
    );


function showError(message) {

    errorBox.textContent =
        message;

    successBox.textContent =
        "";
}


if (!token) {

    showError(
        "Liên kết đặt lại mật khẩu không hợp lệ."
    );

    if (button) {
        button.disabled = true;
    }
}


form?.addEventListener(
    "submit",
    async event => {

        event.preventDefault();

        errorBox.textContent =
            "";

        successBox.textContent =
            "";

        const password =
            newPassword.value;

        const confirm =
            confirmPassword.value;

        if (!password || !confirm) {

            showError(
                "Vui lòng nhập đầy đủ mật khẩu."
            );

            return;
        }

        if (password.length < 8) {

            showError(
                "Mật khẩu phải có ít nhất 8 ký tự."
            );

            return;
        }

        if (!/[A-Z]/.test(password)) {

            showError(
                "Mật khẩu phải có ít nhất 1 chữ hoa."
            );

            return;
        }

        if (!/[a-z]/.test(password)) {

            showError(
                "Mật khẩu phải có ít nhất 1 chữ thường."
            );

            return;
        }

        if (!/\d/.test(password)) {

            showError(
                "Mật khẩu phải có ít nhất 1 chữ số."
            );

            return;
        }

        if (password !== confirm) {

            showError(
                "Mật khẩu xác nhận không khớp."
            );

            return;
        }

        button.disabled =
            true;

        const originalText =
            button.textContent;

        button.textContent =
            "Đang cập nhật...";

        try {

            const response =
                await fetch(
                    `${API_BASE}/api/auth/reset-password`,
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
                                token,
                                newPassword:
                                    password,
                                confirmPassword:
                                    confirm
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
                    "Không thể đặt lại mật khẩu."
                );

                return;
            }

            successBox.textContent =
                result?.message ||
                "Đặt lại mật khẩu thành công.";

            form.reset();

            setTimeout(
                () => {
                    window.location.href =
                        "login.html";
                },
                1200
            );

        } catch (error) {

            console.error(
                "Reset password API error:",
                error
            );

            showError(
                "Không kết nối được tới máy chủ."
            );

        } finally {

            button.disabled =
                false;

            button.textContent =
                originalText;

        }

    }
);