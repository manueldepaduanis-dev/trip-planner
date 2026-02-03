package com.github.manueldepaduanisdev.tripplanner.mappers;

import com.github.manueldepaduanisdev.tripplanner.domain.GeoData;
import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import com.github.manueldepaduanisdev.tripplanner.dto.response.GeoDataResponseDTO;
import com.github.manueldepaduanisdev.tripplanner.dto.response.ItineraryResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {ItineraryLocationMapper.class})
public interface ItineraryMapper {

    ItineraryResponseDTO toDTO(Itinerary entity);

    Itinerary toEntity(ItineraryResponseDTO dto);
}
