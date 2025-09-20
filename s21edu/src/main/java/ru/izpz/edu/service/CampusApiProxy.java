package ru.izpz.edu.service;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiClient;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.api.CampusApi;
import ru.izpz.edu.exception.RetryableApiException;
import ru.izpz.dto.model.CampusesV1DTO;
import ru.izpz.dto.model.ClustersV1DTO;
import ru.izpz.dto.model.CoalitionsV1DTO;
import ru.izpz.dto.model.ParticipantLoginsV1DTO;

import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "campus.api.enabled", havingValue = "true")
public class CampusApiProxy extends CampusApi {

    @Autowired
    public CampusApiProxy(ApiClient apiClient) {
        super(apiClient);
    }

    @Override
    @Retryable(retryFor = {RetryableApiException.class}, maxAttempts = 5, backoff = @Backoff(delay = 2000))
    @RateLimiter(name = "campusApi")
    public CampusesV1DTO getCampuses() throws ApiException {
        //log.info("📡 Запрос списка кампусов...");
        return super.getCampuses();
    }

    @Override
    @Retryable(retryFor = {RetryableApiException.class}, maxAttempts = 5, backoff = @Backoff(delay = 2000))
    @RateLimiter(name = "campusApi")
    public CoalitionsV1DTO getCoalitionsByCampus(UUID campusId, Integer limit, Integer offset) throws ApiException {
        //log.info("📡 Запрос коалиций для кампуса {}...", campusId);
        return super.getCoalitionsByCampus(campusId, limit, offset);
    }

    @Override
    @Retryable(retryFor = {RetryableApiException.class}, maxAttempts = 5, backoff = @Backoff(delay = 2000))
    @RateLimiter(name = "campusApi")
    public ParticipantLoginsV1DTO getParticipantsByCampusId(UUID campusId, Long limit, Long offset) throws ApiException {
        //log.info("📡 Запрос участников для кампуса {}...", campusId);
        return super.getParticipantsByCampusId(campusId, limit, offset);
    }

    @Override
    @Retryable(retryFor = {RetryableApiException.class}, maxAttempts = 5, backoff = @Backoff(delay = 2000))
    @RateLimiter(name = "campusApi")
    public ClustersV1DTO getClustersByCampus(UUID campusId) throws ApiException {
        return super.getClustersByCampus(campusId);
    }
}
