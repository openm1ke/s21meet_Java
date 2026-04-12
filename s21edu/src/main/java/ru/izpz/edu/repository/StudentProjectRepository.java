package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.edu.model.StudentProject;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface StudentProjectRepository extends JpaRepository<StudentProject, UUID> {
    List<StudentProject> findAllByLoginAndSnapshotFalseOrderBySortOrderAsc(String login);
    void deleteByLogin(String login);

    @Query("select max(sp.updatedAt) from StudentProject sp where sp.login = :login")
    OffsetDateTime findMaxUpdatedAtByLogin(@Param("login") String login);

    @Query("""
            select distinct sp.name
            from StudentProject sp
            where sp.snapshot = false and sp.name is not null and sp.name <> ''
            order by sp.name
            """)
    List<String> findDistinctActualProjectNames();

    @Query("""
            select new ru.izpz.dto.ProjectExecutorDto(
                sp.login,
                c.campusName,
                sp.goalStatus,
                null
            )
            from StudentProject sp
            left join Participant p on p.login = sp.login
            left join p.campus c
            where sp.snapshot = false
              and lower(sp.name) like lower(concat('%', :projectName, '%'))
              escape '\\'
            """)
    List<ProjectExecutorDto> findExecutorsByProjectName(
            @Param("projectName") String projectName);
}
