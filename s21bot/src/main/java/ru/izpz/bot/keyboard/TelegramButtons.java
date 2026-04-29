package ru.izpz.bot.keyboard;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.bot.property.BotProperties;

@Service
@RequiredArgsConstructor
public class TelegramButtons {

  private final BotProperties botProperties;
  private final CallbackPayloadSerializer serializer;

  public static final String REGISTRATION_NAME = "Регистрация";
  public static final String REGISTRATION_CODE = "registration";
  public static final String SUBSCRIBE_NAME = "Подписаться на канал";

  public Map<String, String> getRegistrationButton() {
    return Map.of(
        REGISTRATION_NAME, serializer.serialize(new CallbackPayload(REGISTRATION_CODE, null)));
  }

  public Map<String, String> getSubscribeButton() {
    return Map.of(SUBSCRIBE_NAME, botProperties.groupInviteLink());
  }
}
