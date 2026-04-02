package com.fluxnova.controller;

import com.fluxnova.client.FluxNovaClient;
import com.fluxnova.client.dto.TaskResponse;
import com.fluxnova.model.Season;
import com.fluxnova.model.Trip;
import com.fluxnova.model.TripStatus;
import com.fluxnova.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FluxNovaClient fluxNovaClient;
    @MockitoBean TripRepository tripRepository;

    // ── GET /api/admin/processes ─────────────────────────────────────────────

    @Test
    void getActiveProcesses_withActiveTask_returnsEnrichedRow() throws Exception {
        Trip trip = trip(1L, "Beach Vacation", "inst-123");
        when(tripRepository.findAll()).thenReturn(List.of(trip));

        TaskResponse task = new TaskResponse();
        task.setId("task-001");
        task.setName("Review & Confirm Details");
        task.setTaskDefinitionKey("review-and-confirm");
        task.setCreated("2026-03-29T10:00:00.000+0000");
        when(fluxNovaClient.getTasksForInstance("inst-123")).thenReturn(List.of(task));

        mockMvc.perform(get("/api/admin/processes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tripId").value(1))
                .andExpect(jsonPath("$[0].tripTitle").value("Beach Vacation"))
                .andExpect(jsonPath("$[0].instanceId").value("inst-123"))
                .andExpect(jsonPath("$[0].activeTaskKey").value("review-and-confirm"))
                .andExpect(jsonPath("$[0].taskId").value("task-001"));
    }

    @Test
    void getActiveProcesses_noTasks_returnsCompletedLabel() throws Exception {
        Trip trip = trip(2L, "Mountain Trek", "inst-456");
        when(tripRepository.findAll()).thenReturn(List.of(trip));
        when(fluxNovaClient.getTasksForInstance("inst-456")).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/processes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activeTaskName").value("Completed"))
                .andExpect(jsonPath("$[0].taskId").isEmpty());
    }

    @Test
    void getActiveProcesses_clientThrows_returnsUnknown() throws Exception {
        Trip trip = trip(3L, "City Tour", "inst-789");
        when(tripRepository.findAll()).thenReturn(List.of(trip));
        when(fluxNovaClient.getTasksForInstance("inst-789"))
                .thenThrow(new RuntimeException("Engine unreachable"));

        mockMvc.perform(get("/api/admin/processes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activeTaskName").value("Unknown"));
    }

    @Test
    void getActiveProcesses_tripWithoutWorkflow_excluded() throws Exception {
        Trip noWorkflow = trip(4L, "Draft Trip", null);
        when(tripRepository.findAll()).thenReturn(List.of(noWorkflow));

        mockMvc.perform(get("/api/admin/processes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/admin/bpmn ──────────────────────────────────────────────────

    @Test
    void getBpmn_returnsBpmnXml() throws Exception {
        String bpmnXml = "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"/>";
        when(fluxNovaClient.getProcessDefinitionXml("trip-planning-process")).thenReturn(bpmnXml);

        mockMvc.perform(get("/api/admin/bpmn"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(content().string(bpmnXml));
    }

    @Test
    void getBpmn_engineReturnsNull_returns404() throws Exception {
        when(fluxNovaClient.getProcessDefinitionXml("trip-planning-process")).thenReturn(null);

        mockMvc.perform(get("/api/admin/bpmn"))
                .andExpect(status().isNotFound());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Trip trip(Long id, String title, String instanceId) {
        Trip t = new Trip();
        t.setId(id);
        t.setTitle(title);
        t.setStatus(TripStatus.PLANNING);
        t.setSeason(Season.SUMMER);
        t.setWorkflowInstanceId(instanceId);
        return t;
    }
}
