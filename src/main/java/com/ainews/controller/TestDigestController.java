package com.ainews.controller;

import com.ainews.scheduler.WeeklyDigestScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestDigestController {

    private final WeeklyDigestScheduler scheduler;

    public TestDigestController(WeeklyDigestScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostMapping("/test-digest")
    public ResponseEntity<Map<String, String>> triggerDigest() {
        try {
            scheduler.executeWeeklyDigest();
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Digest executed successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
