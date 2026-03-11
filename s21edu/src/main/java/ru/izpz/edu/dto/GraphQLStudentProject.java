package ru.izpz.edu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphQLStudentProject(
        String goalId, String name, String description,
        Integer experience, String dateTime, Integer finalPercentage,
        Integer laboriousness, String executionType, String goalStatus,
        String courseType, String displayedCourseStatus,
        Integer amountAnswers, Integer amountMembers, Integer amountJoinedMembers,
        Integer amountReviewedAnswers, Integer amountCodeReviewMembers,
        Integer amountCurrentCodeReviewMembers, String groupName,
        Integer localCourseId
) {}
