package ru.izpz.edu.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.izpz.edu.config.ProjectsProviderConfig;
import ru.izpz.edu.model.StudentCredentials;
import ru.izpz.edu.repository.StudentCredentialsRepository;
import ru.izpz.edu.scheduler.metrics.TrackSchedulerMetrics;
import ru.izpz.edu.service.CampusCatalog;
import ru.izpz.edu.service.provider.CampusRoutingProjectsProvider;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"projects.scheduler.enabled", "profile.service.enabled"}, havingValue = "true")
public class StudentProjectsScheduler {

    private static final String SCHEDULER_NAME = "projects_sync";
    private static final String PHASE_NAME = "refresh_active_projects";

    private final StudentCredentialsRepository studentCredentialsRepository;
    private final CampusRoutingProjectsProvider campusRoutingProjectsProvider;
    private final CampusCatalog campusCatalog;
    private final ProjectsProviderConfig.ProjectsProperties projectsProperties;
    @Qualifier("projectsSchedulerExecutor")
    private final ExecutorService projectsSchedulerExecutor;

    @Value("${projects.scheduler.page-size:1000}")
    private int pageSize;
    @Value("${projects.scheduler.batch-size:120}")
    private int batchSize;
    @Value("${projects.scheduler.graphql-batch-size:${projects.scheduler.batch-size:120}}")
    private int graphQlBatchSize;
    @Value("${projects.scheduler.rest-batch-size:${projects.scheduler.batch-size:120}}")
    private int restBatchSize;
    @Value("${projects.scheduler.fixed-delay:PT4H}")
    private Duration fixedDelay;
    @Value("${projects.scheduler.task-timeout:PT20S}")
    private Duration taskTimeout = Duration.ofSeconds(20);
    @Value("${projects.scheduler.concurrency:4}")
    private int concurrency;
    @Value("${projects.scheduler.retry-attempts:2}")
    private int retryAttempts;
    @Value("${projects.scheduler.retry-backoff:PT1S}")
    private Duration retryBackoff = Duration.ofSeconds(1);

    @PostConstruct
    void logSchedulerInit() {
        log.info(
            "projects scheduler initialized: fixed_delay={}, page_size={}, batch_size={}, graphql_batch_size={}, rest_batch_size={}, concurrency={}, task_timeout={}s, retry_attempts={}, retry_backoff={}ms, refresh_ttl={}m",
            fixedDelay,
            pageSize,
            batchSize,
            graphQlBatchSize,
            restBatchSize,
            concurrency,
            taskTimeout.toSeconds(),
            Math.max(1, retryAttempts),
            Math.max(0, retryBackoff.toMillis()),
            projectsProperties.getRefreshTtl().toMinutes()
        );
    }

    @Scheduled(fixedDelayString = "${projects.scheduler.fixed-delay:PT4H}")
    @TrackSchedulerMetrics(scheduler = SCHEDULER_NAME, phase = PHASE_NAME)
    public void refreshActiveProjects() {
        if (pageSize <= 0 || batchSize <= 0 || graphQlBatchSize <= 0 || restBatchSize <= 0) {
            log.warn(
                "projects scheduler пропущен: page-size={}, batch-size={}, graphql-batch-size={}, rest-batch-size={}",
                pageSize,
                batchSize,
                graphQlBatchSize,
                restBatchSize
            );
            return;
        }

        OffsetDateTime staleBefore = OffsetDateTime.now().minus(projectsProperties.getRefreshTtl());
        int scanned = 0;
        int success = 0;
        int skippedNoProvider = 0;
        int failed = 0;
        int timedOut = 0;
        Instant startedAt = Instant.now();
        List<String> campuses = campusCatalog.targetCampusIds();

        log.info(
            "projects scheduler started: campuses={}, page_size={}, batch_size={}, graphql_batch_size={}, rest_batch_size={}, concurrency={}, task_timeout={}s, retry_attempts={}, retry_backoff={}ms, refresh_ttl={}m",
            campuses.size(),
            pageSize,
            batchSize,
            graphQlBatchSize,
            restBatchSize,
            concurrency,
            taskTimeout.toSeconds(),
            Math.max(1, retryAttempts),
            Math.max(0, retryBackoff.toMillis()),
            projectsProperties.getRefreshTtl().toMinutes()
        );

        for (String schoolId : campuses) {
            String campusName = campusCatalog.campusName(schoolId);
            String providerType = campusRoutingProjectsProvider.providerTypeForSchoolId(schoolId);
            int campusBatchSize = batchSizeForProvider(providerType);
            long campusActiveTotal = studentCredentialsRepository.countActiveCredentialsBySchoolId(schoolId);
            long campusStaleTotalInitial = studentCredentialsRepository.countStaleActiveCredentialsBySchoolId(schoolId, staleBefore);
            String cursor = "";
            int pageNumber = 0;
            int campusScanned = 0;
            int campusSuccess = 0;
            int campusSkippedNoProvider = 0;
            int campusFailed = 0;
            int campusTimedOut = 0;

            log.info(
                "projects scheduler campus started: campus={}, provider={}, batch_size={}, active_total={}, stale_total_initial={}, remaining_to_update={}",
                campusName,
                providerType,
                campusBatchSize,
                campusActiveTotal,
                campusStaleTotalInitial,
                campusStaleTotalInitial
            );

            while (true) {
                List<StudentCredentials> staleCredentials = studentCredentialsRepository.findStaleActiveCredentialsAfterBySchoolId(
                    cursor,
                    schoolId,
                    staleBefore,
                    PageRequest.of(0, pageSize)
                );
                if (staleCredentials.isEmpty()) {
                    break;
                }

                pageNumber++;
                campusScanned += staleCredentials.size();
                BatchCounters counters = processPage(staleCredentials, pageNumber, campusBatchSize);
                campusSuccess += counters.success();
                campusSkippedNoProvider += counters.skippedNoProvider();
                campusFailed += counters.failed();
                campusTimedOut += counters.timedOut();
                cursor = staleCredentials.getLast().getLogin();
                long campusProcessedTotal = (long) campusSuccess + campusSkippedNoProvider + campusFailed + campusTimedOut;
                long campusRemainingToProcess = Math.max(campusStaleTotalInitial - campusProcessedTotal, 0);
                long campusRemainingSuccessTarget = Math.max(campusStaleTotalInitial - campusSuccess, 0);

                log.info(
                    "projects scheduler campus progress: campus={}, page={}, stale_scanned={}, processed_total={}, success={}, skipped_no_provider={}, failed={}, timeout={}, remaining_to_process={}, remaining_success_target={}, next_cursor={}",
                    campusName,
                    pageNumber,
                    campusScanned,
                    campusProcessedTotal,
                    campusSuccess,
                    campusSkippedNoProvider,
                    campusFailed,
                    campusTimedOut,
                    campusRemainingToProcess,
                    campusRemainingSuccessTarget,
                    cursor
                );
            }

            scanned += campusScanned;
            success += campusSuccess;
            skippedNoProvider += campusSkippedNoProvider;
            failed += campusFailed;
            timedOut += campusTimedOut;
            long campusSkippedFresh = Math.max(campusActiveTotal - campusScanned, 0);
            long campusProcessedTotal = (long) campusSuccess + campusSkippedNoProvider + campusFailed + campusTimedOut;
            long campusRemainingToProcess = Math.max(campusStaleTotalInitial - campusProcessedTotal, 0);
            long campusRemainingSuccessTarget = Math.max(campusStaleTotalInitial - campusSuccess, 0);

            log.info(
                "projects scheduler campus done: campus={}, provider={}, active_total={}, stale_total_initial={}, stale_scanned={}, processed_total={}, skipped_fresh={}, success={}, skipped_no_provider={}, failed={}, timeout={}, remaining_to_process={}, remaining_success_target={}",
                campusName,
                providerType,
                campusActiveTotal,
                campusStaleTotalInitial,
                campusScanned,
                campusProcessedTotal,
                campusSkippedFresh,
                campusSuccess,
                campusSkippedNoProvider,
                campusFailed,
                campusTimedOut,
                campusRemainingToProcess,
                campusRemainingSuccessTarget
            );
        }

        Duration elapsed = Duration.between(startedAt, Instant.now());
        log.info(
            "projects scheduler завершен: stale_scanned={}, success={}, skipped_no_provider={}, failed={}, timeout={}, ttl={}m, elapsed={}s",
            scanned,
            success,
            skippedNoProvider,
            failed,
            timedOut,
            projectsProperties.getRefreshTtl().toMinutes(),
            elapsed.toSeconds()
        );
    }

    private BatchCounters processPage(List<StudentCredentials> credentials, int pageNumber, int currentBatchSize) {
        BatchCounters totals = BatchCounters.empty();
        int effectiveBatchSize = Math.max(1, currentBatchSize);
        int totalBatches = (credentials.size() + effectiveBatchSize - 1) / effectiveBatchSize;
        int maxAttempts = Math.max(1, retryAttempts);

        for (int i = 0; i < credentials.size(); i += effectiveBatchSize) {
            int batchNo = (i / effectiveBatchSize) + 1;
            int end = Math.min(i + effectiveBatchSize, credentials.size());
            List<StudentCredentials> batch = credentials.subList(i, end);
            BatchExecutionResult batchExecution = processBatchWithRetries(batch, pageNumber, batchNo, maxAttempts);
            totals = totals.plus(batchExecution.counters());
            logBatchProgress(pageNumber, batchNo, totalBatches, batch.size(), batchExecution);
        }
        return totals;
    }

    private BatchExecutionResult processBatchWithRetries(
        List<StudentCredentials> batch,
        int pageNumber,
        int batchNo,
        int maxAttempts
    ) {
        Instant batchStartedAt = Instant.now();
        BatchCounters totals = BatchCounters.empty();
        List<StudentCredentials> attemptCredentials = new ArrayList<>(batch);
        for (int attempt = 1; attempt <= maxAttempts && !attemptCredentials.isEmpty(); attempt++) {
            AttemptExecutionResult attemptResult = executeAttempt(attemptCredentials, pageNumber, batchNo, attempt, maxAttempts);
            totals = totals.plus(attemptResult.counters());
            attemptCredentials = attemptResult.nextAttempt();
            if (!delayBeforeNextAttempt(pageNumber, batchNo, attemptCredentials.size(), attempt, maxAttempts)) {
                break;
            }
        }
        long batchElapsedMs = Duration.between(batchStartedAt, Instant.now()).toMillis();
        return new BatchExecutionResult(totals, batchElapsedMs);
    }

    private AttemptExecutionResult executeAttempt(
        List<StudentCredentials> attemptCredentials,
        int pageNumber,
        int batchNo,
        int attempt,
        int maxAttempts
    ) {
        List<StudentCredentials> nextAttempt = new ArrayList<>();
        BatchCounters totals = BatchCounters.empty();
        List<Future<RefreshResult>> futures = new ArrayList<>(attemptCredentials.size());
        for (StudentCredentials studentCredentials : attemptCredentials) {
            futures.add(projectsSchedulerExecutor.submit(() -> refreshProjects(studentCredentials)));
        }
        for (int idx = 0; idx < futures.size(); idx++) {
            StudentCredentials studentCredentials = attemptCredentials.get(idx);
            Future<RefreshResult> future = futures.get(idx);
            RefreshResult result = getTaskResult(studentCredentials, future, pageNumber, batchNo, attempt);
            if (isRetryableResult(result) && attempt < maxAttempts) {
                nextAttempt.add(studentCredentials);
            } else {
                totals = totals.plus(toCounters(result));
            }
        }
        return new AttemptExecutionResult(totals, nextAttempt);
    }

    private boolean delayBeforeNextAttempt(
        int pageNumber,
        int batchNo,
        int pendingRetries,
        int attempt,
        int maxAttempts
    ) {
        if (pendingRetries == 0 || attempt >= maxAttempts) {
            return true;
        }
        long backoffMs = Math.max(0, retryBackoff.toMillis());
        if (backoffMs <= 0) {
            return true;
        }
        try {
            Thread.sleep(backoffMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn(
                "projects scheduler retry backoff interrupted: page={}, batch={}, pending_retries={}",
                pageNumber,
                batchNo,
                pendingRetries
            );
            return false;
        }
    }

    private boolean isRetryableResult(RefreshResult result) {
        return result == RefreshResult.FAILED || result == RefreshResult.TIMEOUT;
    }

    private BatchCounters toCounters(RefreshResult result) {
        return switch (result) {
            case SUCCESS -> new BatchCounters(1, 0, 0, 0);
            case SKIPPED_NO_PROVIDER -> new BatchCounters(0, 1, 0, 0);
            case FAILED -> new BatchCounters(0, 0, 1, 0);
            case TIMEOUT -> new BatchCounters(0, 0, 0, 1);
        };
    }

    private void logBatchProgress(
        int pageNumber,
        int batchNo,
        int totalBatches,
        int batchSize,
        BatchExecutionResult execution
    ) {
        BatchCounters counters = execution.counters();
        if (counters.failed() > 0 || counters.timedOut() > 0) {
            log.warn(
                "projects scheduler batch processed: page={}, batch={}, size={}, success={}, skipped_no_provider={}, failed={}, timeout={}, elapsed_ms={}",
                pageNumber,
                batchNo,
                batchSize,
                counters.success(),
                counters.skippedNoProvider(),
                counters.failed(),
                counters.timedOut(),
                execution.elapsedMs()
            );
            return;
        }
        if (batchNo == 1 || batchNo == totalBatches || batchNo % 10 == 0) {
            log.info(
                "projects scheduler batch progress: page={}, batch={}/{}, size={}, success={}, skipped_no_provider={}, failed={}, timeout={}, elapsed_ms={}",
                pageNumber,
                batchNo,
                totalBatches,
                batchSize,
                counters.success(),
                counters.skippedNoProvider(),
                counters.failed(),
                counters.timedOut(),
                execution.elapsedMs()
            );
            return;
        }
        log.debug(
            "projects scheduler batch processed: page={}, batch={}/{}, size={}, success={}, skipped_no_provider={}, failed={}, timeout={}, elapsed_ms={}",
            pageNumber,
            batchNo,
            totalBatches,
            batchSize,
            counters.success(),
            counters.skippedNoProvider(),
            counters.failed(),
            counters.timedOut(),
            execution.elapsedMs()
        );
    }

    private RefreshResult getTaskResult(
        StudentCredentials credentials,
        Future<RefreshResult> future,
        int pageNumber,
        int batchNo,
        int attempt
    ) {
        String login = credentials.getLogin();
        try {
            return future.get(taskTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn(
                "projects scheduler task timeout: campus={}, page={}, batch={}, attempt={}/{}, login={}, timeout_ms={}",
                campusCatalog.campusName(credentials.getSchoolId()),
                pageNumber,
                batchNo,
                attempt,
                Math.max(1, retryAttempts),
                login,
                taskTimeout.toMillis()
            );
            return RefreshResult.TIMEOUT;
        } catch (ExecutionException e) {
            log.warn(
                "projects scheduler task failed: campus={}, page={}, batch={}, attempt={}/{}, login={}, error={}",
                campusCatalog.campusName(credentials.getSchoolId()),
                pageNumber,
                batchNo,
                attempt,
                Math.max(1, retryAttempts),
                login,
                e.getCause() == null ? e.getMessage() : e.getCause().getMessage()
            );
            return RefreshResult.FAILED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn(
                "projects scheduler task interrupted: campus={}, page={}, batch={}, attempt={}/{}, login={}",
                campusCatalog.campusName(credentials.getSchoolId()),
                pageNumber,
                batchNo,
                attempt,
                Math.max(1, retryAttempts),
                login
            );
            return RefreshResult.FAILED;
        }
    }

    private int batchSizeForProvider(String providerType) {
        if ("graphql".equalsIgnoreCase(providerType)) {
            return graphQlBatchSize;
        }
        return restBatchSize;
    }

    private RefreshResult refreshProjects(StudentCredentials credentials) {
        String login = credentials.getLogin();
        try {
            return switch (campusRoutingProjectsProvider.refreshStudentProjects(credentials)) {
                case SUCCESS -> RefreshResult.SUCCESS;
                case SKIPPED_NO_PROVIDER -> RefreshResult.SKIPPED_NO_PROVIDER;
            };
        } catch (RuntimeException e) {
            log.warn("Ошибка refresh projects для {}: {}", login, e.getMessage());
            return RefreshResult.FAILED;
        }
    }

    private enum RefreshResult {
        SUCCESS,
        SKIPPED_NO_PROVIDER,
        FAILED,
        TIMEOUT
    }

    private record BatchCounters(int success, int skippedNoProvider, int failed, int timedOut) {
        private static BatchCounters empty() {
            return new BatchCounters(0, 0, 0, 0);
        }

        private BatchCounters plus(BatchCounters other) {
            return new BatchCounters(
                success + other.success,
                skippedNoProvider + other.skippedNoProvider,
                failed + other.failed,
                timedOut + other.timedOut
            );
        }
    }

    private record AttemptExecutionResult(BatchCounters counters, List<StudentCredentials> nextAttempt) {
    }

    private record BatchExecutionResult(BatchCounters counters, long elapsedMs) {
    }
}
