package ru.izpz.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import ru.izpz.auth.utils.PasswordConverter;

@SuppressWarnings("JpaDataSourceORMInspection")
@Getter
@Setter
@Entity
@Table(name = "tokens")
public class TokenEntity {

  @Id private String login;

  @Convert(converter = PasswordConverter.class)
  private String password;

  @Column(columnDefinition = "TEXT")
  private String accessToken;

  @Column(columnDefinition = "TEXT")
  private String refreshToken;

  private LocalDateTime expiresAt;
}
