package com.github.manueldepaduanisdev.tripplanner.services;

import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import com.github.manueldepaduanisdev.tripplanner.domain.ItineraryLocation;
import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import com.github.manueldepaduanisdev.tripplanner.repositories.ItineraryRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CompletableFuture;

/**
 * This service handles "large computations" in background (asynchronously)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class ItineraryWorkerService {

    private final ItineraryRepository itineraryRepository;

    @Value("${app.thread.time-per-location}")
    private Long threadSleepTime;
    /**
     *
     * @param itineraryId needed to find the itinerary saved on id and process it
     * @return Future
     */
    @Async("itineraryTaskExecutor")
    public CompletableFuture<Void> processItinerary(@NotBlank String itineraryId) {
        log.info("Start processing itinerary: {}", itineraryId);

        // Get itinerary
        Itinerary itinerary = itineraryRepository.findByIdWithLocations(itineraryId)
            .orElseThrow(() -> {
                log.error("Itinerary not found.");
                return new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No itinerary found for ID: : " + itineraryId
                );
            });

        // Change status to PROCESSING and save it
        itinerary.setStatus(Status.PROCESSING);
        itineraryRepository.save(itinerary);

        try {
            // Doing compute stuff... (mocked)
            for (ItineraryLocation loc : itinerary.getItineraryLocations()) {
                String cityName = loc.getGeoData().getCity();
                log.info("Computing stop...: {}", cityName);

                Thread.sleep(threadSleepTime);
            }

            // Once it's completed, change itinerary status to COMPLETED and save it
            itinerary.setStatus(Status.COMPLETED);
            itineraryRepository.save(itinerary);
            log.info("Itinerary successfully computed: {}", itineraryId);

            // Just for method sign
        } catch (InterruptedException ex) {
            // If thread is interrupted (forced by update)
            log.warn("Computing INTERRUPTED for itinerary: {}", itineraryId);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // If any error was thrown, change itinerary status to FAILED and save it
            log.error("Generic Error inside worker", e);
            itinerary.setStatus(Status.FAILED);
            itineraryRepository.save(itinerary);
        }

        return CompletableFuture.completedFuture(null);
    }
}