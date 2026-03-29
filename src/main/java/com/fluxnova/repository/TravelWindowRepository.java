package com.fluxnova.repository;

import com.fluxnova.model.Season;
import com.fluxnova.model.TravelWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelWindowRepository extends JpaRepository<TravelWindow, Long> {

    List<TravelWindow> findByYearAndSeason(Integer year, Season season);
}
