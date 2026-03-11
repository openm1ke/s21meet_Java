package ru.izpz.edu.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.model.ErrorResponseDTO;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
        ErrorResponseDTO dto = new ErrorResponseDTO();
        dto.setStatus(400);
        dto.setCode("BAD_REQUEST");
        dto.setMessage("bad");
        dto.setExceptionUUID("uuid");

        String bodyJson = objectMapper.writeValueAsString(dto);
        ApiException ex = new ApiException("err", 400, null, bodyJson);

        ResponseEntity<Object> response = handler.handleApiException(ex);
        assertEquals(400, response.getStatusCode().value());
        assertInstanceOf(ErrorResponseDTO.class, response.getBody());
        ErrorResponseDTO body = (ErrorResponseDTO) response.getBody();
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
}
