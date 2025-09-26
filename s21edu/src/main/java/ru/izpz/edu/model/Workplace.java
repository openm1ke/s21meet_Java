package ru.izpz.edu.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Workplace {
    @EmbeddedId
    WorkplaceId id;
    String login;
    Integer expValue;
    Integer levelCode;
    String stageGroupName;
    String stageName;
}
