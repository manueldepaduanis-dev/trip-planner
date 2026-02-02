package com.github.manueldepaduanisdev.tripplanner.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "itinerary_location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private Integer order_index;

    @Column(nullable = false)
    private Boolean isCurrentStop = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "geo_data_id", nullable = false)
    private GeoData geoData;

    @ManyToOne
    @JoinColumn(name = "itinerary_id", nullable = false)
    @JsonIgnore
    private Itinerary itinerary;
}
