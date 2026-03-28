package ru.izpz.edu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.ParticipantCoalitionDto;
import ru.izpz.dto.ParticipantDto;
import ru.izpz.edu.config.CoalitionProviderConfig;
import ru.izpz.edu.model.StudentCoalition;
import ru.izpz.edu.repository.StudentCoalitionRepository;
import ru.izpz.edu.service.provider.CoalitionProvider;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "profile.service.enabled", havingValue = "true")
public class ParticipantCoalitionService {

    private final StudentCoalitionRepository studentCoalitionRepository;
    private final CoalitionProvider coalitionProvider;
    private final CoalitionProviderConfig.CoalitionProperties coalitionProperties;

    public void refreshByLogin(String login) {
        try {
            coalitionProvider.refreshCoalitionByLogin(login);
        } catch (RuntimeException | ApiException e) {
            log.warn("Не удалось обновить данные коалиции для {}: {}", login, e.getMessage());
        }
    }

    public Optional<ParticipantCoalitionDto> findCoalitionDto(String login) {
        Optional<StudentCoalition> coalition = studentCoalitionRepository.findById(login);
        if (isRefreshRequired(coalition.orElse(null))) {
            refreshByLogin(login);
            coalition = studentCoalitionRepository.findById(login);
        }

        return coalition.map(this::toDto);
    }

    public void enrichParticipant(ParticipantDto dto, String login) {
        findCoalitionDto(login).ifPresent(dto::setCoalition);
    }

    private ParticipantCoalitionDto toDto(StudentCoalition coalition) {
        return new ParticipantCoalitionDto(
                coalition.getCoalitionName(),
                coalition.getMemberCount(),
                coalition.getRank()
        );
    }

    private boolean isRefreshRequired(StudentCoalition coalition) {
        if (coalition == null || coalition.getUpdatedAt() == null) {
            return true;
        }
        return coalition.getUpdatedAt().isBefore(OffsetDateTime.now().minus(coalitionProperties.getRefreshTtl()));
    }
}
