package ru.izpz.edu.service.provider;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.izpz.edu.mapper.CampusMapper;
import ru.izpz.edu.service.CampusPersistenceService;
import ru.izpz.edu.service.GraphQLService;

import java.util.List;

/**
 * GraphQL implementation of WorkplaceProvider
 * Handles data retrieval and persistence internally
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphQLWorkplaceProvider implements WorkplaceProvider {

    private final GraphQLService graphQLService;
    private final CampusMapper campusMapper;
    private final CampusPersistenceService persistenceService;

    @Override
    @RateLimiter(name = "platform")
    @Retry(name = "platform")
    public void updateParticipantsByCluster(Long clusterId) {
        log.debug("Updating participants for cluster {} via GraphQL", clusterId);
        
        List<GraphQLService.ClusterSeat> seats = graphQLService.getOccupiedSeats(String.valueOf(clusterId));
        log.debug("Retrieved {} participants for cluster {} via GraphQL", seats.size(), clusterId);
        
        // Convert and persist directly
        if (!seats.isEmpty()) {
            var workplaces = seats.stream()
                .map(seat -> campusMapper.toWorkplaceEntityV2(seat, clusterId))
                .toList();
            persistenceService.replaceParticipants(clusterId, workplaces);
            log.info("Updated {} participants for cluster {} via GraphQL", workplaces.size(), clusterId);
        }
    }
}
