package com.github.manueldepaduanisdev.tripplanner.mappers;

import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import com.github.manueldepaduanisdev.tripplanner.domain.ItineraryLocation;
import com.github.manueldepaduanisdev.tripplanner.dto.response.ItineraryResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {GeoDataMapper.class})
public interface ItineraryLocationMapper {

    ItineraryResponseDTO.LocationResponseDTO toDTO(ItineraryLocation entity);

    ItineraryLocation toEntity(ItineraryResponseDTO.LocationResponseDTO dto);
}
