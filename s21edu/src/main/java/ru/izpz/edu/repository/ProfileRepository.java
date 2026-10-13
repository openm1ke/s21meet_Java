package ru.izpz.edu.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.izpz.edu.model.Profile;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

  Optional<Profile> findByTelegramId(String telegramId);

  boolean existsByS21login(String s21login);

  boolean existsByPendingS21login(String pendingS21login);

  @Modifying
  @Query(
      "update Profile p set p.pendingS21login = null, "
          + "p.pendingRegistrationExpiresAt = null, p.status = ru.izpz.dto.ProfileStatus.CREATED "
          + "where p.s21login is null and p.pendingRegistrationExpiresAt < :now")
  int clearExpiredPendingRegistrations(@Param("now") java.time.OffsetDateTime now);
}
