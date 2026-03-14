package ru.izpz.bot.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.ExponentialBackOff;
import ru.izpz.bot.bot.SimpleBot;
import ru.izpz.bot.property.BotProperties;
import ru.izpz.bot.service.MetricsBackOff;
import ru.izpz.bot.service.MetricsService;
import ru.izpz.bot.service.MessageProcessor;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.Locale;

@Slf4j
@Configuration
public class BotConfig {

    @Bean
    public OkHttpTelegramClient telegramClient(BotProperties botProperties) {
        OkHttpClient okHttpClient = createTelegramOkHttpClient(botProperties);
        return new OkHttpTelegramClient(okHttpClient, botProperties.token());
    }

    private OkHttpClient createTelegramOkHttpClient(BotProperties botProperties) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                // Telegram long polling uses getUpdates timeout=50s, so read timeout must be higher.
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(70))
                .writeTimeout(Duration.ofSeconds(30));

        BotProperties.ProxyProperties proxy = botProperties.proxy();
        if (proxy != null && Boolean.TRUE.equals(proxy.enabled())) {
            if (!StringUtils.hasText(proxy.host()) || proxy.port() == null || proxy.port() <= 0) {
                throw new IllegalStateException("BOT_PROXY_ENABLED=true, but BOT_PROXY_HOST/BOT_PROXY_PORT are invalid");
            }
            Proxy.Type proxyType = resolveProxyType(proxy.type());
            OkHttpClient okHttpClient = builder
                    .proxy(new Proxy(proxyType, new InetSocketAddress(proxy.host(), proxy.port())))
                    .build();
            log.info("Telegram client proxy enabled: type={}, host={}, port={}", proxyType, proxy.host(), proxy.port());
            return okHttpClient;
        }
        return builder.build();
    }

    private Proxy.Type resolveProxyType(String rawType) {
        if (!StringUtils.hasText(rawType)) {
            return Proxy.Type.SOCKS;
        }
        return switch (rawType.trim().toUpperCase(Locale.ROOT)) {
            case "SOCKS" -> Proxy.Type.SOCKS;
            case "HTTP" -> Proxy.Type.HTTP;
            default -> throw new IllegalStateException("Unsupported BOT_PROXY_TYPE: " + rawType);
        };
    }

    @Bean
    public TelegramBotsLongPollingApplication botsApplication(MetricsService metricsService, BotProperties botProperties) {
        OkHttpClient pollingClient = createTelegramOkHttpClient(botProperties);
        return new TelegramBotsLongPollingApplication(
                com.fasterxml.jackson.databind.ObjectMapper::new,
                () -> pollingClient,
                Executors::newSingleThreadScheduledExecutor,
                () -> new MetricsBackOff(new ExponentialBackOff(), metricsService)
        );
    }

    @Bean
    public SimpleBot simpleBot(MessageProcessor messageProcessor) {
        return new SimpleBot(messageProcessor);
    }

    @Bean
    @ConditionalOnProperty(name = "bot.session.enabled", havingValue = "true", matchIfMissing = true)
    public BotSession botSession(TelegramBotsLongPollingApplication botsApplication, SimpleBot simpleBot, BotProperties botProperties) throws Exception {
        BotSession session = botsApplication.registerBot(botProperties.token(), simpleBot);
        log.info("✅ Telegram бот успешно запущен: {}", session.isRunning());
        return session;
    }
}
