package ru.izpz.edu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.CampusDto;
import ru.izpz.dto.Clusters;
import ru.izpz.dto.ProjectsDto;
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.edu.mapper.CampusMapper;
import ru.izpz.edu.mapper.ProjectsMapper;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.repository.WorkplaceRepository;
import ru.izpz.edu.service.provider.ProjectsProvider;
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
    private final ProjectsMapper projectsMapper;
    private final WorkplaceProvider workplaceProvider;
    private final ProjectsProvider projectsProvider;
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

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CampusSnapshot getCampusSnapshot(CampusDto campus) {
        List<Clusters> clusters = getClusters(campus);
        Map<String, Long> programStats = getProgramStatsByCampusId(campus.getUuid());
        return new CampusSnapshot(clusters, programStats);
    }

    public Map<String, Long> getProgramStatsByCampusId(String campusId) {
        List<WorkplaceRepository.StageNameCountView> rows = workplaceRepository
            .countParticipantsByCampusIdAndStageName(campusId).stream()
            .sorted(Comparator.comparing(row -> normalizeProgramName(row.getStageName())))
            .toList();

        Map<String, Long> result = new LinkedHashMap<>();
        for (WorkplaceRepository.StageNameCountView row : rows) {
            result.merge(normalizeProgramName(row.getStageName()), row.getCount(), Long::sum);
        }
        return result;
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

    public void replaceCampusSnapshotByCampusId(String campusId, List<ClusterV1DTO> clustersDto, List<Workplace> workplaces) {
        var clusters = clustersDto.stream()
            .map(dto -> campusMapper.toClusterEntity(dto, campusId))
            .toList();
        persistenceService.replaceCampusSnapshot(campusId, clusters, workplaces);

        clusters.forEach(cluster -> {
            int freePlaces = toNonNegative(cluster.getAvailableCapacity());
            int totalCapacity = toNonNegative(cluster.getCapacity());
            int occupiedPlaces = Math.max(totalCapacity - freePlaces, 0);
            schedulerMetricsService.recordClusterPlaces(campusId, cluster.getName(), freePlaces, occupiedPlaces);
        });
    }

    public List<Cluster> findAllByOrderByCampusIdAsc() {
        return persistenceService.findAllByOrderByCampusIdAsc();
    }

    public List<Cluster> findAllByCampusIdOrderByFloorAsc(String campusId) {
        return persistenceService.findAllByCampusIdOrderByFloorAsc(campusId);
    }

    public List<ProjectsDto> getStudentProjectsByLogin(String login) {
        var projects = projectsProvider.getStudentProjectsByLogin(login);
        return projects.stream()
                .map(projectsMapper::toDto)
                .toList();
    }

    /**
     * Fetch participants using configured provider.
     * @param clusterId the cluster ID
     * @throws ApiException if provider call fails
     */
    public List<Workplace> fetchParticipantsByClusterWithProvider(Long clusterId) throws ApiException {
        return workplaceProvider.fetchParticipantsByCluster(clusterId);
    }

    public void replaceParticipantsByCampusId(String campusId, List<Workplace> workplaces) {
        persistenceService.replaceParticipantsByCampusId(campusId, workplaces);
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

    public record CampusSnapshot(List<Clusters> clusters, Map<String, Long> programStats) {
    }
}
