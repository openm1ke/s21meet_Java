package ru.izpz.bot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@RequiredArgsConstructor
public class SimpleBot implements LongPollingSingleThreadUpdateConsumer {

    private final OkHttpTelegramClient telegramClient;

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            Chat chat = message.getChat();
            if (chat.getType().equals("private")) {
                log.info("{} написал {}", chat.getUserName(), message.getText());
                SendMessage response = SendMessage.builder()
                        .chatId(message.getChatId())
                        .text("Вы написали: " + message.getText())
                        .build();

                try {
                    telegramClient.execute(response);
                } catch (TelegramApiException e) {
                    log.error("Ошибка отправки сообщения", e);
                }
            }
        }
    }
}
