package ru.izpz.edu.repository;

import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.izpz.edu.model.Cluster;

public interface ClusterRepository extends JpaRepository<Cluster, Long> {
  List<Cluster> findAllByCampusIdOrderByFloorAsc(String campusId);

  void deleteAllByCampusId(String campusId);

  List<Cluster> findAllByOrderByCampusIdAsc();

  List<Cluster> findAllByClusterIdIn(Set<Long> clusterIds);
}
