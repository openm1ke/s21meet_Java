package ru.izpz.edu.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import ru.izpz.dto.RegistrationErrorDto;
import ru.izpz.dto.ServiceErrorDto;

class GlobalExceptionHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final GlobalExceptionHandler handler = new GlobalExceptionHandler(objectMapper);

  @Test
  void handleProfileNotFound_shouldReturn404() {
    ResponseEntity<String> response =
        handler.handleProfileNotFound(new EntityNotFoundException("not found"));
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("not found", response.getBody());
  }

  @Test
  void handleRegistrationValidationException_shouldReturnStructuredExpirationDetails() {
    OffsetDateTime expiresAt = OffsetDateTime.parse("2026-10-28T12:00:00+03:00");
    RegistrationValidationException exception =
        new RegistrationValidationException(
            RegistrationErrorDto.REGISTRATION_EXPIRED_CODE,
            "Срок регистрации истек. Начните регистрацию заново",
            "dawnendor",
            expiresAt);

    ResponseEntity<RegistrationErrorDto> response =
        handler.handleRegistrationValidationException(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    RegistrationErrorDto body = response.getBody();
    assertEquals(RegistrationErrorDto.REGISTRATION_EXPIRED_CODE, body.getCode());
    assertEquals("dawnendor", body.getS21login());
    assertEquals(expiresAt, body.getExpiresAt());
  }

  @Test
  void handlePlatformException_shouldReturnParsedBody_whenJsonIsValid() throws Exception {
    ServiceErrorDto dto = new ServiceErrorDto();
    dto.setStatus(400);
    dto.setCode("BAD_REQUEST");
    dto.setMessage("bad");
    dto.setExceptionUUID("uuid");

    String bodyJson = objectMapper.writeValueAsString(dto);
    PlatformClientException ex = new PlatformClientException("err", 400, null, bodyJson);

    ResponseEntity<Object> response = handler.handlePlatformException(ex);
    assertEquals(400, response.getStatusCode().value());
    assertInstanceOf(ServiceErrorDto.class, response.getBody());
    ServiceErrorDto body = (ServiceErrorDto) response.getBody();
    assertEquals("BAD_REQUEST", body.getCode());
    assertEquals("bad", body.getMessage());
    assertEquals(400, body.getStatus());
  }

  @Test
  void handlePlatformException_shouldReturnRawBody_whenJsonInvalid() {
    PlatformClientException ex = new PlatformClientException("err", 502, null, "not-json");

    ResponseEntity<Object> response = handler.handlePlatformException(ex);
    assertEquals(502, response.getStatusCode().value());

    assertInstanceOf(Map.class, response.getBody());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals(502, body.get("status"));
    assertEquals("Ошибка внешнего сервиса", body.get("message"));
  }

  @Test
  void handlePlatformException_shouldReturnFriendlyMessageForRateLimit() {
    PlatformClientException ex = new PlatformRateLimitException("429", 429, Map.of(), "{}");

    ResponseEntity<Object> response = handler.handlePlatformException(ex);
    assertEquals(429, response.getStatusCode().value());
    assertInstanceOf(Map.class, response.getBody());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals(429, body.get("status"));
    assertEquals("Внешний сервис временно перегружен", body.get("message"));
  }

  @Test
  void handlePlatformException_shouldReturnFriendlyMessageForTransientErrors() {
    PlatformClientException ex = new PlatformTransientException("500", 500, Map.of(), "{}");

    ResponseEntity<Object> response = handler.handlePlatformException(ex);
    assertEquals(502, response.getStatusCode().value());
    assertInstanceOf(Map.class, response.getBody());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals(502, body.get("status"));
    assertEquals("Внешний сервис временно недоступен", body.get("message"));
  }

  @Test
  void handlePlatformException_shouldReturnSafeMessageForHtmlForbiddenResponse() {
    PlatformClientException ex =
        new PlatformUnauthorizedException(
            "Forbidden", 403, Map.of(), "<html>secret upstream response</html>");

    ResponseEntity<Object> response = handler.handlePlatformException(ex);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertInstanceOf(Map.class, response.getBody());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals(403, body.get("status"));
    assertEquals("Внешняя платформа запретила доступ", body.get("message"));
    assertEquals(false, body.toString().contains("secret upstream response"));
  }

  @Test
  void handlePlatformException_shouldReturnSafeMessageForNotFoundResponse() {
    PlatformClientException ex =
        new PlatformNotFoundException("Not found", 404, Map.of(), "not-json");

    ResponseEntity<Object> response = handler.handlePlatformException(ex);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertInstanceOf(Map.class, response.getBody());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals(404, body.get("status"));
    assertEquals("Ресурс внешней платформы не найден", body.get("message"));
  }

  @Test
  void handlePlatformException_shouldMapTransportErrorTo503() {
    PlatformClientException ex =
        new PlatformTransportException("transport error", new RuntimeException("timeout"));

    ResponseEntity<Object> response = handler.handlePlatformException(ex);

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertInstanceOf(Map.class, response.getBody());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals(503, body.get("status"));
  }

  @Test
  void handlePlatformException_shouldUseSafeResponseForNullBodyAndInvalidStatus() {
    PlatformClientException ex = new PlatformClientException("transport", 0, null, null);

    ResponseEntity<Object> response = handler.handlePlatformException(ex);

    assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    assertInstanceOf(Map.class, response.getBody());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals(502, body.get("status"));
  }

  @Test
  void handlePlatformException_shouldRejectJsonWithMismatchedStatus() throws Exception {
    ServiceErrorDto dto =
        new ServiceErrorDto()
            .setStatus(500)
            .setCode("INTERNAL_SERVER_ERROR")
            .setMessage("internal")
            .setExceptionUUID("uuid");
    PlatformClientException ex =
        new PlatformClientException(
            "bad request", 400, Map.of(), objectMapper.writeValueAsString(dto));

    ResponseEntity<Object> response = handler.handlePlatformException(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertInstanceOf(Map.class, response.getBody());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals("Ошибка внешнего сервиса", body.get("message"));
  }

  @Test
  void handleAllExceptions_shouldReturn500() {
    ResponseEntity<String> response = handler.handleAllExceptions(new RuntimeException("boom"));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals("Произошла внутренняя ошибка", response.getBody());
  }

  @Test
  void handleValidationException_shouldReturn400WithFieldErrors() {
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors())
        .thenReturn(
            List.of(
                new FieldError("request", "username", "must not be blank"),
                new FieldError("request", "message", "must not be blank")));

    ResponseEntity<Object> response = handler.handleValidationException(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertInstanceOf(Map.class, response.getBody());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.get("status"));
    assertEquals("Ошибка валидации", body.get("message"));
    assertInstanceOf(Map.class, body.get("errors"));

    Map<?, ?> errors = (Map<?, ?>) body.get("errors");
    assertEquals("must not be blank", errors.get("username"));
    assertEquals("must not be blank", errors.get("message"));
  }

  @Test
  void handleIllegalStateException_shouldReturn400() {
    ResponseEntity<String> response =
        handler.handleIllegalStateException(new IllegalStateException("bad state"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("bad state", response.getBody());
  }

  @Test
  void handleCircuitOpen_shouldReturn503() {
    ResponseEntity<Object> response =
        handler.handleCircuitOpen(
            CallNotPermittedException.createCallNotPermittedException(
                io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("cb")));

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertInstanceOf(Map.class, response.getBody());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals(503, body.get("status"));
    assertEquals("Внешний сервис временно недоступен", body.get("message"));
  }
}
