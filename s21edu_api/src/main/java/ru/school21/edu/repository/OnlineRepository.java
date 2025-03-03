package ru.school21.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.school21.edu.model.Online;

import java.util.Optional;
import java.util.UUID;

public interface OnlineRepository extends JpaRepository<Online, UUID> {
    Optional<Online> findByLogin(String login);
}
