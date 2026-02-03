package com.github.manueldepaduanisdev.tripplanner.services;

import com.github.manueldepaduanisdev.tripplanner.domain.GeoData;
import com.github.manueldepaduanisdev.tripplanner.dto.response.GeoDataResponseDTO;
import com.github.manueldepaduanisdev.tripplanner.mappers.GeoDataMapper;
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
    private final GeoDataMapper geoDataMapper;
    /**
     *
     * @param country nullable
     * @param region nullable
     * @param province nullable
     * @param city nullable
     * @return an ascending ordered and filtered list of geo data
     */
    public List<GeoDataResponseDTO> searchGeoData(String country, String region, String province, String city) {

        List<GeoData> geoDataList = _geoDataRepository.searchGeoData(country, region, province, city);
        return geoDataList.stream()
                .map(geoDataMapper::toDTO) // For each element
                .toList();
    }
}
