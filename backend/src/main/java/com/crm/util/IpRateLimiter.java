package com.crm.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe In-memory Rate Limiter per IP address.
 * Enforces a maximum number of requests (default: 5 submissions) within a time window (default: 1 minute).
 */
public final class IpRateLimiter {

    private static final IpRateLimiter INSTANCE = new IpRateLimiter();

    // Default configuration: max 5 requests per 60 seconds (1 minute)
    public static final int DEFAULT_MAX_REQUESTS = 5;
    public static final long DEFAULT_WINDOW_MILLIS = 60_000L; // 1 minute

    private final int maxRequests;
    private final long windowMillis;

    // Map: IP -> WindowData
    private final ConcurrentHashMap<String, WindowData> ipWindowMap = new ConcurrentHashMap<>();

    private IpRateLimiter() {
        this(DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_MILLIS);
    }

    public IpRateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    public static IpRateLimiter getInstance() {
        return INSTANCE;
    }

    /**
     * Checks if a request from the given IP is allowed.
     * If allowed, increments the counter.
     *
     * @param ip IP address of the client
     * @return true if allowed; false if rate limit exceeded
     */
    public boolean tryAcquire(String ip) {
        if (ip == null || ip.isBlank()) {
            ip = "unknown";
        }

        long now = System.currentTimeMillis();

        // Perform lazy cleanup periodically if map grows large
        if (ipWindowMap.size() > 5000) {
            cleanupExpired(now);
        }

        WindowData window = ipWindowMap.compute(ip, (key, existing) -> {
            if (existing == null || (now - existing.windowStartTime >= windowMillis)) {
                return new WindowData(now, new AtomicInteger(1));
            } else {
                existing.counter.incrementAndGet();
                return existing;
            }
        });

        return window.counter.get() <= maxRequests;
    }

    /**
     * Returns remaining allowed requests for this IP in the current window.
     */
    public int getRemainingRequests(String ip) {
        if (ip == null || ip.isBlank()) {
            return maxRequests;
        }
        long now = System.currentTimeMillis();
        WindowData window = ipWindowMap.get(ip);
        if (window == null || (now - window.windowStartTime >= windowMillis)) {
            return maxRequests;
        }
        int remaining = maxRequests - window.counter.get();
        return Math.max(0, remaining);
    }

    /**
     * Extracts client IP address from request, considering proxy headers.
     */
    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // Proxy headers are client-controlled. A trusted container RemoteIpValve may
        // resolve the original address before this method is called.
        return normalizeIp(request.getRemoteAddr());
    }

    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "127.0.0.1";
        }
        // Normalize IPv6 localhost
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip.trim();
    }

    private void cleanupExpired(long now) {
        ipWindowMap.entrySet().removeIf(entry -> (now - entry.getValue().windowStartTime) > (windowMillis * 2));
    }

    /**
     * Clears all tracked IP windows (useful for testing).
     */
    public void reset() {
        ipWindowMap.clear();
    }

    private static final class WindowData {
        final long windowStartTime;
        final AtomicInteger counter;

        WindowData(long windowStartTime, AtomicInteger counter) {
            this.windowStartTime = windowStartTime;
            this.counter = counter;
        }
    }
}
