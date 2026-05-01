package ru.izpz.web.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.izpz.dto.CampusRequest;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsRequest;
import ru.izpz.web.client.EduProfileClient;

@Service
@RequiredArgsConstructor
public class ProjectDirectoryFacade {
  private static final String PROFILE_WEB = "profileWeb";

  private final EduProfileClient eduProfileClient;

  @Retry(name = PROFILE_WEB)
  @CircuitBreaker(name = PROFILE_WEB, fallbackMethod = "fallbackProjectNames")
  public List<String> getProjectNames(String telegramId) {
    return eduProfileClient.getProjectNames(new CampusRequest(telegramId));
  }

  @Retry(name = PROFILE_WEB)
  @CircuitBreaker(name = PROFILE_WEB, fallbackMethod = "fallbackAllProjectNames")
  public List<String> getAllProjectNames() {
    return eduProfileClient.getAllProjectNames();
  }

  @Retry(name = PROFILE_WEB)
  @CircuitBreaker(name = PROFILE_WEB, fallbackMethod = "fallbackProjectExecutors")
  public List<ProjectExecutorDto> getProjectExecutors(ProjectExecutorsRequest request) {
    return eduProfileClient.getProjectExecutors(request);
  }

  @SuppressWarnings("unused")
  private List<String> fallbackProjectNames(String telegramId, Throwable throwable) {
    return Collections.emptyList();
  }

  @SuppressWarnings("unused")
  private List<String> fallbackAllProjectNames(Throwable throwable) {
    return Collections.emptyList();
  }

  @SuppressWarnings("unused")
  private List<ProjectExecutorDto> fallbackProjectExecutors(
      ProjectExecutorsRequest request, Throwable throwable) {
    return Collections.emptyList();
  }
}
