package ru.izpz.edu.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.izpz.dto.ParticipantStatusEnum;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "participant")
public class Participant {

    @Id
    private String login;
    @Enumerated(EnumType.STRING)
    private ParticipantStatusEnum status;

    private String className;
    private String parallelName;

    private int expValue;
    private int level;
    private int expToNextLevel;
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campus_id", nullable = false)
    private ParticipantCampus campus;
}
