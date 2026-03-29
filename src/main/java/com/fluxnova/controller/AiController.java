package com.fluxnova.controller;

import com.fluxnova.ai.TripPlanningAssistant;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final TripPlanningAssistant assistant;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        String userMessage = body.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message is required"));
        }

        // Use tripId as the conversation key when provided, otherwise fall back to the HTTP session
        String tripId = body.get("tripId");
        String conversationId = (tripId != null && !tripId.isBlank())
                ? "trip-" + tripId
                : session.getId();

        String response = assistant.chat(conversationId, userMessage);
        return ResponseEntity.ok(Map.of("response", response));
    }
}
