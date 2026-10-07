"use strict";

let currentDate =
    new Date();

currentDate.setDate(
    1
);


const modal =
    document.getElementById(
        "calendarModal"
    );

const overlay =
    document.getElementById(
        "calendarOverlay"
    );


document
    .getElementById(
        "previousMonth"
    )
    .addEventListener(
        "click",
        () => {

            currentDate.setMonth(
                currentDate.getMonth()
                -
                1
            );

            render();

        }
    );


document
    .getElementById(
        "nextMonth"
    )
    .addEventListener(
        "click",
        () => {

            currentDate.setMonth(
                currentDate.getMonth()
                +
                1
            );

            render();

        }
    );


document
    .getElementById(
        "todayButton"
    )
    .addEventListener(
        "click",
        () => {

            currentDate =
                new Date();

            currentDate.setDate(
                1
            );

            render();

        }
    );


document
    .getElementById(
        "calendarTypeFilter"
    )
    .addEventListener(
        "change",
        render
    );


document
    .getElementById(
        "closeCalendarModal"
    )
    .addEventListener(
        "click",
        closeModal
    );


overlay.addEventListener(
    "click",
    closeModal
);


document.addEventListener(
    "click",
    event => {

        const item =
            event.target.closest(
                "[data-calendar-event]"
            );


        if (!item) {
            return;
        }


        const activities =
            loadActivities();


        const activity =
            activities.find(
                row =>
                    row.id
                    ===
                    item.dataset.calendarEvent
            );


        if (
            activity
        ) {

            openModal(
                activity
            );
        }

    }
);


function render() {

    const year =
        currentDate.getFullYear();


    const month =
        currentDate.getMonth();


    document
        .getElementById(
            "calendarTitle"
        )
        .textContent =
            `Tháng ${month + 1} / ${year}`;


    const grid =
        document.getElementById(
            "calendarGrid"
        );


    grid.innerHTML = "";


    const first =
        new Date(
            year,
            month,
            1
        );


    /*
     * JS:
     * 0 = Sunday
     * Calendar:
     * Monday first
     */
    const offset =
        (
            first.getDay()
            +
            6
        )
        %
        7;


    const start =
        new Date(
            year,
            month,
            1 - offset
        );


    const activities =
        loadActivities();


    const typeFilter =
        document
            .getElementById(
                "calendarTypeFilter"
            )
            .value;


    for (
        let i = 0;
        i < 42;
        i++
    ) {

        const day =
            new Date(
                start
            );


        day.setDate(
            start.getDate()
            +
            i
        );


        const dateKey =
            toDateKey(
                day
            );


        const items =
            activities.filter(
                item =>
                    item.date
                    === dateKey
                    &&
                    (
                        !typeFilter
                        ||
                        item.type
                        === typeFilter
                    )
            );


        const cell =
            document.createElement(
                "article"
            );


        cell.className =
            "calendar-day";


        if (
            day.getMonth()
            !== month
        ) {

            cell.classList.add(
                "other-month"
            );

        }


        if (
            dateKey
            ===
            toDateKey(
                new Date()
            )
        ) {

            cell.classList.add(
                "today"
            );

        }


        cell.innerHTML = `
            <div class="day-number">
                ${day.getDate()}
            </div>

            <div class="day-events"></div>
        `;


        const box =
            cell.querySelector(
                ".day-events"
            );


        for (
            const item
            of items.slice(
                0,
                4
            )
        ) {

            const button =
                document.createElement(
                    "button"
                );


            button.type =
                "button";


            button.className =
                `calendar-event ${item.type}`;


            button.dataset.calendarEvent =
                item.id;


            button.textContent =
                (
                    item.time
                    ?
                    item.time
                    +
                    " "
                    :
                    ""
                )
                +
                item.title;


            box.appendChild(
                button
            );

        }


        if (
            items.length > 4
        ) {

            const more =
                document.createElement(
                    "small"
                );


            more.textContent =
                `+${items.length - 4} hoạt động`;


            box.appendChild(
                more
            );

        }


        grid.appendChild(
            cell
        );

    }

}


function openModal(
    item
) {

    text(
        "calendarModalTitle",
        item.title
    );


    text(
        "detailActivityType",
        typeLabel(
            item.type
        )
    );


    text(
        "detailActivityRelation",
        item.relation
        ||
        "Không liên kết"
    );


    text(
        "detailActivityDate",
        (
            item.date
            ||
            "Chưa đặt lịch"
        )
        +
        (
            item.time
            ?
            " · "
            +
            item.time
            :
            ""
        )
    );


    text(
        "detailActivityOwner",
        item.owner
        ||
        "Chưa phân công"
    );


    text(
        "detailActivityDescription",
        item.description
        ||
        "Không có ghi chú."
    );


    modal.classList.add(
        "open"
    );

    overlay.classList.add(
        "open"
    );

}


function closeModal() {

    modal.classList.remove(
        "open"
    );

    overlay.classList.remove(
        "open"
    );

}


function loadActivities() {

    try {

        return JSON.parse(
            localStorage.getItem(
                "crm_ui_activities"
            )
        )
        ||
        [];

    } catch {

        return [];
    }

}


function typeLabel(type) {

    switch (type) {

        case "call":
            return "Cuộc gọi";

        case "email":
            return "Email";

        case "meeting":
            return "Cuộc họp";

        case "task":
            return "Công việc";

        default:
            return "Ghi chú";
    }

}


function toDateKey(date) {

    return (
        date.getFullYear()
        +
        "-"
        +
        String(
            date.getMonth() + 1
        ).padStart(
            2,
            "0"
        )
        +
        "-"
        +
        String(
            date.getDate()
        ).padStart(
            2,
            "0"
        )
    );

}


function text(
    id,
    value
) {

render();

(async function syncCalendarActivities() {
    try {
        const res = await fetch("http://localhost:8080/crm/api/activities", { credentials: "include" });
        const json = await res.json();
        if (json?.success && Array.isArray(json.data)) {
            const list = json.data.map(item => {
                let d = "";
                let t = "";
                if (item.dueDate) {
                    const dt = new Date(item.dueDate);
                    if (!isNaN(dt.getTime())) {
                        d = dt.toISOString().split("T")[0];
                        t = dt.toTimeString().slice(0, 5);
                    }
                }
                return {
                    id: item.id,
                    type: (item.type || "meeting").toLowerCase(),
                    title: item.subject || item.title || "Lịch hẹn",
                    relation: item.customerName || item.opportunityName || item.relation || "",
                    date: d,
                    time: t,
                    status: (item.status || "OPEN").toUpperCase() === "COMPLETED" ? "done" : "open"
                };
            });
            localStorage.setItem("crm_ui_activities", JSON.stringify(list));
            render();
        }
    } catch (_) {}
})();