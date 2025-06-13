package ru.izpz.bot.keyboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.izpz.bot.dto.CallbackPayload;

public class CallbackPayloadSerializer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String serialize(CallbackPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize callback payload", e);
        }
    }
}
