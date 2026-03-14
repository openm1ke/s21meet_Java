package ru.izpz.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import ru.izpz.dto.StatusChange;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSender {

    private final TelegramExecutorService telegramExecutorService;
    private final MetricsService metricsService;

    public void sendMessage(Long chatId, String text, ReplyKeyboard replyKeyboard) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(replyKeyboard)
                .build();
        telegramExecutorService.execute(msg);
    }

    public void updateMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        EditMessageText edit = EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        telegramExecutorService.execute(edit);
    }

    private void sendNotifications(StatusChange change) {
        var status = change.newStatus() ? " is online" : " is offline";
        var login = change.login() + status;
        for (String telegramId : change.telegramIds()) {
            try {
                SendMessage msg = SendMessage.builder()
                        .chatId(String.valueOf(Long.parseLong(telegramId)))
                        .text(login)
                        .build();
                boolean delivered = telegramExecutorService.execute(msg).isPresent();
                metricsService.recordNotifyDelivery(delivered ? "success" : "error");
            } catch (NumberFormatException e) {
                metricsService.recordProcessingError("notify_delivery", "invalid_telegram_id");
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

        telegramExecutorService.execute(editMarkup);
    }

    public void removeReplyKeyboard(Long chatId, String message) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId.toString())
                .text(message)
                .replyMarkup(new ReplyKeyboardRemove(true)) // удаляет клавиатуру
                .build();

        telegramExecutorService.execute(sendMessage);
    }

    public void answerCallbackQuery(String callbackId, String toastText, boolean showAlert) {
        AnswerCallbackQuery method = AnswerCallbackQuery.builder()
                .callbackQueryId(callbackId)
                .text(toastText)
                .showAlert(showAlert)
                .build();
        telegramExecutorService.execute(method);
    }

    public <T extends Serializable> Optional<T> execute(BotApiMethod<T> method) {
        return telegramExecutorService.execute(method);
    }
}
