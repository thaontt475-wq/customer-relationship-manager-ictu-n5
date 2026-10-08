package com.crm.service.permissions;

import com.crm.dao.permissions.PermissionDAO;
import com.crm.dao.users.UserDAO;
import com.crm.service.audit.AuditLogService;
import com.crm.util.JsonUtil;

import java.util.*;

public class PermissionService {

    private final PermissionDAO dao =
            new PermissionDAO();

    private final UserDAO userDAO =
            new UserDAO();

    private final AuditLogService audit =
            new AuditLogService();


    public Set<String> getPermissions(
            long userId
    ) throws Exception {

        return dao.findByUserId(
                userId
        );
    }


    public boolean hasPermission(
            long userId,
            String permission
    ) throws Exception {

        return getPermissions(
                userId
        )
                .contains(
                        permission
                );
    }


    public Map<String, Object> getUserPermissions(
            long userId
    ) throws Exception {

        if (
                !userDAO.existsById(
                        userId
                )
        ) {
            return null;
        }


        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "userId",
                userId
        );

        data.put(
                "roles",
                dao.findRolesByUserId(
                        userId
                )
        );

        data.put(
                "dataScope",
                dao.findDataScope(
                        userId
                )
        );

        data.put(
                "permissions",
                new TreeSet<>(
                        dao.findByUserId(
                                userId
                        )
                )
        );

        return data;
    }


    public Map<String, Object> assign(
            long userId,
            List<Long> roleIds,
            String dataScope,
            long currentUserId
    ) throws Exception {

        if (
                !userDAO.existsById(
                        userId
                )
        ) {

            throw new IllegalArgumentException(
                    "User không tồn tại"
            );
        }


        if (
                roleIds == null ||
                roleIds.isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "Phải có ít nhất một role"
            );
        }


        for (
                Long roleId :
                roleIds
        ) {

            if (
                    roleId == null ||
                    !dao.roleExists(
                            roleId
                    )
            ) {

                throw new IllegalArgumentException(
                        "Role không hợp lệ"
                );
            }
        }


        if (
                dataScope == null
        ) {

            throw new IllegalArgumentException(
                    "dataScope là bắt buộc"
            );
        }


        dataScope =
                dataScope.trim()
                        .toUpperCase();


        if (
                !dataScope.equals(
                        "SELF"
                ) &&
                !dataScope.equals(
                        "TEAM"
                ) &&
                !dataScope.equals(
                        "ALL"
                )
        ) {

            throw new IllegalArgumentException(
                    "dataScope chỉ nhận SELF, TEAM hoặc ALL"
            );
        }


        Map<String, Object> before =
                getUserPermissions(
                        userId
                );


        dao.assignRoles(
                userId,
                roleIds,
                dataScope,
                currentUserId
        );


        Map<String, Object> after =
                getUserPermissions(
                        userId
                );


        audit.log(
                currentUserId,
                "USER_PERMISSION",
                String.valueOf(
                        userId
                ),
                "ASSIGN_ROLES",
                "Cập nhật role và phạm vi dữ liệu",
                JsonUtil.getGson()
                        .toJson(before),
                JsonUtil.getGson()
                        .toJson(after)
        );


        return after;
    }
}
