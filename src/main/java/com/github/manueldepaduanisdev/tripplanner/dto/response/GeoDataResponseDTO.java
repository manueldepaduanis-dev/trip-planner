package com.github.manueldepaduanisdev.tripplanner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoDataResponseDTO {
    private Long id;
    private String country;
    private String region;
    private String province;
    private String city;
    private Double latitude;
    private Double longitude;
}
