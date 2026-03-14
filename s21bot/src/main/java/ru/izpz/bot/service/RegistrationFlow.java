package ru.izpz.bot.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.izpz.bot.exception.EduLoginCheckException;
import ru.izpz.bot.exception.RocketChatSendException;
import ru.izpz.bot.keyboard.MenuCommandEnum;
import ru.izpz.bot.keyboard.TelegramButtons;
import ru.izpz.bot.keyboard.TelegramKeyboardFactory;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileStatus;
import ru.izpz.dto.RocketChatSendResponse;
import ru.izpz.dto.model.ErrorResponseDTO;
import ru.izpz.dto.model.ParticipantV1DTO;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationFlow {

    private static final String STAGE_REGISTRATION_FLOW = "registration_flow";
    private static final String STAGE_REGISTRATION_VALIDATION = "registration_validation";
    private static final String REASON_FEIGN_EXCEPTION = "feign_exception";

    private final BotProperties botProperties;

    private final ProfileService profileService;
    private final TelegramButtons telegramButtons;
    private final MessageSender messageSender;
    private final TelegramKeyboardFactory telegramKeyboardFactory;
    private final MetricsService metricsService;

    public void startOnboarding(Long chatId) {
        InlineKeyboardMarkup keyboard = telegramKeyboardFactory.createInlineKeyboardMarkup(telegramButtons.getRegistrationButton(), 1);
        messageSender.sendMessage(chatId, "Для регистрации нажмите кнопку ниже", keyboard);
    }

    public void startValidation(Long chatId, ProfileDto profile, String text) {
        try {
            var code = profileService.getVerificationCode(profile.s21login());
            if (code.getSecretCode().equals(text)) {
                profileService.updateProfileStatus(chatId, ProfileStatus.CONFIRMED);
                messageSender.sendMessage(chatId, "Ваш аккаунт был успешно зарегистрирован", telegramKeyboardFactory.createReplyKeyboard(MenuCommandEnum.getAllMenuCommands(), 3));
            } else {
                messageSender.sendMessage(chatId, "Введенный код не совпадает!", telegramKeyboardFactory.removeReplyKeyboard());
            }
        } catch (FeignException e) {
            metricsService.recordProcessingError(STAGE_REGISTRATION_VALIDATION, REASON_FEIGN_EXCEPTION);
            messageSender.sendMessage(chatId, "Ошибка валидации, попробуйте позже", null);
            messageSender.sendMessage(botProperties.admin(), e.contentUTF8(), null);
        } catch (Exception e) {
            metricsService.recordProcessingError(STAGE_REGISTRATION_VALIDATION, "unexpected_exception");
            log.error("Unexpected validation error for chatId={}", chatId, e);
            messageSender.sendMessage(chatId, "Ошибка валидации, попробуйте позже", null);
        }
    }

    public void startRegistration(Long chatId, String text) {
        if (!isValidLogin(text)) {
            messageSender.sendMessage(chatId, "Введенный логин не соответствует требованиям", null);
            return;
        }

        ParticipantV1DTO participant;
        try {
            participant = profileService.checkEduLogin(text);
        } catch (EduLoginCheckException e) {
            metricsService.recordProcessingError(STAGE_REGISTRATION_FLOW, "edu_login_check_exception");
            ErrorResponseDTO error = e.getError();
            messageSender.sendMessage(chatId, "Ошибка проверки логина: " + error.getMessage(), null);
            messageSender.sendMessage(botProperties.admin(), "Ошибка проверки логина: " + error, null);
            return;
        }

        // проверяем что профиль активный
        if (participant.getStatus() != ParticipantV1DTO.StatusEnum.ACTIVE) {
            messageSender.sendMessage(chatId, "Введенный логин не активен", null);
            return;
        }

        // тут можно проверить что профиль на основе Core program
        if (participant.getParallelName() == null || !participant.getParallelName().equals("Core program")) {
            messageSender.sendMessage(chatId, "Введенный логин не на основе! Приходите когда пройдете бассейн", null);
            return;
        }

        ProfileDto profileDto;
        try {
            profileDto = profileService.checkAndSetLogin(chatId, text);
        } catch (FeignException e) {
            metricsService.recordProcessingError(STAGE_REGISTRATION_FLOW, REASON_FEIGN_EXCEPTION);
            messageSender.sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже", null);
            messageSender.sendMessage(botProperties.admin(), e.contentUTF8(), null);
            return;
        }
        // если логин совпадает значит мы его сохранили
        if (profileDto.s21login().equals(text)) {
            RocketChatSendResponse rocketChatResponse;
            try {
                rocketChatResponse = profileService.sendVerificationCode(text);
            } catch (RocketChatSendException e) {
                metricsService.recordProcessingError(STAGE_REGISTRATION_FLOW, "rocketchat_send_exception");
                messageSender.sendMessage(chatId, "Ошибка отправки сообщения в рокетчат, попробуйте позже", null);
                messageSender.sendMessage(botProperties.admin(), e.getMessage(), null);
                return;
            } catch (FeignException e) {
                metricsService.recordProcessingError(STAGE_REGISTRATION_FLOW, REASON_FEIGN_EXCEPTION);
                messageSender.sendMessage(chatId, "Ошибка отправки сообщения в рокетчат, сообщите админу", null);
                messageSender.sendMessage(botProperties.admin(), e.contentUTF8(), null);
                return;
            }

            messageSender.sendMessage(chatId, "В рокет чат был отправлен код для подтверждения для логина " + text, null);
            messageSender.sendMessage(botProperties.admin(), "В рокет чат было отправлено сообщение " + rocketChatResponse.getMessage(), null);
            profileService.updateProfileStatus(chatId, ProfileStatus.VALIDATION);
        }

        // тут мы получает в тексте логин на платформе
        // надо отправить его на бэкенд и получить дто с какими-то полями
        // первое что мы проверяем что профиль существует или нет
        //   - если нет то пишем что вы ошиблись попробуйте еще раз
        // второе если существует
        //   - если он уже зарегистрирован то говорим что этот профиль уже привязан к другому телеграму
        //   - если он не зарегистрирован то смотрим на его статус
        //          - статус заблокирован - не можем зарегать такой профиль (можно поменять статус на блокированный)
        //          - статус заморожен - тоже выставляем блок
        //          - статус не на основе пишем сообщение что не можем зарегать пока не будет на основе (тут другой статус)
        // если все хорошо и профиль активный и на основе то привязываем его к этому телеграм айди и пытаемся начать валидацию


    }

    private boolean isValidLogin(String login) {
        return login != null && login.matches("^[a-zA-Z]{3,30}$");
    }
}
