package ru.izpz.dto;

import java.util.List;

public record ProjectExecutorsPageDto(
    List<ProjectExecutorDto> items,
    int page,
    int size,
    long totalItems,
    int totalPages,
    List<String> availableCampuses,
    List<String> availableStatuses) {}
