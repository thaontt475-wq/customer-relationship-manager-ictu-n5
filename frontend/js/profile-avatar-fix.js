"use strict";

(() => {

    const API_BASE =
        "http://localhost:8080/crm";

    const BACKEND_ORIGIN =
        "http://localhost:8080";


    function absoluteAvatarUrl(url) {

        if (!url) {
            return "";
        }

        const absolute =
            /^https?:\/\//i.test(url)
                ? url
                : BACKEND_ORIGIN +
                  (
                      url.startsWith("/")
                          ? url
                          : "/" + url
                  );

        /*
         * Chống browser cache ảnh cũ.
         */
        const separator =
            absolute.includes("?")
                ? "&"
                : "?";

        return absolute +
            separator +
            "v=" +
            Date.now();
    }


    function showAvatar(
        avatarUrl,
        thumbnailUrl
    ) {

        const image =
            document.getElementById(
                "profileAvatarImage"
            );

        const empty =
            document.getElementById(
                "profileAvatarEmpty"
            );

        const thumbnail =
            document.getElementById(
                "avatarThumbnail"
            );

        const thumbnailSection =
            document.getElementById(
                "thumbnailSection"
            );


        if (avatarUrl) {

            if (image) {

                image.src =
                    absoluteAvatarUrl(
                        avatarUrl
                    );

                image.hidden =
                    false;

                image.style.display =
                    "block";


                image.onload =
                    () => {

                        image.hidden =
                            false;

                        image.style.display =
                            "block";

                        if (empty) {

                            empty.hidden =
                                true;

                            empty.style.display =
                                "none";
                        }
                    };


                image.onerror =
                    error => {

                        console.error(
                            "Không load được avatar:",
                            image.src,
                            error
                        );
                    };
            }


            if (empty) {

                empty.hidden =
                    true;

                empty.style.display =
                    "none";
            }

        } else {

            if (image) {

                image.hidden =
                    true;

                image.style.display =
                    "none";
            }


            if (empty) {

                empty.hidden =
                    false;

                empty.style.display =
                    "";
            }
        }


        if (
            thumbnail &&
            thumbnailUrl
        ) {

            thumbnail.src =
                absoluteAvatarUrl(
                    thumbnailUrl
                );

            thumbnail.hidden =
                false;
        }


        if (thumbnailSection) {

            thumbnailSection.hidden =
                !thumbnailUrl;
        }
    }


    async function loadAvatarFromBackend() {

        try {

            const response =
                await fetch(
                    API_BASE +
                    "/api/profile",
                    {
                        credentials:
                            "include",

                        cache:
                            "no-store"
                    }
                );


            const result =
                await response.json();


            if (
                !response.ok ||
                !result.success
            ) {

                console.error(
                    "Không đọc được profile:",
                    result
                );

                return;
            }


            showAvatar(
                result.data?.avatarUrl,
                result.data?.avatarThumbnailUrl
            );


        } catch (error) {

            console.error(
                "Load avatar error:",
                error
            );
        }
    }


    /*
     * Sau khi profile page load:
     * lấy avatar thật từ DB.
     */
    document.addEventListener(
        "DOMContentLoaded",
        () => {

            setTimeout(
                loadAvatarFromBackend,
                300
            );
        }
    );


    /*
     * Quan sát request upload của profile.js.
     * Sau khi người dùng bấm "Áp dụng ảnh",
     * chờ backend lưu xong rồi GET profile lại.
     */
    document
        .getElementById(
            "applyAvatar"
        )
        ?.addEventListener(
            "click",
            () => {

                /*
                 * profile.js thực hiện upload.
                 * Chờ một chút rồi reload dữ liệu DB.
                 */
                setTimeout(
                    loadAvatarFromBackend,
                    1200
                );

                setTimeout(
                    loadAvatarFromBackend,
                    2500
                );
            }
        );


    /*
     * Hook cho shared-shell/profile.js gọi trực tiếp.
     */
    window.crmReloadProfileAvatar =
        loadAvatarFromBackend;

})();