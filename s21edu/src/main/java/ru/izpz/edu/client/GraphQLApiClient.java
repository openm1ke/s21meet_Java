package ru.izpz.edu.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.izpz.edu.service.TokenService;

/** Клиент выполнения GraphQL-запросов к платформе. */
@Component
@ConditionalOnProperty(name = "api.graphql.enabled", havingValue = "true")
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class GraphQLApiClient implements PlatformGraphQlFacade {

  private static final String GRAPHQL_URL = "https://platform.21-school.ru/services/graphql";
  private static final String SCHOOL_ID = "6bfe3c56-0211-4fe1-9e59-51616caac4dd";
  private static final String ERRORS_FIELD = "errors";
  private static final String DATA_FIELD = "data";
  private static final String GRAPHQL_REQUEST_COUNTER = "edu_graphql_requests_total";
  private static final String GRAPHQL_REQUEST_DURATION = "edu_graphql_request_duration_seconds";
  private static final String TAG_OPERATION = "operation";
  private static final String TAG_OUTCOME = "outcome";
  private static final int HTTP_UNAUTHORIZED = 401;
  private static final int HTTP_FORBIDDEN = 403;
  private static final int HTTP_NOT_FOUND = 404;
  private static final int HTTP_REQUEST_TIMEOUT = 408;
  private static final int HTTP_TOO_MANY_REQUESTS = 429;
  private static final int HTTP_INTERNAL_SERVER_ERROR = 500;
  private static final int HTTP_BAD_GATEWAY = 502;
  private static final int HTTP_SERVICE_UNAVAILABLE = 503;
  private static final int HTTP_GATEWAY_TIMEOUT = 504;
  private static final Set<String> UNAUTHORIZED_ERROR_CODES =
      Set.of("UNAUTHENTICATED", "UNAUTHORIZED", "FORBIDDEN", "ACCESS_DENIED");
  private static final Set<String> NOT_FOUND_ERROR_CODES =
      Set.of("NOT_FOUND", "RESOURCE_NOT_FOUND");
  private static final Set<String> RATE_LIMIT_ERROR_CODES =
      Set.of("TOO_MANY_REQUESTS", "RATE_LIMITED", "RATE_LIMIT");
  private static final Set<String> TRANSIENT_ERROR_CODES =
      Set.of(
          "INTERNAL_ERROR",
          "INTERNAL_SERVER_ERROR",
          "SERVICE_UNAVAILABLE",
          "UNAVAILABLE",
          "TIMEOUT",
          "GATEWAY_TIMEOUT",
          "BAD_GATEWAY");
  private final RestTemplate restTemplate;
  private final TokenService tokenService;
  private final ObjectMapper om;
  private final MeterRegistry meterRegistry;

  @Override
  @RateLimiter(name = "graphqlGlobal")
  @Retry(name = "graphqlGlobal")
  public <T> T execute(
      String operationName, Map<String, Object> variables, String query, Class<T> dataClass) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String outcome = "success";
    try {
      String token = tokenService.getToken();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.setBearerAuth(token);
      headers.add("schoolId", SCHOOL_ID);

      GraphQlRequest body = new GraphQlRequest(operationName, variables, query);
      HttpEntity<GraphQlRequest> entity = new HttpEntity<>(body, headers);
      ResponseEntity<String> resp = executePost(entity);

      String raw = resp.getBody();
      if (raw == null || raw.isBlank()) {
        throw new GraphQlRemoteException("Пустой ответ GraphQL");
      }

      return parseAndConvert(raw, dataClass);
    } catch (RuntimeException e) {
      outcome = "error";
      throw e;
    } finally {
      meterRegistry
          .counter(GRAPHQL_REQUEST_COUNTER, TAG_OPERATION, operationName, TAG_OUTCOME, outcome)
          .increment();
      sample.stop(
          Timer.builder(GRAPHQL_REQUEST_DURATION)
              .description("Duration of GraphQL requests")
              .tag(TAG_OPERATION, operationName)
              .tag(TAG_OUTCOME, outcome)
              .register(meterRegistry));
    }
  }

  /**
   * Выполняет HTTP POST-запрос к GraphQL endpoint.
   *
   * @param entity HTTP-запрос
   * @return HTTP-ответ
   */
  private ResponseEntity<String> executePost(HttpEntity<GraphQlRequest> entity) {
    try {
      return restTemplate.postForEntity(GRAPHQL_URL, entity, String.class);
    } catch (HttpStatusCodeException e) {
      throw toHttpException(e);
    } catch (ResourceAccessException e) {
      throw new GraphQlTransportException("Ошибка соединения с GraphQL-сервисом", e);
    } catch (RestClientException e) {
      throw new GraphQlRemoteException("Ошибка GraphQL-клиента", e);
    }
  }

  /**
   * Разбирает JSON-ответ и преобразует {@code data} в целевой тип.
   *
   * @param raw сырой JSON
   * @param dataClass целевой тип
   * @param <T> тип результата
   * @return данные из поля data
   */
  private <T> T parseAndConvert(String raw, Class<T> dataClass) {
    try {
      JsonNode root = om.readTree(raw);
      if (root.has(ERRORS_FIELD)
          && root.get(ERRORS_FIELD).isArray()
          && !root.get(ERRORS_FIELD).isEmpty()) {
        return handleGraphQlErrors(root.get(ERRORS_FIELD));
      }
      JsonNode data = root.get(DATA_FIELD);
      if (data == null || data.isNull()) {
        throw new GraphQlRemoteException("В ответе нет поля '" + DATA_FIELD + "'");
      }
      return om.convertValue(data, dataClass);
    } catch (JsonProcessingException | IllegalArgumentException e) {
      throw new GraphQlRemoteException("Не удалось распарсить JSON GraphQL", e);
    }
  }

  /**
   * Выбрасывает исключение при наличии ошибок GraphQL.
   *
   * @param errors массив ошибок
   * @param <T> формальный тип результата
   * @return никогда не возвращает значение
   */
  private <T> T handleGraphQlErrors(JsonNode errors) {
    throw toGraphQlErrorException(errors);
  }

  private GraphQlRemoteException toHttpException(HttpStatusCodeException ex) {
    int code = ex.getStatusCode().value();
    HttpHeaders headers = ex.getResponseHeaders();
    Map<String, List<String>> responseHeaders = headers == null ? Collections.emptyMap() : headers;
    String responseBody = ex.getResponseBodyAsString();
    String message = "GraphQL HTTP request failed with status " + code;

    if (code == HTTP_UNAUTHORIZED || code == HTTP_FORBIDDEN) {
      return new GraphQlUnauthorizedException(message, code, responseHeaders, responseBody);
    }
    if (code == HTTP_NOT_FOUND) {
      return new GraphQlNotFoundException(message, code, responseHeaders, responseBody);
    }
    if (code == HTTP_TOO_MANY_REQUESTS) {
      return new GraphQlRateLimitException(message, code, responseHeaders, responseBody);
    }
    if (isTransientStatus(code) || code == HTTP_REQUEST_TIMEOUT) {
      return new GraphQlTransientException(message, code, responseHeaders, responseBody);
    }
    return new GraphQlRemoteException(message, code, responseHeaders, responseBody);
  }

  private GraphQlRemoteException toGraphQlErrorException(JsonNode errors) {
    String errorCode = findRecognizedErrorCode(errors);
    String responseBody = errors.toString();
    if (isOneOf(errorCode, UNAUTHORIZED_ERROR_CODES)) {
      int code = "FORBIDDEN".equals(errorCode) || "ACCESS_DENIED".equals(errorCode) ? 403 : 401;
      return new GraphQlUnauthorizedException(
          "GraphQL response rejected the request", code, Collections.emptyMap(), responseBody);
    }
    if (isOneOf(errorCode, NOT_FOUND_ERROR_CODES)) {
      return new GraphQlNotFoundException(
          "GraphQL resource was not found", HTTP_NOT_FOUND, Collections.emptyMap(), responseBody);
    }
    if (isOneOf(errorCode, RATE_LIMIT_ERROR_CODES)) {
      return new GraphQlRateLimitException(
          "GraphQL request rate limit exceeded",
          HTTP_TOO_MANY_REQUESTS,
          Collections.emptyMap(),
          responseBody);
    }
    if (isOneOf(errorCode, TRANSIENT_ERROR_CODES)) {
      return new GraphQlTransientException(
          "GraphQL service returned a temporary error",
          HTTP_SERVICE_UNAVAILABLE,
          Collections.emptyMap(),
          responseBody);
    }
    return new GraphQlRemoteException(
        "GraphQL response contains a non-retryable error", 0, Collections.emptyMap(), responseBody);
  }

  private String findRecognizedErrorCode(JsonNode errors) {
    String fallbackCode = null;
    for (JsonNode error : errors) {
      String code = readErrorCode(error);
      if (code == null) {
        continue;
      }
      if (isOneOf(code, UNAUTHORIZED_ERROR_CODES)
          || isOneOf(code, NOT_FOUND_ERROR_CODES)
          || isOneOf(code, RATE_LIMIT_ERROR_CODES)
          || isOneOf(code, TRANSIENT_ERROR_CODES)) {
        return code;
      }
      fallbackCode = code;
    }
    return fallbackCode;
  }

  private String readErrorCode(JsonNode error) {
    JsonNode extensions = error.get("extensions");
    String code = textValue(extensions, "code");
    return code == null ? textValue(error, "code") : code;
  }

  private String textValue(JsonNode node, String field) {
    if (node == null) {
      return null;
    }
    JsonNode value = node.get(field);
    return value == null || value.isNull() || value.asText().isBlank()
        ? null
        : value.asText().toUpperCase(java.util.Locale.ROOT);
  }

  private boolean isOneOf(String value, Set<String> values) {
    return value != null && values.contains(value);
  }

  private boolean isTransientStatus(int code) {
    return code == HTTP_INTERNAL_SERVER_ERROR
        || code == HTTP_BAD_GATEWAY
        || code == HTTP_SERVICE_UNAVAILABLE
        || code == HTTP_GATEWAY_TIMEOUT;
  }

  /** Тело GraphQL-запроса. */
  public record GraphQlRequest(String operationName, Map<String, Object> variables, String query) {}
}
