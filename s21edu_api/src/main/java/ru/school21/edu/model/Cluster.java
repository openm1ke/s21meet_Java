package ru.school21.edu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Cluster {
    @Id
    Long clusterId;
    String name;
    Integer capacity;
    Integer availableCapacity;
    Integer floor;
    String campusId;
}
