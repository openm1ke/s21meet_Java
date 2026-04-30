package ru.izpz.edu.exception;

import java.io.Serial;

/** Исключение отсутствия сущности в локальном хранилище. */
public class EntityNotFoundException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  /**
   * Создаёт исключение с сообщением.
   *
   * @param message текст ошибки
   */
  public EntityNotFoundException(String message) {
    super(message);
  }
}
