package ru.izpz.auth.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.izpz.auth.model.TokenEntity;

@Repository
public interface TokenRepository extends JpaRepository<TokenEntity, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from TokenEntity t where t.login = :login")
  Optional<TokenEntity> findForUpdate(@Param("login") String login);
}
