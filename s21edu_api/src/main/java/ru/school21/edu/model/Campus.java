package ru.school21.edu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "campuses")
public class Campus {
    @Id
    private String id; // UUID в виде строки

    private String fullName;
    private String shortName;
}
