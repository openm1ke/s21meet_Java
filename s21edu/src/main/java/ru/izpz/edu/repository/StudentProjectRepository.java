package ru.izpz.edu.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.izpz.dto.ProjectExecutorDto;
import ru.izpz.edu.model.StudentProject;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface StudentProjectRepository extends JpaRepository<StudentProject, UUID> {
  List<StudentProject> findAllByLoginAndSnapshotFalseOrderBySortOrderAsc(String login);

  void deleteByLogin(String login);

  @Query("select max(sp.updatedAt) from StudentProject sp where sp.login = :login")
  OffsetDateTime findMaxUpdatedAtByLogin(@Param("login") String login);

  @Query(
      """
            select distinct sp.name
            from StudentProject sp
            where sp.snapshot = false and sp.name is not null and sp.name <> ''
            order by sp.name
            """)
  List<String> findDistinctActualProjectNames();

  @Query(
      """
            select new ru.izpz.dto.ProjectExecutorDto(
                sp.login,
                c.campusName,
                sp.goalStatus,
                null,
                p.className
            )
            from StudentProject sp
            left join Participant p on p.login = sp.login
            left join p.campus c
            where sp.snapshot = false
              and lower(sp.name) like lower(concat('%', :projectName, '%'))
              escape '\\'
            """)
  List<ProjectExecutorDto> findExecutorsByProjectName(@Param("projectName") String projectName);

  @Query(
      value =
          """
            select new ru.izpz.dto.ProjectExecutorDto(
                sp.login,
                c.campusName,
                sp.goalStatus,
                null,
                p.className
            )
            from StudentProject sp
            left join Participant p on p.login = sp.login
            left join p.campus c
            where sp.snapshot = false
              and lower(sp.name) like lower(concat('%', :projectName, '%'))
              escape '\\'
              and (:campusesEmpty = true or upper(coalesce(c.campusName, '')) in :campuses)
              and (:statusesEmpty = true or sp.goalStatus in :statuses)
            """,
      countQuery =
          """
            select count(sp)
            from StudentProject sp
            left join Participant p on p.login = sp.login
            left join p.campus c
            where sp.snapshot = false
              and lower(sp.name) like lower(concat('%', :projectName, '%'))
              escape '\\'
              and (:campusesEmpty = true or upper(coalesce(c.campusName, '')) in :campuses)
              and (:statusesEmpty = true or sp.goalStatus in :statuses)
            """)
  Page<ProjectExecutorDto> findExecutorsByProjectNamePaged(
      @Param("projectName") String projectName,
      @Param("campuses") List<String> campuses,
      @Param("campusesEmpty") boolean campusesEmpty,
      @Param("statuses") List<String> statuses,
      @Param("statusesEmpty") boolean statusesEmpty,
      Pageable pageable);

  @Query(
      """
            select distinct c.campusName
            from StudentProject sp
            left join Participant p on p.login = sp.login
            left join p.campus c
            where sp.snapshot = false
              and lower(sp.name) like lower(concat('%', :projectName, '%')) escape '\\'
              and c.campusName is not null
            """)
  List<String> findDistinctCampusesByProjectName(@Param("projectName") String projectName);

  @Query(
      """
            select distinct sp.goalStatus
            from StudentProject sp
            where sp.snapshot = false
              and lower(sp.name) like lower(concat('%', :projectName, '%')) escape '\\'
              and sp.goalStatus is not null
            """)
  List<String> findDistinctStatusesByProjectName(@Param("projectName") String projectName);
}
