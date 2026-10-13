package com.crm.dao.winloss;

import com.crm.config.DatabaseConfig;
import com.crm.model.winloss.Competitor;
import com.crm.model.winloss.WinLossReason;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WinLossDAO {

    private synchronized void ensureReasonsSeeded() {
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM win_loss_reasons")) {
            if (rs.next() && rs.getInt(1) == 0) {
                String[] wonReasons = {
                    "Giá cả cạnh tranh",
                    "Chất lượng sản phẩm/dịch vụ vượt trội",
                    "Chăm sóc & tư vấn chuyên nghiệp",
                    "Uy tín thương hiệu",
                    "Giải pháp phù hợp nhu cầu"
                };
                for (String name : wonReasons) {
                    try (PreparedStatement ins = connection.prepareStatement("INSERT INTO win_loss_reasons(type, name, active) VALUES ('WON', ?, TRUE)")) {
                        ins.setString(1, name);
                        ins.executeUpdate();
                    }
                }

                String[] lostReasons = {
                    "Giá quá cao so với ngân sách",
                    "Tính năng chưa đáp ứng đủ",
                    "Chọn đối thủ cạnh tranh khác",
                    "Khách hàng hoãn / hủy dự án",
                    "Không có nhu cầu / dừng liên hệ"
                };
                for (String name : lostReasons) {
                    try (PreparedStatement ins = connection.prepareStatement("INSERT INTO win_loss_reasons(type, name, active) VALUES ('LOST', ?, TRUE)")) {
                        ins.setString(1, name);
                        ins.executeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("ensureReasonsSeeded error: " + e.getMessage());
        }
    }

    private synchronized void ensureCompetitorsSeeded() {
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM competitors")) {
            if (rs.next() && rs.getInt(1) == 0) {
                String[][] comps = {
                    {"Base CRM", "Nền tảng quản trị doanh nghiệp"},
                    {"MISA AMIS", "Phần mềm quản lý bán hàng MISA"},
                    {"1Office", "Phần mềm quản trị tổng thể doanh nghiệp"},
                    {"FastWork", "Quản lý công việc và khách hàng"},
                    {"Khác", "Đối thủ cạnh tranh khác trên thị trường"}
                };
                for (String[] comp : comps) {
                    try (PreparedStatement ins = connection.prepareStatement("INSERT INTO competitors(name, note, active) VALUES (?, ?, TRUE)")) {
                        ins.setString(1, comp[0]);
                        ins.setString(2, comp[1]);
                        ins.executeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("ensureCompetitorsSeeded error: " + e.getMessage());
        }
    }

    public List<WinLossReason> findReasons(
            String type,
            Boolean active
    ) throws SQLException {
        ensureReasonsSeeded();

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

        sql.append(" ORDER BY id ASC");

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
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement check = connection.prepareStatement("SELECT id, type, name, active FROM win_loss_reasons WHERE type = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?))")) {
            check.setString(1, reason.getType());
            check.setString(2, reason.getName());
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    reason.setId(rs.getLong("id"));
                    reason.setType(rs.getString("type"));
                    reason.setName(rs.getString("name"));
                    reason.setActive(rs.getBoolean("active"));
                    return reason;
                }
            }
        }

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
        ensureCompetitorsSeeded();

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

        sql.append(" ORDER BY id ASC");

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
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement check = connection.prepareStatement("SELECT id, name, note, active FROM competitors WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))")) {
            check.setString(1, competitor.getName());
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    competitor.setId(rs.getLong("id"));
                    competitor.setName(rs.getString("name"));
                    competitor.setNote(rs.getString("note"));
                    competitor.setActive(rs.getBoolean("active"));
                    return competitor;
                }
            }
        }

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