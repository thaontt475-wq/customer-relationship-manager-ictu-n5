"use strict";

document.addEventListener(
    "DOMContentLoaded",
    () => {

        requestAnimationFrame(
            () => {

                document.body
                    .classList
                    .add(
                        "crm-page-ready"
                    );

            }
        );


        setupSmoothNavigation();

        preloadInternalPages();

    }
);


/* =========================================================
   SMOOTH NAVIGATION
========================================================= */

function setupSmoothNavigation() {

    document.addEventListener(
        "click",
        event => {

            const link =
                event.target.closest(
                    "a[href]"
                );


            if (!link || event.defaultPrevented || event.button !== 0
                || link.hasAttribute("download")
                || window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
                return;
            }


            const href =
                link.getAttribute(
                    "href"
                );


            if (
                !href
                ||
                href === "#"
                ||
                href.startsWith(
                    "javascript:"
                )
                ||
                href.startsWith(
                    "mailto:"
                )
                ||
                href.startsWith(
                    "tel:"
                )
                ||
                (link.target && link.target !== "_self")
                ||
                event.ctrlKey
                ||
                event.metaKey
                ||
                event.shiftKey
                ||
                event.altKey
            ) {
                return;
            }


            const url =
                new URL(
                    href,
                    window.location.href
                );


            /*
             * Chỉ xử lý link nội bộ cùng origin.
             */
            if (
                url.origin
                !==
                window.location.origin
            ) {
                return;
            }


            /*
             * Nếu chỉ là anchor cùng trang
             * thì để browser xử lý bình thường.
             */
            if (
                url.pathname
                ===
                window.location.pathname
                &&
                url.search
                ===
                window.location.search
                &&
                url.hash
            ) {
                return;
            }


            event.preventDefault();


            document.body
                .classList
                .add(
                    "crm-page-leaving"
                );


            /*
             * Delay rất ngắn để transition có thời gian chạy.
             */
            setTimeout(
                () => {

                    window.location.href =
                        url.href;

                },
                85
            );

        }
    );

}

// A restored page must not retain the fade-out class from its last navigation.
window.addEventListener("pageshow", () => {
    document.body.classList.remove("crm-page-leaving");
});


/* =========================================================
   PRELOAD INTERNAL PAGES
========================================================= */

function preloadInternalPages() {

    const links =
        Array.from(
            document.querySelectorAll(
                'a[href$=".html"]'
            )
        );


    const unique =
        new Set();


    for (
        const link
        of links
    ) {

        const href =
            link.getAttribute(
                "href"
            );


        if (
            !href
            ||
            unique.has(
                href
            )
        ) {
            continue;
        }


        unique.add(
            href
        );


        const preload =
            document.createElement(
                "link"
            );


        preload.rel =
            "prefetch";


        preload.href =
            href;


        document.head
            .appendChild(
                preload
            );

    }

}
