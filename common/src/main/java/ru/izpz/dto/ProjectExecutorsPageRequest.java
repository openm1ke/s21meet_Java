package ru.izpz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProjectExecutorsPageRequest(
    @NotBlank @Size(max = 120) String projectName,
    @Min(0) int page,
    @Min(1) @Max(50) int size,
    @Valid List<@NotBlank @Size(max = 80) String> campuses,
    @Valid List<@NotBlank @Size(max = 80) String> statuses,
    @NotBlank String sortDirection) {}
