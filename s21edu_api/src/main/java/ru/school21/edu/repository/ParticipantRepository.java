package ru.school21.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import ru.school21.edu.model.Participant;

@Service
public interface ParticipantRepository extends JpaRepository<Participant, String> {
}
