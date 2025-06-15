package ru.izpz.bot.keyboard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
public enum SlashCommandEnum {
    START("/start"),
    HELP("/help"),
    ME("/me"),
    DONATE("/donate");

    private final String command;

    private static final Map<String, SlashCommandEnum> COMMANDS_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(SlashCommandEnum::getCommand, e -> e));

    public static boolean contains(String text) {
        return COMMANDS_MAP.containsKey(text);
    }

    public static Optional<SlashCommandEnum> fromText(String text) {
        return Optional.ofNullable(COMMANDS_MAP.get(text));
    }
}
