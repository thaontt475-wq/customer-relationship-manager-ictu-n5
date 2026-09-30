package com.crm.service.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Single-instance temporary login lockout. Successful authentication clears the
 * consecutive failure count; the fifth failure blocks the account key for 15 minutes.
 */
final class LoginAttemptGuard {
    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final Supplier<Instant> nowSupplier;

    LoginAttemptGuard() {
        this(Instant::now);
    }

    LoginAttemptGuard(Supplier<Instant> nowSupplier) {
        this.nowSupplier = Objects.requireNonNull(nowSupplier);
    }

    boolean isBlocked(String accountKey) {
        if (accountKey == null || accountKey.isBlank()) {
            return false;
        }

        Instant now = nowSupplier.get();
        AttemptState state = attempts.computeIfPresent(accountKey, (key, current) ->
                current.blockedUntil() != null && !now.isBefore(current.blockedUntil())
                        ? null
                        : current);
        return state != null && state.blockedUntil() != null;
    }

    void recordFailure(String accountKey) {
        if (accountKey == null || accountKey.isBlank()) {
            return;
        }

        Instant now = nowSupplier.get();
        attempts.compute(accountKey, (key, current) -> {
            if (current != null && current.blockedUntil() != null
                    && now.isBefore(current.blockedUntil())) {
                return current;
            }

            int failures = current == null ? 1 : current.failures() + 1;
            Instant blockedUntil = failures >= MAX_FAILURES
                    ? now.plus(LOCK_DURATION)
                    : null;
            return new AttemptState(failures, blockedUntil);
        });
    }

    void recordSuccess(String accountKey) {
        if (accountKey != null) {
            attempts.remove(accountKey);
        }
    }

    private record AttemptState(int failures, Instant blockedUntil) {
    }
}
