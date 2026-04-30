package ru.izpz.exception;

import java.io.Serial;

public class TokenResponseException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  public TokenResponseException(String message) {
    super(message);
  }

  public TokenResponseException(String message, Throwable cause) {
    super(message, cause);
  }
}
