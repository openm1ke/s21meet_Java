package ru.izpz.edu.dto;

import ru.izpz.edu.model.Profile;
import ru.izpz.edu.model.ProfileStatus;

public record ProfileDto(
    String telegramId,
    String s21login,
    ProfileStatus status,
    String lastCommand
) {
    public static ProfileDto fromEntity(Profile entity) {
        return new ProfileDto(
                entity.getTelegramId(),
                entity.getS21login(),
                entity.getStatus(),
                entity.getLastCommand()
        );
    }
}
