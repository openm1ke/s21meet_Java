package ru.izpz.edu.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.izpz.edu.client.BotClient;
import ru.izpz.dto.NotifyRequest;
import ru.izpz.dto.StatusChange;
import ru.izpz.edu.scheduler.metrics.TrackSchedulerMetrics;
import ru.izpz.edu.service.NotifyService;
import ru.izpz.edu.service.SchedulerMetricsService;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notify.scheduler.enabled", havingValue = "true")
public class NotifyScheduler {

    private static final String SCHEDULER_NAME = "notify_poller";

    private final NotifyService notifyService;
    private final BotClient botClient;
    private final SchedulerMetricsService schedulerMetricsService;

    @Scheduled(fixedDelayString = "${notify.scheduler.fixed-delay:PT30S}")
    @TrackSchedulerMetrics(scheduler = SCHEDULER_NAME, phase = "notify")
    public void poll() {
        long startedAt = System.nanoTime();
        log.info("Notify poller cycle started");
        List<StatusChange> changes = notifyService.computeAndPersistChanges();
        log.info("Notify poller: computed {} status changes", changes.size());

        long deliveries = changes.stream()
            .filter(Objects::nonNull)
            .map(StatusChange::telegramIds)
            .filter(Objects::nonNull)
            .mapToLong(List::size)
            .sum();
        long uniqueUsers = changes.stream()
            .filter(Objects::nonNull)
            .map(StatusChange::telegramIds)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .distinct()
            .count();

        log.info("Notify poller: recipients uniqueUsers={}, deliveries={}", uniqueUsers, deliveries);
        schedulerMetricsService.recordNotifyRecipients(SCHEDULER_NAME, uniqueUsers, deliveries);

        var request = NotifyRequest.builder()
                .changes(changes)
                .build();
        try {
            botClient.notify(request);
            log.info("Notify poller: request sent to bot service, changes={}", changes.size());
            schedulerMetricsService.recordExternalApiSuccess(SCHEDULER_NAME, "bot_api", "notify");
        } catch (Exception e) {
            schedulerMetricsService.recordExternalApiError(SCHEDULER_NAME, "bot_api", "notify", e);
            log.error("Непредвиденная ошибка в планировщике уведомлений", e);
            throw e;
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
            log.info("Notify poller cycle finished in {} ms", durationMs);
        }
    }
}
