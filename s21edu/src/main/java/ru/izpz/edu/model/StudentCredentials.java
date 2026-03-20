package ru.izpz.edu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "student_credentials")
public class StudentCredentials {
    @Id
    String login;
    String studentId;
    String userId;
    String schoolId;
    Boolean isActive;
    Boolean isGraduate;
}
