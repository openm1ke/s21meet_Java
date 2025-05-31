package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.izpz.edu.model.ProfileValidation;

public interface ProfileValidationRepository extends JpaRepository<ProfileValidation, String> {
}
