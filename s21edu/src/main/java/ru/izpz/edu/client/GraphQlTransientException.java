package ru.izpz.edu.client;

import java.io.Serial;
import java.util.List;
import java.util.Map;

/** Временная HTTP-ошибка GraphQL-сервиса. */
public class GraphQlTransientException extends GraphQlRemoteException {
  @Serial private static final long serialVersionUID = 1L;

  /**
   * Создаёт исключение временной ошибки.
   *
   * @param message безопасное сообщение
   * @param code HTTP-код ответа
   * @param responseHeaders заголовки ответа
   * @param responseBody ограниченное тело ответа
   */
  public GraphQlTransientException(
      String message, int code, Map<String, List<String>> responseHeaders, String responseBody) {
    super(message, code, responseHeaders, responseBody);
  }
}
