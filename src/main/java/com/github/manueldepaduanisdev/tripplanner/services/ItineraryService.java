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

import java.util.*;
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

    @Transactional // If something fail -> rollback all.
    public ItineraryResponseDTO createItinerary(@NotNull ItineraryRequestDTO request, @Nullable String sessionId) {
        log.info("Creating new itinerary. Title: '{}', SessionID provided: {}", request.getTitle(), sessionId != null);

        // If session id passed is not present in db -> error
        if(sessionId != null && !sessionId.isBlank()) {
            itineraryRepository.findFirstBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.error("This session ID: {}, is not valid.", sessionId);
                    return new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "This session ID: " + sessionId + ", is not valid."
                    );
                });
        }

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

        log.info("Itinerary created successfully. ID: {}, SessionID: {}", savedItinerary.getId(), savedItinerary.getSessionId());

        // Return itinerary saved
        return itineraryMapper.toDTO(savedItinerary);
    }

    @Transactional
    public ItineraryResponseDTO updateItinerary(@NotBlank String sessionId, @NotBlank String id, @NotNull ItineraryRequestDTO request) {
        log.info("Updating itinerary ID: {} for SessionID: {}", id, sessionId);

        Itinerary itinerary = itineraryRepository.findByIdWithLocationsAndGeoData(sessionId, id)
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

        log.info("Itinerary ID: {} updated and sent to queue.", id);

        return itineraryMapper.toDTO(itineraryUpdated);
    }

    @Transactional
    public ItineraryResponseDTO updateNextStop(@NotBlank String sessionId, @NotBlank String id) {
        log.info("Updating next stop request for itinerary ID: {} - SessionID: {}", id, sessionId);

        Itinerary itinerary = itineraryRepository.findByIdWithLocationsAndGeoData(sessionId, id)
                .orElseThrow(() -> {
                    log.error("Itinerary not found. ID: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found");
                });

        List<ItineraryLocation> locations = itinerary.getItineraryLocations();

        // 1. Find location with active current stop. If no one, value will be null
        ItineraryLocation currentStop = locations.stream()
                .filter(ItineraryLocation::isCurrentStop)
                .findFirst()
                .orElse(null);

        int nextOrderIndex = 0;

        // 2. If is present an active current stop, I'll get next one
        if (currentStop != null) {
            nextOrderIndex = currentStop.getOrderIndex() + 1;
        }

        // 3. Check if is present the new index in the locations list. If not -> last location was the last one
        int indexToSearch = nextOrderIndex;
        Optional<ItineraryLocation> nextStopOptional = locations.stream()
                .filter(l -> l.getOrderIndex() == indexToSearch)
                .findFirst();

        // 4. Update logic
        if (nextStopOptional.isPresent()) {
            // Case: next stop exists

            // a. Disable old stop
            if (currentStop != null) {
                currentStop.setCurrentStop(false);
            }

            // b. New current stop
            ItineraryLocation nextStop = nextStopOptional.get();
            nextStop.setCurrentStop(true);

            log.info("Moved current stop from index {} to {}",
                    (currentStop != null ? currentStop.getOrderIndex() : "START"),
                    nextStop.getOrderIndex());

            // Update db and rerun worker
            Itinerary itineraryUpdated = taskManagerService.handleUpdateInQueue(itinerary);
            return itineraryMapper.toDTO(itineraryUpdated);

        } else {
            // Case: A next stop doesn't exist (last one or empty list)
            log.info("Next stop not found for index {}. Current stop is likely the last one. No changes applied.", indexToSearch);

            // Return as-is
            return itineraryMapper.toDTO(itinerary);
        }
    }

    public List<ItineraryResponseDTO> getList(@NotBlank String sessionId, @Nullable Status status) {
        log.info("Retrieving itinerary list. SessionID: {}, Status Filter: {}", sessionId, status);

        return itineraryRepository.findBySessionIdAndStatus(sessionId, status).stream()
                .map(itineraryMapper::toDTO)
                .toList();
    }

    public ItineraryResponseDTO getById(@NotBlank String sessionId, @NotBlank String id) {
        log.info("Fetching itinerary details. ID: {}, SessionID: {}", id, sessionId);

        Itinerary itinerary = itineraryRepository.findByIdWithLocationsAndGeoData(sessionId, id)
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
            log.warn("GeoData mismatch. Requested IDs count: {}, Found in DB: {}", geoIds.size(), geoDataList.size());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more GeoData IDs provided do not exist.");
        }

        // Map<ID, GeoData>
        Map<Long, GeoData> geoDataMap = geoDataList.stream()
                .collect(Collectors.toMap(GeoData::getId, geo -> geo));

        List<ItineraryLocation> locationsToRet = new ArrayList<>();

        for (ItineraryRequestDTO.LocationRequest locDto : request) {
            GeoData geoData = geoDataMap.get(locDto.getGeoId());

            if (geoData == null) {
                // This should not happen due to size check above, but safety first :)
                log.error("Invalid Geo ID found during mapping: {}", locDto.getGeoId());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Geo ID: " + locDto.getGeoId());
            }

            ItineraryLocation newLocation = ItineraryLocation.builder()
                    .orderIndex(locDto.getOrderIndex())
                    .currentStop(locDto.isCurrentStop())
                    .geoData(geoData)
                    .itinerary(entity)
                    .build();

            locationsToRet.add(newLocation);
        }

        return locationsToRet;
    }
}