package ru.izpz.edu.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.config.CampusSchedulerProperties;
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.edu.service.CampusCatalog;
import ru.izpz.edu.service.CampusService;
import ru.izpz.edu.service.SchedulerMetricsService;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
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
    void processSingleCampusParticipants_runtimeException_recordsExecutionError() throws Exception {
        String campusId = "campus-1";
        ClusterV1DTO cluster = new ClusterV1DTO();
        cluster.setId(123L);
        when(campusClient.getClustersByCampus(campusId)).thenReturn(java.util.List.of(cluster));
        doThrow(new IllegalStateException("boom"))
            .when(campusService)
            .fetchParticipantsByClusterWithProvider(cluster.getId());

        Object result = invokeProcessSingleCampusParticipants(campusId);

        assertFalse((Boolean) success(result));
        verify(metricsService).recordExternalApiError(
            eq("campus_parser"),
            eq("campus_api"),
            eq("get_participants"),
            any(Throwable.class)
        );
    }

    @Test
    void processSingleCampusParticipants_saveException_returnsFailure() throws Exception {
        String campusId = "campus-1";
        ClusterV1DTO cluster = new ClusterV1DTO();
        cluster.setId(123L);
        when(campusClient.getClustersByCampus(campusId)).thenReturn(java.util.List.of(cluster));
        when(campusService.fetchParticipantsByClusterWithProvider(cluster.getId())).thenReturn(java.util.List.of());
        doThrow(new IllegalStateException("save boom"))
            .when(campusService)
            .replaceCampusSnapshotByCampusId(eq(campusId), anyList(), anyList());

        Object result = invokeProcessSingleCampusParticipants(campusId);

        assertFalse((Boolean) success(result));
    }

    private Object invokeProcessSingleCampus(String campusId) throws Exception {
        Method method = CampusScheduler.class.getDeclaredMethod("processSingleCampus", String.class);
        method.setAccessible(true);
        return method.invoke(scheduler, campusId);
    }

    private Object invokeProcessSingleCampusParticipants(String campusId) throws Exception {
        Method method = CampusScheduler.class.getDeclaredMethod("processSingleCampusParticipants", String.class);
        method.setAccessible(true);
        return method.invoke(scheduler, campusId);
    }

    private Object success(Object taskResult) throws Exception {
        Method success = taskResult.getClass().getMethod("success");
        return success.invoke(taskResult);
    }
}
