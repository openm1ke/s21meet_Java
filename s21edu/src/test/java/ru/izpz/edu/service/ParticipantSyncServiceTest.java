package ru.izpz.edu.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.model.ParticipantCampusV1DTO;
import ru.izpz.dto.model.ParticipantV1DTO;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.edu.mapper.ProfileMapper;
import ru.izpz.edu.model.Participant;
import ru.izpz.edu.model.ParticipantCampus;
import ru.izpz.edu.repository.ParticipantCampusRepository;
import ru.izpz.edu.repository.ParticipantRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipantSyncServiceTest {

    @Mock
    private ParticipantApi participantApi;
    @Mock
    private ProfileMapper profileMapper;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private ParticipantCampusRepository participantCampusRepository;

    @InjectMocks
    private ParticipantSyncService participantSyncService;

    @Test
    void getOrSyncByEduLogin_shouldReturnStored_whenCampusExists() throws ApiException {
        Participant stored = new Participant();
        stored.setLogin("testuser");
        stored.setCampus(new ParticipantCampus());
        stored.setUpdatedAt(OffsetDateTime.now());
        when(participantRepository.findByLogin("testuser")).thenReturn(Optional.of(stored));

        Participant result = participantSyncService.getOrSyncByEduLogin("testuser");

        assertSame(stored, result);
        verify(participantApi, never()).getParticipantByLogin(any());
    }

    @Test
    void getOrSyncByEduLogin_shouldSync_whenStoredWithoutCampus() throws ApiException {
        Participant stored = new Participant();
        stored.setLogin("testuser");
        stored.setCampus(null);
        when(participantRepository.findByLogin("testuser")).thenReturn(Optional.of(stored));

        ParticipantV1DTO dto = participantDto("testuser");
        ParticipantCampus campusEntity = new ParticipantCampus();
        campusEntity.setId(dto.getCampus().getId().toString());
        Participant participantEntity = new Participant();
        participantEntity.setLogin("testuser");

        when(participantApi.getParticipantByLogin("testuser")).thenReturn(dto);
        when(profileMapper.toEntity(dto.getCampus())).thenReturn(campusEntity);
        when(profileMapper.toEntity(dto)).thenReturn(participantEntity);
        when(participantRepository.save(participantEntity)).thenReturn(participantEntity);

        Participant result = participantSyncService.getOrSyncByEduLogin("testuser");

        assertSame(participantEntity, result);
        assertSame(campusEntity, participantEntity.getCampus());
        verify(participantCampusRepository).save(campusEntity);
        verify(participantRepository).save(participantEntity);
    }

    @Test
    void getOrSyncByEduLogin_shouldSync_whenStoredIsStale() throws ApiException {
        Participant stored = new Participant();
        stored.setLogin("testuser");
        stored.setCampus(new ParticipantCampus());
        stored.setUpdatedAt(OffsetDateTime.now().minusHours(1));
        when(participantRepository.findByLogin("testuser")).thenReturn(Optional.of(stored));

        ParticipantV1DTO dto = participantDto("testuser");
        ParticipantCampus campusEntity = new ParticipantCampus();
        campusEntity.setId(dto.getCampus().getId().toString());
        Participant participantEntity = new Participant();
        participantEntity.setLogin("testuser");

        when(participantApi.getParticipantByLogin("testuser")).thenReturn(dto);
        when(profileMapper.toEntity(dto.getCampus())).thenReturn(campusEntity);
        when(profileMapper.toEntity(dto)).thenReturn(participantEntity);
        when(participantRepository.save(participantEntity)).thenReturn(participantEntity);

        Participant result = participantSyncService.getOrSyncByEduLogin("testuser");

        assertSame(participantEntity, result);
        verify(participantApi).getParticipantByLogin("testuser");
        verify(participantCampusRepository).save(campusEntity);
        verify(participantRepository).save(participantEntity);
    }

    @Test
    void getOrSyncByEduLogin_shouldSync_whenUpdatedAtMissing() throws ApiException {
        Participant stored = new Participant();
        stored.setLogin("testuser");
        stored.setCampus(new ParticipantCampus());
        stored.setUpdatedAt(null);
        when(participantRepository.findByLogin("testuser")).thenReturn(Optional.of(stored));

        ParticipantV1DTO dto = participantDto("testuser");
        ParticipantCampus campusEntity = new ParticipantCampus();
        campusEntity.setId(dto.getCampus().getId().toString());
        Participant participantEntity = new Participant();
        participantEntity.setLogin("testuser");

        when(participantApi.getParticipantByLogin("testuser")).thenReturn(dto);
        when(profileMapper.toEntity(dto.getCampus())).thenReturn(campusEntity);
        when(profileMapper.toEntity(dto)).thenReturn(participantEntity);
        when(participantRepository.save(participantEntity)).thenReturn(participantEntity);

        Participant result = participantSyncService.getOrSyncByEduLogin("testuser");

        assertSame(participantEntity, result);
        verify(participantApi).getParticipantByLogin("testuser");
        verify(participantCampusRepository).save(campusEntity);
        verify(participantRepository).save(participantEntity);
    }

    @Test
    void syncByEduLogin_shouldSync_whenParticipantMissingInStorage() throws ApiException {
        ParticipantV1DTO dto = participantDto("freshuser");
        ParticipantCampus campusEntity = new ParticipantCampus();
        campusEntity.setId(dto.getCampus().getId().toString());
        Participant participantEntity = new Participant();
        participantEntity.setLogin("freshuser");

        when(participantApi.getParticipantByLogin("freshuser")).thenReturn(dto);
        when(profileMapper.toEntity(dto.getCampus())).thenReturn(campusEntity);
        when(profileMapper.toEntity(dto)).thenReturn(participantEntity);
        when(participantRepository.save(participantEntity)).thenReturn(participantEntity);

        Participant result = participantSyncService.syncByEduLogin("freshuser");

        assertSame(participantEntity, result);
        assertNotNull(participantEntity.getUpdatedAt());
        verify(participantCampusRepository).save(campusEntity);
        verify(participantRepository).save(participantEntity);
    }

    @Test
    void syncByEduLogin_shouldThrow_whenApiReturnsNull() throws ApiException {
        when(participantApi.getParticipantByLogin("ghost")).thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> participantSyncService.syncByEduLogin("ghost")
        );

        assertEquals("Участник не найден для логина ghost", exception.getMessage());
        verify(participantCampusRepository, never()).save(any());
        verify(participantRepository, never()).save(any());
    }

    private ParticipantV1DTO participantDto(String login) {
        ParticipantCampusV1DTO campus = new ParticipantCampusV1DTO();
        campus.setId(UUID.randomUUID());
        campus.setShortName("KZN");

        ParticipantV1DTO dto = new ParticipantV1DTO();
        dto.setLogin(login);
        dto.setClassName("B17");
        dto.setParallelName("java");
        dto.setExpValue(1200L);
        dto.setLevel(5);
        dto.setExpToNextLevel(300L);
        dto.setStatus(ParticipantV1DTO.StatusEnum.ACTIVE);
        dto.setCampus(campus);
        return dto;
    }
}
