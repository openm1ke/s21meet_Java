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
public class ProfileRequest {
    @NotBlank(message = "TelegramId не должен быть пустым")
    @Pattern(
        regexp = "^\\d{5,13}$",
        message = "Telegram ID должен содержать только цифры и быть длиной от 7 до 13 символов"
    )
    private String telegramId;
    private String s21login;
    private ProfileStatus status;
    private String lastCommand;
}
