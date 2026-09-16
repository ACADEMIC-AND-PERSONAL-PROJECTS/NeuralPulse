package com.ainews;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiNewsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiNewsApplication.class, args);
    }
}
