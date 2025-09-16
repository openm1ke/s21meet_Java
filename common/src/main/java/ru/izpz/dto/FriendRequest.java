package ru.izpz.dto;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequest {
    private String telegramId;
    private String login;
}
