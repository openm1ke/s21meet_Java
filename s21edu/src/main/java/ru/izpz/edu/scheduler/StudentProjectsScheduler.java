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
    private static final String UNLIMITED = "unlimited";

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
    @Value("${projects.scheduler.max-logins-per-run:2500}")
    private int maxLoginsPerRun;

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

    @Scheduled(
        initialDelayString = "${projects.scheduler.initial-delay:PT0S}",
        fixedDelayString = "${projects.scheduler.fixed-delay:PT1H}"
    )
    @TrackSchedulerMetrics(scheduler = SCHEDULER_NAME, phase = PHASE_NAME)
    public void refreshActiveProjects() {
        if (hasInvalidConfig()) {
            logInvalidConfig();
            return;
        }

        Instant startedAt = Instant.now();
        OffsetDateTime staleBefore = OffsetDateTime.now().minus(projectsProperties.getRefreshTtl());
        List<String> campuses = campusCatalog.targetCampusIds();
        int remainingRunBudget = maxLoginsPerRun <= 0 ? Integer.MAX_VALUE : maxLoginsPerRun;
        SyncCounters totals = SyncCounters.empty();

        logSchedulerStart(campuses.size());

        for (String schoolId : campuses) {
            if (remainingRunBudget <= 0) {
                log.info(
                    "projects scheduler run budget exhausted: max_logins_per_run={}",
                    maxLoginsPerRun
                );
                break;
            }
            CampusRunResult campusRunResult = processCampus(schoolId, staleBefore, remainingRunBudget);
            remainingRunBudget = campusRunResult.remainingRunBudget();
            totals = totals.plus(campusRunResult.counters());
        }

        Duration elapsed = Duration.between(startedAt, Instant.now());
        long processedTotal = totals.processedTotal();
        int runBudgetUsed = maxLoginsPerRun <= 0 ? totals.scanned() : Math.clamp(totals.scanned(), 0, maxLoginsPerRun);
        int runBudgetRemaining = maxLoginsPerRun <= 0 ? Integer.MAX_VALUE : Math.max(maxLoginsPerRun - runBudgetUsed, 0);
        double successRate = percent(totals.success(), processedTotal);
        double throughputPerSecond = elapsed.toMillis() <= 0 ? 0.0 : (processedTotal * 1000.0) / elapsed.toMillis();
        log.info(
            "projects scheduler done: stale_scanned={}, processed_total={}, success={}, success_rate={}%, skipped_no_provider={}, failed={}, timeout={}, throughput_logins_per_sec={}, ttl={}m, elapsed={}s, run_budget_used={}, run_budget_remaining={}",
            totals.scanned(),
            processedTotal,
            totals.success(),
            formatPercent(successRate),
            totals.skippedNoProvider(),
            totals.failed(),
            totals.timedOut(),
            formatPercent(throughputPerSecond),
            projectsProperties.getRefreshTtl().toMinutes(),
            elapsed.toSeconds(),
            formatRunBudgetValue(runBudgetUsed),
            formatRunBudgetValue(runBudgetRemaining)
        );
    }

    private boolean hasInvalidConfig() {
        return pageSize <= 0 || batchSize <= 0 || graphQlBatchSize <= 0 || restBatchSize <= 0;
    }

    private void logInvalidConfig() {
        log.warn(
            "projects scheduler пропущен: page-size={}, batch-size={}, graphql-batch-size={}, rest-batch-size={}",
            pageSize,
            batchSize,
            graphQlBatchSize,
            restBatchSize
        );
    }

    private void logSchedulerStart(int campusesCount) {
        log.info(
            "projects scheduler started: campuses={}, page_size={}, batch_size={}, graphql_batch_size={}, rest_batch_size={}, concurrency={}, task_timeout={}s, retry_attempts={}, retry_backoff={}ms, refresh_ttl={}m, max_logins_per_run={}",
            campusesCount,
            pageSize,
            batchSize,
            graphQlBatchSize,
            restBatchSize,
            concurrency,
            taskTimeout.toSeconds(),
            Math.max(1, retryAttempts),
            Math.max(0, retryBackoff.toMillis()),
            projectsProperties.getRefreshTtl().toMinutes(),
            formatRunBudgetValue(maxLoginsPerRun)
        );
    }

    private CampusRunResult processCampus(String schoolId, OffsetDateTime staleBefore, int initialRunBudget) {
        String campusName = campusCatalog.campusName(schoolId);
        String providerType = campusRoutingProjectsProvider.providerTypeForSchoolId();
        int campusBatchSize = batchSizeForProvider(providerType);
        long campusActiveTotal = studentCredentialsRepository.countActiveCredentialsBySchoolId(schoolId);
        long campusStaleTotalInitial = studentCredentialsRepository.countStaleActiveCredentialsBySchoolId(schoolId, staleBefore);
        int remainingRunBudget = initialRunBudget;
        String cursor = "";
        int pageNumber = 0;
        SyncCounters counters = SyncCounters.empty();
        boolean hasMorePages = true;

        log.info(
            "projects scheduler campus started: campus={}, provider={}, batch_size={}, active_total={}, stale_total_initial={}, remaining_to_update={}, run_budget_remaining={}",
            campusName,
            providerType,
            campusBatchSize,
            campusActiveTotal,
            campusStaleTotalInitial,
            campusStaleTotalInitial,
            formatRunBudgetValue(remainingRunBudget)
        );

        if (remainingRunBudget < campusStaleTotalInitial) {
            log.info(
                "projects scheduler campus may require multiple runs: campus={}, stale_total_initial={}, run_budget_remaining={}",
                campusName,
                campusStaleTotalInitial,
                formatRunBudgetValue(remainingRunBudget)
            );
        }

        while (remainingRunBudget > 0 && hasMorePages) {
            int fetchPageSize = Math.min(pageSize, remainingRunBudget);
            List<StudentCredentials> staleCredentials = studentCredentialsRepository.findStaleActiveCredentialsAfterBySchoolId(
                cursor,
                schoolId,
                staleBefore,
                PageRequest.of(0, fetchPageSize)
            );
            hasMorePages = !staleCredentials.isEmpty();
            if (!hasMorePages) {
                continue;
            }

            pageNumber++;
            remainingRunBudget = Math.max(remainingRunBudget - staleCredentials.size(), 0);
            log.info(
                "projects scheduler page started: campus={}, page={}, taken_logins={}, run_budget_remaining_after_take={}",
                campusName,
                pageNumber,
                staleCredentials.size(),
                formatRunBudgetValue(remainingRunBudget)
            );

            BatchCounters pageCounters = processPage(staleCredentials, pageNumber, campusBatchSize);
            counters = counters.plus(new SyncCounters(
                staleCredentials.size(),
                pageCounters.success(),
                pageCounters.skippedNoProvider(),
                pageCounters.failed(),
                pageCounters.timedOut()
            ));
            cursor = staleCredentials.getLast().getLogin();
            logCampusProgress(campusName, pageNumber, campusStaleTotalInitial, counters, remainingRunBudget, cursor);
        }

        if (remainingRunBudget <= 0) {
            log.info(
                "projects scheduler campus budget exhausted: campus={}, max_logins_per_run={}",
                campusName,
                maxLoginsPerRun
            );
        }

        logCampusDone(campusName, providerType, campusActiveTotal, campusStaleTotalInitial, counters, remainingRunBudget);
        return new CampusRunResult(counters, remainingRunBudget);
    }

    private void logCampusProgress(
        String campusName,
        int pageNumber,
        long campusStaleTotalInitial,
        SyncCounters counters,
        int remainingRunBudget,
        String cursor
    ) {
        long campusProcessedTotal = counters.processedTotal();
        long campusRemainingToProcess = Math.max(campusStaleTotalInitial - campusProcessedTotal, 0);
        long campusRemainingSuccessTarget = Math.max(campusStaleTotalInitial - counters.success(), 0);
        double progressPercent = percent(campusProcessedTotal, campusStaleTotalInitial);
        double successPercent = percent(counters.success(), campusProcessedTotal);

        log.info(
            "projects scheduler campus progress: campus={}, page={}, stale_scanned={}, processed_total={}, progress={}%, success={}, success_rate={}%, skipped_no_provider={}, failed={}, timeout={}, remaining_to_process={}, remaining_success_target={}, run_budget_remaining={}, next_cursor={}",
            campusName,
            pageNumber,
            counters.scanned(),
            campusProcessedTotal,
            formatPercent(progressPercent),
            counters.success(),
            formatPercent(successPercent),
            counters.skippedNoProvider(),
            counters.failed(),
            counters.timedOut(),
            campusRemainingToProcess,
            campusRemainingSuccessTarget,
            formatRunBudgetValue(remainingRunBudget),
            cursor
        );
    }

    private void logCampusDone(
        String campusName,
        String providerType,
        long campusActiveTotal,
        long campusStaleTotalInitial,
        SyncCounters counters,
        int remainingRunBudget
    ) {
        long campusSkippedFresh = Math.max(campusActiveTotal - counters.scanned(), 0);
        long campusProcessedTotal = counters.processedTotal();
        long campusRemainingToProcess = Math.max(campusStaleTotalInitial - campusProcessedTotal, 0);
        long campusRemainingSuccessTarget = Math.max(campusStaleTotalInitial - counters.success(), 0);
        double campusProgressPercent = percent(campusProcessedTotal, campusStaleTotalInitial);
        double campusSuccessPercent = percent(counters.success(), campusProcessedTotal);

        log.info(
            "projects scheduler campus done: campus={}, provider={}, active_total={}, stale_total_initial={}, stale_scanned={}, processed_total={}, progress={}%, skipped_fresh={}, success={}, success_rate={}%, skipped_no_provider={}, failed={}, timeout={}, remaining_to_process={}, remaining_success_target={}, run_budget_remaining={}",
            campusName,
            providerType,
            campusActiveTotal,
            campusStaleTotalInitial,
            counters.scanned(),
            campusProcessedTotal,
            formatPercent(campusProgressPercent),
            campusSkippedFresh,
            counters.success(),
            formatPercent(campusSuccessPercent),
            counters.skippedNoProvider(),
            counters.failed(),
            counters.timedOut(),
            campusRemainingToProcess,
            campusRemainingSuccessTarget,
            formatRunBudgetValue(remainingRunBudget)
        );
    }

    private String formatRunBudgetValue(int value) {
        if (maxLoginsPerRun <= 0) {
            return UNLIMITED;
        }
        return Integer.toString(Math.max(value, 0));
    }

    private double percent(long value, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return (value * 100.0) / total;
    }

    private String formatPercent(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private String buildBatchLogBlock(BatchLogContext context, BatchCounters counters, long elapsedMs) {
        double batchProgress = percent(context.batchNo(), context.totalBatches());
        long completed = (long) counters.success() + counters.skippedNoProvider() + counters.failed() + counters.timedOut();
        double completionRate = percent(completed, context.batchSize());
        double avgMsPerLogin = context.batchSize() <= 0 ? 0.0 : elapsedMs / (double) context.batchSize();
        double pageDonePercent = percent(context.pageProgress().doneInPage(), context.pageProgress().takenInPage());

        return String.format(
            java.util.Locale.ROOT,
            """
            ==================== PROJECTS SCHEDULER BATCH ====================
            page=%d
            batch=%d/%d (%s%%)
            batch_size=%d
            page_logins: taken=%d, done=%d (%s%%), remaining=%d
            result: success=%d, skipped_no_provider=%d, failed=%d, timeout=%d, completion=%s%%
            timing: elapsed_ms=%d, avg_ms_per_login=%s
            ==================================================================
            """,
            context.pageNumber(),
            context.batchNo(),
            context.totalBatches(),
            formatPercent(batchProgress),
            context.batchSize(),
            context.pageProgress().takenInPage(),
            context.pageProgress().doneInPage(),
            formatPercent(pageDonePercent),
            context.pageProgress().remainingInPage(),
            counters.success(),
            counters.skippedNoProvider(),
            counters.failed(),
            counters.timedOut(),
            formatPercent(completionRate),
            elapsedMs,
            formatPercent(avgMsPerLogin)
        );
    }

    private BatchCounters processPage(List<StudentCredentials> credentials, int pageNumber, int currentBatchSize) {
        BatchCounters totals = BatchCounters.empty();
        int effectiveBatchSize = Math.max(1, currentBatchSize);
        int totalBatches = (credentials.size() + effectiveBatchSize - 1) / effectiveBatchSize;
        int maxAttempts = Math.max(1, retryAttempts);
        int takenInPage = credentials.size();
        int doneInPage = 0;

        for (int i = 0; i < credentials.size(); i += effectiveBatchSize) {
            int batchNo = (i / effectiveBatchSize) + 1;
            int end = Math.min(i + effectiveBatchSize, credentials.size());
            List<StudentCredentials> batch = credentials.subList(i, end);
            BatchExecutionResult batchExecution = processBatchWithRetries(batch, pageNumber, batchNo, maxAttempts);
            totals = totals.plus(batchExecution.counters());
            BatchCounters batchCounters = batchExecution.counters();
            int batchDone = (int) ((long) batchCounters.success()
                + batchCounters.skippedNoProvider()
                + batchCounters.failed()
                + batchCounters.timedOut());
            doneInPage = Math.min(doneInPage + batchDone, takenInPage);
            int remainingInPage = Math.max(takenInPage - doneInPage, 0);
            BatchLogContext context = new BatchLogContext(
                pageNumber,
                batchNo,
                totalBatches,
                batch.size(),
                new PageProgress(takenInPage, doneInPage, remainingInPage)
            );
            logBatchProgress(context, batchExecution);
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

    private void logBatchProgress(BatchLogContext context, BatchExecutionResult execution) {
        BatchCounters counters = execution.counters();
        String logBlock = buildBatchLogBlock(context, counters, execution.elapsedMs());
        if (counters.failed() > 0 || counters.timedOut() > 0) {
            log.warn("{}", logBlock);
            return;
        }
        if (context.batchNo() == 1 || context.batchNo() == context.totalBatches() || context.batchNo() % 10 == 0) {
            log.info("{}", logBlock);
            return;
        }
        log.debug("{}", logBlock);
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

    private record SyncCounters(int scanned, int success, int skippedNoProvider, int failed, int timedOut) {
        private static SyncCounters empty() {
            return new SyncCounters(0, 0, 0, 0, 0);
        }

        private SyncCounters plus(SyncCounters other) {
            return new SyncCounters(
                scanned + other.scanned,
                success + other.success,
                skippedNoProvider + other.skippedNoProvider,
                failed + other.failed,
                timedOut + other.timedOut
            );
        }

        private long processedTotal() {
            return (long) success + skippedNoProvider + failed + timedOut;
        }
    }

    private record CampusRunResult(SyncCounters counters, int remainingRunBudget) {
    }

    private record PageProgress(int takenInPage, int doneInPage, int remainingInPage) {
    }

    private record BatchLogContext(
        int pageNumber,
        int batchNo,
        int totalBatches,
        int batchSize,
        PageProgress pageProgress
    ) {
    }
}
