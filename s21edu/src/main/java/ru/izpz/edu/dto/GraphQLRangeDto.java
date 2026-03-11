package ru.izpz.edu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphQLRangeDto(
        Integer levelCode
) {}
