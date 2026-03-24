package ru.izpz.dto;

public record ProjectsDto(
        String goalId,
        String name,
        String description,
        Integer experience,
        String dateTime,
        Integer finalPercentage,
        Integer laboriousness,
        String executionType,
        String goalStatus,
        Integer amountMembers,
        Integer localCourseId
) {
}
