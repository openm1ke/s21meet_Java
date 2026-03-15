package ru.izpz.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramExecutorService {

    private final TelegramClientProxy telegramClient;
    private final MetricsService metricsService;
    private static final String ERROR_OUTCOME = "error";

    public <T extends Serializable> Optional<T> execute(BotApiMethod<T> method) {
        String methodName = resolveMethodName(method);
        try {
            Optional<T> result = Optional.ofNullable(telegramClient.execute(method));
            metricsService.recordTelegramApiRequest(methodName, result.isPresent() ? "success" : ERROR_OUTCOME);
            return result;
        } catch (TelegramApiException e) {
            metricsService.recordTelegramApiRequest(methodName, ERROR_OUTCOME);
            log.error("Ошибка вызова Telegram API метода {}: {}", methodName, e.getMessage(), e);
            return Optional.empty();
        } catch (RuntimeException e) {
            metricsService.recordTelegramApiRequest(methodName, ERROR_OUTCOME);
            log.error("Неожиданная ошибка вызова Telegram API метода {}: {}", methodName, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private String resolveMethodName(BotApiMethod<?> method) {
        if (method == null || !StringUtils.hasText(method.getMethod())) {
            return "unknown";
        }
        return method.getMethod();
    }
}
