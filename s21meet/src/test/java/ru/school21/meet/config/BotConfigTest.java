package ru.school21.meet.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ru.school21.meet.bot.SimpleBot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Import(BotConfig.class)
class BotConfigTest {

    private TelegramBotsLongPollingApplication botsApplication = mock(TelegramBotsLongPollingApplication.class);

    private OkHttpTelegramClient telegramClient = mock(OkHttpTelegramClient.class);

    private SimpleBot simpleBot = mock(SimpleBot.class);

    @Test
    void contextLoads() {
        assertThat(botsApplication).isNotNull();
        assertThat(telegramClient).isNotNull();
        assertThat(simpleBot).isNotNull();
    }
}
