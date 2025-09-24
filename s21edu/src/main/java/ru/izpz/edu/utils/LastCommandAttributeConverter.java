package ru.izpz.edu.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import ru.izpz.dto.LastCommandState;

@Slf4j
@Converter
public class LastCommandAttributeConverter implements AttributeConverter<LastCommandState, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public String convertToDatabaseColumn(LastCommandState attribute) {
        if (attribute == null) return null;
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            log.error("lastCommand serialize error", e);
            throw new IllegalStateException("lastCommand serialize error", e);
        }
    }

    @Override
    public LastCommandState convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return MAPPER.readValue(dbData, LastCommandState.class);
        } catch (Exception e) {
            log.error("lastCommand deserialize error", e);
            return null;
        }
    }
}
