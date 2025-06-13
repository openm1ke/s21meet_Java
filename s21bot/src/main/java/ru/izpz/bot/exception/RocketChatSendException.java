package ru.izpz.bot.exception;

import lombok.Getter;
import ru.izpz.dto.RocketChatSendResponse;

@Getter
public class RocketChatSendException extends RuntimeException {
    private final RocketChatSendResponse response;

    public RocketChatSendException(RocketChatSendResponse response) {
        super(response.getMessage());
        this.response = response;
    }
}
