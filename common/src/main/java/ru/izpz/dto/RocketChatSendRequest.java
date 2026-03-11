package ru.izpz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RocketChatSendRequest {
    @NotBlank(message = "Username must not be blank")
    private String username;
    @NotBlank(message = "Message must not be blank")
    private String message;
}
