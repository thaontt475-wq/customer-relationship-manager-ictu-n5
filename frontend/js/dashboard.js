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
   MODERN ENTERPRISE CHARTS (APEXCHARTS)
========================================================= */

let funnelChartInstance = null;
let forecastChartInstance = null;
let currentFunnelView = "funnel";
let lastOpportunitiesData = [];
let chartListenersInitialized = false;

function isDarkTheme() {
    return document.documentElement.getAttribute("data-theme") === "dark";
}

function getStageSortOrder(key) {
    const k = String(key || "").toLowerCase();
    if (k === "1" || k === "approach") return 1;
    if (k === "2") return 2;
    if (k === "3" || k === "quote") return 3;
    if (k === "4" || k === "negotiation") return 4;
    if (k === "5" || k === "closing" || k === "won") return 5;
    if (k === "6" || k === "lost") return 6;
    return 99;
}

function getStageColor(key, index) {
    const keyStr = String(key || "").toLowerCase();
    if (keyStr === "1" || keyStr === "approach" || keyStr.includes("tiếp cận") || keyStr.includes("khảo sát")) return "#06b6d4";
    if (keyStr === "2" || keyStr.includes("đánh giá")) return "#0ea5e9";
    if (keyStr === "3" || keyStr === "quote" || keyStr.includes("báo giá") || keyStr.includes("đề xuất")) return "#3b82f6";
    if (keyStr === "4" || keyStr === "negotiation" || keyStr.includes("đàm phán") || keyStr.includes("thương lượng")) return "#8b5cf6";
    if (keyStr === "5" || keyStr === "closing" || keyStr === "won" || keyStr.includes("thành công") || keyStr.includes("chốt")) return "#10b981";
    if (keyStr === "6" || keyStr === "lost" || keyStr.includes("thua")) return "#f43f5e";

    const fallback = ["#06b6d4", "#0ea5e9", "#3b82f6", "#6366f1", "#8b5cf6", "#10b981", "#f59e0b", "#ec4899"];
    return fallback[(index || 0) % fallback.length];
}

function initChartFeatures() {
    if (chartListenersInitialized) return;
    chartListenersInitialized = true;

    const toggle = document.getElementById("funnelViewToggle");
    if (toggle) {
        toggle.querySelectorAll(".view-btn").forEach(btn => {
            btn.addEventListener("click", () => {
                const view = btn.dataset.view;
                if (!view || view === currentFunnelView) return;
                currentFunnelView = view;
                toggle.querySelectorAll(".view-btn").forEach(b => b.classList.toggle("active", b === btn));
                if (lastOpportunitiesData) {
                    renderFunnel(lastOpportunitiesData);
                }
            });
        });
    }

    const observer = new MutationObserver(mutations => {
        for (const m of mutations) {
            if (m.type === "attributes" && m.attributeName === "data-theme") {
                const dark = isDarkTheme();
                if (funnelChartInstance && typeof funnelChartInstance.updateOptions === "function") {
                    funnelChartInstance.updateOptions({
                        theme: { mode: dark ? "dark" : "light" },
                        chart: { foreColor: dark ? "#94a3b8" : "#64748b" },
                        grid: { borderColor: dark ? "#334155" : "#e2e8f0" }
                    }, false, false);
                }
                if (forecastChartInstance && typeof forecastChartInstance.updateOptions === "function") {
                    forecastChartInstance.updateOptions({
                        theme: { mode: dark ? "dark" : "light" },
                        chart: { foreColor: dark ? "#94a3b8" : "#64748b" },
                        grid: { borderColor: dark ? "#334155" : "#e2e8f0" }
                    }, false, false);
                }
            }
        }
    });
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ["data-theme"] });
}

/* =========================================================
   FUNNEL
========================================================= */

function renderFunnel(opportunities) {
    initChartFeatures();
    lastOpportunitiesData = opportunities;

    const chartWrapper = document.getElementById("funnelChartContainer");
    const container = document.getElementById("funnelContainer");
    const empty = document.getElementById("funnelEmpty");
    const footer = document.getElementById("funnelStatsFooter");

    if (!opportunities || !opportunities.length) {
        if (funnelChartInstance) {
            try { funnelChartInstance.destroy(); } catch (_) {}
            funnelChartInstance = null;
        }
        if (chartWrapper) chartWrapper.style.display = "none";
        if (container) {
            container.innerHTML = "";
            container.style.display = "none";
        }
        if (empty) empty.style.display = "flex";
        if (footer) footer.innerHTML = "";
        return;
    }

    const configuration = loadArray("crm_ui_pipeline_stages");
    let stages = [];
    if (configuration.length) {
        stages = configuration.map((stage, index) => ({
            key: normalizeStageKey(stage.name, index),
            label: stage.name
        }));
    }

    const existingKeys = new Set(opportunities.map(item => String(item.stage)));
    const defaults = [
        { key: "1", label: "Khảo sát nhu cầu" },
        { key: "2", label: "Đánh giá nhu cầu" },
        { key: "3", label: "Gửi đề xuất & Báo giá" },
        { key: "4", label: "Thương lượng & Đàm phán" },
        { key: "5", label: "Chốt thành công (Won)" },
        { key: "6", label: "Đóng thua (Lost)" },
        { key: "approach", label: "Tiếp cận" },
        { key: "quote", label: "Báo giá" },
        { key: "negotiation", label: "Đàm phán" },
        { key: "closing", label: "Chốt" }
    ];

    for (const stage of defaults) {
        if (existingKeys.has(stage.key) && !stages.some(item => String(item.key) === String(stage.key))) {
            stages.push(stage);
        }
    }

    for (const k of existingKeys) {
        if (!stages.some(item => String(item.key) === String(k))) {
            stages.push({
                key: String(k),
                label: stageLabel(k)
            });
        }
    }

    const counts = stages
        .map((stage, idx) => {
            const matched = opportunities.filter(item => String(item.stage) === String(stage.key));
            return {
                ...stage,
                count: matched.length,
                value: matched.reduce((sum, item) => sum + Number(item.value || 0), 0),
                color: getStageColor(stage.key, idx)
            };
        })
        .filter(item => item.count > 0);

    counts.sort((a, b) => getStageSortOrder(a.key) - getStageSortOrder(b.key));

    if (!counts.length) {
        if (funnelChartInstance) {
            try { funnelChartInstance.destroy(); } catch (_) {}
            funnelChartInstance = null;
        }
        if (chartWrapper) chartWrapper.style.display = "none";
        if (container) {
            container.innerHTML = "";
            container.style.display = "none";
        }
        if (empty) empty.style.display = "flex";
        if (footer) footer.innerHTML = "";
        return;
    }

    if (empty) empty.style.display = "none";

    const totalDeals = counts.reduce((sum, item) => sum + item.count, 0);
    const totalPipelineValue = counts.reduce((sum, item) => sum + item.value, 0);
    const wonItem = counts.find(item => item.key === "5" || item.key === "closing" || item.key === "won");
    const wonCount = wonItem ? wonItem.count : 0;
    const winRate = totalDeals > 0 ? Math.round((wonCount / totalDeals) * 100) : 0;
    const avgDeal = totalDeals > 0 ? Math.round(totalPipelineValue / totalDeals) : 0;

    if (footer) {
        footer.innerHTML = `
            <div class="stat-chip"><i class="fa-solid fa-briefcase"></i> <span>Tổng cơ hội:</span> <strong>${totalDeals}</strong></div>
            <div class="stat-chip"><i class="fa-solid fa-coins"></i> <span>Tổng Pipeline:</span> <strong>${money(totalPipelineValue)}</strong></div>
            <div class="stat-chip"><i class="fa-solid fa-trophy"></i> <span>Tỷ lệ thắng:</span> <strong>${winRate}%</strong></div>
            <div class="stat-chip"><i class="fa-solid fa-chart-pie"></i> <span>TB/deal:</span> <strong>${money(avgDeal)}</strong></div>
        `;
    }

    if (typeof ApexCharts === "undefined" || !chartWrapper) {
        if (chartWrapper) chartWrapper.style.display = "none";
        if (container) {
            container.style.display = "flex";
            container.innerHTML = "";
            const max = Math.max(...counts.map(item => item.count), 1);
            for (const item of counts) {
                const width = Math.max(18, item.count / max * 100);
                const row = document.createElement("div");
                row.className = "funnel-row";
                row.innerHTML = `
                    <span class="funnel-stage">${escapeHtml(item.label)}</span>
                    <div class="funnel-track">
                        <div class="funnel-bar" style="width:${width}%">${item.count}</div>
                    </div>
                    <span class="funnel-value">${shortMoney(item.value)}</span>
                `;
                container.appendChild(row);
            }
        }
        return;
    }

    if (container) container.style.display = "none";
    chartWrapper.style.display = "block";

    if (funnelChartInstance) {
        try { funnelChartInstance.destroy(); } catch (_) {}
        funnelChartInstance = null;
    }
    chartWrapper.innerHTML = "";

    const dark = isDarkTheme();

    if (currentFunnelView === "donut") {
        const donutOptions = {
            chart: {
                type: "donut",
                height: 290,
                background: "transparent",
                foreColor: dark ? "#94a3b8" : "#64748b",
                animations: { enabled: true, easing: "easeinout", speed: 500 }
            },
            series: counts.map(c => c.value > 0 ? c.value : c.count),
            labels: counts.map(c => c.label),
            colors: counts.map(c => c.color),
            stroke: {
                show: true,
                width: 2,
                colors: [dark ? "#0f172a" : "#ffffff"]
            },
            plotOptions: {
                pie: {
                    donut: {
                        size: "70%",
                        labels: {
                            show: true,
                            name: { show: true, fontSize: "12px", fontWeight: 600, color: dark ? "#94a3b8" : "#64748b" },
                            value: {
                                show: true,
                                fontSize: "17px",
                                fontWeight: 800,
                                color: dark ? "#f8fafc" : "#0f172a",
                                formatter: val => shortMoney(Number(val))
                            },
                            total: {
                                show: true,
                                label: "Tổng Pipeline",
                                color: dark ? "#94a3b8" : "#64748b",
                                fontSize: "11px",
                                formatter: () => shortMoney(totalPipelineValue)
                            }
                        }
                    }
                }
            },
            legend: {
                position: "bottom",
                horizontalAlign: "center",
                fontSize: "11.5px",
                itemMargin: { horizontal: 6, vertical: 3 },
                markers: { radius: 10 }
            },
            dataLabels: {
                enabled: true,
                formatter: (val) => `${Number(val).toFixed(0)}%`,
                style: { fontSize: "11px", fontWeight: 700 }
            },
            tooltip: {
                theme: dark ? "dark" : "light",
                custom: function({ seriesIndex }) {
                    const item = counts[seriesIndex];
                    if (!item) return "";
                    const pctVal = totalPipelineValue > 0 ? ((item.value / totalPipelineValue) * 100).toFixed(1) : 0;
                    return `
                        <div class="crm-chart-tooltip">
                            <div class="tooltip-header" style="border-left: 4px solid ${item.color};">
                                <strong>${escapeHtml(item.label)}</strong>
                            </div>
                            <div class="tooltip-body">
                                <div class="tooltip-row">
                                    <span>Số lượng:</span>
                                    <strong>${item.count} cơ hội</strong>
                                </div>
                                <div class="tooltip-row">
                                    <span>Giá trị giai đoạn:</span>
                                    <strong style="color: ${item.color};">${money(item.value)} (${pctVal}%)</strong>
                                </div>
                            </div>
                        </div>
                    `;
                }
            },
            theme: { mode: dark ? "dark" : "light" }
        };
        funnelChartInstance = new ApexCharts(chartWrapper, donutOptions);
        funnelChartInstance.render();
    } else {
        const barOptions = {
            chart: {
                type: "bar",
                height: Math.max(260, counts.length * 52),
                toolbar: { show: false },
                background: "transparent",
                foreColor: dark ? "#94a3b8" : "#64748b",
                animations: { enabled: true, easing: "easeinout", speed: 500 }
            },
            plotOptions: {
                bar: {
                    horizontal: true,
                    distributed: true,
                    barHeight: "58%",
                    borderRadius: 6,
                    borderRadiusApplication: "end",
                    dataLabels: {
                        position: "center"
                    }
                }
            },
            colors: counts.map(c => c.color),
            series: [{
                name: "Số lượng cơ hội",
                data: counts.map(c => c.count)
            }],
            xaxis: {
                categories: counts.map(c => c.label),
                labels: {
                    style: { fontSize: "11px", fontWeight: 500 }
                },
                axisBorder: { show: false },
                axisTicks: { show: false }
            },
            yaxis: {
                labels: {
                    style: { fontSize: "12px", fontWeight: 600 },
                    maxWidth: 190
                }
            },
            dataLabels: {
                enabled: true,
                textAnchor: "middle",
                formatter: function(val, opt) {
                    const item = counts[opt.dataPointIndex];
                    if (!item) return val;
                    return `${val} cơ hội • ${shortMoney(item.value)}`;
                },
                style: {
                    fontSize: "11px",
                    fontWeight: 700,
                    colors: ["#ffffff"]
                },
                dropShadow: {
                    enabled: true,
                    top: 1,
                    left: 1,
                    blur: 1,
                    color: "#000000",
                    opacity: 0.5
                }
            },
            grid: {
                borderColor: dark ? "#334155" : "#e2e8f0",
                strokeDashArray: 4,
                xaxis: { lines: { show: true } },
                yaxis: { lines: { show: false } },
                padding: { top: 0, right: 15, bottom: 0, left: 10 }
            },
            legend: { show: false },
            tooltip: {
                theme: dark ? "dark" : "light",
                custom: function({ dataPointIndex }) {
                    const item = counts[dataPointIndex];
                    if (!item) return "";
                    const pctCount = ((item.count / totalDeals) * 100).toFixed(1);
                    const pctValue = totalPipelineValue > 0 ? ((item.value / totalPipelineValue) * 100).toFixed(1) : 0;
                    const avgPerDeal = item.count > 0 ? (item.value / item.count) : 0;
                    return `
                        <div class="crm-chart-tooltip">
                            <div class="tooltip-header" style="border-left: 4px solid ${item.color};">
                                <strong>${escapeHtml(item.label)}</strong>
                            </div>
                            <div class="tooltip-body">
                                <div class="tooltip-row">
                                    <span>Số lượng:</span>
                                    <strong>${item.count} cơ hội (${pctCount}%)</strong>
                                </div>
                                <div class="tooltip-row">
                                    <span>Giá trị giai đoạn:</span>
                                    <strong style="color: ${item.color};">${money(item.value)} (${pctValue}%)</strong>
                                </div>
                                <div class="tooltip-row">
                                    <span>Trung bình / deal:</span>
                                    <strong>${money(avgPerDeal)}</strong>
                                </div>
                            </div>
                        </div>
                    `;
                }
            },
            theme: { mode: dark ? "dark" : "light" }
        };
        funnelChartInstance = new ApexCharts(chartWrapper, barOptions);
        funnelChartInstance.render();
    }
}

/* =========================================================
   FORECAST
========================================================= */

function renderForecast(opportunities) {
    const chartWrapper = document.getElementById("forecastChartContainer");
    const container = document.getElementById("forecastBars");
    const empty = document.getElementById("forecastEmpty");
    const totalEl = document.getElementById("forecastTotal");
    const footer = document.getElementById("forecastStatsFooter");

    if (!opportunities || !opportunities.length) {
        if (forecastChartInstance) {
            try { forecastChartInstance.destroy(); } catch (_) {}
            forecastChartInstance = null;
        }
        if (chartWrapper) chartWrapper.style.display = "none";
        if (container) {
            container.innerHTML = "";
            container.style.display = "none";
        }
        if (empty) empty.style.display = "flex";
        if (totalEl) totalEl.textContent = money(0);
        if (footer) footer.innerHTML = "";
        return;
    }

    const groups = new Map();
    for (const item of opportunities) {
        const key = String(item.stage || "unknown");
        if (!groups.has(key)) {
            groups.set(key, {
                key,
                label: stageLabel(key),
                pipelineVal: 0,
                expectedVal: 0,
                count: 0
            });
        }
        const g = groups.get(key);
        const val = Number(item.value || 0);
        const prob = Number(item.probability || 0);
        g.pipelineVal += val;
        g.expectedVal += (val * prob) / 100;
        g.count += 1;
    }

    const stageList = Array.from(groups.values());
    stageList.sort((a, b) => getStageSortOrder(a.key) - getStageSortOrder(b.key));

    const totalPipelineVal = stageList.reduce((sum, s) => sum + s.pipelineVal, 0);
    const totalExpectedVal = stageList.reduce((sum, s) => sum + s.expectedVal, 0);
    const realizationRatio = totalPipelineVal > 0 ? ((totalExpectedVal / totalPipelineVal) * 100) : 0;

    if (totalEl) {
        totalEl.textContent = money(totalExpectedVal);
    }

    if (footer) {
        footer.innerHTML = `
            <div class="stat-chip"><i class="fa-solid fa-layer-group"></i> <span>Tổng phễu danh nghĩa:</span> <strong>${money(totalPipelineVal)}</strong></div>
            <div class="stat-chip"><i class="fa-solid fa-bullseye"></i> <span>Kỳ vọng thực nhận:</span> <strong style="color: #10b981;">${money(totalExpectedVal)}</strong></div>
            <div class="stat-chip"><i class="fa-solid fa-percent"></i> <span>Hệ số hiện thực:</span> <strong>${realizationRatio.toFixed(1)}%</strong></div>
        `;
    }

    if (empty) empty.style.display = "none";

    if (typeof ApexCharts === "undefined" || !chartWrapper) {
        if (chartWrapper) chartWrapper.style.display = "none";
        if (container) {
            container.style.display = "grid";
            container.innerHTML = "";
            const max = Math.max(...stageList.map(s => s.expectedVal), 1);
            for (const s of stageList) {
                const row = document.createElement("div");
                row.className = "forecast-row";
                row.innerHTML = `
                    <span class="forecast-label">${escapeHtml(s.label)}</span>
                    <div class="forecast-track">
                        <div class="forecast-progress" style="width:${(s.expectedVal / max) * 100}%"></div>
                    </div>
                    <span class="forecast-value">${shortMoney(s.expectedVal)}</span>
                `;
                container.appendChild(row);
            }
        }
        return;
    }

    if (container) container.style.display = "none";
    chartWrapper.style.display = "block";

    if (forecastChartInstance) {
        try { forecastChartInstance.destroy(); } catch (_) {}
        forecastChartInstance = null;
    }
    chartWrapper.innerHTML = "";

    const dark = isDarkTheme();

    const options = {
        chart: {
            type: "bar",
            height: Math.max(260, stageList.length * 56),
            toolbar: { show: false },
            background: "transparent",
            foreColor: dark ? "#94a3b8" : "#64748b",
            animations: { enabled: true, easing: "easeinout", speed: 500 }
        },
        plotOptions: {
            bar: {
                horizontal: true,
                barHeight: "68%",
                borderRadius: 4,
                borderRadiusApplication: "end",
                dataLabels: {
                    position: "top"
                }
            }
        },
        colors: ["#3b82f6", "#10b981"],
        series: [
            {
                name: "Giá trị danh nghĩa",
                data: stageList.map(s => s.pipelineVal)
            },
            {
                name: "Kỳ vọng (x Xác suất)",
                data: stageList.map(s => Math.round(s.expectedVal))
            }
        ],
        xaxis: {
            categories: stageList.map(s => s.label),
            labels: {
                formatter: val => shortMoney(Number(val)),
                style: { fontSize: "11px", fontWeight: 500 }
            },
            axisBorder: { show: false },
            axisTicks: { show: false }
        },
        yaxis: {
            labels: {
                style: { fontSize: "12px", fontWeight: 600 },
                maxWidth: 190
            }
        },
        dataLabels: {
            enabled: true,
            textAnchor: "start",
            offsetX: 6,
            formatter: val => shortMoney(Number(val)),
            style: {
                fontSize: "11px",
                fontWeight: 700,
                colors: [dark ? "#93c5fd" : "#2563eb", dark ? "#6ee7b7" : "#059669"]
            }
        },
        grid: {
            borderColor: dark ? "#334155" : "#e2e8f0",
            strokeDashArray: 4,
            xaxis: { lines: { show: true } },
            yaxis: { lines: { show: false } },
            padding: { top: 0, right: 36, bottom: 0, left: 10 }
        },
        legend: {
            position: "top",
            horizontalAlign: "right",
            fontSize: "12px",
            itemMargin: { horizontal: 8, vertical: 0 },
            markers: { radius: 4 }
        },
        tooltip: {
            theme: dark ? "dark" : "light",
            custom: function({ dataPointIndex }) {
                const stage = stageList[dataPointIndex];
                if (!stage) return "";
                const avgProb = stage.pipelineVal > 0 ? ((stage.expectedVal / stage.pipelineVal) * 100) : 0;
                const risk = Math.max(0, stage.pipelineVal - stage.expectedVal);
                return `
                    <div class="crm-chart-tooltip">
                        <div class="tooltip-header" style="border-left: 4px solid #10b981;">
                            <strong>${escapeHtml(stage.label)}</strong>
                            <span style="background: rgba(16,185,129,0.15); color: #10b981; font-size: 11px; font-weight: 700; padding: 2px 7px; border-radius: 4px; margin-left: 8px;">Xác suất: ${avgProb.toFixed(0)}%</span>
                        </div>
                        <div class="tooltip-body">
                            <div class="tooltip-row">
                                <span>Giá trị danh nghĩa:</span>
                                <strong style="color: #3b82f6;">${money(stage.pipelineVal)}</strong>
                            </div>
                            <div class="tooltip-row">
                                <span>Kỳ vọng theo xác suất:</span>
                                <strong style="color: #10b981;">${money(stage.expectedVal)}</strong>
                            </div>
                            <div class="tooltip-row">
                                <span>Rủi ro chênh lệch:</span>
                                <strong style="color: #f43f5e;">-${money(risk)}</strong>
                            </div>
                        </div>
                    </div>
                `;
            }
        },
        theme: { mode: dark ? "dark" : "light" }
    };

    forecastChartInstance = new ApexCharts(chartWrapper, options);
    forecastChartInstance.render();
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
    const keyStr = String(key).trim();
    switch (keyStr) {
        case "1":
        case "approach":
            return "Khảo sát nhu cầu";
        case "2":
            return "Đánh giá nhu cầu";
        case "3":
        case "quote":
            return "Gửi báo giá";
        case "4":
        case "negotiation":
            return "Đàm phán & Thương lượng";
        case "5":
        case "closing":
        case "won":
            return "Chốt thành công (Won)";
        case "6":
        case "lost":
            return "Đóng thua (Lost)";
        default:
            return `Giai đoạn ${keyStr}`;
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
            return '<i class="fa-solid fa-phone"></i>';

        case "email":
            return '<i class="fa-solid fa-envelope"></i>';

        case "meeting":
            return '<i class="fa-solid fa-calendar-days"></i>';

        case "task":
            return '<i class="fa-solid fa-circle-check"></i>';

        default:
            return '<i class="fa-solid fa-pen-to-square"></i>';
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
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

renderDashboard();

(async function syncBackendData() {
    try {
        const API_BASE = `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;
        const [oppRes, quoteRes, actRes] = await Promise.all([
            fetch(`${API_BASE}/api/opportunities`, { credentials: "include" }),
            fetch(`${API_BASE}/api/quotes`, { credentials: "include" }),
            fetch(`${API_BASE}/api/activities`, { credentials: "include" })
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