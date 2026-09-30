package com.crm.service.email;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailServiceTest {

    @Test
    void passwordResetEmailContainsLinkAndExactThirtyMinuteNotice() {
        String resetLink = "http://localhost:8080/reset-password?token=test-token";

        String content = EmailService.buildPasswordResetEmailContent(resetLink);

        assertTrue(content.contains(resetLink));
        assertTrue(content.contains("Liên kết có hiệu lực trong 30 phút."));
    }
}
