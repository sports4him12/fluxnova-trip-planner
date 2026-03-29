package com.fluxnova.controller;

import com.fluxnova.client.dto.ProcessInstanceResponse;
import com.fluxnova.client.dto.TaskResponse;
import com.fluxnova.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips/{tripId}/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    /** Start the planning workflow for a trip. */
    @PostMapping("/start")
    public ResponseEntity<ProcessInstanceResponse> startWorkflow(@PathVariable Long tripId) {
        return ResponseEntity.ok(workflowService.startTripWorkflow(tripId));
    }

    /** Get current workflow status from FluxNova. */
    @GetMapping("/status")
    public ResponseEntity<ProcessInstanceResponse> getStatus(@PathVariable Long tripId) {
        return ResponseEntity.ok(workflowService.getWorkflowStatus(tripId));
    }

    /** List active tasks waiting for user action. */
    @GetMapping("/tasks")
    public ResponseEntity<List<TaskResponse>> getTasks(@PathVariable Long tripId) {
        return ResponseEntity.ok(workflowService.getTasksForTrip(tripId));
    }

    /** Complete a specific task and advance the workflow. */
    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Void> completeTask(
            @PathVariable Long tripId,
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> variables) {
        workflowService.completeTask(tripId, taskId, variables != null ? variables : Map.of());
        return ResponseEntity.noContent().build();
    }

    /** Cancel the workflow and mark the trip as cancelled. */
    @DeleteMapping
    public ResponseEntity<Void> cancelWorkflow(@PathVariable Long tripId) {
        workflowService.cancelTripWorkflow(tripId);
        return ResponseEntity.noContent().build();
    }
}
