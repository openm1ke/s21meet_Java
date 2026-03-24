package ru.izpz.edu.service.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.api.CoalitionApi;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.dto.model.ParticipantCoalitionV1DTO;
import ru.izpz.dto.model.ParticipantLoginsV1DTO;
import ru.izpz.edu.config.CoalitionProviderConfig;
import ru.izpz.edu.model.StudentCoalition;
import ru.izpz.edu.repository.StudentCoalitionRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestApiCoalitionProviderTest {

    @Mock
    private ParticipantApi participantApi;
    @Mock
    private CoalitionApi coalitionApi;
    @Mock
    private StudentCoalitionRepository studentCoalitionRepository;

    private CoalitionProviderConfig.CoalitionProperties properties;

    private RestApiCoalitionProvider provider;

    @BeforeEach
    void setUp() {
        properties = new CoalitionProviderConfig.CoalitionProperties();
        provider = new RestApiCoalitionProvider(participantApi, coalitionApi, studentCoalitionRepository, properties);
    }

    @Test
    void refreshCoalitionByLogin_shouldSkipMemberCount_whenToggleOff() throws ApiException {
        ParticipantCoalitionV1DTO dto = new ParticipantCoalitionV1DTO();
        dto.setCoalitionId(319L);
        dto.setName("Capybaras");
        dto.setRank(271);

        StudentCoalition existing = new StudentCoalition();
        existing.setLogin("testuser");
        existing.setMemberCount(1085);

        when(participantApi.getCoalitionByLogin("testuser")).thenReturn(dto);
        when(studentCoalitionRepository.findById("testuser")).thenReturn(Optional.of(existing));
        when(studentCoalitionRepository.save(any(StudentCoalition.class))).thenAnswer(invocation -> invocation.getArgument(0));

        provider.refreshCoalitionByLogin("testuser");

        assertEquals("Capybaras", existing.getCoalitionName());
        assertEquals(Integer.valueOf(271), existing.getRank());
        assertEquals(Integer.valueOf(1085), existing.getMemberCount());
        verify(coalitionApi, never()).getParticipantsByCoalitionId(any(), any(), any());
    }

    @Test
    void refreshCoalitionByLogin_shouldCountMembersWithPagination_whenToggleOn() throws ApiException {
        properties.getRest().setFetchMemberCount(true);
        properties.getRest().setPageSize(2);

        ParticipantCoalitionV1DTO dto = new ParticipantCoalitionV1DTO();
        dto.setCoalitionId(319L);
        dto.setName("Capybaras");
        dto.setRank(271);

        StudentCoalition entity = new StudentCoalition();
        entity.setLogin("testuser");

        ParticipantLoginsV1DTO page1 = new ParticipantLoginsV1DTO();
        page1.setParticipants(List.of("u1", "u2"));
        ParticipantLoginsV1DTO page2 = new ParticipantLoginsV1DTO();
        page2.setParticipants(List.of("u3"));

        when(participantApi.getCoalitionByLogin("testuser")).thenReturn(dto);
        when(studentCoalitionRepository.findById("testuser")).thenReturn(Optional.of(entity));
        when(coalitionApi.getParticipantsByCoalitionId(319L, 2, 0)).thenReturn(page1);
        when(coalitionApi.getParticipantsByCoalitionId(319L, 2, 2)).thenReturn(page2);
        when(studentCoalitionRepository.save(any(StudentCoalition.class))).thenAnswer(invocation -> invocation.getArgument(0));

        provider.refreshCoalitionByLogin("testuser");

        assertEquals(Integer.valueOf(3), entity.getMemberCount());
        assertEquals("Capybaras", entity.getCoalitionName());
        assertEquals(Integer.valueOf(271), entity.getRank());
        verify(coalitionApi).getParticipantsByCoalitionId(319L, 2, 0);
        verify(coalitionApi).getParticipantsByCoalitionId(319L, 2, 2);
    }

    @Test
    void refreshCoalitionByLogin_shouldSetNullMemberCount_whenCoalitionIdIsNull() throws ApiException {
        properties.getRest().setFetchMemberCount(true);

        ParticipantCoalitionV1DTO dto = new ParticipantCoalitionV1DTO();
        dto.setCoalitionId(null);
        dto.setName("Capybaras");
        dto.setRank(271);

        StudentCoalition entity = new StudentCoalition();
        entity.setLogin("testuser");
        entity.setMemberCount(12);

        when(participantApi.getCoalitionByLogin("testuser")).thenReturn(dto);
        when(studentCoalitionRepository.findById("testuser")).thenReturn(Optional.of(entity));
        when(studentCoalitionRepository.save(any(StudentCoalition.class))).thenAnswer(invocation -> invocation.getArgument(0));

        provider.refreshCoalitionByLogin("testuser");

        assertNull(entity.getMemberCount());
        verify(coalitionApi, never()).getParticipantsByCoalitionId(any(), any(), any());
    }

    @Test
    void refreshCoalitionByLogin_shouldSetNullMemberCount_whenCoalitionApiFails() throws ApiException {
        properties.getRest().setFetchMemberCount(true);
        properties.getRest().setPageSize(2);

        ParticipantCoalitionV1DTO dto = new ParticipantCoalitionV1DTO();
        dto.setCoalitionId(319L);
        dto.setName("Capybaras");
        dto.setRank(271);

        StudentCoalition entity = new StudentCoalition();
        entity.setLogin("testuser");

        when(participantApi.getCoalitionByLogin("testuser")).thenReturn(dto);
        when(studentCoalitionRepository.findById("testuser")).thenReturn(Optional.of(entity));
        when(coalitionApi.getParticipantsByCoalitionId(319L, 2, 0)).thenThrow(new ApiException("boom"));
        when(studentCoalitionRepository.save(any(StudentCoalition.class))).thenAnswer(invocation -> invocation.getArgument(0));

        provider.refreshCoalitionByLogin("testuser");

        assertNull(entity.getMemberCount());
    }

    @Test
    void refreshCoalitionByLogin_shouldClampMemberCountToIntegerMaxValue() throws ApiException {
        properties.getRest().setFetchMemberCount(true);
        properties.getRest().setPageSize(Integer.MAX_VALUE);

        ParticipantCoalitionV1DTO dto = new ParticipantCoalitionV1DTO();
        dto.setCoalitionId(319L);
        dto.setName("Capybaras");
        dto.setRank(271);

        StudentCoalition entity = new StudentCoalition();
        entity.setLogin("testuser");

        ParticipantLoginsV1DTO hugePage = new ParticipantLoginsV1DTO();
        @SuppressWarnings("unchecked")
        List<String> hugeList = mock(List.class);
        when(hugeList.size()).thenReturn(Integer.MAX_VALUE);
        hugePage.setParticipants(hugeList);

        ParticipantLoginsV1DTO tailPage = new ParticipantLoginsV1DTO();
        tailPage.setParticipants(List.of("u1"));

        when(participantApi.getCoalitionByLogin("testuser")).thenReturn(dto);
        when(studentCoalitionRepository.findById("testuser")).thenReturn(Optional.of(entity));
        when(coalitionApi.getParticipantsByCoalitionId(319L, Integer.MAX_VALUE, 0)).thenReturn(hugePage);
        when(coalitionApi.getParticipantsByCoalitionId(319L, Integer.MAX_VALUE, Integer.MAX_VALUE)).thenReturn(tailPage);
        when(studentCoalitionRepository.save(any(StudentCoalition.class))).thenAnswer(invocation -> invocation.getArgument(0));

        provider.refreshCoalitionByLogin("testuser");

        assertEquals(Integer.valueOf(Integer.MAX_VALUE), entity.getMemberCount());
    }
}
