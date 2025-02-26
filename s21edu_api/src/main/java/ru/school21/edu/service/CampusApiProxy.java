package ru.school21.edu.service;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.school21.edu.ApiException;
import ru.school21.edu.api.CampusApi;
import ru.school21.edu.api.ClusterApi;
import ru.school21.edu.model.CampusesV1DTO;
import ru.school21.edu.model.ClustersV1DTO;
import ru.school21.edu.model.CoalitionsV1DTO;
import ru.school21.edu.model.ParticipantLoginsV1DTO;

import java.util.UUID;

@Slf4j
@Service
public class CampusApiProxy extends CampusApi {

    public CampusApiProxy() {
        super();
    }

    @Override
    @RateLimiter(name = "campusApi", fallbackMethod = "fallbackGetCampuses")
    public CampusesV1DTO getCampuses() throws ApiException {
        log.info("📡 Запрос списка кампусов...");
        return super.getCampuses();
    }

    @Override
    @RateLimiter(name = "campusApi", fallbackMethod = "fallbackGetCoalitionsByCampus")
    public CoalitionsV1DTO getCoalitionsByCampus(UUID campusId, Integer limit, Integer offset) throws ApiException {
        log.info("📡 Запрос коалиций для кампуса {}...", campusId);
        return super.getCoalitionsByCampus(campusId, limit, offset);
    }

    @Override
    @RateLimiter(name = "campusApi", fallbackMethod = "fallbackGetParticipantsByCampus")
    public ParticipantLoginsV1DTO getParticipantsByCampusId(UUID campusId, Long limit, Long offset) throws ApiException {
        log.info("📡 Запрос участников для кампуса {}...", campusId);
        return super.getParticipantsByCampusId(campusId, limit, offset);
    }

    @Override
    @RateLimiter(name = "campusApi", fallbackMethod = "fallbackGetClustersByCampus")
    public ClustersV1DTO getClustersByCampus(UUID campusId) throws ApiException {
        log.info("📡 Запрос кластеров для кампуса {}...", campusId);
        return super.getClustersByCampus(campusId);
    }

    public ClustersV1DTO fallbackGetClustersByCampus(UUID campusId, Throwable t) {
        log.warn("⚠️ RateLimiter сработал! Пропускаем кластеры для кампуса {}. Ошибка: {}", campusId, t.getMessage());
        return new ClustersV1DTO();
    }

    public CampusesV1DTO fallbackGetCampuses(Throwable t) {
        log.warn("⚠️ RateLimiter сработал! Пропускаем загрузку кампусов. Ошибка: {}", t.getMessage());
        return new CampusesV1DTO();
    }

    public CoalitionsV1DTO fallbackGetCoalitionsByCampus(UUID campusId, Integer limit, Integer offset, Throwable t) {
        log.warn("⚠️ RateLimiter сработал! Пропускаем коалиции для кампуса {}. Ошибка: {}", campusId, t.getMessage());
        return new CoalitionsV1DTO();
    }

    public ParticipantLoginsV1DTO fallbackGetParticipantsByCampus(UUID campusId, Long limit, Long offset, Throwable t) {
        log.warn("⚠️ RateLimiter сработал! Пропускаем участников для кампуса {}. Ошибка: {}", campusId, t.getMessage());
        return new ParticipantLoginsV1DTO();
    }
}
