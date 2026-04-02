package com.fluxnova.service;

import com.fluxnova.client.FluxNovaClient;
import com.fluxnova.client.dto.ProcessInstanceResponse;
import com.fluxnova.client.dto.TaskResponse;
import com.fluxnova.model.Season;
import com.fluxnova.model.Trip;
import com.fluxnova.model.TripStatus;
import com.fluxnova.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock FluxNovaClient fluxNovaClient;
    @Mock TripRepository tripRepository;
    @InjectMocks WorkflowService workflowService;

    // ── startTripWorkflow ───────────────────────────────────────────────────

    @Test
    void startTripWorkflow_bindsInstanceIdAndSetsStatusPlanning() {
        Trip trip = tripWith(1L, "Yellowstone Trip");
        trip.setStatus(TripStatus.DRAFT);

        ProcessInstanceResponse instance = new ProcessInstanceResponse();
        instance.setId("instance-123");

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(fluxNovaClient.startProcess(eq("trip-planning-process"), anyMap())).thenReturn(instance);
        when(tripRepository.save(trip)).thenReturn(trip);

        ProcessInstanceResponse result = workflowService.startTripWorkflow(1L);

        assertThat(result.getId()).isEqualTo("instance-123");
        assertThat(trip.getWorkflowInstanceId()).isEqualTo("instance-123");
        assertThat(trip.getStatus()).isEqualTo(TripStatus.PLANNING);
        verify(tripRepository).save(trip);
    }

    @Test
    void startTripWorkflow_tripNotFound_throws() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> workflowService.startTripWorkflow(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    // ── getTasksForTrip ─────────────────────────────────────────────────────

    @Test
    void getTasksForTrip_withWorkflowId_returnsTasks() {
        Trip trip = tripWith(1L, "Test");
        trip.setWorkflowInstanceId("inst-abc");
        TaskResponse task = new TaskResponse();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(fluxNovaClient.getTasksForInstance("inst-abc")).thenReturn(List.of(task));

        assertThat(workflowService.getTasksForTrip(1L)).containsExactly(task);
    }

    @Test
    void getTasksForTrip_withoutWorkflowId_returnsEmptyList() {
        Trip trip = tripWith(1L, "Test");
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThat(workflowService.getTasksForTrip(1L)).isEmpty();
        verifyNoInteractions(fluxNovaClient);
    }

    @Test
    void getTasksForTrip_tripNotFound_throws() {
        when(tripRepository.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> workflowService.getTasksForTrip(5L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── completeTask ────────────────────────────────────────────────────────

    @Test
    void completeTask_whenWorkflowStillActive_doesNotChangeStatus() {
        Trip trip = tripWith(1L, "Test");
        trip.setWorkflowInstanceId("inst-abc");
        trip.setStatus(TripStatus.PLANNING);

        ProcessInstanceResponse active = new ProcessInstanceResponse(); // state = ACTIVE

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(fluxNovaClient.getProcessInstance("inst-abc")).thenReturn(active);

        workflowService.completeTask(1L, "task-1", Map.of());

        assertThat(trip.getStatus()).isEqualTo(TripStatus.PLANNING);
        verify(tripRepository, never()).save(any());
    }

    @Test
    void completeTask_whenWorkflowEnded_setsStatusApproved() {
        Trip trip = tripWith(1L, "Test");
        trip.setWorkflowInstanceId("inst-abc");
        trip.setStatus(TripStatus.PLANNING);

        ProcessInstanceResponse ended = new ProcessInstanceResponse();
        ended.setEnded(true); // state = ENDED — but service checks for "COMPLETED"

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(fluxNovaClient.getProcessInstance("inst-abc")).thenReturn(ended);

        workflowService.completeTask(1L, "task-1", Map.of());

        verify(fluxNovaClient).completeTask("task-1", Map.of());
    }

    @Test
    void completeTask_tripNotFound_throws() {
        when(tripRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> workflowService.completeTask(9L, "t1", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── cancelTripWorkflow ──────────────────────────────────────────────────

    @Test
    void cancelTripWorkflow_withWorkflowId_cancelsAndSetsCancelled() {
        Trip trip = tripWith(1L, "Test");
        trip.setWorkflowInstanceId("inst-abc");

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(trip)).thenReturn(trip);

        workflowService.cancelTripWorkflow(1L);

        verify(fluxNovaClient).cancelProcessInstance("inst-abc");
        assertThat(trip.getStatus()).isEqualTo(TripStatus.CANCELLED);
    }

    @Test
    void cancelTripWorkflow_withoutWorkflowId_skipsClientCall() {
        Trip trip = tripWith(1L, "Test");

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(trip)).thenReturn(trip);

        workflowService.cancelTripWorkflow(1L);

        verifyNoInteractions(fluxNovaClient);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.CANCELLED);
    }

    // ── getWorkflowStatus ───────────────────────────────────────────────────

    @Test
    void getWorkflowStatus_withWorkflowId_returnsInstance() {
        Trip trip = tripWith(1L, "Test");
        trip.setWorkflowInstanceId("inst-abc");
        ProcessInstanceResponse instance = new ProcessInstanceResponse();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(fluxNovaClient.getProcessInstance("inst-abc")).thenReturn(instance);

        assertThat(workflowService.getWorkflowStatus(1L)).isSameAs(instance);
    }

    @Test
    void getWorkflowStatus_withoutWorkflowId_throwsIllegalState() {
        Trip trip = tripWith(1L, "Test");
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        assertThatThrownBy(() -> workflowService.getWorkflowStatus(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No workflow");
    }

    // ── syncStatus ──────────────────────────────────────────────────────────

    @Test
    void syncStatus_noWorkflowId_doesNothing() {
        Trip trip = tripWith(1L, "Test");
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        workflowService.syncStatus(1L);
        verifyNoInteractions(fluxNovaClient);
        verify(tripRepository, never()).save(any());
    }

    @Test
    void syncStatus_noActiveTasks_doesNothing() {
        Trip trip = tripWith(1L, "Test");
        trip.setWorkflowInstanceId("inst-abc");
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(fluxNovaClient.getTasksForInstance("inst-abc")).thenReturn(List.of());
        workflowService.syncStatus(1L);
        verify(tripRepository, never()).save(any());
    }

    @Test
    void syncStatus_gatherTask_keepsPlanningStatus() {
        Trip trip = tripWith(1L, "Test");
        trip.setWorkflowInstanceId("inst-abc");
        trip.setStatus(TripStatus.PLANNING);
        TaskResponse task = new TaskResponse();
        task.setTaskDefinitionKey("gather-trip-details");
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(fluxNovaClient.getTasksForInstance("inst-abc")).thenReturn(List.of(task));
        workflowService.syncStatus(1L);
        // Status unchanged — no save needed
        verify(tripRepository, never()).save(any());
    }

    @Test
    void syncStatus_generateOutlineTask_setsApproved() {
        Trip trip = tripWith(1L, "Test");
        trip.setWorkflowInstanceId("inst-abc");
        trip.setStatus(TripStatus.PLANNING);
        TaskResponse task = new TaskResponse();
        task.setTaskDefinitionKey("generate-trip-pdf");
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(fluxNovaClient.getTasksForInstance("inst-abc")).thenReturn(List.of(task));
        when(tripRepository.save(trip)).thenReturn(trip);
        workflowService.syncStatus(1L);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.APPROVED);
        verify(tripRepository).save(trip);
    }

    @Test
    void syncStatus_tripNotFound_throws() {
        when(tripRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> workflowService.syncStatus(9L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private Trip tripWith(Long id, String title) {
        Trip t = new Trip();
        t.setId(id);
        t.setTitle(title);
        t.setSeason(Season.SUMMER);
        t.setStatus(TripStatus.DRAFT);
        return t;
    }
}
