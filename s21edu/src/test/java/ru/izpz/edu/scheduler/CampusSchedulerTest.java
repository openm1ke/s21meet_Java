package ru.izpz.edu.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ApiException;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.service.CampusService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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

    @InjectMocks
    private CampusScheduler scheduler;

    @Test
    void parseMskKznNsk_success_updatesClustersAndParticipants() throws Exception {
        when(campusClient.getClustersByCampus(eq(MSK))).thenReturn(List.of());
        when(campusClient.getClustersByCampus(eq(KZN))).thenReturn(List.of());
        when(campusClient.getClustersByCampus(eq(NSK))).thenReturn(List.of());

        Cluster c1 = new Cluster();
        c1.setClusterId(1L);
        c1.setName("c1");
        Cluster c2 = new Cluster();
        c2.setClusterId(2L);
        c2.setName("c2");

        when(campusService.findAllByOrderByCampusIdAsc()).thenReturn(List.of(c1, c2));

        assertDoesNotThrow(() -> scheduler.parseMskKznNsk());

        verify(campusClient, timeout(1000)).getClustersByCampus(eq(MSK));
        verify(campusClient, timeout(1000)).getClustersByCampus(eq(KZN));
        verify(campusClient, timeout(1000)).getClustersByCampus(eq(NSK));

        verify(campusService, timeout(1000)).replaceClustersByCampusId(eq(MSK), anyList());
        verify(campusService, timeout(1000)).replaceClustersByCampusId(eq(KZN), anyList());
        verify(campusService, timeout(1000)).replaceClustersByCampusId(eq(NSK), anyList());

        verify(campusService, timeout(1000)).replaceParticipantsByClusterIdWithProvider(eq(1L));
        verify(campusService, timeout(1000)).replaceParticipantsByClusterIdWithProvider(eq(2L));
    }

    @Test
    void parseMskKznNsk_whenApiExceptions_doesNotThrowAndContinues() throws Exception {
        when(campusClient.getClustersByCampus(eq(MSK))).thenReturn(List.of());
        when(campusClient.getClustersByCampus(eq(KZN))).thenThrow(new ApiException("boom"));
        when(campusClient.getClustersByCampus(eq(NSK))).thenReturn(List.of());

        Cluster c1 = new Cluster();
        c1.setClusterId(1L);
        c1.setName("c1");
        Cluster c2 = new Cluster();
        c2.setClusterId(2L);
        c2.setName("c2");

        when(campusService.findAllByOrderByCampusIdAsc()).thenReturn(List.of(c1, c2));
        doThrow(new ApiException("participants boom")).when(campusService).replaceParticipantsByClusterIdWithProvider(eq(2L));

        assertDoesNotThrow(() -> scheduler.parseMskKznNsk());

        verify(campusClient, timeout(1000)).getClustersByCampus(eq(MSK));
        verify(campusClient, timeout(1000)).getClustersByCampus(eq(KZN));
        verify(campusClient, timeout(1000)).getClustersByCampus(eq(NSK));

        verify(campusService, timeout(1000)).replaceClustersByCampusId(eq(MSK), anyList());
        verify(campusService, timeout(1000)).replaceClustersByCampusId(eq(NSK), anyList());

        verify(campusService, timeout(1000)).replaceParticipantsByClusterIdWithProvider(eq(1L));
        verify(campusService, timeout(1000)).replaceParticipantsByClusterIdWithProvider(eq(2L));
    }
}
