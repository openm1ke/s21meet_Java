package ru.izpz.bot.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.bot.exception.EduLoginCheckException;
import ru.izpz.bot.exception.InvalidCallbackPayloadException;
import ru.izpz.bot.exception.RocketChatSendException;
import ru.izpz.bot.keyboard.*;
import ru.izpz.dto.*;
import ru.izpz.dto.model.ErrorResponseDTO;
import ru.izpz.dto.model.ParticipantV1DTO;

import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {

    @Value("${bot.admin}")
    private Long ADMIN_ID;
    @Value("${bot.group}")
    private Long GROUP_ID;

    private final OkHttpTelegramClient telegramClient;
    private final ProfileService profileService;
    private final TelegramButtons telegramButtons;

    private static final int PAGE_SIZE = 10;

    public void handleTextMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText().trim();
        try {
            ProfileDto profile = profileService.getProfile(chatId);
            log.info("Profile: {}", profile.toString());
            //sendMessage(chatId, profile.toString());
            parseMessage(chatId, profile, text);
        } catch (FeignException e) {
            sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже", null);
            sendMessage(ADMIN_ID, e.contentUTF8(), null);
        }
    }

    public void parseMessage(Long chatId, ProfileDto profile, String text) {
        switch(profile.status()) {
            case CREATED -> startOnboarding(chatId);
            case REGISTRATION -> startRegistration(chatId, profile, text);
            case VALIDATION -> startValidation(chatId, profile, text);
            case CONFIRMED -> startConfirmed(chatId, profile, text);
        }
    }

    public void handleCallbackMessage(Long chatId, String data, Integer messageId, String callbackId) {
        try {
            CallbackPayload payload = CallbackPayloadSerializer.deserialize(data);

            switch (payload.getCommand()) {
                case TelegramButtons.REGISTRATION_CODE -> updateMessageAndChangeStatusRegistration(chatId, messageId, "Введите логин на платформе");
                case "add_friend" -> {
                    applyAndRefreshKeyboard(chatId, messageId, callbackId, payload.getArgs().get("login"),
                        FriendRequest.Action.TOGGLE_FRIEND, "Статус «друг» переключён");
                }
                case "favorite" -> {
                    applyAndRefreshKeyboard(chatId, messageId, callbackId, payload.getArgs().get("login"),
                        FriendRequest.Action.TOGGLE_FAVORITE, "Избранное переключено");
                }
                case "subscribe" -> {
                    applyAndRefreshKeyboard(chatId, messageId, callbackId, payload.getArgs().get("login"),
                        FriendRequest.Action.TOGGLE_SUBSCRIBE, "Подписка переключена");
                }
                case "set_name" -> {
                    var lastCommand = new LastCommandState();
                    lastCommand.setCommand(LastCommandType.SET_NAME);
                    lastCommand.setArgs(Map.of("login", payload.getArgs().get("login")));
                    setLastCommand(chatId, lastCommand);
                    sendMessage(chatId, "Указать имя", null);
                }
                default -> sendMessage(chatId, "Неизвестная команда: " + data, null);
            }
        } catch (InvalidCallbackPayloadException e) {
            log.error("Получены некорректные данные в callback: {}", data, e);
            sendMessage(chatId, "Некорректный формат данных. Попробуйте еще раз.", null);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void applyAndRefreshKeyboard(
            long chatId, int messageId, String callbackId,
            String login, FriendRequest.Action action, String toastText) throws TelegramApiException {

        FriendDto friend = profileService.applyFriend(chatId, login, action);
        var kb = TelegramKeyboardFactory.getFriendInlineKeyboard(friend);

        telegramClient.execute(TelegramKeyboardFactory.editFriendInlineKeyboard(kb, chatId, messageId));
        telegramClient.execute(TelegramKeyboardFactory.createAnswerCallbackQuery(callbackId, toastText, false));
    }

    private boolean isUserInGroup(Long chatId) {
        log.info("Проверка пользователя {} в группе {}", chatId, GROUP_ID.toString());
        GetChatMember getChatMember = new GetChatMember(GROUP_ID.toString(), chatId);
        try {
            ChatMember chatMember = telegramClient.execute(getChatMember);
            String status = chatMember.getStatus();
            return !("left".equals(status) || "kicked".equals(status));
        } catch (TelegramApiException e) {
            log.error("Ошибка при проверке пользователя {} в группе {}", chatId, GROUP_ID.toString(), e);
            return false;
        }
    }

    private void startConfirmed(Long chatId, ProfileDto profile, String text) {
        // проверка подписан ли на канал
        if (!isUserInGroup(chatId)) {
            ReplyKeyboard keyboard = TelegramKeyboardFactory.createUrlKeyboard(telegramButtons.getSubscribeButton(),1);
            sendMessage(chatId, "Подпишитесь на канал", keyboard);
            return;
        }

        if (SlashCommandEnum.contains(text)) {
            SlashCommandEnum command = SlashCommandEnum.fromText(text).get();
            switch (command) {
                case START -> {
                    ReplyKeyboard keyboard = TelegramKeyboardFactory.createReplyKeyboard(MenuCommandEnum.getAllMenuCommands(), 3);
                    sendMessage(chatId, "Выберите команду", keyboard);
                }
                case ME -> {
                    sendMessage(chatId, "Твой telegram id: " + profile.telegramId(), null);
                }
                case HELP -> {
                    sendMessage(chatId, "Помощь по командам бота", null);
                }
                case DONATE -> sendMessage(chatId, "\uD83D\uDCB8 На работу бота и корм кисе \uD83D\uDE3D", null);
            }
        }

        // если текст это команда меню
        if (MenuCommandEnum.contains(text)) {
            MenuCommandEnum command = MenuCommandEnum.fromText(text).get();
            switch (command) {
                case SEARCH -> {
                    var lastCommand = new LastCommandState();
                    lastCommand.setCommand(LastCommandType.SEARCH);
                    setLastCommand(chatId, lastCommand);
                    sendMessage(chatId, "Введите логин для поиска", null);
                }
                case FRIENDS -> {
                    showFriends(chatId, 0);
                    //sendMessage(chatId, "Друзья", null);
                }
                case PROFILE -> {
                    showProfile(chatId, profile.s21login(), null);
                }
                case EVENTS -> sendMessage(chatId, "События", null);
                case CAMPUS -> {
                    var campusMap = showCampusMap(chatId);
                    sendMessage(chatId, "Кампус " + campusMap.getCampusName() + "\n" + campusMap, null);
                }
                case PROJECTS -> sendMessage(chatId, "Проекты", null);
            }
            return;
        }

        // в ином случае нужно проверить ласт комманд и вызвать нужный метод
        LastCommandType.fromName(profile.lastCommand()).ifPresent(cmd -> {
            switch (cmd) {
                case SEARCH -> {
                    if (isValidLogin(text)) {
                        FriendDto friend = profileService.applyFriend(chatId, text, FriendRequest.Action.NONE);
                        InlineKeyboardMarkup keyboard = TelegramKeyboardFactory.getFriendInlineKeyboard(friend);
                        showProfile(chatId, text, keyboard);
                    } else {
                        sendMessage(chatId, "Логин должен быть от 3 до 30 символов и состоять только из латинских букв", null);
                    }
                }
                case SET_NAME -> {
                    if (text.length() > 100) {
                        sendMessage(chatId, "Имя должно быть не более 100 символов", null);
                    } else {
                        var login = profile.lastCommand().getArgs().get("login");
                        profileService.updateProfileFriendName(chatId, login, FriendRequest.Action.SET_NAME, text);
                        sendMessage(chatId, "Имя успешно обновлено", null);
                    }
                }
            }

            setLastCommand(chatId, null);
        });
    }

    private void showFriends(Long chatId, int page) {
        try {
            var list = profileService.getFriends(chatId, page, PAGE_SIZE);
            boolean hasNext = list.size() == PAGE_SIZE;
            //var keyboard = TelegramKeyboardFactory.getFriendsInlineKeyboard(list, page, hasNext);
            sendMessage(chatId, "Друзья\n\n" + list, null);
        } catch (FeignException e) {
            sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже", null);
            sendMessage(ADMIN_ID, e.contentUTF8(), null);
        }
    }

    private void showProfile(Long chatId, String login, InlineKeyboardMarkup keyboard) {
        try {
            ParticipantDto showProfile = profileService.showParticipant(chatId.toString(), login);
            sendMessage(chatId, "Профиль\n" + showProfile, keyboard);
        } catch (FeignException e) {
            sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже", null);
            sendMessage(ADMIN_ID, e.contentUTF8(), null);
        }
    }

    private void setLastCommand(Long chatId, LastCommandState command) {
        profileService.setLastCommand(chatId, command);
    }

    private CampusResponse showCampusMap(Long chatId) {
        return profileService.showCampusMap(chatId);
    }

    private void startValidation(Long chatId, ProfileDto profile, String text) {
        var code = profileService.getVerificationCode(profile.s21login());
        if (code.getSecretCode().equals(text)) {
            profileService.updateProfileStatus(chatId, ProfileStatus.CONFIRMED);
            sendMessage(chatId, "Ваш аккаунт был успешно зарегистрирован", TelegramKeyboardFactory.createReplyKeyboard(MenuCommandEnum.getAllMenuCommands(), 3));
        } else {
            sendMessage(chatId, "Введенный код не совпадает!", TelegramKeyboardFactory.removeReplyKeyboard());
        }
    }

    private void startRegistration(Long chatId, ProfileDto profile, String text) {
        if (!isValidLogin(text)) {
            sendMessage(chatId, "Введенный логин не соответствует требованиям", null);
            return;
        }

        ParticipantV1DTO participant;
        try {
            participant = profileService.checkEduLogin(text);
        } catch (EduLoginCheckException e) {
            ErrorResponseDTO error = e.getError();
            sendMessage(chatId, "Ошибка проверки логина: " + error.getMessage(), null);
            sendMessage(ADMIN_ID, "Ошибка проверки логина: " + error, null);
            return;
        }

        // проверяем что профиль активный
        if (participant.getStatus() != ParticipantV1DTO.StatusEnum.ACTIVE) {
            sendMessage(chatId, "Введенный логин не активен", null);
            return;
        }

        // тут можно проверить что профиль на основе Core program
        if (participant.getParallelName() == null || !participant.getParallelName().equals("Core program")) {
            sendMessage(chatId, "Введенный логин не на основе! Приходите когда пройдете бассейн", null);
            return;
        }

        ProfileDto profileDto;
        try {
            profileDto = profileService.checkAndSetLogin(chatId, text);
        } catch (FeignException e) {
            sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже", null);
            sendMessage(ADMIN_ID, e.contentUTF8(), null);
            return;
        }
        // если логин совпадает значит мы его сохранили
        if (profileDto.s21login().equals(text)) {
            RocketChatSendResponse rocketChatResponse;
            try {
                rocketChatResponse = profileService.sendVerificationCode(text);
            } catch (RocketChatSendException e) {
                sendMessage(chatId, "Ошибка отправки сообщения в рокетчат, попробуйте позже", null);
                sendMessage(ADMIN_ID, e.getMessage(), null);
                return;
            } catch (FeignException e) {
                sendMessage(chatId, "Ошибка отправки сообщения в рокетчат, сообщите админу", null);
                sendMessage(ADMIN_ID, e.contentUTF8(), null);
                return;
            }

            sendMessage(chatId, "В рокет чат был отправлен код для подтверждения для логина " + text, null);
            sendMessage(ADMIN_ID, "В рокет чат было отправлено сообщение " + rocketChatResponse.getMessage(), null);
            profileService.updateProfileStatus(chatId, ProfileStatus.VALIDATION);
        }

        // тут мы получает в тексте логин на платформе
        // надо отправить его на бэкенд и получить дто с какими-то полями
        // первое что мы проверяем что профиль существует или нет
        //   - если нет то пишем что вы ошиблись попробуйте еще раз
        // второе если существует
        //   - если он уже зарегистрирован то говорим что этот профиль уже привязан к другому телеграму
        //   - если он не зарегистрирован то смотрим на его статус
        //          - статус заблокирован - не можем зарегать такой профиль (можно поменять статус на блокированный)
        //          - статус заморожен - тоже выставляем блок
        //          - статус не на основе пишем сообщение что не можем зарегать пока не будет на основе (тут другой статус)
        // если все хорошо и профиль активный и на основе то привязываем его к этому телеграм айди и пытаемся начать валидацию


    }

    private void startOnboarding(Long chatId) {
        InlineKeyboardMarkup keyboard = TelegramKeyboardFactory.createInlineKeyboardMarkup(telegramButtons.getRegistrationButton(), 1);
        sendMessage(chatId, "Для регистрации нажмите кнопку ниже", keyboard);
    }

    private boolean isValidLogin(String login) {
        return login != null && login.matches("^[a-zA-Z]{3,30}$");
    }

    public void sendMessage(Long chatId, String text, ReplyKeyboard replyKeyboard) {
        SendMessage response = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(replyKeyboard)
                .build();

        try {
            telegramClient.execute(response);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения для {}: {}", chatId, text, e);
        }
    }

    public void removeInlineKeyboard(Long chatId, Integer messageId) {
        EditMessageReplyMarkup editMarkup = EditMessageReplyMarkup.builder()
                .chatId(chatId.toString()) // обязательно String, а не Long
                .messageId(messageId)
                .replyMarkup(null) // это удаляет клавиатуру
                .build();

        try {
            telegramClient.execute(editMarkup);
        } catch (TelegramApiException e) {
            log.error("Ошибка при удалении клавиатуры у сообщения {}: {}", messageId, e.getMessage());
        }
    }

    public void removeReplyKeyboard(Long chatId, String message) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId.toString())
                .text(message)
                .replyMarkup(new ReplyKeyboardRemove(true)) // удаляет клавиатуру
                .build();

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка при удалении reply keyboard: {}", e.getMessage());
        }
    }

    public void updateMessageAndChangeStatusRegistration(Long chatId, Integer messageId, String newText) {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId.toString()) // обязательно как String
                .messageId(messageId)
                .text(newText)
                .replyMarkup(null) // удаляет кнопки
                .build();

        try {
            telegramClient.execute(editMessage);
            profileService.updateProfileStatus(chatId, ProfileStatus.REGISTRATION);
        } catch (TelegramApiException e) {
            log.error("Ошибка при редактировании сообщения {}: {}", messageId, e.getMessage());
        } catch (FeignException e) {
            log.error("Ошибка обработки профиля", e);
        }
    }
}
