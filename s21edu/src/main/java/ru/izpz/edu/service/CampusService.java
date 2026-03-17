package ru.izpz.edu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.CampusDto;
import ru.izpz.dto.Clusters;
import ru.izpz.dto.ProjectsDto;
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.mapper.CampusMapper;
import ru.izpz.edu.mapper.ProjectsMapper;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.repository.WorkplaceRepository;
import ru.izpz.edu.service.provider.WorkplaceProvider;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "campus.service.enabled", havingValue = "true")
public class CampusService {

    private final CampusPersistenceService persistenceService;
    private final CampusMapper campusMapper;
    private final CampusClient campusClient;
    private final ProjectsMapper projectsMapper;
    private final WorkplaceProvider workplaceProvider;
    private final SchedulerMetricsService schedulerMetricsService;
    private final WorkplaceRepository workplaceRepository;

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

    public Map<String, Long> getProgramStatsByCampusId(String campusId) {
        return workplaceRepository.countParticipantsByCampusIdAndStageGroupName(campusId).stream()
            .sorted(Comparator.comparing(row -> normalizeProgramName(row.getStageGroupName())))
            .collect(
                LinkedHashMap::new,
                (result, row) -> result.put(normalizeProgramName(row.getStageGroupName()), row.getCount()),
                Map::putAll
            );
    }

    public void replaceClustersByCampusId(String campusId, List<ClusterV1DTO> clustersDto) {
        if (!clustersDto.isEmpty()) {
            var clusters = clustersDto.stream()
                .map(dto -> campusMapper.toClusterEntity(dto, campusId))
            .toList();
            persistenceService.replaceClusters(campusId, clusters);

            clusters.forEach(cluster -> {
                int freePlaces = toNonNegative(cluster.getAvailableCapacity());
                int totalCapacity = toNonNegative(cluster.getCapacity());
                int occupiedPlaces = Math.max(totalCapacity - freePlaces, 0);
                schedulerMetricsService.recordClusterPlaces(campusId, cluster.getName(), freePlaces, occupiedPlaces);
            });
        }
    }

    public List<Cluster> findAllByOrderByCampusIdAsc() {
        return persistenceService.findAllByOrderByCampusIdAsc();
    }

    public List<ProjectsDto> getStudentProjectsByLogin(String login) {
        var projects = campusClient.getStudentProjectsByLogin(login);
        return projects.stream()
                .map(projectsMapper::toDto)
                .toList();
    }

    /**
     * Replace participants using configured provider
     * @param clusterId the cluster ID
     * @throws ApiException if provider call fails
     */
    public void replaceParticipantsByClusterIdWithProvider(Long clusterId) throws ApiException {
        // Delegate to the configured provider which handles everything internally
        workplaceProvider.updateParticipantsByCluster(clusterId);
    }

    public void refreshParticipantMetrics() {
        schedulerMetricsService.resetParticipantMetrics();

        workplaceRepository.countParticipantsByCampus().forEach(row ->
            schedulerMetricsService.recordParticipantsByCampus(row.getCampusId(), row.getCount())
        );

        workplaceRepository.countParticipantsByCampusAndStageGroup().forEach(row ->
            schedulerMetricsService.recordParticipantsByCampusAndStageGroup(
                row.getCampusId(),
                row.getStageGroupName(),
                row.getCount()
            )
        );

        workplaceRepository.countParticipantsByCampusAndStageName().forEach(row ->
            schedulerMetricsService.recordParticipantsByCampusAndStageName(
                row.getCampusId(),
                row.getStageName(),
                row.getCount()
            )
        );
    }

    private int toNonNegative(Integer value) {
        if (value == null || value < 0) {
            return 0;
        }
        return value;
    }

    private String normalizeProgramName(String stageGroupName) {
        if (stageGroupName == null || stageGroupName.isBlank()) {
            return "No data";
        }
        return stageGroupName;
    }
}
