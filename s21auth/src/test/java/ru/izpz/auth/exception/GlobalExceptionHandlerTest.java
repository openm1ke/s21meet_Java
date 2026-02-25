package ru.izpz.auth.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.izpz.exception.TokenResponseException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void handleAllExceptions_shouldReturnInternalServerError_whenGenericExceptionThrown() {
        Exception testException = new RuntimeException("Test exception message");

        ResponseEntity<String> response = globalExceptionHandler.handleAllExceptions(testException);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Произошла внутренняя ошибка", response.getBody());
    }

    @Test
    void handleAllExceptions_shouldReturnInternalServerError_whenTokenResponseExceptionThrown() {
        TokenResponseException tokenException = new TokenResponseException("Token error");

        ResponseEntity<String> response = globalExceptionHandler.handleAllExceptions(tokenException);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Произошла внутренняя ошибка", response.getBody());
    }

    @Test
    void handleAllExceptions_shouldReturnInternalServerError_whenNullPointerExceptionThrown() {
        NullPointerException nullPointerException = new NullPointerException("Null pointer");

        ResponseEntity<String> response = globalExceptionHandler.handleAllExceptions(nullPointerException);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Произошла внутренняя ошибка", response.getBody());
    }

    @Test
    void handleAllExceptions_shouldReturnInternalServerError_whenIllegalArgumentExceptionThrown() {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Illegal argument");

        ResponseEntity<String> response = globalExceptionHandler.handleAllExceptions(illegalArgumentException);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Произошла внутренняя ошибка", response.getBody());
    }

    @Test
    void handleAllExceptions_shouldReturnInternalServerError_whenCustomExceptionWithCauseThrown() {
        Exception causeException = new RuntimeException("Cause exception");
        Exception customException = new RuntimeException("Custom exception", causeException);

        ResponseEntity<String> response = globalExceptionHandler.handleAllExceptions(customException);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Произошла внутренняя ошибка", response.getBody());
    }
}
