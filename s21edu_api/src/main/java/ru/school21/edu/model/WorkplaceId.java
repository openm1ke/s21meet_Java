package ru.school21.edu.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class WorkplaceId implements Serializable {
    private Long clusterId;
    private String row;
    private Integer number;
}
