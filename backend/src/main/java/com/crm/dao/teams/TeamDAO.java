package com.crm.dao.teams;

import com.crm.model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TeamDAO {

    public List<Team> findAll(Connection conn) throws SQLException {
        String sql = "SELECT id, name FROM teams ORDER BY name";
        List<Team> teams = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                teams.add(new Team(
                        rs.getLong("id"),
                        rs.getString("name")
                ));
            }
        }

        return teams;
    }

    public Team findById(Connection conn, long teamId) throws SQLException {
        String sql = "SELECT id, name FROM teams WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, teamId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Team(
                            rs.getLong("id"),
                            rs.getString("name")
                    );
                }
            }
        }

        return null;
    }

    public int assignUserToTeam(
            Connection conn,
            long userId,
            long teamId) throws SQLException {

        String sql = "UPDATE users SET team_id = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, teamId);
            stmt.setLong(2, userId);
            return stmt.executeUpdate();
        }
    }
}