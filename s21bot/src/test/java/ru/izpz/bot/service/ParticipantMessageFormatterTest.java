package ru.izpz.bot.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.izpz.dto.ParticipantCampusDto;
import ru.izpz.dto.ParticipantCoalitionDto;
import ru.izpz.dto.ParticipantDto;
import ru.izpz.dto.ParticipantSeatDto;
import ru.izpz.dto.ParticipantStatusEnum;
import ru.izpz.dto.ProjectsDto;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticipantMessageFormatterTest {

    @Test
    void format_shouldUseActiveEmojiAndCampusName() {
        ParticipantDto participant = new ParticipantDto();
        participant.setLogin("quarkron");
        participant.setClassName("22_10_MSK");
        participant.setExpValue(21617);
        participant.setLevel(12);
        participant.setParallelName("AP4_Info21");
        participant.setStatus(ParticipantStatusEnum.ACTIVE);
        participant.setCampus(new ParticipantCampusDto("id", "Moscow"));

        String message = ParticipantMessageFormatter.format(participant);

        assertTrue(message.startsWith("✅ quarkron"));
        assertTrue(message.contains("🌊22_10_MSK"));
        assertTrue(message.contains("✨21617 XP (level 12)"));
        assertTrue(message.contains("🕒Last online: -"));
        assertTrue(message.contains("📍Moscow"));
    }

    @Test
    void format_shouldIncludeCoalition_whenPresent() {
        ParticipantDto participant = new ParticipantDto();
        participant.setLogin("quarkron");
        participant.setClassName("22_10_MSK");
        participant.setExpValue(21617);
        participant.setLevel(12);
        participant.setParallelName("AP4_Info21");
        participant.setStatus(ParticipantStatusEnum.ACTIVE);
        participant.setCoalition(new ParticipantCoalitionDto("Capybaras", 1085, 271));
        participant.setCampus(new ParticipantCampusDto("id", "Moscow"));

        String message = ParticipantMessageFormatter.format(participant);

        assertTrue(message.contains("🦙Capybaras 271 / 1085"));
    }

    @Test
    void format_shouldIncludeProjectsShortList_whenProvided() {
        ParticipantDto participant = new ParticipantDto();
        participant.setLogin("quarkron");
        participant.setClassName("22_10_MSK");
        participant.setExpValue(21617);
        participant.setLevel(12);
        participant.setParallelName("AP4_Info21");
        participant.setStatus(ParticipantStatusEnum.ACTIVE);
        participant.setCoalition(new ParticipantCoalitionDto("Capybaras", 1085, 271));
        participant.setCampus(new ParticipantCampusDto("id", "Moscow"));

        String message = ParticipantMessageFormatter.format(
                participant,
                List.of(
                        new ProjectsDto("g1", "Libft", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null),
                        new ProjectsDto("g2", "minishell", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
                )
        );

        Assertions.assertTrue(message.contains("🦙Capybaras 271 / 1085"));
        Assertions.assertTrue(message.contains("📁Libft, minishell"));
    }

    @Test
    void format_shouldNotIncludeProjectsLine_whenProjectsAreNull() {
        ParticipantDto participant = new ParticipantDto();
        participant.setLogin("quarkron");
        participant.setClassName("22_10_MSK");
        participant.setExpValue(21617);
        participant.setLevel(12);
        participant.setParallelName("AP4_Info21");
        participant.setStatus(ParticipantStatusEnum.ACTIVE);
        participant.setCampus(new ParticipantCampusDto("id", "Moscow"));

        String message = ParticipantMessageFormatter.format(participant, null);

        Assertions.assertFalse(message.contains("📁"));
    }

    @Test
    void format_shouldSkipBlankProjectNames_whenBuildingProjectsLine() {
        ParticipantDto participant = new ParticipantDto();
        participant.setLogin("quarkron");
        participant.setClassName("22_10_MSK");
        participant.setExpValue(21617);
        participant.setLevel(12);
        participant.setParallelName("AP4_Info21");
        participant.setStatus(ParticipantStatusEnum.ACTIVE);
        participant.setCampus(new ParticipantCampusDto("id", "Moscow"));

        String message = ParticipantMessageFormatter.format(
                participant,
                List.of(
                        new ProjectsDto("g1", " ", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null),
                        new ProjectsDto("g2", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null),
                        new ProjectsDto("g3", "minishell", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
                )
        );

        Assertions.assertTrue(message.contains("📁minishell"));
        Assertions.assertFalse(message.contains("📁,"));
    }

    @Test
    void format_shouldFallbackForNullsAndUnknownStatus() {
        ParticipantDto participant = new ParticipantDto();
        participant.setLogin(null);
        participant.setClassName(" ");
        participant.setParallelName(null);
        participant.setStatus(null);
        participant.setCampus(new ParticipantCampusDto("id", " "));

        String message = ParticipantMessageFormatter.format(participant);

        assertTrue(message.startsWith("❔ "));
        assertTrue(message.contains("🌊-"));
        assertTrue(message.contains("\uD83D\uDC68\u200D\uD83D\uDCBB-"));
        assertTrue(message.contains("📍Out of campus"));
    }

    @Test
    void format_shouldUseEmojiFromNonActiveStatus() {
        ParticipantDto participant = new ParticipantDto();
        participant.setLogin("u");
        participant.setClassName("c");
        participant.setParallelName("p");
        participant.setStatus(ParticipantStatusEnum.BLOCKED);
        participant.setCampus(null);

        String message = ParticipantMessageFormatter.format(participant);

        assertTrue(message.startsWith("🚫 u"));
    }

    @Test
    void format_shouldFallbackWhenCampusNameIsNull() {
        ParticipantDto participant = new ParticipantDto();
        participant.setLogin("u");
        participant.setClassName("c");
        participant.setParallelName("p");
        participant.setStatus(ParticipantStatusEnum.ACTIVE);
        participant.setCampus(new ParticipantCampusDto("id", null));

        String message = ParticipantMessageFormatter.format(participant);

        assertTrue(message.contains("📍Out of campus"));
    }

    @Test
    void format_shouldShowSeat_whenParticipantOnline() {
        ParticipantDto participant = new ParticipantDto();
        participant.setLogin("u");
        participant.setClassName("c");
        participant.setParallelName("p");
        participant.setStatus(ParticipantStatusEnum.ACTIVE);
        participant.setCampus(new ParticipantCampusDto("id", "Moscow"));
        participant.setIsOnline(true);
        participant.setSeat(new ParticipantSeatDto("Cluster A", "B", 3, "Core", "Stage 1"));

        String message = ParticipantMessageFormatter.format(participant);

        assertTrue(message.contains("🪑Cluster A / Stage 1 / B-3"));
    }

    @Test
    void format_shouldShowLastSeen_whenParticipantOffline() {
        ParticipantDto participant = new ParticipantDto();
        participant.setLogin("u");
        participant.setClassName("c");
        participant.setParallelName("p");
        participant.setStatus(ParticipantStatusEnum.ACTIVE);
        participant.setCampus(new ParticipantCampusDto("id", "Moscow"));
        participant.setIsOnline(false);
        participant.setLastSeenAt(OffsetDateTime.parse("2026-03-22T10:15:00+03:00"));

        String message = ParticipantMessageFormatter.format(participant);

        assertTrue(message.contains("🕒Last online: 22.03.2026 10:15"));
    }
}
