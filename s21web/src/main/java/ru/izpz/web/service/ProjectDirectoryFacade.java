package ru.izpz.web.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.izpz.dto.CampusRequest;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.dto.ProjectExecutorsPageDto;
import ru.izpz.dto.ProjectExecutorsPageRequest;
import ru.izpz.dto.ProjectExecutorsRequest;
import ru.izpz.web.client.EduProfileClient;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused")
public class ProjectDirectoryFacade {
  private static final String PROFILE_WEB = "profileWeb";

  private final EduProfileClient eduProfileClient;

  @Retry(name = PROFILE_WEB)
  @CircuitBreaker(name = PROFILE_WEB, fallbackMethod = "fallbackProjectNames")
  public List<String> getProjectNames(String telegramId) {
    long startedAt = System.nanoTime();
    log.debug(
        "Requesting scoped project names from profile service: telegramIdSuffix={}",
        suffix(telegramId));
    try {
      List<String> projectNames = eduProfileClient.getProjectNames(new CampusRequest(telegramId));
      log.info(
          "Profile service returned scoped project names: telegramIdSuffix={}, count={}, "
              + "elapsedMs={}",
          suffix(telegramId),
          projectNames.size(),
          elapsedMillis(startedAt));
      return projectNames;
    } catch (RuntimeException exception) {
      log.warn(
          "Profile service scoped project names request failed: telegramIdSuffix={}, "
              + "elapsedMs={}, error={}",
          suffix(telegramId),
          elapsedMillis(startedAt),
          exception.toString());
      throw exception;
    }
  }

  @Retry(name = PROFILE_WEB)
  @CircuitBreaker(name = PROFILE_WEB, fallbackMethod = "fallbackAllProjectNames")
  public List<String> getAllProjectNames() {
    long startedAt = System.nanoTime();
    log.debug("Requesting all project names from profile service");
    try {
      List<String> projectNames = eduProfileClient.getAllProjectNames();
      log.info(
          "Profile service returned all project names: count={}, elapsedMs={}",
          projectNames.size(),
          elapsedMillis(startedAt));
      return projectNames;
    } catch (RuntimeException exception) {
      log.warn(
          "Profile service all project names request failed: elapsedMs={}, error={}",
          elapsedMillis(startedAt),
          exception.toString());
      throw exception;
    }
  }

  @Retry(name = PROFILE_WEB)
  @CircuitBreaker(name = PROFILE_WEB, fallbackMethod = "fallbackProjectExecutors")
  public List<ProjectExecutorDto> getProjectExecutors(ProjectExecutorsRequest request) {
    return eduProfileClient.getProjectExecutors(request);
  }

  @Retry(name = PROFILE_WEB)
  @CircuitBreaker(name = PROFILE_WEB, fallbackMethod = "fallbackProjectExecutorsPage")
  public ProjectExecutorsPageDto getProjectExecutorsPage(ProjectExecutorsPageRequest request) {
    long startedAt = System.nanoTime();
    try {
      ProjectExecutorsPageDto page = eduProfileClient.getProjectExecutorsPage(request);
      log.info(
          "Profile service returned project executor page: projectName={}, page={}, size={}, "
              + "items={}, totalItems={}, elapsedMs={}",
          request.projectName(),
          request.page(),
          request.size(),
          page.items().size(),
          page.totalItems(),
          elapsedMillis(startedAt));
      return page;
    } catch (RuntimeException exception) {
      log.warn(
          "Profile service project executor page request failed: projectName={}, page={}, "
              + "size={}, elapsedMs={}, error={}",
          request == null ? null : request.projectName(),
          request == null ? null : request.page(),
          request == null ? null : request.size(),
          elapsedMillis(startedAt),
          exception.toString());
      throw exception;
    }
  }

  private List<String> fallbackProjectNames(String telegramId, Throwable throwable) {
    log.error(
        "Returning empty scoped project names fallback: telegramIdSuffix={}, cause={}",
        suffix(telegramId),
        throwable.toString(),
        throwable);
    return Collections.emptyList();
  }

  private List<String> fallbackAllProjectNames(Throwable throwable) {
    log.error(
        "Returning empty all project names fallback: cause={}", throwable.toString(), throwable);
    return Collections.emptyList();
  }

  private List<ProjectExecutorDto> fallbackProjectExecutors(
      ProjectExecutorsRequest request, Throwable throwable) {
    log.error(
        "Returning empty project executors fallback: projectName={}, cause={}",
        request == null ? null : request.projectName(),
        throwable.toString(),
        throwable);
    return Collections.emptyList();
  }

  private ProjectExecutorsPageDto fallbackProjectExecutorsPage(
      ProjectExecutorsPageRequest request, Throwable throwable) {
    log.error(
        "Returning empty project executors page fallback: projectName={}, page={}, cause={}",
        request == null ? null : request.projectName(),
        request == null ? null : request.page(),
        throwable.toString(),
        throwable);
    int page = request == null ? 0 : request.page();
    int size = request == null ? 20 : request.size();
    return new ProjectExecutorsPageDto(List.of(), page, size, 0, 0, List.of(), List.of());
  }

  private static long elapsedMillis(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000;
  }

  private static String suffix(String value) {
    if (value == null || value.isBlank()) {
      return "-";
    }
    return value.length() <= 4 ? value : "..." + value.substring(value.length() - 4);
  }
}
