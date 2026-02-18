package ru.izpz.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.izpz.dto.StatusChange;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSender {

    private final TelegramClientProxy telegramClient;

    public void sendMessage(Long chatId, String text, ReplyKeyboard replyKeyboard) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(replyKeyboard)
                .build();
        execute(msg);
    }

    public void updateMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        EditMessageText edit = EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        execute(edit);
    }

    private void sendNotifications(StatusChange change) {
        var status = change.newStatus() ? " is online" : " is offline";
        var login = change.login() + status;
        for (String telegramId : change.telegramIds()) {
            try {
                sendMessage(Long.parseLong(telegramId), login, null);
            } catch (NumberFormatException e) {
                log.warn("Некорректный telegramId для уведомления: {} (login={})", telegramId, change.login());
            }
        }
    }

    public void sendStatusChanges(List<StatusChange> changes) {
        for (StatusChange c : changes) {
            sendNotifications(c);
        }
    }

    public void removeInlineKeyboard(Long chatId, Integer messageId) {
        EditMessageReplyMarkup editMarkup = EditMessageReplyMarkup.builder()
                .chatId(chatId.toString()) // обязательно String, а не Long
                .messageId(messageId)
                .replyMarkup(null) // это удаляет клавиатуру
                .build();

        execute(editMarkup);
    }

    public void removeReplyKeyboard(Long chatId, String message) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId.toString())
                .text(message)
                .replyMarkup(new ReplyKeyboardRemove(true)) // удаляет клавиатуру
                .build();

        execute(sendMessage);
    }

    public void answerCallbackQuery(String callbackId, String toastText, boolean showAlert) {
        AnswerCallbackQuery method = AnswerCallbackQuery.builder()
                .callbackQueryId(callbackId)
                .text(toastText)
                .showAlert(showAlert)
                .build();
        execute(method);
    }

    public <T extends Serializable> Optional<T> execute(BotApiMethod<T> method) {
        try {
            return Optional.ofNullable(telegramClient.execute(method));
        } catch (TelegramApiException e) {
            log.error("Ошибка вызова Telegram API метода {}: {}", method.getMethod(), e.getMessage(), e);
            return Optional.empty();
        }
    }
}
