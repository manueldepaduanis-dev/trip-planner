package com.github.manueldepaduanisdev.tripplanner.services;

import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import com.github.manueldepaduanisdev.tripplanner.domain.ItineraryLocation;
import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import com.github.manueldepaduanisdev.tripplanner.repositories.ItineraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * This service handles "large computations" in background (asynchronously)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryWorkerService {

    private final ItineraryRepository _itineraryRepository;
    private final long TIME_PER_LOCATION = 4L;

    /**
     *
     * @param itineraryId needed to find the itinerary saved on id and process it
     * @return Future
     */
    @Async("itineraryTaskExecutor")
    public CompletableFuture<Void> processItinerary(String itineraryId) {
        log.info("Start processing itinerary: {}", itineraryId);

        // Get itinerary
        Itinerary itinerary = _itineraryRepository
                .findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found."));
        try {
            // Change status to PROCESSING and save it
            itinerary.setStatus(Status.PROCESSING);
            _itineraryRepository.save(itinerary);

            // Doing compute stuff... (mocked)
            for (ItineraryLocation loc : itinerary.getItineraryLocations()) {
                String cityName = loc.getGeoData().getCity();
                log.info("Computing stop...: {}", cityName);

                Thread.sleep(TIME_PER_LOCATION);
            }

            // Once it's completed, change itinerary status to COMPLETED and save it
            itinerary.setStatus(Status.COMPLETED);
            _itineraryRepository.save(itinerary);
            log.info("Itinerary successfully computed: {}", itineraryId);
        } catch (InterruptedException ex) {
            // If thread is interrupted (forced by update)
            log.warn("Computing INTERRUPTED for itinerary: {}", itineraryId);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // If any error was thrown, change itinerary status to FAILED and save it
            log.error("Generic Error inside worker", e);
            itinerary.setStatus(Status.FAILED);
            _itineraryRepository.save(itinerary);
        }

        // Just for method sign
        return CompletableFuture.completedFuture(null);
    }

    public long calculateTimeRemaining(String itineraryId, String session_id) {
        Optional<Itinerary> currentItinerary = _itineraryRepository.findById(itineraryId);
        if(currentItinerary.isEmpty()) return 0L;

        LocalDateTime comparisonDate = currentItinerary.get().getUpdatedAt() != null
                ? currentItinerary.get().getUpdatedAt()
                : currentItinerary.get().getCreatedAt();

        var countLocations = _itineraryRepository.countLocations(session_id, itineraryId, comparisonDate);

        return countLocations * TIME_PER_LOCATION;
    }
}
