package ru.izpz.bot.service;

import ru.izpz.dto.EventDto;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public final class EventMessageFormatter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private EventMessageFormatter() {
    }

    public static String format(EventDto event) {
        StringBuilder message = new StringBuilder();
        message.append("\uD83D\uDDD3\uFE0F ").append(safeOrDash(event.name())).append("\n");
        message.append("\uD83D\uDCAC ").append(safeOrDash(event.description())).append("\n");
        message.append("📍 ").append(safeOrDash(event.location())).append("\n");
        message.append("\uD83D\uDE4C️ ").append(formatDateRange(event.startDateTime(), event.endDateTime())).append("\n");
        message.append("👥 ").append(formatRegistered(event.registerCount())).append(" / ").append(formatCapacity(event.capacity())).append("\n");
        message.append("\uD83D\uDC68\u200D\uD83D\uDCBC ").append(formatOrganizers(event.organizers()));
        return message.toString();
    }

    private static String formatDate(OffsetDateTime value) {
        if (value == null) {
            return "-";
        }
        return DATE_FORMAT.format(value);
    }

    private static String formatCapacity(Integer capacity) {
        if (capacity == null) {
            return "-";
        }
        return String.valueOf(capacity);
    }

    private static String formatRegistered(Integer registered) {
        if (registered == null) {
            return "-";
        }
        return String.valueOf(registered);
    }

    private static String formatDateRange(OffsetDateTime start, OffsetDateTime end) {
        return formatDate(start) + " - " + formatDate(end);
    }

    private static String formatOrganizers(List<String> organizers) {
        if (organizers == null || organizers.isEmpty()) {
            return "-";
        }
        String joined = organizers.stream()
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining(", "));
        return joined.isBlank() ? "-" : joined;
    }

    private static String safeOrDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
