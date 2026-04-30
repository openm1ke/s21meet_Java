package ru.izpz.edu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Online {
  @Id
  @GeneratedValue(generator = "UUID")
  @Column(updatable = false, nullable = false)
  UUID id;

  String login;
  Boolean isOnline;
  OffsetDateTime lastSeenAt;
}
