package ru.school21.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.school21.edu.model.Workplace;

@Repository
public interface WorkplaceRepository extends JpaRepository<Workplace, Long> {
}
