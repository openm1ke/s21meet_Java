package ru.izpz.edu.exception;

import java.io.Serial;

/** Транзитная ошибка соединения с внешней платформой. */
public class PlatformTransportException extends PlatformClientException {
  @Serial private static final long serialVersionUID = 1L;

  /**
   * Создаёт исключение транспортного уровня.
   *
   * @param message безопасное сообщение
   * @param cause исходная ошибка соединения
   */
  public PlatformTransportException(String message, Throwable cause) {
    super(message, cause);
  }
}
