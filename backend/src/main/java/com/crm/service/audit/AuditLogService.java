package com.crm.service.audit;

import com.crm.dao.audit.AuditLogDAO;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuditLogService {

    private final AuditLogDAO dao =
            new AuditLogDAO();


    public void log(
            Long userId,
            String entity,
            String entityId,
            String action,
            String description
    ) {

        try {

            dao.insert(
                    userId,
                    entity,
                    entityId,
                    action,
                    description
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    public void log(
            Long userId,
            String entity,
            String entityId,
            String action,
            String description,
            String beforeValue,
            String afterValue
    ) {

        try {

            dao.insert(
                    userId,
                    entity,
                    entityId,
                    action,
                    description,
                    beforeValue,
                    afterValue
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    public Map<String, Object> search(
            String entity,
            String action,
            Long userId,
            String from,
            String to,
            int page,
            int size
    ) throws Exception {

        if (
                page < 1 ||
                size < 1 ||
                size > 100
        ) {

            throw new IllegalArgumentException(
                    "page/size không hợp lệ"
            );
        }


        if (
                from != null &&
                !from.isBlank()
        ) {

            LocalDate.parse(
                    from
            );
        }


        if (
                to != null &&
                !to.isBlank()
        ) {

            LocalDate.parse(
                    to
            );
        }


        if (
                from != null &&
                !from.isBlank() &&
                to != null &&
                !to.isBlank() &&
                LocalDate.parse(from)
                        .isAfter(
                                LocalDate.parse(to)
                        )
        ) {

            throw new IllegalArgumentException(
                    "from không được sau to"
            );
        }


        long totalItems =
                dao.count(
                        entity,
                        action,
                        userId,
                        from,
                        to
                );


        int totalPages =
                (int) Math.ceil(
                        totalItems /
                        (double) size
                );


        Map<String, Object> data =
                new LinkedHashMap<>();


        data.put(
                "items",
                dao.search(
                        entity,
                        action,
                        userId,
                        from,
                        to,
                        page,
                        size
                )
        );

        data.put(
                "page",
                page
        );

        data.put(
                "size",
                size
        );

        data.put(
                "totalItems",
                totalItems
        );

        data.put(
                "totalPages",
                totalPages
        );


        return data;
    }
}