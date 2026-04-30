package ru.izpz.edu.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.izpz.edu.model.ProfileValidation;

public interface ProfileValidationRepository extends JpaRepository<ProfileValidation, String> {
  Optional<ProfileValidation> findByS21login(String s21login);
}
