package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.izpz.edu.model.Online;

import java.util.Optional;
import java.util.UUID;

public interface OnlineRepository extends JpaRepository<Online, UUID> {

    @Modifying
    @Query("update Online o set o.isOnline = :newStatus where o.login = :login")
    int updateStatusByLogin(@Param("login") String login, @Param("newStatus") boolean newStatus);

    Optional<Online> findByLogin(String login);
}
