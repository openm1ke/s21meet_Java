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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
            SlashCommandEnum.fromText(text).ifPresent(command -> handleSlashCommand(chatId, profile, command));
        }

        // если текст это команда меню
        if (MenuCommandEnum.contains(text)) {
            MenuCommandEnum.fromText(text).ifPresent(command -> {
                // Записываем метрику для команд клавиатуры
                metricsService.recordButtonPress(command.name(), ButtonMetricType.KEYBOARD);
                handleMenuCommand(chatId, profile, command);
            });
            return;
        }

        // в ином случае нужно проверить ласт комманд и вызвать нужный метод
        LastCommandType.fromName(profile.lastCommand()).ifPresent(cmd -> {
            // Записываем метрику для LastCommand
            metricsService.recordButtonPress(cmd.name(), ButtonMetricType.LAST_COMMAND);
            handleLastCommand(chatId, profile, text, cmd);
            callbackHandler.setLastCommand(chatId, null, null);
        });
    }

    private void handleSlashCommand(Long chatId, ProfileDto profile, SlashCommandEnum command) {
        switch (command) {
            case START -> {
                ReplyKeyboard keyboard = telegramKeyboardFactory.createReplyKeyboard(MenuCommandEnum.getAllMenuCommands(), 3);
                messageSender.sendMessage(chatId, "Выберите команду", keyboard);
            }
            case ME -> messageSender.sendMessage(chatId, "Твой telegram id: " + profile.telegramId(), null);
            case HELP -> messageSender.sendMessage(chatId, "Помощь по командам бота", null);
            case DONATE -> messageSender.sendMessage(chatId, "\uD83D\uDCB8 На работу бота и корм кисе \uD83D\uDE3D", null);
        }
    }

    private void handleMenuCommand(Long chatId, ProfileDto profile, MenuCommandEnum command) {
        switch (command) {
            case SEARCH -> {
                callbackHandler.setLastCommand(chatId, LastCommandType.SEARCH, null);
                messageSender.sendMessage(chatId, "Введите логин для поиска", null);
            }
            case FRIENDS -> callbackHandler.showFriends(chatId, 0, null);
            case PROFILE -> {
                ParticipantDto showProfile = profileService.showParticipant(chatId.toString(), profile.s21login());
                messageSender.sendMessage(chatId, ParticipantMessageFormatter.format(showProfile), null);
            }
            case EVENTS -> callbackHandler.showEvents(chatId, 0, null);
            case CAMPUS -> {
                var campusMap = showCampusMap(chatId);
                messageSender.sendMessage(chatId, formatCampusMessage(campusMap), null);
            }
            case PROJECTS -> {
                var projectsByLogin = getProjectsByLogin(profile.s21login());
                messageSender.sendMessage(chatId, projectsByLogin, null);
            }
        }
    }

    private void handleLastCommand(Long chatId, ProfileDto profile, String text, LastCommandType cmd) {
        if (cmd == LastCommandType.SEARCH) {
            callbackHandler.showProfile(chatId, text);
        } else if (cmd == LastCommandType.SET_NAME) {
            if (text.length() > 100) {
                messageSender.sendMessage(chatId, "Имя должно быть не более 100 символов", null);
            } else {
                var login = profile.lastCommand().args().get("login").toString();
                profileService.applyFriend(chatId, login, FriendRequest.Action.SET_NAME, text);
                messageSender.sendMessage(chatId, "Имя успешно обновлено", null);
            }
        }
        // Остальные enum-значения либо игнорируются, либо не выставляются в LastCommandState.
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

    private String formatCampusMessage(CampusResponse campusMap) {
        List<Clusters> clusters = campusMap.getClusters() == null ? List.of() : campusMap.getClusters();
        int busy = calculateBusy(clusters);
        int free = calculateFree(clusters);
        int all = calculateAll(clusters);

        StringBuilder text = new StringBuilder();
        text.append("🏕️ ").append(safeString(campusMap.getCampusName())).append(" campus 🎪\n");
        text.append("🪑 Busy ").append(busy).append(" / Free ").append(free).append(" / All ").append(all);

        appendFloorsSection(text, clusters);
        appendProgramStatsSection(text, campusMap.getProgramStats());

        return text.toString().trim();
    }

    private int calculateBusy(List<Clusters> clusters) {
        return clusters.stream()
                .mapToInt(cluster -> toNonNegative(cluster.getCapacity()) - toNonNegative(cluster.getAvailableCapacity()))
                .sum();
    }

    private int calculateFree(List<Clusters> clusters) {
        return clusters.stream()
                .mapToInt(cluster -> toNonNegative(cluster.getAvailableCapacity()))
                .sum();
    }

    private int calculateAll(List<Clusters> clusters) {
        return clusters.stream()
                .mapToInt(cluster -> toNonNegative(cluster.getCapacity()))
                .sum();
    }

    private void appendFloorsSection(StringBuilder text, List<Clusters> clusters) {
        Map<Integer, List<Clusters>> clustersByFloor = groupClustersByFloor(clusters);
        if (clustersByFloor.isEmpty()) {
            return;
        }
        text.append("\n\n");
        for (Map.Entry<Integer, List<Clusters>> entry : clustersByFloor.entrySet()) {
            appendFloor(text, entry.getKey(), entry.getValue());
        }
    }

    private Map<Integer, List<Clusters>> groupClustersByFloor(List<Clusters> clusters) {
        Map<Integer, List<Clusters>> clustersByFloor = new TreeMap<>();
        for (Clusters cluster : clusters) {
            int floor = cluster.getFloor() == null ? 0 : cluster.getFloor();
            clustersByFloor.computeIfAbsent(floor, key -> new ArrayList<>()).add(cluster);
        }
        return clustersByFloor;
    }

    private void appendFloor(StringBuilder text, int floor, List<Clusters> floorClusters) {
        text.append(getFloorEmoji(floor)).append(" Floor\n");
        String clusterIcon = floor % 2 == 0 ? "🔸" : "🔹";
        floorClusters.stream()
                .sorted(Comparator.comparing(cluster -> safeString(cluster.getName())))
                .forEach(cluster -> appendClusterLine(text, clusterIcon, cluster));
    }

    private void appendClusterLine(StringBuilder text, String clusterIcon, Clusters cluster) {
        int clusterAll = toNonNegative(cluster.getCapacity());
        int clusterFree = toNonNegative(cluster.getAvailableCapacity());
        int clusterBusy = Math.max(clusterAll - clusterFree, 0);
        text.append(clusterIcon)
                .append(" ")
                .append(safeString(cluster.getName()))
                .append(" - ")
                .append(clusterBusy)
                .append(" / ")
                .append(clusterFree)
                .append(" / ")
                .append(clusterAll)
                .append("\n");
    }

    private void appendProgramStatsSection(StringBuilder text, Map<String, Long> programStats) {
        if (programStats == null || programStats.isEmpty()) {
            return;
        }
        text.append("\n");
        programStats.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(entry -> entry.getValue() == null ? 0L : entry.getValue())
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .forEach(entry -> text
                        .append(formatProgramLabel(entry.getKey()))
                        .append(": ")
                        .append(entry.getValue() == null ? 0L : entry.getValue())
                        .append("\n"));
    }

    private int toNonNegative(Integer value) {
        if (value == null || value < 0) {
            return 0;
        }
        return value;
    }

    private String safeString(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value;
    }

    private String getFloorEmoji(int floorNumber) {
        String floor = String.valueOf(floorNumber);
        StringBuilder result = new StringBuilder();
        for (char ch : floor.toCharArray()) {
            result.append(switch (ch) {
                case '0' -> "0️⃣";
                case '1' -> "1️⃣";
                case '2' -> "2️⃣";
                case '3' -> "3️⃣";
                case '4' -> "4️⃣";
                case '5' -> "5️⃣";
                case '6' -> "6️⃣";
                case '7' -> "7️⃣";
                case '8' -> "8️⃣";
                case '9' -> "9️⃣";
                default -> String.valueOf(ch);
            });
        }
        return result.toString();
    }

    private String formatProgramLabel(String programName) {
        String normalized = programName == null ? "" : programName.trim();
        String lower = normalized.toLowerCase();
        if (lower.equals("no data")) {
            return "👽 No data";
        }
        if (lower.contains("intensive") || lower.contains("parallel")) {
            return "⚡ " + normalized;
        }
        return "🧢 " + normalized;
    }
}
