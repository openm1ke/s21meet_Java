package ru.izpz.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenRequest {
    @NotBlank(message = "Логин не должен быть пустым")
    private String login;
    @NotBlank(message = "Пароль не должен быть пустым")
    private String password;
}
