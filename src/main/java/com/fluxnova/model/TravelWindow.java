package com.fluxnova.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "travel_windows")
@Getter
@Setter
public class TravelWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Integer year;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Season season;

    @Min(1)
    private int availableDays;

    @Column(length = 500)
    private String notes;
}
