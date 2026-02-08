package com.github.manueldepaduanisdev.tripplanner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryRequestDTO {

    @NotBlank(message = "Title is required.")
    private String title;

    @NotEmpty(message = "Add at least one stop.")
    private List<LocationRequest> locations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationRequest {
        @NotNull
        private Long geoId;
        @NotNull
        private Integer orderIndex;
        private boolean currentStop;
    }
}
