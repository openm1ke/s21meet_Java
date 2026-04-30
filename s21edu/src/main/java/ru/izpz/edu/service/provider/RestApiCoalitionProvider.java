package ru.izpz.edu.service.provider;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.dto.model.ParticipantCoalitionV1DTO;
import ru.izpz.edu.client.PlatformApiFacade;
import ru.izpz.edu.config.CoalitionProviderConfig;
import ru.izpz.edu.exception.PlatformClientException;
import ru.izpz.edu.model.StudentCoalition;
import ru.izpz.edu.repository.StudentCoalitionRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"profile.service.enabled", "api.participant.enabled", "api.coalition.enabled"},
    havingValue = "true")
public class RestApiCoalitionProvider implements CoalitionProvider {

  private final PlatformApiFacade platformApi;
  private final StudentCoalitionRepository studentCoalitionRepository;
  private final CoalitionProviderConfig.CoalitionProperties coalitionProperties;

  @Override
  public void refreshCoalitionByLogin(String login) {
    ParticipantCoalitionV1DTO coalitionDto = platformApi.getCoalitionByLogin(login);
    StudentCoalition entity =
        studentCoalitionRepository.findById(login).orElseGet(StudentCoalition::new);

    entity.setLogin(login);
    entity.setCoalitionName(coalitionDto.getName());
    entity.setRank(coalitionDto.getRank());

    if (coalitionProperties.getRest().isFetchMemberCount()) {
      entity.setMemberCount(fetchMemberCount(coalitionDto.getCoalitionId()));
    }

    entity.setUpdatedAt(OffsetDateTime.now());
    studentCoalitionRepository.save(entity);
  }

  private Integer fetchMemberCount(Long coalitionId) {
    if (coalitionId == null) {
      return null;
    }

    int pageSize = Math.max(1, coalitionProperties.getRest().getPageSize());
    int offset = 0;
    long total = 0L;

    while (true) {
      try {
        var page = platformApi.getParticipantsByCoalitionId(coalitionId, pageSize, offset);
        int pageSizeActual = page != null ? page.getParticipants().size() : 0;
        total += pageSizeActual;

        if (pageSizeActual < pageSize) {
          break;
        }
        offset += pageSize;
      } catch (PlatformClientException e) {
        log.warn(
            "Не удалось получить количество участников для coalitionId={}: {}",
            coalitionId,
            e.getMessage());
        return null;
      }
    }

    if (total > Integer.MAX_VALUE) {
      log.warn(
          "Количество участников коалиции coalitionId={} превышает Integer.MAX_VALUE, значение будет обрезано",
          coalitionId);
      return Integer.MAX_VALUE;
    }
    return (int) total;
  }
}
