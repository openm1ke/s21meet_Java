package ru.izpz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RocketChatSendResponse {
    private boolean success;
    private String message;
}