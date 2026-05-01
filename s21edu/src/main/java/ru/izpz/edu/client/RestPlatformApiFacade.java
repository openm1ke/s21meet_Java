package ru.izpz.edu.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.izpz.dto.api.CampusApi;
import ru.izpz.dto.api.ClusterApi;
import ru.izpz.dto.api.CoalitionApi;
import ru.izpz.dto.api.EventApi;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.dto.model.ClusterMapV1DTO;
import ru.izpz.dto.model.ClustersV1DTO;
import ru.izpz.dto.model.EventsV1DTO;
import ru.izpz.dto.model.ParticipantCoalitionV1DTO;
import ru.izpz.dto.model.ParticipantLoginsV1DTO;
import ru.izpz.dto.model.ParticipantProjectsV1DTO;
import ru.izpz.dto.model.ParticipantV1DTO;
import ru.izpz.edu.exception.PlatformClientException;
import ru.izpz.edu.exception.PlatformNotFoundException;
import ru.izpz.edu.exception.PlatformRateLimitException;
import ru.izpz.edu.exception.PlatformTransientException;
import ru.izpz.edu.exception.PlatformUnauthorizedException;

/** Реализация фасада доступа к REST API платформы через сгенерированные OpenAPI-клиенты. */
public class RestPlatformApiFacade implements PlatformApiFacade {
  private static final String EXTERNAL_GLOBAL = "externalGlobal";
  private static final String PARTICIPANT_PROFILE = "participantProfile";
  private static final String REQUEST_FAILED_MESSAGE = "Platform request failed";
  private static final int HTTP_UNAUTHORIZED = 401;
  private static final int HTTP_FORBIDDEN = 403;
  private static final int HTTP_NOT_FOUND = 404;
  private static final int HTTP_RATE_LIMIT = 429;
  private static final int HTTP_SERVER_ERROR = 500;

  private final CampusApi campusApi;
  private final ClusterApi clusterApi;
  private final ParticipantApi participantApi;
  private final CoalitionApi coalitionApi;
  private final EventApi eventApi;

  public RestPlatformApiFacade(
      CampusApi campusApi,
      ClusterApi clusterApi,
      ParticipantApi participantApi,
      CoalitionApi coalitionApi,
      EventApi eventApi) {
    this.campusApi = campusApi;
    this.clusterApi = clusterApi;
    this.participantApi = participantApi;
    this.coalitionApi = coalitionApi;
    this.eventApi = eventApi;
  }

  @Override
  @RateLimiter(name = EXTERNAL_GLOBAL)
  @Retry(name = EXTERNAL_GLOBAL)
  public ClustersV1DTO getClustersByCampus(UUID campusId) {
    try {
      return campusApi.getClustersByCampus(campusId);
    } catch (RestClientResponseException ex) {
      throw toPlatformException(ex);
    } catch (RestClientException ex) {
      throw new PlatformClientException(REQUEST_FAILED_MESSAGE, ex);
    }
  }

  @Override
  @RateLimiter(name = EXTERNAL_GLOBAL)
  @Retry(name = EXTERNAL_GLOBAL)
  public ClusterMapV1DTO getParticipantsByClusterId(
      Long clusterId, Integer limit, Integer offset, Boolean occupied) {
    try {
      return clusterApi.getParticipantsByCoalitionId1(clusterId, limit, offset, occupied);
    } catch (RestClientResponseException ex) {
      throw toPlatformException(ex);
    } catch (RestClientException ex) {
      throw new PlatformClientException(REQUEST_FAILED_MESSAGE, ex);
    }
  }

  @Override
  @RateLimiter(name = EXTERNAL_GLOBAL)
  @Retry(name = EXTERNAL_GLOBAL)
  public ParticipantLoginsV1DTO getParticipantsByCampusId(UUID campusId, long limit, long offset) {
    try {
      return campusApi.getParticipantsByCampusId(campusId, limit, offset);
    } catch (RestClientResponseException ex) {
      throw toPlatformException(ex);
    } catch (RestClientException ex) {
      throw new PlatformClientException(REQUEST_FAILED_MESSAGE, ex);
    }
  }

  @Override
  @RateLimiter(name = EXTERNAL_GLOBAL)
  @Retry(name = EXTERNAL_GLOBAL)
  public EventsV1DTO getEvents(
      OffsetDateTime from, OffsetDateTime to, String type, Long limit, Long offset) {
    try {
      return eventApi.getEvents(from, to, type, limit, offset);
    } catch (RestClientResponseException ex) {
      throw toPlatformException(ex);
    } catch (RestClientException ex) {
      throw new PlatformClientException(REQUEST_FAILED_MESSAGE, ex);
    }
  }

  @Override
  @RateLimiter(name = EXTERNAL_GLOBAL)
  @Retry(name = EXTERNAL_GLOBAL)
  public ParticipantProjectsV1DTO getParticipantProjectsByLogin(
      String login, long limit, long offset, String status) {
    try {
      return participantApi.getParticipantProjectsByLogin(login, limit, offset, status);
    } catch (RestClientResponseException ex) {
      throw toPlatformException(ex);
    } catch (RestClientException ex) {
      throw new PlatformClientException(REQUEST_FAILED_MESSAGE, ex);
    }
  }

  @Override
  @RateLimiter(name = EXTERNAL_GLOBAL)
  @Retry(name = EXTERNAL_GLOBAL)
  public ParticipantCoalitionV1DTO getCoalitionByLogin(String login) {
    try {
      return participantApi.getCoalitionByLogin(login);
    } catch (RestClientResponseException ex) {
      throw toPlatformException(ex);
    } catch (RestClientException ex) {
      throw new PlatformClientException(REQUEST_FAILED_MESSAGE, ex);
    }
  }

  @Override
  @RateLimiter(name = EXTERNAL_GLOBAL)
  @Retry(name = EXTERNAL_GLOBAL)
  public ParticipantLoginsV1DTO getParticipantsByCoalitionId(
      Long coalitionId, int limit, int offset) {
    try {
      return coalitionApi.getParticipantsByCoalitionId(coalitionId, limit, offset);
    } catch (RestClientResponseException ex) {
      throw toPlatformException(ex);
    } catch (RestClientException ex) {
      throw new PlatformClientException(REQUEST_FAILED_MESSAGE, ex);
    }
  }

  @Override
  @RateLimiter(name = EXTERNAL_GLOBAL)
  @Retry(name = PARTICIPANT_PROFILE)
  @CircuitBreaker(name = PARTICIPANT_PROFILE)
  public ParticipantV1DTO getParticipantByLogin(String login) {
    try {
      return participantApi.getParticipantByLogin(login);
    } catch (RestClientResponseException ex) {
      throw toPlatformException(ex);
    } catch (RestClientException ex) {
      throw new PlatformClientException(REQUEST_FAILED_MESSAGE, ex);
    }
  }

  /**
   * Преобразует ошибку HTTP-клиента в доменное исключение платформы.
   *
   * @param ex ошибка ответа REST-клиента
   * @return специализированное исключение платформы
   */
  private PlatformClientException toPlatformException(RestClientResponseException ex) {
    HttpHeaders headers = ex.getResponseHeaders();
    Map<String, List<String>> mappedHeaders = headers == null ? Collections.emptyMap() : headers;
    int code = ex.getStatusCode().value();
    String message = ex.getStatusText();
    String body = ex.getResponseBodyAsString();
    if (code == HTTP_UNAUTHORIZED || code == HTTP_FORBIDDEN) {
      return new PlatformUnauthorizedException(message, code, mappedHeaders, body);
    }
    if (code == HTTP_NOT_FOUND) {
      return new PlatformNotFoundException(message, code, mappedHeaders, body);
    }
    if (code == HTTP_RATE_LIMIT) {
      return new PlatformRateLimitException(message, code, mappedHeaders, body);
    }
    if (code >= HTTP_SERVER_ERROR) {
      return new PlatformTransientException(message, code, mappedHeaders, body);
    }
    return new PlatformClientException(message, code, mappedHeaders, body);
  }
}
