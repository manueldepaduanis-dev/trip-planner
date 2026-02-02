package com.github.manueldepaduanisdev.tripplanner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "itineraryTaskExecutor")
    public Executor itineraryTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // So we can test pool easily
        executor.setCorePoolSize(5);

        // Handle backpressure and test pool easily
        executor.setQueueCapacity(100);

        // Backup threads
        executor.setMaxPoolSize(5);

        // Threads pool name prefix to search inside logs
        executor.setThreadNamePrefix("ItineraryWorker-");

        executor.initialize();
        return executor;
    }
}
