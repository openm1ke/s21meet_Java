package ru.izpz.edu.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.izpz.dto.ServiceErrorDto;

/** Глобальный обработчик исключений REST-слоя. */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final ObjectMapper objectMapper;
  private static final String STATUS_FIELD = "status";
  private static final String MESSAGE_FIELD = "message";
  private static final String EXTERNAL_SERVICE_MESSAGE = "Ошибка внешнего сервиса";

  /**
   * Обрабатывает исключения клиента платформы.
   *
   * @param ex исключение внешнего API
   * @return тело ответа с кодом ошибки
   */
  @ExceptionHandler(PlatformClientException.class)
  public ResponseEntity<Object> handlePlatformException(PlatformClientException ex) {
    if (ex instanceof PlatformRateLimitException) {
      return safeResponse(
          HttpStatus.TOO_MANY_REQUESTS.value(), "Внешний сервис временно перегружен");
    }
    if (ex instanceof PlatformTransportException) {
      return safeResponse(
          HttpStatus.SERVICE_UNAVAILABLE.value(), "Внешний сервис временно недоступен");
    }
    if (ex instanceof PlatformTransientException) {
      return safeResponse(HttpStatus.BAD_GATEWAY.value(), "Внешний сервис временно недоступен");
    }

    if (ex instanceof PlatformUnauthorizedException) {
      int status = normalizeStatus(ex.getCode(), HttpStatus.BAD_GATEWAY.value());
      String message =
          status == HttpStatus.FORBIDDEN.value()
              ? "Внешняя платформа запретила доступ"
              : "Внешняя платформа отклонила авторизацию";
      return safeResponse(status, message);
    }

    if (ex instanceof PlatformNotFoundException) {
      return safeResponse(HttpStatus.NOT_FOUND.value(), "Ресурс внешней платформы не найден");
    }

    int fallbackStatus = normalizeStatus(ex.getCode(), HttpStatus.BAD_GATEWAY.value());
    String responseBody = ex.getResponseBody();
    if (responseBody == null || responseBody.isBlank()) {
      log.warn(
          "Пустой ответ внешнего API: status={}, error_type={}, content_type={}",
          fallbackStatus,
          ex.getClass().getSimpleName(),
          contentType(ex));
      return safeResponse(fallbackStatus, EXTERNAL_SERVICE_MESSAGE);
    }

    try {
      ServiceErrorDto error = objectMapper.readValue(responseBody, ServiceErrorDto.class);
      if (!isValidError(error, fallbackStatus)) {
        throw new IllegalArgumentException("Invalid external error response");
      }
      log.warn(
          "Ошибка внешнего API: status={}, code={}, exception_uuid={}",
          error.getStatus(),
          error.getCode(),
          error.getExceptionUUID());
      return ResponseEntity.status(fallbackStatus).body(error);
    } catch (Exception parseEx) {
      log.warn(
          "Не удалось распарсить ответ внешнего API: status={}, error_type={}, "
              + "content_type={}, body_length={}, parse_error_type={}",
          fallbackStatus,
          ex.getClass().getSimpleName(),
          contentType(ex),
          responseBody.length(),
          parseEx.getClass().getSimpleName());
      return safeResponse(fallbackStatus, EXTERNAL_SERVICE_MESSAGE);
    }
  }

  private ResponseEntity<Object> safeResponse(int status, String message) {
    return ResponseEntity.status(status).body(Map.of(STATUS_FIELD, status, MESSAGE_FIELD, message));
  }

  private int normalizeStatus(int status, int fallback) {
    return status >= 400 && status <= 599 ? status : fallback;
  }

  private boolean isValidError(ServiceErrorDto error, int expectedStatus) {
    return error != null
        && error.getStatus() != null
        && error.getStatus() == expectedStatus
        && hasText(error.getExceptionUUID())
        && hasText(error.getCode())
        && hasText(error.getMessage());
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String contentType(PlatformClientException ex) {
    return ex.getResponseHeaders().entrySet().stream()
        .filter(entry -> HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(entry.getKey()))
        .flatMap(
            entry ->
                entry.getValue() == null
                    ? java.util.stream.Stream.empty()
                    : entry.getValue().stream())
        .findFirst()
        .orElse("unknown");
  }

  @ExceptionHandler(CallNotPermittedException.class)
  public ResponseEntity<Object> handleCircuitOpen(CallNotPermittedException ex) {
    log.warn("Circuit breaker open: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of(STATUS_FIELD, 503, MESSAGE_FIELD, "Внешний сервис временно недоступен"));
  }

  /**
   * Обрабатывает случаи отсутствия сущности.
   *
   * @param e исключение отсутствия сущности
   * @return HTTP 404
   */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<String> handleProfileNotFound(EntityNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
  }

  /**
   * Возвращает ошибки валидации входных данных.
   *
   * @param ex исключение валидации
   * @return HTTP 400 со списком ошибок по полям
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new LinkedHashMap<>();

    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

    log.warn("Ошибка валидации: {}", errors);

    Map<String, Object> body =
        Map.of(
            STATUS_FIELD,
            HttpStatus.BAD_REQUEST.value(),
            MESSAGE_FIELD,
            "Ошибка валидации",
            "errors",
            errors);

    return ResponseEntity.badRequest().body(body);
  }

  /**
   * Обрабатывает ошибки некорректного состояния запроса.
   *
   * @param e исключение состояния
   * @return HTTP 400
   */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<String> handleIllegalStateException(IllegalStateException e) {
    log.warn("IllegalStateException: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
  }

  /**
   * Резервный обработчик всех необработанных исключений.
   *
   * @param ex необработанное исключение
   * @return HTTP 500
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleAllExceptions(Exception ex) {
    // Логирование с полным stack trace
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Произошла внутренняя ошибка");
  }
}
