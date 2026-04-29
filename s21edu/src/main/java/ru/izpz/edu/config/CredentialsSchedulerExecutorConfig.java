package ru.izpz.edu.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CredentialsSchedulerExecutorConfig {

  @Bean(name = "credentialsSchedulerExecutor", destroyMethod = "shutdown")
  public ExecutorService credentialsSchedulerExecutor(
      @Value("${credentials.scheduler.concurrency:12}") int concurrency) {
    int poolSize = Math.max(1, concurrency);
    return Executors.newFixedThreadPool(
        poolSize, Thread.ofPlatform().name("credentials-scheduler-", 0).factory());
  }
}
