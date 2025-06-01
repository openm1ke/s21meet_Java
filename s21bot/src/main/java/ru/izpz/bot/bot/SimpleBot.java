package ru.izpz.bot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.izpz.bot.service.MessageProcessor;

@Slf4j
@RequiredArgsConstructor
public class SimpleBot implements LongPollingSingleThreadUpdateConsumer {

    private final MessageProcessor messageProcessor;

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String type = message.getChat().getType();
            if (type.equals("private")) {
                log.info("{} написал {}", message.getChat().getUserName(), message.getText());
                messageProcessor.process(message);
            }
        }
    }
}
