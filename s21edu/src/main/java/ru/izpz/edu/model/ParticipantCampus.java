package ru.izpz.edu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "participant_campus")
public class ParticipantCampus {
    @Id
    private String id;
    private String campusName;
}
