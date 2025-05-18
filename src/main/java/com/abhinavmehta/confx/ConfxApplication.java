package com.abhinavmehta.confx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync // Enable asynchronous event processing
@EnableScheduling // Added for @Scheduled tasks like heartbeats
public class ConfxApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfxApplication.class, args);
    }

} 