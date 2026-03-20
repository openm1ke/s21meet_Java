package ru.izpz.bot.service;

import ru.izpz.dto.ParticipantCampusDto;
import ru.izpz.dto.ParticipantCoalitionDto;
import ru.izpz.dto.ParticipantDto;
import ru.izpz.dto.ParticipantStatusEnum;

public final class ParticipantMessageFormatter {

    private ParticipantMessageFormatter() {
    }

    public static String format(ParticipantDto participant) {
        StringBuilder message = new StringBuilder();
        message.append(statusEmoji(participant.getStatus())).append(" ").append(safe(participant.getLogin())).append("\n\n");
        message.append("🌊").append(safeOrDash(participant.getClassName())).append("\n");
        message.append("✨").append(participant.getExpValue()).append(" XP (level ").append(participant.getLevel()).append(")\n");
        message.append("\uD83D\uDC68\u200D\uD83D\uDCBB").append(safeOrDash(participant.getParallelName())).append("\n\n");
        appendCoalition(message, participant);
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

    private static String formatCampus(ParticipantCampusDto campus) {
        if (campus == null || campus.getCampusName() == null || campus.getCampusName().isBlank()) {
            return "Out of campus";
        }
        return campus.getCampusName();
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
