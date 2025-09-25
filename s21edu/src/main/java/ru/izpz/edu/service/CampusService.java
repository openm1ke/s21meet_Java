package ru.izpz.edu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.dto.CampusDto;
import ru.izpz.dto.Clusters;
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.dto.model.WorkplaceV1DTO;
import ru.izpz.edu.mapper.CampusMapper;
import ru.izpz.edu.model.Cluster;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "campus.service.enabled", havingValue = "true")
public class CampusService {

    private final GraphQLService graphQLService;
    private final CampusPersistenceService persistenceService;
    private final CampusMapper campusMapper;

    public List<Clusters> getClusters(CampusDto campus) {
        return persistenceService.findAllByCampusIdOrderByFloorAsc(campus.getUuid()).stream()
            .map(cluster -> Clusters.builder()
                .name(cluster.getName())
                .capacity(cluster.getCapacity())
                .availableCapacity(cluster.getAvailableCapacity())
                .floor(cluster.getFloor())
                .build())
            .toList();
    }

    public void replaceClustersByCampusId(String campusId, List<ClusterV1DTO> clustersDto) {
        if (!clustersDto.isEmpty()) {
            var clusters = clustersDto.stream()
                .map(dto -> campusMapper.toClusterEntity(dto, campusId))
            .toList();
            persistenceService.replaceClusters(campusId, clusters);
        }
    }

    public List<Cluster> findAllByOrderByCampusIdAsc() {
        return persistenceService.findAllByOrderByCampusIdAsc();
    }

    public void replaceParticipantsByClusterId(Long clusterId, List<WorkplaceV1DTO> workplacesDto) {
        if (!workplacesDto.isEmpty()) {
            var workplaces = workplacesDto.stream()
                .map(dto -> campusMapper.toWorkplaceEntity(dto, clusterId))
            .toList();
            persistenceService.replaceParticipants(clusterId, workplaces);
        }
    }
}
