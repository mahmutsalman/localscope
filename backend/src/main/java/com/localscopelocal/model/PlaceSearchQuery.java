package com.localscopelocal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a place search query with parameters
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchQuery {
    private Double longitude;
    private Double latitude;
    private Integer radius;
    
    /**
     * Creates a cache key from the query parameters
     * @return A string representation of the query parameters
     */
    public String createCacheKey() {
        return String.format("%.6f:%.6f:%d", longitude, latitude, radius);
    }
    
    // Explicit getters and setters
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Integer getRadius() {
        return radius;
    }
    
    public void setRadius(Integer radius) {
        this.radius = radius;
    }
} 