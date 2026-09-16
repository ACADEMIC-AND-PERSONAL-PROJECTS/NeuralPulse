package com.ainews.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
public class HealthController {

    @Value("${app.version:1.0.0}")
    private String version;

    private String lastRun = "never";

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "version", version,
                "lastRun", lastRun
        );
    }

    public void updateLastRun() {
        this.lastRun = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
