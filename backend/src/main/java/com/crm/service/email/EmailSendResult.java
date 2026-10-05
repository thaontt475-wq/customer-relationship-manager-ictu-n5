package com.crm.service.email;

import java.io.Serializable;

public record EmailSendResult(
        boolean success,
        String statusMessage,
        String resetLink,
        String subject,
        String content,
        String toEmail
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
