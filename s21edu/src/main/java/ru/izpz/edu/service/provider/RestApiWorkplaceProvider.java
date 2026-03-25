package ru.izpz.edu.service.provider;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.api.ClusterApi;
import ru.izpz.dto.model.WorkplaceV1DTO;
import ru.izpz.edu.mapper.CampusMapper;
import ru.izpz.edu.model.Workplace;

import java.util.List;

/**
 * REST API implementation of WorkplaceProvider.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestApiWorkplaceProvider implements WorkplaceProvider {

    private final ClusterApi clusterApi;
    private final CampusMapper campusMapper;
    @Override
    @RateLimiter(name = "platform")
    @Retry(name = "platform")
    public List<Workplace> fetchParticipantsByCluster(Long clusterId) throws ApiException {
        log.debug("Fetching participants for cluster {} via REST API", clusterId);
        var response = clusterApi.getParticipantsByCoalitionId1(clusterId, 1000, 0, true);

        if (response == null) {
            log.warn("API returned null for cluster {}", clusterId);
            throw new ApiException("API returned null for cluster " + clusterId);
        }

        List<WorkplaceV1DTO> workplacesDto = response.getClusterMap() == null ? List.of() : response.getClusterMap();
        log.debug("Retrieved {} participants for cluster {} via REST API", workplacesDto.size(), clusterId);
        return workplacesDto.stream()
            .map(dto -> campusMapper.toWorkplaceEntity(dto, clusterId))
            .toList();
    }
}
