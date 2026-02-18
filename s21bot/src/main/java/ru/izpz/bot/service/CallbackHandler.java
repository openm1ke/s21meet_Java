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
import ru.izpz.dto.*;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackHandler {

    @Value("${bot.admin}")
    private Long ADMIN_ID;

    private final ProfileService profileService;
    private final TelegramKeyboardFactory telegramKeyboardFactory;
    private final CallbackPayloadSerializer callbackPayloadSerializer;
    private final MessageSender messageSender;

    private static final int ROW_SIZE = 3;
    private static final int PAGE_SIZE = 2;

    public void handleCallbackMessage(Long chatId, String data, Integer messageId, String callbackId) {
        try {
            CallbackPayload payload = callbackPayloadSerializer.deserialize(data);

            switch (payload.getCommand()) {
                case TelegramButtons.REGISTRATION_CODE -> updateMessageAndChangeStatusRegistration(chatId, messageId, "Введите логин на платформе");
                case "show_friend" -> {
                    var login = payload.getArgs().get("login");
                    showProfile(chatId, login);
                }
                case "friends_page" -> {
                    int page = Integer.parseInt(payload.getArgs().get("page"));
                    showFriends(chatId, page, messageId);
                }
                case "add_friend" -> applyAndRefreshKeyboard(chatId, messageId, callbackId, payload.getArgs().get("login"),
                        FriendRequest.Action.TOGGLE_FRIEND, "Статус «друг» переключён");
                case "favorite" -> applyAndRefreshKeyboard(chatId, messageId, callbackId, payload.getArgs().get("login"),
                        FriendRequest.Action.TOGGLE_FAVORITE, "Избранное переключено");
                case "subscribe" -> applyAndRefreshKeyboard(chatId, messageId, callbackId, payload.getArgs().get("login"),
                        FriendRequest.Action.TOGGLE_SUBSCRIBE, "Подписка переключена");
                case "set_name" -> {
                    setLastCommand(chatId, LastCommandType.SET_NAME, Map.of("login", payload.getArgs().get("login")));
                    messageSender.sendMessage(chatId, "Указать имя", null);
                }
                case "event" -> {
                    var event = Long.parseLong(payload.getArgs().get("id"));
                    var eventDto = profileService.getEvent(event);
                    messageSender.sendMessage(chatId, eventDto.toString(), null);
                }
                case "events_page" -> {
                    var page = Integer.parseInt(payload.getArgs().get("page"));
                    showEvents(chatId, page, messageId);
                }
                default -> messageSender.sendMessage(chatId, "Неизвестная команда: " + data, null);
            }
        } catch (InvalidCallbackPayloadException e) {
            log.error("Получены некорректные данные в callback: {}", data, e);
            messageSender.sendMessage(chatId, "Некорректный формат данных. Попробуйте еще раз.", null);
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
            var events = profileService.getEvents(chatId, page, PAGE_SIZE);
            var keyboard = telegramKeyboardFactory.eventsListKeyboard(events, ROW_SIZE, page);
            var eventsListText = telegramKeyboardFactory.eventsListText(events);
            if (messageId != null) {
                messageSender.updateMessage(chatId, messageId, eventsListText, keyboard);
            } else {
                messageSender.sendMessage(chatId, "События\n\n" + eventsListText, keyboard);
            }
        } catch (FeignException e) {
            messageSender.sendMessage(chatId, "Ошибка обработки событий, попробуйте позже", null);
            messageSender.sendMessage(ADMIN_ID, e.contentUTF8(), null);
        }
    }

    public void showFriends(Long chatId, int page, Integer messageId) {
        try {
            var list = profileService.getFriends(chatId, page, PAGE_SIZE);
            var keyboard = telegramKeyboardFactory.friendsListKeyboard(list, ROW_SIZE, page);
            String friendsListText = telegramKeyboardFactory.friendsListText(list);
            if (messageId != null) {
                messageSender.updateMessage(chatId, messageId, friendsListText, keyboard);
            } else {
                messageSender.sendMessage(chatId, friendsListText, keyboard);
            }
        } catch (FeignException e) {
            messageSender.sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже", null);
            messageSender.sendMessage(ADMIN_ID, e.contentUTF8(), null);
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
            messageSender.sendMessage(chatId, "Профиль\n" + showProfile, keyboard);
        } catch (EduLoginCheckException e) {
            messageSender.sendMessage(chatId, "Ошибка проверки логина: " + e.getError().getMessage(), null);
            messageSender.sendMessage(ADMIN_ID, "Ошибка проверки логина: " + e.getError(), null);
        }
    }

    public void setLastCommand(Long chatId, LastCommandType type, Map<String, Object> args) {
        var lastCommand = new LastCommandState(type, args);
        try {
            profileService.setLastCommand(chatId, lastCommand);
        } catch (FeignException e) {
            messageSender.sendMessage(chatId, "Ошибка установки lastCommand", null);
            messageSender.sendMessage(ADMIN_ID, e.contentUTF8(), null);
        }
    }

    public void updateMessageAndChangeStatusRegistration(Long chatId, Integer messageId, String newText) {
        try {
            messageSender.updateMessage(chatId, messageId, newText, null);
            profileService.updateProfileStatus(chatId, ProfileStatus.REGISTRATION);
        } catch (FeignException e) {
            log.error("Ошибка обработки профиля", e);
        }
    }

    private boolean isValidLogin(String login) {
        return login != null && login.matches("^[a-zA-Z]{3,30}$");
    }
}
