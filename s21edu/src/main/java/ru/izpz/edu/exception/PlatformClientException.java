package ru.izpz.edu.exception;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Базовое исключение ошибок внешней платформы. */
public class PlatformClientException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  private final int code;
  private final Map<String, List<String>> responseHeaders;
  private final String responseBody;

  /**
   * Создаёт исключение с причиной.
   *
   * @param message сообщение
   * @param cause причина
   */
  public PlatformClientException(String message, Throwable cause) {
    this(message, cause, 0, Collections.emptyMap(), null);
  }

  /**
   * Создаёт исключение с HTTP-метаданными.
   *
   * @param message сообщение
   * @param code HTTP-код
   * @param responseHeaders заголовки ответа
   * @param responseBody тело ответа
   */
  public PlatformClientException(
      String message, int code, Map<String, List<String>> responseHeaders, String responseBody) {
    this(message, null, code, responseHeaders, responseBody);
  }

  /**
   * Создаёт исключение с причиной и HTTP-метаданными.
   *
   * @param message сообщение
   * @param cause причина
   * @param code HTTP-код
   * @param responseHeaders заголовки ответа
   * @param responseBody тело ответа
   */
  public PlatformClientException(
      String message,
      Throwable cause,
      int code,
      Map<String, List<String>> responseHeaders,
      String responseBody) {
    super(message, cause);
    this.code = code;
    this.responseHeaders = responseHeaders == null ? Collections.emptyMap() : responseHeaders;
    this.responseBody = responseBody;
  }

  /**
   * Возвращает HTTP-код ошибки.
   *
   * @return код ответа
   */
  public int getCode() {
    return code;
  }

  /**
   * Возвращает заголовки ответа платформы.
   *
   * @return заголовки ответа
   */
  public Map<String, List<String>> getResponseHeaders() {
    return responseHeaders;
  }

  /**
   * Возвращает тело ответа платформы.
   *
   * @return тело ответа
   */
  public String getResponseBody() {
    return responseBody;
  }
}
