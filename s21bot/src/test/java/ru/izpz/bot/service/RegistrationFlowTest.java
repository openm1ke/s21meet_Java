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
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import ru.izpz.bot.exception.EduLoginCheckException;
import ru.izpz.bot.exception.RocketChatSendException;
import ru.izpz.bot.keyboard.TelegramButtons;
import ru.izpz.bot.keyboard.TelegramKeyboardFactory;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.dto.ProfileCodeResponse;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileStatus;
import ru.izpz.dto.RocketChatSendResponse;
import ru.izpz.dto.model.ErrorResponseDTO;
import ru.izpz.dto.model.ParticipantV1DTO;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationFlowTest {

    @Mock
    private BotProperties botProperties;

    @Mock
    private ProfileService profileService;

    @Mock
    private TelegramButtons telegramButtons;

    @Mock
    private MessageSender messageSender;

    @Mock
    private TelegramKeyboardFactory telegramKeyboardFactory;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private RegistrationFlow registrationFlow;

    private final Long chatId = 10L;

    @BeforeEach
    void setUp() {
        lenient().when(botProperties.admin()).thenReturn(999L);
    }

    @Test
    void startOnboarding_sendsRegistrationButton() {
        InlineKeyboardMarkup kb = mock(InlineKeyboardMarkup.class);
        when(telegramButtons.getRegistrationButton()).thenReturn(java.util.Map.of("Регистрация", "data"));
        when(telegramKeyboardFactory.createInlineKeyboardMarkup(anyMap(), eq(1))).thenReturn(kb);

        registrationFlow.startOnboarding(chatId);

        verify(messageSender).sendMessage(chatId, "Для регистрации нажмите кнопку ниже", kb);
    }

    @Test
    void startValidation_correctCode_setsConfirmedAndSendsMenuKeyboard() {
        ProfileDto profile = new ProfileDto(chatId.toString(), "login", ProfileStatus.VALIDATION, null);
        ProfileCodeResponse code = new ProfileCodeResponse("login", "123", OffsetDateTime.now());
        ReplyKeyboardMarkup menuKb = mock(ReplyKeyboardMarkup.class);

        when(profileService.getVerificationCode("login")).thenReturn(code);
        when(telegramKeyboardFactory.createReplyKeyboard(anyList(), eq(3))).thenReturn(menuKb);

        registrationFlow.startValidation(chatId, profile, "123");

        verify(profileService).updateProfileStatus(chatId, ProfileStatus.CONFIRMED);
        verify(messageSender).sendMessage(chatId, "Ваш аккаунт был успешно зарегистрирован", menuKb);
    }

    @Test
    void startValidation_wrongCode_sendsRemoveKeyboard() {
        ProfileDto profile = new ProfileDto(chatId.toString(), "login", ProfileStatus.VALIDATION, null);
        ProfileCodeResponse code = new ProfileCodeResponse("login", "123", OffsetDateTime.now());
        ReplyKeyboard removeKb = mock(ReplyKeyboard.class);

        when(profileService.getVerificationCode("login")).thenReturn(code);
        when(telegramKeyboardFactory.removeReplyKeyboard()).thenReturn(removeKb);

        registrationFlow.startValidation(chatId, profile, "000");

        verify(profileService, never()).updateProfileStatus(ArgumentMatchers.anyLong(), ArgumentMatchers.any());
        verify(messageSender).sendMessage(chatId, "Введенный код не совпадает!", removeKb);
    }

    @Test
    void startValidation_whenFeignException_recordsMetricAndNotifiesUserAndAdmin() {
        ProfileDto profile = new ProfileDto(chatId.toString(), "login", ProfileStatus.VALIDATION, null);
        FeignException ex = createFeignException(500, "boom");
        when(profileService.getVerificationCode("login")).thenThrow(ex);

        registrationFlow.startValidation(chatId, profile, "123");

        verify(metricsService).recordProcessingError("registration_validation", "feign_exception");
        verify(messageSender).sendMessage(chatId, "Ошибка валидации, попробуйте позже", null);
        verify(messageSender).sendMessage(999L, ex.contentUTF8(), null);
    }

    @Test
    void startRegistration_invalidLogin_sendsError() {
        registrationFlow.startRegistration(chatId, "12");

        verify(messageSender).sendMessage(chatId, "Введенный логин не соответствует требованиям", null);
        verifyNoInteractions(profileService);
    }

    @Test
    void startRegistration_checkEduLoginThrowsEduLoginCheckException_sendsUserAndAdminMessages() {
        ErrorResponseDTO error = new ErrorResponseDTO().status(400).message("bad");
        when(profileService.checkEduLogin("abc")).thenThrow(new EduLoginCheckException(error));

        registrationFlow.startRegistration(chatId, "abc");

        verify(metricsService).recordProcessingError("registration_flow", "edu_login_check_exception");
        verify(messageSender).sendMessage(chatId, "Ошибка проверки логина: bad", null);
        verify(messageSender).sendMessage(999L, "Ошибка проверки логина: " + error, null);
    }

    @Test
    void startRegistration_inactiveParticipant_sendsError() {
        ParticipantV1DTO participant = new ParticipantV1DTO();
        participant.setStatus(ParticipantV1DTO.StatusEnum.BLOCKED);
        when(profileService.checkEduLogin("abc")).thenReturn(participant);

        registrationFlow.startRegistration(chatId, "abc");

        verify(messageSender).sendMessage(chatId, "Введенный логин не активен", null);
        verify(profileService, never()).checkAndSetLogin(anyLong(), anyString());
    }

    @Test
    void startRegistration_parallelNameNull_sendsError() {
        ParticipantV1DTO participant = new ParticipantV1DTO();
        participant.setStatus(ParticipantV1DTO.StatusEnum.ACTIVE);
        participant.setParallelName(null);
        when(profileService.checkEduLogin("abc")).thenReturn(participant);

        registrationFlow.startRegistration(chatId, "abc");

        verify(messageSender).sendMessage(chatId, "Введенный логин не на основе! Приходите когда пройдете бассейн", null);
        verify(profileService, never()).checkAndSetLogin(anyLong(), anyString());
    }

    @Test
    void startRegistration_nonCoreProgram_sendsError() {
        ParticipantV1DTO participant = new ParticipantV1DTO();
        participant.setStatus(ParticipantV1DTO.StatusEnum.ACTIVE);
        participant.setParallelName("Piscine");
        when(profileService.checkEduLogin("abc")).thenReturn(participant);

        registrationFlow.startRegistration(chatId, "abc");

        verify(messageSender).sendMessage(chatId, "Введенный логин не на основе! Приходите когда пройдете бассейн", null);
        verify(profileService, never()).checkAndSetLogin(anyLong(), anyString());
    }

    @Test
    void startRegistration_checkAndSetLoginFeignException_sendsUserAndAdmin() {
        ParticipantV1DTO participant = new ParticipantV1DTO();
        participant.setStatus(ParticipantV1DTO.StatusEnum.ACTIVE);
        participant.setParallelName("Core program");
        when(profileService.checkEduLogin("abc")).thenReturn(participant);

        FeignException ex = createFeignException(500, "err");
        when(profileService.checkAndSetLogin(chatId, "abc")).thenThrow(ex);

        registrationFlow.startRegistration(chatId, "abc");

        verify(metricsService).recordProcessingError("registration_flow", "feign_exception");
        verify(messageSender).sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже", null);
        verify(messageSender).sendMessage(999L, ex.contentUTF8(), null);
    }

    @Test
    void startRegistration_sendVerificationCodeSuccess_updatesStatusValidationAndNotifiesAdmin() {
        ParticipantV1DTO participant = new ParticipantV1DTO();
        participant.setStatus(ParticipantV1DTO.StatusEnum.ACTIVE);
        participant.setParallelName("Core program");
        when(profileService.checkEduLogin("abc")).thenReturn(participant);

        ProfileDto updated = new ProfileDto(chatId.toString(), "abc", ProfileStatus.REGISTRATION, null);
        when(profileService.checkAndSetLogin(chatId, "abc")).thenReturn(updated);

        RocketChatSendResponse rc = new RocketChatSendResponse(true, "ok");
        when(profileService.sendVerificationCode("abc")).thenReturn(rc);

        registrationFlow.startRegistration(chatId, "abc");

        verify(messageSender).sendMessage(chatId, "В рокет чат был отправлен код для подтверждения для логина abc", null);
        verify(messageSender).sendMessage(999L, "В рокет чат было отправлено сообщение ok", null);
        verify(profileService).updateProfileStatus(chatId, ProfileStatus.VALIDATION);
    }

    @Test
    void startRegistration_whenCheckAndSetLoginReturnsDifferentLogin_doesNotStartValidation() {
        ParticipantV1DTO participant = new ParticipantV1DTO();
        participant.setStatus(ParticipantV1DTO.StatusEnum.ACTIVE);
        participant.setParallelName("Core program");
        when(profileService.checkEduLogin("abc")).thenReturn(participant);

        ProfileDto updated = new ProfileDto(chatId.toString(), "zzz", ProfileStatus.REGISTRATION, null);
        when(profileService.checkAndSetLogin(chatId, "abc")).thenReturn(updated);

        registrationFlow.startRegistration(chatId, "abc");

        verify(profileService, never()).sendVerificationCode(ArgumentMatchers.anyString());
        verify(profileService, never()).updateProfileStatus(ArgumentMatchers.anyLong(), ArgumentMatchers.any());
        verify(messageSender, never()).sendMessage(eq(999L), ArgumentMatchers.anyString(), ArgumentMatchers.any());
    }

    @Test
    void startRegistration_sendVerificationCodeThrowsRocketChatSendException_notifiesUserAndAdmin() {
        ParticipantV1DTO participant = new ParticipantV1DTO();
        participant.setStatus(ParticipantV1DTO.StatusEnum.ACTIVE);
        participant.setParallelName("Core program");
        when(profileService.checkEduLogin("abc")).thenReturn(participant);

        ProfileDto updated = new ProfileDto(chatId.toString(), "abc", ProfileStatus.REGISTRATION, null);
        when(profileService.checkAndSetLogin(chatId, "abc")).thenReturn(updated);

        when(profileService.sendVerificationCode("abc")).thenThrow(new RocketChatSendException(new RocketChatSendResponse(false, "fail")));

        registrationFlow.startRegistration(chatId, "abc");

        verify(metricsService).recordProcessingError("registration_flow", "rocketchat_send_exception");
        verify(messageSender).sendMessage(chatId, "Ошибка отправки сообщения в рокетчат, попробуйте позже", null);
        verify(messageSender).sendMessage(eq(999L), contains("fail"), eq(null));
        verify(profileService, never()).updateProfileStatus(ArgumentMatchers.anyLong(), ArgumentMatchers.any());
    }

    @Test
    void startRegistration_sendVerificationCodeThrowsFeignException_notifiesUserAndAdmin() {
        ParticipantV1DTO participant = new ParticipantV1DTO();
        participant.setStatus(ParticipantV1DTO.StatusEnum.ACTIVE);
        participant.setParallelName("Core program");
        when(profileService.checkEduLogin("abc")).thenReturn(participant);

        ProfileDto updated = new ProfileDto(chatId.toString(), "abc", ProfileStatus.REGISTRATION, null);
        when(profileService.checkAndSetLogin(chatId, "abc")).thenReturn(updated);

        FeignException ex = createFeignException(500, "boom");
        when(profileService.sendVerificationCode("abc")).thenThrow(ex);

        registrationFlow.startRegistration(chatId, "abc");

        verify(metricsService).recordProcessingError("registration_flow", "feign_exception");
        verify(messageSender).sendMessage(chatId, "Ошибка отправки сообщения в рокетчат, сообщите админу", null);
        verify(messageSender).sendMessage(999L, ex.contentUTF8(), null);
        verify(profileService, never()).updateProfileStatus(ArgumentMatchers.anyLong(), ArgumentMatchers.any());
    }

    @Test
    void startRegistration_nullLoginInput_sendsInvalidLoginError() {
        registrationFlow.startRegistration(chatId, null);

        verify(messageSender).sendMessage(chatId, "Введенный логин не соответствует требованиям", null);
        verifyNoInteractions(profileService);
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
