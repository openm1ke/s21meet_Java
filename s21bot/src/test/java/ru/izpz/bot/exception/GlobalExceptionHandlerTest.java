package ru.izpz.bot.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import feign.FeignException;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handleFeignException_shouldReturnBadGateway() {
    Request request =
        Request.create(
            Request.HttpMethod.GET,
            "http://localhost/test",
            Collections.emptyMap(),
            null,
            StandardCharsets.UTF_8,
            null);
    FeignException exception =
        FeignException.errorStatus(
            "test",
            Response.builder()
                .status(500)
                .request(request)
                .headers(Collections.emptyMap())
                .build());

    ResponseEntity<String> actual = handler.handleFeignException(exception);

    assertEquals(HttpStatus.BAD_GATEWAY, actual.getStatusCode());
    assertEquals("Ошибка при обращении к внешнему сервису", actual.getBody());
  }
}
