package ru.izpz.edu.exception;

import java.io.Serial;
import java.time.OffsetDateTime;
import lombok.Getter;

@Getter
public class RegistrationValidationException extends IllegalStateException {
  @Serial private static final long serialVersionUID = 1L;

  private final String code;
  private final String s21login;
  private final OffsetDateTime expiresAt;

  public RegistrationValidationException(
      String code, String message, String s21login, OffsetDateTime expiresAt) {
    super(message);
    this.code = code;
    this.s21login = s21login;
    this.expiresAt = expiresAt;
  }
}
