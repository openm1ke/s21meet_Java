package ru.izpz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequest {
    @NotBlank
    private String telegramId;
    @NotBlank
    private String login;
    @NotNull
    Action action;
    String name;

    public enum Action { TOGGLE_FRIEND, TOGGLE_FAVORITE, TOGGLE_SUBSCRIBE, SET_NAME, NONE }
}
