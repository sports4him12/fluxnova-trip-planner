package com.fluxnova.service;

import com.fluxnova.model.Destination;
import com.fluxnova.model.Season;
import com.fluxnova.model.Trip;
import com.fluxnova.model.TravelWindow;
import com.fluxnova.repository.DestinationRepository;
import com.fluxnova.repository.TravelWindowRepository;
import com.fluxnova.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final DestinationRepository destinationRepository;
    private final TravelWindowRepository travelWindowRepository;

    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    public Trip getTripById(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + id));
    }

    public List<Trip> getTripsBySeason(Season season) {
        return tripRepository.findBySeason(season);
    }

    @Transactional
    public Trip createTrip(Trip trip, Long destinationId, Long travelWindowId) {
        if (destinationId != null) {
            Destination dest = destinationRepository.findById(destinationId)
                    .orElseThrow(() -> new IllegalArgumentException("Destination not found: " + destinationId));
            trip.setDestination(dest);
        }
        if (travelWindowId != null) {
            TravelWindow window = travelWindowRepository.findById(travelWindowId)
                    .orElseThrow(() -> new IllegalArgumentException("TravelWindow not found: " + travelWindowId));
            trip.setTravelWindow(window);
        }
        return tripRepository.save(trip);
    }

    @Transactional
    public Trip updateTrip(Long id, Trip updates) {
        Trip existing = getTripById(id);
        existing.setTitle(updates.getTitle());
        existing.setSeason(updates.getSeason());
        existing.setStartDate(updates.getStartDate());
        existing.setEndDate(updates.getEndDate());
        existing.setNotes(updates.getNotes());
        return tripRepository.save(existing);
    }

    @Transactional
    public void deleteTrip(Long id) {
        tripRepository.deleteById(id);
    }

    // ── Destinations ────────────────────────────────────────────────────────

    public List<Destination> getAllDestinations() {
        return destinationRepository.findAll();
    }

    public List<Destination> getDestinationsBySeason(Season season) {
        return destinationRepository.findByBestSeason(season);
    }

    @Transactional
    public Destination saveDestination(Destination destination) {
        return destinationRepository.save(destination);
    }

    // ── Travel Windows ───────────────────────────────────────────────────────

    public List<TravelWindow> getTravelWindows(Integer year, Season season) {
        if (year != null && season != null) {
            return travelWindowRepository.findByYearAndSeason(year, season);
        }
        return travelWindowRepository.findAll();
    }

    @Transactional
    public TravelWindow saveTravelWindow(TravelWindow window) {
        return travelWindowRepository.save(window);
    }
}
