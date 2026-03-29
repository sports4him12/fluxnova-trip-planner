package com.fluxnova.service;

import com.fluxnova.model.*;
import com.fluxnova.repository.DestinationRepository;
import com.fluxnova.repository.TravelWindowRepository;
import com.fluxnova.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock TripRepository tripRepository;
    @Mock DestinationRepository destinationRepository;
    @Mock TravelWindowRepository travelWindowRepository;
    @InjectMocks TripService tripService;

    // ── getAllTrips ─────────────────────────────────────────────────────────

    @Test
    void getAllTrips_returnsList() {
        Trip t = new Trip();
        when(tripRepository.findAll()).thenReturn(List.of(t));
        assertThat(tripService.getAllTrips()).containsExactly(t);
    }

    // ── getTripById ─────────────────────────────────────────────────────────

    @Test
    void getTripById_found_returnsTrip() {
        Trip t = new Trip();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(t));
        assertThat(tripService.getTripById(1L)).isSameAs(t);
    }

    @Test
    void getTripById_notFound_throwsIllegalArgument() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> tripService.getTripById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    // ── getTripsBySeason ────────────────────────────────────────────────────

    @Test
    void getTripsBySeason_delegatesToRepository() {
        Trip t = new Trip();
        when(tripRepository.findBySeason(Season.SUMMER)).thenReturn(List.of(t));
        assertThat(tripService.getTripsBySeason(Season.SUMMER)).containsExactly(t);
    }

    // ── createTrip ──────────────────────────────────────────────────────────

    @Test
    void createTrip_noDestinationNoWindow_savesDirectly() {
        Trip t = new Trip();
        when(tripRepository.save(t)).thenReturn(t);
        assertThat(tripService.createTrip(t, null, null)).isSameAs(t);
        verify(destinationRepository, never()).findById(any());
        verify(travelWindowRepository, never()).findById(any());
    }

    @Test
    void createTrip_withDestination_setsDestinationOnTrip() {
        Trip t = new Trip();
        Destination dest = new Destination();
        when(destinationRepository.findById(1L)).thenReturn(Optional.of(dest));
        when(tripRepository.save(t)).thenReturn(t);
        Trip result = tripService.createTrip(t, 1L, null);
        assertThat(result.getDestination()).isSameAs(dest);
    }

    @Test
    void createTrip_destinationNotFound_throws() {
        when(destinationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> tripService.createTrip(new Trip(), 99L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createTrip_withTravelWindow_setsTravelWindowOnTrip() {
        Trip t = new Trip();
        TravelWindow w = new TravelWindow();
        when(travelWindowRepository.findById(2L)).thenReturn(Optional.of(w));
        when(tripRepository.save(t)).thenReturn(t);
        Trip result = tripService.createTrip(t, null, 2L);
        assertThat(result.getTravelWindow()).isSameAs(w);
    }

    @Test
    void createTrip_travelWindowNotFound_throws() {
        when(travelWindowRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> tripService.createTrip(new Trip(), null, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    // ── updateTrip ──────────────────────────────────────────────────────────

    @Test
    void updateTrip_updatesFieldsAndSaves() {
        Trip existing = new Trip();
        existing.setTitle("Old");
        Trip updates = new Trip();
        updates.setTitle("New");
        updates.setSeason(Season.FALL);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tripRepository.save(existing)).thenReturn(existing);
        Trip result = tripService.updateTrip(1L, updates);
        assertThat(result.getTitle()).isEqualTo("New");
        assertThat(result.getSeason()).isEqualTo(Season.FALL);
    }

    // ── deleteTrip ──────────────────────────────────────────────────────────

    @Test
    void deleteTrip_callsRepositoryDelete() {
        tripService.deleteTrip(5L);
        verify(tripRepository).deleteById(5L);
    }

    // ── Destinations ────────────────────────────────────────────────────────

    @Test
    void getAllDestinations_returnsList() {
        Destination d = new Destination();
        when(destinationRepository.findAll()).thenReturn(List.of(d));
        assertThat(tripService.getAllDestinations()).containsExactly(d);
    }

    @Test
    void getDestinationsBySeason_delegatesToRepository() {
        Destination d = new Destination();
        when(destinationRepository.findByBestSeason(Season.WINTER)).thenReturn(List.of(d));
        assertThat(tripService.getDestinationsBySeason(Season.WINTER)).containsExactly(d);
    }

    @Test
    void saveDestination_delegatesToRepository() {
        Destination d = new Destination();
        when(destinationRepository.save(d)).thenReturn(d);
        assertThat(tripService.saveDestination(d)).isSameAs(d);
    }

    // ── TravelWindows ────────────────────────────────────────────────────────

    @Test
    void getTravelWindows_withYearAndSeason_delegatesToFilteredQuery() {
        TravelWindow w = new TravelWindow();
        when(travelWindowRepository.findByYearAndSeason(2026, Season.SPRING)).thenReturn(List.of(w));
        assertThat(tripService.getTravelWindows(2026, Season.SPRING)).containsExactly(w);
        verify(travelWindowRepository, never()).findAll();
    }

    @Test
    void getTravelWindows_withNullParams_returnsAll() {
        TravelWindow w = new TravelWindow();
        when(travelWindowRepository.findAll()).thenReturn(List.of(w));
        assertThat(tripService.getTravelWindows(null, null)).containsExactly(w);
    }

    @Test
    void getTravelWindows_withOnlyYear_returnsAll() {
        when(travelWindowRepository.findAll()).thenReturn(List.of());
        tripService.getTravelWindows(2026, null);
        verify(travelWindowRepository).findAll();
    }

    @Test
    void saveTravelWindow_delegatesToRepository() {
        TravelWindow w = new TravelWindow();
        when(travelWindowRepository.save(w)).thenReturn(w);
        assertThat(tripService.saveTravelWindow(w)).isSameAs(w);
    }
}
