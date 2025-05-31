package ru.izpz.edu.model;

import ru.izpz.dto.ProfileStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
public class Profile {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(updatable = false, nullable = false)
    UUID id;
    String telegramId;
    String s21login;
    @Enumerated(EnumType.STRING)
    ProfileStatus status;
    String lastCommand;
}
