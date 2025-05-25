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
    private String login; // Используем логин как первичный ключ

    // Для простоты здесь пароль сохраняется в открытом виде,
    // но в реальном приложении это не рекомендуется.
    private String password;

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    // Время истечения срока действия access token
    private LocalDateTime expiresAt;
}
