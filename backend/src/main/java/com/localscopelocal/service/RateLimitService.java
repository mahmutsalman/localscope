package com.localscopelocal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to handle rate limiting for API requests
 * Implements both per-IP and global rate limiting
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    @Value("${rate.limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${rate.limit.ip.requests:10}")
    private int ipRequestLimit;

    @Value("${rate.limit.ip.window.minutes:1}")
    private int ipWindowMinutes;

    @Value("${rate.limit.global.requests:100}")
    private int globalRequestLimit;

    @Value("${rate.limit.global.window.minutes:1}")
    private int globalWindowMinutes;

    @Value("${rate.limit.burst.allowance:5}")
    private int burstAllowance;

    // In-memory storage for rate limiting counters
    private final ConcurrentMap<String, RateLimitCounter> ipCounters = new ConcurrentHashMap<>();
    private final RateLimitCounter globalCounter = new RateLimitCounter();

    /**
     * Check if the request from the given IP address is allowed
     *
     * @param ipAddress The IP address making the request
     * @return RateLimitResult indicating if the request is allowed and remaining quota
     */
    public RateLimitResult isRequestAllowed(String ipAddress) {
        if (!rateLimitEnabled) {
            return new RateLimitResult(true, ipRequestLimit, globalRequestLimit, "Rate limiting disabled");
        }

        // Check global rate limit first
        RateLimitResult globalResult = checkGlobalRateLimit();
        if (!globalResult.isAllowed()) {
            return globalResult;
        }

        // Check IP-specific rate limit
        RateLimitResult ipResult = checkIpRateLimit(ipAddress);
        if (!ipResult.isAllowed()) {
            return ipResult;
        }

        // Both checks passed, increment counters
        incrementCounters(ipAddress);

        return new RateLimitResult(true, 
                Math.max(0, ipRequestLimit - getIpCounter(ipAddress).getCount()),
                Math.max(0, globalRequestLimit - globalCounter.getCount()),
                "Request allowed");
    }

    /**
     * Check global rate limit
     */
    private RateLimitResult checkGlobalRateLimit() {
        cleanupExpiredCounters();
        
        if (globalCounter.getCount() >= globalRequestLimit + burstAllowance) {
            log.warn("Global rate limit exceeded: {} requests in {} minutes", 
                    globalCounter.getCount(), globalWindowMinutes);
            return new RateLimitResult(false, 0, 0, 
                    "Global rate limit exceeded. Try again later.");
        }
        
        return new RateLimitResult(true, 0, 
                Math.max(0, globalRequestLimit - globalCounter.getCount()), "");
    }

    /**
     * Check IP-specific rate limit
     */
    private RateLimitResult checkIpRateLimit(String ipAddress) {
        RateLimitCounter counter = getIpCounter(ipAddress);
        
        if (counter.getCount() >= ipRequestLimit + burstAllowance) {
            log.warn("IP rate limit exceeded for {}: {} requests in {} minutes", 
                    ipAddress, counter.getCount(), ipWindowMinutes);
            return new RateLimitResult(false, 0, 
                    Math.max(0, globalRequestLimit - globalCounter.getCount()),
                    "Too many requests from your IP address. Please try again later.");
        }
        
        return new RateLimitResult(true, 
                Math.max(0, ipRequestLimit - counter.getCount()), 0, "");
    }

    /**
     * Increment both IP and global counters
     */
    private void incrementCounters(String ipAddress) {
        getIpCounter(ipAddress).increment();
        globalCounter.increment();
        
        log.debug("Request allowed for IP: {}. IP count: {}, Global count: {}", 
                ipAddress, getIpCounter(ipAddress).getCount(), globalCounter.getCount());
    }

    /**
     * Get or create counter for IP address
     */
    private RateLimitCounter getIpCounter(String ipAddress) {
        return ipCounters.computeIfAbsent(ipAddress, k -> new RateLimitCounter());
    }

    /**
     * Clean up expired counters to prevent memory leaks
     */
    private void cleanupExpiredCounters() {
        LocalDateTime now = LocalDateTime.now();
        
        // Clean up IP counters
        ipCounters.entrySet().removeIf(entry -> {
            LocalDateTime windowStart = now.minus(ipWindowMinutes, ChronoUnit.MINUTES);
            return entry.getValue().getLastReset().isBefore(windowStart);
        });
        
        // Reset global counter if window expired
        LocalDateTime globalWindowStart = now.minus(globalWindowMinutes, ChronoUnit.MINUTES);
        if (globalCounter.getLastReset().isBefore(globalWindowStart)) {
            globalCounter.reset();
        }
        
        // Reset IP counters if their windows expired
        LocalDateTime ipWindowStart = now.minus(ipWindowMinutes, ChronoUnit.MINUTES);
        ipCounters.values().forEach(counter -> {
            if (counter.getLastReset().isBefore(ipWindowStart)) {
                counter.reset();
            }
        });
    }

    /**
     * Get current rate limit status for monitoring
     */
    public RateLimitStatus getCurrentStatus() {
        cleanupExpiredCounters();
        return new RateLimitStatus(
                rateLimitEnabled,
                globalCounter.getCount(),
                globalRequestLimit,
                ipCounters.size(),
                ipRequestLimit
        );
    }

    /**
     * Counter class to track requests within a time window
     */
    private static class RateLimitCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile LocalDateTime lastReset = LocalDateTime.now();

        public int getCount() {
            return count.get();
        }

        public void increment() {
            count.incrementAndGet();
        }

        public void reset() {
            count.set(0);
            lastReset = LocalDateTime.now();
        }

        public LocalDateTime getLastReset() {
            return lastReset;
        }
    }

    /**
     * Result of rate limit check
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final int remainingIpRequests;
        private final int remainingGlobalRequests;
        private final String message;

        public RateLimitResult(boolean allowed, int remainingIpRequests, 
                             int remainingGlobalRequests, String message) {
            this.allowed = allowed;
            this.remainingIpRequests = remainingIpRequests;
            this.remainingGlobalRequests = remainingGlobalRequests;
            this.message = message;
        }

        public boolean isAllowed() { return allowed; }
        public int getRemainingIpRequests() { return remainingIpRequests; }
        public int getRemainingGlobalRequests() { return remainingGlobalRequests; }
        public String getMessage() { return message; }
    }

    /**
     * Current status of rate limiting for monitoring
     */
    public static class RateLimitStatus {
        private final boolean enabled;
        private final int currentGlobalRequests;
        private final int globalLimit;
        private final int activeIpAddresses;
        private final int ipLimit;

        public RateLimitStatus(boolean enabled, int currentGlobalRequests, int globalLimit,
                             int activeIpAddresses, int ipLimit) {
            this.enabled = enabled;
            this.currentGlobalRequests = currentGlobalRequests;
            this.globalLimit = globalLimit;
            this.activeIpAddresses = activeIpAddresses;
            this.ipLimit = ipLimit;
        }

        public boolean isEnabled() { return enabled; }
        public int getCurrentGlobalRequests() { return currentGlobalRequests; }
        public int getGlobalLimit() { return globalLimit; }
        public int getActiveIpAddresses() { return activeIpAddresses; }
        public int getIpLimit() { return ipLimit; }
    }
} 