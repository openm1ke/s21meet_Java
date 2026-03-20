package ru.izpz.edu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "student_coalition")
public class StudentCoalition {
    @Id
    String login;
    String userId;
    String coalitionName;
    Integer memberCount;
    Integer rank;
    OffsetDateTime updatedAt;
}
