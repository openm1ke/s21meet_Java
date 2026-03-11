package ru.izpz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantRequest {
    @NotBlank(message = "TelegramId не должен быть пустым")
    @Pattern(
        regexp = "^\\d{5,13}$",
        message = "Telegram ID должен содержать только цифры и быть длиной от 5 до 13 символов"
    )
    private String telegramId;
    @NotBlank(message = "eduLogin не должен быть пустым")
    private String eduLogin;
}
