package ru.izpz.edu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ProfileValidation {
  @Id String s21login;
  String secretCode;
  OffsetDateTime expiresAt;
}
