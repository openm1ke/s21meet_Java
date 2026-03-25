package ru.izpz.edu.service.provider;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiException;
import ru.izpz.edu.mapper.CampusMapper;
import ru.izpz.edu.service.GraphQLService;
import ru.izpz.edu.model.Workplace;

import java.util.List;

/**
 * GraphQL implementation of WorkplaceProvider.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphQLWorkplaceProvider implements WorkplaceProvider {

    private final GraphQLService graphQLService;
    private final CampusMapper campusMapper;
    @Override
    @RateLimiter(name = "platform")
    @Retry(name = "platform")
    public List<Workplace> fetchParticipantsByCluster(Long clusterId) {
        log.debug("Fetching participants for cluster {} via GraphQL", clusterId);
        List<GraphQLService.ClusterSeat> seats = graphQLService.getOccupiedSeats(String.valueOf(clusterId));
        log.debug("Retrieved {} participants for cluster {} via GraphQL", seats.size(), clusterId);
        return seats.stream()
            .map(seat -> campusMapper.toWorkplaceEntityV2(seat, clusterId))
            .toList();
    }
}
