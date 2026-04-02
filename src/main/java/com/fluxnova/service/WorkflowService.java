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

        // After completing the last task the process instance is deleted by Camunda,
        // so getProcessInstance returns 404. Treat that as "ended".
        try {
            ProcessInstanceResponse instance = fluxNovaClient.getProcessInstance(trip.getWorkflowInstanceId());
            if (instance.isEnded()) {
                trip.setStatus(TripStatus.BOOKED);
                tripRepository.save(trip);
            }
        } catch (Exception e) {
            // 404 = process ended and was cleaned up — mark trip as BOOKED
            trip.setStatus(TripStatus.BOOKED);
            tripRepository.save(trip);
        }
    }

    /** Sync the trip's local status to match the active task in the workflow. */
    @Transactional
    public void syncStatus(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (trip.getWorkflowInstanceId() == null) return;

        List<TaskResponse> tasks = fluxNovaClient.getTasksForInstance(trip.getWorkflowInstanceId());
        if (tasks.isEmpty()) return;

        String taskKey = tasks.get(0).getTaskDefinitionKey();
        TripStatus newStatus = switch (taskKey) {
            case "gather-trip-details" -> TripStatus.PLANNING;
            case "review-and-confirm"  -> TripStatus.PLANNING;
            case "generate-trip-pdf"   -> TripStatus.APPROVED;
            default -> trip.getStatus();
        };

        if (newStatus != trip.getStatus()) {
            trip.setStatus(newStatus);
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
