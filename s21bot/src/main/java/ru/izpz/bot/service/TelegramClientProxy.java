package ru.izpz.bot.service;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramClientProxy {

    private final OkHttpTelegramClient delegate;

    @RateLimiter(name = "telegram")
    @Retry(name = "telegram")
    public <T extends Serializable> T execute(BotApiMethod<T> method)
            throws TelegramApiException {
        return delegate.execute(method);
    }

}
