package ru.izpz.edu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.repository.ClusterRepository;
import ru.izpz.edu.repository.WorkplaceRepository;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor
public class CampusPersistenceService {

    private final ClusterRepository clusterRepository;
    private final WorkplaceRepository workplaceRepository;

    @Transactional
    public void replaceClusters(String campusId, List<Cluster> clusters) {
        clusterRepository.deleteAllByCampusId(campusId);
        if (!clusters.isEmpty()) clusterRepository.saveAll(clusters);
    }

    @Transactional
    public void replaceParticipants(long clusterId, List<Workplace> workplaces) {
        workplaceRepository.deleteByIdClusterId(clusterId);
        if (!workplaces.isEmpty()) workplaceRepository.saveAll(workplaces);
    }

    @Transactional
    public void replaceParticipantsByCampusId(String campusId, List<Workplace> workplaces) {
        Set<Long> clusterIds = clusterRepository.findAllByCampusIdOrderByFloorAsc(campusId).stream()
            .map(Cluster::getClusterId)
            .collect(toSet());
        if (!clusterIds.isEmpty()) {
            workplaceRepository.deleteByIdClusterIdIn(clusterIds);
        }
        if (!workplaces.isEmpty()) {
            workplaceRepository.saveAll(workplaces);
        }
    }

    @Transactional
    public void replaceCampusSnapshot(String campusId, List<Cluster> clusters, List<Workplace> workplaces) {
        Set<Long> existingClusterIds = clusterRepository.findAllByCampusIdOrderByFloorAsc(campusId).stream()
            .map(Cluster::getClusterId)
            .collect(toSet());
        if (!existingClusterIds.isEmpty()) {
            workplaceRepository.deleteByIdClusterIdIn(existingClusterIds);
        }
        clusterRepository.deleteAllByCampusId(campusId);
        if (!clusters.isEmpty()) {
            clusterRepository.saveAll(clusters);
        }
        if (!workplaces.isEmpty()) {
            workplaceRepository.saveAll(workplaces);
        }
    }

    @Transactional(readOnly = true)
    public List<Cluster> findAllByCampusIdOrderByFloorAsc(String uuid) {
        return clusterRepository.findAllByCampusIdOrderByFloorAsc(uuid);
    }

    @Transactional(readOnly = true)
    public List<Cluster> findAllByOrderByCampusIdAsc() {
        return clusterRepository.findAllByOrderByCampusIdAsc();
    }
}
