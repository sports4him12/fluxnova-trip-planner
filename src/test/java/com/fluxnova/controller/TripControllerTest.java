package com.fluxnova.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluxnova.model.Destination;
import com.fluxnova.model.Season;
import com.fluxnova.model.TravelWindow;
import com.fluxnova.model.Trip;
import com.fluxnova.service.TripPdfService;
import com.fluxnova.service.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TripController.class)
class TripControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean TripService tripService;
    @MockitoBean TripPdfService tripPdfService;

    // ── GET /api/trips ──────────────────────────────────────────────────────

    @Test
    void listTrips_noFilter_returnsAll() throws Exception {
        when(tripService.getAllTrips()).thenReturn(List.of(trip(1L, "Yellowstone")));
        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Yellowstone"));
    }

    @Test
    void listTrips_withSeasonFilter_filtersBySeason() throws Exception {
        when(tripService.getTripsBySeason(Season.SUMMER)).thenReturn(List.of(trip(1L, "Summer Trip")));
        mockMvc.perform(get("/api/trips").param("season", "SUMMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Summer Trip"));
    }

    // ── GET /api/trips/{id} ─────────────────────────────────────────────────

    @Test
    void getTrip_returnsTrip() throws Exception {
        when(tripService.getTripById(1L)).thenReturn(trip(1L, "Yellowstone"));
        mockMvc.perform(get("/api/trips/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Yellowstone"));
    }

    // ── POST /api/trips ─────────────────────────────────────────────────────

    @Test
    void createTrip_returns201() throws Exception {
        Trip input = trip(null, "New Trip");
        Trip saved = trip(1L, "New Trip");
        when(tripService.createTrip(any(Trip.class), isNull(), isNull())).thenReturn(saved);

        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createTrip_withDestinationId_passesIdToService() throws Exception {
        Trip saved = trip(1L, "New Trip");
        when(tripService.createTrip(any(Trip.class), eq(5L), isNull())).thenReturn(saved);

        mockMvc.perform(post("/api/trips")
                        .param("destinationId", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(trip(null, "New Trip"))))
                .andExpect(status().isCreated());
    }

    // ── PUT /api/trips/{id} ─────────────────────────────────────────────────

    @Test
    void updateTrip_returnsUpdatedTrip() throws Exception {
        Trip updated = trip(1L, "Updated");
        when(tripService.updateTrip(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/trips/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    // ── DELETE /api/trips/{id} ──────────────────────────────────────────────

    @Test
    void deleteTrip_returns204() throws Exception {
        mockMvc.perform(delete("/api/trips/1"))
                .andExpect(status().isNoContent());
        verify(tripService).deleteTrip(1L);
    }

    // ── GET /api/trips/destinations ─────────────────────────────────────────

    @Test
    void listDestinations_noFilter_returnsAll() throws Exception {
        Destination d = new Destination();
        d.setId(1L);
        d.setName("Yellowstone");
        when(tripService.getAllDestinations()).thenReturn(List.of(d));

        mockMvc.perform(get("/api/trips/destinations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Yellowstone"));
    }

    @Test
    void listDestinations_withSeason_filtersBySeason() throws Exception {
        when(tripService.getDestinationsBySeason(Season.FALL)).thenReturn(List.of());
        mockMvc.perform(get("/api/trips/destinations").param("season", "FALL"))
                .andExpect(status().isOk());
        verify(tripService).getDestinationsBySeason(Season.FALL);
    }

    @Test
    void createDestination_returns201() throws Exception {
        Destination d = new Destination();
        d.setId(1L);
        d.setName("Yellowstone");
        when(tripService.saveDestination(any())).thenReturn(d);

        mockMvc.perform(post("/api/trips/destinations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(d)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Yellowstone"));
    }

    // ── GET /api/trips/travel-windows ───────────────────────────────────────

    @Test
    void listTravelWindows_returnsAll() throws Exception {
        TravelWindow w = new TravelWindow();
        w.setId(1L);
        w.setYear(2026);
        when(tripService.getTravelWindows(null, null)).thenReturn(List.of(w));

        mockMvc.perform(get("/api/trips/travel-windows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].year").value(2026));
    }

    @Test
    void createTravelWindow_returns201() throws Exception {
        TravelWindow w = new TravelWindow();
        w.setId(1L);
        w.setYear(2026);
        w.setSeason(Season.SUMMER);
        w.setAvailableDays(7);
        when(tripService.saveTravelWindow(any())).thenReturn(w);

        mockMvc.perform(post("/api/trips/travel-windows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(w)))
                .andExpect(status().isCreated());
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private Trip trip(Long id, String title) {
        Trip t = new Trip();
        t.setId(id);
        t.setTitle(title);
        t.setSeason(Season.SUMMER);
        return t;
    }
}
