package ru.izpz.edu.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.izpz.dto.model.ParticipantV1DTO;
import ru.izpz.edu.client.PlatformApiFacade;
import ru.izpz.edu.exception.PlatformClientException;
import ru.izpz.edu.exception.PlatformNotFoundException;
import ru.izpz.edu.exception.PlatformUnauthorizedException;
import ru.izpz.edu.mapper.ProfileMapper;
import ru.izpz.edu.model.Participant;
import ru.izpz.edu.model.ParticipantCampus;
import ru.izpz.edu.repository.ParticipantCampusRepository;
import ru.izpz.edu.repository.ParticipantRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "profile.service.enabled", havingValue = "true")
public class ParticipantSyncService {
  private final PlatformApiFacade platformApi;
  private final ProfileMapper profileMapper;
  private final ParticipantRepository participantRepository;
  private final ParticipantCampusRepository participantCampusRepository;

  @Value("${participant.refresh-ttl:PT15M}")
  private Duration participantRefreshTtl = Duration.ofMinutes(15);

  public ParticipantV1DTO fetchByEduLogin(String eduLogin) {
    return platformApi.getParticipantByLogin(eduLogin);
  }

  @Transactional
  public Participant syncByEduLogin(String eduLogin) {
    return syncByEduLoginInternal(eduLogin);
  }

  @Transactional
  public Participant getOrSyncByEduLogin(String eduLogin) {
    var stored = participantRepository.findByLogin(eduLogin);
    if (stored.isPresent()
        && stored.get().getCampus() != null
        && !isRefreshRequired(stored.get())) {
      return stored.get();
    }
    try {
      return syncByEduLoginInternal(eduLogin);
    } catch (RuntimeException e) {
      return fallbackParticipantFromCache(eduLogin, e);
    }
  }

  private Participant syncByEduLoginInternal(String eduLogin) {
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
    participant.setUpdatedAt(OffsetDateTime.now());
    return participantRepository.save(participant);
  }

  private boolean isRefreshRequired(Participant participant) {
    if (participant.getUpdatedAt() == null) {
      return true;
    }
    return participant.getUpdatedAt().isBefore(OffsetDateTime.now().minus(participantRefreshTtl));
  }

  @SuppressWarnings("unused")
  private Participant fallbackParticipantFromCache(String eduLogin, Throwable throwable) {
    if (throwable instanceof PlatformNotFoundException
        || throwable instanceof PlatformUnauthorizedException) {
      throw (RuntimeException) throwable;
    }

    return participantRepository
        .findByLogin(eduLogin)
        .filter(participant -> participant.getCampus() != null)
        .map(
            participant -> {
              log.warn(
                  "Fallback для участника {}: используем кэш БД, причина: {}",
                  eduLogin,
                  throwable.getMessage());
              return participant;
            })
        .orElseThrow(
            () ->
                new PlatformClientException(
                    "Временная ошибка получения данных участника, попробуйте позже",
                    503,
                    null,
                    null));
  }
}
