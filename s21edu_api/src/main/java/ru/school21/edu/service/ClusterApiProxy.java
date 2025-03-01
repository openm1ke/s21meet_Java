package ru.school21.edu.service;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import ru.school21.edu.ApiClient;
import ru.school21.edu.ApiException;
import ru.school21.edu.api.ClusterApi;
import ru.school21.edu.model.ClusterMapV1DTO;

@Slf4j
@Service
@ConditionalOnProperty(name = "cluster.api.enabled", havingValue = "true", matchIfMissing = true)
public class ClusterApiProxy extends ClusterApi {

    @Autowired
    public ClusterApiProxy(ApiClient apiClient) {
        super(apiClient);
    }

    @Override
    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 2000))
    @RateLimiter(name = "campusApi")
    public ClusterMapV1DTO getParticipantsByCoalitionId1(Long clusterId, Integer limit, Integer offset, Boolean occupied) throws ApiException {
        //log.info("📡 Запрос участников для кластера {}...", clusterId);
        return super.getParticipantsByCoalitionId1(clusterId, limit, offset, occupied);
    }
}