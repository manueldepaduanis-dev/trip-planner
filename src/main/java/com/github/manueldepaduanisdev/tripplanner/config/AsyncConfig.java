package com.github.manueldepaduanisdev.tripplanner.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    @Bean(name = "itineraryTaskExecutor")
    public Executor itineraryTaskExecutor() {
        log.info("Initializing Async ThreadPoolTaskExecutor 'itineraryTaskExecutor'...");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // So we can test pool easily
        executor.setCorePoolSize(5);

        // Handle backpressure and test pool easily
        executor.setQueueCapacity(100);

        // Backup threads
        executor.setMaxPoolSize(5);

        // Threads pool name prefix to search inside logs
        executor.setThreadNamePrefix("ItineraryWorker-");

        // Log configuration parameters for debugging purposes
        log.info("Executor configuration -> CorePoolSize: {}, MaxPoolSize: {}, QueueCapacity: {}, ThreadPrefix: '{}'",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity(),
                executor.getThreadNamePrefix());

        executor.initialize();

        log.info("Async ItineraryTaskExecutor initialized successfully.");
        return executor;
    }
}