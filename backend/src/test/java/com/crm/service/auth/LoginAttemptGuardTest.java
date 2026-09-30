package com.crm.service.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptGuardTest {

    @Test
    void fifthConsecutiveFailureBlocksForFifteenMinutes() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-09-28T00:00:00Z"));
        LoginAttemptGuard guard = new LoginAttemptGuard(now::get);

        for (int attempt = 1; attempt < LoginAttemptGuard.MAX_FAILURES; attempt++) {
            guard.recordFailure("employee@example.test");
            assertFalse(guard.isBlocked("employee@example.test"));
        }

        guard.recordFailure("employee@example.test");
        assertTrue(guard.isBlocked("employee@example.test"));

        now.set(now.get().plus(LoginAttemptGuard.LOCK_DURATION).minusSeconds(1));
        assertTrue(guard.isBlocked("employee@example.test"));

        now.set(now.get().plusSeconds(1));
        assertFalse(guard.isBlocked("employee@example.test"));
    }

    @Test
    void successfulLoginClearsConsecutiveFailures() {
        LoginAttemptGuard guard = new LoginAttemptGuard();
        for (int attempt = 0; attempt < 4; attempt++) {
            guard.recordFailure("employee@example.test");
        }

        guard.recordSuccess("employee@example.test");
        guard.recordFailure("employee@example.test");

        assertFalse(guard.isBlocked("employee@example.test"));
    }
}
