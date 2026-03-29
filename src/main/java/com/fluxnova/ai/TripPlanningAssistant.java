package com.fluxnova.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface TripPlanningAssistant {

    @SystemMessage("""
            You are a family trip planning assistant integrated with a workflow orchestration system.
            You help families decide on travel destinations based on the season, available days,
            and personal preferences. You can suggest destinations, explain workflow steps,
            and help interpret the current state of a trip planning workflow.
            Keep your responses concise and family-friendly.
            """)
    String chat(String userMessage);
}
