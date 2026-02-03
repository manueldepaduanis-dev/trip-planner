package com.github.manueldepaduanisdev.tripplanner.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "geo_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String country;

    private String region;

    private String province;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;
}
