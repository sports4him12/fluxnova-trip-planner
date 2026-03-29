package com.fluxnova.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluxnova.client.dto.ProcessInstanceResponse;
import com.fluxnova.client.dto.TaskResponse;
import com.fluxnova.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkflowController.class)
class WorkflowControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean WorkflowService workflowService;

    // ── POST /api/trips/{tripId}/workflow/start ──────────────────────────────

    @Test
    void startWorkflow_returnsOkWithInstance() throws Exception {
        ProcessInstanceResponse instance = new ProcessInstanceResponse();
        instance.setId("inst-abc");
        when(workflowService.startTripWorkflow(1L)).thenReturn(instance);

        mockMvc.perform(post("/api/trips/1/workflow/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("inst-abc"));
    }

    // ── GET /api/trips/{tripId}/workflow/status ──────────────────────────────

    @Test
    void getStatus_returnsOkWithInstance() throws Exception {
        ProcessInstanceResponse instance = new ProcessInstanceResponse();
        instance.setId("inst-abc");
        instance.setProcessDefinitionId("trip-planning-process:1:xyz");
        when(workflowService.getWorkflowStatus(1L)).thenReturn(instance);

        mockMvc.perform(get("/api/trips/1/workflow/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("inst-abc"));
    }

    // ── GET /api/trips/{tripId}/workflow/tasks ───────────────────────────────

    @Test
    void getTasks_returnsList() throws Exception {
        TaskResponse task = new TaskResponse();
        task.setId("task-1");
        task.setName("Review Destination");
        when(workflowService.getTasksForTrip(1L)).thenReturn(List.of(task));

        mockMvc.perform(get("/api/trips/1/workflow/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("task-1"))
                .andExpect(jsonPath("$[0].name").value("Review Destination"));
    }

    @Test
    void getTasks_emptyList_returnsOk() throws Exception {
        when(workflowService.getTasksForTrip(2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/trips/2/workflow/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── POST /api/trips/{tripId}/workflow/tasks/{taskId}/complete ────────────

    @Test
    void completeTask_withVariables_returns204() throws Exception {
        Map<String, Object> vars = Map.of("approved", true);

        mockMvc.perform(post("/api/trips/1/workflow/tasks/task-1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vars)))
                .andExpect(status().isNoContent());

        verify(workflowService).completeTask(eq(1L), eq("task-1"), anyMap());
    }

    @Test
    void completeTask_noBody_returns204WithEmptyMap() throws Exception {
        mockMvc.perform(post("/api/trips/1/workflow/tasks/task-1/complete"))
                .andExpect(status().isNoContent());

        verify(workflowService).completeTask(eq(1L), eq("task-1"), eq(Map.of()));
    }

    // ── DELETE /api/trips/{tripId}/workflow ──────────────────────────────────

    @Test
    void cancelWorkflow_returns204() throws Exception {
        mockMvc.perform(delete("/api/trips/1/workflow"))
                .andExpect(status().isNoContent());

        verify(workflowService).cancelTripWorkflow(1L);
    }
}
