package ru.izpz.edu.service.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.edu.mapper.CampusMapper;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.model.WorkplaceId;
import ru.izpz.edu.service.GraphQLService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphQLWorkplaceProviderTest {

    @Mock
    private GraphQLService graphQLService;

    @Mock
    private CampusMapper campusMapper;

    @InjectMocks
    private GraphQLWorkplaceProvider provider;

    @Test
    void fetchParticipantsByCluster_shouldReturnEmpty_whenNoSeats() {
        when(graphQLService.getOccupiedSeats("1")).thenReturn(List.of());

        List<Workplace> result = provider.fetchParticipantsByCluster(1L);

        assertTrue(result.isEmpty());
        verify(campusMapper, never()).toWorkplaceEntityV2(any(), anyLong());
    }

    @Test
    void fetchParticipantsByCluster_shouldReturnMappedList_whenSeatsPresent() {
        GraphQLService.ClusterSeat seat = new GraphQLService.ClusterSeat(
                "1", "A", 101, "login", 10, 1, "g", "s"
        );
        when(graphQLService.getOccupiedSeats("1")).thenReturn(List.of(seat));

        Workplace workplace = new Workplace();
        workplace.setId(new WorkplaceId(1L, "A", 101));
        workplace.setLogin("login");
        when(campusMapper.toWorkplaceEntityV2(seat, 1L)).thenReturn(workplace);

        List<Workplace> result = provider.fetchParticipantsByCluster(1L);

        verify(campusMapper).toWorkplaceEntityV2(seat, 1L);
        assertEquals(1, result.size());
        assertSame(workplace, result.getFirst());
    }
}
