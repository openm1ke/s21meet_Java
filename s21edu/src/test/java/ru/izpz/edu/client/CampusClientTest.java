package ru.izpz.edu.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.api.CampusApi;
import ru.izpz.dto.api.ClusterApi;
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.dto.model.ClustersV1DTO;
import ru.izpz.dto.model.ClusterMapV1DTO;
import ru.izpz.dto.model.WorkplaceV1DTO;
import ru.izpz.edu.service.GraphQLService;
import ru.izpz.edu.dto.GraphQLStudentProject;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampusClientTest {

    @Mock
    private CampusApi campusApi;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private GraphQLService graphQLService;

    @InjectMocks
    private CampusClient campusClient;

    @Test
    void getClustersByCampus_shouldThrow_whenResponseNull() throws Exception {
        String campusId = UUID.randomUUID().toString();
        when(campusApi.getClustersByCampus(UUID.fromString(campusId))).thenReturn(null);

        assertThrows(ApiException.class, () -> campusClient.getClustersByCampus(campusId));
    }

    @Test
    void getClustersByCampus_shouldReturnClusters_whenResponseNotNull() throws Exception {
        String campusId = UUID.randomUUID().toString();

        ClusterV1DTO c1 = new ClusterV1DTO();
        ClustersV1DTO resp = new ClustersV1DTO();
        resp.setClusters(List.of(c1));

        when(campusApi.getClustersByCampus(UUID.fromString(campusId))).thenReturn(resp);

        List<ClusterV1DTO> result = campusClient.getClustersByCampus(campusId);

        assertEquals(1, result.size());
        assertSame(c1, result.getFirst());
    }

    @Test
    void getParticipantsByCluster_shouldThrow_whenResponseNull() throws Exception {
        when(clusterApi.getParticipantsByCoalitionId1(1L, 1000, 0, true)).thenReturn(null);

        assertThrows(ApiException.class, () -> campusClient.getParticipantsByCluster(1L));
    }

    @Test
    void getParticipantsByCluster_shouldReturnMap_whenResponseNotNull() throws Exception {
        WorkplaceV1DTO w = new WorkplaceV1DTO();
        ClusterMapV1DTO resp = new ClusterMapV1DTO();
        resp.setClusterMap(List.of(w));

        when(clusterApi.getParticipantsByCoalitionId1(1L, 1000, 0, true)).thenReturn(resp);

        List<WorkplaceV1DTO> result = campusClient.getParticipantsByCluster(1L);

        assertEquals(1, result.size());
        assertSame(w, result.getFirst());
    }

    @Test
    void getParticipantsByClusterV2_shouldDelegateToGraphqlService() {
        GraphQLService.ClusterSeat seat = new GraphQLService.ClusterSeat("1", "A", 1, "u", null, null, null, null);
        when(graphQLService.getOccupiedSeats("1")).thenReturn(List.of(seat));

        List<GraphQLService.ClusterSeat> result = campusClient.getParticipantsByClusterV2(1L);

        assertEquals(1, result.size());
        verify(graphQLService).getOccupiedSeats("1");
    }

    @Test
    void getStudentProjectsByLogin_shouldDelegateToGraphqlService() {
        GraphQLStudentProject p = new GraphQLStudentProject("g", "n", "d", 1, "dt", 1, 1, "e", "gs", "ct", "ds", 1, 1, 1, 1, 1, 1, "grp", 1);
        when(graphQLService.getStudentProjectsByLogin("login")).thenReturn(List.of(p));

        List<GraphQLStudentProject> result = campusClient.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        verify(graphQLService).getStudentProjectsByLogin("login");
    }
}
