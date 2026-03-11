package ru.izpz.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramExecutorService {

    private final TelegramClientProxy telegramClient;

    public <T extends Serializable> Optional<T> execute(BotApiMethod<T> method) {
        try {
            return Optional.ofNullable(telegramClient.execute(method));
        } catch (TelegramApiException e) {
            log.error("Ошибка вызова Telegram API метода {}: {}", method.getMethod(), e.getMessage(), e);
            return Optional.empty();
        }
    }
}
