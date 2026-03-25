package ru.izpz.edu.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.config.CampusSchedulerProperties;
import ru.izpz.edu.model.Workplace;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.edu.scheduler.metrics.SchedulerRunStatus;
import ru.izpz.edu.service.CampusCatalog;
import ru.izpz.edu.service.CampusService;
import ru.izpz.edu.service.SchedulerMetricsService;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampusSchedulerFailurePathsTest {

    @Mock
    private CampusClient campusClient;

    @Mock
    private CampusService campusService;

    @Mock
    private SchedulerMetricsService metricsService;

    private AutoCloseable mocks;
    private CampusScheduler scheduler;
    private CampusSchedulerProperties schedulerProperties;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        executor = Executors.newFixedThreadPool(3);
        CampusCatalog campusCatalog = new CampusCatalog();
        schedulerProperties = new CampusSchedulerProperties();
        scheduler = new CampusScheduler(
            campusClient,
            campusService,
            metricsService,
            campusCatalog,
            schedulerProperties,
            executor
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdownNow();
        mocks.close();
    }

    @Test
    void parseMskKznNsk_clustersExceedTimeout_marksRunAsFailed() throws ApiException {
        schedulerProperties.getTimeout().setGlobal(Duration.ofMillis(1));
        when(campusClient.getClustersByCampus(anyString())).thenAnswer(invocation -> {
            blockUntilInterrupted();
            return List.<ClusterV1DTO>of();
        });

        scheduler.parseMskKznNsk();

        verify(metricsService).recordRunStatus("campus_parser", SchedulerRunStatus.FAILED);
        verify(campusService, never()).replaceCampusSnapshotByCampusId(anyString(), anyList(), anyList());
    }

    @Test
    void parseMskKznNsk_participantsExceedTimeout_marksRunAsFailed() throws ApiException {
        schedulerProperties.getTimeout().setGlobal(Duration.ofMillis(1));
        ClusterV1DTO cluster = new ClusterV1DTO();
        cluster.setId(100L);
        cluster.setName("cluster");
        when(campusClient.getClustersByCampus(anyString())).thenReturn(List.of(cluster));
        doAnswer(invocation -> {
            blockUntilInterrupted();
            return List.<Workplace>of();
        }).when(campusService).fetchParticipantsByClusterWithProvider(cluster.getId());

        scheduler.parseMskKznNsk();

        verify(metricsService).recordRunStatus("campus_parser", SchedulerRunStatus.FAILED);
    }

    @Test
    void parseMskKznNsk_perCampusTimeoutAfterPartialSuccess_marksRunAsPartial() throws ApiException {
        schedulerProperties.getTimeout().setGlobal(Duration.ofSeconds(2));
        schedulerProperties.getTimeout().setPerCampus(Duration.ofMillis(20));
        ClusterV1DTO cluster = new ClusterV1DTO();
        cluster.setId(100L);
        cluster.setName("cluster");
        when(campusClient.getClustersByCampus(anyString())).thenReturn(List.of(cluster));

        AtomicInteger callOrdinal = new AtomicInteger();
        doAnswer(invocation -> {
            if (callOrdinal.getAndIncrement() == 0) {
                return List.of(new Workplace());
            }
            blockUntilInterrupted();
            return List.<Workplace>of();
        }).when(campusService).fetchParticipantsByClusterWithProvider(cluster.getId());

        scheduler.parseMskKznNsk();

        verify(metricsService).recordRunStatus("campus_parser", SchedulerRunStatus.PARTIAL);
        verify(campusService).refreshParticipantMetrics();
        verify(campusService, atLeastOnce()).replaceCampusSnapshotByCampusId(anyString(), anyList(), anyList());
    }

    @Test
    void parseMskKznNsk_globalTimeoutAfterPartialSuccess_marksRunAsPartial() throws ApiException {
        schedulerProperties.getTimeout().setGlobal(Duration.ofMillis(120));
        schedulerProperties.getTimeout().setPerCampus(Duration.ofSeconds(5));

        ClusterV1DTO cluster = new ClusterV1DTO();
        cluster.setId(101L);
        cluster.setName("cluster-global-timeout");
        when(campusClient.getClustersByCampus(anyString())).thenReturn(List.of(cluster));

        AtomicInteger callOrdinal = new AtomicInteger();
        doAnswer(invocation -> {
            if (callOrdinal.getAndIncrement() == 0) {
                return List.of(new Workplace());
            }
            blockUntilInterrupted();
            return List.<Workplace>of();
        }).when(campusService).fetchParticipantsByClusterWithProvider(cluster.getId());

        scheduler.parseMskKznNsk();

        verify(metricsService).recordRunStatus("campus_parser", SchedulerRunStatus.PARTIAL);
        verify(campusService).refreshParticipantMetrics();
        verify(campusService, atLeastOnce()).replaceCampusSnapshotByCampusId(anyString(), anyList(), anyList());
    }

    @Test
    void parseMskKznNsk_partialErrors_recordsPartialStatus() throws ApiException {
        schedulerProperties.getTimeout().setGlobal(Duration.ofSeconds(1));
        AtomicInteger callOrdinal = new AtomicInteger();
        when(campusClient.getClustersByCampus(anyString())).thenAnswer(invocation -> {
            if (callOrdinal.getAndIncrement() == 0) {
                throw new ApiException("boom");
            }
            ClusterV1DTO cluster = new ClusterV1DTO();
            cluster.setId(1L);
            cluster.setName("cluster");
            return List.of(cluster);
        });

        when(campusService.fetchParticipantsByClusterWithProvider(1L)).thenReturn(List.of(new Workplace()));

        scheduler.parseMskKznNsk();

        verify(metricsService).recordRunStatus("campus_parser", SchedulerRunStatus.PARTIAL);
    }

    @Test
    void parseMskKznNsk_participantsError_recordsPartialStatus() throws ApiException {
        schedulerProperties.getTimeout().setGlobal(Duration.ofSeconds(1));
        ClusterV1DTO cluster = new ClusterV1DTO();
        cluster.setId(2L);
        cluster.setName("cluster");
        when(campusClient.getClustersByCampus(anyString())).thenReturn(List.of(cluster));
        doThrow(new ApiException("boom"))
            .when(campusService)
            .fetchParticipantsByClusterWithProvider(cluster.getId());

        scheduler.parseMskKznNsk();

        verify(metricsService).recordRunStatus("campus_parser", SchedulerRunStatus.PARTIAL);
    }

    private static void blockUntilInterrupted() {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
