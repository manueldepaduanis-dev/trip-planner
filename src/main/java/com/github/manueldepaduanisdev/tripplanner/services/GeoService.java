package com.github.manueldepaduanisdev.tripplanner.services;

import com.github.manueldepaduanisdev.tripplanner.domain.GeoData;
import com.github.manueldepaduanisdev.tripplanner.dto.response.GeoDataResponseDTO;
import com.github.manueldepaduanisdev.tripplanner.repositories.GeoDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service used to retrieve Geo Data information
 */
@Service
@RequiredArgsConstructor
public class GeoService {

    private final GeoDataRepository _geoDataRepository;

    /**
     *
     * @param country nullable
     * @param region nullable
     * @param province nullable
     * @param city nullable
     * @return an ascending ordered and filtered list of geo data
     */
    public List<GeoDataResponseDTO> searchGeoData(String country, String region, String province, String city) {

        List<GeoData> geoDataList = _geoDataRepository.searchItineraries(country, region, province, city);
        return geoDataList.stream()
                .map(this::mapToDTO) // For each element
                .toList();
    }

    /**
     * Get the data entity and return the mapped DTO
     * @param entity to map into DTO
     * @return DTO
     */
    private GeoDataResponseDTO mapToDTO(GeoData entity) {
        return GeoDataResponseDTO.builder()
                .id(entity.getId())
                .country(entity.getCountry())
                .region(entity.getRegion())
                .province(entity.getProvince())
                .city(entity.getCity())
                .build();
    }
}
