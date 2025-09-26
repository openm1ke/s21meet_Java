package ru.izpz.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.izpz.auth.utils.PasswordConverter;

import java.time.LocalDateTime;

@SuppressWarnings("JpaDataSourceORMInspection")
@Getter
@Setter
@Entity
@Table(name = "tokens")
public class TokenEntity {

    @Id
    private String login;

    @Convert(converter = PasswordConverter.class)
    private String password;

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    private LocalDateTime expiresAt;
}
