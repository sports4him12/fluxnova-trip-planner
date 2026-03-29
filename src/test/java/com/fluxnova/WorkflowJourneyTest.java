package com.fluxnova;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluxnova.ai.TripPlanningAssistant;
import com.fluxnova.client.FluxNovaClient;
import com.fluxnova.client.dto.ProcessInstanceResponse;
import com.fluxnova.client.dto.TaskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end journey test for the trip planning workflow.
 *
 * Scenario: West Coast family trip from Richmond VA
 *   - 5 people, initially proposed as 7 days
 *   - Workflow starts, details gathered via AI chat
 *   - User rejects at review step: duration should be 6 days, not 7
 *   - Workflow loops back to review-and-confirm
 *   - User approves on second attempt (6 days confirmed)
 *   - Workflow advances past user tasks (no more tasks remain)
 *
 * External dependencies (FluxNovaClient, TripPlanningAssistant) are mocked;
 * the real controller → service → JPA → H2 chain is exercised throughout.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowJourneyTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean FluxNovaClient fluxNovaClient;
    @MockitoBean TripPlanningAssistant tripPlanningAssistant;

    // Shared workflow instance returned by the mocked engine
    private static final String INSTANCE_ID = "inst-west-coast-journey";

    @BeforeEach
    void setUpMocks() {
        // ── AI assistant responses (chained per call order) ──────────────────
        when(tripPlanningAssistant.chat(any(), any()))
                // 1. User describes the trip
                .thenReturn("Got it! West Coast from Richmond VA, 5 people, 7 days in summer. Any special preferences?")
                // 2. User says no preferences → assistant confirms all details
                .thenReturn("Perfect! Here's what I have:\n- Destination: West Coast\n- Season: SUMMER\n- Duration: 7 days\n- Group: 5 people\nDoes everything look right?")
                // 3. User says duration is wrong — should be 6 days
                .thenReturn("Got it! I've updated the duration to 6 days. So now:\n- Destination: West Coast\n- Season: SUMMER\n- Duration: 6 days\n- Group: 5 people\nShall I confirm this?")
                // 4. User approves
                .thenReturn("Wonderful! All details confirmed. Click 'Confirm & Advance' in the workflow panel to proceed with booking.");

        // ── Workflow instance (active throughout user tasks) ─────────────────
        ProcessInstanceResponse activeInstance = new ProcessInstanceResponse();
        activeInstance.setId(INSTANCE_ID);
        activeInstance.setProcessDefinitionId("trip-planning-process:1:abc");
        // ended = false by default → completeTask will NOT set status to BOOKED

        when(fluxNovaClient.startProcess(eq("trip-planning-process"), anyMap()))
                .thenReturn(activeInstance);

        when(fluxNovaClient.getProcessInstance(INSTANCE_ID))
                .thenReturn(activeInstance);

        // ── Tasks returned in sequence as the workflow advances ───────────────
        // Call 1 (after start):            gather-trip-details
        // Call 2 (after gather complete):  review-and-confirm  [task-review-1]
        // Call 3 (after first rejection):  review-and-confirm  [task-review-2 — looped back]
        // Call 4 (after approval):         empty               [workflow past user tasks]
        when(fluxNovaClient.getTasksForInstance(INSTANCE_ID))
                .thenReturn(List.of(task("task-gather-1", "Gather Trip Details via AI Chat", "gather-trip-details")))
                .thenReturn(List.of(task("task-review-1", "Review & Confirm Trip Details",   "review-and-confirm")))
                .thenReturn(List.of(task("task-review-2", "Review & Confirm Trip Details",   "review-and-confirm")))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("West Coast family trip: gather → reject (6 days, not 7) → approve → workflow advances")
    void westCoastTrip_withOneRejection_completesWorkflow() throws Exception {

        // ══ 1. Create the trip ════════════════════════════════════════════════
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title",  "West Coast Family Trip",
                "season", "SUMMER",
                "status", "DRAFT",
                "notes",  "West Coast from Richmond VA, 5 people, initially 7 days"
        ));

        String tripJson = mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("West Coast Family Trip"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        long tripId = objectMapper.readTree(tripJson).get("id").asLong();

        // ══ 2. Chat: user describes the trip ══════════════════════════════════
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tripId",  String.valueOf(tripId),
                                "message", "We want to go to the West Coast from Richmond VA " +
                                           "with 5 people for 7 days this summer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value(containsString("West Coast")));

        // ══ 3. Chat: no special preferences → assistant summarises ════════════
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tripId",  String.valueOf(tripId),
                                "message", "No special preferences"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value(containsString("7 days")));

        // ══ 4. Start the planning workflow ════════════════════════════════════
        mockMvc.perform(post("/api/trips/{id}/workflow/start", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INSTANCE_ID));

        // Trip must now be PLANNING with the instance bound
        mockMvc.perform(get("/api/trips/{id}", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANNING"))
                .andExpect(jsonPath("$.workflowInstanceId").value(INSTANCE_ID));

        // ══ 5. Verify first task: gather-trip-details ════════════════════════
        mockMvc.perform(get("/api/trips/{id}/workflow/tasks", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].taskDefinitionKey").value("gather-trip-details"))
                .andExpect(jsonPath("$[0].id").value("task-gather-1"));

        // ══ 6. Complete gather-trip-details ═══════════════════════════════════
        mockMvc.perform(post("/api/trips/{id}/workflow/tasks/{taskId}/complete", tripId, "task-gather-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());

        // ══ 7. Verify next task: review-and-confirm ═══════════════════════════
        mockMvc.perform(get("/api/trips/{id}/workflow/tasks", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskDefinitionKey").value("review-and-confirm"))
                .andExpect(jsonPath("$[0].id").value("task-review-1"));

        // ══ 8. Chat: user notices the duration is wrong (7 days → 6 days) ════
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tripId",  String.valueOf(tripId),
                                "message", "Actually it should be 6 days, not 7"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value(containsString("6 days")));

        // ══ 9. REJECT — complete with confirmed=false ════════════════════════
        mockMvc.perform(post("/api/trips/{id}/workflow/tasks/{taskId}/complete", tripId, "task-review-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("confirmed", false))))
                .andExpect(status().isNoContent());

        // ══ 10. Verify review-and-confirm loops back with a new task ══════════
        mockMvc.perform(get("/api/trips/{id}/workflow/tasks", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskDefinitionKey").value("review-and-confirm"))
                // Different task ID confirms the engine created a new task instance
                .andExpect(jsonPath("$[0].id").value("task-review-2"));

        // ══ 11. Chat: user confirms 6 days is correct ════════════════════════
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tripId",  String.valueOf(tripId),
                                "message", "Yes, 6 days looks correct — let's go ahead"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value(containsString("Confirm")));

        // ══ 12. APPROVE — complete with confirmed=true ════════════════════════
        mockMvc.perform(post("/api/trips/{id}/workflow/tasks/{taskId}/complete", tripId, "task-review-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("confirmed", true))))
                .andExpect(status().isNoContent());

        // ══ 13. No more user tasks — workflow advanced past approval gate ══════
        mockMvc.perform(get("/api/trips/{id}/workflow/tasks", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        // ══ 14. Verify the full sequence of engine interactions ═══════════════
        verify(fluxNovaClient).startProcess(eq("trip-planning-process"), anyMap());
        verify(fluxNovaClient).completeTask(eq("task-gather-1"),  anyMap());
        verify(fluxNovaClient).completeTask(eq("task-review-1"),  anyMap()); // rejection
        verify(fluxNovaClient).completeTask(eq("task-review-2"),  anyMap()); // approval
        verify(fluxNovaClient, times(3)).completeTask(any(), anyMap());       // exactly 3 task completions
        verify(tripPlanningAssistant, times(4)).chat(any(), any());           // 4 chat exchanges
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private TaskResponse task(String id, String name, String taskDefinitionKey) {
        TaskResponse t = new TaskResponse();
        t.setId(id);
        t.setName(name);
        t.setTaskDefinitionKey(taskDefinitionKey);
        return t;
    }

    private static org.hamcrest.Matcher<String> containsString(String s) {
        return org.hamcrest.Matchers.containsString(s);
    }
}
