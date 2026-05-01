package ru.izpz.edu.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.model.ClusterMapV1DTO;
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.dto.model.ClustersV1DTO;
import ru.izpz.dto.model.ParticipantLoginsV1DTO;
import ru.izpz.dto.model.WorkplaceV1DTO;
import ru.izpz.edu.dto.StudentProjectData;
import ru.izpz.edu.exception.PlatformClientException;
import ru.izpz.edu.service.GraphQLService;

@ExtendWith(MockitoExtension.class)
class CampusClientTest {
  private static final String CAMPUS_ID = "00000000-0000-0000-0000-000000000001";
  private static final UUID CAMPUS_UUID = UUID.fromString(CAMPUS_ID);

  @Mock private PlatformApiFacade platformApi;

  @Mock private GraphQLService graphQlService;

  @InjectMocks private CampusClient campusClient;

  @Test
  void getClustersByCampus_shouldThrow_whenResponseNull() {
    when(platformApi.getClustersByCampus(CAMPUS_UUID)).thenReturn(null);

    assertThrows(PlatformClientException.class, () -> campusClient.getClustersByCampus(CAMPUS_ID));
  }

  @Test
  void getClustersByCampus_shouldReturnClusters_whenResponseNotNull() {
    ClusterV1DTO c1 = new ClusterV1DTO();
    ClustersV1DTO resp = new ClustersV1DTO();
    resp.setClusters(List.of(c1));

    when(platformApi.getClustersByCampus(CAMPUS_UUID)).thenReturn(resp);

    List<ClusterV1DTO> result = campusClient.getClustersByCampus(CAMPUS_ID);

    assertEquals(1, result.size());
    assertSame(c1, result.getFirst());
  }

  @Test
  void getParticipantsByCluster_shouldThrow_whenResponseNull() {
    when(platformApi.getParticipantsByClusterId(1L, 1000, 0, true)).thenReturn(null);

    assertThrows(PlatformClientException.class, () -> campusClient.getParticipantsByCluster(1L));
  }

  @Test
  void getParticipantsByCluster_shouldReturnMap_whenResponseNotNull() {
    WorkplaceV1DTO w = new WorkplaceV1DTO();
    ClusterMapV1DTO resp = new ClusterMapV1DTO();
    resp.setClusterMap(List.of(w));

    when(platformApi.getParticipantsByClusterId(1L, 1000, 0, true)).thenReturn(resp);

    List<WorkplaceV1DTO> result = campusClient.getParticipantsByCluster(1L);

    assertEquals(1, result.size());
    assertSame(w, result.getFirst());
  }

  @Test
  void getParticipantsByClusterV2_shouldDelegateToGraphqlService() {
    GraphQLService.ClusterSeat seat =
        new GraphQLService.ClusterSeat("1", "A", 1, "u", null, null, null, null);
    when(graphQlService.getOccupiedSeats("1")).thenReturn(List.of(seat));

    List<GraphQLService.ClusterSeat> result = campusClient.getParticipantsByClusterV2(1L);

    assertEquals(1, result.size());
    verify(graphQlService).getOccupiedSeats("1");
  }

  @Test
  void getStudentProjectsByLogin_shouldDelegateToGraphqlService() {
    StudentProjectData p = new StudentProjectData("g", "n", "d", 1, "dt", 1, 1, "e", "gs", 1, 1);
    when(graphQlService.getStudentProjectsByLogin("login")).thenReturn(List.of(p));

    List<StudentProjectData> result = campusClient.getStudentProjectsByLogin("login");

    assertEquals(1, result.size());
    verify(graphQlService).getStudentProjectsByLogin("login");
  }

  @Test
  void getParticipantsByCampus_shouldThrow_whenResponseNull() {
    when(platformApi.getParticipantsByCampusId(CAMPUS_UUID, 1000L, 0L)).thenReturn(null);

    assertThrows(
        PlatformClientException.class,
        () -> campusClient.getParticipantsByCampus(CAMPUS_ID, 1000L, 0L));
  }

  @Test
  void getParticipantsByCampus_shouldReturnLogins_whenResponseNotNull() {
    ParticipantLoginsV1DTO response = new ParticipantLoginsV1DTO();
    response.setParticipants(List.of("u1", "u2"));
    when(platformApi.getParticipantsByCampusId(CAMPUS_UUID, 1000L, 0L)).thenReturn(response);

    List<String> result = campusClient.getParticipantsByCampus(CAMPUS_ID, 1000L, 0L);

    assertEquals(List.of("u1", "u2"), result);
  }
}
