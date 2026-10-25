package ru.izpz.bot.exception;

import java.io.Serial;
import lombok.Getter;
import ru.izpz.dto.RegistrationErrorDto;

@Getter
public class RegistrationExpiredException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  private final transient RegistrationErrorDto error;

  public RegistrationExpiredException(RegistrationErrorDto error) {
    super(error.getMessage());
    this.error = error;
  }
}
