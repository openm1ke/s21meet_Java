package ru.izpz.edu.mapper;

import org.junit.jupiter.api.Test;
import ru.izpz.dto.model.EventV1DTO;
import ru.izpz.edu.model.Event;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventMapperTest {

    private final EventMapper mapper = org.mapstruct.factory.Mappers.getMapper(EventMapper.class);

    @Test
    void toEntity_shouldMapFields() {
        EventV1DTO dto = new EventV1DTO();
        dto.setId(42L);
        dto.setName("e");
        dto.setDescription("desc");
        dto.setStartDateTime(OffsetDateTime.now());
        dto.setEndDateTime(OffsetDateTime.now());

        Event entity = mapper.toEntity(dto);

        assertEquals(42L, entity.getId());
        assertEquals("e", entity.getName());
        assertEquals("desc", entity.getDescription());
        assertNotNull(entity.getStartDateTime());
        assertNotNull(entity.getEndDateTime());
    }
}
