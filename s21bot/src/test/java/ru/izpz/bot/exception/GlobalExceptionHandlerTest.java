package ru.izpz.bot.exception;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleFeignException_shouldReturnBadGateway() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://localhost/test",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null
        );
        Response response = Response.builder()
                .status(500)
                .request(request)
                .headers(Collections.emptyMap())
                .build();
        FeignException exception = FeignException.errorStatus("test", response);

        ResponseEntity<String> actual = handler.handleFeignException(exception);

        assertEquals(HttpStatus.BAD_GATEWAY, actual.getStatusCode());
        assertEquals("Ошибка при обращении к внешнему сервису", actual.getBody());
    }
}
