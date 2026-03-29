package com.fluxnova.controller;

import com.fluxnova.ai.TripPlanningAssistant;
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
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String userMessage = body.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message is required"));
        }
        String response = assistant.chat(userMessage);
        return ResponseEntity.ok(Map.of("response", response));
    }
}
