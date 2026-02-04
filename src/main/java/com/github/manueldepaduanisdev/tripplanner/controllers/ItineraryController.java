package com.github.manueldepaduanisdev.tripplanner.controllers;

import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import com.github.manueldepaduanisdev.tripplanner.dto.request.ItineraryRequestDTO;
import com.github.manueldepaduanisdev.tripplanner.dto.response.ItineraryResponseDTO;
import com.github.manueldepaduanisdev.tripplanner.services.ItineraryService;
import com.github.manueldepaduanisdev.tripplanner.services.ItineraryTaskManagerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
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
        ItineraryResponseDTO response = itineraryService.createItinerary(itinerary, sessionId);

        // Start worker to process new itinerary (asynchronously)
        taskManagerService.submitTask(response.getId());

        response.setEstimatedWaitSeconds(
                taskManagerService.calculateTimeRemaining(response.getId(), response.getCreatedAt())
        );
        // Return again session id
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header("X-Session-ID", response.getSessionId())
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItineraryResponseDTO> update(
            @PathVariable @NotBlank String id,
            @RequestBody @Valid ItineraryRequestDTO itinerary,
            @RequestHeader(value = "X-Session-ID", required = true) String sessionId
    ) {

        if (sessionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: SessionId missing.");
        }

        ItineraryResponseDTO response = itineraryService.updateItinerary(sessionId, id, itinerary);

        // Start worker to process new itinerary (asynchronously)
        taskManagerService.submitTask(response.getId());

        response.setEstimatedWaitSeconds(
                taskManagerService.calculateTimeRemaining(response.getId(), response.getUpdatedAt())
        );
        // Return again session id
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header("X-Session-ID", response.getSessionId())
                .body(response);
    }

    @GetMapping()
    public ResponseEntity<List<ItineraryResponseDTO>> get(
            @RequestParam(required = false) Status status,
            @RequestHeader(value = "X-Session-ID", required = true) String sessionId
    ) {
        if (sessionId.isBlank()) {
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

        // Return again session id
        return ResponseEntity.status(HttpStatus.OK)
                .header("X-Session-ID", sessionId)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItineraryResponseDTO> getById(
            @PathVariable @NotBlank String id,
            @RequestHeader(value = "X-Session-ID", required = true) String sessionId
    ) {

        if (sessionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: SessionId missing.");
        }

        ItineraryResponseDTO response = itineraryService.getById(sessionId, id);

        // If status is not failed or completed -> calculate time remaining. Else time remaining is 0
        response.setEstimatedWaitSeconds(
                (response.getStatus() != Status.FAILED && response.getStatus() != Status.COMPLETED)
                        ? taskManagerService.calculateTimeRemaining(response.getId(), response.getUpdatedAt())
                        : 0
        );

        // Return again session id
        return ResponseEntity.status(HttpStatus.OK)
                .header("X-Session-ID", response.getSessionId())
                .body(response);
    }
}