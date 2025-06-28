package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.izpz.edu.model.Participant;

public interface ParticipantRepository extends JpaRepository<Participant, String> {
}
