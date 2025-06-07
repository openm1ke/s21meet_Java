package ru.izpz.edu.exception;

import ru.izpz.dto.ApiException;

public class NonRetryableApiException extends ApiException {
    public NonRetryableApiException(int code, String message, Throwable cause) {
        super(message, cause, code, null, null);
    }
}
