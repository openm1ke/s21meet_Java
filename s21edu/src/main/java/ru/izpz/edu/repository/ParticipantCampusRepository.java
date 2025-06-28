package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.izpz.edu.model.ParticipantCampus;

public interface ParticipantCampusRepository extends JpaRepository<ParticipantCampus, String> {
}
