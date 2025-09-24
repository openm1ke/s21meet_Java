package ru.izpz.dto;

import java.util.Arrays;
import java.util.Optional;

public enum LastCommandType {
    SEARCH,
    SET_NAME,
    NONE;

    public static Optional<LastCommandType> fromName(LastCommandState state) {
        if (state == null || state.command() == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(e -> e.name().equalsIgnoreCase(state.command().name()))
                .findFirst();
    }
}
