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
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileStatus;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    @Mock
    private BotProperties botProperties;

    @Mock
    private ProfileService profileService;

    @Mock
    private MessageSender messageSender;

    @Mock
    private CallbackHandler callbackHandler;

    @Mock
    private RegistrationFlow registrationFlow;

    @Mock
    private ConfirmedFlow confirmedFlow;

    @InjectMocks
    private MessageProcessor messageProcessor;

    @BeforeEach
    void setUp() {
        lenient().when(botProperties.admin()).thenReturn(999L);
    }

    @Test
    void handleCallbackMessage_delegatesToCallbackHandler() {
        messageProcessor.handleCallbackMessage(1L, "d", 2, "cb");
        verify(callbackHandler).handleCallbackMessage(1L, "d", 2, "cb");
    }

    @Test
    void parseMessage_confirmed_delegatesToConfirmedFlow() {
        ProfileDto profile = new ProfileDto("1", "abc", ProfileStatus.CONFIRMED, null);
        messageProcessor.parseMessage(1L, profile, "t");
        verify(confirmedFlow).startConfirmed(1L, profile, "t");
    }

    @Test
    void handleTextMessage_whenProfileServiceThrowsFeignException_sendsUserAndAdmin() {
        Message msg = mock(Message.class);
        when(msg.getChatId()).thenReturn(1L);
        when(msg.getText()).thenReturn("hi");

        FeignException ex = createFeignException(500, "boom");
        when(profileService.getProfile(1L)).thenThrow(ex);

        messageProcessor.handleTextMessage(msg);

        verify(messageSender).sendMessage(1L, "Ошибка обработки профиля, попробуйте позже", null);
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
