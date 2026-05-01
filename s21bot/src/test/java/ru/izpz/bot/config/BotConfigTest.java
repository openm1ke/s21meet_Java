package ru.izpz.bot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ru.izpz.bot.bot.SimpleBot;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.bot.service.MessageProcessor;
import ru.izpz.bot.service.MetricsService;

@ExtendWith(MockitoExtension.class)
class BotConfigTest {
  private static final String TOKEN = "test-token";
  private static final String INVITE = "https://example.org/invite";
  private static final String WEBAPP = "https://example.org/webapp";

  @Mock private TelegramBotsLongPollingApplication botsApplication;

  @Mock private MessageProcessor messageProcessor;

  @Mock private BotSession botSession;

  @Mock private MetricsService metricsService;
  private BotConfig config;

  @BeforeEach
  void setUp() {
    config = new BotConfig();
  }

  @Test
  void botSession_registersBotAndReturnsSession() throws Exception {
    SimpleBot simpleBot = new SimpleBot(messageProcessor);
    BotProperties properties = properties(null);

    when(botsApplication.registerBot(TOKEN, simpleBot)).thenReturn(botSession);
    when(botSession.isRunning()).thenReturn(true);

    assertSame(botSession, config.botSession(botsApplication, simpleBot, properties));
    verify(botsApplication).registerBot(TOKEN, simpleBot);
  }

  @Test
  void simpleBot_createsInstance() {
    assertNotNull(config.simpleBot(messageProcessor));
  }

  @Test
  void botsApplication_createsInstance() {
    BotProperties properties = properties(null);
    okhttp3.OkHttpClient okHttpClient = config.telegramOkHttpClient(properties);
    assertNotNull(config.botsApplication(metricsService, okHttpClient));
  }

  @Test
  void telegramClient_proxyDisabled_usesDefaultClient() {
    BotProperties properties =
        properties(new BotProperties.ProxyProperties(false, "SOCKS", "xray-client", 1080));

    okhttp3.OkHttpClient okHttpClient = config.telegramOkHttpClient(properties);
    OkHttpTelegramClient client = config.telegramClient(okHttpClient, properties);
    okhttp3.OkHttpClient rawClient = extractInternalClient(client);

    assertNotNull(client);
    assertSame(okHttpClient, rawClient);
    assertSame(null, rawClient.proxy());
  }

  @Test
  void telegramClient_proxyNull_usesDefaultClient() {
    BotProperties properties = properties(null);

    okhttp3.OkHttpClient okHttpClient = config.telegramOkHttpClient(properties);
    OkHttpTelegramClient client = config.telegramClient(okHttpClient, properties);
    okhttp3.OkHttpClient rawClient = extractInternalClient(client);

    assertNotNull(client);
    assertSame(null, rawClient.proxy());
  }

  @ParameterizedTest
  @MethodSource("validProxyCases")
  void telegramClient_proxyEnabled_appliesResolvedProxyConfig(
      String type, int port, Proxy.Type expectedType) {
    BotProperties properties =
        properties(new BotProperties.ProxyProperties(true, type, "xray-client", port));

    okhttp3.OkHttpClient okHttpClient = config.telegramOkHttpClient(properties);
    OkHttpTelegramClient client = config.telegramClient(okHttpClient, properties);
    okhttp3.OkHttpClient rawClient = extractInternalClient(client);
    Proxy proxy = rawClient.proxy();
    InetSocketAddress address = assertInstanceOf(InetSocketAddress.class, proxy.address());

    assertEquals("xray-client", address.getHostString());
    assertNotNull(client);
    assertNotNull(proxy);
    assertEquals(expectedType, proxy.type());
    assertEquals(port, address.getPort());
  }

  @ParameterizedTest
  @MethodSource("invalidProxyCases")
  void telegramClient_proxyEnabled_invalidConfig_throwsException(
      String type, String host, Integer port) {
    BotProperties properties =
        properties(new BotProperties.ProxyProperties(true, type, host, port));
    assertThrows(IllegalStateException.class, () -> config.telegramOkHttpClient(properties));
  }

  private static Stream<Arguments> validProxyCases() {
    return Stream.of(
        Arguments.of("SOCKS", 1080, Proxy.Type.SOCKS),
        Arguments.of("HTTP", 3128, Proxy.Type.HTTP),
        Arguments.of("", 1080, Proxy.Type.SOCKS));
  }

  private static Stream<Arguments> invalidProxyCases() {
    return Stream.of(
        Arguments.of("SOCKS", "", 0),
        Arguments.of("SOCKS", "xray-client", null),
        Arguments.of("SOCKS", "xray-client", -1),
        Arguments.of("INVALID", "xray-client", 1080));
  }

  private static BotProperties properties(BotProperties.ProxyProperties proxy) {
    return new BotProperties(TOKEN, 1L, 1L, INVITE, WEBAPP, proxy);
  }

  private okhttp3.OkHttpClient extractInternalClient(OkHttpTelegramClient telegramClient) {
    return (okhttp3.OkHttpClient) ReflectionTestUtils.getField(telegramClient, "client");
  }
}
