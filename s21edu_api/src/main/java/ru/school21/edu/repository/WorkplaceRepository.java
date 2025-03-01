package ru.school21.edu.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.school21.edu.model.Workplace;
import ru.school21.edu.model.WorkplaceId;

@Repository
public interface WorkplaceRepository extends JpaRepository<Workplace, WorkplaceId> {
    @Transactional
    void deleteByIdClusterId(Long clusterId);

    boolean existsByLogin(String login);
}
