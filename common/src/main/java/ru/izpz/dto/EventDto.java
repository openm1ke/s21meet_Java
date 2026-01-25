package ru.izpz.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record EventDto(
    Long id,
    String type,
    String name,
    String description,
    String location,
    OffsetDateTime startDateTime,
    OffsetDateTime endDateTime,
    List<String>organizers,
    Integer capacity,
    Integer registerCount
) {}
