package ru.izpz.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonWebApp;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import ru.izpz.bot.property.BotProperties;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramWebAppMenuService {

    private static final String DEFAULT_BUTTON_TEXT = "\uD83D\uDD0E";
    private static final String PLACEHOLDER_WEBAPP_DOMAIN = "REPLACE_WITH_PUBLIC_WEBAPP_DOMAIN";

    private final MessageSender messageSender;
    private final BotProperties botProperties;
    private final Set<Long> configuredChats = ConcurrentHashMap.newKeySet();

    public void ensureMenuButton(Long chatId) {
        if (chatId == null || configuredChats.contains(chatId)) {
            return;
        }

        String webAppUrl = botProperties.webAppUrl();
        if (!isValidWebAppUrl(webAppUrl)) {
            log.warn("Web App menu button skipped for chatId={} because bot.web-app-url is invalid or placeholder: {}", chatId, webAppUrl);
            return;
        }

        MenuButtonWebApp menuButton = MenuButtonWebApp.builder()
            .text(DEFAULT_BUTTON_TEXT)
            .webAppInfo(WebAppInfo.builder().url(webAppUrl).build())
            .build();

        SetChatMenuButton method = SetChatMenuButton.builder()
            .chatId(chatId.toString())
            .menuButton(menuButton)
            .build();

        boolean applied = messageSender.execute(method).orElse(false);
        if (applied) {
            configuredChats.add(chatId);
            log.info("Web App menu button is set for chatId={} with url={}", chatId, webAppUrl);
        } else {
            log.warn("Failed to set Web App menu button for chatId={}", chatId);
        }
    }

    private boolean isValidWebAppUrl(String rawUrl) {
        if (!StringUtils.hasText(rawUrl) || rawUrl.contains(PLACEHOLDER_WEBAPP_DOMAIN)) {
            return false;
        }

        try {
            URI uri = URI.create(rawUrl.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && StringUtils.hasText(uri.getHost());
        } catch (Exception ex) {
            log.warn("Некорректный bot.web-app-url: {}", rawUrl);
            return false;
        }
    }
}
