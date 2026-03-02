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
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.config.CampusSchedulerProperties;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.service.CampusCatalog;
import ru.izpz.edu.service.CampusService;
import ru.izpz.edu.service.SchedulerMetricsService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
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
            new SchedulerMetricsService(meterRegistry, campusCatalog),
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
        when(campusClient.getClustersByCampus(MSK)).thenReturn(List.of());
        when(campusClient.getClustersByCampus(KZN)).thenReturn(List.of());
        when(campusClient.getClustersByCampus(NSK)).thenReturn(List.of());

        Cluster c1 = new Cluster();
        c1.setClusterId(1L);
        c1.setName("c1");
        Cluster c2 = new Cluster();
        c2.setClusterId(2L);
        c2.setName("c2");

        when(campusService.findAllByOrderByCampusIdAsc()).thenReturn(List.of(c1, c2));

        assertDoesNotThrow(() -> scheduler.parseMskKznNsk());

        ArgumentCaptor<String> campusIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> clustersCaptor = ArgumentCaptor.forClass(List.class);

        verify(campusClient, timeout(1000)).getClustersByCampus(MSK);
        verify(campusClient, timeout(1000)).getClustersByCampus(KZN);
        verify(campusClient, timeout(1000)).getClustersByCampus(NSK);

        verify(campusService, timeout(1000).times(3)).replaceClustersByCampusId(campusIdCaptor.capture(), clustersCaptor.capture());

        verify(campusService, timeout(1000)).replaceParticipantsByClusterIdWithProvider(1L);
        verify(campusService, timeout(1000)).replaceParticipantsByClusterIdWithProvider(2L);

        List<String> campusIds = campusIdCaptor.getAllValues();
        org.junit.jupiter.api.Assertions.assertTrue(campusIds.containsAll(List.of(MSK, KZN, NSK)));
    }

    @Test
    void parseMskKznNsk_whenApiExceptions_doesNotThrowAndContinues() throws Exception {
        when(campusClient.getClustersByCampus(MSK)).thenReturn(List.of());
        when(campusClient.getClustersByCampus(KZN)).thenThrow(new ApiException("boom"));
        when(campusClient.getClustersByCampus(NSK)).thenReturn(List.of());

        Cluster c1 = new Cluster();
        c1.setClusterId(1L);
        c1.setName("c1");
        Cluster c2 = new Cluster();
        c2.setClusterId(2L);
        c2.setName("c2");

        when(campusService.findAllByOrderByCampusIdAsc()).thenReturn(List.of(c1, c2));
        doThrow(new ApiException("participants boom")).when(campusService).replaceParticipantsByClusterIdWithProvider(2L);

        assertDoesNotThrow(() -> scheduler.parseMskKznNsk());

        ArgumentCaptor<String> campusIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> clustersCaptor = ArgumentCaptor.forClass(List.class);

        verify(campusClient, timeout(1000)).getClustersByCampus(MSK);
        verify(campusClient, timeout(1000)).getClustersByCampus(KZN);
        verify(campusClient, timeout(1000)).getClustersByCampus(NSK);

        verify(campusService, timeout(1000).times(2)).replaceClustersByCampusId(campusIdCaptor.capture(), clustersCaptor.capture());

        verify(campusService, timeout(1000)).replaceParticipantsByClusterIdWithProvider(1L);
        verify(campusService, timeout(1000)).replaceParticipantsByClusterIdWithProvider(2L);

        List<String> campusIds = campusIdCaptor.getAllValues();
        org.junit.jupiter.api.Assertions.assertTrue(campusIds.containsAll(List.of(MSK, NSK)));
    }
}
