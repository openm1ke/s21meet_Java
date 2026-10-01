package ru.izpz.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileRegistrationConfirmationRequest(
    @NotBlank String telegramId, @NotBlank String secretCode) {}
