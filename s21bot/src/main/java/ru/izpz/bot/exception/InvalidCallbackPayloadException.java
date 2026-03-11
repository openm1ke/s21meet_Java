package ru.izpz.bot.exception;

public class InvalidCallbackPayloadException extends RuntimeException {
    public InvalidCallbackPayloadException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCallbackPayloadException(String message) {
        super(message);
    }
}
