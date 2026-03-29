package com.fluxnova.repository;

import com.fluxnova.model.Season;
import com.fluxnova.model.Trip;
import com.fluxnova.model.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findBySeason(Season season);

    List<Trip> findByStatus(TripStatus status);

    Optional<Trip> findByWorkflowInstanceId(String workflowInstanceId);
}
