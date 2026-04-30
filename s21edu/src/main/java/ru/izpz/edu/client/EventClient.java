package ru.izpz.edu.client;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.izpz.dto.model.EventV1DTO;
import ru.izpz.edu.exception.PlatformClientException;

/** Клиент получения событий платформы. */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventClient {

  private final PlatformApiFacade platformApi;

  /**
   * Возвращает список событий за интервал.
   *
   * @param from начало интервала
   * @param to конец интервала
   * @param type тип события
   * @param limit размер страницы
   * @param offset смещение
   * @return список событий
   */
  public List<EventV1DTO> getEvents(
      OffsetDateTime from, OffsetDateTime to, String type, Long limit, Long offset) {
    try {
      var response = platformApi.getEvents(from, to, type, limit, offset);
      if (response == null) {
        log.warn("API вернул пустой ответ для событий");
        throw new PlatformClientException("API вернул пустой ответ для событий", null);
      }
      return response.getEvents();
    } catch (RuntimeException e) {
      log.error("Неожиданная ошибка при получении событий", e);
      throw new PlatformClientException("Unexpected error while fetching events", e);
    }
  }
}
