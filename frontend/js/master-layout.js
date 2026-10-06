"use strict";

const body =
    document.body;

const collapseButton =
    document.getElementById(
        "sidebarCollapse"
    );

const navItems =
    document.querySelectorAll(
        ".sidebar .nav-item"
    );

const navParents =
    document.querySelectorAll(
        ".nav-parent"
    );

const userMenuButton =
    document.getElementById(
        "userMenuButton"
    );

const userDropdown =
    document.getElementById(
        "userDropdown"
    );

const mobileMenuButton =
    document.getElementById(
        "mobileMenuButton"
    );

const mobileDrawer =
    document.getElementById(
        "mobileDrawer"
    );

const drawerOverlay =
    document.getElementById(
        "drawerOverlay"
    );

const bottomItems =
    document.querySelectorAll(
        ".bottom-item"
    );


collapseButton.addEventListener(
    "click",
    () => {

        body.classList.toggle(
            "sidebar-collapsed"
        );

    }
);


navItems.forEach(item => {

    if (
        !item.classList.contains(
            "nav-parent"
        )
    ) {

        item.addEventListener(
            "click",
            event => {

                event.preventDefault();

                navItems.forEach(i => {
                    i.classList.remove(
                        "active"
                    );
                });

                item.classList.add(
                    "active"
                );

            }
        );

    }

});


navParents.forEach(parent => {

    parent.addEventListener(
        "click",
        () => {

            const subnav =
                parent.nextElementSibling;

            if (
                subnav
                && subnav.classList.contains(
                    "subnav"
                )
            ) {
                subnav.classList.toggle(
                    "open"
                );
            }

        }
    );

});


userMenuButton.addEventListener(
    "click",
    event => {

        event.stopPropagation();

        userDropdown.classList.toggle(
            "open"
        );

    }
);


document.addEventListener(
    "click",
    event => {

        if (
            !userDropdown.contains(
                event.target
            )
        ) {
            userDropdown.classList.remove(
                "open"
            );
        }

    }
);


mobileMenuButton.addEventListener(
    "click",
    () => {

        mobileDrawer.classList.add(
            "open"
        );

        drawerOverlay.classList.add(
            "open"
        );

    }
);


drawerOverlay.addEventListener(
    "click",
    closeDrawer
);


document.addEventListener(
    "keydown",
    event => {

        if (
            event.key === "Escape"
        ) {
            closeDrawer();

            userDropdown.classList.remove(
                "open"
            );
        }

    }
);


bottomItems.forEach(item => {

    item.addEventListener(
        "click",
        event => {

            event.preventDefault();

            bottomItems.forEach(i => {
                i.classList.remove(
                    "active"
                );
            });

            item.classList.add(
                "active"
            );

        }
    );

});


function closeDrawer() {

    mobileDrawer.classList.remove(
        "open"
    );

    drawerOverlay.classList.remove(
        "open"
    );

}