package com.crm.service.users;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OwnershipTransferService {

    public boolean isSupported() {
        return true;
    }

    public void lockOwnershipRows(Connection conn, long targetUserId) throws SQLException {
        lockRows(conn, "customers", targetUserId);
        lockRows(conn, "opportunities", targetUserId);
    }

    public boolean hasOwnership(Connection conn, long targetUserId) throws SQLException {
        return countOwnedRows(conn, "customers", targetUserId) > 0
                || countOwnedRows(conn, "opportunities", targetUserId) > 0;
    }

    public void transferAll(Connection conn, long targetUserId, long recipientUserId) throws SQLException {
        transferRows(conn, "customers", targetUserId, recipientUserId);
        transferRows(conn, "opportunities", targetUserId, recipientUserId);
    }

    public List<OwnedRecord> findOwnedRecords(Connection conn, long ownerUserId) throws SQLException {
        List<OwnedRecord> records = new ArrayList<>();
        records.addAll(findOwnedRecords(conn, "customers", "CUSTOMER", ownerUserId));
        records.addAll(findOwnedRecords(conn, "opportunities", "OPPORTUNITY", ownerUserId));
        return records;
    }

    public boolean isTransferComplete(Connection conn, long targetUserId) throws SQLException {
        long remainingCustomers = countOwnedRows(conn, "customers", targetUserId);
        long remainingOpportunities = countOwnedRows(conn, "opportunities", targetUserId);
        return remainingCustomers == 0 && remainingOpportunities == 0;
    }

    private void lockRows(Connection conn, String table, long ownerUserId) throws SQLException {
        String sql = "SELECT id FROM " + table + " WHERE owner_user_id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ownerUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Consume every row so all matching ownership records are locked.
                }
            }
        }
    }

    private void transferRows(Connection conn, String table, long targetUserId,
                              long recipientUserId) throws SQLException {
        String sql = "UPDATE " + table + " SET owner_user_id = ? WHERE owner_user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, recipientUserId);
            stmt.setLong(2, targetUserId);
            stmt.executeUpdate();
        }
    }

    private long countOwnedRows(Connection conn, String table, long ownerUserId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE owner_user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ownerUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private List<OwnedRecord> findOwnedRecords(Connection conn, String table, String objectType,
                                               long ownerUserId) throws SQLException {
        String sql = "SELECT id FROM " + table + " WHERE owner_user_id = ? ORDER BY id";
        List<OwnedRecord> records = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ownerUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(new OwnedRecord(objectType, rs.getLong("id")));
                }
            }
        }
        return records;
    }

    public record OwnedRecord(String objectType, long objectId) { }
}
