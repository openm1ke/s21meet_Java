package ru.izpz.edu.exception;

import java.io.Serial;
import java.util.List;
import java.util.Map;

/**
 * Ошибка превышения лимита запросов внешней платформы.
 */
public class PlatformRateLimitException extends PlatformClientException {
  @Serial private static final long serialVersionUID = 1L;

  /**
   * Создаёт исключение 429.
   *
   * @param message сообщение
   * @param code HTTP-код
   * @param responseHeaders заголовки ответа
   * @param responseBody тело ответа
   */
  public PlatformRateLimitException(
      String message, int code, Map<String, List<String>> responseHeaders, String responseBody) {
    super(message, code, responseHeaders, responseBody);
  }
}
