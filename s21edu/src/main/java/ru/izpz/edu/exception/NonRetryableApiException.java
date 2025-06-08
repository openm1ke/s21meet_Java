package ru.izpz.edu.exception;

import ru.izpz.dto.ApiException;

import java.util.List;
import java.util.Map;

public class NonRetryableApiException extends ApiException {
    public NonRetryableApiException(int code, String message, Map<String, List<String>> errors, String responseBody, Throwable cause) {
        super(message, cause, code, errors, responseBody);
    }
}
