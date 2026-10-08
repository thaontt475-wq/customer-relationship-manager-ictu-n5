package com.crm.service.users;

import com.crm.config.DatabaseConfig;
import com.crm.dao.users.UserDAO;
import com.crm.service.audit.AuditLogService;
import com.crm.util.JsonUtil;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserStatusService {

    private final UserDAO userDAO =
            new UserDAO();

    private final AuditLogService audit =
            new AuditLogService();


    public Map<String, Object> lock(
            long targetUserId,
            long currentUserId,
            String reason
    ) throws Exception {

        if (
                targetUserId ==
                currentUserId
        ) {

            throw new IllegalArgumentException(
                    "Không được tự khóa chính mình"
            );
        }


        if (
                !userDAO.existsById(
                        targetUserId
                )
        ) {

            throw new IllegalArgumentException(
                    "User không tồn tại"
            );
        }


        String beforeStatus =
                getStatus(
                        targetUserId
                );


        logStatus(
                targetUserId,
                currentUserId,
                "LOCK",
                reason
        );


        Map<String, Object> before =
                new LinkedHashMap<>();

        before.put(
                "status",
                beforeStatus
        );


        Map<String, Object> after =
                new LinkedHashMap<>();

        after.put(
                "status",
                "LOCKED"
        );

        after.put(
                "reason",
                reason
        );


        audit.log(
                currentUserId,
                "USER",
                String.valueOf(
                        targetUserId
                ),
                "LOCK",
                "Khóa tài khoản người dùng",
                JsonUtil.getGson()
                        .toJson(before),
                JsonUtil.getGson()
                        .toJson(after)
        );


        return statusResult(
                targetUserId,
                "LOCKED"
        );
    }


    public Map<String, Object> unlock(
            long targetUserId,
            long currentUserId
    ) throws Exception {

        if (
                !userDAO.existsById(
                        targetUserId
                )
        ) {

            throw new IllegalArgumentException(
                    "User không tồn tại"
            );
        }


        String beforeStatus =
                getStatus(
                        targetUserId
                );


        logStatus(
                targetUserId,
                currentUserId,
                "UNLOCK",
                null
        );


        Map<String, Object> before =
                new LinkedHashMap<>();

        before.put(
                "status",
                beforeStatus
        );


        Map<String, Object> after =
                new LinkedHashMap<>();

        after.put(
                "status",
                "ACTIVE"
        );


        audit.log(
                currentUserId,
                "USER",
                String.valueOf(
                        targetUserId
                ),
                "UNLOCK",
                "Mở khóa tài khoản người dùng",
                JsonUtil.getGson()
                        .toJson(before),
                JsonUtil.getGson()
                        .toJson(after)
        );


        return statusResult(
                targetUserId,
                "ACTIVE"
        );
    }


    public Map<String, Object> transfer(
            long fromUserId,
            long toUserId,
            long currentUserId
    ) throws Exception {

        if (
                fromUserId ==
                toUserId
        ) {

            throw new IllegalArgumentException(
                    "User nguồn và user nhận phải khác nhau"
            );
        }


        if (
                !userDAO.existsById(
                        fromUserId
                ) ||
                !userDAO.existsById(
                        toUserId
                )
        ) {

            throw new IllegalArgumentException(
                    "User nguồn hoặc user nhận không tồn tại"
            );
        }


        int organizationUnits;


        try (
                Connection connection =
                        DatabaseConfig.getConnection()
        ) {

            connection.setAutoCommit(
                    false
            );


            try {

                try (
                        PreparedStatement statement =
                                connection.prepareStatement("""
                                UPDATE organization_units
                                SET manager_id = ?
                                WHERE manager_id = ?
                                """)
                ) {

                    statement.setLong(
                            1,
                            toUserId
                    );

                    statement.setLong(
                            2,
                            fromUserId
                    );


                    organizationUnits =
                            statement.executeUpdate();
                }


                try (
                        PreparedStatement statement =
                                connection.prepareStatement("""
                                INSERT INTO data_transfer_logs(
                                    from_user_id,
                                    to_user_id,
                                    performed_by
                                )
                                VALUES (?, ?, ?)
                                """)
                ) {

                    statement.setLong(
                            1,
                            fromUserId
                    );

                    statement.setLong(
                            2,
                            toUserId
                    );

                    statement.setLong(
                            3,
                            currentUserId
                    );

                    statement.executeUpdate();
                }


                connection.commit();


            } catch (Exception e) {

                connection.rollback();

                throw e;
            }
        }


        Map<String, Object> counts =
                new LinkedHashMap<>();

        counts.put(
                "organizationUnits",
                organizationUnits
        );

        counts.put(
                "total",
                organizationUnits
        );


        Map<String, Object> before =
                new LinkedHashMap<>();

        before.put(
                "ownerUserId",
                fromUserId
        );


        Map<String, Object> after =
                new LinkedHashMap<>();

        after.put(
                "ownerUserId",
                toUserId
        );

        after.put(
                "organizationUnits",
                organizationUnits
        );


        audit.log(
                currentUserId,
                "USER",
                String.valueOf(
                        fromUserId
                ),
                "TRANSFER_DATA",
                "Chuyển dữ liệu sang user #" +
                        toUserId,
                JsonUtil.getGson()
                        .toJson(before),
                JsonUtil.getGson()
                        .toJson(after)
        );


        return counts;
    }


    private void logStatus(
            long userId,
            long performedBy,
            String action,
            String reason
    ) throws Exception {

        String sql = """
                INSERT INTO account_lock_logs(
                    user_id,
                    action_type,
                    reason,
                    performed_by
                )
                VALUES (?, ?, ?, ?)
                """;


        try (
                Connection connection =
                        DatabaseConfig.getConnection()
        ) {

            connection.setAutoCommit(
                    false
            );


            try (
                    PreparedStatement update =
                            connection.prepareStatement("""
                            UPDATE users
                            SET
                                status = ?,
                                session_version = session_version + 1,
                                failed_login_attempts = 0,
                                locked_until = NULL
                            WHERE id = ?
                              AND status <> 'DELETED'
                            """);

                    PreparedStatement statement =
                            connection.prepareStatement(
                                    sql
                            )
            ) {

                update.setString(
                        1,
                        "LOCK".equals(action)
                                ? "LOCKED"
                                : "ACTIVE"
                );

                update.setLong(
                        2,
                        userId
                );


                if (
                        update.executeUpdate()
                        != 1
                ) {

                    throw new IllegalArgumentException(
                            "User không tồn tại"
                    );
                }


                statement.setLong(
                        1,
                        userId
                );

                statement.setString(
                        2,
                        action
                );

                statement.setString(
                        3,
                        reason
                );

                statement.setLong(
                        4,
                        performedBy
                );

                statement.executeUpdate();


                connection.commit();


            } catch (Exception e) {

                connection.rollback();

                throw e;
            }
        }
    }


    private String getStatus(
            long userId
    ) throws SQLException {

        String sql = """
                SELECT status
                FROM users
                WHERE id = ?
                """;


        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        )
        ) {

            statement.setLong(
                    1,
                    userId
            );


            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                if (!rs.next()) {

                    return null;
                }


                return rs.getString(
                        "status"
                );
            }
        }
    }


    private Map<String, Object> statusResult(
            long userId,
            String status
    ) {

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put(
                "userId",
                userId
        );

        result.put(
                "status",
                status
        );

        return result;
    }
}