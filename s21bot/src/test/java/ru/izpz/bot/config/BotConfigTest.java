package ru.izpz.bot.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ru.izpz.bot.bot.SimpleBot;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.bot.service.MessageProcessor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotConfigTest {

    @Mock
    private TelegramBotsLongPollingApplication botsApplication;

    @Mock
    private MessageProcessor messageProcessor;

    @Mock
    private BotSession botSession;

    @Test
    void botSession_registersBotAndReturnsSession() throws Exception {
        BotConfig config = new BotConfig();
        SimpleBot simpleBot = new SimpleBot(messageProcessor);
        BotProperties properties = new BotProperties("test-token", 1L, 1L, "https://example.org/invite");

        when(botsApplication.registerBot("test-token", simpleBot)).thenReturn(botSession);
        when(botSession.isRunning()).thenReturn(true);

        BotSession result = config.botSession(botsApplication, simpleBot, properties);

        assertSame(botSession, result);
        verify(botsApplication).registerBot("test-token", simpleBot);
    }

    @Test
    void simpleBot_createsInstance() {
        BotConfig config = new BotConfig();
        assertNotNull(config.simpleBot(messageProcessor));
    }
}
