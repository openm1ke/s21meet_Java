package ru.izpz.edu.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.ServiceErrorDto;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(objectMapper);

    @Test
    void handleProfileNotFound_shouldReturn404() {
        ResponseEntity<String> response = handler.handleProfileNotFound(new EntityNotFoundException("not found"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("not found", response.getBody());
    }

    @Test
    void handleApiException_shouldReturnParsedBody_whenJsonIsValid() throws Exception {
        ServiceErrorDto dto = new ServiceErrorDto();
        dto.setStatus(400);
        dto.setCode("BAD_REQUEST");
        dto.setMessage("bad");
        dto.setExceptionUUID("uuid");

        String bodyJson = objectMapper.writeValueAsString(dto);
        ApiException ex = new ApiException("err", 400, null, bodyJson);

        ResponseEntity<Object> response = handler.handleApiException(ex);
        assertEquals(400, response.getStatusCode().value());
        assertInstanceOf(ServiceErrorDto.class, response.getBody());
        ServiceErrorDto body = (ServiceErrorDto) response.getBody();
        assertEquals("BAD_REQUEST", body.getCode());
        assertEquals("bad", body.getMessage());
        assertEquals(400, body.getStatus());
    }

    @Test
    void handleApiException_shouldReturnRawBody_whenJsonInvalid() {
        ApiException ex = new ApiException("err", 502, null, "not-json");

        ResponseEntity<Object> response = handler.handleApiException(ex);
        assertEquals(502, response.getStatusCode().value());

        assertInstanceOf(Map.class, response.getBody());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(502, body.get("status"));
        assertNotNull(body.get("message"));
        assertEquals("not-json", body.get("raw"));
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
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "username", "must not be blank"),
                new FieldError("request", "message", "must not be blank")
        ));

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
        ResponseEntity<String> response = handler.handleIllegalStateException(
                new IllegalStateException("bad state")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad state", response.getBody());
    }
}
