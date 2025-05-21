package com.localscopelocal.repository;

import com.localscopelocal.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Place entity
 */
@Repository
public interface PlaceRepository extends JpaRepository<Place, String> {
    
    /**
     * Find places based on query parameters (for caching)
     * 
     * @param longitude the query longitude
     * @param latitude the query latitude
     * @param radius the query radius
     * @return list of places matching the query
     */
    @Query("SELECT p FROM Place p WHERE " +
           "p.queryLongitude = :longitude AND " +
           "p.queryLatitude = :latitude AND " +
           "p.queryRadius = :radius")
    List<Place> findBySearchParameters(Double longitude, Double latitude, Integer radius);
} 