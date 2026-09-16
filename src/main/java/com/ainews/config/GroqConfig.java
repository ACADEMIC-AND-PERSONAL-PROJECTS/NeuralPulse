package com.ainews.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GroqConfig {

    @Value("${app.groq.api-url:https://api.groq.com/openai/v1}")
    private String apiUrl;

    @Value("${app.groq.model:qwen/qwen3.8-27b}")
    private String model;

    @Bean("groqRestClient")
    public RestClient groqRestClient() {
        String apiKey = System.getenv("GROQ_API");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("GROQ_API_KEY");
        }
        return RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String getModel() {
        return model;
    }
}
