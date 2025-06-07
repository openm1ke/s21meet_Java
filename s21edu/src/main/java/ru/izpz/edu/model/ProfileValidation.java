package ru.izpz.edu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
public class ProfileValidation {
    @Id
    String telegramId;
    String secretCode;
    OffsetDateTime expiresAt;
}
