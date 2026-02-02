package com.github.manueldepaduanisdev.tripplanner.services;

import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import com.github.manueldepaduanisdev.tripplanner.repositories.ItineraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Task Manager in which a running task can be updated and could be stopped
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryTaskManagerService {

    private final ItineraryWorkerService workerService;
    private final ItineraryRepository itineraryRepository;

    // Used Concurrent because many users (threads), could access to this Map simultaneously
    private final Map<String, Future<?>> activeTasks = new ConcurrentHashMap<>();

    /**
     * Method in which is created a new process itinerary task and send it to queue.
     * @param itineraryId To find and process itinerary
     */
    public void submitTask(String itineraryId) {

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
     * @param itineraryId key to get active tasks in threads pool
     */
    public void handleUpdateAndRequeue(String itineraryId) {
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

        // Update itinerary status to QUEUE
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found during re-queue"));

        itinerary.setStatus(Status.QUEUED);
        itineraryRepository.save(itinerary);

        // Move itinerary in queue
        submitTask(itineraryId);
    }
}
