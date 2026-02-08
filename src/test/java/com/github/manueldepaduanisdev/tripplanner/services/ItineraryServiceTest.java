package com.github.manueldepaduanisdev.tripplanner.services;

import com.github.manueldepaduanisdev.tripplanner.domain.GeoData;
import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import com.github.manueldepaduanisdev.tripplanner.domain.ItineraryLocation;
import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import com.github.manueldepaduanisdev.tripplanner.dto.request.ItineraryRequestDTO;
import com.github.manueldepaduanisdev.tripplanner.dto.response.ItineraryResponseDTO;
import com.github.manueldepaduanisdev.tripplanner.mappers.ItineraryMapper;
import com.github.manueldepaduanisdev.tripplanner.repositories.GeoDataRepository;
import com.github.manueldepaduanisdev.tripplanner.repositories.ItineraryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ItineraryServiceTest {

    @Mock
    private ItineraryRepository itineraryRepository;
    @Mock
    private GeoDataRepository geoDataRepository;
    @Mock
    private ItineraryTaskManagerService taskManagerService;
    @Mock
    private ItineraryMapper itineraryMapper;

    @InjectMocks
    private ItineraryService itineraryService;

    // Helper for create common data
    private ItineraryRequestDTO createRequest() {
        return ItineraryRequestDTO.builder()
                .title("Itinerary test")
                .locations(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("")
    void createItinerary_ShouldCreateNewWithGeneratedSessionId() {
        ItineraryRequestDTO request = createRequest();

        // Return the arguemnt that i passed earlier
        when(itineraryRepository.save(any(Itinerary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(itineraryMapper.toDTO(any(Itinerary.class)))
                .thenReturn(ItineraryResponseDTO.builder().build());

        ItineraryResponseDTO result = itineraryService.createItinerary(request, null);

        Assertions.assertNotNull(result);

        ArgumentCaptor<Itinerary> itineraryCaptor = ArgumentCaptor.forClass(Itinerary.class);

        verify(itineraryRepository).save(itineraryCaptor.capture());

        Itinerary savedItinerary = itineraryCaptor.getValue();

        Assertions.assertEquals(request.getTitle(), savedItinerary.getTitle());
        Assertions.assertNotNull(savedItinerary.getSessionId());
        Assertions.assertEquals(Status.QUEUED, savedItinerary.getStatus());

        Assertions.assertDoesNotThrow(() -> java.util.UUID.fromString(savedItinerary.getSessionId()));
    }

    @Test
    void createItinerary_ShouldThrowExceptionIfSessionIdPassedIsNotPresentInDB() {
        ItineraryRequestDTO request = createRequest();
        String sessionId = "random-session-id";

        when(itineraryRepository.findFirstBySessionId(sessionId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> {
            itineraryService.createItinerary(request, sessionId);
        });

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateItinerary_ShouldUpdateFieldsAndCallTaskManager() {
        String sessionId = "session-id";
        String id = "itinerary-id";

        Itinerary existingItinerary = Itinerary.builder()
                .id(id)
                .sessionId(sessionId)
                .title("Old title")
                .status(Status.QUEUED)
                .itineraryLocations(new ArrayList<>())
                .build();

        ItineraryRequestDTO request = ItineraryRequestDTO.builder()
                .title("New title")
                .locations(new ArrayList<>())
                .build();

        when(itineraryRepository.findByIdWithLocationsAndGeoData(sessionId, id))
                .thenReturn(Optional.of(existingItinerary));

        when(taskManagerService.handleUpdateInQueue(any(Itinerary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(itineraryMapper.toDTO(any())).thenReturn(ItineraryResponseDTO.builder().build());

        itineraryService.updateItinerary(sessionId, id, request);

        ArgumentCaptor<Itinerary> captor = ArgumentCaptor.forClass(Itinerary.class);
        verify(taskManagerService).handleUpdateInQueue(captor.capture());

        Itinerary updatedItinerary = captor.getValue();

        Assertions.assertEquals("New title", updatedItinerary.getTitle());

        Assertions.assertEquals(id, updatedItinerary.getId());
    }

    @Test
    void updateItinerary_ShouldThrowExceptionIfComboSessionIdAndIdPassedIsNotPresentInDB() {
        ItineraryRequestDTO request = createRequest();
        String wrongSessionId = "Random-session-id";
        String wrongId = "Random-itinerary-id";

        when(itineraryRepository.findByIdWithLocationsAndGeoData(wrongSessionId, wrongId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class, () -> {
            itineraryService.updateItinerary(wrongSessionId, wrongId, request);
        });

        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
