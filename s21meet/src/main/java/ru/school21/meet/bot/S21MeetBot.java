package ru.school21.meet.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.school21.meet.bot.MessageHandler;
import ru.school21.meet.bot.MyCommandMenu;

@Slf4j
@Component
public class S21MeetBot extends TelegramLongPollingBot {

    @Value("${bot.token}")
    private String botToken;
    @Value("${bot.username}")
    private String botUsername;
    @Autowired
    @Lazy
    private MessageHandler messageHandler;

    /**
     * Returns the token of the bot.
     *
     * @return the token of the bot.
     */
    @Override
    public String getBotToken() {
        return botToken;
    }

    /**
     * Returns the username of the bot.
     *
     * @return the username of the bot.
     */
    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Handles an update by passing the message from the update to the
     * {@link MessageHandler}.
     *
     * @param update the update received from Telegram
     */
    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText() && update.getMessage().isUserMessage()) {
            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            log.info("chatid {} with message {}", chatId, message);
            messageHandler.handle(chatId, message);
        }
    }

    /**
     * Initializes the command menu for this bot by deleting all commands in
     * all group chats.
     */
    public void initializeCommandMenu() {
        try {
            execute(MyCommandMenu.deleteMenuGroup());
            execute(MyCommandMenu.deleteMenuPrivate());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
