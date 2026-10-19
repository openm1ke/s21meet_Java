package ru.izpz.bot.service;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.izpz.bot.keyboard.SlashCommandEnum;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.dto.ProfileDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {
  private static final String MESSAGE_PROCESSOR = "message_processor";

  private final BotProperties botProperties;

  private final ProfileService profileService;
  private final MessageSender messageSender;
  private final CallbackHandler callbackHandler;
  private final RegistrationFlow registrationFlow;
  private final ConfirmedFlow confirmedFlow;
  private final MetricsService metricsService;

  public void handleTextMessage(Message message) {
    Long chatId = message.getChatId();
    String text = message.getText().trim();
    deleteLoginDeepLinkMessage(message, text);
    try {
      ProfileDto profile = profileService.getProfile(chatId);
      log.info("Profile: {}", profile.toString());
      parseMessage(chatId, profile, text);
    } catch (CallNotPermittedException e) {
      metricsService.recordProcessingError(MESSAGE_PROCESSOR, "circuit_breaker_open");
      messageSender.sendMessage(
          chatId, "Сервис профилей временно недоступен, попробуйте позже", null);
    } catch (FeignException e) {
      metricsService.recordProcessingError(MESSAGE_PROCESSOR, "feign_exception");
      messageSender.sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже", null);
      messageSender.sendMessage(botProperties.admin(), e.contentUTF8(), null);
    } catch (Exception e) {
      metricsService.recordProcessingError(MESSAGE_PROCESSOR, "unexpected_exception");
      log.error("Unexpected error while handling text message", e);
      messageSender.sendMessage(chatId, "Произошла внутренняя ошибка, попробуйте позже", null);
    }
  }

  private void deleteLoginDeepLinkMessage(Message message, String text) {
    if (!isLoginDeepLink(text)) {
      return;
    }

    try {
      boolean deleted = messageSender.deleteMessage(message.getChatId(), message.getMessageId());
      if (!deleted) {
        log.debug(
            "Не удалось удалить deep link сообщение: chatId={}, messageId={}",
            message.getChatId(),
            message.getMessageId());
      }
    } catch (RuntimeException e) {
      log.warn(
          "Ошибка удаления deep link сообщения: chatId={}, messageId={}",
          message.getChatId(),
          message.getMessageId(),
          e);
    }
  }

  private boolean isLoginDeepLink(String text) {
    return SlashCommandEnum.parse(text)
        .filter(parsed -> parsed.command() == SlashCommandEnum.START)
        .map(SlashCommandEnum.ParsedCommand::args)
        .filter(args -> args.size() == 1)
        .map(args -> args.get(0))
        .filter(argument -> argument.startsWith("login_") && argument.length() > "login_".length())
        .isPresent();
  }

  public void parseMessage(Long chatId, ProfileDto profile, String text) {
    switch (profile.status()) {
      case CREATED -> registrationFlow.startOnboarding(chatId);
      case REGISTRATION -> registrationFlow.startRegistration(chatId, text);
      case VALIDATION -> registrationFlow.startValidation(chatId, profile, text);
      case CONFIRMED -> confirmedFlow.startConfirmed(chatId, profile, text);
      default -> log.debug("Unhandled profile status: {}", profile.status());
    }
  }

  public void handleCallbackMessage(
      Long chatId, String data, Integer messageId, String callbackId) {
    try {
      callbackHandler.handleCallbackMessage(chatId, data, messageId, callbackId);
    } catch (Exception e) {
      metricsService.recordProcessingError("callback_dispatch", "unexpected_exception");
      log.error("Unexpected error while handling callback message", e);
      messageSender.sendMessage(chatId, "Произошла внутренняя ошибка, попробуйте позже", null);
    }
  }
}
