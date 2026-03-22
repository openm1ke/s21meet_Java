package ru.izpz.bot.service;

import ru.izpz.dto.ProjectsDto;
import ru.izpz.dto.ParticipantCampusDto;
import ru.izpz.dto.ParticipantCoalitionDto;
import ru.izpz.dto.ParticipantDto;
import ru.izpz.dto.ParticipantSeatDto;
import ru.izpz.dto.ParticipantStatusEnum;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public final class ParticipantMessageFormatter {

    private static final DateTimeFormatter LAST_SEEN_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private ParticipantMessageFormatter() {
    }

    public static String format(ParticipantDto participant) {
        return format(participant, List.of());
    }

    public static String format(ParticipantDto participant, List<ProjectsDto> projects) {
        StringBuilder message = new StringBuilder();
        message.append(statusEmoji(participant.getStatus())).append(" ").append(safe(participant.getLogin())).append("\n\n");
        message.append("🌊").append(safeOrDash(participant.getClassName())).append("\n");
        message.append("✨").append(participant.getExpValue()).append(" XP (level ").append(participant.getLevel()).append(")\n");
        message.append("\uD83D\uDC68\u200D\uD83D\uDCBB").append(safeOrDash(participant.getParallelName())).append("\n\n");
        appendCoalition(message, participant);
        appendProjects(message, projects);
        appendPresence(message, participant);
        message.append("📍").append(formatCampus(participant.getCampus()));
        return message.toString();
    }

    private static void appendCoalition(StringBuilder message, ParticipantDto participant) {
        ParticipantCoalitionDto coalition = participant.getCoalition();
        if (coalition == null || coalition.getName() == null || coalition.getName().isBlank()) {
            return;
        }
        String rank = coalition.getRank() == null ? "-" : String.valueOf(coalition.getRank());
        String memberCount = coalition.getMemberCount() == null ? "-" : String.valueOf(coalition.getMemberCount());
        message.append("🦙").append(coalition.getName()).append(" ").append(rank).append(" / ").append(memberCount).append("\n\n");
    }

    private static void appendProjects(StringBuilder message, List<ProjectsDto> projects) {
        if (projects == null || projects.isEmpty()) {
            return;
        }
        String names = projects.stream()
                .map(ProjectsDto::name)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining(", "));
        if (names.isBlank()) {
            return;
        }
        message.append("📁").append(names).append("\n\n");
    }

    private static String formatCampus(ParticipantCampusDto campus) {
        if (campus == null || campus.getCampusName() == null || campus.getCampusName().isBlank()) {
            return "Out of campus";
        }
        return campus.getCampusName();
    }

    private static void appendPresence(StringBuilder message, ParticipantDto participant) {
        if (Boolean.TRUE.equals(participant.getIsOnline())) {
            String seat = formatSeat(participant);
            message.append("🪑").append(seat).append("\n");
            return;
        }
        message.append("🕒").append("Last online: ").append(formatLastSeen(participant.getLastSeenAt())).append("\n");
    }

    private static String formatSeat(ParticipantDto participant) {
        ParticipantSeatDto seat = participant.getSeat();
        if (seat == null) {
            return "- / - / --";
        }
        String cluster = safeOrDash(seat.getClusterName());
        String stage = safeOrDash(seat.getStageName());
        String row = safeOrDash(seat.getRow());
        String number = seat.getNumber() == null ? "-" : String.valueOf(seat.getNumber());
        return cluster + " / " + stage + " / " + row + "-" + number;
    }

    private static String formatLastSeen(OffsetDateTime lastSeenAt) {
        if (lastSeenAt == null) {
            return "-";
        }
        return LAST_SEEN_FORMAT.format(lastSeenAt);
    }

    private static String safeOrDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private static String statusEmoji(ParticipantStatusEnum status) {
        if (status == null) {
            return "❔";
        }
        if (status == ParticipantStatusEnum.ACTIVE) {
            return "✅";
        }
        return status.getEmoji();
    }
}
