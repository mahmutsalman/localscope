package com.localscopelocal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import java.time.LocalDateTime;

/**
 * Entity class representing a place from Google Places API (v1)
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Place {

    @Id
    private String id; // Corresponds to "name" in API response (e.g., "places/ChIJN1t_tDeuEmsRUsoyG83frY4")
    
    private String displayName;
    private String formattedAddress;
    private Double rating; // Remains similar
    private String primaryType;
    private String websiteUri;
    // The 'icon' field is not directly available in the new API in the same way.
    // We might need to construct it or use a default if needed based on 'primaryType'.
    // For now, let's remove it or decide on a strategy later.
    // private String icon;
    
    private Double latitude;
    private Double longitude;
    
    @Lob
    @Column(columnDefinition = "CLOB")
    private String rawResponse; // Stores the raw JSON for the individual place object
    
    // For caching - will store the query parameters as a composite key
    private Double queryLongitude;
    private Double queryLatitude;
    private Integer queryRadius;
    
    private LocalDateTime createdAt;
    
    // Explicit getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public void setPrimaryType(String primaryType) {
        this.primaryType = primaryType;
    }

    public String getWebsiteUri() {
        return websiteUri;
    }

    public void setWebsiteUri(String websiteUri) {
        this.websiteUri = websiteUri;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public Double getQueryLongitude() {
        return queryLongitude;
    }

    public void setQueryLongitude(Double queryLongitude) {
        this.queryLongitude = queryLongitude;
    }

    public Double getQueryLatitude() {
        return queryLatitude;
    }

    public void setQueryLatitude(Double queryLatitude) {
        this.queryLatitude = queryLatitude;
    }

    public Integer getQueryRadius() {
        return queryRadius;
    }

    public void setQueryRadius(Integer queryRadius) {
        this.queryRadius = queryRadius;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
} 