package ru.izpz.bot.exception;

import java.io.Serial;

public class RegistrationNotStartedException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  public RegistrationNotStartedException(String message) {
    super(message);
  }
}
