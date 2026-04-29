package ru.izpz.edu.service.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.model.ClusterMapV1DTO;
import ru.izpz.dto.model.WorkplaceV1DTO;
import ru.izpz.edu.client.PlatformApiFacade;
import ru.izpz.edu.exception.PlatformClientException;
import ru.izpz.edu.mapper.CampusMapper;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.model.WorkplaceId;

@ExtendWith(MockitoExtension.class)
class RestApiWorkplaceProviderTest {

  @Mock private PlatformApiFacade platformApi;

  @Mock private CampusMapper campusMapper;

  @InjectMocks private RestApiWorkplaceProvider provider;

  @Test
  void fetchParticipantsByCluster_shouldThrow_whenApiReturnsNull() {
    when(platformApi.getParticipantsByClusterId(1L, 1000, 0, true)).thenReturn(null);

    assertThrows(PlatformClientException.class, () -> provider.fetchParticipantsByCluster(1L));
  }

  @Test
  void fetchParticipantsByCluster_shouldReturnEmpty_whenEmptyClusterMap() {
    ClusterMapV1DTO dto = new ClusterMapV1DTO();
    dto.setClusterMap(List.of());
    when(platformApi.getParticipantsByClusterId(1L, 1000, 0, true)).thenReturn(dto);

    List<Workplace> result = provider.fetchParticipantsByCluster(1L);

    assertTrue(result.isEmpty());
    verify(campusMapper, never()).toWorkplaceEntity(any(), anyLong());
  }

  @Test
  void fetchParticipantsByCluster_shouldReturnEmpty_whenClusterMapIsNull() {
    ClusterMapV1DTO dto = new ClusterMapV1DTO();
    dto.setClusterMap(null);
    when(platformApi.getParticipantsByClusterId(1L, 1000, 0, true)).thenReturn(dto);

    List<Workplace> result = provider.fetchParticipantsByCluster(1L);

    assertTrue(result.isEmpty());
    verify(campusMapper, never()).toWorkplaceEntity(any(), anyLong());
  }

  @Test
  void fetchParticipantsByCluster_shouldReturnMappedList_whenNonEmptyClusterMap() {
    WorkplaceV1DTO w = new WorkplaceV1DTO();
    ClusterMapV1DTO dto = new ClusterMapV1DTO();
    dto.setClusterMap(List.of(w));
    when(platformApi.getParticipantsByClusterId(1L, 1000, 0, true)).thenReturn(dto);

    Workplace workplace = new Workplace();
    workplace.setId(new WorkplaceId(1L, "A", 101));
    workplace.setLogin("login");
    when(campusMapper.toWorkplaceEntity(w, 1L)).thenReturn(workplace);

    List<Workplace> result = provider.fetchParticipantsByCluster(1L);

    verify(campusMapper).toWorkplaceEntity(w, 1L);
    assertEquals(1, result.size());
    assertSame(workplace, result.getFirst());
  }
}
