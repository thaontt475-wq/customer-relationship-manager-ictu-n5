package com.crm.service.users;

import java.sql.Connection;
import java.sql.SQLException;

public class OwnershipTransferService {

    public void transferAll(Connection conn, long targetUserId, long recipientUserId) throws SQLException {
        // No business ownership tables exist in the current Sprint-1 schema.
        // This extension point must not be treated as evidence that Sales data was transferred.
    }
}
