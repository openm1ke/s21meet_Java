package ru.izpz.bot.service;

import feign.FeignException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.izpz.bot.exception.EduLoginCheckException;
import ru.izpz.bot.exception.RocketChatSendException;
import ru.izpz.bot.keyboard.MenuCommandEnum;
import ru.izpz.bot.keyboard.TelegramButtons;
import ru.izpz.bot.keyboard.TelegramKeyboardFactory;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.dto.ParticipantDto;
import ru.izpz.dto.ParticipantStatusEnum;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileStatus;
import ru.izpz.dto.RocketChatSendResponse;
import ru.izpz.dto.ServiceErrorDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationFlow {

  private static final String STAGE_REGISTRATION_FLOW = "registration_flow";
  private static final String STAGE_REGISTRATION_VALIDATION = "registration_validation";
  private static final String REASON_FEIGN_EXCEPTION = "feign_exception";

  private final BotProperties botProperties;

  private final ProfileService profileService;
  private final TelegramButtons telegramButtons;
  private final MessageSender messageSender;
  private final TelegramKeyboardFactory telegramKeyboardFactory;
  private final MetricsService metricsService;
  private final TelegramWebAppMenuService telegramWebAppMenuService;

  public void startOnboarding(Long chatId) {
    InlineKeyboardMarkup keyboard =
        telegramKeyboardFactory.createInlineKeyboardMarkup(
            telegramButtons.getRegistrationButton(), 1);
    messageSender.sendMessage(chatId, "Для регистрации нажмите кнопку ниже", keyboard);
  }

  public void startValidation(Long chatId, ProfileDto profile, String text) {
    try {
      profileService.confirmRegistration(chatId, text);
      telegramWebAppMenuService.ensureMenuButton(chatId);
      messageSender.sendMessage(
          chatId,
          "Ваш аккаунт был успешно зарегистрирован",
          telegramKeyboardFactory.createReplyKeyboard(MenuCommandEnum.getAllMenuCommands(), 3));
    } catch (FeignException e) {
      metricsService.recordProcessingError(STAGE_REGISTRATION_VALIDATION, REASON_FEIGN_EXCEPTION);
      messageSender.sendMessage(chatId, "Ошибка валидации, попробуйте позже", null);
      messageSender.sendMessage(botProperties.admin(), e.contentUTF8(), null);
    } catch (IllegalStateException e) {
      metricsService.recordProcessingError(STAGE_REGISTRATION_VALIDATION, "invalid_code");
      messageSender.sendMessage(
          chatId, e.getMessage(), telegramKeyboardFactory.removeReplyKeyboard());
    } catch (Exception e) {
      metricsService.recordProcessingError(STAGE_REGISTRATION_VALIDATION, "unexpected_exception");
      log.error("Unexpected validation error for chatId={}", chatId, e);
      messageSender.sendMessage(chatId, "Ошибка валидации, попробуйте позже", null);
    }
  }

  public void startRegistration(Long chatId, String text) {
    if (!isValidLogin(text)) {
      messageSender.sendMessage(chatId, "Введенный логин не соответствует требованиям", null);
      return;
    }

    String login = text.toLowerCase(Locale.ROOT);

    ParticipantDto participant;
    try {
      participant = profileService.checkEduLogin(login);
    } catch (EduLoginCheckException e) {
      metricsService.recordProcessingError(STAGE_REGISTRATION_FLOW, "edu_login_check_exception");
      ServiceErrorDto error = e.getError();
      messageSender.sendMessage(chatId, "Ошибка проверки логина: " + error.getMessage(), null);
      messageSender.sendMessage(botProperties.admin(), "Ошибка проверки логина: " + error, null);
      return;
    }

    // Для бота доступны активные и замороженные профили.
    if (!isRegistrationAllowed(participant.getStatus())) {
      messageSender.sendMessage(chatId, "Введенный логин недоступен для регистрации", null);
      return;
    }

    // тут можно проверить что профиль на основе Core program
    if (participant.getParallelName() == null
        || !participant.getParallelName().equals("Core program")) {
      messageSender.sendMessage(
          chatId, "Введенный логин не на основе! Приходите когда пройдете бассейн", null);
      return;
    }

    try {
      profileService.checkAndSetLogin(chatId, login);
    } catch (FeignException e) {
      metricsService.recordProcessingError(STAGE_REGISTRATION_FLOW, REASON_FEIGN_EXCEPTION);
      messageSender.sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже", null);
      messageSender.sendMessage(botProperties.admin(), e.contentUTF8(), null);
      return;
    } catch (IllegalStateException e) {
      metricsService.recordProcessingError(STAGE_REGISTRATION_FLOW, "login_already_in_use");
      messageSender.sendMessage(chatId, e.getMessage(), null);
      return;
    }
    // если логин совпадает значит мы его сохранили
    RocketChatSendResponse rocketChatResponse;
    try {
      rocketChatResponse = profileService.sendVerificationCode(login);
    } catch (RocketChatSendException e) {
      metricsService.recordProcessingError(STAGE_REGISTRATION_FLOW, "rocketchat_send_exception");
      messageSender.sendMessage(
          chatId, "Ошибка отправки сообщения в рокетчат, попробуйте позже", null);
      messageSender.sendMessage(botProperties.admin(), e.getMessage(), null);
      return;
    } catch (FeignException e) {
      metricsService.recordProcessingError(STAGE_REGISTRATION_FLOW, REASON_FEIGN_EXCEPTION);
      messageSender.sendMessage(
          chatId, "Ошибка отправки сообщения в рокетчат, сообщите админу", null);
      messageSender.sendMessage(botProperties.admin(), e.contentUTF8(), null);
      return;
    }

    messageSender.sendMessage(chatId, "В рокет чат был отправлен код для подтверждения", null);
    messageSender.sendHtmlMessageWithoutWebPreview(
        botProperties.admin(),
        formatAdminVerificationMessage(login, rocketChatResponse.getMessage()),
        null);
    profileService.updateProfileStatus(chatId, ProfileStatus.VALIDATION);

    // тут мы получает в тексте логин на платформе
    // надо отправить его на бэкенд и получить дто с какими-то полями
    // первое что мы проверяем что профиль существует или нет
    //   - если нет то пишем что вы ошиблись попробуйте еще раз
    // второе если существует
    //   - если он уже зарегистрирован то говорим что этот профиль уже привязан к другому телеграму
    //   - если он не зарегистрирован то смотрим на его статус
    //          - статус заблокирован - не можем зарегать такой профиль (можно поменять статус на
    // блокированный)
    //          - статус заморожен - разрешаем регистрацию в боте
    //          - статус не на основе пишем сообщение что не можем зарегать пока не будет на основе
    // (тут другой статус)
    // если все хорошо и профиль активный или замороженный и на основе то привязываем его к этому
    // телеграм айди и
    // пытаемся начать валидацию

  }

  private String formatAdminVerificationMessage(String login, String message) {
    String escapedLogin = HtmlUtils.htmlEscape(login);
    String escapedMessage = HtmlUtils.htmlEscape(String.valueOf(message));
    String botUsername = botProperties.username();
    if (botUsername == null || botUsername.isBlank()) {
      return "В Rocket.Chat отправлено сообщение для логина "
          + escapedLogin
          + ": "
          + escapedMessage;
    }

    String normalizedUsername = botUsername.trim();
    if (normalizedUsername.startsWith("@")) {
      normalizedUsername = normalizedUsername.substring(1);
    }
    String deepLink = "https://t.me/" + normalizedUsername + "?start=login_" + login;
    return "🔎 <a href=\""
        + deepLink
        + "\">Открыть профиль "
        + escapedLogin
        + "</a>\nВ Rocket.Chat отправлено сообщение: "
        + escapedMessage;
  }

  private boolean isValidLogin(String login) {
    return login != null && login.matches("^[a-zA-Z]{3,30}$");
  }

  private boolean isRegistrationAllowed(ParticipantStatusEnum status) {
    return status == ParticipantStatusEnum.ACTIVE || status == ParticipantStatusEnum.FROZEN;
  }
}
