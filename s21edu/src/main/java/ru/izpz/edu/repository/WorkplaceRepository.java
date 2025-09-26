package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.model.WorkplaceId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkplaceRepository extends JpaRepository<Workplace, WorkplaceId> {
    void deleteByIdClusterId(Long clusterId);

    boolean existsByLogin(String login);

    @Query("select distinct w.login from Workplace w")
    List<String> findDistinctLogins();

    Optional<Workplace> findByLogin(String telegramId);

    List<Workplace> findAllByLoginIn(Collection<String> logins);
}
