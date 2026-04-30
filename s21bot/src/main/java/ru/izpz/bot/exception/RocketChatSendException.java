package ru.izpz.bot.exception;

import java.io.Serial;
import lombok.Getter;
import ru.izpz.dto.RocketChatSendResponse;

@Getter
public class RocketChatSendException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;
  private final transient RocketChatSendResponse response;

  public RocketChatSendException(RocketChatSendResponse response) {
    super(response.getMessage());
    this.response = response;
  }
}
