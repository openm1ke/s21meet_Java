package ru.izpz.edu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphQLStudentCredentialsDto(
        String studentId, String userId, String schoolId,
        Boolean isActive, Boolean isGraduate
) {}
