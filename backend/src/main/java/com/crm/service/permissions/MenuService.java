package com.crm.service.permissions;

import java.util.*;

public class MenuService {

    public List<Map<String, String>> getMenu(
            Set<String> permissions
    ) {

        List<Map<String, String>> menu =
                new ArrayList<>();

        /*
         * Dashboard luôn hiển thị sau khi đăng nhập.
         */
        menu.add(
                item(
                        "DASHBOARD",
                        "Tổng quan",
                        "dashboard.html",
                        "workspace",
                        "dashboard"
                )
        );


        /* =====================================================
           WORKSPACE
        ===================================================== */

        /*
         * Các module nghiệp vụ Customer / Opportunity /
         * Activity / Quote sẽ tự xuất hiện khi permission
         * tương ứng được bổ sung ở các sprint sau.
         */

        if (permissions.contains("customer.read")) {

            menu.add(
                    item(
                            "CUSTOMERS",
                            "Khách hàng",
                            "customers.html",
                            "workspace",
                            "customer"
                    )
            );
        }


        if (permissions.contains("opportunity.read")) {

            menu.add(
                    item(
                            "OPPORTUNITIES",
                            "Cơ hội",
                            "pipeline.html",
                            "workspace",
                            "opportunity"
                    )
            );
        }


        if (permissions.contains("activity.read")) {

            menu.add(
                    item(
                            "ACTIVITIES",
                            "Hoạt động",
                            "activities.html",
                            "workspace",
                            "activity"
                    )
            );

            menu.add(
                    item(
                            "CALENDAR",
                            "Lịch",
                            "calendar.html",
                            "workspace",
                            "calendar"
                    )
            );
        }


        if (permissions.contains("task.read")) {

            menu.add(
                    item(
                            "TASKS",
                            "Công việc",
                            "tasks.html",
                            "workspace",
                            "task"
                    )
            );
        }


        if (permissions.contains("quote.read")) {

            menu.add(
                    item(
                            "QUOTATIONS",
                            "Báo giá",
                            "quotations.html",
                            "workspace",
                            "quote"
                    )
            );
        }


        if (permissions.contains("product.read")) {

            menu.add(
                    item(
                            "PRODUCTS",
                            "Sản phẩm",
                            "products.html",
                            "workspace",
                            "product"
                    )
            );
        }


        /* =====================================================
           ADMIN
        ===================================================== */

        if (permissions.contains("user.read")) {

            menu.add(
                    item(
                            "USERS",
                            "Người dùng",
                            "users.html",
                            "admin",
                            "users"
                    )
            );
        }


        if (permissions.contains("user.import")) {

            menu.add(
                    item(
                            "USER_IMPORT",
                            "Nhập người dùng Excel",
                            "import-users.html",
                            "admin",
                            "import"
                    )
            );
        }


        if (permissions.contains("audit.read")) {

            menu.add(
                    item(
                            "AUDIT",
                            "Nhật ký hệ thống",
                            "audit-log.html",
                            "admin",
                            "audit"
                    )
            );
        }


        if (permissions.contains("organization.read")) {

            menu.add(
                    item(
                            "ORGANIZATION",
                            "Tổ chức",
                            "organization.html",
                            "admin",
                            "organization"
                    )
            );
        }


        if (permissions.contains("masterdata.read")) {

            menu.add(
                    item(
                            "MASTER_DATA",
                            "Danh mục dùng chung",
                            "master-data.html",
                            "admin",
                            "master"
                    )
            );
        }


        if (permissions.contains("customfield.read")) {

            menu.add(
                    item(
                            "CUSTOM_FIELDS",
                            "Trường tùy chỉnh",
                            "custom-fields.html",
                            "admin",
                            "custom"
                    )
            );
        }


        if (permissions.contains("pipeline.read")) {

            menu.add(
                    item(
                            "PIPELINE_SETTINGS",
                            "Cấu hình Pipeline",
                            "pipeline-settings.html",
                            "admin",
                            "settings"
                    )
            );
        }


        if (
                permissions.contains("winloss.read")
                ||
                permissions.contains("competitor.read")
        ) {

            menu.add(
                    item(
                            "WIN_LOSS",
                            "Lý do thắng/thua & Đối thủ",
                            "win-loss-settings.html",
                            "admin",
                            "winLoss"
                    )
            );
        }


        return menu;
    }


    private Map<String, String> item(
            String code,
            String label,
            String path,
            String section,
            String icon
    ) {

        Map<String, String> item =
                new LinkedHashMap<>();

        item.put(
                "code",
                code
        );

        item.put(
                "label",
                label
        );

        item.put(
                "path",
                path
        );

        item.put(
                "section",
                section
        );

        item.put(
                "icon",
                icon
        );

        return item;
    }
}