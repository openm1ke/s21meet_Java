package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.izpz.edu.model.Cluster;

import java.util.List;

public interface ClusterRepository extends JpaRepository<Cluster, Long> {
    List<Cluster> findAllByCampusIdOrderByFloorAsc(String campusId);
}
