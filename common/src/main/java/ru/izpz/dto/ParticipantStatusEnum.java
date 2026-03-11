package ru.izpz.dto;

import lombok.Getter;

@Getter
public enum ParticipantStatusEnum {
    ACTIVE("✅"),
    TEMPORARY_BLOCKING("\uD83D\uDC4A"),
    EXPELLED("⛔"),
    BLOCKED("🚫"),
    FROZEN("\uD83E\uDD76"),
    STUDY_COMPLETED("\uD83C\uDFC6");

    private final String emoji;

    ParticipantStatusEnum(String emoji) {
        this.emoji = emoji;
    }

}
