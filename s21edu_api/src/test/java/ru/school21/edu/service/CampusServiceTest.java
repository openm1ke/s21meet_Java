package ru.school21.edu.service;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.school21.edu.ApiClient;
import ru.school21.edu.ApiException;
import ru.school21.edu.model.ClusterV1DTO;
import ru.school21.edu.model.ClustersV1DTO;
import ru.school21.edu.repository.ClusterRepository;
import ru.school21.edu.repository.WorkplaceRepository;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CampusServiceTest {

    @InjectMocks
    private CampusService campusService;

    @Mock
    private CampusApiProxy campusApi;

    @Mock
    private ClusterApiProxy clusterApi;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private WorkplaceRepository workplaceRepository;

    @Mock
    private ApiClient apiClient;

    @Mock
    private TokenService tokenService;

    private static final UUID CAMPUS_ID = UUID.fromString("6bfe3c56-0211-4fe1-9e59-51616caac4dd");
    private static final Long CLUSTER_ID = 123L;

    @Test
    void getClustersByCampus_shouldLogWarning_whenApiReturnsNull() throws ApiException {
        when(campusApi.getClustersByCampus(CAMPUS_ID)).thenReturn(null);

        campusService.getClustersByCampus(CAMPUS_ID);

        verify(clusterRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void getClustersByCampus_shouldSaveClusters_whenApiReturnsClusters() throws ApiException {
        ClustersV1DTO response = new ClustersV1DTO();
        ClusterV1DTO cluster = new ClusterV1DTO();
        cluster.setId(CLUSTER_ID);
        cluster.setName("Test Cluster");
        cluster.setCapacity(100);
        cluster.setAvailableCapacity(50);
        cluster.setFloor(2);
        response.setClusters(List.of(cluster));

        when(campusApi.getClustersByCampus(CAMPUS_ID)).thenReturn(response);

        campusService.getClustersByCampus(CAMPUS_ID);

        verify(clusterRepository).saveAllAndFlush(anyList());
    }
}
