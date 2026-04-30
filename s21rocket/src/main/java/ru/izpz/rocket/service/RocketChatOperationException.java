package ru.izpz.rocket.service;

import java.io.Serial;

public class RocketChatOperationException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  public RocketChatOperationException(String message) {
    super(message);
  }
}
