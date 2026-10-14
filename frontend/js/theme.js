"use strict";

(function () {

    const KEY = "crm_ui_theme";

    function getTheme() {

        const saved =
            localStorage.getItem(KEY);

        return saved === "dark"
            ? "dark"
            : "light";
    }


    function applyTheme(theme) {

        const normalized =
            theme === "dark"
                ? "dark"
                : "light";


        document.documentElement
            .setAttribute(
                "data-theme",
                normalized
            );


        /*
         * XÓA chuẩn cũ để không còn xung đột.
         */
        document.documentElement
            .removeAttribute(
                "data-crm-theme"
            );


        localStorage.setItem(
            KEY,
            normalized
        );


        updateButton(normalized);
    }


    function updateButton(theme) {

        const button =
            document.getElementById(
                "themeToggle"
            );

        if (!button) {
            return;
        }

        const isDark =
            theme === "dark";

        button.innerHTML =
            isDark
                ? '<i class="fa-solid fa-sun theme-icon-sun" aria-hidden="true"></i>'
                : '<i class="fa-solid fa-moon theme-icon-moon" aria-hidden="true"></i>';

        const label =
            isDark
                ? "Chuyển sang giao diện sáng"
                : "Chuyển sang giao diện tối";

        button.title = label;
        button.setAttribute("aria-label", label);
    }


    function toggleTheme() {

        const current =
            getTheme();

        applyTheme(
            current === "dark"
                ? "light"
                : "dark"
        );
    }


    /*
     * Áp dụng ngay.
     */
    applyTheme(
        getTheme()
    );


    /*
     * Capture phase để chặn mọi listener theme cũ
     * trong shared-shell.js / ui-core.js.
     */
    document.addEventListener(
        "click",
        function (event) {

            const button =
                event.target.closest(
                    "#themeToggle"
                );

            if (!button) {
                return;
            }


            event.preventDefault();
            event.stopImmediatePropagation();


            toggleTheme();

        },
        true
    );


    document.addEventListener(
        "DOMContentLoaded",
        function () {

            applyTheme(
                getTheme()
            );
        }
    );


    window.crmToggleTheme =
        toggleTheme;

    window.crmApplyTheme =
        applyTheme;

})();