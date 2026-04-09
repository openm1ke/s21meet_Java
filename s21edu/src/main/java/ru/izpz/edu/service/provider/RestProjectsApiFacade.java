package ru.izpz.edu.service.provider;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.dto.model.ParticipantProjectsV1DTO;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"profile.service.enabled", "api.participant.enabled"}, havingValue = "true")
public class RestProjectsApiFacade {

    private final ParticipantApi participantApi;

    @RateLimiter(name = "projectsRest")
    @Retry(name = "projectsRest")
    public ParticipantProjectsV1DTO getParticipantProjectsByLogin(String login, long pageSize, String status) throws ApiException {
        return participantApi.getParticipantProjectsByLogin(login, pageSize, 0L, status);
    }
}
