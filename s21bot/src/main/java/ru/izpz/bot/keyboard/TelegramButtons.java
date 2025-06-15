package ru.izpz.bot.keyboard;

import ru.izpz.bot.dto.CallbackPayload;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TelegramButtons {

    public static final String REGISTRATION_NAME = "Регистрация";
    public static final String REGISTRATION_CODE = "registration";

    public static final Map<String, String> registration_button = Map.of(
            REGISTRATION_NAME, CallbackPayloadSerializer.serialize(new CallbackPayload(REGISTRATION_CODE))
    );

}
