package com.github.manueldepaduanisdev.tripplanner.dto.response;

import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ItineraryResponseDTO {
    private String id; //UUID
    private String title;
    private Status status;
    private String sessionId;
    private LocalDateTime createdAt;
    private Long estimatedWaitSeconds;
    private String waitMessage;

    private List<LocationResponseDTO> locations;

    @Data
    @Builder
    public static class LocationResponseDTO {
        private Long id;
        private Integer orderIndex;
        private Boolean isCurrentStop;

        private GeoDataResponseDTO geoData;
    }
}
