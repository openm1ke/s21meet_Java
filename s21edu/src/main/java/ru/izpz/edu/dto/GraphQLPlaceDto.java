package ru.izpz.edu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphQLPlaceDto(
        String row, Integer number,
        String stageGroupName, String stageName, String studentType,
        GraphQLUserDto user, GraphQLExpDto experience
) {}
