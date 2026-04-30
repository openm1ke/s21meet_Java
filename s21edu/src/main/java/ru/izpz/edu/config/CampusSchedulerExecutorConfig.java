package ru.izpz.edu.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CampusSchedulerExecutorConfig {

  @Bean(name = "campusSchedulerExecutor", destroyMethod = "shutdown")
  public ExecutorService campusSchedulerExecutor(CampusSchedulerProperties properties) {
    int poolSize = Math.max(1, properties.getParticipantsMaxConcurrency());
    return Executors.newFixedThreadPool(
        poolSize, Thread.ofPlatform().name("campus-scheduler-", 0).factory());
  }
}
