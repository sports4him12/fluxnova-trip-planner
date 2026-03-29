package com.fluxnova.service;

import com.fluxnova.client.FluxNovaClient;
import com.fluxnova.client.dto.ProcessInstanceResponse;
import com.fluxnova.client.dto.TaskResponse;
import com.fluxnova.model.Trip;
import com.fluxnova.model.TripStatus;
import com.fluxnova.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private static final String TRIP_PLANNING_PROCESS_KEY = "trip-planning-process";

    private final FluxNovaClient fluxNovaClient;
    private final TripRepository tripRepository;

    /** Start a FluxNova workflow for the given trip and bind the instance ID. */
    @Transactional
    public ProcessInstanceResponse startTripWorkflow(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        Map<String, Object> variables = Map.of(
                "tripId", trip.getId(),
                "tripTitle", trip.getTitle(),
                "season", trip.getSeason().name(),
                "destination", trip.getDestination() != null ? trip.getDestination().getName() : "",
                "status", trip.getStatus().name()
        );

        ProcessInstanceResponse instance = fluxNovaClient.startProcess(TRIP_PLANNING_PROCESS_KEY, variables);

        trip.setWorkflowInstanceId(instance.getId());
        trip.setStatus(TripStatus.PLANNING);
        tripRepository.save(trip);

        return instance;
    }

    /** Get the active tasks for a trip's workflow. */
    public List<TaskResponse> getTasksForTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (trip.getWorkflowInstanceId() == null) {
            return List.of();
        }
        return fluxNovaClient.getTasksForInstance(trip.getWorkflowInstanceId());
    }

    /** Complete a task within a trip's workflow. */
    @Transactional
    public void completeTask(Long tripId, String taskId, Map<String, Object> variables) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        fluxNovaClient.completeTask(taskId, variables);

        // Refresh and sync status from the workflow instance
        ProcessInstanceResponse instance = fluxNovaClient.getProcessInstance(trip.getWorkflowInstanceId());
        if ("COMPLETED".equalsIgnoreCase(instance.getState())) {
            trip.setStatus(TripStatus.APPROVED);
            tripRepository.save(trip);
        }
    }

    /** Cancel the workflow for a trip. */
    @Transactional
    public void cancelTripWorkflow(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (trip.getWorkflowInstanceId() != null) {
            fluxNovaClient.cancelProcessInstance(trip.getWorkflowInstanceId());
        }

        trip.setStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);
    }

    /** Get the raw process instance state from FluxNova. */
    public ProcessInstanceResponse getWorkflowStatus(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (trip.getWorkflowInstanceId() == null) {
            throw new IllegalStateException("No workflow started for trip: " + tripId);
        }

        return fluxNovaClient.getProcessInstance(trip.getWorkflowInstanceId());
    }
}
