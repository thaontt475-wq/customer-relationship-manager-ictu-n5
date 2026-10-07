"use strict";


document
    .getElementById(
        "refreshDashboard"
    )
    .addEventListener(
        "click",
        renderDashboard
    );


window.addEventListener(
    "storage",
    renderDashboard
);


function renderDashboard() {

    const opportunities =
        loadArray(
            "crm_ui_opportunities"
        );


    const quotes =
        loadArray(
            "crm_ui_quotes"
        );


    const activities =
        loadArray(
            "crm_ui_activities"
        );


    const users =
        loadArray(
            "crm_ui_users"
        );


    const products =
        loadArray(
            "crm_ui_products"
        );


    renderKpis(
        opportunities,
        quotes,
        activities
    );


    renderFunnel(
        opportunities
    );


    renderForecast(
        opportunities
    );


    renderRecentActivities(
        activities
    );


    renderTasks(
        activities
    );


    text(
        "systemUsers",
        users.length
    );


    text(
        "systemProducts",
        products.length
    );


    text(
        "systemQuotes",
        quotes.length
    );


    text(
        "systemScheduled",
        activities.filter(
            item =>
                Boolean(
                    item.date
                )
        ).length
    );

}


/* =========================================================
   KPI
========================================================= */

function renderKpis(
    opportunities,
    quotes,
    activities
) {

    const opportunityValue =
        opportunities.reduce(
            (
                sum,
                item
            ) =>
                sum
                +
                Number(
                    item.value
                    ||
                    0
                ),
            0
        );


    const approvedQuotes =
        quotes.filter(
            quote =>
                quote.status
                ===
                "approved"
        );


    const approvedValue =
        approvedQuotes.reduce(
            (
                sum,
                quote
            ) =>
                sum
                +
                Number(
                    quote.total
                    ||
                    0
                ),
            0
        );


    const tasks =
        activities.filter(
            item =>
                item.type
                === "task"
                &&
                item.status
                !== "done"
        );


    const lateTasks =
        tasks.filter(
            task =>
                isLate(
                    task
                )
        );


    text(
        "kpiRevenue",
        money(
            opportunityValue
        )
    );


    text(
        "kpiOpportunities",
        opportunities.length
    );


    text(
        "kpiApprovedQuotes",
        approvedQuotes.length
    );


    text(
        "kpiApprovedValue",
        "Tổng giá trị "
        +
        money(
            approvedValue
        )
    );


    text(
        "kpiTasks",
        tasks.length
    );


    text(
        "kpiLateTasks",
        `${lateTasks.length} công việc quá hạn`
    );

}


/* =========================================================
   FUNNEL
========================================================= */

function renderFunnel(
    opportunities
) {

    const container =
        document.getElementById(
            "funnelContainer"
        );


    const empty =
        document.getElementById(
            "funnelEmpty"
        );


    container.innerHTML = "";


    if (
        !opportunities.length
    ) {

        container.style.display =
            "none";

        empty.style.display =
            "flex";

        return;
    }


    container.style.display =
        "flex";

    empty.style.display =
        "none";


    const configuration =
        loadArray(
            "crm_ui_pipeline_stages"
        );


    let stages;


    if (
        configuration.length
    ) {

        stages =
            configuration.map(
                (
                    stage,
                    index
                ) => ({
                    key:
                        normalizeStageKey(
                            stage.name,
                            index
                        ),

                    label:
                        stage.name
                })
            );

    } else {

        stages = [
            {
                key: "approach",
                label: "Tiếp cận"
            },
            {
                key: "quote",
                label: "Báo giá"
            },
            {
                key: "negotiation",
                label: "Đàm phán"
            },
            {
                key: "closing",
                label: "Chốt"
            }
        ];
    }


    /*
     * Pipeline hiện dùng stage key cố định.
     * Nếu có config custom nhưng không khớp,
     * vẫn hiển thị stage mặc định bên dưới.
     */
    const existingKeys =
        new Set(
            opportunities.map(
                item =>
                    item.stage
            )
        );


    const defaults = [
        {
            key: "approach",
            label: "Tiếp cận"
        },
        {
            key: "quote",
            label: "Báo giá"
        },
        {
            key: "negotiation",
            label: "Đàm phán"
        },
        {
            key: "closing",
            label: "Chốt"
        }
    ];


    for (
        const stage
        of defaults
    ) {

        if (
            existingKeys.has(
                stage.key
            )
            &&
            !stages.some(
                item =>
                    item.key
                    === stage.key
            )
        ) {

            stages.push(
                stage
            );

        }

    }


    const counts =
        stages.map(
            stage => ({

                ...stage,

                count:
                    opportunities.filter(
                        item =>
                            item.stage
                            === stage.key
                    ).length,

                value:
                    opportunities
                        .filter(
                            item =>
                                item.stage
                                === stage.key
                        )
                        .reduce(
                            (
                                sum,
                                item
                            ) =>
                                sum
                                +
                                Number(
                                    item.value
                                    ||
                                    0
                                ),
                            0
                        )

            })
        )
        .filter(
            item =>
                item.count
                >
                0
        );


    if (
        !counts.length
    ) {

        container.style.display =
            "none";

        empty.style.display =
            "flex";

        return;
    }


    const max =
        Math.max(
            ...counts.map(
                item =>
                    item.count
            ),
            1
        );


    for (
        const item
        of counts
    ) {

        const width =
            Math.max(
                18,
                item.count
                /
                max
                *
                100
            );


        const row =
            document.createElement(
                "div"
            );


        row.className =
            "funnel-row";


        row.innerHTML = `
            <span class="funnel-stage">
                ${escapeHtml(item.label)}
            </span>

            <div class="funnel-track">

                <div
                    class="funnel-bar"
                    style="width:${width}%"
                >
                    ${item.count}
                </div>

            </div>

            <span class="funnel-value">
                ${shortMoney(item.value)}
            </span>
        `;


        container.appendChild(
            row
        );

    }

}


/* =========================================================
   FORECAST
========================================================= */

function renderForecast(
    opportunities
) {

    const container =
        document.getElementById(
            "forecastBars"
        );


    const empty =
        document.getElementById(
            "forecastEmpty"
        );


    container.innerHTML = "";


    if (
        !opportunities.length
    ) {

        container.style.display =
            "none";

        empty.style.display =
            "flex";


        text(
            "forecastTotal",
            money(0)
        );


        return;
    }


    container.style.display =
        "grid";

    empty.style.display =
        "none";


    const forecastTotal =
        opportunities.reduce(
            (
                sum,
                item
            ) =>
                sum
                +
                (
                    Number(
                        item.value
                        ||
                        0
                    )
                    *
                    Number(
                        item.probability
                        ||
                        0
                    )
                    /
                    100
                ),
            0
        );


    text(
        "forecastTotal",
        money(
            forecastTotal
        )
    );


    const groups =
        new Map();


    for (
        const item
        of opportunities
    ) {

        const key =
            item.stage
            ||
            "unknown";


        const value =
            Number(
                item.value
                ||
                0
            )
            *
            Number(
                item.probability
                ||
                0
            )
            /
            100;


        groups.set(
            key,
            (
                groups.get(key)
                ||
                0
            )
            +
            value
        );

    }


    const max =
        Math.max(
            ...groups.values(),
            1
        );


    for (
        const [
            key,
            value
        ]
        of groups
    ) {

        const row =
            document.createElement(
                "div"
            );


        row.className =
            "forecast-row";


        row.innerHTML = `
            <span class="forecast-label">
                ${stageLabel(key)}
            </span>

            <div class="forecast-track">

                <div
                    class="forecast-progress"
                    style="width:${value / max * 100}%"
                ></div>

            </div>

            <span class="forecast-value">
                ${shortMoney(value)}
            </span>
        `;


        container.appendChild(
            row
        );

    }

}


/* =========================================================
   ACTIVITY
========================================================= */

function renderRecentActivities(
    activities
) {

    const container =
        document.getElementById(
            "recentActivities"
        );


    container.innerHTML = "";


    const latest =
        [...activities]
            .sort(
                (
                    a,
                    b
                ) =>
                    String(
                        b.createdAt
                        ||
                        ""
                    )
                    .localeCompare(
                        String(
                            a.createdAt
                            ||
                            ""
                        )
                    )
            )
            .slice(
                0,
                5
            );


    document
        .getElementById(
            "recentActivitiesEmpty"
        )
        .style
        .display =
            latest.length
            ?
            "none"
            :
            "flex";


    for (
        const item
        of latest
    ) {

        const row =
            document.createElement(
                "div"
            );


        row.className =
            "recent-item";


        row.innerHTML = `
            <div class="recent-icon">
                ${activityIcon(item.type)}
            </div>

            <div class="recent-copy">

                <strong>
                    ${escapeHtml(item.title || "Hoạt động")}
                </strong>

                <span>
                    ${
                        escapeHtml(
                            item.relation
                            ||
                            typeLabel(
                                item.type
                            )
                        )
                    }
                </span>

            </div>

            <small>
                ${escapeHtml(item.date || "—")}
            </small>
        `;


        container.appendChild(
            row
        );

    }

}


/* =========================================================
   TASKS
========================================================= */

function renderTasks(
    activities
) {

    const container =
        document.getElementById(
            "dashboardTasks"
        );


    container.innerHTML = "";


    const tasks =
        activities
            .filter(
                item =>
                    item.type
                    === "task"
                    &&
                    item.status
                    !== "done"
            )
            .sort(
                (
                    a,
                    b
                ) =>
                    String(
                        a.date
                        ||
                        "9999-99-99"
                    )
                    .localeCompare(
                        String(
                            b.date
                            ||
                            "9999-99-99"
                        )
                    )
            )
            .slice(
                0,
                5
            );


    document
        .getElementById(
            "dashboardTasksEmpty"
        )
        .style
        .display =
            tasks.length
            ?
            "none"
            :
            "flex";


    for (
        const task
        of tasks
    ) {

        const row =
            document.createElement(
                "div"
            );


        row.className =
            "dashboard-task";


        row.innerHTML = `
            <div class="recent-icon">
                ✓
            </div>

            <div class="dashboard-task-copy">

                <strong>
                    ${escapeHtml(task.title)}
                </strong>

                <span>
                    ${
                        escapeHtml(
                            task.date
                            ||
                            "Chưa có hạn"
                        )
                    }
                    ${
                        task.owner
                        ?
                        " · "
                        +
                        escapeHtml(
                            task.owner
                        )
                        :
                        ""
                    }
                </span>

            </div>

            ${
                isLate(task)
                ?
                `
                <span class="task-warning">
                    Quá hạn
                </span>
                `
                :
                ""
            }
        `;


        container.appendChild(
            row
        );

    }

}


/* =========================================================
   HELPERS
========================================================= */

function loadArray(
    key
) {

    try {

        const value =
            JSON.parse(
                localStorage.getItem(
                    key
                )
            );


        return Array.isArray(
            value
        )
        ?
        value
        :
        [];

    } catch {

        return [];

    }

}


function money(value) {

    return new Intl.NumberFormat(
        "vi-VN",
        {
            style: "currency",
            currency: "VND",
            maximumFractionDigits: 0
        }
    )
    .format(
        Number(value)
        ||
        0
    );

}


function shortMoney(value) {

    const number =
        Number(value)
        ||
        0;


    if (
        number >=
        1000000000
    ) {

        return (
            number
            /
            1000000000
        )
        .toFixed(1)
        +
        "B ₫";

    }


    if (
        number >=
        1000000
    ) {

        return (
            number
            /
            1000000
        )
        .toFixed(1)
        +
        "M ₫";

    }


    return money(
        number
    );

}


function stageLabel(key) {

    switch (key) {

        case "approach":
            return "Tiếp cận";

        case "quote":
            return "Báo giá";

        case "negotiation":
            return "Đàm phán";

        case "closing":
            return "Chốt";

        default:
            return key;
    }

}


function normalizeStageKey(
    name,
    index
) {

    const normalized =
        String(name)
            .trim()
            .toLowerCase();


    if (
        normalized.includes(
            "tiếp cận"
        )
    ) {
        return "approach";
    }


    if (
        normalized.includes(
            "báo giá"
        )
    ) {
        return "quote";
    }


    if (
        normalized.includes(
            "đàm phán"
        )
    ) {
        return "negotiation";
    }


    if (
        normalized.includes(
            "chốt"
        )
    ) {
        return "closing";
    }


    return (
        "custom_"
        +
        index
    );

}


function isLate(
    task
) {

    if (
        !task.date
        ||
        task.status
        === "done"
    ) {
        return false;
    }


    return task.date
        <
        todayString();

}


function todayString() {

    const date =
        new Date();


    return (
        date.getFullYear()
        +
        "-"
        +
        String(
            date.getMonth()
            +
            1
        )
        .padStart(
            2,
            "0"
        )
        +
        "-"
        +
        String(
            date.getDate()
        )
        .padStart(
            2,
            "0"
        )
    );

}


function activityIcon(type) {

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


function text(
    id,
    value
) {

    const element =
        document.getElementById(
            id
        );


    if (
        element
    ) {

        element.textContent =
            value;

    }

}


function escapeHtml(value) {

    return String(
        value
        ??
        ""
    )
    .replace(/&/g,"&amp;")
    .replace(/</g,"&lt;")
    .replace(/>/g,"&gt;")
    .replace(/"/g,"&quot;")
renderDashboard();

(async function syncBackendData() {
    try {
        const [oppRes, quoteRes, actRes] = await Promise.all([
            fetch("http://localhost:8080/crm/api/opportunities", { credentials: "include" }),
            fetch("http://localhost:8080/crm/api/quotes", { credentials: "include" }),
            fetch("http://localhost:8080/crm/api/activities", { credentials: "include" })
        ]);

        const [oppJson, quoteJson, actJson] = await Promise.all([
            oppRes.json().catch(() => null),
            quoteRes.json().catch(() => null),
            actRes.json().catch(() => null)
        ]);

        let changed = false;

        if (oppJson?.success && Array.isArray(oppJson.data)) {
            const opps = oppJson.data.map(o => ({
                id: o.id,
                name: o.name,
                customer: o.customerName || "",
                value: Number(o.amount || 0),
                stage: String(o.stageId),
                stageName: o.stageName,
                probability: o.probability || 0,
                closeDate: o.expectedCloseDate || "",
                owner: o.ownerName || "Tôi",
                status: o.status
            }));
            localStorage.setItem("crm_ui_opportunities", JSON.stringify(opps));
            changed = true;
        }

        if (quoteJson?.success && Array.isArray(quoteJson.data)) {
            const qs = quoteJson.data.map(q => ({
                id: q.id,
                code: q.quoteNumber || `BG-${q.id}`,
                customer: q.customerName || "",
                total: Number(q.totalAmount || 0),
                subtotal: Number(q.subtotal || 0),
                discountPercent: Number(q.discountPercent || 0),
                status: (q.status || "").toLowerCase()
            }));
            localStorage.setItem("crm_ui_quotes", JSON.stringify(qs));
            changed = true;
        }

        if (actJson?.success && Array.isArray(actJson.data)) {
            const acts = actJson.data.map(a => ({
                id: a.id,
                type: (a.type || "task").toLowerCase(),
                title: a.subject || "Hoạt động",
                status: (a.status || "OPEN").toUpperCase() === "COMPLETED" ? "done" : "open"
            }));
            localStorage.setItem("crm_ui_activities", JSON.stringify(acts));
            changed = true;
        }

        if (changed) {
            renderDashboard();
        }
    } catch (_) {}
})();