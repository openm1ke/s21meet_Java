package ru.izpz.edu.client;

import java.io.Serial;

/**
 * Исключение ошибок удалённого GraphQL-вызова.
 */
public class GraphQlRemoteException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  /**
   * Создаёт исключение с сообщением.
   *
   * @param message текст ошибки
   */
  public GraphQlRemoteException(String message) {
    super(message);
  }

  /**
   * Создаёт исключение с причиной.
   *
   * @param message текст ошибки
   * @param cause первопричина
   */
  public GraphQlRemoteException(String message, Throwable cause) {
    super(message, cause);
  }
}
