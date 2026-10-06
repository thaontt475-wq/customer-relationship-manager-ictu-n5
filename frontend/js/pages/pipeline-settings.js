"use strict";

const API_BASE =
    "http://localhost:8080/crm";

const PIPELINE_ID = 1;

let stages = [];


const stageList =
    document.getElementById(
        "stageList"
    );

const stageEmpty =
    document.getElementById(
        "stageEmpty"
    );

const drawer =
    document.getElementById(
        "stageDrawer"
    );

const overlay =
    document.getElementById(
        "stageOverlay"
    );


document
    .getElementById(
        "addStageButton"
    )
    ?.addEventListener(
        "click",
        () => openStageDrawer()
    );


document
    .getElementById(
        "closeStageDrawer"
    )
    ?.addEventListener(
        "click",
        closeStageDrawer
    );


document
    .getElementById(
        "cancelStage"
    )
    ?.addEventListener(
        "click",
        closeStageDrawer
    );


overlay
    ?.addEventListener(
        "click",
        closeStageDrawer
    );


document
    .getElementById(
        "stageForm"
    )
    ?.addEventListener(
        "submit",
        async event => {

            event.preventDefault();

            await saveStage();
        }
    );


document.addEventListener(
    "click",
    async event => {

        const edit =
            event.target.closest(
                "[data-stage-edit]"
            );

        if (edit) {

            openStageDrawer(
                Number(
                    edit.dataset.stageEdit
                )
            );

            return;
        }


        const up =
            event.target.closest(
                "[data-stage-up]"
            );

        if (up) {

            await moveStage(
                Number(
                    up.dataset.stageUp
                ),
                -1
            );

            return;
        }


        const down =
            event.target.closest(
                "[data-stage-down]"
            );

        if (down) {

            await moveStage(
                Number(
                    down.dataset.stageDown
                ),
                1
            );
        }
    }
);


async function api(
    path,
    options = {}
) {

    const response =
        await fetch(
            API_BASE + path,
            {
                credentials:
                    "include",

                headers: {
                    "Content-Type":
                        "application/json",

                    ...(options.headers || {})
                },

                ...options
            }
        );


    const result =
        await response.json()
            .catch(
                () => ({
                    success: false,
                    message:
                        "Phản hồi API không hợp lệ"
                })
            );


    if (!response.ok) {

        throw new Error(
            result.message
            ||
            "Có lỗi xảy ra"
        );
    }


    return result;
}


async function loadStages() {

    try {

        const result =
            await api(
                `/api/pipeline-stages?pipelineId=${PIPELINE_ID}`
            );


        stages =
            Array.isArray(
                result.data
            )
            ?
            result.data
            :
            [];


        stages.sort(
            (a, b) =>
                Number(a.orderNo)
                -
                Number(b.orderNo)
        );


        render();

    } catch (error) {

        console.error(
            "Load pipeline stages error:",
            error
        );

        stages = [];

        render();

        alert(
            error.message
            ||
            "Không thể tải pipeline."
        );
    }
}


async function saveStage() {

    const name =
        value(
            "stageName"
        );

    const probability =
        Number(
            document
                .getElementById(
                    "stageProbability"
                )
                ?.value
        );

    const exitCondition =
        value(
            "stageCondition"
        );

    const conditionRequired =
        Boolean(
            document
                .getElementById(
                    "stageConditionRequired"
                )
                ?.checked
        );

    const editingId =
        value(
            "editingStageIndex"
        );

    const error =
        document.getElementById(
            "stageFormError"
        );


    if (error) {

        error.textContent = "";
    }


    if (!name) {

        if (error) {

            error.textContent =
                "Vui lòng nhập tên giai đoạn.";
        }

        return;
    }


    if (
        !Number.isFinite(
            probability
        )
        ||
        probability < 0
        ||
        probability > 100
    ) {

        if (error) {

            error.textContent =
                "Xác suất phải nằm trong khoảng 0 - 100%.";
        }

        return;
    }


    if (
        conditionRequired
        &&
        !exitCondition
    ) {

        if (error) {

            error.textContent =
                "Hãy nhập điều kiện bắt buộc trước khi bật yêu cầu.";
        }

        return;
    }


    const existing =
        editingId
        ?
        stages.find(
            item =>
                Number(item.id)
                ===
                Number(editingId)
        )
        :
        null;


    const body = {
        pipelineId:
            PIPELINE_ID,

        name,

        orderNo:
            existing
            ?
            Number(
                existing.orderNo
            )
            :
            nextOrderNo(),

        probability,

        exitCondition,

        conditionRequired,

        active:
            existing
            ?
            Boolean(
                existing.active
            )
            :
            true
    };


    try {

        if (editingId) {

            await api(
                `/api/pipeline-stages/${editingId}`,
                {
                    method:
                        "PUT",

                    body:
                        JSON.stringify(
                            body
                        )
                }
            );

        } else {

            await api(
                "/api/pipeline-stages",
                {
                    method:
                        "POST",

                    body:
                        JSON.stringify(
                            body
                        )
                }
            );
        }


        closeStageDrawer();

        await loadStages();

    } catch (err) {

        console.error(
            "Save stage error:",
            err
        );

        if (error) {

            error.textContent =
                err.message
                ||
                "Không thể lưu giai đoạn.";
        }
    }
}


function nextOrderNo() {

    if (
        stages.length === 0
    ) {

        return 1;
    }


    return Math.max(
        ...stages.map(
            item =>
                Number(
                    item.orderNo
                )
        )
    ) + 1;
}


async function moveStage(
    id,
    delta
) {

    const index =
        stages.findIndex(
            item =>
                Number(item.id)
                ===
                Number(id)
        );


    const targetIndex =
        index + delta;


    if (
        index < 0
        ||
        targetIndex < 0
        ||
        targetIndex >= stages.length
    ) {

        return;
    }


    const current =
        stages[index];

    const target =
        stages[targetIndex];


    const currentOrder =
        Number(
            current.orderNo
        );

    const targetOrder =
        Number(
            target.orderNo
        );


    const temporaryOrder =
        Math.max(
            ...stages.map(
                item =>
                    Number(item.orderNo)
            )
        )
        +
        1000;


    try {

        await updateStageOrder(
            current,
            temporaryOrder
        );

        await updateStageOrder(
            target,
            currentOrder
        );

        await updateStageOrder(
            current,
            targetOrder
        );


        await loadStages();

    } catch (error) {

        console.error(
            "Move pipeline stage error:",
            error
        );

        alert(
            error.message
            ||
            "Không thể đổi thứ tự giai đoạn."
        );

        await loadStages();
    }
}


async function updateStageOrder(
    stage,
    orderNo
) {

    await api(
        `/api/pipeline-stages/${stage.id}`,
        {
            method:
                "PUT",

            body:
                JSON.stringify({
                    pipelineId:
                        stage.pipelineId
                        ??
                        PIPELINE_ID,

                    name:
                        stage.name,

                    orderNo,

                    probability:
                        Number(
                            stage.probability
                        ),

                    exitCondition:
                        stage.exitCondition
                        ??
                        "",

                    conditionRequired:
                        Boolean(
                            stage.conditionRequired
                        ),

                    active:
                        Boolean(
                            stage.active
                        )
                })
        }
    );
}


function openStageDrawer(
    id = null
) {

    document
        .getElementById(
            "stageForm"
        )
        ?.reset();


    setValue(
        "editingStageIndex",
        ""
    );


    const error =
        document.getElementById(
            "stageFormError"
        );

    if (error) {

        error.textContent = "";
    }


    const title =
        document.getElementById(
            "stageDrawerTitle"
        );


    if (title) {

        title.textContent =
            "Thêm giai đoạn";
    }


    if (id !== null) {

        const stage =
            stages.find(
                item =>
                    Number(item.id)
                    ===
                    Number(id)
            );


        if (stage) {

            setValue(
                "editingStageIndex",
                stage.id
            );

            setValue(
                "stageName",
                stage.name
            );

            setValue(
                "stageProbability",
                stage.probability
            );

            setValue(
                "stageCondition",
                stage.exitCondition
                ??
                ""
            );


            const required =
                document.getElementById(
                    "stageConditionRequired"
                );

            if (required) {

                required.checked =
                    Boolean(
                        stage.conditionRequired
                    );
            }


            if (title) {

                title.textContent =
                    "Sửa giai đoạn";
            }
        }
    }


    drawer
        ?.classList
        .add(
            "open"
        );

    overlay
        ?.classList
        .add(
            "open"
        );
}


function closeStageDrawer() {

    drawer
        ?.classList
        .remove(
            "open"
        );

    overlay
        ?.classList
        .remove(
            "open"
        );
}


function render() {

    if (
        !stageList
        ||
        !stageEmpty
    ) {

        return;
    }


    stageList.innerHTML =
        "";


    const visible =
        stages.filter(
            item =>
                item.active !== false
        );


    stageEmpty.style.display =
        visible.length
        ?
        "none"
        :
        "flex";


    visible.forEach(
        (
            stage,
            index
        ) => {

            const item =
                document.createElement(
                    "article"
                );


            item.className =
                "stage-item";


            item.innerHTML = `
                <div class="stage-order-buttons">

                    <button
                        type="button"
                        data-stage-up="${stage.id}"
                        ${index === 0 ? "disabled" : ""}
                    >
                        ↑
                    </button>

                    <button
                        type="button"
                        data-stage-down="${stage.id}"
                        ${
                            index ===
                            visible.length - 1
                            ?
                            "disabled"
                            :
                            ""
                        }
                    >
                        ↓
                    </button>

                </div>

                <div class="stage-name">

                    <strong>
                        ${escapeHtml(stage.name)}
                    </strong>

                    <span>
                        Giai đoạn ${index + 1}
                    </span>

                </div>

                <div>
                    <span class="probability-badge">
                        ${Number(stage.probability)}% thắng
                    </span>
                </div>

                <div class="
                    stage-condition
                    ${stage.conditionRequired ? "required" : ""}
                ">
                    ${
                        stage.exitCondition
                        ?
                        escapeHtml(
                            stage.exitCondition
                        )
                        :
                        "Không có điều kiện rời giai đoạn"
                    }
                </div>

                <div>
                    <button
                        type="button"
                        data-stage-edit="${stage.id}"
                    >
                        Sửa
                    </button>
                </div>
            `;


            stageList.appendChild(
                item
            );
        }
    );
}


function value(id) {

    return document
        .getElementById(id)
        ?.value
        ?.trim()
        ??
        "";
}


function setValue(
    id,
    newValue
) {

    const element =
        document.getElementById(
            id
        );

    if (element) {

        element.value =
            newValue ?? "";
    }
}


function escapeHtml(value) {

    return String(
        value ?? ""
    )
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}


loadStages();