package ru.izpz.edu.utils;

import org.junit.jupiter.api.Test;
import ru.izpz.dto.LastCommandState;
import ru.izpz.dto.LastCommandType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LastCommandAttributeConverterTest {

    private final LastCommandAttributeConverter converter = new LastCommandAttributeConverter();

    @Test
    void convertToDatabaseColumn_shouldReturnNull_whenAttributeNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttribute_shouldReturnNull_whenDbDataNullOrBlank() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute(""));
        assertNull(converter.convertToEntityAttribute("   "));
    }

    @Test
    void convertToDatabaseColumn_andBack_shouldRoundTrip() {
        LastCommandState state = new LastCommandState(LastCommandType.SEARCH, Map.of("q", "abc"));
        String json = converter.convertToDatabaseColumn(state);
        assertNotNull(json);

        LastCommandState restored = converter.convertToEntityAttribute(json);
        assertNotNull(restored);
        assertEquals(state.command(), restored.command());
        assertEquals(state.args().get("q"), restored.args().get("q"));
    }

    @Test
    void convertToEntityAttribute_shouldReturnNull_whenInvalidJson() {
        assertNull(converter.convertToEntityAttribute("{not-json"));
    }

    @Test
    void convertToDatabaseColumn_shouldThrow_whenSerializationFails() {
        Object badValue = new Object() {
            @SuppressWarnings("unused")
            public Object getBroken() {
                throw new RuntimeException("boom");
            }
        };
        LastCommandState state = new LastCommandState(LastCommandType.SEARCH, Map.of("bad", badValue));

        assertThrows(IllegalStateException.class, () -> converter.convertToDatabaseColumn(state));
    }
}
