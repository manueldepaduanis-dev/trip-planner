package com.github.manueldepaduanisdev.tripplanner.services;

import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import com.github.manueldepaduanisdev.tripplanner.domain.ItineraryLocation;
import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import com.github.manueldepaduanisdev.tripplanner.dto.request.ItineraryRequestDTO;
import com.github.manueldepaduanisdev.tripplanner.dto.response.ItineraryResponseDTO;
import com.github.manueldepaduanisdev.tripplanner.repositories.ItineraryRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Parent itinerary service.
 */
@Service
@AllArgsConstructor
public class ItineraryService {

    private final ItineraryRepository _itineraryRepository;
    private final ItineraryTaskManagerService _taskManagerService;

    /**
     *
     * @param request API request
     * @param sessionId optional sessionId
     */
    @Transactional // If something fail -> rollback all.
    public ItineraryResponseDTO createItineraryProcess(ItineraryRequestDTO request, String sessionId) {
        String finalSessionId = (sessionId == null || sessionId.isBlank())
                ? UUID.randomUUID().toString()
                : sessionId;

        // Create new itinerary entity
        Itinerary newItinerary = new Itinerary();
        newItinerary.setTitle(request.getTitle());
        newItinerary.setSessionId(finalSessionId);
        newItinerary.setStatus(Status.QUEUED);

        // Save itinerary
        Itinerary itinerarySaved = _itineraryRepository.save(newItinerary);

        // Start worker to process new itinerary (asynchronously)
        _taskManagerService.submitTask(itinerarySaved.getId());

        // Return itinerary saved
        return mapToResponseDTO(itinerarySaved);
    }

    /**
     * Mapper from entity to DTO
     * @param itinerary entity
     * @return DTO mapped
     */
    private ItineraryResponseDTO mapToResponseDTO(Itinerary itinerary) {
        return ItineraryResponseDTO.builder()
                .id(itinerary.getId())
                .sessionId(itinerary.getSessionId())
                .title(itinerary.getTitle())
                .status(itinerary.getStatus())
                .build();
    }
}
