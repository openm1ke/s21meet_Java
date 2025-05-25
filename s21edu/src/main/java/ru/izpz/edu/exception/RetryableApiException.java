package ru.izpz.edu.exception;

import ru.izpz.edu.ApiException;

public class RetryableApiException extends ApiException {
    public RetryableApiException(int code, String message, Throwable cause) {
        super(message, cause, code, null, null);
    }
}



