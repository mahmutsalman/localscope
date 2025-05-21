package com.localscopelocal.controller;

import com.localscopelocal.model.Place;
import com.localscopelocal.model.PlaceSearchQuery;
import com.localscopelocal.service.PlaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for place-related endpoints
 */
@RestController
@RequestMapping("/api/places")
@CrossOrigin(origins = "*") // Allow cross-origin requests for frontend access
public class PlaceController {

    private static final Logger log = LoggerFactory.getLogger(PlaceController.class);

    private final PlaceService placeService;

    @Autowired
    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    /**
     * GET endpoint to search for nearby places
     *
     * @param longitude Longitude coordinate
     * @param latitude Latitude coordinate
     * @param radius Search radius in meters
     * @return List of places matching the search criteria
     */
    @GetMapping
    public ResponseEntity<List<Place>> getNearbyPlaces(
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam Integer radius) {
        
        log.info("Received request for places at lon: {}, lat: {}, radius: {}", longitude, latitude, radius);
        
        // Validate parameters
        if (longitude < -180 || longitude > 180) {
            return ResponseEntity.badRequest().build();
        }
        
        if (latitude < -90 || latitude > 90) {
            return ResponseEntity.badRequest().build();
        }
        
        if (radius <= 0 || radius > 50000) { // Google Places API limit
            return ResponseEntity.badRequest().build();
        }
        
        PlaceSearchQuery query = new PlaceSearchQuery();
        query.setLongitude(longitude);
        query.setLatitude(latitude);
        query.setRadius(radius);
        
        List<Place> places = placeService.getNearbyPlaces(query);
        
        return ResponseEntity.ok(places);
    }
} 