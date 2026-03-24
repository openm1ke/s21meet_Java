package ru.izpz.edu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StudentProjectData(
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
) {}
