package ru.izpz.edu.service.provider;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.edu.dto.StudentProjectData;
import ru.izpz.edu.exception.PlatformClientException;
import ru.izpz.edu.exception.PlatformRateLimitException;
import ru.izpz.edu.exception.PlatformTransientException;
import ru.izpz.edu.exception.PlatformTransportException;
import ru.izpz.edu.model.StudentCredentials;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "profile.service.enabled", havingValue = "true")
public class CampusRoutingProjectsProvider {

  private final ObjectProvider<RestApiProjectsProvider> restProvider;

  public List<StudentProjectData> getStudentProjectsByLogin(String login) {
    ProjectsProvider provider = resolveProviderForLogin();
    if (provider == null) {
      log.warn("Projects provider недоступен для login={}", login);
      return List.of();
    }
    return provider.getStudentProjectsByLogin(login);
  }

  public RefreshResult refreshStudentProjects(StudentCredentials credentials) {
    return refreshStudentProjects(credentials.getLogin());
  }

  public RefreshResult refreshStudentProjects(String login) {
    ProjectsProvider provider = resolveProvider();
    if (provider == null) {
      return RefreshResult.SKIPPED_NO_PROVIDER;
    }
    try {
      provider.refreshStudentProjectsByLogin(login);
      return RefreshResult.SUCCESS;
    } catch (PlatformRateLimitException
        | PlatformTransientException
        | PlatformTransportException e) {
      log.warn(
          "Временная ошибка обновления проектов для {}: type={}, status={}",
          login,
          e.getClass().getSimpleName(),
          e.getCode());
      return RefreshResult.RETRYABLE_FAILURE;
    } catch (PlatformClientException e) {
      log.warn(
          "Постоянная ошибка обновления проектов для {}: type={}, status={}",
          login,
          e.getClass().getSimpleName(),
          e.getCode());
      return RefreshResult.FAILED;
    } catch (RuntimeException e) {
      log.warn(
          "Непредвиденная ошибка обновления проектов для {}: type={}",
          login,
          e.getClass().getSimpleName());
      return RefreshResult.FAILED;
    }
  }

  public String providerTypeForSchoolId() {
    return providerType(resolveProvider());
  }

  private ProjectsProvider resolveProviderForLogin() {
    return resolveProvider();
  }

  private ProjectsProvider resolveProvider() {
    return restProvider.getIfAvailable();
  }

  private String providerType(ProjectsProvider provider) {
    if (provider == null) {
      return "none";
    }
    if (provider instanceof RestApiProjectsProvider) {
      return "rest";
    }
    return provider.getClass().getSimpleName();
  }

  public enum RefreshResult {
    SUCCESS,
    SKIPPED_NO_PROVIDER,
    RETRYABLE_FAILURE,
    FAILED
  }
}
