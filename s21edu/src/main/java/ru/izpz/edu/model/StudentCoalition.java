package ru.izpz.edu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "student_coalition")
public class StudentCoalition {
  @Id String login;
  String userId;
  String coalitionName;
  Integer memberCount;
  Integer rank;
  OffsetDateTime updatedAt;
}
