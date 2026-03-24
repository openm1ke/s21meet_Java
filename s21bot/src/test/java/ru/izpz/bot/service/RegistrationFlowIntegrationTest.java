package ru.izpz.bot.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.izpz.bot.client.ProfileClient;
import ru.izpz.bot.client.RocketChatClient;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.bot.keyboard.CallbackPayloadSerializer;
import ru.izpz.bot.keyboard.TelegramButtons;
import ru.izpz.dto.ParticipantDto;
import ru.izpz.dto.ParticipantStatusEnum;
import ru.izpz.dto.ProfileCodeResponse;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileRequest;
import ru.izpz.dto.ProfileStatus;
import ru.izpz.dto.RocketChatSendResponse;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class RegistrationFlowIntegrationTest {

    @Autowired
    private MessageProcessor messageProcessor;

    @Autowired
    private CallbackPayloadSerializer callbackPayloadSerializer;

    @MockitoBean
    private ProfileClient profileClient;

    @MockitoBean
    private RocketChatClient rocketChatClient;

    @MockitoBean
    private MessageSender messageSender;

    @Test
    void registration_happyPath_shouldReachConfirmedStatus() {
        Long chatId = 42L;
        String telegramId = chatId.toString();
        String login = "perseste";
        String code = "1234";

        ProfileDto created = new ProfileDto(telegramId, null, ProfileStatus.CREATED, null);
        ProfileDto registration = new ProfileDto(telegramId, null, ProfileStatus.REGISTRATION, null);
        ProfileDto validation = new ProfileDto(telegramId, login, ProfileStatus.VALIDATION, null);

        when(profileClient.getOrCreateProfile(telegramId))
                .thenReturn(created, registration, validation);
        when(profileClient.updateProfileStatus(any(ProfileRequest.class)))
                .thenAnswer(invocation -> {
                    ProfileRequest request = invocation.getArgument(0);
                    return new ProfileDto(request.getTelegramId(), login, request.getStatus(), null);
                });

        ParticipantDto participant = new ParticipantDto();
        participant.setLogin(login);
        participant.setStatus(ParticipantStatusEnum.ACTIVE);
        participant.setParallelName("Core program");
        when(profileClient.checkEduLogin(login)).thenReturn(participant);
        when(profileClient.checkAndSetLogin(any(ProfileRequest.class)))
                .thenReturn(new ProfileDto(telegramId, login, ProfileStatus.REGISTRATION, null));
        when(profileClient.getProfileCode(any()))
                .thenReturn(new ProfileCodeResponse(login, code, OffsetDateTime.now()))
                .thenReturn(new ProfileCodeResponse(login, code, OffsetDateTime.now()));
        when(rocketChatClient.sendMessage(any()))
                .thenReturn(new RocketChatSendResponse(true, "ok"));

        messageProcessor.handleTextMessage(textMessage(chatId, "/start"));
        messageProcessor.handleCallbackMessage(
                chatId,
                callbackPayloadSerializer.serialize(new CallbackPayload(TelegramButtons.REGISTRATION_CODE, null)),
                100,
                "cb-1"
        );
        messageProcessor.handleTextMessage(textMessage(chatId, login));
        messageProcessor.handleTextMessage(textMessage(chatId, code));

        List<ProfileStatus> statuses = capturedStatuses();
        assertEquals(List.of(ProfileStatus.REGISTRATION, ProfileStatus.VALIDATION, ProfileStatus.CONFIRMED), statuses);

        verify(messageSender, atLeastOnce()).sendMessage(eq(chatId),
                eq("В рокет чат был отправлен код для подтверждения для логина " + login), any());
        verify(messageSender, atLeastOnce()).sendMessage(eq(chatId),
                eq("Ваш аккаунт был успешно зарегистрирован"), any());
    }

    @Test
    void registration_whenRocketChatFails_shouldStayRegistrationAndRetrySameLogin() {
        Long chatId = 43L;
        String telegramId = chatId.toString();
        String login = "perseste";
        String code = "9999";

        ProfileDto created = new ProfileDto(telegramId, null, ProfileStatus.CREATED, null);
        ProfileDto registration1 = new ProfileDto(telegramId, null, ProfileStatus.REGISTRATION, null);
        ProfileDto registration2 = new ProfileDto(telegramId, login, ProfileStatus.REGISTRATION, null);

        when(profileClient.getOrCreateProfile(telegramId))
                .thenReturn(created, registration1, registration2);
        when(profileClient.updateProfileStatus(any(ProfileRequest.class)))
                .thenAnswer(invocation -> {
                    ProfileRequest request = invocation.getArgument(0);
                    return new ProfileDto(request.getTelegramId(), login, request.getStatus(), null);
                });

        ParticipantDto participant = new ParticipantDto();
        participant.setLogin(login);
        participant.setStatus(ParticipantStatusEnum.ACTIVE);
        participant.setParallelName("Core program");
        when(profileClient.checkEduLogin(login)).thenReturn(participant);
        when(profileClient.checkAndSetLogin(any(ProfileRequest.class)))
                .thenReturn(new ProfileDto(telegramId, login, ProfileStatus.REGISTRATION, null));
        when(profileClient.getProfileCode(any()))
                .thenReturn(new ProfileCodeResponse(login, code, OffsetDateTime.now()))
                .thenReturn(new ProfileCodeResponse(login, code, OffsetDateTime.now()));
        when(rocketChatClient.sendMessage(any()))
                .thenThrow(createFeignException(502, "rocket down"))
                .thenReturn(new RocketChatSendResponse(true, "ok"));

        messageProcessor.handleTextMessage(textMessage(chatId, "/start"));
        messageProcessor.handleCallbackMessage(
                chatId,
                callbackPayloadSerializer.serialize(new CallbackPayload(TelegramButtons.REGISTRATION_CODE, null)),
                101,
                "cb-2"
        );
        messageProcessor.handleTextMessage(textMessage(chatId, login));
        messageProcessor.handleTextMessage(textMessage(chatId, login));

        List<ProfileStatus> statuses = capturedStatuses();
        assertEquals(List.of(ProfileStatus.REGISTRATION, ProfileStatus.VALIDATION), statuses);

        verify(profileClient, times(2)).checkAndSetLogin(any(ProfileRequest.class));
        verify(messageSender, atLeastOnce()).sendMessage(eq(chatId),
                eq("Ошибка отправки сообщения в рокетчат, сообщите админу"), any());
        verify(messageSender, atLeastOnce()).sendMessage(eq(chatId),
                eq("В рокет чат был отправлен код для подтверждения для логина " + login), any());
    }

    @Test
    void registration_whenEduLoginCheckFails_shouldNotifyAndStopFlow() {
        Long chatId = 44L;
        String telegramId = chatId.toString();
        String login = "perseste";

        when(profileClient.getOrCreateProfile(telegramId))
                .thenReturn(new ProfileDto(telegramId, null, ProfileStatus.REGISTRATION, null));
        when(profileClient.checkEduLogin(login))
                .thenThrow(createFeignException(400, "{\"status\":400,\"message\":\"login not found\"}"));

        messageProcessor.handleTextMessage(textMessage(chatId, login));

        verify(messageSender, atLeastOnce()).sendMessage(eq(chatId),
                eq("Ошибка проверки логина: login not found"), any());
        verify(profileClient, never()).checkAndSetLogin(any(ProfileRequest.class));
        verify(profileClient, never()).updateProfileStatus(any(ProfileRequest.class));
    }

    @Test
    void registration_whenParticipantNotActive_shouldRejectLogin() {
        Long chatId = 45L;
        String telegramId = chatId.toString();
        String login = "perseste";

        when(profileClient.getOrCreateProfile(telegramId))
                .thenReturn(new ProfileDto(telegramId, null, ProfileStatus.REGISTRATION, null));
        ParticipantDto blocked = new ParticipantDto();
        blocked.setLogin(login);
        blocked.setStatus(ParticipantStatusEnum.BLOCKED);
        blocked.setParallelName("Core program");
        when(profileClient.checkEduLogin(login)).thenReturn(blocked);

        messageProcessor.handleTextMessage(textMessage(chatId, login));

        verify(messageSender, atLeastOnce()).sendMessage(eq(chatId), eq("Введенный логин не активен"), any());
        verify(profileClient, never()).checkAndSetLogin(any(ProfileRequest.class));
    }

    @Test
    void registration_whenParticipantNotCore_shouldRejectLogin() {
        Long chatId = 46L;
        String telegramId = chatId.toString();
        String login = "perseste";

        when(profileClient.getOrCreateProfile(telegramId))
                .thenReturn(new ProfileDto(telegramId, null, ProfileStatus.REGISTRATION, null));
        ParticipantDto nonCore = new ParticipantDto();
        nonCore.setLogin(login);
        nonCore.setStatus(ParticipantStatusEnum.ACTIVE);
        nonCore.setParallelName("Piscine");
        when(profileClient.checkEduLogin(login)).thenReturn(nonCore);

        messageProcessor.handleTextMessage(textMessage(chatId, login));

        verify(messageSender, atLeastOnce()).sendMessage(eq(chatId),
                eq("Введенный логин не на основе! Приходите когда пройдете бассейн"), any());
        verify(profileClient, never()).checkAndSetLogin(any(ProfileRequest.class));
    }

    @Test
    void validation_whenCodeMismatch_shouldStayValidationAndNotifyUser() {
        Long chatId = 47L;
        String telegramId = chatId.toString();
        String login = "perseste";

        when(profileClient.getOrCreateProfile(telegramId))
                .thenReturn(new ProfileDto(telegramId, login, ProfileStatus.VALIDATION, null));
        when(profileClient.getProfileCode(any()))
                .thenReturn(new ProfileCodeResponse(login, "1111", OffsetDateTime.now()));

        messageProcessor.handleTextMessage(textMessage(chatId, "2222"));

        verify(messageSender, atLeastOnce()).sendMessage(eq(chatId), eq("Введенный код не совпадает!"), any());
        verify(profileClient, never()).updateProfileStatus(any(ProfileRequest.class));
    }

    @Test
    void validation_whenGetCodeFails_shouldNotifyValidationError() {
        Long chatId = 48L;
        String telegramId = chatId.toString();
        String login = "perseste";

        when(profileClient.getOrCreateProfile(telegramId))
                .thenReturn(new ProfileDto(telegramId, login, ProfileStatus.VALIDATION, null));
        when(profileClient.getProfileCode(any()))
                .thenThrow(createFeignException(502, "edu down"));

        messageProcessor.handleTextMessage(textMessage(chatId, "1234"));

        verify(messageSender, atLeastOnce()).sendMessage(eq(chatId),
                eq("Ошибка валидации, попробуйте позже"), any());
        verify(profileClient, never()).updateProfileStatus(any(ProfileRequest.class));
    }

    @Test
    void registration_whenStatusUpdateToValidationFails_shouldSendGenericProcessingError() {
        Long chatId = 49L;
        String telegramId = chatId.toString();
        String login = "perseste";
        String code = "1234";

        when(profileClient.getOrCreateProfile(telegramId))
                .thenReturn(new ProfileDto(telegramId, null, ProfileStatus.REGISTRATION, null));
        ParticipantDto participant = new ParticipantDto();
        participant.setLogin(login);
        participant.setStatus(ParticipantStatusEnum.ACTIVE);
        participant.setParallelName("Core program");
        when(profileClient.checkEduLogin(login)).thenReturn(participant);
        when(profileClient.checkAndSetLogin(any(ProfileRequest.class)))
                .thenReturn(new ProfileDto(telegramId, login, ProfileStatus.REGISTRATION, null));
        when(profileClient.getProfileCode(any()))
                .thenReturn(new ProfileCodeResponse(login, code, OffsetDateTime.now()));
        when(rocketChatClient.sendMessage(any()))
                .thenReturn(new RocketChatSendResponse(true, "ok"));
        when(profileClient.updateProfileStatus(any(ProfileRequest.class)))
                .thenThrow(createFeignException(500, "status update failed"));

        messageProcessor.handleTextMessage(textMessage(chatId, login));

        verify(messageSender, atLeastOnce()).sendMessage(eq(chatId),
                eq("В рокет чат был отправлен код для подтверждения для логина " + login), any());
        verify(messageSender, atLeastOnce()).sendMessage(eq(chatId),
                eq("Ошибка обработки профиля, попробуйте позже"), any());
    }

    private List<ProfileStatus> capturedStatuses() {
        List<ProfileStatus> statuses = new ArrayList<>();
        verify(profileClient, atLeastOnce()).updateProfileStatus(any(ProfileRequest.class));
        org.mockito.Mockito.mockingDetails(profileClient).getInvocations().stream()
                .filter(invocation -> "updateProfileStatus".equals(invocation.getMethod().getName()))
                .forEach(invocation -> {
                    ProfileRequest request = (ProfileRequest) invocation.getArgument(0);
                    statuses.add(request.getStatus());
                });
        return statuses;
    }

    private Message textMessage(Long chatId, String text) {
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getText()).thenReturn(text);
        return message;
    }

    private FeignException createFeignException(int status, String message) {
        Request request = Request.create(Request.HttpMethod.POST, "/api/rocketchat/send",
                java.util.Collections.emptyMap(), null, new RequestTemplate());
        Response response = Response.builder()
                .status(status)
                .request(request)
                .body(message, StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("RocketChat", response);
    }
}
