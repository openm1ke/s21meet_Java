package ru.izpz.bot.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bot")
public record BotProperties(
        String token,
        Long admin,
        Long group,
        String groupInviteLink,
        ProxyProperties proxy
) {
    public record ProxyProperties(
            Boolean enabled,
            String type,
            String host,
            Integer port
    ) {
    }
}
