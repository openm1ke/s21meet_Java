package ru.izpz.bot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ru.izpz.bot.bot.SimpleBot;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.bot.service.MessageProcessor;

@Slf4j
@Configuration
public class BotConfig {

    @Bean
    public OkHttpTelegramClient telegramClient(BotProperties botProperties) {
        return new OkHttpTelegramClient(botProperties.token());
    }

    @Bean
    public TelegramBotsLongPollingApplication botsApplication() {
        return new TelegramBotsLongPollingApplication();
    }

    @Bean
    public SimpleBot simpleBot(MessageProcessor messageProcessor) {
        return new SimpleBot(messageProcessor);
    }

    @Bean
    public BotSession botSession(TelegramBotsLongPollingApplication botsApplication, SimpleBot simpleBot, BotProperties botProperties) throws Exception {
        BotSession session = botsApplication.registerBot(botProperties.token(), simpleBot);
        log.info("✅ Telegram бот успешно запущен: {}", session.isRunning());
        return session;
    }
}
