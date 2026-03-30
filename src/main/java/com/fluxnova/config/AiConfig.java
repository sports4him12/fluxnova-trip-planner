package com.fluxnova.config;

import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * Shared store keyed by conversation ID (e.g. "trip-1").
     * LangChain4J's @AiService wires this in automatically when @MemoryId is declared
     * on the service method, giving each trip its own persistent message history.
     */
    @Bean
    public InMemoryChatMemoryStore chatMemoryStore() {
        return new InMemoryChatMemoryStore();
    }
}
