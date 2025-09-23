package ru.izpz.edu.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.model.ErrorResponseDTO;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException ex) {
        try {
            ErrorResponseDTO error = objectMapper.readValue(ex.getResponseBody(), ErrorResponseDTO.class);
            log.warn("Ошибка от внешнего API [{}]: {}", error.getCode(), error.getMessage());
            return ResponseEntity
                    .status(error.getStatus())
                    .body(error);
        } catch (Exception parseEx) {
            log.warn("Не удалось распарсить тело ошибки API: {}", ex.getResponseBody(), parseEx);
            return ResponseEntity
                    .status(ex.getCode())
                    .body(Map.of(
                            "status", ex.getCode(),
                            "message", ex.getMessage(),
                            "raw", ex.getResponseBody()
                    ));
        }
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<String> handleProfileNotFound(ProfileNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Ошибка валидации: {}", errors);

        Map<String, Object> body = Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "message", "Ошибка валидации",
                "errors", errors
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex) {
        // Логирование с полным stack trace
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Произошла внутренняя ошибка");
    }
}
