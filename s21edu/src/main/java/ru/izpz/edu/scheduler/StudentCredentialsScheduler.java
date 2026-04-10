package ru.izpz.edu.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiException;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.dto.GraphQLStudentCredentialsDto;
import ru.izpz.edu.repository.StudentCredentialsRepository;
import ru.izpz.edu.service.CampusCatalog;
import ru.izpz.edu.service.GraphQLService;
import ru.izpz.edu.service.SchedulerMetricsService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "credentials.scheduler.enabled", havingValue = "true")
public class StudentCredentialsScheduler {

    private static final String SCHEDULER_NAME = "credentials_sync";

    private final CampusClient campusClient;
    private final CampusCatalog campusCatalog;
    private final GraphQLService graphQLService;
    private final StudentCredentialsRepository studentCredentialsRepository;
    private final SchedulerMetricsService schedulerMetricsService;
    @Qualifier("credentialsSchedulerExecutor")
    private final ExecutorService credentialsSchedulerExecutor;

    @Value("${credentials.scheduler.page-size:1000}")
    private int pageSize;
    @Value("${credentials.scheduler.batch-size:200}")
    private int batchSize;

    @Scheduled(
        initialDelayString = "${credentials.scheduler.initial-delay:PT0S}",
        fixedDelayString = "${credentials.scheduler.fixed-delay:PT12H}"
    )
    public void runNightlySync() {
        if (pageSize <= 0 || batchSize <= 0) {
            log.warn(
                "credentials scheduler пропущен: page-size={}, batch-size={}",
                pageSize,
                batchSize
            );
            return;
        }

        Instant startedAt = Instant.now();
        schedulerMetricsService.setCredentialsSyncInProgress(SCHEDULER_NAME, true);
        try {
            PhaseStats syncPhase = syncAllLoginsFromCampus();

            Duration elapsed = Duration.between(startedAt, Instant.now());
            schedulerMetricsService.recordCredentialsSyncRun(
                SCHEDULER_NAME,
                new SchedulerMetricsService.CredentialsSyncRunStats(
                    syncPhase.scanned,
                    syncPhase.requested,
                    syncPhase.alreadySaved,
                    syncPhase.success,
                    syncPhase.noData,
                    syncPhase.failed
                ),
                elapsed
            );

            log.info(
                "credentials nightly sync завершен: scanned={}, requested={}, already_saved={}, success={}, no_data={}, failed={}, elapsed={}s",
                syncPhase.scanned,
                syncPhase.requested,
                syncPhase.alreadySaved,
                syncPhase.success,
                syncPhase.noData,
                syncPhase.failed,
                elapsed.toSeconds()
            );
        } finally {
            schedulerMetricsService.setCredentialsSyncInProgress(SCHEDULER_NAME, false);
        }
    }

    private PhaseStats syncAllLoginsFromCampus() {
        Set<String> seenLogins = new HashSet<>();
        int scanned = 0;
        int alreadySaved = 0;
        int success = 0;
        int noData = 0;
        int failed = 0;
        int requested = 0;

        for (String campusId : campusCatalog.targetCampusIds()) {
            CampusScanResult campusResult = scanCampus(campusId, seenLogins);
            scanned += campusResult.scanned();
            alreadySaved += campusResult.alreadySaved();
            success += campusResult.success();
            noData += campusResult.noData();
            failed += campusResult.failed();
            requested += campusResult.requested();
        }

        log.info(
            "credentials phase full завершена: scanned={}, requested={}, already_saved={}, success={}, no_data={}, failed={}",
            scanned, requested, alreadySaved, success, noData, failed
        );
        return new PhaseStats(scanned, requested, alreadySaved, success, noData, failed);
    }

    private CampusScanResult scanCampus(String campusId, Set<String> seenLogins) {
        int scanned = 0;
        int alreadySaved = 0;
        int success = 0;
        int noData = 0;
        int failed = 0;
        int requested = 0;
        long offset = 0L;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<String> page = fetchParticipantsPage(campusId, offset);
            if (page.isEmpty()) {
                hasNextPage = false;
            } else {
                scanned += page.size();
                offset += page.size();
                List<String> uniquePageLogins = page.stream()
                    .filter(login -> login != null && !login.isBlank())
                    .filter(seenLogins::add)
                    .toList();
                if (!uniquePageLogins.isEmpty()) {
                    Set<String> existing = new HashSet<>(studentCredentialsRepository.findExistingLogins(uniquePageLogins));
                    alreadySaved += existing.size();
                    List<String> missingLogins = uniquePageLogins.stream()
                        .filter(login -> !existing.contains(login))
                        .toList();
                    BatchCounters counters = processLoginsInParallel(missingLogins, "full");
                    success += counters.success;
                    noData += counters.noData;
                    failed += counters.failed;
                    requested += missingLogins.size();
                }
                hasNextPage = page.size() >= pageSize;
            }
        }

        return new CampusScanResult(scanned, requested, alreadySaved, success, noData, failed);
    }

    private List<String> fetchParticipantsPage(String campusId, long offset) {
        try {
            return campusClient.getParticipantsByCampus(campusId, pageSize, offset);
        } catch (ApiException e) {
            log.error("Ошибка получения участников кампуса {} на offset={}", campusId, offset, e);
            return List.of();
        }
    }

    private BatchCounters processLoginsInParallel(List<String> logins, String phaseTag) {
        int success = 0;
        int noData = 0;
        int failed = 0;

        for (int i = 0; i < logins.size(); i += batchSize) {
            int batchNumber = (i / batchSize) + 1;
            int end = Math.min(i + batchSize, logins.size());
            List<String> batch = logins.subList(i, end);
            Instant batchStartedAt = Instant.now();
            List<CompletableFuture<FetchResult>> futures = new ArrayList<>(batch.size());
            for (String login : batch) {
                futures.add(CompletableFuture.supplyAsync(() -> refreshCredentials(login), credentialsSchedulerExecutor));
            }

            int batchSuccess = 0;
            int batchNoData = 0;
            int batchFailed = 0;
            for (CompletableFuture<FetchResult> future : futures) {
                FetchResult result = future.join();
                switch (result) {
                    case SUCCESS -> {
                        success++;
                        batchSuccess++;
                    }
                    case NO_DATA -> {
                        noData++;
                        batchNoData++;
                    }
                    case FAILED -> {
                        failed++;
                        batchFailed++;
                    }
                }
            }

            Duration batchElapsed = Duration.between(batchStartedAt, Instant.now());
            schedulerMetricsService.recordCredentialsSyncBatch(
                SCHEDULER_NAME,
                batch.size(),
                batchSuccess,
                batchNoData,
                batchFailed,
                batchElapsed
            );
            log.info(
                "credentials phase {} batch processed: batch={}, size={}, success={}, no_data={}, failed={}, elapsed_ms={}",
                phaseTag,
                batchNumber,
                batch.size(),
                batchSuccess,
                batchNoData,
                batchFailed,
                batchElapsed.toMillis()
            );
        }
        return new BatchCounters(success, noData, failed);
    }

    private FetchResult refreshCredentials(String login) {
        try {
            GraphQLStudentCredentialsDto credentials = graphQLService.refreshCredentialsWithLimits(login);
            return credentials == null ? FetchResult.NO_DATA : FetchResult.SUCCESS;
        } catch (RuntimeException e) {
            log.warn("Ошибка sync credentials для {}: {}", login, e.getMessage());
            return FetchResult.FAILED;
        }
    }

    private enum FetchResult {
        SUCCESS,
        NO_DATA,
        FAILED
    }

    private record BatchCounters(int success, int noData, int failed) {
    }

    private record PhaseStats(int scanned, int requested, int alreadySaved, int success, int noData, int failed) {
    }

    private record CampusScanResult(int scanned, int requested, int alreadySaved, int success, int noData, int failed) {
    }
}
