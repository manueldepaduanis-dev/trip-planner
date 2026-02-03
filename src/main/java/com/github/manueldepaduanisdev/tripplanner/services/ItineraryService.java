package com.github.manueldepaduanisdev.tripplanner.services;

import com.github.manueldepaduanisdev.tripplanner.domain.GeoData;
import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import com.github.manueldepaduanisdev.tripplanner.domain.ItineraryLocation;
import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import com.github.manueldepaduanisdev.tripplanner.dto.request.ItineraryRequestDTO;
import com.github.manueldepaduanisdev.tripplanner.dto.response.ItineraryResponseDTO;
import com.github.manueldepaduanisdev.tripplanner.mappers.ItineraryLocationMapper;
import com.github.manueldepaduanisdev.tripplanner.mappers.ItineraryMapper;
import com.github.manueldepaduanisdev.tripplanner.repositories.GeoDataRepository;
import com.github.manueldepaduanisdev.tripplanner.repositories.ItineraryRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Parent itinerary service.
 */
@Service
@AllArgsConstructor
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final GeoDataRepository geoDataRepository;
    private final ItineraryTaskManagerService taskManagerService;
    private final ItineraryMapper itineraryMapper;
    private final ItineraryLocationMapper itineraryLocationMapper;

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
        Itinerary newItinerary = Itinerary.builder()
                .title(request.getTitle())
                .sessionId(finalSessionId)
                .status(Status.QUEUED)
                .itineraryLocations(new ArrayList<>())
                .build();

        for (ItineraryRequestDTO.LocationRequest locDto : request.getLocations()) {

            GeoData geoData = geoDataRepository.findById(locDto.getGeoId())
                    .orElseThrow(() -> new RuntimeException("City not found with ID: " + locDto.getGeoId()));

            ItineraryLocation newLocation = ItineraryLocation.builder()
                    .orderIndex(locDto.getOrderIndex())
                    .isCurrentStop(locDto.isCurrentStop())
                    .geoData(geoData)
                    .itinerary(newItinerary)
                    .build();

            newItinerary.getItineraryLocations().add(newLocation);
        }

        // Saved itinerary
        Itinerary savedItinerary = itineraryRepository.save(newItinerary);

        // Return itinerary saved
        return itineraryMapper.toDTO(savedItinerary);
    }
}
