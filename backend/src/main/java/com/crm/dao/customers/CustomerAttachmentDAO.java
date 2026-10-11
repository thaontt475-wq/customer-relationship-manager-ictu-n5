package com.crm.dao.customers;

import com.crm.config.DatabaseConfig;
import java.sql.*;

public class CustomerAttachmentDAO {
    public record Attachment(long customerId, String name, String location) {}
    public Attachment find(long id) throws SQLException {
        try (Connection c=DatabaseConfig.getConnection(); PreparedStatement p=c.prepareStatement(
                "SELECT customer_id,file_name,file_url FROM customer_attachments WHERE id=? AND is_deleted=0")) {
            p.setLong(1,id);
            try (ResultSet r=p.executeQuery()) {
                return r.next() ? new Attachment(r.getLong(1),r.getString(2),r.getString(3)) : null;
            }
        }
    }
}
