package com.github.manueldepaduanisdev.tripplanner.services;

import com.github.manueldepaduanisdev.tripplanner.domain.GeoData;
import com.github.manueldepaduanisdev.tripplanner.dto.response.GeoDataResponseDTO;
import com.github.manueldepaduanisdev.tripplanner.mappers.GeoDataMapper;
import com.github.manueldepaduanisdev.tripplanner.repositories.GeoDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service used to retrieve Geo Data information
 */
@Service
@RequiredArgsConstructor
@Slf4j
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
        log.info("Searching GeoData in DB. Params -> Country: [{}], Region: [{}], Province: [{}], City: [{}]",
                country, region, province, city);

        List<GeoData> geoDataList = _geoDataRepository.searchGeoData(country, region, province, city);

        log.info("Search finished. Retrieved and mapping {} GeoData entities.", geoDataList.size());

        return geoDataList.stream()
                .map(geoDataMapper::toDTO) // For each element
                .toList();
    }
}