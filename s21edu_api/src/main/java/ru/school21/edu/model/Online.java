package ru.school21.edu.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
public class Online {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(updatable = false, nullable = false)
    UUID id;
    String login;
    Boolean isOnline;
}
