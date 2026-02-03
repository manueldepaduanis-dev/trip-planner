package com.github.manueldepaduanisdev.tripplanner.mappers;

import com.github.manueldepaduanisdev.tripplanner.domain.GeoData;
import com.github.manueldepaduanisdev.tripplanner.dto.response.GeoDataResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GeoDataMapper {

    GeoDataResponseDTO toDTO(GeoData entity);

    GeoData toEntity(GeoDataResponseDTO dto);
}
