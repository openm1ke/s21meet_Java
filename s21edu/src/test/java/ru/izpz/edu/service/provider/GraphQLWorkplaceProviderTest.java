package ru.izpz.edu.service.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.edu.mapper.CampusMapper;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.model.WorkplaceId;
import ru.izpz.edu.service.CampusPersistenceService;
import ru.izpz.edu.service.GraphQLService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphQLWorkplaceProviderTest {

    @Mock
    private GraphQLService graphQLService;

    @Mock
    private CampusMapper campusMapper;

    @Mock
    private CampusPersistenceService persistenceService;

    @InjectMocks
    private GraphQLWorkplaceProvider provider;

    @Test
    void updateParticipantsByCluster_shouldDoNothing_whenNoSeats() {
        when(graphQLService.getOccupiedSeats("1")).thenReturn(List.of());

        provider.updateParticipantsByCluster(1L);

        verify(persistenceService, never()).replaceParticipants(anyLong(), anyList());
        verify(campusMapper, never()).toWorkplaceEntityV2(any(), anyLong());
    }

    @Test
    void updateParticipantsByCluster_shouldPersist_whenSeatsPresent() {
        GraphQLService.ClusterSeat seat = new GraphQLService.ClusterSeat(
                "1", "A", 101, "login", 10, 1, "g", "s"
        );
        when(graphQLService.getOccupiedSeats("1")).thenReturn(List.of(seat));

        Workplace workplace = new Workplace();
        workplace.setId(new WorkplaceId(1L, "A", 101));
        workplace.setLogin("login");
        when(campusMapper.toWorkplaceEntityV2(seat, 1L)).thenReturn(workplace);

        provider.updateParticipantsByCluster(1L);

        verify(campusMapper).toWorkplaceEntityV2(seat, 1L);
        verify(persistenceService).replaceParticipants(eq(1L), argThat(list -> list.size() == 1));
    }
}
