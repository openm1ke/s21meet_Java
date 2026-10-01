package ru.izpz.edu.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import ru.izpz.dto.LastCommandState;
import ru.izpz.dto.ProfileStatus;
import ru.izpz.edu.utils.LastCommandAttributeConverter;

@Getter
@Setter
@Entity
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class Profile {
  @Id
  @GeneratedValue(generator = "UUID")
  @Column(updatable = false, nullable = false)
  UUID id;

  String telegramId;
  String s21login;

  @Column(name = "pending_s21login")
  String pendingS21login;

  @Column(name = "pending_registration_expires_at")
  OffsetDateTime pendingRegistrationExpiresAt;

  @Enumerated(EnumType.STRING)
  ProfileStatus status;

  @Column(name = "last_command", columnDefinition = "TEXT")
  @Convert(converter = LastCommandAttributeConverter.class)
  LastCommandState lastCommand;
}
