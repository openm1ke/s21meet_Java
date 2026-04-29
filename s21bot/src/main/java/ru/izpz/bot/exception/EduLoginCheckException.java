package ru.izpz.bot.exception;

import java.io.Serial;
import lombok.Getter;
import ru.izpz.dto.ServiceErrorDto;

@Getter
public class EduLoginCheckException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;
  private final transient ServiceErrorDto error;

  public EduLoginCheckException(ServiceErrorDto error) {
    super(error.getMessage());
    this.error = error;
  }
}
