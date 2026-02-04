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
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Parent itinerary service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final GeoDataRepository geoDataRepository;
    private final ItineraryTaskManagerService taskManagerService;
    private final ItineraryMapper itineraryMapper;

    /**
     *
     * @param request API request
     * @param sessionId optional sessionId
     */
    @Transactional // If something fail -> rollback all.
    public ItineraryResponseDTO createItinerary(@NotNull ItineraryRequestDTO request, @Nullable String sessionId) {
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

        newItinerary.setItineraryLocations(createLocations(newItinerary, request.getLocations()));

        // Saved itinerary
        Itinerary savedItinerary = itineraryRepository.save(newItinerary);

        // Return itinerary saved
        return itineraryMapper.toDTO(savedItinerary);
    }

    @Transactional
    public ItineraryResponseDTO updateItinerary(@NotBlank String sessionId, @NotBlank String id, @NotNull ItineraryRequestDTO request) {

        Itinerary itinerary = itineraryRepository.findBySessionIdAndId(sessionId, id)
                .orElseThrow(() -> {
                    log.error("Itinerary to update not found with ID: {}.", id);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No itinerary found for sessionId: " + sessionId + " and ID: : " + id
                    );
                });

        List<ItineraryLocation> newLocations = createLocations(itinerary, request.getLocations());
        itinerary.setTitle(request.getTitle());
        itinerary.setItineraryLocations(newLocations);

        Itinerary itineraryUpdated = taskManagerService.handleUpdateInQueue(itinerary);

        return itineraryMapper.toDTO(itineraryUpdated);
    }

    public List<ItineraryResponseDTO> getList(@NotBlank String sessionId, @Nullable Status status) {

        return itineraryRepository.findBySessionIdAndStatus(sessionId, status).stream()
                .map(itineraryMapper::toDTO)
                .toList();
    }

    public ItineraryResponseDTO getById(@NotBlank String sessionId, @NotBlank String id) {
        Itinerary itinerary = itineraryRepository.findBySessionIdAndId(sessionId, id)
                .orElseThrow(() -> {
                    log.error("Itinerary not found with ID: {}.", id);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No itinerary found for sessionId: " + sessionId + " and ID: : " + id
                    );
                });

        return itineraryMapper.toDTO(itinerary);
    }

    private List<ItineraryLocation> createLocations(@NotNull Itinerary entity, @NotNull List<ItineraryRequestDTO.LocationRequest> request) {
        if (request.isEmpty()) return new ArrayList<>();

        List<Long> geoIds = request.stream()
                .map(ItineraryRequestDTO.LocationRequest::getGeoId)
                .toList();

        List<GeoData> geoDataList = geoDataRepository.findAllById(geoIds);

        if (geoDataList.size() != geoIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more GeoData IDs provided do not exist.");
        }

        // Map<ID, GeoData>
        Map<Long, GeoData> geoDataMap = geoDataList.stream()
                .collect(Collectors.toMap(GeoData::getId, geo -> geo));

        List<ItineraryLocation> locationsToRet = new ArrayList<>();

        for (ItineraryRequestDTO.LocationRequest locDto : request) {
            GeoData geoData = geoDataMap.get(locDto.getGeoId());

            if (geoData == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Geo ID: " + locDto.getGeoId());
            }

            ItineraryLocation newLocation = ItineraryLocation.builder()
                    .orderIndex(locDto.getOrderIndex())
                    .isCurrentStop(locDto.isCurrentStop())
                    .geoData(geoData)
                    .itinerary(entity)
                    .build();

            locationsToRet.add(newLocation);
        }

        return locationsToRet;
    }
}