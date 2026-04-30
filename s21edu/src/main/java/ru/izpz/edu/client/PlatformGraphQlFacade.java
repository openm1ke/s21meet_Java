package ru.izpz.edu.client;

import java.util.Map;

/**
 * Фасад вызова GraphQL-операций платформы.
 */
public interface PlatformGraphQlFacade {

  /**
   * Выполняет GraphQL-операцию и преобразует поле {@code data} в заданный тип.
   *
   * @param operationName имя операции
   * @param variables переменные запроса
   * @param query GraphQL-запрос
   * @param dataClass целевой тип данных
   * @param <T> тип результата
   * @return данные ответа
   */
  <T> T execute(
      String operationName, Map<String, Object> variables, String query, Class<T> dataClass);
}
