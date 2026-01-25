package ru.izpz.edu.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiException;
import ru.izpz.edu.client.EventClient;
import ru.izpz.edu.service.EventService;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.scheduler.enabled", havingValue = "true")
public class EventScheduler {

    private final EventClient eventClient;
    private final EventService eventService;

    @Scheduled(fixedDelayString = "${event.scheduler.fixed-delay:PT1M}")
    public void scheduleEvents() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime week = now.plusDays(7);

        try {
            var events = eventClient.getEvents(now, week, null, 50L, 0L);
            eventService.saveEvents(events);
            log.info("События сохранены: {}", events.size());

        } catch (ApiException e) {
            log.error("Ошибка получения событий для кампуса", e);
        }
    }
}
