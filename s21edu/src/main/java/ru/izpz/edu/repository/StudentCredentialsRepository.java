package ru.izpz.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.izpz.edu.model.StudentCredentials;

public interface StudentCredentialsRepository extends JpaRepository<StudentCredentials, String> {
}
