package ru.izpz.bot.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.dto.ProfileDto;


@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {

    private final BotProperties botProperties;

    private final ProfileService profileService;
    private final MessageSender messageSender;
    private final CallbackHandler callbackHandler;
    private final RegistrationFlow registrationFlow;
    private final ConfirmedFlow confirmedFlow;
    private final MetricsService metricsService;

    public void handleTextMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText().trim();
        try {
            ProfileDto profile = profileService.getProfile(chatId);
            log.info("Profile: {}", profile.toString());
            parseMessage(chatId, profile, text);
        } catch (FeignException e) {
            metricsService.recordProcessingError("message_processor", "feign_exception");
            messageSender.sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже", null);
            messageSender.sendMessage(botProperties.admin(), e.contentUTF8(), null);
        } catch (Exception e) {
            metricsService.recordProcessingError("message_processor", "unexpected_exception");
            log.error("Unexpected error while handling text message", e);
            messageSender.sendMessage(chatId, "Произошла внутренняя ошибка, попробуйте позже", null);
        }
    }

    public void parseMessage(Long chatId, ProfileDto profile, String text) {
        switch(profile.status()) {
            case CREATED -> registrationFlow.startOnboarding(chatId);
            case REGISTRATION -> registrationFlow.startRegistration(chatId, text);
            case VALIDATION -> registrationFlow.startValidation(chatId, profile, text);
            case CONFIRMED -> confirmedFlow.startConfirmed(chatId, profile, text);
        }
    }

    public void handleCallbackMessage(Long chatId, String data, Integer messageId, String callbackId) {
        try {
            callbackHandler.handleCallbackMessage(chatId, data, messageId, callbackId);
        } catch (Exception e) {
            metricsService.recordProcessingError("callback_dispatch", "unexpected_exception");
            log.error("Unexpected error while handling callback message", e);
            messageSender.sendMessage(chatId, "Произошла внутренняя ошибка, попробуйте позже", null);
        }
    }

}
