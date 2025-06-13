package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.izpz.edu.model.ProfileValidation;

import java.util.Optional;

public interface ProfileValidationRepository extends JpaRepository<ProfileValidation, String> {
    Optional<ProfileValidation> findByS21login(String s21login);
}
