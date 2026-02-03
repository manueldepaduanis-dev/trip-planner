package com.github.manueldepaduanisdev.tripplanner.repositories;


import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, String> {

    List<Itinerary> findByStatus(Status status, Sort sort);
    // Get position in queued.
    long countByStatusAndCreatedAtBefore(Status status, LocalDateTime createdAt);
}
