package ru.izpz.edu.client;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Исключение ошибок удалённого GraphQL-вызова. */
public class GraphQlRemoteException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;
  private static final int MAX_RESPONSE_BODY_LENGTH = 16_384;

  private final int code;
  private final Map<String, List<String>> responseHeaders;
  private final String responseBody;

  /**
   * Создаёт исключение с сообщением.
   *
   * @param message текст ошибки
   */
  public GraphQlRemoteException(String message) {
    this(message, null, 0, Collections.emptyMap(), null);
  }

  /**
   * Создаёт исключение с причиной.
   *
   * @param message текст ошибки
   * @param cause первопричина
   */
  public GraphQlRemoteException(String message, Throwable cause) {
    this(message, cause, 0, Collections.emptyMap(), null);
  }

  /**
   * Создаёт исключение с HTTP-метаданными.
   *
   * @param message безопасное сообщение
   * @param code HTTP-код ответа
   * @param responseHeaders заголовки ответа
   * @param responseBody ограниченное тело ответа
   */
  public GraphQlRemoteException(
      String message, int code, Map<String, List<String>> responseHeaders, String responseBody) {
    this(message, null, code, responseHeaders, responseBody);
  }

  /**
   * Создаёт исключение с причиной и HTTP-метаданными.
   *
   * @param message безопасное сообщение
   * @param cause первопричина
   * @param code HTTP-код ответа
   * @param responseHeaders заголовки ответа
   * @param responseBody ограниченное тело ответа
   */
  public GraphQlRemoteException(
      String message,
      Throwable cause,
      int code,
      Map<String, List<String>> responseHeaders,
      String responseBody) {
    super(message, cause);
    this.code = code;
    this.responseHeaders = responseHeaders == null ? Collections.emptyMap() : responseHeaders;
    this.responseBody = limitResponseBody(responseBody);
  }

  /**
   * Возвращает HTTP-код ответа.
   *
   * @return HTTP-код или 0, если HTTP-ответ не получен
   */
  public int getCode() {
    return code;
  }

  /**
   * Возвращает заголовки ответа.
   *
   * @return заголовки ответа
   */
  public Map<String, List<String>> getResponseHeaders() {
    return responseHeaders;
  }

  /**
   * Возвращает ограниченное тело ответа.
   *
   * @return тело ответа или {@code null}
   */
  public String getResponseBody() {
    return responseBody;
  }

  private static String limitResponseBody(String body) {
    if (body == null || body.length() <= MAX_RESPONSE_BODY_LENGTH) {
      return body;
    }
    return body.substring(0, MAX_RESPONSE_BODY_LENGTH);
  }
}
