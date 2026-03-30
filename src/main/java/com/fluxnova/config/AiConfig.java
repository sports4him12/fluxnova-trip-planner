package com.fluxnova.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * Provides per-conversation chat memory keyed by the @MemoryId value.
     *
     * AiServicesAutoConfig looks for ChatMemoryProvider beans (not ChatMemoryStore).
     * A single InMemoryChatMemoryStore is shared across all conversations so that
     * message history is preserved between requests for the same conversation ID.
     * maxMessages=20 gives plenty of room for a full trip-planning dialogue.
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        InMemoryChatMemoryStore store = new InMemoryChatMemoryStore();
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(store)
                .build();
    }
}
