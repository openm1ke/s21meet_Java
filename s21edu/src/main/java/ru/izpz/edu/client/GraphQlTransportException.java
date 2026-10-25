package ru.izpz.edu.client;

import java.io.Serial;

/** Сетевая ошибка соединения с GraphQL-сервисом. */
public class GraphQlTransportException extends GraphQlRemoteException {
  @Serial private static final long serialVersionUID = 1L;

  /**
   * Создаёт исключение транспортного уровня.
   *
   * @param message безопасное сообщение
   * @param cause исходная ошибка соединения
   */
  public GraphQlTransportException(String message, Throwable cause) {
    super(message, cause);
  }
}
