package ru.izpz.edu.service.provider;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.dto.model.ParticipantProjectsV1DTO;
import ru.izpz.edu.client.PlatformApiFacade;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"profile.service.enabled", "api.participant.enabled"},
    havingValue = "true")
public class RestProjectsApiFacade {

  private final PlatformApiFacade platformApi;

  @RateLimiter(name = "projectsRest")
  public ParticipantProjectsV1DTO getParticipantProjectsByLogin(
      String login, long pageSize, String status) {
    return platformApi.getParticipantProjectsByLogin(login, pageSize, 0L, status);
  }
}
