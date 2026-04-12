package ru.izpz.bot.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.bot.exception.EduLoginCheckException;
import ru.izpz.bot.exception.InvalidCallbackPayloadException;
import ru.izpz.bot.keyboard.CallbackPayloadSerializer;
import ru.izpz.bot.keyboard.TelegramButtons;
import ru.izpz.bot.keyboard.TelegramKeyboardFactory;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.dto.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackHandler {

    private final BotProperties botProperties;

    private final ProfileService profileService;
    private final TelegramKeyboardFactory telegramKeyboardFactory;
    private final CallbackPayloadSerializer callbackPayloadSerializer;
    private final MessageSender messageSender;
    private final MetricsService metricsService;
    @Value("${bot.page-size:10}")
    private int pageSize = 10;

    private static final int ROW_SIZE = 5;
    private static final String LOGIN = "login";
    private static final String REASON_FEIGN_EXCEPTION = "feign_exception";
    private static final String STAGE_SHOW_EVENTS = "show_events";
    private static final String STAGE_SHOW_FRIENDS = "show_friends";
    private static final String STAGE_SHOW_PROFILE = "show_profile";
    private static final String STAGE_SET_LAST_COMMAND = "set_last_command";
    private static final String STAGE_REGISTRATION_UPDATE = "registration_update";

    public void handleCallbackMessage(Long chatId, String data, Integer messageId, String callbackId) {
        try {
            CallbackPayload payload = callbackPayloadSerializer.deserialize(data);
            
            // Записываем метрику для inline кнопок
            metricsService.recordButtonPress(payload.getCommand(), ButtonMetricType.INLINE);

            switch (payload.getCommand()) {
                case TelegramButtons.REGISTRATION_CODE -> updateMessageAndChangeStatusRegistration(chatId, messageId, "Введите логин на платформе");
                case "show_friend" -> {
                    var login = payload.getArgs().get(LOGIN);
                    showProfile(chatId, login);
                }
                case "friends_page" -> {
                    int page = Integer.parseInt(payload.getArgs().get("page"));
                    showFriends(chatId, page, messageId);
                }
                case "add_friend" -> applyAndRefreshKeyboard(chatId, messageId, callbackId, payload.getArgs().get(LOGIN),
                        FriendRequest.Action.TOGGLE_FRIEND, "Статус «друг» переключён");
                case "favorite" -> applyAndRefreshKeyboard(chatId, messageId, callbackId, payload.getArgs().get(LOGIN),
                        FriendRequest.Action.TOGGLE_FAVORITE, "Избранное переключено");
                case "subscribe" -> applyAndRefreshKeyboard(chatId, messageId, callbackId, payload.getArgs().get(LOGIN),
                        FriendRequest.Action.TOGGLE_SUBSCRIBE, "Статус подписки переключён");
                case "set_name" -> {
                    setLastCommand(chatId, LastCommandType.SET_NAME, Map.of(LOGIN, payload.getArgs().get(LOGIN)));
                    messageSender.sendMessage(chatId, "Указать имя", null);
                }
                case "event" -> {
                    var event = Long.parseLong(payload.getArgs().get("id"));
                    var eventDto = profileService.getEvent(event);
                    messageSender.sendMessageWithoutWebPreview(chatId, EventMessageFormatter.format(eventDto), null);
                }
                case "events_page" -> {
                    var page = Integer.parseInt(payload.getArgs().get("page"));
                    showEvents(chatId, page, messageId);
                }
                default -> messageSender.sendMessage(chatId, "Неизвестная команда: " + data, null);
            }
        } catch (InvalidCallbackPayloadException e) {
            metricsService.recordProcessingError("callback_handler", "invalid_payload");
            log.error("Получены некорректные данные в callback: {}", data, e);
            messageSender.sendMessage(chatId, "Некорректный формат данных. Попробуйте еще раз.", null);
        } catch (Exception e) {
            metricsService.recordProcessingError("callback_handler", "unexpected_exception");
            log.error("Unexpected callback handling error: data={}", data, e);
            messageSender.sendMessage(chatId, "Ошибка обработки команды, попробуйте позже", null);
        }
    }

    private void applyAndRefreshKeyboard(
            long chatId, int messageId, String callbackId,
            String login, FriendRequest.Action action, String toastText) {

        FriendDto friend = profileService.applyFriend(chatId, login, action, null);
        var kb = telegramKeyboardFactory.getFriendInlineKeyboard(login, friend);

        messageSender.execute(telegramKeyboardFactory.editFriendInlineKeyboard(kb, chatId, messageId));
        messageSender.execute(telegramKeyboardFactory.createAnswerCallbackQuery(callbackId, toastText, false));
    }

    public void showEvents(Long chatId, int page, Integer messageId) {
        try {
            var events = profileService.getEvents(chatId, page, pageSize);
            var keyboard = telegramKeyboardFactory.eventsListKeyboard(events, ROW_SIZE, page);
            var eventsListText = telegramKeyboardFactory.eventsListText(events);
            if (messageId != null) {
                messageSender.updateMessage(chatId, messageId, eventsListText, keyboard);
            } else {
                messageSender.sendMessage(chatId, "События\n\n" + eventsListText, keyboard);
            }
        } catch (FeignException e) {
            metricsService.recordProcessingError(STAGE_SHOW_EVENTS, REASON_FEIGN_EXCEPTION);
            messageSender.sendMessage(chatId, "Ошибка обработки событий, попробуйте позже", null);
            messageSender.sendMessage(botProperties.admin(), e.contentUTF8(), null);
        }
    }

    public void showFriends(Long chatId, int page, Integer messageId) {
        try {
            var list = profileService.getFriends(chatId, page, pageSize);
            var keyboard = telegramKeyboardFactory.friendsListKeyboard(list, ROW_SIZE, page);
            String friendsListText = telegramKeyboardFactory.friendsListText(list);
            if (messageId != null) {
                messageSender.updateMessage(chatId, messageId, friendsListText, keyboard);
            } else {
                messageSender.sendMessage(chatId, friendsListText, keyboard);
            }
        } catch (FeignException e) {
            metricsService.recordProcessingError(STAGE_SHOW_FRIENDS, REASON_FEIGN_EXCEPTION);
            messageSender.sendMessage(chatId, "Ошибка обработки друзей, попробуйте позже", null);
            messageSender.sendMessage(botProperties.admin(), e.contentUTF8(), null);
        }
    }

    public void showProfile(Long chatId, String login) {
        if (!isValidLogin(login)) {
            messageSender.sendMessage(chatId, "Логин должен быть от 3 до 30 символов и состоять только из латинских букв", null);
            return;
        }
        try {
            profileService.checkEduLogin(login);
            FriendDto friend = profileService.applyFriend(chatId, login, FriendRequest.Action.NONE, null);
            InlineKeyboardMarkup keyboard = telegramKeyboardFactory.getFriendInlineKeyboard(login, friend);
            ParticipantDto showProfile = profileService.showParticipant(chatId.toString(), login);
            List<ProjectsDto> projects = loadProjects(login);
            messageSender.sendMessage(chatId, ParticipantMessageFormatter.format(showProfile, projects), keyboard);
        } catch (FeignException e) {
            metricsService.recordProcessingError(STAGE_SHOW_PROFILE, REASON_FEIGN_EXCEPTION);
            messageSender.sendMessage(chatId, "Ошибка поиска профиля, попробуйте позже", null);
            messageSender.sendMessage(botProperties.admin(), e.contentUTF8(), null);
        } catch (EduLoginCheckException e) {
            metricsService.recordProcessingError(STAGE_SHOW_PROFILE, "edu_login_check_exception");
            messageSender.sendMessage(chatId, "Ошибка проверки логина: " + e.getError().getMessage(), null);
            messageSender.sendMessage(botProperties.admin(), "Ошибка проверки логина: " + e.getError(), null);
        }
    }

    public void setLastCommand(Long chatId, LastCommandType type, Map<String, Object> args) {
        var lastCommand = new LastCommandState(type, args);
        try {
            profileService.setLastCommand(chatId, lastCommand);
        } catch (FeignException e) {
            metricsService.recordProcessingError(STAGE_SET_LAST_COMMAND, REASON_FEIGN_EXCEPTION);
            messageSender.sendMessage(chatId, "Ошибка установки lastCommand", null);
            messageSender.sendMessage(botProperties.admin(), e.contentUTF8(), null);
        }
    }

    public void updateMessageAndChangeStatusRegistration(Long chatId, Integer messageId, String newText) {
        try {
            messageSender.updateMessage(chatId, messageId, newText, null);
            profileService.updateProfileStatus(chatId, ProfileStatus.REGISTRATION);
        } catch (FeignException e) {
            metricsService.recordProcessingError(STAGE_REGISTRATION_UPDATE, REASON_FEIGN_EXCEPTION);
            log.error("Ошибка обработки профиля", e);
        }
    }

    private boolean isValidLogin(String login) {
        return login != null && login.matches("^[a-zA-Z]{3,30}$");
    }

    private List<ProjectsDto> loadProjects(String login) {
        try {
            return profileService.getProjects(login);
        } catch (FeignException e) {
            log.warn("Не удалось получить проекты для {}: {}", login, e.getMessage());
            return List.of();
        }
    }
}
