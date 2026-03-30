package com.fluxnova.controller;

import com.fluxnova.ai.TripPlanningAssistant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final TripPlanningAssistant assistant;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> body) {

        String userMessage = body.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message is required"));
        }

        // Prefer the client-supplied conversationId (stable UUID from localStorage or "trip-{id}").
        // Fall back to a random ID only as a last resort — this keeps memory stable across requests.
        String conversationId = body.get("conversationId");
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        String response = assistant.chat(conversationId, userMessage);
        return ResponseEntity.ok(Map.of("response", response));
    }
}
