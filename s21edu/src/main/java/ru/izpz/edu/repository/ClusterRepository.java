package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.izpz.edu.model.Cluster;

public interface ClusterRepository extends JpaRepository<Cluster, Long> {
}
