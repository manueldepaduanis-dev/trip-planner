package com.github.manueldepaduanisdev.tripplanner.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ItineraryResponse {
    private String id; //UUID
    private String title;
    private String status;
    private LocalDateTime createdAt;

    private List<LocationResponse> locations;

    @Data
    @Builder
    public static class LocationResponse {
        private Long id;
        private Integer orderIndex;
        private Boolean isCurrentStop;

        private GeoDataResponseDTO geoData;
    }
}
