package ru.izpz.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDto {
    private String login;
    private String className;
    private String parallelName;
    private int expValue;
    private int level;
    private int expToNextLevel;
    private ParticipantStatusEnum status;
    private ParticipantCampusDto campus;
    private ParticipantCoalitionDto coalition;
    private Boolean isOnline;
    private ParticipantSeatDto seat;
    private OffsetDateTime lastSeenAt;
}
