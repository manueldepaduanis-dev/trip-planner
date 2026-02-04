package com.github.manueldepaduanisdev.tripplanner.services;

import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import com.github.manueldepaduanisdev.tripplanner.repositories.ItineraryRepository;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Task Manager in which a running task can be updated and could be stopped
 */
@Service
@Slf4j
@Validated
public class ItineraryTaskManagerService {

    private final ItineraryWorkerService workerService;
    private final ItineraryRepository itineraryRepository;

    // Used Concurrent because many users (threads), could access to this Map simultaneously
    private final Map<String, Future<?>> activeTasks = new ConcurrentHashMap<>();

    private final Long threadSleepTime;

    public ItineraryTaskManagerService(
            ItineraryWorkerService workerService,
            ItineraryRepository itineraryRepository,
            @Value("${app.thread.time-per-location}") Long threadSleepTime
    ) {
        this.workerService = workerService;
        this.itineraryRepository = itineraryRepository;
        this.threadSleepTime = threadSleepTime;
        log.info("ItineraryTaskManagerService initialized. Configured sleep time per location: {} ms", threadSleepTime);
    }

    /**
     * Method in which is created a new process itinerary task and send it to queue.
     * @param itineraryId To find and process itinerary
     */
    public void submitTask(@NotBlank String itineraryId) {
        log.info("Submitting new asynchronous task for itinerary ID: {}", itineraryId);

        CompletableFuture<Void> future = workerService.processItinerary(itineraryId);
        activeTasks.put(itineraryId, future);

        future.whenComplete((result, exception) -> {
            activeTasks.remove(itineraryId);
            if (exception != null) {
                log.error("Task for itinerary ID: {} failed with exception.", itineraryId, exception);
            } else {
                log.info("Task for itinerary ID: {} completed successfully. Removed from active tasks map.", itineraryId);
            }
        });

        log.info("Task registered and running for id: {}", itineraryId);
    }

    /**
     * If is present a running task with the key passed:
     * COMPLETED -> Nothing
     * QUEUED/PROCESSING -> Stop it, get updated one, change status and restart it.
     * @param itineraryToUpdate key to get active tasks in threads pool
     */
    public Itinerary handleUpdateInQueue(@NotNull Itinerary itineraryToUpdate) {
        String itineraryId = itineraryToUpdate.getId();
        log.info("Handling update request (stop & requeue) for itinerary ID: {}", itineraryId);

        Future<?> runningTask = activeTasks.get(itineraryId);

        if (runningTask != null) {
            // If task is running
            if (!runningTask.isDone()) {
                // This will force the InterruptedException in WorkerService.
                boolean cancelled = runningTask.cancel(true);
                log.info("Attempted to stop running task for itinerary ID: {}. Result: {}",
                        itineraryId, cancelled ? "SUCCESS" : "FAIL");
            } else {
                log.info("Task for itinerary ID: {} was found in map but is already done.", itineraryId);
            }
            // Clean not at all running task
            activeTasks.remove(itineraryId);
        } else {
            log.info("No active task found to cancel for itinerary ID: {}. Proceeding with status update.", itineraryId);
        }

        itineraryToUpdate.setStatus(Status.QUEUED);
        itineraryRepository.save(itineraryToUpdate);

        log.info("Itinerary ID: {} status reset to QUEUED and saved.", itineraryId);

        return itineraryToUpdate;
    }

    public long calculateTimeRemaining(@NotBlank String itineraryId, @Nullable LocalDateTime date) {
        if (itineraryId == null || itineraryId.isBlank()) return 0L;

        LocalDateTime comparisonDate = date;
        if (comparisonDate == null) {
            Optional<Itinerary> currentItinerary = itineraryRepository.findById(itineraryId);
            if (currentItinerary.isEmpty()) {
                log.warn("Cannot calculate time remaining: Itinerary ID {} not found in DB.", itineraryId);
                return 0L;
            }

            comparisonDate = currentItinerary.get().getUpdatedAt();
        }

        var countLocations = itineraryRepository.countLocations(itineraryId, comparisonDate);
        long estimatedTime = (countLocations * threadSleepTime) / 1000;

        log.debug("Calculated time remaining for itinerary ID: {}. Locations remaining: {}, Est. Seconds: {}",
                itineraryId, countLocations, estimatedTime);

        return estimatedTime;
    }
}