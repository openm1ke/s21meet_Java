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
import ru.izpz.edu.service.CampusPersistenceService;

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

    @Mock
    private CampusPersistenceService persistenceService;

    @InjectMocks
    private RestApiWorkplaceProvider provider;

    @Test
    void updateParticipantsByCluster_shouldThrow_whenApiReturnsNull() throws ApiException {
        when(clusterApi.getParticipantsByCoalitionId1(1L, 1000, 0, true)).thenReturn(null);

        assertThrows(ApiException.class, () -> provider.updateParticipantsByCluster(1L));
        verify(persistenceService, never()).replaceParticipants(anyLong(), anyList());
    }

    @Test
    void updateParticipantsByCluster_shouldDoNothing_whenEmptyClusterMap() throws Exception {
        ClusterMapV1DTO dto = new ClusterMapV1DTO();
        dto.setClusterMap(List.of());
        when(clusterApi.getParticipantsByCoalitionId1(1L, 1000, 0, true)).thenReturn(dto);

        provider.updateParticipantsByCluster(1L);

        verify(persistenceService, never()).replaceParticipants(anyLong(), anyList());
        verify(campusMapper, never()).toWorkplaceEntity(any(), anyLong());
    }

    @Test
    void updateParticipantsByCluster_shouldPersist_whenNonEmptyClusterMap() throws Exception {
        WorkplaceV1DTO w = new WorkplaceV1DTO();
        ClusterMapV1DTO dto = new ClusterMapV1DTO();
        dto.setClusterMap(List.of(w));
        when(clusterApi.getParticipantsByCoalitionId1(1L, 1000, 0, true)).thenReturn(dto);

        Workplace workplace = new Workplace();
        workplace.setId(new WorkplaceId(1L, "A", 101));
        workplace.setLogin("login");
        when(campusMapper.toWorkplaceEntity(w, 1L)).thenReturn(workplace);

        provider.updateParticipantsByCluster(1L);

        verify(campusMapper).toWorkplaceEntity(w, 1L);
        verify(persistenceService).replaceParticipants(eq(1L), argThat(list -> list.size() == 1));
    }
}
