package ru.izpz.edu.scheduler;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.izpz.edu.client.EventClient;
import ru.izpz.edu.exception.PlatformClientException;
import ru.izpz.edu.scheduler.metrics.TrackSchedulerMetrics;
import ru.izpz.edu.service.EventService;
import ru.izpz.edu.service.SchedulerMetricsService;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.scheduler.enabled", havingValue = "true")
public class EventScheduler {

  private static final String SCHEDULER_NAME = "event_parser";
  private static final String EVENT_API_CLIENT = "event_api";
  private static final String OPERATION_GET_EVENTS = "get_events";

  private final EventClient eventClient;
  private final EventService eventService;
  private final SchedulerMetricsService schedulerMetricsService;

  @Scheduled(fixedDelayString = "${event.scheduler.fixed-delay:PT1M}")
  @TrackSchedulerMetrics(scheduler = SCHEDULER_NAME, phase = OPERATION_GET_EVENTS)
  public void scheduleEvents() {
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime week = now.plusDays(7);
    var events = fetchEvents(now, week);

    var saveStats = eventService.saveEvents(events);
    schedulerMetricsService.recordEventsSaved(
        SCHEDULER_NAME, saveStats.created(), saveStats.updated());
    log.info(
        "События сохранены: {} (новых: {}, обновлено: {})",
        events.size(),
        saveStats.created(),
        saveStats.updated());
  }

  private java.util.List<ru.izpz.dto.model.EventV1DTO> fetchEvents(
      OffsetDateTime from, OffsetDateTime to) {
    try {
      var events = eventClient.getEvents(from, to, null, 50L, 0L);
      schedulerMetricsService.recordExternalApiSuccess(
          SCHEDULER_NAME, EVENT_API_CLIENT, OPERATION_GET_EVENTS);
      return events;
    } catch (PlatformClientException e) {
      schedulerMetricsService.recordExternalApiError(
          SCHEDULER_NAME, EVENT_API_CLIENT, OPERATION_GET_EVENTS, e);
      log.error("Ошибка получения событий для кампуса", e);
      throw e;
    } catch (RuntimeException e) {
      schedulerMetricsService.recordExternalApiError(
          SCHEDULER_NAME, EVENT_API_CLIENT, OPERATION_GET_EVENTS, e);
      log.error("Непредвиденная ошибка вызова event API", e);
      throw e;
    }
  }
}
