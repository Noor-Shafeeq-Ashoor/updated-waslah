package com.ga.waslah.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;


@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates a dedicated thread pool for notification processing.
     *
     * @return the configured executor for asynchronous notifications
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Keep two threads ready for common background tasks.
        executor.setCorePoolSize(2);

        // Allow the pool to grow when several tasks arrive together.
        executor.setMaxPoolSize(4);

        // Queue up to 100 tasks while all threads are busy.
        executor.setQueueCapacity(100);

        // Make background threads easy to identify in application logs.
        executor.setThreadNamePrefix("waslah-notification-");

        // Allow accepted tasks to finish during a graceful shutdown.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);

        return executor;
    }
}