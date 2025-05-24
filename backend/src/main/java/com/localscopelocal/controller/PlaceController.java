package com.localscopelocal.controller;

import com.localscopelocal.model.Place;
import com.localscopelocal.model.PlaceSearchQuery;
import com.localscopelocal.service.PlaceService;
import com.localscopelocal.service.RateLimitService;
import com.localscopelocal.service.IpAddressUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for place-related endpoints
 */
@RestController
@RequestMapping("/api/places")
@CrossOrigin(origins = "*") // Allow cross-origin requests for frontend access
public class PlaceController {

    private static final Logger log = LoggerFactory.getLogger(PlaceController.class);

    private final PlaceService placeService;
    private final RateLimitService rateLimitService;

    @Autowired
    public PlaceController(PlaceService placeService, RateLimitService rateLimitService) {
        this.placeService = placeService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * GET endpoint to search for nearby places with rate limiting
     *
     * @param longitude Longitude coordinate
     * @param latitude Latitude coordinate
     * @param radius Search radius in meters
     * @param request HTTP request for IP extraction
     * @return List of places matching the search criteria or rate limit error
     */
    @GetMapping
    public ResponseEntity<?> getNearbyPlaces(
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam Integer radius,
            HttpServletRequest request) {
        
        String clientIp = IpAddressUtil.getNormalizedIpAddress(request);
        log.info("Received request for places from IP: {} at lon: {}, lat: {}, radius: {}", 
                clientIp, longitude, latitude, radius);
        
        // Check rate limits first
        RateLimitService.RateLimitResult rateLimitResult = rateLimitService.isRequestAllowed(clientIp);
        if (!rateLimitResult.isAllowed()) {
            log.warn("Rate limit exceeded for IP: {} - {}", clientIp, rateLimitResult.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Rate limit exceeded");
            errorResponse.put("message", rateLimitResult.getMessage());
            errorResponse.put("remainingIpRequests", rateLimitResult.getRemainingIpRequests());
            errorResponse.put("remainingGlobalRequests", rateLimitResult.getRemainingGlobalRequests());
            
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }
        
        // Validate parameters
        if (longitude < -180 || longitude > 180) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid longitude. Must be between -180 and 180."));
        }
        
        if (latitude < -90 || latitude > 90) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid latitude. Must be between -90 and 90."));
        }
        
        if (radius <= 0 || radius > 50000) { // Google Places API limit
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid radius. Must be between 1 and 50000 meters."));
        }
        
        PlaceSearchQuery query = new PlaceSearchQuery();
        query.setLongitude(longitude);
        query.setLatitude(latitude);
        query.setRadius(radius);
        
        try {
            List<Place> places = placeService.getNearbyPlaces(query);
            
            // Add rate limit info to response headers
            Map<String, Object> response = new HashMap<>();
            response.put("places", places);
            response.put("count", places.size());
            response.put("rateLimitInfo", Map.of(
                    "remainingIpRequests", rateLimitResult.getRemainingIpRequests(),
                    "remainingGlobalRequests", rateLimitResult.getRemainingGlobalRequests()
            ));
            
            log.info("Successfully returned {} places for IP: {}", places.size(), clientIp);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing request for IP: {}", clientIp, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An error occurred while processing your request."));
        }
    }

    /**
     * GET endpoint to check current rate limit status
     *
     * @param request HTTP request for IP extraction
     * @return Current rate limit status for the requesting IP
     */
    @GetMapping("/rate-limit-status")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(HttpServletRequest request) {
        String clientIp = IpAddressUtil.getNormalizedIpAddress(request);
        RateLimitService.RateLimitStatus status = rateLimitService.getCurrentStatus();
        
        Map<String, Object> response = new HashMap<>();
        response.put("clientIp", clientIp);
        response.put("rateLimitEnabled", status.isEnabled());
        response.put("globalRequests", status.getCurrentGlobalRequests());
        response.put("globalLimit", status.getGlobalLimit());
        response.put("activeIpAddresses", status.getActiveIpAddresses());
        response.put("ipLimit", status.getIpLimit());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("places", Collections.emptyList());
        errorResponse.put("count", 0);
        return errorResponse;
    }
} 