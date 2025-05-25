package ru.izpz.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@SuppressWarnings("JpaDataSourceORMInspection")
@Getter
@Setter
@Entity
@Table(name = "tokens")
public class TokenEntity {

    @Id
    private String login;

    private String password;

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    private LocalDateTime expiresAt;
}
