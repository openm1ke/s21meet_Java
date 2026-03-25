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
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.config.CampusSchedulerProperties;
import ru.izpz.edu.model.Workplace;
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
    private static final String CAMPUS_API_CLIENT = "campus_api";
    private static final String PHASE_CAMPUS_SNAPSHOT_REFRESH = "campus_snapshot_refresh";
    private static final String OPERATION_GET_CLUSTERS = "get_clusters";
    private static final String OPERATION_GET_PARTICIPANTS = "get_participants";

    @Scheduled(fixedDelayString = "${campus.scheduler.fixed-delay:PT30S}")
    public void parseMskKznNsk() {
        List<String> campuses = campusCatalog.targetCampusIds();
        
        log.info("Обновление снапшота кампусов для Москвы, Казани и Новосибирска");
        StopWatch stopWatch = new StopWatch("campus");

        Timer.Sample clustersSample = schedulerMetricsService.startPhaseTimer();
        PhaseOutcome clustersOutcome;
        try {
            clustersOutcome = processClusters(campuses, stopWatch);
        } finally {
            schedulerMetricsService.stopPhaseTimer(SCHEDULER_NAME, PHASE_CAMPUS_SNAPSHOT_REFRESH, clustersSample);
        }
        if (!clustersOutcome.completed()) {
            if (clustersOutcome.hasSuccessfulCampuses()) {
                campusService.refreshParticipantMetrics();
                schedulerMetricsService.recordRunStatus(SCHEDULER_NAME, SchedulerRunStatus.PARTIAL);
                log.warn("Цикл парсинга завершен частично: часть кампусов обновлена, часть не успела до таймаута");
            } else {
                schedulerMetricsService.recordRunStatus(SCHEDULER_NAME, SchedulerRunStatus.FAILED);
                log.error("Цикл парсинга прерван: превышено время обновления снапшота кампусов");
            }
            return;
        }
        campusService.refreshParticipantMetrics();
        
        if (clustersOutcome.hasErrors()) {
            schedulerMetricsService.recordRunStatus(SCHEDULER_NAME, SchedulerRunStatus.PARTIAL);
        } else {
            schedulerMetricsService.recordRunStatus(SCHEDULER_NAME, SchedulerRunStatus.SUCCESS);
            schedulerMetricsService.recordLastSuccess(SCHEDULER_NAME);
        }

        log.info("Данные участников из Москвы, Казани и Новосибирска по кластерам обновлены.");
        log.info("Время обновления: {}", stopWatch.prettyPrint());
    }

    private PhaseOutcome processClusters(List<String> campuses, StopWatch stopWatch) {
        Duration clustersTimeout = schedulerProperties.getTimeout().getGlobal();
        Duration campusTimeout = schedulerProperties.getTimeout().getPerCampus();
        stopWatch.start(PHASE_CAMPUS_SNAPSHOT_REFRESH);
        try {
            List<CompletableFuture<TaskResult>> futures = submitClusterTasks(campuses, campusTimeout);
            return awaitPhaseResults(PHASE_CAMPUS_SNAPSHOT_REFRESH, clustersTimeout, futures);
        } finally {
            stopWatch.stop();
        }
    }

    private List<CompletableFuture<TaskResult>> submitClusterTasks(List<String> campuses, Duration campusTimeout) {
        return campuses.stream()
            .map(id -> CompletableFuture.supplyAsync(() -> processSingleCampus(id), campusSchedulerExecutor)
                .completeOnTimeout(
                    TaskResult.timeoutResult(),
                    campusTimeout.toMillis(),
                    TimeUnit.MILLISECONDS
                )
                .thenApply(result -> {
                    if (result.errorType() == SchedulerErrorReason.TIMEOUT) {
                        log.error(
                            "Превышено время обновления кампуса {} ({}) ({} сек)",
                            campusCatalog.campusName(id),
                            id,
                            campusTimeout.toSeconds()
                        );
                    }
                    return result;
                }))
            .toList();
    }

    private TaskResult processSingleCampus(String id) {
        return processSingleCampusParticipants(id);
    }

    private TaskResult processSingleCampusParticipants(String campusId) {
        try {
            var clusters = campusClient.getClustersByCampus(campusId);
            schedulerMetricsService.recordExternalApiSuccess(SCHEDULER_NAME, CAMPUS_API_CLIENT, OPERATION_GET_CLUSTERS);
            return processSingleCampusParticipants(campusId, clusters);
        } catch (ApiException e) {
            schedulerMetricsService.recordExternalApiError(SCHEDULER_NAME, CAMPUS_API_CLIENT, OPERATION_GET_CLUSTERS, e);
            log.error("Ошибка получения кластеров для кампуса {} ({})", campusCatalog.campusName(campusId), campusId, e);
            return TaskResult.errorResult(SchedulerErrorReason.API_EXCEPTION);
        } catch (Exception e) {
            schedulerMetricsService.recordExternalApiError(SCHEDULER_NAME, CAMPUS_API_CLIENT, OPERATION_GET_CLUSTERS, e);
            log.error("Непредвиденная ошибка получения кластеров для кампуса {} ({})", campusCatalog.campusName(campusId), campusId, e);
            return TaskResult.errorResult(SchedulerErrorReason.EXECUTION_EXCEPTION);
        }
    }

    private TaskResult processSingleCampusParticipants(String campusId, List<ClusterV1DTO> clusters) {
        List<Workplace> aggregated = new java.util.ArrayList<>();
        for (ClusterV1DTO cluster : clusters) {
            TaskResult clusterResult = processSingleCluster(cluster, aggregated);
            if (!clusterResult.success()) {
                return clusterResult;
            }
        }
        try {
            campusService.replaceCampusSnapshotByCampusId(campusId, clusters, aggregated);
            return TaskResult.successResult();
        } catch (Exception e) {
            log.error("Непредвиденная ошибка сохранения снапшота кампуса {} ({})", campusCatalog.campusName(campusId), campusId, e);
            return TaskResult.errorResult(SchedulerErrorReason.EXECUTION_EXCEPTION);
        }
    }

    private TaskResult processSingleCluster(ClusterV1DTO c, List<Workplace> accumulator) {
        long cid = c.getId();
        try {
            accumulator.addAll(campusService.fetchParticipantsByClusterWithProvider(cid));
            schedulerMetricsService.recordExternalApiSuccess(SCHEDULER_NAME, CAMPUS_API_CLIENT, OPERATION_GET_PARTICIPANTS);
            log.info("Updated participants for cluster {} ({})", c.getName(), cid);
            return TaskResult.successResult();
        } catch (ApiException e) {
            schedulerMetricsService.recordExternalApiError(SCHEDULER_NAME, CAMPUS_API_CLIENT, OPERATION_GET_PARTICIPANTS, e);
            log.error("Ошибка получения участников для кластера {}", cid, e);
            return TaskResult.errorResult(SchedulerErrorReason.API_EXCEPTION);
        } catch (Exception e) {
            schedulerMetricsService.recordExternalApiError(SCHEDULER_NAME, CAMPUS_API_CLIENT, OPERATION_GET_PARTICIPANTS, e);
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
            return buildPhaseOutcome(false, futures, phase);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancelPendingFutures(futures);
            schedulerMetricsService.recordPhaseIssue(SCHEDULER_NAME, phase, SchedulerErrorReason.INTERRUPTED);
            log.error("Прервано ожидание результатов фазы {}", phase, e);
            return buildPhaseOutcome(false, futures, phase);
        } catch (Exception e) {
            schedulerMetricsService.recordPhaseIssue(SCHEDULER_NAME, phase, SchedulerErrorReason.EXECUTION_EXCEPTION);
            log.error("Ошибка ожидания результатов фазы {}", phase, e);
            return buildPhaseOutcome(false, futures, phase);
        }
        return buildPhaseOutcome(true, futures, phase);
    }

    private PhaseOutcome buildPhaseOutcome(boolean completed, List<CompletableFuture<TaskResult>> futures, String phase) {
        boolean hasErrors = !completed;
        boolean hasSuccessfulCampuses = false;
        for (CompletableFuture<TaskResult> future : futures) {
            if (future.isDone() && !future.isCancelled()) {
                try {
                    TaskResult result = future.join();
                    schedulerMetricsService.recordPhaseRequest(SCHEDULER_NAME, phase, result.status());
                    if (!result.success()) {
                        schedulerMetricsService.recordPhaseIssue(SCHEDULER_NAME, phase, result.errorType());
                        hasErrors = true;
                    } else {
                        hasSuccessfulCampuses = true;
                    }
                } catch (Exception e) {
                    schedulerMetricsService.recordPhaseIssue(SCHEDULER_NAME, phase, SchedulerErrorReason.EXECUTION_EXCEPTION);
                    hasErrors = true;
                }
            } else {
                hasErrors = true;
            }
        }
        return new PhaseOutcome(completed, hasErrors, hasSuccessfulCampuses);
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

        private static TaskResult timeoutResult() {
            return new TaskResult(false, SchedulerPhaseRequestStatus.FAILED, SchedulerErrorReason.TIMEOUT);
        }

        private static TaskResult errorResult(SchedulerErrorReason errorType) {
            return new TaskResult(false, SchedulerPhaseRequestStatus.FAILED, errorType);
        }
    }

    private record PhaseOutcome(boolean completed, boolean hasErrors, boolean hasSuccessfulCampuses) {
    }
}
