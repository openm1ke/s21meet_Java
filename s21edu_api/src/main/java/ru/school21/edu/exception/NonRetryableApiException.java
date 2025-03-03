package ru.school21.edu.exception;

import ru.school21.edu.ApiException;

public class NonRetryableApiException extends ApiException {
    public NonRetryableApiException(int code, String message, Throwable cause) {
        super(message, cause, code, null, null);
    }
}
