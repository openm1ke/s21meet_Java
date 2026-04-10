package ru.izpz.edu.scheduler;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.config.CampusSchedulerProperties;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.scheduler.metrics.SchedulerErrorClassifier;
import ru.izpz.edu.service.CampusCatalog;
import ru.izpz.edu.service.CampusService;
import ru.izpz.edu.service.SchedulerMetricsService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusSchedulerTest {

    private static final String MSK = "6bfe3c56-0211-4fe1-9e59-51616caac4dd";
    private static final String KZN = "7c293c9c-f28c-4b10-be29-560e4b000a34";
    private static final String NSK = "46e7d965-21e9-4936-bea9-f5ea0d1fddf2";

    @Mock
    private CampusClient campusClient;

    @Mock
    private CampusService campusService;

    private CampusScheduler scheduler;
    private MeterRegistry meterRegistry;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        executorService = Executors.newFixedThreadPool(4);
        CampusCatalog campusCatalog = new CampusCatalog();
        CampusSchedulerProperties schedulerProperties = new CampusSchedulerProperties();
        scheduler = new CampusScheduler(
            campusClient,
            campusService,
            new SchedulerMetricsService(meterRegistry, campusCatalog, new SchedulerErrorClassifier()),
            campusCatalog,
            schedulerProperties,
            executorService
        );
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    void parseMskKznNsk_success_updatesClustersAndParticipants() throws Exception {
        ClusterV1DTO c1 = new ClusterV1DTO();
        c1.setId(1L);
        c1.setName("c1");
        ClusterV1DTO c2 = new ClusterV1DTO();
        c2.setId(2L);
        c2.setName("c2");
        ClusterV1DTO c3 = new ClusterV1DTO();
        c3.setId(3L);
        c3.setName("c3");

        when(campusClient.getClustersByCampus(MSK)).thenReturn(List.of(c1));
        when(campusClient.getClustersByCampus(KZN)).thenReturn(List.of(c2));
        when(campusClient.getClustersByCampus(NSK)).thenReturn(List.of(c3));
        when(campusService.fetchParticipantsByClusterWithProvider(1L)).thenReturn(List.of(new Workplace()));
        when(campusService.fetchParticipantsByClusterWithProvider(2L)).thenReturn(List.of(new Workplace()));
        when(campusService.fetchParticipantsByClusterWithProvider(3L)).thenReturn(List.of(new Workplace()));

        assertDoesNotThrow(() -> scheduler.parseMskKznNsk());

        ArgumentCaptor<String> campusIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> clustersCaptor = ArgumentCaptor.forClass(List.class);

        verify(campusClient).getClustersByCampus(MSK);
        verify(campusClient).getClustersByCampus(KZN);
        verify(campusClient).getClustersByCampus(NSK);

        verify(campusService, times(3)).replaceCampusSnapshotByCampusId(campusIdCaptor.capture(), clustersCaptor.capture(), anyList());

        verify(campusService).fetchParticipantsByClusterWithProvider(1L);
        verify(campusService).fetchParticipantsByClusterWithProvider(2L);
        verify(campusService).fetchParticipantsByClusterWithProvider(3L);
        verify(campusService).replaceCampusSnapshotByCampusId(eq(MSK), anyList(), argThat(list -> list.size() == 1));
        verify(campusService).replaceCampusSnapshotByCampusId(eq(KZN), anyList(), argThat(list -> list.size() == 1));
        verify(campusService).replaceCampusSnapshotByCampusId(eq(NSK), anyList(), argThat(list -> list.size() == 1));

        List<String> campusIds = campusIdCaptor.getAllValues();
        org.junit.jupiter.api.Assertions.assertTrue(campusIds.containsAll(List.of(MSK, KZN, NSK)));
    }

    @Test
    void parseMskKznNsk_whenApiExceptions_doesNotThrowAndContinues() throws Exception {
        ClusterV1DTO c1 = new ClusterV1DTO();
        c1.setId(1L);
        c1.setName("c1");
        ClusterV1DTO c2 = new ClusterV1DTO();
        c2.setId(2L);
        c2.setName("c2");
        ClusterV1DTO c3 = new ClusterV1DTO();
        c3.setId(3L);
        c3.setName("c3");

        when(campusClient.getClustersByCampus(MSK)).thenReturn(List.of(c1));
        when(campusClient.getClustersByCampus(KZN)).thenThrow(new ApiException("boom"));
        when(campusClient.getClustersByCampus(NSK)).thenReturn(List.of(c3));
        when(campusService.fetchParticipantsByClusterWithProvider(1L)).thenReturn(List.of(new Workplace()));
        when(campusService.fetchParticipantsByClusterWithProvider(3L)).thenReturn(List.of(new Workplace()));

        assertDoesNotThrow(() -> scheduler.parseMskKznNsk());

        ArgumentCaptor<String> campusIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> clustersCaptor = ArgumentCaptor.forClass(List.class);

        verify(campusClient).getClustersByCampus(MSK);
        verify(campusClient).getClustersByCampus(KZN);
        verify(campusClient).getClustersByCampus(NSK);

        verify(campusService, times(2)).replaceCampusSnapshotByCampusId(campusIdCaptor.capture(), clustersCaptor.capture(), anyList());

        verify(campusService).fetchParticipantsByClusterWithProvider(1L);
        verify(campusService).fetchParticipantsByClusterWithProvider(3L);
        verify(campusService, times(2)).replaceCampusSnapshotByCampusId(anyString(), anyList(), anyList());

        List<String> campusIds = campusIdCaptor.getAllValues();
        org.junit.jupiter.api.Assertions.assertTrue(campusIds.containsAll(List.of(MSK, NSK)));
    }
}
