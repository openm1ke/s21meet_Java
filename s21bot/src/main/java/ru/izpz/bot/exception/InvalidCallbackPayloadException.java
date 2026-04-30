package ru.izpz.bot.exception;

import java.io.Serial;

public class InvalidCallbackPayloadException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  public InvalidCallbackPayloadException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidCallbackPayloadException(String message) {
    super(message);
  }
}
