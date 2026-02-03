package com.github.manueldepaduanisdev.tripplanner.repositories;


import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, String> {

    @Query("SELECT COUNT(l) FROM Itinerary i JOIN i.itineraryLocations l " +
            "WHERE (i.id = :itineraryId " +
            "OR (i.id != :itineraryId) " +
            "AND ((i.updatedAt IS NULL AND i.createdAt < :date) OR (i.updatedAt IS NOT NULL AND i.updatedAt < :date)))")
    long countLocations(@Param("itineraryId") String itineraryId,
                           @Param("date") LocalDateTime date);

    @Query("SELECT i FROM Itinerary i " +
            "LEFT JOIN FETCH i.itineraryLocations loc " +
            "LEFT JOIN FETCH loc.geoData " +
            "WHERE i.id = :id")
    Optional<Itinerary> findByIdWithLocations(@Param("id") String id);
}
