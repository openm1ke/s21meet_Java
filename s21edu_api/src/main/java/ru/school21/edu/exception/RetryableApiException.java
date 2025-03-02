package ru.school21.edu.exception;

import ru.school21.edu.ApiException;

public class RetryableApiException extends ApiException {
    public RetryableApiException(int code, String message, Throwable cause) {
        super(message, cause, code, null, null);
    }
}



