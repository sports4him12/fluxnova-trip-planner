package com.fluxnova.ai;

import com.fluxnova.client.dto.ProcessInstanceResponse;
import com.fluxnova.client.dto.TaskResponse;
import com.fluxnova.model.Destination;
import com.fluxnova.model.Season;
import com.fluxnova.model.Trip;
import com.fluxnova.model.TripStatus;
import com.fluxnova.service.TripService;
import com.fluxnova.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowAgentToolsTest {

    @Mock TripService tripService;
    @Mock WorkflowService workflowService;
    @InjectMocks WorkflowAgentTools tools;

    // ── listTrips ────────────────────────────────────────────────────────────

    @Test
    void listTrips_noSeason_returnsAllTrips() {
        Trip t = trip(1L, "Yellowstone", Season.SUMMER);
        when(tripService.getAllTrips()).thenReturn(List.of(t));

        String result = tools.listTrips(null);
        assertThat(result).contains("Yellowstone").contains("SUMMER");
    }

    @Test
    void listTrips_withSeason_filtersBySeason() {
        Trip t = trip(1L, "Aspen", Season.WINTER);
        when(tripService.getTripsBySeason(Season.WINTER)).thenReturn(List.of(t));

        String result = tools.listTrips("WINTER");
        assertThat(result).contains("Aspen");
        verify(tripService, never()).getAllTrips();
    }

    @Test
    void listTrips_empty_returnsNoTripsMessage() {
        when(tripService.getAllTrips()).thenReturn(List.of());
        assertThat(tools.listTrips(null)).isEqualTo("No trips found.");
    }

    @Test
    void listTrips_blankSeason_returnsAllTrips() {
        when(tripService.getAllTrips()).thenReturn(List.of());
        tools.listTrips("  ");
        verify(tripService).getAllTrips();
    }

    // ── getDestinationsForSeason ─────────────────────────────────────────────

    @Test
    void getDestinationsForSeason_returnsFormattedList() {
        Destination d = new Destination();
        d.setId(1L);
        d.setName("Yellowstone");
        d.setRegion("Wyoming");
        d.setTags("hiking");
        when(tripService.getDestinationsBySeason(Season.SUMMER)).thenReturn(List.of(d));

        String result = tools.getDestinationsForSeason("SUMMER");
        assertThat(result).contains("Yellowstone").contains("hiking");
    }

    @Test
    void getDestinationsForSeason_empty_returnsNotFoundMessage() {
        when(tripService.getDestinationsBySeason(Season.FALL)).thenReturn(List.of());
        assertThat(tools.getDestinationsForSeason("FALL")).contains("No destinations");
    }

    // ── getWorkflowTasks ─────────────────────────────────────────────────────

    @Test
    void getWorkflowTasks_withActiveTasks_returnsFormattedList() {
        TaskResponse task = new TaskResponse();
        task.setId("task-1");
        task.setName("Review Destination");
        task.setTaskDefinitionKey("review-destination");
        when(workflowService.getTasksForTrip(1L)).thenReturn(List.of(task));

        String result = tools.getWorkflowTasks(1L);
        assertThat(result).contains("task-1").contains("Review Destination");
    }

    @Test
    void getWorkflowTasks_empty_returnsNoTasksMessage() {
        when(workflowService.getTasksForTrip(1L)).thenReturn(List.of());
        assertThat(tools.getWorkflowTasks(1L)).contains("No active tasks");
    }

    // ── startWorkflow ────────────────────────────────────────────────────────

    @Test
    void startWorkflow_returnsInstanceIdAndState() {
        ProcessInstanceResponse instance = new ProcessInstanceResponse();
        instance.setId("inst-abc");
        when(workflowService.startTripWorkflow(1L)).thenReturn(instance);

        String result = tools.startWorkflow(1L);
        assertThat(result).contains("inst-abc").contains("ACTIVE");
    }

    // ── getWorkflowStatus ────────────────────────────────────────────────────

    @Test
    void getWorkflowStatus_returnsFormattedStatus() {
        ProcessInstanceResponse instance = new ProcessInstanceResponse();
        instance.setId("inst-abc");
        instance.setProcessDefinitionId("trip-planning-process:1:xyz");
        when(workflowService.getWorkflowStatus(1L)).thenReturn(instance);

        String result = tools.getWorkflowStatus(1L);
        assertThat(result).contains("inst-abc").contains("trip-planning-process");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Trip trip(Long id, String title, Season season) {
        Trip t = new Trip();
        t.setId(id);
        t.setTitle(title);
        t.setSeason(season);
        t.setStatus(TripStatus.DRAFT);
        return t;
    }
}
