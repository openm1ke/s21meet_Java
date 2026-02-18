package ru.izpz.bot.keyboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.bot.property.BotProperties;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramButtonsTest {

    @Mock
    private BotProperties botProperties;

    @Mock
    private CallbackPayloadSerializer serializer;

    @InjectMocks
    private TelegramButtons telegramButtons;

    @Test
    void getRegistrationButton_usesSerializer() {
        when(serializer.serialize(any(CallbackPayload.class))).thenReturn("data");

        Map<String, String> btn = telegramButtons.getRegistrationButton();

        assertEquals("data", btn.get(TelegramButtons.REGISTRATION_NAME));

        verify(serializer).serialize(argThat(p -> TelegramButtons.REGISTRATION_CODE.equals(p.getCommand()) && p.getArgs() == null));
    }

    @Test
    void getSubscribeButton_usesInviteLinkFromProperties() {
        when(botProperties.groupInviteLink()).thenReturn("https://t.me/join");

        Map<String, String> btn = telegramButtons.getSubscribeButton();

        assertEquals("https://t.me/join", btn.get(TelegramButtons.SUBSCRIBE_NAME));
    }
}
