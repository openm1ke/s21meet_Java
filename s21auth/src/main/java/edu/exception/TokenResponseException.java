package edu.exception;

public class TokenResponseException extends RuntimeException {

    public TokenResponseException(String message) {
        super(message);
    }

    public TokenResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
