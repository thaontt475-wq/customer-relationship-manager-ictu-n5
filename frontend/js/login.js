document.addEventListener("DOMContentLoaded", () => {

    const form =
        document.getElementById("loginForm");

    const email =
        document.getElementById("email");

    const password =
        document.getElementById("password");

    const togglePassword =
        document.getElementById("togglePassword");

    const submitButton =
        form?.querySelector('button[type="submit"]');

    const API_BASE =
        "http://localhost:8080/crm";


    /* =========================
       SHOW / HIDE PASSWORD
    ========================= */

    togglePassword?.addEventListener(
        "click",
        () => {

            const hidden =
                password.type === "password";

            password.type =
                hidden
                    ? "text"
                    : "password";

            togglePassword.setAttribute(
                "aria-pressed",
                hidden
                    ? "true"
                    : "false"
            );

        }
    );


    /* =========================
       ERROR HELPERS
    ========================= */

    function findMessageBox() {

        return (
            document.getElementById("loginError") ||
            document.getElementById("loginMessage") ||
            document.querySelector(".login-error") ||
            document.querySelector(".form-error")
        );

    }


    function showError(message) {

        const box =
            findMessageBox();

        if (box) {

            box.textContent =
                message;

            box.hidden =
                false;

            box.style.display =
                "";

        } else {

            alert(message);

        }

    }


    function clearError() {

        const box =
            findMessageBox();

        if (box) {

            box.textContent =
                "";

            box.hidden =
                true;

        }

    }


    function setLoading(loading) {

        if (!submitButton) {
            return;
        }

        submitButton.disabled =
            loading;

        if (
            !submitButton.dataset.originalText
        ) {

            submitButton.dataset.originalText =
                submitButton.textContent.trim();

        }

        submitButton.textContent =
            loading
                ? "Đang đăng nhập..."
                : submitButton.dataset.originalText;

    }


    /* =========================
       LOGIN
    ========================= */

    form?.addEventListener(
        "submit",
        async event => {

            event.preventDefault();

            clearError();

            const emailValue =
                email?.value.trim() || "";

            const passwordValue =
                password?.value || "";

            if (!emailValue) {

                showError(
                    "Vui lòng nhập email."
                );

                email?.focus();

                return;
            }

            if (!passwordValue) {

                showError(
                    "Vui lòng nhập mật khẩu."
                );

                password?.focus();

                return;
            }

            setLoading(true);

            try {

                const response =
                    await fetch(
                        `${API_BASE}/api/auth/login`,
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
                                    email:
                                        emailValue,

                                    password:
                                        passwordValue
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


                /* 401 - sai tài khoản */

                if (response.status === 401) {

                    const remaining =
                        result?.data
                            ?.remainingAttempts;

                    let message =
                        result?.message ||
                        "Sai email hoặc mật khẩu.";

                    if (
                        remaining !== undefined &&
                        remaining !== null
                    ) {

                        message +=
                            ` Còn ${remaining} lần thử.`;

                    }

                    showError(message);

                    return;
                }


                /* 423 - account locked */

                if (response.status === 423) {

                    const lockedUntil =
                        result?.data
                            ?.lockedUntil;

                    let message =
                        result?.message ||
                        "Tài khoản đang bị khóa tạm thời.";

                    if (lockedUntil) {

                        const date =
                            new Date(
                                lockedUntil
                            );

                        if (
                            !Number.isNaN(
                                date.getTime()
                            )
                        ) {

                            message +=
                                ` Thử lại sau ${date.toLocaleString("vi-VN")}.`;

                        }

                    }

                    showError(message);

                    return;
                }


                if (!response.ok) {

                    showError(
                        result?.message ||
                        `Không thể đăng nhập. HTTP ${response.status}`
                    );

                    return;
                }


                if (!result?.success) {

                    showError(
                        result?.message ||
                        "Đăng nhập không thành công."
                    );

                    return;
                }


                /* =========================
                   VERIFY SESSION
                ========================= */

                const sessionResponse =
                    await fetch(
                        `${API_BASE}/api/auth/session`,
                        {
                            method:
                                "GET",

                            credentials:
                                "include",

                            headers: {
                                "Accept":
                                    "application/json"
                            }
                        }
                    );

                let sessionResult =
                    null;

                try {

                    sessionResult =
                        await sessionResponse.json();

                } catch (_) {

                    sessionResult =
                        null;

                }


                if (
                    !sessionResponse.ok ||
                    !sessionResult?.success ||
                    !sessionResult?.data
                        ?.authenticated
                ) {

                    showError(
                        "Đăng nhập thành công nhưng không xác nhận được phiên đăng nhập."
                    );

                    return;
                }


                /*
                 * Chỉ cache thông tin session phục vụ UI.
                 * Nguồn xác thực thật vẫn là Backend session.
                 */

                localStorage.setItem(
                    "crm_ui_session",
                    JSON.stringify(
                        sessionResult.data
                    )
                );


                window.location.href =
                    "dashboard.html";

            } catch (error) {

                console.error(
                    "Login API error:",
                    error
                );

                showError(
                    "Không kết nối được tới máy chủ. Kiểm tra Backend Tomcat đang chạy."
                );

            } finally {

                setLoading(false);

            }

        }
    );

});