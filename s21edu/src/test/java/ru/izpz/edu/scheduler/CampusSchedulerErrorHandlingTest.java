package ru.izpz.edu.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.config.CampusSchedulerProperties;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.service.CampusCatalog;
import ru.izpz.edu.service.CampusService;
import ru.izpz.edu.service.SchedulerMetricsService;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampusSchedulerErrorHandlingTest {

    @Mock
    private CampusClient campusClient;

    @Mock
    private CampusService campusService;

    @Mock
    private SchedulerMetricsService metricsService;

    private CampusScheduler scheduler;
    private AutoCloseable mocks;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        executor = Executors.newSingleThreadExecutor();
        CampusCatalog campusCatalog = new CampusCatalog();
        CampusSchedulerProperties properties = new CampusSchedulerProperties();
        scheduler = new CampusScheduler(
            campusClient,
            campusService,
            metricsService,
            campusCatalog,
            properties,
            executor
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdownNow();
        mocks.close();
    }

    @Test
    void processSingleCampus_runtimeException_recordsExecutionError() throws Exception {
        String campusId = "campus-error";
        when(campusClient.getClustersByCampus(campusId)).thenThrow(new IllegalStateException("boom"));

        Object result = invokeProcessSingleCampus(campusId);

        assertFalse((Boolean) success(result));
        verify(metricsService).recordExternalApiError(
            eq("campus_parser"),
            eq("campus_api"),
            eq("get_clusters"),
            any(Throwable.class)
        );
    }

    @Test
    void processSingleCluster_runtimeException_recordsExecutionError() throws Exception {
        Cluster cluster = new Cluster();
        cluster.setClusterId(123L);
        doThrow(new IllegalStateException("boom"))
            .when(campusService)
            .replaceParticipantsByClusterIdWithProvider(cluster.getClusterId());

        Object result = invokeProcessSingleCluster(cluster);

        assertFalse((Boolean) success(result));
        verify(metricsService).recordExternalApiError(
            eq("campus_parser"),
            eq("campus_api"),
            eq("get_participants"),
            any(Throwable.class)
        );
    }

    private Object invokeProcessSingleCampus(String campusId) throws Exception {
        Method method = CampusScheduler.class.getDeclaredMethod("processSingleCampus", String.class);
        method.setAccessible(true);
        return method.invoke(scheduler, campusId);
    }

    private Object invokeProcessSingleCluster(Cluster cluster) throws Exception {
        Method method = CampusScheduler.class.getDeclaredMethod("processSingleCluster", Cluster.class);
        method.setAccessible(true);
        return method.invoke(scheduler, cluster);
    }

    private Object success(Object taskResult) throws Exception {
        Method success = taskResult.getClass().getMethod("success");
        return success.invoke(taskResult);
    }
}
