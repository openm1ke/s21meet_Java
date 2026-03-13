package ru.izpz.edu.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.config.CampusSchedulerProperties;
import ru.izpz.edu.scheduler.metrics.SchedulerErrorReason;
import ru.izpz.edu.scheduler.metrics.SchedulerPhaseRequestStatus;
import ru.izpz.edu.service.CampusCatalog;
import ru.izpz.edu.service.CampusService;
import ru.izpz.edu.service.SchedulerMetricsService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CampusSchedulerAwaitPhaseResultsTest {

    @Mock
    private CampusClient campusClient;

    @Mock
    private CampusService campusService;

    @Mock
    private SchedulerMetricsService schedulerMetricsService;

    private CampusScheduler scheduler;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
        scheduler = new CampusScheduler(
            campusClient,
            campusService,
            schedulerMetricsService,
            new CampusCatalog(),
            new CampusSchedulerProperties(),
            executor
        );
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void awaitPhaseResults_recordsPhaseRequestsAndRetries() throws Exception {
        Object successResult = createTaskResult(true, SchedulerPhaseRequestStatus.SUCCESS, SchedulerErrorReason.NONE);
        List<CompletableFuture<Object>> futures = List.of(CompletableFuture.completedFuture(successResult));
        invokeAwaitPhase("clusters", Duration.ofMillis(100), futures);

        verify(schedulerMetricsService).recordPhaseRequest("campus_parser", "clusters", SchedulerPhaseRequestStatus.SUCCESS);
        verify(schedulerMetricsService, never()).recordPhaseIssue("campus_parser", "clusters", SchedulerErrorReason.NONE);
    }

    @Test
    void awaitPhaseResults_recordsIssueWhenTaskFails() throws Exception {
        Object failureResult = createTaskResult(false, SchedulerPhaseRequestStatus.FAILED, SchedulerErrorReason.API_EXCEPTION);
        List<CompletableFuture<Object>> futures = List.of(CompletableFuture.completedFuture(failureResult));
        invokeAwaitPhase("participants", Duration.ofMillis(100), futures);

        verify(schedulerMetricsService).recordPhaseRequest("campus_parser", "participants", SchedulerPhaseRequestStatus.FAILED);
        verify(schedulerMetricsService).recordPhaseIssue("campus_parser", "participants", SchedulerErrorReason.API_EXCEPTION);
    }

    @Test
    void awaitPhaseResults_timeout_recordsTimeoutIssue() throws Exception {
        List<CompletableFuture<Object>> futures = List.of(new CompletableFuture<>());
        invokeAwaitPhase("clusters", Duration.ofMillis(1), futures);

        verify(schedulerMetricsService).recordPhaseIssue("campus_parser", "clusters", SchedulerErrorReason.TIMEOUT);
    }

    @Test
    void awaitPhaseResults_timeout_cancelsPendingFutures() throws Exception {
        CompletableFuture<Object> future = new CompletableFuture<>();
        invokeAwaitPhase("clusters", Duration.ofMillis(1), List.of(future));

        assertTrue(future.isCancelled());
    }

    @Test
    void awaitPhaseResults_interrupted_recordsInterruptedIssue() throws Exception {
        List<CompletableFuture<Object>> futures = List.of(new CompletableFuture<>());
        Thread.currentThread().interrupt();
        try {
            invokeAwaitPhase("participants", Duration.ofMillis(1), futures);
        } finally {
            if (Thread.interrupted()) {
                Thread.currentThread().interrupt();
            }
        }

        verify(schedulerMetricsService).recordPhaseIssue("campus_parser", "participants", SchedulerErrorReason.INTERRUPTED);
    }

    @Test
    void awaitPhaseResults_exception_recordsExecutionIssue() throws Exception {
        CompletableFuture<Object> failure = new CompletableFuture<>();
        failure.completeExceptionally(new IllegalStateException("boom"));
        List<CompletableFuture<Object>> futures = List.of(failure);

        invokeAwaitPhase("clusters", Duration.ofMillis(100), futures);

        verify(schedulerMetricsService).recordPhaseIssue("campus_parser", "clusters", SchedulerErrorReason.EXECUTION_EXCEPTION);
    }

    @Test
    void cancelPendingFutures_handlesIncompleteFuture() throws Exception {
        CompletableFuture<Object> future = new CompletableFuture<>();
        Method cancelMethod = CampusScheduler.class.getDeclaredMethod("cancelPendingFutures", List.class);
        cancelMethod.setAccessible(true);
        cancelMethod.invoke(scheduler, List.of(future));

        assertTrue(future.isCancelled());
    }

    private void invokeAwaitPhase(String phase, Duration timeout, List<CompletableFuture<Object>> futures) throws Exception {
        Method awaitMethod = CampusScheduler.class.getDeclaredMethod("awaitPhaseResults", String.class, Duration.class, List.class);
        awaitMethod.setAccessible(true);
        awaitMethod.invoke(scheduler, phase, timeout, futures);
    }

    private Object createTaskResult(boolean success, SchedulerPhaseRequestStatus status, SchedulerErrorReason reason) throws Exception {
        Class<?> taskResultClass = Class.forName("ru.izpz.edu.scheduler.CampusScheduler$TaskResult");
        Constructor<?> constructor = taskResultClass.getDeclaredConstructor(boolean.class, SchedulerPhaseRequestStatus.class, SchedulerErrorReason.class);
        constructor.setAccessible(true);
        return constructor.newInstance(success, status, reason);
    }
}
