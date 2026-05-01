package ru.izpz.edu.exception;

import java.io.Serial;
import java.util.List;
import java.util.Map;

/** Временная ошибка внешней платформы (5xx). */
public class PlatformTransientException extends PlatformClientException {
  @Serial private static final long serialVersionUID = 1L;

  /**
   * Создаёт исключение временной ошибки.
   *
   * @param message сообщение
   * @param code HTTP-код
   * @param responseHeaders заголовки ответа
   * @param responseBody тело ответа
   */
  public PlatformTransientException(
      String message, int code, Map<String, List<String>> responseHeaders, String responseBody) {
    super(message, code, responseHeaders, responseBody);
  }
}
