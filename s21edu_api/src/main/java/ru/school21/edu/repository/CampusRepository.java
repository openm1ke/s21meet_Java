package ru.school21.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.school21.edu.model.Campus;

@Repository
public interface CampusRepository extends JpaRepository<Campus, String> {
}

