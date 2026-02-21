package ru.izpz.edu.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ApiException;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.model.Cluster;
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.edu.service.CampusService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampusSchedulerTest {

    @Mock
    private CampusClient campusClient;

    @Mock
    private CampusService campusService;

    @InjectMocks
    private CampusScheduler scheduler;

    @Test
    void parseMskKznNsk_shouldNotThrow_whenApiThrows() throws Exception {
        when(campusClient.getClustersByCampus(anyString())).thenThrow(new ApiException("boom"));
        when(campusService.findAllByOrderByCampusIdAsc()).thenReturn(List.of());

        scheduler.parseMskKznNsk();

        verify(campusClient, atLeastOnce()).getClustersByCampus(anyString());
        verify(campusService).findAllByOrderByCampusIdAsc();
    }

    @Test
    void parseMskKznNsk_shouldUpdateParticipants_forClusters() throws Exception {
        Cluster cluster = new Cluster();
        cluster.setClusterId(1L);
        cluster.setName("c");

        when(campusService.findAllByOrderByCampusIdAsc()).thenReturn(List.of(cluster));
        when(campusClient.getClustersByCampus(anyString())).thenReturn(List.of(new ClusterV1DTO()));
        doNothing().when(campusService).replaceClustersByCampusId(anyString(), anyList());
        doNothing().when(campusService).replaceParticipantsByClusterIdWithProvider(1L);

        scheduler.parseMskKznNsk();

        verify(campusService).replaceParticipantsByClusterIdWithProvider(1L);
    }
}
