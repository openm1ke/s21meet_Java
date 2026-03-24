package ru.izpz.edu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.dto.model.ParticipantV1DTO;
import ru.izpz.dto.ApiException;
import ru.izpz.edu.mapper.ProfileMapper;
import ru.izpz.edu.model.Participant;
import ru.izpz.edu.model.ParticipantCampus;
import ru.izpz.edu.repository.ParticipantCampusRepository;
import ru.izpz.edu.repository.ParticipantRepository;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "profile.service.enabled", havingValue = "true")
public class ParticipantSyncService {

    private final ParticipantApi participantApi;
    private final ProfileMapper profileMapper;
    private final ParticipantRepository participantRepository;
    private final ParticipantCampusRepository participantCampusRepository;

    public ParticipantV1DTO fetchByEduLogin(String eduLogin) throws ApiException {
        return participantApi.getParticipantByLogin(eduLogin);
    }

    @Transactional
    public Participant syncByEduLogin(String eduLogin) throws ApiException {
        return syncByEduLoginInternal(eduLogin);
    }

    @Transactional
    public Participant getOrSyncByEduLogin(String eduLogin) throws ApiException {
        var stored = participantRepository.findByLogin(eduLogin);
        if (stored.isPresent() && stored.get().getCampus() != null) {
            return stored.get();
        }
        return syncByEduLoginInternal(eduLogin);
    }

    private Participant syncByEduLoginInternal(String eduLogin) throws ApiException {
        ParticipantV1DTO participant = fetchByEduLogin(eduLogin);
        if (participant == null) {
            throw new IllegalStateException("Участник не найден для логина " + eduLogin);
        }
        return saveFromDto(participant);
    }

    private Participant saveFromDto(ParticipantV1DTO participantDto) {
        ParticipantCampus campus = profileMapper.toEntity(participantDto.getCampus());
        participantCampusRepository.save(campus);

        Participant participant = profileMapper.toEntity(participantDto);
        participant.setCampus(campus);
        return participantRepository.save(participant);
    }
}
