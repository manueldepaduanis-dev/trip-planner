package com.github.manueldepaduanisdev.tripplanner.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeoDataResponseDTO {
    private Long id;
    private String country;
    private String region;
    private String province;
    private String city;
}
