package ru.izpz.bot.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.bot.exception.InvalidCallbackPayloadException;
import ru.izpz.bot.keyboard.CallbackPayloadSerializer;
import ru.izpz.bot.keyboard.TelegramKeyboardFactory;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.dto.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallbackHandlerTest {

    @Mock
    private BotProperties botProperties;

    @Mock
    private ProfileService profileService;

    @Mock
    private TelegramKeyboardFactory telegramKeyboardFactory;

    @Mock
    private CallbackPayloadSerializer callbackPayloadSerializer;

    @Mock
    private MessageSender messageSender;

    @InjectMocks
    private CallbackHandler callbackHandler;

    private final Long CHAT_ID = 10L;

    @BeforeEach
    void setUp() {
        lenient().when(botProperties.admin()).thenReturn(999L);
    }

    @Test
    void handleCallbackMessage_invalidPayload_sendsErrorMessage() {
        when(callbackPayloadSerializer.deserialize("bad")).thenThrow(new InvalidCallbackPayloadException("x", null));

        callbackHandler.handleCallbackMessage(CHAT_ID, "bad", 1, "cb");

        verify(messageSender).sendMessage(CHAT_ID, "Некорректный формат данных. Попробуйте еще раз.", null);
    }

    @Test
    void handleCallbackMessage_friendsPage_callsShowFriends() {
        when(callbackPayloadSerializer.deserialize("data")).thenReturn(new CallbackPayload("friends_page", Map.of("page", "2")));

        FriendsSliceDto slice = new FriendsSliceDto(java.util.Collections.emptyList(), 2, 2, false);
        when(profileService.getFriends(CHAT_ID, 2, 2)).thenReturn(slice);
        when(telegramKeyboardFactory.friendsListKeyboard(eq(slice), anyInt(), eq(2))).thenReturn(mock(InlineKeyboardMarkup.class));
        when(telegramKeyboardFactory.friendsListText(eq(slice))).thenReturn("text");

        callbackHandler.handleCallbackMessage(CHAT_ID, "data", 5, "cb");

        verify(messageSender).updateMessage(eq(CHAT_ID), eq(5), eq("text"), any());
    }

    @Test
    void showEvents_feignException_sendsUserAndAdmin() {
        FeignException ex = createFeignException(500, "boom");
        when(profileService.getEvents(CHAT_ID, 0, 2)).thenThrow(ex);

        callbackHandler.showEvents(CHAT_ID, 0, null);

        verify(messageSender).sendMessage(CHAT_ID, "Ошибка обработки событий, попробуйте позже", null);
        verify(messageSender).sendMessage(999L, ex.contentUTF8(), null);
    }

    private FeignException createFeignException(int status, String message) {
        Request request = Request.create(Request.HttpMethod.GET, "/test",
                java.util.Collections.emptyMap(), null, new RequestTemplate());
        Response response = Response.builder()
                .status(status)
                .request(request)
                .body(message, StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("Test", response);
    }
}
