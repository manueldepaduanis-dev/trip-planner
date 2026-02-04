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
    }

    /**
     * Method in which is created a new process itinerary task and send it to queue.
     * @param itineraryId To find and process itinerary
     */
    public void submitTask(@NotBlank String itineraryId) {

        CompletableFuture<Void> future = workerService.processItinerary(itineraryId);
        activeTasks.put(itineraryId, future);

        future.whenComplete((result, exception) -> {
            activeTasks.remove(itineraryId);
            log.info("Task completed (or deleted). Removed from Concurrent Hash Map id: {}", itineraryId);
        });

        log.info("Task registered for id: {}", itineraryId);
    }

    /**
     * If is present a running task with the key passed:
     * COMPLETED -> Nothing
     * QUEUED/PROCESSING -> Stop it, get updated one, change status and restart it.
     * @param itineraryToUpdate key to get active tasks in threads pool
     */
    public Itinerary handleUpdateInQueue(@NotNull Itinerary itineraryToUpdate) {
        String itineraryId = itineraryToUpdate.getId();
        log.info("Handling request for interruption and restart for id: {}", itineraryId);

        Future<?> runningTask = activeTasks.get(itineraryId);

        if (runningTask != null) {
            // If task is running
            if (!runningTask.isDone()) {
                // This will force the InterruptedException in WorkerService.
                boolean cancelled = runningTask.cancel(true);
                log.info("Try to stop task {}: {}", itineraryId, cancelled ? "SUCCESS" : "FAIL");
            }
            // Clean not at all running task
            activeTasks.remove(itineraryId);
        }

        itineraryToUpdate.setStatus(Status.QUEUED);
        itineraryRepository.save(itineraryToUpdate);

        return itineraryToUpdate;
    }

    public long calculateTimeRemaining(@NotBlank String itineraryId, @Nullable LocalDateTime date) {
        if (itineraryId == null || itineraryId.isBlank()) return 0L;

        LocalDateTime comparisonDate = date;
        if (comparisonDate == null) {
            Optional<Itinerary> currentItinerary = itineraryRepository.findById(itineraryId);
            if (currentItinerary.isEmpty()) return 0L;

            comparisonDate = currentItinerary.get().getUpdatedAt();
        }

        var countLocations = itineraryRepository.countLocations(itineraryId, comparisonDate);

        return (countLocations * threadSleepTime) / 1000;
    }
}