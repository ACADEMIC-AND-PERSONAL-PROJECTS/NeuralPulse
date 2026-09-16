package com.ainews.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.ZoneId;

@Configuration
public class AppConfig {

    @Value("${app.news.days-lookback:7}")
    private int daysLookback;

    @Value("${app.news.archive-dir:./news}")
    private String archiveDir;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .build();
    }

    @Bean
    public ZoneId appTimezone() {
        return ZoneId.of("Africa/Dakar");
    }

    public int getDaysLookback() {
        return daysLookback;
    }

    public String getArchiveDir() {
        return archiveDir;
    }
}
