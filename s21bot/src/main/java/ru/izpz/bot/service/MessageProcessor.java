package ru.izpz.bot.service;

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
import ru.izpz.dto.ProfileDto;
import ru.izpz.dto.ProfileStatus;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {

    @Value("${bot.admin}")
    private Long ADMIN_ID;

    private final OkHttpTelegramClient telegramClient;
    private final ProfileService profileService;

    public void process(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();
        ProfileDto profile;
        try {
            profile = profileService.getProfile(chatId);
            log.info("Profile: {}", profile.toString());
            //sendMessage(chatId, profile.toString());
            parseMessage(chatId, profile, text);
        } catch (FeignException e) {
            sendMessage(chatId, "Ошибка обработки профиля, попробуйте позже");
            sendMessage(ADMIN_ID, e.contentUTF8());
        }
    }

    public void parseMessage(Long chatId, ProfileDto profile, String text) {
        switch(profile.status()) {
            case CREATED -> sendMessage(chatId, "Для регистрации нажмите кнопку ниже");
            case VALIDATION -> sendMessage(chatId, "Вам был отправлен код для подтверждения");
            case REGISTERED -> sendMessage(chatId, "Вы зарегистрированы");
        }
    }

    public void sendMessage(Long chatId, String message) {

        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text("Регистрация")
                .callbackData("start_registration")
                .build();
        var row = new InlineKeyboardRow();
        row.add(button);

        List<InlineKeyboardRow> keyboard = List.of(row);

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build();

        SendMessage response = SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .replyMarkup(markup)
                .build();
        try {
            telegramClient.execute(response);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения для {}: {}", chatId, message, e);
        }
    }

    public void handleCallback(Long chatId, String data, Integer messageId) {
        switch (data) {
            case "start_registration" -> {
                //sendMessage(chatId, "Вы нажали кнопку регистрации");
                //removeInlineKeyboard(chatId, messageId);
                profileService.updateProfileStatus(chatId, ProfileStatus.VALIDATION);
                editMessageAndRemoveKeyboard(chatId, messageId, "Введите логин на платформе");
            }
            case "help" -> sendMessage(chatId, "Вот инструкция по использованию бота...");
            default -> sendMessage(chatId, "Неизвестная команда: " + data);
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

    public void editMessageAndRemoveKeyboard(Long chatId, Integer messageId, String newText) {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId.toString()) // обязательно как String
                .messageId(messageId)
                .text(newText)
                .replyMarkup(null) // удаляет кнопки
                .build();

        try {
            telegramClient.execute(editMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка при редактировании сообщения {}: {}", messageId, e.getMessage());
        }
    }
}
