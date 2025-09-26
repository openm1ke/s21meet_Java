package ru.izpz.dto;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendDto {
    String telegramId;
    String login;
    String name;
    Boolean isFriend;
    Boolean isSubscribe;
    Boolean isFavorite;
    Boolean isOnline;
}
