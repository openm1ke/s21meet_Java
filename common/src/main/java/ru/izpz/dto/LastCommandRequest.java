package ru.izpz.dto;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastCommandRequest {
    private String telegramId;
    private String command;
}
