package ru.izpz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectExecutorsRequest(
        @NotBlank
        @Size(max = 120)
        String projectName
) {
}
