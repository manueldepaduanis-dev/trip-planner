package com.github.manueldepaduanisdev.tripplanner.repositories;

import com.github.manueldepaduanisdev.tripplanner.domain.GeoData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeoRepository extends JpaRepository<GeoData, Long> {
}
