"use strict";

/*
 * Audit UI hiện không sinh log giả.
 * Backend sẽ cung cấp dữ liệu thật sau.
 */

const auditLogs = [];


[
    "auditUser",
    "auditObject",
    "auditFrom",
    "auditTo"
]
.forEach(
    id => {

        document
            .getElementById(id)
            .addEventListener(
                id === "auditUser"
                    ? "input"
                    : "change",
                render
            );
    }
);


document
    .getElementById(
        "clearAudit"
    )
    .addEventListener(
        "click",
        () => {

            document
                .getElementById(
                    "auditUser"
                )
                .value = "";

            document
                .getElementById(
                    "auditObject"
                )
                .value = "";

            document
                .getElementById(
                    "auditFrom"
                )
                .value = "";

            document
                .getElementById(
                    "auditTo"
                )
                .value = "";

            render();

        }
    );


function render() {

    const body =
        document.getElementById(
            "auditBody"
        );


    body.innerHTML = "";


    document
        .getElementById(
            "auditEmpty"
        )
        .style
        .display =
            auditLogs.length
            ?
            "none"
            :
            "flex";

}


render();