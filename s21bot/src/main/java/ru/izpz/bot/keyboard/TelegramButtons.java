package ru.izpz.bot.keyboard;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.izpz.bot.dto.CallbackPayload;

import java.util.Map;

@Component
@Getter
public class TelegramButtons {

    private final String groupInviteLink;

    public static final String REGISTRATION_NAME = "Регистрация";
    public static final String REGISTRATION_CODE = "registration";
    public static final String SUBSCRIBE_NAME = "Подписаться на канал";

    public TelegramButtons(@Value("${bot.group-invite-link}") String groupInviteLink) {
        this.groupInviteLink = groupInviteLink;
    }

    public Map<String, String> getRegistrationButton() {
        return Map.of(
                REGISTRATION_NAME,
                CallbackPayloadSerializer.serialize(new CallbackPayload(REGISTRATION_CODE))
        );
    }

    public Map<String, String> getSubscribeButton() {
        return Map.of(
                SUBSCRIBE_NAME,
                groupInviteLink
        );
    }
}
