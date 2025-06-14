package ru.izpz.bot.keyboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.bot.exception.InvalidCallbackPayloadException;

@Slf4j
public class CallbackPayloadSerializer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String serialize(CallbackPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new InvalidCallbackPayloadException("Failed to serialize callback payload", e);
        }
    }

    public static CallbackPayload deserialize(String data) {
        try {
            return objectMapper.readValue(data, CallbackPayload.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing callback data: {}", data, e);
            throw new InvalidCallbackPayloadException("Invalid callback payload data", e);
        }
    }
}

