package com.fluxnova.repository;

import com.fluxnova.model.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DestinationRepository extends JpaRepository<Destination, Long> {

    @Query("SELECT DISTINCT d FROM Destination d JOIN d.bestSeasons s WHERE s = :season")
    List<Destination> findByBestSeason(@Param("season") com.fluxnova.model.Season season);
}
