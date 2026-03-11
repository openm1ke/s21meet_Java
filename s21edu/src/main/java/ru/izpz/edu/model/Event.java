package ru.izpz.edu.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    private Long id;
    private String type;
    private String name;
    private String description;
    private String location;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private List<String> organizers;
    private Integer capacity;
    private Integer registerCount;
}
