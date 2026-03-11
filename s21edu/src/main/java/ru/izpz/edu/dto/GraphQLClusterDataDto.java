package ru.izpz.edu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphQLClusterDataDto(
        List<GraphQLPlaceDto> occupiedPlaces
) {}
