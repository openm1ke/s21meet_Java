package ru.izpz.bot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.bot.exception.EduLoginCheckException;
import ru.izpz.bot.exception.RocketChatSendException;
import ru.izpz.bot.keyboard.Buttons;
import ru.izpz.bot.keyboard.TelegramButtons;
import ru.izpz.bot.keyboard.TelegramKeyboardFactory;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileStatus;
import ru.izpz.dto.RocketChatSendResponse;
import ru.izpz.dto.model.ErrorResponseDTO;
import ru.izpz.dto.model.ParticipantV1DTO;


@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {

    @Value("${bot.admin}")
    private Long ADMIN_ID;

    private final OkHttpTelegramClient telegramClient;
    private final ProfileService profileService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void handleTextMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText().trim();
        ProfileDto profile;
        try {
            profile = profileService.getProfile(chatId);
            log.info("Profile: {}", profile.toString());
            //sendMessage(chatId, profile.toString());
            parseMessage(chatId, profile, text);
        } catch (FeignException e) {
            sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже", null);
            sendMessage(ADMIN_ID, e.contentUTF8(), null);
        }
    }

    public void handleCallbackMessage(Long chatId, String data, Integer messageId) {
        CallbackPayload payload;
        try {
            payload = objectMapper.readValue(data, CallbackPayload.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing callback data: {}", data, e);
            sendMessage(ADMIN_ID, e.getMessage(), null);
            return;
        }

        switch (payload.getCommand()) {
            case Buttons.REGISTRATION_CODE -> updateMessageAndChangeStatusRegistration(chatId, messageId, "Введите логин на платформе");
            default -> sendMessage(chatId, "Неизвестная команда: " + data, null);
        }
    }

    public void parseMessage(Long chatId, ProfileDto profile, String text) {
        switch(profile.status()) {
            case CREATED -> startOnboarding(chatId);
            case REGISTRATION -> startRegistration(chatId, profile, text);
            case VALIDATION -> startValidation(chatId, profile, text);
            case CONFIRMED -> startConfirmed(chatId, profile, text);
        }
    }

    private void startConfirmed(Long chatId, ProfileDto profile, String text) {
        if (text.equals("/start")) {
            // Отрпавить сообщение с текстом "выбери команду" из меню
            //startOnboarding(chatId);
            ReplyKeyboard keyboard = TelegramKeyboardFactory.createReplyKeyboard(TelegramButtons.MAIN_MENU, 3);
            sendMessage(chatId, "Выберите команду", keyboard);
        }
        // в ином случае нужно проверить ласт комманд и вызвать нужный метож
    }

    private void startValidation(Long chatId, ProfileDto profile, String text) {
        var code = profileService.getVerificationCode(profile.s21login());
        if (code.getSecretCode().equals(text)) {
            profileService.updateProfileStatus(chatId, ProfileStatus.CONFIRMED);
            sendMessage(chatId, "Ваш аккаунт был успешно зарегистрирован", TelegramKeyboardFactory.createReplyKeyboard(TelegramButtons.MAIN_MENU, 3));
        } else {
            sendMessage(chatId, "Введенный код не совпадает!", TelegramKeyboardFactory.removeReplyKeyboard());
        }
    }


    private void startRegistration(Long chatId, ProfileDto profile, String text) {
        if (!isValidLogin(text)) {
            sendMessage(chatId, "Введенный логин не соответствует требованиям", null);
            return;
        }

        ParticipantV1DTO participant;
        try {
            participant = profileService.checkEduLogin(text);
        } catch (EduLoginCheckException e) {
            ErrorResponseDTO error = e.getError();
            sendMessage(chatId, "Ошибка проверки логина: " + error.getMessage(), null);
            sendMessage(ADMIN_ID, "Ошибка проверки логина: " + error, null);
            return;
        }

        System.out.println(participant);

        if (participant.getStatus() != ParticipantV1DTO.StatusEnum.ACTIVE) {
            sendMessage(chatId, "Введенный логин не активен", null);
            return;
        }

        ProfileDto profileDto;
        try {
            profileDto = profileService.checkAndSetLogin(chatId, text);
        } catch (FeignException e) {
            sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже", null);
            sendMessage(ADMIN_ID, e.contentUTF8(), null);
            return;
        }
        // если логин совпадает значит мы его сохранили
        if (profileDto.s21login().equals(text)) {
            RocketChatSendResponse rocketChatResponse;
            try {
                rocketChatResponse = profileService.sendVerificationCode(text);
            } catch (RocketChatSendException e) {
                sendMessage(chatId, "Ошибка отправки сообщения в рокетчат, попробуйте позже", null);
                sendMessage(ADMIN_ID, e.getMessage(), null);
                return;
            } catch (FeignException e) {
                sendMessage(chatId, "Ошибка отправки сообщения в рокетчат, сообщите админу", null);
                sendMessage(ADMIN_ID, e.contentUTF8(), null);
                return;
            }

            sendMessage(chatId, "В рокет чат был отправлен код для подтверждения для логина " + text, null);
            sendMessage(ADMIN_ID, "В рокет чат было отправлено сообщение " + rocketChatResponse.getMessage(), null);
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

    private void startOnboarding(Long chatId) {
        InlineKeyboardMarkup keyboard = TelegramKeyboardFactory.createInlineKeyboardMarkup(Buttons.registration_button, 1);
        sendMessage(chatId, "Для регистрации нажмите кнопку ниже", keyboard);
    }

    private boolean isValidLogin(String login) {
        return login != null && login.matches("^[a-zA-Z]{3,9}$");
    }

    public void sendMessage(Long chatId, String text, ReplyKeyboard replyKeyboard) {
        SendMessage response = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(replyKeyboard)
                .build();

        try {
            telegramClient.execute(response);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения для {}: {}", chatId, text, e);
        }
    }

    public void removeInlineKeyboard(Long chatId, Integer messageId) {
        EditMessageReplyMarkup editMarkup = EditMessageReplyMarkup.builder()
                .chatId(chatId.toString()) // обязательно String, а не Long
                .messageId(messageId)
                .replyMarkup(null) // это удаляет клавиатуру
                .build();

        try {
            telegramClient.execute(editMarkup);
        } catch (TelegramApiException e) {
            log.error("Ошибка при удалении клавиатуры у сообщения {}: {}", messageId, e.getMessage());
        }
    }

    public void removeReplyKeyboard(Long chatId, String message) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId.toString())
                .text(message)
                .replyMarkup(new ReplyKeyboardRemove(true)) // удаляет клавиатуру
                .build();

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка при удалении reply keyboard: {}", e.getMessage());
        }
    }

    public void updateMessageAndChangeStatusRegistration(Long chatId, Integer messageId, String newText) {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId.toString()) // обязательно как String
                .messageId(messageId)
                .text(newText)
                .replyMarkup(null) // удаляет кнопки
                .build();

        try {
            telegramClient.execute(editMessage);
            profileService.updateProfileStatus(chatId, ProfileStatus.REGISTRATION);
        } catch (TelegramApiException e) {
            log.error("Ошибка при редактировании сообщения {}: {}", messageId, e.getMessage());
        } catch (FeignException e) {
            log.error("Ошибка обработки профиля", e);
        }
    }
}
