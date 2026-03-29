package com.fluxnova.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class FluxNovaClientConfig {

    @Value("${fluxnova.api.base-url}")
    private String baseUrl;

    @Value("${fluxnova.api.connect-timeout-ms}")
    private int connectTimeoutMs;

    @Value("${fluxnova.api.read-timeout-ms}")
    private int readTimeoutMs;

    @Bean
    public WebClient fluxNovaWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
