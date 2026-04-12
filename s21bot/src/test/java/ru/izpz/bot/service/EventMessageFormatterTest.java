package ru.izpz.bot.service;

import org.junit.jupiter.api.Test;
import ru.izpz.dto.EventDto;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EventMessageFormatterTest {

    @Test
    void format_shouldRenderEventCardWithRequestedFields() {
        EventDto event = new EventDto(
                11L,
                "meetup",
                "Java Meetup",
                "Доклады и нетворкинг",
                "Кампус Москва",
                OffsetDateTime.parse("2026-04-15T18:30:00+03:00"),
                OffsetDateTime.parse("2026-04-15T21:00:00+03:00"),
                List.of("S21 Team", "DevRel"),
                90,
                54
        );

        String message = EventMessageFormatter.format(event);

        assertTrue(message.startsWith("🗓️ Java Meetup"));
        assertTrue(message.contains("\n💬 Доклады и нетворкинг\n"));
        assertTrue(message.contains("📍 Кампус Москва"));
        assertTrue(message.contains("🙌️ 15.04.2026 18:30 - 15.04.2026 21:00"));
        assertTrue(message.contains("👥 54 / 90"));
        assertTrue(message.contains("👨‍💼 S21 Team, DevRel"));
    }

    @Test
    void format_shouldFallbackToDashForMissingData() {
        EventDto event = new EventDto(
                12L,
                null,
                null,
                "",
                " ",
                null,
                null,
                null,
                null,
                null
        );

        String message = EventMessageFormatter.format(event);

        assertTrue(message.startsWith("🗓️ -"));
        assertTrue(message.contains("\n💬 -\n"));
        assertTrue(message.contains("📍 -"));
        assertTrue(message.contains("🙌️ - - -"));
        assertTrue(message.contains("👥 - / -"));
        assertTrue(message.contains("👨‍💼 -"));
    }

    @Test
    void format_shouldFallbackToDashForEmptyOrganizers() {
        EventDto event = new EventDto(
                13L,
                "Name",
                "Title",
                "Desc",
                "Loc",
                OffsetDateTime.parse("2026-04-15T18:30:00+03:00"),
                OffsetDateTime.parse("2026-04-15T21:00:00+03:00"),
                List.of(),
                10,
                5
        );

        String message = EventMessageFormatter.format(event);

        assertTrue(message.contains("👨‍💼 -"));
    }

    @Test
    void format_shouldFilterBlankAndNullOrganizers() {
        EventDto event = new EventDto(
                14L,
                "type",
                "Title",
                "Desc",
                "Loc",
                OffsetDateTime.parse("2026-04-16T10:00:00+03:00"),
                OffsetDateTime.parse("2026-04-16T12:00:00+03:00"),
                List.of("Org A", " ", "Org B"),
                20,
                10
        );

        String message = EventMessageFormatter.format(event);

        assertTrue(message.contains("👨‍💼 Org A, Org B"));
    }

    @Test
    void format_shouldFallbackWhenOrganizersContainOnlyBlankValues() {
        EventDto event = new EventDto(
                15L,
                "type",
                "Title",
                "Desc",
                "Loc",
                OffsetDateTime.parse("2026-04-16T10:00:00+03:00"),
                OffsetDateTime.parse("2026-04-16T12:00:00+03:00"),
                java.util.Arrays.asList(" ", null, ""),
                20,
                10
        );

        String message = EventMessageFormatter.format(event);

        assertTrue(message.contains("👨‍💼 -"));
    }
}
