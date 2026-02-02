package com.github.manueldepaduanisdev.tripplanner.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private Integer order_index;

    @Column(nullable = false)
    private Boolean isCurrentStep = false;

    @Column(nullable = false)
    @ManyToOne()
}
