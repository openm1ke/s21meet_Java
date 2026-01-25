package ru.izpz.edu.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import ru.izpz.dto.EventDto;
import ru.izpz.dto.model.EventV1DTO;
import ru.izpz.edu.model.Event;

@Mapper(componentModel = "spring")
public interface EventMapper {
    Event toEntity(EventV1DTO dto);
    void update(@MappingTarget Event target, EventV1DTO dto);

    EventDto fromEventToDto(Event event);
    Event fromDtoToEvent(EventDto dto);
}
