package ru.izpz.edu.model;

import jakarta.persistence.Column;
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
    @Column(nullable = true)
    String login;
}
