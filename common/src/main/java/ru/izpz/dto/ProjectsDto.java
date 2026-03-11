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
        String courseType,
        String displayedCourseStatus,
        Integer amountAnswers,
        Integer amountMembers,
        Integer amountJoinedMembers,
        Integer amountReviewedAnswers,
        Integer amountCodeReviewMembers,
        Integer amountCurrentCodeReviewMembers,
        String groupName,
        Integer localCourseId
) {
}
