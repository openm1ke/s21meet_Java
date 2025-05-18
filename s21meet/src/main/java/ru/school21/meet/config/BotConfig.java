package ru.school21.meet.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ru.school21.meet.bot.SimpleBot;

@Slf4j
@Configuration
public class BotConfig {

    @Value("${bot.token}")
    private String botToken;

    @Bean
    public OkHttpTelegramClient telegramClient() {
        return new OkHttpTelegramClient(botToken);
    }

    @Bean
    public TelegramBotsLongPollingApplication botsApplication() {
        return new TelegramBotsLongPollingApplication();
    }

    @Bean
    public SimpleBot simpleBot(OkHttpTelegramClient telegramClient) {
        return new SimpleBot(telegramClient);
    }

    @Bean
    public BotSession botSession(TelegramBotsLongPollingApplication botsApplication, SimpleBot simpleBot) throws Exception {
        BotSession session = botsApplication.registerBot(botToken, simpleBot);
        log.info("✅ Telegram бот успешно запущен: {}", session.isRunning());
        return session;
    }
}
