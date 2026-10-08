"use strict";

document.addEventListener("DOMContentLoaded", () => {

    /*
     * Desktop không dùng chế độ sidebar collapsed nữa.
     */
    document.body.classList.remove("sidebar-collapsed");

    localStorage.removeItem(
        "crm_ui_sidebar_collapsed"
    );

    setupUserDropdown();
    setupMobileDrawer();
    setupGlobalSearch();
    setupGlobalAvatar();
    setupActiveNavigation();

});


/* =========================================================
   USER DROPDOWN
========================================================= */

function setupUserDropdown() {

    const button =
        document.getElementById(
            "userButton"
        );

    const dropdown =
        document.getElementById(
            "userDropdown"
        );

    if (
        !button
        ||
        !dropdown
    ) {
        return;
    }


    button.addEventListener(
        "click",
        event => {

            event.stopPropagation();

            dropdown.classList.toggle(
                "open"
            );

        }
    );


    document.addEventListener(
        "click",
        event => {

            if (
                !dropdown.contains(event.target)
                &&
                !button.contains(event.target)
            ) {

                dropdown.classList.remove(
                    "open"
                );

            }

        }
    );

}


/* =========================================================
   MOBILE DRAWER
   Chỉ mobile mới click ngoài để đóng.
========================================================= */

function setupMobileDrawer() {

    const button =
        document.getElementById(
            "mobileMenuButton"
        );

    const drawer =
        document.getElementById(
            "mobileDrawer"
        );

    const overlay =
        document.getElementById(
            "drawerOverlay"
        );


    if (
        !button
        ||
        !drawer
    ) {
        return;
    }


    function openDrawer() {

        if (
            window.innerWidth > 768
        ) {
            return;
        }

        drawer.classList.add(
            "open"
        );

        overlay?.classList.add(
            "open"
        );

        document.body.classList.add(
            "mobile-menu-open"
        );

    }


    function closeDrawer() {

        drawer.classList.remove(
            "open"
        );

        overlay?.classList.remove(
            "open"
        );

        document.body.classList.remove(
            "mobile-menu-open"
        );

    }


    button.addEventListener(
        "click",
        event => {

            event.stopPropagation();

            if (
                drawer.classList.contains(
                    "open"
                )
            ) {

                closeDrawer();

            } else {

                openDrawer();

            }

        }
    );


    /*
     * Click overlay => đóng.
     */
    overlay?.addEventListener(
        "click",
        closeDrawer
    );


    /*
     * Click bất kỳ nơi nào ngoài drawer
     * chỉ áp dụng mobile.
     */
    document.addEventListener(
        "click",
        event => {

            if (
                window.innerWidth > 768
            ) {
                return;
            }


            if (
                !drawer.classList.contains(
                    "open"
                )
            ) {
                return;
            }


            if (
                drawer.contains(
                    event.target
                )
                ||
                button.contains(
                    event.target
                )
            ) {
                return;
            }


            closeDrawer();

        }
    );


    /*
     * Chọn menu mobile xong cũng đóng drawer.
     */
    drawer.addEventListener(
        "click",
        event => {

            const link =
                event.target.closest(
                    "a[href]"
                );

            if (link) {

                closeDrawer();

            }

        }
    );


    /*
     * Escape => đóng drawer.
     */
    document.addEventListener(
        "keydown",
        event => {

            if (
                event.key === "Escape"
                &&
                window.innerWidth <= 768
            ) {

                closeDrawer();

            }

        }
    );


    /*
     * Nếu resize từ mobile -> desktop
     * thì đóng drawer mobile.
     */
    window.addEventListener(
        "resize",
        () => {

            if (
                window.innerWidth > 768
            ) {

                closeDrawer();

            }

        }
    );

}


/* =========================================================
   GLOBAL SEARCH
========================================================= */

function setupGlobalSearch() {

    document.addEventListener(
        "keydown",
        event => {

            if (
                (
                    event.ctrlKey
                    ||
                    event.metaKey
                )
                &&
                event.key.toLowerCase()
                === "k"
            ) {

                event.preventDefault();

                document
                    .getElementById(
                        "globalSearch"
                    )
                    ?.focus();

            }

        }
    );

}


/* =========================================================
   GLOBAL AVATAR
========================================================= */

function setupGlobalAvatar() {

    updateGlobalAvatar();


    window.addEventListener(
        "storage",
        event => {

            if (
                event.key
                ===
                "crm_ui_profile_avatar"
            ) {

                updateGlobalAvatar();

            }

        }
    );

}


function updateGlobalAvatar() {

    const avatar =
        localStorage.getItem(
            "crm_ui_profile_avatar"
        );


    if (!avatar) {
        return;
    }


    document
        .querySelectorAll(
            ".crm-avatar"
        )
        .forEach(
            element => {

                element.textContent = "";

                element.style.backgroundImage =
                    `url("${avatar}")`;

                element.style.backgroundSize =
                    "cover";

                element.style.backgroundPosition =
                    "center";

                element.style.backgroundRepeat =
                    "no-repeat";

            }
        );

}


window.crmUpdateGlobalAvatar =
    updateGlobalAvatar;


/* =========================================================
   ACTIVE NAV
========================================================= */

function setupActiveNavigation() {

    const currentFile =
        window.location.pathname
            .split("/")
            .pop()
        ||
        "dashboard.html";


    document
        .querySelectorAll(
            ".crm-nav-item"
        )
        .forEach(
            item => {

                const href =
                    item.getAttribute(
                        "href"
                    );


                item.classList.toggle(
                    "active",
                    Boolean(
                        href
                        &&
                        href !== "#"
                        &&
                        href.split("?")[0]
                        === currentFile
                    )
                );

            }
        );


    document
        .querySelectorAll(
            ".crm-bottom-item"
        )
        .forEach(
            item => {

                const href =
                    item.getAttribute(
                        "href"
                    );


                item.classList.toggle(
                    "active",
                    Boolean(
                        href
                        &&
                        href.split("?")[0]
                        === currentFile
                    )
                );

            }
        );

}