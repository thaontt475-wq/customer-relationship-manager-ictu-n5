package com.crm.dao.leads;

import com.crm.config.DatabaseConfig;
import com.crm.model.leads.Lead;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

/** Persistence for public form ownership and transactional submission deduplication. No DDL. */
public class LeadCaptureDAO {
    public String register(long owner, String name) throws SQLException {
        if (name == null || name.isBlank()) name = "crm-web-lead-form";
        if (!name.matches("[A-Za-z0-9_-]{1,100}"))
            throw new IllegalArgumentException("formId chỉ gồm chữ, số, _ và -, tối đa 100 ký tự");
        try (Connection c = DatabaseConfig.getConnection()) {
            try (PreparedStatement p = c.prepareStatement("""
                    INSERT INTO lead_capture_forms(token,owner_user_id,form_name)
                    VALUES (?,?,?) ON DUPLICATE KEY UPDATE form_name=VALUES(form_name)
                    """)) {
                p.setString(1, UUID.randomUUID().toString()); p.setLong(2, owner); p.setString(3, name);
                p.executeUpdate();
            }
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT token FROM lead_capture_forms WHERE owner_user_id=? AND form_name=?")) {
                p.setLong(1, owner); p.setString(2, name);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) throw new SQLException("Cannot register lead form");
                    return r.getString(1);
                }
            }
        }
    }

    public Lead submit(Lead lead, String requestId) throws SQLException {
        String hash = fingerprint(lead);
        try (Connection c = DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Serializes submissions for this form, including simultaneous retries.
                try (PreparedStatement p = c.prepareStatement("""
                        SELECT f.owner_user_id FROM lead_capture_forms f
                        JOIN users u ON u.id=f.owner_user_id AND u.status='ACTIVE'
                        WHERE f.token=? AND f.active=1
                        AND EXISTS (SELECT 1 FROM user_roles ur
                          JOIN role_permissions rp ON rp.role_id=ur.role_id
                          JOIN permissions perm ON perm.id=rp.permission_id
                          WHERE ur.user_id=u.id AND perm.code='lead.create') FOR UPDATE
                        """)) {
                    p.setString(1, lead.getFormId());
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("Biểu mẫu không tồn tại hoặc đã ngừng hoạt động");
                        lead.setOwnerUserId(r.getLong(1));
                    }
                }
                try (PreparedStatement p = c.prepareStatement("""
                        SELECT lead_id,payload_hash FROM lead_capture_submissions WHERE form_token=? AND request_id=?
                        """)) {
                    p.setString(1, lead.getFormId()); p.setString(2, requestId);
                    try (ResultSet r = p.executeQuery()) {
                        if (r.next()) {
                            if (!hash.equals(r.getString(2)))
                                throw new IllegalStateException("Mã gửi đã được dùng cho dữ liệu khác. Vui lòng tải lại biểu mẫu.");
                            lead.setId(r.getLong(1)); c.commit(); return lead;
                        }
                    }
                }
                try (PreparedStatement p = c.prepareStatement("""
                        SELECT id,name FROM master_data WHERE type='lead-source' AND active=1
                        AND code IN ('WEBSITE','WEB_FORM') ORDER BY code LIMIT 1 FOR SHARE
                        """ ); ResultSet r = p.executeQuery()) {
                    if (!r.next()) throw new IllegalArgumentException("Nguồn Lead Website chưa được cấu hình hoặc đã ngừng hoạt động");
                    lead.setSourceId(r.getLong(1)); lead.setSourceName(r.getString(2));
                }
                // Use the validated, normalized source and owner on the same transaction.
                try (PreparedStatement p = c.prepareStatement("""
                        INSERT INTO leads(full_name,email,phone,company_name,notes,source,source_name,source_id,
                          owner_user_id,form_id,ip_address,status) VALUES (?,?,?,?,?,'WEB_FORM',?,?,?,?,?,'NEW')
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    p.setString(1, lead.getFullName()); p.setString(2, lead.getEmail()); p.setString(3, lead.getPhone());
                    p.setString(4, lead.getCompanyName()); p.setString(5, lead.getNotes()); p.setString(6, lead.getSourceName());
                    p.setLong(7, lead.getSourceId()); p.setLong(8, lead.getOwnerUserId());
                    p.setString(9, lead.getFormId()); p.setString(10, lead.getIpAddress()); p.executeUpdate();
                    try (ResultSet r = p.getGeneratedKeys()) {
                        if (!r.next()) throw new SQLException("Cannot generate lead ID");
                        lead.setId(r.getLong(1));
                    }
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE leads SET code=? WHERE id=?")) {
                    p.setString(1, lead.getCode()); p.setLong(2, lead.getId()); p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("""
                        INSERT INTO lead_capture_submissions(form_token,request_id,payload_hash,lead_id) VALUES (?,?,?,?)
                        """)) {
                    p.setString(1, lead.getFormId()); p.setString(2, requestId); p.setString(3, hash);
                    p.setLong(4, lead.getId()); p.executeUpdate();
                }
                c.commit(); return lead;
            } catch (SQLException | RuntimeException e) {
                c.rollback(); throw e;
            }
        }
    }

    private String fingerprint(Lead lead) {
        try {
            String payload = com.crm.util.JsonUtil.getGson().toJson(Arrays.asList(
                    lead.getFullName(), lead.getEmail(), lead.getPhone(), lead.getCompanyName(), lead.getNotes(), lead.getSource()));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
