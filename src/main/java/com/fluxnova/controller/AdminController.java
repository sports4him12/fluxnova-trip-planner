package com.fluxnova.controller;

import com.fluxnova.client.FluxNovaClient;
import com.fluxnova.client.dto.TaskResponse;
import com.fluxnova.model.Trip;
import com.fluxnova.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final FluxNovaClient fluxNovaClient;
    private final TripRepository tripRepository;

    /**
     * Returns all trips that have an active workflow instance, enriched with
     * their current Camunda task so the admin page can show what step each is on.
     */
    @GetMapping("/processes")
    public List<Map<String, Object>> getActiveProcesses() {
        List<Trip> tripsWithWorkflow = tripRepository.findAll().stream()
                .filter(t -> t.getWorkflowInstanceId() != null)
                .toList();

        return tripsWithWorkflow.stream().map(trip -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tripId",      trip.getId());
            row.put("tripTitle",   trip.getTitle());
            row.put("tripStatus",  trip.getStatus() != null ? trip.getStatus().name() : "UNKNOWN");
            row.put("instanceId",  trip.getWorkflowInstanceId());
            row.put("season",      trip.getSeason() != null ? trip.getSeason().name() : "");

            // Fetch active tasks — may be empty if workflow ended
            try {
                List<TaskResponse> tasks = fluxNovaClient.getTasksForInstance(trip.getWorkflowInstanceId());
                if (!tasks.isEmpty()) {
                    TaskResponse task = tasks.get(0);
                    row.put("activeTaskKey",  task.getTaskDefinitionKey());
                    row.put("activeTaskName", task.getName());
                    row.put("taskId",         task.getId());
                    row.put("taskCreated",    task.getCreated());
                } else {
                    row.put("activeTaskKey",  null);
                    row.put("activeTaskName", "Completed");
                    row.put("taskId",         null);
                    row.put("taskCreated",    null);
                }
            } catch (Exception e) {
                row.put("activeTaskKey",  null);
                row.put("activeTaskName", "Unknown");
                row.put("taskId",         null);
                row.put("taskCreated",    null);
            }

            return row;
        }).collect(Collectors.toList());
    }

    /**
     * Serves the BPMN XML so the frontend bpmn-js viewer can render it.
     * Fetches the currently deployed definition from the Camunda engine.
     */
    @GetMapping(value = "/bpmn", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getBpmn() {
        String xml = fluxNovaClient.getProcessDefinitionXml("trip-planning-process");
        if (xml == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }
}
