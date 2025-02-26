package ru.school21.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.school21.edu.model.Cluster;

public interface ClusterRepository extends JpaRepository<Cluster, Long> {
}
