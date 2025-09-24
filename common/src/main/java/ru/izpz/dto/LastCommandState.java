package ru.izpz.dto;

import java.util.Map;

public record LastCommandState(LastCommandType command, Map<String, Object> args) {
}
