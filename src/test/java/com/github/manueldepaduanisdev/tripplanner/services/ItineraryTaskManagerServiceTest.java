package com.github.manueldepaduanisdev.tripplanner.services;

import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import com.github.manueldepaduanisdev.tripplanner.repositories.ItineraryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItineraryTaskManagerServiceTest {

    @Mock
    private ItineraryWorkerService workerService;

    @Mock
    private ItineraryRepository itineraryRepository;

    private ItineraryTaskManagerService taskManagerService;

    private final Long THREAD_SLEEP_TIME = 2000L;

    @BeforeEach
    void setUp() {
        taskManagerService = new ItineraryTaskManagerService(workerService, itineraryRepository, THREAD_SLEEP_TIME);
    }

    @Test
    void submitTask_ShouldCallWorkerAndAddAnActiveTask() {

        String itineraryId = "Test-itinerary-id";
        CompletableFuture<Void> future = new CompletableFuture<>();
        when(workerService.processItinerary(itineraryId))
                .thenReturn(future);

        taskManagerService.submitTask(itineraryId);

        verify(workerService, times(1)).processItinerary(itineraryId);
    }

    @Test
    void handleUpdateInQueue_ShouldCancelTaskIfTaskIsRunning() {
        String itineraryId  = "itinerary-id";

        Itinerary itinerary = Itinerary.builder()
                .id(itineraryId)
                .status(Status.PROCESSING)
                .build();

        CompletableFuture<Void> mockFuture = mock(CompletableFuture.class);
        when(workerService.processItinerary(itineraryId)).thenReturn(mockFuture);
        when(mockFuture.isDone()).thenReturn(false);
        taskManagerService.submitTask(itineraryId);
        Itinerary result = taskManagerService.handleUpdateInQueue(itinerary);

        verify(mockFuture).cancel(true);
        Assertions.assertEquals(Status.QUEUED, result.getStatus());
        verify(itineraryRepository).save(itinerary);
    }

    @Test
    void handleUpdateInQueue_ShouldNotCancelIfTaskIsAlreadyDone() {
        String itineraryId = "id-done";
        Itinerary itinerary = Itinerary.builder().id(itineraryId).build();

        CompletableFuture<Void> mockFuture = mock(CompletableFuture.class);
        when(workerService.processItinerary(itineraryId)).thenReturn(mockFuture);

        when(mockFuture.isDone()).thenReturn(true);

        taskManagerService.submitTask(itineraryId);

        taskManagerService.handleUpdateInQueue(itinerary);

        verify(mockFuture, never()).cancel(anyBoolean());
        verify(itineraryRepository).save(itinerary);
    }

    @Test
    void handleUpdateInQueue_ShouldJustSaveIfNoTaskIsInMap() {
        Itinerary itinerary = Itinerary.builder().id("no-task-id").status(Status.FAILED).build();

        taskManagerService.handleUpdateInQueue(itinerary);

        Assertions.assertEquals(Status.QUEUED, itinerary.getStatus());
        verify(itineraryRepository).save(itinerary);
    }


    @Test
    void calculateTimeRemaining_ShouldReturnZeroIfIdIsInvalid() {
        Assertions.assertEquals(0L, taskManagerService.calculateTimeRemaining(null, null));
        Assertions.assertEquals(0L, taskManagerService.calculateTimeRemaining("", null));
    }

    @Test
    void calculateTimeRemaining_ShouldCalculateWithProvidedDate() {
        String id = "calc-id";
        LocalDateTime providedDate = LocalDateTime.now();

        when(itineraryRepository.countLocations(id, providedDate)).thenReturn(5L);

        long result = taskManagerService.calculateTimeRemaining(id, providedDate);

        Assertions.assertEquals(10L, result);
        verify(itineraryRepository).countLocations(id, providedDate);
        verify(itineraryRepository, never()).findById(anyString());
    }

    @Test
    void calculateTimeRemaining_ShouldFetchItineraryIfDateIsNull() {
        String id = "fetch-id";
        LocalDateTime dbDate = LocalDateTime.now().minusDays(1);

        Itinerary itinerary = Itinerary.builder()
                .id(id)
                .updatedAt(dbDate)
                .build();

        when(itineraryRepository.findById(id)).thenReturn(Optional.of(itinerary));

        when(itineraryRepository.countLocations(id, dbDate)).thenReturn(3L);

        long result = taskManagerService.calculateTimeRemaining(id, null);

        Assertions.assertEquals(6L, result);
        verify(itineraryRepository).findById(id);
        verify(itineraryRepository).countLocations(id, dbDate);
    }

    @Test
    void calculateTimeRemaining_ShouldReturnZeroIfItineraryNotFoundInDB() {
        String id = "ghost-id";
        when(itineraryRepository.findById(id)).thenReturn(Optional.empty());

        long result = taskManagerService.calculateTimeRemaining(id, null);

        Assertions.assertEquals(0L, result);
    }
}
