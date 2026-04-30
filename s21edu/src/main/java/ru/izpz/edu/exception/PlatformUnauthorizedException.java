package ru.izpz.edu.exception;

import java.io.Serial;
import java.util.List;
import java.util.Map;

/** Ошибка авторизации/доступа внешней платформы. */
public class PlatformUnauthorizedException extends PlatformClientException {
  @Serial private static final long serialVersionUID = 1L;

  /**
   * Создаёт исключение 401/403.
   *
   * @param message сообщение
   * @param code HTTP-код
   * @param responseHeaders заголовки ответа
   * @param responseBody тело ответа
   */
  public PlatformUnauthorizedException(
      String message, int code, Map<String, List<String>> responseHeaders, String responseBody) {
    super(message, code, responseHeaders, responseBody);
  }
}
