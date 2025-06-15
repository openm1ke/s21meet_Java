package ru.izpz.bot.keyboard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
public enum MenuCommandEnum {

    SEARCH("\uD83D\uDD0E Поиск"),
    FRIENDS("😸 Друзья"),
    PROFILE("👀 Профиль"),
    EVENTS("\uD83D\uDCDD События"),
    CAMPUS("\uD83C\uDFEB Кампус"),
    PROJECTS("\uD83D\uDCBC Проекты");

    private final String command;

    private static final Map<String, MenuCommandEnum> COMMANDS_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(MenuCommandEnum::getCommand, e -> e));

    public static boolean contains(String text) {
        return COMMANDS_MAP.containsKey(text);
    }

    public static List<String> getAllMenuCommands() {
        return Arrays.stream(values())
                .map(MenuCommandEnum::getCommand)
                .collect(Collectors.toList());
    }

    public static Optional<MenuCommandEnum> fromText(String text) {
        return Optional.ofNullable(COMMANDS_MAP.get(text));
    }
}
