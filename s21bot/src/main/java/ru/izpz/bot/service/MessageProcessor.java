package ru.izpz.bot.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
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

    private final ProfileService profileService;
    private final TelegramButtons telegramButtons;
    private final MessageSender messageSender;
    private final TelegramKeyboardFactory telegramKeyboardFactory;
    private final CallbackPayloadSerializer callbackPayloadSerializer;

    private static final int ROW_SIZE = 3;
    private static final int PAGE_SIZE = 2;

    public void handleTextMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText().trim();
        try {
            ProfileDto profile = profileService.getProfile(chatId);
            log.info("Profile: {}", profile.toString());
            //sendMessage(chatId, profile.toString());
            parseMessage(chatId, profile, text);
        } catch (FeignException e) {
            messageSender.sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже", null);
            messageSender.sendMessage(ADMIN_ID, e.contentUTF8(), null);
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

    private boolean isUserInGroup(Long chatId) {
        log.info("Проверка пользователя {} в группе {}", chatId, GROUP_ID.toString());
        GetChatMember getChatMember = new GetChatMember(GROUP_ID.toString(), chatId);
        ChatMember chatMember = messageSender.execute(getChatMember);
        if (chatMember == null) {
            return false;
        }
        String status = chatMember.getStatus();
        return !("left".equals(status) || "kicked".equals(status));
    }

    private void startConfirmed(Long chatId, ProfileDto profile, String text) {
        // проверка подписан ли на канал
        if (!isUserInGroup(chatId)) {
            ReplyKeyboard keyboard = telegramKeyboardFactory.createUrlKeyboard(telegramButtons.getSubscribeButton(),1);
            messageSender.sendMessage(chatId, "Подпишитесь на канал", keyboard);
            return;
        }

        if (SlashCommandEnum.contains(text)) {
            SlashCommandEnum.fromText(text).ifPresent(command -> {
                switch (command) {
                    case START -> {
                        ReplyKeyboard keyboard = telegramKeyboardFactory.createReplyKeyboard(MenuCommandEnum.getAllMenuCommands(), 3);
                        messageSender.sendMessage(chatId, "Выберите команду", keyboard);
                    }
                    case ME -> {
                        messageSender.sendMessage(chatId, "Твой telegram id: " + profile.telegramId(), null);
                    }
                    case HELP -> {
                        messageSender.sendMessage(chatId, "Помощь по командам бота", null);
                    }
                    case DONATE ->
                            messageSender.sendMessage(chatId, "\uD83D\uDCB8 На работу бота и корм кисе \uD83D\uDE3D", null);
                }
            });
        }

        // если текст это команда меню
        if (MenuCommandEnum.contains(text)) {
            MenuCommandEnum.fromText(text).ifPresent(command -> {
                switch (command) {
                    case SEARCH -> {
                        setLastCommand(chatId, LastCommandType.SEARCH, null);
                        messageSender.sendMessage(chatId, "Введите логин для поиска", null);
                    }
                    case FRIENDS -> {
                        showFriends(chatId, 0, null);
                    }
                    case PROFILE -> {
                        ParticipantDto showProfile = profileService.showParticipant(chatId.toString(), profile.s21login());
                        messageSender.sendMessage(chatId, "Профиль\n" + showProfile, null);
                    }
                    case EVENTS -> {
                        showEvents(chatId, 0, null);
                    }
                    case CAMPUS -> {
                        var campusMap = showCampusMap(chatId);
                        messageSender.sendMessage(chatId, "Кампус " + campusMap.getCampusName() + "\n" + campusMap, null);
                    }
                    case PROJECTS -> {
                        var projectsByLogin = getProjectsByLogin(profile.s21login());
                        messageSender.sendMessage(chatId, projectsByLogin, null);
                    }
                }
            });
            return;
        }

        // в ином случае нужно проверить ласт комманд и вызвать нужный метод
        LastCommandType.fromName(profile.lastCommand()).ifPresent(cmd -> {
            switch (cmd) {
                case SEARCH -> {
                    showProfile(chatId, text);
                }
                case SET_NAME -> {
                    if (text.length() > 100) {
                        messageSender.sendMessage(chatId, "Имя должно быть не более 100 символов", null);
                    } else {
                        var login = profile.lastCommand().args().get("login").toString();
                        profileService.applyFriend(chatId, login, FriendRequest.Action.SET_NAME, text);
                        messageSender.sendMessage(chatId, "Имя успешно обновлено", null);
                    }
                }
            }

            setLastCommand(chatId, null, null);
        });
    }

    private void showEvents(Long chatId, int page, Integer messageId) {
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

    private void showFriends(Long chatId, int page, Integer messageId) {
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

    private void showProfile(Long chatId, String login) {
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

    private void setLastCommand(Long chatId, LastCommandType type, Map<String, Object> args) {
        var lastCommand = new LastCommandState(type, args);
        try {
            profileService.setLastCommand(chatId, lastCommand);
        }  catch (FeignException e) {
            messageSender.sendMessage(chatId, "Ошибка установки lastCommand", null);
            messageSender.sendMessage(ADMIN_ID, e.contentUTF8(), null);
        }
    }

    private String getProjectsByLogin(String login) {
        var projects = profileService.getProjects(login);
        if (projects.isEmpty()) {
            return "У вас нет активных проектов";
        }
        
        StringBuilder result = new StringBuilder("Ваши активные проекты:\n\n");
        for (ProjectsDto project : projects) {
            result.append(String.format("📁 %s\n", project.name()));
            if (project.description() != null && !project.description().isEmpty()) {
                result.append(String.format("📝 %s\n", project.description()));
            }
            if (project.experience() != null) {
                result.append(String.format("⭐ Опыт: %d\n", project.experience()));
            }
            if (project.displayedCourseStatus() != null) {
                result.append(String.format("📊 Статус: %s\n", project.displayedCourseStatus()));
            }
            result.append("\n");
        }
        return result.toString();
    }

    private CampusResponse showCampusMap(Long chatId) {
        return profileService.showCampusMap(chatId);
    }

    private void startValidation(Long chatId, ProfileDto profile, String text) {
        var code = profileService.getVerificationCode(profile.s21login());
        if (code.getSecretCode().equals(text)) {
            profileService.updateProfileStatus(chatId, ProfileStatus.CONFIRMED);
            messageSender.sendMessage(chatId, "Ваш аккаунт был успешно зарегистрирован", telegramKeyboardFactory.createReplyKeyboard(MenuCommandEnum.getAllMenuCommands(), 3));
        } else {
            messageSender.sendMessage(chatId, "Введенный код не совпадает!", telegramKeyboardFactory.removeReplyKeyboard());
        }
    }

    private void startRegistration(Long chatId, ProfileDto profile, String text) {
        if (!isValidLogin(text)) {
            messageSender.sendMessage(chatId, "Введенный логин не соответствует требованиям", null);
            return;
        }

        ParticipantV1DTO participant;
        try {
            participant = profileService.checkEduLogin(text);
        } catch (EduLoginCheckException e) {
            ErrorResponseDTO error = e.getError();
            messageSender.sendMessage(chatId, "Ошибка проверки логина: " + error.getMessage(), null);
            messageSender.sendMessage(ADMIN_ID, "Ошибка проверки логина: " + error, null);
            return;
        }

        // проверяем что профиль активный
        if (participant.getStatus() != ParticipantV1DTO.StatusEnum.ACTIVE) {
            messageSender.sendMessage(chatId, "Введенный логин не активен", null);
            return;
        }

        // тут можно проверить что профиль на основе Core program
        if (participant.getParallelName() == null || !participant.getParallelName().equals("Core program")) {
            messageSender.sendMessage(chatId, "Введенный логин не на основе! Приходите когда пройдете бассейн", null);
            return;
        }

        ProfileDto profileDto;
        try {
            profileDto = profileService.checkAndSetLogin(chatId, text);
        } catch (FeignException e) {
            messageSender.sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже", null);
            messageSender.sendMessage(ADMIN_ID, e.contentUTF8(), null);
            return;
        }
        // если логин совпадает значит мы его сохранили
        if (profileDto.s21login().equals(text)) {
            RocketChatSendResponse rocketChatResponse;
            try {
                rocketChatResponse = profileService.sendVerificationCode(text);
            } catch (RocketChatSendException e) {
                messageSender.sendMessage(chatId, "Ошибка отправки сообщения в рокетчат, попробуйте позже", null);
                messageSender.sendMessage(ADMIN_ID, e.getMessage(), null);
                return;
            } catch (FeignException e) {
                messageSender.sendMessage(chatId, "Ошибка отправки сообщения в рокетчат, сообщите админу", null);
                messageSender.sendMessage(ADMIN_ID, e.contentUTF8(), null);
                return;
            }

            messageSender.sendMessage(chatId, "В рокет чат был отправлен код для подтверждения для логина " + text, null);
            messageSender.sendMessage(ADMIN_ID, "В рокет чат было отправлено сообщение " + rocketChatResponse.getMessage(), null);
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
        InlineKeyboardMarkup keyboard = telegramKeyboardFactory.createInlineKeyboardMarkup(telegramButtons.getRegistrationButton(), 1);
        messageSender.sendMessage(chatId, "Для регистрации нажмите кнопку ниже", keyboard);
    }

    private boolean isValidLogin(String login) {
        return login != null && login.matches("^[a-zA-Z]{3,30}$");
    }

    public void updateMessageAndChangeStatusRegistration(Long chatId, Integer messageId, String newText) {
        try {
            messageSender.updateMessage(chatId, messageId, newText, null);
            profileService.updateProfileStatus(chatId, ProfileStatus.REGISTRATION);
        } catch (FeignException e) {
            log.error("Ошибка обработки профиля", e);
        }
    }
}
