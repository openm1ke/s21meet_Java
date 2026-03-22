package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.izpz.edu.model.StudentProject;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface StudentProjectRepository extends JpaRepository<StudentProject, UUID> {
    List<StudentProject> findAllByLoginAndSnapshotFalseOrderBySortOrderAsc(String login);
    void deleteByLogin(String login);

    @Query("select max(sp.updatedAt) from StudentProject sp where sp.login = :login")
    OffsetDateTime findMaxUpdatedAtByLogin(@Param("login") String login);
}
