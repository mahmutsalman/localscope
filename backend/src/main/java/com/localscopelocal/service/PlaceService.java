package com.localscopelocal.service;

import com.localscopelocal.model.Place;
import com.localscopelocal.model.PlaceSearchQuery;
import com.localscopelocal.repository.PlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for place-related operations, including caching
 */
@Service
public class PlaceService {

    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

    private final PlaceRepository placeRepository;
    private final GooglePlacesService googlePlacesService;

    @Autowired
    public PlaceService(PlaceRepository placeRepository, GooglePlacesService googlePlacesService) {
        this.placeRepository = placeRepository;
        this.googlePlacesService = googlePlacesService;
    }

    /**
     * Get nearby places based on the provided search query
     * First checks cache, then calls Google Places API if needed
     *
     * @param query The search query with location and radius
     * @return List of Place objects representing nearby locations
     */
    public List<Place> getNearbyPlaces(PlaceSearchQuery query) {
        log.info("Searching for places with query: {}", query);
        
        // Check if we have cached results for this query
        List<Place> cachedResults = placeRepository.findBySearchParameters(
                query.getLongitude(), 
                query.getLatitude(), 
                query.getRadius()
        );
        
        if (!cachedResults.isEmpty()) {
            log.info("Found {} cached places for query", cachedResults.size());
            return cachedResults;
        }
        
        log.info("No cached results found, fetching from Google Places API");
        
        // Fetch from Google Places API
        List<Place> places = googlePlacesService.fetchNearbyPlaces(query);
        
        // Save to cache if we got results
        if (!places.isEmpty()) {
            log.info("Saving {} new places to cache", places.size());
            placeRepository.saveAll(places);
        }
        
        return places;
    }
} 