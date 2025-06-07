package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.izpz.edu.model.Profile;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByTelegramId(String telegramId);
}
