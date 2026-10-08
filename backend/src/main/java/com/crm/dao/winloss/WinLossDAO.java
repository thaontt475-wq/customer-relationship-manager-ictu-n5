package com.crm.dao.winloss;

import com.crm.config.DatabaseConfig;
import com.crm.model.winloss.Competitor;
import com.crm.model.winloss.WinLossReason;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WinLossDAO {

    public List<WinLossReason> findReasons(
            String type,
            Boolean active
    ) throws SQLException {

        StringBuilder sql =
                new StringBuilder("""
                SELECT id, type, name, active
                FROM win_loss_reasons
                WHERE 1 = 1
                """);

        List<Object> params =
                new ArrayList<>();

        if (type != null) {
            sql.append(" AND type = ?");
            params.add(type);
        }

        if (active != null) {
            sql.append(" AND active = ?");
            params.add(active);
        }

        sql.append(" ORDER BY name");

        List<WinLossReason> result =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject(
                        i + 1,
                        params.get(i)
                );
            }

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    WinLossReason reason =
                            new WinLossReason();

                    reason.setId(
                            rs.getLong("id")
                    );

                    reason.setType(
                            rs.getString("type")
                    );

                    reason.setName(
                            rs.getString("name")
                    );

                    reason.setActive(
                            rs.getBoolean("active")
                    );

                    result.add(reason);
                }
            }
        }

        return result;
    }

    public WinLossReason createReason(
            WinLossReason reason
    ) throws SQLException {

        String sql = """
                INSERT INTO win_loss_reasons(
                    type,
                    name,
                    active
                )
                VALUES (?, ?, TRUE)
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setString(
                    1,
                    reason.getType()
            );

            statement.setString(
                    2,
                    reason.getName()
            );

            statement.executeUpdate();

            try (
                    ResultSet keys =
                            statement.getGeneratedKeys()
            ) {

                if (keys.next()) {
                    reason.setId(
                            keys.getLong(1)
                    );
                }
            }
        }

        reason.setActive(true);

        return reason;
    }

    public List<Competitor> findCompetitors(
            String keyword,
            Boolean active
    ) throws SQLException {

        StringBuilder sql =
                new StringBuilder("""
                SELECT id, name, note, active
                FROM competitors
                WHERE 1 = 1
                """);

        List<Object> params =
                new ArrayList<>();

        if (
                keyword != null &&
                !keyword.isBlank()
        ) {

            sql.append(
                    " AND LOWER(name) LIKE ?"
            );

            params.add(
                    "%" +
                    keyword.toLowerCase() +
                    "%"
            );
        }

        if (active != null) {
            sql.append(" AND active = ?");
            params.add(active);
        }

        sql.append(" ORDER BY name");

        List<Competitor> result =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject(
                        i + 1,
                        params.get(i)
                );
            }

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                while (rs.next()) {

                    Competitor competitor =
                            new Competitor();

                    competitor.setId(
                            rs.getLong("id")
                    );

                    competitor.setName(
                            rs.getString("name")
                    );

                    competitor.setNote(
                            rs.getString("note")
                    );

                    competitor.setActive(
                            rs.getBoolean("active")
                    );

                    result.add(competitor);
                }
            }
        }

        return result;
    }

    public Competitor createCompetitor(
            Competitor competitor
    ) throws SQLException {

        String sql = """
                INSERT INTO competitors(
                    name,
                    note,
                    active
                )
                VALUES (?, ?, TRUE)
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setString(
                    1,
                    competitor.getName()
            );

            statement.setString(
                    2,
                    competitor.getNote()
            );

            statement.executeUpdate();

            try (
                    ResultSet keys =
                            statement.getGeneratedKeys()
            ) {

                if (keys.next()) {
                    competitor.setId(
                            keys.getLong(1)
                    );
                }
            }
        }

        competitor.setActive(true);

        return competitor;
    }
}