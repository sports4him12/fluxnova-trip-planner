package com.fluxnova.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "destinations")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String country;

    private String region;

    // Comma-separated best seasons stored as a string; resolved via @ElementCollection
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "destination_seasons", joinColumns = @JoinColumn(name = "destination_id"))
    @Column(name = "season")
    private List<Season> bestSeasons;

    @Column(length = 1000)
    private String description;

    // Free-text activity tags (e.g. "hiking,beach,museum")
    private String tags;
}
