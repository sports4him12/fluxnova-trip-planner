package com.fluxnova.controller;

import com.fluxnova.model.Destination;
import com.fluxnova.model.Season;
import com.fluxnova.model.TravelWindow;
import com.fluxnova.model.Trip;
import com.fluxnova.service.TripPdfService;
import com.fluxnova.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripPdfService tripPdfService;

    private final TripService tripService;

    @GetMapping
    public List<Trip> listTrips(@RequestParam(required = false) Season season) {
        if (season != null) return tripService.getTripsBySeason(season);
        return tripService.getAllTrips();
    }

    @GetMapping("/{id}")
    public Trip getTrip(@PathVariable Long id) {
        return tripService.getTripById(id);
    }

    @PostMapping
    public ResponseEntity<Trip> createTrip(
            @Valid @RequestBody Trip trip,
            @RequestParam(required = false) Long destinationId,
            @RequestParam(required = false) Long travelWindowId) {
        Trip created = tripService.createTrip(trip, destinationId, travelWindowId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Trip updateTrip(@PathVariable Long id, @Valid @RequestBody Trip trip) {
        return tripService.updateTrip(id, trip);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }

    // ── Destinations ────────────────────────────────────────────────────────

    @GetMapping("/destinations")
    public List<Destination> listDestinations(@RequestParam(required = false) Season season) {
        if (season != null) return tripService.getDestinationsBySeason(season);
        return tripService.getAllDestinations();
    }

    @PostMapping("/destinations")
    public ResponseEntity<Destination> createDestination(@Valid @RequestBody Destination destination) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.saveDestination(destination));
    }

    // ── Travel Windows ───────────────────────────────────────────────────────

    @GetMapping("/travel-windows")
    public List<TravelWindow> listTravelWindows(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Season season) {
        return tripService.getTravelWindows(year, season);
    }

    @PostMapping("/travel-windows")
    public ResponseEntity<TravelWindow> createTravelWindow(@Valid @RequestBody TravelWindow window) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.saveTravelWindow(window));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Trip trip = tripService.getTripById(id);
        byte[] pdf = tripPdfService.generate(trip);
        String filename = trip.getTitle().replaceAll("[^a-zA-Z0-9\\-_ ]", "").trim().replace(' ', '_') + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
