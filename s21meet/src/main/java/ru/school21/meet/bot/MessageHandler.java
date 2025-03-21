package ru.school21.meet.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.school21.meet.service.TelegramMessageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageHandler {
    private final TelegramMessageService telegramMessageService;

    public void handle(long chatId, String message) {
        message = message.trim();
        telegramMessageService.sendMessage(chatId, message);
    }
}
