package com.github.manueldepaduanisdev.tripplanner.controllers;

import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import com.github.manueldepaduanisdev.tripplanner.dto.request.ItineraryRequestDTO;
import com.github.manueldepaduanisdev.tripplanner.dto.response.ItineraryResponseDTO;
import com.github.manueldepaduanisdev.tripplanner.services.ItineraryService;
import com.github.manueldepaduanisdev.tripplanner.services.ItineraryTaskManagerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/itinerary")
@AllArgsConstructor
@Validated // Validate params of methods
@Slf4j
public class ItineraryController {

    private ItineraryService itineraryService;
    private ItineraryTaskManagerService taskManagerService;

    /**
     *
     * @param itinerary body
     * @param sessionId optional session id
     * @return 202 Accepted and itinerary saved
     */
    @PostMapping
    public ResponseEntity<ItineraryResponseDTO> create(
            @RequestBody @Valid ItineraryRequestDTO itinerary,
            // Read from header the session id
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId
    ) {
        log.info("Received request to create a new itinerary. Title: '{}', SessionID present: {}",
                itinerary.getTitle(), (sessionId != null && !sessionId.isBlank()));

        // If there are two or more location with isCurrentStop to true -> error
        if (itinerary.getLocations().stream().filter(ItineraryRequestDTO.LocationRequest::isCurrentStop).count() > 1) {
            log.warn("There are 2 ore more locations with isCurrentStop to true. Please correct and re-create.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Select just one current stop.");
        }

        ItineraryResponseDTO response = itineraryService.createItinerary(itinerary, sessionId);
        log.info("Itinerary created successfully with ID: {}. Assigned SessionID: {}", response.getId(), response.getSessionId());

        // Start worker to process new itinerary (asynchronously)
        taskManagerService.submitTask(response.getId());
        log.info("Async processing task submitted for itinerary ID: {}", response.getId());

        response.setEstimatedWaitSeconds(
                taskManagerService.calculateTimeRemaining(response.getId(), response.getUpdatedAt())
        );
        // Return again session id
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header("X-Session-ID", response.getSessionId())
                .body(response);
    }

    /**
     * Update all itinerary entity
     * @param id itinerary
     * @param itinerary new itinerary body
     * @param sessionId session id
     * @return itinerary updated
     */
    @PutMapping("/{id}")
    public ResponseEntity<ItineraryResponseDTO> update(
            @PathVariable @NotBlank String id,
            @RequestBody @Valid ItineraryRequestDTO itinerary,
            @RequestHeader(value = "X-Session-ID", required = true) String sessionId
    ) {
        log.info("Received request to update itinerary ID: {} for SessionID: {}", id, sessionId);

        if (sessionId.isBlank()) {
            log.warn("Update failed: SessionID is missing or blank for itinerary ID: {}", id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: SessionId missing.");
        }

        // If there are two or more location with isCurrentStop to true -> error
        if (itinerary.getLocations().stream().filter(ItineraryRequestDTO.LocationRequest::isCurrentStop).count() > 1) {
            log.warn("There are 2 ore more locations with isCurrentStop to true. Please correct and re-update.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Select just one current stop.");
        }

        ItineraryResponseDTO response = itineraryService.updateItinerary(sessionId, id, itinerary);
        log.info("Itinerary ID: {} updated successfully. Status set to QUEUED.", id);

        // Start worker to process new itinerary (asynchronously)
        taskManagerService.submitTask(response.getId());
        log.info("Async processing task re-submitted for itinerary ID: {}", response.getId());

        response.setEstimatedWaitSeconds(
                taskManagerService.calculateTimeRemaining(response.getId(), response.getUpdatedAt())
        );
        // Return again session id
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header("X-Session-ID", response.getSessionId())
                .body(response);
    }

    /**
     * Update itinerary next stop
     * @param id itinerary id
     * @param sessionId session id
     * @return itinerary updated
     */
    @PatchMapping("/{id}/next-stop")
    public ResponseEntity<ItineraryResponseDTO> updateNextStop(
            @PathVariable @NotBlank String id,
            @RequestHeader(value = "X-Session-ID", required = true) String sessionId
    ) {
        log.info("Received request to update next stop with itinerary ID: {} for SessionID: {}", id, sessionId);

        if (sessionId.isBlank()) {
            log.warn("Update failed: SessionID is missing or blank for itinerary ID: {}", id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: SessionId missing.");
        }

        ItineraryResponseDTO response = itineraryService.updateNextStop(sessionId, id);
        log.info("Itinerary ID: {} updated successfully. Status set to QUEUED.", id);

        taskManagerService.submitTask(response.getId());
        log.info("Async processing task re-submitted for itinerary ID: {}", response.getId());

        response.setEstimatedWaitSeconds(
                taskManagerService.calculateTimeRemaining(response.getId(), response.getUpdatedAt())
        );
        // Return again session id
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header("X-Session-ID", response.getSessionId())
                .body(response);
    }

    /**
     * Get the itinerary list filtered by status
     * @param status status filter
     * @param sessionId session id
     * @return Itinerary list
     */
    @GetMapping()
    public ResponseEntity<List<ItineraryResponseDTO>> get(
            @RequestParam(required = false) Status status,
            @RequestHeader(value = "X-Session-ID", required = true) String sessionId
    ) {
        log.info("Fetching itinerary list for SessionID: {}. Filter Status: {}", sessionId, status);

        if (sessionId.isBlank()) {
            log.warn("Get List failed: SessionID is missing.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: SessionId missing.");
        }

        List<ItineraryResponseDTO> response = itineraryService.getList(sessionId, status);

        response.forEach(itinerary -> {
            var waitSeconds = 0L;
            if(itinerary.getStatus() != Status.FAILED && itinerary.getStatus() != Status.COMPLETED) {
                waitSeconds = taskManagerService.calculateTimeRemaining(itinerary.getId(), itinerary.getUpdatedAt());
            }
            itinerary.setEstimatedWaitSeconds(waitSeconds);
        });

        log.info("Returning {} itineraries for SessionID: {}", response.size(), sessionId);

        // Return again session id
        return ResponseEntity.status(HttpStatus.OK)
                .header("X-Session-ID", sessionId)
                .body(response);
    }

    /**
     * Get itinerary by id
     * @param id itinerary id
     * @param sessionId session id
     * @return itinerary detail
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItineraryResponseDTO> getById(
            @PathVariable @NotBlank String id,
            @RequestHeader(value = "X-Session-ID", required = true) String sessionId
    ) {
        log.info("Fetching details for itinerary ID: {} and SessionID: {}", id, sessionId);

        if (sessionId.isBlank()) {
            log.warn("Get By ID failed: SessionID is missing for request on ID: {}", id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: SessionId missing.");
        }

        ItineraryResponseDTO response = itineraryService.getById(sessionId, id);

        // If status is not failed or completed -> calculate time remaining. Else time remaining is 0
        response.setEstimatedWaitSeconds(
                (response.getStatus() != Status.FAILED && response.getStatus() != Status.COMPLETED)
                        ? taskManagerService.calculateTimeRemaining(response.getId(), response.getUpdatedAt())
                        : 0
        );

        log.debug("Itinerary details retrieved successfully for ID: {}", id);

        // Return again session id
        return ResponseEntity.status(HttpStatus.OK)
                .header("X-Session-ID", response.getSessionId())
                .body(response);
    }
}