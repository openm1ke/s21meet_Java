package ru.izpz.dto;

import lombok.Getter;

@Getter
public enum ParticipantStatusEnum {
    ACTIVE("⚡"),
    TEMPORARY_BLOCKING("⚠\uFE0F"),
    EXPELLED("❌"),
    BLOCKED("🚫"),
    FROZEN("❄\uFE0F"),
    STUDY_COMPLETED("🎓");

    private final String emoji;

    ParticipantStatusEnum(String emoji) {
        this.emoji = emoji;
    }

}
