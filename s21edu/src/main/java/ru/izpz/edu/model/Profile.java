package ru.izpz.edu.model;

import lombok.*;
import ru.izpz.dto.ProfileStatus;
import jakarta.persistence.*;

import java.util.UUID;

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
    @Enumerated(EnumType.STRING)
    ProfileStatus status;
    @Column(name = "last_command", columnDefinition = "TEXT")
    String lastCommand;
}
