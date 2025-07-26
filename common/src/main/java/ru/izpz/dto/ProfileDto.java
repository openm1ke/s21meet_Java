package ru.izpz.dto;

public record ProfileDto(
    String telegramId,
    String s21login,
    ProfileStatus status,
    String lastCommand
) {}
