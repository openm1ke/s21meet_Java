package ru.school21.meet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.school21.meet.bot.S21MeetBot;

@Slf4j
@Configuration
public class BotConfig {

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.username}")
    private String botUsername;

    @Bean
    public S21MeetBot s21MeetBot() {
        return new S21MeetBot();
    }

    /**
     * Registers the bot with the Telegram API and initializes the command menu.
     * @param s21MeetBot The bot to register.
     * @return The TelegramBotsApi instance.
     * @throws Exception If there is an error registering the bot.
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(S21MeetBot s21MeetBot) throws Exception {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(s21MeetBot);
        s21MeetBot.initializeCommandMenu();
        return telegramBotsApi;
    }
}
