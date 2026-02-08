package com.github.manueldepaduanisdev.tripplanner.dto.response;

import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryResponseDTO {
    private String id; //UUID
    private String title;
    private Status status;
    private String sessionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long estimatedWaitSeconds;

    private List<LocationResponseDTO> itineraryLocations;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LocationResponseDTO {
        private long id;
        private int orderIndex;
        private boolean currentStop = false;

        private GeoDataResponseDTO geoData;
    }
}
