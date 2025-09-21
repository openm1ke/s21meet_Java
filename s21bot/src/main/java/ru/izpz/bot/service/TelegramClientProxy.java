package ru.izpz.bot.service;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramClientProxy {

    private final OkHttpTelegramClient delegate;

    @RateLimiter(name = "telegram")
    @Retry(name = "telegram")
    public void sendMessage(Long chatId, String text, ReplyKeyboard keyboard) throws TelegramApiException {
        var msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        delegate.execute(msg);
    }

    @RateLimiter(name = "telegram")
    @Retry(name = "telegram")
    public void editMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup kb) throws TelegramApiException {
        var edit = EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .text(text)
                .replyMarkup(kb)
                .build();
        delegate.execute(edit);
    }

    @RateLimiter(name = "telegram")
    @Retry(name = "telegram")
    public <T extends Serializable> T execute(BotApiMethod<T> method)
            throws TelegramApiException {
        return delegate.execute(method);
    }

}
