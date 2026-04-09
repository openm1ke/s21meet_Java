package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.izpz.edu.model.StudentCredentials;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface StudentCredentialsRepository extends JpaRepository<StudentCredentials, String> {
    @Query("select sc.login from StudentCredentials sc where sc.login in :logins")
    List<String> findExistingLogins(@Param("logins") Collection<String> logins);

    @Query("""
        select sc
        from StudentCredentials sc
        where sc.isActive = true
          and sc.userId is not null
          and sc.login > :cursor
        order by sc.login asc
        """)
    List<StudentCredentials> findActiveCredentialsAfter(@Param("cursor") String cursor, Pageable pageable);

    @Query("""
        select sc
        from StudentCredentials sc
        where sc.isActive = true
          and sc.userId is not null
          and sc.schoolId = :schoolId
          and sc.login > :cursor
          and not exists (
              select 1
              from StudentProject sp
              where sp.login = sc.login
                and sp.updatedAt >= :staleBefore
          )
        order by sc.login asc
        """)
    List<StudentCredentials> findStaleActiveCredentialsAfterBySchoolId(
        @Param("cursor") String cursor,
        @Param("schoolId") String schoolId,
        @Param("staleBefore") OffsetDateTime staleBefore,
        Pageable pageable
    );

    @Query("""
        select count(sc)
        from StudentCredentials sc
        where sc.isActive = true
          and sc.userId is not null
          and sc.schoolId = :schoolId
          and not exists (
              select 1
              from StudentProject sp
              where sp.login = sc.login
                and sp.updatedAt >= :staleBefore
          )
        """)
    long countStaleActiveCredentialsBySchoolId(
        @Param("schoolId") String schoolId,
        @Param("staleBefore") OffsetDateTime staleBefore
    );

    @Query("""
        select count(sc)
        from StudentCredentials sc
        where sc.isActive = true
          and sc.userId is not null
          and sc.schoolId = :schoolId
        """)
    long countActiveCredentialsBySchoolId(@Param("schoolId") String schoolId);
}
