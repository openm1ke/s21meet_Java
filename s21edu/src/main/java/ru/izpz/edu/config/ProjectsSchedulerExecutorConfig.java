package ru.izpz.edu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ProjectsSchedulerExecutorConfig {

    @Bean(name = "projectsSchedulerExecutor", destroyMethod = "shutdown")
    public ExecutorService projectsSchedulerExecutor(
        @Value("${projects.scheduler.concurrency:8}") int concurrency
    ) {
        int poolSize = Math.max(1, concurrency);
        return Executors.newFixedThreadPool(poolSize, Thread.ofPlatform().name("projects-scheduler-", 0).factory());
    }
}
