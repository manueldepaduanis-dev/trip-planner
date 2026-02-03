package com.github.manueldepaduanisdev.tripplanner.repositories;


import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, String> {

    @Query("SELECT COUNT(l) FROM Itinerary i JOIN i.itineraryLocations l " +
            "WHERE i.sessionId = :sessionId " +
            "AND i.id != :itineraryId " +
            "AND ((i.updatedAt IS NULL AND i.createdAt < :date) OR (i.updatedAt IS NOT NULL AND i.updatedAt < :date))")
    long countLocations(@Param("sessionId") String sessionId,
                           @Param("itineraryId") String itineraryId,
                           @Param("date") LocalDateTime date);
}
