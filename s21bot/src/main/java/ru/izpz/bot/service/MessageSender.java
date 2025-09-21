package ru.izpz.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.izpz.dto.StatusChange;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSender {

    private final OkHttpTelegramClient telegramClient;

    public void sendMessage(String chatId, String text, ReplyKeyboardMarkup replyKeyboardMarkup) {
        this.sendMessage(Long.valueOf(chatId), text, replyKeyboardMarkup);
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

    private void sendNotifications(StatusChange change) {
        var status = change.newStatus() ? " is online" : " is offline";
        var login = change.login() + status;
        for (String telegramId : change.telegramIds()) {
            sendMessage(telegramId, login, null);
        }
    }

    public void sendStatusChanges(List<StatusChange> changes) {
        for (StatusChange c : changes) {
            sendNotifications(c);
        }
    }
}
