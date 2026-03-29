package com.fluxnova.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "trips")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    private Destination destination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_window_id")
    private TravelWindow travelWindow;

    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Season season;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status = TripStatus.DRAFT;

    @Column(length = 1000)
    private String notes;

    // FluxNova process instance ID — set once a workflow is started for this trip
    private String workflowInstanceId;

    // Tracks the current active task key within the FluxNova workflow
    private String currentTaskKey;
}
