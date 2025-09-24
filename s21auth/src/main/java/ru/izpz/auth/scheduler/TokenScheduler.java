package ru.izpz.auth.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.izpz.auth.service.TokenService;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "token.scheduler.enabled", havingValue = "true")
public class TokenScheduler {

    @Value("${edu.login}")
    private String defaultLogin;
    @Value("${edu.password}")
    private String defaultPassword;

    private final TokenService tokenService;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("Init default token for {}", defaultLogin);
        tokenService.getAccessToken(defaultLogin, defaultPassword);
    }

    /**
     * Периодически обновляет токены для всех сохранённых пользователей.
     * Выполняется каждые 5 минут.
     */
    @Scheduled(fixedDelayString = "${token.scheduler.refresh-delay:300000}")
    public void refreshTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenService.findAll().forEach(tokenEntity -> {
            if (tokenEntity.getExpiresAt() == null ||
                    tokenEntity.getExpiresAt().isBefore(now.plusMinutes(10))) {
                try {
                    tokenService.getAccessToken(tokenEntity.getLogin(), tokenEntity.getPassword());
                    log.info("Токен для {} успешно обновлён.", tokenEntity.getLogin());
                } catch (Exception e) {
                    log.error("Ошибка обновления токена для {}: {}", tokenEntity.getLogin(), e.getMessage(), e);
                }
            }
        });
    }
}
