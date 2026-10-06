"use strict";

let opportunities = loadPipelineOpportunities();

const stages = {
    approach: {
        label: "Tiếp cận",
        probability: 10
    },
    quote: {
        label: "Báo giá",
        probability: 60
    },
    negotiation: {
        label: "Đàm phán",
        probability: 80
    },
    closing: {
        label: "Chốt",
        probability: 100
    }
};

const stageOrder = [
    "approach",
    "quote",
    "negotiation",
    "closing"
];

let draggedIndex = null;
let closingIndex = null;

const opportunityDrawer =
    document.getElementById("opportunityDrawer");

const opportunityOverlay =
    document.getElementById("opportunityOverlay");

const opportunityForm =
    document.getElementById("opportunityForm");

const searchInput =
    document.getElementById("pipelineSearch");

const ownerFilter =
    document.getElementById("ownerFilter");


document
    .getElementById("addOpportunityButton")
    .addEventListener("click", () => {
        openOpportunityDrawer();
    });


document
    .getElementById("closeOpportunityDrawer")
    .addEventListener("click", closeOpportunityDrawer);


document
    .getElementById("cancelOpportunity")
    .addEventListener("click", closeOpportunityDrawer);


opportunityOverlay.addEventListener(
    "click",
    closeOpportunityDrawer
);


document
    .getElementById("opportunityStage")
    .addEventListener("change", event => {

        const selectedStage =
            stages[event.target.value];

        document
            .getElementById("opportunityProbability")
            .value =
                selectedStage.probability;
    });


opportunityForm.addEventListener(
    "submit",
    event => {

        event.preventDefault();

        const name =
            fieldValue("opportunityName");

        const error =
            document.getElementById(
                "opportunityNameError"
            );

        if (!name) {
            error.textContent =
                "Vui lòng nhập tên cơ hội.";
            return;
        }

        error.textContent = "";

        const probability =
            clamp(
                Number(
                    document
                        .getElementById(
                            "opportunityProbability"
                        )
                        .value
                ),
                0,
                100
            );

        const record = {
            name,
            customer:
                fieldValue(
                    "opportunityCustomer"
                ),
            value:
                Math.max(
                    0,
                    Number(
                        document
                            .getElementById(
                                "opportunityValue"
                            )
                            .value
                    ) || 0
                ),
            closeDate:
                fieldValue(
                    "opportunityCloseDate"
                ),
            stage:
                fieldValue(
                    "opportunityStage"
                ),
            probability,
            owner:
                fieldValue(
                    "opportunityOwner"
                )
        };

        const editIndex =
            document
                .getElementById(
                    "opportunityEditIndex"
                )
                .value;

        if (editIndex === "") {
            opportunities.push(record);
        } else {
            opportunities[
                Number(editIndex)
            ] = record;
        }

        closeOpportunityDrawer();
        render();
    }
);


searchInput.addEventListener(
    "input",
    render
);


ownerFilter.addEventListener(
    "change",
    render
);


document
    .getElementById(
        "resetPipelineFilter"
    )
    .addEventListener(
        "click",
        () => {

            searchInput.value = "";
            ownerFilter.value = "";

            render();
        }
    );


/* =========================================
   DRAG & DROP
========================================= */

document
    .querySelectorAll(
        "[data-drop-zone]"
    )
    .forEach(zone => {

        zone.addEventListener(
            "dragover",
            event => {

                event.preventDefault();

                zone.classList.add(
                    "drag-over"
                );
            }
        );

        zone.addEventListener(
            "dragenter",
            event => {

                event.preventDefault();

                zone.classList.add(
                    "drag-over"
                );
            }
        );

        zone.addEventListener(
            "dragleave",
            event => {

                if (
                    !zone.contains(
                        event.relatedTarget
                    )
                ) {
                    zone.classList.remove(
                        "drag-over"
                    );
                }
            }
        );

        zone.addEventListener(
            "drop",
            event => {

                event.preventDefault();

                zone.classList.remove(
                    "drag-over"
                );

                const droppedIndex =
                    draggedIndex !== null
                        ? draggedIndex
                        : Number(
                            event.dataTransfer
                                .getData(
                                    "text/plain"
                                )
                        );

                const nextStage =
                    zone.dataset.dropZone;

                if (
                    droppedIndex === null
                    ||
                    !opportunities[
                        droppedIndex
                    ]
                ) {
                    return;
                }

                opportunities[
                    droppedIndex
                ].stage =
                    nextStage;

                opportunities[
                    droppedIndex
                ].probability =
                    stages[
                        nextStage
                    ].probability;

                draggedIndex = null;

                render();
            }
        );

    });


/* =========================================
   MENU ACTIONS
========================================= */

document.addEventListener(
    "click",
    event => {

        const menuButton =
            event.target.closest(
                "[data-card-menu]"
            );

        if (menuButton) {

            event.stopPropagation();

            const index =
                menuButton.dataset.cardMenu;

            document
                .querySelectorAll(
                    ".card-menu"
                )
                .forEach(menu => {

                    if (
                        menu.dataset.menu
                        !== index
                    ) {
                        menu.classList.remove(
                            "open"
                        );
                    }
                });

            document
                .querySelector(
                    `[data-menu="${index}"]`
                )
                ?.classList
                .toggle("open");

            return;
        }


        const editButton =
            event.target.closest(
                "[data-edit-opportunity]"
            );

        if (editButton) {

            openOpportunityDrawer(
                Number(
                    editButton
                        .dataset
                        .editOpportunity
                )
            );

            return;
        }


        const nextButton =
            event.target.closest(
                "[data-next-stage]"
            );

        if (nextButton) {

            moveNextStage(
                Number(
                    nextButton
                        .dataset
                        .nextStage
                )
            );

            return;
        }


        const prevButton =
            event.target.closest(
                "[data-prev-stage]"
            );

        if (prevButton) {

            movePrevStage(
                Number(
                    prevButton
                        .dataset
                        .prevStage
                )
            );

            return;
        }


        const wonButton =
            event.target.closest(
                "[data-close-won]"
            );

        if (wonButton) {

            openCloseDeal(
                Number(
                    wonButton
                        .dataset
                        .closeWon
                ),
                "won"
            );

            return;
        }


        const lostButton =
            event.target.closest(
                "[data-close-lost]"
            );

        if (lostButton) {

            openCloseDeal(
                Number(
                    lostButton
                        .dataset
                        .closeLost
                ),
                "lost"
            );

            return;
        }


        if (
            !event.target.closest(
                ".opportunity-card"
            )
        ) {
            document
                .querySelectorAll(
                    ".card-menu"
                )
                .forEach(menu => {
                    menu.classList.remove(
                        "open"
                    );
                });
        }

    }
);


/* =========================================
   WIN / LOSS
========================================= */

const dealModal =
    document.getElementById(
        "closeDealModal"
    );

const dealOverlay =
    document.getElementById(
        "closeDealOverlay"
    );


document
    .querySelectorAll(
        ".deal-mode"
    )
    .forEach(button => {

        button.addEventListener(
            "click",
            () => {

                setDealMode(
                    button.dataset.dealMode
                );
            }
        );
    });


document
    .querySelectorAll(
        "[data-close-deal]"
    )
    .forEach(button => {

        button.addEventListener(
            "click",
            closeDeal
        );
    });


document
    .getElementById("closeDealX")
    .addEventListener(
        "click",
        closeDeal
    );


dealOverlay.addEventListener(
    "click",
    closeDeal
);


document
    .getElementById("wonForm")
    .addEventListener(
        "submit",
        event => {

            event.preventDefault();

            const value =
                Number(
                    document
                        .getElementById(
                            "wonValue"
                        )
                        .value
                );

            const date =
                document
                    .getElementById(
                        "wonDate"
                    )
                    .value;

            const error =
                document
                    .getElementById(
                        "wonError"
                    );

            if (
                !value
                ||
                value <= 0
                ||
                !date
            ) {

                error.textContent =
                    "Vui lòng nhập giá trị chốt và ngày ký.";

                return;
            }

            error.textContent = "";

            if (
                closingIndex !== null
                &&
                opportunities[
                    closingIndex
                ]
            ) {
                opportunities.splice(
                    closingIndex,
                    1
                );
            }

            closeDeal();
            render();
        }
    );


document
    .getElementById("lostForm")
    .addEventListener(
        "submit",
        event => {

            event.preventDefault();

            const reason =
                document
                    .getElementById(
                        "lossReason"
                    )
                    .value;

            const error =
                document
                    .getElementById(
                        "lostError"
                    );

            if (!reason) {

                error.textContent =
                    "Vui lòng chọn lý do thua.";

                return;
            }

            error.textContent = "";

            if (
                closingIndex !== null
                &&
                opportunities[
                    closingIndex
                ]
            ) {
                opportunities.splice(
                    closingIndex,
                    1
                );
            }

            closeDeal();
            render();
        }
    );


/* =========================================
   RENDER
========================================= */

function render() {

    savePipelineOpportunities(); /* PIPELINE_SYNC */

    document
        .querySelectorAll(
            "[data-drop-zone]"
        )
        .forEach(zone => {
            zone.innerHTML = "";
        });


    const query =
        searchInput
            .value
            .trim()
            .toLowerCase();

    const owner =
        ownerFilter.value;


    const filtered =
        opportunities
            .map(
                (item,index) => ({
                    item,
                    index
                })
            )
            .filter(entry => {

                const text =
                    (
                        entry.item.name
                        + " "
                        + entry.item.customer
                    )
                    .toLowerCase();

                const searchOk =
                    !query
                    ||
                    text.includes(query);

                const ownerOk =
                    !owner
                    ||
                    entry.item.owner
                        .includes(owner);

                return (
                    searchOk
                    &&
                    ownerOk
                );
            });


    for (const entry of filtered) {

        const zone =
            document
                .querySelector(
                    `[data-drop-zone="${entry.item.stage}"]`
                );

        if (!zone) continue;

        zone.appendChild(
            createCard(
                entry.item,
                entry.index
            )
        );
    }

    updateColumnSummaries();
}


function createCard(
    record,
    index
) {

    const card =
        document.createElement(
            "article"
        );

    card.className =
        "opportunity-card";

    card.draggable =
        window.innerWidth > 768;

    card.dataset.index =
        String(index);


    const currentStageIndex =
        stageOrder.indexOf(
            record.stage
        );

    const canPrev =
        currentStageIndex > 0;

    const canNext =
        currentStageIndex
        <
        stageOrder.length - 1;


    card.innerHTML = `
        <button
            class="card-menu-btn"
            type="button"
            data-card-menu="${index}"
        >
            ⋮
        </button>

        <div
            class="card-menu"
            data-menu="${index}"
        >

            <button
                type="button"
                data-edit-opportunity="${index}"
            >
                Sửa
            </button>

            ${
                canPrev
                ? `
                <button
                    type="button"
                    data-prev-stage="${index}"
                >
                    ← Giai đoạn trước
                </button>
                `
                : ""
            }

            ${
                canNext
                ? `
                <button
                    type="button"
                    data-next-stage="${index}"
                >
                    Giai đoạn tiếp →
                </button>
                `
                : ""
            }

            <button
                type="button"
                class="won"
                data-close-won="${index}"
            >
                ✓ Đóng Thắng
            </button>

            <button
                type="button"
                class="lost"
                data-close-lost="${index}"
            >
                ✕ Đóng Thua
            </button>

        </div>

        <h3>
            ${escapeHtml(record.name)}
        </h3>

        <div class="opportunity-customer">
            ${escapeHtml(
                record.customer
                ||
                "Chưa chọn khách hàng"
            )}
        </div>

        <strong class="opportunity-value">
            ${formatMoney(record.value)}
        </strong>

        <div class="opportunity-meta">

            <span>
                ${record.probability}% xác suất
            </span>

            <span>
                ${escapeHtml(
                    record.closeDate
                    ||
                    "—"
                )}
            </span>

        </div>

        <div
            style="
                display:flex;
                align-items:center;
                justify-content:space-between;
                margin-top:10px;
            "
        >

            <small
                style="
                    color:#64748b;
                    font-size:10px;
                "
            >
                ${escapeHtml(
                    stages[
                        record.stage
                    ].label
                )}
            </small>

            <div class="card-avatar">
                ${initials(
                    record.owner
                    ||
                    "U"
                )}
            </div>

        </div>
    `;


    card.addEventListener(
        "dragstart",
        event => {

            draggedIndex = index;

            event.dataTransfer
                .effectAllowed =
                    "move";

            event.dataTransfer
                .setData(
                    "text/plain",
                    String(index)
                );

            requestAnimationFrame(
                () => {
                    card.classList.add(
                        "dragging"
                    );
                }
            );
        }
    );


    card.addEventListener(
        "dragend",
        () => {

            draggedIndex = null;

            card.classList.remove(
                "dragging"
            );

            document
                .querySelectorAll(
                    ".column-body"
                )
                .forEach(zone => {
                    zone.classList.remove(
                        "drag-over"
                    );
                });
        }
    );


    return card;
}


function moveNextStage(index) {

    const record =
        opportunities[index];

    if (!record) return;

    const currentIndex =
        stageOrder.indexOf(
            record.stage
        );

    if (
        currentIndex
        <
        stageOrder.length - 1
    ) {

        const nextStage =
            stageOrder[
                currentIndex + 1
            ];

        record.stage =
            nextStage;

        record.probability =
            stages[
                nextStage
            ].probability;

        render();
    }
}


function movePrevStage(index) {

    const record =
        opportunities[index];

    if (!record) return;

    const currentIndex =
        stageOrder.indexOf(
            record.stage
        );

    if (currentIndex > 0) {

        const prevStage =
            stageOrder[
                currentIndex - 1
            ];

        record.stage =
            prevStage;

        record.probability =
            stages[
                prevStage
            ].probability;

        render();
    }
}


function updateColumnSummaries() {

    document
        .querySelectorAll(
            ".pipeline-column"
        )
        .forEach(column => {

            const stage =
                column.dataset.stage;

            const items =
                opportunities.filter(
                    item =>
                        item.stage
                        === stage
                );

            const total =
                items.reduce(
                    (
                        sum,
                        item
                    ) =>
                        sum
                        +
                        item.value,
                    0
                );

            column
                .querySelector(
                    ".column-count"
                )
                .textContent =
                    `${items.length} cơ hội`;

            column
                .querySelector(
                    ".column-total"
                )
                .textContent =
                    formatMoney(total);
        });
}


/* =========================================
   DRAWER
========================================= */

function openOpportunityDrawer(
    index = null
) {

    opportunityForm.reset();

    document
        .getElementById(
            "opportunityEditIndex"
        )
        .value = "";

    document
        .getElementById(
            "opportunityNameError"
        )
        .textContent = "";

    document
        .getElementById(
            "opportunityDrawerTitle"
        )
        .textContent =
            "Thêm cơ hội";

    document
        .getElementById(
            "opportunityProbability"
        )
        .value = 10;


    if (
        index !== null
        &&
        opportunities[index]
    ) {

        const record =
            opportunities[index];

        document
            .getElementById(
                "opportunityDrawerTitle"
            )
            .textContent =
                "Sửa cơ hội";

        document
            .getElementById(
                "opportunityEditIndex"
            )
            .value =
                String(index);

        setField(
            "opportunityName",
            record.name
        );

        setField(
            "opportunityCustomer",
            record.customer
        );

        setField(
            "opportunityValue",
            record.value
        );

        setField(
            "opportunityCloseDate",
            record.closeDate
        );

        setField(
            "opportunityStage",
            record.stage
        );

        setField(
            "opportunityProbability",
            record.probability
        );

        setField(
            "opportunityOwner",
            record.owner
        );
    }

    opportunityDrawer
        .classList
        .add("open");

    opportunityOverlay
        .classList
        .add("open");
}


function closeOpportunityDrawer() {

    opportunityDrawer
        .classList
        .remove("open");

    opportunityOverlay
        .classList
        .remove("open");
}


/* =========================================
   CLOSE DEAL
========================================= */

function openCloseDeal(
    index,
    mode
) {

    closingIndex = index;

    document
        .getElementById(
            "wonForm"
        )
        .reset();

    document
        .getElementById(
            "lostForm"
        )
        .reset();

    document
        .getElementById(
            "wonError"
        )
        .textContent = "";

    document
        .getElementById(
            "lostError"
        )
        .textContent = "";

    setDealMode(mode);

    dealModal
        .classList
        .add("open");

    dealOverlay
        .classList
        .add("open");
}


function closeDeal() {

    dealModal
        .classList
        .remove("open");

    dealOverlay
        .classList
        .remove("open");

    closingIndex = null;
}


function setDealMode(mode) {

    const won =
        mode === "won";

    document
        .querySelectorAll(
            ".deal-mode"
        )
        .forEach(button => {

            button.classList.toggle(
                "active",
                button.dataset.dealMode
                    === mode
            );
        });

    document
        .getElementById(
            "wonForm"
        )
        .hidden =
            !won;

    document
        .getElementById(
            "lostForm"
        )
        .hidden =
            won;

    document
        .getElementById(
            "closeDealEmoji"
        )
        .textContent =
            won
                ? "🎉"
                : "☹️";

    document
        .getElementById(
            "closeDealTitle"
        )
        .textContent =
            won
                ? "Đóng Thắng Cơ Hội"
                : "Đóng Thua Cơ Hội";
}


/* =========================================
   HELPERS
========================================= */

function fieldValue(id) {

    return document
        .getElementById(id)
        .value
        .trim();
}


function setField(
    id,
    value
) {

    document
        .getElementById(id)
        .value =
            value ?? "";
}


function formatMoney(value) {

    return new Intl.NumberFormat(
        "vi-VN",
        {
            style: "currency",
            currency: "VND",
            maximumFractionDigits: 0
        }
    ).format(
        Number(value) || 0
    );
}


function initials(value) {

    const parts =
        String(value)
            .trim()
            .split(/\s+/)
            .filter(Boolean);

    if (!parts.length) {
        return "U";
    }

    if (parts.length === 1) {
        return parts[0]
            .slice(0,2)
            .toUpperCase();
    }

    return (
        parts[0][0]
        +
        parts[
            parts.length - 1
        ][0]
    )
        .toUpperCase();
}


function clamp(
    value,
    min,
    max
) {

    if (!Number.isFinite(value)) {
        return min;
    }

    return Math.min(
        max,
        Math.max(
            min,
            value
        )
    );
}


function escapeHtml(value) {

    return String(value)
        .replace(/&/g,"&amp;")
        .replace(/</g,"&lt;")
        .replace(/>/g,"&gt;")
        .replace(/"/g,"&quot;")
        .replace(/'/g,"&#039;");
}


render();

/* =========================================================
   POINTER DRAG PATCH
   Kéo card: Tiếp cận -> Báo giá -> Đàm phán -> Chốt
========================================================= */

let pointerDrag = {
    active: false,
    index: null,
    card: null,
    ghost: null,
    startX: 0,
    startY: 0,
    offsetX: 0,
    offsetY: 0,
    moved: false
};


document.addEventListener(
    "pointerdown",
    event => {

        const card =
            event.target.closest(
                ".opportunity-card"
            );

        if (!card) {
            return;
        }

        /* Không drag khi bấm menu / button */
        if (
            event.target.closest(
                "button, a, input, select"
            )
        ) {
            return;
        }


        const index =
            Number(
                card.dataset.index
            );

        if (
            !Number.isInteger(index)
            ||
            !opportunities[index]
        ) {
            return;
        }


        /*
         * Tắt native HTML5 drag,
         * dùng pointer drag của mình.
         */
        card.draggable = false;


        const rect =
            card.getBoundingClientRect();


        pointerDrag.active = true;
        pointerDrag.index = index;
        pointerDrag.card = card;

        pointerDrag.startX =
            event.clientX;

        pointerDrag.startY =
            event.clientY;

        pointerDrag.offsetX =
            event.clientX
            - rect.left;

        pointerDrag.offsetY =
            event.clientY
            - rect.top;

        pointerDrag.moved = false;


        card.setPointerCapture?.(
            event.pointerId
        );

    }
);


document.addEventListener(
    "pointermove",
    event => {

        if (
            !pointerDrag.active
            ||
            !pointerDrag.card
        ) {
            return;
        }


        const dx =
            Math.abs(
                event.clientX
                - pointerDrag.startX
            );

        const dy =
            Math.abs(
                event.clientY
                - pointerDrag.startY
            );


        /*
         * Chưa di chuyển đủ 5px
         * thì chưa coi là drag.
         */
        if (
            !pointerDrag.moved
            &&
            dx < 5
            &&
            dy < 5
        ) {
            return;
        }


        if (!pointerDrag.moved) {

            pointerDrag.moved = true;

            createDragGhost();

            pointerDrag.card
                .classList
                .add(
                    "pointer-drag-source"
                );
        }


        event.preventDefault();


        moveDragGhost(
            event.clientX,
            event.clientY
        );


        highlightDropZone(
            event.clientX,
            event.clientY
        );

    },
    {
        passive: false
    }
);


document.addEventListener(
    "pointerup",
    event => {

        if (!pointerDrag.active) {
            return;
        }


        if (pointerDrag.moved) {

            const dropZone =
                getDropZoneAtPoint(
                    event.clientX,
                    event.clientY
                );


            if (
                dropZone
                &&
                pointerDrag.index !== null
                &&
                opportunities[
                    pointerDrag.index
                ]
            ) {

                const nextStage =
                    dropZone
                        .dataset
                        .dropZone;


                /*
                 * Đổi stage.
                 */
                opportunities[
                    pointerDrag.index
                ].stage =
                    nextStage;


                /*
                 * Đổi xác suất theo stage.
                 */
                opportunities[
                    pointerDrag.index
                ].probability =
                    stages[
                        nextStage
                    ].probability;


                /*
                 * Render lại board.
                 */
                render();
            }
        }


        cleanupPointerDrag();

    }
);


document.addEventListener(
    "pointercancel",
    cleanupPointerDrag
);


function createDragGhost() {

    const source =
        pointerDrag.card;

    if (!source) {
        return;
    }


    const rect =
        source
            .getBoundingClientRect();


    const ghost =
        source
            .cloneNode(true);


    ghost.classList.add(
        "pipeline-drag-ghost"
    );


    ghost
        .querySelectorAll(
            ".card-menu"
        )
        .forEach(menu => {
            menu.remove();
        });


    ghost.style.width =
        rect.width + "px";


    document.body
        .appendChild(
            ghost
        );


    pointerDrag.ghost =
        ghost;

}


function moveDragGhost(
    clientX,
    clientY
) {

    const ghost =
        pointerDrag.ghost;

    if (!ghost) {
        return;
    }


    ghost.style.left =
        (
            clientX
            - pointerDrag.offsetX
        )
        + "px";


    ghost.style.top =
        (
            clientY
            - pointerDrag.offsetY
        )
        + "px";

}


function getDropZoneAtPoint(
    x,
    y
) {

    /*
     * Ghost có pointer-events:none
     * nên elementsFromPoint thấy được
     * cột phía dưới.
     */
    const elements =
        document.elementsFromPoint(
            x,
            y
        );


    for (
        const element
        of elements
    ) {

        const zone =
            element.closest?.(
                "[data-drop-zone]"
            );


        if (zone) {
            return zone;
        }


        const column =
            element.closest?.(
                ".pipeline-column"
            );


        if (column) {

            return column.querySelector(
                "[data-drop-zone]"
            );

        }
    }


    return null;

}


function highlightDropZone(
    x,
    y
) {

    document
        .querySelectorAll(
            "[data-drop-zone]"
        )
        .forEach(zone => {
            zone.classList.remove(
                "pointer-drop-active"
            );
        });


    const zone =
        getDropZoneAtPoint(
            x,
            y
        );


    if (zone) {

        zone.classList.add(
            "pointer-drop-active"
        );

    }

}


function cleanupPointerDrag() {

    if (
        pointerDrag.card
    ) {

        pointerDrag.card
            .classList
            .remove(
                "pointer-drag-source"
            );

    }


    if (
        pointerDrag.ghost
    ) {

        pointerDrag.ghost
            .remove();

    }


    document
        .querySelectorAll(
            "[data-drop-zone]"
        )
        .forEach(zone => {

            zone.classList.remove(
                "pointer-drop-active"
            );

            zone.classList.remove(
                "drag-over"
            );

        });


    pointerDrag = {
        active: false,
        index: null,
        card: null,
        ghost: null,
        startX: 0,
        startY: 0,
        offsetX: 0,
        offsetY: 0,
        moved: false
    };

}

/* =========================================================
   PIPELINE LOCAL STORAGE
   FE prototype only - chưa kết nối Backend
========================================================= */

function loadPipelineOpportunities() {

    try {

        const value =
            JSON.parse(
                localStorage.getItem(
                    "crm_ui_opportunities"
                )
            );

        return Array.isArray(value)
            ? value
            : [];

    } catch {

        return [];

    }

}


function savePipelineOpportunities() {

    localStorage.setItem(
        "crm_ui_opportunities",
        JSON.stringify(
            opportunities
        )
    );

}
