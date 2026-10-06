"use strict";

let activities =
    loadActivities();


const drawer =
    document.getElementById(
        "activityDrawer"
    );

const overlay =
    document.getElementById(
        "activityOverlay"
    );


document
    .getElementById(
        "createActivity"
    )
    .addEventListener(
        "click",
        () => openDrawer()
    );


document
    .getElementById(
        "closeActivityDrawer"
    )
    .addEventListener(
        "click",
        closeDrawer
    );


document
    .getElementById(
        "cancelActivity"
    )
    .addEventListener(
        "click",
        closeDrawer
    );


overlay.addEventListener(
    "click",
    closeDrawer
);


[
    "activitySearch",
    "activityTypeFilter",
    "activityStatusFilter"
]
.forEach(
    id => {

        document
            .getElementById(id)
            .addEventListener(
                id === "activitySearch"
                    ?
                    "input"
                    :
                    "change",
                render
            );

    }
);


document
    .getElementById(
        "activityForm"
    )
    .addEventListener(
        "submit",
        event => {

            event.preventDefault();

            saveActivity();

        }
    );


document.addEventListener(
    "click",
    event => {

        const edit =
            event.target.closest(
                "[data-edit-activity]"
            );


        if (edit) {

            openDrawer(
                edit.dataset.editActivity
            );

            return;
        }


        const complete =
            event.target.closest(
                "[data-complete-activity]"
            );


        if (complete) {

            const item =
                activities.find(
                    row =>
                        row.id
                        ===
                        complete.dataset.completeActivity
                );


            if (item) {

                item.status =
                    item.status === "done"
                    ?
                    "open"
                    :
                    "done";


                saveActivities();

                render();
            }

        }

    }
);


function saveActivity() {

    const title =
        value(
            "activityTitle"
        );


    const error =
        document.getElementById(
            "activityError"
        );


    error.textContent = "";


    if (!title) {

        error.textContent =
            "Vui lòng nhập tiêu đề hoạt động.";

        return;
    }


    const editingId =
        value(
            "editingActivityId"
        );


    const existing =
        activities.find(
            item =>
                item.id
                === editingId
        );


    const record = {

        id:
            editingId
            ||
            "act_"
            +
            Date.now(),

        type:
            value(
                "activityType"
            ),

        title,

        relation:
            value(
                "activityRelation"
            ),

        date:
            value(
                "activityDate"
            ),

        time:
            value(
                "activityTime"
            ),

        owner:
            value(
                "activityOwner"
            ),

        description:
            value(
                "activityDescription"
            ),

        reminder:
            document
                .getElementById(
                    "activityReminder"
                )
                .checked,

        status:
            existing
            ?
            existing.status
            :
            "open",

        createdAt:
            existing
            ?
            existing.createdAt
            :
            new Date()
                .toISOString()
    };


    if (editingId) {

        const index =
            activities.findIndex(
                item =>
                    item.id
                    === editingId
            );


        if (
            index >= 0
        ) {

            activities[index] =
                record;
        }

    } else {

        activities.unshift(
            record
        );
    }


    saveActivities();

    closeDrawer();

    render();

}


function render() {

    const query =
        value(
            "activitySearch"
        )
        .toLowerCase();


    const type =
        value(
            "activityTypeFilter"
        );


    const status =
        value(
            "activityStatusFilter"
        );


    const filtered =
        activities.filter(
            item => {

                const text =
                    (
                        item.title
                        +
                        " "
                        +
                        item.relation
                        +
                        " "
                        +
                        item.description
                    )
                    .toLowerCase();


                return (
                    (
                        !query
                        ||
                        text.includes(
                            query
                        )
                    )
                    &&
                    (
                        !type
                        ||
                        item.type
                        === type
                    )
                    &&
                    (
                        !status
                        ||
                        item.status
                        === status
                    )
                );

            }
        );


    const list =
        document.getElementById(
            "activityList"
        );


    list.innerHTML = "";


    document
        .getElementById(
            "activityEmpty"
        )
        .style
        .display =
            filtered.length
            ?
            "none"
            :
            "flex";


    for (
        const item
        of filtered
    ) {

        const article =
            document.createElement(
                "article"
            );


        article.className =
            "activity-row";


        article.innerHTML = `
            <div class="activity-icon ${item.type}">
                ${typeIcon(item.type)}
            </div>

            <div class="activity-main">

                <strong>
                    ${escapeHtml(item.title)}
                </strong>

                <p>
                    ${escapeHtml(item.description || "Không có ghi chú")}
                </p>

            </div>

            <div class="activity-relation">
                ${escapeHtml(item.relation || "Không liên kết")}
            </div>

            <div class="activity-date">
                ${formatSchedule(item)}
            </div>

            <div>

                <span class="activity-status ${statusClass(item)}">
                    ${statusLabel(item)}
                </span>

                <div class="activity-actions">

                    <button
                        type="button"
                        data-edit-activity="${item.id}"
                    >
                        Sửa
                    </button>

                    <button
                        type="button"
                        data-complete-activity="${item.id}"
                    >
                        ${
                            item.status === "done"
                            ?
                            "Mở lại"
                            :
                            "Hoàn thành"
                        }
                    </button>

                </div>

            </div>
        `;


        list.appendChild(
            article
        );

    }


    updateSummary();

}


function updateSummary() {

    const now =
        todayString();


    const open =
        activities.filter(
            item =>
                item.status
                === "open"
        );


    const done =
        activities.filter(
            item =>
                item.status
                === "done"
        );


    const late =
        activities.filter(
            item =>
                item.status
                === "open"
                &&
                item.date
                &&
                item.date
                <
                now
        );


    text(
        "totalActivity",
        activities.length
    );

    text(
        "openActivity",
        open.length
    );

    text(
        "doneActivity",
        done.length
    );

    text(
        "lateActivity",
        late.length
    );

}


function openDrawer(
    id = null
) {

    document
        .getElementById(
            "activityForm"
        )
        .reset();


    setValue(
        "editingActivityId",
        ""
    );


    document
        .getElementById(
            "activityError"
        )
        .textContent = "";


    document
        .getElementById(
            "activityDrawerTitle"
        )
        .textContent =
            "Tạo hoạt động";


    if (id) {

        const item =
            activities.find(
                row =>
                    row.id
                    === id
            );


        if (item) {

            setValue(
                "editingActivityId",
                item.id
            );

            setValue(
                "activityType",
                item.type
            );

            setValue(
                "activityTitle",
                item.title
            );

            setValue(
                "activityRelation",
                item.relation
            );

            setValue(
                "activityDate",
                item.date
            );

            setValue(
                "activityTime",
                item.time
            );

            setValue(
                "activityOwner",
                item.owner
            );

            setValue(
                "activityDescription",
                item.description
            );


            document
                .getElementById(
                    "activityReminder"
                )
                .checked =
                    Boolean(
                        item.reminder
                    );


            document
                .getElementById(
                    "activityDrawerTitle"
                )
                .textContent =
                    "Sửa hoạt động";

        }

    }


    drawer.classList.add(
        "open"
    );

    overlay.classList.add(
        "open"
    );

}


function closeDrawer() {

    drawer.classList.remove(
        "open"
    );

    overlay.classList.remove(
        "open"
    );

}


function statusClass(item) {

    if (
        item.status
        === "done"
    ) {
        return "done";
    }


    if (
        item.date
        &&
        item.date
        <
        todayString()
    ) {
        return "late";
    }


    return "open";
}


function statusLabel(item) {

    if (
        item.status
        === "done"
    ) {
        return "Hoàn thành";
    }


    if (
        item.date
        &&
        item.date
        <
        todayString()
    ) {
        return "Quá hạn";
    }


    return "Đang mở";
}


function typeIcon(type) {

    switch (type) {

        case "call":
            return "☎";

        case "email":
            return "✉";

        case "meeting":
            return "♙";

        case "task":
            return "✓";

        default:
            return "✎";
    }

}


function formatSchedule(item) {

    if (
        !item.date
    ) {
        return "Chưa đặt lịch";
    }


    return item.date
        +
        (
            item.time
            ?
            " · "
            +
            item.time
            :
            ""
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


function saveActivities() {

    localStorage.setItem(
        "crm_ui_activities",
        JSON.stringify(
            activities
        )
    );

}


function todayString() {

    const date =
        new Date();


    const year =
        date.getFullYear();


    const month =
        String(
            date.getMonth()
            +
            1
        )
        .padStart(
            2,
            "0"
        );


    const day =
        String(
            date.getDate()
        )
        .padStart(
            2,
            "0"
        );


    return `${year}-${month}-${day}`;
}


function value(id) {

    return document
        .getElementById(id)
        .value
        .trim();

}


function setValue(
    id,
    value
) {

    document
        .getElementById(id)
        .value =
            value ?? "";

}


function text(
    id,
    value
) {

    document
        .getElementById(id)
        .textContent =
            value;
}


function escapeHtml(value) {

    return String(value ?? "")
        .replace(/&/g,"&amp;")
        .replace(/</g,"&lt;")
        .replace(/>/g,"&gt;")
        .replace(/"/g,"&quot;")
        .replace(/'/g,"&#039;");
}


render();