package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.izpz.edu.model.Participant;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, String> {
    @Query("select p.login as login, p.status as status from Participant p where p.login in :logins")
    List<ParticipantView> findAllViewByLoginIn(@Param("logins") List<String> logins);
}
