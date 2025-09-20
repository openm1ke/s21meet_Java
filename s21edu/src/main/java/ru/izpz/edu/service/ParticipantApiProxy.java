package ru.izpz.edu.service;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import ru.izpz.dto.ApiClient;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.dto.model.ParticipantV1DTO;
import ru.izpz.edu.exception.RetryableApiException;

@Slf4j
@Component
@ConditionalOnProperty(name = "participant.api.enabled", havingValue = "true")
public class ParticipantApiProxy extends ParticipantApi {

    @Autowired
    public ParticipantApiProxy(ApiClient apiClient) {
        super(apiClient);
    }

    @Override
    @Retryable(retryFor = {RetryableApiException.class}, maxAttempts = 5, backoff = @Backoff(delay = 2000))
    @RateLimiter(name = "campusApi")
    public ParticipantV1DTO getParticipantByLogin(@NotNull String login) throws ApiException {
        return super.getParticipantByLogin(login);
    }
}
