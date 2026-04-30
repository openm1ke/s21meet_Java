package ru.izpz.edu.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "campus.scheduler")
public class CampusSchedulerProperties {

  private Duration fixedDelay = Duration.ofSeconds(30);
  private int participantsMaxConcurrency = 8;
  private Timeout timeout = new Timeout();

  @Getter
  @Setter
  public static class Timeout {
    private Duration global = Duration.ofSeconds(60);
    private Duration perCampus = Duration.ofSeconds(20);
  }
}
