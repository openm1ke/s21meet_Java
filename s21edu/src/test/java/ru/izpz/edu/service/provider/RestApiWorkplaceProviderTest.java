package ru.izpz.edu.service.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.api.ClusterApi;
import ru.izpz.dto.model.ClusterMapV1DTO;
import ru.izpz.dto.model.WorkplaceV1DTO;
import ru.izpz.edu.mapper.CampusMapper;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.model.WorkplaceId;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestApiWorkplaceProviderTest {

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private CampusMapper campusMapper;

    @InjectMocks
    private RestApiWorkplaceProvider provider;

    @Test
    void fetchParticipantsByCluster_shouldThrow_whenApiReturnsNull() throws ApiException {
        when(clusterApi.getParticipantsByCoalitionId1(1L, 1000, 0, true)).thenReturn(null);

        assertThrows(ApiException.class, () -> provider.fetchParticipantsByCluster(1L));
    }

    @Test
    void fetchParticipantsByCluster_shouldReturnEmpty_whenEmptyClusterMap() throws Exception {
        ClusterMapV1DTO dto = new ClusterMapV1DTO();
        dto.setClusterMap(List.of());
        when(clusterApi.getParticipantsByCoalitionId1(1L, 1000, 0, true)).thenReturn(dto);

        List<Workplace> result = provider.fetchParticipantsByCluster(1L);

        assertTrue(result.isEmpty());
        verify(campusMapper, never()).toWorkplaceEntity(any(), anyLong());
    }

    @Test
    void fetchParticipantsByCluster_shouldReturnEmpty_whenClusterMapIsNull() throws Exception {
        ClusterMapV1DTO dto = new ClusterMapV1DTO();
        dto.setClusterMap(null);
        when(clusterApi.getParticipantsByCoalitionId1(1L, 1000, 0, true)).thenReturn(dto);

        List<Workplace> result = provider.fetchParticipantsByCluster(1L);

        assertTrue(result.isEmpty());
        verify(campusMapper, never()).toWorkplaceEntity(any(), anyLong());
    }

    @Test
    void fetchParticipantsByCluster_shouldReturnMappedList_whenNonEmptyClusterMap() throws Exception {
        WorkplaceV1DTO w = new WorkplaceV1DTO();
        ClusterMapV1DTO dto = new ClusterMapV1DTO();
        dto.setClusterMap(List.of(w));
        when(clusterApi.getParticipantsByCoalitionId1(1L, 1000, 0, true)).thenReturn(dto);

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
