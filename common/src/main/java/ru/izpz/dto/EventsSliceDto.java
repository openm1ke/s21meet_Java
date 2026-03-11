package ru.izpz.dto;

import java.util.List;

public record EventsSliceDto(
    List<EventDto> content,
    int page,
    int size,
    boolean hasNext
) {
}
