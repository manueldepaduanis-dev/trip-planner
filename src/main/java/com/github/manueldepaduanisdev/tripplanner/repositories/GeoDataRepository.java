package com.github.manueldepaduanisdev.tripplanner.repositories;

import com.github.manueldepaduanisdev.tripplanner.domain.GeoData;
import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GeoDataRepository extends JpaRepository<GeoData, Long> {

    @Query("SELECT gd FROM GeoData gd WHERE " +
            "(:country IS NULL OR LOWER(gd.country) LIKE LOWER(CONCAT('%', :country, '%'))) AND " +
            "(:region IS NULL OR LOWER(gd.region) LIKE LOWER(CONCAT('%', :region, '%'))) AND " +
            "(:province IS NULL OR LOWER(gd.province) LIKE LOWER(CONCAT('%', :province, '%'))) AND " +
            "(:city IS NULL OR LOWER(gd.city) LIKE LOWER(CONCAT('%', :city, '%')))" +
            "ORDER BY gd.country ASC, gd.region ASC, gd.province ASC, gd.city ASC")
    List<GeoData> searchItineraries(@Param("country") String country,
                                    @Param("region") String region,
                                    @Param("province") String province,
                                    @Param("city") String city);
}
