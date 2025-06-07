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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.izpz.bot.dto.CallbackPayload;
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileStatus;

import java.util.List;
import java.util.Map;

import static ru.izpz.bot.service.Buttons.registration_button;

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
            case "registration" -> updateMessageAndChangeStatusRegistration(chatId, messageId, "Введите логин на платформе");
            case "help" -> sendMessage(chatId, "Вот инструкция по использованию бота...", null);
            default -> sendMessage(chatId, "Неизвестная команда: " + data, null);
        }
    }

    public void parseMessage(Long chatId, ProfileDto profile, String text) {
        switch(profile.status()) {
            case CREATED -> startOnboarding(chatId);
            case REGISTRATION -> startRegistration(chatId, profile, text);
            case VALIDATION -> {

                sendMessage(chatId, "Вам был отправлен код для подтверждения для логина " + text + " в рокет чат", null);
            }

            case CONFIRMED -> sendMessage(chatId, "Вы зарегистрированы", null);
        }
    }



    private void startRegistration(Long chatId, ProfileDto profile, String text) {
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
        sendMessage(chatId, "Для регистрации нажмите кнопку ниже", registration_button);
    }



    public void sendMessage(Long chatId, String text, Map<String, String> buttons) {
        InlineKeyboardMarkup markup = buttons == null || buttons.isEmpty()
                ? null
                : createInlineKeyboardMarkup(buttons);

        SendMessage response = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(markup)
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

    public InlineKeyboardMarkup createInlineKeyboardMarkup(Map<String, String> buttons) {
        var row = new InlineKeyboardRow();

        buttons.forEach((key, value) -> {
            var button = InlineKeyboardButton.builder()
                    .text(key)
                    .callbackData(value)
                    .build();
            row.add(button);
        });

        var markup = InlineKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .build();
        return markup;
    }
}
