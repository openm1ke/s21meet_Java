package ru.izpz.bot.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ru.izpz.bot.bot.SimpleBot;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.bot.service.MessageProcessor;
import ru.izpz.bot.service.MetricsService;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Mock
    private MetricsService metricsService;

    @Test
    void botSession_registersBotAndReturnsSession() throws Exception {
        BotConfig config = new BotConfig();
        SimpleBot simpleBot = new SimpleBot(messageProcessor);
        BotProperties properties = new BotProperties("test-token", 1L, 1L, "https://example.org/invite", null);

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

    @Test
    void botsApplication_createsInstance() {
        BotConfig config = new BotConfig();
        BotProperties properties = new BotProperties("test-token", 1L, 1L, "https://example.org/invite", null);
        okhttp3.OkHttpClient okHttpClient = config.telegramOkHttpClient(properties);
        assertNotNull(config.botsApplication(metricsService, okHttpClient));
    }

    @Test
    void telegramClient_proxyDisabled_usesDefaultClient() throws Exception {
        BotConfig config = new BotConfig();
        BotProperties properties = new BotProperties(
                "test-token",
                1L,
                1L,
                "https://example.org/invite",
                new BotProperties.ProxyProperties(false, "SOCKS", "xray-client", 1080)
        );

        okhttp3.OkHttpClient okHttpClient = config.telegramOkHttpClient(properties);
        OkHttpTelegramClient client = config.telegramClient(okHttpClient, properties);
        okhttp3.OkHttpClient rawClient = extractInternalClient(client);

        assertNotNull(client);
        assertSame(okHttpClient, rawClient);
        assertSame(null, rawClient.proxy());
    }

    @Test
    void telegramClient_proxyNull_usesDefaultClient() throws Exception {
        BotConfig config = new BotConfig();
        BotProperties properties = new BotProperties(
                "test-token",
                1L,
                1L,
                "https://example.org/invite",
                null
        );

        okhttp3.OkHttpClient okHttpClient = config.telegramOkHttpClient(properties);
        OkHttpTelegramClient client = config.telegramClient(okHttpClient, properties);
        okhttp3.OkHttpClient rawClient = extractInternalClient(client);

        assertNotNull(client);
        assertSame(null, rawClient.proxy());
    }

    @Test
    void telegramClient_proxyEnabled_socksConfigApplied() throws Exception {
        BotConfig config = new BotConfig();
        BotProperties properties = new BotProperties(
                "test-token",
                1L,
                1L,
                "https://example.org/invite",
                new BotProperties.ProxyProperties(true, "SOCKS", "xray-client", 1080)
        );

        okhttp3.OkHttpClient okHttpClient = config.telegramOkHttpClient(properties);
        OkHttpTelegramClient client = config.telegramClient(okHttpClient, properties);
        okhttp3.OkHttpClient rawClient = extractInternalClient(client);
        Proxy proxy = rawClient.proxy();
        InetSocketAddress address = assertInstanceOf(InetSocketAddress.class, proxy.address());

        assertNotNull(client);
        assertNotNull(proxy);
        assertEquals(Proxy.Type.SOCKS, proxy.type());
        assertEquals("xray-client", address.getHostString());
        assertEquals(1080, address.getPort());
    }

    @Test
    void telegramClient_proxyEnabled_httpConfigApplied() throws Exception {
        BotConfig config = new BotConfig();
        BotProperties properties = new BotProperties(
                "test-token",
                1L,
                1L,
                "https://example.org/invite",
                new BotProperties.ProxyProperties(true, "HTTP", "xray-client", 3128)
        );

        okhttp3.OkHttpClient okHttpClient = config.telegramOkHttpClient(properties);
        OkHttpTelegramClient client = config.telegramClient(okHttpClient, properties);
        okhttp3.OkHttpClient rawClient = extractInternalClient(client);
        Proxy proxy = rawClient.proxy();
        InetSocketAddress address = assertInstanceOf(InetSocketAddress.class, proxy.address());

        assertNotNull(client);
        assertNotNull(proxy);
        assertEquals(Proxy.Type.HTTP, proxy.type());
        assertEquals("xray-client", address.getHostString());
        assertEquals(3128, address.getPort());
    }

    @Test
    void telegramClient_proxyEnabled_blankType_defaultsToSocks() throws Exception {
        BotConfig config = new BotConfig();
        BotProperties properties = new BotProperties(
                "test-token",
                1L,
                1L,
                "https://example.org/invite",
                new BotProperties.ProxyProperties(true, "", "xray-client", 1080)
        );

        okhttp3.OkHttpClient okHttpClient = config.telegramOkHttpClient(properties);
        OkHttpTelegramClient client = config.telegramClient(okHttpClient, properties);
        okhttp3.OkHttpClient rawClient = extractInternalClient(client);
        Proxy proxy = rawClient.proxy();

        assertNotNull(client);
        assertNotNull(proxy);
        assertEquals(Proxy.Type.SOCKS, proxy.type());
    }

    @Test
    void telegramClient_proxyEnabled_invalidHostOrPort_throwsException() {
        BotConfig config = new BotConfig();
        BotProperties properties = new BotProperties(
                "test-token",
                1L,
                1L,
                "https://example.org/invite",
                new BotProperties.ProxyProperties(true, "SOCKS", "", 0)
        );

        assertThrows(IllegalStateException.class, () -> config.telegramOkHttpClient(properties));
    }

    @Test
    void telegramClient_proxyEnabled_nullPort_throwsException() {
        BotConfig config = new BotConfig();
        BotProperties properties = new BotProperties(
                "test-token",
                1L,
                1L,
                "https://example.org/invite",
                new BotProperties.ProxyProperties(true, "SOCKS", "xray-client", null)
        );

        assertThrows(IllegalStateException.class, () -> config.telegramOkHttpClient(properties));
    }

    @Test
    void telegramClient_proxyEnabled_negativePort_throwsException() {
        BotConfig config = new BotConfig();
        BotProperties properties = new BotProperties(
                "test-token",
                1L,
                1L,
                "https://example.org/invite",
                new BotProperties.ProxyProperties(true, "SOCKS", "xray-client", -1)
        );

        assertThrows(IllegalStateException.class, () -> config.telegramOkHttpClient(properties));
    }

    @Test
    void telegramClient_proxyEnabled_unsupportedType_throwsException() {
        BotConfig config = new BotConfig();
        BotProperties properties = new BotProperties(
                "test-token",
                1L,
                1L,
                "https://example.org/invite",
                new BotProperties.ProxyProperties(true, "INVALID", "xray-client", 1080)
        );

        assertThrows(IllegalStateException.class, () -> config.telegramOkHttpClient(properties));
    }

    private okhttp3.OkHttpClient extractInternalClient(OkHttpTelegramClient telegramClient) throws Exception {
        Field field = OkHttpTelegramClient.class.getDeclaredField("client");
        field.setAccessible(true);
        return (okhttp3.OkHttpClient) field.get(telegramClient);
    }
}
