package ru.izpz.edu.scheduler.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.izpz.edu.service.SchedulerMetricsService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SchedulerMetricsAspectTest.TestConfig.class)
class SchedulerMetricsAspectTest {

    @org.springframework.beans.factory.annotation.Autowired
    private TestScheduler testScheduler;

    @org.springframework.beans.factory.annotation.Autowired
    private SchedulerMetricsService schedulerMetricsService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(schedulerMetricsService);
    }

    @Test
    void shouldRecordSuccessMetrics() {
        testScheduler.success();

        verify(schedulerMetricsService).startPhaseTimer();
        verify(schedulerMetricsService).recordPhaseRequest("test_scheduler", "test_phase", SchedulerPhaseRequestStatus.SUCCESS);
        verify(schedulerMetricsService).recordRunStatus("test_scheduler", SchedulerRunStatus.SUCCESS);
        verify(schedulerMetricsService).recordLastSuccess("test_scheduler");
        verify(schedulerMetricsService, never()).recordPhaseIssue(eq("test_scheduler"), eq("test_phase"), any());
    }

    @Test
    void shouldRecordFailureMetrics() {
        assertThrows(RuntimeException.class, () -> testScheduler.failRuntime());

        verify(schedulerMetricsService).startPhaseTimer();
        verify(schedulerMetricsService).recordPhaseRequest("test_scheduler", "test_phase", SchedulerPhaseRequestStatus.FAILED);
        verify(schedulerMetricsService).recordPhaseIssue("test_scheduler", "test_phase", SchedulerErrorReason.EXECUTION_EXCEPTION);
        verify(schedulerMetricsService).recordRunStatus("test_scheduler", SchedulerRunStatus.FAILED);
        verify(schedulerMetricsService, never()).recordLastSuccess(any());
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        SchedulerMetricsService schedulerMetricsService() {
            return Mockito.mock(SchedulerMetricsService.class);
        }

        @Bean
        SchedulerErrorClassifier schedulerErrorClassifier() {
            return new SchedulerErrorClassifier();
        }

        @Bean
        SchedulerMetricsAspect schedulerMetricsAspect(
            SchedulerMetricsService schedulerMetricsService,
            SchedulerErrorClassifier schedulerErrorClassifier
        ) {
            return new SchedulerMetricsAspect(schedulerMetricsService, schedulerErrorClassifier);
        }

        @Bean
        TestScheduler testScheduler() {
            return new TestScheduler();
        }
    }

    static class TestScheduler {
        @TrackSchedulerMetrics(scheduler = "test_scheduler", phase = "test_phase")
        public void success() {
            // no-op
        }

        @TrackSchedulerMetrics(scheduler = "test_scheduler", phase = "test_phase")
        public void failRuntime() {
            throw new RuntimeException("boom");
        }
    }
}
