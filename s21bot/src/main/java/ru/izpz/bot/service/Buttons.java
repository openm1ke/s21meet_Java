package ru.izpz.bot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.izpz.bot.dto.CallbackPayload;

import java.util.Map;

public class Buttons {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String registration;

    static {
        try {
            registration = objectMapper.writeValueAsString(new CallbackPayload("registration"));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Map<String, String> registration_button = Map.of("Регистрация", registration);
}
