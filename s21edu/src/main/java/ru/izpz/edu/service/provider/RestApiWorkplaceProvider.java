package ru.izpz.edu.service.provider;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.izpz.dto.model.WorkplaceV1DTO;
import ru.izpz.edu.client.PlatformApiFacade;
import ru.izpz.edu.exception.PlatformClientException;
import ru.izpz.edu.mapper.CampusMapper;
import ru.izpz.edu.model.Workplace;

/** REST API implementation of WorkplaceProvider. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestApiWorkplaceProvider implements WorkplaceProvider {

  private final PlatformApiFacade platformApi;
  private final CampusMapper campusMapper;

  @Override
  @RateLimiter(name = "campusWorkplace")
  public List<Workplace> fetchParticipantsByCluster(Long clusterId) {
    log.debug("Fetching participants for cluster {} via REST API", clusterId);
    var response = platformApi.getParticipantsByClusterId(clusterId, 1000, 0, true);

    if (response == null) {
      log.warn("API returned null for cluster {}", clusterId);
      throw new PlatformClientException("API returned null for cluster " + clusterId, null);
    }

    List<WorkplaceV1DTO> workplacesDto =
        response.getClusterMap() == null ? List.of() : response.getClusterMap();
    log.debug(
        "Retrieved {} participants for cluster {} via REST API", workplacesDto.size(), clusterId);
    return workplacesDto.stream()
        .map(dto -> campusMapper.toWorkplaceEntity(dto, clusterId))
        .toList();
  }
}
