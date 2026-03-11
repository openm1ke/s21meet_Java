package ru.izpz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCodeRequest {
    @NotBlank(message = "s21login не должен быть пустым")
    private String s21login;
}
