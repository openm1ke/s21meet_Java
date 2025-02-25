package ru.school21.edu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Coalition {
    @Id
    Long coalitionId;
    String name;
    String campusId;
}
