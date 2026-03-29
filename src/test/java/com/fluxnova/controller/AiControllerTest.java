package com.fluxnova.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluxnova.ai.TripPlanningAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean TripPlanningAssistant assistant;

    // ── POST /api/ai/chat ────────────────────────────────────────────────────

    @Test
    void chat_validMessage_returnsResponse() throws Exception {
        when(assistant.chat(any(), any())).thenReturn("Here are your trips...");

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "What trips are available?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Here are your trips..."));
    }

    @Test
    void chat_missingMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("message is required"));

        verifyNoInteractions(assistant);
    }

    @Test
    void chat_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("message is required"));

        verifyNoInteractions(assistant);
    }
}
