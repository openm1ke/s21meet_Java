package ru.izpz.dto;

public record ProjectExecutorDto(
        String login,
        String campusName,
        String projectStatus,
        String campusPlace
) {
}
