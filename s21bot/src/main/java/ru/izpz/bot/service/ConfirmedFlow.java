package ru.izpz.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import ru.izpz.bot.keyboard.MenuCommandEnum;
import ru.izpz.bot.keyboard.SlashCommandEnum;
import ru.izpz.bot.keyboard.TelegramButtons;
import ru.izpz.bot.keyboard.TelegramKeyboardFactory;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.dto.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmedFlow {

    private final BotProperties botProperties;
    private final ProfileService profileService;
    private final TelegramButtons telegramButtons;
    private final MessageSender messageSender;
    private final TelegramKeyboardFactory telegramKeyboardFactory;
    private final CallbackHandler callbackHandler;
    private final MetricsService metricsService;

    public void startConfirmed(Long chatId, ProfileDto profile, String text) {
        // проверка подписан ли на канал
        if (!isUserInGroup(chatId)) {
            ReplyKeyboard keyboard = telegramKeyboardFactory.createUrlKeyboard(telegramButtons.getSubscribeButton(), 1);
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
                    case ME -> messageSender.sendMessage(chatId, "Твой telegram id: " + profile.telegramId(), null);
                    case HELP -> messageSender.sendMessage(chatId, "Помощь по командам бота", null);
                    case DONATE -> messageSender.sendMessage(chatId, "\uD83D\uDCB8 На работу бота и корм кисе \uD83D\uDE3D", null);
                }
            });
        }

        // если текст это команда меню
        if (MenuCommandEnum.contains(text)) {
            MenuCommandEnum.fromText(text).ifPresent(command -> {
                // Записываем метрику для команд клавиатуры
                metricsService.recordButtonPress(chatId, command.name(), "keyboard");
                
                switch (command) {
                    case SEARCH -> {
                        callbackHandler.setLastCommand(chatId, LastCommandType.SEARCH, null);
                        messageSender.sendMessage(chatId, "Введите логин для поиска", null);
                    }
                    case FRIENDS -> callbackHandler.showFriends(chatId, 0, null);
                    case PROFILE -> {
                        ParticipantDto showProfile = profileService.showParticipant(chatId.toString(), profile.s21login());
                        messageSender.sendMessage(chatId, "Профиль\n" + showProfile, null);
                    }
                    case EVENTS -> callbackHandler.showEvents(chatId, 0, null);
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
            // Записываем метрику для LastCommand
            metricsService.recordButtonPress(chatId, cmd.name(), "last_command");
            
            switch (cmd) {
                case SEARCH -> callbackHandler.showProfile(chatId, text);
                case SET_NAME -> {
                    if (text.length() > 100) {
                        messageSender.sendMessage(chatId, "Имя должно быть не более 100 символов", null);
                    } else {
                        var login = profile.lastCommand().args().get("login").toString();
                        profileService.applyFriend(chatId, login, FriendRequest.Action.SET_NAME, text);
                        messageSender.sendMessage(chatId, "Имя успешно обновлено", null);
                    }
                }
                default -> log.warn("Unhandled LastCommandType: {}", cmd);
            }

            callbackHandler.setLastCommand(chatId, null, null);
        });
    }

    private boolean isUserInGroup(Long chatId) {
        Long groupId = botProperties.group();
        log.info("Проверка пользователя {} в группе {}", chatId, groupId.toString());
        GetChatMember getChatMember = new GetChatMember(groupId.toString(), chatId);
        return messageSender.execute(getChatMember)
                .map(ChatMember::getStatus)
                .map(status -> !("left".equals(status) || "kicked".equals(status)))
                .orElse(false);
    }

    private String getProjectsByLogin(String login) {
        var projects = profileService.getProjects(login);
        if (projects.isEmpty()) {
            return "У вас нет активных проектов";
        }

        StringBuilder result = new StringBuilder("Ваши активные проекты:%n%n");
        for (ProjectsDto project : projects) {
            result.append(String.format("\uD83D\uDCC1 %s%n", project.name()));
            if (project.description() != null && !project.description().isEmpty()) {
                result.append(String.format("\uD83D\uDCDD %s%n", project.description()));
            }
            if (project.experience() != null) {
                result.append(String.format("⭐ Опыт: %d%n", project.experience()));
            }
            if (project.displayedCourseStatus() != null) {
                result.append(String.format("\uD83D\uDCCA Статус: %s%n", project.displayedCourseStatus()));
            }
            result.append("\n");
        }
        return result.toString();
    }

    private CampusResponse showCampusMap(Long chatId) {
        return profileService.showCampusMap(chatId);
    }
}
