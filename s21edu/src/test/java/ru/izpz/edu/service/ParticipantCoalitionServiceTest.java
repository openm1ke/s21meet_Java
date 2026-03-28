package ru.izpz.edu.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.ParticipantCoalitionDto;
import ru.izpz.dto.ParticipantDto;
import ru.izpz.edu.config.CoalitionProviderConfig;
import ru.izpz.edu.model.StudentCoalition;
import ru.izpz.edu.repository.StudentCoalitionRepository;
import ru.izpz.edu.service.provider.CoalitionProvider;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipantCoalitionServiceTest {

    @Mock
    private StudentCoalitionRepository studentCoalitionRepository;
    @Mock
    private CoalitionProvider coalitionProvider;
    @Mock
    private CoalitionProviderConfig.CoalitionProperties coalitionProperties;

    @InjectMocks
    private ParticipantCoalitionService participantCoalitionService;

    @Test
    void findCoalitionDto_shouldReturnStoredValue_withoutRefresh() throws ApiException {
        StudentCoalition coalition = new StudentCoalition();
        coalition.setLogin("testuser");
        coalition.setCoalitionName("Capybaras");
        coalition.setMemberCount(1085);
        coalition.setRank(271);
        coalition.setUpdatedAt(OffsetDateTime.now().minusMinutes(5));
        when(studentCoalitionRepository.findById("testuser")).thenReturn(Optional.of(coalition));
        when(coalitionProperties.getRefreshTtl()).thenReturn(Duration.ofMinutes(15));

        Optional<ParticipantCoalitionDto> result = participantCoalitionService.findCoalitionDto("testuser");

        assertTrue(result.isPresent());
        assertEquals("Capybaras", result.get().getName());
        assertEquals(Integer.valueOf(1085), result.get().getMemberCount());
        assertEquals(Integer.valueOf(271), result.get().getRank());
        verify(coalitionProvider, never()).refreshCoalitionByLogin(anyString());
    }

    @Test
    void findCoalitionDto_shouldRefresh_whenMissingInDatabase() throws ApiException {
        StudentCoalition coalition = new StudentCoalition();
        coalition.setLogin("testuser");
        coalition.setCoalitionName("Capybaras");
        coalition.setMemberCount(1085);
        coalition.setRank(271);
        when(studentCoalitionRepository.findById("testuser"))
                .thenReturn(Optional.empty(), Optional.of(coalition));

        Optional<ParticipantCoalitionDto> result = participantCoalitionService.findCoalitionDto("testuser");

        assertTrue(result.isPresent());
        verify(coalitionProvider).refreshCoalitionByLogin("testuser");
        verify(studentCoalitionRepository, times(2)).findById("testuser");
    }

    @Test
    void findCoalitionDto_shouldRefresh_whenStoredValueIsExpired() throws ApiException {
        when(coalitionProperties.getRefreshTtl()).thenReturn(Duration.ofMinutes(15));
        StudentCoalition expired = new StudentCoalition();
        expired.setLogin("testuser");
        expired.setUpdatedAt(OffsetDateTime.now().minusMinutes(30));
        StudentCoalition refreshed = new StudentCoalition();
        refreshed.setLogin("testuser");
        refreshed.setCoalitionName("Capybaras");
        refreshed.setMemberCount(1085);
        refreshed.setRank(271);
        when(studentCoalitionRepository.findById("testuser"))
                .thenReturn(Optional.of(expired), Optional.of(refreshed));

        Optional<ParticipantCoalitionDto> result = participantCoalitionService.findCoalitionDto("testuser");

        assertTrue(result.isPresent());
        assertEquals("Capybaras", result.get().getName());
        verify(coalitionProvider).refreshCoalitionByLogin("testuser");
        verify(studentCoalitionRepository, times(2)).findById("testuser");
    }

    @Test
    void refreshByLogin_shouldNotThrow_whenProviderFails() throws ApiException {
        doThrow(new RuntimeException("boom")).when(coalitionProvider).refreshCoalitionByLogin("testuser");

        assertDoesNotThrow(() -> participantCoalitionService.refreshByLogin("testuser"));
    }

    @Test
    void enrichParticipant_shouldSetCoalition_whenCoalitionExists() {
        StudentCoalition coalition = new StudentCoalition();
        coalition.setLogin("testuser");
        coalition.setCoalitionName("Capybaras");
        coalition.setMemberCount(1085);
        coalition.setRank(271);
        coalition.setUpdatedAt(OffsetDateTime.now().minusMinutes(5));
        when(studentCoalitionRepository.findById("testuser")).thenReturn(Optional.of(coalition));
        when(coalitionProperties.getRefreshTtl()).thenReturn(Duration.ofMinutes(15));

        ParticipantDto participant = new ParticipantDto();
        participantCoalitionService.enrichParticipant(participant, "testuser");

        assertNotNull(participant.getCoalition());
        assertEquals("Capybaras", participant.getCoalition().getName());
    }
}
