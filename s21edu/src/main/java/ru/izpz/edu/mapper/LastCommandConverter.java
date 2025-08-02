package ru.izpz.edu.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.izpz.dto.LastCommandState;

public class LastCommandConverter {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String serialize(LastCommandState state) {
        try {
            return state == null ? null : mapper.writeValueAsString(state);
        } catch (Exception e) {
            throw new RuntimeException("Serialization error", e);
        }
    }

    public static LastCommandState deserialize(String json) {
        try {
            return (json == null || json.isBlank())
                    ? null
                    : mapper.readValue(json, LastCommandState.class);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization error", e);
        }
    }
}