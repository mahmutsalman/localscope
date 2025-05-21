package com.localscopelocal.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.localscopelocal.model.Place;
import com.localscopelocal.model.PlaceSearchQuery;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to interact with Google Places API (v1 - Nearby Search New)
 */
@Service
public class GooglePlacesService {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesService.class);

    @Value("${google.places.api.key}")
    private String apiKey;

    @Value("${google.places.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // Define a default FieldMask to request specific fields
    private static final String DEFAULT_FIELD_MASK = "places.id,places.displayName,places.formattedAddress,places.primaryType,places.websiteUri,places.rating,places.location";

    /**
     * Fetch nearby places from Google Places API (v1 - Nearby Search New)
     *
     * @param query The search query with location and radius
     * @return List of Place objects representing nearby locations
     */
    public List<Place> fetchNearbyPlaces(PlaceSearchQuery query) {
        log.info("Fetching places from Google API (v1) for query: {}", query);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Goog-Api-Key", apiKey);
            headers.set("X-Goog-FieldMask", DEFAULT_FIELD_MASK);

            // Construct the request body
            JsonObject requestBody = new JsonObject();
            // Add includedTypes if you want to filter by type, e.g., ["restaurant"]
            // requestBody.addProperty("includedTypes", "restaurant"); // Example
            requestBody.addProperty("maxResultCount", 20); // Max results (up to 20)

            JsonObject locationRestriction = new JsonObject();
            JsonObject circle = new JsonObject();
            JsonObject center = new JsonObject();
            center.addProperty("latitude", query.getLatitude());
            center.addProperty("longitude", query.getLongitude());
            circle.add("center", center);
            circle.addProperty("radius", query.getRadius().doubleValue());
            locationRestriction.add("circle", circle);
            requestBody.add("locationRestriction", locationRestriction);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            log.debug("Google Places API Request URL: {}", apiUrl);
            log.debug("Google Places API Request Headers: {}", headers);
            log.debug("Google Places API Request Body: {}", requestBody.toString());

            ResponseEntity<String> responseEntity = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

            log.debug("Google Places API Response Status: {}", responseEntity.getStatusCode());
            log.debug("Google Places API Response Body: {}", responseEntity.getBody());

            return parseResponse(responseEntity.getBody(), query);

        } catch (Exception e) {
            log.error("Error fetching places from Google API (v1)", e);
            return new ArrayList<>();
        }
    }

    /**
     * Parse the JSON response from Google Places API (v1)
     *
     * @param responseBody The JSON response string from Google Places API
     * @param query The original search query
     * @return List of Place objects representing nearby locations
     */
    private List<Place> parseResponse(String responseBody, PlaceSearchQuery query) {
        List<Place> places = new ArrayList<>();
        if (responseBody == null || responseBody.isEmpty()) {
            log.warn("Received empty response from Google Places API");
            return places;
        }

        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            
            // The new API returns a list of places directly under the "places" key
            if (jsonResponse.has("places")) {
                JsonArray results = jsonResponse.getAsJsonArray("places");
                
                for (JsonElement resultElement : results) {
                    JsonObject placeJson = resultElement.getAsJsonObject();
                    Place place = new Place();
                    
                    // The ID in the new API is under "name" field, e.g. "places/ChIJN1t_tDeuEmsRUsoyG83frY4"
                    // We'll use the place ID part if available, or the full name as ID.
                    // However, the fieldMask requests 'places.id' which should give the shorter ID.
                    if (placeJson.has("id")) {
                        place.setId(placeJson.get("id").getAsString());
                    } else if (placeJson.has("name")) { // Fallback if 'id' is not present with the field mask
                        place.setId(placeJson.get("name").getAsString()); 
                    }
                    
                    if (placeJson.has("displayName")) {
                        JsonObject displayNameObj = placeJson.getAsJsonObject("displayName");
                        if(displayNameObj.has("text")) {
                           place.setDisplayName(displayNameObj.get("text").getAsString());
                        }                       
                    }
                    
                    if (placeJson.has("formattedAddress")) {
                        place.setFormattedAddress(placeJson.get("formattedAddress").getAsString());
                    }
                    
                    if (placeJson.has("primaryType")) {
                        place.setPrimaryType(placeJson.get("primaryType").getAsString());
                    }

                    if (placeJson.has("websiteUri")) {
                        place.setWebsiteUri(placeJson.get("websiteUri").getAsString());
                    }
                    
                    if (placeJson.has("rating")) {
                        place.setRating(placeJson.get("rating").getAsDouble());
                    }
                    
                    // Get geometry/location
                    if (placeJson.has("location")) {
                        JsonObject location = placeJson.getAsJsonObject("location");
                        if (location.has("latitude") && location.has("longitude")) {
                            place.setLatitude(location.get("latitude").getAsDouble());
                            place.setLongitude(location.get("longitude").getAsDouble());
                        }
                    }
                    
                    // Save the query parameters for caching
                    place.setQueryLongitude(query.getLongitude());
                    place.setQueryLatitude(query.getLatitude());
                    place.setQueryRadius(query.getRadius());
                    
                    // Save timestamp
                    place.setCreatedAt(LocalDateTime.now());
                    
                    // Save the raw JSON for this place object
                    place.setRawResponse(placeJson.toString());
                    
                    places.add(place);
                }
            } else {
                 log.warn("Google Places API response does not contain 'places' array. Response: {}", responseBody);
            }
            
        } catch (Exception e) {
            log.error("Error parsing Google Places API (v1) response: {}", responseBody, e);
        }
        
        return places;
    }
} 