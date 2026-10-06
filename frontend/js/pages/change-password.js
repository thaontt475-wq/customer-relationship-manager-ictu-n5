"use strict";

const API_BASE =
    "http://localhost:8080/crm";

const form =
    document.getElementById(
        "changePasswordForm"
    );

const currentPassword =
    document.getElementById(
        "currentPassword"
    );

const newPassword =
    document.getElementById(
        "newPassword"
    );

const confirmPassword =
    document.getElementById(
        "confirmPassword"
    );

const passwordError =
    document.getElementById(
        "passwordError"
    );

const passwordMessage =
    document.getElementById(
        "passwordMessage"
    );


/* =========================================================
   SHOW / HIDE PASSWORD
========================================================= */

document
    .querySelectorAll(
        "[data-password-toggle]"
    )
    .forEach(button => {

        button.addEventListener(
            "click",
            () => {

                const input =
                    document.getElementById(
                        button.dataset.passwordToggle
                    );

                if (!input) {
                    return;
                }

                const showing =
                    input.type === "text";

                input.type =
                    showing
                        ? "password"
                        : "text";

                button.setAttribute(
                    "aria-label",
                    showing
                        ? "Hiện mật khẩu"
                        : "Ẩn mật khẩu"
                );
            }
        );
    });


/* =========================================================
   PASSWORD RULES
========================================================= */

newPassword?.addEventListener(
    "input",
    updateRules
);

function updateRules() {

    if (!newPassword) {
        return;
    }

    const value =
        newPassword.value;

    updateRule(
        "ruleLength",
        value.length >= 8,
        "Ít nhất 8 ký tự"
    );

    updateRule(
        "ruleLetter",
        /[A-Z]/.test(value) &&
        /[a-z]/.test(value),
        "Có chữ hoa và chữ thường"
    );

    updateRule(
        "ruleNumber",
        /\d/.test(value),
        "Có chữ số"
    );
}


function updateRule(
    id,
    valid,
    label
) {

    const element =
        document.getElementById(id);

    if (!element) {
        return;
    }

    element.classList.toggle(
        "valid",
        valid
    );

    element.textContent =
        `${valid ? "✓" : "○"} ${label}`;
}


/* =========================================================
   HELPERS
========================================================= */

function showError(message) {

    if (passwordError) {
        passwordError.textContent =
            message;
    }

    if (passwordMessage) {
        passwordMessage.textContent =
            "";
    }
}


function showSuccess(message) {

    if (passwordMessage) {
        passwordMessage.textContent =
            message;
    }

    if (passwordError) {
        passwordError.textContent =
            "";
    }
}


/* =========================================================
   SUBMIT
========================================================= */

form?.addEventListener(
    "submit",
    async event => {

        event.preventDefault();

        showError("");

        const current =
            currentPassword?.value || "";

        const next =
            newPassword?.value || "";

        const confirm =
            confirmPassword?.value || "";

        if (
            !current ||
            !next ||
            !confirm
        ) {

            showError(
                "Vui lòng nhập đầy đủ các trường mật khẩu."
            );

            return;
        }

        if (
            next.length < 8 ||
            !/[A-Z]/.test(next) ||
            !/[a-z]/.test(next) ||
            !/\d/.test(next)
        ) {

            showError(
                "Mật khẩu mới phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số."
            );

            return;
        }

        if (next !== confirm) {

            showError(
                "Mật khẩu xác nhận không khớp."
            );

            return;
        }

        if (current === next) {

            showError(
                "Mật khẩu mới phải khác mật khẩu hiện tại."
            );

            return;
        }

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const originalText =
            button?.textContent || "";

        if (button) {

            button.disabled =
                true;

            button.textContent =
                "Đang cập nhật...";
        }

        try {

            const response =
                await fetch(
                    `${API_BASE}/api/auth/change-password`,
                    {
                        method: "POST",

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
                                currentPassword:
                                    current,

                                newPassword:
                                    next,

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

                result =
                    null;
            }

            if (response.status === 401) {

                showError(
                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
                );

                setTimeout(
                    () => {
                        window.location.href =
                            "login.html";
                    },
                    1000
                );

                return;
            }

            if (!response.ok) {

                showError(
                    result?.message ||
                    `Không thể đổi mật khẩu. HTTP ${response.status}`
                );

                return;
            }

            if (!result?.success) {

                showError(
                    result?.message ||
                    "Không thể đổi mật khẩu."
                );

                return;
            }

            showSuccess(
                result.message ||
                "Đổi mật khẩu thành công. Vui lòng đăng nhập lại."
            );

            /*
             * Backend đã invalidate HttpSession.
             * FE chỉ dọn cache UI.
             */
            localStorage.removeItem(
                "crm_ui_session"
            );

            sessionStorage.clear();

            form.reset();

            updateRules();

            setTimeout(
                () => {

                    window.location.href =
                        "login.html";

                },
                1200
            );

        } catch (error) {

            console.error(
                "Change password API error:",
                error
            );

            showError(
                "Không kết nối được tới máy chủ."
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
);


updateRules();