package ru.izpz.edu.scheduler;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import ru.izpz.dto.ApiException;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.config.CampusSchedulerProperties;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.scheduler.metrics.SchedulerErrorReason;
import ru.izpz.edu.scheduler.metrics.SchedulerPhaseRequestStatus;
import ru.izpz.edu.scheduler.metrics.SchedulerRunStatus;
import ru.izpz.edu.service.CampusCatalog;
import ru.izpz.edu.service.CampusService;
import ru.izpz.edu.service.SchedulerMetricsService;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "campus.scheduler.enabled", havingValue = "true")
public class CampusScheduler {

    private final CampusClient campusClient;
    private final CampusService campusService;
    private final SchedulerMetricsService schedulerMetricsService;
    private final CampusCatalog campusCatalog;
    private final CampusSchedulerProperties schedulerProperties;
    @Qualifier("campusSchedulerExecutor")
    private final ExecutorService campusSchedulerExecutor;

    private static final String SCHEDULER_NAME = "campus_parser";

    @Scheduled(fixedDelayString = "${campus.scheduler.fixed-delay:PT30S}")
    public void parseMskKznNsk() {
        List<String> campuses = campusCatalog.targetCampusIds();
        
        log.info("Получение кластеров для Москвы, Казани и Новосибирска");
        StopWatch stopWatch = new StopWatch("campus");

        Timer.Sample clustersSample = schedulerMetricsService.startPhaseTimer();
        PhaseOutcome clustersOutcome;
        try {
            clustersOutcome = processClusters(campuses, stopWatch);
            if (!clustersOutcome.completed()) {
                log.error("Цикл парсинга прерван: превышено время фазы получения кластеров");
                schedulerMetricsService.recordRunStatus(SCHEDULER_NAME, SchedulerRunStatus.FAILED);
                return;
            }
        } finally {
            schedulerMetricsService.stopPhaseTimer(SCHEDULER_NAME, "clusters", clustersSample);
        }

        Timer.Sample participantsSample = schedulerMetricsService.startPhaseTimer();
        PhaseOutcome participantsOutcome;
        try {
            participantsOutcome = processParticipants(stopWatch);
            if (!participantsOutcome.completed()) {
                log.error("Цикл парсинга прерван: превышено время фазы получения участников");
                schedulerMetricsService.recordRunStatus(SCHEDULER_NAME, SchedulerRunStatus.FAILED);
                return;
            }
            campusService.refreshParticipantMetrics();
        } finally {
            schedulerMetricsService.stopPhaseTimer(SCHEDULER_NAME, "participants", participantsSample);
        }
        
        if (clustersOutcome.hasErrors() || participantsOutcome.hasErrors()) {
            schedulerMetricsService.recordRunStatus(SCHEDULER_NAME, SchedulerRunStatus.PARTIAL);
        } else {
            schedulerMetricsService.recordRunStatus(SCHEDULER_NAME, SchedulerRunStatus.SUCCESS);
            schedulerMetricsService.recordLastSuccess(SCHEDULER_NAME);
        }

        log.info("Данные участников из Москвы, Казани и Новосибирска по кластерам обновлены.");
        log.info("Время обновления: {}", stopWatch.prettyPrint());
    }

    private PhaseOutcome processClusters(List<String> campuses, StopWatch stopWatch) {
        Duration clustersTimeout = schedulerProperties.getTimeout().getClusters();
        stopWatch.start("get clusters");
        try {
            List<CompletableFuture<TaskResult>> futures = submitClusterTasks(campuses);
            return awaitPhaseResults("clusters", clustersTimeout, futures);
        } finally {
            stopWatch.stop();
        }
    }

    private List<CompletableFuture<TaskResult>> submitClusterTasks(List<String> campuses) {
        return campuses.stream()
            .map(id -> CompletableFuture.supplyAsync(() -> processSingleCampus(id), campusSchedulerExecutor))
            .toList();
    }

    private TaskResult processSingleCampus(String id) {
        try {
            var clusters = campusClient.getClustersByCampus(id);
            campusService.replaceClustersByCampusId(id, clusters);
            schedulerMetricsService.recordExternalApiSuccess(SCHEDULER_NAME, "campus_api", "get_clusters");
            return TaskResult.successResult();
        } catch (ApiException e) {
            schedulerMetricsService.recordExternalApiError(SCHEDULER_NAME, "campus_api", "get_clusters", e);
            log.error("Ошибка получения кластеров для кампуса {} ({})", campusCatalog.campusName(id), id, e);
            return TaskResult.errorResult(SchedulerErrorReason.API_EXCEPTION);
        } catch (Exception e) {
            schedulerMetricsService.recordExternalApiError(SCHEDULER_NAME, "campus_api", "get_clusters", e);
            log.error("Непредвиденная ошибка получения кластеров для кампуса {} ({})", campusCatalog.campusName(id), id, e);
            return TaskResult.errorResult(SchedulerErrorReason.EXECUTION_EXCEPTION);
        }
    }

    private PhaseOutcome processParticipants(StopWatch stopWatch) {
        Duration participantsTimeout = schedulerProperties.getTimeout().getParticipants();
        stopWatch.start("get participants");
        try {
            var clusterList = campusService.findAllByOrderByCampusIdAsc();
            List<CompletableFuture<TaskResult>> futures = submitParticipantTasks(clusterList);
            return awaitPhaseResults("participants", participantsTimeout, futures);
        } finally {
            stopWatch.stop();
        }
    }

    private List<CompletableFuture<TaskResult>> submitParticipantTasks(List<Cluster> clusterList) {
        return clusterList.stream()
            .map(c -> CompletableFuture.supplyAsync(() -> processSingleCluster(c), campusSchedulerExecutor))
            .toList();
    }

    private TaskResult processSingleCluster(Cluster c) {
        long cid = c.getClusterId();
        try {
            campusService.replaceParticipantsByClusterIdWithProvider(cid);
            schedulerMetricsService.recordExternalApiSuccess(SCHEDULER_NAME, "campus_api", "get_participants");
            log.info("Updated participants for cluster {} ({})", c.getName(), cid);
            return TaskResult.successResult();
        } catch (ApiException e) {
            schedulerMetricsService.recordExternalApiError(SCHEDULER_NAME, "campus_api", "get_participants", e);
            log.error("Ошибка получения участников для кластера {}", cid, e);
            return TaskResult.errorResult(SchedulerErrorReason.API_EXCEPTION);
        } catch (Exception e) {
            schedulerMetricsService.recordExternalApiError(SCHEDULER_NAME, "campus_api", "get_participants", e);
            log.error("Непредвиденная ошибка получения участников для кластера {}", cid, e);
            return TaskResult.errorResult(SchedulerErrorReason.EXECUTION_EXCEPTION);
        }
    }

    private PhaseOutcome awaitPhaseResults(String phase, Duration timeout, List<CompletableFuture<TaskResult>> futures) {
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        try {
            allOf.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            cancelPendingFutures(futures);
            schedulerMetricsService.recordPhaseIssue(SCHEDULER_NAME, phase, SchedulerErrorReason.TIMEOUT);
            log.error("Превышено время выполнения фазы {} ({} сек)", phase, timeout.toSeconds(), e);
            return new PhaseOutcome(false, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancelPendingFutures(futures);
            schedulerMetricsService.recordPhaseIssue(SCHEDULER_NAME, phase, SchedulerErrorReason.INTERRUPTED);
            log.error("Прервано ожидание результатов фазы {}", phase, e);
            return new PhaseOutcome(false, true);
        } catch (Exception e) {
            schedulerMetricsService.recordPhaseIssue(SCHEDULER_NAME, phase, SchedulerErrorReason.EXECUTION_EXCEPTION);
            log.error("Ошибка ожидания результатов фазы {}", phase, e);
            return new PhaseOutcome(false, true);
        }

        boolean hasErrors = false;
        for (CompletableFuture<TaskResult> future : futures) {
            TaskResult result = future.join();
            schedulerMetricsService.recordPhaseRequest(SCHEDULER_NAME, phase, result.status());
            if (!result.success()) {
                schedulerMetricsService.recordPhaseIssue(SCHEDULER_NAME, phase, result.errorType());
                hasErrors = true;
            }
        }
        return new PhaseOutcome(true, hasErrors);
    }

    private void cancelPendingFutures(List<CompletableFuture<TaskResult>> futures) {
        for (CompletableFuture<TaskResult> future : futures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }

    private record TaskResult(boolean success, SchedulerPhaseRequestStatus status, SchedulerErrorReason errorType) {
        private static TaskResult successResult() {
            return new TaskResult(true, SchedulerPhaseRequestStatus.SUCCESS, SchedulerErrorReason.NONE);
        }

        private static TaskResult errorResult(SchedulerErrorReason errorType) {
            return new TaskResult(false, SchedulerPhaseRequestStatus.FAILED, errorType);
        }
    }

    private record PhaseOutcome(boolean completed, boolean hasErrors) {
    }
}
