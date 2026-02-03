package com.github.manueldepaduanisdev.tripplanner.controllers;

import com.github.manueldepaduanisdev.tripplanner.dto.request.ItineraryRequestDTO;
import com.github.manueldepaduanisdev.tripplanner.dto.response.ItineraryResponseDTO;
import com.github.manueldepaduanisdev.tripplanner.services.ItineraryService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/itinerary")
@AllArgsConstructor
public class ItineraryController {

    private ItineraryService _itineraryService;

    /**
     *
     * @param request body
     * @param sessionId optional session id
     * @return 202 Accepted and itinerary saved
     */
    @PostMapping
    public ResponseEntity<ItineraryResponseDTO> createItinerary(
            @RequestBody ItineraryRequestDTO request,
            // Read from header the session id
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId
    ) {
        ItineraryResponseDTO response = _itineraryService.createItineraryProcess(request, sessionId);

        // Return again session id
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header("X-Session-ID", response.getSessionId())
                .body(response);
    }
}
