package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.izpz.edu.model.StudentCoalition;

public interface StudentCoalitionRepository extends JpaRepository<StudentCoalition, String> {
}
