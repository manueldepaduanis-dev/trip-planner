package com.github.manueldepaduanisdev.tripplanner.controllers;

import com.github.manueldepaduanisdev.tripplanner.dto.response.GeoDataResponseDTO;
import com.github.manueldepaduanisdev.tripplanner.services.GeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
// When FE will be implemented
// @CrossOrigin(origins = "*")
public class GeoController {

    private final GeoService _geoService;

    /**
     * Search geo data with optional filters
     * Method: GET
     * Url: /api/geo?country=Italy&region=Lazio&Province=Rome&City=Rome
     * @return ordered and filtered geo data list
     */
    @GetMapping
    public ResponseEntity<List<GeoDataResponseDTO>> getGeoData(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city
    ) {
        List<GeoDataResponseDTO> results = _geoService.searchGeoData(country, region, province, city);
        return ResponseEntity.ok(results);
    }
}
