package ru.izpz.edu.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiException;
import ru.izpz.edu.client.EventClient;
import ru.izpz.edu.scheduler.metrics.TrackSchedulerMetrics;
import ru.izpz.edu.service.EventService;
import ru.izpz.edu.service.SchedulerMetricsService;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.scheduler.enabled", havingValue = "true")
public class EventScheduler {

    private static final String SCHEDULER_NAME = "event_parser";

    private final EventClient eventClient;
    private final EventService eventService;
    private final SchedulerMetricsService schedulerMetricsService;

    @Scheduled(fixedDelayString = "${event.scheduler.fixed-delay:PT1M}")
    @TrackSchedulerMetrics(scheduler = SCHEDULER_NAME, phase = "get_events")
    public void scheduleEvents() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime week = now.plusDays(7);
        var events = fetchEvents(now, week);

        var saveStats = eventService.saveEvents(events);
        if (saveStats != null) {
            schedulerMetricsService.recordEventsSaved(SCHEDULER_NAME, saveStats.created(), saveStats.updated());
        }
        log.info("События сохранены: {} (новых: {}, обновлено: {})",
            events.size(),
            saveStats != null ? saveStats.created() : 0,
            saveStats != null ? saveStats.updated() : 0
        );
    }

    private java.util.List<ru.izpz.dto.model.EventV1DTO> fetchEvents(OffsetDateTime from, OffsetDateTime to) throws Exception {
        try {
            var events = eventClient.getEvents(from, to, null, 50L, 0L);
            schedulerMetricsService.recordExternalApiSuccess(SCHEDULER_NAME, "event_api", "get_events");
            return events;
        } catch (ApiException e) {
            schedulerMetricsService.recordExternalApiError(SCHEDULER_NAME, "event_api", "get_events", e);
            log.error("Ошибка получения событий для кампуса", e);
            throw e;
        } catch (Exception e) {
            schedulerMetricsService.recordExternalApiError(SCHEDULER_NAME, "event_api", "get_events", e);
            log.error("Непредвиденная ошибка вызова event API", e);
            throw e;
        }
    }
}
