package com.localscopelocal.controller;

import com.localscopelocal.service.RateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for administrative and monitoring endpoints
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final RateLimitService rateLimitService;

    @Autowired
    public AdminController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /**
     * Get comprehensive rate limiting statistics
     *
     * @return Rate limiting statistics and current status
     */
    @GetMapping("/rate-limit-stats")
    public ResponseEntity<Map<String, Object>> getRateLimitStats() {
        log.info("Rate limit statistics requested");
        
        RateLimitService.RateLimitStatus status = rateLimitService.getCurrentStatus();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("rateLimitEnabled", status.isEnabled());
        stats.put("global", Map.of(
                "currentRequests", status.getCurrentGlobalRequests(),
                "limit", status.getGlobalLimit(),
                "utilizationPercentage", Math.round(((double) status.getCurrentGlobalRequests() / status.getGlobalLimit()) * 100)
        ));
        stats.put("perIp", Map.of(
                "activeIpAddresses", status.getActiveIpAddresses(),
                "ipLimit", status.getIpLimit()
        ));
        stats.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Simple health check endpoint
     *
     * @return Service health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("service", "LocalScopeLocal API");
        
        // Add rate limiting health
        RateLimitService.RateLimitStatus rateLimitStatus = rateLimitService.getCurrentStatus();
        health.put("rateLimiting", Map.of(
                "enabled", rateLimitStatus.isEnabled(),
                "healthy", rateLimitStatus.getCurrentGlobalRequests() < rateLimitStatus.getGlobalLimit()
        ));
        
        return ResponseEntity.ok(health);
    }

    /**
     * Get system information (be careful with sensitive data)
     *
     * @return Basic system information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("application", "LocalScopeLocal");
        info.put("version", "1.0.0");
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("timestamp", System.currentTimeMillis());
        
        // Add memory information
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("totalMemory", runtime.totalMemory());
        memory.put("freeMemory", runtime.freeMemory());
        memory.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        memory.put("maxMemory", runtime.maxMemory());
        info.put("memory", memory);
        
        return ResponseEntity.ok(info);
    }
} 