"use strict";

/*
 * Prototype frontend-only.
 * Không fetch.
 * Không backend.
 * Nội dung chỉ sinh từ thao tác người dùng.
 */

const timeline = [];

const timelineList =
    document.getElementById("timelineList");

const timelineEmpty =
    document.getElementById("timelineEmpty");

const composer =
    document.getElementById("activityComposer");

const noteInput =
    document.getElementById("activityNote");

const typeInput =
    document.getElementById("activityType");

const modal =
    document.getElementById("companyModal");

const modalOverlay =
    document.getElementById("companyModalOverlay");


document
    .querySelectorAll(
        ".right-tab"
    )
    .forEach(tab => {

        tab.addEventListener(
            "click",
            () => {

                document
                    .querySelectorAll(
                        ".right-tab"
                    )
                    .forEach(item => {
                        item.classList.remove(
                            "active"
                        );
                    });

                tab.classList.add(
                    "active"
                );

                const target =
                    tab.dataset.tab;

                document.getElementById(
                    "contactsPanel"
                ).hidden =
                    target !== "contacts";

                document.getElementById(
                    "opportunitiesPanel"
                ).hidden =
                    target !== "opportunities";

            }
        );

    });


composer.addEventListener(
    "submit",
    event => {

        event.preventDefault();

        const text =
            noteInput.value.trim();

        if (!text) {
            noteInput.focus();
            return;
        }

        timeline.unshift({
            type: typeInput.value,
            text,
            time: new Date()
        });

        noteInput.value = "";

        renderTimeline();

    }
);


document
    .querySelectorAll(
        "[data-quick]"
    )
    .forEach(button => {

        button.addEventListener(
            "click",
            () => {

                const type =
                    button.dataset.quick;

                typeInput.value =
                    type === "call"
                        ? "call"
                        : type === "email"
                            ? "email"
                            : "note";

                noteInput.focus();

            }
        );

    });


document
    .getElementById(
        "editCompanyButton"
    )
    .addEventListener(
        "click",
        openCompanyModal
    );


document
    .getElementById(
        "closeCompanyModal"
    )
    .addEventListener(
        "click",
        closeCompanyModal
    );


document
    .getElementById(
        "cancelCompanyEdit"
    )
    .addEventListener(
        "click",
        closeCompanyModal
    );


modalOverlay.addEventListener(
    "click",
    closeCompanyModal
);


document
    .getElementById(
        "company360Form"
    )
    .addEventListener(
        "submit",
        event => {

            event.preventDefault();

            writeDisplay(
                "company360Name",
                value("edit360Name")
            );

            writeDisplay(
                "company360Tax",
                value("edit360Tax")
            );

            writeDisplay(
                "company360Industry",
                value("edit360Industry")
            );

            writeDisplay(
                "company360Phone",
                value("edit360Phone")
            );

            writeDisplay(
                "company360Website",
                value("edit360Website")
            );

            closeCompanyModal();

        }
    );


document.addEventListener(
    "keydown",
    event => {

        if (event.key === "Escape") {
            closeCompanyModal();
        }

    }
);


function renderTimeline() {

    timelineList
        .querySelectorAll(
            ".timeline-item"
        )
        .forEach(item => item.remove());


    timelineEmpty.style.display =
        timeline.length
            ? "none"
            : "block";


    for (const item of timeline) {

        const article =
            document.createElement("article");

        article.className =
            "timeline-item";

        article.innerHTML = `
            <div class="timeline-item-head">

                <span class="timeline-type">
                    ${typeLabel(item.type)}
                </span>

                <time class="timeline-time">
                    ${item.time.toLocaleTimeString(
                        "vi-VN",
                        {
                            hour: "2-digit",
                            minute: "2-digit"
                        }
                    )}
                </time>

            </div>

            <div class="timeline-text">
                ${escapeHtml(item.text)}
            </div>
        `;

        timelineList.appendChild(
            article
        );

    }

}


function openCompanyModal() {

    setValue(
        "edit360Name",
        displayValue("company360Name")
    );

    setValue(
        "edit360Tax",
        displayValue("company360Tax")
    );

    setValue(
        "edit360Industry",
        displayValue("company360Industry")
    );

    setValue(
        "edit360Phone",
        displayValue("company360Phone")
    );

    setValue(
        "edit360Website",
        displayValue("company360Website")
    );

    modal.classList.add(
        "open"
    );

    modalOverlay.classList.add(
        "open"
    );

}


function closeCompanyModal() {

    modal.classList.remove(
        "open"
    );

    modalOverlay.classList.remove(
        "open"
    );

}


function typeLabel(type) {

    switch (type) {

        case "call":
            return "☎ Cuộc gọi";

        case "email":
            return "✉ Email";

        case "meeting":
            return "▣ Cuộc gặp";

        default:
            return "✎ Ghi chú";

    }

}


function value(id) {

    return document
        .getElementById(id)
        .value
        .trim();

}


function displayValue(id) {

    const value =
        document
            .getElementById(id)
            .textContent
            .trim();

    return value === "—"
        ? ""
        : value;

}


function setValue(
    id,
    value
) {

    document
        .getElementById(id)
        .value =
            value || "";

}


function writeDisplay(
    id,
    value
) {

    document
        .getElementById(id)
        .textContent =
            value || "—";

}


function escapeHtml(value) {

    return String(value)
        .replace(/&/g,"&amp;")
        .replace(/</g,"&lt;")
        .replace(/>/g,"&gt;")
        .replace(/"/g,"&quot;")
        .replace(/'/g,"&#039;");

}