package com.chatterai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class ClaudeApiConfig {

    private static final String ANTHROPIC_BASE_URL = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final AiProperties aiProperties;

    @Bean
    public RestClient claudeRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = aiProperties.getTimeoutSeconds() * 1000;
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(ANTHROPIC_BASE_URL)
                .defaultHeader("x-api-key", aiProperties.getClaude().getApiKey())
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
