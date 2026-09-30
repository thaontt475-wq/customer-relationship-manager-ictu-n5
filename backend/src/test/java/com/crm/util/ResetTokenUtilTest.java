package com.crm.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResetTokenUtilTest {

    @Test
    void resetTokenExpiresAfterApproximatelyThirtyMinutes() {
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime expiresAt = ResetTokenUtil.calculateExpiry();
        long seconds = Duration.between(before, expiresAt).getSeconds();

        assertTrue(seconds >= 29 * 60 && seconds <= 31 * 60);
    }
}
