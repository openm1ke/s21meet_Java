package ru.school21.edu.service;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import ru.school21.edu.ApiClient;
import ru.school21.edu.ApiException;
import ru.school21.edu.api.CampusApi;
import ru.school21.edu.model.CampusesV1DTO;
import ru.school21.edu.model.ClustersV1DTO;
import ru.school21.edu.model.CoalitionsV1DTO;
import ru.school21.edu.model.ParticipantLoginsV1DTO;

import java.util.UUID;

@Slf4j
@Service
public class CampusApiProxy extends CampusApi {

    @Autowired
    public CampusApiProxy(ApiClient apiClient) {
        super(apiClient);
    }

    @Override
    @Retryable(backoff = @Backoff(delay = 2000))
    @RateLimiter(name = "campusApi", fallbackMethod = "fallbackGetCampuses")
    public CampusesV1DTO getCampuses() throws ApiException {
        log.info("📡 Запрос списка кампусов...");
        return super.getCampuses();
    }

    @Override
    @Retryable(backoff = @Backoff(delay = 2000))
    @RateLimiter(name = "campusApi", fallbackMethod = "fallbackGetCoalitionsByCampus")
    public CoalitionsV1DTO getCoalitionsByCampus(UUID campusId, Integer limit, Integer offset) throws ApiException {
        log.info("📡 Запрос коалиций для кампуса {}...", campusId);
        return super.getCoalitionsByCampus(campusId, limit, offset);
    }

    @Override
    @Retryable(backoff = @Backoff(delay = 2000))
    @RateLimiter(name = "campusApi", fallbackMethod = "fallbackGetParticipantsByCampus")
    public ParticipantLoginsV1DTO getParticipantsByCampusId(UUID campusId, Long limit, Long offset) throws ApiException {
        log.info("📡 Запрос участников для кампуса {}...", campusId);
        return super.getParticipantsByCampusId(campusId, limit, offset);
    }

    @Override
    @Retryable(backoff = @Backoff(delay = 2000))
    @RateLimiter(name = "campusApi", fallbackMethod = "fallbackGetClustersByCampus")
    public ClustersV1DTO getClustersByCampus(UUID campusId) throws ApiException {
        log.info("📡 Запрос кластеров для кампуса {}...", campusId);
        return super.getClustersByCampus(campusId);
    }

    public ClustersV1DTO fallbackGetClustersByCampus(UUID campusId, Throwable t) throws ApiException {
        log.warn("⚠️ RateLimiter сработал! Произошла ошибка при загрузке кластеров для кампуса {}. Ошибка: {}", campusId, t.getMessage());
        throw new ApiException(t.getMessage());
    }

    public CampusesV1DTO fallbackGetCampuses(Throwable t) throws ApiException {
        log.warn("⚠️ RateLimiter сработал! Произошла ошибка при загрузке кампусов. Ошибка: {}", t.getMessage());
        throw new ApiException(t.getMessage());
    }

    public CoalitionsV1DTO fallbackGetCoalitionsByCampus(UUID campusId, Integer limit, Integer offset, Throwable t) throws ApiException {
        log.warn("⚠️ RateLimiter сработал! Произошла ошибка при загрузке коалиции для кампуса {}. Ошибка: {}", campusId, t.getMessage());
        throw new ApiException(t.getMessage());
    }

    public ParticipantLoginsV1DTO fallbackGetParticipantsByCampus(UUID campusId, Long limit, Long offset, Throwable t) throws ApiException {
        log.warn("⚠️ RateLimiter сработал! Произошла ошибка при загрузке участников для кампуса {}. Ошибка: {}", campusId, t.getMessage());
        throw new ApiException(t.getMessage());
    }
}
