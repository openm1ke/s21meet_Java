package ru.izpz.edu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.repository.ClusterRepository;
import ru.izpz.edu.repository.WorkplaceRepository;

import java.util.List;

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

    @Transactional(readOnly = true)
    public List<Cluster> findAllByCampusIdOrderByFloorAsc(String uuid) {
        return clusterRepository.findAllByCampusIdOrderByFloorAsc(uuid);
    }

    @Transactional(readOnly = true)
    public List<Cluster> findAllByOrderByCampusIdAsc() {
        return clusterRepository.findAllByOrderByCampusIdAsc();
    }
}
