package ru.izpz.edu.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  /**
   * Обрабатывает исключения клиента платформы.
   *
   * @param ex исключение внешнего API
   * @return тело ответа с кодом ошибки
   */
  @ExceptionHandler(PlatformClientException.class)
  public ResponseEntity<Object> handlePlatformException(PlatformClientException ex) {
    if (ex instanceof PlatformRateLimitException) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .body(Map.of(STATUS_FIELD, 429, MESSAGE_FIELD, "Внешний сервис временно перегружен"));
    }
    if (ex instanceof PlatformTransientException) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(Map.of(STATUS_FIELD, 502, MESSAGE_FIELD, "Внешний сервис временно недоступен"));
    }
    try {
      ServiceErrorDto error = objectMapper.readValue(ex.getResponseBody(), ServiceErrorDto.class);
      log.warn("Ошибка от внешнего API [{}]: {}", error.getCode(), error.getMessage());
      return ResponseEntity.status(error.getStatus()).body(error);
    } catch (Exception parseEx) {
      log.warn("Не удалось распарсить тело ошибки API: {}", ex.getResponseBody(), parseEx);
      return ResponseEntity.status(ex.getCode())
          .body(Map.of(STATUS_FIELD, ex.getCode(), MESSAGE_FIELD, "Ошибка внешнего сервиса"));
    }
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
