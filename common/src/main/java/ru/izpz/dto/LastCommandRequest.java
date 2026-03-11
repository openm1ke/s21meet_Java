package ru.izpz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastCommandRequest {
    @NotBlank(message = "TelegramId не должен быть пустым")
    @Pattern(
        regexp = "^\\d{5,13}$",
        message = "Telegram ID должен содержать только цифры и быть длиной от 5 до 13 символов"
    )
    private String telegramId;
    @NotNull(message = "command не должен быть null")
    private LastCommandState command;
}
